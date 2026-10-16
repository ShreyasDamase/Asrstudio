package org.asr.studio.ui.camera

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.asr.studio.data.CameraFrameData
import org.asr.studio.data.LightSensorData
import org.asr.studio.ui.theme.StudioColors
import org.asr.studio.ui.theme.StudioSpacing
import org.asr.studio.ui.theme.StudioTypeSize
import org.asr.studio.ui.theme.StudioFonts
import org.asr.studio.ui.theme.StudioRadii
import org.asr.studio.ui.components.StudioToggle

@Composable
fun CameraScreen(viewModel: CameraViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val lightSensor by viewModel.lightSensor.collectAsState()
    var flashlightOn by remember { mutableStateOf(false) }
    var brightness by remember { mutableStateOf(1.0f) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val stackLightingPanel = maxWidth < 620.dp || maxHeight < 520.dp
        val cameraFeed: @Composable (Modifier) -> Unit = { feedModifier ->
            Box(modifier = feedModifier, contentAlignment = Alignment.Center) {
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
            }
        }
        val lightingPanel: @Composable (Modifier) -> Unit = { panelModifier ->
            LightingPanel(
                modifier = panelModifier,
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
        if (stackLightingPanel) {
            Column(modifier = Modifier.fillMaxSize()) {
                cameraFeed(Modifier.weight(1f).fillMaxWidth())
                lightingPanel(Modifier.fillMaxWidth().padding(12.dp))
            }
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                cameraFeed(Modifier.fillMaxSize())
                lightingPanel(Modifier.align(Alignment.BottomEnd).padding(16.dp))
            }
        }
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
        lux == null -> "No light data"
        lux < 10f -> "Very low light"
        lux < 50f -> "Low light"
        lux < 200f -> "Dim"
        lux < 1000f -> "Indoor"
        lux < 10000f -> "Bright"
        else -> "Direct sunlight"
    }
    val showWarning = lux != null && lux < 50f && !flashlightOn

    Column(
        modifier = modifier
            .widthIn(max = 320.dp)
            .background(StudioColors.panel.copy(alpha = 0.97f), StudioRadii.panelShape)
            .border(1.dp, StudioColors.borderStrong, StudioRadii.panelShape)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "Lighting",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = StudioColors.textPrimary,
            )
            if (showWarning) {
                Text(
                    "Low light",
                    style = MaterialTheme.typography.labelSmall,
                    color = StudioColors.warning,
                )
            }
        }

        StudioToggle(
            checked = flashlightOn,
            onCheckedChange = { onToggleFlashlight() },
            label = "Flashlight",
            supportingText = "Phone camera light",
            modifier = Modifier.padding(horizontal = StudioSpacing.xs),
        )

        HorizontalDivider(color = StudioColors.border.copy(alpha = 0.8f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "Ambient light",
                style = MaterialTheme.typography.bodySmall,
                color = StudioColors.textSecondary,
            )
            Text(
                text = if (lux != null) "${"%.0f".format(lux)} lux" else "No data",
                style = MaterialTheme.typography.bodySmall,
                color = StudioColors.textPrimary,
                fontFamily = StudioFonts.mono,
            )
        }
        if (lux != null) {
            Text(
                luxLabel,
                style = MaterialTheme.typography.labelSmall,
                color = StudioColors.textMuted,
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
                modifier = Modifier.fillMaxSize().background(StudioColors.canvas),
                contentAlignment = Alignment.Center
            ) {
                Text("Decoding error", color = StudioColors.error)
            }
        }

        // Metadata Overlay
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .background(StudioColors.canvas.copy(alpha = 0.6f))
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
        color = StudioColors.textPrimary,
        fontSize = StudioTypeSize.supporting,
        fontFamily = StudioFonts.mono
    )
}
