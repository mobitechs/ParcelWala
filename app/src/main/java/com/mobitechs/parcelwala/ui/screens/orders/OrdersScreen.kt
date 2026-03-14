// ui/screens/orders/OrdersScreen.kt
package com.mobitechs.parcelwala.ui.screens.orders

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Payment
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Inventory
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PersonPin
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.StarRate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.mobitechs.parcelwala.ui.theme.AppRadius
import com.mobitechs.parcelwala.ui.theme.AppSpacing
import com.mobitechs.parcelwala.ui.viewmodel.OrdersViewModel
import com.mobitechs.parcelwala.ui.viewmodel.RatingSubmitState
import com.mobitechs.parcelwala.utils.Constants
import com.mobitechs.parcelwala.utils.DateTimeUtils
import kotlinx.coroutines.launch

// ── Status configuration ──────────────────────────────────────────────────────

private data class StatusConfig(
    val icon: ImageVector,
    val color: Color,
    val bgColor: Color,
    @StringRes val labelRes: Int = 0,
    val labelFallback: String = ""
)

@Composable
private fun StatusConfig.resolveLabel(): String =
    if (labelRes != 0) stringResource(labelRes) else labelFallback

private fun getStatusConfig(status: String): StatusConfig =
    when (status.lowercase()) {
        Constants.OrderStatus.DELIVERY_COMPLETED,
        Constants.OrderStatus.COMPLETED          -> StatusConfig(
            Icons.Rounded.CheckCircle,
            AppColors.Pickup, AppColors.GreenLight,
            labelRes = R.string.status_delivered
        )
        Constants.OrderStatus.CANCELLED          -> StatusConfig(
            Icons.Rounded.Cancel,
            AppColors.Drop, AppColors.ErrorLight,
            labelRes = R.string.cancelled_label
        )
        Constants.OrderStatus.SEARCHING          -> StatusConfig(
            Icons.Rounded.Search,
            AppColors.WarningAmberDark, AppColors.WarningAmberBg,
            labelRes = R.string.status_searching
        )
        Constants.OrderStatus.ASSIGNED           -> StatusConfig(
            Icons.Rounded.PersonPin,
            AppColors.Blue, AppColors.PrimaryLight,
            labelRes = R.string.status_assigned
        )
        Constants.OrderStatus.ARRIVING,
        Constants.OrderStatus.DRIVER_ARRIVING    -> StatusConfig(
            Icons.Rounded.DirectionsCar,
            AppColors.Blue, AppColors.PrimaryLight,
            labelRes = R.string.status_arriving
        )
        Constants.OrderStatus.PICKED_UP          -> StatusConfig(
            Icons.Rounded.Inventory,
            AppColors.Primary, AppColors.PrimaryLight,
            labelRes = R.string.status_picked_up
        )
        Constants.OrderStatus.IN_PROGRESS,
        Constants.OrderStatus.IN_PROGRESS_SPACE  -> StatusConfig(
            Icons.Rounded.LocalShipping,
            AppColors.WarningAmberDark, AppColors.WarningAmberBg,
            labelRes = R.string.status_in_progress
        )
        Constants.OrderStatus.PENDING            -> StatusConfig(
            Icons.Rounded.Schedule,
            AppColors.Gray600, AppColors.Gray100,
            labelRes = R.string.status_pending
        )
        else                                     -> StatusConfig(
            Icons.Rounded.Schedule,
            AppColors.Gray600, AppColors.Gray100,
            labelFallback = status.replaceFirstChar { it.uppercase() }
        )
    }

private fun isCompletedStatus(status: String) =
    status.lowercase() in Constants.OrderStatus.COMPLETED_SET

private fun isCancelledStatus(status: String) =
    status.lowercase() == Constants.OrderStatus.CANCELLED

private fun isSearchingStatus(status: String) =
    status.lowercase() == Constants.OrderStatus.SEARCHING

private fun isActiveStatus(status: String) =
    status.lowercase() in Constants.OrderStatus.ACTIVE_SET

private fun needsRating(order: OrderResponse) =
    isCompletedStatus(order.status) && (order.rating == null || order.rating == 0)

// ── Vehicle icon ──────────────────────────────────────────────────────────────

private fun getVehicleIcon(vehicleType: String): String = when {
    vehicleType.contains(Constants.VehicleType.TWO_WHEELER,   ignoreCase = true) -> "🏍️"
    vehicleType.contains(Constants.VehicleType.BIKE,          ignoreCase = true) -> "🏍️"
    vehicleType.contains(Constants.VehicleType.THREE_WHEELER, ignoreCase = true) -> "🛺"
    vehicleType.contains(Constants.VehicleType.AUTO,          ignoreCase = true) -> "🛺"
    vehicleType.contains(Constants.VehicleType.TATA_ACE,      ignoreCase = true) -> "🚚"
    vehicleType.contains(Constants.VehicleType.PICKUP,        ignoreCase = true) -> "🚙"
    vehicleType.contains(Constants.VehicleType.TEMPO,         ignoreCase = true) -> "🚛"
    vehicleType.contains(Constants.VehicleType.HAMAL,         ignoreCase = true) -> "🚶"
    vehicleType.contains(Constants.VehicleType.MINI_TRUCK,    ignoreCase = true) -> "🚛"
    else                                                                          -> "📦"
}

// ── Filter chips ──────────────────────────────────────────────────────────────

private data class FilterChipData(
    @StringRes val labelRes: Int,
    val filterKey: String?,
    val icon: ImageVector,
    val color: Color
)

private val filterChips = listOf(
    FilterChipData(R.string.filter_all,             null,                          Icons.Rounded.Receipt,      AppColors.Gray600),
    FilterChipData(R.string.status_searching,       Constants.FilterKey.SEARCHING, Icons.Rounded.Search,       AppColors.WarningAmberDark),
    FilterChipData(R.string.status_in_progress,     Constants.FilterKey.ACTIVE,    Icons.Rounded.LocalShipping,AppColors.WarningAmberDark),
    FilterChipData(R.string.label_status_completed, Constants.FilterKey.COMPLETED, Icons.Rounded.CheckCircle,  AppColors.Pickup),
    FilterChipData(R.string.cancelled_label,        Constants.FilterKey.CANCELLED, Icons.Rounded.Cancel,       AppColors.Drop)
)

// ── Screen ────────────────────────────────────────────────────────────────────

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
            is RatingSubmitState.Success -> {
                snackbarHostState.showSnackbar(state.message); viewModel.clearRatingState()
            }
            is RatingSubmitState.Error -> {
                snackbarHostState.showSnackbar(state.message); viewModel.clearRatingState()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            Surface(color = AppColors.White, shadowElevation = 0.dp, tonalElevation = 0.dp) {
                Column {
                    TopAppBar(
                        title = {
                            Text(
                                text = stringResource(R.string.my_orders),
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp
                                ),
                                color = AppColors.TextPrimary
                            )
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            scrolledContainerColor = Color.Transparent
                        )
                    )
                    StatusFilterRow(
                        selectedFilter = selectedFilter,
                        onFilterSelected = { viewModel.onFilterSelected(it) }
                    )
                    HorizontalDivider(color = AppColors.Gray100, thickness = 1.dp)
                }
            }
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = AppColors.Gray900,
                    contentColor = AppColors.White,
                    shape = RoundedCornerShape(AppRadius.MD),
                    modifier = Modifier.padding(AppSpacing.LG)
                )
            }
        },
        containerColor = AppColors.LightGray50
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (hasActiveBooking) ActiveBookingBanner()
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { isRefreshing = true; viewModel.refreshOrders() },
                modifier = Modifier.fillMaxSize()
            ) {
                when {
                    uiState.isLoading && uiState.orders.isEmpty() -> LoadingState()
                    uiState.error != null && uiState.orders.isEmpty() ->
                        ErrorState(
                            error = uiState.error ?: "",
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
                                        message = completeOrCancelMessage,
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            } else onBookAgain(order)
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
            bookingNumber = order.bookingNumber,
            fare = order.fare.toInt(),
            existingCustomerRating = if ((order.rating ?: 0) > 0) order.rating?.toDouble() else null,
            existingCustomerFeedback = order.review,
            driverRatingForCustomer = null,
            driverFeedbackForCustomer = null,
            onDismiss = { if (!isSubmitting) orderToRate = null },
            onSubmit = { rating, feedback ->
                viewModel.submitRating(order.bookingId, rating, feedback)
                orderToRate = null
            },
            isSubmitting = isSubmitting
        )
    }
}

// ── Active booking banner ─────────────────────────────────────────────────────

@Composable
private fun ActiveBookingBanner() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = AppColors.AmberLight,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.LG, vertical = AppSpacing.MD),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.MD)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(AppColors.Warning.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Info,
                    contentDescription = null,
                    tint = AppColors.Warning,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = stringResource(R.string.active_booking_banner_orders),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Medium, lineHeight = 18.sp
                ),
                color = AppColors.OrangeDark
            )
        }
    }
}

// ── Filter row ────────────────────────────────────────────────────────────────

@Composable
private fun StatusFilterRow(selectedFilter: String?, onFilterSelected: (String?) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = AppSpacing.MD)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.SM)
    ) {
        Spacer(modifier = Modifier.width(AppSpacing.LG))
        filterChips.forEach { chip ->
            val isSelected = selectedFilter == chip.filterKey
            FilterChip(
                selected = isSelected,
                onClick = { onFilterSelected(chip.filterKey) },
                label = {
                    Text(
                        text = stringResource(chip.labelRes),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = chip.icon,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                },
                shape = RoundedCornerShape(AppRadius.Full),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = chip.color.copy(alpha = 0.1f),
                    selectedLabelColor = chip.color,
                    selectedLeadingIconColor = chip.color,
                    containerColor = AppColors.Gray100,
                    labelColor = AppColors.Gray600,
                    iconColor = AppColors.Gray600.copy(alpha = 0.6f)
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true, selected = isSelected,
                    borderColor = Color.Transparent,
                    selectedBorderColor = chip.color.copy(alpha = 0.25f)
                ),
                modifier = Modifier.height(36.dp)
            )
        }
        Spacer(modifier = Modifier.width(AppSpacing.LG))
    }
}

// ── Orders list ───────────────────────────────────────────────────────────────

@Composable
private fun OrdersList(
    orders: List<OrderResponse>,
    onOrderClick: (OrderResponse) -> Unit,
    onBookAgain: (OrderResponse) -> Unit,
    onRateOrder: (OrderResponse) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = AppSpacing.LG, end = AppSpacing.LG,
            top = AppSpacing.LG, bottom = 96.dp
        ),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.MD)
    ) {
        itemsIndexed(items = orders, key = { _, order -> order.bookingId }) { index, order ->
            var visible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { visible = true }
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(250, delayMillis = index * 40)) + slideInVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    initialOffsetY = { it / 5 }
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
    }
}

// ── Order card ────────────────────────────────────────────────────────────────

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
    val statusLabel = statusConfig.resolveLabel()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 2.dp, shape = AppRadius.Card,
                ambientColor = AppColors.Black.copy(alpha = 0.04f),
                spotColor = AppColors.Black.copy(alpha = 0.08f)
            ),
        shape = AppRadius.Card,
        color = AppColors.White,
        onClick = onClick
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Status badge row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = AppSpacing.LG, end = AppSpacing.LG,
                        top = AppSpacing.LG, bottom = AppSpacing.XS
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(AppRadius.Full),
                    color = statusConfig.bgColor,
                    tonalElevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier.padding(
                            horizontal = AppSpacing.SM, vertical = AppSpacing.XS
                        ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.XS)
                    ) {
                        Icon(
                            imageVector = statusConfig.icon, contentDescription = null,
                            tint = statusConfig.color, modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = statusLabel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                                letterSpacing = 0.3.sp
                            ),
                            color = statusConfig.color
                        )
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                StatusTrailingContent(
                    statusConfig = statusConfig,
                    order = order,
                    searching = searching,
                    active = active,
                    completed = completed,
                    cancelled = cancelled
                )
            }

            // Vehicle + fare row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.LG, vertical = AppSpacing.MD),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(AppRadius.MD))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    AppColors.Primary.copy(alpha = 0.06f),
                                    AppColors.Primary.copy(alpha = 0.12f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = getVehicleIcon(order.vehicleType), fontSize = 22.sp)
                }
                Spacer(modifier = Modifier.width(AppSpacing.MD))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = order.vehicleType,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold, letterSpacing = (-0.1).sp
                        ),
                        color = AppColors.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(AppSpacing.XXS))
                    Text(
                        text = DateTimeUtils.formatDateTime(order.createdAt),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = AppColors.TextSecondary
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = stringResource(R.string.fare_format, order.fare.toInt()),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp
                        ),
                        color = AppColors.TextPrimary
                    )
                    order.paymentMethod?.let { method ->
                        if (method.isNotBlank()) {
                            Spacer(modifier = Modifier.height(AppSpacing.XXS))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Payment,
                                    contentDescription = null,
                                    tint = AppColors.Gray600.copy(alpha = 0.6f),
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = method,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.sp, fontWeight = FontWeight.Medium
                                    ),
                                    color = AppColors.Gray600
                                )
                            }
                        }
                    }
                }
            }

            RouteSection(order = order)
            OrderCardFooter(
                order = order, completed = completed, cancelled = cancelled,
                showRating = showRating, onBookAgain = onBookAgain, onRateOrder = onRateOrder
            )
        }
    }
}

// ── Status trailing content ───────────────────────────────────────────────────

@Composable
private fun StatusTrailingContent(
    statusConfig: StatusConfig, order: OrderResponse,
    searching: Boolean, active: Boolean, completed: Boolean, cancelled: Boolean
) {
    when {
        searching -> Text(
            text = stringResource(R.string.looking_for_rider),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = statusConfig.color.copy(alpha = 0.7f)
        )
        active -> order.driverName?.let { name ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.XS)
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(statusConfig.color.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Person, contentDescription = null,
                        tint = statusConfig.color, modifier = Modifier.size(12.dp)
                    )
                }
                Text(
                    text = name,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold, fontSize = 11.sp
                    ),
                    color = statusConfig.color
                )
            }
        }
        completed -> {
            val rating = order.rating ?: 0
            if (rating > 0) {
                Surface(
                    shape = RoundedCornerShape(AppRadius.Full),
                    color = AppColors.AmberLight
                ) {
                    Row(
                        modifier = Modifier.padding(
                            horizontal = AppSpacing.SM, vertical = AppSpacing.XS
                        ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Star, contentDescription = null,
                            tint = AppColors.Warning, modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = stringResource(R.string.rating_value_format, rating),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold, fontSize = 11.sp
                            ),
                            color = AppColors.OrangeDark
                        )
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(AppRadius.Full),
                    color = AppColors.AmberLight
                ) {
                    Text(
                        text = stringResource(R.string.unrated),
                        modifier = Modifier.padding(
                            horizontal = AppSpacing.SM, vertical = AppSpacing.XS
                        ),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold, fontSize = 10.sp
                        ),
                        color = AppColors.Warning
                    )
                }
            }
        }
        cancelled -> order.cancellationReason?.let { reason ->
            Text(
                text = reason,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = statusConfig.color.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 140.dp)
            )
        }
    }
}

// ── Route section ─────────────────────────────────────────────────────────────

@Composable
private fun RouteSection(order: OrderResponse) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.LG)
            .clip(RoundedCornerShape(AppRadius.MD))
            .background(AppColors.LightGray50)
            .padding(AppSpacing.MD)
    ) {
        RoutePoint(
            color = AppColors.Pickup,
            label = order.pickupContactName ?: stringResource(R.string.pickup_fallback),
            address = order.pickupAddress
        )
        Column(
            modifier = Modifier.padding(start = 7.dp, top = 4.dp, bottom = 4.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            repeat(3) {
                Box(
                    modifier = Modifier
                        .size(3.dp)
                        .background(AppColors.DividerLight, CircleShape)
                )
            }
        }
        RoutePoint(
            color = AppColors.Drop,
            label = order.dropContactName ?: stringResource(R.string.drop_fallback),
            address = order.dropAddress
        )
    }
}

@Composable
private fun RoutePoint(color: Color, label: String, address: String) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.MD)
    ) {
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .size(16.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold, fontSize = 12.sp
                ),
                color = AppColors.TextPrimary
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = address,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp, lineHeight = 16.sp
                ),
                color = AppColors.TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ── Card footer ───────────────────────────────────────────────────────────────

@Composable
private fun OrderCardFooter(
    order: OrderResponse,
    completed: Boolean, cancelled: Boolean, showRating: Boolean,
    onBookAgain: () -> Unit, onRateOrder: () -> Unit
) {
    Spacer(modifier = Modifier.height(AppSpacing.MD))
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = AppSpacing.LG),
        color = AppColors.Gray100, thickness = 1.dp
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.MD, vertical = AppSpacing.SM),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.MD),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f, fill = false)
        ) {
            if (order.distance != null && order.distance > 0) {
                InfoChip(
                    icon = Icons.Outlined.Route,
                    text = stringResource(R.string.distance_km_format, order.distance)
                )
            }
            val customerRating = order.rating ?: 0
            if (completed && customerRating > 0) {
                CustomerRatingChip(rating = customerRating, feedback = order.review)
            }
            if ((order.distance == null || order.distance <= 0) && !(completed && customerRating > 0)) {
                InfoChip(
                    icon = Icons.Outlined.Tag,
                    text = stringResource(R.string.booking_number_format, order.bookingNumber)
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.XS),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showRating) {
                SmallActionButton(
                    onClick = onRateOrder, icon = Icons.Rounded.StarRate,
                    label = stringResource(R.string.rate_label),
                    containerColor = AppColors.AmberLight, contentColor = AppColors.OrangeDark
                )
            }
            if (completed || cancelled) {
                SmallActionButton(
                    onClick = onBookAgain, icon = Icons.Rounded.Replay,
                    label = stringResource(R.string.book_again),
                    containerColor = AppColors.Primary.copy(alpha = 0.08f),
                    contentColor = AppColors.Primary
                )
            }
        }
    }
}

@Composable
private fun SmallActionButton(
    onClick: () -> Unit, icon: ImageVector, label: String,
    containerColor: Color, contentColor: Color
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(AppRadius.Full),
        color = containerColor, tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = AppSpacing.MD, vertical = AppSpacing.SM),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.XS)
        ) {
            Icon(
                imageVector = icon, contentDescription = null,
                tint = contentColor, modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 0.2.sp
                ),
                color = contentColor
            )
        }
    }
}

@Composable
private fun InfoChip(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.XS)
    ) {
        Icon(
            imageVector = icon, contentDescription = null,
            tint = AppColors.Gray600.copy(alpha = 0.5f), modifier = Modifier.size(14.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp, fontWeight = FontWeight.Medium
            ),
            color = AppColors.Gray600
        )
    }
}

@Composable
private fun CustomerRatingChip(rating: Int, feedback: String?) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.XS)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            repeat(5) { index ->
                Icon(
                    imageVector = if (index < rating) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                    contentDescription = null,
                    tint = if (index < rating) AppColors.Warning else AppColors.Gray600.copy(alpha = 0.25f),
                    modifier = Modifier.size(13.dp)
                )
            }
        }
        if (!feedback.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .size(3.dp)
                    .clip(CircleShape)
                    .background(AppColors.Gray600.copy(alpha = 0.3f))
            )
            Text(
                text = stringResource(R.string.feedback_quote_format, feedback),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp, fontWeight = FontWeight.Normal, lineHeight = 14.sp
                ),
                color = AppColors.Gray600, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 120.dp)
            )
        }
    }
}

// ── State composables ─────────────────────────────────────────────────────────

@Composable
private fun LoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppSpacing.XL)
        ) {
            CircularProgressIndicator(
                color = AppColors.Primary,
                modifier = Modifier.size(36.dp),
                strokeWidth = 3.dp
            )
            Text(
                text = stringResource(R.string.loading_orders),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
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
            verticalArrangement = Arrangement.spacedBy(AppSpacing.LG),
            modifier = Modifier.padding(AppSpacing.X4L)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(AppColors.ErrorLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.ErrorOutline,
                    contentDescription = stringResource(R.string.error_content_description),
                    tint = AppColors.Drop, modifier = Modifier.size(36.dp)
                )
            }
            Spacer(modifier = Modifier.height(AppSpacing.SM))
            Text(
                text = stringResource(R.string.something_went_wrong),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold, letterSpacing = (-0.2).sp
                ),
                color = AppColors.TextPrimary
            )
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextSecondary, textAlign = TextAlign.Center, lineHeight = 22.sp
            )
            Spacer(modifier = Modifier.height(AppSpacing.SM))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary),
                shape = RoundedCornerShape(AppRadius.MD),
                contentPadding = PaddingValues(
                    horizontal = AppSpacing.X3L, vertical = AppSpacing.MD
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Icon(Icons.Rounded.Refresh, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(AppSpacing.SM))
                Text(
                    stringResource(R.string.try_again),
                    fontWeight = FontWeight.SemiBold, letterSpacing = 0.2.sp
                )
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
            verticalArrangement = Arrangement.spacedBy(AppSpacing.MD),
            modifier = Modifier.padding(AppSpacing.X4L)
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background((filterLabel?.color ?: AppColors.Primary).copy(alpha = 0.06f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = filterLabel?.icon ?: Icons.Outlined.Receipt,
                    contentDescription = null,
                    tint = (filterLabel?.color ?: AppColors.Primary).copy(alpha = 0.5f),
                    modifier = Modifier.size(40.dp)
                )
            }
            Spacer(modifier = Modifier.height(AppSpacing.SM))
            Text(
                text = when (filter) {
                    Constants.FilterKey.COMPLETED -> stringResource(R.string.no_completed_orders)
                    Constants.FilterKey.CANCELLED -> stringResource(R.string.no_cancelled_orders)
                    Constants.FilterKey.SEARCHING -> stringResource(R.string.no_searching_orders)
                    Constants.FilterKey.ACTIVE    -> stringResource(R.string.no_active_orders)
                    else                          -> stringResource(R.string.no_orders_yet)
                },
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp
                ),
                color = AppColors.TextPrimary
            )
            Text(
                text = when (filter) {
                    Constants.FilterKey.COMPLETED -> stringResource(R.string.completed_orders_empty_subtitle)
                    Constants.FilterKey.CANCELLED -> stringResource(R.string.cancelled_orders_empty_subtitle)
                    Constants.FilterKey.SEARCHING -> stringResource(R.string.searching_orders_empty_subtitle)
                    Constants.FilterKey.ACTIVE    -> stringResource(R.string.active_orders_empty_subtitle)
                    else                          -> stringResource(R.string.orders_empty_subtitle)
                },
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                color = AppColors.TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}