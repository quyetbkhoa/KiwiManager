package com.kiwi.manager.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Kiwi Signature Accents (Used for buttons, badges, icons, highlights)
val KiwiNeon = Color(0xFF76FF03)
val KiwiNeonDark = Color(0xFF43A047)
val KiwiAccent = Color(0xFF8EE53F)
val KiwiGlow = Color(0x3376FF03)
val KiwiGlowStrong = Color(0x6676FF03)
val KiwiGold = Color(0xFFFFD54F)

// Neutral Glassmorphism Tokens
val GlassBorderLight = Color(0x20000000)
val GlassBorderDark = Color(0x22FFFFFF)
val GlassBorderOled = Color(0x1FFFFFFF)
val GlassFillSubtle = Color(0x0DFFFFFF)
val GlassFillMedium = Color(0x18FFFFFF)

// Status Action Colors
val StatusSuccess = Color(0xFF00E676)
val StatusUpdate = Color(0xFFFF9100)
val StatusNotInstalled = Color(0xFF9E9E9E)
val StatusConnecting = Color(0xFF29B6F6)

// Neutral Theme Palettes (Pure Dark / OLED / Clean Light)
// Light: Neutral Clean White & Light Gray
val LightPrimary = Color(0xFF2E7D32)
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFE8F5E9)
val LightSecondary = Color(0xFF616161)
val LightBackground = Color(0xFFF8F9FA)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFEEEEEE)

// Dark: Neutral Charcoal / Slate Dark
val DarkPrimary = Color(0xFF76FF03)
val DarkOnPrimary = Color(0xFF000000)
val DarkPrimaryContainer = Color(0xFF1E281E)
val DarkSecondary = Color(0xFF9E9E9E)
val DarkBackground = Color(0xFF121212)
val DarkSurface = Color(0xFF1E1E1E)
val DarkSurfaceVariant = Color(0xFF2C2C2C)

// OLED: Pure Absolute Black
val OledBackground = Color(0xFF000000)
val OledSurface = Color(0xFF0F0F0F)
val OledSurfaceVariant = Color(0xFF1C1C1C)

// Gradients
val KiwiGradient = Brush.horizontalGradient(
    colors = listOf(Color(0xFF76FF03), Color(0xFF00E676))
)

val GlassCardGradient = Brush.verticalGradient(
    colors = listOf(Color(0x18FFFFFF), Color(0x08FFFFFF))
)

val GlassBorderGradient = Brush.verticalGradient(
    colors = listOf(Color(0x33FFFFFF), Color(0x12FFFFFF), Color(0x20FFFFFF))
)
