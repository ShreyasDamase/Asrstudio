package org.asr.studio.ui.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.asr.studio.ui.theme.StudioColors

/** Keep this value aligned with desktopApp's nativeDistributions.packageVersion. */
private const val STUDIO_VERSION = "1.0.1"

@Composable
fun AboutScreen() {
    Column(
        modifier = Modifier.fillMaxSize().padding(28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("About ASR Studio", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        Text(
            "ASR Studio is the desktop diagnostics and visualization application for the ASR Android sensor runtime.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = MaterialTheme.shapes.medium,
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                AboutRow("Application", "ASR Studio")
                AboutRow("Version", STUDIO_VERSION)
                AboutRow("Platform", "Desktop")
                AboutRow("Maintainer", "Shreyas Damase (https://github.com/ShreyasDamase)")
                AboutRow("Data connection", "ASR Android Runtime over TCP · port 4242")
                AboutRow("ROS 2 DDS", "Configured separately on the Android device and ROS 2 host")
            }
        }

        Text("Connection guides and ROS 2 topic references are in Documentation. Privacy Policy and Buy Me a Coffee have their own sidebar pages.",
            style = MaterialTheme.typography.bodySmall,
            color = StudioColors.textSecondary)
    }
}

@Composable
private fun AboutRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(label, modifier = Modifier.width(160.dp), style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}
