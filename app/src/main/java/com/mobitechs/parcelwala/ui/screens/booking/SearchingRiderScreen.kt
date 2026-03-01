// ui/screens/booking/SearchingRiderScreen.kt
package com.mobitechs.parcelwala.ui.screens.booking

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
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
import com.google.maps.android.compose.*
import com.mobitechs.parcelwala.R
import com.mobitechs.parcelwala.data.manager.ActiveBookingManager
import com.mobitechs.parcelwala.data.model.realtime.BookingStatusType
import com.mobitechs.parcelwala.data.model.request.SavedAddress
import com.mobitechs.parcelwala.data.model.response.FareDetails
import com.mobitechs.parcelwala.data.model.response.formatRupee
import com.mobitechs.parcelwala.data.repository.RouteInfo
import com.mobitechs.parcelwala.ui.components.AddressesCard
import com.mobitechs.parcelwala.ui.components.SignalRDebugPanel
import com.mobitechs.parcelwala.ui.theme.AppColors
import com.mobitechs.parcelwala.ui.viewmodel.BookingViewModel
import com.mobitechs.parcelwala.ui.viewmodel.RiderTrackingViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchingRiderScreen(
    bookingId: String,
    pickupAddress: SavedAddress,
    dropAddress: SavedAddress,
    selectedFareDetails: FareDetails,
    fare: Double, // ✅ Int → Double
    onRiderFound: () -> Unit,
    onContactSupport: () -> Unit,
    onViewDetails: () -> Unit,
    onCancelBooking: (String) -> Unit = {},
    bookingViewModel: BookingViewModel = hiltViewModel(),
    riderTrackingViewModel: RiderTrackingViewModel = hiltViewModel()
) {
    var showCancelSheet by remember { mutableStateOf(false) }

    val routeInfo by bookingViewModel.routeInfo.collectAsState()
    val isRouteLoading by bookingViewModel.isRouteLoading.collectAsState()
    val activeBooking by bookingViewModel.activeBooking.collectAsState()
    val bookingUiState by bookingViewModel.uiState.collectAsState()
    val trackingUiState by riderTrackingViewModel.uiState.collectAsState()
    val currentStatus = trackingUiState.currentStatus


    LaunchedEffect(bookingId) {
        riderTrackingViewModel.connectToBooking(
            bookingId = bookingId,
            pickupLatitude = pickupAddress.latitude,
            pickupLongitude = pickupAddress.longitude
        )
    }

    val totalTimeMs = ActiveBookingManager.SEARCH_TIMEOUT_MS
    val fallbackStartTime = remember { System.currentTimeMillis() }
    val searchStartTime = activeBooking?.searchStartTime ?: fallbackStartTime
    val searchAttempt = activeBooking?.searchAttempts ?: 1
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
    val showRetryUi = isTimedOut || trackingUiState.isNoRiderAvailable

    LaunchedEffect(searchStartTime, isSearching) {
        if (!isSearching) return@LaunchedEffect
        val initialElapsed = System.currentTimeMillis() - searchStartTime
        remainingTimeMs = maxOf(0L, totalTimeMs - initialElapsed)
        isTimedOut = remainingTimeMs <= 0
        while (remainingTimeMs > 0 && isSearching) {
            delay(1000L)
            val newElapsed = System.currentTimeMillis() - searchStartTime
            remainingTimeMs = maxOf(0L, totalTimeMs - newElapsed)
        }
        if (remainingTimeMs <= 0) isTimedOut = true
    }

    val minutes = (remainingTimeMs / 60000).toInt()
    val seconds = ((remainingTimeMs % 60000) / 1000).toInt()
    val timeText = String.format("%02d:%02d", minutes, seconds)

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

    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.Expanded
        )
    )

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetContainerColor = Color.Transparent,
        sheetShadowElevation = 0.dp,
        sheetShape = RoundedCornerShape(0.dp),
        sheetDragHandle = null,
        sheetPeekHeight = 340.dp,
        sheetContent = {
            Column(Modifier.fillMaxWidth()) {
                Box(
                    Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    routeInfo?.let { route ->
                        Card(
                            modifier = Modifier.align(Alignment.CenterEnd),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
                            elevation = CardDefaults.cardElevation(6.dp)
                        ) {
                            Row(
                                Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.Route, null, tint = AppColors.Primary, modifier = Modifier.size(14.dp))
                                    Text(route.distanceText, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                                }
                                Box(Modifier.width(1.dp).height(14.dp).background(AppColors.DividerLight))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.Timer, null, tint = AppColors.Primary, modifier = Modifier.size(14.dp))
                                    Text(route.durationText, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                                }
                            }
                        }
                    }
                }

                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(
                            AppColors.Surface,
                            RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                        )
                ) {
                    Column(Modifier.fillMaxWidth()) {
                        Box(
                            Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                Modifier.width(40.dp).height(4.dp)
                                    .background(AppColors.DragHandle, RoundedCornerShape(2.dp))
                            )
                        }

                        SearchingSheetBody(
                            bookingId = bookingId,
                            pickupAddress = pickupAddress,
                            dropAddress = dropAddress,
                            selectedFareDetails = selectedFareDetails,
                            fare = fare,
                            routeInfo = routeInfo,
                            isSearching = isSearching,
                            isRiderAssigned = isRiderAssigned,
                            isTimedOut = showRetryUi,
                            remainingTimeMs = remainingTimeMs,
                            totalTimeMs = totalTimeMs,
                            timeText = timeText,
                            searchAttempt = searchAttempt,
                            onRetry = { riderTrackingViewModel.retrySearch() },
                            onContactSupport = onContactSupport,
                            onCancelBooking = { showCancelSheet = true }
                        )
                    }
                }
            }
        },
        content = { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                RouteMapView(
                    pickupAddress = pickupAddress,
                    dropAddress = dropAddress,
                    routeInfo = routeInfo,
                    isLoading = isRouteLoading,
                    modifier = Modifier.fillMaxSize()
                )

                SearchingStatusPill(
                    isSearching = isSearching,
                    isRiderAssigned = isRiderAssigned,
                    isTimedOut = showRetryUi,
                    timeText = timeText,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(top = 12.dp)
                )

                if (isRouteLoading) {
                    Surface(
                        modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 64.dp),
                        shape = RoundedCornerShape(20.dp),
                        color = AppColors.Surface,
                        shadowElevation = 4.dp
                    ) {
                        Row(
                            Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(Modifier.size(16.dp), color = AppColors.Primary, strokeWidth = 2.dp)
                            Text(stringResource(R.string.label_finding_route), style = MaterialTheme.typography.labelMedium, color = AppColors.TextSecondary)
                        }
                    }
                }
            }
        }
    )

    if (showCancelSheet) {
        CancellationReasonBottomSheet(
            onDismiss = { showCancelSheet = false },
            onConfirmCancel = { reason ->
                showCancelSheet = false
                onCancelBooking(reason)
            },
            isLoading = bookingUiState.isLoading
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// FLOATING STATUS PILL
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun SearchingStatusPill(
    isSearching: Boolean,
    isRiderAssigned: Boolean,
    isTimedOut: Boolean,
    timeText: String,
    modifier: Modifier = Modifier
) {
    val riderFoundLabel = stringResource(R.string.label_rider_found)
    val noDriversLabel = stringResource(R.string.label_no_drivers_available)
    val searchingLabel = stringResource(R.string.label_searching_drivers, timeText)

    val (label, containerColor, contentColor) = when {
        isRiderAssigned -> Triple(riderFoundLabel, AppColors.Pickup, Color.White)
        isTimedOut -> Triple(noDriversLabel, AppColors.PrimaryLight, AppColors.OrangeDark)
        else -> Triple(searchingLabel, AppColors.Surface, AppColors.TextPrimary)
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(6.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isSearching) {
                val infiniteTransition = rememberInfiniteTransition(label = "pill_pulse")
                val alpha by infiniteTransition.animateFloat(
                    initialValue = 1f, targetValue = 0.3f,
                    animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
                    label = "dot"
                )
                Box(Modifier.size(8.dp).background(AppColors.Primary.copy(alpha = alpha), CircleShape))
            } else {
                Icon(
                    if (isRiderAssigned) Icons.Default.CheckCircle else Icons.Default.Warning,
                    null,
                    tint = contentColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// SHEET BODY
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun SearchingSheetBody(
    bookingId: String,
    pickupAddress: SavedAddress,
    dropAddress: SavedAddress,
    selectedFareDetails: FareDetails,
    fare: Double, // ✅ Int → Double
    routeInfo: RouteInfo?,
    isSearching: Boolean,
    isRiderAssigned: Boolean,
    isTimedOut: Boolean,
    remainingTimeMs: Long,
    totalTimeMs: Long,
    timeText: String,
    searchAttempt: Int,
    onRetry: () -> Unit,
    onContactSupport: () -> Unit,
    onCancelBooking: () -> Unit
) {
    val pickupFallback = stringResource(R.string.label_pickup)
    val dropFallback = stringResource(R.string.label_drop)
    val vehicleFallback = stringResource(R.string.label_vehicle_fallback)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            SearchingAnimationCard(
                isSearching = isSearching,
                isRiderAssigned = isRiderAssigned,
                isTimedOut = isTimedOut,
                remainingTimeMs = remainingTimeMs,
                totalTimeMs = totalTimeMs,
                timeText = timeText,
                searchAttempt = searchAttempt,
                onRetry = onRetry
            )

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = AppColors.LightGray50),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(Modifier.size(10.dp).background(AppColors.Pickup, CircleShape))
                            Box(Modifier.width(2.dp).height(28.dp).background(AppColors.Border))
                        }
                        Column(Modifier.weight(1f)) {
                            Text(
                                pickupAddress.contactName ?: pickupFallback,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                pickupAddress.address,
                                style = MaterialTheme.typography.labelSmall,
                                color = AppColors.TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(Modifier.size(10.dp).background(AppColors.Drop, CircleShape))
                        Column(Modifier.weight(1f)) {
                            Text(
                                dropAddress.contactName ?: dropFallback,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                dropAddress.address,
                                style = MaterialTheme.typography.labelSmall,
                                color = AppColors.TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = AppColors.LightGray50),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            selectedFareDetails.vehicleTypeIcon ?: "🚚",
                            fontSize = 28.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            selectedFareDetails.vehicleTypeName ?: vehicleFallback,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            selectedFareDetails.capacity ?: "",
                            style = MaterialTheme.typography.labelSmall,
                            color = AppColors.TextSecondary
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = AppColors.LightGray50),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            formatRupee(fare), // ✅ was "₹$fare"
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.Primary
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Wallet, null, tint = AppColors.Pickup, modifier = Modifier.size(16.dp))
                            Text(stringResource(R.string.label_cash), style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        HorizontalDivider(color = AppColors.Border.copy(alpha = 0.3f))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onContactSupport,
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.Primary),
                border = BorderStroke(1.dp, AppColors.Primary)
            ) {
                Icon(Icons.Default.Headset, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.label_support), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }

            OutlinedButton(
                onClick = onCancelBooking,
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.Drop),
                border = BorderStroke(1.dp, AppColors.Drop)
            ) {
                Icon(Icons.Default.Close, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.label_cancel), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// SEARCHING ANIMATION CARD
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun SearchingAnimationCard(
    isSearching: Boolean,
    isRiderAssigned: Boolean,
    isTimedOut: Boolean,
    remainingTimeMs: Long,
    totalTimeMs: Long,
    timeText: String,
    searchAttempt: Int,
    onRetry: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "search_anim")

    val ring1Scale by infiniteTransition.animateFloat(
        initialValue = 0.6f, targetValue = 1.4f,
        animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "ring1"
    )
    val ring1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "ring1a"
    )
    val ring2Scale by infiniteTransition.animateFloat(
        initialValue = 0.6f, targetValue = 1.4f,
        animationSpec = infiniteRepeatable(tween(2000, delayMillis = 600, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "ring2"
    )
    val ring2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(2000, delayMillis = 600, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "ring2a"
    )

    val bgColor = when {
        isRiderAssigned -> AppColors.GreenLight
        isTimedOut -> AppColors.PrimaryLight
        else -> AppColors.LightGray50
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = Modifier.size(100.dp), contentAlignment = Alignment.Center) {
                when {
                    isRiderAssigned -> {
                        Box(Modifier.size(80.dp).background(AppColors.Pickup.copy(0.15f), CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.CheckCircle, null, tint = AppColors.Pickup, modifier = Modifier.size(44.dp))
                        }
                    }
                    isTimedOut -> {
                        Box(Modifier.size(80.dp).background(AppColors.Warning.copy(0.15f), CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.SearchOff, null, tint = AppColors.OrangeDark, modifier = Modifier.size(40.dp))
                        }
                    }
                    else -> {
                        Box(Modifier.size(100.dp).scale(ring1Scale).alpha(ring1Alpha).background(AppColors.Primary.copy(alpha = 0.3f), CircleShape))
                        Box(Modifier.size(100.dp).scale(ring2Scale).alpha(ring2Alpha).background(AppColors.Primary.copy(alpha = 0.2f), CircleShape))
                        Box(Modifier.size(56.dp).background(AppColors.Primary, CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.LocalShipping, null, tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = when {
                    isRiderAssigned -> stringResource(R.string.label_rider_found_title)
                    isTimedOut -> stringResource(R.string.label_no_riders_available)
                    else -> stringResource(R.string.label_searching_for_drivers)
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = when {
                    isRiderAssigned -> AppColors.Pickup
                    isTimedOut -> AppColors.OrangeDark
                    else -> AppColors.TextPrimary
                }
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = when {
                    isRiderAssigned -> stringResource(R.string.label_connecting_driver)
                    isTimedOut -> stringResource(R.string.label_all_drivers_busy)
                    else -> stringResource(R.string.label_checking_drivers, searchAttempt)
                },
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            when {
                isRiderAssigned -> {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = AppColors.Pickup,
                        trackColor = AppColors.Pickup.copy(alpha = 0.2f)
                    )
                }
                isTimedOut -> {
                    Button(
                        onClick = onRetry,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary)
                    ) {
                        Icon(Icons.Default.Refresh, null, Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.label_try_again), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(stringResource(R.string.label_attempt_completed, searchAttempt), style = MaterialTheme.typography.labelSmall, color = AppColors.TextHint, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
                else -> {
                    val progress = ((totalTimeMs - remainingTimeMs).toFloat() / totalTimeMs).coerceIn(0f, 1f)
                    val progressColor = when {
                        progress > 0.66f -> AppColors.Drop
                        progress > 0.33f -> AppColors.Warning
                        else -> AppColors.Primary
                    }

                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Timer, null, tint = progressColor, modifier = Modifier.size(18.dp))
                            Text(timeText, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = progressColor)
                        }
                        Text(stringResource(R.string.label_remaining), style = MaterialTheme.typography.labelSmall, color = AppColors.TextHint)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = progressColor,
                        trackColor = AppColors.Border,
                        strokeCap = StrokeCap.Round
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// FULL-SCREEN MAP
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun RouteMapView(
    pickupAddress: SavedAddress,
    dropAddress: SavedAddress,
    routeInfo: RouteInfo?,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    val pickupLabel = stringResource(R.string.label_pickup)
    val dropLabel = stringResource(R.string.label_drop)
    val pickupLatLng = LatLng(pickupAddress.latitude, pickupAddress.longitude)
    val dropLatLng = LatLng(dropAddress.latitude, dropAddress.longitude)
    val cameraPositionState = rememberCameraPositionState()

    LaunchedEffect(routeInfo, pickupLatLng, dropLatLng) {
        try {
            val boundsBuilder = LatLngBounds.builder()
                .include(pickupLatLng)
                .include(dropLatLng)
            routeInfo?.polylinePoints?.forEach { boundsBuilder.include(it) }
            cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 100), 800)
        } catch (e: Exception) {
            val center = LatLng(
                (pickupLatLng.latitude + dropLatLng.latitude) / 2,
                (pickupLatLng.longitude + dropLatLng.longitude) / 2
            )
            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(center, 13f))
        }
    }

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        properties = MapProperties(mapType = MapType.NORMAL),
        uiSettings = MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = false, mapToolbarEnabled = false, compassEnabled = false),
        contentPadding = PaddingValues(bottom = 280.dp, top = 60.dp)
    ) {
        Marker(state = MarkerState(position = pickupLatLng), title = pickupLabel, icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
        Marker(state = MarkerState(position = dropLatLng), title = dropLabel, icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
        routeInfo?.let { route ->
            Polyline(points = route.polylinePoints, color = AppColors.RouteShadow, width = 14f, jointType = JointType.ROUND, startCap = RoundCap(), endCap = RoundCap())
            Polyline(points = route.polylinePoints, color = AppColors.Primary, width = 10f, jointType = JointType.ROUND, startCap = RoundCap(), endCap = RoundCap(), zIndex = 1f)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// CANCELLATION BOTTOM SHEET
// ═══════════════════════════════════════════════════════════════════════════════

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

    val reasonDriverDelayed = stringResource(R.string.label_reason_driver_delayed)
    val reasonChangePlans = stringResource(R.string.label_reason_change_plans)
    val reasonWrongAddress = stringResource(R.string.label_reason_wrong_address)
    val reasonPriceHigh = stringResource(R.string.label_reason_price_high)
    val reasonBookingMistake = stringResource(R.string.label_reason_booking_mistake)
    val reasonOther = stringResource(R.string.label_reason_other)
    val cancelledFallback = stringResource(R.string.label_cancelled)
    val otherFallback = stringResource(R.string.label_other_reason)

    val reasons = listOf(
        CancellationReason("driver_delayed", reasonDriverDelayed, Icons.Default.Timer),
        CancellationReason("change_plans", reasonChangePlans, Icons.Default.EventBusy),
        CancellationReason("wrong_address", reasonWrongAddress, Icons.Default.LocationOff),
        CancellationReason("price_high", reasonPriceHigh, Icons.Default.MoneyOff),
        CancellationReason("booking_mistake", reasonBookingMistake, Icons.Default.ErrorOutline),
        CancellationReason("other", reasonOther, Icons.Default.MoreHoriz)
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = AppColors.Surface,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = {
            Box(Modifier.padding(vertical = 12.dp).width(40.dp).height(4.dp).background(AppColors.Border, RoundedCornerShape(2.dp)))
        }
    ) {
        Column(Modifier.fillMaxHeight(0.9f).fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(stringResource(R.string.title_cancel_trip), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.label_select_reason), style = MaterialTheme.typography.bodyMedium, color = AppColors.TextSecondary)
                }
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, stringResource(R.string.content_desc_close), tint = AppColors.TextSecondary) }
            }

            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
                reasons.forEach { reason ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).clickable {
                            selectedReason = reason.id
                            if (reason.id != "other") { otherReason = ""; keyboardController?.hide(); focusManager.clearFocus() }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
                        border = BorderStroke(if (selectedReason == reason.id) 2.dp else 1.dp, if (selectedReason == reason.id) AppColors.Primary else AppColors.Border)
                    ) {
                        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(reason.icon, null, tint = if (selectedReason == reason.id) AppColors.Primary else AppColors.TextSecondary, modifier = Modifier.size(24.dp))
                            Text(reason.title, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, fontWeight = if (selectedReason == reason.id) FontWeight.Bold else FontWeight.Normal, color = if (selectedReason == reason.id) AppColors.Primary else AppColors.TextPrimary)
                            RadioButton(selected = selectedReason == reason.id, onClick = { selectedReason = reason.id }, colors = RadioButtonDefaults.colors(selectedColor = AppColors.Primary))
                        }
                    }
                }

                if (selectedReason == "other") {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = otherReason, onValueChange = { otherReason = it },
                        modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.label_please_specify)) },
                        placeholder = { Text(stringResource(R.string.hint_enter_reason)) }, minLines = 3, maxLines = 5,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { keyboardController?.hide(); focusManager.clearFocus() }),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AppColors.Primary, focusedLabelColor = AppColors.Primary)
                    )
                }

                Spacer(Modifier.height(16.dp))

                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = AppColors.AmberWarnBg)) {
                    Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                        Icon(Icons.Default.Warning, null, tint = AppColors.Warning, modifier = Modifier.size(20.dp))
                        Column {
                            Text(stringResource(R.string.label_cancellation_policy), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(stringResource(R.string.label_cancellation_warning), style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
            }

            Surface(color = AppColors.Surface, shadowElevation = 8.dp) {
                Row(Modifier.fillMaxWidth().padding(20.dp).navigationBarsPadding(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, AppColors.Primary), colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.Primary)) { Text(stringResource(R.string.label_keep_trip), fontWeight = FontWeight.Bold) }
                    Button(onClick = { if (selectedReason != null && (selectedReason != "other" || otherReason.isNotBlank())) showConfirmDialog = true }, modifier = Modifier.weight(1f).height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = AppColors.Drop, disabledContainerColor = AppColors.Drop.copy(0.3f)), shape = RoundedCornerShape(14.dp), enabled = selectedReason != null && (selectedReason != "other" || otherReason.isNotBlank()) && !isLoading) {
                        if (isLoading) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp) else Text(stringResource(R.string.label_cancel_trip), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            icon = { Icon(Icons.Default.ErrorOutline, null, tint = AppColors.Drop, modifier = Modifier.size(32.dp)) },
            title = { Text(stringResource(R.string.title_confirm_cancellation), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.label_confirm_cancel_message), color = AppColors.TextSecondary, textAlign = TextAlign.Center) },
            confirmButton = {
                Button(onClick = { val reason = if (selectedReason == "other") otherReason.ifBlank { otherFallback } else reasons.find { it.id == selectedReason }?.title ?: cancelledFallback; onConfirmCancel(reason); showConfirmDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = AppColors.Drop), enabled = !isLoading) {
                    if (isLoading) CircularProgressIndicator(Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp) else Text(stringResource(R.string.label_yes_cancel))
                }
            },
            dismissButton = { TextButton(onClick = { showConfirmDialog = false }) { Text(stringResource(R.string.label_no_keep_it), color = AppColors.Primary) } },
            containerColor = AppColors.Surface
        )
    }
}

private data class CancellationReason(val id: String, val title: String, val icon: ImageVector)