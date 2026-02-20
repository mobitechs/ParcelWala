// ui/screens/orders/OrdersScreen.kt
package com.mobitechs.parcelwala.ui.screens.orders

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mobitechs.parcelwala.R
import com.mobitechs.parcelwala.data.model.response.OrderResponse
import com.mobitechs.parcelwala.ui.components.RatingDialog
import com.mobitechs.parcelwala.ui.theme.AppColors
import com.mobitechs.parcelwala.ui.viewmodel.OrdersViewModel
import com.mobitechs.parcelwala.ui.viewmodel.RatingSubmitState
import com.mobitechs.parcelwala.utils.DateTimeUtils
import kotlinx.coroutines.launch

private object Tokens {
    val SpaceXXS = 2.dp; val SpaceXS = 4.dp; val SpaceSM = 8.dp; val SpaceMD = 12.dp
    val SpaceLG = 16.dp; val SpaceXL = 20.dp; val SpaceXXL = 24.dp; val Space3XL = 32.dp; val Space4XL = 40.dp
    val RadiusSM = 8.dp; val RadiusMD = 12.dp; val RadiusLG = 16.dp; val RadiusXL = 20.dp; val RadiusFull = 100.dp
    val CardElevation = 0.5.dp; val CardShape = RoundedCornerShape(RadiusLG)
    val SurfaceWhite = Color(0xFFFFFFFF); val SurfacePage = Color(0xFFF6F7F9)
    val SurfaceSubtle = Color(0xFFF0F1F3); val BorderLight = Color(0xFFE8EAED); val BorderSubtle = Color(0xFFF0F1F3)
    val StatusGreen = Color(0xFF16A34A); val StatusGreenBg = Color(0xFFDCFCE7)
    val StatusRed = Color(0xFFDC2626); val StatusRedBg = Color(0xFFFEE2E2)
    val StatusOrange = Color(0xFFEA580C); val StatusOrangeBg = Color(0xFFFFF7ED)
    val StatusBlue = Color(0xFF2563EB); val StatusBlueBg = Color(0xFFDBEAFE)
    val StatusTeal = Color(0xFF0D9488); val StatusTealBg = Color(0xFFCCFBF1)
    val StatusGray = Color(0xFF6B7280); val StatusGrayBg = Color(0xFFF3F4F6)
    val Amber = Color(0xFFF59E0B); val AmberBg = Color(0xFFFFFBEB)
}

private data class StatusConfig(val icon: ImageVector, val color: Color, val bgColor: Color, val displayLabel: String)

private fun getStatusConfig(status: String): StatusConfig {
    return when (status.lowercase()) {
        "delivery_completed", "completed" -> StatusConfig(Icons.Rounded.CheckCircle, Tokens.StatusGreen, Tokens.StatusGreenBg, "Delivered")
        "cancelled" -> StatusConfig(Icons.Rounded.Cancel, Tokens.StatusRed, Tokens.StatusRedBg, "Cancelled")
        "searching" -> StatusConfig(Icons.Rounded.Search, Tokens.StatusOrange, Tokens.StatusOrangeBg, "Searching")
        "assigned" -> StatusConfig(Icons.Rounded.PersonPin, Tokens.StatusBlue, Tokens.StatusBlueBg, "Assigned")
        "arriving", "driver_arriving" -> StatusConfig(Icons.Rounded.DirectionsCar, Tokens.StatusBlue, Tokens.StatusBlueBg, "Arriving")
        "picked_up" -> StatusConfig(Icons.Rounded.Inventory, Tokens.StatusTeal, Tokens.StatusTealBg, "Picked Up")
        "in_progress", "in progress" -> StatusConfig(Icons.Rounded.LocalShipping, Tokens.StatusOrange, Tokens.StatusOrangeBg, "In Progress")
        "pending" -> StatusConfig(Icons.Rounded.Schedule, Tokens.StatusGray, Tokens.StatusGrayBg, "Pending")
        else -> StatusConfig(Icons.Rounded.Schedule, Tokens.StatusGray, Tokens.StatusGrayBg, status.replaceFirstChar { it.uppercase() })
    }
}

private fun isCompletedStatus(status: String): Boolean = status.lowercase() in listOf("delivery_completed", "completed")
private fun isCancelledStatus(status: String): Boolean = status.lowercase() == "cancelled"
private fun isSearchingStatus(status: String): Boolean = status.lowercase() == "searching"
private fun isActiveStatus(status: String): Boolean = status.lowercase() in listOf("in_progress", "in progress", "assigned", "arriving", "driver_arriving", "picked_up")
private fun needsRating(order: OrderResponse): Boolean = isCompletedStatus(order.status) && (order.rating == null || order.rating == 0)

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

private data class FilterChipData(val label: String, val filterKey: String?, val icon: ImageVector, val color: Color)

private val filterChips = listOf(
    FilterChipData("All", null, Icons.Rounded.Receipt, Tokens.StatusGray),
    FilterChipData("Searching", "searching", Icons.Rounded.Search, Tokens.StatusOrange),
    FilterChipData("In Progress", "active", Icons.Rounded.LocalShipping, Tokens.StatusOrange),
    FilterChipData("Completed", "completed", Icons.Rounded.CheckCircle, Tokens.StatusGreen),
    FilterChipData("Cancelled", "cancelled", Icons.Rounded.Cancel, Tokens.StatusRed)
)

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
    val ratingSubmitState by viewModel.ratingSubmitState.collectAsState()
    var orderToRate by remember { mutableStateOf<OrderResponse?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val completeOrCancelMessage = stringResource(R.string.complete_or_cancel_active)

    LaunchedEffect(uiState.isLoading) { if (!uiState.isLoading) isRefreshing = false }
    LaunchedEffect(ratingSubmitState) {
        when (val state = ratingSubmitState) {
            is RatingSubmitState.Success -> { snackbarHostState.showSnackbar(state.message); viewModel.clearRatingState() }
            is RatingSubmitState.Error -> { snackbarHostState.showSnackbar(state.message); viewModel.clearRatingState() }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            Surface(color = Tokens.SurfaceWhite, shadowElevation = 0.dp, tonalElevation = 0.dp) {
                Column {
                    TopAppBar(
                        title = {
                            Text(
                                text = stringResource(R.string.my_orders),
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
                                color = AppColors.TextPrimary
                            )
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, scrolledContainerColor = Color.Transparent)
                    )
                    StatusFilterRow(selectedFilter = selectedFilter, onFilterSelected = { viewModel.onFilterSelected(it) })
                    HorizontalDivider(color = Tokens.BorderSubtle, thickness = 1.dp)
                }
            }
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(snackbarData = data, containerColor = Color(0xFF1F2937), contentColor = Color.White, shape = RoundedCornerShape(Tokens.RadiusMD), modifier = Modifier.padding(Tokens.SpaceLG))
            }
        },
        containerColor = Tokens.SurfacePage
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (hasActiveBooking) { ActiveBookingBanner() }
            PullToRefreshBox(isRefreshing = isRefreshing, onRefresh = { isRefreshing = true; viewModel.refreshOrders() }, modifier = Modifier.fillMaxSize()) {
                when {
                    uiState.isLoading && uiState.orders.isEmpty() -> LoadingState()
                    uiState.error != null && uiState.orders.isEmpty() -> ErrorState(error = uiState.error ?: "", onRetry = { viewModel.refreshOrders() })
                    uiState.orders.isEmpty() -> EmptyOrdersState(filter = selectedFilter)
                    else -> OrdersList(
                        orders = uiState.orders, onOrderClick = onOrderClick,
                        onBookAgain = { order ->
                            if (hasActiveBooking) { scope.launch { snackbarHostState.showSnackbar(message = completeOrCancelMessage, duration = SnackbarDuration.Short) } }
                            else { onBookAgain(order) }
                        },
                        onRateOrder = { orderToRate = it }
                    )
                }
            }
        }
    }

    orderToRate?.let { order ->
        val isSubmitting = ratingSubmitState is RatingSubmitState.Submitting
        RatingDialog(
            bookingNumber = order.bookingNumber, fare = order.fare.toInt(),
            existingCustomerRating = if ((order.rating ?: 0) > 0) order.rating?.toDouble() else null,
            existingCustomerFeedback = order.review, driverRatingForCustomer = null, driverFeedbackForCustomer = null,
            onDismiss = { if (!isSubmitting) orderToRate = null },
            onSubmit = { rating, feedback -> viewModel.submitRating(order.bookingId, rating, feedback); orderToRate = null },
            isSubmitting = isSubmitting
        )
    }
}

@Composable
private fun ActiveBookingBanner() {
    Surface(modifier = Modifier.fillMaxWidth(), color = Tokens.AmberBg, tonalElevation = 0.dp) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = Tokens.SpaceLG, vertical = Tokens.SpaceMD), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Tokens.SpaceMD)) {
            Box(modifier = Modifier.size(28.dp).clip(CircleShape).background(Tokens.Amber.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                Icon(imageVector = Icons.Rounded.Info, contentDescription = null, tint = Tokens.Amber, modifier = Modifier.size(16.dp))
            }
            Text(text = stringResource(R.string.active_booking_banner_orders), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium, lineHeight = 18.sp), color = Color(0xFF92400E))
        }
    }
}

@Composable
private fun StatusFilterRow(selectedFilter: String?, onFilterSelected: (String?) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(bottom = Tokens.SpaceMD).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Tokens.SpaceSM)) {
        Spacer(modifier = Modifier.width(Tokens.SpaceLG))
        filterChips.forEach { chip ->
            val isSelected = selectedFilter == chip.filterKey
            FilterChip(
                selected = isSelected, onClick = { onFilterSelected(chip.filterKey) },
                label = { Text(text = chip.label, style = MaterialTheme.typography.labelMedium.copy(fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium, fontSize = 13.sp)) },
                leadingIcon = { Icon(imageVector = chip.icon, contentDescription = null, modifier = Modifier.size(15.dp)) },
                shape = RoundedCornerShape(Tokens.RadiusFull),
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = chip.color.copy(alpha = 0.1f), selectedLabelColor = chip.color, selectedLeadingIconColor = chip.color, containerColor = Tokens.SurfaceSubtle, labelColor = Tokens.StatusGray, iconColor = Tokens.StatusGray.copy(alpha = 0.6f)),
                border = FilterChipDefaults.filterChipBorder(enabled = true, selected = isSelected, borderColor = Color.Transparent, selectedBorderColor = chip.color.copy(alpha = 0.25f)),
                modifier = Modifier.height(36.dp)
            )
        }
        Spacer(modifier = Modifier.width(Tokens.SpaceLG))
    }
}

@Composable
private fun OrdersList(orders: List<OrderResponse>, onOrderClick: (OrderResponse) -> Unit, onBookAgain: (OrderResponse) -> Unit, onRateOrder: (OrderResponse) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(start = Tokens.SpaceLG, end = Tokens.SpaceLG, top = Tokens.SpaceLG, bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(Tokens.SpaceMD)) {
        itemsIndexed(items = orders, key = { _, order -> order.bookingId }) { index, order ->
            var visible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { visible = true }
            AnimatedVisibility(visible = visible, enter = fadeIn(tween(250, delayMillis = index * 40)) + slideInVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium), initialOffsetY = { it / 5 })) {
                OrderCard(order = order, onClick = { onOrderClick(order) }, onBookAgain = { onBookAgain(order) }, onRateOrder = { onRateOrder(order) })
            }
        }
    }
}

@Composable
private fun OrderCard(order: OrderResponse, onClick: () -> Unit, onBookAgain: () -> Unit, onRateOrder: () -> Unit) {
    val statusConfig = getStatusConfig(order.status)
    val completed = isCompletedStatus(order.status); val cancelled = isCancelledStatus(order.status)
    val searching = isSearchingStatus(order.status); val active = isActiveStatus(order.status)
    val showRating = needsRating(order)

    Surface(modifier = Modifier.fillMaxWidth().shadow(elevation = 2.dp, shape = Tokens.CardShape, ambientColor = Color.Black.copy(alpha = 0.04f), spotColor = Color.Black.copy(alpha = 0.08f)), shape = Tokens.CardShape, color = Tokens.SurfaceWhite, onClick = onClick) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth().padding(start = Tokens.SpaceLG, end = Tokens.SpaceLG, top = Tokens.SpaceLG, bottom = Tokens.SpaceXS), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(Tokens.RadiusFull), color = statusConfig.bgColor, tonalElevation = 0.dp) {
                    Row(modifier = Modifier.padding(horizontal = Tokens.SpaceSM, vertical = Tokens.SpaceXS), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Tokens.SpaceXS)) {
                        Icon(imageVector = statusConfig.icon, contentDescription = null, tint = statusConfig.color, modifier = Modifier.size(13.dp))
                        Text(text = statusConfig.displayLabel, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 11.sp, letterSpacing = 0.3.sp), color = statusConfig.color)
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                StatusTrailingContent(statusConfig = statusConfig, order = order, searching = searching, active = active, completed = completed, cancelled = cancelled)
            }

            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = Tokens.SpaceLG, vertical = Tokens.SpaceMD), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(Tokens.RadiusMD)).background(Brush.verticalGradient(colors = listOf(AppColors.Primary.copy(alpha = 0.06f), AppColors.Primary.copy(alpha = 0.12f)))), contentAlignment = Alignment.Center) {
                    Text(text = getVehicleIcon(order.vehicleType), fontSize = 22.sp)
                }
                Spacer(modifier = Modifier.width(Tokens.SpaceMD))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = order.vehicleType, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.1).sp), color = AppColors.TextPrimary)
                    Spacer(modifier = Modifier.height(Tokens.SpaceXXS))
                    Text(text = DateTimeUtils.formatDateTime(order.createdAt), style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp), color = AppColors.TextSecondary)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "₹${order.fare.toInt()}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp), color = AppColors.TextPrimary)
                    order.paymentMethod?.let { method ->
                        if (method.isNotBlank()) {
                            Spacer(modifier = Modifier.height(Tokens.SpaceXXS))
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                Icon(imageVector = Icons.Outlined.Payment, contentDescription = null, tint = Tokens.StatusGray.copy(alpha = 0.6f), modifier = Modifier.size(12.dp))
                                Text(text = method, style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium), color = Tokens.StatusGray)
                            }
                        }
                    }
                }
            }

            RouteSection(order = order)
            OrderCardFooter(order = order, completed = completed, cancelled = cancelled, showRating = showRating, onBookAgain = onBookAgain, onRateOrder = onRateOrder)
        }
    }
}

@Composable
private fun StatusTrailingContent(statusConfig: StatusConfig, order: OrderResponse, searching: Boolean, active: Boolean, completed: Boolean, cancelled: Boolean) {
    when {
        searching -> { Text(text = stringResource(R.string.looking_for_rider), style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp), color = statusConfig.color.copy(alpha = 0.7f)) }
        active -> {
            order.driverName?.let { name ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Tokens.SpaceXS)) {
                    Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(statusConfig.color.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                        Icon(imageVector = Icons.Rounded.Person, contentDescription = null, tint = statusConfig.color, modifier = Modifier.size(12.dp))
                    }
                    Text(text = name, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 11.sp), color = statusConfig.color)
                }
            }
        }
        completed -> {
            val rating = order.rating ?: 0
            if (rating > 0) {
                Surface(shape = RoundedCornerShape(Tokens.RadiusFull), color = Tokens.AmberBg) {
                    Row(modifier = Modifier.padding(horizontal = Tokens.SpaceSM, vertical = Tokens.SpaceXS), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        Icon(imageVector = Icons.Rounded.Star, contentDescription = null, tint = Tokens.Amber, modifier = Modifier.size(13.dp))
                        Text(text = "$rating.0", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp), color = Color(0xFF92400E))
                    }
                }
            } else {
                Surface(shape = RoundedCornerShape(Tokens.RadiusFull), color = Tokens.AmberBg) {
                    Text(text = stringResource(R.string.unrated), modifier = Modifier.padding(horizontal = Tokens.SpaceSM, vertical = Tokens.SpaceXS), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 10.sp), color = Tokens.Amber)
                }
            }
        }
        cancelled -> {
            order.cancellationReason?.let { reason ->
                Text(text = reason, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = statusConfig.color.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 140.dp))
            }
        }
    }
}

@Composable
private fun RouteSection(order: OrderResponse) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = Tokens.SpaceLG).clip(RoundedCornerShape(Tokens.RadiusMD)).background(Tokens.SurfacePage).padding(Tokens.SpaceMD)) {
        RoutePoint(color = AppColors.Pickup, label = order.pickupContactName ?: stringResource(R.string.pickup_fallback), address = order.pickupAddress)
        Column(modifier = Modifier.padding(start = 7.dp, top = 4.dp, bottom = 4.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            repeat(3) { Box(modifier = Modifier.size(3.dp).background(Tokens.BorderLight, CircleShape)) }
        }
        RoutePoint(color = AppColors.Drop, label = order.dropContactName ?: stringResource(R.string.drop_fallback), address = order.dropAddress)
    }
}

@Composable
private fun RoutePoint(color: Color, label: String, address: String) {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(Tokens.SpaceMD)) {
        Box(modifier = Modifier.padding(top = 2.dp).size(16.dp).clip(CircleShape).background(color.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
            Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(color))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 12.sp), color = AppColors.TextPrimary)
            Spacer(modifier = Modifier.height(1.dp))
            Text(text = address, style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 16.sp), color = AppColors.TextSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun OrderCardFooter(order: OrderResponse, completed: Boolean, cancelled: Boolean, showRating: Boolean, onBookAgain: () -> Unit, onRateOrder: () -> Unit) {
    Spacer(modifier = Modifier.height(Tokens.SpaceMD))
    HorizontalDivider(modifier = Modifier.padding(horizontal = Tokens.SpaceLG), color = Tokens.BorderSubtle, thickness = 1.dp)
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = Tokens.SpaceMD, vertical = Tokens.SpaceSM), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(horizontalArrangement = Arrangement.spacedBy(Tokens.SpaceMD), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f, fill = false)) {
            if (order.distance != null && order.distance > 0) { InfoChip(icon = Icons.Outlined.Route, text = String.format("%.1f km", order.distance)) }
            val customerRating = order.rating ?: 0
            if (completed && customerRating > 0) { CustomerRatingChip(rating = customerRating, feedback = order.review) }
            if ((order.distance == null || order.distance <= 0) && !(completed && customerRating > 0)) { InfoChip(icon = Icons.Outlined.Tag, text = "#${order.bookingNumber}") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Tokens.SpaceXS), verticalAlignment = Alignment.CenterVertically) {
            if (showRating) { SmallActionButton(onClick = onRateOrder, icon = Icons.Rounded.StarRate, label = stringResource(R.string.rate_label), containerColor = Tokens.AmberBg, contentColor = Color(0xFF92400E)) }
            if (completed || cancelled) { SmallActionButton(onClick = onBookAgain, icon = Icons.Rounded.Replay, label = stringResource(R.string.book_again), containerColor = AppColors.Primary.copy(alpha = 0.08f), contentColor = AppColors.Primary) }
        }
    }
}

@Composable
private fun SmallActionButton(onClick: () -> Unit, icon: ImageVector, label: String, containerColor: Color, contentColor: Color) {
    Surface(onClick = onClick, shape = RoundedCornerShape(Tokens.RadiusFull), color = containerColor, tonalElevation = 0.dp) {
        Row(modifier = Modifier.padding(horizontal = Tokens.SpaceMD, vertical = Tokens.SpaceSM), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Tokens.SpaceXS)) {
            Icon(imageVector = icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(14.dp))
            Text(text = label, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 0.2.sp), color = contentColor)
        }
    }
}

@Composable
private fun InfoChip(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Tokens.SpaceXS)) {
        Icon(imageVector = icon, contentDescription = null, tint = Tokens.StatusGray.copy(alpha = 0.5f), modifier = Modifier.size(14.dp))
        Text(text = text, style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium), color = Tokens.StatusGray)
    }
}

@Composable
private fun CustomerRatingChip(rating: Int, feedback: String?) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Tokens.SpaceXS)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(1.dp)) {
            repeat(5) { index ->
                Icon(imageVector = if (index < rating) Icons.Rounded.Star else Icons.Rounded.StarBorder, contentDescription = null, tint = if (index < rating) Tokens.Amber else Tokens.StatusGray.copy(alpha = 0.25f), modifier = Modifier.size(13.dp))
            }
        }
        if (!feedback.isNullOrBlank()) {
            Box(modifier = Modifier.size(3.dp).clip(CircleShape).background(Tokens.StatusGray.copy(alpha = 0.3f)))
            Text(text = "\"$feedback\"", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Normal, lineHeight = 14.sp), color = Tokens.StatusGray, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 120.dp))
        }
    }
}

@Composable
private fun LoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Tokens.SpaceXL)) {
            CircularProgressIndicator(color = AppColors.Primary, modifier = Modifier.size(36.dp), strokeWidth = 3.dp)
            Text(text = stringResource(R.string.loading_orders), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = AppColors.TextSecondary)
        }
    }
}

@Composable
private fun ErrorState(error: String, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Tokens.SpaceLG), modifier = Modifier.padding(Tokens.Space4XL)) {
            Box(modifier = Modifier.size(72.dp).clip(CircleShape).background(Tokens.StatusRedBg), contentAlignment = Alignment.Center) {
                Icon(imageVector = Icons.Rounded.ErrorOutline, contentDescription = stringResource(R.string.error_content_description), tint = Tokens.StatusRed, modifier = Modifier.size(36.dp))
            }
            Spacer(modifier = Modifier.height(Tokens.SpaceSM))
            Text(text = stringResource(R.string.something_went_wrong), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.2).sp), color = AppColors.TextPrimary)
            Text(text = error, style = MaterialTheme.typography.bodyMedium, color = AppColors.TextSecondary, textAlign = TextAlign.Center, lineHeight = 22.sp)
            Spacer(modifier = Modifier.height(Tokens.SpaceSM))
            Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary), shape = RoundedCornerShape(Tokens.RadiusMD), contentPadding = PaddingValues(horizontal = Tokens.Space3XL, vertical = Tokens.SpaceMD), elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)) {
                Icon(Icons.Rounded.Refresh, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(Tokens.SpaceSM))
                Text(stringResource(R.string.try_again), fontWeight = FontWeight.SemiBold, letterSpacing = 0.2.sp)
            }
        }
    }
}

@Composable
private fun EmptyOrdersState(filter: String?) {
    val filterLabel = filterChips.find { it.filterKey == filter }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Tokens.SpaceMD), modifier = Modifier.padding(Tokens.Space4XL)) {
            Box(modifier = Modifier.size(88.dp).clip(CircleShape).background((filterLabel?.color ?: AppColors.Primary).copy(alpha = 0.06f)), contentAlignment = Alignment.Center) {
                Icon(imageVector = filterLabel?.icon ?: Icons.Outlined.Receipt, contentDescription = null, tint = (filterLabel?.color ?: AppColors.Primary).copy(alpha = 0.5f), modifier = Modifier.size(40.dp))
            }
            Spacer(modifier = Modifier.height(Tokens.SpaceSM))
            Text(
                text = when (filter) {
                    "completed" -> stringResource(R.string.no_completed_orders)
                    "cancelled" -> stringResource(R.string.no_cancelled_orders)
                    "searching" -> stringResource(R.string.no_searching_orders)
                    "active" -> stringResource(R.string.no_active_orders)
                    else -> stringResource(R.string.no_orders_yet)
                },
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
                color = AppColors.TextPrimary
            )
            Text(
                text = when (filter) {
                    "completed" -> stringResource(R.string.completed_orders_empty_subtitle)
                    "cancelled" -> stringResource(R.string.cancelled_orders_empty_subtitle)
                    "searching" -> stringResource(R.string.searching_orders_empty_subtitle)
                    "active" -> stringResource(R.string.active_orders_empty_subtitle)
                    else -> stringResource(R.string.orders_empty_subtitle)
                },
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                color = AppColors.TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}