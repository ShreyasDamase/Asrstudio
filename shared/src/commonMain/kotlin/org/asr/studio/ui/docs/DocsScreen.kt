package org.asr.studio.ui.docs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private enum class DocSection(
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    GettingStarted("Getting Started", Icons.Default.Description),
    Connectivity("Connectivity (Wi-Fi & ADB)", Icons.Default.Router),
    HardwareModules("Hardware Modules", Icons.Default.Memory),
    AdbReference("ADB Command Reference", Icons.Default.Code),
    Ros2Integration("ROS 2 & Protocol", Icons.Default.VolumeUp)
}

@Composable
fun DocsScreen() {
    var selectedSection by remember { mutableStateOf(DocSection.GettingStarted) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Section Selector Tabs ────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DocSection.entries.forEach { section ->
                FilterChip(
                    selected = selectedSection == section,
                    onClick = { selectedSection = section },
                    label = {
                        Text(
                            text = section.title,
                            fontSize = 12.sp,
                            fontWeight = if (selectedSection == section) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

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
            }
        }
    }
}

// ── 1. Getting Started Document ──────────────────────────────────────────────
@Composable
private fun GettingStartedDoc() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("🚀 ASR System Architecture & Getting Started", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("Android Sensor Runtime (ASR) acts as the low-latency hardware abstraction layer for mobile robotics. ASR Studio is the laboratory instrument used to monitor telemetry, render live camera frames, inspect binary packets, and issue high-level hardware commands.", style = MaterialTheme.typography.bodyMedium)

        DocCard(title = "System Component Overview") {
            DocBullet("Android Sensor Runtime (cameraros)", "Runs on mobile Android device. Captures raw CameraX frames, IMU (Accel, Gyro, Quaternion), GPS, Battery, and Speaker TTS.")
            DocBullet("ASR Studio (asrstudio)", "Desktop engineering workstation built with Compose Multiplatform. Renders live camera feed, graph telemetry, and auto-configures USB ADB bridges.")
            DocBullet("Binary Protocol (ASR-TCP)", "Standardized IEEE 802.3 CRC32-validated binary protocol running over TCP port 4242.")
        }

        DocCard(title = "Quickstart Workflow") {
            DocCodeBlock("""
# 1. Build and install ASR Runtime on Android device
./gradlew :app:installDebug

# 2. Launch ASR Studio Desktop App
./gradlew :shared:run

# 3. Connect via USB ADB (Recommended)
adb reverse tcp:4242 tcp:4242

# 4. In ASR Studio -> Devices -> Click "Listen (Server) ★"
# 5. On Android Phone -> Tap START -> Tap CONNECT
            """.trimIndent())
        }
    }
}

// ── 2. Connectivity Document ──────────────────────────────────────────────────
@Composable
private fun ConnectivityDoc() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("📡 Connectivity: Wi-Fi vs. USB / ADB Mode", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("ASR supports dual transport layers: Local Wi-Fi (wireless mobility) and USB / ADB (zero-latency physical cable connection).", style = MaterialTheme.typography.bodyMedium)

        DocCard(title = "Wi-Fi Mode (LAN)") {
            Text("1. Ensure Desktop and Android phone are connected to the same Wi-Fi network.")
            Text("2. In ASR Studio -> Select 'Wi-Fi' tab -> Click 'Listen (Server) ★'.")
            Text("3. On Android phone -> Enter Desktop IP (e.g. 192.168.1.42) -> Tap CONNECT.")
        }

        DocCard(title = "USB / ADB Mode (Reverse TCP Forwarding)") {
            DocCodeBlock("""
# Why use ADB Reverse?
# adb reverse tcp:4242 tcp:4242 forwards connections originating from 
# Android phone (127.0.0.1:4242) over USB cable to ASR Studio listening on Mac port 4242.

# Command to configure ADB Reverse:
adb reverse tcp:4242 tcp:4242

# Clear stale/conflicting rules if necessary:
adb forward --remove-all
adb reverse --remove-all
adb reverse tcp:4242 tcp:4242
            """.trimIndent())
        }

        DocCard(title = "1-Click Auto Setup in ASR Studio") {
            Text("In ASR Studio, open Devices -> USB / ADB tab -> Click '⚡ Auto-Configure USB / ADB (1-Click Setup)'. ASR Studio automatically cleans stale rules and configures adb reverse for you.")
        }
    }
}

// ── 3. Hardware Modules Document ─────────────────────────────────────────────
@Composable
private fun HardwareModulesDoc() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("🔊 Hardware Modules Reference", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        DocCard(title = "📷 Camera Module (id: 'camera')") {
            DocBullet("Resolution", "640x480 YUV420_888 converted to NV21 with Stride-aware memory alignment.")
            DocBullet("Encoding", "JPEG (Quality 75, ~25 KB/frame at 30 FPS).")
            DocBullet("Topic ID", "0x01 (CAMERA_FRAME)")
        }

        DocCard(title = "🧠 IMU Module (id: 'imu')") {
            DocBullet("Sensors", "Accelerometer (m/s²), Gyroscope (rad/s), Gravity Vector, Rotation Vector.")
            DocBullet("Quaternion", "Full 4-component Quaternion [w, x, y, z] output at 100 Hz.")
            DocBullet("Topic ID", "0x02 (IMU)")
        }

        DocCard(title = "🔊 Speaker Module (id: 'speaker')") {
            DocBullet("Engine", "Android Text-To-Speech (TTS) + UtteranceProgressListener.")
            DocBullet("Commands", "speaker.speak (JSON: {\"cmd\":\"speaker.speak\",\"params\":{\"text\":\"...\"}})")
            DocBullet("Intent Action", "org.asr.runtime.SPEAK (--es text \"...\")")
        }

        DocCard(title = "🎤 Audio Microphone Module (id: 'audio')") {
            DocBullet("Format", "16 kHz Mono PCM 16-bit Signed Little Endian.")
        }

        DocCard(title = "📍 GPS Module (id: 'gps')") {
            DocBullet("Fields", "Latitude, Longitude, Altitude (m ASL), Accuracy (m), Bearing, Speed (m/s).")
        }
    }
}

// ── 4. ADB Command Reference ─────────────────────────────────────────────────
@Composable
private fun AdbReferenceDoc() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("💻 Complete ADB Terminal Command Reference", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("Copy-pasteable ADB CLI commands to control, query, and test the ASR Runtime on Android.", style = MaterialTheme.typography.bodyMedium)

        DocCard(title = "1. Runtime Lifecycle Commands") {
            DocCodeBlock("""
# Start ASR Service (Foreground)
adb shell "am start-foreground-service -a org.asr.runtime.START org.asr.runtime/org.asr.runtime.core.AsrService"

# Stop ASR Service
adb shell "am start-foreground-service -a org.asr.runtime.STOP org.asr.runtime/org.asr.runtime.core.AsrService"

# Restart ASR Service
adb shell "am start-foreground-service -a org.asr.runtime.RESTART org.asr.runtime/org.asr.runtime.core.AsrService"
            """.trimIndent())
        }

        DocCard(title = "2. Enable Profiles") {
            DocCodeBlock("""
# Enable Full Sensor Profile (Camera + IMU + GPS + Audio + Speaker + Battery)
adb shell "am start-foreground-service -a org.asr.runtime.PROFILE_ENABLE --es profile full_sensor org.asr.runtime/org.asr.runtime.core.AsrService"

# Enable Teleop Profile (Camera + Audio + Speaker)
adb shell "am start-foreground-service -a org.asr.runtime.PROFILE_ENABLE --es profile teleop org.asr.runtime/org.asr.runtime.core.AsrService"

# Enable Mapping Profile (Camera + IMU)
adb shell "am start-foreground-service -a org.asr.runtime.PROFILE_ENABLE --es profile mapping org.asr.runtime/org.asr.runtime.core.AsrService"
            """.trimIndent())
        }

        DocCard(title = "3. Individual Sensor Module Control") {
            DocCodeBlock("""
# Start Camera Module
adb shell "am start-foreground-service -a org.asr.runtime.MODULE_START --es module_id camera org.asr.runtime/org.asr.runtime.core.AsrService"

# Start Speaker Module
adb shell "am start-foreground-service -a org.asr.runtime.MODULE_START --es module_id speaker org.asr.runtime/org.asr.runtime.core.AsrService"

# Stop Camera Module
adb shell "am start-foreground-service -a org.asr.runtime.MODULE_STOP --es module_id camera org.asr.runtime/org.asr.runtime.core.AsrService"
            """.trimIndent())
        }

        DocCard(title = "4. Speaker Text-To-Speech (TTS) Output") {
            DocCodeBlock("""
# Trigger Speech Intent
adb shell "am start-foreground-service -a org.asr.runtime.SPEAK --es text 'ASR speaker test successful' org.asr.runtime/org.asr.runtime.core.AsrService"

# Trigger Obstacle Warning Speech
adb shell "am start-foreground-service -a org.asr.runtime.SPEAK --es text 'Warning. Obstacle detected.' org.asr.runtime/org.asr.runtime.core.AsrService"
            """.trimIndent())
        }

        DocCard(title = "5. Verification & Logcat Diagnostics") {
            DocCodeBlock("""
# View Active Modules & Speaker Logs
adb logcat -d -t 150 | grep -iE "(AsrSpeaker|AsrCamera|AsrService|Modules)"

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
        Text("⚡ ROS 2 Integration & Binary Protocol Specification", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        DocCard(title = "Binary Frame Layout (28-Byte Header)") {
            DocCodeBlock("""
Offset  Type     Field          Description
0-1     UInt8[2] Magic          'A', 'S' (0x41, 0x53)
2       UInt8    Version        Protocol Version (0x01)
3       UInt8    TopicId        0x01=Camera, 0x02=IMU, 0x03=Command, 0x04=GPS, 0x06=Speaker
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

        DocCard(title = "ROS 2 Node Topic Mapping") {
            DocBullet("/asr/camera/compressed", "sensor_msgs/msg/CompressedImage (JPEG)")
            DocBullet("/asr/imu/data", "sensor_msgs/msg/Imu (Accel + Gyro + Quaternion)")
            DocBullet("/asr/gps/fix", "sensor_msgs/msg/NavSatFix")
            DocBullet("/asr/speaker/goal", "std_msgs/msg/String (TTS Speech Command)")
        }
    }
}

// ── Helper UI Components ─────────────────────────────────────────────────────
@Composable
private fun DocCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            content()
        }
    }
}

@Composable
private fun DocBullet(label: String, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("•", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(label, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Text("—", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        Text(text, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun DocCodeBlock(code: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(6.dp)),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = code,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            modifier = Modifier.padding(12.dp),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
