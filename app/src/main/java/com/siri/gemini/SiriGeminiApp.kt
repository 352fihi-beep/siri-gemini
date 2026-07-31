package com.siri.gemini

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.siri.gemini.ota.OtaWorker

class SiriGeminiApp : Application() {
    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        OtaWorker.enqueue(this)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)

            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_GESTURE,
                    getString(R.string.notification_channel_gesture),
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "AirPods stem + AAP listener"
                    setShowBadge(false)
                }
            )

            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_OTA,
                    "Updates",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Siri Gemini OTA updates"
                }
            )
        }
    }

    companion object {
        const val CHANNEL_GESTURE = "airpods_gesture"
        const val CHANNEL_OTA = "siri_gemini_ota"
    }
}
