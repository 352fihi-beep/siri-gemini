package com.siri.gemini.ble

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
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
import com.siri.gemini.prefs.UserPrefs
import com.siri.gemini.qol.BatteryNotification
import com.siri.gemini.qol.CaseOpenPopup
import com.siri.gemini.qol.EarDetectionController
import com.siri.gemini.qol.LeaveBehindMonitor
import com.siri.gemini.qol.RssiTracker
import com.siri.gemini.widget.AirPodsWidgetProvider
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

class AirPodsGestureService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val scanning = AtomicBoolean(false)
    private val lastNearbyMs = AtomicLong(0L)
    private var aggressive = false
    private var lastLidOpen: Boolean? = null

    private var aap: AapConnection? = null
    private var bondedAirPods: BluetoothDevice? = null
    private lateinit var prefs: UserPrefs
    private lateinit var earCtrl: EarDetectionController
    private lateinit var leaveBehind: LeaveBehindMonitor

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result ?: return
            val rssi = result.rssi
            RssiTracker.add(rssi)
            leaveBehind.onRssi(rssi)

            val mfg = result.scanRecord?.getManufacturerSpecificData(ContinuityParser.APPLE_COMPANY_ID)
            val status = ContinuityParser.parse(mfg) ?: return

            lastNearbyMs.set(System.currentTimeMillis())
            GestureEventBus.tryEmit(GestureEventBus.Event.AirPodsNearby(status))

            val approx = AapProtocol.BatteryInfo(
                left = status.leftBattery,
                right = status.rightBattery,
                case = status.caseBattery
            )
            AirPodsWidgetProvider.refreshAll(this@AirPodsGestureService, approx, null)

            // Battery notification only with real numbers
            if (approx.left != null || approx.right != null || approx.case != null) {
                val nm = getSystemService(NotificationManager::class.java)
                nm.notify(
                    BatteryNotification.ID,
                    BatteryNotification.build(
                        this@AirPodsGestureService,
                        approx,
                        aap?.noiseMode?.value ?: AapProtocol.NoiseMode.OFF,
                        prefs.airpodsName
                    )
                )
            }

            // Case-open popup on real lid transition
            val lid = status.lidOpen
            if (lid == true && lastLidOpen == false) {
                CaseOpenPopup.show(this@AirPodsGestureService, approx, prefs.airpodsName)
            }
            if (lid != null) lastLidOpen = lid

            if (status.leftInEar != null || status.rightInEar != null) {
                earCtrl.onEars(
                    AapProtocol.EarDetection(
                        status.leftInEar == true,
                        status.rightInEar == true
                    )
                )
            }

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

    override fun onCreate() {
        super.onCreate()
        prefs = UserPrefs(this)
        earCtrl = EarDetectionController(this)
        leaveBehind = LeaveBehindMonitor(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val base = NotificationCompat.Builder(this, SiriGeminiApp.CHANNEL_GESTURE)
            .setContentTitle(getString(R.string.notification_title_listening))
            .setContentText(getString(R.string.notification_text_ready))
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        startForeground(42, base)

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
            Log.w(TAG, "Bluetooth off or missing")
            return
        }
        val scanner = adapter.bluetoothLeScanner ?: run {
            scanning.set(false)
            return
        }
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
            scanner.startScan(listOf(filter), settings, scanCallback)
        } catch (e: SecurityException) {
            Log.e(TAG, "BLE permission", e)
            scanning.set(false)
            return
        }

        scope.launch {
            while (isActive) {
                delay(15_000)
                val ago = System.currentTimeMillis() - lastNearbyMs.get()
                val want = ago < 30_000
                if (want != aggressive) {
                    aggressive = want
                    try { scanner.stopScan(scanCallback) } catch (_: Exception) {}
                    scanning.set(false)
                    startScanning()
                }
                // Stop scan after prolonged absence (Campaign 2 power)
                if (ago > 300_000) {
                    try { scanner.stopScan(scanCallback) } catch (_: Exception) {}
                    scanning.set(false)
                    GestureEventBus.tryEmit(GestureEventBus.Event.AirPodsLost)
                } else if (ago > 60_000) {
                    GestureEventBus.tryEmit(GestureEventBus.Event.AirPodsLost)
                }
            }
        }
    }

    override fun onDestroy() {
        aap?.close()
        scanning.set(false)
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "AirPodsGesture"
        const val ACTION_TRIGGER_STEM = "com.siri.gemini.action.TRIGGER_STEM"
        const val ACTION_SET_NOISE = "com.siri.gemini.action.SET_NOISE"
        const val EXTRA_NOISE_MODE = "noise_mode"
    }
}
