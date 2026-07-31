package com.siri.gemini.voice

import android.content.Intent
import android.speech.RecognitionService

/**
 * Required companion for VoiceInteractionService.
 * Will host the on-device STT pipeline (Vosk / Whisper / AICore).
 */
class SiriGeminiRecognitionService : RecognitionService() {
    override fun onStartListening(recognizerIntent: Intent, listener: Callback) {
        // TODO: start local STT
        // For now just acknowledge
    }

    override fun onCancel(listener: Callback) {}
    override fun onStopListening(listener: Callback) {}
}
