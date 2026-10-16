package org.asr.studio.ui.support

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.asr.studio.openExternalUrl
import org.asr.studio.ui.theme.StudioColors
import org.asr.studio.ui.theme.StudioFonts

private const val BUY_ME_A_COFFEE_URL = "https://buymeacoffee.com/shreyasdamase"
private val HeroBlack = Color(0xFF080808)
private val HeroWhite = Color(0xFFF4F4F0)
private val HeroMuted = Color(0xFFAAAAA5)

@Composable
fun SupportScreen() {
    var openError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        SupportHero(
            onSupport = { openError = openExternalUrl(BUY_ME_A_COFFEE_URL) },
        )
        openError?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        SupportDetails()
    }
}

@Composable
private fun SupportHero(onSupport: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = HeroBlack),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF292929)),
        shape = RoundedCornerShape(8.dp),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
        ) {
            val compact = maxWidth < 620.dp
            if (compact) {
                Column(verticalArrangement = Arrangement.spacedBy(22.dp)) {
                    CoffeeMugArtwork(
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        artworkSize = 204.dp,
                    )
                    SupportHeroCopy(onSupport = onSupport)
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SupportHeroCopy(modifier = Modifier.weight(1f), onSupport = onSupport)
                    CoffeeMugArtwork(artworkSize = 224.dp)
                }
            }
        }
    }
}

@Composable
private fun SupportHeroCopy(
    modifier: Modifier = Modifier,
    onSupport: () -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(StudioColors.accent, RoundedCornerShape(1.dp)),
            )
            Text(
                "ASR STUDIO  /  COMMUNITY",
                style = MaterialTheme.typography.labelSmall,
                color = HeroMuted,
                fontFamily = StudioFonts.mono,
                fontWeight = FontWeight.Bold,
            )
        }
        Text(
            "Good tools\nare worth\nkeeping alive.",
            style = MaterialTheme.typography.headlineMedium,
            color = HeroWhite,
            fontWeight = FontWeight.Bold,
            lineHeight = MaterialTheme.typography.headlineMedium.lineHeight,
        )
        Text(
            "ASR Studio is built to make device data easier to understand. If it has made your work a little easier, an optional coffee helps support the time behind its fixes and future improvements.",
            style = MaterialTheme.typography.bodyMedium,
            color = HeroMuted,
        )
        Button(onClick = onSupport) {
            Icon(Icons.Default.LocalCafe, contentDescription = null)
            Spacer(Modifier.size(10.dp))
            Text("BUY ME A COFFEE")
        }
        Text(
            "OPTIONAL · OPENS BUY ME A COFFEE",
            style = MaterialTheme.typography.labelSmall,
            color = HeroMuted,
            fontFamily = StudioFonts.mono,
        )
    }
}

@Composable
private fun CoffeeMugArtwork(
    modifier: Modifier = Modifier,
    artworkSize: androidx.compose.ui.unit.Dp,
) {
    var tapCount by remember { mutableIntStateOf(0) }
    var mugTilt by remember { mutableFloatStateOf(0f) }
    val animatedTilt by animateFloatAsState(
        targetValue = mugTilt,
        animationSpec = tween(durationMillis = 120),
        label = "coffee-mug-tilt",
    )
    val steamTransition = rememberInfiniteTransition(label = "coffee-steam")
    val steamAlpha by steamTransition.animateFloat(
        initialValue = 0.28f,
        targetValue = 0.78f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "coffee-steam-alpha",
    )

    LaunchedEffect(tapCount) {
        if (tapCount > 0) {
            mugTilt = if (tapCount % 2 == 1) 7f else -7f
            delay(180)
            mugTilt = 0f
        }
    }

    Box(
        modifier = modifier
            .size(artworkSize)
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF111111))
            .border(1.dp, Color(0xFF292929), RoundedCornerShape(4.dp))
            .semantics { contentDescription = "Animated Nothing-inspired coffee mug artwork" },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { rotationZ = animatedTilt }
                .semantics { contentDescription = "Coffee mug illustration" },
        ) {
            val dotSpacing = 16.dp.toPx()
            val dotRadius = 1.15.dp.toPx()
            val dotColor = Color.White.copy(alpha = 0.14f)
            val columns = (size.width / dotSpacing).toInt()
            val rows = (size.height / dotSpacing).toInt()
            for (row in 0..rows) {
                for (column in 0..columns) {
                    drawCircle(
                        color = dotColor,
                        radius = dotRadius,
                        center = androidx.compose.ui.geometry.Offset(
                            x = column * dotSpacing + dotSpacing / 2f,
                            y = row * dotSpacing + dotSpacing / 2f,
                        ),
                    )
                }
            }

            val mugWidth = size.width * 0.48f
            val mugHeight = size.height * 0.43f
            val mugLeft = (size.width - mugWidth) / 2f - size.width * 0.035f
            val mugTop = size.height * 0.41f
            val strokeWidth = 4.dp.toPx()
            val mugColor = HeroWhite
            val mugHandleLeft = mugLeft + mugWidth * 0.86f

            drawLine(
                color = mugColor.copy(alpha = steamAlpha),
                start = androidx.compose.ui.geometry.Offset(size.width * 0.43f, size.height * 0.3f),
                end = androidx.compose.ui.geometry.Offset(size.width * 0.46f, size.height * 0.23f),
                strokeWidth = strokeWidth * 0.65f,
                cap = StrokeCap.Square,
            )
            drawLine(
                color = mugColor.copy(alpha = steamAlpha),
                start = androidx.compose.ui.geometry.Offset(size.width * 0.56f, size.height * 0.31f),
                end = androidx.compose.ui.geometry.Offset(size.width * 0.59f, size.height * 0.22f),
                strokeWidth = strokeWidth * 0.65f,
                cap = StrokeCap.Square,
            )
            drawRoundRect(
                color = mugColor,
                topLeft = androidx.compose.ui.geometry.Offset(mugLeft, mugTop),
                size = androidx.compose.ui.geometry.Size(mugWidth, mugHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(5.dp.toPx()),
            )
            drawRoundRect(
                color = mugColor,
                topLeft = androidx.compose.ui.geometry.Offset(mugHandleLeft, mugTop + mugHeight * 0.16f),
                size = androidx.compose.ui.geometry.Size(mugWidth * 0.34f, mugHeight * 0.62f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(mugWidth * 0.16f),
                style = Stroke(width = strokeWidth),
            )
            drawRect(
                color = StudioColors.accent,
                topLeft = androidx.compose.ui.geometry.Offset(mugLeft + mugWidth * 0.16f, mugTop + mugHeight * 0.37f),
                size = androidx.compose.ui.geometry.Size(mugWidth * 0.17f, mugHeight * 0.17f),
            )
            drawRect(
                color = Color(0xFF111111),
                topLeft = androidx.compose.ui.geometry.Offset(mugLeft, mugTop + mugHeight + 6.dp.toPx()),
                size = androidx.compose.ui.geometry.Size(mugWidth, 3.dp.toPx()),
            )
        }
        Text(
            text = "ASR",
            modifier = Modifier.padding(top = artworkSize * 0.19f),
            color = Color(0xFF111111),
            fontFamily = StudioFonts.mono,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun SupportDetails() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "WHAT YOUR SUPPORT MEANS",
            style = MaterialTheme.typography.labelSmall,
            color = StudioColors.textSecondary,
            fontFamily = StudioFonts.mono,
            fontWeight = FontWeight.Bold,
        )
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val compact = maxWidth < 620.dp
            if (compact) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SupportDetailCard(
                        number = "01",
                        title = "Independent by design",
                        description = "Your support helps make room for ongoing maintenance, fixes, and thoughtful improvements.",
                    )
                    SupportDetailCard(
                        number = "02",
                        title = "Always your choice",
                        description = "ASR Studio is useful with or without a contribution. No features are locked behind support.",
                    )
                    SupportDetailCard(
                        number = "03",
                        title = "Outside the app",
                        description = "The button opens Buy Me a Coffee in your browser. Studio never handles payment details.",
                    )
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SupportDetailCard(
                        modifier = Modifier.weight(1f).heightIn(min = 180.dp),
                        number = "01",
                        title = "Independent by design",
                        description = "Your support helps make room for ongoing maintenance, fixes, and thoughtful improvements.",
                    )
                    SupportDetailCard(
                        modifier = Modifier.weight(1f).heightIn(min = 180.dp),
                        number = "02",
                        title = "Always your choice",
                        description = "ASR Studio is useful with or without a contribution. No features are locked behind support.",
                    )
                    SupportDetailCard(
                        modifier = Modifier.weight(1f).heightIn(min = 180.dp),
                        number = "03",
                        title = "Outside the app",
                        description = "The button opens Buy Me a Coffee in your browser. Studio never handles payment details.",
                    )
                }
            }
        }
    }
}

@Composable
private fun SupportDetailCard(
    modifier: Modifier = Modifier,
    number: String,
    title: String,
    description: String,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, StudioColors.border),
        shape = RoundedCornerShape(6.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Text(number, color = StudioColors.accent, fontFamily = StudioFonts.mono, fontWeight = FontWeight.Bold)
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
