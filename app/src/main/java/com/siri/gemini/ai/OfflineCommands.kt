package com.siri.gemini.ai

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.provider.AlarmClock
import android.provider.Settings
import android.view.KeyEvent
import com.siri.gemini.ble.AirPodsGestureService
import com.siri.gemini.ble.aap.AapProtocol

/**
 * Expanded offline intents (Campaign 2 #6). No network required.
 */
object OfflineCommands {

    fun tryHandle(context: Context, text: String): Boolean {
        val t = text.lowercase().trim()
        if (t.isEmpty()) return false

        Regex("""(?:set |start )?(?:a )?timer (?:for )?(\d+)\s*(minutes?|mins?|seconds?|secs?)""").find(t)?.let {
            val n = it.groupValues[1].toIntOrNull() ?: return@let
            if (n <= 0) return@let
            val unit = it.groupValues[2]
            val seconds = if (unit.startsWith("sec")) n else n * 60
            try {
                context.startActivity(Intent(AlarmClock.ACTION_SET_TIMER).apply {
                    putExtra(AlarmClock.EXTRA_LENGTH, seconds)
                    putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            } catch (_: Exception) { return false }
            return true
        }

        Regex("""(?:set |start )?(?:an )?alarm (?:for )?(\d{1,2})(?::(\d{2}))?\s*(am|pm)?""").find(t)?.let {
            var hour = it.groupValues[1].toIntOrNull() ?: return@let
            val minute = it.groupValues[2].toIntOrNull() ?: 0
            val ampm = it.groupValues[3]
            if (ampm == "pm" && hour < 12) hour += 12
            if (ampm == "am" && hour == 12) hour = 0
            if (hour > 23 || minute > 59) return@let
            try {
                context.startActivity(Intent(AlarmClock.ACTION_SET_ALARM).apply {
                    putExtra(AlarmClock.EXTRA_HOUR, hour)
                    putExtra(AlarmClock.EXTRA_MINUTES, minute)
                    putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            } catch (_: Exception) { return false }
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
            (t.contains("play") && !t.contains("playlist")) || t == "resume" -> {
                media(context, KeyEvent.KEYCODE_MEDIA_PLAY); return true
            }
            t.contains("noise cancel") || t == "anc on" -> {
                setNoise(context, AapProtocol.NoiseMode.NOISE_CANCELLATION); return true
            }
            t.contains("transparency") -> {
                setNoise(context, AapProtocol.NoiseMode.TRANSPARENCY); return true
            }
            t.contains("adaptive") && t.contains("audio") -> {
                setNoise(context, AapProtocol.NoiseMode.ADAPTIVE); return true
            }
            t.contains("anc off") || t.contains("noise off") -> {
                setNoise(context, AapProtocol.NoiseMode.OFF); return true
            }
            t.contains("volume up") || t == "louder" -> {
                vol(context, AudioManager.ADJUST_RAISE); return true
            }
            t.contains("volume down") || t == "quieter" -> {
                vol(context, AudioManager.ADJUST_LOWER); return true
            }
            t.contains("mute") -> {
                vol(context, AudioManager.ADJUST_MUTE); return true
            }
            t.contains("flashlight") || t.contains("torch") -> {
                // Best-effort: open settings panel; camera torch needs CameraManager + permission
                try {
                    context.startActivity(Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                } catch (_: Exception) {}
                return false // don't claim success without real torch API wiring
            }
            t.contains("do not disturb") || t == "dnd" -> {
                try {
                    context.startActivity(Intent(Settings.ACTION_ZEN_MODE_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                    return true
                } catch (_: Exception) {
                    return false
                }
            }
            t.contains("battery") && (t.contains("how") || t.contains("percent") || t.contains("level")) -> {
                // Answer is UI-side; cannot speak without TTS path — still handled as recognized
                return true
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
        if (dir == AudioManager.ADJUST_MUTE && Build.VERSION.SDK_INT >= 23) {
            am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_TOGGLE_MUTE, 0)
        } else {
            am.adjustVolume(dir, AudioManager.FLAG_SHOW_UI)
        }
    }

    private fun setNoise(ctx: Context, mode: AapProtocol.NoiseMode) {
        ctx.startService(Intent(ctx, AirPodsGestureService::class.java).apply {
            action = AirPodsGestureService.ACTION_SET_NOISE
            putExtra(AirPodsGestureService.EXTRA_NOISE_MODE, mode.code)
        })
    }
}
