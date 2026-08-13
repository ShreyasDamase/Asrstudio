package org.asr.studio.ui.diagnostics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import org.asr.studio.currentTimeMillis
import org.asr.studio.data.GpsData

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
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            GpsCard(modifier = Modifier.weight(1.35f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Navigation Plot", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(10.dp))
                    StatusBadge(if (gps == null) "WAITING FOR FIX" else "LIVE GPS")
                }
                Spacer(Modifier.height(14.dp))
                GpsPlot(gps = gps, history = history, modifier = Modifier.fillMaxWidth().aspectRatio(1.65f))
            }

            GpsCard(modifier = Modifier.weight(1f)) {
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

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            GpsCard(modifier = Modifier.weight(1f)) {
                Text("Motion", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(14.dp))
                SpeedStrip(history, Modifier.fillMaxWidth().height(170.dp))
            }
            GpsCard(modifier = Modifier.weight(1f)) {
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
    }
}

@Composable
private fun GpsCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(modifier = Modifier.padding(18.dp), content = content)
    }
}

@Composable
private fun GpsPlot(gps: GpsData?, history: List<GpsData>, modifier: Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    Canvas(modifier = modifier.background(Color(0xFF0B0E11), RoundedCornerShape(8.dp))) {
        val pad = 26f
        repeat(9) { i ->
            val x = pad + (size.width - pad * 2f) * i / 8f
            val y = pad + (size.height - pad * 2f) * i / 8f
            drawLine(Color(0xFF1B2230), Offset(x, pad), Offset(x, size.height - pad), 1f)
            drawLine(Color(0xFF1B2230), Offset(pad, y), Offset(size.width - pad, y), 1f)
        }
        drawLine(Color(0x5532D6C6), Offset(size.width / 2f, pad), Offset(size.width / 2f, size.height - pad), 1.4f)
        drawLine(Color(0x5532D6C6), Offset(pad, size.height / 2f), Offset(size.width - pad, size.height / 2f), 1.4f)

        if (gps == null || history.isEmpty()) {
            drawCircle(Color(0x335EEAD4), 54f, Offset(size.width / 2f, size.height / 2f), style = Stroke(2f))
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
            drawPath(path, Color(0xFF60A5FA), style = Stroke(width = 3f, cap = StrokeCap.Round))
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
        drawLine(Color(0xFFFFD166), latest, arrowEnd, strokeWidth = 3f, cap = StrokeCap.Round)
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
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
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
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
            .padding(14.dp),
    ) {
        Text(label.uppercase(), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text(value, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
    }
}

@Composable
private fun BearingCompass(bearing: Float, modifier: Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    Canvas(modifier = modifier) {
        val radius = min(size.width, size.height) * 0.42f
        val center = Offset(size.width / 2f, size.height / 2f)
        drawCircle(Color(0xFF0B0E11), radius, center)
        drawCircle(Color(0xFF303746), radius, center, style = Stroke(2f))
        repeat(24) { i ->
            val angle = (i * 15f - 90f) * PI.toFloat() / 180f
            val inner = radius - if (i % 6 == 0) 16f else 8f
            drawLine(
                Color(0xFF4A5362),
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
        drawCircle(Color(0xFFFFD166), 5f, center)
    }
}

@Composable
private fun SpeedStrip(history: List<GpsData>, modifier: Modifier) {
    Canvas(modifier = modifier.background(Color(0xFF0B0E11), RoundedCornerShape(8.dp)).padding(10.dp)) {
        val pad = 18f
        val maxSpeed = max(1f, history.maxOfOrNull { it.speed } ?: 1f)
        repeat(4) { i ->
            val y = pad + (size.height - pad * 2f) * i / 3f
            drawLine(Color(0xFF222836), Offset(pad, y), Offset(size.width - pad, y), 1f)
        }
        if (history.size < 2) return@Canvas
        val path = Path()
        history.forEachIndexed { index, item ->
            val x = pad + (size.width - pad * 2f) * index / max(1, history.lastIndex)
            val y = size.height - pad - (item.speed / maxSpeed) * (size.height - pad * 2f)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, Color(0xFFFFD166), style = Stroke(width = 2.5f, cap = StrokeCap.Round))
    }
}

@Composable
private fun HealthRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ReadoutRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun StatusBadge(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.primary,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f), RoundedCornerShape(5.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f), RoundedCornerShape(5.dp))
            .padding(horizontal = 7.dp, vertical = 2.dp),
    )
}
