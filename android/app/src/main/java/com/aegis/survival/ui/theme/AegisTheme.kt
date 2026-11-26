package com.aegis.survival.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// True AMOLED Pitch Black palette for OLED battery conservation
val AmoledBlack = Color(0xFF000000)
val SurfaceElevated = Color(0xFF0B0B10)
val SurfaceInteractive = Color(0xFF12121A)
val HairlineBorder = Color(0xFF222232)

val CrimsonAccent = Color(0xFFFF4757)
val CoralPrimary = Color(0xFFFF6B6B)
val TextPrimary = Color(0xFFEAEAF2)
val TextSecondary = Color(0xFF9A9AB4)
val TextMuted = Color(0xFF6E6E8A)

val StatusOk = Color(0xFF2ED573)
val StatusWarn = Color(0xFFFFA502)
val StatusInfo = Color(0xFF1E90FF)
val StatusDanger = Color(0xFFFF4757)

private val AegisAmoledColorScheme = darkColorScheme(
    primary = CrimsonAccent,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF2A0C12),
    onPrimaryContainer = CoralPrimary,
    secondary = CoralPrimary,
    onSecondary = AmoledBlack,
    background = AmoledBlack,
    onBackground = TextPrimary,
    surface = SurfaceElevated,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceInteractive,
    onSurfaceVariant = TextSecondary,
    outline = HairlineBorder,
    outlineVariant = Color(0xFF1A1A26),
    error = StatusDanger,
    onError = Color(0xFFFFFFFF),
)

private val AegisTypography = Typography(
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.2.sp,
        color = TextPrimary,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.15.sp,
        color = TextPrimary,
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = TextPrimary,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp,
        color = TextPrimary,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.15.sp,
        color = TextSecondary,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
        color = TextSecondary,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.4.sp,
        color = TextMuted,
    ),
)

@Composable
fun AegisTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AegisAmoledColorScheme,
        typography = AegisTypography,
        content = content,
    )
}
