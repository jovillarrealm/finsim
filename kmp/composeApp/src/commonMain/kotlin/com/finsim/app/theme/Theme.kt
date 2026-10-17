package com.finsim.app.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// finsim Core Brand Palette (Esmeralda & Menta Financiera - High Contrast & Vibrancy)
val BrandForest = Color(0xFF144436)       // Primary deep forest green: headers, hero elements & structure
val BrandDark = Color(0xFF0F2922)         // Primary text color: high-contrast dark forest
val BrandEmerald = Color(0xFF059669)      // Vibrant emerald: primary actions & positive metrics
val BrandMint = Color(0xFF10B981)         // Energetic financial mint accent
val BrandMintLight = Color(0xFFECFDF5)    // Soft mint container for badges, chips & hover backgrounds
val BrandLime = Color(0xFFD7E8A3)         // Soft lime accent
val BrandLimeDark = Color(0xFF059669)     // Focus & interactive accent
val BrandSage = Color(0xFF059669)         // Success & favorable metric green
val BrandSageLight = Color(0xFFECFDF5)    // Badge background: soft mint sage
val BrandAmber = Color(0xFFB45309)        // Warm financial amber for interest charges
val BrandAmberLight = Color(0xFFFEF3C7)   // Soft amber container
val BrandAmberText = Color(0xFF92400E)    // High contrast warning text
val BrandPlum = Color(0xFF6B4B82)         // Warm plum for insurance charges
val BrandPlumLight = Color(0xFFF5F3FF)    // Soft lavender-plum container for insurance

// Backgrounds & Neutral Surfaces
val BackgroundClean = Color(0xFFF8FAF8)   // Fresh luminous background
val BackgroundWarm = BackgroundClean      // Alias for compatibility
val SurfaceCard = Color(0xFFFFFFFF)       // Crisp white surface for cards & panels
val SurfaceWarm = SurfaceCard             // Alias for compatibility
val SurfaceHighlight = Color(0xFFF0FDF4)  // Soft emerald highlight surface for key KPIs
val BorderSubtle = Color(0xFFE2E8F0)      // Clean subtle border
val BorderSage = Color(0xFFD1DCD0)        // Soft sage line
val TextMain = Color(0xFF0F2922)          // High-contrast primary text
val TextForestMuted = Color(0xFF52665D)   // Secondary text
val BorderInput = Color(0xFFCBD5C6)       // Form input border

// Aliases for seamless component compatibility
val BrandNavy = BrandDark
val BrandBlue = BrandForest
val BrandTeal = BrandEmerald
val BrandOrange = BrandAmber
val BrandPurple = BrandPlum
val BackgroundLight = BackgroundClean
val SurfaceLight = SurfaceCard
val BorderLight = BorderSubtle
val TextMuted = TextForestMuted

private val LightColorScheme = lightColorScheme(
    primary = BrandForest,
    secondary = BrandEmerald,
    tertiary = BrandAmber,
    background = BackgroundClean,
    surface = SurfaceCard,
    surfaceVariant = SurfaceHighlight,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = TextMain,
    onSurface = TextMain,
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
