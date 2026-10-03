package com.mobitechs.parcelwala.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * Check if location permission is granted
 */
fun Context.hasLocationPermission(): Boolean {
    return ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
}

/**
 * Location permission, plus a way to ask for it AGAIN.
 *
 * ─────────────────────────────────────────────────────────────────────────
 * WHY [request] EXISTS
 * ─────────────────────────────────────────────────────────────────────────
 *
 * The original helper asked once, on first composition, and returned only the
 * boolean. That is fine for a screen that degrades quietly, and wrong for one
 * with a control that DEPENDS on the permission: the location picker now shows
 * "Use my current location for pickup" whenever the pickup is empty, and with
 * the permission denied that button called straight into the location service,
 * which threw a SecurityException into an error field nothing renders. The
 * customer tapped a button and nothing happened at all — no address, no error,
 * no permission dialog, because the one-shot prompt had already been spent.
 *
 * Handing the caller the launcher lets the button do the obvious thing: ask.
 */
@androidx.compose.runtime.Stable
class LocationPermissionState(
    val granted: MutableState<Boolean>,
    /** Show the system permission dialog again. Safe to call when granted. */
    val request: () -> Unit
)

@Composable
fun rememberLocationPermission(
    onPermissionResult: (Boolean) -> Unit
): LocationPermissionState {
    val context = LocalContext.current
    val permissionGranted = remember {
        mutableStateOf(context.hasLocationPermission())
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        permissionGranted.value = isGranted
        onPermissionResult(isGranted)
    }

    LaunchedEffect(Unit) {
        if (!permissionGranted.value) {
            launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    return remember(permissionGranted, launcher) {
        LocationPermissionState(
            granted = permissionGranted,
            request = { launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION) }
        )
    }
}

/**
 * Composable for handling location permission.
 *
 * Kept for the call sites that only need the boolean.
 */
@Composable
fun rememberLocationPermissionState(
    onPermissionResult: (Boolean) -> Unit
): MutableState<Boolean> = rememberLocationPermission(onPermissionResult).granted