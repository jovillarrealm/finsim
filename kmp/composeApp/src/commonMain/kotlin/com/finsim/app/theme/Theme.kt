package com.finsim.app.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// finsim Core Brand Palette («Warm Botanical Precision» - Stitch Project 6070787729708745637)
val BrandForest = Color(0xFF0F382A)       // Primary botanical pine: headers, buttons, structure & CTA
val BrandDark = Color(0xFF0F2922)         // Primary text color: high-contrast botanical forest
val BrandChartreuse = Color(0xFFC4F278)   // Radiant chartreuse: secondary buttons & brand dot
val BrandMint = Color(0xFF84E296)         // Financial mint: focus ring & interactive highlights
val BrandEmerald = Color(0xFF0D9488)      // Extra amortization & favorable metrics
val BrandMintLight = Color(0xFFECFDF5)    // Soft mint container for badges & chips
val BrandLime = BrandChartreuse           // Alias for backward compatibility
val BrandLimeDark = Color(0xFF607848)     // Subheading text accent
val BrandSage = Color(0xFF065F46)         // Success text
val BrandSageLight = Color(0xFFECFDF5)    // Badge background: soft mint sage
val BrandAmber = Color(0xFFD97706)        // Terracotta amber for interest charges
val BrandAmberLight = Color(0xFFFEF3C7)   // Soft amber container
val BrandAmberText = Color(0xFF92400E)    // High contrast warning text
val BrandPlum = Color(0xFF7C3AED)         // Amethyst violet for insurance charges
val BrandPlumLight = Color(0xFFFAF5FF)    // Soft violet container for insurance

// Semantic Flow & Waterfall Colors
val FlowRegularPrincipal = Color(0xFF2563EB)
val FlowRegularPrincipalDark = Color(0xFF1D4ED8)
val FlowExtraPrincipal = Color(0xFF0D9488)
val FlowExtraPrincipalDark = Color(0xFF059669)
val FlowInterest = Color(0xFFD97706)
val FlowInterestDark = Color(0xFFEA580C)
val FlowInsurance = Color(0xFF7C3AED)
val FlowInsuranceDark = Color(0xFF8B5CF6)
val FlowCursor = Color(0xFF111827)

// Backgrounds & Neutral Surfaces
val BackgroundClean = Color(0xFFF8F9F5)   // Warm porcelain ground
val BackgroundWarm = BackgroundClean      // Alias for compatibility
val SurfaceCard = Color(0xFFFFFFFF)       // Crisp white surface for cards & panels
val SurfaceWarm = SurfaceCard             // Alias for compatibility
val SurfaceHighlight = Color(0xFFF0FDF4)  // Soft mint highlight surface for key KPIs
val BorderSubtle = Color(0xFFE2E8DF)      // Hairline sage border
val BorderSage = Color(0xFFE2E8DF)        // Soft sage line
val TextMain = Color(0xFF0F2922)          // High-contrast primary text
val TextForestMuted = Color(0xFF52665D)   // Secondary text
val BorderInput = Color(0xFFCBD5C6)       // Form input border

// Thematic Semantic Tint Containers & Borders
val KpiMintBg = Color(0xFFECFDF5)
val KpiMintBorder = Color(0xFFA7F3D0)
val KpiMintText = Color(0xFF065F46)

val KpiSkyBg = Color(0xFFF0F9FF)
val KpiSkyBorder = Color(0xFFBAE6FD)
val KpiSkyText = Color(0xFF0369A1)

val KpiAmberBg = Color(0xFFFFFBEB)
val KpiAmberBorder = Color(0xFFFDE68A)
val KpiAmberText = Color(0xFF92400E)

val KpiPlumBg = Color(0xFFFAF5FF)
val KpiPlumBorder = Color(0xFFE9D5FF)
val KpiPlumText = Color(0xFF6B21A8)

val TableZebraRow = Color(0xFFF9FBFA)
val TableHeaderBg = Color(0xFFE6ECE2)

// Aliases for seamless component compatibility
val BrandNavy = BrandDark
val BrandBlue = FlowRegularPrincipal
val BrandTeal = FlowExtraPrincipal
val BrandOrange = FlowInterest
val BrandPurple = FlowInsurance
val BackgroundLight = BackgroundClean
val SurfaceLight = SurfaceCard
val BorderLight = BorderSubtle
val TextMuted = TextForestMuted

private val LightColorScheme = lightColorScheme(
    primary = BrandForest,
    secondary = BrandChartreuse,
    tertiary = BrandMint,
    background = BackgroundClean,
    surface = SurfaceCard,
    surfaceVariant = SurfaceHighlight,
    onPrimary = Color.White,
    onSecondary = BrandDark,
    onBackground = TextMain,
    onSurface = TextMain,
    outline = BorderSage
)

private val defaultTypography = Typography()

val FinsimTypography = Typography(
    displayLarge = defaultTypography.displayLarge.copy(fontFeatureSettings = "tnum"),
    displayMedium = defaultTypography.displayMedium.copy(fontFeatureSettings = "tnum"),
    displaySmall = defaultTypography.displaySmall.copy(fontFeatureSettings = "tnum"),
    headlineLarge = defaultTypography.headlineLarge.copy(fontFeatureSettings = "tnum"),
    headlineMedium = defaultTypography.headlineMedium.copy(fontFeatureSettings = "tnum"),
    headlineSmall = defaultTypography.headlineSmall.copy(fontFeatureSettings = "tnum"),
    titleLarge = defaultTypography.titleLarge.copy(fontFeatureSettings = "tnum"),
    titleMedium = defaultTypography.titleMedium.copy(fontFeatureSettings = "tnum"),
    titleSmall = defaultTypography.titleSmall.copy(fontFeatureSettings = "tnum"),
    bodyLarge = defaultTypography.bodyLarge.copy(fontFeatureSettings = "tnum"),
    bodyMedium = defaultTypography.bodyMedium.copy(fontFeatureSettings = "tnum"),
    bodySmall = defaultTypography.bodySmall.copy(fontFeatureSettings = "tnum"),
    labelLarge = defaultTypography.labelLarge.copy(fontFeatureSettings = "tnum"),
    labelMedium = defaultTypography.labelMedium.copy(fontFeatureSettings = "tnum"),
    labelSmall = defaultTypography.labelSmall.copy(fontFeatureSettings = "tnum")
)

@Composable
fun FinsimTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = FinsimTypography,
        content = content
    )
}
