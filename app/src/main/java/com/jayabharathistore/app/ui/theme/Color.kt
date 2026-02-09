package com.jayabharathistore.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush

// Primary Brand Colors - Modern Deep Purple (Zepto/Blinkit style vibe)
// Primary Brand Colors - Modern Deep Purple (Zepto/Blinkit style vibe)
val PrimaryPurple = Color(0xFF4F46E5) // Indigo 600 - More modern than Deep Violet
val PrimaryPurpleDark = Color(0xFF3730A3) // Indigo 800
val PrimaryPurpleLight = Color(0xFF818CF8) // Indigo 400
val PrimaryPurpleSurface = Color(0xFFEEF2FF) // Indigo 50

// Accent Colors - Vibrant Orange & Coral for CTAs
val AccentOrange = Color(0xFFFF6D00) // High visibility orange
val AccentOrangeDark = Color(0xFFE65100)
val AccentOrangeLight = Color(0xFFFF9E80)
val AccentOrangeSurface = Color(0xFFFFF3E0)
val AccentCoral = Color(0xFFFF7F50) // Coral color

// Secondary Accents - For badges, highlights
val SecondaryTeal = Color(0xFF14B8A6) // Teal 500
val SecondaryPink = Color(0xFFEC4899) // Pink 500
val SecondaryYellow = Color(0xFFF59E0B) // Amber 500

// Neutral Colors - Slate Scale for better contrast
val Neutral900 = Color(0xFF0F172A) // Slate 900
val Neutral800 = Color(0xFF1E293B)
val Neutral700 = Color(0xFF334155)
val Neutral600 = Color(0xFF475569)
val Neutral500 = Color(0xFF64748B) // Secondary text
val Neutral400 = Color(0xFF94A3B8) // Placeholders
val Neutral300 = Color(0xFFCBD5E1) // Borders
val Neutral200 = Color(0xFFE2E8F0) // Dividers
val Neutral100 = Color(0xFFF1F5F9) // Page background
val Neutral50 = Color(0xFFF8FAFC)
val White = Color(0xFFFFFFFF)

// Dark Theme Colors
val BackgroundDark = Color(0xFF0B0F19)
val SurfaceDark = Color(0xFF111827)
val TextPrimaryDark = White

// Text Colors on Dark Backgrounds (Light Text)
val TextPrimaryLight = White
val TextSecondaryLight = White.copy(alpha = 0.8f)

// Text Colors on Light Backgrounds (Dark Text)
val TextPrimaryDarkText = Neutral900
val TextSecondaryDarkText = Neutral500

// Status Colors
val SuccessGreen = Color(0xFF10B981) // Emerald 500
val SuccessGreenSurface = Color(0xFFD1FAE5)
val WarningYellow = Color(0xFFF59E0B) // Amber 500
val WarningYellowSurface = Color(0xFFFEF3C7)
val ErrorRed = Color(0xFFEF4444) // Red 500
val ErrorRedSurface = Color(0xFFFEE2E2)
val InfoBlue = Color(0xFF3B82F6) // Blue 500
val InfoBlueSurface = Color(0xFFDBEAFE)

// Gradient Colors - Premium Visusals
val PurpleGradientStart = Color(0xFF6366F1) // Indigo 500
val PurpleGradientEnd = Color(0xFF8B5CF6) // Violet 500
val OrangeGradientStart = Color(0xFFFF9100)
val OrangeGradientEnd = Color(0xFFFFAB40)
val DarkGradientStart = Color(0xFF0F172A)
val DarkGradientEnd = Color(0xFF1E293B)
val PrimaryGradient = Brush.horizontalGradient(listOf(PurpleGradientStart, PurpleGradientEnd))
val CardGradient = Brush.verticalGradient(listOf(Color.White, Color(0xFFF8FAFC)))

// Glassmorphism
val GlassWhite = Color.White.copy(alpha = 0.95f)
val GlassGrey = Neutral100.copy(alpha = 0.8f)

// Semantic Aliases
val TextPrimary = Neutral900
val TextSecondary = Neutral500
val TextTertiary = Neutral400
val BorderColor = Neutral200
val BackgroundLight = Neutral50
val SurfaceWhite = White

// Legacy colors for compatibility
val Purple80 = PrimaryPurpleLight
val PurpleGrey80 = Neutral300
val Pink80 = AccentOrangeLight

val Purple40 = PrimaryPurple
val PurpleGrey40 = Neutral600
val Pink40 = AccentOrange
