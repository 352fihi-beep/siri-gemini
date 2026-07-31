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
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.siri.gemini.ble.AirPodsGestureService
import com.siri.gemini.ota.OtaWorker

class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = results.values.all { it }
        if (granted) startGestureService()
        else Toast.makeText(this, "BLE / location permission required", Toast.LENGTH_LONG).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Kick OTA checker (doze-aware, 12 h)
        OtaWorker.enqueue(this)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SettingsScreen(
                        onStartService = { ensurePermissionsAndStart() },
                        onOpenAssistantSettings = {
                            startActivity(Intent(Settings.ACTION_VOICE_INPUT_SETTINGS))
                        },
                        onSimulateStem = {
                            val i = Intent(this, AirPodsGestureService::class.java).apply {
                                action = AirPodsGestureService.ACTION_TRIGGER_STEM
                            }
                            startService(i)
                            Toast.makeText(this, "Stem-press event emitted", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }

    private fun ensurePermissionsAndStart() {
        val needed = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                != PackageManager.PERMISSION_GRANTED
            ) needed += Manifest.permission.BLUETOOTH_SCAN
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED
            ) needed += Manifest.permission.BLUETOOTH_CONNECT
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) needed += Manifest.permission.ACCESS_FINE_LOCATION

        if (needed.isEmpty()) startGestureService()
        else permissionLauncher.launch(needed.toTypedArray())
    }

    private fun startGestureService() {
        val i = Intent(this, AirPodsGestureService::class.java)
        ContextCompat.startForegroundService(this, i)
        Toast.makeText(this, "Gesture listener running", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun SettingsScreen(
    onStartService: () -> Unit,
    onOpenAssistantSettings: () -> Unit,
    onSimulateStem: () -> Unit
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

        Button(onClick = onStartService, modifier = Modifier.fillMaxWidth()) {
            Text("Start gesture listener")
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onOpenAssistantSettings, modifier = Modifier.fillMaxWidth()) {
            Text("Set as default assistant")
        }
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onSimulateStem) {
            Text("Simulate stem press (dev)")
        }

        Spacer(Modifier.height(24.dp))
        Text(
            "No cloud API keys · GrapheneOS ready · OTA enabled",
            style = MaterialTheme.typography.labelSmall
        )
    }
}
