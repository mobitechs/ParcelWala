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
    val Blue = Blue500
    val TextPrimary = Gray900
    val TextSecondary = Gray600
    val TextHint = Gray400
    val Border = Gray300
    val Background = Gray50
    val Surface = Color.White
    val Divider = Gray200

    val Success = Green500
    val Warning = Color(0xFFFFA000)
    val Error = Red500
    val DisabledBackground = Color.LightGray

    val ErrorLight = Red400

    val Amber = Amber500
    val Purple = Purple500
    val DarkSurface = com.mobitechs.parcelwala.ui.theme.DarkSurface
    val DarkSurfaceVariant = com.mobitechs.parcelwala.ui.theme.DarkSurfaceVariant
    val DarkDivider = com.mobitechs.parcelwala.ui.theme.DarkDivider

    val OrangeDark = com.mobitechs.parcelwala.ui.theme.OrangeDark
    val AmberLight = com.mobitechs.parcelwala.ui.theme.AmberLight
    val GreenLight = com.mobitechs.parcelwala.ui.theme.GreenLight
    val LightGray50 = com.mobitechs.parcelwala.ui.theme.LightGray50
    val AmberWarnBg = com.mobitechs.parcelwala.ui.theme.AmberWarnBg
    val RouteShadow = com.mobitechs.parcelwala.ui.theme.RouteShadow
    val DividerLight = com.mobitechs.parcelwala.ui.theme.DividerLight
    val DragHandle = com.mobitechs.parcelwala.ui.theme.DragHandle
}