package org.asr.studio.ui.speaker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.asr.studio.ui.components.StudioToggle
import org.asr.studio.ui.theme.StudioSpacing
import org.asr.studio.ui.theme.StudioRadii
import org.asr.studio.ui.theme.StudioFonts

@Composable
fun SpeakerScreen(viewModel: SpeakerViewModel) {
    val telemetry by viewModel.telemetry.collectAsState()
    val logEvents by viewModel.logEvents.collectAsState()

    var customText by remember { mutableStateOf("ASR speaker test successful") }
    val naturalMode by viewModel.naturalMode.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(StudioSpacing.page),
        verticalArrangement = Arrangement.spacedBy(StudioSpacing.lg)
    ) {

        // ── Status Overview Card ──────────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(StudioSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(StudioSpacing.md)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(StudioSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "Speaker",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "Speaker output",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val details = listOf(
                        "TTS Engine" to telemetry.ttsEngine,
                        "Language" to telemetry.language,
                        "Volume" to "${telemetry.volume}%",
                        "Output" to "Android TTS",
                    )
                    val columns = if (maxWidth < 560.dp) 2 else 4
                    Column(verticalArrangement = Arrangement.spacedBy(StudioSpacing.md)) {
                        details.chunked(columns).forEach { rowDetails ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                rowDetails.forEach { (label, value) ->
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
                                    }
                                }
                                repeat(columns - rowDetails.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                }
            }
        }

        // ── Quick Speech Tests ───────────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(StudioSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(StudioSpacing.md)
            ) {
                Text(
                    text = "Quick tests",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )

                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val tests: List<Pair<String, () -> Unit>> = listOf(
                        "Voice" to viewModel::testVoice,
                        "Warning" to viewModel::testWarning,
                        "Sequence" to viewModel::testSequence,
                    )
                    val narrow = maxWidth < 520.dp
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(StudioSpacing.sm),
                    ) {
                        tests.chunked(if (narrow) 1 else 3).forEach { rowTests ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(StudioSpacing.sm),
                            ) {
                                rowTests.forEach { (label, action) ->
                                    OutlinedButton(
                                        onClick = action,
                                        modifier = Modifier.weight(1f),
                                        shape = StudioRadii.controlShape,
                                    ) {
                                        Text(label, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                                repeat((if (narrow) 1 else 3) - rowTests.size) {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── Custom Speech Test Input ─────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(StudioSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(StudioSpacing.md)
            ) {
                Text(
                    text = "Custom speech",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )

                OutlinedTextField(
                    value = customText,
                    onValueChange = { customText = it },
                    label = { Text("Text to speak") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = StudioRadii.controlShape,
                )

                StudioToggle(
                    checked = naturalMode,
                    onCheckedChange = viewModel::setNaturalMode,
                    label = "Natural speech formatting",
                    supportingText = if (naturalMode) "Enabled" else "Send text as written",
                )

                if (naturalMode && customText.isNotBlank()) {
                    val ssmlPreview = remember(customText) { 
                        org.asr.studio.speech.SpeechNaturaliser.preview(customText) 
                    }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Natural Speech Preview", style = MaterialTheme.typography.labelSmall, 
                                 fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(
                                text = ssmlPreview,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = StudioFonts.mono,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Button(
                    onClick = { viewModel.speak(customText) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = customText.isNotBlank(),
                    shape = StudioRadii.controlShape,
                ) {
                    Text("Send speech command")
                }
            }
        }

        // ── Speaker Telemetry & Diagnostics Table ────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(StudioSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(StudioSpacing.md)
            ) {
                Text(
                    text = "Activity",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )

                ActivityRow("Commands sent", telemetry.commandsSent.toString())
                ActivityRow("Completed", telemetry.completedCount.toString())
                ActivityRow("Failed", telemetry.failedCount.toString())
                ActivityRow("Last latency", "${telemetry.lastLatencyMs} ms")
                ActivityRow("Last speech text", telemetry.lastSpeechText, mono = true)
                ActivityRow(
                    "Last result",
                    telemetry.lastResult.lowercase().replaceFirstChar { it.uppercase() },
                )
            }
        }

        // ── Execution Log ───────────────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(StudioSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(StudioSpacing.sm)
            ) {
                Text(
                    text = "Speech Execution Event Log",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )

                if (logEvents.isEmpty()) {
                    Text(
                        text = "No speech commands sent yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .verticalScroll(rememberScrollState())
                            .background(MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.small)
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(StudioSpacing.sm)
                    ) {
                        logEvents.forEachIndexed { index, event ->
                            if (index > 0) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = event.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = event.status.lowercase().replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }

    }
}

@Composable
private fun ActivityRow(label: String, value: String, mono: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = value,
            modifier = Modifier.weight(1f).padding(start = StudioSpacing.lg),
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = if (mono) StudioFonts.mono else FontFamily.Default,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
        )
    }
}
