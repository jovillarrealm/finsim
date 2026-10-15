package com.finsim.app.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// finsim Core Brand Palette (Natural, Warm, Amicable Financial Laboratory)
// Based on the original identity in src/styles.css: --green: #173f32, --lime: #d7e8a3, --muted: #617168, --line: #dce3d8
val BrandForest = Color(0xFF173F32)       // Original primary --green: deep pine forest
val BrandDark = Color(0xFF203D33)         // Primary text color: rich dark forest
val BrandLime = Color(0xFFD7E8A3)         // Original --lime: friendly energetic lime accent
val BrandLimeDark = Color(0xFF839954)     // Original brand-dot & focus accent: warm olive lime
val BrandSage = Color(0xFF285C38)         // Original success & favorable metric green
val BrandSageLight = Color(0xFFEAF0DF)    // Original .badge background: soft sage
val BrandAmber = Color(0xFFB45309)        // Warm financial amber for interest charges
val BrandAmberLight = Color(0xFFF9E8BC)   // Original .badge.warning background: golden wheat
val BrandAmberText = Color(0xFF6B4B0B)    // Original .badge.warning text
val BrandPlum = Color(0xFF6B4B82)         // Warm plum for insurance charges
val BrandPlumLight = Color(0xFFF4EDF7)    // Soft lavender-plum container for insurance

// Backgrounds & Neutral Surfaces
val BackgroundWarm = Color(0xFFF6F5EE)    // Original body background: warm alabaster paper
val SurfaceWarm = Color(0xFFFFFEF9)       // Original .panel: warm white surface
val SurfaceHighlight = Color(0xFFF0F5EC)  // Soft green highlight surface for key KPIs
val BorderSage = Color(0xFFDCE3D8)        // Original --line: soft sage olive line
val TextForestMuted = Color(0xFF617168)   // Original --muted: warm olive slate
val BorderInput = Color(0xFFCBD5C6)       // Form input border

// Aliases for seamless component compatibility
val BrandNavy = BrandDark
val BrandBlue = BrandForest               // Replaces default cold corporate blue with Brand Forest!
val BrandTeal = BrandSage                 // Friendly natural green
val BrandOrange = BrandAmber              // Friendly warm amber
val BrandPurple = BrandPlum               // Friendly warm plum
val BackgroundLight = BackgroundWarm
val SurfaceLight = SurfaceWarm
val BorderLight = BorderSage
val TextMuted = TextForestMuted

private val LightColorScheme = lightColorScheme(
    primary = BrandForest,
    secondary = BrandLimeDark,
    tertiary = BrandAmber,
    background = BackgroundWarm,
    surface = SurfaceWarm,
    surfaceVariant = SurfaceHighlight,
    onPrimary = Color.White,
    onSecondary = BrandDark,
    onBackground = BrandDark,
    onSurface = BrandDark,
    outline = BorderSage
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
