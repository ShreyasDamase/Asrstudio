package org.asr.studio

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.Icons
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.asr.studio.data.StudioService
import org.asr.studio.ui.camera.CameraScreen
import org.asr.studio.ui.camera.CameraViewModel
import org.asr.studio.ui.devices.DevicesScreen
import org.asr.studio.ui.devices.DevicesViewModel
import org.asr.studio.ui.diagnostics.DiagnosticsScreen
import org.asr.studio.ui.diagnostics.DiagnosticsViewModel
import org.asr.studio.ui.diagnostics.GpsScreen
import org.asr.studio.ui.diagnostics.MicrophoneScreen
import org.asr.studio.ui.docs.DocsScreen
import org.asr.studio.ui.inspector.PacketInspectorScreen
import org.asr.studio.ui.inspector.PacketInspectorViewModel
import org.asr.studio.ui.speaker.SpeakerScreen
import org.asr.studio.ui.speaker.SpeakerViewModel
import org.asr.studio.ui.theme.AsrStudioTheme
import org.asr.studio.ui.theme.StudioColors
import org.asr.studio.ui.theme.StudioIconSize
import org.asr.studio.ui.theme.StudioSpacing

private enum class AppScreen(
    val label: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val group: String,
) {
    Dashboard(
        label = "Dashboard",
        title = "Dashboard",
        description = "Live device health and sensor activity.",
        icon = Icons.Default.Dashboard,
        group = "WORKSPACE",
    ),
    Devices(
        label = "Devices",
        title = "Device Connection",
        description = "Manage the phone connection and runtime controls.",
        icon = Icons.Default.Router,
        group = "WORKSPACE",
    ),
    Camera(
        label = "Camera",
        title = "Camera Diagnostics",
        description = "Live camera feed and capture details.",
        icon = Icons.Default.PhotoCamera,
        group = "SENSORS",
    ),
    Speaker(
        label = "Speaker",
        title = "Speaker Diagnostics",
        description = "Test Text-to-Speech (TTS), voice alerts, and speaker hardware telemetry.",
        icon = Icons.Default.VolumeUp,
        group = "SENSORS",
    ),
    Imu(
        label = "IMU",
        title = "IMU Diagnostics",
        description = "Orientation, inertial readings, and calibration.",
        icon = Icons.Default.Memory,
        group = "SENSORS",
    ),
    Microphone(
        label = "Microphone",
        title = "Microphone Monitor",
        description = "Audio level, waveform, and microphone controls.",
        icon = Icons.Default.Mic,
        group = "SENSORS",
    ),
    Gps(
        label = "GPS",
        title = "GPS Diagnostics",
        description = "Location fix, motion, and receiver health.",
        icon = Icons.Default.Place,
        group = "SENSORS",
    ),
    Protocol(
        label = "Protocol",
        title = "Protocol Inspector",
        description = "Inspect packets, sequence numbers, CRC, and payloads.",
        icon = Icons.AutoMirrored.Filled.Article,
        group = "TOOLS",
    ),
    Docs(
        label = "Docs",
        title = "Documentation",
        description = "Connection guides and sensor references.",
        icon = Icons.Default.Description,
        group = "HELP",
    ),
    About(
        label = "About",
        title = "About ASR Studio",
        description = "Application details, version, and project links.",
        icon = Icons.Default.Info,
        group = "HELP",
    ),
}

@Composable
@Preview
fun App() {
    val studioService = StudioService.instance
    val devicesViewModel = remember { DevicesViewModel(studioService) }
    val diagnosticsViewModel = remember { DiagnosticsViewModel(studioService) }
    val inspectorViewModel = remember { PacketInspectorViewModel(studioService) }
    val cameraViewModel = remember { CameraViewModel(studioService) }
    val speakerViewModel = remember { SpeakerViewModel(studioService) }

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
                        cameraViewModel = cameraViewModel,
                        speakerViewModel = speakerViewModel,
                        studioService = studioService,
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
private fun Sidebar(
    selectedScreen: AppScreen,
    onScreenSelected: (AppScreen) -> Unit,
) {
    Column(
        modifier = Modifier
            .width(220.dp)
            .fillMaxHeight()
            .background(StudioColors.panel)
            .padding(horizontal = StudioSpacing.md, vertical = StudioSpacing.lg),
    ) {
        Text(
            text = "ASR Studio",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Desktop diagnostics",
            modifier = Modifier.padding(start = 1.dp, top = 3.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(StudioSpacing.page))

        listOf("WORKSPACE", "SENSORS", "TOOLS", "HELP").forEach { group ->
            Text(
                text = group,
                modifier = Modifier.padding(start = StudioSpacing.md, top = StudioSpacing.md, bottom = StudioSpacing.sm),
                style = MaterialTheme.typography.labelSmall,
                color = StudioColors.textMuted,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.8.sp,
            )
            AppScreen.entries.filter { it.group == group }.forEach { screen ->
                NavigationItem(
                    screen = screen,
                    selected = screen == selectedScreen,
                    onClick = { onScreenSelected(screen) },
                )
            }
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
        if (selected) StudioColors.accentSurface else Color.Transparent
    val contentColor =
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .background(background, MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .padding(horizontal = StudioSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = screen.icon,
            contentDescription = screen.label,
            modifier = Modifier.size(StudioIconSize.navigation),
            tint = contentColor,
        )
        Spacer(modifier = Modifier.width(StudioSpacing.md))
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
            .height(64.dp)
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = StudioSpacing.page),
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
                text = screen.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = "TCP · " + when (connectionState) {
                org.asr.studio.protocol.ConnectionState.Connected -> "Connected"
                org.asr.studio.protocol.ConnectionState.Connecting -> "Connecting"
                else -> "Disconnected"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ScreenContent(
    screen: AppScreen,
    devicesViewModel: DevicesViewModel,
    diagnosticsViewModel: DiagnosticsViewModel,
    inspectorViewModel: PacketInspectorViewModel,
    cameraViewModel: CameraViewModel,
    speakerViewModel: SpeakerViewModel,
    studioService: org.asr.studio.data.StudioService,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        when (screen) {
            AppScreen.Dashboard -> org.asr.studio.ui.dashboard.DashboardScreen(
                sensorRepository = studioService.sensorRepository,
                cameraRepository = studioService.cameraRepository
            )
            AppScreen.Devices -> DevicesScreen(devicesViewModel)
            AppScreen.Imu -> DiagnosticsScreen(diagnosticsViewModel)
            AppScreen.Gps -> GpsScreen(diagnosticsViewModel)
            AppScreen.Protocol -> PacketInspectorScreen(inspectorViewModel)
            AppScreen.Camera -> CameraScreen(cameraViewModel)
            AppScreen.Speaker -> SpeakerScreen(speakerViewModel)
            AppScreen.Microphone -> MicrophoneScreen(studioService.sensorRepository)
            AppScreen.Docs -> DocsScreen()
            AppScreen.About -> org.asr.studio.ui.about.AboutScreen()
            else -> PlaceholderContent(screen)
        }
    }
}

@Composable
private fun PlaceholderContent(screen: AppScreen) {
    Surface(
        modifier = Modifier.padding(28.dp),
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = screen.icon,
                contentDescription = null,
                modifier = Modifier.size(StudioIconSize.prominent),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = screen.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = screen.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun StatusBar(studioService: StudioService) {
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
        StatusItem(label = "Transport", value = "TCP")
        StatusItem(label = "Packets", value = diag.totalPackets.toString())
        StatusItem(label = "Sequence gaps", value = diag.dropCount.toString())
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
