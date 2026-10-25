package com.mobitechs.parcelwala.ui.booking2

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import com.mobitechs.parcelwala.data.model.response.formatRupee
import com.mobitechs.parcelwala.ui.components.ctaButtonColors
import com.mobitechs.parcelwala.ui.theme.AppColors

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
 * WHAT USED TO BE THREE SCREENS IS NOW THREE CHIPS
 *
 * Goods type, coupon and payment method were BookingConfirmationScreen,
 * CouponScreen and a chunk of ReviewBookingScreen's 1,429 lines. Each now has
 * a sensible default (Documents, no coupon, Cash) and appears as a chip the
 * customer can tap if they care. Most will not, and that is the point: a
 * default nobody has to think about is worth more than an option everybody has
 * to evaluate.
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

            draft.vehicles.isEmpty() -> Text(
                "No vehicles available for this route right now.",
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextSecondary,
                modifier = Modifier.padding(vertical = 20.dp)
            )

            else -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                draft.vehicles.forEach { vehicle ->
                    VehicleRow(
                        vehicle = vehicle,
                        isSelected = vehicle.id == draft.selectedVehicleId,
                        onClick = { onSelectVehicle(vehicle.id) }
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

        // The CTA repeats the price. No surprises, ever.
        Button(
            onClick = onBook,
            enabled = draft.selectedVehicle != null,
            modifier = Modifier
                .fillMaxWidth()
                .height(SendParcelTokens.ButtonHeight),
            shape = RoundedCornerShape(SendParcelTokens.CornerRadius),
            colors = ctaButtonColors()
        ) {
            Text(
                text = draft.selectedVehicle?.let {
                    "Book ${it.name.lowercase()} · ${formatRupee(draft.total)}"
                } ?: "Select a vehicle",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
private fun VehicleRow(
    vehicle: VehicleOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    // A locked row is dimmed, not removed — see VehicleOption.disabledReason.
    val enabled = vehicle.isSelectable
    val contentAlpha = if (enabled) 1f else 0.45f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(
                BorderStroke(
                    if (isSelected) 2.dp else 1.dp,
                    when {
                        !enabled -> AppColors.Border
                        isSelected -> AppColors.Primary
                        else -> AppColors.Border
                    }
                ),
                RoundedCornerShape(12.dp)
            )
            .background(
                when {
                    !enabled -> AppColors.SurfaceVariant
                    isSelected -> AppColors.Primary.copy(alpha = 0.04f)
                    else -> Color.White
                }
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.alpha(contentAlpha)) { VehicleIcon(vehicle) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    vehicle.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (enabled) AppColors.TextPrimary else AppColors.TextHint
                )
                if (vehicle.isRecommended && enabled) {
                    Text(
                        vehicle.recommendLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        // Amber for a sized-to-fit recommendation, navy for the
                        // cheapest-is-popular default. The stronger claim gets
                        // the accent colour; navy is the colour of everything
                        // tappable and would not stand out among the rows.
                        color = if (vehicle.recommendLabel == "Recommended")
                            AppColors.AccentDark else AppColors.Primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (vehicle.recommendLabel == "Recommended")
                                    AppColors.AccentLight else AppColors.PrimaryLight
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Text(
                // On a locked row the reason IS the subtitle. Putting it
                // anywhere else — a tooltip, a snackbar on tap — means the
                // customer has to go looking for why the cheap option is grey,
                // and most will conclude they are being upsold instead.
                text = vehicle.disabledReason ?: buildString {
                    append(vehicle.capacityLabel)
                    vehicle.etaMinutes?.takeIf { it > 0 }?.let { append(" · $it min away") }
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (enabled) AppColors.TextSecondary else AppColors.Drop
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            formatRupee(vehicle.fare),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            // The price stays readable on a locked row. Dimming it too would
            // hide the very comparison that makes the lock feel fair.
            color = if (enabled) AppColors.TextPrimary else AppColors.TextHint
        )
    }
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
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
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
            maxLines = 1
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

/**
 * The vehicle artwork, with two fallbacks behind it.
 *
 * WHY THIS IS NOT JUST AN AsyncImage
 *
 * The row used to hand whichever of the two icon fields was non-blank to
 * AsyncImage. The server sends BOTH a root-relative image path and an emoji,
 * and the path won that check — so Coil was asked to fetch
 * "/images/vehicles/bike.png" with nothing to resolve it against. It failed
 * silently, drew nothing, and every row on the fare sheet lost its vehicle.
 *
 * Three tiers, in order of fidelity:
 *
 *   1. the real artwork, once it has actually decoded
 *   2. the emoji the server sends alongside it — which is what the rest of the
 *      app already displays, so the fallback is not a downgrade in style
 *   3. a generic truck glyph, if the server sent neither
 *
 * Rendering the emoji until the image reports Success (rather than while it is
 * merely Loading) is deliberate: it means the slot is never empty, so the list
 * does not flash a row of holes on every fare refresh.
 */
@Composable
private fun VehicleIcon(vehicle: VehicleOption) {
    // One value, not a painter plus a boolean about it: `hasArtwork` already
    // implied a non-null painter, so the `&& painter != null` the branch needed
    // to satisfy the compiler was dead code the compiler then warned about.
    val artwork = vehicle.iconUrl
        ?.let { rememberAsyncImagePainter(model = it) }
        ?.takeIf { it.state is AsyncImagePainter.State.Success }

    Box(Modifier.size(34.dp), contentAlignment = Alignment.Center) {
        when {
            artwork != null -> Image(
                painter = artwork,
                contentDescription = null,
                modifier = Modifier.size(30.dp)
            )

            !vehicle.iconEmoji.isNullOrBlank() -> Text(
                text = vehicle.iconEmoji,
                fontSize = 24.sp
            )

            else -> Icon(
                Icons.Default.LocalShipping, null,
                tint = AppColors.Primary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
