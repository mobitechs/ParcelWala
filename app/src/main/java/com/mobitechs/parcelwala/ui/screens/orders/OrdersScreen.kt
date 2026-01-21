// ui/screens/orders/OrdersScreen.kt
package com.mobitechs.parcelwala.ui.screens.orders

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mobitechs.parcelwala.data.model.response.OrderResponse
import com.mobitechs.parcelwala.ui.theme.AppColors
import com.mobitechs.parcelwala.ui.viewmodel.OrdersViewModel
import com.mobitechs.parcelwala.utils.DateTimeUtils

/**
 * Orders Screen
 * Displays list of past and scheduled orders
 * Passes OrderResponse object directly on card click (no API call needed)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen(
    onOrderClick: (OrderResponse) -> Unit = {},  // Pass entire object
    onBookAgain: (OrderResponse) -> Unit = {},
    viewModel: OrdersViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val tabs = listOf("Past", "Scheduled")
    var isRefreshing by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isLoading) {
        if (!uiState.isLoading) isRefreshing = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Orders",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    )
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
            // Tab Row
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = AppColors.Primary,
                indicator = { tabPositions ->
                    if (selectedTab < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            height = 3.dp,
                            color = AppColors.Primary
                        )
                    }
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { viewModel.onTabSelected(index) },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == index) AppColors.Primary else AppColors.TextSecondary
                            )
                        }
                    )
                }
            }

            // Content
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
                    uiState.orders.isEmpty() -> EmptyOrdersState()
                    else -> OrdersList(
                        orders = uiState.orders,
                        onOrderClick = onOrderClick,  // Pass entire object
                        onBookAgain = onBookAgain
                    )
                }
            }
        }
    }
}

/**
 * Orders List
 */
@Composable
private fun OrdersList(
    orders: List<OrderResponse>,
    onOrderClick: (OrderResponse) -> Unit,
    onBookAgain: (OrderResponse) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(items = orders, key = { it.bookingId }) { order ->
            OrderCard(
                order = order,
                onClick = { onOrderClick(order) },  // Pass entire order object
                onBookAgain = { onBookAgain(order) }
            )
        }
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

/**
 * Get vehicle icon based on vehicle type
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
        vehicleType.contains("Mini Truck", ignoreCase = true) -> "🚛"
        else -> "📦"
    }
}

/**
 * Order Card
 */
@Composable
private fun OrderCard(
    order: OrderResponse,
    onClick: () -> Unit,
    onBookAgain: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(AppColors.Background),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = getVehicleIcon(order.vehicleType),
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }
                    Column {
                        Text(
                            text = order.vehicleType,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.TextPrimary
                        )
                        Text(
                            text =  DateTimeUtils.formatDateTime(order.createdAt),
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.TextSecondary
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "₹${order.fare}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    )
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Details",
                        tint = AppColors.TextSecondary
                    )
                }
            }

            // Address Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = AppColors.Background)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    AddressRow(
                        name = order.pickupContactName ?: "Unknown",
                        phone = order.pickupContactPhone ?: "",
                        address = order.pickupAddress,
                        isPickup = true
                    )
                    DottedDivider(modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 8.dp))
                    AddressRow(
                        name = order.dropContactName ?: "Unknown",
                        phone = order.dropContactPhone ?: "",
                        address = order.dropAddress,
                        isPickup = false
                    )
                }
            }

            // Footer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusBadge(status = order.status)
                Button(
                    onClick = onBookAgain,
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary),
                    shape = RoundedCornerShape(24.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp)
                ) {
                    Text(text = "Book Again", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Status Badge
 */
@Composable
private fun StatusBadge(status: String) {
    val (icon, color) = when (status) {
        "Completed" -> Pair(Icons.Default.CheckCircle, AppColors.Pickup)
        "Cancelled" -> Pair(Icons.Default.Cancel, AppColors.Drop)
        "In Progress" -> Pair(Icons.Default.LocalShipping, AppColors.Primary)
        "Searching" -> Pair(Icons.Default.Search, Color(0xFFFF9800))
        "Assigned" -> Pair(Icons.Default.Person, AppColors.Primary)
        "Arriving" -> Pair(Icons.Default.DirectionsCar, AppColors.Primary)
        "Picked Up" -> Pair(Icons.Default.Inventory, AppColors.Pickup)
        "Pending" -> Pair(Icons.Default.Schedule, AppColors.TextSecondary)
        else -> Pair(Icons.Default.Schedule, AppColors.TextSecondary)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = status,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = status,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = color
        )
    }
}

/**
 * Address Row
 */
@Composable
private fun AddressRow(name: String, phone: String, address: String, isPickup: Boolean) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .size(10.dp)
                .background(
                    color = if (isPickup) AppColors.Pickup else AppColors.Drop,
                    shape = CircleShape
                )
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary
                )
                if (phone.isNotEmpty()) {
                    Text(
                        text = " • $phone",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextSecondary
                    )
                }
            }
            Text(
                text = address,
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Dotted Divider
 */
@Composable
private fun DottedDivider(modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(3) { Box(modifier = Modifier.size(3.dp).background(AppColors.TextHint, CircleShape)) }
    }
}

/**
 * Loading State
 */
@Composable
private fun LoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CircularProgressIndicator(color = AppColors.Primary, modifier = Modifier.size(48.dp))
            Text(text = "Loading orders...", style = MaterialTheme.typography.bodyMedium, color = AppColors.TextSecondary)
        }
    }
}

/**
 * Error State
 */
@Composable
private fun ErrorState(error: String, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(32.dp)) {
            Icon(imageVector = Icons.Default.ErrorOutline, contentDescription = "Error", tint = AppColors.Drop, modifier = Modifier.size(64.dp))
            Text(text = "Something went wrong", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
            Text(text = error, style = MaterialTheme.typography.bodyMedium, color = AppColors.TextSecondary)
            Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary)) { Text("Retry") }
        }
    }
}

/**
 * Empty Orders State
 */
@Composable
private fun EmptyOrdersState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(32.dp)) {
            Icon(imageVector = Icons.Default.Receipt, contentDescription = "No Orders", tint = AppColors.TextHint, modifier = Modifier.size(80.dp))
            Text(text = "No Orders Yet", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
            Text(text = "Your past orders will appear here", style = MaterialTheme.typography.bodyMedium, color = AppColors.TextSecondary)
        }
    }
}