package org.asr.studio.ui.devices

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.asr.studio.protocol.ConnectionState

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

private enum class ConnectMode { WIFI, ADB }

@Composable
fun DevicesScreen(viewModel: DevicesViewModel) {
    val connectionState by viewModel.connectionState.collectAsState()
    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("4242") }
    var mode by remember { mutableStateOf(ConnectMode.WIFI) }

    // Tracks which mode was used to establish the current connection
    var activeConnectionMode by remember { mutableStateOf<ConnectMode?>(null) }

    LaunchedEffect(connectionState) {
        if (connectionState != ConnectionState.Disconnected && activeConnectionMode == null) {
            activeConnectionMode = mode
        } else if (connectionState == ConnectionState.Disconnected) {
            activeConnectionMode = null
        }
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // ── Mode selector ────────────────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Connection Mode",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = mode == ConnectMode.WIFI,
                        onClick = { mode = ConnectMode.WIFI; if (connectionState == ConnectionState.Disconnected) host = "" },
                        label = {
                            Text(
                                if (activeConnectionMode == ConnectMode.WIFI) "📡  Wi-Fi (ACTIVE)" else "📡  Wi-Fi"
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = mode == ConnectMode.ADB,
                        onClick = { mode = ConnectMode.ADB; if (connectionState == ConnectionState.Disconnected) host = "127.0.0.1" },
                        label = {
                            Text(
                                if (activeConnectionMode == ConnectMode.ADB) "🔌  USB / ADB (ACTIVE)" else "🔌  USB / ADB"
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                Text(
                    text = when (mode) {
                        ConnectMode.WIFI ->
                            "Enter the desktop's local IP (e.g. 192.168.1.42). " +
                            "Both devices must be on the same Wi-Fi network. " +
                            "Click Listen (Server) below."
                        ConnectMode.ADB ->
                            "Run on desktop:  adb forward tcp:4242 tcp:4242\n" +
                            "Then enter 127.0.0.1 as host and click Listen (Server)."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // ── Mode Locked Warning (if tab doesn't match active connection) ──────
        if (activeConnectionMode != null && mode != activeConnectionMode) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🔒 Currently connected via ${if (activeConnectionMode == ConnectMode.WIFI) "Wi-Fi" else "USB / ADB"}. Click 'Disconnect' below to switch modes.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        // ── Connection configuration ─────────────────────────────────────────
        val isCurrentModeLocked = activeConnectionMode != null && mode != activeConnectionMode
        val isInputEnabled = connectionState == ConnectionState.Disconnected && !isCurrentModeLocked

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Connection Configuration",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it },
                    label = {
                        Text(
                            when (mode) {
                                ConnectMode.WIFI -> "Desktop IP (e.g. 192.168.1.42)"
                                ConnectMode.ADB  -> "Host (127.0.0.1 for ADB)"
                            }
                        )
                    },
                    placeholder = {
                        Text(if (mode == ConnectMode.ADB) "127.0.0.1" else "192.168.1.42")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = isInputEnabled
                )

                OutlinedTextField(
                    value = port,
                    onValueChange = { port = it },
                    label = { Text("Port") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = isInputEnabled
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val resolvedHost = host.ifBlank {
                                if (mode == ConnectMode.ADB) "127.0.0.1" else ""
                            }
                            val resolvedPort = port.toIntOrNull() ?: 4242
                            viewModel.connect(resolvedHost, resolvedPort)
                        },
                        modifier = Modifier.weight(1f),
                        enabled = isInputEnabled && (mode == ConnectMode.ADB || host.isNotBlank())
                    ) {
                        Text("Connect (Client)")
                    }

                    Button(
                        onClick = { viewModel.listen(port.toIntOrNull() ?: 4242) },
                        modifier = Modifier.weight(1f),
                        enabled = isInputEnabled,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Listen (Server) ★")
                    }
                }

                if (connectionState == ConnectionState.Connected) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { viewModel.startRuntime() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiary
                            )
                        ) {
                            Text("Start ASR Runtime")
                        }

                        Button(
                            onClick = { viewModel.stopRuntime() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Stop ASR Runtime")
                        }
                    }
                }

                Button(
                    onClick = {
                        viewModel.disconnect()
                        activeConnectionMode = null
                    },
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

        // ── Status pill ──────────────────────────────────────────────────────
        Card(modifier = Modifier.fillMaxWidth()) {
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
                        text = when (connectionState) {
                            ConnectionState.Connected -> "Connected (${if (activeConnectionMode == ConnectMode.ADB) "USB / ADB" else "Wi-Fi"})"
                            ConnectionState.Connecting -> "Connecting..."
                            else -> "Disconnected"
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = when (connectionState) {
                            ConnectionState.Connected  -> MaterialTheme.colorScheme.primary
                            ConnectionState.Connecting -> MaterialTheme.colorScheme.secondary
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
                Text(
                    text = when (connectionState) {
                        ConnectionState.Connected  -> "●"
                        ConnectionState.Connecting -> "◌"
                        else -> "○"
                    },
                    style = MaterialTheme.typography.headlineMedium,
                    color = when (connectionState) {
                        ConnectionState.Connected  -> MaterialTheme.colorScheme.primary
                        ConnectionState.Connecting -> MaterialTheme.colorScheme.secondary
                        else -> MaterialTheme.colorScheme.outline
                    }
                )
            }
        }
    }
}
