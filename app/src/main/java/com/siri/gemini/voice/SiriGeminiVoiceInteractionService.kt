package com.siri.gemini.voice

import android.service.voice.VoiceInteractionService

/**
 * System-level assistant entry point.
 * Kept alive by the framework once selected as default assistant.
 */
class SiriGeminiVoiceInteractionService : VoiceInteractionService() {
    override fun onReady() {
        super.onReady()
        // Ready to receive session requests (from stem press or hotword)
    }
}
