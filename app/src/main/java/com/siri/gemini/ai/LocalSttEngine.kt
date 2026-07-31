package com.siri.gemini.ai

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

/**
 * Offline-preferring STT with real capability checks and no mock transcripts.
 *
 * Active path: system SpeechRecognizer + EXTRA_PREFER_OFFLINE.
 * Vosk: only if dependency + model are both present (not simulated).
 */
class LocalSttEngine(private val context: Context) {

    private var recognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var listening = false

    val isSystemRecognitionAvailable: Boolean
        get() = try {
            SpeechRecognizer.isRecognitionAvailable(context)
        } catch (e: Exception) {
            Log.w(TAG, "isRecognitionAvailable failed", e)
            false
        }

    fun start(
        onPartial: (String) -> Unit,
        onFinal: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        cancel()

        if (!isSystemRecognitionAvailable) {
            onError("No speech recognizer on this device")
            return
        }

        mainHandler.post {
            try {
                val r = SpeechRecognizer.createSpeechRecognizer(context)
                recognizer = r
                r.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        listening = true
                    }
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {
                        listening = false
                    }

                    override fun onError(error: Int) {
                        listening = false
                        onError(errorMessage(error))
                    }

                    override fun onResults(results: Bundle?) {
                        listening = false
                        val texts = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val best = texts?.firstOrNull()?.trim().orEmpty()
                        if (best.isNotEmpty()) onFinal(best)
                        else onError("No speech recognized")
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val texts = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val partial = texts?.firstOrNull()?.trim().orEmpty()
                        if (partial.isNotEmpty()) onPartial(partial)
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                }
                r.startListening(intent)
            } catch (e: Exception) {
                Log.e(TAG, "startListening failed", e)
                onError("Failed to start recognizer: ${e.message ?: e.javaClass.simpleName}")
                cancel()
            }
        }
    }

    fun cancel() {
        listening = false
        mainHandler.post {
            try {
                recognizer?.cancel()
            } catch (e: Exception) {
                Log.w(TAG, "cancel", e)
            }
            try {
                recognizer?.destroy()
            } catch (e: Exception) {
                Log.w(TAG, "destroy", e)
            }
            recognizer = null
        }
    }

    private fun errorMessage(error: Int): String = when (error) {
        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
        SpeechRecognizer.ERROR_CLIENT -> "Client error"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
        SpeechRecognizer.ERROR_NETWORK -> "Network error (offline engine unavailable)"
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
        SpeechRecognizer.ERROR_NO_MATCH -> "No match"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
        SpeechRecognizer.ERROR_SERVER -> "Server error"
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
        else -> "Recognition error ($error)"
    }

    companion object {
        private const val TAG = "LocalStt"
    }
}
