package com.personaltrainer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ─── Brand palette ──────────────────────────────────────────────────────────
// Deep energetic tones — midnight blue base with electric accent
val Primary = Color(0xFF6C63FF)           // electric indigo
val PrimaryContainer = Color(0xFF3D3780)
val OnPrimary = Color(0xFFFFFFFF)
val OnPrimaryContainer = Color(0xFFE0DEFF)

val Secondary = Color(0xFF00D4AA)          // teal accent (progress, health)
val SecondaryContainer = Color(0xFF004D3D)
val OnSecondary = Color(0xFF000000)
val OnSecondaryContainer = Color(0xFF9EFADF)

val Tertiary = Color(0xFFFF6B6B)           // warm red (alerts, urgency)
val TertiaryContainer = Color(0xFF5C1919)
val OnTertiary = Color(0xFFFFFFFF)
val OnTertiaryContainer = Color(0xFFFFDAD5)

val Surface = Color(0xFF0F0F1A)            // near-black surface
val SurfaceVariant = Color(0xFF1C1C2E)     // slightly elevated
val SurfaceContainer = Color(0xFF24243A)   // cards
val OnSurface = Color(0xFFF0EFF8)
val OnSurfaceVariant = Color(0xFFABA9C3)
val Background = Color(0xFF0A0A14)
val OnBackground = Color(0xFFF0EFF8)

val Outline = Color(0xFF3D3C5C)
val OutlineVariant = Color(0xFF2D2C47)

private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    secondary = Secondary,
    onSecondary = OnSecondary,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = OnSecondaryContainer,
    tertiary = Tertiary,
    onTertiary = OnTertiary,
    tertiaryContainer = TertiaryContainer,
    onTertiaryContainer = OnTertiaryContainer,
    background = Background,
    onBackground = OnBackground,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
    outline = Outline,
    outlineVariant = OutlineVariant
)

/**
 * The app is dark-only — the dark colour scheme is applied on both
 * light and dark system settings.  Toggle this if a light theme is needed later.
 */
@Composable
fun PersonalTrainerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = AppTypography,
        content = content
    )
}
