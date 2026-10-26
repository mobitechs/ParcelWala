package com.mobitechs.parcelwala.ui.booking2

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobitechs.parcelwala.data.model.response.formatRupee
import com.mobitechs.parcelwala.ui.components.VehicleImage
import com.mobitechs.parcelwala.ui.components.ctaButtonColors
import com.mobitechs.parcelwala.ui.theme.AppColors
import kotlinx.coroutines.delay

/**
 * ════════════════════════════════════════════════════════════════════════════
 * VEHICLE + FARE SHEET
 * ════════════════════════════════════════════════════════════════════════════
 *
 * THE MOST IMPORTANT SCREEN IN THE APP.
 *
 * This is where the customer learns the price, and in the new flow they reach
 * it on tap two instead of tap eight. Everything about it is arranged around
 * that: each vehicle row carries its fare on the right, the CTA repeats the
 * price so there is never a surprise, and the whole thing sits over a live
 * route preview so the number has visible context.
 *
 * VARIANTS — SCOOTER / BIKE, OPEN / CLOSED BODY (design option A)
 *
 * One row per VEHICLE, not per variant. Variants of the same vehicle (same
 * `name`, different `vehicle_type_id`) are grouped into a [VehicleFamily]. The
 * collapsed row reads "Up to 100 kg · Open or closed body" and shows the
 * cheapest variant's price. Selecting it expands an inline chooser — two chips
 * with photo and price ("Closed body ₹150") and one line on what the
 * difference means. A vehicle with a single variant has nothing to choose, so
 * its row simply names it ("Up to 20 kg · Scooter").
 *
 * The list stays one glance long, every price stays visible, and the choice
 * only appears where there is one to make.
 *
 * WHAT USED TO BE THREE SCREENS IS NOW THREE CHIPS
 *
 * Goods type, coupon and payment method were BookingConfirmationScreen,
 * CouponScreen and a chunk of ReviewBookingScreen's 1,429 lines. Each now has
 * a sensible default (Documents, no coupon, Cash) and appears as a chip the
 * customer can tap if they care.
 *
 * GST is deliberately absent. It is a per-ACCOUNT fact that belongs in profile
 * settings, not a decision to make on every single booking.
 */
@Composable
fun VehicleFareSheet(
    draft: BookingDraft,
    onSelectVehicle: (String) -> Unit,
    onEditGoodsType: () -> Unit,
    onEditPayment: () -> Unit,
    onEditCoupon: () -> Unit,
    onBook: () -> Unit,
    modifier: Modifier = Modifier
) {
    val families = draft.vehicleFamilies
    val selectedFamily = families.firstOrNull { family ->
        family.variants.any { it.id == draft.selectedVehicleId }
    }

    // The variant the customer last picked in each vehicle (family key →
    // vehicle id). Without it, choosing "Closed body", glancing at the 2
    // wheeler and tapping back would silently reset to the cheaper open body —
    // a different vehicle at a different price than the one they chose.
    val lastPicked = remember { mutableStateMapOf<String, String>() }
    LaunchedEffect(selectedFamily?.key, draft.selectedVehicleId) {
        val family = selectedFamily ?: return@LaunchedEffect
        draft.selectedVehicleId?.let { lastPicked[family.key] = it }
    }

    // The list can now grow (an expanded row is ~110dp taller), so it scrolls
    // inside a cap instead of pushing the sheet off the top of a small phone.
    val maxListHeight = (LocalConfiguration.current.screenHeightDp * 0.55f).dp

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
            .background(Color.White)
            .padding(horizontal = 20.dp)
            .padding(top = 10.dp, bottom = 18.dp)
            .navigationBarsPadding()
    ) {
        Box(
            Modifier
                .width(38.dp)
                .height(4.dp)
                .background(AppColors.DragHandle, RoundedCornerShape(2.dp))
                .align(Alignment.CenterHorizontally)
        )
        Spacer(Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                "Choose a vehicle",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = AppColors.Heading
            )
            // Trip distance as a STATIC label. Deliberately not a live ETA —
            // there is no driver yet, so a countdown here would be fiction.
            draft.routeDistanceKm?.let {
                Text(
                    String.format(java.util.Locale.getDefault(), "%.1f km trip", it),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextSecondary
                )
            }
        }
        Spacer(Modifier.height(12.dp))

        when {
            draft.isLoadingFares -> FareLoading()

            families.isEmpty() -> Text(
                "No vehicles available for this route right now.",
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextSecondary,
                modifier = Modifier.padding(vertical = 20.dp)
            )

            else -> Column(
                modifier = Modifier
                    .heightIn(max = maxListHeight)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                families.forEach { family ->
                    VehicleFamilyRow(
                        family = family,
                        selectedVehicleId = draft.selectedVehicleId,
                        rememberedVariantId = lastPicked[family.key],
                        onSelectVehicle = onSelectVehicle
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // ── Three chips replacing three screens ────────────────────────────
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OptionChip(
                icon = Icons.Default.Wallet,
                label = if (draft.paymentMethod.equals("cash", true)) "Cash" else "Online",
                modifier = Modifier.weight(1f),
                onClick = onEditPayment
            )
            OptionChip(
                icon = Icons.Default.Inventory2,
                // The moving label carries the whole item list
                // ("House Shifting · Sofa x1, Double bed x1 …"), which is what
                // the driver needs and far more than a chip can show. The head
                // of it is the part that identifies the job.
                label = if (draft.isMovingBooking) {
                    draft.goodsType.substringBefore(" · ")
                } else {
                    draft.goodsType
                },
                modifier = Modifier.weight(1f),
                // Read-only for a move: there is nothing left to choose, and
                // opening the goods picker here would discard the item summary
                // and the computed weight.
                onClick = if (draft.isMovingBooking) null else onEditGoodsType
            )
            OptionChip(
                icon = Icons.Default.LocalOffer,
                label = draft.couponCode ?: "Coupon",
                isAccent = draft.couponCode != null,
                modifier = Modifier.weight(1f),
                onClick = onEditCoupon
            )
        }

        if (draft.couponDiscount > 0) {
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Coupon ${draft.couponCode.orEmpty()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.Pickup
                )
                Text(
                    "− ${formatRupee(draft.couponDiscount)}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.Pickup
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // The CTA repeats the price. No surprises, ever. When the vehicle had a
        // real choice of variants, the CTA names the one picked too, so
        // "Book 3 wheeler · closed body · ₹150" is unambiguous.
        Button(
            onClick = onBook,
            enabled = draft.selectedVehicle != null,
            modifier = Modifier
                .fillMaxWidth()
                .height(SendParcelTokens.ButtonHeight),
            shape = RoundedCornerShape(SendParcelTokens.CornerRadius),
            colors = ctaButtonColors()
        ) {
            // No colour override on these Texts: ctaButtonColors() already gives
            // OnCta (navy). White on the amber CTA is 2.2:1 contrast — hard to
            // read in sunlight — which the theme guide rules out.
            val vehicle = draft.selectedVehicle
            if (vehicle == null) {
                Text("Select a vehicle", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            } else {
                // Two Texts so only the description can shrink. With one string,
                // a long name or a larger phone font cut off the END — the price,
                // which is the one thing this button promises to show.
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = buildString {
                            append("Book ").append(vehicle.name.lowercase())
                            vehicle.variantLabel
                                ?.takeIf { selectedFamily?.hasChoice == true }
                                ?.let { append(" · ").append(it.lowercase()) }
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Text(
                        text = " · " + formatRupee(draft.total),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

/**
 * One vehicle. Collapsed: photo, name, capacity line, price. Selected with more
 * than one variant: expands into [VariantChooser].
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VehicleFamilyRow(
    family: VehicleFamily,
    selectedVehicleId: String?,
    rememberedVariantId: String?,
    onSelectVehicle: (String) -> Unit
) {
    val selectedVariant = family.variants.firstOrNull { it.id == selectedVehicleId }
    val isSelected = selectedVariant != null
    // What the collapsed row pictures and prices: the chosen variant when this
    // row is selected, otherwise the one a tap would select — the customer's
    // own earlier pick in this vehicle first, then the family default.
    val shown = selectedVariant
        ?: family.variants.firstOrNull { it.id == rememberedVariantId && it.isSelectable }
        ?: family.defaultVariant
    val expanded = isSelected && family.hasChoice

    // A locked row is dimmed, not removed — see VehicleOption.disabledReason.
    val enabled = family.isSelectable
    val contentAlpha = if (enabled) 1f else 0.45f
    val shape = RoundedCornerShape(12.dp)

    val bringIntoView = remember { BringIntoViewRequester() }
    LaunchedEffect(expanded) {
        if (expanded) {
            delay(220) // let animateContentSize finish growing the row first
            bringIntoView.bringIntoView()
        }
    }
    // The card is a COLUMN: the vehicle line on top, the variant chooser
    // underneath. It used to be a Row — the vehicle line (fillMaxWidth) took
    // all the width, the chooser was squeezed to 0dp beside it, and its text
    // wrapped one letter per line. That made the card very tall with nothing
    // visible in it: the "empty space" under a selected Tata Ace / 3 Wheeler.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .bringIntoViewRequester(bringIntoView)
            .clip(shape)
            .border(
                BorderStroke(
                    if (isSelected) 2.dp else 1.dp,
                    if (enabled && isSelected) AppColors.Primary else AppColors.Border
                ),
                shape
            )
            .background(
                when {
                    !enabled -> AppColors.SurfaceVariant
                    isSelected -> AppColors.Primary.copy(alpha = 0.04f)
                    else -> Color.White
                }
            )
            .animateContentSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled) { onSelectVehicle(shown.id) }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            VehicleImage(
                imageUrl = shown.iconUrl,
                emoji = shown.iconEmoji,
                modifier = Modifier
                    .alpha(contentAlpha)
                    .size(width = 64.dp, height = 44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (enabled) AppColors.SurfaceVariant else Color.White)
                    .padding(3.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        family.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = if (enabled) AppColors.TextPrimary else AppColors.TextHint,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (family.isRecommended && enabled) {
                        RecommendBadge(family.recommendLabel)
                    }
                }
                Text(
                    // On a locked row the reason IS the subtitle. Putting it
                    // anywhere else — a tooltip, a snackbar on tap — means the
                    // customer has to go looking for why the cheap option is grey,
                    // and most will conclude they are being upsold instead.
                    text = family.disabledReason ?: subtitleFor(family, shown, isSelected),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (enabled) AppColors.TextSecondary else AppColors.Drop,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                formatRupee(shown.fare),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                // The price stays readable on a locked row. Dimming it too would
                // hide the very comparison that makes the lock feel fair.
                color = if (enabled) AppColors.TextPrimary else AppColors.TextHint
            )
        }

        if (expanded) {
            VariantChooser(
                family = family,
                selected = shown,
                onSelectVehicle = onSelectVehicle
            )
        }
    }
}

/**
 * "Up to 100 kg · Open or closed body · 7 min away".
 *
 * The variant part depends on the row's state: a collapsed row with a choice
 * lists the options, a single-variant vehicle names its one variant, and an
 * expanded row leaves it out because the chips right below say it.
 */
private fun subtitleFor(family: VehicleFamily, shown: VehicleOption, isSelected: Boolean): String =
    buildList {
        add(shown.capacityLabel)
        when {
            family.hasChoice && !isSelected -> add(family.choiceSummary)
            !family.hasChoice -> shown.variantLabel?.let { add(it) }
        }
        shown.etaMinutes?.takeIf { it > 0 }?.let { add("$it min away") }
    }.filter { it.isNotBlank() }.joinToString(" · ")

/**
 * The inline chooser under a selected vehicle: one chip per variant with its
 * photo and price, then a line on what the selected variant is good for.
 */
@Composable
private fun VariantChooser(
    family: VehicleFamily,
    selected: VehicleOption,
    onSelectVehicle: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            family.choiceTitle.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.6.sp,
            color = AppColors.TextSecondary
        )

        if (family.variants.size <= 3) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                family.variants.forEach { variant ->
                    VariantChip(
                        variant = variant,
                        label = family.chipLabel(variant),
                        isSelected = variant.id == selected.id,
                        modifier = Modifier.weight(1f),
                        onClick = { onSelectVehicle(variant.id) }
                    )
                }
            }
        } else {
            // Four or more variants would be crushed into equal thirds — scroll.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                family.variants.forEach { variant ->
                    VariantChip(
                        variant = variant,
                        label = family.chipLabel(variant),
                        isSelected = variant.id == selected.id,
                        modifier = Modifier.width(150.dp),
                        onClick = { onSelectVehicle(variant.id) }
                    )
                }
            }
        }

        selected.hint?.takeIf { it.isNotBlank() }?.let { hint ->
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.Outlined.Info,
                    contentDescription = null,
                    tint = AppColors.Gray600,
                    modifier = Modifier
                        .padding(top = 1.dp)
                        .size(14.dp)
                )
                Text(
                    hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.Gray600
                )
            }
        }
    }
}

/**
 * The chip's label. Under a "BODY TYPE" heading the chips say "Open" and
 * "Closed" — the heading already says "body", and the full "Closed body" next
 * to the photo cut off as "Closed bo…" on a 360dp phone. Same suffix rule as
 * [VehicleFamily.choiceSummary]. Other labels ("Scooter", "Bike") are kept.
 */
private fun VehicleFamily.chipLabel(variant: VehicleOption): String {
    val label = variant.variantLabel ?: "Standard"
    val suffix = " body"
    val allBody = variants.all { it.variantLabel?.endsWith(suffix, ignoreCase = true) == true }
    return if (allBody) label.dropLast(suffix.length) else label
}

@Composable
private fun VariantChip(
    variant: VehicleOption,
    label: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val enabled = variant.isSelectable
    val shape = RoundedCornerShape(10.dp)
    // Just the variant's own fare. A "+₹…" difference to the cheapest variant
    // used to follow it; it was dropped because it only magnified fare-setup
    // mistakes (one Tata Ace showed "+₹1982") without helping the choice.
    val priceLine = if (!enabled) variant.disabledReason.orEmpty() else formatRupee(variant.fare)

    Row(
        modifier = modifier
            .heightIn(min = 52.dp)
            .clip(shape)
            .border(
                BorderStroke(
                    if (isSelected) 2.dp else 1.dp,
                    if (isSelected) AppColors.Primary else AppColors.Border
                ),
                shape
            )
            .background(if (isSelected) Color.White else AppColors.Gray50)
            .selectable(
                selected = isSelected,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick
            )
            .alpha(if (enabled) 1f else 0.45f)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        VehicleImage(
            imageUrl = variant.iconUrl,
            emoji = variant.iconEmoji,
            emojiSize = 18.sp,
            glyphSize = 18.dp,
            modifier = Modifier.size(width = 42.dp, height = 28.dp)
        )
        Column(Modifier.weight(1f)) {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = AppColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                priceLine,
                style = MaterialTheme.typography.bodySmall,
                color = if (enabled) AppColors.Gray600 else AppColors.Drop,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RecommendBadge(label: String) {
    // Amber for a sized-to-fit recommendation, navy for the cheapest-is-popular
    // default. The stronger claim gets the accent colour; navy is the colour of
    // everything tappable and would not stand out among the rows.
    val isRecommended = label == "Recommended"
    Text(
        label,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = if (isRecommended) AppColors.AccentDark else AppColors.Primary,
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isRecommended) AppColors.AccentLight else AppColors.PrimaryLight)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

/**
 * A chip. A null [onClick] makes it a label rather than a control — used for
 * facts the customer has already settled and cannot usefully change here.
 */
@Composable
private fun OptionChip(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    isAccent: Boolean = false,
    onClick: (() -> Unit)?
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .border(
                BorderStroke(1.dp, if (isAccent) AppColors.Primary else AppColors.Border),
                RoundedCornerShape(20.dp)
            )
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            )
            // 8dp, not 10: three chips share ~100dp each on a 360dp phone, and
            // "Documents" needs the extra 4dp to show in full.
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            icon, null,
            tint = if (isAccent) AppColors.Primary else AppColors.TextSecondary,
            modifier = Modifier.size(15.dp)
        )
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = if (isAccent) AppColors.Primary else AppColors.TextSecondary,
            maxLines = 1,
            // A long goods type ("Electronics", "Furniture") used to be cut
            // mid-letter at the chip edge; an ellipsis at least reads as "more".
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun FareLoading() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        CircularProgressIndicator(color = AppColors.Primary, strokeWidth = 3.dp)
        Text(
            "Getting prices…",
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.TextSecondary
        )
    }
}