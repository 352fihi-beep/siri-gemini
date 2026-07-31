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
 * Offline-first STT.
 *
 * Priority:
 * 1. Vosk (when model + lib are present) — true offline, privacy-first
 * 2. System SpeechRecognizer with EXTRA_PREFER_OFFLINE
 * 3. Error surface
 *
 * Vosk integration: add `implementation("com.alphacephei:vosk-android:0.3.47")`
 * (or latest) and ship a small model under assets/. The Vosk path is
 * activated automatically when the model is found.
 */
class LocalSttEngine(private val context: Context) {

    private var recognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var voskAvailable = false // set true after successful model load

    init {
        // Probe for Vosk model (assets/model-en-us or similar)
        try {
            context.assets.open("model/am/final.mdl").close()
            voskAvailable = true
            Log.i(TAG, "Vosk model detected — will prefer offline Vosk path")
        } catch (_: Exception) {
            voskAvailable = false
        }
    }

    fun start(
        onPartial: (String) -> Unit,
        onFinal: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        cancel()

        if (voskAvailable) {
            startVosk(onPartial, onFinal, onError)
            return
        }

        // System offline path
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
                            SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                                "Network required (offline engine missing)"
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
                    putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                }
                startListening(intent)
            }
        }
    }

    private fun startVosk(
        onPartial: (String) -> Unit,
        onFinal: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        // Real Vosk wiring (requires vosk-android AAR + model):
        // val model = Model(assetPath)
        // val recognizer = Recognizer(model, 16000f)
        // ... audio record loop → recognizer.acceptWaveForm → partial/final JSON
        onError("Vosk model present but native binding not yet linked — using system STT")
        // Fall through would be ideal; for now surface the message so the
        // developer knows the model was detected.
        // Re-entry to system path:
        voskAvailable = false
        start(onPartial, onFinal, onError)
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
