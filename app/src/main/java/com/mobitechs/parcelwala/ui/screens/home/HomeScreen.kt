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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mobitechs.parcelwala.data.manager.ActiveBooking
import com.mobitechs.parcelwala.data.manager.ActiveBookingManager
import com.mobitechs.parcelwala.data.manager.BookingStatus
import com.mobitechs.parcelwala.data.model.response.VehicleTypeResponse
import com.mobitechs.parcelwala.ui.components.*
import com.mobitechs.parcelwala.ui.theme.AppColors
import com.mobitechs.parcelwala.ui.viewmodel.HomeViewModel
import kotlinx.coroutines.delay

/**
 * Home Screen
 * Main landing screen showing vehicle types, pickup location, and active booking
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

    Scaffold(
        containerColor = AppColors.Background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Parcel Wala",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.TextPrimary
                        )
                        Text(
                            text = "Your delivery partner",
                            style = MaterialTheme.typography.labelSmall,
                            color = AppColors.TextSecondary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO: Notifications */ }) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = AppColors.Primary
                        )
                    }
                    IconButton(onClick = { /* TODO: Profile */ }) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Profile",
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
                    message = "Loading vehicles...",
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
                        visible = activeBooking != null,
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

                    // Pickup Location
                    PickupLocationCard(
                        location = uiState.pickupLocation,
                        onClick = onNavigateToLocationSearch
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Section Header
                    Text(
                        text = "Select Vehicle Type",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Vehicles Grid
                    if (uiState.vehicleTypes.isNotEmpty()) {
                        VehicleTypesGrid(
                            vehicleTypes = uiState.vehicleTypes,
                            onVehicleSelected = { onNavigateToLocationSearch() }
                        )
                    } else {
                        EmptyState(
                            icon = Icons.Default.DirectionsCar,
                            title = "No vehicles available",
                            subtitle = "Please try again later",
                            actionText = "Retry",
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
                    title = { Text("Error") },
                    text = { Text(error) },
                    confirmButton = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("OK", color = AppColors.Primary)
                        }
                    },
                    containerColor = Color.White
                )
            }
        }
    }
}

/**
 * Active Booking Card - Professional Ola/Uber Style
 * Shows complete booking details with pickup/drop addresses
 * Includes 3-minute countdown timer with progress bar and retry option
 */
@Composable
private fun ActiveBookingCard(
    activeBooking: ActiveBooking,
    onClick: () -> Unit,
    onRetry: () -> Unit
) {
    // ═══════════════════════════════════════════════════════════════
    // COUNTDOWN TIMER STATE (3 minutes = 180 seconds)
    // ═══════════════════════════════════════════════════════════════
    val totalTimeMs = ActiveBookingManager.SEARCH_TIMEOUT_MS

    // Stable fallback time - only calculated once
    val fallbackStartTime = remember { System.currentTimeMillis() }

    // Get searchStartTime from activeBooking, with stable fallback
    val searchStartTime = activeBooking.searchStartTime

    var remainingTimeMs by remember(searchStartTime) {
        val elapsed = System.currentTimeMillis() - searchStartTime
        mutableStateOf(maxOf(0L, totalTimeMs - elapsed))
    }
    var isTimedOut by remember(searchStartTime) {
        val elapsed = System.currentTimeMillis() - searchStartTime
        mutableStateOf(elapsed >= totalTimeMs)
    }

    // Only run countdown for SEARCHING status
    val isSearching = activeBooking.status == BookingStatus.SEARCHING

    // Countdown timer effect - synced with searchStartTime
    LaunchedEffect(searchStartTime, isSearching) {
        if (!isSearching) return@LaunchedEffect

        // Calculate initial remaining time based on actual elapsed time
        val initialElapsed = System.currentTimeMillis() - searchStartTime
        remainingTimeMs = maxOf(0L, totalTimeMs - initialElapsed)
        isTimedOut = remainingTimeMs <= 0

        // Continue countdown only if not already timed out
        while (remainingTimeMs > 0) {
            delay(1000L)
            val newElapsed = System.currentTimeMillis() - searchStartTime
            remainingTimeMs = maxOf(0L, totalTimeMs - newElapsed)
        }

        isTimedOut = true
    }

    // Format remaining time as MM:SS
    val minutes = (remainingTimeMs / 60000).toInt()
    val seconds = ((remainingTimeMs % 60000) / 1000).toInt()
    val timeText = String.format("%02d:%02d", minutes, seconds)

    // Pulse animation for the searching indicator
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = if (isTimedOut && isSearching)
                    Color(0xFFFF9800).copy(alpha = 0.3f)
                else
                    AppColors.Primary.copy(alpha = 0.3f)
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // ═══════════════════════════════════════════════════════════════
            // TOP SECTION - Status Header with Gradient
            // ═══════════════════════════════════════════════════════════════
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = if (isTimedOut && isSearching) {
                                listOf(
                                    Color(0xFFFF9800),
                                    Color(0xFFFF9800).copy(alpha = 0.85f)
                                )
                            } else {
                                listOf(
                                    AppColors.Primary,
                                    AppColors.Primary.copy(alpha = 0.85f)
                                )
                            }
                        )
                    )
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Animated Status Indicator
                    Box(
                        modifier = Modifier.size(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!isTimedOut || !isSearching) {
                            // Searching animation
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .scale(pulseScale)
                                    .background(
                                        color = Color.White.copy(alpha = 0.15f),
                                        shape = CircleShape
                                    )
                            )
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        color = Color.White.copy(alpha = 0.2f),
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
                                    imageVector = Icons.Default.LocalShipping,
                                    contentDescription = null,
                                    tint = if (isTimedOut) Color(0xFFFF9800) else AppColors.Primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        } else {
                            // Timeout warning icon
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SearchOff,
                                    contentDescription = null,
                                    tint = Color(0xFFFF9800),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    // Status Info
                    Column(modifier = Modifier.weight(1f)) {
                        // Status Text with animated dot
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (!isTimedOut && isSearching) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .scale(pulseScale)
                                        .background(
                                            color = Color.White.copy(alpha = pulseAlpha),
                                            shape = CircleShape
                                        )
                                )
                            }
                            Text(
                                text = when {
                                    isTimedOut && isSearching -> "No riders available"
                                    activeBooking.status == BookingStatus.SEARCHING -> "Finding your rider..."
                                    activeBooking.status == BookingStatus.RIDER_ASSIGNED -> "Rider assigned!"
                                    activeBooking.status == BookingStatus.RIDER_EN_ROUTE -> "Rider on the way"
                                    activeBooking.status == BookingStatus.PICKED_UP -> "Goods picked up"
                                    activeBooking.status == BookingStatus.IN_TRANSIT -> "On the way to drop"
                                    else -> "Booking in progress"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Trip ID & Attempt count
                        Text(
                            text = if (isSearching) {
                                "Trip #${activeBooking.bookingId} • Attempt ${activeBooking.searchAttempts}"
                            } else {
                                "Trip #${activeBooking.bookingId}"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }

                    // Timer Display (only for searching status)
                    if (isSearching) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = timeText,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = if (isTimedOut) "Timed out" else "remaining",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    } else {
                        // Arrow for non-searching states
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    color = Color.White.copy(alpha = 0.2f),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = "View Details",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // ═══════════════════════════════════════════════════════════════
            // PROGRESS BAR SECTION (only for searching status)
            // ═══════════════════════════════════════════════════════════════
            if (isSearching) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    if (!isTimedOut) {
                        // Calculate elapsed time
                        val elapsedMs = (totalTimeMs - remainingTimeMs).coerceIn(0L, totalTimeMs)
                        val elapsedMinutes = (elapsedMs / 60000).toInt()
                        val elapsedSeconds = ((elapsedMs % 60000) / 1000).toInt()
                        val elapsedTimeText = String.format("%d:%02d", elapsedMinutes, elapsedSeconds)

                        // Progress fills from left to right (0 = empty, 1 = full)
                        val elapsedProgress = (elapsedMs.toFloat() / totalTimeMs.toFloat()).coerceIn(0f, 1f)

                        // Progress bar
                        LinearProgressIndicator(
                            progress = { elapsedProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = when {
                                elapsedProgress > 0.66f -> AppColors.Drop  // Last minute - red
                                elapsedProgress > 0.33f -> Color(0xFFFF9800)  // Middle - orange
                                else -> AppColors.Primary  // First minute - Primary
                            },
                            trackColor = AppColors.Border,
                            strokeCap = StrokeCap.Round
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Progress labels - elapsed time on left, total time on right
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = elapsedTimeText,  // Shows elapsed time (0:00 → 3:00)
                                style = MaterialTheme.typography.labelSmall,
                                color = AppColors.TextSecondary
                            )
                            Text(
                                text = "3:00",
                                style = MaterialTheme.typography.labelSmall,
                                color = AppColors.TextHint
                            )
                        }
                    } else {
                        // ═══════════════════════════════════════════════════════════════
                        // RETRY BUTTON (when timed out)
                        // ═══════════════════════════════════════════════════════════════
                        Button(
                            onClick = onRetry,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            shape = RoundedCornerShape(12.dp),
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
                            Text(
                                text = "Retry Search",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = AppColors.Border
                )
            }

            // ═══════════════════════════════════════════════════════════════
            // MIDDLE SECTION - Route Details
            // ═══════════════════════════════════════════════════════════════
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
                        Text(
                            text = activeBooking.pickupAddress.contactName ?: "Sender",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = AppColors.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = activeBooking.pickupAddress.address,
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
                        Text(
                            text = activeBooking.dropAddress.contactName ?: "Receiver",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = AppColors.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = activeBooking.dropAddress.address,
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.TextSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // ═══════════════════════════════════════════════════════════════
            // BOTTOM SECTION - Vehicle & Fare Info
            // ═══════════════════════════════════════════════════════════════
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = AppColors.Border
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Vehicle Info
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Vehicle Icon
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                color = AppColors.Primary.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = activeBooking.fareDetails.vehicleTypeIcon ?: "🚚",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }

                    Column {
                        Text(
                            text = activeBooking.fareDetails.vehicleTypeName ?: "Vehicle",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = AppColors.TextPrimary
                        )
                        Text(
                            text = activeBooking.fareDetails.capacity ?: "",
                            style = MaterialTheme.typography.labelSmall,
                            color = AppColors.TextSecondary
                        )
                    }
                }

                // Fare Info
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "₹${activeBooking.fare}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.Primary
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Payments,
                            contentDescription = null,
                            tint = AppColors.TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Cash",
                            style = MaterialTheme.typography.labelSmall,
                            color = AppColors.TextSecondary
                        )
                    }
                }
            }

            // ═══════════════════════════════════════════════════════════════
            // TAP TO VIEW HINT
            // ═══════════════════════════════════════════════════════════════
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppColors.Background)
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Tap to view details",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = AppColors.Primary
                    )
                    Icon(
                        imageVector = Icons.Default.TouchApp,
                        contentDescription = null,
                        tint = AppColors.Primary,
                        modifier = Modifier.size(16.dp)
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
    onClick: () -> Unit
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
                        color = AppColors.Pickup.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Pickup",
                    tint = AppColors.Pickup,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Pick up from",
                    style = MaterialTheme.typography.labelLarge,
                    color = AppColors.TextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = location,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Change",
                tint = AppColors.Primary,
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
    onVehicleSelected: (VehicleTypeResponse) -> Unit
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
                        modifier = Modifier.weight(1f)
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
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .aspectRatio(0.9f)
            .clickable { onClick(vehicle) },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(AppColors.Primary.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = vehicle.icon,
                    style = MaterialTheme.typography.displayMedium,
                    modifier = Modifier.padding(8.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = vehicle.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary,
                textAlign = TextAlign.Center,
                maxLines = 2,
                minLines = 2
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CurrencyRupee,
                    contentDescription = null,
                    tint = AppColors.Primary,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = "${vehicle.basePrice}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.Primary
                )
                Text(
                    text = " onwards",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.TextSecondary
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
                text = "Announcements",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary
            )
            TextButton(onClick = { /* TODO */ }) {
                Text(
                    text = "View all",
                    color = AppColors.Primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
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
                            color = Color(0xFFFFF3E0),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = Color(0xFFFF9800),
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "New Service!",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.Primary
                    )
                    Text(
                        text = "Introducing Loading-Unloading Service",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.TextPrimary
                    )
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "View",
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
            text = "Delivery hai?",
            style = MaterialTheme.typography.displaySmall,
            color = AppColors.Primary.copy(alpha = 0.15f),
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Ho Jayega!",
            style = MaterialTheme.typography.displaySmall,
            color = AppColors.Primary.copy(alpha = 0.15f),
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )
    }
}