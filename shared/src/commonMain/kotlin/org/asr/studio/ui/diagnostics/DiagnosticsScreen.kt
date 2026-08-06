package org.asr.studio.ui.diagnostics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.asr.studio.data.Diagnostics
import org.asr.studio.data.ImuData

@Composable
fun DiagnosticsScreen(viewModel: DiagnosticsViewModel) {
    val diag by viewModel.diagnostics.collectAsState()
    val imu by viewModel.imu.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        StatsSection(diag)
        ImuSection(imu)
    }
}

@Composable
private fun StatsSection(diag: Diagnostics) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Connection Statistics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth()) {
                StatItem("Packet Rate", "${"%.1f".format(diag.packetRate)} pps", Modifier.weight(1f))
                StatItem("Byte Rate", "${"%.1f".format(diag.byteRate / 1024.0)} kbps", Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                StatItem("Total Drops", diag.dropCount.toString(), Modifier.weight(1f))
                StatItem("Heartbeat", if (diag.lastHeartbeatTimeMs > 0) "${(org.asr.studio.currentTimeMillis() - diag.lastHeartbeatTimeMs) / 1000}s ago" else "Never", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ImuSection(imu: ImuData?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "IMU Raw Values",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (imu == null) {
                Text(
                    text = "No IMU data received yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Accelerometer", style = MaterialTheme.typography.labelLarge)
                        Text("X: ${"%.3f".format(imu.accelX)}")
                        Text("Y: ${"%.3f".format(imu.accelY)}")
                        Text("Z: ${"%.3f".format(imu.accelZ)}")
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Gyroscope", style = MaterialTheme.typography.labelLarge)
                        Text("X: ${"%.3f".format(imu.gyroX)}")
                        Text("Y: ${"%.3f".format(imu.gyroY)}")
                        Text("Z: ${"%.3f".format(imu.gyroZ)}")
                    }
                }
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}
