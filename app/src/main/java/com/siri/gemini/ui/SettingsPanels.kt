package com.siri.gemini.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.siri.gemini.prefs.UserPrefs

/**
 * QoL panels: stem mapping (#4), conversational awareness (#8),
 * rename (#9), hearing aid (#10), EQ (#17), history toggle (#20).
 */
@Composable
fun StemMappingPanel(prefs: UserPrefs) {
    var single by remember { mutableStateOf(prefs.stemSingle) }
    var double by remember { mutableStateOf(prefs.stemDouble) }
    var longP by remember { mutableStateOf(prefs.stemLong) }

    Text("Stem actions", color = Color.White.copy(0.85f), style = MaterialTheme.typography.titleSmall)
    Spacer(Modifier.height(8.dp))
    StemDropdown("Single press", single, UserPrefs.STEM_ACTIONS) {
        single = it; prefs.stemSingle = it
    }
    StemDropdown("Double press", double, UserPrefs.STEM_ACTIONS) {
        double = it; prefs.stemDouble = it
    }
    StemDropdown("Long press", longP, UserPrefs.STEM_ACTIONS) {
        longP = it; prefs.stemLong = it
    }
}

@Composable
private fun StemDropdown(label: String, value: String, options: List<String>, onChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.White.copy(0.75f))
        Box {
            TextButton(onClick = { expanded = true }) { Text(value, color = Color(0xFF9EC5FF)) }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { opt ->
                    DropdownMenuItem(
                        text = { Text(opt) },
                        onClick = { onChange(opt); expanded = false }
                    )
                }
            }
        }
    }
}

@Composable
fun ConversationalAwarenessToggle(prefs: UserPrefs) {
    var on by remember { mutableStateOf(prefs.conversationalAwareness) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("Conversational Awareness", color = Color.White.copy(0.85f))
        Switch(checked = on, onCheckedChange = { on = it; prefs.conversationalAwareness = it })
    }
}

@Composable
fun RenamePanel(prefs: UserPrefs, onFind: () -> Unit) {
    var name by remember { mutableStateOf(prefs.airpodsName) }
    Text("Name", color = Color.White.copy(0.85f), style = MaterialTheme.typography.titleSmall)
    OutlinedTextField(
        value = name,
        onValueChange = { name = it; prefs.airpodsName = it },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
    TextButton(onClick = onFind) { Text("Play sound on AirPods (find)") }
}

@Composable
fun HearingAidPanel(prefs: UserPrefs) {
    Text("Hearing aid", color = Color.White.copy(0.85f), style = MaterialTheme.typography.titleSmall)
    SliderRow("Amplification", prefs.haAmplification) { prefs.haAmplification = it }
    SliderRow("Balance", prefs.haBalance) { prefs.haBalance = it }
    SliderRow("Tone", prefs.haTone) { prefs.haTone = it }
    var boost by remember { mutableStateOf(prefs.haConversationBoost) }
    var loud by remember { mutableStateOf(prefs.haLoudReduction) }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("Conversation boost", color = Color.White.copy(0.75f))
        Switch(checked = boost, onCheckedChange = { boost = it; prefs.haConversationBoost = it })
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("Loud sound reduction", color = Color.White.copy(0.75f))
        Switch(checked = loud, onCheckedChange = { loud = it; prefs.haLoudReduction = it })
    }
}

@Composable
fun EqPanel(prefs: UserPrefs) {
    Text("Custom EQ", color = Color.White.copy(0.85f), style = MaterialTheme.typography.titleSmall)
    SliderRow("Bass", prefs.eqBass + 50) { prefs.eqBass = it - 50 }
    SliderRow("Treble", prefs.eqTreble + 50) { prefs.eqTreble = it - 50 }
}

@Composable
private fun SliderRow(label: String, value: Int, onChange: (Int) -> Unit) {
    var v by remember { mutableStateOf(value.toFloat()) }
    Text("$label: ${v.toInt()}", color = Color.White.copy(0.7f))
    Slider(
        value = v,
        onValueChange = { v = it; onChange(it.toInt()) },
        valueRange = 0f..100f
    )
}

@Composable
fun SpatialIndicator(active: Boolean) {
    // QoL #16
    Text(
        if (active) "Spatial Audio: Active" else "Spatial Audio: —",
        color = Color.White.copy(0.6f),
        style = MaterialTheme.typography.labelMedium
    )
}
