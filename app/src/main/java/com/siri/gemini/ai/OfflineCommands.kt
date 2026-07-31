package com.siri.gemini.ai

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.provider.AlarmClock
import android.util.Log
import android.view.KeyEvent
import com.siri.gemini.ble.AirPodsGestureService
import com.siri.gemini.ble.aap.AapProtocol

/**
 * QoL #13 — fixed local intents so the assistant stays useful offline / on GrapheneOS.
 */
object OfflineCommands {

    fun tryHandle(context: Context, text: String): Boolean {
        val t = text.lowercase().trim()

        // Timer / alarm
        Regex("""(?:set |start )?(?:a )?timer (?:for )?(\d+)\s*(minutes?|mins?|seconds?|secs?)""").find(t)?.let {
            val n = it.groupValues[1].toIntOrNull() ?: return@let
            val unit = it.groupValues[2]
            val seconds = if (unit.startsWith("sec")) n else n * 60
            context.startActivity(Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, seconds)
                putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            return true
        }

        when {
            t.contains("next track") || t == "next" || t == "skip" -> {
                media(context, KeyEvent.KEYCODE_MEDIA_NEXT); return true
            }
            t.contains("previous") || t == "back" -> {
                media(context, KeyEvent.KEYCODE_MEDIA_PREVIOUS); return true
            }
            t.contains("pause") || t.contains("stop music") -> {
                media(context, KeyEvent.KEYCODE_MEDIA_PAUSE); return true
            }
            t.contains("play") && !t.contains("playlist") -> {
                media(context, KeyEvent.KEYCODE_MEDIA_PLAY); return true
            }
            t.contains("noise cancel") || t == "anc on" -> {
                setNoise(context, AapProtocol.NoiseMode.NOISE_CANCELLATION); return true
            }
            t.contains("transparency") -> {
                setNoise(context, AapProtocol.NoiseMode.TRANSPARENCY); return true
            }
            t.contains("anc off") || t.contains("noise off") -> {
                setNoise(context, AapProtocol.NoiseMode.OFF); return true
            }
            t.contains("volume up") -> {
                vol(context, AudioManager.ADJUST_RAISE); return true
            }
            t.contains("volume down") -> {
                vol(context, AudioManager.ADJUST_LOWER); return true
            }
        }
        return false
    }

    private fun media(ctx: Context, code: Int) {
        val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, code))
        am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, code))
    }

    private fun vol(ctx: Context, dir: Int) {
        val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        am.adjustVolume(dir, AudioManager.FLAG_SHOW_UI)
    }

    private fun setNoise(ctx: Context, mode: AapProtocol.NoiseMode) {
        ctx.startService(Intent(ctx, AirPodsGestureService::class.java).apply {
            action = AirPodsGestureService.ACTION_SET_NOISE
            putExtra(AirPodsGestureService.EXTRA_NOISE_MODE, mode.code)
        })
    }
}
