package dev.anilbeesetti.nextplayer.feature.player.mod69

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

private val REVERBS = listOf("None", "Small room", "Medium room", "Large room", "Medium hall", "Large hall", "Plate")

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun Mod69AudioPage() {
    val context = LocalContext.current
    LaunchedEffect(Unit) { Mod69AudioStore.init(context) }
    val c by Mod69AudioStore.state.collectAsState()
    val freqs = Mod69AudioFx.bandFreqsHz

    // ---- Equalizer ----
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text("Equalizer", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        Switch(checked = c.eqOn, onCheckedChange = { on -> Mod69AudioStore.update { it.copy(eqOn = on) } })
    }
    if (freqs.isEmpty()) {
        Text("Start a video to load your phone's equalizer.", style = MaterialTheme.typography.bodySmall)
    }
    if (c.eqOn && freqs.isNotEmpty()) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(
                selected = c.eqPreset < 0,
                onClick = { Mod69AudioStore.update { it.copy(eqPreset = -1) } },
                label = { Text("Custom") },
            )
            Mod69AudioFx.presetNames.forEachIndexed { i, name ->
                FilterChip(
                    selected = c.eqPreset == i,
                    onClick = { Mod69AudioStore.update { it.copy(eqPreset = i) } },
                    label = { Text(name) },
                )
            }
        }
        if (c.eqPreset < 0) {
            freqs.forEachIndexed { i, hz ->
                val level = c.bands.getOrNull(i) ?: 0
                var v by remember(i, level) { mutableFloatStateOf(level.toFloat()) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (hz >= 1000) "${hz / 1000}k" else "$hz",
                        modifier = Modifier.width(44.dp),
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Slider(
                        value = v,
                        onValueChange = { v = it },
                        onValueChangeFinished = {
                            Mod69AudioStore.update { cfg ->
                                val list = MutableList(freqs.size) { j -> cfg.bands.getOrNull(j) ?: 0 }
                                list[i] = v.toInt()
                                cfg.copy(bands = list)
                            }
                        },
                        valueRange = Mod69AudioFx.levelMin.toFloat()..Mod69AudioFx.levelMax.toFloat(),
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "%+.0f dB".format(v / 100f),
                        modifier = Modifier.width(56.dp),
                        textAlign = TextAlign.End,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
            OutlinedButton(onClick = { Mod69AudioStore.update { it.copy(bands = emptyList()) } }) { Text("Flat") }
        }
    }

    StrengthSlider("Bass boost", c.bass) { s -> Mod69AudioStore.update { it.copy(bass = s) } }
    StrengthSlider("Virtualizer (wider sound)", c.virtualizer) { s -> Mod69AudioStore.update { it.copy(virtualizer = s) } }
    Text("Reverb", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        REVERBS.forEachIndexed { i, name ->
            FilterChip(
                selected = c.reverb == i,
                onClick = { Mod69AudioStore.update { it.copy(reverb = i) } },
                label = { Text(name) },
            )
        }
    }

    HorizontalDivider(Modifier.padding(vertical = 12.dp))

    // ---- Sync ----
    DelayRow("Audio delay", c.delayMs) { d -> Mod69AudioStore.update { it.copy(delayMs = d) } }
    DelayRow(
        "Bluetooth extra delay" + if (Mod69AudioFx.bluetoothActive) " (in use now)" else "",
        c.btDelayMs,
    ) { d -> Mod69AudioStore.update { it.copy(btDelayMs = d) } }
    Text(
        "+ makes the sound come later, − earlier. Bluetooth delay is added only when Bluetooth audio is connected.",
        style = MaterialTheme.typography.bodySmall,
    )

    HorizontalDivider(Modifier.padding(vertical = 12.dp))

    Text("Channels", style = MaterialTheme.typography.labelLarge)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf("Stereo", "Mono", "Swap L/R").forEachIndexed { i, name ->
            FilterChip(
                selected = c.channelMode == i,
                onClick = { Mod69AudioStore.update { it.copy(channelMode = i) } },
                label = { Text(name) },
            )
        }
    }
    SwitchLine("Fade in on start and seek", c.fade) { on -> Mod69AudioStore.update { it.copy(fade = on) } }
    SwitchLine("Volume levelling (quiet parts louder, loud parts softer)", c.leveling) { on ->
        Mod69AudioStore.update { it.copy(leveling = on) }
    }
    Text(
        "External audio file: open the audio track menu and choose \"Select audio\".",
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun StrengthSlider(label: String, value: Int, onDone: (Int) -> Unit) {
    var v by remember(value) { mutableFloatStateOf(value.toFloat()) }
    Text(
        "$label: ${(v / 10).toInt()}%",
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(top = 8.dp),
    )
    Slider(value = v, onValueChange = { v = it }, onValueChangeFinished = { onDone(v.toInt()) }, valueRange = 0f..1000f)
}

@Composable
private fun DelayRow(label: String, value: Int, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Column69(label, value)
        OutlinedButton(onClick = { onChange(value - 50) }) { Text("−50") }
        OutlinedButton(onClick = { onChange(0) }, modifier = Modifier.padding(horizontal = 4.dp)) { Text("0") }
        OutlinedButton(onClick = { onChange(value + 50) }) { Text("+50") }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.Column69(label: String, value: Int) {
    androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Text("%+.2f s".format(value / 1000f), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SwitchLine(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
