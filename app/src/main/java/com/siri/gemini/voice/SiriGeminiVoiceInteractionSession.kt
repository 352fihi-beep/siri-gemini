package com.siri.gemini.voice

import android.content.Context
import android.os.Bundle
import android.service.voice.VoiceInteractionSession
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import com.siri.gemini.ui.GlassmorphicAssistantOverlay

/**
 * The visible assistant surface. Glassmorphic by design.
 */
class SiriGeminiVoiceInteractionSession(context: Context) : VoiceInteractionSession(context) {

    override fun onCreateContentView(): View {
        return ComposeView(context).apply {
            setContent {
                GlassmorphicAssistantOverlay(
                    onDismiss = { hide() }
                )
            }
        }
    }

    override fun onShow(args: Bundle?, showFlags: Int) {
        super.onShow(args, showFlags)
        // Start listening / show UI
    }
}
