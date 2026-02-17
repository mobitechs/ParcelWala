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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.mobitechs.parcelwala.ui.theme.*

@Composable
fun RatingDialog(
    bookingNumber: String,
    fare: Int,
    onDismiss: () -> Unit,
    onSubmit: (rating: Int, feedback: String) -> Unit,
    isSubmitting: Boolean = false,
    // Customer's existing rating of driver (for updates from history)
    existingCustomerRating: Double? = null,
    existingCustomerFeedback: String? = null,
    // Driver's rating of customer (info display only)
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
            colors = CardDefaults.cardColors(containerColor = Color.White)
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
                    tint = Orange500
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Delivery Completed!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Booking #$bookingNumber",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "₹$fare",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Green500
                )

                // ── Driver's rating of customer (info box) ──
                if (driverRatingForCustomer != null && driverRatingForCustomer > 0) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Green500.copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, Green500.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Driver rated you",
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
                                            Color(0xFFFFC107)
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
                    text = if (isUpdate) "Update your rating" else "Rate the driver",
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
                                Orange500
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
                    label = { Text("Feedback (Optional)") },
                    placeholder = { Text("Share your experience...") },
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
                        Text("Skip")
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
                            containerColor = Orange500
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
                            Text(if (isUpdate) "Update" else "Submit")
                        }
                    }
                }

                if (rating == 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Please select a rating to submit",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}