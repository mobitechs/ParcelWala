package com.mobitechs.parcelwala.ui.screens.booking


import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mobitechs.parcelwala.data.model.request.SavedAddress
import com.mobitechs.parcelwala.ui.components.IconButtonWithBackground
import com.mobitechs.parcelwala.ui.components.InfoCard
import com.mobitechs.parcelwala.ui.components.JourneyConnector
import com.mobitechs.parcelwala.ui.components.SecondaryButton
import com.mobitechs.parcelwala.ui.components.VehicleType
import com.mobitechs.parcelwala.ui.theme.AppColors
import com.mobitechs.parcelwala.ui.viewmodel.BookingViewModel

/**
 * Searching for Rider Screen
 * Shows animated search state while finding nearby riders
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchingRiderScreen(
    bookingId: String,
    pickupAddress: SavedAddress,
    dropAddress: SavedAddress,
    selectedVehicle: VehicleType,
    fare: Int,
    onRiderFound: () -> Unit,
    onContactSupport: () -> Unit,
    onViewDetails: () -> Unit,
    viewModel: BookingViewModel = hiltViewModel()
) {
    var showCancelSheet by remember { mutableStateOf(false) }
    var showGSTBanner by remember { mutableStateOf(true) }

    // Animation for pulse effect
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Trip $bookingId",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Finding your rider...",
                            style = MaterialTheme.typography.labelSmall,
                            color = AppColors.TextSecondary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* Share */ }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = AppColors.Primary
                        )
                    }
                    IconButton(onClick = { /* Info */ }) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Info",
                            tint = AppColors.Primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        containerColor = AppColors.Background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // GST Benefits Banner (Collapsible)
                if (showGSTBanner) {
                    GSTBenefitsBanner(
                        onClose = { showGSTBanner = false },
                        onAddGSTIN = { /* Navigate to GST */ }
                    )
                }

                // Searching Animation Card
                SearchingAnimationCard(
                    scale = scale,
                    alpha = alpha,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Journey Details Card
                JourneyDetailsCard(
                    pickupAddress = pickupAddress,
                    dropAddress = dropAddress,
                    onViewDetails = onViewDetails,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Payment Summary
                PaymentSummaryCard(
                    paymentMethod = "Cash",
                    amount = fare,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.weight(1f))

                // Support Section
                SupportSection(
                    onContactSupport = onContactSupport,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )

                // Cancel Trip Button
                OutlinedButton(
                    onClick = { showCancelSheet = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = AppColors.Drop
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.Drop)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Cancel Trip",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }
    }

    // Cancel Reason Bottom Sheet
    if (showCancelSheet) {
        CancellationReasonBottomSheet(
            onDismiss = { showCancelSheet = false },
            onConfirmCancel = { reason ->
                viewModel.cancelBooking(reason)
                showCancelSheet = false
            }
        )
    }
}

/**
 * Cancellation Reason Bottom Sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CancellationReasonBottomSheet(
    onDismiss: () -> Unit,
    onConfirmCancel: (String) -> Unit
) {
    var selectedReason by remember { mutableStateOf<String?>(null) }
    var otherReason by remember { mutableStateOf("") }
    var showConfirmDialog by remember { mutableStateOf(false) }

    val cancellationReasons = listOf(
        CancellationReason(
            id = "driver_delayed",
            title = "Driver is taking too long",
            icon = Icons.Default.Timer
        ),
        CancellationReason(
            id = "change_plans",
            title = "Change of plans",
            icon = Icons.Default.EventBusy
        ),
        CancellationReason(
            id = "wrong_address",
            title = "Wrong pickup/drop location",
            icon = Icons.Default.LocationOff
        ),
        CancellationReason(
            id = "price_high",
            title = "Price is too high",
            icon = Icons.Default.MoneyOff
        ),
        CancellationReason(
            id = "booking_mistake",
            title = "Booked by mistake",
            icon = Icons.Default.ErrorOutline
        ),
        CancellationReason(
            id = "other",
            title = "Other reason",
            icon = Icons.Default.MoreHoriz
        )
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .background(
                        color = AppColors.Border,
                        shape = RoundedCornerShape(2.dp)
                    )
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Cancel Trip?",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    )
                    Text(
                        text = "Please select a reason",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.TextSecondary
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = AppColors.TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Cancellation Reasons List
            cancellationReasons.forEach { reason ->
                CancellationReasonItem(
                    reason = reason,
                    isSelected = selectedReason == reason.id,
                    onClick = { selectedReason = reason.id }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Other Reason Text Field
            if (selectedReason == "other") {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = otherReason,
                    onValueChange = { otherReason = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Please specify") },
                    placeholder = { Text("Enter your reason...") },
                    minLines = 3,
                    maxLines = 5,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppColors.Primary,
                        focusedLabelColor = AppColors.Primary
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Warning Message
            InfoCard(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFFF9800),
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "Cancellation Policy",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Frequent cancellations may result in temporary suspension of your account.",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SecondaryButton(
                    text = "Keep Trip",
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                )

                Button(
                    onClick = {
                        if (selectedReason != null) {
                            showConfirmDialog = true
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.Drop
                    ),
                    shape = RoundedCornerShape(16.dp),
                    enabled = selectedReason != null && (selectedReason != "other" || otherReason.isNotBlank())
                ) {
                    Text(
                        text = "Cancel Trip",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }
    }

    // Final Confirmation Dialog
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = AppColors.Drop,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Confirm Cancellation",
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to cancel this trip? This action cannot be undone.",
                    color = AppColors.TextSecondary,
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val reason = if (selectedReason == "other") {
                            otherReason.ifBlank { "Other" }
                        } else {
                            cancellationReasons.find { it.id == selectedReason }?.title ?: "Cancelled by user"
                        }
                        onConfirmCancel(reason)
                        showConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.Drop
                    )
                ) {
                    Text("Yes, Cancel")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("No, Keep It", color = AppColors.Primary)
                }
            },
            containerColor = Color.White
        )
    }
}

/**
 * Cancellation Reason Item
 */
@Composable
private fun CancellationReasonItem(
    reason: CancellationReason,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                AppColors.Primary.copy(alpha = 0.1f)
            else
                Color.White
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) AppColors.Primary else AppColors.Border
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = reason.icon,
                contentDescription = reason.title,
                tint = if (isSelected) AppColors.Primary else AppColors.TextSecondary,
                modifier = Modifier.size(24.dp)
            )

            Text(
                text = reason.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) AppColors.Primary else AppColors.TextPrimary,
                modifier = Modifier.weight(1f)
            )

            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = AppColors.Primary,
                    unselectedColor = AppColors.Border
                )
            )
        }
    }
}

/**
 * Cancellation Reason Data Class
 */
data class CancellationReason(
    val id: String,
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

// Rest of the composables remain the same (GST Banner, Searching Animation, etc.)
// Keep all the existing composables from the previous SearchingRiderScreen.kt

/**
 * GST Benefits Banner
 */
@Composable
private fun GSTBenefitsBanner(
    onClose: () -> Unit,
    onAddGSTIN: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(
            containerColor = AppColors.Primary
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "DON'T MISS",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Text(
                    text = "GST Benefits!",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onAddGSTIN,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = AppColors.Primary
                    ),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text(
                        text = "Add GSTIN →",
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            IconButton(
                onClick = onClose,
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White
                )
            }
        }
    }
}

/**
 * Searching Animation Card
 */
@Composable
private fun SearchingAnimationCard(
    scale: Float,
    alpha: Float,
    modifier: Modifier = Modifier
) {
    InfoCard(
        modifier = modifier,
        elevation = 4.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Animated Pulse Circle
            Box(
                modifier = Modifier.size(60.dp),
                contentAlignment = Alignment.Center
            ) {
                // Outer pulse
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .scale(scale)
                        .alpha(alpha * 0.3f)
                        .background(
                            color = AppColors.Primary,
                            shape = CircleShape
                        )
                )
                // Inner circle
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = AppColors.Primary,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Searching for drivers nearby ...",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Finding partner near you.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextSecondary
                )
            }

            // Expand Map Icon
            IconButtonWithBackground(
                icon = Icons.Default.Fullscreen,
                contentDescription = "Expand Map",
                onClick = { /* Expand map */ },
                size = 40.dp,
                backgroundColor = AppColors.Primary.copy(alpha = 0.1f),
                iconTint = AppColors.Primary
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Mini Map Preview
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(
                    color = AppColors.Background,
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Map,
                    contentDescription = "Map",
                    tint = AppColors.TextHint,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = "Map View",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextHint
                )
            }
        }
    }
}

/**
 * Journey Details Card
 */
@Composable
private fun JourneyDetailsCard(
    pickupAddress: SavedAddress,
    dropAddress: SavedAddress,
    onViewDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    InfoCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(AppColors.Pickup, CircleShape)
                    )
                    Text(
                        text = pickupAddress.contactName ?: "Pickup",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    )
                }
                Text(
                    text = pickupAddress.address.take(40) + if (pickupAddress.address.length > 40) "..." else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextSecondary,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        JourneyConnector(modifier = Modifier.padding(start = 4.dp))

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(AppColors.Drop, CircleShape)
                    )
                    Text(
                        text = dropAddress.contactName ?: "Drop",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    )
                }
                Text(
                    text = dropAddress.address.take(40) + if (dropAddress.address.length > 40) "..." else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextSecondary,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(
            onClick = onViewDetails,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.List,
                contentDescription = null,
                tint = AppColors.Primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "View Details",
                color = AppColors.Primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Payment Summary Card
 */
@Composable
private fun PaymentSummaryCard(
    paymentMethod: String,
    amount: Int,
    modifier: Modifier = Modifier
) {
    InfoCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Wallet,
                    contentDescription = "Payment",
                    tint = AppColors.Pickup,
                    modifier = Modifier.size(32.dp)
                )
                Column {
                    Text(
                        text = paymentMethod,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    )
                    Text(
                        text = "Payment Method",
                        style = MaterialTheme.typography.labelSmall,
                        color = AppColors.TextSecondary
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "₹$amount",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.Primary
                )
                TextButton(
                    onClick = { /* View breakup */ },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = "View Breakup",
                        style = MaterialTheme.typography.labelSmall,
                        color = AppColors.Primary
                    )
                }
            }
        }
    }
}

/**
 * Support Section
 */
@Composable
private fun SupportSection(
    onContactSupport: () -> Unit,
    modifier: Modifier = Modifier
) {
    InfoCard(
        modifier = modifier,
        elevation = 2.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Facing issue in this order?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = AppColors.TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                TextButton(
                    onClick = onContactSupport,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Headset,
                        contentDescription = null,
                        tint = AppColors.Primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Contact Support",
                        color = AppColors.Primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = AppColors.TextHint
            )
        }
    }
}