package com.siri.gemini.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Glassmorphic overlay — elegant gradient + soft panel.
 * Designed for low overdraw and fast composition on mid-range hardware.
 */
@Composable
fun GlassmorphicAssistantOverlay(
    listening: Boolean = true,
    partialTranscript: String = "",
    finalTranscript: String? = null,
    onDismiss: () -> Unit = {},
    onMicTap: () -> Unit = {}
) {
    val infinite = rememberInfiniteTransition(label = "pulse")
    val scale by infinite.animateFloat(
        initialValue = 1f,
        targetValue = if (listening) 1.12f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "micScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xE00A0A0F),
                        Color(0xCC0A0A14)
                    )
                )
            )
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        // Glass panel
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight()
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(0x44FFFFFF),
                            Color(0x22FFFFFF)
                        )
                    )
                )
                .padding(horizontal = 24.dp, vertical = 28.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Siri Gemini",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White.copy(alpha = 0.95f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Pulsing mic
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .scale(scale)
                        .clip(CircleShape)
                        .background(
                            if (listening) Color(0xFF5B8CFF)
                            else Color(0x33FFFFFF)
                        )
                        .clickable(onClick = onMicTap),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Microphone",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(Modifier.height(18.dp))

                Text(
                    text = when {
                        finalTranscript != null -> finalTranscript
                        partialTranscript.isNotBlank() -> partialTranscript
                        listening -> "Listening… (on-device)"
                        else -> "Tap mic to speak"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                if (finalTranscript == null && partialTranscript.isBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "No cloud · GrapheneOS ready",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.55f)
                    )
                }
            }
        }
    }
}
