package org.asr.studio.ui.diagnostics

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.sqrt
import org.asr.studio.currentTimeMillis
import org.asr.studio.data.Diagnostics
import org.asr.studio.data.ImuData
import org.asr.studio.ui.theme.StudioColors
import org.asr.studio.ui.theme.StudioRadii
import org.asr.studio.ui.theme.StudioTypeSize

private enum class ImuTab(val label: String) {
    Overview("Overview"),
    Accelerometer("Accelerometer"),
    Gyroscope("Gyroscope"),
    Gravity("Gravity"),
    Rotation("Rotation Vector"),
    LinearAcceleration("Linear Acceleration"),
    Magnetometer("Magnetometer"),
}

@Composable
fun DiagnosticsScreen(viewModel: DiagnosticsViewModel) {
    val diag by viewModel.diagnostics.collectAsState()
    val imu by viewModel.imu.collectAsState()
    val calibratedImu by viewModel.calibratedImu.collectAsState()
    val refState by viewModel.orientationReference.collectAsState()
    var selectedTab by remember { mutableStateOf(ImuTab.Overview) }
    val history = remember { mutableStateListOf<ImuData>() }

    LaunchedEffect(imu?.timestampNs) {
        val sample = imu ?: return@LaunchedEffect
        history.add(sample)
        if (history.size > 140) history.removeRange(0, history.size - 140)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ImuTab.entries.forEach { tab ->
                TabPill(
                    label = tab.label,
                    selected = selectedTab == tab,
                    onClick = { selectedTab = tab },
                )
            }
        }

        when (selectedTab) {
            ImuTab.Overview -> ImuOverview(diag, imu, calibratedImu, refState, viewModel, history)
            ImuTab.Accelerometer -> VectorSensorTab(
                title = "Accelerometer",
                badge = "RAW",
                unit = "m/s2",
                x = imu?.accelX ?: 0f,
                y = imu?.accelY ?: 0f,
                z = imu?.accelZ ?: 0f,
                magnitude = imu?.let { vectorMagnitude(it.accelX, it.accelY, it.accelZ) } ?: 0f,
                range = 12f,
                rate = "400 Hz",
                packetRate = diag.packetRate,
                history = history.map { Triple(it.accelX, it.accelY, it.accelZ) },
                explanation = "Measures total specific force on the device. At rest the magnitude should sit near local gravity, about 9.81 m/s2.",
                relation = "Accelerometer equals Gravity plus Linear Acceleration, so it mixes tilt and actual movement.",
            )
            ImuTab.Gyroscope -> VectorSensorTab(
                title = "Gyroscope",
                badge = "RAW",
                unit = "rad/s",
                x = imu?.gyroX ?: 0f,
                y = imu?.gyroY ?: 0f,
                z = imu?.gyroZ ?: 0f,
                magnitude = imu?.let { vectorMagnitude(it.gyroX, it.gyroY, it.gyroZ) } ?: 0f,
                range = 8f,
                rate = "400 Hz",
                packetRate = diag.packetRate,
                history = history.map { Triple(it.gyroX, it.gyroY, it.gyroZ) },
                explanation = "Measures angular velocity around each axis, or how fast the device is spinning right now.",
                relation = "Integrated over time and fused with accelerometer and magnetometer references, it drives the rotation vector.",
            )
            ImuTab.Gravity -> VectorSensorTab(
                title = "Gravity",
                badge = "DERIVED",
                unit = "m/s2",
                x = imu?.gravityX ?: 0f,
                y = imu?.gravityY ?: 0f,
                z = imu?.gravityZ ?: 0f,
                magnitude = imu?.let { vectorMagnitude(it.gravityX, it.gravityY, it.gravityZ) } ?: 0f,
                range = 12f,
                rate = "200 Hz",
                packetRate = diag.packetRate,
                history = history.map { Triple(it.gravityX, it.gravityY, it.gravityZ) },
                explanation = "A fused estimate of gravity alone. It follows device tilt and should remain near 9.81 m/s2 even while the phone moves.",
                relation = "Accelerometer minus Gravity gives Linear Acceleration.",
            )
            ImuTab.Rotation -> RotationTab(imu)
            ImuTab.Magnetometer -> VectorSensorTab(
                title = "Magnetometer",
                badge = "RAW",
                unit = "μT",
                x = imu?.magX ?: 0f,
                y = imu?.magY ?: 0f,
                z = imu?.magZ ?: 0f,
                magnitude = imu?.let { vectorMagnitude(it.magX, it.magY, it.magZ) } ?: 0f,
                range = 100f,
                rate = "50 Hz",
                packetRate = diag.packetRate,
                history = history.map { Triple(it.magX, it.magY, it.magZ) },
                explanation = "Raw magnetic field strength measured by the phone's magnetometer in microtesla (μT). The Earth's field is typically 25–65 μT depending on location.",
                relation = "Magnetometer readings are fused with accelerometer data to produce stable absolute heading (yaw) in the Rotation Vector. Near metal objects or electronics, readings may be disturbed.",
            )
            ImuTab.LinearAcceleration -> {
                val lx = (imu?.accelX ?: 0f) - (imu?.gravityX ?: 0f)
                val ly = (imu?.accelY ?: 0f) - (imu?.gravityY ?: 0f)
                val lz = (imu?.accelZ ?: 0f) - (imu?.gravityZ ?: 0f)
                VectorSensorTab(
                    title = "Linear Acceleration",
                    badge = "DERIVED",
                    unit = "m/s2",
                    x = lx,
                    y = ly,
                    z = lz,
                    magnitude = vectorMagnitude(lx, ly, lz),
                    range = 10f,
                    rate = "200 Hz",
                    packetRate = diag.packetRate,
                    history = history.map { Triple(it.accelX - it.gravityX, it.accelY - it.gravityY, it.accelZ - it.gravityZ) },
                    explanation = "The device's own acceleration after subtracting gravity. Near zero means the phone is still; spikes mean real motion, impact, or vibration.",
                    relation = "Linear Acceleration equals Accelerometer minus Gravity.",
                )
            }
        }
    }
}

@Composable
private fun ImuOverview(
    diag: Diagnostics,
    imu: ImuData?,
    calibratedImu: org.asr.studio.math.CalibratedImuData?,
    refState: org.asr.studio.math.OrientationReference,
    viewModel: DiagnosticsViewModel,
    history: List<ImuData>
) {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SensorChip("Accelerometer", "400 Hz", imu != null, Modifier.weight(1f))
            SensorChip("Gyroscope", "400 Hz", imu != null, Modifier.weight(1f))
            SensorChip("Gravity", "200 Hz", imu != null, Modifier.weight(1f))
            SensorChip("Rotation Vector", "fused", imu != null, Modifier.weight(1f))
            SensorChip("Magnetometer", "50 Hz", imu != null, Modifier.weight(1f))
        }

        // ── Orientation Reference / Re-Zero Control Panel ────────────────────
        DiagnosticsCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Orientation Reference (Re-Zero Calibration)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Establishes current phone pose as robot zero pose (0°, 0°, 0°). Raw sensor data remains untouched.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Surface(
                    color = when (refState.state) {
                        org.asr.studio.math.ReferenceState.NOT_SET -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                        org.asr.studio.math.ReferenceState.SESSION_SET -> StudioColors.success.copy(alpha = 0.15f)
                        org.asr.studio.math.ReferenceState.SAVED -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    },
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = when (refState.state) {
                            org.asr.studio.math.ReferenceState.NOT_SET -> "Reference: Not Set"
                            org.asr.studio.math.ReferenceState.SESSION_SET -> "Reference: Session Active"
                            org.asr.studio.math.ReferenceState.SAVED -> "Reference: Saved Mount Calibrated"
                        },
                        color = when (refState.state) {
                            org.asr.studio.math.ReferenceState.NOT_SET -> MaterialTheme.colorScheme.secondary
                            org.asr.studio.math.ReferenceState.SESSION_SET -> StudioColors.success
                            org.asr.studio.math.ReferenceState.SAVED -> MaterialTheme.colorScheme.primary
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = StudioTypeSize.supporting,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { viewModel.setZero() },
                    modifier = Modifier.weight(1f),
                    enabled = imu != null,
                    shape = StudioRadii.controlShape,
                ) {
                    Text("Set Current as Zero")
                }

                OutlinedButton(
                    onClick = { viewModel.resetReference() },
                    modifier = Modifier.weight(1f),
                    enabled = refState.state != org.asr.studio.math.ReferenceState.NOT_SET,
                    shape = StudioRadii.controlShape,
                ) {
                    Text("Reset Reference")
                }

                Button(
                    onClick = { viewModel.saveMountCalibration() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    enabled = refState.state != org.asr.studio.math.ReferenceState.NOT_SET,
                    shape = StudioRadii.controlShape,
                ) {
                    Text("Save Mount Calibration")
                }
            }

            Spacer(Modifier.height(14.dp))

            // Dual Readout: Raw Sensor Orientation vs Robot Relative Orientation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Raw Readout
                val rawEuler = calibratedImu?.rawEuler ?: org.asr.studio.math.EulerAngles(0f, 0f, 0f, 0f, 0f, 0f)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f), MaterialTheme.shapes.medium)
                        .padding(12.dp)
                ) {
                    Text("RAW SENSOR ORIENTATION", fontSize = StudioTypeSize.caption, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Text("Pitch: ${"%.1f".format(rawEuler.pitchDeg)}°  Roll: ${"%.1f".format(rawEuler.rollDeg)}°  Yaw: ${"%.1f".format(rawEuler.yawDeg)}°", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold, fontSize = StudioTypeSize.controlLabel)
                }

                // Robot Relative Readout
                val relEuler = calibratedImu?.relativeEuler ?: org.asr.studio.math.EulerAngles(0f, 0f, 0f, 0f, 0f, 0f)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            if (refState.state != org.asr.studio.math.ReferenceState.NOT_SET) StudioColors.success.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                            MaterialTheme.shapes.medium
                        )
                        .padding(12.dp)
                ) {
                    Text("ROBOT RELATIVE ORIENTATION (CALIBRATED)", fontSize = StudioTypeSize.caption, fontWeight = FontWeight.Bold, color = if (refState.state != org.asr.studio.math.ReferenceState.NOT_SET) StudioColors.success else MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Text("Pitch: ${"%.1f".format(relEuler.pitchDeg)}°  Roll: ${"%.1f".format(relEuler.rollDeg)}°  Yaw: ${"%.1f".format(relEuler.yawDeg)}°", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = StudioTypeSize.controlLabel, color = if (refState.state != org.asr.studio.math.ReferenceState.NOT_SET) StudioColors.success else MaterialTheme.colorScheme.onSurface)
                }
            }
        }

        DiagnosticsCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Live device orientation", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(10.dp))
                Badge("IMU")
                Spacer(Modifier.weight(1f))
                Text("Drag to orbit · scroll to zoom", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(12.dp))
            OrientationCanvas(imu, modifier = Modifier.fillMaxWidth().height(420.dp))
            AxisLegend()
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                PhonePosePanel(imu, modifier = Modifier.weight(1f))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Euler angles", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    EulerCards(imu)
                }
            }
        }

        DiagnosticsCard {
            Text("Gravity vs. Linear Acceleration", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                VectorSummary("Accelerometer", "RAW", imu?.accelX ?: 0f, imu?.accelY ?: 0f, imu?.accelZ ?: 0f, Modifier.weight(1f))
                VectorSummary("Gravity", "DERIVED", imu?.gravityX ?: 0f, imu?.gravityY ?: 0f, imu?.gravityZ ?: 0f, Modifier.weight(1f))
                val lx = (imu?.accelX ?: 0f) - (imu?.gravityX ?: 0f)
                val ly = (imu?.accelY ?: 0f) - (imu?.gravityY ?: 0f)
                val lz = (imu?.accelZ ?: 0f) - (imu?.gravityZ ?: 0f)
                VectorSummary("Linear Acceleration", "DERIVED", lx, ly, lz, Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "Gravity tracks tilt; linear acceleration stays near zero at rest and spikes only during real motion.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        DiagnosticsCard {
            Text("Raw & Protocol Diagnostics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(14.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                StatItem("Packet Rate", "${"%.1f".format(diag.packetRate)} pps", Modifier.weight(1f))
                StatItem("Byte Rate", "${"%.1f".format(diag.byteRate / 1024.0)} kbps", Modifier.weight(1f))
                StatItem("Total Drops", diag.dropCount.toString(), Modifier.weight(1f))
                StatItem("Heartbeat", if (diag.lastHeartbeatTimeMs > 0) "${(currentTimeMillis() - diag.lastHeartbeatTimeMs) / 1000}s ago" else "Never", Modifier.weight(1f))
            }
            Spacer(Modifier.height(14.dp))
            MiniHistoryStrip(history)
        }
    }
}

@Composable
private fun VectorSensorTab(
    title: String,
    badge: String,
    unit: String,
    x: Float,
    y: Float,
    z: Float,
    magnitude: Float,
    range: Float,
    rate: String,
    packetRate: Double,
    history: List<Triple<Float, Float, Float>>,
    explanation: String,
    relation: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            DiagnosticsCard(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(10.dp))
                    Badge(badge)
                    Spacer(Modifier.weight(1f))
                    Text("$rate · $unit", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(16.dp))
                AxisBar("X", x, range, AxisX)
                AxisBar("Y", y, range, AxisY)
                AxisBar("Z", z, range, AxisZ)
                Spacer(Modifier.height(18.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    StatItem("Sampling Rate", rate, Modifier.weight(1f))
                    StatItem("Calibration", "Good", Modifier.weight(1f))
                    StatItem("Packet Rate", "${"%.1f".format(packetRate)} pps", Modifier.weight(1f))
                    StatItem("Magnitude", "${"%.2f".format(magnitude)} $unit", Modifier.weight(1f))
                }
            }
            DiagnosticsCard(modifier = Modifier.weight(1f)) {
                Text("Time Series", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(14.dp))
                TimeSeriesCanvas(history, range, Modifier.fillMaxWidth().height(210.dp))
                AxisLegend(endLabel = "range +/-${range.toInt()} $unit")
            }
        }
        DiagnosticsCard {
            Text("What This Measures", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Text(explanation, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            Text(relation, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun RotationTab(imu: ImuData?) {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            DiagnosticsCard(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Orientation", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(10.dp))
                    Badge("FUSION OUTPUT")
                }
                Spacer(Modifier.height(14.dp))
                OrientationCanvas(imu, modifier = Modifier.fillMaxWidth().height(430.dp))
            }
            DiagnosticsCard(modifier = Modifier.weight(1f)) {
                Text("Phone Pose", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(14.dp))
                PhonePosePanel(imu)
                Spacer(Modifier.height(18.dp))
                Text("Quaternion Components", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                QuaternionBar("W", imu?.rotW ?: 1f, StudioColors.quaternionW)
                QuaternionBar("X", imu?.rotX ?: 0f, AxisX)
                QuaternionBar("Y", imu?.rotY ?: 0f, AxisY)
                QuaternionBar("Z", imu?.rotZ ?: 0f, AxisZ)
                Spacer(Modifier.height(18.dp))
                EulerCards(imu)
                Spacer(Modifier.height(18.dp))
                Text(
                    "A unit quaternion describes device orientation relative to the world frame. The pitch, roll, and yaw cards are a readable conversion of the same fused signal.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun DiagnosticsCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(modifier = Modifier.padding(18.dp), content = content)
    }
}

@Composable
private fun TabPill(label: String, selected: Boolean, onClick: () -> Unit) {
    val color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier
            .height(38.dp)
            .background(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                StudioRadii.controlShape,
            )
            .border(1.dp, color.copy(alpha = if (selected) 0.45f else 0.16f), StudioRadii.controlShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = color, fontWeight = FontWeight.SemiBold, fontSize = StudioTypeSize.controlLabel)
    }
}

@Composable
private fun SensorChip(label: String, meta: String, active: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .height(58.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f), StudioRadii.controlShape)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), StudioRadii.controlShape)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            label,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            softWrap = false,
        )
        Text(
            if (active) meta else "No data",
            color = if (active) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.outline,
            fontFamily = FontFamily.Monospace,
            fontSize = StudioTypeSize.caption,
            maxLines = 1,
            softWrap = false,
        )
    }
}

@Composable
private fun AxisBar(label: String, value: Float, range: Float, color: Color) {
    Row(modifier = Modifier.fillMaxWidth().height(30.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.width(24.dp), color = color, fontWeight = FontWeight.Bold)
        Canvas(modifier = Modifier.weight(1f).height(16.dp)) {
            val centerX = size.width / 2f
            drawRoundRect(StudioColors.canvas, size = size, cornerRadius = androidx.compose.ui.geometry.CornerRadius(5f, 5f))
            drawLine(StudioColors.borderStrong, Offset(centerX, 0f), Offset(centerX, size.height), strokeWidth = 1.2f)
            val clamped = value.coerceIn(-range, range)
            val end = centerX + (clamped / range) * centerX
            drawLine(color, Offset(centerX, size.height / 2f), Offset(end, size.height / 2f), strokeWidth = size.height, cap = StrokeCap.Round)
        }
        Text(formatNumber(value), modifier = Modifier.width(82.dp), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun QuaternionBar(label: String, value: Float, color: Color) {
    Row(modifier = Modifier.fillMaxWidth().height(30.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.width(26.dp), color = color, fontWeight = FontWeight.Bold)
        Canvas(modifier = Modifier.weight(1f).height(15.dp)) {
            val centerX = size.width / 2f
            drawRoundRect(StudioColors.canvas, size = size, cornerRadius = androidx.compose.ui.geometry.CornerRadius(5f, 5f))
            drawLine(StudioColors.borderStrong, Offset(centerX, 0f), Offset(centerX, size.height), strokeWidth = 1.1f)
            val end = centerX + value.coerceIn(-1f, 1f) * centerX
            drawLine(color, Offset(centerX, size.height / 2f), Offset(end, size.height / 2f), strokeWidth = size.height, cap = StrokeCap.Round)
        }
        Text(formatNumber(value), modifier = Modifier.width(70.dp), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun TimeSeriesCanvas(values: List<Triple<Float, Float, Float>>, range: Float, modifier: Modifier) {
    Canvas(modifier = modifier.background(StudioColors.canvas, MaterialTheme.shapes.medium).padding(10.dp)) {
        val pad = 18f
        val left = pad
        val top = pad
        val width = size.width - pad * 2f
        val height = size.height - pad * 2f
        repeat(3) { i ->
            val y = top + height * (i + 1) / 4f
            drawLine(StudioColors.border, Offset(left, y), Offset(left + width, y), strokeWidth = 1f)
        }
        fun drawSeries(points: List<Float>, color: Color) {
            if (points.size < 2) return
            val path = Path()
            points.forEachIndexed { index, value ->
                val x = left + width * index / max(1, points.lastIndex)
                val y = top + height / 2f - (value.coerceIn(-range, range) / range) * (height / 2f)
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, color, style = Stroke(width = 2.3f, cap = StrokeCap.Round))
        }
        drawSeries(values.map { it.first }, AxisX)
        drawSeries(values.map { it.second }, AxisY)
        drawSeries(values.map { it.third }, AxisZ)
    }
}

@Composable
private fun OrientationCanvas(imu: ImuData?, modifier: Modifier) {
    JmeOrientationViewport(imu = imu, modifier = modifier)
}

@Composable
private fun AxisLegend(endLabel: String = "") {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
        LegendItem("X", AxisX)
        LegendItem("Y", AxisY)
        LegendItem("Z", AxisZ)
        Spacer(Modifier.weight(1f))
        if (endLabel.isNotBlank()) Text(endLabel, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 14.dp)) {
        Box(Modifier.size(10.dp).background(color, MaterialTheme.shapes.medium))
        Spacer(Modifier.width(5.dp))
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun VectorSummary(title: String, badge: String, x: Float, y: Float, z: Float, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.55f), MaterialTheme.shapes.medium)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), MaterialTheme.shapes.medium)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            Badge(badge)
        }
        Spacer(Modifier.height(10.dp))
        ValueRow("X", x)
        ValueRow("Y", y)
        ValueRow("Z", z)
    }
}

@Composable
private fun MiniHistoryStrip(history: List<ImuData>) {
    if (history.isEmpty()) {
        Text("No IMU packets received yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        val last = history.last()
        VectorSummary("Accelerometer", "RAW", last.accelX, last.accelY, last.accelZ, Modifier.weight(1f))
        VectorSummary("Gyroscope", "RAW", last.gyroX, last.gyroY, last.gyroZ, Modifier.weight(1f))
        VectorSummary("Gravity", "DERIVED", last.gravityX, last.gravityY, last.gravityZ, Modifier.weight(1f))
    }
}

@Composable
private fun PhonePosePanel(imu: ImuData?, modifier: Modifier = Modifier) {
    val pose = imu.toPhonePose()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.55f), MaterialTheme.shapes.medium)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), MaterialTheme.shapes.medium)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(pose.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(10.dp))
            Badge(pose.badge)
        }
        Spacer(Modifier.height(6.dp))
        Text(pose.detail, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PoseMetric("Flat tilt", "${"%.0f".format(pose.flatTiltDeg)} deg", Modifier.weight(1f))
            PoseMetric("Vertical tilt", "${"%.0f".format(pose.verticalTiltDeg)} deg", Modifier.weight(1f))
            PoseMetric("Dominant axis", pose.axisLabel, Modifier.weight(1f))
        }
    }
}

@Composable
private fun PoseMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(StudioColors.canvas, MaterialTheme.shapes.medium)
            .height(64.dp)
            .padding(horizontal = 8.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(
            label.uppercase(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp,
            lineHeight = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            softWrap = false,
        )
        Text(value, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold, fontSize = StudioTypeSize.compactMetric, maxLines = 1)
    }
}

@Composable
private fun EulerCards(imu: ImuData?) {
    val euler = imu.toEuler()
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        AngleCard("Pitch · X", euler.pitchDeg, AxisX, Modifier.weight(1f))
        AngleCard("Roll · Y", euler.rollDeg, AxisY, Modifier.weight(1f))
        AngleCard("Yaw · Z", euler.yawDeg, AxisZ, Modifier.weight(1f))
    }
}

@Composable
private fun AngleCard(label: String, value: Float, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.55f), MaterialTheme.shapes.medium)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), MaterialTheme.shapes.medium)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, color = color, fontSize = StudioTypeSize.supporting, fontWeight = FontWeight.Bold)
        Text("${"%.1f".format(value)} deg", fontFamily = FontFamily.Monospace, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun Badge(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.primary,
        fontSize = StudioTypeSize.caption,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f), MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f), MaterialTheme.shapes.medium)
            .padding(horizontal = 7.dp, vertical = 2.dp),
    )
}

@Composable
private fun ValueRow(label: String, value: Float) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(formatNumber(value), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun StatItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
    }
}

private enum class PoseKind {
    Portrait,
    Landscape,
    Flat,
    Unknown,
}

private data class PhonePose(
    val kind: PoseKind,
    val title: String,
    val detail: String,
    val badge: String,
    val axisLabel: String,
    val gravityX: Float,
    val gravityY: Float,
    val gravityZ: Float,
    val flatTiltDeg: Float,
    val verticalTiltDeg: Float,
)

private data class EulerAngles(
    val pitchDeg: Float,
    val rollDeg: Float,
    val yawDeg: Float,
    val pitchRad: Float,
    val rollRad: Float,
    val yawRad: Float,
)

private fun ImuData?.toPhonePose(): PhonePose {
    if (this == null) {
        return PhonePose(
            kind = PoseKind.Unknown,
            title = "Waiting for IMU",
            detail = "No live gravity vector has arrived yet.",
            badge = "NO DATA",
            axisLabel = "--",
            gravityX = 0f,
            gravityY = 1f,
            gravityZ = 0f,
            flatTiltDeg = 0f,
            verticalTiltDeg = 0f,
        )
    }

    val gx = gravityX
    val gy = gravityY
    val gz = gravityZ
    val mag = max(0.001f, vectorMagnitude(gx, gy, gz))
    val absX = abs(gx)
    val absY = abs(gy)
    val absZ = abs(gz)
    val flatTilt = (acos((absZ / mag).coerceIn(0f, 1f)) * 180f / PI.toFloat()).coerceIn(0f, 180f)
    val verticalTilt = (acos((max(absX, absY) / mag).coerceIn(0f, 1f)) * 180f / PI.toFloat()).coerceIn(0f, 180f)

    return when {
        absZ >= absX && absZ >= absY -> PhonePose(
            kind = PoseKind.Flat,
            title = if (gz >= 0f) "Flat, Screen Up" else "Flat, Screen Down",
            detail = if (gz >= 0f) "The display is facing upward, like the phone is lying on a table." else "The display is facing downward.",
            badge = "HORIZONTAL",
            axisLabel = if (gz >= 0f) "+Z" else "-Z",
            gravityX = gx,
            gravityY = gy,
            gravityZ = gz,
            flatTiltDeg = flatTilt,
            verticalTiltDeg = verticalTilt,
        )
        absY >= absX -> PhonePose(
            kind = PoseKind.Portrait,
            title = if (gy >= 0f) "Portrait Upright" else "Portrait Upside Down",
            detail = if (gy >= 0f) "The phone is vertical, with the top edge above the bottom edge." else "The phone is vertical, but the top edge is lower than the bottom edge.",
            badge = "VERTICAL",
            axisLabel = if (gy >= 0f) "+Y" else "-Y",
            gravityX = gx,
            gravityY = gy,
            gravityZ = gz,
            flatTiltDeg = flatTilt,
            verticalTiltDeg = verticalTilt,
        )
        else -> PhonePose(
            kind = PoseKind.Landscape,
            title = if (gx >= 0f) "Landscape Right" else "Landscape Left",
            detail = if (gx >= 0f) "The phone is vertical on its side, with the right edge lower." else "The phone is vertical on its side, with the left edge lower.",
            badge = "SIDEWAYS",
            axisLabel = if (gx >= 0f) "+X" else "-X",
            gravityX = gx,
            gravityY = gy,
            gravityZ = gz,
            flatTiltDeg = flatTilt,
            verticalTiltDeg = verticalTilt,
        )
    }
}

private fun ImuData?.toEuler(): EulerAngles {
    if (this == null) return EulerAngles(0f, 0f, 0f, 0f, 0f, 0f)
    val ysqr = rotY * rotY
    val t0 = 2.0 * (rotW * rotX + rotY * rotZ)
    val t1 = 1.0 - 2.0 * (rotX * rotX + ysqr)
    val roll = atan2(t0, t1)
    val t2 = (2.0 * (rotW * rotY - rotZ * rotX)).coerceIn(-1.0, 1.0)
    val pitch = kotlin.math.asin(t2)
    val t3 = 2.0 * (rotW * rotZ + rotX * rotY)
    val t4 = 1.0 - 2.0 * (ysqr + rotZ * rotZ)
    val yaw = atan2(t3, t4)
    return EulerAngles(
        pitchDeg = (pitch * 180.0 / PI).toFloat(),
        rollDeg = (roll * 180.0 / PI).toFloat(),
        yawDeg = ((yaw * 180.0 / PI + 360.0) % 360.0).toFloat(),
        pitchRad = pitch.toFloat(),
        rollRad = roll.toFloat(),
        yawRad = yaw.toFloat(),
    )
}

private fun vectorMagnitude(x: Float, y: Float, z: Float): Float = sqrt(x * x + y * y + z * z)

private fun formatNumber(value: Float): String {
    val cleaned = if (abs(value) < 0.0005f) 0f else value
    return "%.3f".format(cleaned)
}

private val AxisX = StudioColors.axisX
private val AxisY = StudioColors.success
private val AxisZ = StudioColors.axisZ
