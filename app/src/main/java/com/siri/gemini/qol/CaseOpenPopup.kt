package com.siri.gemini.qol

import android.content.Context
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.TextView
import com.siri.gemini.R
import com.siri.gemini.ble.aap.AapProtocol

/**
 * QoL #1 — iOS-style case-open battery popup.
 */
object CaseOpenPopup {

    private var showing = false

    fun show(context: Context, battery: AapProtocol.BatteryInfo?, name: String = "AirPods") {
        if (showing) return
        showing = true

        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val view = LayoutInflater.from(context).inflate(R.layout.popup_case_open, null)

        view.findViewById<TextView>(R.id.popup_name).text = name
        view.findViewById<TextView>(R.id.popup_left).text = battery?.left?.let { "$it%" } ?: "—"
        view.findViewById<TextView>(R.id.popup_right).text = battery?.right?.let { "$it%" } ?: "—"
        view.findViewById<TextView>(R.id.popup_case).text = battery?.case?.let { "$it%" } ?: "—"

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = 80
        }

        try {
            wm.addView(view, params)
            Handler(Looper.getMainLooper()).postDelayed({
                try { wm.removeView(view) } catch (_: Exception) {}
                showing = false
            }, 3500)
        } catch (e: Exception) {
            // SYSTEM_ALERT_WINDOW not granted — fall back silently
            showing = false
        }
    }
}
