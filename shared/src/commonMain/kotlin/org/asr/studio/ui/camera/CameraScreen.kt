package org.asr.studio.ui.camera

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.asr.studio.data.CameraFrameData

@Composable
fun CameraScreen(viewModel: CameraViewModel) {
    val uiState by viewModel.uiState.collectAsState()

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
