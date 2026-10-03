package com.mobitechs.parcelwala.ui.moving

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobitechs.parcelwala.data.model.moving.LoadEstimate
import com.mobitechs.parcelwala.data.model.moving.MovingItem
import com.mobitechs.parcelwala.ui.theme.AppColors

/**
 * ════════════════════════════════════════════════════════════════════════════
 * SMART SHIFTING — SHARED UI PIECES
 * ════════════════════════════════════════════════════════════════════════════
 *
 * Layout only. No state of their own, no navigation, no ViewModel — every one of
 * these takes its values and its callbacks from the screen above it, which is
 * what keeps the four screens of this flow looking like one flow.
 */

/** Standard measurements for the moving flow. */
object MovingTokens {
    val ScreenPadding = 20.dp
    val CardCorner = 16.dp
    val ItemCorner = 14.dp
    val RowHeight = 68.dp
    val StepperSize = 30.dp
}

/**
 * The progress rail at the top of every step.
 *
 * WHY A RAIL AND NOT A "STEP 2 OF 4" LABEL
 *
 * The moving flow asks up to four questions before it shows a price, which is
 * three more than the parcel flow. That is justified — a house move genuinely
 * needs them — but it is also exactly the shape of funnel people abandon,
 * because from inside step two there is no way to tell whether step ten is
 * coming. A rail that shows the whole journey at once answers "how much longer"
 * before the customer has to wonder.
 *
 * ─────────────────────────────────────────────────────────────────────────
 * WHY IT IS ALWAYS FOUR DOTS, EVEN WHEN THE BULKY SCREEN IS SKIPPED
 * ─────────────────────────────────────────────────────────────────────────
 *
 * The first attempt sized the rail to the journey: three dots when the bulky
 * question would be skipped, four when it would be asked. It was worse in every
 * way. The category screen has no selection yet, so it could not know, and
 * always drew four — and then the item screen, which recomputes live as items
 * are added, flipped between three and four WHILE THE CUSTOMER WAS TAPPING
 * QUANTITIES, animating each time. A progress indicator that changes length as
 * you use it is not reassurance, it is noise.
 *
 * A fixed four is stable, honest as a progress bar, and a skipped step simply
 * means the customer arrives at the end sooner. Nobody counts the dots; they
 * read how much is left.
 */
@Composable
fun MovingStepRail(
    currentStep: Int,
    totalSteps: Int = 4,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = MovingTokens.ScreenPadding, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        repeat(totalSteps) { index ->
            val isDone = index < currentStep
            val isCurrent = index == currentStep

            val color by animateColorAsState(
                targetValue = when {
                    isDone -> AppColors.Primary
                    isCurrent -> AppColors.Primary
                    else -> AppColors.Border
                },
                animationSpec = tween(220),
                label = "railColor"
            )

            Box(
                modifier = Modifier
                    .weight(if (isCurrent) 1.6f else 1f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color)
            )
        }
    }
}

/**
 * A category tile on "What are you moving?".
 *
 * Square-ish and icon-led on purpose: this is the first screen of a flow the
 * customer has never seen, and a grid of pictures is read in one glance where a
 * list of sentences has to be worked through.
 */
@Composable
fun MovingCategoryTile(
    icon: String,
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(MovingTokens.CardCorner))
            .background(if (isSelected) AppColors.PrimaryLight else AppColors.Surface)
            .border(
                BorderStroke(
                    width = if (isSelected) 1.5.dp else 1.dp,
                    color = if (isSelected) AppColors.Primary else AppColors.Border
                ),
                RoundedCornerShape(MovingTokens.CardCorner)
            )
            .clickable(onClick = onClick)
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    if (isSelected) AppColors.Surface else AppColors.PrimaryLight
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(text = icon, fontSize = 24.sp)
        }

        Spacer(Modifier.height(10.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = AppColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = AppColors.TextSecondary,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * One catalog row: icon, name, hint, and a stepper on the right.
 *
 * ─────────────────────────────────────────────────────────────────────────
 * WHY THE STEPPER IS ALWAYS VISIBLE, NEVER A CHECKBOX THAT BECOMES ONE
 * ─────────────────────────────────────────────────────────────────────────
 *
 * The obvious design shows a checkbox, and swaps in a quantity control once the
 * item is ticked. It costs one extra tap on every single row, and — worse — it
 * hides the fact that quantity is adjustable at all until after you have
 * committed. People end up with "1 chair" when they meant six, and only discover
 * it when the vehicle arrives. The stepper sitting there from the start makes
 * quantity part of the question instead of a follow-up to it.
 *
 * At zero the minus button is drawn disabled rather than removed, so the row
 * does not change width the moment you touch it.
 */
@Composable
fun MovingItemRow(
    item: MovingItem,
    quantity: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isSelected = quantity > 0

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MovingTokens.ItemCorner))
            .background(AppColors.Surface)
            .border(
                BorderStroke(
                    width = if (isSelected) 1.5.dp else 1.dp,
                    color = if (isSelected) AppColors.Primary else AppColors.Border
                ),
                RoundedCornerShape(MovingTokens.ItemCorner)
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(if (isSelected) AppColors.PrimaryLight else AppColors.SurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(text = item.icon, fontSize = 20.sp)
        }

        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            // ── The size line ──────────────────────────────────────────
            //
            // "3 seater · approx 6.5 x 3 x 3 ft · 45 kg".
            //
            // This is the whole point of the row. A customer picking "Sofa" has
            // to know WHICH sofa the app means before the quantity they set is
            // worth anything — and feet and kilos are the only units they can
            // check against the actual object sitting in the room. Cubic feet
            // appear nowhere in this flow; they are the sizing engine's unit,
            // not the customer's.
            item.detailLine.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        MovingQuantityStepper(
            quantity = quantity,
            onIncrement = onIncrement,
            onDecrement = onDecrement
        )
    }
}

/** − 1 + . The whole quantity interaction, in one place. */
@Composable
fun MovingQuantityStepper(
    quantity: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        StepperButton(
            icon = Icons.Default.Remove,
            enabled = quantity > 0,
            contentDescription = "Remove one",
            onClick = onDecrement
        )

        Text(
            text = quantity.toString(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = if (quantity > 0) AppColors.TextPrimary else AppColors.TextHint,
            modifier = Modifier.width(26.dp),
            maxLines = 1,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        StepperButton(
            icon = Icons.Default.Add,
            enabled = true,
            contentDescription = "Add one",
            isPrimary = quantity > 0,
            onClick = onIncrement
        )
    }
}

@Composable
private fun StepperButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
    isPrimary: Boolean = false
) {
    val background = when {
        !enabled -> AppColors.SurfaceVariant
        isPrimary -> AppColors.Primary
        else -> AppColors.PrimaryLight
    }
    val tint = when {
        !enabled -> AppColors.TextHint
        isPrimary -> AppColors.White
        else -> AppColors.Primary
    }

    Box(
        modifier = Modifier
            .size(MovingTokens.StepperSize)
            .clip(RoundedCornerShape(9.dp))
            .background(background)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(16.dp)
        )
    }
}

/**
 * The horizontal load bar on the recommendation screen.
 *
 * Colour carries meaning here and is not decoration:
 *   teal    — comfortable, room to spare
 *   navy    — a good, efficient fit
 *   amber   — nearly full, worth a second look at the list
 *
 * Red is deliberately absent. A bar the engine produced can never exceed the
 * vehicle it just chose, so a red state would only ever be reachable by the
 * customer overriding the recommendation — and in that case the honest UI is the
 * "won't fit in one trip" message, not a scary bar.
 */
@Composable
fun MovingLoadMeter(
    utilizationPercent: Int,
    modifier: Modifier = Modifier
) {
    val target = (utilizationPercent.coerceIn(0, 100)) / 100f
    val progress by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(600),
        label = "loadMeter"
    )

    val barColor = when {
        utilizationPercent <= 60 -> AppColors.Secondary
        utilizationPercent <= 88 -> AppColors.Primary
        else -> AppColors.Accent
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Load utilisation",
                style = MaterialTheme.typography.labelMedium,
                color = AppColors.TextSecondary
            )
            Text(
                text = "$utilizationPercent%",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = barColor
            )
        }

        Spacer(Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(AppColors.SurfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress)
                    .clip(RoundedCornerShape(4.dp))
                    .background(barColor)
            )
        }
    }
}

/** A single "why this vehicle" line: teal tick, then the reason. */
@Composable
fun MovingReasonRow(
    text: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(AppColors.SecondaryLight),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = AppColors.Secondary,
                modifier = Modifier.size(11.dp)
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.TextSecondary,
            lineHeight = 18.sp
        )
    }
}

/**
 * The small amber "Recommended" pill.
 *
 * Amber, not navy, because navy is the colour of everything the customer can
 * tap. An accent that appears on every button stops meaning "look here".
 */
@Composable
fun MovingRecommendedBadge(
    text: String = "Recommended",
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = AppColors.AccentDark,
        fontSize = 10.sp,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(AppColors.AccentLight)
            .padding(horizontal = 7.dp, vertical = 3.dp)
    )
}

/**
 * The load summary: "Medium load · 6 items · about 180 kg".
 *
 * ─────────────────────────────────────────────────────────────────────────
 * WHY THERE ARE NO CUBIC FEET HERE
 * ─────────────────────────────────────────────────────────────────────────
 *
 * This strip used to read "Items 6 · Space ~48 cu.ft · Weight ~180 kg". The
 * middle figure was the single most important number in the flow and the one
 * nobody could use: a customer standing in their flat has no idea whether 48
 * cubic feet is a lot, and no way to check it against anything they can see.
 *
 * A cubic foot is the sizing engine's unit. The customer's units are a WORD for
 * how big the job is, a COUNT they can verify against their own list, and a
 * WEIGHT they have an intuition for. The band also does something the number
 * could not: it says what kind of vehicle this is heading towards, one screen
 * before the vehicle appears.
 */
@Composable
fun MovingLoadSummaryStrip(
    estimate: LoadEstimate,
    modifier: Modifier = Modifier,
    background: Color = AppColors.PrimaryLight,
    /**
     * The line under the band name.
     *
     * Passed in rather than read off [LoadSize], because it is a claim about a
     * FLEET. The band used to carry its own — SMALL said "Fits on a bike or
     * scooter" — which was wrong in two directions: the band runs to 8 cubic
     * feet while a bike holds 3, and an operator with no bike had every small
     * load told it fits on one, directly above a card recommending an auto.
     *
     * Blank when there is no recommendation yet, which is honest: at that point
     * we do not know.
     */
    hint: String = ""
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = estimate.loadSize.label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = AppColors.Primary
            )
            Text(
                text = listOfNotNull(
                    "${estimate.itemCount} item" + (if (estimate.itemCount == 1) "" else "s"),
                    estimate.weightLabel.takeIf { it.isNotBlank() }
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextSecondary
            )
        }

        if (hint.isNotBlank()) {
            Spacer(Modifier.height(3.dp))
            Text(
                text = hint,
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.TextSecondary,
                fontSize = 11.sp
            )
        }
    }
}

