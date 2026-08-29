package com.mobitechs.parcelwala.ui.booking2

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mobitechs.parcelwala.data.model.response.formatRupee
import com.mobitechs.parcelwala.ui.theme.AppColors

/**
 * ════════════════════════════════════════════════════════════════════════════
 * CONFIRM BOOKING
 * ════════════════════════════════════════════════════════════════════════════
 *
 * The delivery-details and pickup-details screens that used to live here have
 * moved to [AddressContactScreen], which shows the map, the address and the
 * contact together instead of asking about a location the customer cannot see.
 * Both ends now render from that one component, so there is no longer a pair of
 * near-identical screens here to keep in sync.
 *
 * What remains is the last step. Everything on it is a FACT, not a DECISION —
 * that is the test for the end of a funnel. The customer is verifying, not
 * choosing, and anything that asks them to choose again belongs earlier.
 */

// ═══════════════════════════════════════════════════════════════════════════
// CONFIRM
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Everything here is a FACT, not a DECISION — that is the test for the last
 * step of a funnel. Both contacts now carry their phone numbers, so the
 * customer can verify the two things most likely to be wrong (a mistyped
 * number, the wrong person) without going back.
 */
@Composable
fun ConfirmBookingScreen(
    draft: BookingDraft,
    isSubmitting: Boolean,
    errorMessage: String?,
    onEditGoodsType: () -> Unit,
    onEditPayment: () -> Unit,
    onConfirm: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val senderValid = draft.sender.name.trim().length >= 2 &&
            draft.sender.phone.filter { it.isDigit() }.length == 10

    SendParcelScaffold(
        title = "Confirm booking",
        onBack = onBack,
        modifier = modifier,
        bottomBar = {
            SendParcelBottomBar(
                label = "Confirm booking",
                enabled = draft.canConfirm && senderValid,
                onClick = onConfirm,
                isLoading = isSubmitting,
                helperLabel = "Total",
                helperValue = formatRupee(draft.total)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = SendParcelTokens.ScreenPadding)
        ) {
            Spacer(Modifier.height(12.dp))

            SendParcelCard {
                Column(Modifier.padding(14.dp)) {
                    StopRow(
                        dotColor = AppColors.Pickup,
                        title = contactLine(draft.sender.name.ifBlank { "You" }, draft.sender.phone),
                        subtitle = draft.pickup?.address.orEmpty()
                    )
                    Box(
                        Modifier
                            .padding(start = 4.dp, top = 5.dp, bottom = 5.dp)
                            .width(1.dp)
                            .height(16.dp)
                            .background(AppColors.Border)
                    )
                    StopRow(
                        dotColor = AppColors.Drop,
                        title = contactLine(
                            draft.receiver.name.ifBlank { "Receiver" },
                            draft.receiver.phone
                        ),
                        subtitle = draft.drop?.address.orEmpty()
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            SendParcelCard {
                Column(Modifier.padding(horizontal = 14.dp)) {
                    SummaryRow(
                        icon = Icons.Default.Inventory2,
                        label = draft.selectedVehicle?.name?.takeIf { it.isNotBlank() }
                            ?: "Vehicle",
                        value = formatRupee(draft.subtotal)
                    )
                    HorizontalDivider(color = AppColors.Border)
                    // Goods type and weight, changeable from here.
                    //
                    // They were previously readable only as a fragment appended
                    // to the vehicle name, with no way to alter either without
                    // going back through the fare step — so a customer who
                    // realised at the last moment that they were sending food
                    // rather than documents, or something heavier than the
                    // category default assumes, had no way to say so. The rider
                    // finds out on arrival, which is the worst possible moment.
                    SummaryRow(
                        icon = Icons.Default.Category,
                        label = listOfNotNull(
                            draft.goodsType.takeIf { it.isNotBlank() } ?: "Item type",
                            draft.goodsWeightKg
                                ?.takeIf { it > 0.0 }
                                ?.let { "~${formatWeight(it)} kg" }
                        ).joinToString(" · "),
                        value = "Change",
                        valueColor = AppColors.Primary,
                        onClick = onEditGoodsType
                    )
                    if (draft.couponDiscount > 0) {
                        HorizontalDivider(color = AppColors.Border)
                        SummaryRow(
                            icon = Icons.Default.LocalOffer,
                            label = draft.couponCode.orEmpty().ifBlank { "Coupon" },
                            value = "− ${formatRupee(draft.couponDiscount)}",
                            valueColor = AppColors.Pickup
                        )
                    }
                    HorizontalDivider(color = AppColors.Border)
                    SummaryRow(
                        icon = Icons.Default.Wallet,
                        label = if (draft.paymentMethod.equals("cash", true)) {
                            "Cash on delivery"
                        } else "Pay online",
                        value = "Change",
                        valueColor = AppColors.Primary,
                        onClick = onEditPayment
                    )
                }
            }

            val blocker = when {
                errorMessage != null -> errorMessage
                draft.selectedVehicle == null -> "Go back and choose a vehicle to see the price."
                !draft.receiver.isValid -> "Add the receiver's name and 10-digit mobile number."
                !senderValid -> "Add the sender's name and 10-digit mobile number."
                else -> null
            }
            blocker?.let {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.Error,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppColors.Error.copy(alpha = 0.07f), RoundedCornerShape(10.dp))
                        .padding(12.dp)
                )
            }

            Spacer(Modifier.height(28.dp))
        }
    }
}

private fun contactLine(name: String, phone: String): String {
    val digits = phone.filter { it.isDigit() }
    return if (digits.length == 10) "$name · $digits" else name
}

@Composable
private fun StopRow(dotColor: Color, title: String, subtitle: String) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            Modifier
                .padding(top = 5.dp)
                .size(9.dp)
                .background(dotColor, CircleShape)
        )
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SummaryRow(
    icon: ImageVector,
    label: String,
    value: String,
    valueColor: Color = AppColors.TextPrimary,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = AppColors.TextSecondary, modifier = Modifier.size(19.dp))
        Spacer(Modifier.width(12.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.TextPrimary,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = valueColor
        )
    }
}