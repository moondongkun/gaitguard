package com.gaitguard.app.ui.theme

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Tokens that don't fit neatly into Material's ColorScheme — ambient blobs,
 * glass-pane fill, refined borders, brand accents — exposed via
 * MaterialTheme.gg.* so screens always read the right value for the current mode.
 */
@Immutable
data class GgTokens(
    val ambientA: androidx.compose.ui.graphics.Color,
    val ambientB: androidx.compose.ui.graphics.Color,
    val surfaceElev: androidx.compose.ui.graphics.Color,
    val border: androidx.compose.ui.graphics.Color,
    val borderStrong: androidx.compose.ui.graphics.Color,
    val ink: androidx.compose.ui.graphics.Color,
    val ink2: androidx.compose.ui.graphics.Color,
    val ink3: androidx.compose.ui.graphics.Color,
    val inkMute: androidx.compose.ui.graphics.Color,
    val good: androidx.compose.ui.graphics.Color = BrandTeal2,
    val warn: androidx.compose.ui.graphics.Color = BrandAmber,
    val bad:  androidx.compose.ui.graphics.Color = BrandRed,
    val violet: androidx.compose.ui.graphics.Color = BrandViolet,
    val isDark: Boolean,
)

private val LightTokens = GgTokens(
    ambientA = LightAmbientA, ambientB = LightAmbientB,
    surfaceElev = LightSurfaceElev,
    border = LightBorder, borderStrong = LightBorderStrong,
    ink = LightInk, ink2 = LightInk2, ink3 = LightInk3, inkMute = LightInkMute,
    isDark = false
)
private val DarkTokens = GgTokens(
    ambientA = DarkAmbientA, ambientB = DarkAmbientB,
    surfaceElev = DarkSurfaceElev,
    border = DarkBorder, borderStrong = DarkBorderStrong,
    ink = DarkInk, ink2 = DarkInk2, ink3 = DarkInk3, inkMute = DarkInkMute,
    isDark = true
)

val LocalGgTokens = staticCompositionLocalOf { LightTokens }

/** `MaterialTheme.gg` — short read-site accessor. */
val MaterialTheme.gg: GgTokens
    @Composable get() = LocalGgTokens.current

// ── Dark-mode pref glue (unchanged) ─────────────────────────────────────────
val LocalDarkMode = staticCompositionLocalOf { false }
fun isDarkModeEnabled(context: Context): Boolean =
    context.getSharedPreferences("gaitguard_settings", Context.MODE_PRIVATE)
        .getBoolean("dark_mode", false)
fun setDarkModeEnabled(context: Context, enabled: Boolean) =
    context.getSharedPreferences("gaitguard_settings", Context.MODE_PRIVATE)
        .edit().putBoolean("dark_mode", enabled).apply()

// ── Material color schemes ──────────────────────────────────────────────────
private val LightColorScheme = lightColorScheme(
    primary              = BrandNavy,
    onPrimary            = androidx.compose.ui.graphics.Color.White,
    primaryContainer     = BrandNavy2,
    onPrimaryContainer   = androidx.compose.ui.graphics.Color(0xFFE5EDFF),
    secondary            = BrandTeal2,
    onSecondary          = androidx.compose.ui.graphics.Color.White,
    secondaryContainer   = androidx.compose.ui.graphics.Color(0xFFCCF7EE),
    onSecondaryContainer = androidx.compose.ui.graphics.Color(0xFF003322),
    tertiary             = BrandViolet,
    onTertiary           = androidx.compose.ui.graphics.Color.White,
    error                = BrandRed,
    onError              = androidx.compose.ui.graphics.Color.White,
    background           = LightBg,
    onBackground         = LightInk,
    surface              = LightSurface,
    onSurface            = LightInk,
    surfaceVariant       = LightSurface2,
    onSurfaceVariant     = LightInk2,
    outline              = LightInk3,
    outlineVariant       = LightInkMute,
)
private val DarkColorScheme = darkColorScheme(
    primary              = DarkPrimary,
    onPrimary            = DarkOnPrimary,
    primaryContainer     = androidx.compose.ui.graphics.Color(0xFF003BA5),
    onPrimaryContainer   = androidx.compose.ui.graphics.Color(0xFFD9E2FF),
    secondary            = BrandTeal,
    onSecondary          = androidx.compose.ui.graphics.Color(0xFF003730),
    secondaryContainer   = androidx.compose.ui.graphics.Color(0xFF005045),
    onSecondaryContainer = androidx.compose.ui.graphics.Color(0xFFCCF7EE),
    tertiary             = BrandViolet,
    onTertiary           = androidx.compose.ui.graphics.Color.White,
    error                = androidx.compose.ui.graphics.Color(0xFFFFB4AB),
    onError              = androidx.compose.ui.graphics.Color(0xFF690005),
    background           = DarkBg,
    onBackground         = DarkInk,
    surface              = DarkSurface,
    onSurface            = DarkInk,
    surfaceVariant       = DarkSurface2,
    onSurfaceVariant     = DarkInk2,
    outline              = DarkInk3,
    outlineVariant       = DarkInkMute,
)

@Composable
fun GaitGuardTheme(
    darkMode: Boolean = false,
    content: @Composable () -> Unit
) {
    val tokens = if (darkMode) DarkTokens else LightTokens
    CompositionLocalProvider(
        LocalDarkMode provides darkMode,
        LocalGgTokens provides tokens
    ) {
        MaterialTheme(
            colorScheme = if (darkMode) DarkColorScheme else LightColorScheme,
            typography  = GaitGuardTypography,
            content     = content
        )
    }
}
