package com.siri.gemini.qol

import android.content.Context
import android.media.AudioManager
import android.util.Log
import android.view.KeyEvent
import com.siri.gemini.ble.aap.AapProtocol
import com.siri.gemini.prefs.UserPrefs

/**
 * QoL #3 — ear detection → auto play/pause via media key simulation.
 */
class EarDetectionController(private val context: Context) {

    private val prefs = UserPrefs(context)
    private var lastLeft = false
    private var lastRight = false
    private var lastActionMs = 0L
    private val debounceMs = 1000L // Prevent audio thrashing on rapid in/out

    fun onEars(detection: AapProtocol.EarDetection) {
        if (!prefs.earAutoPause) return

        val anyIn = detection.leftInEar || detection.rightInEar
        val wasAny = lastLeft || lastRight
        val now = System.currentTimeMillis()

        if (now - lastActionMs < debounceMs) {
            lastLeft = detection.leftInEar
            lastRight = detection.rightInEar
            return
        }

        if (wasAny && !anyIn) {
            // Both out → pause
            sendMediaKey(KeyEvent.KEYCODE_MEDIA_PAUSE)
            Log.i(TAG, "Ears out → pause")
            lastActionMs = now
        } else if (!wasAny && anyIn) {
            // At least one in → play
            sendMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY)
            Log.i(TAG, "Ears in → play")
            lastActionMs = now
        }

        lastLeft = detection.leftInEar
        lastRight = detection.rightInEar
    }

    private fun sendMediaKey(keyCode: Int) {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val eventDown = KeyEvent(KeyEvent.ACTION_DOWN, keyCode)
        val eventUp = KeyEvent(KeyEvent.ACTION_UP, keyCode)
        am.dispatchMediaKeyEvent(eventDown)
        am.dispatchMediaKeyEvent(eventUp)
    }

    companion object {
        private const val TAG = "EarDetect"
    }
}
