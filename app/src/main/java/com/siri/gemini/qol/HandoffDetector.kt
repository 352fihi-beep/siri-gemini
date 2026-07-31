package com.siri.gemini.qol

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.widget.Toast

/**
 * QoL #12 — detect when audio route leaves AirPods (multi-device handoff).
 */
class HandoffDetector(private val context: Context) {

    private val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var hadAirPods = false

    private val callback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) {
            checkRoute()
        }
        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {
            checkRoute()
        }
    }

    fun start() {
        am.registerAudioDeviceCallback(callback, Handler(Looper.getMainLooper()))
        checkRoute()
    }

    fun stop() {
        try { am.unregisterAudioDeviceCallback(callback) } catch (_: Exception) {}
    }

    private fun checkRoute() {
        val devices = am.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        val airpodsNow = devices.any {
            val n = it.productName?.toString()?.lowercase().orEmpty()
            n.contains("airpods") || n.contains("beats")
        }
        if (hadAirPods && !airpodsNow) {
            Toast.makeText(context, "Audio moved from AirPods", Toast.LENGTH_SHORT).show()
        }
        hadAirPods = airpodsNow
    }
}
