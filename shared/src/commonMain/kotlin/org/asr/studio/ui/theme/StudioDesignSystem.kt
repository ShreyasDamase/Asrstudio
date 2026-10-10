package org.asr.studio.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Shared visual tokens for every ASR Studio screen. Keep UI styling decisions here. */
object StudioColors {
    val canvas = Color(0xFF101316)
    val panel = Color(0xFF171B20)
    val panelRaised = Color(0xFF1D2329)
    val panelInset = Color(0xFF12171B)
    val border = Color(0xFF303840)
    val borderStrong = Color(0xFF46515B)

    val textPrimary = Color(0xFFE4E8EC)
    val textSecondary = Color(0xFFA4ADB6)
    val textMuted = Color(0xFF77828C)

    val accent = Color(0xFF77CFC2)
    val accentText = Color(0xFF9ADBD1)
    val accentSurface = Color(0xFF203331)
    val success = Color(0xFF78BD9B)
    val successSurface = Color(0xFF1D3029)
    val warning = Color(0xFFD4B36C)
    val warningSurface = Color(0xFF332F24)
    val error = Color(0xFFD27D7D)
    val errorSurface = Color(0xFF352626)
    val info = Color(0xFF8FB8D8)

    // Axis colors are intentionally distinct so sensor dimensions remain readable.
    val axisX = Color(0xFFE07C7C)
    val axisY = Color(0xFF78BD9B)
    val axisZ = Color(0xFF79A9D4)

    // Purpose-built visualization tones, kept here so graph colors remain coordinated.
    val plotCrosshair = accent.copy(alpha = 0.34f)
    val plotHalo = accent.copy(alpha = 0.20f)
    val plotMarker = Color(0xFF4A5362)
    val quaternionW = textPrimary
    val globeOcean = Color(0xFF102632)
    val globeLand = Color(0xFF22170D)
    val sceneFaceFront = Color(0xFF131722)
    val sceneFaceFrontEdge = Color(0xFF2A3144)
    val sceneFaceBack = Color(0xFF1E2330)
    val sceneFaceBackEdge = Color(0xFF2E374D)
    val sceneFaceSide = Color(0xFF171B26)
    val sceneFaceSideEdge = Color(0xFF262E40)
    val sceneFaceTop = Color(0xFF202636)
    val sceneFaceTopEdge = Color(0xFF333E57)
    val sceneFaceBottom = Color(0xFF10131B)
    val sceneFaceBottomEdge = Color(0xFF1C2230)
    val sceneShadow = Color(0xFF05070A)
    val sceneAccentDeep = Color(0xFF071412)
    val sceneMarkerShadow = Color(0xFF030712)
    val sceneCameraBase = Color(0xFF0F131D)
    val sceneMagenta = Color(0xFFD184B1)
    val modelNeutral = Color(0xFF94A3B8)
    val modelScreen = Color(0xFF0F172A)
    val modelPhone = Color(0xFF020617)
}

object StudioRadii {
    /** Buttons, inputs, selected navigation rows and compact status labels. */
    val control = 2.dp

    /** Panels and cards: restrained desktop corners, never pill-shaped. */
    val panel = 2.dp

    val controlShape = RoundedCornerShape(control)
    val panelShape = RoundedCornerShape(panel)

    /** Use only for small status dots. */
    val dot = 50.dp
}

object StudioSpacing {
    val xxs = 2.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val page = 24.dp
}

object StudioTypeSize {
    val caption = 11.sp
    val supporting = 12.sp
    val controlLabel = 13.sp
    val body = 14.sp
    val section = 17.sp
    val page = 21.sp
    val compactMetric = 15.sp
    val detailMetric = 18.sp
    val metric = 22.sp
}

object StudioIconSize {
    val small = 16.dp
    val navigation = 18.dp
    val standard = 20.dp
    val prominent = 24.dp
}

@Composable
fun AsrStudioTheme(content: @Composable () -> Unit) {
    val typography = Typography()
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = StudioColors.accent,
            onPrimary = StudioColors.canvas,
            secondary = StudioColors.warning,
            onSecondary = StudioColors.canvas,
            tertiary = StudioColors.info,
            background = StudioColors.canvas,
            onBackground = StudioColors.textPrimary,
            surface = StudioColors.panel,
            onSurface = StudioColors.textPrimary,
            surfaceVariant = StudioColors.panelRaised,
            onSurfaceVariant = StudioColors.textSecondary,
            outline = StudioColors.borderStrong,
            outlineVariant = StudioColors.border,
            error = StudioColors.error,
            errorContainer = StudioColors.errorSurface,
            onErrorContainer = StudioColors.textPrimary,
            primaryContainer = StudioColors.accentSurface,
            onPrimaryContainer = StudioColors.accentText,
            secondaryContainer = StudioColors.warningSurface,
            onSecondaryContainer = StudioColors.textPrimary,
        ),
        typography = typography.copy(
            titleLarge = typography.titleLarge.copy(fontSize = StudioTypeSize.page),
            titleMedium = typography.titleMedium.copy(fontSize = StudioTypeSize.section),
            headlineSmall = typography.headlineSmall.copy(fontSize = StudioTypeSize.metric),
            headlineMedium = typography.headlineMedium.copy(fontSize = 26.sp),
            bodyLarge = typography.bodyLarge.copy(fontSize = StudioTypeSize.body),
            bodyMedium = typography.bodyMedium.copy(fontSize = StudioTypeSize.body),
            bodySmall = typography.bodySmall.copy(fontSize = StudioTypeSize.supporting),
            labelLarge = typography.labelLarge.copy(fontSize = StudioTypeSize.controlLabel),
            labelMedium = typography.labelMedium.copy(fontSize = StudioTypeSize.supporting),
            labelSmall = typography.labelSmall.copy(fontSize = StudioTypeSize.caption),
        ),
        shapes = Shapes(
            extraSmall = StudioRadii.controlShape,
            small = StudioRadii.controlShape,
            medium = StudioRadii.panelShape,
            large = StudioRadii.panelShape,
            extraLarge = StudioRadii.panelShape,
        ),
        content = content,
    )
}
