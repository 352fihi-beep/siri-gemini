package com.siri.gemini.qol

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.util.Log
import android.view.KeyEvent
import com.siri.gemini.ble.AirPodsGestureService
import com.siri.gemini.ble.GestureEventBus
import com.siri.gemini.ble.aap.AapProtocol
import com.siri.gemini.prefs.UserPrefs

/**
 * QoL #4 — map stem actions to user-configured behaviors.
 */
class StemActionRouter(private val context: Context) {

    private val prefs = UserPrefs(context)

    fun handle(action: AapProtocol.StemAction) {
        val key = when (action) {
            AapProtocol.StemAction.SINGLE_PRESS -> prefs.stemSingle
            AapProtocol.StemAction.DOUBLE_PRESS -> prefs.stemDouble
            AapProtocol.StemAction.LONG_PRESS -> prefs.stemLong
            else -> prefs.stemSingle
        }
        execute(key)
    }

    fun execute(key: String) {
        Log.i(TAG, "Stem → $key")
        when (key) {
            "assistant" -> GestureEventBus.tryEmit(GestureEventBus.Event.StemPress(source = "stem_mapped"))
            "play_pause" -> media(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
            "next" -> media(KeyEvent.KEYCODE_MEDIA_NEXT)
            "prev" -> media(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
            "anc_cycle" -> cycleAnc()
            "volume_up" -> volume(AudioManager.ADJUST_RAISE)
            "volume_down" -> volume(AudioManager.ADJUST_LOWER)
            "none" -> {}
        }
    }

    private fun media(code: Int) {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, code))
        am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, code))
    }

    private fun volume(direction: Int) {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        am.adjustVolume(direction, AudioManager.FLAG_SHOW_UI)
    }

    private fun cycleAnc() {
        // Service will advance mode; for now emit OFF as placeholder cycle start
        context.startService(Intent(context, AirPodsGestureService::class.java).apply {
            action = AirPodsGestureService.ACTION_SET_NOISE
            putExtra(AirPodsGestureService.EXTRA_NOISE_MODE, AapProtocol.NoiseMode.NOISE_CANCELLATION.code)
        })
    }

    companion object {
        private const val TAG = "StemRouter"
    }
}
