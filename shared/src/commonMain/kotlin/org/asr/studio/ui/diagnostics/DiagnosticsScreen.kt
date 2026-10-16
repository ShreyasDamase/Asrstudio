package org.asr.studio.ui.diagnostics

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.Modifier
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
import org.asr.studio.ui.theme.StudioFonts
import org.asr.studio.ui.theme.StudioSpacing
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
@OptIn(ExperimentalLayoutApi::class)
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
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(StudioSpacing.sm),
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
                formula = "|a| = sqrt(ax^2 + ay^2 + az^2)",
                meaning = "Each axis is acceleration in the phone frame. The magnitude combines all three axes into one reading.",
                note = "At rest, the reading includes gravity and is usually near 9.81 m/s2. Motion and tilt both affect the axes.",
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
                formula = "delta_theta ~= omega * delta_t",
                meaning = "Angular velocity omega is reported per axis in rad/s. Multiplying by a short time interval estimates that axis's angle change in radians.",
                note = "The rotation vector is fused upstream; gyro integration alone accumulates bias and drift.",
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
                formula = "|g| = sqrt(gx^2 + gy^2 + gz^2)",
                meaning = "The three values are the runtime's estimated gravity vector in the phone frame; its direction changes as the phone tilts.",
                note = "Gravity is supplied by the phone runtime's sensor pipeline. Its exact filter/estimator is implementation-specific.",
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
                formula = "|B| = sqrt(Bx^2 + By^2 + Bz^2)",
                meaning = "The axes measure magnetic-field components in microtesla (uT); the magnitude is the total field strength.",
                note = "A compass heading needs calibration, tilt compensation, and local magnetic-declination correction. Nearby metal can distort readings.",
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
                    formula = "a_linear = a_accel - g",
                    meaning = "Subtract the estimated gravity component on each axis from the accelerometer reading to isolate device motion.",
                    note = "Near zero suggests little linear movement; vibration, impacts, and estimator lag can still produce residuals.",
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
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(StudioSpacing.sm),
        ) {
            SensorChip("Accelerometer", "400 Hz", imu != null, Modifier.width(150.dp))
            SensorChip("Gyroscope", "400 Hz", imu != null, Modifier.width(150.dp))
            SensorChip("Gravity", "200 Hz", imu != null, Modifier.width(150.dp))
            SensorChip("Rotation Vector", "fused", imu != null, Modifier.width(150.dp))
            SensorChip("Magnetometer", "50 Hz", imu != null, Modifier.width(150.dp))
        }

        // ── Orientation Reference / Re-Zero Control Panel ────────────────────
        DiagnosticsCard {
            Column(verticalArrangement = Arrangement.spacedBy(StudioSpacing.sm)) {
                Text("Orientation Reference (Re-Zero Calibration)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Establishes current phone pose as robot zero pose (0°, 0°, 0°). Raw sensor data remains untouched.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val narrow = maxWidth < 680.dp
                val calibrationActions: List<@Composable (Modifier) -> Unit> = listOf(
                    { modifier ->
                        Button(
                            onClick = { viewModel.setZero() },
                            modifier = modifier,
                            enabled = imu != null,
                            shape = StudioRadii.controlShape,
                        ) {
                            Text("Set Current as Zero", maxLines = 1, softWrap = false)
                        }
                    },
                    { modifier ->
                        OutlinedButton(
                            onClick = { viewModel.resetReference() },
                            modifier = modifier,
                            enabled = refState.state != org.asr.studio.math.ReferenceState.NOT_SET,
                            shape = StudioRadii.controlShape,
                        ) {
                            Text("Reset Reference", maxLines = 1, softWrap = false)
                        }
                    },
                    { modifier ->
                        Button(
                            onClick = { viewModel.saveMountCalibration() },
                            modifier = modifier,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            enabled = refState.state != org.asr.studio.math.ReferenceState.NOT_SET,
                            shape = StudioRadii.controlShape,
                        ) {
                            Text("Save Mount Calibration", maxLines = 1, softWrap = false)
                        }
                    },
                )
                if (narrow) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        calibrationActions.forEach { action -> action(Modifier.fillMaxWidth()) }
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        calibrationActions.forEach { action -> action(Modifier.weight(1f)) }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Dual Readout: Raw Sensor Orientation vs Robot Relative Orientation
            val rawEuler = calibratedImu?.rawEuler ?: org.asr.studio.math.EulerAngles(0f, 0f, 0f, 0f, 0f, 0f)
            val relEuler = calibratedImu?.relativeEuler ?: org.asr.studio.math.EulerAngles(0f, 0f, 0f, 0f, 0f, 0f)
            val relativeColor = if (refState.state != org.asr.studio.math.ReferenceState.NOT_SET) {
                StudioColors.success
            } else {
                MaterialTheme.colorScheme.onSurface
            }
            val relativeSurface = if (refState.state != org.asr.studio.math.ReferenceState.NOT_SET) {
                StudioColors.success.copy(alpha = 0.1f)
            } else {
                MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
            }
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val rawReadout: @Composable (Modifier) -> Unit = { modifier ->
                    EulerReadout(
                        title = "RAW SENSOR ORIENTATION",
                        value = "P ${"%.1f".format(rawEuler.pitchDeg)}°   R ${"%.1f".format(rawEuler.rollDeg)}°   Y ${"%.1f".format(rawEuler.yawDeg)}°",
                        modifier = modifier,
                    )
                }
                val relativeReadout: @Composable (Modifier) -> Unit = { modifier ->
                    EulerReadout(
                        title = "ROBOT RELATIVE ORIENTATION (CALIBRATED)",
                        value = "P ${"%.1f".format(relEuler.pitchDeg)}°   R ${"%.1f".format(relEuler.rollDeg)}°   Y ${"%.1f".format(relEuler.yawDeg)}°",
                        modifier = modifier,
                        valueColor = relativeColor,
                        containerColor = relativeSurface,
                    )
                }
                if (maxWidth < 680.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(StudioSpacing.sm)) {
                        rawReadout(Modifier.fillMaxWidth())
                        relativeReadout(Modifier.fillMaxWidth())
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        rawReadout(Modifier.weight(1f))
                        relativeReadout(Modifier.weight(1f))
                    }
                }
            }
        }

        DiagnosticsCard {
            Column(verticalArrangement = Arrangement.spacedBy(StudioSpacing.xs)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Live device orientation", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(10.dp))
                    Badge("IMU")
                }
                Text(
                    "Drag to orbit · scroll to zoom",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    softWrap = false,
                )
            }

            Spacer(Modifier.height(12.dp))
            OrientationCanvas(imu, modifier = Modifier.fillMaxWidth().height(420.dp))
            AxisLegend()
            Spacer(Modifier.height(8.dp))
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                if (maxWidth < 680.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(StudioSpacing.md)) {
                        PhonePosePanel(imu, modifier = Modifier.fillMaxWidth())
                        Column {
                            Text("Euler angles", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(8.dp))
                            EulerCards(imu)
                        }
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        PhonePosePanel(imu, modifier = Modifier.weight(1f))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Euler angles", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(8.dp))
                            EulerCards(imu)
                        }
                    }
                }
            }
        }

        DiagnosticsCard {
            Text("Gravity vs. Linear Acceleration", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(14.dp))
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val lx = (imu?.accelX ?: 0f) - (imu?.gravityX ?: 0f)
                val ly = (imu?.accelY ?: 0f) - (imu?.gravityY ?: 0f)
                val lz = (imu?.accelZ ?: 0f) - (imu?.gravityZ ?: 0f)
                val summaries: List<@Composable (Modifier) -> Unit> = listOf(
                    { modifier -> VectorSummary("Accelerometer", "RAW", imu?.accelX ?: 0f, imu?.accelY ?: 0f, imu?.accelZ ?: 0f, modifier) },
                    { modifier -> VectorSummary("Gravity", "DERIVED", imu?.gravityX ?: 0f, imu?.gravityY ?: 0f, imu?.gravityZ ?: 0f, modifier) },
                    { modifier -> VectorSummary("Linear Acceleration", "DERIVED", lx, ly, lz, modifier) },
                )
                val columns = if (maxWidth < 680.dp) 1 else 3
                Column(verticalArrangement = Arrangement.spacedBy(StudioSpacing.sm)) {
                    summaries.chunked(columns).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            rowItems.forEach { item -> item(Modifier.weight(1f)) }
                        }
                    }
                }
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
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val stats = listOf(
                    "Packet Rate" to "${"%.1f".format(diag.packetRate)} pps",
                    "Byte Rate" to "${"%.1f".format(diag.byteRate / 1024.0)} kbps",
                    "Total Drops" to diag.dropCount.toString(),
                    "Heartbeat" to if (diag.lastHeartbeatTimeMs > 0) "${(currentTimeMillis() - diag.lastHeartbeatTimeMs) / 1000}s ago" else "Never",
                )
                val columns = if (maxWidth < 680.dp) 2 else 4
                Column(verticalArrangement = Arrangement.spacedBy(StudioSpacing.md)) {
                    stats.chunked(columns).forEach { rowStats ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(StudioSpacing.sm)) {
                            rowStats.forEach { (label, value) -> StatItem(label, value, Modifier.weight(1f)) }
                            repeat(columns - rowStats.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            MiniHistoryStrip(history)
        }
    }
}

@Composable
private fun EulerReadout(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    containerColor: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
) {
    Column(
        modifier = modifier
            .background(containerColor, MaterialTheme.shapes.medium)
            .padding(12.dp),
    ) {
        Text(title, fontSize = StudioTypeSize.caption, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Text(value, fontFamily = StudioFonts.mono, fontWeight = FontWeight.SemiBold, fontSize = StudioTypeSize.controlLabel, color = valueColor, maxLines = 1, softWrap = false)
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
    formula: String,
    meaning: String,
    note: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val compact = maxWidth < 820.dp
            val sensorCard: @Composable (Modifier) -> Unit = { cardModifier ->
                DiagnosticsCard(modifier = cardModifier) {
                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        if (maxWidth < 560.dp) {
                            Column(verticalArrangement = Arrangement.spacedBy(StudioSpacing.xs)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.width(10.dp))
                                    Badge(badge)
                                }
                                Text("$rate · $unit", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.width(10.dp))
                                Badge(badge)
                                Spacer(Modifier.weight(1f))
                                Text("$rate · $unit", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    AxisBar("X", x, range, AxisX)
                    AxisBar("Y", y, range, AxisY)
                    AxisBar("Z", z, range, AxisZ)
                    Spacer(Modifier.height(18.dp))
                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val stats = listOf(
                            "Sampling Rate" to rate,
                            "Calibration" to "Good",
                            "Packet Rate" to "${"%.1f".format(packetRate)} pps",
                            "Magnitude" to "${"%.2f".format(magnitude)} $unit",
                        )
                        val columns = if (maxWidth < 620.dp) 2 else 4
                        Column(verticalArrangement = Arrangement.spacedBy(StudioSpacing.md)) {
                            stats.chunked(columns).forEach { rowStats ->
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(StudioSpacing.sm)) {
                                    rowStats.forEach { (label, value) -> StatItem(label, value, Modifier.weight(1f)) }
                                    repeat(columns - rowStats.size) { Spacer(Modifier.weight(1f)) }
                                }
                            }
                        }
                    }
                }
            }
            val chartCard: @Composable (Modifier) -> Unit = { cardModifier ->
                DiagnosticsCard(modifier = cardModifier) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Signal trace", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(10.dp))
                        Badge("${history.size} SAMPLES")
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Each trace shows one axis over recent packets; latest sample is marked at the right.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("+${range.toInt()} $unit", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("0", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        Text("-${range.toInt()} $unit", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(StudioSpacing.xs))
                    TimeSeriesCanvas(history, range, Modifier.fillMaxWidth().height(210.dp))
                    AxisLegend(endLabel = "UNIT $unit", latest = history.lastOrNull())
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("OLDER", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("NOW", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }
            }
            if (compact) {
                Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    sensorCard(Modifier.fillMaxWidth())
                    chartCard(Modifier.fillMaxWidth())
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    sensorCard(Modifier.weight(1f))
                    chartCard(Modifier.weight(1f))
                }
            }
        }
        SignalFormulaCard(
            formula = formula,
            meaning = meaning,
            note = note,
        )
    }
}

@Composable
private fun RotationTab(imu: ImuData?) {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val compact = maxWidth < 880.dp
            val orientationCard: @Composable (Modifier) -> Unit = { modifier ->
                DiagnosticsCard(modifier = modifier) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Orientation", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(10.dp))
                        Badge("FUSION OUTPUT")
                    }
                    Spacer(Modifier.height(14.dp))
                    OrientationCanvas(imu, modifier = Modifier.fillMaxWidth().height(if (compact) 340.dp else 430.dp))
                }
            }
            val poseCard: @Composable (Modifier) -> Unit = { modifier ->
                DiagnosticsCard(modifier = modifier) {
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
                    Spacer(Modifier.height(14.dp))
                    SignalFormulaCard(
                        formula = "q = (w, x, y, z); w^2 + x^2 + y^2 + z^2 ~= 1",
                        meaning = "The received unit quaternion encodes 3D orientation. ASR Studio converts it to Euler angles for display.",
                        note = "Roll = atan2(2(wx + yz), 1 - 2(x^2 + y^2)); pitch = asin(clamp(2(wy - zx), -1, 1)); yaw = atan2(2(wz + xy), 1 - 2(y^2 + z^2)). Fusion is performed upstream.",
                    )
                }
            }
            if (compact) {
                Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    orientationCard(Modifier.fillMaxWidth())
                    poseCard(Modifier.fillMaxWidth())
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    orientationCard(Modifier.weight(1f))
                    poseCard(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SignalFormulaCard(
    formula: String,
    meaning: String,
    note: String,
) {
    DiagnosticsCard {
        Text("Signal notes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(StudioSpacing.md))
        Text("FORMULA", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(StudioSpacing.xs))
        Text(
            formula,
            modifier = Modifier
                .fillMaxWidth()
                .background(StudioColors.canvas, StudioRadii.controlShape)
                .padding(horizontal = StudioSpacing.md, vertical = StudioSpacing.sm),
            fontFamily = StudioFonts.mono,
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(StudioSpacing.md))
        Text("HOW TO READ IT", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(StudioSpacing.xs))
        Text(meaning, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(StudioSpacing.sm))
        Text(note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            .heightIn(min = 38.dp)
            .widthIn(max = 180.dp)
            .background(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                StudioRadii.controlShape,
            )
            .border(1.dp, color.copy(alpha = if (selected) 0.45f else 0.16f), StudioRadii.controlShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            color = color,
            fontWeight = FontWeight.SemiBold,
            fontSize = StudioTypeSize.controlLabel,
            maxLines = 2,
        )
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
            fontFamily = StudioFonts.mono,
            fontSize = StudioTypeSize.caption,
            maxLines = 1,
            softWrap = false,
        )
    }
}

@Composable
private fun AxisBar(label: String, value: Float, range: Float, color: Color) {
    val canvasColor = StudioColors.canvas
    val borderColor = StudioColors.borderStrong
    Row(modifier = Modifier.fillMaxWidth().height(30.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.width(24.dp), color = color, fontWeight = FontWeight.Bold)
        Canvas(modifier = Modifier.weight(1f).height(18.dp)) {
            val centerX = size.width / 2f
            drawRect(canvasColor, size = size)
            drawRect(borderColor, size = size, style = Stroke(width = 1f))
            drawLine(borderColor, Offset(centerX, 0f), Offset(centerX, size.height), strokeWidth = 1.2f, cap = StrokeCap.Butt)
            val clamped = value.coerceIn(-range, range)
            val end = centerX + (clamped / range) * centerX
            val fillWidth = max(4f, abs(end - centerX))
            val fillStart = if (abs(end - centerX) < 4f) centerX - 2f else min(end, centerX)
            drawRect(
                color = color,
                topLeft = Offset(fillStart, 3f),
                size = Size(fillWidth, size.height - 6f),
            )
        }
        Text(formatNumber(value), modifier = Modifier.width(82.dp), fontFamily = StudioFonts.mono, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun QuaternionBar(label: String, value: Float, color: Color) {
    val canvasColor = StudioColors.canvas
    val borderColor = StudioColors.borderStrong
    Row(modifier = Modifier.fillMaxWidth().height(30.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.width(26.dp), color = color, fontWeight = FontWeight.Bold)
        Canvas(modifier = Modifier.weight(1f).height(15.dp)) {
            val centerX = size.width / 2f
            drawRect(canvasColor, size = size)
            drawRect(borderColor, size = size, style = Stroke(width = 1f))
            drawLine(borderColor, Offset(centerX, 0f), Offset(centerX, size.height), strokeWidth = 1.1f, cap = StrokeCap.Butt)
            val end = centerX + value.coerceIn(-1f, 1f) * centerX
            val fillWidth = max(4f, abs(end - centerX))
            val fillStart = if (abs(end - centerX) < 4f) centerX - 2f else min(end, centerX)
            drawRect(
                color = color,
                topLeft = Offset(fillStart, 2f),
                size = Size(fillWidth, size.height - 4f),
            )
        }
        Text(formatNumber(value), modifier = Modifier.width(70.dp), fontFamily = StudioFonts.mono, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun TimeSeriesCanvas(values: List<Triple<Float, Float, Float>>, range: Float, modifier: Modifier) {
    val gridColor = StudioColors.border
    Canvas(modifier = modifier.background(StudioColors.canvas, MaterialTheme.shapes.medium).padding(10.dp)) {
        val pad = 18f
        val left = pad
        val top = pad
        val width = size.width - pad * 2f
        val height = size.height - pad * 2f
        for (i in 0..4) {
            val y = top + height * i / 4f
            val isZero = i == 2
            drawLine(
                color = if (isZero) StudioColors.accent.copy(alpha = 0.5f) else gridColor,
                start = Offset(left, y),
                end = Offset(left + width, y),
                strokeWidth = if (isZero) 1.5f else 1f,
                cap = StrokeCap.Butt,
            )
        }
        for (i in 0..4) {
            val x = left + width * i / 4f
            drawLine(gridColor.copy(alpha = 0.55f), Offset(x, top), Offset(x, top + height), strokeWidth = 1f, cap = StrokeCap.Butt)
        }
        fun drawSeries(axis: Int, color: Color) {
            if (values.size < 2) return
            val path = Path()
            values.forEachIndexed { index, sample ->
                val value = when (axis) {
                    0 -> sample.first
                    1 -> sample.second
                    else -> sample.third
                }
                val x = left + width * index / max(1, values.lastIndex)
                val y = top + height / 2f - (value.coerceIn(-range, range) / range) * (height / 2f)
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, color, style = Stroke(width = 2f, cap = StrokeCap.Butt))
            val latest = when (axis) {
                0 -> values.last().first
                1 -> values.last().second
                else -> values.last().third
            }.coerceIn(-range, range)
            val markerX = left + width
            val markerY = top + height / 2f - (latest / range) * (height / 2f)
            drawRect(
                color = color,
                topLeft = Offset(markerX - 4f, markerY - 4f),
                size = Size(8f, 8f),
            )
        }
        drawSeries(0, AxisX)
        drawSeries(1, AxisY)
        drawSeries(2, AxisZ)
    }
}

@Composable
private fun OrientationCanvas(imu: ImuData?, modifier: Modifier) {
    JmeOrientationViewport(imu = imu, modifier = modifier)
}

@Composable
private fun AxisLegend(endLabel: String = "", latest: Triple<Float, Float, Float>? = null) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
        val firstRow: @Composable () -> Unit = {
            LegendItem("X", AxisX, latest?.first)
            LegendItem("Y", AxisY, latest?.second)
            LegendItem("Z", AxisZ, latest?.third)
        }
        if (maxWidth < 440.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(StudioSpacing.xs)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    firstRow()
                }
                if (endLabel.isNotBlank()) {
                    Text(endLabel, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(StudioSpacing.sm),
            ) {
                firstRow()
                Spacer(Modifier.weight(1f))
                if (endLabel.isNotBlank()) Text(endLabel, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun LegendItem(label: String, color: Color, value: Float? = null) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 14.dp)) {
        Box(Modifier.size(8.dp).background(color, StudioRadii.controlShape))
        Spacer(Modifier.width(5.dp))
        Text(
            if (value == null) label else "$label ${formatNumber(value)}",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = if (value == null) MaterialTheme.typography.bodySmall.fontFamily else StudioFonts.mono,
            maxLines = 1,
            softWrap = false,
        )
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
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val last = history.last()
        val summaries: List<@Composable (Modifier) -> Unit> = listOf(
            { modifier -> VectorSummary("Accelerometer", "RAW", last.accelX, last.accelY, last.accelZ, modifier) },
            { modifier -> VectorSummary("Gyroscope", "RAW", last.gyroX, last.gyroY, last.gyroZ, modifier) },
            { modifier -> VectorSummary("Gravity", "DERIVED", last.gravityX, last.gravityY, last.gravityZ, modifier) },
        )
        if (maxWidth < 760.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(StudioSpacing.sm)) {
                summaries.forEach { summary -> summary(Modifier.fillMaxWidth()) }
            }
        } else {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(StudioSpacing.md)) {
                summaries.forEach { summary -> summary(Modifier.weight(1f)) }
            }
        }
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
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val metrics: List<@Composable (Modifier) -> Unit> = listOf(
                { metricModifier -> PoseMetric("Flat tilt", "${"%.0f".format(pose.flatTiltDeg)} deg", metricModifier) },
                { metricModifier -> PoseMetric("Vertical tilt", "${"%.0f".format(pose.verticalTiltDeg)} deg", metricModifier) },
                { metricModifier -> PoseMetric("Dominant axis", pose.axisLabel, metricModifier) },
            )
            if (maxWidth < 420.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(StudioSpacing.sm)) {
                    metrics.forEach { metric -> metric(Modifier.fillMaxWidth()) }
                }
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(StudioSpacing.sm)) {
                    metrics.forEach { metric -> metric(Modifier.weight(1f)) }
                }
            }
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
        Text(value, fontFamily = StudioFonts.mono, fontWeight = FontWeight.SemiBold, fontSize = StudioTypeSize.compactMetric, maxLines = 1)
    }
}

@Composable
private fun EulerCards(imu: ImuData?) {
    val euler = imu.toEuler()
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val angles: List<@Composable (Modifier) -> Unit> = listOf(
            { modifier -> AngleCard("Pitch · X", euler.pitchDeg, AxisX, modifier) },
            { modifier -> AngleCard("Roll · Y", euler.rollDeg, AxisY, modifier) },
            { modifier -> AngleCard("Yaw · Z", euler.yawDeg, AxisZ, modifier) },
        )
        if (maxWidth < 460.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(StudioSpacing.sm)) {
                angles.forEach { angle -> angle(Modifier.fillMaxWidth()) }
            }
        } else {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(StudioSpacing.sm)) {
                angles.forEach { angle -> angle(Modifier.weight(1f)) }
            }
        }
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
        Text("${"%.1f".format(value)} deg", fontFamily = StudioFonts.mono, fontSize = 20.sp, fontWeight = FontWeight.Bold)
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
        Text(formatNumber(value), fontFamily = StudioFonts.mono, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun StatItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, fontFamily = StudioFonts.mono, fontWeight = FontWeight.SemiBold)
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
