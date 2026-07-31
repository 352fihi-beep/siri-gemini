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
 * Local / on-device STT wrapper.
 * Uses the system SpeechRecognizer first (many GrapheneOS builds ship a local engine).
 * Later can be swapped for Vosk / Whisper.cpp quantized without changing call sites.
 */
class LocalSttEngine(private val context: Context) {

    private var recognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    fun start(
        onPartial: (String) -> Unit,
        onFinal: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        cancel()

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onError("No on-device recognizer available")
            return
        }

        mainHandler.post {
            recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {}
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {}

                    override fun onError(error: Int) {
                        val msg = when (error) {
                            SpeechRecognizer.ERROR_NO_MATCH -> "No match"
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Timeout"
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Mic permission"
                            else -> "STT error $error"
                        }
                        onError(msg)
                    }

                    override fun onResults(results: Bundle?) {
                        val texts = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val best = texts?.firstOrNull().orEmpty()
                        if (best.isNotBlank()) onFinal(best) else onError("Empty result")
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val texts = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        texts?.firstOrNull()?.let { onPartial(it) }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true) // force on-device when possible
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                }
                startListening(intent)
            }
        }
    }

    fun cancel() {
        mainHandler.post {
            try {
                recognizer?.cancel()
                recognizer?.destroy()
            } catch (e: Exception) {
                Log.w(TAG, "cancel", e)
            }
            recognizer = null
        }
    }

    companion object {
        private const val TAG = "LocalStt"
    }
}
