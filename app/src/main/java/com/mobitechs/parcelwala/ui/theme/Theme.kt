package com.mobitechs.parcelwala.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * Material 3 scheme, mapped onto the "Navy + Teal + Amber" tokens in
 * [AppColors].
 *
 * Every role is filled explicitly. Left to its own defaults Material derives
 * containers and "on" colours from the seed, which produced washed lavender
 * surfaces under the old purple brand and would produce washed indigo ones now —
 * visible mostly in components the app does not style by hand (menus, snackbars,
 * date pickers, ripple containers), which is exactly where an inconsistency is
 * hardest to track down.
 */
private val LightColorScheme = lightColorScheme(
    // ── Primary: Deep Navy. Buttons, selection, brand surfaces.
    primary = AppColors.Primary,
    onPrimary = AppColors.White,
    primaryContainer = AppColors.PrimaryLight,
    onPrimaryContainer = AppColors.PrimaryDeep,
    inversePrimary = AppColors.PrimaryMuted,

    // ── Secondary: Teal. Confirmation and "included" states.
    secondary = AppColors.Secondary,
    onSecondary = AppColors.White,
    secondaryContainer = AppColors.SecondaryLight,
    onSecondaryContainer = AppColors.SecondaryDark,

    // ── Tertiary: Amber. Accent, recommendation, ratings.
    tertiary = AppColors.Accent,
    onTertiary = AppColors.TextPrimary,
    tertiaryContainer = AppColors.AccentLight,
    onTertiaryContainer = AppColors.AccentDark,

    // ── Error
    error = AppColors.Error,
    onError = AppColors.White,
    errorContainer = AppColors.ErrorLight,
    onErrorContainer = AppColors.Error,

    // ── Background / surface
    background = AppColors.Background,
    onBackground = AppColors.TextPrimary,
    surface = AppColors.Surface,
    onSurface = AppColors.TextPrimary,
    surfaceVariant = AppColors.SurfaceVariant,
    onSurfaceVariant = AppColors.TextSecondary,
    surfaceTint = AppColors.Primary,

    // ── Outline
    outline = AppColors.Border,
    outlineVariant = AppColors.DividerLight,

    // ── Inverse (snackbars, tooltips)
    inverseSurface = AppColors.PrimaryDeep,
    inverseOnSurface = AppColors.PrimaryLight,

    scrim = AppColors.Black
)

/**
 * Status-bar icon colour is NOT set here.
 *
 * `MainActivity` already calls `enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(...))`,
 * which is what makes the clock and battery legible over the navy header. Setting
 * it a second time from a `SideEffect` here would fight that call on every
 * recomposition for no gain.
 */
@Composable
fun ParcelWalaTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
