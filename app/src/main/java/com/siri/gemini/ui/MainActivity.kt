package com.siri.gemini.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.siri.gemini.ai.AiCoreBridge
import com.siri.gemini.ble.AirPodsGestureService
import com.siri.gemini.ble.aap.AapProtocol
import com.siri.gemini.ota.OtaWorker

class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.all { it }) startGestureService()
        else Toast.makeText(this, "Permissions required for BLE + mic", Toast.LENGTH_LONG).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        OtaWorker.enqueue(this)

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF0A0A10)) {
                    ControlCenter(
                        aiCoreAvailable = AiCoreBridge.isAvailable(this),
                        onStartService = { ensurePermissionsAndStart() },
                        onOpenAssistant = { startActivity(Intent(Settings.ACTION_VOICE_INPUT_SETTINGS)) },
                        onSimulateStem = {
                            startService(Intent(this, AirPodsGestureService::class.java).apply {
                                action = AirPodsGestureService.ACTION_TRIGGER_STEM
                            })
                            Toast.makeText(this, "Stem event emitted", Toast.LENGTH_SHORT).show()
                        },
                        onNoiseMode = { mode ->
                            startService(Intent(this, AirPodsGestureService::class.java).apply {
                                action = AirPodsGestureService.ACTION_SET_NOISE
                                putExtra(AirPodsGestureService.EXTRA_NOISE_MODE, mode.code)
                            })
                        }
                    )
                }
            }
        }
    }

    private fun ensurePermissionsAndStart() {
        val needed = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED)
                needed += Manifest.permission.BLUETOOTH_SCAN
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED)
                needed += Manifest.permission.BLUETOOTH_CONNECT
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            needed += Manifest.permission.ACCESS_FINE_LOCATION
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
            needed += Manifest.permission.RECORD_AUDIO

        if (needed.isEmpty()) startGestureService()
        else permissionLauncher.launch(needed.toTypedArray())
    }

    private fun startGestureService() {
        ContextCompat.startForegroundService(this, Intent(this, AirPodsGestureService::class.java))
        Toast.makeText(this, "Gesture + AAP listener running", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun ControlCenter(
    aiCoreAvailable: Boolean,
    onStartService: () -> Unit,
    onOpenAssistant: () -> Unit,
    onSimulateStem: () -> Unit,
    onNoiseMode: (AapProtocol.NoiseMode) -> Unit
) {
    var selectedNoise by remember { mutableStateOf(AapProtocol.NoiseMode.OFF) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        // Hero
        Text(
            "Siri Gemini",
            style = MaterialTheme.typography.headlineLarge,
            color = Color.White
        )
        Text(
            "AirPods H2 · on-device · LibrePods-class",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.65f)
        )

        Spacer(Modifier.height(28.dp))

        // Primary actions
        GlassCard {
            ActionRow(Icons.Default.Hearing, "Start gesture + AAP listener", onStartService)
            Divider(color = Color.White.copy(0.08f))
            ActionRow(Icons.Default.RecordVoiceOver, "Set as default assistant", onOpenAssistant)
            Divider(color = Color.White.copy(0.08f))
            ActionRow(Icons.Default.TouchApp, "Simulate stem press", onSimulateStem)
        }

        Spacer(Modifier.height(20.dp))

        // Noise control (LibrePods-style)
        Text("Noise Control", style = MaterialTheme.typography.titleSmall, color = Color.White.copy(0.8f))
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            NoiseChip("Off", selectedNoise == AapProtocol.NoiseMode.OFF) {
                selectedNoise = AapProtocol.NoiseMode.OFF
                onNoiseMode(AapProtocol.NoiseMode.OFF)
            }
            NoiseChip("ANC", selectedNoise == AapProtocol.NoiseMode.NOISE_CANCELLATION) {
                selectedNoise = AapProtocol.NoiseMode.NOISE_CANCELLATION
                onNoiseMode(AapProtocol.NoiseMode.NOISE_CANCELLATION)
            }
            NoiseChip("Trans", selectedNoise == AapProtocol.NoiseMode.TRANSPARENCY) {
                selectedNoise = AapProtocol.NoiseMode.TRANSPARENCY
                onNoiseMode(AapProtocol.NoiseMode.TRANSPARENCY)
            }
            NoiseChip("Adapt", selectedNoise == AapProtocol.NoiseMode.ADAPTIVE) {
                selectedNoise = AapProtocol.NoiseMode.ADAPTIVE
                onNoiseMode(AapProtocol.NoiseMode.ADAPTIVE)
            }
        }

        Spacer(Modifier.height(24.dp))

        // Status cards
        Text("System", style = MaterialTheme.typography.titleSmall, color = Color.White.copy(0.8f))
        Spacer(Modifier.height(10.dp))
        GlassCard {
            StatusLine("AICore / Gemini Nano", if (aiCoreAvailable) "Available" else "Not on this device")
            Divider(color = Color.White.copy(0.08f))
            StatusLine("Offline STT", "System + Vosk-ready")
            Divider(color = Color.White.copy(0.08f))
            StatusLine("OTA", "GitHub Releases · 12h check")
            Divider(color = Color.White.copy(0.08f))
            StatusLine("Protocol", "Continuity + AAP scaffold")
        }

        Spacer(Modifier.height(32.dp))
        Text(
            "Full L2CAP stem events require completing the LibrePods packet sequences.\n" +
            "Vosk needs the model in assets/. AICore needs the Edge SDK on a supported Pixel.",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.45f)
        )
    }
}

@Composable
fun GlassCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0x33FFFFFF), Color(0x14FFFFFF))
                )
            )
            .padding(4.dp),
        content = content
    )
}

@Composable
fun ActionRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Icon(icon, contentDescription = null, tint = Color(0xFF8BB4FF))
        Spacer(Modifier.width(14.dp))
        Text(label, color = Color.White, modifier = Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, null, tint = Color.White.copy(0.4f))
    }
}

@Composable
fun StatusLine(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.White.copy(0.75f), style = MaterialTheme.typography.bodyMedium)
        Text(value, color = Color(0xFF9EC5FF), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun RowScope.NoiseChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        modifier = Modifier.weight(1f),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Color(0xFF5B8CFF),
            selectedLabelColor = Color.White,
            containerColor = Color(0x22FFFFFF),
            labelColor = Color.White.copy(0.85f)
        )
    )
}
