package com.mobitechs.parcelwala.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color


private val LightColorScheme = lightColorScheme(
    primary = Orange500,
    onPrimary = Color.White,
    primaryContainer = OrangeLight,
    onPrimaryContainer = Orange700,

    secondary = Orange600,
    onSecondary = Color.White,
    secondaryContainer = OrangeLight,
    onSecondaryContainer = Orange700,

    tertiary = Green500,
    onTertiary = Color.White,

    error = Red500,
    onError = Color.White,

    background = Gray50,
    onBackground = Gray900,

    surface = Color.White,
    onSurface = Gray900,
    surfaceVariant = Gray100,
    onSurfaceVariant = Gray700,

    outline = Gray300,
    outlineVariant = Gray200
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

// Reusable Colors
object AppColors {
    val Primary = Orange500
    val PrimaryDark = Orange600
    val PrimaryLight = OrangeLight
    val Pickup = Green500
    val Drop = Red500
    val TextPrimary = Gray900
    val TextSecondary = Gray600
    val TextHint = Gray400
    val Border = Gray300
    val Background = Gray50
    val Surface = Color.White
    val Divider = Gray200
}