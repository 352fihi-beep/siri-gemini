package com.siri.gemini.ai

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log

/**
 * Gemini Nano / AICore binding.
 *
 * Detection strategy (no hard dependency):
 * - Look for AICore / Google AI services package
 * - Check known Pixel model families that ship Nano
 * - Feature flag still gates the actual generate() call so GrapheneOS
 *   builds remain clean when AICore is absent.
 *
 * Real generation requires the AI Edge / AICore SDK on a supported device.
 */
object AiCoreBridge {

    // Flip to true only after adding the AICore dependency and testing on device
    const val FEATURE_FLAG = true

    private val SUPPORTED_MODELS = setOf(
        "pixel 8", "pixel 8 pro", "pixel 8a",
        "pixel 9", "pixel 9 pro", "pixel 9 pro xl", "pixel 9a",
        "pixel 10", "pixel 10 pro" // future-proof
    )

    fun isAvailable(context: Context): Boolean {
        if (!FEATURE_FLAG) return false

        val pm = context.packageManager
        val hasAiCore = try {
            pm.getPackageInfo("com.google.android.aicore", 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        } || try {
            pm.getPackageInfo("com.google.android.as", 0) // private compute services sometimes host it
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }

        val model = Build.MODEL.lowercase()
        val deviceSupported = SUPPORTED_MODELS.any { model.contains(it) }

        val available = hasAiCore && deviceSupported
        Log.i(TAG, "AICore available=$available (pkg=$hasAiCore, model=$model)")
        return available
    }

    fun generate(
        context: Context,
        prompt: String,
        onResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!isAvailable(context)) {
            onError("Gemini Nano / AICore not available on this device")
            return
        }
        // Future: bind to AICore Session / GenerativeModel
        // val model = GenerativeModel(...)
        // model.generateContent(prompt)
        Log.i(TAG, "AICore generate stub for prompt length=${prompt.length}")
        onError("AICore detected but SDK binding not yet linked — add AI Edge dependency")
    }

    companion object {
        private const val TAG = "AiCoreBridge"
    }
}
