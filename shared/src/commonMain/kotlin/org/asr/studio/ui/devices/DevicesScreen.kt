package org.asr.studio.ui.devices

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.asr.studio.protocol.ConnectionState

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

import org.asr.studio.protocol.ConnectMode
import org.asr.studio.ui.theme.StudioRadii
import org.asr.studio.ui.theme.StudioColors

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
                                if (activeConnectionMode == ConnectMode.WIFI) "Wi-Fi (ACTIVE)" else "Wi-Fi",
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        modifier = Modifier.weight(1f),
                        shape = StudioRadii.controlShape,
                    )
                    FilterChip(
                        selected = viewModel.mode == ConnectMode.ADB,
                        onClick = { viewModel.mode = ConnectMode.ADB; if (connectionState == ConnectionState.Disconnected) viewModel.host = "127.0.0.1" },
                        label = {
                            Text(
                                if (activeConnectionMode == ConnectMode.ADB) "USB / ADB (ACTIVE)" else "USB / ADB",
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        modifier = Modifier.weight(1f),
                        shape = StudioRadii.controlShape,
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
                    enabled = isInputEnabled,
                    shape = StudioRadii.controlShape,
                )

                OutlinedTextField(
                    value = viewModel.port,
                    onValueChange = { viewModel.port = it },
                    label = { Text("Port") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = isInputEnabled,
                    shape = StudioRadii.controlShape,
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
                            containerColor = StudioColors.panelRaised,
                            contentColor = StudioColors.textPrimary,
                            disabledContainerColor = StudioColors.panelRaised,
                            disabledContentColor = StudioColors.textMuted,
                        ),
                        shape = StudioRadii.controlShape,
                    ) {
                        Text("Auto-Configure USB / ADB (1-Click Setup)", maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                    }

                    viewModel.adbStatusMessage?.let { status ->
                        Text(
                            text = status,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = if (status.startsWith("Configured:")) StudioColors.accentText else MaterialTheme.colorScheme.error,
                        )
                    }
                }

                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val narrow = maxWidth < 520.dp
                    val connectAction: @Composable (Modifier) -> Unit = { buttonModifier ->
                        OutlinedButton(
                            onClick = {
                                val resolvedHost = viewModel.host.ifBlank {
                                    if (viewModel.mode == ConnectMode.ADB) "127.0.0.1" else ""
                                }
                                val resolvedPort = viewModel.port.toIntOrNull() ?: 4242
                                viewModel.connect(resolvedHost, resolvedPort)
                            },
                            modifier = buttonModifier,
                            enabled = isInputEnabled && (viewModel.mode == ConnectMode.ADB || viewModel.host.isNotBlank()),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = StudioColors.textSecondary,
                                disabledContentColor = StudioColors.textMuted,
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, StudioColors.borderStrong),
                            shape = StudioRadii.controlShape,
                        ) {
                            Text("Connect (Client)", maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                        }
                    }

                    val listenAction: @Composable (Modifier) -> Unit = { buttonModifier ->
                        Button(
                            onClick = {
                                val listenHost = if (viewModel.mode == ConnectMode.ADB) "127.0.0.1" else "0.0.0.0"
                                viewModel.listen(listenHost, viewModel.port.toIntOrNull() ?: 4242)
                            },
                            modifier = buttonModifier,
                            enabled = isInputEnabled,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StudioColors.accent,
                                contentColor = StudioColors.canvas,
                                disabledContainerColor = StudioColors.panelRaised,
                                disabledContentColor = StudioColors.textMuted,
                            ),
                            shape = StudioRadii.controlShape,
                        ) {
                            Text("Listen (Server)", maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                        }
                    }

                    if (narrow) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            connectAction(Modifier.fillMaxWidth())
                            listenAction(Modifier.fillMaxWidth())
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            connectAction(Modifier.weight(1f))
                            listenAction(Modifier.weight(1f))
                        }
                    }
                }

                if (connectionState == ConnectionState.Connected) {
                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val narrow = maxWidth < 520.dp
                        val runtimeActions: List<@Composable (Modifier) -> Unit> = listOf(
                            { buttonModifier ->
                                Button(
                                    onClick = { viewModel.startRuntime() },
                                    modifier = buttonModifier,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = StudioColors.accent,
                                        contentColor = StudioColors.canvas,
                                    ),
                                    shape = StudioRadii.controlShape,
                                ) {
                                    Text("Start ASR Runtime", maxLines = 1, softWrap = false)
                                }
                            },
                            { buttonModifier ->
                                OutlinedButton(
                                    onClick = { viewModel.stopRuntime() },
                                    modifier = buttonModifier,
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = StudioColors.textSecondary,
                                        disabledContentColor = StudioColors.textMuted,
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, StudioColors.borderStrong),
                                    shape = StudioRadii.controlShape,
                                ) {
                                    Text("Stop ASR Runtime", maxLines = 1, softWrap = false)
                                }
                            },
                        )
                        if (narrow) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                runtimeActions.forEach { action -> action(Modifier.fillMaxWidth()) }
                            }
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                runtimeActions.forEach { action -> action(Modifier.weight(1f)) }
                            }
                        }
                    }
                }

                OutlinedButton(
                    onClick = {
                        viewModel.disconnect()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = StudioColors.textSecondary,
                        disabledContentColor = StudioColors.textMuted,
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, StudioColors.borderStrong),
                    enabled = connectionState != ConnectionState.Disconnected,
                    shape = StudioRadii.controlShape,
                ) {
                    Text("Disconnect")
                }
            }
        }

        // ── Status pill ──────────────────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth().border(1.dp, StudioColors.border, StudioRadii.panelShape),
            colors = CardDefaults.cardColors(containerColor = StudioColors.panel),
            shape = StudioRadii.panelShape,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp).fillMaxWidth(),
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
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = when (connectionState) {
                        ConnectionState.Connected  -> StudioColors.accentText
                        ConnectionState.Connecting -> StudioColors.warning
                        else -> StudioColors.textPrimary
                        },
                    )
                }
                Box(
                    Modifier
                        .size(12.dp)
                        .background(
                            when (connectionState) {
                                ConnectionState.Connected -> StudioColors.accent
                                ConnectionState.Connecting -> StudioColors.warning
                                else -> StudioColors.textMuted
                            },
                            StudioRadii.controlShape,
                        )
                )
            }
        }
    }
}
