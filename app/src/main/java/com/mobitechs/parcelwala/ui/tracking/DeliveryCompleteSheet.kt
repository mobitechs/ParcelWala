package com.mobitechs.parcelwala.ui.tracking

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mobitechs.parcelwala.data.model.response.formatRupee
import com.mobitechs.parcelwala.ui.theme.AppColors

/**
 * ════════════════════════════════════════════════════════════════════════════
 * DELIVERY COMPLETE
 * ════════════════════════════════════════════════════════════════════════════
 *
 * Replaces the old `RatingDialog` on the tracking screen.
 *
 * WHAT WAS WRONG WITH THE OLD ONE
 *
 *  - A large navy star as the hero. A star is the RATING control; using it as
 *    the success mark meant the same shape carried two unrelated meanings on
 *    one screen. A tick says "done"; a star says "score this".
 *  - "Booking #46" got more visual weight than the amount.
 *  - It said "Delivery Completed!" but never confirmed what was PAID or HOW —
 *    the last thing a customer wants after a cash handover is ambiguity about
 *    whether the payment registered. The driver's screen showed a payment chip
 *    and a breakdown; the customer's showed nothing.
 *  - Submit sat disabled with "Please select a rating to submit" underneath —
 *    an error message for something the customer had not done wrong yet.
 *
 * The rating is genuinely optional here. Skip is a real, equal-weight choice
 * rather than a consolation prize, because a delivery service that nags for
 * stars after every parcel trains people to dismiss the dialog without reading.
 */
@Composable
fun DeliveryCompleteSheet(
    bookingId: String,
    totalPaid: Double,
    paymentMethod: String,
    driverName: String,
    waitingCharge: Double,
    isSubmitting: Boolean,
    onSubmit: (rating: Int, feedback: String) -> Unit,
    onSkip: () -> Unit
) {
    var rating by remember { mutableIntStateOf(0) }
    var feedback by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = { },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Color.White,
                        RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                    )
                    .padding(24.dp)
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ── Success mark: a tick, not a star ───────────────────────
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(AppColors.Pickup.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Check, null,
                        tint = AppColors.Pickup,
                        modifier = Modifier.size(34.dp)
                    )
                }
                Spacer(Modifier.height(14.dp))

                Text(
                    "Parcel delivered",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Booking #$bookingId",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextHint
                )

                Spacer(Modifier.height(16.dp))

                // ── What was paid, and how. The point of the screen. ───────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppColors.Gray50, RoundedCornerShape(14.dp))
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        if (paymentMethod.equals("cash", true)) "Paid in cash"
                        else "Paid online",
                        style = MaterialTheme.typography.labelMedium,
                        color = AppColors.Pickup,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        formatRupee(totalPaid),
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    )
                    if (waitingCharge > 0) {
                        Spacer(Modifier.height(6.dp))
                        HorizontalDivider(color = AppColors.Border)
                        Spacer(Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Includes waiting charges",
                                style = MaterialTheme.typography.bodySmall,
                                color = AppColors.TextSecondary
                            )
                            Text(
                                formatRupee(waitingCharge),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = AppColors.TextPrimary
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                Text(
                    "How was ${driverName.ifBlank { "your driver" }}?",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.TextPrimary
                )
                Spacer(Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    (1..5).forEach { star ->
                        val selected = star <= rating
                        val scale by animateFloatAsState(
                            targetValue = if (selected) 1.12f else 1f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                            label = "star$star"
                        )
                        Icon(
                            imageVector = if (selected) Icons.Default.Star
                            else Icons.Outlined.StarBorder,
                            contentDescription = "$star star${if (star > 1) "s" else ""}",
                            tint = if (selected) AppColors.StarYellow else AppColors.Border,
                            modifier = Modifier
                                .size(42.dp)
                                .scale(scale)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { rating = star }
                        )
                    }
                }

                // The feedback box only appears once a rating is given. Showing
                // an empty text field first asks for effort before the customer
                // has decided they care.
                if (rating > 0) {
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = feedback,
                        onValueChange = { feedback = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                if (rating <= 3) "What went wrong?" else "Anything to add?",
                                color = AppColors.TextHint
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AppColors.Primary,
                            unfocusedBorderColor = AppColors.Border,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )
                }

                Spacer(Modifier.height(18.dp))

                Button(
                    onClick = { if (rating > 0) onSubmit(rating, feedback) else onSkip() },
                    enabled = !isSubmitting,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary)
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        // One button that always works. The old one sat disabled
                        // with "Please select a rating to submit" underneath —
                        // scolding the customer for something they had not done
                        // wrong. Rating is optional; the button reflects that.
                        Text(
                            text = if (rating > 0) "Submit rating" else "Done",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                    }
                }

                if (rating > 0) {
                    TextButton(onClick = onSkip, enabled = !isSubmitting) {
                        Text(
                            "Skip",
                            color = AppColors.TextSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Thanks for using ParcelWala",
                        style = MaterialTheme.typography.labelSmall,
                        color = AppColors.TextHint,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
