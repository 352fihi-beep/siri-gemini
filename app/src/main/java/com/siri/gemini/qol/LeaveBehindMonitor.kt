package com.siri.gemini.qol

import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import com.siri.gemini.SiriGeminiApp
import com.siri.gemini.prefs.UserPrefs
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * Campaign 2 #3 — leave-behind reminder from real RSSI samples.
 * Fires only after sustained weak signal, not on a single blip.
 */
class LeaveBehindMonitor(private val context: Context) {

    private val prefs = UserPrefs(context)
    private val lastStrongMs = AtomicLong(System.currentTimeMillis())
    private val weakStreak = AtomicInteger(0)
    private var notified = false

    fun onRssi(rssi: Int) {
        if (!prefs.leaveBehindEnabled) return
        if (rssi == Integer.MIN_VALUE) return

        val now = System.currentTimeMillis()
        if (rssi >= RSSI_STRONG) {
            lastStrongMs.set(now)
            weakStreak.set(0)
            notified = false
            return
        }
        if (rssi <= RSSI_WEAK) {
            val streak = weakStreak.incrementAndGet()
            val awayMs = now - lastStrongMs.get()
            if (!notified && streak >= WEAK_SAMPLES && awayMs >= AWAY_MS) {
                notified = true
                notifyLeaveBehind(rssi)
            }
        }
    }

    private fun notifyLeaveBehind(rssi: Int) {
        Log.i(TAG, "Leave-behind likely rssi=$rssi")
        val nm = context.getSystemService(NotificationManager::class.java)
        val n = NotificationCompat.Builder(context, SiriGeminiApp.CHANNEL_OTA)
            .setContentTitle("AirPods may be left behind")
            .setContentText("Signal weak (RSSI $rssi dBm). Check your case.")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        nm.notify(LEAVE_ID, n)
    }

    companion object {
        private const val TAG = "LeaveBehind"
        private const val RSSI_STRONG = -70
        private const val RSSI_WEAK = -90
        private const val WEAK_SAMPLES = 4
        private const val AWAY_MS = 45_000L
        private const val LEAVE_ID = 77
    }
}
