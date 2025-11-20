package com.mobitechs.parcelwala.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Light Color Scheme
private val LightColorScheme = lightColorScheme(
    primary = PrimaryOrange,
    onPrimary = OnPrimaryWhite,
    primaryContainer = PrimaryContainerLightOrange,
    onPrimaryContainer = OnPrimaryContainerDarkBrown,

    secondary = SecondaryNavy,
    onSecondary = OnSecondaryWhite,
    secondaryContainer = SecondaryContainerLightBlue,
    onSecondaryContainer = OnSecondaryContainerDarkBlue,

    tertiary = TertiaryWarmGray,
    onTertiary = OnTertiaryWhite,
    tertiaryContainer = TertiaryContainerCream,
    onTertiaryContainer = OnTertiaryContainerDarkBrown,

    background = BackgroundOffWhite,
    onBackground = OnBackgroundDark,

    surface = SurfaceOffWhite,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantBeige,
    onSurfaceVariant = OnSurfaceVariantBrown,

    error = ErrorRed,
    onError = OnErrorWhite,
    errorContainer = ErrorContainerLightRed,
    onErrorContainer = OnErrorContainerDarkRed,

    outline = OutlineBrown,
    outlineVariant = OutlineVariantLightBrown,
    scrim = ScrimBlack
)

@Composable
fun ParcelWalaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Set to false to use custom colors
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// Custom theme accessor for easier usage
object AppTheme {
    val colors: ColorScheme
        @Composable
        get() = MaterialTheme.colorScheme

    val typography: Typography
        @Composable
        get() = MaterialTheme.typography

    val shapes: Shapes
        @Composable
        get() = MaterialTheme.shapes
}