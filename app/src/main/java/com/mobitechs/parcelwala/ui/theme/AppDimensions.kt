package com.mobitechs.parcelwala.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * Canonical spacing scale used across the app.
 * Use these instead of raw dp literals so sizing is consistent and
 * easy to update from a single place.
 */
object AppSpacing {
    val XXS = 2.dp
    val XS  = 4.dp
    val SM  = 8.dp
    val MD  = 12.dp
    val LG  = 16.dp
    val XL  = 20.dp
    val XXL = 24.dp
    val X3L = 32.dp
    val X4L = 40.dp
}

/**
 * Canonical corner-radius and elevation scale used across the app.
 */
object AppRadius {
    val SM          = 8.dp
    val MD          = 12.dp
    val LG          = 16.dp
    val XL          = 20.dp
    val Full        = 100.dp
    val CardElevation = 0.5.dp
    /** Standard card shape — RoundedCornerShape(16 dp). */
    val Card        = RoundedCornerShape(LG)
}