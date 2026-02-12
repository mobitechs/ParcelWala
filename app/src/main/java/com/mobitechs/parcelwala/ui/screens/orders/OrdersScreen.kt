// ui/screens/orders/OrdersScreen.kt
package com.mobitechs.parcelwala.ui.screens.orders

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mobitechs.parcelwala.data.model.response.OrderResponse
import com.mobitechs.parcelwala.ui.theme.AppColors
import com.mobitechs.parcelwala.ui.viewmodel.OrdersViewModel
import com.mobitechs.parcelwala.utils.DateTimeUtils
import kotlinx.coroutines.launch

// ═══════════════════════════════════════════════════════════════
// STATUS CONFIGURATION — matches ACTUAL backend values
// Backend sends: searching, delivery_completed, cancelled,
//                assigned, arriving, picked_up, in_progress, pending
// ═══════════════════════════════════════════════════════════════

private data class StatusConfig(
    val icon: ImageVector,
    val color: Color,
    val displayLabel: String
)

private fun getStatusConfig(status: String): StatusConfig {
    return when (status.lowercase()) {
        "delivery_completed", "completed" -> StatusConfig(
            Icons.Default.CheckCircle, Color(0xFF2E7D32), "Delivered"
        )
        "cancelled" -> StatusConfig(
            Icons.Default.Cancel, Color(0xFFD32F2F), "Cancelled"
        )
        "searching" -> StatusConfig(
            Icons.Default.Search, Color(0xFFFF9800), "Searching"
        )
        "assigned" -> StatusConfig(
            Icons.Default.PersonPin, Color(0xFF1976D2), "Assigned"
        )
        "arriving", "driver_arriving" -> StatusConfig(
            Icons.Default.DirectionsCar, Color(0xFF1976D2), "Arriving"
        )
        "picked_up" -> StatusConfig(
            Icons.Default.Inventory, Color(0xFF00897B), "Picked Up"
        )
        "in_progress", "in progress" -> StatusConfig(
            Icons.Default.LocalShipping, Color(0xFFFF6B35), "In Progress"
        )
        "pending" -> StatusConfig(
            Icons.Default.Schedule, Color(0xFF757575), "Pending"
        )
        else -> StatusConfig(
            Icons.Default.Schedule, Color(0xFF757575), status.replaceFirstChar { it.uppercase() }
        )
    }
}

private fun isCompletedStatus(status: String): Boolean =
    status.lowercase() in listOf("delivery_completed", "completed")

private fun isCancelledStatus(status: String): Boolean =
    status.lowercase() == "cancelled"

private fun isSearchingStatus(status: String): Boolean =
    status.lowercase() == "searching"

private fun isActiveStatus(status: String): Boolean =
    status.lowercase() in listOf(
        "in_progress", "in progress", "assigned",
        "arriving", "driver_arriving", "picked_up"
    )

/** Backend sends rating=0 for unrated (not null) */
private fun needsRating(order: OrderResponse): Boolean =
    isCompletedStatus(order.status) && (order.rating == null || order.rating == 0)

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
        vehicleType.contains("Mini Truck", ignoreCase = true) -> "🚛"
        else -> "📦"
    }
}

// ═══════════════════════════════════════════════════════════════
// FILTER CHIP DATA
// ═══════════════════════════════════════════════════════════════

private data class FilterChipData(
    val label: String,
    val filterKey: String?,
    val icon: ImageVector,
    val color: Color
)

private val filterChips = listOf(
    FilterChipData("All", null, Icons.Outlined.Receipt, Color(0xFF757575)),
    FilterChipData("Searching", "searching", Icons.Default.Search, Color(0xFFFF9800)),
    FilterChipData("In Progress", "active", Icons.Default.LocalShipping, Color(0xFFFF6B35)),
    FilterChipData("Completed", "completed", Icons.Default.CheckCircle, Color(0xFF2E7D32)),
    FilterChipData("Cancelled", "cancelled", Icons.Default.Cancel, Color(0xFFD32F2F))
)

// ═══════════════════════════════════════════════════════════════
// MAIN SCREEN
// ═══════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen(
    onOrderClick: (OrderResponse) -> Unit = {},
    onBookAgain: (OrderResponse) -> Unit = {},
    viewModel: OrdersViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val hasActiveBooking by viewModel.hasActiveBooking.collectAsState()
    var isRefreshing by remember { mutableStateOf(false) }

    // Rating dialog state
    var orderToRate by remember { mutableStateOf<OrderResponse?>(null) }

    // Snackbar for blocking messages
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(uiState.isLoading) {
        if (!uiState.isLoading) isRefreshing = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "My Orders",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = Color(0xFF323232),
                    contentColor = Color.White,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        containerColor = AppColors.Background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // ═══ FILTER CHIPS ROW ═══
            StatusFilterRow(
                selectedFilter = selectedFilter,
                onFilterSelected = { viewModel.onFilterSelected(it) }
            )

            // ═══ ACTIVE BOOKING BANNER ═══
            if (hasActiveBooking) {
                ActiveBookingBanner()
            }

            // ═══ CONTENT ═══
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    isRefreshing = true
                    viewModel.refreshOrders()
                },
                modifier = Modifier.fillMaxSize()
            ) {
                when {
                    uiState.isLoading && uiState.orders.isEmpty() -> LoadingState()
                    uiState.error != null && uiState.orders.isEmpty() -> ErrorState(
                        error = uiState.error ?: "Unknown error",
                        onRetry = { viewModel.refreshOrders() }
                    )
                    uiState.orders.isEmpty() -> EmptyOrdersState(filter = selectedFilter)
                    else -> OrdersList(
                        orders = uiState.orders,
                        onOrderClick = onOrderClick,
                        onBookAgain = { order ->
                            if (hasActiveBooking) {
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = "You have an active booking. Complete or cancel it first.",
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            } else {
                                onBookAgain(order)
                            }
                        },
                        onRateOrder = { orderToRate = it }
                    )
                }
            }
        }
    }

    // ═══ RATING DIALOG ═══
    orderToRate?.let { order ->
        RatingDialog(
            order = order,
            onDismiss = { orderToRate = null },
            onSubmitRating = { rating, review ->
                viewModel.submitRating(order.bookingId, rating, review)
                orderToRate = null
            }
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// ACTIVE BOOKING BANNER
// ═══════════════════════════════════════════════════════════════

@Composable
private fun ActiveBookingBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFFF3E0))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            tint = Color(0xFFE65100),
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = "You have an active booking. Book Again is disabled until it's completed or cancelled.",
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFFE65100),
            lineHeight = 16.sp
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// FILTER CHIPS ROW
// ═══════════════════════════════════════════════════════════════

@Composable
private fun StatusFilterRow(
    selectedFilter: String?,
    onFilterSelected: (String?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(vertical = 12.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Spacer(modifier = Modifier.width(12.dp))

        filterChips.forEach { chip ->
            val isSelected = selectedFilter == chip.filterKey

            FilterChip(
                selected = isSelected,
                onClick = { onFilterSelected(chip.filterKey) },
                label = {
                    Text(
                        text = chip.label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 12.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = chip.icon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                shape = RoundedCornerShape(20.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = chip.color.copy(alpha = 0.12f),
                    selectedLabelColor = chip.color,
                    selectedLeadingIconColor = chip.color,
                    containerColor = Color.Transparent,
                    labelColor = AppColors.TextSecondary,
                    iconColor = AppColors.TextHint
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = AppColors.Border,
                    selectedBorderColor = chip.color.copy(alpha = 0.4f)
                )
            )
        }

        Spacer(modifier = Modifier.width(12.dp))
    }

    HorizontalDivider(color = AppColors.Border, thickness = 0.5.dp)
}

// ═══════════════════════════════════════════════════════════════
// ORDERS LIST
// ═══════════════════════════════════════════════════════════════

@Composable
private fun OrdersList(
    orders: List<OrderResponse>,
    onOrderClick: (OrderResponse) -> Unit,
    onBookAgain: (OrderResponse) -> Unit,
    onRateOrder: (OrderResponse) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        itemsIndexed(
            items = orders,
            key = { _, order -> order.bookingId }
        ) { index, order ->
            var visible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { visible = true }

            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(300, delayMillis = index * 50)) +
                        slideInVertically(
                            tween(300, delayMillis = index * 50),
                            initialOffsetY = { it / 4 }
                        )
            ) {
                OrderCard(
                    order = order,
                    onClick = { onOrderClick(order) },
                    onBookAgain = { onBookAgain(order) },
                    onRateOrder = { onRateOrder(order) }
                )
            }
        }
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

// ═══════════════════════════════════════════════════════════════
// ORDER CARD
// ═══════════════════════════════════════════════════════════════

@Composable
private fun OrderCard(
    order: OrderResponse,
    onClick: () -> Unit,
    onBookAgain: () -> Unit,
    onRateOrder: () -> Unit
) {
    val statusConfig = getStatusConfig(order.status)
    val completed = isCompletedStatus(order.status)
    val cancelled = isCancelledStatus(order.status)
    val searching = isSearchingStatus(order.status)
    val active = isActiveStatus(order.status)
    val showRating = needsRating(order)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
//            .drawBehind {
//                drawRoundRect(
//                    color = statusConfig.color,
//                    topLeft = Offset(0f, 0f),
//                    size = Size(8.dp.toPx(), size.height),
//                    cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx())
//                )
//            }
        ,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {

            // ═══ STATUS BANNER ═══
            StatusBanner(
                statusConfig = statusConfig,
                order = order,
                searching = searching,
                active = active,
                completed = completed,
                cancelled = cancelled
            )

            // ═══ HEADER: Vehicle + Fare ═══
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(AppColors.Primary.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = getVehicleIcon(order.vehicleType),
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = order.vehicleType,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    )
                    Text(
                        text = DateTimeUtils.formatDateTime(order.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextSecondary,
                        fontSize = 11.sp
                    )
                }

                Text(
                    text = "₹${order.fare.toInt()}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary
                )
            }

            // ═══ ROUTE: Pickup → Drop ═══
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                RoutePoint(
                    color = AppColors.Pickup,
                    label = order.pickupContactName ?: "Pickup",
                    address = order.pickupAddress
                )

                Column(
                    modifier = Modifier.padding(start = 7.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    repeat(3) {
                        Box(
                            modifier = Modifier
                                .size(3.dp)
                                .background(AppColors.Border, CircleShape)
                        )
                    }
                }

                RoutePoint(
                    color = AppColors.Drop,
                    label = order.dropContactName ?: "Drop",
                    address = order.dropAddress
                )
            }

            // ═══ FOOTER ═══
            OrderCardFooter(
                order = order,
                completed = completed,
                cancelled = cancelled,
                showRating = showRating,
                onBookAgain = onBookAgain,
                onRateOrder = onRateOrder
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// STATUS BANNER
// ═══════════════════════════════════════════════════════════════

@Composable
private fun StatusBanner(
    statusConfig: StatusConfig,
    order: OrderResponse,
    searching: Boolean,
    active: Boolean,
    completed: Boolean,
    cancelled: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(statusConfig.color.copy(alpha = 0.06f))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = statusConfig.icon,
            contentDescription = null,
            tint = statusConfig.color,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = statusConfig.displayLabel,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = statusConfig.color,
            modifier = Modifier.weight(1f)
        )

        when {
            searching -> {
                Text(
                    text = "Looking for rider...",
                    style = MaterialTheme.typography.labelSmall,
                    color = statusConfig.color.copy(alpha = 0.7f),
                    fontSize = 10.sp
                )
            }
            active -> {
                order.driverName?.let { name ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = statusConfig.color,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = name,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = statusConfig.color,
                            fontSize = 10.sp
                        )
                    }
                }
            }
            completed -> {
                val rating = order.rating ?: 0
                if (rating > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        repeat(5) { index ->
                            Icon(
                                imageVector = if (index < rating) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = null,
                                tint = Color(0xFFFFC107),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                } else {
                    Text(
                        text = "Unrated",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFFFC107),
                        fontWeight = FontWeight.Medium,
                        fontSize = 10.sp
                    )
                }
            }
            cancelled -> {
                order.cancellationReason?.let { reason ->
                    Text(
                        text = reason,
                        style = MaterialTheme.typography.labelSmall,
                        color = statusConfig.color.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 10.sp,
                        modifier = Modifier.widthIn(max = 140.dp)
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// ORDER CARD FOOTER
// ═══════════════════════════════════════════════════════════════

@Composable
private fun OrderCardFooter(
    order: OrderResponse,
    completed: Boolean,
    cancelled: Boolean,
    showRating: Boolean,
    onBookAgain: () -> Unit,
    onRateOrder: () -> Unit
) {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = AppColors.Border,
        thickness = 0.5.dp
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Info chips
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (order.distance != null && order.distance > 0) {
                InfoChip(
                    icon = Icons.Outlined.Route,
                    text = String.format("%.1f km", order.distance)
                )
            }
            order.paymentMethod?.let { method ->
                if (method.isNotBlank()) {
                    InfoChip(
                        icon = Icons.Outlined.Payment,
                        text = method
                    )
                }
            }
            if ((order.distance == null || order.distance <= 0) && order.paymentMethod.isNullOrBlank()) {
                InfoChip(
                    icon = Icons.Outlined.Tag,
                    text = "#${order.bookingNumber}"
                )
            }
        }

        // Right: Action buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ⭐ RATE button (completed + unrated)
            if (showRating) {
                TextButton(
                    onClick = onRateOrder,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color(0xFFFFC107)
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.StarRate,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "Rate",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            // 🔄 BOOK AGAIN button (completed or cancelled)
            if (completed || cancelled) {
                TextButton(
                    onClick = onBookAgain,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = AppColors.Primary
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Replay,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "Book Again",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// RATING DIALOG
// ═══════════════════════════════════════════════════════════════

@Composable
private fun RatingDialog(
    order: OrderResponse,
    onDismiss: () -> Unit,
    onSubmitRating: (Int, String) -> Unit
) {
    var selectedRating by remember { mutableIntStateOf(0) }
    var reviewText by remember { mutableStateOf("") }

    val ratingLabels = listOf("", "Poor", "Below Average", "Good", "Very Good", "Excellent")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp),
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(AppColors.Primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = getVehicleIcon(order.vehicleType),
                        style = MaterialTheme.typography.headlineMedium
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Rate your delivery",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary
                )

                order.driverName?.let { name ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Driver: $name",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextSecondary
                    )
                }
            }
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    repeat(5) { index ->
                        val starIndex = index + 1
                        IconButton(
                            onClick = { selectedRating = starIndex },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = if (starIndex <= selectedRating)
                                    Icons.Default.Star
                                else
                                    Icons.Default.StarBorder,
                                contentDescription = "Star $starIndex",
                                tint = if (starIndex <= selectedRating)
                                    Color(0xFFFFC107)
                                else
                                    AppColors.Border,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }

                if (selectedRating > 0) {
                    Text(
                        text = ratingLabels[selectedRating],
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFFFC107)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = reviewText,
                    onValueChange = { reviewText = it },
                    label = { Text("Write a review (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppColors.Primary,
                        focusedLabelColor = AppColors.Primary,
                        cursorColor = AppColors.Primary
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmitRating(selectedRating, reviewText) },
                enabled = selectedRating > 0,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.Primary,
                    disabledContainerColor = AppColors.Border
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Submit Rating",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Skip", color = AppColors.TextSecondary)
            }
        }
    )
}

// ═══════════════════════════════════════════════════════════════
// SHARED COMPOSABLES
// ═══════════════════════════════════════════════════════════════

@Composable
private fun RoutePoint(
    color: Color,
    label: String,
    address: String
) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .size(16.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.TextPrimary,
                fontSize = 12.sp
            )
            Text(
                text = address,
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun InfoChip(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = AppColors.TextHint,
            modifier = Modifier.size(13.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = AppColors.TextSecondary,
            fontSize = 10.sp
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// EMPTY / LOADING / ERROR STATES
// ═══════════════════════════════════════════════════════════════

@Composable
private fun LoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                color = AppColors.Primary,
                modifier = Modifier.size(40.dp),
                strokeWidth = 3.dp
            )
            Text(
                text = "Loading orders...",
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextSecondary
            )
        }
    }
}

@Composable
private fun ErrorState(error: String, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(AppColors.Drop.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = "Error",
                    tint = AppColors.Drop,
                    modifier = Modifier.size(36.dp)
                )
            }
            Text(
                text = "Something went wrong",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary
            )
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextSecondary,
                textAlign = TextAlign.Center
            )
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 32.dp, vertical = 12.dp)
            ) {
                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Retry", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun EmptyOrdersState(filter: String?) {
    val filterLabel = filterChips.find { it.filterKey == filter }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(
                        (filterLabel?.color ?: AppColors.Primary).copy(alpha = 0.08f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = filterLabel?.icon ?: Icons.Outlined.Receipt,
                    contentDescription = null,
                    tint = (filterLabel?.color ?: AppColors.Primary).copy(alpha = 0.6f),
                    modifier = Modifier.size(44.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = when (filter) {
                    "completed" -> "No Completed Orders"
                    "cancelled" -> "No Cancelled Orders"
                    "searching" -> "No Searching Orders"
                    "active" -> "No Active Orders"
                    else -> "No Orders Yet"
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary
            )
            Text(
                text = when (filter) {
                    "completed" -> "Your completed deliveries will appear here"
                    "cancelled" -> "No cancelled orders to show"
                    "searching" -> "No orders currently searching for a rider"
                    "active" -> "No deliveries in progress right now"
                    else -> "Your orders will appear here once you book a delivery"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}