package com.siri.gemini.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * QoL #14 — first-run checklist.
 */
@Composable
fun OnboardingScreen(
    batteryOptOk: Boolean,
    onGrantBatteryOpt: () -> Unit,
    onStartService: () -> Unit,
    onOpenAssistant: () -> Unit,
    onDone: () -> Unit
) {
    var step by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text("Welcome to Siri Gemini", style = MaterialTheme.typography.headlineMedium, color = Color.White)
        Spacer(Modifier.height(8.dp))
        Text(
            "AirPods stem → on-device assistant. A few steps for best results.",
            color = Color.White.copy(0.7f)
        )
        Spacer(Modifier.height(28.dp))

        ChecklistItem(1, "Grant BLE, location & mic", step >= 0)
        ChecklistItem(2, "Start gesture listener", step >= 1)
        ChecklistItem(3, "Set as default assistant", step >= 2)
        ChecklistItem(4, "Disable battery optimization", batteryOptOk)
        ChecklistItem(5, "Add home-screen widget (optional)", true)

        Spacer(Modifier.height(24.dp))

        when (step) {
            0 -> Button(onClick = { onStartService(); step = 1 }, modifier = Modifier.fillMaxWidth()) {
                Text("Start listener")
            }
            1 -> Button(onClick = { onOpenAssistant(); step = 2 }, modifier = Modifier.fillMaxWidth()) {
                Text("Open assistant settings")
            }
            2 -> {
                if (!batteryOptOk) {
                    Button(onClick = onGrantBatteryOpt, modifier = Modifier.fillMaxWidth()) {
                        Text("Exempt from battery optimization")
                    }
                } else {
                    Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
                        Text("Finish")
                    }
                }
            }
            else -> Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
                Text("Finish")
            }
        }

        if (batteryOptOk && step >= 2) {
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
                Text("Done")
            }
        }
    }
}

@Composable
private fun ChecklistItem(n: Int, label: String, done: Boolean) {
    Row(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(
            if (done) "✓" else "$n.",
            color = if (done) Color(0xFF6FCF97) else Color.White.copy(0.5f),
            modifier = Modifier.width(28.dp)
        )
        Text(label, color = Color.White.copy(if (done) 0.9f else 0.6f))
    }
}
