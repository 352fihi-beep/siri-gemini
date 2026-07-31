package com.siri.gemini.ai

import android.content.Context
import android.util.Log

/**
 * Optional Gemini Nano / AICore binding behind a feature flag.
 * Never required for core functionality — keeps GrapheneOS path clean.
 *
 * Real AICore integration needs the Google AI Edge / AICore APIs and a
 * supported device (Pixel 8+ class). This is a safe no-op scaffold.
 */
object AiCoreBridge {

    const val FEATURE_FLAG = false   // flip when AICore deps + device checks are ready

    fun isAvailable(context: Context): Boolean {
        if (!FEATURE_FLAG) return false
        // Future: check for AICore package / system feature
        // return context.packageManager.hasSystemFeature("com.google.aicore")
        return false
    }

    fun generate(prompt: String, onResult: (String) -> Unit, onError: (String) -> Unit) {
        if (!FEATURE_FLAG) {
            onError("AICore disabled (feature flag)")
            return
        }
        // Future: bind to AICore / Gemini Nano session
        Log.i(TAG, "AICore generate called (stub)")
        onError("AICore not bound yet")
    }

    companion object {
        private const val TAG = "AiCoreBridge"
    }
}
