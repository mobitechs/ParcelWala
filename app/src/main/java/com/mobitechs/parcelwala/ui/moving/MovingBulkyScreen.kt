package com.mobitechs.parcelwala.ui.moving

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobitechs.parcelwala.data.model.moving.MovingDraft
import com.mobitechs.parcelwala.ui.booking2.SendParcelBottomBar
import com.mobitechs.parcelwala.ui.booking2.SendParcelScaffold
import com.mobitechs.parcelwala.ui.theme.AppColors

/**
 * ════════════════════════════════════════════════════════════════════════════
 * STEP 3 — "Anything bulky?"
 * ════════════════════════════════════════════════════════════════════════════
 *
 * WHY THIS IS ITS OWN SCREEN AND NOT A CHECKBOX ON THE LIST
 *
 * "Bulky" is not a size — the catalog already knows the sizes. It is a HANDLING
 * property: does this need two people, a wide door, a lift, and space around it
 * on the vehicle? A wardrobe and a hundred cartons occupy similar cubic feet and
 * are completely different jobs, and the difference is invisible in the volume
 * figure the previous screen computed.
 *
 * It also changes the answer. A "yes" adds twenty per cent padding
 * (`BULKY_FACTOR`) and can move the recommendation up a vehicle class — which is
 * a consequential enough decision to deserve its own moment rather than a
 * checkbox someone scrolls past.
 *
 * WHY THE CATALOG'S GUESS IS PRE-SELECTED BUT NOT LOCKED
 *
 * If the customer has already picked a fridge, the answer is yes and pretending
 * otherwise wastes their time — so the card arrives pre-answered with the reason
 * shown. But it stays changeable in one direction: a customer can always tell us
 * about a bulky item the catalog does not know about. They cannot talk us out of
 * one it does — see `MovingRecommendationEngine.estimate`, where a "no" over a
 * fridge is ignored. Shrinking a vehicle on the strength of a misunderstood
 * question is the one failure this flow must not have.
 */
@Composable
fun MovingBulkyScreen(
    draft: MovingDraft,
    onAnswer: (Boolean) -> Unit,
    onContinue: () -> Unit,
    onBack: () -> Unit
) {
    val answer = draft.hasBulkyItems

    SendParcelScaffold(
        title = "Any bulky or heavy items?",
        subtitle = "Step 3 of 4",
        onBack = onBack,
        bottomBar = {
            SendParcelBottomBar(
                label = "See recommended vehicle",
                enabled = answer != null,
                onClick = onContinue
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            MovingStepRail(currentStep = 2)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MovingTokens.ScreenPadding)
            ) {
                // The toolbar asks the question; this answers "what counts?",
                // which is the part a customer actually needs help with. It used
                // to repeat the question here in a larger font first.
                Text(
                    text = "Things that need two people or a wide doorway — a wardrobe, " +
                            "a double-door fridge, a mattress, a big sofa.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextSecondary,
                    lineHeight = 20.sp
                )

                Spacer(Modifier.height(18.dp))

                Row(
                    modifier = Modifier.height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    BulkyChoiceCard(
                        icon = "📦",
                        title = "Yes",
                        subtitle = "I have large or heavy items",
                        isSelected = answer == true,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        onClick = { onAnswer(true) }
                    )
                    BulkyChoiceCard(
                        icon = "🎒",
                        title = "No",
                        subtitle = "Everything is easy to carry",
                        isSelected = answer == false,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        onClick = { onAnswer(false) }
                    )
                }

                // A note explaining "we already spotted bulky items" used to
                // live here. It is gone because the state it described cannot
                // occur any more: `needsBulkyQuestion` returns false the moment
                // the catalog knows the load is bulky, so this screen is never
                // reached in that case. Dead UI that implies an impossible state
                // is worse than no UI.

                Spacer(Modifier.height(22.dp))

                // What we are about to size the vehicle on. Last chance to catch
                // a mis-tapped quantity before it becomes the wrong truck.
                MovingLoadSummaryStrip(estimate = draft.estimate)

                if (draft.estimate.suggestedHelpers > 0) {
                    Spacer(Modifier.height(12.dp))
                    HelpersNote(count = draft.estimate.suggestedHelpers)
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun BulkyChoiceCard(
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
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Text(text = icon, fontSize = 26.sp)

            // The tick is drawn only when selected — an empty circle on both
            // cards reads as "neither is chosen yet" even after a tap.
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(AppColors.Primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = AppColors.White,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = AppColors.TextPrimary
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.TextSecondary,
            fontSize = 12.sp,
            lineHeight = 16.sp
        )
    }
}

@Composable
private fun HelpersNote(count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AppColors.AccentLight)
            .padding(12.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("💪", fontSize = 15.sp)
        Text(
            text = "We suggest $count loading helper" + (if (count > 1) "s" else "") +
                    " for a load this size. You can add them at the price step.",
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.AccentDark,
            lineHeight = 17.sp
        )
    }
}
