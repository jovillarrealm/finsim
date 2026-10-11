package com.finsim.app.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val BrandNavy = Color(0xFF0F172A)
val BrandBlue = Color(0xFF2563EB)
val BrandTeal = Color(0xFF0D9488)
val BrandOrange = Color(0xFFEA580C)
val BrandPurple = Color(0xFF7C3AED)
val BackgroundLight = Color(0xFFF8FAFC)
val SurfaceLight = Color(0xFFFFFFFF)
val BorderLight = Color(0xFFE2E8F0)
val TextMuted = Color(0xFF64748B)

private val LightColorScheme = lightColorScheme(
    primary = BrandBlue,
    secondary = BrandTeal,
    tertiary = BrandOrange,
    background = BackgroundLight,
    surface = SurfaceLight,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = BrandNavy,
    onSurface = BrandNavy
)

@Composable
fun FinsimTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content
    )
}
