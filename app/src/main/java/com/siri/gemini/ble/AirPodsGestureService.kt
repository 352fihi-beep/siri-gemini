package com.siri.gemini.ble

import android.annotation.SuppressLint
import android.app.Notification
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Intent
import android.os.IBinder
import android.os.ParcelUuid
import android.util.Log
import androidx.core.app.NotificationCompat
import com.siri.gemini.R
import com.siri.gemini.SiriGeminiApp
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * Foreground service (connectedDevice) that owns filtered BLE scanning.
 * Adaptive duty cycle: low-power when nothing nearby, boosted when AirPods appear.
 * Emits GestureEventBus events on detected activity (stem-press placeholder via rapid status change).
 */
class AirPodsGestureService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var scanner: BluetoothLeScanner? = null
    private val scanning = AtomicBoolean(false)
    private val lastNearbyMs = AtomicLong(0L)
    private var aggressive = false

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result ?: return
            val mfg = result.scanRecord?.getManufacturerSpecificData(ContinuityParser.APPLE_COMPANY_ID)
            val status = ContinuityParser.parse(mfg) ?: return

            lastNearbyMs.set(System.currentTimeMillis())
            GestureEventBus.tryEmit(GestureEventBus.Event.AirPodsNearby(status))

            // Heuristic "stem press" proxy until full L2CAP bridge is wired:
            // rapid successive ads with in-ear change or high RSSI delta can be used later.
            // For now we expose a manual trigger path + log for RE.
            Log.d(TAG, "AirPods Continuity: L=${status.leftBattery} R=${status.rightBattery} inEar=${status.leftInEar}/${status.rightInEar}")
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
                // Explicit stem-press simulation / L2CAP future entry point
                scope.launch {
                    GestureEventBus.emit(GestureEventBus.Event.StemPress())
                }
            }
            else -> startScanning()
        }
        return START_STICKY
    }

    @SuppressLint("MissingPermission")
    private fun startScanning() {
        if (scanning.getAndSet(true)) return

        val btManager = getSystemService(BluetoothManager::class.java)
        val adapter = btManager?.adapter
        if (adapter == null || !adapter.isEnabled) {
            Log.w(TAG, "Bluetooth not available/enabled")
            scanning.set(false)
            return
        }

        scanner = adapter.bluetoothLeScanner
        val filter = ScanFilter.Builder()
            .setManufacturerData(
                ContinuityParser.APPLE_COMPANY_ID,
                // Match any Apple Continuity payload; length mask keeps it broad but filtered
                byteArrayOf(),
                byteArrayOf()
            )
            .build()

        val settings = ScanSettings.Builder()
            .setScanMode(
                if (aggressive) ScanSettings.SCAN_MODE_LOW_LATENCY
                else ScanSettings.SCAN_MODE_LOW_POWER
            )
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
            .setNumOfMatches(ScanSettings.MATCH_NUM_MAX_ADVERTISEMENT)
            .setReportDelay(0)
            .build()

        try {
            scanner?.startScan(listOf(filter), settings, scanCallback)
            Log.i(TAG, "BLE Continuity scan started (aggressive=$aggressive)")
        } catch (e: SecurityException) {
            Log.e(TAG, "Missing BLE permission", e)
            scanning.set(false)
        }

        // Adaptive duty-cycle supervisor
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
        try {
            scanner?.stopScan(scanCallback)
        } catch (_: Exception) {}
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
        try {
            scanner?.stopScan(scanCallback)
        } catch (_: Exception) {}
        scanning.set(false)
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "AirPodsGesture"
        private const val NOTIFICATION_ID = 42
        const val ACTION_TRIGGER_STEM = "com.siri.gemini.action.TRIGGER_STEM"
    }
}
