package com.kiwi.manager.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Brand accents. The vivid green is reserved for compact highlights; primary
// controls use a darker green in light mode to preserve contrast.
val KiwiNeon = Color(0xFF9BEF67)
val KiwiNeonDark = Color(0xFF2E7D32)
val KiwiAccent = Color(0xFF78D64B)
val KiwiGlow = Color(0x299BEF67)
val KiwiGlowStrong = Color(0x4D9BEF67)
val KiwiGold = Color(0xFFFFD166)

// Neutral Glassmorphism Tokens
val GlassBorderLight = Color(0x20000000)
val GlassBorderDark = Color(0x22FFFFFF)
val GlassBorderOled = Color(0x1FFFFFFF)
val GlassFillSubtle = Color(0x0DFFFFFF)
val GlassFillMedium = Color(0x18FFFFFF)

// Status Action Colors
val StatusSuccess = Color(0xFF36C275)
val StatusUpdate = Color(0xFFF59E0B)
val StatusNotInstalled = Color(0xFF8A9390)
val StatusConnecting = Color(0xFF3B82F6)
val StatusError = Color(0xFFE45A5A)

// Neutral Theme Palettes (Pure Dark / OLED / Clean Light)
// Light: Neutral Clean White & Light Gray
val LightPrimary = Color(0xFF316B25)
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFDDF6CF)
val LightOnPrimaryContainer = Color(0xFF0C2A08)
val LightSecondary = Color(0xFF566350)
val LightOnSecondary = Color(0xFFFFFFFF)
val LightSecondaryContainer = Color(0xFFDAE8D3)
val LightBackground = Color(0xFFF7FAF4)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFE9EEE6)
val LightOnBackground = Color(0xFF171D15)
val LightOnSurface = Color(0xFF171D15)
val LightOnSurfaceVariant = Color(0xFF434A40)
val LightOutline = Color(0xFF747C70)
val LightOutlineVariant = Color(0xFFC4C9C0)

// Dark: Neutral Charcoal / Slate Dark
val DarkPrimary = Color(0xFF9BEF67)
val DarkOnPrimary = Color(0xFF143800)
val DarkPrimaryContainer = Color(0xFF24520E)
val DarkOnPrimaryContainer = Color(0xFFB7FF8D)
val DarkSecondary = Color(0xFFBDCBB6)
val DarkOnSecondary = Color(0xFF283326)
val DarkSecondaryContainer = Color(0xFF3E4A3B)
val DarkBackground = Color(0xFF10150F)
val DarkSurface = Color(0xFF171D16)
val DarkSurfaceVariant = Color(0xFF272E25)
val DarkOnBackground = Color(0xFFE1E6DE)
val DarkOnSurface = Color(0xFFE1E6DE)
val DarkOnSurfaceVariant = Color(0xFFC4C9C0)
val DarkOutline = Color(0xFF8E938A)
val DarkOutlineVariant = Color(0xFF434A40)

// OLED: Pure Absolute Black
val OledBackground = Color(0xFF000000)
val OledSurface = Color(0xFF0B0F0A)
val OledSurfaceVariant = Color(0xFF181E17)

// Gradients
val KiwiGradient = Brush.horizontalGradient(
    colors = listOf(KiwiNeon, StatusSuccess)
)

val GlassCardGradient = Brush.verticalGradient(
    colors = listOf(Color(0x18FFFFFF), Color(0x08FFFFFF))
)

val GlassBorderGradient = Brush.verticalGradient(
    colors = listOf(Color(0x33FFFFFF), Color(0x12FFFFFF), Color(0x20FFFFFF))
)
