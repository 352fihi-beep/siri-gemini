package com.siri.gemini.voice

import android.content.Context
import android.os.Bundle
import android.service.voice.VoiceInteractionSession
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import com.siri.gemini.ai.LocalSttEngine
import com.siri.gemini.ui.GlassmorphicAssistantOverlay

/**
 * Visible assistant surface. Glassmorphic + local STT ready.
 */
class SiriGeminiVoiceInteractionSession(context: Context) : VoiceInteractionSession(context) {

    private val stt = LocalSttEngine(context)

    override fun onCreateContentView(): View {
        return ComposeView(context).apply {
            setContent {
                var listening by remember { mutableStateOf(true) }
                var partial by remember { mutableStateOf("") }
                var finalText by remember { mutableStateOf<String?>(null) }

                GlassmorphicAssistantOverlay(
                    listening = listening,
                    partialTranscript = partial,
                    finalTranscript = finalText,
                    onDismiss = {
                        stt.cancel()
                        hide()
                    },
                    onMicTap = {
                        listening = true
                        partial = ""
                        finalText = null
                        stt.start(
                            onPartial = { partial = it },
                            onFinal = {
                                finalText = it
                                listening = false
                                // TODO: feed to local LLM / command router
                            },
                            onError = {
                                partial = "Error: $it"
                                listening = false
                            }
                        )
                    }
                )
            }
        }
    }

    override fun onShow(args: Bundle?, showFlags: Int) {
        super.onShow(args, showFlags)
        // Auto-start listening when opened via stem press
    }

    override fun onHide() {
        stt.cancel()
        super.onHide()
    }
}
