package org.asr.studio.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.asr.studio.data.*
import org.asr.studio.runAdbShell
import org.asr.studio.ui.theme.StudioColors
import org.asr.studio.ui.theme.StudioSpacing
import org.asr.studio.ui.theme.StudioRadii
import org.asr.studio.ui.theme.StudioTypeSize
import org.asr.studio.ui.theme.StudioFonts
import org.asr.studio.ui.components.StudioToggle

/**
 * Real-time Robotics Dashboard & Hardware Thermal Manager.
 *
 * Features:
 * - Mobile Health Monitoring (Battery %, Temperature, Voltage, Charging status, Lux).
 * - Individual Sensor / Hardware Power Toggles (Camera, IMU, GPS, Audio, Speaker, Environment).
 * - Master "ALL ON" and "ALL OFF" controls to prevent phone overheating during selective sensor calibration.
 */
@Composable
fun DashboardScreen(
    sensorRepository: SensorRepository,
    cameraRepository: CameraRepository
) {
    val battery by sensorRepository.battery.collectAsState()
    val lightSensor by sensorRepository.lightSensor.collectAsState()
    val imu by sensorRepository.imu.collectAsState()
    val gps by sensorRepository.gps.collectAsState()
    val mic by sensorRepository.mic.collectAsState()
    val latestCameraFrame by cameraRepository.latestFrame.collectAsState()

    val scope = rememberCoroutineScope()
    var adbStatusMessage by remember { mutableStateOf("") }

    val moduleStates by sensorRepository.modulePowerStates.collectAsState()

    val cameraActive = moduleStates["camera"] ?: true
    val imuActive = moduleStates["imu"] ?: true
    val gpsActive = moduleStates["gps"] ?: true
    val audioActive = moduleStates["audio"] ?: true
    val speakerActive = moduleStates["speaker"] ?: true
    val envActive = moduleStates["environment"] ?: true

    fun sendModuleCommand(moduleId: String, start: Boolean) {
        sensorRepository.setModulePower(moduleId, start)
        val action = if (start) "org.asr.runtime.MODULE_START" else "org.asr.runtime.MODULE_STOP"
        scope.launch {
            adbStatusMessage = "Sending $moduleId ${if (start) "START" else "STOP"}..."
            val result = withContext(Dispatchers.IO) {
                runAdbShell(
                    "am", "start-foreground-service",
                    "-a", action,
                    "--es", "module_id", moduleId,
                    "org.asr.runtime/.core.AsrService"
                )
            }
            adbStatusMessage = "Module '$moduleId' -> ${if (start) "ACTIVATED" else "STOPPED"}"
        }
    }

    fun toggleAll(startAll: Boolean) {
        sensorRepository.setAllModulesPower(startAll)
        listOf("camera", "imu", "gps", "audio", "speaker", "environment").forEach { mod ->
            val action = if (startAll) "org.asr.runtime.MODULE_START" else "org.asr.runtime.MODULE_STOP"
            scope.launch {
                withContext(Dispatchers.IO) {
                    runAdbShell(
                        "am", "start-foreground-service",
                        "-a", action,
                        "--es", "module_id", mod,
                        "org.asr.runtime/.core.AsrService"
                    )
                }
            }
        }
        adbStatusMessage = if (startAll) "All modules ACTIVATED" else "All modules STOPPED"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = StudioSpacing.page, vertical = StudioSpacing.lg)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(StudioSpacing.lg)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("Device overview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("Health, sensor modules, and telemetry received from the phone.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val twoColumnLayout = maxWidth < 720.dp
            val batteryLevel = battery?.level ?: 0f
            val batteryColor = when {
                batteryLevel > 50f -> StudioColors.success
                batteryLevel > 20f -> StudioColors.warning
                else -> StudioColors.error
            }
            val tempC = battery?.temperature ?: 0f
            val tempColor = when {
                tempC < 38f -> StudioColors.success
                tempC < 43f -> StudioColors.warning
                else -> StudioColors.error
            }
            val hasSensorData = imu != null || latestCameraFrame != null
            val healthCards: List<@Composable (Modifier) -> Unit> = listOf(
                { modifier ->
                    HealthCard(
                        title = "Battery",
                        value = if (battery != null) "${batteryLevel.toInt()}%" else "--",
                        subtitle = if (battery?.isCharging == true) "Charging (${"%.1f".format(battery!!.voltage / 1000f)}V)" else "Discharging",
                        valueColor = batteryColor,
                        modifier = modifier,
                    )
                },
                { modifier ->
                    HealthCard(
                    title = "Battery temp",
                    value = if (battery != null) "${"%.1f".format(tempC)}°C" else "--",
                    subtitle = if (tempC > 42f) "High Thermal Load — stop unused modules" else "Optimal thermal range",
                    valueColor = tempColor,
                    modifier = modifier,
                    )
                },
                { modifier ->
                    HealthCard(
                        title = "Ambient light",
                        value = if (lightSensor != null) "${lightSensor!!.lux.toInt()} lux" else "--",
                        subtitle = if (lightSensor != null && lightSensor!!.lux < 50f) "Low light environment" else "Normal light environment",
                        valueColor = StudioColors.accent,
                        modifier = modifier,
                    )
                },
                { modifier ->
                    HealthCard(
                        title = "Telemetry",
                        value = if (hasSensorData) "DATA SEEN" else "NO DATA",
                        subtitle = when {
                            latestCameraFrame != null -> "Camera ${latestCameraFrame!!.width}×${latestCameraFrame!!.height} · ${"%.0f".format(latestCameraFrame!!.fps)} fps"
                            imu != null -> "IMU sample available"
                            else -> "Waiting for sensor messages"
                        },
                        valueColor = if (hasSensorData) StudioColors.success else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = modifier,
                    )
                },
            )

            if (twoColumnLayout) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    healthCards.chunked(2).forEach { rowCards ->
                        Row(
                            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            rowCards.forEach { card -> card(Modifier.weight(1f).fillMaxHeight()) }
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    healthCards.forEach { card -> card(Modifier.weight(1f).fillMaxHeight()) }
                }
            }
        }

        // ── Sensor Power & Thermal Management ───────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(modifier = Modifier.padding(StudioSpacing.lg), verticalArrangement = Arrangement.spacedBy(StudioSpacing.lg)) {
                Column(verticalArrangement = Arrangement.spacedBy(StudioSpacing.sm)) {
                    Column {
                        Text("Sensor modules", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("Control phone sensors independently.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(StudioSpacing.sm)) {
                        OutlinedButton(
                            onClick = { toggleAll(true) },
                            modifier = Modifier.weight(1f),
                            shape = StudioRadii.controlShape,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                            contentPadding = PaddingValues(horizontal = StudioSpacing.md, vertical = StudioSpacing.xs),
                        ) {
                            Text("ENABLE ALL", fontWeight = FontWeight.Medium, fontSize = StudioTypeSize.controlLabel, maxLines = 1, softWrap = false)
                        }

                        OutlinedButton(
                            onClick = { toggleAll(false) },
                            modifier = Modifier.weight(1f),
                            shape = StudioRadii.controlShape,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                            contentPadding = PaddingValues(horizontal = StudioSpacing.md, vertical = StudioSpacing.xs),
                        ) {
                            Text("DISABLE ALL", fontWeight = FontWeight.Medium, fontSize = StudioTypeSize.controlLabel, maxLines = 1, softWrap = false)
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                BoxWithConstraints {
                    val columns = when {
                        maxWidth < 560.dp -> 1
                        maxWidth < 900.dp -> 2
                        else -> 3
                    }
                    val modules = listOf(
                        Triple("Camera Stream", "High CPU / ISP load", "camera" to cameraActive),
                        Triple("IMU Suite", "Accel, Gyro, Mag · 400 Hz", "imu" to imuActive),
                        Triple("GPS Navigation", "High power GPS lock", "gps" to gpsActive),
                        Triple("Microphone Array", "16 kHz PCM Audio", "audio" to audioActive),
                        Triple("TTS Speaker", "Voice alert synthesis", "speaker" to speakerActive),
                        Triple("Environment", "Light / Temp sensors", "environment" to envActive),
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(StudioSpacing.md)) {
                        modules.chunked(columns).forEach { rowModules ->
                            Row(
                                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                                horizontalArrangement = Arrangement.spacedBy(StudioSpacing.md),
                            ) {
                                rowModules.forEach { (name, subtext, module) ->
                                    SensorToggleCard(
                                        name = name,
                                        subtext = subtext,
                                        active = module.second,
                                        onToggle = { sendModuleCommand(module.first, it) },
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                                repeat(columns - rowModules.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                }

                if (adbStatusMessage.isNotBlank()) {
                    Text(
                        adbStatusMessage,
                        fontSize = StudioTypeSize.caption,
                        fontFamily = StudioFonts.mono,
                        color = StudioColors.accent,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun HealthCard(
    title: String,
    value: String,
    subtitle: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.fillMaxHeight().padding(StudioSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(StudioSpacing.xs),
        ) {
            Text(title.uppercase(), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant, fontFamily = StudioFonts.mono)
            Text(value, fontSize = StudioTypeSize.metric, fontWeight = FontWeight.SemiBold, fontFamily = StudioFonts.mono, color = valueColor)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SensorToggleCard(
    name: String,
    subtext: String,
    active: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxHeight().border(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
            shape = MaterialTheme.shapes.medium
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.60f)
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        StudioToggle(
            checked = active,
            onCheckedChange = onToggle,
            label = name,
            supportingText = subtext,
            modifier = Modifier.padding(horizontal = StudioSpacing.md).heightIn(min = 88.dp),
        )
    }
}
