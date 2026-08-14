package org.asr.studio.ui.docs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
fun DocsScreen() {
    var selectedSection by remember { mutableStateOf(DocSection.GettingStarted) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // ── Professional Tab Row ─────────────────────────────────────────────
        ScrollableTabRow(
            selectedTabIndex = selectedSection.ordinal,
            edgePadding = 0.dp,
            containerColor = Color(0xFF141923),
            contentColor = Color(0xFF5EEAD4),
            indicator = { tabPositions ->
                if (selectedSection.ordinal < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedSection.ordinal]),
                        color = Color(0xFF43D9C5),
                        height = 3.dp
                    )
                }
            },
            divider = {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
            }
        ) {
            DocSection.entries.forEach { section ->
                val selected = selectedSection == section
                Tab(
                    selected = selected,
                    onClick = { selectedSection = section },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                        ) {
                            Icon(
                                imageVector = section.icon,
                                contentDescription = section.title,
                                modifier = Modifier.size(16.dp),
                                tint = if (selected) Color(0xFF43D9C5) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = section.title,
                                fontSize = 13.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                )
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
        Text("ASR System Architecture & Getting Started", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("Android Sensor Runtime (ASR) acts as the low-latency hardware abstraction layer for mobile robotics. ASR Studio is the engineering diagnostics workstation used to monitor telemetry, render live camera frames, inspect binary packets, and issue high-level hardware commands.", style = MaterialTheme.typography.bodyMedium)

        DocCard(title = "System Component Overview", icon = Icons.Default.Layers) {
            DocBullet("Android Sensor Runtime (cameraros)", "Runs on mobile Android device. Captures raw CameraX frames, IMU (Accel, Gyro, Quaternion, Magnetometer), GPS, Battery, Audio Mic, and Speaker TTS.")
            DocBullet("ASR Studio (asrstudio)", "Desktop engineering workstation built with Compose Multiplatform. Renders live camera feed, 3D CAD IMU viewport, live PCM audio speaker monitor, and auto-configures USB ADB bridges.")
            DocBullet("Binary Protocol (ASR-TCP)", "Standardized IEEE 802.3 CRC32-validated binary protocol running over TCP port 4242.")
        }

        DocCard(title = "Quickstart Workflow", icon = Icons.Default.PlayArrow) {
            DocCodeBlock("""
# 1. Build and install ASR Runtime on Android device
./gradlew :app:installDebug

# 2. Launch ASR Studio Desktop App
./gradlew :desktopApp:run

# 3. Connect via USB ADB (Recommended)
adb reverse tcp:4242 tcp:4242

# 4. In ASR Studio -> Devices -> Click "Listen (Server)"
# 5. On Android Phone -> Tap START -> Tap CONNECT
            """.trimIndent())
        }
    }
}

// ── 2. Connectivity Document ──────────────────────────────────────────────────
@Composable
private fun ConnectivityDoc() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Connectivity: Wi-Fi vs. USB / ADB Mode", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("ASR supports dual transport layers: Local Wi-Fi (wireless mobility) and USB / ADB (zero-latency physical cable connection).", style = MaterialTheme.typography.bodyMedium)

        DocCard(title = "Wi-Fi Mode (LAN)", icon = Icons.Default.Wifi) {
            DocBullet("Network", "Ensure Desktop and Android phone are connected to the same Wi-Fi network subnet.")
            DocBullet("Server Listener", "In ASR Studio -> Select 'Wi-Fi' tab -> Click 'Listen (Server)'.")
            DocBullet("Android Connection", "On Android phone -> Enter Desktop IP (e.g. 192.168.1.42) -> Tap CONNECT.")
        }

        DocCard(title = "USB / ADB Mode (Reverse TCP Forwarding)", icon = Icons.Default.Cable) {
            Text("adb reverse tcp:4242 tcp:4242 forwards connections originating from Android phone (127.0.0.1:4242) over USB cable directly to ASR Studio listening on port 4242.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            DocCodeBlock("""
# Command to configure ADB Reverse:
adb reverse tcp:4242 tcp:4242

# Clear stale or conflicting rules if necessary:
adb forward --remove-all
adb reverse --remove-all
adb reverse tcp:4242 tcp:4242
            """.trimIndent())
        }

        DocCard(title = "1-Click Auto Setup in ASR Studio", icon = Icons.Default.FlashOn) {
            Text("In ASR Studio, open Devices -> USB / ADB tab -> Click 'Auto-Configure USB / ADB (1-Click Setup)'. ASR Studio automatically clears stale rules and configures adb reverse in the background.")
        }
    }
}

// ── 3. Hardware Modules Document ─────────────────────────────────────────────
@Composable
private fun HardwareModulesDoc() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Hardware Modules Reference", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        DocCard(title = "Camera Module (id: 'camera')", icon = Icons.Default.PhotoCamera) {
            DocBullet("Resolution", "640x480 YUV420_888 converted to NV21 with Stride-aware memory alignment.")
            DocBullet("Encoding", "JPEG (Quality 75, ~25 KB/frame at 30 FPS).")
            DocBullet("Topic ID", "0x01 (CAMERA_FRAME)")
        }

        DocCard(title = "IMU Module (id: 'imu')", icon = Icons.Default.Explore) {
            DocBullet("Sensors", "Accelerometer (m/s²), Gyroscope (rad/s), Gravity Vector, Magnetometer (μT), Rotation Vector.")
            DocBullet("Quaternion", "Full 4-component Quaternion [w, x, y, z] output at 100 Hz.")
            DocBullet("Topic ID", "0x02 (IMU)")
        }

        DocCard(title = "Speaker Module (id: 'speaker')", icon = Icons.Default.VolumeUp) {
            DocBullet("Engine", "Android Text-To-Speech (TTS) + SSML Natural Speech Prosody.")
            DocBullet("Commands", "speaker.speak (JSON: {\"cmd\":\"speaker.speak\",\"params\":{\"text\":\"...\",\"ssml\":true}})")
            DocBullet("Intent Action", "org.asr.runtime.SPEAK (--es text \"...\")")
        }

        DocCard(title = "Audio Microphone Module (id: 'audio')", icon = Icons.Default.Mic) {
            DocBullet("Format", "16 kHz Mono PCM 16-bit Signed Little Endian.")
            DocBullet("Telemetry", "RMS level, peak hold level, noise floor, and VAD sent at ~20 Hz.")
            DocBullet("Topic ID", "0x05 (AUDIO) & 0x10 (AUDIO_RAW)")
        }

        DocCard(title = "GPS Module (id: 'gps')", icon = Icons.Default.Place) {
            DocBullet("Fields", "Latitude, Longitude, Altitude (m ASL), Accuracy (m), Bearing, Speed (m/s).")
            DocBullet("Topic ID", "0x03 (GPS)")
        }

        DocCard(title = "Flashlight Module (id: 'flashlight')", icon = Icons.Default.FlashlightOn) {
            DocBullet("Control", "Physical rear camera LED torch toggle.")
            DocBullet("Topic ID", "0x0E (FLASHLIGHT)")
        }

        DocCard(title = "Ambient Light Sensor (id: 'light_sensor')", icon = Icons.Default.LightMode) {
            DocBullet("Output", "Ambient illuminance in lux.")
            DocBullet("Topic ID", "0x0F (LIGHT_SENSOR)")
        }
    }
}

// ── 4. ADB Command Reference ─────────────────────────────────────────────────
@Composable
private fun AdbReferenceDoc() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Complete ADB Terminal Command Reference", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("Standard ADB CLI commands to control, query, and test the ASR Runtime on Android devices.", style = MaterialTheme.typography.bodyMedium)

        DocCard(title = "1. Runtime Lifecycle Commands", icon = Icons.Default.PowerSettingsNew) {
            DocCodeBlock("""
# Start ASR Service (Foreground)
adb shell "am start-foreground-service -a org.asr.runtime.START org.asr.runtime/org.asr.runtime.core.AsrService"

# Stop ASR Service
adb shell "am start-foreground-service -a org.asr.runtime.STOP org.asr.runtime/org.asr.runtime.core.AsrService"

# Restart ASR Service
adb shell "am start-foreground-service -a org.asr.runtime.RESTART org.asr.runtime/org.asr.runtime.core.AsrService"
            """.trimIndent())
        }

        DocCard(title = "2. Enable Sensor Profiles", icon = Icons.Default.Tune) {
            DocCodeBlock("""
# Enable Full Sensor Profile (Camera + IMU + GPS + Audio + Speaker + Battery)
adb shell "am start-foreground-service -a org.asr.runtime.PROFILE_ENABLE --es profile full_sensor org.asr.runtime/org.asr.runtime.core.AsrService"

# Enable Teleop Profile (Camera + Audio + Speaker)
adb shell "am start-foreground-service -a org.asr.runtime.PROFILE_ENABLE --es profile teleop org.asr.runtime/org.asr.runtime.core.AsrService"

# Enable Mapping Profile (Camera + IMU)
adb shell "am start-foreground-service -a org.asr.runtime.PROFILE_ENABLE --es profile mapping org.asr.runtime/org.asr.runtime.core.AsrService"
            """.trimIndent())
        }

        DocCard(title = "3. Individual Sensor Module Control", icon = Icons.Default.ToggleOn) {
            DocCodeBlock("""
# Start Audio Microphone Module
adb shell "am start-foreground-service -a org.asr.runtime.MODULE_START --es module_id audio org.asr.runtime/org.asr.runtime.core.AsrService"

# Stop Audio Microphone Module
adb shell "am start-foreground-service -a org.asr.runtime.MODULE_STOP --es module_id audio org.asr.runtime/org.asr.runtime.core.AsrService"

# Start Camera Module
adb shell "am start-foreground-service -a org.asr.runtime.MODULE_START --es module_id camera org.asr.runtime/org.asr.runtime.core.AsrService"
            """.trimIndent())
        }

        DocCard(title = "4. Speaker Text-To-Speech (TTS) Output", icon = Icons.Default.RecordVoiceOver) {
            DocCodeBlock("""
# Trigger Speech Intent
adb shell "am start-foreground-service -a org.asr.runtime.SPEAK --es text 'ASR speaker test successful' org.asr.runtime/org.asr.runtime.core.AsrService"

# Trigger Obstacle Warning Speech
adb shell "am start-foreground-service -a org.asr.runtime.SPEAK --es text 'Warning. Obstacle detected.' org.asr.runtime/org.asr.runtime.core.AsrService"
            """.trimIndent())
        }

        DocCard(title = "5. Verification & Logcat Diagnostics", icon = Icons.Default.Analytics) {
            DocCodeBlock("""
# View Active Modules & Speaker Logs
adb logcat -d -t 150 | grep -iE "(AsrAudio|AsrCamera|AsrService|Modules)"

# List Installed Packages
adb shell "pm list packages | grep asr"
            """.trimIndent())
        }
    }
}

// ── 5. ROS 2 Integration Document ─────────────────────────────────────────────
@Composable
private fun Ros2IntegrationDoc() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("ROS 2 Integration & Binary Protocol Specification", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        DocCard(title = "Binary Frame Layout (28-Byte Header)", icon = Icons.Default.DataObject) {
            DocCodeBlock("""
Offset  Type     Field          Description
0-1     UInt8[2] Magic          'A', 'S' (0x41, 0x53)
2       UInt8    Version        Protocol Version (0x01)
3       UInt8    TopicId        0x01=Camera, 0x02=IMU, 0x03=GPS, 0x05=Audio, 0x08=Command
4       UInt8    Flags          Compression / Encryption flags
5       UInt8    DeviceId       Hardware Device Identifier
6-13    UInt64   TimestampNs    Hardware Sensor Timestamp (ns)
14-17   UInt32   Seq            Monotonically increasing sequence number
18-21   UInt32   PayloadLength  Size of payload in bytes
22-25   UInt32   StoredCrc      IEEE 802.3 CRC32 of payload
26-27   UInt16   Reserved       Alignment padding
28..             Payload        Tightly packed payload bytes
            """.trimIndent())
        }

        DocCard(title = "ROS 2 Node Topic Mapping", icon = Icons.Default.Hub) {
            DocBullet("/asr/camera/compressed", "sensor_msgs/msg/CompressedImage (JPEG)")
            DocBullet("/asr/imu/data", "sensor_msgs/msg/Imu (Accel + Gyro + Quaternion)")
            DocBullet("/asr/gps/fix", "sensor_msgs/msg/NavSatFix")
            DocBullet("/asr/speaker/goal", "std_msgs/msg/String (TTS Speech Command)")
            DocBullet("/asr/audio/level", "asr_msgs/msg/AudioLevel (Telemetry)")
            DocBullet("/asr/audio/raw", "audio_common_msgs/msg/AudioData or std_msgs/msg/Int16MultiArray")
            DocBullet("/asr/flashlight/cmd", "asr_msgs/msg/FlashlightCmd")
            DocBullet("/asr/flashlight/state", "asr_msgs/msg/FlashlightState")
            DocBullet("/asr/light_sensor/lux", "sensor_msgs/msg/Illuminance")
            DocBullet("/asr/battery/state", "sensor_msgs/msg/BatteryState")
        }
    }
}

// ── 6. Sensor Catalog Document ───────────────────────────────────────────────
@Composable
private fun SensorCatalogDoc() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Android Sensor Catalog — ROS 2 Topic Reference",
             style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
            "Every sensor listed below is available on a standard Android smartphone. " +
            "ASR exposes each as a typed ROS 2 topic — no additional hardware purchase required. " +
            "Connect your phone via USB or Wi-Fi to unlock a complete robotics sensor suite.",
            style = MaterialTheme.typography.bodyMedium
        )

        DocCard(title = "Camera — /asr/camera/compressed", icon = Icons.Default.PhotoCamera) {
            DocBullet("ROS 2 msg", "sensor_msgs/msg/CompressedImage (JPEG)")
            DocBullet("Resolution", "640×480 to 1280×720, JPEG quality 75")
            DocBullet("Rate", "30 FPS")
            DocBullet("Topic ID", "0x01 (CAMERA_FRAME)")
            DocCodeBlock("""
ros2 topic echo /asr/camera/compressed --no-arr
# or view with rqt:
ros2 run rqt_image_view rqt_image_view
            """.trimIndent())
        }

        DocCard(title = "IMU (Accel + Gyro + Rotation) — /asr/imu/data", icon = Icons.Default.Explore) {
            DocBullet("ROS 2 msg", "sensor_msgs/msg/Imu")
            DocBullet("Quaternion", "Full [w, x, y, z] at 100 Hz from Android Rotation Vector sensor")
            DocBullet("Accel", "m/s² linear acceleration (gravity removed)")
            DocBullet("Gyro", "rad/s angular velocity")
            DocBullet("Topic ID", "0x02 (IMU)")
            DocCodeBlock("""
import rclpy
from sensor_msgs.msg import Imu

def imu_callback(msg):
    q = msg.orientation
    print(f'Quaternion: w={q.w:.3f} x={q.x:.3f} y={q.y:.3f} z={q.z:.3f}')
    print(f'Gyro: {msg.angular_velocity.x:.3f} rad/s')

node.create_subscription(Imu, '/asr/imu/data', imu_callback, 10)
            """.trimIndent())
        }

        DocCard(title = "Magnetometer — /asr/imu/magnetic_field", icon = Icons.Default.Navigation) {
            DocBullet("ROS 2 msg", "sensor_msgs/msg/MagneticField")
            DocBullet("Unit", "microtesla (μT), typically 25–65 μT (Earth's field)")
            DocBullet("Rate", "50 Hz")
            DocBullet("Note", "Fused with accelerometer for stable yaw. Avoid placing near motors/magnets.")
        }

        DocCard(title = "GPS — /asr/gps/fix", icon = Icons.Default.Place) {
            DocBullet("ROS 2 msg", "sensor_msgs/msg/NavSatFix")
            DocBullet("Fields", "Latitude, Longitude, Altitude (m ASL), Accuracy (m), Bearing, Speed (m/s)")
            DocBullet("Rate", "1 Hz (GPS hardware limited)")
            DocBullet("Topic ID", "0x03 (GPS)")
        }

        DocCard(title = "Speaker / TTS — /asr/speaker/say", icon = Icons.Default.VolumeUp) {
            DocBullet("ROS 2 msg", "std_msgs/msg/String (plain text or SSML)")
            DocBullet("Engine", "Android TextToSpeech — supports SSML markup for natural speech")
            DocBullet("SSML", "Send <speak><prosody rate='95%'>...</prosody></speak> for human-sounding output")
            DocBullet("Topic ID", "0x08 (COMMAND)")
            DocCodeBlock("""
# Publish speech command from ROS 2
ros2 topic pub /asr/speaker/say std_msgs/msg/String "{data: 'Warning, obstacle detected'}"
            """.trimIndent())
        }

        DocCard(title = "Microphone Level — /asr/audio/level & /asr/audio/raw", icon = Icons.Default.Mic) {
            DocBullet("ROS 2 msg", "asr_msgs/msg/AudioLevel (custom) & audio_common_msgs/msg/AudioData")
            DocBullet("Fields", "rms_level (0–1), peak_level (0–1), noise_floor_dbfs, voice_active (bool), sample_rate_hz")
            DocBullet("Rate", "~20 Hz telemetry + live 16 kHz PCM stream")
            DocBullet("Topic ID", "0x05 (AUDIO) & 0x10 (AUDIO_RAW)")
            DocBullet("Use case", "Speech recognition pipeline: subscribe /asr/audio/raw → feed to Whisper/Vosk")
        }

        DocCard(title = "Flashlight — /asr/flashlight/cmd & /asr/flashlight/state", icon = Icons.Default.FlashlightOn) {
            DocBullet("Command msg", "asr_msgs/msg/FlashlightCmd — fields: on (bool), brightness (float32, 0–1)")
            DocBullet("State msg", "asr_msgs/msg/FlashlightState — current on/brightness reported by phone")
            DocBullet("Topic ID", "0x0E (FLASHLIGHT)")
            DocBullet("Use case", "Dark environment navigation — trigger when ambient lux < 50")
            DocCodeBlock("""
# Toggle flashlight on from ROS 2
ros2 topic pub /asr/flashlight/cmd asr_msgs/msg/FlashlightCmd "{on: true, brightness: 1.0}"
            """.trimIndent())
        }

        DocCard(title = "Ambient Light Sensor — /asr/light_sensor/lux", icon = Icons.Default.LightMode) {
            DocBullet("ROS 2 msg", "sensor_msgs/msg/Illuminance")
            DocBullet("Unit", "lux (< 10=very dark, 10–50=dark, 200–1000=indoor, 1000–10000=daylight)")
            DocBullet("Topic ID", "0x0F (LIGHT_SENSOR)")
            DocBullet("Use case", "Auto-trigger flashlight when entering dark spaces")
        }

        DocCard(title = "Battery — /asr/battery/state", icon = Icons.Default.BatteryChargingFull) {
            DocBullet("ROS 2 msg", "sensor_msgs/msg/BatteryState")
            DocBullet("Fields", "Level (0–1), Temperature (°C), Voltage (V), IsCharging (bool)")
            DocBullet("Topic ID", "0x04 (BATTERY)")
        }

        DocCard(title = "Why Use Android Instead of Dedicated Hardware?", icon = Icons.Default.Smartphone) {
            Text(
                "• A standard Android phone includes: 6-axis IMU, GPS, magnetometer, high-resolution camera, " +
                "microphone, ambient light sensor, barometer, proximity sensor, and speaker — all in one integrated package.",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                "• Dedicated robotics sensors (LIDAR, stereo cameras, industrial IMUs) cost \$500–\$5000 each.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "• ASR makes robotics accessible to students, hobbyists, and researchers worldwide " +
                "by treating the smartphone as a first-class robotics hardware abstraction suite.",
                style = MaterialTheme.typography.bodySmall
            )
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
            containerColor = Color(0xFF141923)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF222B3D)),
        shape = RoundedCornerShape(8.dp)
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
                        modifier = Modifier.size(18.dp),
                        tint = Color(0xFF43D9C5)
                    )
                }
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFECEFF4)
                )
            }
            HorizontalDivider(color = Color(0xFF222B3D))
            content()
        }
    }
}

@Composable
private fun DocBullet(label: String, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("•", fontWeight = FontWeight.Bold, color = Color(0xFF43D9C5))
        Text(label, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFFE5E9F0))
        Text("—", color = Color(0xFF8892B0), fontSize = 13.sp)
        Text(text, fontSize = 13.sp, color = Color(0xFFCBD5E1))
    }
}

@Composable
private fun DocCodeBlock(code: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF222B3D), RoundedCornerShape(6.dp)),
        color = Color(0xFF0B0E14),
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = code,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            modifier = Modifier.padding(14.dp),
            color = Color(0xFF5EEAD4)
        )
    }
}
