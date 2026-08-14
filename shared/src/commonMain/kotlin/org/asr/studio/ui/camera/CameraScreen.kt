package org.asr.studio.ui.camera

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.asr.studio.data.CameraFrameData
import org.asr.studio.data.LightSensorData

@Composable
fun CameraScreen(viewModel: CameraViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val lightSensor by viewModel.lightSensor.collectAsState()
    var flashlightOn by remember { mutableStateOf(false) }
    var brightness by remember { mutableStateOf(1.0f) }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when (val state = uiState) {
            is CameraUiState.WaitingForRuntime -> {
                Text("Waiting for Runtime...", style = MaterialTheme.typography.bodyLarge)
            }
            is CameraUiState.WaitingForPackets -> {
                Text("Connected: Waiting for camera packets...", style = MaterialTheme.typography.bodyLarge)
            }
            is CameraUiState.Streaming -> {
                CameraView(state.frame)
            }
        }

        // Lighting Panel — always visible (even while waiting, so user can pre-arm flashlight)
        LightingPanel(
            modifier = Modifier.align(Alignment.BottomStart).padding(12.dp),
            lightSensor = lightSensor,
            flashlightOn = flashlightOn,
            brightness = brightness,
            onToggleFlashlight = {
                flashlightOn = !flashlightOn
                viewModel.toggleFlashlight()
            },
            onBrightnessChange = { level ->
                brightness = level
                viewModel.setFlashlightBrightness(level)
            }
        )
    }
}

@Composable
private fun LightingPanel(
    modifier: Modifier = Modifier,
    lightSensor: LightSensorData?,
    flashlightOn: Boolean,
    brightness: Float,
    onToggleFlashlight: () -> Unit,
    onBrightnessChange: (Float) -> Unit,
) {
    val lux = lightSensor?.lux
    val luxLabel = when {
        lux == null -> "No data"
        lux < 10f -> "🌑 Very Dark"
        lux < 50f -> "🌒 Dark"
        lux < 200f -> "🌓 Dim"
        lux < 1000f -> "💡 Indoor"
        lux < 10000f -> "☀️ Daylight"
        else -> "🌞 Direct Sunlight"
    }
    val showWarning = lux != null && lux < 50f && !flashlightOn

    Column(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.75f))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text("LIGHTING", fontSize = 10.sp, fontWeight = FontWeight.Bold,
             color = Color.White.copy(alpha = 0.6f), fontFamily = FontFamily.Monospace)

        // Flashlight Toggle
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("💡 Flashlight", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            Switch(
                checked = flashlightOn,
                onCheckedChange = { onToggleFlashlight() },
                modifier = Modifier.height(24.dp),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFFFFD166),
                    checkedTrackColor = Color(0xFFFFD166).copy(alpha = 0.4f)
                )
            )
        }

        // Ambient Light
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Ambient:", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
            Text(
                text = if (lux != null) "${"%.0f".format(lux)} lux  $luxLabel" else luxLabel,
                fontSize = 11.sp, color = Color.White, fontFamily = FontFamily.Monospace
            )
        }

        // Warning banner
        if (showWarning) {
            Text(
                "⚠️ Low light — enable flashlight",
                fontSize = 11.sp,
                color = Color(0xFFFFD166),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun CameraView(frame: CameraFrameData) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (frame.image != null) {
            Image(
                bitmap = frame.image,
                contentDescription = "Live Camera",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Text("Decoding error", color = Color.Red)
            }
        }

        // Metadata Overlay
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(8.dp)
        ) {
            MetadataText("Resolution", "${frame.width}x${frame.height}")
            MetadataText("FPS", "%.1f".format(frame.fps))
            MetadataText("Latency", "${frame.latencyMs}ms")
            MetadataText("Frame ID", frame.frameId.toString())
            MetadataText("ISO", frame.iso.toString())
            MetadataText("Exposure", "${frame.exposureNs / 1_000_000}ms")
        }
    }
}

@Composable
private fun MetadataText(label: String, value: String) {
    Text(
        text = "$label: $value",
        color = Color.White,
        fontSize = 12.sp,
        fontFamily = FontFamily.Monospace
    )
}
