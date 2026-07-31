package com.siri.gemini.ble

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.siri.gemini.R
import com.siri.gemini.SiriGeminiApp
import kotlinx.coroutines.*

/**
 * Foreground service (connectedDevice) that owns BLE scanning + future L2CAP bridge.
 * Adaptive duty cycle: aggressive only when candidate AirPods are nearby.
 */
class AirPodsGestureService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())
        scope.launch {
            // TODO: start filtered BLE scan (ScanFilter.Builder().setManufacturerData(APPLE_COMPANY_ID, …))
            // TODO: on stem press event → trigger VoiceInteractionSession
        }
        return START_STICKY
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, SiriGeminiApp.CHANNEL_GESTURE)
            .setContentTitle(getString(R.string.notification_title_listening))
            .setContentText(getString(R.string.notification_text_ready))
            .setSmallIcon(android.R.drawable.ic_btn_speak_now) // replace with proper icon
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val NOTIFICATION_ID = 42
    }
}
