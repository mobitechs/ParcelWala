package com.mobitechs.parcelwala.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    // Primary
    primary = AppColors.Primary,
    onPrimary = AppColors.White,
    primaryContainer = AppColors.PrimaryContainer,
    onPrimaryContainer = AppColors.PrimaryDeep,

    // Secondary (darker teal variant)
    secondary = AppColors.PrimaryDark,
    onSecondary = AppColors.White,
    secondaryContainer = AppColors.PrimaryLight,
    onSecondaryContainer = AppColors.PrimaryDeep,

    // Tertiary (accent - amber/gold)
    tertiary = AppColors.Accent,
    onTertiary = AppColors.White,
    tertiaryContainer = AppColors.AmberLight,
    onTertiaryContainer = AppColors.OrangeDark,

    // Error
    error = AppColors.Error,
    onError = AppColors.White,
    errorContainer = AppColors.ErrorLight,
    onErrorContainer = AppColors.Error,

    // Background
    background = AppColors.Background,
    onBackground = AppColors.TextPrimary,

    // Surface
    surface = AppColors.Surface,
    onSurface = AppColors.TextPrimary,
    surfaceVariant = AppColors.LightGray50,
    onSurfaceVariant = AppColors.TextSecondary,

    // Outline
    outline = AppColors.Border,
    outlineVariant = AppColors.DividerLight,

    // Inverse (for snackbars, etc.)
    inverseSurface = AppColors.PrimaryDeep,
    inverseOnSurface = AppColors.PrimaryLight,
    inversePrimary = AppColors.PrimaryMuted
)

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