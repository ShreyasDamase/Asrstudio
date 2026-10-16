package org.asr.studio.ui.docs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.asr.studio.ui.theme.StudioColors
import org.asr.studio.ui.theme.StudioIconSize
import org.asr.studio.ui.theme.StudioTypeSize
import org.asr.studio.ui.theme.StudioFonts

private enum class DocSection(
    val title: String,
    val icon: ImageVector
) {
    GettingStarted("Getting Started", Icons.Default.RocketLaunch),
    Connectivity("Connectivity", Icons.Default.Wifi),
    HardwareModules("Hardware Modules", Icons.Default.Memory),
    AdbReference("ADB Reference", Icons.Default.Terminal),
    Ros2Integration("ROS 2 & Protocol", Icons.Default.Hub),
    SensorCatalog("Sensor Catalog", Icons.Default.Sensors)
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun DocsScreen() {
    var selectedSection by remember { mutableStateOf(DocSection.GettingStarted) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(StudioColors.panelRaised, MaterialTheme.shapes.small)
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DocSection.entries.forEach { section ->
                val selected = selectedSection == section
                Surface(
                    modifier = Modifier
                        .clickable { selectedSection = section }
                        .border(
                            width = 1.dp,
                            color = if (selected) StudioColors.accent.copy(alpha = 0.55f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                            shape = MaterialTheme.shapes.small,
                        ),
                    color = if (selected) StudioColors.accent.copy(alpha = 0.12f) else Color.Transparent,
                    shape = MaterialTheme.shapes.small,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                    ) {
                        Icon(
                            imageVector = section.icon,
                            contentDescription = null,
                            modifier = Modifier.size(StudioIconSize.small),
                            tint = if (selected) StudioColors.accent else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = section.title,
                            fontSize = StudioTypeSize.controlLabel,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) StudioColors.textPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                }
            }
        }

        // ── Section Content ──────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            when (selectedSection) {
                DocSection.GettingStarted -> GettingStartedDoc()
                DocSection.Connectivity -> ConnectivityDoc()
                DocSection.HardwareModules -> HardwareModulesDoc()
                DocSection.AdbReference -> AdbReferenceDoc()
                DocSection.Ros2Integration -> Ros2IntegrationDoc()
                DocSection.SensorCatalog -> SensorCatalogDoc()
            }
        }
    }
}

// ── 1. Getting Started Document ──────────────────────────────────────────────
@Composable
private fun GettingStartedDoc() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Getting Started with ASR", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("ASR Android Runtime runs the phone sensors and publishes ROS 2 data. ASR Studio is a separate desktop application for TCP telemetry and diagnostics. ROS 2 DDS and Studio TCP have independent controls.", style = MaterialTheme.typography.bodyMedium)

        DocCard(title = "System Component Overview", icon = Icons.Default.Layers) {
            DocBullet("ASR Android Runtime", "Runs phone sensor modules and exposes ROS 2 DDS and optional TCP connections.")
            DocBullet("ASR Studio", "Desktop diagnostics client for camera, IMU, audio, GPS, device state, and ASR packet inspection.")
            DocBullet("ROS 2 USB Link Monitor", "Optional companion tool for USB-tether DDS link and sensor-flow diagnostics. Wi-Fi DDS uses normal ROS 2 nodes.")
        }

        DocCard(title = "First Connection", icon = Icons.Default.PlayArrow) {
            DocCodeBlock("""
# 1. On Android: open ASR Runtime, grant needed permissions, and start Runtime.
# 2. For the desktop app: use the TCP section in ASR Runtime and ASR Studio -> Devices.
# 3. For ROS 2: enable Wi-Fi DDS or USB DDS separately in the phone app.
# 4. For Wi-Fi DDS, use a normal ROS 2 node. For USB-tether diagnostics, follow the USB Link Monitor guide.

ROS_DOMAIN_ID=0
RMW_IMPLEMENTATION=rmw_fastrtps_cpp
            """.trimIndent())
            Text("The ROS environment lines apply to the ROS 2 host. Android uses Cyclone DDS. Keep the domain ID consistent on both participants.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ── 2. Connectivity Document ──────────────────────────────────────────────────
@Composable
private fun ConnectivityDoc() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Connectivity: TCP and ROS 2 DDS", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("TCP connects the phone to ASR Studio. ROS 2 DDS connects the phone to ROS 2 participants. They are independent paths; enabling one does not enable the other.", style = MaterialTheme.typography.bodyMedium)

        DocCard(title = "Wi-Fi TCP to ASR Studio", icon = Icons.Default.Wifi) {
            DocBullet("Network", "Connect the desktop and phone to a network that allows them to reach each other.")
            DocBullet("ASR Studio", "Open Devices, select Wi-Fi, enter the desktop's local IP, then choose Listen (Server).")
            DocBullet("Android", "Enable TCP and connect to the desktop IP on port 4242.")
        }

        DocCard(title = "USB / ADB TCP to ASR Studio", icon = Icons.Default.Cable) {
            DocBullet("Prepare", "Connect the phone, authorize USB debugging, and keep the device visible in adb devices.")
            DocBullet("Configure", "In ASR Studio -> Devices -> USB / ADB, click Auto-Configure USB / ADB (1-Click Setup). It applies adb reverse for port 4242.")
            DocBullet("Connect", "Choose Listen (Server) in Studio, then enable TCP in Android with host 127.0.0.1 and port 4242.")
            DocBullet("Scope", "ADB forwarding is only the Studio TCP path. It does not enable ROS 2 DDS or USB tethering.")
        }

        DocCard(title = "ROS 2 DDS (USB and Wi-Fi)", icon = Icons.Default.Hub) {
            DocBullet("Phone", "Enable USB DDS (with USB tethering) or Wi-Fi DDS in the phone app. State remains 'Waiting for discovery' until a ROS 2 peer listens.")
            DocBullet("Host", "Use a sourced ROS 2 environment with rmw_fastrtps_cpp and the same ROS_DOMAIN_ID (default 0).")
            DocBullet("Wi-Fi", "Use a regular ROS 2 node or subscriber on the network; the USB Link Monitor is not needed for Wi-Fi DDS.")
            DocBullet("USB", "Enable Android USB tethering and USB DDS. Follow the companion USB Link Monitor guide for host diagnostics.")
        }
    }
}

// ── 3. Hardware Modules Document ─────────────────────────────────────────────
@Composable
private fun HardwareModulesDoc() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Hardware Modules Reference", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        DocCard(title = "Camera Module (id: 'camera')", icon = Icons.Default.PhotoCamera) {
            DocBullet("Image path", "CameraX YUV_420_888 frames are converted and JPEG-compressed by the Android runtime.")
            DocBullet("Configuration", "Resolution and frame rate are configurable; actual availability depends on the camera.")
            DocBullet("Topic ID", "0x01 (CAMERA_FRAME)")
        }

        DocCard(title = "IMU Module (id: 'imu')", icon = Icons.Default.Explore) {
            DocBullet("v1 packet fields", "Accelerometer (m/s²), gyroscope (rad/s), gravity vector (m/s²), and a four-component rotation quaternion [w, x, y, z].")
            DocBullet("Not in the current v1 packet", "Linear acceleration and magnetometer samples are not serialized by the current Android IMU packet.")
            DocBullet("Topic ID", "0x02 (IMU)")
        }

        DocCard(title = "Speaker Module (id: 'speaker')", icon = Icons.Default.VolumeUp) {
            DocBullet("Engine", "Android Text-to-Speech.")
            DocBullet("TCP command", "speaker.speak accepts a text parameter; the current Android handler speaks that text.")
            DocBullet("Intent Action", "org.asr.runtime.SPEAK (--es text \"...\")")
        }

        DocCard(title = "Audio Microphone Module (id: 'audio')", icon = Icons.Default.Mic) {
            DocBullet("Format", "16 kHz Mono PCM 16-bit Signed Little Endian.")
            DocBullet("Telemetry", "RMS level, peak hold level, noise floor, and VAD sent at ~20 Hz.")
            DocBullet("Topic IDs", "0x05 (AUDIO diagnostics); raw PCM is currently sent on 0x10, which Android's frozen registry names RESERVED_10.")
        }

        DocCard(title = "GPS Module (id: 'gps')", icon = Icons.Default.Place) {
            DocBullet("Fields", "Latitude, Longitude, Altitude (m ASL), Accuracy (m), Bearing, Speed (m/s).")
            DocBullet("Topic ID", "0x03 (GPS)")
        }

        DocCard(title = "Flashlight Module (id: 'flashlight')", icon = Icons.Default.FlashlightOn) {
            DocBullet("Control", "Physical rear camera LED torch toggle.")
            DocBullet("Control path", "Uses the COMMAND packet topic (0x08); Android does not define a separate flashlight telemetry topic in its v1 registry.")
        }

        DocCard(title = "Environment Sensors (id: 'environment')", icon = Icons.Default.LightMode) {
            DocBullet("Output", "Illuminance, pressure, ambient temperature, and relative humidity when the device supports them.")
            DocBullet("Topic ID", "0x06 (ENVIRONMENT), with the sensor type and value in the payload.")
        }
    }
}

// ── 4. ADB Command Reference ─────────────────────────────────────────────────
@Composable
private fun AdbReferenceDoc() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Complete ADB Terminal Command Reference", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("Standard ADB CLI commands to control, query, and test the ASR Runtime on Android devices.", style = MaterialTheme.typography.bodyMedium)

        DocCard(title = "1. Runtime Lifecycle & Port Forwarding", icon = Icons.Default.PowerSettingsNew) {
            DocCodeBlock("""
# 1. Reverse TCP Port for direct USB streaming (Linux / macOS / Windows)
adb reverse tcp:4242 tcp:4242

# 2. Start ASR Service (Foreground)
adb shell "am start-foreground-service -a org.asr.runtime.START org.asr.runtime/org.asr.runtime.core.AsrService"

# 3. Stop ASR Service
adb shell "am start-foreground-service -a org.asr.runtime.STOP org.asr.runtime/org.asr.runtime.core.AsrService"

# 4. Restart ASR Service (Full re-initialization)
adb shell "am start-foreground-service -a org.asr.runtime.RESTART org.asr.runtime/org.asr.runtime.core.AsrService"

# 5. Emergency Reset & Relaunch App
adb shell "am force-stop org.asr.runtime && am start -n org.asr.runtime/.MainActivity"
            """.trimIndent())
        }

        DocCard(title = "2. Mission Profiles (Group Activation)", icon = Icons.Default.Tune) {
            DocCodeBlock("""
# Enable Full Sensor Suite (Camera + IMU + GPS + Audio + Speaker + Battery + Environment)
adb shell "am start-foreground-service -a org.asr.runtime.PROFILE_ENABLE --es profile full_sensor org.asr.runtime/org.asr.runtime.core.AsrService"

# Enable Teleoperation Profile (Camera + Audio + Speaker)
adb shell "am start-foreground-service -a org.asr.runtime.PROFILE_ENABLE --es profile teleop org.asr.runtime/org.asr.runtime.core.AsrService"

# Enable Mapping / V-SLAM Profile (Camera + IMU)
adb shell "am start-foreground-service -a org.asr.runtime.PROFILE_ENABLE --es profile mapping org.asr.runtime/org.asr.runtime.core.AsrService"

# Enable Outdoor Navigation Profile (IMU + GPS + Battery)
adb shell "am start-foreground-service -a org.asr.runtime.PROFILE_ENABLE --es profile navigation org.asr.runtime/org.asr.runtime.core.AsrService"
            """.trimIndent())
        }

        DocCard(title = "3. Individual Sensor Module Control", icon = Icons.Default.ToggleOn) {
            DocCodeBlock("""
# Start Modules (audio, camera, imu, gps, speaker, battery, environment)
adb shell "am start-foreground-service -a org.asr.runtime.MODULE_START --es module_id audio org.asr.runtime/org.asr.runtime.core.AsrService"
adb shell "am start-foreground-service -a org.asr.runtime.MODULE_START --es module_id camera org.asr.runtime/org.asr.runtime.core.AsrService"
adb shell "am start-foreground-service -a org.asr.runtime.MODULE_START --es module_id imu org.asr.runtime/org.asr.runtime.core.AsrService"

# Stop Modules
adb shell "am start-foreground-service -a org.asr.runtime.MODULE_STOP --es module_id audio org.asr.runtime/org.asr.runtime.core.AsrService"
adb shell "am start-foreground-service -a org.asr.runtime.MODULE_STOP --es module_id camera org.asr.runtime/org.asr.runtime.core.AsrService"
            """.trimIndent())
        }

        DocCard(title = "4. Hardware Parameters (Camera, Flashlight & Speaker)", icon = Icons.Default.Settings) {
            DocCodeBlock("""
# Configure Camera (Back / Front lens, resolution, fps)
adb shell "am start-foreground-service -a org.asr.runtime.CAMERA_CONFIG --es lens back --ei width 640 --ei height 480 --ei fps 30 org.asr.runtime/org.asr.runtime.core.AsrService"

# Control Flashlight Torch (Turn ON / OFF)
adb shell "am start-foreground-service -a org.asr.runtime.FLASHLIGHT --ez on true org.asr.runtime/org.asr.runtime.core.AsrService"
adb shell "am start-foreground-service -a org.asr.runtime.FLASHLIGHT --ez on false org.asr.runtime/org.asr.runtime.core.AsrService"

# Trigger Text-To-Speech (TTS) Voice Command
adb shell "am start-foreground-service -a org.asr.runtime.SPEAK --es text 'Warning. Dynamic obstacle detected.' org.asr.runtime/org.asr.runtime.core.AsrService"
            """.trimIndent())
        }

        DocCard(title = "5. Live Diagnostics & Logcat Telemetry", icon = Icons.Default.Analytics) {
            DocCodeBlock("""
# Query Runtime Status
adb shell "am start-foreground-service -a org.asr.runtime.STATUS org.asr.runtime/org.asr.runtime.core.AsrService"

# Stream High-Speed Telemetry and Frame Benchmark Logs
adb logcat -s PhoneCamera:V AsrAudio:V AsrService:V AsrTransport:V
            """.trimIndent())
        }
    }
}

// ── 5. ROS 2 Integration Document ─────────────────────────────────────────────
@Composable
private fun Ros2IntegrationDoc() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("ROS 2 DDS Integration", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        DocCard(title = "DDS setup", icon = Icons.Default.Hub) {
            DocBullet("Android middleware", "ASR Android Runtime uses Cyclone DDS.")
            DocBullet("Host middleware", "The validated host setup uses rmw_fastrtps_cpp. It interoperates with the Android Cyclone DDS participant.")
            DocBullet("Domain", "Set the same ROS_DOMAIN_ID on the phone and host. The default is 0.")
            DocBullet("Wi-Fi", "Enable Wi-Fi DDS in the phone app and use a normal ROS 2 node or subscriber. The ASR USB helper is not used for Wi-Fi.")
            DocBullet("USB", "Enable Android USB tethering and USB DDS, then run the asr_ros2 USB link monitor on the host if you need persistent diagnostics.")
        }

        DocCard(title = "Binary Frame Layout (28-Byte Header)", icon = Icons.Default.DataObject) {
            DocCodeBlock("""
Offset  Bytes  Field          Description
0-1     2      Magic          A5 AA
2       1      Version        Protocol version
3       1      Topic ID       ASR packet topic identifier
4       1      Flags          Packet flags
5       1      Device ID      Device identifier
6-7     2      Reserved       Little-endian reserved field
8-15    8      Timestamp      Nanoseconds, little-endian
16-19   4      Sequence       Little-endian sequence number
20-23   4      Payload length Little-endian byte count
24-27   4      CRC32          IEEE CRC32 over header bytes 0-23 and payload
28..    n      Payload        Topic-specific payload
            """.trimIndent())
        }

        DocCard(title = "Topic namespace", icon = Icons.Default.AccountTree) {
            Text("Topics are published below /asr_android_<device-id>. For example, phone 168 publishes IMU on /asr_android_168/imu. Exact message availability depends on the device, permission, and module state.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            DocBullet("imu", "sensor_msgs/msg/Imu")
            DocBullet("camera/image/compressed", "sensor_msgs/msg/CompressedImage")
            DocBullet("camera/camera_info", "sensor_msgs/msg/CameraInfo")
            DocBullet("gps", "sensor_msgs/msg/NavSatFix")
            DocBullet("battery", "sensor_msgs/msg/BatteryState")
            DocBullet("environment/illuminance", "sensor_msgs/msg/Illuminance")
            DocBullet("audio/raw", "std_msgs/msg/Int16MultiArray")
            DocBullet("Inbound subscriptions", "The runtime subscribes to test/in, speaker/speech, and flashlight (std_msgs/msg/String). These are commands sent to the phone, not published sensor topics.")
        }

        DocCard(title = "USB monitor scope", icon = Icons.Default.Cable) {
            Text("The companion asr_ros2 helper is a USB-tether link and sensor-flow monitor. It checks the ROS graph and actual callbacks separately for IMU, compressed images, battery, and runtime status. It is optional for DDS and is not the Wi-Fi connection path.", style = MaterialTheme.typography.bodySmall)
        }
    }
}

// ── 6. Sensor Catalog Document ───────────────────────────────────────────────
@Composable
private fun SensorCatalogDoc() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("ROS 2 Sensor Topics", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("Topic names use the device namespace /asr_android_<device-id>. Availability depends on the phone's hardware, Android permissions, and which runtime modules are enabled. Rates may vary by device.", style = MaterialTheme.typography.bodyMedium)

        DocCard(title = "Find the phone topics", icon = Icons.Default.Search) {
            DocCodeBlock("""
ros2 topic list | grep '^/asr_android_'
ros2 topic echo /asr_android_168/imu
ros2 topic echo /asr_android_168/camera/image/compressed
            """.trimIndent())
            Text("Replace 168 with the device ID shown by the namespace on your ROS 2 graph.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        DocCard(title = "Motion and camera", icon = Icons.Default.Explore) {
            DocBullet("/imu", "sensor_msgs/msg/Imu — fused orientation, angular velocity, and acceleration fields")
            DocBullet("/camera/image/compressed", "sensor_msgs/msg/CompressedImage — compressed camera frames")
            DocBullet("/camera/camera_info", "sensor_msgs/msg/CameraInfo — calibration and image dimensions")
            DocBullet("/motion/steps", "std_msgs/msg/UInt32 — available on devices with a step counter")
        }

        DocCard(title = "Location, battery, and environment", icon = Icons.Default.Sensors) {
            DocBullet("/gps", "sensor_msgs/msg/NavSatFix — location when permission and a fix are available")
            DocBullet("/battery", "sensor_msgs/msg/BatteryState — event-driven state updates")
            DocBullet("/environment/illuminance", "sensor_msgs/msg/Illuminance, when supported")
            DocBullet("/environment/pressure", "sensor_msgs/msg/FluidPressure, when supported")
            DocBullet("/environment/temperature", "sensor_msgs/msg/Temperature, when supported")
            DocBullet("/environment/relative_humidity", "sensor_msgs/msg/RelativeHumidity, when supported")
        }

        DocCard(title = "Audio and device commands", icon = Icons.Default.Mic) {
            DocBullet("/audio/raw", "std_msgs/msg/Int16MultiArray — microphone samples when audio capture is enabled")
            DocBullet("/status", "std_msgs/msg/String — runtime heartbeat")
            DocBullet("/capabilities", "std_msgs/msg/String — device capability information")
            DocBullet("/speaker/speech, /flashlight, /test/in", "std_msgs/msg/String subscriptions for inbound speech, flashlight, and test/control commands")
        }

        DocCard(title = "USB monitor coverage", icon = Icons.Default.Cable) {
            Text("The companion USB Link Monitor currently reports graph and message flow for IMU, compressed camera frames, battery, and runtime status. Use ROS 2 tools or your application node to inspect the other topics.", style = MaterialTheme.typography.bodySmall)
        }
    }
}

// ── Helper UI Components ─────────────────────────────────────────────────────
@Composable
private fun DocCard(
    title: String,
    icon: ImageVector? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = StudioColors.panelRaised
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, StudioColors.border),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        modifier = Modifier.size(StudioIconSize.navigation),
                        tint = StudioColors.accent
                    )
                }
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = StudioColors.textPrimary
                )
            }
            HorizontalDivider(color = StudioColors.border)
            content()
        }
    }
}

@Composable
private fun DocBullet(label: String, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Top) {
        Text("•", fontWeight = FontWeight.Bold, color = StudioColors.accent)
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = StudioColors.textPrimary)) {
                    append(label)
                }
                append(" — ")
                withStyle(SpanStyle(color = StudioColors.textSecondary)) {
                    append(text)
                }
            },
            fontSize = StudioTypeSize.controlLabel,
            color = StudioColors.textSecondary,
        )
    }
}

@Composable
private fun DocCodeBlock(code: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, StudioColors.border, MaterialTheme.shapes.medium),
        color = StudioColors.panelInset,
        shape = MaterialTheme.shapes.medium
    ) {
        Text(
            text = code,
            fontFamily = StudioFonts.mono,
            fontSize = StudioTypeSize.supporting,
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(14.dp),
            color = StudioColors.accent
        )
    }
}
