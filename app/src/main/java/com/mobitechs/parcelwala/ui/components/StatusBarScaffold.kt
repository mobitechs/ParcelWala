// ui/components/StatusBarScaffold.kt
package com.mobitechs.parcelwala.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowInsetsControllerCompat
import com.mobitechs.parcelwala.ui.theme.AppColors

/**
 * StatusBarScaffold
 *
 * Drop-in replacement for Scaffold that:
 *  1. Colors the status bar to match your toolbar color
 *  2. Adds the correct top padding so content sits below system icons
 *  3. Restores transparent + dark icons when the screen is disposed
 *
 * Usage — Orange toolbar (HomeScreen / AccountScreen / OrdersScreen style):
 *
 *   StatusBarScaffold(
 *       statusBarColor = AppColors.Primary,
 *       darkStatusBarIcons = false,
 *       topBar = {
 *           TopAppBar(
 *               title = { Text("My Screen", color = Color.White) },
 *               colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.Primary)
 *           )
 *       }
 *   ) { padding ->
 *       // your screen content
 *   }
 *
 * Usage — White toolbar (any plain screen):
 *
 *   StatusBarScaffold(
 *       statusBarColor = Color.White,
 *       darkStatusBarIcons = true,
 *       topBar = { TopAppBar(...) }
 *   ) { padding ->
 *       // your screen content
 *   }
 *
 * Usage — Dynamic color (OrderDetailsScreen — color depends on order status):
 *
 *   StatusBarScaffold(
 *       statusBarColor = statusConfig.color,
 *       darkStatusBarIcons = false,
 *       topBar = { /* coloured header Box with TopAppBar inside */ }
 *   ) { padding ->
 *       // your screen content
 *   }
 */
@Composable
fun StatusBarScaffold(
    // Color applied to the status bar background AND the topBar surface
    statusBarColor: Color = AppColors.Primary,
    // false = white icons (use on dark/colored bars)
    // true  = dark icons  (use on white/light bars)
    darkStatusBarIcons: Boolean = false,
    // Standard Scaffold parameters
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    containerColor: Color = AppColors.Background,
    contentWindowInsets: WindowInsets = WindowInsets(0),
    content: @Composable (PaddingValues) -> Unit
) {
    val view = LocalView.current
    val statusBarColorArgb = android.graphics.Color.argb(
        (statusBarColor.alpha * 255).toInt(),
        (statusBarColor.red * 255).toInt(),
        (statusBarColor.green * 255).toInt(),
        (statusBarColor.blue * 255).toInt()
    )

    // Apply and restore status bar color + icon style
    DisposableEffect(statusBarColorArgb, darkStatusBarIcons) {
        val window = (view.context as? android.app.Activity)?.window
        val controller = window?.let { WindowInsetsControllerCompat(it, view) }

        window?.statusBarColor = statusBarColorArgb
        controller?.isAppearanceLightStatusBars = darkStatusBarIcons

        onDispose {
            // Restore transparent status bar with dark icons when leaving screen
            window?.statusBarColor = android.graphics.Color.TRANSPARENT
            controller?.isAppearanceLightStatusBars = true
        }
    }

    Scaffold(
        topBar = {
            StatusBarAwareTopBar(
                backgroundColor = statusBarColor,
                content = topBar
            )
        },
        bottomBar = bottomBar,
        snackbarHost = snackbarHost,
        containerColor = containerColor,
        contentWindowInsets = contentWindowInsets,
        content = content
    )
}

/**
 * Wraps any topBar content with a Surface that:
 * - matches the statusBarColor
 * - adds a Spacer equal to the status bar height above the content
 *
 * This ensures your TopAppBar title/icons never overlap the system status icons.
 *
 * If you need a fully custom header (like OrderDetailsScreen's coloured Box),
 * pass useDefaultSurface = false and manage the Spacer yourself inside your topBar.
 */
@Composable
fun StatusBarAwareTopBar(
    backgroundColor: Color,
    useDefaultSurface: Boolean = true,
    content: @Composable () -> Unit
) {
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    if (useDefaultSurface) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = backgroundColor,
            shadowElevation = androidx.compose.ui.unit.Dp(0f),
            tonalElevation = androidx.compose.ui.unit.Dp(0f)
        ) {
            Column {
                Spacer(modifier = Modifier.height(statusBarHeight))
                content()
            }
        }
    } else {
        // Caller manages layout; just expose the height via a Spacer at the top
        Column {
            Spacer(modifier = Modifier.height(statusBarHeight))
            content()
        }
    }
}