package com.siri.gemini.qol

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.siri.gemini.R
import com.siri.gemini.SiriGeminiApp
import com.siri.gemini.ble.aap.AapProtocol
import com.siri.gemini.ui.MainActivity

/**
 * QoL #2 — persistent low-priority notification with live battery + ANC.
 */
object BatteryNotification {

    const val ID = 43

    fun build(
        context: Context,
        battery: AapProtocol.BatteryInfo?,
        noise: AapProtocol.NoiseMode,
        name: String = "AirPods"
    ): Notification {
        val left = battery?.left?.let { "$it%" } ?: "—"
        val right = battery?.right?.let { "$it%" } ?: "—"
        val caseB = battery?.case?.let { "$it%" } ?: "—"
        val text = "L $left  ·  R $right  ·  Case $caseB  ·  ${noise.name.replace('_', ' ')}"

        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, SiriGeminiApp.CHANNEL_BATTERY)
            .setContentTitle(name)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_lock_idle_charging)
            .setContentIntent(open)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .build()
    }
}
