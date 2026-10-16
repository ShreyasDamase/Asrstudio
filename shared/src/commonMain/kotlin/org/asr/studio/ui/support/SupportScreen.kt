package org.asr.studio.ui.support

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.asr.studio.openExternalUrl
import org.asr.studio.ui.theme.StudioColors

private const val BUY_ME_A_COFFEE_URL = "https://buymeacoffee.com/shreyasdamase"

@Composable
fun SupportScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Buy Me a Coffee", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            "Optional support helps the maintainer continue improving ASR Studio. Contributions are handled by Buy Me a Coffee, not by this app.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        CoffeeSupportCard()
    }
}

@Composable
private fun CoffeeSupportCard() {
    var tapCount by remember { mutableIntStateOf(0) }
    var mugTilt by remember { mutableFloatStateOf(0f) }
    var openError by remember { mutableStateOf<String?>(null) }
    val animatedTilt by animateFloatAsState(
        targetValue = mugTilt,
        animationSpec = tween(durationMillis = 90),
        label = "coffee-mug-tilt",
    )

    LaunchedEffect(tapCount) {
        if (tapCount > 0) {
            mugTilt = if (tapCount % 2 == 1) 10f else -10f
            delay(180)
            mugTilt = 0f
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = StudioColors.panelInset),
        border = androidx.compose.foundation.BorderStroke(1.dp, StudioColors.border),
        shape = MaterialTheme.shapes.medium,
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            val compact = maxWidth < 400.dp
            if (compact) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    CoffeeMugIllustration(animatedTilt, StudioColors.textPrimary, StudioColors.accent)
                    CoffeeSupportCopy(
                        onSupport = {
                            tapCount += 1
                            openError = openExternalUrl(BUY_ME_A_COFFEE_URL)
                        },
                        openError = openError,
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CoffeeMugIllustration(animatedTilt, StudioColors.textPrimary, StudioColors.accent)
                    CoffeeSupportCopy(
                        modifier = Modifier.weight(1f),
                        onSupport = {
                            tapCount += 1
                            openError = openExternalUrl(BUY_ME_A_COFFEE_URL)
                        },
                        openError = openError,
                    )
                }
            }
        }
    }
}

@Composable
private fun CoffeeMugIllustration(tilt: Float, foreground: Color, accent: Color) {
    Canvas(
        modifier = Modifier
            .size(76.dp)
            .graphicsLayer { rotationZ = tilt }
            .semantics { contentDescription = "Coffee mug illustration" },
    ) {
        val strokeWidth = 3.dp.toPx()
        val mugLeft = size.width * 0.2f
        val mugTop = size.height * 0.34f
        val mugWidth = size.width * 0.5f
        val mugHeight = size.height * 0.48f
        drawLine(
            color = foreground,
            start = androidx.compose.ui.geometry.Offset(size.width * 0.34f, size.height * 0.2f),
            end = androidx.compose.ui.geometry.Offset(size.width * 0.39f, size.height * 0.1f),
            strokeWidth = strokeWidth,
        )
        drawLine(
            color = foreground,
            start = androidx.compose.ui.geometry.Offset(size.width * 0.52f, size.height * 0.2f),
            end = androidx.compose.ui.geometry.Offset(size.width * 0.57f, size.height * 0.1f),
            strokeWidth = strokeWidth,
        )
        drawRoundRect(
            color = foreground,
            topLeft = androidx.compose.ui.geometry.Offset(mugLeft, mugTop),
            size = androidx.compose.ui.geometry.Size(mugWidth, mugHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()),
        )
        drawRoundRect(
            color = foreground,
            topLeft = androidx.compose.ui.geometry.Offset(mugLeft + mugWidth * 0.88f, mugTop + mugHeight * 0.16f),
            size = androidx.compose.ui.geometry.Size(mugWidth * 0.38f, mugHeight * 0.62f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(mugWidth * 0.16f),
            style = Stroke(width = strokeWidth),
        )
        drawLine(
            color = accent,
            start = androidx.compose.ui.geometry.Offset(mugLeft + mugWidth * 0.18f, mugTop + mugHeight * 0.56f),
            end = androidx.compose.ui.geometry.Offset(mugLeft + mugWidth * 0.78f, mugTop + mugHeight * 0.56f),
            strokeWidth = strokeWidth,
        )
    }
}

@Composable
private fun CoffeeSupportCopy(
    modifier: Modifier = Modifier,
    onSupport: () -> Unit,
    openError: String?,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("KEEP GOOD TOOLS BREWING", style = MaterialTheme.typography.labelSmall, color = StudioColors.accent, fontWeight = FontWeight.Bold)
        Text("Support ASR Studio", style = MaterialTheme.typography.titleMedium, color = StudioColors.textPrimary, fontWeight = FontWeight.Bold)
        Text(
            "If Studio helps your work, you can support its maintainer with an optional coffee.",
            style = MaterialTheme.typography.bodySmall,
            color = StudioColors.textSecondary,
        )
        Button(onClick = onSupport) {
            Icon(Icons.Default.LocalCafe, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text("BUY ME A COFFEE ↗")
        }
        openError?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
    }
}
