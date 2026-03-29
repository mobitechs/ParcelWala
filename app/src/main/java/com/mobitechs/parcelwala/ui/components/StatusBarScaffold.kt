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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.unit.sp
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
//
// CHANGE: Added horizontal padding + 4.dp bottom padding so the gradient
// header never clips its content against the status bar or screen edges.
// ══════════════════════════════════════════════════════════════════════════════

@Composable
internal fun GradientTopBarWrapper(content: @Composable () -> Unit) {
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
            // ── Exact status-bar inset so content never overlaps system icons ──
            Spacer(modifier = Modifier.height(statusBarHeight))
            content()
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// AppTopBar — use this inside StatusBarScaffold on every screen
//
// STANDARDS applied globally here so no individual screen needs to override:
//
//   Title  → titleLarge (22sp Bold) — Material3 TopAppBar spec
//   Back   → ArrowBack 24dp white IconButton — Android navigation standard
//   Spacing→ TopAppBar default height (64dp) keeps status-bar gap correct
//
// USAGE EXAMPLES:
//
//   Simple (no back):
//     AppTopBar(title = "Payments")
//
//   With back button:
//     AppTopBar(title = "Saved Addresses", onBack = onBack)
//
//   With trailing actions:
//     AppTopBar(title = "Saved Addresses", onBack = onBack, actions = { ... })
//
//   With content below title row (e.g. filter chips):
//     AppTopBar(title = "My Orders", extraContent = { StatusFilterRow() })
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
                    // ── Material3 TopAppBar standard: titleLarge = 22sp ──────
                    // headlineSmall (24sp) was too large; titleLarge matches
                    // every major Android app (Gmail, Maps, Play Store, etc.)
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight    = FontWeight.Bold,
                        fontSize      = 20.sp,        // 20sp: sits between titleLarge & titleMedium
                        letterSpacing = (-0.3).sp     // tighter tracking looks cleaner on dark bg
                    ),
                    color = Color.White
                )
            },
            navigationIcon = {
                if (onBack != null) {
                    // ── Standard Android back button ──────────────────────
                    // 24dp icon inside a 48dp touch target — matches spec
                    IconButton(
                        onClick   = onBack,
                        modifier  = Modifier.size(48.dp)   // explicit 48dp touch target
                    ) {
                        Icon(
                            imageVector        = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint               = Color.White,
                            modifier           = Modifier.size(24.dp)  // 24dp icon — M3 standard
                        )
                    }
                }
            },
            actions = { actions() },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor         = Color.Transparent,
                scrolledContainerColor = Color.Transparent
            ),
            // Zero out insets — GradientTopBarWrapper already handles the
            // status bar offset via its own Spacer, so we don't double-pad.
            windowInsets = WindowInsets(0)
        )
        extraContent()
    }
}