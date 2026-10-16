package org.asr.studio.ui.privacy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.asr.studio.openExternalUrl

private const val STUDIO_REPOSITORY_URL = "https://github.com/ShreyasDamase/Asrstudio"

@Composable
fun PrivacyPolicyScreen() {
    var repositoryOpenError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("ASR Studio Privacy Policy", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            "This policy describes ASR Studio, the desktop diagnostics application. It does not describe ASR Android Runtime or the optional ROS 2 USB Link Monitor.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        PolicySection("Data handled by Studio") {
            PolicyParagraph("When you connect Studio to an Android device, Studio receives the ASR packets sent to the TCP endpoint you configure. Depending on enabled Android modules, packet payloads can include camera frames, location, motion sensors, microphone levels, raw microphone audio, battery state, and device diagnostics. Studio displays and processes this information locally.")
            PolicyParagraph("Studio keeps a rolling in-memory history of up to 1,000 received packets for its packet inspector and diagnostics. The packet history is not written to disk by Studio and is cleared when the application exits. Screens can also hold recent samples or the current camera frame in memory while they are open.")
            PolicyParagraph("If you enable live speaker monitoring, raw microphone audio received from the phone is played through the computer's audio output. Studio sends user-requested controls and text-to-speech commands to the phone over the same TCP connection.")
            PolicyParagraph("Studio does not include an analytics or telemetry upload service. Its TCP connection is directly between Studio and the endpoint you choose; Studio is not a cloud relay or ROS 2 DDS bridge. Connection and packet metadata and errors may be written to the process console.")
        }

        PolicySection("Preferences") {
            PolicyParagraph("Studio saves the light/dark theme choice in the local computer's Java Preferences store. The application does not save its packet history there.")
        }

        PolicySection("Network security") {
            PolicyParagraph("The ASR TCP protocol used by Studio does not provide application-level encryption or authentication. In listen mode, Studio defaults to binding to 0.0.0.0:4242, which can make it reachable through the computer's network interfaces. Use a trusted network, configure the host and port deliberately, and stop listening when the connection is no longer needed. ADB reverse is an alternate USB route for the same TCP protocol; it does not add application-level encryption.")
        }

        PolicySection("External links") {
            PolicyParagraph("The optional Buy Me a Coffee button opens https://buymeacoffee.com/shreyasdamase in your browser. Studio does not process contributions or receive payment details. Buy Me a Coffee and any payment providers it uses handle information submitted on its site under their own policies.")
        }

        PolicySection("Changes and questions") {
            PolicyParagraph("This policy should be reviewed whenever Studio's data handling changes. For questions or corrections, use the ASR Studio GitHub repository.")
        }
        Button(onClick = { repositoryOpenError = openExternalUrl(STUDIO_REPOSITORY_URL) }) {
            Text("OPEN ASR STUDIO REPOSITORY ↗")
        }
        repositoryOpenError?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun PolicySection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                content()
            },
        )
    }
}

@Composable
private fun PolicyParagraph(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
