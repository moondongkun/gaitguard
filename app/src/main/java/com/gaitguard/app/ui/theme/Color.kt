package com.gaitguard.app.ui.theme

import androidx.compose.ui.graphics.Color

// ════════════════════════════════════════════════════════════════
//  GaitGuard — Modernized palette
//  Refined for spirit-faithful match to the Vitality mockups,
//  rebranded as GaitGuard. Both light + dark are first-class.
// ════════════════════════════════════════════════════════════════

// ── Brand ────────────────────────────────────────────────────────
val BrandNavy        = Color(0xFF002F6F)   // deep navy (hero gradient start anchor)
val BrandNavy2       = Color(0xFF0A47B5)   // mid navy
val BrandNavy3       = Color(0xFF1D67E0)   // active blue
val BrandTeal        = Color(0xFF44DDC1)   // vitality teal (accent)
val BrandTeal2       = Color(0xFF1AB89C)   // teal dark (good/healthy)
val BrandAmber       = Color(0xFFF5A623)   // amber (caution)
val BrandRed         = Color(0xFFE5484D)   // red (critical)
val BrandViolet      = Color(0xFF7B61FF)   // engine accent

// ── Light surfaces ───────────────────────────────────────────────
val LightBg               = Color(0xFFF1F4FA)
val LightSurface          = Color(0xFFFFFFFF)
val LightSurface2         = Color(0xFFFAFBFE)
val LightSurfaceElev      = Color(0xBDFFFFFF)   // ~0.74 alpha
val LightBorder           = Color(0x140F2346)   // 8% ink
val LightBorderStrong     = Color(0x240F2346)
val LightInk              = Color(0xFF0B1A33)
val LightInk2             = Color(0xFF2E3A52)
val LightInk3             = Color(0xFF5B6680)
val LightInkMute          = Color(0xFF93A0BA)
val LightAmbientA         = Color(0x1A3663E0)   // navy tint blob
val LightAmbientB         = Color(0x1244DDC1)   // teal tint blob

// ── Dark surfaces ────────────────────────────────────────────────
val DarkBg                = Color(0xFF07090E)
val DarkSurface           = Color(0xFF11151D)
val DarkSurface2          = Color(0xFF161B26)
val DarkSurfaceElev       = Color(0xBD161B26)
val DarkBorder            = Color(0x12FFFFFF)
val DarkBorderStrong      = Color(0x26FFFFFF)
val DarkInk               = Color(0xFFEDF1FA)
val DarkInk2              = Color(0xFFC5CCDC)
val DarkInk3              = Color(0xFF8A93AB)
val DarkInkMute           = Color(0xFF5A6378)
val DarkPrimary           = Color(0xFFB6CBFF)
val DarkOnPrimary         = Color(0xFF00184A)
val DarkAmbientA          = Color(0x383663E0)
val DarkAmbientB          = Color(0x1F44DDC1)

// ── Legacy aliases (so existing screens compile during migration) ──
val Primary               = BrandNavy
val OnPrimary             = Color.White
val PrimaryContainer      = BrandNavy2
val OnPrimaryContainer    = Color(0xFFD9E2FF)
val Secondary             = BrandTeal2
val OnSecondary           = Color.White
val SecondaryContainer    = Color(0xFF68FADD)
val OnSecondaryContainer  = Color(0xFF003322)
val SecondaryFixed        = Color(0xFF68FADD)
val SecondaryFixedDim     = BrandTeal
val Tertiary              = Color(0xFF453200)
val OnTertiary            = Color.White
val TertiaryContainer     = Color(0xFF614800)
val OnTertiaryContainer   = Color(0xFFFFDF9E)
val Error                 = BrandRed
val OnError               = Color.White
val ErrorContainer        = Color(0xFFFFDAD6)
val OnErrorContainer      = Color(0xFF410002)
val Surface               = LightSurface
val OnSurface             = LightInk
val SurfaceBright         = LightBg
val SurfaceContainer      = Color(0xFFECEEF1)
val SurfaceContainerLow   = Color(0xFFF2F4F7)
val SurfaceContainerHigh  = Color(0xFFE6E8EB)
val SurfaceContainerHighest = Color(0xFFE0E3E6)
val SurfaceVariant        = Color(0xFFE0E3E6)
val Background            = LightBg
val OnBackground          = LightInk
val Outline               = LightInk3
val OutlineVariant        = Color(0xFFC3C6D4)
val GaugeBackground       = Color(0xFFE6E8EB)
val GaugeSafe             = BrandTeal
val GaugeAmber            = BrandAmber
val GaugeRed              = BrandRed

// Dark legacy aliases
val DarkPrimaryContainer   = Color(0xFF003BA5)
val DarkOnPrimaryContainer = Color(0xFFD9E2FF)
val DarkSecondary          = BrandTeal
val DarkOnSecondary        = Color(0xFF003730)
val DarkSecondaryContainer = Color(0xFF005045)
val DarkSurfaceContainer   = Color(0xFF1E2125)
val DarkSurfaceContainerHigh = Color(0xFF252A2E)
val DarkBackground         = DarkBg
val DarkOnSurface          = DarkInk
val DarkOnBackground       = DarkInk
val DarkOutline            = DarkInk3
val DarkOutlineVariant     = DarkBorderStrong
val DarkError              = Color(0xFFFFB4AB)
val DarkOnError            = Color(0xFF690005)
val DarkGaugeBackground    = Color(0xFF2A2E33)
