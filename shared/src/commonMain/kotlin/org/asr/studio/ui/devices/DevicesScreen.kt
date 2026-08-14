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

import org.asr.studio.protocol.ConnectMode

@Composable
fun DevicesScreen(viewModel: DevicesViewModel) {
    val connectionState by viewModel.connectionState.collectAsState()
    val activeConnectionMode by viewModel.activeMode.collectAsState()

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
                        selected = viewModel.mode == ConnectMode.WIFI,
                        onClick = { viewModel.mode = ConnectMode.WIFI; if (connectionState == ConnectionState.Disconnected) viewModel.host = "" },
                        label = {
                            Text(
                                if (activeConnectionMode == ConnectMode.WIFI) "Wi-Fi (ACTIVE)" else "Wi-Fi"
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = viewModel.mode == ConnectMode.ADB,
                        onClick = { viewModel.mode = ConnectMode.ADB; if (connectionState == ConnectionState.Disconnected) viewModel.host = "127.0.0.1" },
                        label = {
                            Text(
                                if (activeConnectionMode == ConnectMode.ADB) "USB / ADB (ACTIVE)" else "USB / ADB"
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                Text(
                    text = when (viewModel.mode) {
                        ConnectMode.WIFI ->
                            "Enter the desktop's local IP (e.g. 192.168.1.42). " +
                            "Both devices must be on the same Wi-Fi network. " +
                            "Click Listen (Server) below."
                        ConnectMode.ADB ->
                            "Run on desktop terminal:  adb reverse tcp:4242 tcp:4242\n" +
                            "Click Listen (Server) below. On Android, tap CONNECT (host 127.0.0.1)."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // ── Mode Locked Warning (if tab doesn't match active connection) ──────
        if (activeConnectionMode != null && viewModel.mode != activeConnectionMode) {
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
                        text = "Currently connected via ${if (activeConnectionMode == ConnectMode.WIFI) "Wi-Fi" else "USB / ADB"}. Click 'Disconnect' below to switch modes.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        // ── Connection configuration ─────────────────────────────────────────
        val isCurrentModeLocked = activeConnectionMode != null && viewModel.mode != activeConnectionMode
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
                    value = viewModel.host,
                    onValueChange = { viewModel.host = it },
                    label = {
                        Text(
                            when (viewModel.mode) {
                                ConnectMode.WIFI -> "Desktop IP (e.g. 192.168.1.42)"
                                ConnectMode.ADB  -> "Host (127.0.0.1 for ADB)"
                            }
                        )
                    },
                    placeholder = {
                        Text(if (viewModel.mode == ConnectMode.ADB) "127.0.0.1" else "192.168.1.42")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = isInputEnabled
                )

                OutlinedTextField(
                    value = viewModel.port,
                    onValueChange = { viewModel.port = it },
                    label = { Text("Port") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = isInputEnabled
                )

                if (viewModel.mode == ConnectMode.ADB) {
                    Button(
                        onClick = {
                            val p = viewModel.port.toIntOrNull() ?: 4242
                            viewModel.autoSetupAdb(p)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = isInputEnabled,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Text("Auto-Configure USB / ADB (1-Click Setup)")
                    }

                    viewModel.adbStatusMessage?.let { status ->
                        Text(
                            text = status,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = if (status.startsWith("✅")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val resolvedHost = viewModel.host.ifBlank {
                                if (viewModel.mode == ConnectMode.ADB) "127.0.0.1" else ""
                            }
                            val resolvedPort = viewModel.port.toIntOrNull() ?: 4242
                            viewModel.connect(resolvedHost, resolvedPort)
                        },
                        modifier = Modifier.weight(1f),
                        enabled = isInputEnabled && (viewModel.mode == ConnectMode.ADB || viewModel.host.isNotBlank())
                    ) {
                        Text("Connect (Client)")
                    }

                    Button(
                        onClick = {
                            val listenHost = if (viewModel.mode == ConnectMode.ADB) "127.0.0.1" else "0.0.0.0"
                            viewModel.listen(listenHost, viewModel.port.toIntOrNull() ?: 4242)
                        },
                        modifier = Modifier.weight(1f),
                        enabled = isInputEnabled,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Listen (Server)")
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
