package com.siri.gemini.ble

import android.annotation.SuppressLint
import android.app.Notification
import android.app.Service
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
import com.siri.gemini.widget.AirPodsWidgetProvider
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

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

            // Push Continuity battery into widget when AAP not yet connected
            val approx = AapProtocol.BatteryInfo(
                left = status.leftBattery,
                right = status.rightBattery,
                case = status.caseBattery
            )
            AirPodsWidgetProvider.refreshAll(this@AirPodsGestureService, approx, null)

            if (aap == null && bondedAirPods != null) {
                aap = AapConnection(bondedAirPods!!, scope, applicationContext).also { it.connect() }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.w(TAG, "scan failed $errorCode")
            scanning.set(false)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())

        when (intent?.action) {
            ACTION_TRIGGER_STEM -> scope.launch {
                GestureEventBus.emit(GestureEventBus.Event.StemPress(source = "manual"))
            }
            ACTION_SET_NOISE -> {
                val mode = AapProtocol.NoiseMode.from(
                    intent.getIntExtra(EXTRA_NOISE_MODE, AapProtocol.NoiseMode.OFF.code)
                )
                aap?.setNoiseMode(mode)
                    ?: AirPodsWidgetProvider.refreshAll(this, null, mode)
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
        val adapter = getSystemService(BluetoothManager::class.java)?.adapter ?: return
        bondedAirPods = adapter.bondedDevices?.firstOrNull { dev ->
            val n = dev.name?.lowercase().orEmpty()
            n.contains("airpods") || n.contains("beats")
        }
    }

    @SuppressLint("MissingPermission")
    private fun startScanning() {
        if (scanning.getAndSet(true)) return
        val adapter = getSystemService(BluetoothManager::class.java)?.adapter
        if (adapter == null || !adapter.isEnabled) {
            scanning.set(false)
            return
        }
        scanner = adapter.bluetoothLeScanner
        val filter = ScanFilter.Builder()
            .setManufacturerData(ContinuityParser.APPLE_COMPANY_ID, byteArrayOf(), byteArrayOf())
            .build()
        val settings = ScanSettings.Builder()
            .setScanMode(if (aggressive) ScanSettings.SCAN_MODE_LOW_LATENCY else ScanSettings.SCAN_MODE_LOW_POWER)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
            .setReportDelay(0)
            .build()
        try {
            scanner?.startScan(listOf(filter), settings, scanCallback)
        } catch (e: SecurityException) {
            scanning.set(false)
        }

        scope.launch {
            while (isActive) {
                delay(15_000)
                val ago = System.currentTimeMillis() - lastNearbyMs.get()
                val want = ago < 30_000
                if (want != aggressive) {
                    aggressive = want
                    restartScan()
                }
                if (ago > 60_000) GestureEventBus.tryEmit(GestureEventBus.Event.AirPodsLost)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun restartScan() {
        try { scanner?.stopScan(scanCallback) } catch (_: Exception) {}
        scanning.set(false)
        startScanning()
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, SiriGeminiApp.CHANNEL_GESTURE)
            .setContentTitle(getString(R.string.notification_title_listening))
            .setContentText(getString(R.string.notification_text_ready))
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

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
