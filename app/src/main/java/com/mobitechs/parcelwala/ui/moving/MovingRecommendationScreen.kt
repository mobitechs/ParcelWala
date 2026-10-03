package com.mobitechs.parcelwala.ui.moving

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import com.mobitechs.parcelwala.data.model.moving.LoadEstimate
import com.mobitechs.parcelwala.data.model.moving.MovingDraft
import com.mobitechs.parcelwala.data.model.moving.VehicleLoadProfile
import com.mobitechs.parcelwala.ui.booking2.SendParcelBottomBar
import com.mobitechs.parcelwala.ui.booking2.SendParcelScaffold
import com.mobitechs.parcelwala.ui.theme.AppColors
import com.mobitechs.parcelwala.utils.MovingRecommendationEngine
import kotlin.math.roundToInt

/**
 * ════════════════════════════════════════════════════════════════════════════
 * STEP 4 — "Here's the vehicle you need"
 * ════════════════════════════════════════════════════════════════════════════
 *
 * The payoff screen. Everything before it was the customer answering questions;
 * this is the app answering theirs.
 *
 * ─────────────────────────────────────────────────────────────────────────
 * WHY THERE IS NO PRICE ON THIS SCREEN
 * ─────────────────────────────────────────────────────────────────────────
 *
 * Because there cannot honestly be one yet. Fares are distance-based and this
 * screen runs BEFORE pickup and drop are known. Showing "from ₹500" here would
 * be a number the customer anchors on and that the next screen then contradicts
 * — which is precisely the pattern that makes people distrust a price and
 * abandon a booking.
 *
 * What this screen sells instead is confidence in the SIZE, which is the thing
 * the customer could not work out for themselves: the vehicle, what their load
 * weighs against what it can carry, how full it will be, and three checkable
 * reasons. Real prices arrive one screen later, on the existing fare sheet, with
 * the recommendation carried over and pre-selected.
 *
 * ─────────────────────────────────────────────────────────────────────────
 * WHY THE OTHER VEHICLES ARE ALWAYS OPEN, NOT BEHIND A TOGGLE
 * ─────────────────────────────────────────────────────────────────────────
 *
 * They used to sit behind a "See other options (2)" row that had to be tapped.
 * Two things were wrong with that. It read as a link rather than a list — pushed
 * below "Edit items", it looked like one more secondary action in a stack of
 * them, and people did not connect it to the card at the top. And it hid the one
 * piece of information that makes the recommendation trustworthy: that the app
 * looked at the whole fleet and this is where it landed.
 *
 * The alternatives now render open, immediately under the recommended card,
 * where the comparison is the point. Only vehicles that CAN take the load appear
 * — an "alternative" the goods do not fit into is not an option, it is a trap.
 *
 * ─────────────────────────────────────────────────────────────────────────
 * ONE VEHICLE. ALWAYS. NEVER A REFUSAL.
 * ─────────────────────────────────────────────────────────────────────────
 *
 * An earlier version of this screen had a second outcome: for a load bigger than
 * anything in the fleet it showed "Needs 2+ trips", an amber card and a support
 * link. It was accurate, and it was the wrong thing to put here — it lands at the
 * exact moment the customer is deciding whether to trust the estimate, it reads
 * as the app turning the job down, and the customer's answer to it is to close
 * the app.
 *
 * The screen now always shows ONE recommended vehicle. The oversized case still
 * exists in the engine as `VehicleRecommendation.fitsInOneTrip`, which is never
 * rendered — it rides to the server on the booking payload so operations can
 * call the customer and arrange a second vehicle before a driver is dispatched.
 */
@Composable
fun MovingRecommendationScreen(
    draft: MovingDraft,
    isLoading: Boolean,
    onEditItems: () -> Unit,
    onChooseVehicle: (Int) -> Unit,
    onContinue: () -> Unit,
    onRetry: () -> Unit,
    onBack: () -> Unit
) {
    val recommendation = draft.recommendation

    SendParcelScaffold(
        title = "Recommended vehicle",
        subtitle = "Based on what you're moving",
        onBack = onBack,
        bottomBar = {
            SendParcelBottomBar(
                label = when {
                    recommendation == null && draft.error != null -> "Try again"
                    recommendation == null -> "Working it out…"
                    else -> "Add pickup & drop"
                },
                // The button stays live in the error state, as the retry.
                enabled = recommendation != null || draft.error != null,
                isLoading = isLoading && recommendation == null,
                onClick = if (recommendation == null && draft.error != null) onRetry
                else onContinue,
                helperLabel = recommendation?.let { "Selected" },
                helperValue = recommendation?.vehicle?.name
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            MovingStepRail(currentStep = 3)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MovingTokens.ScreenPadding)
            ) {
                when {
                    recommendation == null && isLoading -> LoadingState()

                    // A failed or empty vehicle fetch is a NETWORK problem, and
                    // it must not be reported as "you didn't add any items".
                    //
                    // Before this branch existed, `draft.error` was set and
                    // nothing rendered it: the customer who had just listed
                    // twelve items was told to add some, above a CTA reading
                    // "Working it out…" that stayed disabled forever, with no
                    // spinner and no way to retry.
                    recommendation == null && draft.error != null -> ErrorState(
                        message = draft.error!!,
                        onRetry = onRetry
                    )

                    recommendation == null -> EmptyState(onEditItems = onEditItems)

                    else -> {
                        // ── "We worked it out" banner ──────────────────────
                        EstimateCompleteBanner(
                            itemCount = draft.estimate.itemCount,
                            summary = draft.itemsSummary(3)
                        )

                        Spacer(Modifier.height(14.dp))

                        RecommendedVehicleCard(
                            vehicle = recommendation.vehicle,
                            estimate = draft.estimate,
                            utilizationPercent = recommendation.utilizationPercent
                        )

                        // ── Other vehicles that also fit ───────────────────
                        //
                        // Directly under the card, open. See the file header.
                        if (recommendation.alternatives.isNotEmpty()) {
                            Spacer(Modifier.height(18.dp))

                            SectionLabel(
                                title = "Other vehicles that fit",
                                caption = "Pick a bigger one if you'd rather have " +
                                        "extra room. Prices come at the next step."
                            )

                            Spacer(Modifier.height(10.dp))

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                recommendation.alternatives.forEach { alt ->
                                    AlternativeVehicleRow(
                                        vehicle = alt,
                                        estimate = draft.estimate,
                                        utilizationPercent = MovingRecommendationEngine
                                            .utilization(draft.estimate, alt),
                                        onClick = { onChooseVehicle(alt.vehicleTypeId) }
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(18.dp))

                        // ── Why this vehicle ───────────────────────────────
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(MovingTokens.CardCorner))
                                .background(AppColors.Surface)
                                .border(
                                    BorderStroke(1.dp, AppColors.Border),
                                    RoundedCornerShape(MovingTokens.CardCorner)
                                )
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Why this vehicle?",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = AppColors.Heading
                            )
                            recommendation.reasons.forEach { MovingReasonRow(it) }
                        }

                        Spacer(Modifier.height(12.dp))

                        MovingLoadSummaryStrip(
                            estimate = draft.estimate,
                            background = AppColors.Surface,
                            hint = MovingRecommendationEngine.loadHintFor(recommendation.vehicle)
                        )

                        Spacer(Modifier.height(12.dp))

                        // ── Change what's being moved ──────────────────────
                        SecondaryAction(
                            icon = Icons.Default.Edit,
                            label = "Edit items · ${draft.itemsSummary(2)}",
                            onClick = onEditItems
                        )

                        Spacer(Modifier.height(14.dp))

                        Text(
                            text = "Final price is calculated once you add pickup and drop.",
                            style = MaterialTheme.typography.labelSmall,
                            color = AppColors.TextHint,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )

                        Spacer(Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

/** A quiet uppercase section heading with an optional line of explanation. */
@Composable
private fun SectionLabel(title: String, caption: String? = null) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = AppColors.Heading,
            fontSize = 11.sp,
            letterSpacing = 0.8.sp
        )
        caption?.let {
            Spacer(Modifier.height(4.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextHint,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
        }
    }
}

/** "Smart estimate complete" — the teal moment from the design. */
@Composable
private fun EstimateCompleteBanner(itemCount: Int, summary: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MovingTokens.CardCorner))
            .background(AppColors.SecondaryLight)
            .padding(14.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            Icons.Default.AutoAwesome,
            contentDescription = null,
            tint = AppColors.Secondary,
            modifier = Modifier.size(18.dp)
        )
        Column {
            Text(
                text = "Smart estimate complete",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = AppColors.SecondaryDark
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "We sized the load from your $itemCount items — $summary",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.SecondaryDark,
                lineHeight = 17.sp
            )
        }
    }
}

@Composable
private fun RecommendedVehicleCard(
    vehicle: VehicleLoadProfile,
    estimate: LoadEstimate,
    utilizationPercent: Int
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MovingTokens.CardCorner))
            .background(AppColors.Surface)
            .border(
                BorderStroke(1.5.dp, AppColors.Primary),
                RoundedCornerShape(MovingTokens.CardCorner)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppColors.PrimaryLight)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Best fit for your load",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = AppColors.Primary
            )
            MovingRecommendedBadge()
        }

        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                VehicleArtwork(vehicle)

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 14.dp)
                ) {
                    Text(
                        text = vehicle.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = MovingRecommendationEngine.capacityLabel(vehicle),
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextSecondary,
                        lineHeight = 17.sp
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            CapacityFacts(vehicle = vehicle, estimate = estimate)

            Spacer(Modifier.height(14.dp))

            MovingLoadMeter(utilizationPercent = utilizationPercent)
        }
    }
}

/**
 * ─────────────────────────────────────────────────────────────────────────
 * "29 kg of 750 kg" — the customer's load against the vehicle's limit
 * ─────────────────────────────────────────────────────────────────────────
 *
 * The load meter above it is a PERCENTAGE, and a percentage is only meaningful
 * once you know both numbers it came from. A card reading "11% full" over a
 * three-wheeler tells a customer with four boxes almost nothing — 11% of what?
 * Two figures side by side answer it in one glance, and they are the two the
 * customer can sanity-check: the weight they can feel when they lift the boxes,
 * and the weight painted on the side of the vehicle.
 *
 * The weight is shown even when the load is VOLUME-bound (which most house moves
 * are), because it is still the honest answer to "will my stuff be too heavy for
 * this?" — the question people actually ask. When space is the binding
 * constraint the caption says so, so a customer looking at "29 kg of 750 kg" and
 * a bar at 60% is not left thinking the app cannot do arithmetic.
 */
@Composable
private fun CapacityFacts(vehicle: VehicleLoadProfile, estimate: LoadEstimate) {
    val loadKg = estimate.totalWeightKg
    val capacityKg = vehicle.capacityKg

    // ── NOTHING RATHER THAN "— of 750 kg" ─────────────────────────────────
    //
    // `MovingItem.weightKg` is a plain non-null Double, and Gson's Unsafe path
    // skips Kotlin defaults, so a server catalog row that omits `weight_kg`
    // arrives as 0.0 without complaint. A load made only of such rows rendered
    // an em-dash where the headline figure belongs, under a heading promising a
    // total weight. A block that cannot state its fact should not be drawn.
    if (loadKg <= 0.0 || capacityKg <= 0.0) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AppColors.SurfaceVariant)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Total weight of your items",
                style = MaterialTheme.typography.labelMedium,
                color = AppColors.TextSecondary
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = kgLabel(loadKg),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (loadKg > capacityKg) AppColors.AccentDark
                    else AppColors.TextPrimary
                )
                Text(
                    text = "  of ${kgLabel(capacityKg)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextSecondary
                )
            }
        }

        // ── THE CAPTION HAS TO SURVIVE THE OVERSIZED CASE ─────────────────
        //
        // `recommend` has a branch for a load bigger than the whole fleet: it
        // returns the largest vehicle with `fitsInOneTrip = false` WITHOUT
        // checking payload, because refusing the customer here is worse than
        // taking the booking and having operations call them. That is the right
        // trade, and it means this card can legitimately be handed 1900 kg
        // against a 1500 kg limit.
        //
        // The first version branched only on `isVolumeBound`, which is true for
        // almost every house move — so that card read "1900 kg of 1500 kg" above
        // "Well within the weight limit", and the meter, which coerces to 100,
        // could not contradict it either. Two of the three elements were saying
        // something false about the third.
        //
        // The overweight caption stays constructive rather than a warning: the
        // flow never tells a customer their booking is a problem, it tells them
        // a person will be in touch. Amber, because that is this palette's
        // colour for "pay attention", and it is the one place on this screen
        // that earns it.
        val isOverweight = loadKg > capacityKg
        val caption = when {
            isOverweight ->
                "This is a big load for one vehicle — our team will call to " +
                        "confirm the details before pickup."
            estimate.isVolumeBound ->
                "Well within the weight limit — space is what decides the vehicle here."
            else -> "Comfortably within what this vehicle can carry."
        }

        Spacer(Modifier.height(4.dp))
        Text(
            text = caption,
            style = MaterialTheme.typography.labelSmall,
            color = if (isOverweight) AppColors.AccentDark else AppColors.TextHint,
            fontSize = 11.sp,
            lineHeight = 15.sp
        )
    }
}

/** "29 kg", "800 g", "4.5 kg" — never a decimal a customer would not write. */
private fun kgLabel(kg: Double): String = when {
    kg <= 0 -> "—"
    kg < 1.0 -> "${(kg * 1000).roundToInt()} g"
    kg < 10.0 -> "${(kg * 10).roundToInt() / 10.0} kg"
    else -> "${kg.roundToInt()} kg"
}

/**
 * One of the larger vehicles the load also fits into.
 *
 * Carries the same two facts as the recommended card — capacity and how full it
 * would be — because that is the whole basis of the comparison. A row that only
 * said "Pickup ›" would be asking the customer to upgrade on the strength of a
 * name.
 */
@Composable
private fun AlternativeVehicleRow(
    vehicle: VehicleLoadProfile,
    estimate: LoadEstimate,
    utilizationPercent: Int,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MovingTokens.ItemCorner))
            .background(AppColors.Surface)
            .border(
                BorderStroke(1.dp, AppColors.Border),
                RoundedCornerShape(MovingTokens.ItemCorner)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(AppColors.SurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(text = vehicle.icon.ifBlank { "🚚" }, fontSize = 19.sp)
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp)
        ) {
            Text(
                text = vehicle.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = MovingRecommendationEngine.capacityLabel(vehicle),
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.TextSecondary,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(3.dp))
            // The weight pair only when both halves are real — see CapacityFacts
            // for why a catalog row can arrive weighing nothing.
            val hasWeights = estimate.totalWeightKg > 0.0 && vehicle.capacityKg > 0.0
            Text(
                text = if (hasWeights) {
                    "${kgLabel(estimate.totalWeightKg)} of " +
                            "${kgLabel(vehicle.capacityKg)} · $utilizationPercent% full"
                } else {
                    "$utilizationPercent% full"
                },
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.TextHint,
                fontSize = 11.sp
            )
        }

        Spacer(Modifier.width(10.dp))

        // "Choose" rather than a chevron: the row is a decision, not navigation.
        Text(
            text = "Choose",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = AppColors.Primary,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(AppColors.PrimaryLight)
                .padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

/**
 * Vehicle artwork with the same three-tier fallback the fare sheet uses:
 * real image → the server's emoji → a generic truck glyph.
 *
 * Rendering the emoji until the image reports Success (rather than only while it
 * is Loading) is what keeps the slot from flashing empty on a slow connection.
 */
@Composable
private fun VehicleArtwork(vehicle: VehicleLoadProfile) {
    // One value, not a painter plus a boolean about it: `hasArtwork` implied a
    // non-null painter, so the `&& painter != null` the branch below needed to
    // satisfy the compiler was dead code the compiler then warned about.
    val artwork = vehicle.imageUrl
        ?.takeIf { it.startsWith("http", ignoreCase = true) }
        ?.let { rememberAsyncImagePainter(model = it) }
        ?.takeIf { it.state is AsyncImagePainter.State.Success }

    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(AppColors.SurfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        when {
            artwork != null -> Image(
                painter = artwork,
                contentDescription = null,
                modifier = Modifier.size(52.dp)
            )
            vehicle.icon.isNotBlank() -> Text(text = vehicle.icon, fontSize = 30.sp)
            else -> Icon(
                Icons.Default.LocalShipping,
                contentDescription = null,
                tint = AppColors.Primary,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun SecondaryAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MovingTokens.ItemCorner))
            .border(
                BorderStroke(1.dp, AppColors.Border),
                RoundedCornerShape(MovingTokens.ItemCorner)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = AppColors.Primary,
            modifier = Modifier.size(17.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = AppColors.Primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun LoadingState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        CircularProgressIndicator(color = AppColors.Primary, strokeWidth = 3.dp)
        Text(
            text = "Working out the right vehicle…",
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.TextSecondary
        )
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("📡", fontSize = 40.sp)
        Spacer(Modifier.height(12.dp))
        Text(
            text = "We couldn't load vehicles",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = AppColors.Heading
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.TextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        // Says the list is safe. Without it the obvious reading of a failure
        // here is that four screens of typing have been lost.
        Text(
            text = "Your items are saved.",
            style = MaterialTheme.typography.labelMedium,
            color = AppColors.TextHint
        )
        Spacer(Modifier.height(18.dp))
        SecondaryAction(
            icon = Icons.Default.Refresh,
            label = "Try again",
            onClick = onRetry
        )
    }
}

@Composable
private fun EmptyState(onEditItems: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("📦", fontSize = 40.sp)
        Spacer(Modifier.height(12.dp))
        Text(
            text = "We couldn't size this load",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = AppColors.Heading
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Add the items you're moving and we'll pick the vehicle.",
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.TextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(18.dp))
        SecondaryAction(
            icon = Icons.Default.Edit,
            label = "Choose items",
            onClick = onEditItems
        )
    }
}
