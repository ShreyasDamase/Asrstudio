package org.asr.studio.ui.diagnostics

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.log10
import kotlin.math.max
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.asr.studio.data.MicData
import org.asr.studio.playMonitorFeedback
import org.asr.studio.runAdbShell
import org.asr.studio.ui.components.StudioToggle
import org.asr.studio.ui.theme.StudioColors
import org.asr.studio.ui.theme.StudioSpacing
import org.asr.studio.ui.theme.StudioTypeSize

@Composable
private fun DiagnosticsCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(modifier = Modifier.padding(StudioSpacing.lg), content = content)
    }
}

/**
 * Microphone Monitor Screen.
 *
 * Displays real-time microphone telemetry from the Android device:
 * - RMS input level (dBFS VU meter)
 * - Peak hold level
 * - Noise floor
 * - Voice Activity Detection (VAD) status
 * - Rolling RMS waveform (last N samples)
 * - Live Speaker Monitor Output (MacBook sound card)
 *
 * ROS 2 topic mapping: /asr/audio/level & /asr/audio/raw
 */
@Composable
fun MicrophoneScreen(sensorRepository: org.asr.studio.data.SensorRepository) {
    val mic by sensorRepository.mic.collectAsState()
    val moduleStates by sensorRepository.modulePowerStates.collectAsState()
    val isMicActive = moduleStates["audio"] ?: true
    val rmsHistory = remember { mutableStateListOf<Float>() }

    var audioFeedbackEnabled by remember { mutableStateOf(false) }
    var adbResult by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    val asrService = "org.asr.runtime/.core.AsrService"
    val moduleId = "audio"

    LaunchedEffect(mic?.timestampNs) {
        val sample = mic ?: return@LaunchedEffect
        rmsHistory.add(sample.rmsLevel)
        if (rmsHistory.size > 200) rmsHistory.removeRange(0, rmsHistory.size - 200)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(StudioSpacing.lg)
    ) {
        // ── Header Status Row ────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(StudioSpacing.md)
        ) {
            MicStatusChip(
                "Input",
                when { mic != null -> "Receiving"; isMicActive -> "Waiting"; else -> "Stopped" },
                Modifier.weight(1f),
            )
            MicStatusChip(
                "Sample rate",
                mic?.let { "${it.sampleRateHz / 1000} kHz" } ?: "—",
                Modifier.weight(1f),
            )
            MicStatusChip(
                "Voice activity",
                when { mic == null -> "No signal"; mic?.voiceActive == true -> "Detected"; else -> "Quiet" },
                Modifier.weight(1f),
            )
        }

        // ── Microphone Control & Live Audio Monitor ──────────────────────────
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.30f)
            ),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(StudioSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(StudioSpacing.sm),
            ) {
                StudioToggle(
                    checked = isMicActive,
                    onCheckedChange = { nextState ->
                        sensorRepository.setModulePower("audio", nextState)
                        if (!nextState) {
                            sensorRepository.clearMic()
                            rmsHistory.clear()
                        }
                        scope.launch {
                            adbResult = if (nextState) "Starting microphone" else "Stopping microphone"
                            val action = if (nextState) "org.asr.runtime.MODULE_START" else "org.asr.runtime.MODULE_STOP"
                            val result = withContext(Dispatchers.Default) {
                                runAdbShell(
                                    "am", "start-foreground-service",
                                    "-a", action,
                                    "--es", "module_id", moduleId,
                                    asrService
                                )
                            }
                            adbResult = result
                        }
                    },
                    label = "Microphone",
                    supportingText = when {
                        mic != null -> "Capturing audio"
                        isMicActive -> "Waiting for audio data"
                        else -> "Stopped"
                    },
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.30f))

                // Live Audio Monitor Output via Desktop
                StudioToggle(
                    checked = audioFeedbackEnabled,
                    onCheckedChange = { enabled ->
                        audioFeedbackEnabled = enabled
                        sensorRepository.isLiveSpeakerOutputEnabled = enabled
                    },
                    label = "Live speaker monitor",
                    supportingText = "Play microphone audio through this computer",
                )

                if (adbResult.isNotBlank()) {
                    Text(
                        adbResult,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = if (adbResult.startsWith("ADB error:") || adbResult.startsWith("Could not run")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // ── VU Meter Card ────────────────────────────────────────────────────
        DiagnosticsCard {
            Text("Input Level Meter", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(14.dp))
            VuMeterBar(
                label = "RMS Level",
                levelDb = toDbFS(mic?.rmsLevel ?: 0f),
                color = StudioColors.accent
            )
            Spacer(Modifier.height(6.dp))
            VuMeterBar(
                label = "Peak Hold",
                levelDb = toDbFS(mic?.peakLevel ?: 0f),
                color = StudioColors.accent
            )
            Spacer(Modifier.height(6.dp))
            VuMeterBar(
                label = "Noise Floor",
                levelDb = mic?.noiseFloor ?: -60f,
                color = StudioColors.accent
            )
            Spacer(Modifier.height(14.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                StatItemMic("RMS", "${"%.1f".format(toDbFS(mic?.rmsLevel ?: 0f))} dBFS", Modifier.weight(1f))
                StatItemMic("Peak", "${"%.1f".format(toDbFS(mic?.peakLevel ?: 0f))} dBFS", Modifier.weight(1f))
                StatItemMic("Noise Floor", "${"%.1f".format(mic?.noiseFloor ?: -60f)} dBFS", Modifier.weight(1f))
                StatItemMic("VAD", if (mic?.voiceActive == true) "Active" else "Silent", Modifier.weight(1f))
            }
        }

        // ── Waveform History Canvas ──────────────────────────────────────────
        DiagnosticsCard {
            Text("RMS Waveform (last ${rmsHistory.size} samples)",
                 style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            MicWaveformCanvas(rmsHistory, Modifier.fillMaxWidth().height(160.dp))
        }

        // ── ROS 2 Topic Info Card ────────────────────────────────────────────
        DiagnosticsCard {
            Text("ROS 2 Topic Reference", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Text("Subscribe to microphone telemetry in your ROS 2 node:",
                 color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = StudioColors.canvas,
                shape = MaterialTheme.shapes.medium
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        "/asr/audio/level  [asr_msgs/msg/AudioLevel]",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = StudioColors.accent,
                        fontSize = StudioTypeSize.controlLabel
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Fields: float32 rms_level  float32 peak_level  float32 noise_floor_dbfs  bool voice_active",
                        fontFamily = FontFamily.Monospace,
                        color = StudioColors.textMuted,
                        fontSize = StudioTypeSize.caption
                    )
                }
            }
        }
    }
}

/** Convert normalised level (0..1) to dBFS */
private fun toDbFS(level: Float): Float {
    if (level <= 0f) return -60f
    return (20f * log10(level)).coerceIn(-60f, 0f)
}

@Composable
private fun VuMeterBar(label: String, levelDb: Float, color: Color) {
    val fraction = ((levelDb + 60f) / 60f).coerceIn(0f, 1f)
    Row(modifier = Modifier.fillMaxWidth().height(26.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.width(90.dp), fontSize = StudioTypeSize.supporting,
             color = MaterialTheme.colorScheme.onSurfaceVariant)
        Canvas(modifier = Modifier.weight(1f).height(14.dp)) {
            drawRoundRect(StudioColors.canvas, size = size,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f))
            if (fraction > 0f) {
                drawRoundRect(
                    color = color.copy(alpha = 0.85f),
                    size = androidx.compose.ui.geometry.Size(size.width * fraction, size.height),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                )
            }
        }
        Text("${"%.0f".format(levelDb)}dB", modifier = Modifier.width(56.dp),
             fontFamily = FontFamily.Monospace, fontSize = StudioTypeSize.caption, color = color)
    }
}

@Composable
private fun MicWaveformCanvas(history: List<Float>, modifier: Modifier) {
    Canvas(modifier = modifier.background(StudioColors.canvas, MaterialTheme.shapes.medium).padding(10.dp)) {
        if (history.size < 2) return@Canvas
        val pad = 8f
        val w = size.width - pad * 2f
        val h = size.height - pad * 2f
        drawLine(StudioColors.border, Offset(pad, pad + h / 2f), Offset(pad + w, pad + h / 2f), 1f)
        val path = Path()
        history.forEachIndexed { index, rms ->
            val x = pad + w * index / max(1, history.lastIndex)
            val barH = (rms.coerceIn(0f, 1f)) * h
            val y = pad + h - barH
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        val filled = Path().apply {
            addPath(path)
            lineTo(pad + w, pad + h)
            lineTo(pad, pad + h)
            close()
        }
        drawPath(filled, StudioColors.success.copy(alpha = 0.20f))
        drawPath(path, StudioColors.success, style = Stroke(width = 2f, cap = StrokeCap.Round))
    }
}

@Composable
private fun MicStatusChip(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.medium)
            .padding(horizontal = StudioSpacing.md, vertical = StudioSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(StudioSpacing.xxs),
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun StatItemMic(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall,
             color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleSmall,
             fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
    }
}
