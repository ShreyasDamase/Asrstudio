package org.asr.studio

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import org.asr.studio.data.StudioService
import org.asr.studio.ui.devices.DevicesScreen
import org.asr.studio.ui.devices.DevicesViewModel
import org.asr.studio.ui.diagnostics.DiagnosticsScreen
import org.asr.studio.ui.diagnostics.DiagnosticsViewModel
import org.asr.studio.ui.inspector.PacketInspectorScreen
import org.asr.studio.ui.inspector.PacketInspectorViewModel

private enum class AppScreen(
    val label: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
) {
    Dashboard(
        label = "Dashboard",
        title = "Dashboard",
        description = "Runtime overview placeholder. Live status and metrics are planned for a later phase.",
        icon = Icons.Default.Dashboard,
    ),
    Devices(
        label = "Devices",
        title = "Device Connection",
        description = "Connection controls are intentionally not implemented during Phase 1.",
        icon = Icons.Default.Router,
    ),
    Camera(
        label = "Camera",
        title = "Camera Diagnostics",
        description = "Camera stream diagnostics will be added after protocol compatibility is verified.",
        icon = Icons.Default.PhotoCamera,
    ),
    Imu(
        label = "IMU",
        title = "IMU Diagnostics",
        description = "Sensor graphs and packet views are placeholders until the diagnostics phase.",
        icon = Icons.Default.Memory,
    ),
    Gps(
        label = "GPS",
        title = "GPS Diagnostics",
        description = "Location and fix-state visualization will be implemented in a later phase.",
        icon = Icons.Default.Place,
    ),
    Protocol(
        label = "Protocol",
        title = "Protocol Inspector",
        description = "Packet parsing waits for inspection of the Android reference implementation.",
        icon = Icons.AutoMirrored.Filled.Article,
    ),
    Logs(
        label = "Logs",
        title = "Logs",
        description = "Structured runtime logs are not wired during Phase 1.",
        icon = Icons.Default.Terminal,
    ),
    Docs(
        label = "Docs",
        title = "Documentation",
        description = "Embedded documentation will be added after the core shell is complete.",
        icon = Icons.Default.Description,
    ),
    Settings(
        label = "Settings",
        title = "Settings",
        description = "Preferences and persistence are out of scope for Phase 1.",
        icon = Icons.Default.Settings,
    ),
}

@Composable
@Preview
fun App() {
    val studioService = StudioService.instance
    val devicesViewModel = remember { DevicesViewModel(studioService) }
    val diagnosticsViewModel = remember { DiagnosticsViewModel(studioService) }
    val inspectorViewModel = remember { PacketInspectorViewModel(studioService) }

    AsrStudioTheme {
        var selectedScreen by remember { mutableStateOf(AppScreen.Dashboard) }

        Surface(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxSize()) {
                Sidebar(
                    selectedScreen = selectedScreen,
                    onScreenSelected = { selectedScreen = it },
                )
                VerticalDivider(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
                Column(modifier = Modifier.fillMaxSize()) {
                    TitleBar(
                        screen = selectedScreen,
                        studioService = studioService
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    ScreenContent(
                        screen = selectedScreen,
                        devicesViewModel = devicesViewModel,
                        diagnosticsViewModel = diagnosticsViewModel,
                        inspectorViewModel = inspectorViewModel,
                        modifier = Modifier.weight(1f),
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    StatusBar(studioService = studioService)
                }
            }
        }
    }
}

@Composable
private fun AsrStudioTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF80CBC4),
            secondary = Color(0xFFFFD166),
            tertiary = Color(0xFFFF8A80),
            surface = Color(0xFF111418),
            surfaceVariant = Color(0xFF1A2027),
            background = Color(0xFF0B0E11),
        ),
        content = content,
    )
}

@Composable
private fun Sidebar(
    selectedScreen: AppScreen,
    onScreenSelected: (AppScreen) -> Unit,
) {
    Column(
        modifier = Modifier
            .width(232.dp)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp),
    ) {
        Text(
            text = "ASR Studio",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "Desktop diagnostics",
            modifier = Modifier.padding(top = 4.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(24.dp))

        AppScreen.entries.forEach { screen ->
            NavigationItem(
                screen = screen,
                selected = screen == selectedScreen,
                onClick = { onScreenSelected(screen) },
            )
        }
    }
}

@Composable
private fun NavigationItem(
    screen: AppScreen,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val background =
        if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else Color.Transparent
    val contentColor =
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(background, MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = screen.icon,
            contentDescription = screen.label,
            modifier = Modifier.size(20.dp),
            tint = contentColor,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = screen.label,
            color = contentColor,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

@Composable
private fun TitleBar(
    screen: AppScreen,
    studioService: StudioService
) {
    val connectionState by studioService.tcpClient.connectionState.collectAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(
                text = screen.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Phase 2 diagnostics",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = connectionState.name,
            style = MaterialTheme.typography.labelLarge,
            color = if (connectionState == org.asr.studio.protocol.ConnectionState.Connected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
        )
    }
}

@Composable
private fun ScreenContent(
    screen: AppScreen,
    devicesViewModel: DevicesViewModel,
    diagnosticsViewModel: DiagnosticsViewModel,
    inspectorViewModel: PacketInspectorViewModel,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        when (screen) {
            AppScreen.Devices -> DevicesScreen(devicesViewModel)
            AppScreen.Imu -> DiagnosticsScreen(diagnosticsViewModel)
            AppScreen.Protocol -> PacketInspectorScreen(inspectorViewModel)
            else -> PlaceholderContent(screen)
        }
    }
}

@Composable
private fun PlaceholderContent(screen: AppScreen) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = screen.icon,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = screen.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = screen.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StatusBar(studioService: StudioService) {
    val connectionState by studioService.tcpClient.connectionState.collectAsState()
    val diag by studioService.packetRepository.diagnostics.collectAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        StatusItem(label = "Connection", value = connectionState.name)
        StatusItem(label = "Packets", value = diag.totalPackets.toString())
        StatusItem(label = "Drops", value = diag.dropCount.toString())
        StatusItem(label = "Phase", value = "Diagnostics")
    }
}

@Composable
private fun StatusItem(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
