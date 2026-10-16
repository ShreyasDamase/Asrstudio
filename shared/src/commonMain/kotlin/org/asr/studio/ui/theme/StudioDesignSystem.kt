package org.asr.studio.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.Font
import org.asr.studio.generated.resources.Res
import org.asr.studio.generated.resources.space_grotesk
import org.asr.studio.generated.resources.space_mono

/** Shared visual tokens for every ASR Studio screen. Keep UI styling decisions here. */
object StudioColors {
    val canvas: Color @Composable get() = MaterialTheme.colorScheme.background
    val panel: Color @Composable get() = MaterialTheme.colorScheme.surface
    val panelRaised: Color @Composable get() = MaterialTheme.colorScheme.surfaceVariant
    val panelInset: Color @Composable get() = MaterialTheme.colorScheme.background
    val border: Color @Composable get() = MaterialTheme.colorScheme.outlineVariant
    val borderStrong: Color @Composable get() = MaterialTheme.colorScheme.outline
    val textPrimary: Color @Composable get() = MaterialTheme.colorScheme.onSurface
    val textSecondary: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
    val textMuted: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.68f)

    val accent = Color(0xFFD71921)
    val accentText: Color @Composable get() = MaterialTheme.colorScheme.onPrimaryContainer
    val accentSurface: Color @Composable get() = MaterialTheme.colorScheme.primaryContainer
    val success = Color(0xFF78BD9B)
    val successSurface = Color(0xFF1D3029)
    val warning = Color(0xFFD4B36C)
    val warningSurface = Color(0xFF332F24)
    val error: Color @Composable get() = MaterialTheme.colorScheme.error
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
    val quaternionW: Color @Composable get() = MaterialTheme.colorScheme.onSurface
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

object StudioFonts {
    val body: FontFamily
        @Composable get() = FontFamily(Font(Res.font.space_grotesk, FontWeight.Normal))
    val mono: FontFamily
        @Composable get() = FontFamily(Font(Res.font.space_mono, FontWeight.Normal))
}

object StudioRadii {
    /** Buttons, inputs, selected navigation rows and compact status labels. */
    val control = 4.dp

    /** Panels and cards: restrained desktop corners, never pill-shaped. */
    val panel = 12.dp

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
fun AsrStudioTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    val typography = Typography()
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = StudioColors.accent,
            onPrimary = Color.White,
            secondary = StudioColors.warning,
            onSecondary = Color(0xFF000000),
            tertiary = StudioColors.info,
            background = Color(0xFF000000),
            onBackground = Color(0xFFE8E8E8),
            surface = Color(0xFF111111),
            onSurface = Color(0xFFE8E8E8),
            surfaceVariant = Color(0xFF1A1A1A),
            onSurfaceVariant = Color(0xFF999999),
            outline = Color(0xFF333333),
            outlineVariant = Color(0xFF222222),
            error = Color(0xFFD27D7D),
            errorContainer = StudioColors.errorSurface,
            onErrorContainer = Color(0xFFE8E8E8),
            primaryContainer = Color(0xFF321416),
            onPrimaryContainer = Color(0xFFFFB3B4),
            secondaryContainer = StudioColors.warningSurface,
            onSecondaryContainer = Color(0xFFE8E8E8),
        )
    } else {
        lightColorScheme(
            primary = StudioColors.accent,
            onPrimary = Color.White,
            secondary = StudioColors.warning,
            onSecondary = Color(0xFF1A1A1A),
            tertiary = StudioColors.info,
            background = Color(0xFFF5F5F5),
            onBackground = Color(0xFF1A1A1A),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF1A1A1A),
            surfaceVariant = Color(0xFFF0F0F0),
            onSurfaceVariant = Color(0xFF666666),
            outline = Color(0xFFCCCCCC),
            outlineVariant = Color(0xFFE8E8E8),
            error = StudioColors.accent,
            errorContainer = Color(0xFFFFE6E6),
            onErrorContainer = Color(0xFF1A1A1A),
            primaryContainer = Color(0xFFFBE9E9),
            onPrimaryContainer = Color(0xFF8F1117),
            secondaryContainer = Color(0xFFFFF3D7),
            onSecondaryContainer = Color(0xFF1A1A1A),
        )
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography.copy(
            titleLarge = typography.titleLarge.copy(fontFamily = StudioFonts.body, fontSize = StudioTypeSize.page),
            titleMedium = typography.titleMedium.copy(fontFamily = StudioFonts.body, fontSize = StudioTypeSize.section),
            headlineSmall = typography.headlineSmall.copy(fontFamily = StudioFonts.mono, fontSize = StudioTypeSize.metric),
            headlineMedium = typography.headlineMedium.copy(fontFamily = StudioFonts.mono, fontSize = 26.sp),
            bodyLarge = typography.bodyLarge.copy(fontFamily = StudioFonts.body, fontSize = StudioTypeSize.body),
            bodyMedium = typography.bodyMedium.copy(fontFamily = StudioFonts.body, fontSize = StudioTypeSize.body),
            bodySmall = typography.bodySmall.copy(fontFamily = StudioFonts.body, fontSize = StudioTypeSize.supporting),
            labelLarge = typography.labelLarge.copy(fontFamily = StudioFonts.mono, fontSize = StudioTypeSize.controlLabel),
            labelMedium = typography.labelMedium.copy(fontFamily = StudioFonts.mono, fontSize = StudioTypeSize.supporting),
            labelSmall = typography.labelSmall.copy(fontFamily = StudioFonts.mono, fontSize = StudioTypeSize.caption),
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
