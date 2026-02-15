// ui/screens/booking/RiderFoundScreen.kt
// ✅ UPDATED: Bottom sheet matches DriverActiveBookingScreen pattern
//    - Sheet starts EXPANDED
//    - Transparent container with floating ETA chip + white rounded box
//    - Custom drag handle inside the white box
package com.mobitechs.parcelwala.ui.screens.booking

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.Dash
import com.google.android.gms.maps.model.Gap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.*
import com.mobitechs.parcelwala.R
import com.mobitechs.parcelwala.data.model.realtime.BookingStatusType
import com.mobitechs.parcelwala.data.model.realtime.RiderInfo
import com.mobitechs.parcelwala.data.model.request.SavedAddress
import com.mobitechs.parcelwala.ui.theme.AppColors
import com.mobitechs.parcelwala.ui.viewmodel.RiderTrackingViewModel
import com.mobitechs.parcelwala.ui.viewmodel.WaitingTimerState
import com.mobitechs.parcelwala.ui.viewmodel.RatingUiState
import kotlinx.coroutines.delay

private const val TAG = "RiderFoundScreen"

// ═══════════════════════════════════════════════════════════════════════════════
// MAIN SCREEN — Full Map + BottomSheetScaffold (Expanded by default)
// ═══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RiderFoundScreen(
    bookingId: String,
    pickupAddress: SavedAddress,
    dropAddress: SavedAddress,
    fare: Int,
    onCancelBooking: (String) -> Unit,
    onContactSupport: () -> Unit,
    viewModel: RiderTrackingViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    val rider by viewModel.assignedRider.collectAsState()
    val riderLocation by viewModel.riderLocation.collectAsState()
    val otp by viewModel.bookingOtp.collectAsState()
    val etaMinutes by viewModel.etaMinutes.collectAsState()
    val distanceKm by viewModel.distanceKm.collectAsState()
    val driverToPickupRoute by viewModel.driverToPickupRoute.collectAsState()
    val pickupToDropRoute by viewModel.pickupToDropRoute.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val waitingState by viewModel.waitingState.collectAsState()
    val ratingState by viewModel.ratingState.collectAsState()

    val currentStatus = uiState.currentStatus

    val isPrePickup = currentStatus == BookingStatusType.RIDER_ASSIGNED ||
            currentStatus == BookingStatusType.RIDER_ENROUTE ||
            currentStatus == BookingStatusType.ARRIVED
    val isDriverArrived = currentStatus == BookingStatusType.ARRIVED
    val isPostPickup = currentStatus == BookingStatusType.PICKED_UP ||
            currentStatus == BookingStatusType.IN_TRANSIT ||
            currentStatus == BookingStatusType.ARRIVED_DELIVERY
    val isDelivered = currentStatus == BookingStatusType.DELIVERED

    // Driver LatLng: live location > initial assignment location
    val driverLatLng: LatLng? = remember(riderLocation, rider) {
        val liveLat = riderLocation?.latitude
        val liveLng = riderLocation?.longitude
        if (liveLat != null && liveLng != null && liveLat != 0.0 && liveLng != 0.0) {
            return@remember LatLng(liveLat, liveLng)
        }
        val assignedLat = rider?.currentLatitude
        val assignedLng = rider?.currentLongitude
        if (assignedLat != null && assignedLng != null && assignedLat != 0.0 && assignedLng != 0.0) {
            return@remember LatLng(assignedLat, assignedLng)
        }
        null
    }

    var showCancelDialog by remember { mutableStateOf(false) }

    // ── BottomSheetScaffold — EXPANDED, Transparent (matches DriverActiveBookingScreen) ──
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
                // 1. Floating ETA chip — above the white box, aligned end
                if (isPrePickup || isPostPickup) {
                    val hasEta = etaMinutes != null && etaMinutes!! > 0
                    val hasDistance = distanceKm != null && distanceKm!! > 0.0
                    if (hasEta || hasDistance) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            CompactEtaChip(
                                etaMinutes = etaMinutes,
                                distanceKm = distanceKm,
                                isPostPickup = isPostPickup,
                                formatDistance = { viewModel.formatDistance(it) },
                                modifier = Modifier.align(Alignment.CenterEnd)
                            )
                        }
                    }
                }

                // 2. White box — the actual sheet visual
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(
                            Color.White,
                            RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                        )
                ) {
                    Column(Modifier.fillMaxWidth()) {
                        // Drag handle
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp, bottom = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                Modifier
                                    .width(40.dp)
                                    .height(4.dp)
                                    .background(Color(0xFFE0E0E0), RoundedCornerShape(2.dp))
                            )
                        }

                        // Sheet body
                        SheetBody(
                            rider = rider,
                            otp = otp,
                            isPrePickup = isPrePickup,
                            isDriverArrived = isDriverArrived,
                            isPostPickup = isPostPickup,
                            isDelivered = isDelivered,
                            waitingState = waitingState,
                            pickupAddress = pickupAddress,
                            dropAddress = dropAddress,
                            fare = fare,
                            currentStatus = currentStatus,
                            onCallRider = {
                                rider?.riderPhone?.let { phone ->
                                    context.startActivity(
                                        Intent(Intent.ACTION_DIAL).apply {
                                            data = Uri.parse("tel:$phone")
                                        }
                                    )
                                }
                            },
                            onContactSupport = onContactSupport,
                            onCancelBooking = { showCancelDialog = true }
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
                // ── MAP ──
                LiveTrackingMap(
                    pickupLatLng = LatLng(pickupAddress.latitude, pickupAddress.longitude),
                    dropLatLng = LatLng(dropAddress.latitude, dropAddress.longitude),
                    driverLatLng = driverLatLng,
                    driverHeading = riderLocation?.heading,
                    driverToPickupRoute = driverToPickupRoute,
                    pickupToDropRoute = pickupToDropRoute,
                    isPrePickup = isPrePickup,
                    vehicleType = rider?.vehicleType,
                    modifier = Modifier.fillMaxSize()
                )

                // ── Floating status pill on map ──
                StatusPill(
                    status = currentStatus,
                    etaMinutes = etaMinutes,
                    distanceKm = distanceKm,
                    isDriverArrived = isDriverArrived,
                    formatDistance = { viewModel.formatDistance(it) },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(top = 12.dp)
                )
            }
        }
    )

    // ── Rating Dialog ──
    if (ratingState.showRatingDialog) {
        RatingBottomSheet(
            ratingState = ratingState,
            onSubmitRating = { rating, feedback ->
                viewModel.submitRating(ratingState.bookingId, rating, feedback)
            },
            onDismiss = { viewModel.onRatingCompleted() }
        )
    }

    // ── Cancel Dialog ──
    if (showCancelDialog) {
        CancelBookingDialog(
            onDismiss = { showCancelDialog = false },
            onConfirm = { reason ->
                showCancelDialog = false
                onCancelBooking(reason)
            }
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// COMPACT ETA CHIP — Floats above the white sheet (matches DriverActiveBookingScreen)
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun CompactEtaChip(
    etaMinutes: Int?,
    distanceKm: Double?,
    isPostPickup: Boolean,
    formatDistance: (Double?) -> String,
    modifier: Modifier = Modifier
) {
    val hasEta = etaMinutes != null && etaMinutes > 0
    val hasDistance = distanceKm != null && distanceKm > 0.0

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (hasEta) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.AccessTime, null,
                        tint = if (isPostPickup) Color(0xFF7C3AED) else Color(0xFF1A73E8),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        "~${etaMinutes} min",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
            if (hasEta && hasDistance) {
                Box(
                    Modifier
                        .width(1.dp)
                        .height(14.dp)
                        .background(Color(0xFFE5E7EB))
                )
            }
            if (hasDistance) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.Route, null,
                        tint = AppColors.Primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        formatDistance(distanceKm),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
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
private fun LiveTrackingMap(
    pickupLatLng: LatLng,
    dropLatLng: LatLng,
    driverLatLng: LatLng?,
    driverHeading: Double?,
    driverToPickupRoute: List<LatLng>,
    pickupToDropRoute: List<LatLng>,
    isPrePickup: Boolean,
    vehicleType: String?,
    modifier: Modifier = Modifier
) {
    val driverMarkerState = rememberMarkerState(
        position = driverLatLng ?: pickupLatLng
    )

    LaunchedEffect(driverLatLng) {
        if (driverLatLng != null) {
            driverMarkerState.position = driverLatLng
        }
    }

    val bikeMarkerIcon = remember {
        try {
            BitmapDescriptorFactory.fromResource(R.drawable.bike_marker)
        } catch (e: Exception) {
            BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
        }
    }

    val cameraPositionState = rememberCameraPositionState()

    LaunchedEffect(driverLatLng, isPrePickup) {
        try {
            val boundsBuilder = LatLngBounds.builder()
            if (isPrePickup) {
                boundsBuilder.include(pickupLatLng)
                if (driverLatLng != null) boundsBuilder.include(driverLatLng)
            } else {
                boundsBuilder.include(pickupLatLng)
                boundsBuilder.include(dropLatLng)
                if (driverLatLng != null) boundsBuilder.include(driverLatLng)
            }
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 120), 600
            )
        } catch (e: Exception) {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(pickupLatLng, 14f)
            )
        }
    }

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        properties = MapProperties(mapType = MapType.NORMAL),
        uiSettings = MapUiSettings(
            zoomControlsEnabled = false,
            myLocationButtonEnabled = false,
            mapToolbarEnabled = false,
            compassEnabled = false
        ),
        contentPadding = PaddingValues(bottom = 280.dp, top = 60.dp)
    ) {

        Marker(state = MarkerState(position = pickupLatLng), title = "Pickup", icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
        Marker(state = MarkerState(position = dropLatLng), title = "Drop", icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))

        // Driver marker
        if (driverLatLng != null) {
            Marker(
                state = driverMarkerState,
                title = "Driver",
                snippet = vehicleType ?: "Vehicle",
                icon = bikeMarkerIcon,
                rotation = driverHeading?.toFloat() ?: 0f,
                flat = true,
                anchor = androidx.compose.ui.geometry.Offset(0.5f, 0.5f),
                zIndex = 2f
            )
        }

        // Route polyline
        if (isPrePickup) {
            if (driverToPickupRoute.isNotEmpty()) {
                Polyline(
                    points = driverToPickupRoute,
                    color = AppColors.Primary,
                    width = 14f
                )
            } else if (driverLatLng != null) {
                Polyline(
                    points = listOf(driverLatLng, pickupLatLng),
                    color = AppColors.Primary.copy(alpha = 0.5f),
                    width = 8f,
                    pattern = listOf(Dash(20f), Gap(10f))
                )
            }
        } else {
            if (pickupToDropRoute.isNotEmpty()) {
                Polyline(
                    points = pickupToDropRoute,
                    color = AppColors.Primary,
                    width = 14f
                )
            } else {
                Polyline(
                    points = listOf(pickupLatLng, dropLatLng),
                    color = AppColors.Primary.copy(alpha = 0.5f),
                    width = 8f,
                    pattern = listOf(Dash(20f), Gap(10f))
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// SHEET BODY — All content scrolls together
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun SheetBody(
    rider: RiderInfo?,
    otp: String?,
    isPrePickup: Boolean,
    isDriverArrived: Boolean,
    isPostPickup: Boolean,
    isDelivered: Boolean,
    waitingState: WaitingTimerState,
    pickupAddress: SavedAddress,
    dropAddress: SavedAddress,
    fare: Int,
    currentStatus: BookingStatusType,
    onCallRider: () -> Unit,
    onContactSupport: () -> Unit,
    onCancelBooking: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        // ── Scrollable content ──
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            // Waiting Timer
            if (waitingState.isActive && isDriverArrived) {
                WaitingChargeCard(waitingState = waitingState)
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Driver Card
            rider?.let { r ->
                DriverCardWithOtp(
                    rider = r,
                    otp = if (isPrePickup) otp else null,
                    isDriverArrived = isDriverArrived,
                    onCallRider = onCallRider
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Journey Summary
            JourneySummaryCard(
                pickupAddress = pickupAddress,
                dropAddress = dropAddress,
                fare = fare,
                waitingCharge = waitingState.waitingCharge,
                currentStatus = currentStatus
            )

            Spacer(modifier = Modifier.height(12.dp))
        }

        // ── Bottom buttons (above nav bar) ──
        if (!isDelivered) {
            HorizontalDivider(color = AppColors.Border.copy(alpha = 0.3f))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onContactSupport,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.Primary),
                    border = BorderStroke(1.dp, AppColors.Primary)
                ) {
                    Icon(Icons.Default.Headset, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Support", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }

                if (isPrePickup) {
                    OutlinedButton(
                        onClick = onCancelBooking,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.Drop),
                        border = BorderStroke(1.dp, AppColors.Drop)
                    ) {
                        Icon(Icons.Default.Close, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Cancel", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// STATUS PILL
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun StatusPill(
    status: BookingStatusType,
    etaMinutes: Int?,
    distanceKm: Double?,
    isDriverArrived: Boolean,
    formatDistance: (Double?) -> String,
    modifier: Modifier = Modifier
) {
    val (label, containerColor, contentColor) = when (status) {
        BookingStatusType.RIDER_ASSIGNED,
        BookingStatusType.RIDER_ENROUTE -> Triple(
            buildString {
                append("Driver on the way")
                val dist = formatDistance(distanceKm)
                if (dist.isNotEmpty()) append(" · $dist")
                etaMinutes?.let { if (it > 0) append(" · ~${it} min") }
            },
            Color.White, AppColors.TextPrimary
        )
        BookingStatusType.ARRIVED -> Triple(
            "Driver has arrived at pickup", AppColors.Pickup, Color.White
        )
        BookingStatusType.PICKED_UP,
        BookingStatusType.IN_TRANSIT -> Triple(
            buildString {
                append("Parcel on the way")
                etaMinutes?.let { if (it > 0) append(" · ~${it} min") }
            },
            AppColors.Primary, Color.White
        )
        BookingStatusType.ARRIVED_DELIVERY -> Triple(
            "Arriving at delivery", AppColors.Pickup, Color.White
        )
        BookingStatusType.DELIVERED -> Triple(
            "Delivered successfully!", AppColors.Pickup, Color.White
        )
        else -> Triple("Connecting...", Color.White, AppColors.TextPrimary)
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
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val alpha by infiniteTransition.animateFloat(
                initialValue = 1f, targetValue = 0.3f,
                animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
                label = "dot"
            )
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        if (containerColor == Color.White) AppColors.Pickup.copy(alpha = alpha)
                        else Color.White.copy(alpha = alpha),
                        CircleShape
                    )
            )
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
// DRIVER CARD WITH OTP
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun DriverCardWithOtp(
    rider: RiderInfo,
    otp: String?,
    isDriverArrived: Boolean,
    onCallRider: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Driver Photo
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(AppColors.Background)
                        .border(2.dp, AppColors.Primary, CircleShape)
                ) {
                    if (rider.photoUrl != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(rider.photoUrl).crossfade(true).build(),
                            contentDescription = "Driver photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            Icons.Default.Person, null,
                            tint = AppColors.TextHint,
                            modifier = Modifier
                                .size(30.dp)
                                .align(Alignment.Center)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        rider.riderName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (rider.rating != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    Icons.Default.Star, null,
                                    tint = Color(0xFFFFB300),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    String.format("%.1f", rider.rating),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        rider.totalTrips?.let {
                            Text("·", color = AppColors.TextHint, fontSize = 10.sp)
                            Text(
                                "$it trips",
                                style = MaterialTheme.typography.labelSmall,
                                color = AppColors.TextSecondary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(getVehicleEmoji(rider.vehicleType), fontSize = 14.sp)
                        rider.vehicleType?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.labelSmall,
                                color = AppColors.TextSecondary
                            )
                        }
                        Text("·", color = AppColors.TextHint, fontSize = 10.sp)
                        Text(
                            rider.vehicleNumber,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.TextPrimary
                        )
                    }
                }

                // Call button
                IconButton(
                    onClick = onCallRider,
                    modifier = Modifier
                        .size(44.dp)
                        .background(AppColors.Pickup.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(Icons.Default.Call, "Call rider", tint = AppColors.Pickup)
                }
            }

            // OTP Section (only pre-pickup)
            if (otp != null) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = AppColors.Border.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Lock, null,
                            tint = AppColors.Primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Column {
                            Text(
                                "Pickup OTP",
                                style = MaterialTheme.typography.labelSmall,
                                color = AppColors.TextSecondary
                            )
                            Text(
                                "Share with driver",
                                style = MaterialTheme.typography.labelSmall,
                                color = AppColors.TextHint,
                                fontSize = 10.sp
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        otp.filter { it.isDigit() }.take(6).forEach { digit ->
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(AppColors.Primary, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    digit.toString(),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// WAITING CHARGE CARD
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun WaitingChargeCard(
    waitingState: WaitingTimerState,
    modifier: Modifier = Modifier
) {
    val isFreeOver = waitingState.isFreeWaitingOver
    val infiniteTransition = rememberInfiniteTransition(label = "waiting")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isFreeOver) 0.5f else 0.7f,
        animationSpec = infiniteRepeatable(
            tween(if (isFreeOver) 500 else 1000),
            RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isFreeOver) AppColors.Drop.copy(alpha = 0.06f)
            else Color(0xFFFFF3E0)
        ),
        elevation = CardDefaults.cardElevation(0.dp),
        border = BorderStroke(
            1.dp,
            if (isFreeOver) AppColors.Drop.copy(alpha = 0.3f)
            else Color(0xFFFF9800).copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    Modifier
                        .size(8.dp)
                        .background(
                            if (isFreeOver) AppColors.Drop.copy(alpha = pulseAlpha)
                            else Color(0xFFFF9800).copy(alpha = pulseAlpha),
                            CircleShape
                        )
                )
                Text(
                    if (isFreeOver) "Waiting charges applied" else "Driver is waiting",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isFreeOver) AppColors.Drop else Color(0xFFE65100)
                )
                Spacer(Modifier.weight(1f))
                Text(
                    waitingState.totalTimeFormatted,
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.TextHint
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    if (!isFreeOver) {
                        CircularTimerDisplay(
                            waitingState.freeTimeFormatted,
                            waitingState.freeWaitingProgress,
                            Color(0xFFFF9800),
                            Color(0xFFE65100),
                            "FREE"
                        )
                    } else {
                        CircularTimerDisplay(
                            "₹${waitingState.waitingCharge}",
                            waitingState.currentMinuteSeconds / 60f,
                            AppColors.Drop,
                            AppColors.Drop,
                            "${waitingState.extraMinutesCharged} min"
                        )
                    }
                }
                Box(
                    Modifier
                        .width(1.dp)
                        .height(60.dp)
                        .background(AppColors.Border)
                )
                Column(
                    Modifier
                        .weight(1f)
                        .padding(start = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (!isFreeOver) {
                        Text("⏱️", fontSize = 24.sp)
                        Text(
                            "Free waiting",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            "Hurry to pickup!",
                            style = MaterialTheme.typography.labelSmall,
                            color = AppColors.TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Text("💰", fontSize = 24.sp)
                        Text(
                            "₹${RiderTrackingViewModel.CHARGE_PER_MINUTE}/min",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.Drop,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            "Added to fare",
                            style = MaterialTheme.typography.labelSmall,
                            color = AppColors.TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            if (isFreeOver) {
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(AppColors.Drop.copy(alpha = 0.08f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "₹${RiderTrackingViewModel.CHARGE_PER_MINUTE} × ${waitingState.extraMinutesCharged} min",
                        style = MaterialTheme.typography.labelSmall,
                        color = AppColors.TextSecondary
                    )
                    Text(
                        "+ ₹${waitingState.waitingCharge}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.Drop
                    )
                }
            } else {
                Spacer(Modifier.height(6.dp))
                Text(
                    "After 3 min, ₹${RiderTrackingViewModel.CHARGE_PER_MINUTE}/min applies",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.TextHint,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun CircularTimerDisplay(
    mainText: String,
    progress: Float,
    trackColor: Color,
    textColor: Color,
    subText: String
) {
    Box(contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            progress = { 1f },
            modifier = Modifier.size(70.dp),
            color = trackColor.copy(alpha = 0.15f),
            strokeWidth = 5.dp,
            strokeCap = StrokeCap.Round
        )
        CircularProgressIndicator(
            progress = { progress },
            modifier = Modifier.size(70.dp),
            color = trackColor,
            strokeWidth = 5.dp,
            strokeCap = StrokeCap.Round
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                mainText,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Text(
                subText,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = trackColor.copy(alpha = 0.7f),
                fontSize = 9.sp
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// JOURNEY SUMMARY CARD
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun JourneySummaryCard(
    pickupAddress: SavedAddress,
    dropAddress: SavedAddress,
    fare: Int,
    waitingCharge: Int = 0,
    currentStatus: BookingStatusType
) {
    val totalFare = fare + waitingCharge

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Pickup
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier
                            .size(10.dp)
                            .background(AppColors.Pickup, CircleShape)
                    )
                    Box(
                        Modifier
                            .width(2.dp)
                            .height(28.dp)
                            .background(AppColors.Border)
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        pickupAddress.contactName ?: "Pickup",
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

            // Drop
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    Modifier
                        .size(10.dp)
                        .background(AppColors.Drop, CircleShape)
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        dropAddress.contactName ?: "Drop",
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

            Spacer(modifier = Modifier.height(12.dp))

            // Fare section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(10.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("💵", fontSize = 16.sp)
                        Text(
                            if (waitingCharge > 0) "Trip Fare" else "Cash Payment",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.TextSecondary
                        )
                    }
                    Text(
                        "₹$fare",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (waitingCharge > 0) FontWeight.SemiBold else FontWeight.Bold,
                        color = if (waitingCharge > 0) AppColors.TextPrimary else AppColors.Primary
                    )
                }

                if (waitingCharge > 0) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("⏱️", fontSize = 16.sp)
                            Text(
                                "Waiting Charge",
                                style = MaterialTheme.typography.bodySmall,
                                color = AppColors.Drop
                            )
                        }
                        Text(
                            "+ ₹$waitingCharge",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = AppColors.Drop
                        )
                    }
                    HorizontalDivider(
                        Modifier.padding(vertical = 4.dp),
                        color = AppColors.Border
                    )
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Total (Cash)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "₹$totalFare",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.Primary
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// RATING DIALOG
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun RatingBottomSheet(
    ratingState: RatingUiState,
    onSubmitRating: (Int, String?) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedRating by remember { mutableIntStateOf(0) }
    var feedback by remember { mutableStateOf("") }
    val starLabels = listOf("", "Terrible", "Bad", "Okay", "Good", "Excellent")

    if (ratingState.isSubmitted) {
        AlertDialog(
            onDismissRequest = onDismiss,
            shape = RoundedCornerShape(24.dp),
            containerColor = Color.White,
            title = null,
            text = {
                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("✅", fontSize = 48.sp)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Thank you!",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Your feedback helps us improve",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {}
        )
        return
    }

    AlertDialog(
        onDismissRequest = {},
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White,
        title = {
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("🎉", fontSize = 40.sp)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Delivery Complete!",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                if (ratingState.totalFare > 0) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Total: ₹${ratingState.totalFare}",
                        style = MaterialTheme.typography.titleMedium,
                        color = AppColors.Primary,
                        fontWeight = FontWeight.Bold
                    )
                    if (ratingState.waitingCharge > 0) {
                        Text(
                            "(incl. ₹${ratingState.waitingCharge} waiting)",
                            style = MaterialTheme.typography.labelSmall,
                            color = AppColors.TextHint
                        )
                    }
                }
            }
        },
        text = {
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Rate ${ratingState.driverName}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextSecondary
                )
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (i in 1..5) {
                        IconButton(
                            onClick = { selectedRating = i },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Text(
                                if (i <= selectedRating) "⭐" else "☆",
                                fontSize = if (i <= selectedRating) 32.sp else 28.sp
                            )
                        }
                    }
                }
                if (selectedRating > 0) {
                    Text(
                        starLabels[selectedRating],
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.Primary
                    )
                }
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = feedback,
                    onValueChange = { feedback = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            "Share your experience (optional)",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.TextHint
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppColors.Primary,
                        unfocusedBorderColor = AppColors.Border
                    )
                )
                ratingState.error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        it,
                        style = MaterialTheme.typography.labelSmall,
                        color = AppColors.Drop
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSubmitRating(selectedRating, feedback.takeIf { it.isNotBlank() })
                },
                enabled = selectedRating > 0 && !ratingState.isSubmitting,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.Primary,
                    disabledContainerColor = AppColors.Border
                )
            ) {
                if (ratingState.isSubmitting) {
                    CircularProgressIndicator(
                        Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        "Submit Rating",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    "Skip",
                    color = AppColors.TextHint,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    )
}

// ═══════════════════════════════════════════════════════════════════════════════
// CANCEL DIALOG
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun CancelBookingDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var selectedReason by remember { mutableStateOf<String?>(null) }
    val reasons = listOf(
        "Driver taking too long",
        "Found another ride",
        "Plans changed",
        "Wrong pickup/drop location",
        "Other reason"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = Color.White,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    Modifier
                        .size(40.dp)
                        .background(AppColors.Drop.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("❌", fontSize = 20.sp)
                }
                Text("Cancel Booking?", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column {
                Text(
                    "Please select a reason:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextSecondary
                )
                Spacer(Modifier.height(12.dp))
                reasons.forEach { reason ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { selectedReason = reason }
                            .background(
                                if (selectedReason == reason) AppColors.Primary.copy(alpha = 0.08f)
                                else Color.Transparent
                            )
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedReason == reason,
                            onClick = { selectedReason = reason },
                            colors = RadioButtonDefaults.colors(selectedColor = AppColors.Primary)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(reason, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { selectedReason?.let { onConfirm(it) } },
                enabled = selectedReason != null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.Drop,
                    disabledContainerColor = AppColors.Border
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cancel Booking", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "Go Back",
                    color = AppColors.TextSecondary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    )
}

// ═══════════════════════════════════════════════════════════════════════════════
// UTILS
// ═══════════════════════════════════════════════════════════════════════════════

private fun getVehicleEmoji(vehicleType: String?): String {
    if (vehicleType == null) return "🚗"
    return when {
        vehicleType.contains("bike", true) || vehicleType.contains("two", true) -> "🏍️"
        vehicleType.contains("auto", true) || vehicleType.contains("three", true) -> "🛺"
        vehicleType.contains("ace", true) || vehicleType.contains("mini", true) || vehicleType.contains("pickup", true) -> "🚛"
        vehicleType.contains("truck", true) || vehicleType.contains("eicher", true) -> "🚚"
        vehicleType.contains("van", true) -> "🚐"
        else -> "🚗"
    }
}