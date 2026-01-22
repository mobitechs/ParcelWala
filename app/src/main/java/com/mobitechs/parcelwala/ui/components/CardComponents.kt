// ui/components/CardComponents.kt
package com.mobitechs.parcelwala.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobitechs.parcelwala.data.model.request.SavedAddress
import com.mobitechs.parcelwala.ui.theme.AppColors

// ============ ADDRESS CARDS ============


@Composable
fun AddressesCard(pickupContactName:String?,pickupContactPhone:String?,pickupAddress:String?, dropContactName:String?,dropContactPhone:String?,dropAddress:String?) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Pickup Location
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            // Pickup Icon with connector line
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(
                            color = AppColors.Pickup.copy(alpha = 0.15f),
                            shape = CircleShape
                        )
                        .border(
                            width = 2.dp,
                            color = AppColors.Pickup,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(AppColors.Pickup, CircleShape)
                    )
                }

                // Connector Line
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(32.dp)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    AppColors.Pickup,
                                    AppColors.Drop
                                )
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Pickup Address Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "PICKUP",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.Pickup,
                    letterSpacing = 1.sp
                )


                Spacer(modifier = Modifier.height(2.dp))
                if (!pickupContactName.isNullOrEmpty() || !pickupContactPhone.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Sender",
                            tint = AppColors.Pickup,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = buildString {
                                if (!pickupContactName.isNullOrEmpty()) {
                                    append(pickupContactName)
                                }
                                if (!pickupContactPhone.isNullOrEmpty()) {
                                    if (isNotEmpty()) append(" • ")
                                    append(pickupContactPhone)
                                }
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Text(
                    text = pickupAddress.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Drop Location
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            // Drop Icon
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(
                        color = AppColors.Drop.copy(alpha = 0.15f),
                        shape = CircleShape
                    )
                    .border(
                        width = 2.dp,
                        color = AppColors.Drop,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(AppColors.Drop, CircleShape)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Drop Address Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "DROP",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.Drop,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(2.dp))

                if (!dropContactName.isNullOrEmpty() || !dropContactPhone.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Receiver",
                            tint = AppColors.Drop,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = buildString {
                                if (!dropContactName.isNullOrEmpty()) {
                                    append(dropContactName)
                                }
                                if (!dropContactPhone.isNullOrEmpty()) {
                                    if (isNotEmpty()) append(" • ")
                                    append(dropContactPhone)
                                }
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Text(
                    text = dropAddress.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
