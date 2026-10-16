package org.asr.studio.ui.diagnostics

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.PI
import kotlin.math.sin
import org.asr.studio.currentTimeMillis
import org.asr.studio.data.GpsData
import org.asr.studio.ui.theme.StudioColors
import org.asr.studio.ui.theme.StudioTypeSize
import org.asr.studio.ui.theme.StudioFonts

@Composable
fun GpsScreen(viewModel: DiagnosticsViewModel) {
    val gps by viewModel.gps.collectAsState()
    val history = remember { mutableStateListOf<GpsData>() }
    var lastGpsReceivedAtMs by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(gps?.timestampNs) {
        val sample = gps ?: return@LaunchedEffect
        lastGpsReceivedAtMs = currentTimeMillis()
        history.add(sample)
        if (history.size > 180) history.removeRange(0, history.size - 180)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val compact = maxWidth < 820.dp
            if (compact) {
                Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    GpsPlotPanel(gps, history)
                    GpsFixDetailsPanel(gps)
                    GpsMotionPanel(history)
                    GpsReceiverPanel(gps, history, lastGpsReceivedAtMs)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                    ) {
                        GpsPlotPanel(gps, history, Modifier.weight(1.35f))
                        GpsFixDetailsPanel(gps, Modifier.weight(1f))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                    ) {
                        GpsMotionPanel(history, Modifier.weight(1f))
                        GpsReceiverPanel(gps, history, lastGpsReceivedAtMs, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun GpsPlotPanel(gps: GpsData?, history: List<GpsData>, modifier: Modifier = Modifier) {
    GpsCard(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Navigation Plot", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(10.dp))
            StatusBadge(if (gps == null) "WAITING FOR FIX" else "LIVE GPS")
        }
        Spacer(Modifier.height(14.dp))
        GpsPlot(gps, history, Modifier.fillMaxWidth().aspectRatio(1.65f))
    }
}

@Composable
private fun GpsFixDetailsPanel(gps: GpsData?, modifier: Modifier = Modifier) {
    GpsCard(modifier) {
        Text("Fix Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        CoordinateReadout(gps)
        Spacer(Modifier.height(18.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricTile("Accuracy", gps?.let { "${"%.1f".format(it.accuracy)} m" } ?: "--", Modifier.weight(1f))
            MetricTile("Altitude", gps?.let { "${"%.1f".format(it.alt)} m" } ?: "--", Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricTile("Speed", gps?.let { "${"%.2f".format(it.speed)} m/s" } ?: "--", Modifier.weight(1f))
            MetricTile("Bearing", gps?.let { "${"%.1f".format(it.bearing)} deg" } ?: "--", Modifier.weight(1f))
        }
        Spacer(Modifier.height(18.dp))
        BearingCompass(gps?.bearing ?: 0f, modifier = Modifier.fillMaxWidth().height(180.dp))
    }
}

@Composable
private fun GpsMotionPanel(history: List<GpsData>, modifier: Modifier = Modifier) {
    GpsCard(modifier) {
        Text("Motion", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(14.dp))
        SpeedStrip(history, Modifier.fillMaxWidth().height(170.dp))
    }
}

@Composable
private fun GpsReceiverPanel(
    gps: GpsData?,
    history: List<GpsData>,
    lastGpsReceivedAtMs: Long?,
    modifier: Modifier = Modifier,
) {
    GpsCard(modifier) {
        Text("Receiver Health", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(14.dp))
        val ageSeconds = lastGpsReceivedAtMs?.let { max(0L, (currentTimeMillis() - it) / 1000L) }
        HealthRow("Fix status", if (gps == null) "No fix yet" else "Location packets received")
        HealthRow("Last update", ageSeconds?.let { "$it s ago" } ?: "Never")
        HealthRow("Track samples", history.size.toString())
        HealthRow("Map mode", "API-free local plot")
        Spacer(Modifier.height(12.dp))
        Text(
            "This view uses ASR's GPS packets directly. It does not need Google Maps credentials; add a map tile provider later only if road or satellite context is important.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun GpsCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(modifier = Modifier.fillMaxHeight().padding(18.dp), content = content)
    }
}

@Composable
private fun GpsPlot(gps: GpsData?, history: List<GpsData>, modifier: Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    val gridColor = StudioColors.border
    Canvas(modifier = modifier.background(StudioColors.canvas, MaterialTheme.shapes.medium)) {
        val pad = 26f
        repeat(9) { i ->
            val x = pad + (size.width - pad * 2f) * i / 8f
            val y = pad + (size.height - pad * 2f) * i / 8f
            drawLine(gridColor, Offset(x, pad), Offset(x, size.height - pad), 1f)
            drawLine(gridColor, Offset(pad, y), Offset(size.width - pad, y), 1f)
        }
        drawLine(StudioColors.plotCrosshair, Offset(size.width / 2f, pad), Offset(size.width / 2f, size.height - pad), 1.4f)
        drawLine(StudioColors.plotCrosshair, Offset(pad, size.height / 2f), Offset(size.width - pad, size.height / 2f), 1.4f)

        if (gps == null || history.isEmpty()) {
            drawCircle(StudioColors.plotHalo, 54f, Offset(size.width / 2f, size.height / 2f), style = Stroke(2f))
            drawCircle(primary, 6f, Offset(size.width / 2f, size.height / 2f))
            return@Canvas
        }

        val origin = history.first()
        val points = history.map { projectMeters(origin, it, size.width, size.height, pad) }
        if (points.size > 1) {
            val path = Path().apply {
                points.forEachIndexed { index, point ->
                    if (index == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
                }
            }
            drawPath(path, StudioColors.axisZ, style = Stroke(width = 3f, cap = StrokeCap.Round))
        }
        points.forEachIndexed { index, point ->
            val alpha = 0.2f + 0.8f * index / max(1, points.lastIndex)
            drawCircle(primary.copy(alpha = alpha), if (index == points.lastIndex) 7f else 3f, point)
        }
        val latest = points.last()
        val accuracyPx = max(10f, min(size.minDimension * 0.28f, (gps.accuracy / 30f) * size.minDimension * 0.18f))
        drawCircle(primary.copy(alpha = 0.13f), accuracyPx, latest)
        drawCircle(primary.copy(alpha = 0.55f), accuracyPx, latest, style = Stroke(1.5f))
        val bearing = (gps.bearing - 90f) * PI.toFloat() / 180f
        val arrowEnd = Offset(latest.x + cos(bearing) * 42f, latest.y + sin(bearing) * 42f)
        drawLine(StudioColors.warning, latest, arrowEnd, strokeWidth = 3f, cap = StrokeCap.Round)
    }
}

private fun projectMeters(origin: GpsData, point: GpsData, width: Float, height: Float, pad: Float): Offset {
    val metersPerLat = 111_320.0
    val metersPerLon = 111_320.0 * cos(origin.lat * PI / 180.0)
    val dx = (point.lon - origin.lon) * metersPerLon
    val dy = (point.lat - origin.lat) * metersPerLat
    val scale = 3.0
    val x = (width / 2.0 + dx * scale).toFloat().coerceIn(pad, width - pad)
    val y = (height / 2.0 - dy * scale).toFloat().coerceIn(pad, height - pad)
    return Offset(x, y)
}

@Composable
private fun CoordinateReadout(gps: GpsData?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.55f), MaterialTheme.shapes.medium)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), MaterialTheme.shapes.medium)
            .padding(14.dp),
    ) {
        ReadoutRow("Latitude", gps?.let { "%.7f".format(it.lat) } ?: "--")
        ReadoutRow("Longitude", gps?.let { "%.7f".format(it.lon) } ?: "--")
    }
}

@Composable
private fun MetricTile(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.55f), MaterialTheme.shapes.medium)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), MaterialTheme.shapes.medium)
            .padding(14.dp),
    ) {
        Text(label.uppercase(), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = StudioTypeSize.caption, fontWeight = FontWeight.Bold)
        Text(value, fontFamily = StudioFonts.mono, fontWeight = FontWeight.SemiBold, fontSize = StudioTypeSize.detailMetric)
    }
}

@Composable
private fun BearingCompass(bearing: Float, modifier: Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    val canvasColor = StudioColors.canvas
    val borderColor = StudioColors.borderStrong
    Canvas(modifier = modifier) {
        val radius = min(size.width, size.height) * 0.42f
        val center = Offset(size.width / 2f, size.height / 2f)
        drawCircle(canvasColor, radius, center)
        drawCircle(borderColor, radius, center, style = Stroke(2f))
        repeat(24) { i ->
            val angle = (i * 15f - 90f) * PI.toFloat() / 180f
            val inner = radius - if (i % 6 == 0) 16f else 8f
            drawLine(
                StudioColors.plotMarker,
                Offset(center.x + cos(angle) * inner, center.y + sin(angle) * inner),
                Offset(center.x + cos(angle) * radius, center.y + sin(angle) * radius),
                2f,
            )
        }
        val angle = (bearing - 90f) * PI.toFloat() / 180f
        val tip = Offset(center.x + cos(angle) * (radius - 18f), center.y + sin(angle) * (radius - 18f))
        val left = Offset(center.x + cos(angle + 2.55f) * 24f, center.y + sin(angle + 2.55f) * 24f)
        val right = Offset(center.x + cos(angle - 2.55f) * 24f, center.y + sin(angle - 2.55f) * 24f)
        drawPath(Path().apply { moveTo(tip.x, tip.y); lineTo(left.x, left.y); lineTo(center.x, center.y); lineTo(right.x, right.y); close() }, primary)
        drawCircle(StudioColors.warning, 5f, center)
    }
}

@Composable
private fun SpeedStrip(history: List<GpsData>, modifier: Modifier) {
    val gridColor = StudioColors.border
    Canvas(modifier = modifier.background(StudioColors.canvas, MaterialTheme.shapes.medium).padding(10.dp)) {
        val pad = 18f
        val maxSpeed = max(1f, history.maxOfOrNull { it.speed } ?: 1f)
        repeat(4) { i ->
            val y = pad + (size.height - pad * 2f) * i / 3f
            drawLine(gridColor, Offset(pad, y), Offset(size.width - pad, y), 1f)
        }
        if (history.size < 2) return@Canvas
        val path = Path()
        history.forEachIndexed { index, item ->
            val x = pad + (size.width - pad * 2f) * index / max(1, history.lastIndex)
            val y = size.height - pad - (item.speed / maxSpeed) * (size.height - pad * 2f)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, StudioColors.warning, style = Stroke(width = 2.5f, cap = StrokeCap.Round))
    }
}

@Composable
private fun HealthRow(label: String, value: String) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        if (maxWidth < 380.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, fontWeight = FontWeight.SemiBold)
            }
        } else {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun ReadoutRow(label: String, value: String) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        if (maxWidth < 380.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, fontFamily = StudioFonts.mono, fontWeight = FontWeight.SemiBold)
            }
        } else {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, fontFamily = StudioFonts.mono, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun StatusBadge(text: String) {
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
