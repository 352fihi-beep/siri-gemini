package com.siri.gemini.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.siri.gemini.R
import com.siri.gemini.ble.AirPodsGestureService
import com.siri.gemini.ble.aap.AapProtocol
import com.siri.gemini.ui.MainActivity

/**
 * Home-screen widget: battery levels + one-tap ANC / Transparency / Off.
 */
class AirPodsWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (id in appWidgetIds) {
            updateWidget(context, appWidgetManager, id, null, AapProtocol.NoiseMode.OFF)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_SET_NOISE -> {
                val mode = intent.getIntExtra(EXTRA_MODE, AapProtocol.NoiseMode.OFF.code)
                context.startService(Intent(context, AirPodsGestureService::class.java).apply {
                    action = AirPodsGestureService.ACTION_SET_NOISE
                    putExtra(AirPodsGestureService.EXTRA_NOISE_MODE, mode)
                })
                // Optimistic UI refresh
                refreshAll(context, null, AapProtocol.NoiseMode.from(mode))
            }
            ACTION_REFRESH -> {
                refreshAll(context, null, null)
            }
        }
    }

    companion object {
        const val ACTION_SET_NOISE = "com.siri.gemini.widget.SET_NOISE"
        const val ACTION_REFRESH = "com.siri.gemini.widget.REFRESH"
        const val EXTRA_MODE = "mode"

        fun refreshAll(
            context: Context,
            battery: AapProtocol.BatteryInfo?,
            noise: AapProtocol.NoiseMode?
        ) {
            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(ComponentName(context, AirPodsWidgetProvider::class.java))
            for (id in ids) {
                updateWidget(context, mgr, id, battery, noise ?: AapProtocol.NoiseMode.OFF)
            }
        }

        private fun updateWidget(
            context: Context,
            mgr: AppWidgetManager,
            id: Int,
            battery: AapProtocol.BatteryInfo?,
            noise: AapProtocol.NoiseMode
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_airpods)

            val left = battery?.left?.let { "$it%" } ?: "—"
            val right = battery?.right?.let { "$it%" } ?: "—"
            val case = battery?.case?.let { "$it%" } ?: "—"
            views.setTextViewText(R.id.widget_bat_left, left)
            views.setTextViewText(R.id.widget_bat_right, right)
            views.setTextViewText(R.id.widget_bat_case, case)

            views.setTextViewText(R.id.widget_noise_label, noise.name.replace('_', ' '))

            // Noise buttons
            views.setOnClickPendingIntent(
                R.id.btn_noise_off,
                noisePending(context, AapProtocol.NoiseMode.OFF)
            )
            views.setOnClickPendingIntent(
                R.id.btn_noise_anc,
                noisePending(context, AapProtocol.NoiseMode.NOISE_CANCELLATION)
            )
            views.setOnClickPendingIntent(
                R.id.btn_noise_trans,
                noisePending(context, AapProtocol.NoiseMode.TRANSPARENCY)
            )

            // Open app
            val open = PendingIntent.getActivity(
                context, 0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, open)

            mgr.updateAppWidget(id, views)
        }

        private fun noisePending(context: Context, mode: AapProtocol.NoiseMode): PendingIntent {
            val i = Intent(context, AirPodsWidgetProvider::class.java).apply {
                action = ACTION_SET_NOISE
                putExtra(EXTRA_MODE, mode.code)
            }
            return PendingIntent.getBroadcast(
                context, mode.code, i,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}
