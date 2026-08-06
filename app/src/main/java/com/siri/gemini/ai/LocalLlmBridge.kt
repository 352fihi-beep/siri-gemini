package com.siri.gemini.ai

import android.content.Context
import android.util.Log

/**
 * Campaign 2 experimental #14 — on-device small LLM bridge.
 * No weights bundled; reports capability only. Wire MediaPipe / ExecuTorch / picoLLM later.
 */
object LocalLlmBridge {

    private const val TAG = "LocalLlm"

    fun isAvailable(context: Context): Boolean {
        // Detect optional native lib without loading mock answers
        return try {
            System.loadLibrary("llama") // optional; will throw if absent
            true
        } catch (_: UnsatisfiedLinkError) {
            false
        } catch (_: Exception) {
            false
        }
    }

    fun generate(context: Context, prompt: String, onResult: (String) -> Unit, onError: (String) -> Unit) {
        if (prompt.isBlank()) {
            onError("Empty prompt")
            return
        }
        if (!isAvailable(context)) {
            onError("Local LLM library not installed")
            return
        }
        onError("LLM library present but session API not linked")
        Log.i(TAG, "generate refused — no silent mock output")
    }
}
