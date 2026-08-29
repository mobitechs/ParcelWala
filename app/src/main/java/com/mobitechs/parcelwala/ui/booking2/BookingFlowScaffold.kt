package com.mobitechs.parcelwala.ui.booking2

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobitechs.parcelwala.ui.theme.AppColors

/**
 * ════════════════════════════════════════════════════════════════════════════
 * BOOKING FLOW v2 — SHARED CHROME
 * ════════════════════════════════════════════════════════════════════════════
 *
 * Every screen in the flow uses [SendParcelScaffold]. Nothing rolls its own
 * header, background or bottom button.
 *
 * WHY THIS IS ITS OWN FILE
 * The first pass let each screen build its own top row. The result was five
 * screens with five slightly different header heights, three different back
 * affordances, and one screen (the fare step) that had no back button at all
 * because its scaffold accepted an `onBack` and never rendered it. Consistency
 * is not a polish item here — an inconsistent header reads as broken, and a
 * missing one is a dead end.
 *
 * Everything in this file is layout only. No state, no navigation decisions.
 */

/** Standard measurements, so nothing drifts. */
object SendParcelTokens {
    val ScreenPadding = 20.dp
    val BarHeight = 56.dp
    val ButtonHeight = 54.dp
    val CornerRadius = 14.dp
    val FieldSpacing = 12.dp
}

/**
 * The one top bar for the whole flow.
 *
 * Back is a real 44dp touch target — the previous inline `IconButton` inside a
 * `Row` with a sibling `Column` using `weight(1f)` could be squeezed by a long
 * title, which is how a back button ends up looking present but not working.
 */
@Composable
fun SendParcelTopBar(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Go back",
                    tint = AppColors.TextPrimary
                )
            }
        } else {
            Spacer(Modifier.size(12.dp))
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary,
                maxLines = 1
            )
            subtitle?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextSecondary,
                    maxLines = 1
                )
            }
        }

        actions()
    }
}

/**
 * Standard page: white top bar, tinted body, optional sticky bottom action.
 *
 * The bottom bar is deliberately OUTSIDE the scrolling body. A primary action
 * that scrolls off screen is the most common reason a form feels broken on a
 * short phone.
 */
@Composable
fun SendParcelScaffold(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    bottomBar: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.Background)
    ) {
        SendParcelTopBar(title = title, subtitle = subtitle, onBack = onBack)
        Box(modifier = Modifier.weight(1f)) { content() }
        bottomBar?.invoke()
    }
}

/**
 * The one primary button style for the flow.
 *
 * [helperText] is for the thing above the button that changes — a total, a
 * validation hint. Keeping it inside this component is what stops one screen
 * showing a total in 18sp and the next in 24sp.
 */
@Composable
fun SendParcelBottomBar(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    isLoading: Boolean = false,
    helperLabel: String? = null,
    helperValue: String? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(SendParcelTokens.ScreenPadding)
            .navigationBarsPadding()
    ) {
        if (helperLabel != null || helperValue != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    helperLabel.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextSecondary
                )
                Text(
                    helperValue.orEmpty(),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary
                )
            }
        }
        Button(
            onClick = onClick,
            enabled = enabled && !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(SendParcelTokens.ButtonHeight),
            shape = RoundedCornerShape(SendParcelTokens.CornerRadius),
            colors = ButtonDefaults.buttonColors(
                containerColor = AppColors.Primary,
                disabledContainerColor = AppColors.DisabledBackground
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = Color.White
                )
            } else {
                Text(
                    label,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White
                )
            }
        }
    }
}

/** One card style for the whole flow. */
@Composable
fun SendParcelCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(SendParcelTokens.CornerRadius))
    ) { content() }
}

/** One text-field colour set for the whole flow. */
@Composable
fun sendParcelFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = AppColors.Primary,
    unfocusedBorderColor = AppColors.Border,
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White,
    errorContainerColor = Color.White
)

/** Circular icon button that floats over a map. */
@Composable
fun MapFloatingButton(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(42.dp)
            .background(Color.White, CircleShape)
    ) {
        Box(contentAlignment = Alignment.Center) { icon() }
    }
}
