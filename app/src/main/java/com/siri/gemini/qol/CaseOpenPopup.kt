package com.siri.gemini.qol

import android.content.Context
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.TextView
import com.siri.gemini.R
import com.siri.gemini.ble.aap.AapProtocol

/**
 * Case-open battery popup. Shows only with real data and overlay permission.
 */
object CaseOpenPopup {

    private const val TAG = "CaseOpenPopup"
    private var showing = false

    fun show(context: Context, battery: AapProtocol.BatteryInfo?, name: String) {
        if (showing) return
        if (!Settings.canDrawOverlays(context)) {
            Log.i(TAG, "Overlay permission not granted — skip popup")
            return
        }
        // Require at least one real battery reading
        if (battery == null || (battery.left == null && battery.right == null && battery.case == null)) {
            Log.d(TAG, "No battery data — skip popup")
            return
        }

        showing = true
        val appCtx = context.applicationContext
        val wm = appCtx.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        try {
            val view = LayoutInflater.from(appCtx).inflate(R.layout.popup_case_open, null)
            view.findViewById<TextView>(R.id.popup_name).text = name.ifBlank { "AirPods" }
            view.findViewById<TextView>(R.id.popup_left).text = battery.left?.let { "$it%" } ?: "—"
            view.findViewById<TextView>(R.id.popup_right).text = battery.right?.let { "$it%" } ?: "—"
            view.findViewById<TextView>(R.id.popup_case).text = battery.case?.let { "$it%" } ?: "—"

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

            wm.addView(view, params)
            Handler(Looper.getMainLooper()).postDelayed({
                try { wm.removeView(view) } catch (e: Exception) {
                    Log.w(TAG, "removeView", e)
                }
                showing = false
            }, 3500)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show popup", e)
            showing = false
        }
    }
}
