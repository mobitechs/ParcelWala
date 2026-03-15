// ui/components/StatusBarScaffold.kt
package com.mobitechs.parcelwala.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsControllerCompat
import com.mobitechs.parcelwala.R
import com.mobitechs.parcelwala.ui.theme.AppColors

// ══════════════════════════════════════════════════════════════════════════════
// StatusBarScaffold
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun StatusBarScaffold(
    statusBarColor: Color = AppColors.PrimaryDeep,
    darkStatusBarIcons: Boolean = false,
    useGradientTopBar: Boolean = true,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    containerColor: Color = AppColors.Background,
    contentWindowInsets: WindowInsets = WindowInsets(0),
    content: @Composable (PaddingValues) -> Unit
) {
    val view = LocalView.current
    val resolvedColor = AppColors.PrimaryDeep
    val statusBarColorArgb = android.graphics.Color.argb(
        (resolvedColor.alpha * 255).toInt(),
        (resolvedColor.red * 255).toInt(),
        (resolvedColor.green * 255).toInt(),
        (resolvedColor.blue * 255).toInt()
    )

    DisposableEffect(statusBarColorArgb, darkStatusBarIcons) {
        val window = (view.context as? android.app.Activity)?.window
        val controller = window?.let { WindowInsetsControllerCompat(it, view) }
        window?.statusBarColor = statusBarColorArgb
        controller?.isAppearanceLightStatusBars = darkStatusBarIcons
        onDispose {
            window?.statusBarColor = android.graphics.Color.TRANSPARENT
            controller?.isAppearanceLightStatusBars = true
        }
    }

    Scaffold(
        topBar = {
            if (useGradientTopBar) {
                GradientTopBarWrapper(content = topBar)
            } else {
                topBar()
            }
        },
        bottomBar = bottomBar,
        snackbarHost = snackbarHost,
        containerColor = containerColor,
        contentWindowInsets = contentWindowInsets,
        content = content
    )
}

// ══════════════════════════════════════════════════════════════════════════════
// GradientTopBarWrapper — internal, never use directly
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun GradientTopBarWrapper(content: @Composable () -> Unit) {
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val gradient = Brush.verticalGradient(
        colors = listOf(AppColors.PrimaryDeep, AppColors.Primary)
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
            .background(brush = gradient)
    ) {
        Column {
            Spacer(modifier = Modifier.height(statusBarHeight))
            content()
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// AppTopBar — use this inside StatusBarScaffold on every screen
//
// EXAMPLES:
//
// Simple (Payments, Account tab screens with no back):
//   AppTopBar(title = "Payments")
//
// With back button (SavedAddresses, ProfileDetails, Language):
//   AppTopBar(title = "Saved Addresses", onBack = onBack)
//
// With trailing actions (SavedAddresses + Add button):
//   AppTopBar(
//       title = "Saved Addresses",
//       onBack = onBack,
//       actions = { OutlinedButton(...) }
//   )
//
// With content below the title row (OrdersScreen filter chips):
//   AppTopBar(
//       title = "My Orders",
//       extraContent = { StatusFilterRow(...) }
//   )
// ══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {},
    extraContent: @Composable () -> Unit = {}
) {
    Column {
        TopAppBar(
            title = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            navigationIcon = {
                if (onBack != null) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = Color.White
                        )
                    }
                }
            },
            actions = { actions() },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent
            ),
            windowInsets = WindowInsets(0)
        )
        extraContent()
    }
}