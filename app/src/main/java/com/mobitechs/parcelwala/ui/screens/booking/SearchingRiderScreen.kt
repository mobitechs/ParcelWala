// ui/screens/booking/SearchingRiderScreen.kt
package com.mobitechs.parcelwala.ui.screens.booking

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.RoundCap
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.mobitechs.parcelwala.data.manager.ActiveBookingManager
import com.mobitechs.parcelwala.data.model.realtime.BookingStatusType
import com.mobitechs.parcelwala.data.model.request.SavedAddress
import com.mobitechs.parcelwala.data.model.response.FareDetails
import com.mobitechs.parcelwala.data.repository.RouteInfo
import com.mobitechs.parcelwala.ui.components.AddressesCard
import com.mobitechs.parcelwala.ui.components.InfoCard
import com.mobitechs.parcelwala.ui.components.SignalRDebugPanel
import com.mobitechs.parcelwala.ui.theme.AppColors
import com.mobitechs.parcelwala.ui.viewmodel.BookingViewModel
import com.mobitechs.parcelwala.ui.viewmodel.RiderTrackingViewModel
import kotlinx.coroutines.delay

/**
 * ════════════════════════════════════════════════════════════════════════════
 * SEARCHING RIDER SCREEN
 * ════════════════════════════════════════════════════════════════════════════
 *
 * Flow (Like Ola/Uber/Porter):
 * 1. User lands here after booking confirmation
 * 2. Timer starts counting down (3 minutes)
 * 3. RiderTrackingViewModel connects to real-time service
 * 4. When RIDER_ASSIGNED: NavGraph handles navigation to RiderFoundScreen
 * 5. When NO_RIDER/TIMEOUT: Show retry UI
 * 6. Cancel: Calls API via BookingViewModel, then navigates home
 *
 * KEY: Timer STOPS when rider is assigned (handled by status check)
 * ════════════════════════════════════════════════════════════════════════════
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchingRiderScreen(
    bookingId: String,
    pickupAddress: SavedAddress,
    dropAddress: SavedAddress,
    selectedFareDetails: FareDetails,
    fare: Int,
    onRiderFound: () -> Unit,
    onContactSupport: () -> Unit,
    onViewDetails: () -> Unit,
    onCancelBooking: (String) -> Unit = {},  // ✅ Added callback for cancel with reason
    bookingViewModel: BookingViewModel = hiltViewModel(),
    riderTrackingViewModel: RiderTrackingViewModel = hiltViewModel()
) {
    var showCancelSheet by remember { mutableStateOf(false) }

    // Observe from BookingViewModel
    val routeInfo by bookingViewModel.routeInfo.collectAsState()
    val isRouteLoading by bookingViewModel.isRouteLoading.collectAsState()
    val activeBooking by bookingViewModel.activeBooking.collectAsState()
    val bookingUiState by bookingViewModel.uiState.collectAsState()

    // Observe from RiderTrackingViewModel
    val trackingUiState by riderTrackingViewModel.uiState.collectAsState()
    val currentStatus = trackingUiState.currentStatus

    // ═══════════════════════════════════════════════════════════════════════
    // CONNECT TO REAL-TIME SERVICE ON SCREEN LOAD
    // ═══════════════════════════════════════════════════════════════════════
    LaunchedEffect(bookingId) {
        riderTrackingViewModel.connectToBooking(
            bookingId = bookingId,
            pickupLatitude = pickupAddress.latitude,
            pickupLongitude = pickupAddress.longitude
        )
    }

    // ═══════════════════════════════════════════════════════════════════════
    // COUNTDOWN TIMER - STOPS WHEN RIDER IS ASSIGNED
    // ═══════════════════════════════════════════════════════════════════════
    val totalTimeMs = ActiveBookingManager.SEARCH_TIMEOUT_MS
    val fallbackStartTime = remember { System.currentTimeMillis() }
    val searchStartTime = activeBooking?.searchStartTime ?: fallbackStartTime
    val searchAttempt = activeBooking?.searchAttempts ?: 1

    // ✅ KEY: Check if we should be searching (not assigned yet)
    val isSearching = currentStatus == BookingStatusType.SEARCHING
    val isRiderAssigned = currentStatus == BookingStatusType.RIDER_ASSIGNED

    var remainingTimeMs by remember(searchStartTime) {
        val elapsed = System.currentTimeMillis() - searchStartTime
        mutableStateOf(maxOf(0L, totalTimeMs - elapsed))
    }

    var isTimedOut by remember(searchStartTime) {
        val elapsed = System.currentTimeMillis() - searchStartTime
        mutableStateOf(elapsed >= totalTimeMs)
    }

    // Show retry UI when timed out OR no rider available
    val showRetryUi = isTimedOut || trackingUiState.isNoRiderAvailable

    // ✅ Timer only runs when SEARCHING status
    LaunchedEffect(searchStartTime, isSearching) {
        if (!isSearching) {
            // Not searching anymore (rider assigned or other status)
            return@LaunchedEffect
        }

        val initialElapsed = System.currentTimeMillis() - searchStartTime
        remainingTimeMs = maxOf(0L, totalTimeMs - initialElapsed)
        isTimedOut = remainingTimeMs <= 0

        while (remainingTimeMs > 0 && isSearching) {
            delay(1000L)
            val newElapsed = System.currentTimeMillis() - searchStartTime
            remainingTimeMs = maxOf(0L, totalTimeMs - newElapsed)
        }

        if (remainingTimeMs <= 0) {
            isTimedOut = true
        }
    }

    // Format time
    val minutes = (remainingTimeMs / 60000).toInt()
    val seconds = ((remainingTimeMs % 60000) / 1000).toInt()
    val timeText = String.format("%02d:%02d", minutes, seconds)

    // Calculate route
    LaunchedEffect(pickupAddress, dropAddress) {
        if (pickupAddress.latitude != 0.0 && dropAddress.latitude != 0.0) {
            bookingViewModel.calculateRoute(
                pickupLat = pickupAddress.latitude,
                pickupLng = pickupAddress.longitude,
                dropLat = dropAddress.latitude,
                dropLng = dropAddress.longitude
            )
        }
    }

    // Pulse animation
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            when {
                                isRiderAssigned -> {
                                    // ✅ Show rider assigned status
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = AppColors.Pickup,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Rider assigned! Navigating...",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AppColors.Pickup
                                    )
                                }

                                showRetryUi -> {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = Color(0xFFFF9800),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "No drivers found",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFFF9800)
                                    )
                                }

                                else -> {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .scale(pulseScale)
                                            .background(AppColors.Primary, CircleShape)
                                    )
                                    Text(
                                        text = trackingUiState.statusMessage
                                            ?: "Finding your rider...",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AppColors.Primary
                                    )
                                }
                            }
                        }
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
            // MAP (45%)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.45f)
            ) {
                RouteMapView(
                    pickupAddress = pickupAddress,
                    dropAddress = dropAddress,
                    routeInfo = routeInfo,
                    isLoading = isRouteLoading
                )

                // Distance & Time Overlay
                routeInfo?.let { route ->
                    Card(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(24.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.Route,
                                    null,
                                    tint = AppColors.Primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    route.distanceText,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Box(Modifier
                                .width(1.dp)
                                .height(24.dp)
                                .background(AppColors.Border))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.Timer,
                                    null,
                                    tint = AppColors.Primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    route.durationText,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                if (isRouteLoading) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .background(Color.White.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(
                                Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = AppColors.Primary
                            )
                            Text(
                                "Loading route...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = AppColors.TextSecondary
                            )
                        }
                    }
                }
            }

            // DETAILS (55%)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // Status Card
                SearchingStatusCard(
                    isTimedOut = showRetryUi,
                    isRiderAssigned = isRiderAssigned,
                    remainingTimeMs = remainingTimeMs,
                    totalTimeMs = totalTimeMs,
                    timeText = timeText,
                    searchAttempt = searchAttempt,
                    pulseScale = pulseScale,
                    onRetry = { riderTrackingViewModel.retrySearch() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Journey Details
                JourneyDetailsCard(
                    pickupAddress = pickupAddress,
                    dropAddress = dropAddress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Vehicle & Payment
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    VehicleInfoCard(selectedFareDetails, Modifier.weight(1f))
                    PaymentCard("Cash", fare, Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onContactSupport,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.Primary),
                        border = BorderStroke(1.dp, AppColors.Primary)
                    ) {
                        Icon(Icons.Default.Headset, null, Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Support", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { showCancelSheet = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.Drop),
                        border = BorderStroke(1.dp, AppColors.Drop)
                    ) {
                        Icon(Icons.Default.Close, null, Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Cancel", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            SignalRDebugPanel(
                connectionState = riderTrackingViewModel.connectionState.collectAsState().value,
                bookingId = bookingId,
                isVisible = true  // Set to BuildConfig.DEBUG in production
            )
        }
    }

    // Cancel Sheet
    if (showCancelSheet) {
        CancellationReasonBottomSheet(
            onDismiss = { showCancelSheet = false },
            onConfirmCancel = { reason ->
                showCancelSheet = false
                onCancelBooking(reason)  // ✅ Call the callback that triggers API
            },
            isLoading = bookingUiState.isLoading
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// STATUS CARD
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun SearchingStatusCard(
    isTimedOut: Boolean,
    isRiderAssigned: Boolean,
    remainingTimeMs: Long,
    totalTimeMs: Long,
    timeText: String,
    searchAttempt: Int,
    pulseScale: Float,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isRiderAssigned -> Color(0xFFE8F5E9)  // Light green
                isTimedOut -> Color(0xFFFFF3E0)       // Light orange
                else -> Color.White
            }
        ),
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
                // Icon
                Box(modifier = Modifier.size(56.dp), contentAlignment = Alignment.Center) {
                    when {
                        isRiderAssigned -> {
                            // ✅ Rider Assigned - Green checkmark
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(AppColors.Pickup.copy(0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    null,
                                    tint = AppColors.Pickup,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        isTimedOut -> {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(Color(0xFFFF9800).copy(0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.SearchOff,
                                    null,
                                    tint = Color(0xFFFF9800),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        else -> {
                            Box(
                                Modifier
                                    .size(56.dp)
                                    .scale(pulseScale)
                                    .alpha(0.3f)
                                    .background(AppColors.Primary, CircleShape)
                            )
                            Box(
                                Modifier
                                    .size(40.dp)
                                    .background(AppColors.Primary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.LocalShipping,
                                    null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }

                // Text
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when {
                            isRiderAssigned -> "Rider Found! 🎉"
                            isTimedOut -> "No riders available"
                            else -> "Searching for drivers"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            isRiderAssigned -> AppColors.Pickup
                            isTimedOut -> Color(0xFFE65100)
                            else -> AppColors.TextPrimary
                        }
                    )
                    Text(
                        text = when {
                            isRiderAssigned -> "Navigating to rider details..."
                            isTimedOut -> "Try again or wait for availability"
                            else -> "Searching nearby... Attempt $searchAttempt"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextSecondary
                    )
                }

                // Timer (hide when rider assigned)
                if (!isRiderAssigned) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val progress =
                            ((totalTimeMs - remainingTimeMs).toFloat() / totalTimeMs).coerceIn(
                                0f,
                                1f
                            )
                        Text(
                            text = timeText,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                isTimedOut -> Color(0xFFE65100)
                                progress > 0.66f -> AppColors.Drop
                                progress > 0.33f -> Color(0xFFFF9800)
                                else -> AppColors.Primary
                            }
                        )
                        Text(
                            text = if (isTimedOut) "Timed out" else "remaining",
                            style = MaterialTheme.typography.labelSmall,
                            color = AppColors.TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            when {
                isRiderAssigned -> {
                    // ✅ Show navigation progress
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = AppColors.Pickup
                    )
                }

                isTimedOut -> {
                    Button(
                        onClick = onRetry,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary)
                    ) {
                        Icon(Icons.Default.Refresh, null, Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Retry Search", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Search attempt $searchAttempt completed",
                        style = MaterialTheme.typography.labelSmall,
                        color = AppColors.TextSecondary,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }

                else -> {
                    val elapsedMs = (totalTimeMs - remainingTimeMs).coerceIn(0L, totalTimeMs)
                    val elapsedProgress = (elapsedMs.toFloat() / totalTimeMs).coerceIn(0f, 1f)

                    LinearProgressIndicator(
                        progress = { elapsedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = when {
                            elapsedProgress > 0.66f -> AppColors.Drop
                            elapsedProgress > 0.33f -> Color(0xFFFF9800)
                            else -> AppColors.Primary
                        },
                        trackColor = AppColors.Border,
                        strokeCap = StrokeCap.Round
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        val elapsedMinutes = (elapsedMs / 60000).toInt()
                        val elapsedSeconds = ((elapsedMs % 60000) / 1000).toInt()
                        Text(
                            "${elapsedMinutes}:${String.format("%02d", elapsedSeconds)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = AppColors.TextSecondary
                        )
                        Text(
                            "3:00",
                            style = MaterialTheme.typography.labelSmall,
                            color = AppColors.TextHint
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// HELPER COMPOSABLES
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun RouteMapView(
    pickupAddress: SavedAddress,
    dropAddress: SavedAddress,
    routeInfo: RouteInfo?,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    val pickupLatLng = LatLng(pickupAddress.latitude, pickupAddress.longitude)
    val dropLatLng = LatLng(dropAddress.latitude, dropAddress.longitude)
    val cameraPositionState = rememberCameraPositionState()

    val boundsBuilder = remember(pickupLatLng, dropLatLng, routeInfo) {
        LatLngBounds.builder()
            .include(pickupLatLng)
            .include(dropLatLng)
            .apply { routeInfo?.polylinePoints?.forEach { include(it) } }
    }

    LaunchedEffect(routeInfo, pickupLatLng, dropLatLng) {
        try {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngBounds(
                    boundsBuilder.build(),
                    80
                ), 1000
            )
        } catch (e: Exception) {
            val center = LatLng(
                (pickupLatLng.latitude + dropLatLng.latitude) / 2,
                (pickupLatLng.longitude + dropLatLng.longitude) / 2
            )
            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(center, 13f))
        }
    }

    GoogleMap(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)),
        cameraPositionState = cameraPositionState,
        properties = MapProperties(mapType = MapType.NORMAL),
        uiSettings = MapUiSettings(
            zoomControlsEnabled = false,
            myLocationButtonEnabled = false,
            mapToolbarEnabled = false,
            compassEnabled = false
        )
    ) {
        Marker(
            MarkerState(pickupLatLng).toString(),
            title = "Pickup",
            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
        )
        Marker(
            MarkerState(dropLatLng).toString(),
            title = "Drop",
            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
        )
        routeInfo?.let {
            Polyline(
                it.polylinePoints,
                color = Color(0xFF1565C0),
                width = 14f,
                jointType = JointType.ROUND,
                startCap = RoundCap(),
                endCap = RoundCap()
            )
            Polyline(
                it.polylinePoints,
                color = Color(0xFF2196F3),
                width = 10f,
                jointType = JointType.ROUND,
                startCap = RoundCap(),
                endCap = RoundCap(),
                zIndex = 1f
            )
        }
    }
}

@Composable
private fun JourneyDetailsCard(
    pickupAddress: SavedAddress,
    dropAddress: SavedAddress,
    modifier: Modifier = Modifier
) {
    InfoCard(modifier = modifier) {


        AddressesCard(pickupAddress.contactName,pickupAddress.contactPhone,pickupAddress.address, dropAddress.contactName,dropAddress.contactPhone,dropAddress.address)
    }
}

@Composable
private fun VehicleInfoCard(fareDetails: FareDetails, modifier: Modifier = Modifier) {
    InfoCard(modifier = modifier) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                fareDetails.vehicleTypeIcon ?: "🚚",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(Modifier.height(4.dp))
            Text(
                fareDetails.vehicleTypeName ?: "Vehicle",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                fareDetails.capacity ?: "",
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.TextSecondary
            )
        }
    }
}

@Composable
private fun PaymentCard(method: String, amount: Int, modifier: Modifier = Modifier) {
    InfoCard(modifier = modifier) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "₹$amount",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = AppColors.Primary
            )
            Spacer(Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    Icons.Default.Wallet,
                    null,
                    tint = AppColors.Pickup,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    method,
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextSecondary
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// CANCELLATION BOTTOM SHEET
// ═══════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CancellationReasonBottomSheet(
    onDismiss: () -> Unit,
    onConfirmCancel: (String) -> Unit,
    isLoading: Boolean = false
) {
    var selectedReason by remember { mutableStateOf<String?>(null) }
    var otherReason by remember { mutableStateOf("") }
    var showConfirmDialog by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val reasons = listOf(
        CancellationReason("driver_delayed", "Driver is taking too long", Icons.Default.Timer),
        CancellationReason("change_plans", "Change of plans", Icons.Default.EventBusy),
        CancellationReason(
            "wrong_address",
            "Wrong pickup/drop location",
            Icons.Default.LocationOff
        ),
        CancellationReason("price_high", "Price is too high", Icons.Default.MoneyOff),
        CancellationReason("booking_mistake", "Booked by mistake", Icons.Default.ErrorOutline),
        CancellationReason("other", "Other reason", Icons.Default.MoreHoriz)
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = {
            Box(
                Modifier
                    .padding(vertical = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .background(AppColors.Border, RoundedCornerShape(2.dp))
            )
        }
    ) {
        Column(Modifier
            .fillMaxHeight(0.9f)
            .fillMaxWidth()) {
            // Header
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Cancel Trip?",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Please select a reason",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.TextSecondary
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Default.Close,
                        "Close",
                        tint = AppColors.TextSecondary
                    )
                }
            }

            // Reasons
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                reasons.forEach { reason ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .clickable {
                                selectedReason = reason.id
                                if (reason.id != "other") {
                                    otherReason = ""
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
                                }
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(
                            if (selectedReason == reason.id) 2.dp else 1.dp,
                            if (selectedReason == reason.id) AppColors.Primary else AppColors.Border
                        )
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                reason.icon,
                                null,
                                tint = if (selectedReason == reason.id) AppColors.Primary else AppColors.TextSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                reason.title,
                                Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (selectedReason == reason.id) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedReason == reason.id) AppColors.Primary else AppColors.TextPrimary
                            )
                            RadioButton(
                                selected = selectedReason == reason.id,
                                onClick = { selectedReason = reason.id },
                                colors = RadioButtonDefaults.colors(selectedColor = AppColors.Primary)
                            )
                        }
                    }
                }

                if (selectedReason == "other") {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = otherReason,
                        onValueChange = { otherReason = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Please specify") },
                        placeholder = { Text("Enter your reason...") },
                        minLines = 3,
                        maxLines = 5,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { keyboardController?.hide(); focusManager.clearFocus() }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AppColors.Primary,
                            focusedLabelColor = AppColors.Primary
                        )
                    )
                }

                Spacer(Modifier.height(16.dp))

                InfoCard {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            null,
                            tint = Color(0xFFFF9800),
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                "Cancellation Policy",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Frequent cancellations may result in temporary suspension.",
                                style = MaterialTheme.typography.bodySmall,
                                color = AppColors.TextSecondary
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
            }

            // Buttons
            Surface(color = Color.White, shadowElevation = 8.dp) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, AppColors.Primary),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.Primary)
                    ) { Text("Keep Trip", fontWeight = FontWeight.Bold) }

                    Button(
                        onClick = {
                            if (selectedReason != null && (selectedReason != "other" || otherReason.isNotBlank())) {
                                showConfirmDialog = true
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.Drop,
                            disabledContainerColor = AppColors.Drop.copy(0.3f)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        enabled = selectedReason != null && (selectedReason != "other" || otherReason.isNotBlank()) && !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Cancel Trip", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Confirm Dialog
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            icon = {
                Icon(
                    Icons.Default.ErrorOutline,
                    null,
                    tint = AppColors.Drop,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = { Text("Confirm Cancellation", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Are you sure you want to cancel this trip?",
                    color = AppColors.TextSecondary,
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val reason = if (selectedReason == "other") otherReason.ifBlank { "Other" }
                        else reasons.find { it.id == selectedReason }?.title ?: "Cancelled"
                        onConfirmCancel(reason)
                        showConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Drop),
                    enabled = !isLoading
                ) {
                    if (isLoading) CircularProgressIndicator(
                        Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    else Text("Yes, Cancel")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showConfirmDialog = false
                }) { Text("No, Keep It", color = AppColors.Primary) }
            },
            containerColor = Color.White
        )
    }
}

private data class CancellationReason(val id: String, val title: String, val icon: ImageVector)