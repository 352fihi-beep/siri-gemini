package com.siri.gemini.boot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.siri.gemini.ble.AirPodsGestureService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            // Optional: auto-start if user previously enabled
            // context.startForegroundService(Intent(context, AirPodsGestureService::class.java))
        }
    }
}
