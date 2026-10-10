package org.asr.studio.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Battery Level Card
            val batteryLevel = battery?.level ?: 0f
            val batteryColor = when {
                batteryLevel > 50f -> StudioColors.success
                batteryLevel > 20f -> StudioColors.warning
                else -> StudioColors.error
            }
            HealthCard(
                title = "Battery",
                value = if (battery != null) "${batteryLevel.toInt()}%" else "--",
                subtitle = if (battery?.isCharging == true) "Charging (${"%.1f".format(battery!!.voltage / 1000f)}V)" else "Discharging",
                valueColor = batteryColor,
                modifier = Modifier.weight(1f)
            )

            // Thermal / Temperature Card
            val tempC = battery?.temperature ?: 0f
            val tempColor = when {
                tempC < 38f -> StudioColors.success // Cool
                tempC < 43f -> StudioColors.warning // Warm
                else -> StudioColors.error        // Hot
            }
            HealthCard(
                title = "Battery temp",
                value = if (battery != null) "${"%.1f".format(tempC)}°C" else "--",
                subtitle = if (tempC > 42f) "High Thermal Load — stop unused modules" else "Optimal thermal range",
                valueColor = tempColor,
                modifier = Modifier.weight(1f)
            )

            // Ambient Lux
            HealthCard(
                title = "Ambient light",
                value = if (lightSensor != null) "${lightSensor!!.lux.toInt()} lux" else "--",
                subtitle = if (lightSensor != null && lightSensor!!.lux < 50f) "Low light environment" else "Normal light environment",
                valueColor = StudioColors.accent,
                modifier = Modifier.weight(1f)
            )

            // Active Packet Stream Rate
            val hasSensorData = imu != null || latestCameraFrame != null
            HealthCard(
                title = "Telemetry",
                value = if (hasSensorData) "DATA SEEN" else "NO DATA",
                subtitle = when {
                    latestCameraFrame != null -> "Camera ${latestCameraFrame!!.width}×${latestCameraFrame!!.height} · ${"%.0f".format(latestCameraFrame!!.fps)} fps"
                    imu != null -> "IMU sample available"
                    else -> "Waiting for sensor messages"
                },
                valueColor = if (hasSensorData) StudioColors.accentText else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
        }

        // ── Sensor Power & Thermal Management ───────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(modifier = Modifier.padding(StudioSpacing.lg), verticalArrangement = Arrangement.spacedBy(StudioSpacing.lg)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Sensor modules", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Control phone sensors independently.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(StudioSpacing.sm)) {
                        OutlinedButton(
                            onClick = { toggleAll(true) },
                            shape = StudioRadii.controlShape,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                            contentPadding = PaddingValues(horizontal = StudioSpacing.md, vertical = StudioSpacing.xs),
                        ) {
                            Text("Enable all", fontWeight = FontWeight.Medium, fontSize = StudioTypeSize.controlLabel)
                        }

                        OutlinedButton(
                            onClick = { toggleAll(false) },
                            shape = StudioRadii.controlShape,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                            contentPadding = PaddingValues(horizontal = StudioSpacing.md, vertical = StudioSpacing.xs),
                        ) {
                            Text("Disable all", fontWeight = FontWeight.Medium, fontSize = StudioTypeSize.controlLabel)
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                Column(verticalArrangement = Arrangement.spacedBy(StudioSpacing.md)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(StudioSpacing.md), modifier = Modifier.fillMaxWidth()) {
                        SensorToggleCard(
                            name = "Camera Stream",
                            subtext = "High CPU / ISP load",
                            active = cameraActive,
                            onToggle = { sendModuleCommand("camera", it) },
                            modifier = Modifier.weight(1f)
                        )
                        SensorToggleCard(
                            name = "IMU Suite",
                            subtext = "Accel, Gyro, Mag · 400 Hz",
                            active = imuActive,
                            onToggle = { sendModuleCommand("imu", it) },
                            modifier = Modifier.weight(1f)
                        )
                        SensorToggleCard(
                            name = "GPS Navigation",
                            subtext = "High power GPS lock",
                            active = gpsActive,
                            onToggle = { sendModuleCommand("gps", it) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(StudioSpacing.md), modifier = Modifier.fillMaxWidth()) {
                        SensorToggleCard(
                            name = "Microphone Array",
                            subtext = "16 kHz PCM Audio",
                            active = audioActive,
                            onToggle = { sendModuleCommand("audio", it) },
                            modifier = Modifier.weight(1f)
                        )
                        SensorToggleCard(
                            name = "TTS Speaker",
                            subtext = "Voice alert synthesis",
                            active = speakerActive,
                            onToggle = { sendModuleCommand("speaker", it) },
                            modifier = Modifier.weight(1f)
                        )
                        SensorToggleCard(
                            name = "Environment",
                            subtext = "Light / Temp sensors",
                            active = envActive,
                            onToggle = { sendModuleCommand("environment", it) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (adbStatusMessage.isNotBlank()) {
                    Text(
                        adbStatusMessage,
                        fontSize = StudioTypeSize.caption,
                        fontFamily = FontFamily.Monospace,
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
        Column(modifier = Modifier.padding(StudioSpacing.lg), verticalArrangement = Arrangement.spacedBy(StudioSpacing.xs)) {
            Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontSize = StudioTypeSize.metric, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace, color = valueColor)
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
        modifier = modifier.border(
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
            modifier = Modifier.padding(horizontal = StudioSpacing.md),
        )
    }
}
