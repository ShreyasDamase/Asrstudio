package org.asr.studio.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // ── Top Bar: Mobile Health & Telemetry Metrics ──────────────────────
        Text("Mobile Health & Telemetry", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Battery Level Card
            val batteryLevel = battery?.level ?: 0f
            val batteryColor = when {
                batteryLevel > 50f -> Color(0xFF4ADE80)
                batteryLevel > 20f -> Color(0xFFFBBF24)
                else -> Color(0xFFF87171)
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
                tempC < 38f -> Color(0xFF4ADE80) // Cool
                tempC < 43f -> Color(0xFFFBBF24) // Warm
                else -> Color(0xFFF87171)        // Hot
            }
            HealthCard(
                title = "Battery Temp",
                value = if (battery != null) "${"%.1f".format(tempC)}°C" else "--",
                subtitle = if (tempC > 42f) "High Thermal Load — stop unused modules" else "Optimal thermal range",
                valueColor = tempColor,
                modifier = Modifier.weight(1f)
            )

            // Ambient Lux
            HealthCard(
                title = "Ambient Light",
                value = if (lightSensor != null) "${lightSensor!!.lux.toInt()} lux" else "--",
                subtitle = if (lightSensor != null && lightSensor!!.lux < 50f) "Low light environment" else "Normal light environment",
                valueColor = Color(0xFF5EEAD4),
                modifier = Modifier.weight(1f)
            )

            // Active Packet Stream Rate
            HealthCard(
                title = "Data Stream",
                value = if (imu != null || latestCameraFrame != null) "ONLINE" else "IDLE",
                subtitle = if (latestCameraFrame != null) "${latestCameraFrame!!.width}x${latestCameraFrame!!.height} @ ${"%.0f".format(latestCameraFrame!!.fps)}fps" else "Sensors streaming",
                valueColor = if (imu != null || latestCameraFrame != null) Color(0xFF5EEAD4) else Color(0xFF8B90A3),
                modifier = Modifier.weight(1f)
            )
        }

        // ── Sensor Power & Thermal Management ───────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Hardware Sensor Power & Thermal Control", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "Selectively disable modules to prevent phone overheating and extend battery life during calibration.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Master ALL ON / ALL OFF Buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { toggleAll(true) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF059669),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("ALL ON", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Button(
                            onClick = { toggleAll(false) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFDC2626),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("ALL OFF", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                // Sensor Toggles Grid (2 Rows x 3 Cols)
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        SensorToggleCard(
                            name = "Camera Stream",
                            subtext = "High CPU / ISP load",
                            active = cameraActive,
                            isStreaming = latestCameraFrame != null,
                            onToggle = { sendModuleCommand("camera", it) },
                            modifier = Modifier.weight(1f)
                        )
                        SensorToggleCard(
                            name = "IMU Suite",
                            subtext = "Accel, Gyro, Mag (400Hz)",
                            active = imuActive,
                            isStreaming = imu != null,
                            onToggle = { sendModuleCommand("imu", it) },
                            modifier = Modifier.weight(1f)
                        )
                        SensorToggleCard(
                            name = "GPS Navigation",
                            subtext = "High power GPS lock",
                            active = gpsActive,
                            isStreaming = gps != null,
                            onToggle = { sendModuleCommand("gps", it) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        SensorToggleCard(
                            name = "Microphone Array",
                            subtext = "16 kHz PCM Audio",
                            active = audioActive,
                            isStreaming = mic != null,
                            onToggle = { sendModuleCommand("audio", it) },
                            modifier = Modifier.weight(1f)
                        )
                        SensorToggleCard(
                            name = "TTS Speaker",
                            subtext = "Voice alert synthesis",
                            active = speakerActive,
                            isStreaming = true,
                            onToggle = { sendModuleCommand("speaker", it) },
                            modifier = Modifier.weight(1f)
                        )
                        SensorToggleCard(
                            name = "Environment",
                            subtext = "Light / Temp sensors",
                            active = envActive,
                            isStreaming = lightSensor != null,
                            onToggle = { sendModuleCommand("environment", it) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (adbStatusMessage.isNotBlank()) {
                    Text(
                        adbStatusMessage,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF5EEAD4),
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
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = valueColor)
            Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SensorToggleCard(
    name: String,
    subtext: String,
    active: Boolean,
    isStreaming: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.border(
            width = 1.dp,
            color = if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f) else Color.Transparent,
            shape = RoundedCornerShape(8.dp)
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.60f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        Modifier
                            .size(7.dp)
                            .background(
                                color = if (active && isStreaming) Color(0xFF4ADE80) else if (active) Color(0xFFFBBF24) else Color(0xFF64748B),
                                shape = CircleShape
                            )
                    )
                    Text(name, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                }
                Spacer(Modifier.height(2.dp))
                Text(subtext, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Switch(
                checked = active,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFF5EEAD4),
                    checkedTrackColor = Color(0xFF0F766E)
                )
            )
        }
    }
}
