package org.asr.studio.ui.devices

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.asr.studio.protocol.ConnectionState

@Composable
fun DevicesScreen(viewModel: DevicesViewModel) {
    val connectionState by viewModel.connectionState.collectAsState()
    var host by remember { mutableStateOf("127.0.0.1") }
    var port by remember { mutableStateOf("9000") }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Connection Configuration",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it },
                    label = { Text("Host") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = connectionState == ConnectionState.Disconnected
                )

                OutlinedTextField(
                    value = port,
                    onValueChange = { port = it },
                    label = { Text("Port") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = connectionState == ConnectionState.Disconnected
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { viewModel.connect(host, port.toIntOrNull() ?: 9000) },
                        modifier = Modifier.weight(1f),
                        enabled = connectionState == ConnectionState.Disconnected
                    ) {
                        Text("Connect (Client)")
                    }

                    Button(
                        onClick = { viewModel.listen(port.toIntOrNull() ?: 9000) },
                        modifier = Modifier.weight(1f),
                        enabled = connectionState == ConnectionState.Disconnected
                    ) {
                        Text("Listen (Server)")
                    }
                }

                Button(
                    onClick = { viewModel.disconnect() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    enabled = connectionState != ConnectionState.Disconnected
                ) {
                    Text("Disconnect")
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Connection State",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = connectionState.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = when (connectionState) {
                            ConnectionState.Connected -> MaterialTheme.colorScheme.primary
                            ConnectionState.Connecting   -> MaterialTheme.colorScheme.secondary
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
            }
        }
    }
}
