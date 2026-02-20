// ui/screens/home/HomeScreen.kt
package com.mobitechs.parcelwala.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mobitechs.parcelwala.R
import com.mobitechs.parcelwala.data.manager.ActiveBooking
import com.mobitechs.parcelwala.data.manager.ActiveBookingManager
import com.mobitechs.parcelwala.data.manager.BookingStatus
import com.mobitechs.parcelwala.data.model.response.VehicleTypeResponse
import com.mobitechs.parcelwala.ui.components.AddressesCard
import com.mobitechs.parcelwala.ui.components.EmptyState
import com.mobitechs.parcelwala.ui.components.LoadingIndicator
import com.mobitechs.parcelwala.ui.theme.AppColors
import com.mobitechs.parcelwala.ui.theme.WarningAmber
import com.mobitechs.parcelwala.ui.theme.WarningAmberBg
import com.mobitechs.parcelwala.ui.theme.WarningAmberDark
import com.mobitechs.parcelwala.ui.viewmodel.HomeViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Home Screen
 * Main landing screen showing vehicle types, pickup location, and active booking
 *
 * KEY BEHAVIOR:
 * - When active booking exists → shows active booking card prominently
 * - When active booking exists → blocks new booking with snackbar message
 * - Vehicle grid & pickup card are dimmed/disabled during active booking
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToLocationSearch: () -> Unit,
    onNavigateToActiveBooking: (ActiveBooking) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val activeBooking by viewModel.activeBooking.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // ✅ Determine if new booking should be blocked
    val hasActiveBooking = activeBooking != null

    val activeBookingSnackbar = stringResource(R.string.active_booking_snackbar)
    val completeCurrentSnackbar = stringResource(R.string.complete_current_booking_snackbar)

    Scaffold(
        containerColor = AppColors.Background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.app_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.TextPrimary
                        )
                        Text(
                            text = stringResource(R.string.app_subtitle),
                            style = MaterialTheme.typography.labelSmall,
                            color = AppColors.TextSecondary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO: Notifications */ }) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = stringResource(R.string.notifications),
                            tint = AppColors.Primary
                        )
                    }
                    IconButton(onClick = { /* TODO: Profile */ }) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = stringResource(R.string.profile_content_description),
                            tint = AppColors.Primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                LoadingIndicator(
                    message = stringResource(R.string.loading_vehicles),
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    // ✅ Active Booking Card (shows when there's an active booking)
                    AnimatedVisibility(
                        visible = hasActiveBooking,
                        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
                    ) {
                        activeBooking?.let { booking ->
                            ActiveBookingCard(
                                activeBooking = booking,
                                onClick = { onNavigateToActiveBooking(booking) },
                                onRetry = { viewModel.retrySearch() }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // ✅ Pickup Location - Blocked when active booking exists
                    PickupLocationCard(
                        location = uiState.pickupLocation,
                        onClick = {
                            if (hasActiveBooking) {
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = activeBookingSnackbar,
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            } else {
                                onNavigateToLocationSearch()
                            }
                        },
                        isDisabled = hasActiveBooking
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Section Header
                    Text(
                        text = stringResource(R.string.select_vehicle_type),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (hasActiveBooking) AppColors.TextHint else AppColors.TextPrimary,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )

                    // ✅ Active booking blocking banner
                    if (hasActiveBooking) {
                        ActiveBookingBlockingBanner(
                            onViewBooking = {
                                activeBooking?.let { onNavigateToActiveBooking(it) }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // ✅ Vehicles Grid - Disabled when active booking exists
                    if (uiState.vehicleTypes.isNotEmpty()) {
                        VehicleTypesGrid(
                            vehicleTypes = uiState.vehicleTypes,
                            onVehicleSelected = {
                                if (hasActiveBooking) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            message = completeCurrentSnackbar,
                                            duration = SnackbarDuration.Short
                                        )
                                    }
                                } else {
                                    onNavigateToLocationSearch()
                                }
                            },
                            isDisabled = hasActiveBooking
                        )
                    } else {
                        EmptyState(
                            icon = Icons.Default.DirectionsCar,
                            title = stringResource(R.string.no_vehicles_available),
                            subtitle = stringResource(R.string.try_again_later),
                            actionText = stringResource(R.string.retry),
                            onAction = { viewModel.refresh() },
                            modifier = Modifier.padding(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Announcements
                    AnnouncementsSection()

                    Spacer(modifier = Modifier.height(32.dp))

                    // Marketing Text
                    MarketingText()

                    Spacer(modifier = Modifier.height(100.dp))
                }
            }

            // Error Dialog
            uiState.error?.let { error ->
                AlertDialog(
                    onDismissRequest = { viewModel.clearError() },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = AppColors.Drop
                        )
                    },
                    title = { Text(stringResource(R.string.error_dialog_title)) },
                    text = { Text(error) },
                    confirmButton = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text(stringResource(R.string.ok), color = AppColors.Primary)
                        }
                    },
                    containerColor = Color.White
                )
            }
        }
    }
}

/**
 * Active Booking Blocking Banner
 * Shown between "Select Vehicle Type" header and the grid when booking is active
 */
@Composable
private fun ActiveBookingBlockingBanner(
    onViewBooking: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .clickable(onClick = onViewBooking),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = WarningAmberBg
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Block,
                contentDescription = null,
                tint = WarningAmber,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = stringResource(R.string.active_booking_banner_text),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = WarningAmberDark,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = WarningAmber,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * Active Booking Card - Improved Professional Design
 * Shows complete booking details with pickup/drop addresses
 * Includes 3-minute countdown timer with progress bar and retry option
 */
@Composable
private fun ActiveBookingCard(
    activeBooking: ActiveBooking,
    onClick: () -> Unit,
    onRetry: () -> Unit
) {
    // ═══ COUNTDOWN TIMER STATE (3 minutes = 180 seconds) ═══
    val totalTimeMs = ActiveBookingManager.SEARCH_TIMEOUT_MS
    val searchStartTime = activeBooking.searchStartTime

    var remainingTimeMs by remember(searchStartTime) {
        val elapsed = System.currentTimeMillis() - searchStartTime
        mutableStateOf(maxOf(0L, totalTimeMs - elapsed))
    }
    var isTimedOut by remember(searchStartTime) {
        val elapsed = System.currentTimeMillis() - searchStartTime
        mutableStateOf(elapsed >= totalTimeMs)
    }

    val isSearching = activeBooking.status == BookingStatus.SEARCHING

    // Countdown timer effect
    LaunchedEffect(searchStartTime, isSearching) {
        if (!isSearching) return@LaunchedEffect
        val initialElapsed = System.currentTimeMillis() - searchStartTime
        remainingTimeMs = maxOf(0L, totalTimeMs - initialElapsed)
        isTimedOut = remainingTimeMs <= 0
        while (remainingTimeMs > 0) {
            delay(1000L)
            val newElapsed = System.currentTimeMillis() - searchStartTime
            remainingTimeMs = maxOf(0L, totalTimeMs - newElapsed)
        }
        isTimedOut = true
    }

    val minutes = (remainingTimeMs / 60000).toInt()
    val seconds = ((remainingTimeMs % 60000) / 1000).toInt()
    val timeText = String.format("%02d:%02d", minutes, seconds)

    // Pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // Card colors based on state
    val headerGradient = if (isTimedOut && isSearching) {
        listOf(WarningAmber, WarningAmber.copy(alpha = 0.85f))
    } else {
        listOf(AppColors.Primary, AppColors.Primary.copy(alpha = 0.9f))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = if (isTimedOut && isSearching)
                    WarningAmber.copy(alpha = 0.25f)
                else
                    AppColors.Primary.copy(alpha = 0.25f)
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {

            // ═══ HEADER: Status with Gradient ═══
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(brush = Brush.horizontalGradient(headerGradient))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Status Icon with pulse
                    Box(
                        modifier = Modifier.size(44.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!isTimedOut || !isSearching) {
                            // Animated rings
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .scale(pulseScale)
                                    .background(
                                        color = Color.White.copy(alpha = 0.12f),
                                        shape = CircleShape
                                    )
                            )
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when (activeBooking.status) {
                                        BookingStatus.SEARCHING -> Icons.Default.LocalShipping
                                        BookingStatus.RIDER_ASSIGNED -> Icons.Default.Person
                                        BookingStatus.RIDER_EN_ROUTE -> Icons.Default.DirectionsCar
                                        BookingStatus.PICKED_UP -> Icons.Default.LocalShipping
                                        BookingStatus.IN_TRANSIT -> Icons.Default.LocalShipping
                                        else -> Icons.Default.LocalShipping
                                    },
                                    contentDescription = null,
                                    tint = if (isTimedOut) WarningAmber else AppColors.Primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SearchOff,
                                    contentDescription = null,
                                    tint = WarningAmber,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Status text
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (!isTimedOut && isSearching) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .scale(pulseScale)
                                        .background(Color.White, CircleShape)
                                )
                            }
                            Text(
                                text = when {
                                    isTimedOut && isSearching -> stringResource(R.string.no_riders_available)
                                    activeBooking.status == BookingStatus.SEARCHING -> stringResource(R.string.finding_rider)
                                    activeBooking.status == BookingStatus.RIDER_ASSIGNED -> stringResource(R.string.rider_assigned)
                                    activeBooking.status == BookingStatus.RIDER_EN_ROUTE -> stringResource(R.string.rider_on_the_way)
                                    activeBooking.status == BookingStatus.PICKED_UP -> stringResource(R.string.goods_picked_up)
                                    activeBooking.status == BookingStatus.IN_TRANSIT -> stringResource(R.string.on_the_way_to_drop)
                                    else -> stringResource(R.string.booking_in_progress)
                                },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isSearching) {
                                stringResource(R.string.trip_format, activeBooking.bookingId, activeBooking.searchAttempts)
                            } else {
                                stringResource(R.string.trip_id_format, activeBooking.bookingId)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }

                    // Timer or Arrow
                    if (isSearching) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = timeText,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = if (isTimedOut) stringResource(R.string.timed_out) else stringResource(R.string.remaining),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 9.sp
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color.White.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = stringResource(R.string.view_details),
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // ═══ PROGRESS BAR (searching only) ═══
            if (isSearching) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    if (!isTimedOut) {
                        val elapsedMs = (totalTimeMs - remainingTimeMs).coerceIn(0L, totalTimeMs)
                        val elapsedProgress = (elapsedMs.toFloat() / totalTimeMs.toFloat()).coerceIn(0f, 1f)

                        LinearProgressIndicator(
                            progress = { elapsedProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = when {
                                elapsedProgress > 0.66f -> AppColors.Drop
                                elapsedProgress > 0.33f -> WarningAmber
                                else -> AppColors.Primary
                            },
                            trackColor = AppColors.Border,
                            strokeCap = StrokeCap.Round
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        val elapsedMinutes = (elapsedMs / 60000).toInt()
                        val elapsedSeconds = ((elapsedMs % 60000) / 1000).toInt()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = String.format("%d:%02d", elapsedMinutes, elapsedSeconds),
                                style = MaterialTheme.typography.labelSmall,
                                color = AppColors.TextSecondary,
                                fontSize = 10.sp
                            )
                            Text(
                                text = stringResource(R.string.time_format_total),
                                style = MaterialTheme.typography.labelSmall,
                                color = AppColors.TextHint,
                                fontSize = 10.sp
                            )
                        }
                    } else {
                        // Retry button
                        Button(
                            onClick = onRetry,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = stringResource(R.string.retry_search), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = AppColors.Border
                )
            }

            // ═══ ROUTE DETAILS ═══
            AddressesCard(
                activeBooking.pickupAddress.contactName,
                activeBooking.pickupAddress.contactPhone,
                activeBooking.pickupAddress.address,
                activeBooking.dropAddress.contactName,
                activeBooking.dropAddress.contactPhone,
                activeBooking.dropAddress.address
            )

            // ═══ VEHICLE & FARE ═══
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = AppColors.Border
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                color = AppColors.Primary.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = activeBooking.fareDetails.vehicleTypeIcon ?: "🚚",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Column {
                        Text(
                            text = activeBooking.fareDetails.vehicleTypeName ?: stringResource(R.string.vehicle_fallback),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = AppColors.TextPrimary
                        )
                        Text(
                            text = activeBooking.fareDetails.capacity ?: "",
                            style = MaterialTheme.typography.labelSmall,
                            color = AppColors.TextSecondary,
                            fontSize = 10.sp
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = stringResource(R.string.fare_format, activeBooking.fare),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.Primary
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Payments,
                            contentDescription = null,
                            tint = AppColors.TextSecondary,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = stringResource(R.string.cash),
                            style = MaterialTheme.typography.labelSmall,
                            color = AppColors.TextSecondary,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            // ═══ TAP TO VIEW ═══
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppColors.Background)
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.tap_to_view_details),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = AppColors.Primary,
                        fontSize = 11.sp
                    )
                    Icon(
                        imageVector = Icons.Default.TouchApp,
                        contentDescription = null,
                        tint = AppColors.Primary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

/**
 * Pickup Location Card
 */
@Composable
private fun PickupLocationCard(
    location: String,
    onClick: () -> Unit,
    isDisabled: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = if (isDisabled) AppColors.Border.copy(alpha = 0.3f)
                        else AppColors.Pickup.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = stringResource(R.string.pickup_content_description),
                    tint = if (isDisabled) AppColors.TextHint else AppColors.Pickup,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.pick_up_from),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isDisabled) AppColors.TextHint else AppColors.TextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = location,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isDisabled) AppColors.TextHint else AppColors.TextPrimary,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = if (isDisabled) Icons.Default.Block else Icons.Default.Edit,
                contentDescription = stringResource(R.string.change),
                tint = if (isDisabled) AppColors.TextHint else AppColors.Primary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * Vehicle Types Grid
 */
@Composable
private fun VehicleTypesGrid(
    vehicleTypes: List<VehicleTypeResponse>,
    onVehicleSelected: (VehicleTypeResponse) -> Unit,
    isDisabled: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        vehicleTypes.chunked(3).forEach { rowVehicles ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowVehicles.forEach { vehicle ->
                    VehicleCard(
                        vehicle = vehicle,
                        onClick = onVehicleSelected,
                        modifier = Modifier.weight(1f),
                        isDisabled = isDisabled
                    )
                }
                repeat(3 - rowVehicles.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

/**
 * Vehicle Card with Emoji Icon
 */
@Composable
private fun VehicleCard(
    vehicle: VehicleTypeResponse,
    onClick: (VehicleTypeResponse) -> Unit,
    modifier: Modifier = Modifier,
    isDisabled: Boolean = false
) {
    Card(
        modifier = modifier
            .aspectRatio(0.9f)
            .clickable { onClick(vehicle) },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDisabled) 0.dp else 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (isDisabled) AppColors.Border.copy(alpha = 0.2f)
                        else AppColors.Primary.copy(alpha = 0.08f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = vehicle.icon,
                    style = MaterialTheme.typography.displayMedium,
                    modifier = Modifier.padding(4.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = vehicle.name,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = if (isDisabled) AppColors.TextHint else AppColors.TextPrimary,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CurrencyRupee,
                    contentDescription = null,
                    tint = if (isDisabled) AppColors.TextHint else AppColors.Primary,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = "${vehicle.basePrice}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isDisabled) AppColors.TextHint else AppColors.Primary
                )
                Text(
                    text = stringResource(R.string.onwards),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isDisabled) AppColors.TextHint else AppColors.TextSecondary
                )
            }
        }
    }
}

/**
 * Announcements Section
 */
@Composable
private fun AnnouncementsSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.announcements),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary
            )
            TextButton(onClick = { /* TODO */ }) {
                Text(
                    text = stringResource(R.string.view_all),
                    color = AppColors.Primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = WarningAmberBg,
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = WarningAmber,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.new_service),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.Primary
                    )
                    Text(
                        text = stringResource(R.string.new_service_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.TextPrimary
                    )
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = stringResource(R.string.view_label),
                    tint = AppColors.TextHint
                )
            }
        }
    }
}

/**
 * Marketing Text
 */
@Composable
private fun MarketingText() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.marketing_line1),
            style = MaterialTheme.typography.displaySmall,
            color = AppColors.Primary.copy(alpha = 0.15f),
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(R.string.marketing_line2),
            style = MaterialTheme.typography.displaySmall,
            color = AppColors.Primary.copy(alpha = 0.15f),
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )
    }
}