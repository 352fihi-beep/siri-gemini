package com.siri.gemini.ble

import android.annotation.SuppressLint
import android.app.Notification
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.siri.gemini.R
import com.siri.gemini.SiriGeminiApp
import com.siri.gemini.ble.aap.AapConnection
import com.siri.gemini.ble.aap.AapProtocol
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * Foreground service owning:
 * 1. Continuity BLE ad scan (nearby + battery hints)
 * 2. AAP / L2CAP connection for full stem + ANC + ear events (LibrePods path)
 */
class AirPodsGestureService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var scanner: BluetoothLeScanner? = null
    private val scanning = AtomicBoolean(false)
    private val lastNearbyMs = AtomicLong(0L)
    private var aggressive = false

    private var aap: AapConnection? = null
    private var bondedAirPods: BluetoothDevice? = null

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result ?: return
            val mfg = result.scanRecord?.getManufacturerSpecificData(ContinuityParser.APPLE_COMPANY_ID)
            val status = ContinuityParser.parse(mfg) ?: return

            lastNearbyMs.set(System.currentTimeMillis())
            GestureEventBus.tryEmit(GestureEventBus.Event.AirPodsNearby(status))

            // If we see Continuity and have a bonded device, try AAP upgrade
            if (aap == null && bondedAirPods != null) {
                aap = AapConnection(bondedAirPods!!, scope).also { it.connect() }
            }

            Log.d(TAG, "Continuity: L=${status.leftBattery} R=${status.rightBattery}")
        }

        override fun onScanFailed(errorCode: Int) {
            Log.w(TAG, "BLE scan failed: $errorCode")
            scanning.set(false)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())

        when (intent?.action) {
            ACTION_TRIGGER_STEM -> {
                scope.launch {
                    GestureEventBus.emit(GestureEventBus.Event.StemPress(source = "manual"))
                }
            }
            ACTION_SET_NOISE -> {
                val modeCode = intent.getIntExtra(EXTRA_NOISE_MODE, AapProtocol.NoiseMode.OFF.code)
                aap?.setNoiseMode(AapProtocol.NoiseMode.from(modeCode))
            }
            else -> {
                discoverBondedAirPods()
                startScanning()
            }
        }
        return START_STICKY
    }

    @SuppressLint("MissingPermission")
    private fun discoverBondedAirPods() {
        val btManager = getSystemService(BluetoothManager::class.java)
        val adapter = btManager?.adapter ?: return
        bondedAirPods = adapter.bondedDevices?.firstOrNull { dev ->
            val name = dev.name?.lowercase().orEmpty()
            name.contains("airpods") || name.contains("beats")
        }
        if (bondedAirPods != null) {
            Log.i(TAG, "Found bonded AirPods: ${bondedAirPods?.name}")
        }
    }

    @SuppressLint("MissingPermission")
    private fun startScanning() {
        if (scanning.getAndSet(true)) return

        val btManager = getSystemService(BluetoothManager::class.java)
        val adapter = btManager?.adapter
        if (adapter == null || !adapter.isEnabled) {
            scanning.set(false)
            return
        }

        scanner = adapter.bluetoothLeScanner
        val filter = ScanFilter.Builder()
            .setManufacturerData(ContinuityParser.APPLE_COMPANY_ID, byteArrayOf(), byteArrayOf())
            .build()

        val settings = ScanSettings.Builder()
            .setScanMode(
                if (aggressive) ScanSettings.SCAN_MODE_LOW_LATENCY
                else ScanSettings.SCAN_MODE_LOW_POWER
            )
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
            .setReportDelay(0)
            .build()

        try {
            scanner?.startScan(listOf(filter), settings, scanCallback)
            Log.i(TAG, "Continuity scan started (aggressive=$aggressive)")
        } catch (e: SecurityException) {
            Log.e(TAG, "Missing BLE permission", e)
            scanning.set(false)
        }

        scope.launch {
            while (isActive) {
                delay(15_000)
                val ago = System.currentTimeMillis() - lastNearbyMs.get()
                val shouldBeAggressive = ago < 30_000
                if (shouldBeAggressive != aggressive) {
                    aggressive = shouldBeAggressive
                    restartScan()
                }
                if (ago > 60_000) {
                    GestureEventBus.tryEmit(GestureEventBus.Event.AirPodsLost)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun restartScan() {
        try { scanner?.stopScan(scanCallback) } catch (_: Exception) {}
        scanning.set(false)
        startScanning()
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, SiriGeminiApp.CHANNEL_GESTURE)
            .setContentTitle(getString(R.string.notification_title_listening))
            .setContentText(getString(R.string.notification_text_ready))
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    @SuppressLint("MissingPermission")
    override fun onDestroy() {
        try { scanner?.stopScan(scanCallback) } catch (_: Exception) {}
        aap?.close()
        scanning.set(false)
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "AirPodsGesture"
        private const val NOTIFICATION_ID = 42
        const val ACTION_TRIGGER_STEM = "com.siri.gemini.action.TRIGGER_STEM"
        const val ACTION_SET_NOISE = "com.siri.gemini.action.SET_NOISE"
        const val EXTRA_NOISE_MODE = "noise_mode"
    }
}
