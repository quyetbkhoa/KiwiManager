package com.kiwi.manager.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Kiwi Signature Accents
val KiwiNeon = Color(0xFF76FF03)
val KiwiNeonDark = Color(0xFF4CAF50)
val KiwiAccent = Color(0xFF8EE53F)
val KiwiGlow = Color(0x4076FF03)
val KiwiGlowStrong = Color(0x8076FF03)
val KiwiGold = Color(0xFFFFD54F)

// Glassmorphism Token Colors
val GlassBorderLight = Color(0x3366BB2B)
val GlassBorderDark = Color(0x26FFFFFF)
val GlassBorderNeon = Color(0x5076FF03)
val GlassFillSubtle = Color(0x0DFFFFFF)
val GlassFillMedium = Color(0x1AFFFFFF)
val GlassFillCard = Color(0x1F2E3B2E)

// Status Colors
val StatusSuccess = Color(0xFF00E676)
val StatusUpdate = Color(0xFFFF9100)
val StatusNotInstalled = Color(0xFF78909C)
val StatusConnecting = Color(0xFF29B6F6)

// Theme Palettes
val LightPrimary = Color(0xFF43A047)
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFC8F5A0)
val LightSecondary = Color(0xFF795548)
val LightBackground = Color(0xFFF7FAF4)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFE8EDE4)

val DarkPrimary = Color(0xFF76FF03)
val DarkOnPrimary = Color(0xFF0A2200)
val DarkPrimaryContainer = Color(0xFF1E3A0E)
val DarkSecondary = Color(0xFFA1887F)
val DarkBackground = Color(0xFF0F140F)
val DarkSurface = Color(0xFF161E16)
val DarkSurfaceVariant = Color(0xFF222B22)

val OledBackground = Color(0xFF000000)
val OledSurface = Color(0xFF080C08)
val OledSurfaceVariant = Color(0xFF111711)

// Gradients
val KiwiGradient = Brush.horizontalGradient(
    colors = listOf(Color(0xFF76FF03), Color(0xFF00E676))
)

val GlassCardGradient = Brush.verticalGradient(
    colors = listOf(Color(0x22FFFFFF), Color(0x08FFFFFF))
)

val GlassBorderGradient = Brush.verticalGradient(
    colors = listOf(Color(0x6076FF03), Color(0x10FFFFFF), Color(0x0576FF03))
)
