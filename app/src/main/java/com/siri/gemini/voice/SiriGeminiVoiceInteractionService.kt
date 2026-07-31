package com.siri.gemini.voice

import android.os.Bundle
import android.service.voice.VoiceInteractionService
import android.util.Log
import com.siri.gemini.ble.GestureEventBus
import kotlinx.coroutines.*

/**
 * System-level assistant entry point.
 * Kept alive by the framework once selected as default assistant.
 * Listens to GestureEventBus and starts a session on stem press.
 */
class SiriGeminiVoiceInteractionService : VoiceInteractionService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onReady() {
        super.onReady()
        Log.i(TAG, "VoiceInteractionService ready")
        scope.launch {
            GestureEventBus.events.collect { event ->
                when (event) {
                    is GestureEventBus.Event.StemPress -> {
                        Log.i(TAG, "Stem press → starting session")
                        val args = Bundle().apply {
                            putString("trigger", "stem_press")
                            putString("source", event.source)
                        }
                        // Requires this service to be the active default assistant
                        try {
                            showSession(args, 0)
                        } catch (e: Exception) {
                            Log.e(TAG, "showSession failed (is default assistant set?)", e)
                        }
                    }
                    else -> { /* nearby / lost handled elsewhere if needed */ }
                }
            }
        }
    }

    override fun onShutdown() {
        scope.cancel()
        super.onShutdown()
    }

    companion object {
        private const val TAG = "SiriGeminiVIS"
    }
}
