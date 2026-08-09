package com.siri.gemini.voice

import android.content.Context
import android.os.Bundle
import android.service.voice.VoiceInteractionSession
import android.util.Log
import android.view.View
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import com.siri.gemini.ai.CommandHistory
import com.siri.gemini.ai.LocalSttEngine
import com.siri.gemini.ai.OfflineCommands
import com.siri.gemini.ui.GlassmorphicAssistantOverlay

class SiriGeminiVoiceInteractionSession(context: Context) : VoiceInteractionSession(context) {

    private val stt = LocalSttEngine(context)
    private val history = CommandHistory(context)

    override fun onCreateContentView(): View {
        return ComposeView(context).apply {
            setContent {
                var listening by remember { mutableStateOf(false) }
                var partial by remember { mutableStateOf("") }
                var finalText by remember { mutableStateOf<String?>(null) }
                var errorText by remember { mutableStateOf<String?>(null) }

                fun beginListen() {
                    listening = true
                    partial = ""
                    finalText = null
                    errorText = null
                    stt.start(
                        onPartial = { partial = it },
                        onFinal = { text ->
                            finalText = text
                            listening = false
                            history.add(text)
                            val handled = try {
                                OfflineCommands.tryHandle(context, text)
                            } catch (e: Exception) {
                                Log.e("VIS", "OfflineCommands failed", e)
                                false
                            }
                            if (handled) {
                                // Brief confirmation then dismiss
                                postDelayed({ hide() }, 800)
                            }
                        },
                        onError = { err ->
                            errorText = err
                            listening = false
                        }
                    )
                }

                GlassmorphicAssistantOverlay(
                    listening = listening,
                    partialTranscript = partial.ifBlank { errorText.orEmpty() },
                    finalTranscript = finalText,
                    onDismiss = {
                        stt.cancel()
                        hide()
                    },
                    onMicTap = { beginListen() }
                )
            }
        }
    }

    override fun onShow(args: Bundle?, showFlags: Int) {
        super.onShow(args, showFlags)
        // Auto-start listening when the voice interaction overlay is shown
    }

    override fun onHide() {
        stt.cancel()
        super.onHide()
    }
}
