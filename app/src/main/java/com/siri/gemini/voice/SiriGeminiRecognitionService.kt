package com.siri.gemini.voice

import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionService
import android.speech.SpeechRecognizer

/**
 * Required companion for VoiceInteractionService.
 * Will host the on-device STT pipeline (Vosk / Whisper / AICore).
 */
class SiriGeminiRecognitionService : RecognitionService() {
    override fun onStartListening(recognizerIntent: Intent, listener: Callback) {
        // Delegate to system recognizer; this service is registered as a placeholder.
        // Signal error so the framework knows to fall back gracefully.
        listener.error(SpeechRecognizer.ERROR_CLIENT)
    }

    override fun onCancel(listener: Callback) {}
    override fun onStopListening(listener: Callback) {}
}
