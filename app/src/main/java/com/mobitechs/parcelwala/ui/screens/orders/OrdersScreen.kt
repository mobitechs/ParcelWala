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
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
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
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Replay
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
import androidx.compose.runtime.SideEffect
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
import com.mobitechs.parcelwala.ui.components.StatusBarScaffold
import com.mobitechs.parcelwala.ui.theme.AppColors
import com.mobitechs.parcelwala.ui.theme.AppRadius
import com.mobitechs.parcelwala.ui.theme.AppSpacing
import com.mobitechs.parcelwala.ui.viewmodel.OrdersViewModel
import com.mobitechs.parcelwala.ui.viewmodel.RatingSubmitState
import com.mobitechs.parcelwala.utils.Constants
import com.mobitechs.parcelwala.utils.DateTimeUtils
import kotlinx.coroutines.launch

// ── Status configuration ──────────────────────────────────────────────────────

// getAccentColor is screen-specific — delegates to the shared getStatusConfig
private fun getAccentColor(status: String): Color = getStatusConfig(status).color

// ── Filter chips ──────────────────────────────────────────────────────────────

private data class FilterChipData(
    @StringRes val labelRes: Int,
    val filterKey: String?,
    val icon: ImageVector,
    // selectedColor: color shown when this chip is active.
    // "All" uses AppColors.Primary (teal) so its selected state is clearly visible.
    // Other chips use their own semantic color.
    val selectedColor: Color,
)

private val filterChips = listOf(
    FilterChipData(
        labelRes = R.string.filter_all,
        filterKey = null,
        icon = Icons.Rounded.Receipt,
        selectedColor = AppColors.Primary          // ← teal, clearly visible when "All" is active
    ),
    FilterChipData(
        labelRes = R.string.status_searching,
        filterKey = Constants.FilterKey.SEARCHING,
        icon = Icons.Rounded.Search,
        selectedColor = AppColors.WarningAmberDark
    ),
    FilterChipData(
        labelRes = R.string.status_in_progress,
        filterKey = Constants.FilterKey.ACTIVE,
        icon = Icons.Rounded.LocalShipping,
        selectedColor = AppColors.WarningAmberDark
    ),
    FilterChipData(
        labelRes = R.string.label_status_completed,
        filterKey = Constants.FilterKey.COMPLETED,
        icon = Icons.Rounded.CheckCircle,
        selectedColor = AppColors.Pickup
    ),
    FilterChipData(
        labelRes = R.string.cancelled_label,
        filterKey = Constants.FilterKey.CANCELLED,
        icon = Icons.Rounded.Cancel,
        selectedColor = AppColors.Drop
    )
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
                snackbarHostState.showSnackbar(state.message)
                viewModel.clearRatingState()
            }

            is RatingSubmitState.Error -> {
                snackbarHostState.showSnackbar(state.message)
                viewModel.clearRatingState()
            }

            else -> {}
        }
    }



    StatusBarScaffold(
        statusBarColor = AppColors.Primary,
        darkStatusBarIcons = false,
        topBar = {


            Surface(color = AppColors.Primary, shadowElevation = 0.dp, tonalElevation = 0.dp) {
                Column {

                    TopAppBar(
                        title = {
                            Text(
                                text = stringResource(R.string.my_orders),
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-0.3).sp
                                ),
                                color = AppColors.White
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
            existingCustomerRating = if ((order.rating
                    ?: 0) > 0
            ) order.rating?.toDouble() else null,
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
                    fontWeight = FontWeight.Medium,
                    lineHeight = 18.sp
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
                    // Selected state — uses chip.selectedColor so "All" gets teal,
                    // others get their own semantic color
                    selectedContainerColor = chip.selectedColor.copy(alpha = 0.12f),
                    selectedLabelColor = chip.selectedColor,
                    selectedLeadingIconColor = chip.selectedColor,
                    // Unselected state — same neutral gray for all chips
                    containerColor = AppColors.Gray100,
                    labelColor = AppColors.Gray600,
                    iconColor = AppColors.Gray600.copy(alpha = 0.6f)
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = Color.Transparent,
                    selectedBorderColor = chip.selectedColor.copy(alpha = 0.3f)
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


@Composable
private fun OrderCard(
    order: OrderResponse,
    onClick: () -> Unit,
    onBookAgain: () -> Unit,
    onRateOrder: () -> Unit
) {
    val statusConfig = getStatusConfig(order.status)
    val accentColor = getAccentColor(order.status)
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
                elevation = 1.dp,
                shape = AppRadius.Card,
                ambientColor = AppColors.Black.copy(alpha = 0.03f),
                spotColor = AppColors.Black.copy(alpha = 0.06f)
            ),
        shape = AppRadius.Card,
        color = AppColors.White,
        onClick = onClick
    ) {
        Row(modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)) {

            // ── Left accent bar (status color) ────────────────────────────
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(accentColor)
            )

            // ── Card body ─────────────────────────────────────────────────
            Column(modifier = Modifier.fillMaxWidth()) {

                // Header: icon + booking number + date + status badge
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = AppSpacing.MD,
                            end = AppSpacing.LG,
                            top = AppSpacing.LG,
                            bottom = AppSpacing.SM
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.SM)
                ) {
                    // Vehicle emoji box — tinted with accent color
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(AppRadius.MD))
                            .background(accentColor.copy(alpha = 0.08f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = getVehicleIcon(order.vehicleType),
                            fontSize = 20.sp
                        )
                    }

                    // Booking number + date
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = order.bookingNumber,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.1).sp
                            ),
                            color = AppColors.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = DateTimeUtils.formatDateTime(order.createdAt),
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = AppColors.TextSecondary
                        )
                    }

                    // Status badge
                    Surface(
                        shape = RoundedCornerShape(AppRadius.Full),
                        color = statusConfig.bgColor,
                        tonalElevation = 0.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(
                                horizontal = AppSpacing.SM,
                                vertical = AppSpacing.XS
                            ),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = statusConfig.icon,
                                contentDescription = null,
                                tint = statusConfig.color,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = statusLabel,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 10.sp,
                                    letterSpacing = 0.3.sp
                                ),
                                color = statusConfig.color
                            )
                        }
                    }
                }

                // Route box — contains meta strip + pickup/drop
                RouteSection(
                    order = order,
                    completed = completed,
                    cancelled = cancelled,
                    searching = searching,
                    active = active
                )

                // Divider
                HorizontalDivider(
                    modifier = Modifier.padding(
                        start = AppSpacing.MD,
                        end = AppSpacing.LG,
                        top = AppSpacing.SM
                    ),
                    color = AppColors.Gray100,
                    thickness = 1.dp
                )

                // Fare row — left side:
                //   cancelled → plain cancellation reason text (no background)
                //   otherwise → payment method pill
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = AppSpacing.MD,
                            end = AppSpacing.LG,
                            top = AppSpacing.SM,
                            bottom = if (showRating || completed || cancelled) 0.dp else AppSpacing.LG
                        ),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left side
                    if (cancelled && !order.cancellationReason.isNullOrBlank()) {
                        Text(
                            text = order.cancellationReason!!,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = AppColors.TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = AppSpacing.SM)
                        )
                    } else {
                        order.paymentMethod?.takeIf { it.isNotBlank() }?.let { method ->
                            Surface(
                                shape = RoundedCornerShape(AppRadius.Full),
                                color = AppColors.Gray100,
                                tonalElevation = 0.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(
                                        horizontal = AppSpacing.SM,
                                        vertical = AppSpacing.XS
                                    ),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Payment,
                                        contentDescription = null,
                                        tint = AppColors.Gray600.copy(alpha = 0.6f),
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Text(
                                        text = method,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        ),
                                        color = AppColors.Gray600
                                    )
                                }
                            }
                        } ?: Spacer(modifier = Modifier.width(1.dp))
                    }

                    // Fare — always in accent color
                    Text(
                        text = stringResource(R.string.fare_format, order.fare.toInt()),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.3).sp
                        ),
                        color = accentColor
                    )
                }

                // Action buttons
                if (showRating || completed || cancelled) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = AppSpacing.MD,
                                end = AppSpacing.LG,
                                top = AppSpacing.SM,
                                bottom = AppSpacing.LG
                            ),
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.SM)
                    ) {
                        if (showRating) {
                            ActionButton(
                                onClick = onRateOrder,
                                icon = Icons.Rounded.StarRate,
                                label = stringResource(R.string.rate_label),
                                containerColor = AppColors.AmberLight,
                                contentColor = AppColors.OrangeDark,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (completed || cancelled) {
                            ActionButton(
                                onClick = onBookAgain,
                                icon = Icons.Rounded.Replay,
                                label = stringResource(R.string.book_again),
                                containerColor = AppColors.PrimaryLight,
                                contentColor = AppColors.PrimaryDark,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Route section ─────────────────────────────────────────────────────────────

@Composable
private fun RouteSection(
    order: OrderResponse,
    completed: Boolean,
    cancelled: Boolean,
    searching: Boolean,
    active: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = AppSpacing.MD, end = AppSpacing.LG)
            .clip(RoundedCornerShape(AppRadius.MD))
            .background(AppColors.LightGray50)
            .padding(AppSpacing.MD)
    ) {
        // ── Meta strip: vehicle type · contextual right side ─────────────
        // Right side:
        //   active    → driver name (tinted primary)
        //   searching → "Looking for rider…"
        //   all else  → distance when available, booking number as fallback
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = AppSpacing.SM),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Vehicle type — left
            Text(
                text = order.vehicleType,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp
                ),
                color = AppColors.TextPrimary
            )

            // Right side
            when {
                active && !order.driverName.isNullOrBlank() -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Person,
                            contentDescription = null,
                            tint = AppColors.Primary,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = order.driverName!!,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.sp
                            ),
                            color = AppColors.Primary
                        )
                    }
                }

                searching -> {
                    Text(
                        text = stringResource(R.string.looking_for_rider),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = AppColors.WarningAmberDark
                    )
                }

                order.distance != null && order.distance > 0 -> {
                    // Distance shown for ALL other statuses including cancelled
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Route,
                            contentDescription = null,
                            tint = AppColors.Gray600.copy(alpha = 0.5f),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = stringResource(R.string.distance_km_format, order.distance),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = AppColors.Gray600
                        )
                    }
                }

                else -> {
                    Text(
                        text = stringResource(R.string.booking_number_format, order.bookingNumber),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = AppColors.Gray600
                    )
                }
            }
        }

        // Thin divider between meta and route points
        HorizontalDivider(color = AppColors.DividerLight, thickness = 1.dp)
        Spacer(modifier = Modifier.height(AppSpacing.SM))

        // Pickup point
        RoutePoint(
            color = AppColors.Pickup,
            label = order.pickupContactName ?: stringResource(R.string.pickup_fallback),
            address = order.pickupAddress
        )

        // Connector dots
        Column(
            modifier = Modifier.padding(start = 4.dp, top = 3.dp, bottom = 3.dp),
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

        // Drop point
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
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.SM)
    ) {
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .size(14.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp
                ),
                color = AppColors.TextPrimary
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = address,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                ),
                color = AppColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ── Action button ─────────────────────────────────────────────────────────────

@Composable
private fun ActionButton(
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(AppRadius.MD),
        color = containerColor,
        tonalElevation = 0.dp,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = AppSpacing.SM),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(AppSpacing.XS))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                ),
                color = contentColor
            )
        }
    }
}

// ── Info chip (kept for any future reuse) ─────────────────────────────────────

@Composable
private fun InfoChip(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.XS)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = AppColors.Gray600.copy(alpha = 0.5f),
            modifier = Modifier.size(13.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
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
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Normal,
                    lineHeight = 14.sp
                ),
                color = AppColors.Gray600,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
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
                    tint = AppColors.Drop,
                    modifier = Modifier.size(36.dp)
                )
            }
            Spacer(modifier = Modifier.height(AppSpacing.SM))
            Text(
                text = stringResource(R.string.something_went_wrong),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.2).sp
                ),
                color = AppColors.TextPrimary
            )
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
            Spacer(modifier = Modifier.height(AppSpacing.SM))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary),
                shape = RoundedCornerShape(AppRadius.MD),
                contentPadding = PaddingValues(
                    horizontal = AppSpacing.X3L,
                    vertical = AppSpacing.MD
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Icon(Icons.Rounded.Refresh, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(AppSpacing.SM))
                Text(
                    stringResource(R.string.try_again),
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.2.sp
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
                    .background(
                        (filterLabel?.selectedColor ?: AppColors.Primary).copy(alpha = 0.06f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = filterLabel?.icon ?: Icons.Outlined.Receipt,
                    contentDescription = null,
                    tint = (filterLabel?.selectedColor ?: AppColors.Primary).copy(alpha = 0.5f),
                    modifier = Modifier.size(40.dp)
                )
            }
            Spacer(modifier = Modifier.height(AppSpacing.SM))
            Text(
                text = when (filter) {
                    Constants.FilterKey.COMPLETED -> stringResource(R.string.no_completed_orders)
                    Constants.FilterKey.CANCELLED -> stringResource(R.string.no_cancelled_orders)
                    Constants.FilterKey.SEARCHING -> stringResource(R.string.no_searching_orders)
                    Constants.FilterKey.ACTIVE -> stringResource(R.string.no_active_orders)
                    else -> stringResource(R.string.no_orders_yet)
                },
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.3).sp
                ),
                color = AppColors.TextPrimary
            )
            Text(
                text = when (filter) {
                    Constants.FilterKey.COMPLETED -> stringResource(R.string.completed_orders_empty_subtitle)
                    Constants.FilterKey.CANCELLED -> stringResource(R.string.cancelled_orders_empty_subtitle)
                    Constants.FilterKey.SEARCHING -> stringResource(R.string.searching_orders_empty_subtitle)
                    Constants.FilterKey.ACTIVE -> stringResource(R.string.active_orders_empty_subtitle)
                    else -> stringResource(R.string.orders_empty_subtitle)
                },
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                color = AppColors.TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}