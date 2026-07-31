package com.siri.gemini.ui

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.siri.gemini.ble.AirPodsGestureService

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SettingsScreen(
                        onStartService = {
                            startForegroundService(Intent(this, AirPodsGestureService::class.java))
                        },
                        onOpenAssistantSettings = {
                            startActivity(Intent(Settings.ACTION_VOICE_INPUT_SETTINGS))
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    onStartService: () -> Unit,
    onOpenAssistantSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Siri Gemini", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "AirPods H2 stem → on-device assistant",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(32.dp))
        Button(onClick = onStartService) {
            Text("Start gesture listener")
        }
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = onOpenAssistantSettings) {
            Text("Set as default assistant")
        }
    }
}
