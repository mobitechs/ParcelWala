// ui/screens/orders/OrderDetailsScreen.kt
package com.mobitechs.parcelwala.ui.screens.orders

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mobitechs.parcelwala.data.model.response.OrderResponse
import com.mobitechs.parcelwala.ui.components.AddressesCard
import com.mobitechs.parcelwala.ui.theme.AppColors
import com.mobitechs.parcelwala.utils.DateTimeUtils

/**
 * Order Details Screen
 * Receives OrderResponse directly - no API call needed
 * Displays all order information as received
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailsScreen(
    order: OrderResponse,  // Receive order directly
    onBack: () -> Unit,
    onBookAgain: (OrderResponse) -> Unit = {},
    onCallDriver: (String) -> Unit = {},
    onCallSupport: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Order Details",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "#${order.bookingNumber}",
                            style = MaterialTheme.typography.labelMedium,
                            color = AppColors.TextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = AppColors.TextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onCallSupport) {
                        Icon(
                            imageVector = Icons.Default.Support,
                            contentDescription = "Support",
                            tint = AppColors.Primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = AppColors.Background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Scrollable Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                // Order Status Header
                OrderStatusHeader(order = order)

                Spacer(modifier = Modifier.height(16.dp))

                // Vehicle & Trip Info
                VehicleTripCard(order = order)

                Spacer(modifier = Modifier.height(16.dp))

                // OTP Section (if available)
                if (!order.otp.isNullOrEmpty()) {
                    OtpCard(otp = order.otp!!)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Driver Info (if assigned)
                if (order.driverName != null) {
                    DriverInfoCard(
                        order = order,
                        onCallDriver = { order.driverPhone?.let { onCallDriver(it) } }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Addresses Section
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    AddressesCard(
                        order.pickupContactName,
                        order.pickupContactPhone,
                        order.pickupAddress,
                        order.dropContactName,
                        order.dropContactPhone,
                        order.dropAddress
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Goods Information (if available)
                if (order.goodsType != null || order.goodsWeight != null) {
                    GoodsInfoCard(order = order)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Payment & Fare Details
                PaymentDetailsCard(order = order)

                Spacer(modifier = Modifier.height(16.dp))

                // Coupon Info (if applied)
                if (!order.couponCode.isNullOrEmpty()) {
                    CouponCard(order = order)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // GST Info (if available)
                if (!order.gstin.isNullOrEmpty()) {
                    GstInfoCard(gstin = order.gstin!!)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Cancellation Info (if cancelled)
                if (order.status == "Cancelled" && !order.cancellationReason.isNullOrEmpty()) {
                    CancellationInfoCard(order = order)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Rating (if completed and rated)
                if (order.status == "Completed" && order.rating != null) {
                    RatingCard(order = order)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Spacer(modifier = Modifier.height(100.dp))
            }

            // Bottom Action - Book Again
            Surface(
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                Button(
                    onClick = { onBookAgain(order) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.Primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Book Again", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Get vehicle icon
 */
private fun getVehicleIcon(vehicleType: String): String {
    return when {
        vehicleType.contains("2 Wheeler", ignoreCase = true) -> "🏍️"
        vehicleType.contains("Bike", ignoreCase = true) -> "🏍️"
        vehicleType.contains("3 Wheeler", ignoreCase = true) -> "🛺"
        vehicleType.contains("Auto", ignoreCase = true) -> "🛺"
        vehicleType.contains("Tata Ace", ignoreCase = true) -> "🚚"
        vehicleType.contains("Pickup", ignoreCase = true) -> "🚙"
        vehicleType.contains("Tempo", ignoreCase = true) -> "🚛"
        vehicleType.contains("Hamal", ignoreCase = true) -> "🚶"
        else -> "📦"
    }
}

/**
 * Get status color
 */
private fun getStatusColor(status: String): Color {
    return when (status) {
        "Completed" -> AppColors.Pickup
        "Cancelled" -> AppColors.Drop
        "In Progress" -> AppColors.Primary
        "Searching" -> Color(0xFFFF9800)
        "Assigned" -> AppColors.Primary
        "Arriving" -> AppColors.Primary
        "Picked Up" -> AppColors.Pickup
        "Pending" -> AppColors.TextSecondary
        else -> AppColors.TextSecondary
    }
}

/**
 * Get status icon
 */
private fun getStatusIcon(status: String): ImageVector {
    return when (status) {
        "Completed" -> Icons.Default.CheckCircle
        "Cancelled" -> Icons.Default.Cancel
        "In Progress" -> Icons.Default.LocalShipping
        "Searching" -> Icons.Default.Search
        "Assigned" -> Icons.Default.Person
        "Arriving" -> Icons.Default.DirectionsCar
        "Picked Up" -> Icons.Default.Inventory
        "Pending" -> Icons.Default.Schedule
        else -> Icons.Default.Schedule
    }
}

/**
 * Order Status Header
 */
@Composable
private fun OrderStatusHeader(order: OrderResponse) {
    val color = getStatusColor(order.status)
    val bgColor = color.copy(alpha = 0.1f)
    val icon = getStatusIcon(order.status)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = order.status,
                tint = color,
                modifier = Modifier.size(48.dp)
            )
            Column {
                Text(
                    text = order.status,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(
                    text = DateTimeUtils.formatDateTime(order.createdAt), // ✅ Updated
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextSecondary
                )
            }
        }
    }
}

/**
 * Vehicle & Trip Info Card
 */
@Composable
private fun VehicleTripCard(order: OrderResponse) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(
                            color = AppColors.Primary.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = getVehicleIcon(order.vehicleType),
                        style = MaterialTheme.typography.displaySmall
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = order.vehicleType,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    )
                    order.vehicleModel?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppColors.TextSecondary
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "₹${order.fare}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.Primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = AppColors.Divider)
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TripStatItem(
                    icon = Icons.Default.Route,
                    label = "Distance",
                    value = order.distance?.let { "${it} km" } ?: "N/A"
                )
                TripStatItem(
                    icon = Icons.Default.Timer,
                    label = "Duration",
                    value = order.estimatedTime ?: "N/A"
                )
            }
        }
    }
}

/**
 * Trip Stat Item
 */
@Composable
private fun TripStatItem(
    icon: ImageVector,
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = AppColors.Primary,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = AppColors.TextPrimary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = AppColors.TextSecondary
        )
    }
}

/**
 * OTP Card
 */
@Composable
private fun OtpCard(otp: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.Primary.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Delivery OTP",
                    style = MaterialTheme.typography.labelMedium,
                    color = AppColors.TextSecondary
                )
                Text(
                    text = "Share with driver to confirm",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextSecondary
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                otp.forEach { digit ->
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                color = AppColors.Primary,
                                shape = RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = digit.toString(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

/**
 * Driver Info Card
 */
@Composable
private fun DriverInfoCard(
    order: OrderResponse,
    onCallDriver: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Driver Details",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(AppColors.Background),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Driver",
                        tint = AppColors.TextSecondary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = order.driverName ?: "Driver",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    )
                    order.vehicleNumber?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppColors.TextSecondary
                        )
                    }
                    order.driverRating?.let { rating ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Rating",
                                tint = Color(0xFFFFC107),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "$rating",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = AppColors.TextPrimary
                            )
                        }
                    }
                }

                if (!order.driverPhone.isNullOrEmpty()) {
                    FilledIconButton(
                        onClick = onCallDriver,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = AppColors.Pickup
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Call Driver",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}


/**
 * Goods Info Card
 */
@Composable
private fun GoodsInfoCard(order: OrderResponse) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Goods Information",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                order.goodsType?.let {
                    InfoItem(label = "Type", value = it)
                }
                order.goodsWeight?.let {
                    InfoItem(label = "Weight", value = "${it} kg")
                }
                order.goodsPackages?.let {
                    InfoItem(label = "Packages", value = "$it")
                }
                order.goodsValue?.let {
                    InfoItem(label = "Value", value = "₹$it")
                }
            }

            order.instructions?.let { instructions ->
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = AppColors.Divider)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Instructions",
                    style = MaterialTheme.typography.labelMedium,
                    color = AppColors.TextSecondary
                )
                Text(
                    text = instructions,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextPrimary
                )
            }
        }
    }
}

/**
 * Info Item
 */
@Composable
private fun InfoItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = AppColors.TextPrimary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = AppColors.TextSecondary
        )
    }
}

/**
 * Payment Details Card
 */
@Composable
private fun PaymentDetailsCard(order: OrderResponse) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Payment Details",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary
                )
                order.paymentStatus?.let { status ->
                    Surface(
                        color = getStatusColor(status).copy(alpha = 0.1f),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            text = status,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = getStatusColor(status),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Payment Method",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextSecondary
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = when (order.paymentMethod) {
                            "Cash" -> Icons.Default.Money
                            "UPI" -> Icons.Default.Payment
                            "Card" -> Icons.Default.CreditCard
                            "Wallet" -> Icons.Default.Wallet
                            else -> Icons.Default.Payment
                        },
                        contentDescription = null,
                        tint = AppColors.Primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = order.paymentMethod ?: "N/A",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = AppColors.TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = AppColors.Divider)
            Spacer(modifier = Modifier.height(12.dp))

            order.baseFare?.let { baseFare ->
                FareRow(label = "Base Fare", amount = "₹$baseFare")
            } ?: FareRow(label = "Trip Fare", amount = "₹${order.fare}")

            order.discountAmount?.let { discount ->
                if (discount > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    FareRow(
                        label = "Discount",
                        amount = "-₹$discount",
                        valueColor = AppColors.Pickup
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = AppColors.Divider)
            Spacer(modifier = Modifier.height(12.dp))

            FareRow(
                label = "Total Amount",
                amount = "₹${order.fare}",
                isBold = true,
                valueColor = AppColors.Primary
            )
        }
    }
}

/**
 * Fare Row
 */
@Composable
private fun FareRow(
    label: String,
    amount: String,
    isBold: Boolean = false,
    valueColor: Color = AppColors.TextPrimary
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = if (isBold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = AppColors.TextPrimary
        )
        Text(
            text = amount,
            style = if (isBold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = valueColor
        )
    }
}

/**
 * Coupon Card
 */
@Composable
private fun CouponCard(order: OrderResponse) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.Pickup.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.LocalOffer,
                contentDescription = "Coupon",
                tint = AppColors.Pickup,
                modifier = Modifier.size(24.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = order.couponCode!!,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.Pickup
                )
                Text(
                    text = "Coupon Applied",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.TextSecondary
                )
            }
            order.discountAmount?.let {
                Text(
                    text = "-₹$it",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.Pickup
                )
            }
        }
    }
}

/**
 * GST Info Card
 */
@Composable
private fun GstInfoCard(gstin: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Receipt,
                contentDescription = "GST",
                tint = AppColors.Primary,
                modifier = Modifier.size(24.dp)
            )
            Column {
                Text(
                    text = "GSTIN",
                    style = MaterialTheme.typography.labelMedium,
                    color = AppColors.TextSecondary
                )
                Text(
                    text = gstin,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary
                )
            }
        }
    }
}

/**
 * Cancellation Info Card
 */
@Composable
private fun CancellationInfoCard(order: OrderResponse) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.Drop.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Cancel,
                    contentDescription = "Cancelled",
                    tint = AppColors.Drop,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Order Cancelled",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.Drop
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Reason: ${order.cancellationReason}",
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextSecondary
            )
            order.cancelledBy?.let {
                Text(
                    text = "Cancelled by: $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextSecondary
                )
            }
        }
    }
}

/**
 * Rating Card
 */
@Composable
private fun RatingCard(order: OrderResponse) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Your Rating",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                repeat(5) { index ->
                    Icon(
                        imageVector = if (index < (order.rating
                                ?: 0)
                        ) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = null,
                        tint = Color(0xFFFFC107),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            order.review?.let { review ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "\"$review\"",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextSecondary
                )
            }
        }
    }
}