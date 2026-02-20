// ui/components/RatingDialog.kt (CUSTOMER APP VERSION)
package com.mobitechs.parcelwala.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.mobitechs.parcelwala.R
import com.mobitechs.parcelwala.ui.theme.*

@Composable
fun RatingDialog(
    bookingNumber: String,
    fare: Int,
    onDismiss: () -> Unit,
    onSubmit: (rating: Int, feedback: String) -> Unit,
    isSubmitting: Boolean = false,
    existingCustomerRating: Double? = null,
    existingCustomerFeedback: String? = null,
    driverRatingForCustomer: Double? = null,
    driverFeedbackForCustomer: String? = null
) {
    var rating by remember {
        mutableIntStateOf(existingCustomerRating?.toInt() ?: 0)
    }
    var feedback by remember {
        mutableStateOf(existingCustomerFeedback ?: "")
    }

    val isUpdate = existingCustomerRating != null && existingCustomerRating > 0

    Dialog(onDismissRequest = { if (!isSubmitting) onDismiss() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = AppColors.Surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Success Icon
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = AppColors.Primary
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.label_delivery_completed),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = stringResource(R.string.label_booking_number, bookingNumber),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "₹$fare",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.Pickup
                )

                // ── Driver's rating of customer (info box) ──
                if (driverRatingForCustomer != null && driverRatingForCustomer > 0) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = AppColors.Pickup.copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, AppColors.Pickup.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = stringResource(R.string.label_driver_rated_you),
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary,
                                fontSize = 12.sp
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                repeat(5) { index ->
                                    Icon(
                                        imageVector = if (index < driverRatingForCustomer.toInt()) {
                                            Icons.Filled.Star
                                        } else {
                                            Icons.Outlined.StarOutline
                                        },
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = if (index < driverRatingForCustomer.toInt()) {
                                            AppColors.Amber
                                        } else {
                                            DividerColor
                                        }
                                    )
                                    if (index < 4) Spacer(modifier = Modifier.width(2.dp))
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "$driverRatingForCustomer",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    fontSize = 14.sp
                                )
                            }

                            if (!driverFeedbackForCustomer.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "\"$driverFeedbackForCustomer\"",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = DividerColor)
                Spacer(modifier = Modifier.height(20.dp))

                // ── Customer rates driver ──
                Text(
                    text = if (isUpdate) stringResource(R.string.label_update_rating) else stringResource(R.string.label_rate_driver),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Star Rating
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(5) { index ->
                        Icon(
                            imageVector = if (index < rating) {
                                Icons.Filled.Star
                            } else {
                                Icons.Outlined.StarOutline
                            },
                            contentDescription = "${index + 1} stars",
                            modifier = Modifier
                                .size(40.dp)
                                .clickable(enabled = !isSubmitting) {
                                    rating = index + 1
                                },
                            tint = if (index < rating) {
                                AppColors.Primary
                            } else {
                                DividerColor
                            }
                        )
                        if (index < 4) Spacer(modifier = Modifier.width(10.dp))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Feedback TextField
                OutlinedTextField(
                    value = feedback,
                    onValueChange = { feedback = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.label_feedback_optional)) },
                    placeholder = { Text(stringResource(R.string.label_feedback_placeholder)) },
                    maxLines = 3,
                    enabled = !isSubmitting,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        enabled = !isSubmitting,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(R.string.label_skip))
                    }

                    Button(
                        onClick = {
                            if (rating > 0) {
                                onSubmit(rating, feedback.trim())
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = rating > 0 && !isSubmitting,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.Primary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(if (isUpdate) stringResource(R.string.label_update) else stringResource(R.string.label_submit))
                        }
                    }
                }

                if (rating == 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.label_select_rating_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}