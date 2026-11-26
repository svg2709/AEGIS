package com.aegis.survival.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Immutable
data class AegisPalette(
    val canvas: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val surfaceTinted: Color,
    val border: Color,
    val borderSubtle: Color,
    val accent: Color,
    val accentSoft: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val ok: Color,
    val warn: Color,
    val info: Color,
    val danger: Color,
    val isAmoledActive: Boolean,
    val isUltraBatterySaver: Boolean,
)

// True AMOLED Pitch Black palette — shuts off OLED subpixels completely
val AmoledPalette = AegisPalette(
    canvas = Color(0xFF000000),
    surface = Color(0xFF09090D),
    surfaceElevated = Color(0xFF111118),
    surfaceTinted = Color(0xFF180A0E),
    border = Color(0xFF20202E),
    borderSubtle = Color(0xFF14141E),
    accent = Color(0xFFFF4757),
    accentSoft = Color(0xFFFF7582),
    textPrimary = Color(0xFFF2F2F7),
    textSecondary = Color(0xFFA6A6BE),
    textMuted = Color(0xFF72728C),
    ok = Color(0xFF2ED573),
    warn = Color(0xFFFFB142),
    info = Color(0xFF38A1FF),
    danger = Color(0xFFFF4757),
    isAmoledActive = true,
    isUltraBatterySaver = false,
)

// Ultra AMOLED Battery Saver palette — activated automatically when Android Battery Saver is ON
val UltraBatterySaverAmoledPalette = AegisPalette(
    canvas = Color(0xFF000000),
    surface = Color(0xFF000000),
    surfaceElevated = Color(0xFF08080C),
    surfaceTinted = Color(0xFF100608),
    border = Color(0xFF1D1D29),
    borderSubtle = Color(0xFF13131C),
    accent = Color(0xFFE63E4E),
    accentSoft = Color(0xFFFF6B78),
    textPrimary = Color(0xFFEBEBF2),
    textSecondary = Color(0xFF9C9CB5),
    textMuted = Color(0xFF6B6B84),
    ok = Color(0xFF27C268),
    warn = Color(0xFFE89B35),
    info = Color(0xFF2E8DE0),
    danger = Color(0xFFE63E4E),
    isAmoledActive = true,
    isUltraBatterySaver = true,
)

// Refined Obsidian Dark palette — layered depth when user selects Obsidian & Battery Saver is OFF
val ObsidianPalette = AegisPalette(
    canvas = Color(0xFF090B12),
    surface = Color(0xFF111420),
    surfaceElevated = Color(0xFF181C2C),
    surfaceTinted = Color(0xFF221118),
    border = Color(0xFF272C42),
    borderSubtle = Color(0xFF1A1E30),
    accent = Color(0xFFFF4757),
    accentSoft = Color(0xFFFF7A85),
    textPrimary = Color(0xFFF5F6FA),
    textSecondary = Color(0xFFADB2CC),
    textMuted = Color(0xFF787E9C),
    ok = Color(0xFF32E07D),
    warn = Color(0xFFFFB74D),
    info = Color(0xFF47A8FF),
    danger = Color(0xFFFF4F5E),
    isAmoledActive = false,
    isUltraBatterySaver = false,
)

val LocalAegisPalette = staticCompositionLocalOf { AmoledPalette }

@Composable
fun AegisTheme(
    useAmoled: Boolean = true,
    batterySaverActive: Boolean = false,
    fontScale: Float = 1.0f,
    content: @Composable () -> Unit,
) {
    val palette = when {
        batterySaverActive -> UltraBatterySaverAmoledPalette
        useAmoled -> AmoledPalette
        else -> ObsidianPalette
    }

    val colorScheme = darkColorScheme(
        primary = palette.accent,
        onPrimary = Color.White,
        primaryContainer = palette.surfaceTinted,
        onPrimaryContainer = palette.accentSoft,
        secondary = palette.accentSoft,
        onSecondary = palette.canvas,
        background = palette.canvas,
        onBackground = palette.textPrimary,
        surface = palette.surface,
        onSurface = palette.textPrimary,
        surfaceVariant = palette.surfaceElevated,
        onSurfaceVariant = palette.textSecondary,
        outline = palette.border,
        outlineVariant = palette.borderSubtle,
        error = palette.danger,
        onError = Color.White,
    )

    val scale = fontScale.coerceIn(0.88f, 1.32f)
    val typography = Typography(
        headlineMedium = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold,
            fontSize = (24 * scale).sp,
            lineHeight = (31 * scale).sp,
            letterSpacing = (-0.2).sp,
            color = palette.textPrimary,
        ),
        headlineSmall = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold,
            fontSize = (20 * scale).sp,
            lineHeight = (27 * scale).sp,
            letterSpacing = 0.sp,
            color = palette.textPrimary,
        ),
        titleMedium = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.SemiBold,
            fontSize = (16 * scale).sp,
            lineHeight = (22 * scale).sp,
            letterSpacing = 0.1.sp,
            color = palette.textPrimary,
        ),
        titleSmall = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.SemiBold,
            fontSize = (14.5f * scale).sp,
            lineHeight = (20 * scale).sp,
            letterSpacing = 0.1.sp,
            color = palette.textPrimary,
        ),
        bodyLarge = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Normal,
            fontSize = (15.5f * scale).sp,
            lineHeight = (25.5f * scale).sp,
            letterSpacing = 0.15.sp,
            color = palette.textPrimary,
        ),
        bodyMedium = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Normal,
            fontSize = (14 * scale).sp,
            lineHeight = (22 * scale).sp,
            letterSpacing = 0.15.sp,
            color = palette.textSecondary,
        ),
        labelMedium = TextStyle(
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.35.sp,
            color = palette.textSecondary,
        ),
        labelSmall = TextStyle(
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = 15.sp,
            letterSpacing = 0.35.sp,
            color = palette.textMuted,
        ),
    )

    CompositionLocalProvider(LocalAegisPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content,
        )
    }
}
