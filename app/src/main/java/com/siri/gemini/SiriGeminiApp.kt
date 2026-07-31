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
        // OTA is also enqueued from MainActivity; safe to call early
        OtaWorker.enqueue(this)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_GESTURE,
                getString(R.string.notification_channel_gesture),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Low-priority channel for AirPods stem detection"
                setShowBadge(false)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_GESTURE = "airpods_gesture"
    }
}
