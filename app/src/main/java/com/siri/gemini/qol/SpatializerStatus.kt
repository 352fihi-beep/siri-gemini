package com.siri.gemini.qol

import android.content.Context
import android.media.AudioManager
import android.media.Spatializer
import android.os.Build
import android.util.Log

/**
 * Campaign 2 #5 — Android 13+ Spatializer API readout (real device capability).
 */
object SpatializerStatus {

    data class Report(
        val available: Boolean,
        val enabled: Boolean,
        val headTracked: Boolean,
        val detail: String
    )

    fun query(context: Context): Report {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return Report(false, false, false, "Spatializer requires Android 13+")
        }
        return try {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val sp = am.spatializer
            val avail = sp.immersiveAudioLevel != Spatializer.SPATIALIZER_IMMERSIVE_LEVEL_NONE
            val enabled = try { sp.isEnabled } catch (_: Exception) { false }
            val head = try { sp.isHeadTrackerAvailable } catch (_: Exception) { false }
            Report(
                available = avail,
                enabled = enabled,
                headTracked = head,
                detail = "immersive=${sp.immersiveAudioLevel} enabled=$enabled headTracker=$head"
            )
        } catch (e: Exception) {
            Log.w("SpatializerStatus", "query failed", e)
            Report(false, false, false, "Unavailable: ${e.message}")
        }
    }

    fun summary(context: Context): String {
        val r = query(context)
        return when {
            !r.available -> "Spatial audio: not available"
            r.enabled && r.headTracked -> "Spatial audio: on (head tracking capable)"
            r.enabled -> "Spatial audio: on"
            else -> "Spatial audio: available, off"
        }
    }
}
