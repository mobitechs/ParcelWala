// ui/screens/booking/RiderFoundScreen.kt
// ✅ POLISHED: Industry-standard UI with proper null handling
package com.mobitechs.parcelwala.ui.screens.booking

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.*
import com.mobitechs.parcelwala.data.model.realtime.RiderInfo
import com.mobitechs.parcelwala.data.model.request.SavedAddress
import com.mobitechs.parcelwala.ui.theme.AppColors
import com.mobitechs.parcelwala.ui.viewmodel.RiderTrackingViewModel
import kotlinx.coroutines.delay

/**
 * ════════════════════════════════════════════════════════════════════════════
 * RIDER FOUND SCREEN - PREMIUM UI
 * ════════════════════════════════════════════════════════════════════════════
 *
 * ✅ Features:
 *    - Zoomable/Interactive map
 *    - Proper null handling with fallback displays
 *    - Clean ETA/Distance display (no overlap)
 *    - OTP card with visibility control
 *    - Professional driver info card
 *    - Smooth animations
 *
 * ════════════════════════════════════════════════════════════════════════════
 */
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

    // State from ViewModel
    val rider by viewModel.assignedRider.collectAsState()
    val riderLocation by viewModel.riderLocation.collectAsState()
    val otp by viewModel.bookingOtp.collectAsState()
    val etaMinutes by viewModel.etaMinutes.collectAsState()
    val distanceKm by viewModel.distanceKm.collectAsState()

    // Animation states
    var showContent by remember { mutableStateOf(false) }
    var showCancelDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(150)
        showContent = true
    }

    Scaffold(
        topBar = {
            Surface(
                shadowElevation = 2.dp,
                color = Color.White
            ) {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Animated checkmark
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(AppColors.Success.copy(alpha = 0.1f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = AppColors.Success,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Rider Assigned",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = AppColors.TextPrimary
                                )
                                Text(
                                    text = "Trip #$bookingId",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AppColors.TextSecondary
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { /* Share trip */ }) {
                            Icon(
                                Icons.Outlined.Share,
                                contentDescription = "Share",
                                tint = AppColors.Primary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.White
                    )
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
            // Scrollable content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                // ═══════════════════════════════════════════════════════════════
                // MAP SECTION - Zoomable
                // ═══════════════════════════════════════════════════════════════
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    InteractiveMapView(
                        pickupAddress = pickupAddress,
                        dropAddress = dropAddress,
                        riderLatitude = riderLocation?.latitude ?: rider?.currentLatitude,
                        riderLongitude = riderLocation?.longitude ?: rider?.currentLongitude,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ═══════════════════════════════════════════════════════════════
                // ETA & DISTANCE CARD - Fixed layout
                // ═══════════════════════════════════════════════════════════════
                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn() + slideInVertically { -20 }
                ) {
                    ETADistanceCard(
                        etaMinutes = etaMinutes ?: riderLocation?.etaMinutes ?: rider?.etaMinutes,
                        distanceKm = distanceKm ?: riderLocation?.distanceMeters?.let { it / 1000.0 },
                        isLive = riderLocation != null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ═══════════════════════════════════════════════════════════════
                // DRIVER DETAILS CARD
                // ═══════════════════════════════════════════════════════════════
                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn() + slideInVertically { 20 }
                ) {
                    DriverDetailsCard(
                        rider = rider,
                        onCallDriver = {
                            rider?.riderPhone?.let { phone ->
                                if (phone.isNotBlank()) {
                                    val intent = Intent(Intent.ACTION_DIAL).apply {
                                        data = Uri.parse("tel:$phone")
                                    }
                                    context.startActivity(intent)
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ═══════════════════════════════════════════════════════════════
                // OTP CARD - Only show if OTP exists
                // ═══════════════════════════════════════════════════════════════
                AnimatedVisibility(
                    visible = showContent && !otp.isNullOrBlank(),
                    enter = fadeIn() + expandVertically()
                ) {
                    OTPDisplayCard(
                        otp = otp ?: "----",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    )
                }

                // Show placeholder if OTP is null
                AnimatedVisibility(
                    visible = showContent && otp.isNullOrBlank(),
                    enter = fadeIn() + expandVertically()
                ) {
                    OTPPendingCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ═══════════════════════════════════════════════════════════════
                // TRIP DETAILS CARD
                // ═══════════════════════════════════════════════════════════════
                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn() + slideInVertically { 20 }
                ) {
                    TripDetailsCard(
                        pickupAddress = pickupAddress,
                        dropAddress = dropAddress,
                        fare = fare,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // ═══════════════════════════════════════════════════════════════
            // BOTTOM ACTION BUTTONS - Fixed at bottom
            // ═══════════════════════════════════════════════════════════════
            Surface(
                shadowElevation = 8.dp,
                color = Color.White
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onContactSupport,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = AppColors.Primary
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = Brush.linearGradient(listOf(AppColors.Primary, AppColors.Primary))
                        )
                    ) {
                        Icon(
                            Icons.Outlined.HeadsetMic,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Support", fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedButton(
                        onClick = { showCancelDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = AppColors.Error
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = Brush.linearGradient(listOf(AppColors.Error, AppColors.Error))
                        )
                    ) {
                        Icon(
                            Icons.Outlined.Close,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Cancel", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }

    // Cancel Dialog
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
// INTERACTIVE MAP - Zoomable & Pannable
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun InteractiveMapView(
    pickupAddress: SavedAddress,
    dropAddress: SavedAddress,
    riderLatitude: Double?,
    riderLongitude: Double?,
    modifier: Modifier = Modifier
) {
    val pickupLatLng = LatLng(pickupAddress.latitude, pickupAddress.longitude)
    val dropLatLng = LatLng(dropAddress.latitude, dropAddress.longitude)

    val riderLatLng = if (riderLatitude != null && riderLongitude != null &&
        riderLatitude != 0.0 && riderLongitude != 0.0
    ) {
        LatLng(riderLatitude, riderLongitude)
    } else null

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(pickupLatLng, 14f)
    }

    // Auto-adjust camera to show all markers
    LaunchedEffect(riderLatLng, pickupLatLng) {
        try {
            val boundsBuilder = LatLngBounds.builder()
                .include(pickupLatLng)
                .include(dropLatLng)

            riderLatLng?.let { boundsBuilder.include(it) }

            val bounds = boundsBuilder.build()
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngBounds(bounds, 80),
                durationMs = 500
            )
        } catch (e: Exception) {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(pickupLatLng, 13f)
            )
        }
    }

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        properties = MapProperties(
            mapType = MapType.NORMAL,
            isMyLocationEnabled = true
        ),
        uiSettings = MapUiSettings(
            zoomControlsEnabled = true,        // ✅ Enable zoom controls
            zoomGesturesEnabled = true,        // ✅ Enable pinch zoom
            scrollGesturesEnabled = true,      // ✅ Enable pan
            tiltGesturesEnabled = true,
            rotationGesturesEnabled = true,
            myLocationButtonEnabled = true,
            mapToolbarEnabled = true,
            compassEnabled = true
        )
    ) {
        // Pickup marker (Green)
        Marker(
            state = MarkerState(position = pickupLatLng),
            title = "Pickup",
            snippet = pickupAddress.address.take(30),
            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
        )

        // Drop marker (Red)
        Marker(
            state = MarkerState(position = dropLatLng),
            title = "Drop",
            snippet = dropAddress.address.take(30),
            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
        )

        // Driver marker (Blue) - Only show if location available
        riderLatLng?.let { location ->
            Marker(
                state = MarkerState(position = location),
                title = "Driver",
                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// ETA & DISTANCE CARD - Clean layout with no overlap
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun ETADistanceCard(
    etaMinutes: Int?,
    distanceKm: Double?,
    isLive: Boolean,
    modifier: Modifier = Modifier
) {
    // Pulsing animation for live indicator
    val infiniteTransition = rememberInfiniteTransition(label = "live_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isLive) AppColors.Success.copy(alpha = 0.08f) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isLive) 0.dp else 2.dp),
        border = if (isLive) {
            androidx.compose.foundation.BorderStroke(1.dp, AppColors.Success.copy(alpha = 0.3f))
        } else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Live Tracking Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Pulsing dot
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(
                            if (isLive) AppColors.Success.copy(alpha = pulseAlpha)
                            else AppColors.Warning,
                            CircleShape
                        )
                )
                Text(
                    text = if (isLive) "Live Tracking" else "Waiting for location...",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isLive) AppColors.Success else AppColors.Warning
                )

                Spacer(modifier = Modifier.weight(1f))

                if (!isLive) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = AppColors.Warning
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ETA & Distance Row - FIXED LAYOUT
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // ETA Column
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Schedule,
                            contentDescription = null,
                            tint = AppColors.Primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = if (etaMinutes != null && etaMinutes > 0) {
                                "$etaMinutes"
                            } else {
                                "--"
                            },
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.Primary
                        )
                    }
                    Text(
                        text = if (etaMinutes != null && etaMinutes > 0) "min ETA" else "ETA",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextSecondary
                    )
                }

                // Vertical Divider
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(50.dp)
                        .background(AppColors.Border)
                )

                // Distance Column
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Outlined.NearMe,
                            contentDescription = null,
                            tint = AppColors.Pickup,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = if (distanceKm != null) {
                                String.format("%.1f", distanceKm)
                            } else {
                                "--"
                            },
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.Pickup
                        )
                    }
                    Text(
                        text = if (distanceKm != null) "km away" else "Distance",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextSecondary
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// DRIVER DETAILS CARD - With proper null handling
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun DriverDetailsCard(
    rider: RiderInfo?,
    onCallDriver: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
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
            // Driver Photo
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .border(2.dp, AppColors.Primary.copy(alpha = 0.3f), CircleShape)
                    .background(AppColors.Primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                if (!rider?.photoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(rider?.photoUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Driver photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = AppColors.Primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Driver Info
            Column(modifier = Modifier.weight(1f)) {
                // Name
                Text(
                    text = rider?.riderName?.takeIf { it.isNotBlank() } ?: "Driver",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Rating & Trips Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Rating
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFC107),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = rider?.rating?.let {
                                String.format("%.1f", it)
                            } ?: "New",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = AppColors.TextPrimary
                        )
                    }

                    // Divider dot
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .background(AppColors.TextHint, CircleShape)
                    )

                    // Total Trips
                    Text(
                        text = rider?.totalTrips?.let { "$it trips" } ?: "New Driver",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Vehicle Info
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Outlined.LocalShipping,
                        contentDescription = null,
                        tint = AppColors.Primary,
                        modifier = Modifier.size(16.dp)
                    )

                    val vehicleText = buildString {
                        rider?.vehicleType?.takeIf { it.isNotBlank() }?.let {
                            append(it)
                            append(" • ")
                        }
                        append(rider?.vehicleNumber?.takeIf { it.isNotBlank() } ?: "Vehicle")
                    }

                    Text(
                        text = vehicleText,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = AppColors.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Call Button
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .shadow(4.dp, CircleShape)
                    .clip(CircleShape)
                    .background(AppColors.Pickup)
                    .clickable(onClick = onCallDriver),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Call,
                    contentDescription = "Call driver",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// OTP DISPLAY CARD - Prominent display
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun OTPDisplayCard(
    otp: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            AppColors.Primary.copy(alpha = 0.05f),
                            AppColors.Primary.copy(alpha = 0.1f)
                        )
                    )
                )
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = AppColors.Primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Pickup OTP",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // OTP Digits
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                otp.take(4).padEnd(4, '-').forEach { digit ->
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .shadow(4.dp, RoundedCornerShape(12.dp))
                            .clip(RoundedCornerShape(12.dp))
                            .background(AppColors.Primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = digit.toString(),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Share this OTP with driver at pickup",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextHint,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// OTP PENDING CARD - When OTP is not yet received
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun OTPPendingCard(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp,
                color = AppColors.Primary
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Generating OTP...",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.TextPrimary
                )
                Text(
                    text = "OTP will appear here once driver confirms",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextSecondary
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// TRIP DETAILS CARD
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun TripDetailsCard(
    pickupAddress: SavedAddress,
    dropAddress: SavedAddress,
    fare: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Pickup
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(AppColors.Pickup, CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(32.dp)
                            .background(AppColors.Border)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = pickupAddress.contactName?.takeIf { it.isNotBlank() } ?: "Pickup",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.TextPrimary
                    )
                    Text(
                        text = pickupAddress.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Drop
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(AppColors.Drop, CircleShape)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = dropAddress.contactName?.takeIf { it.isNotBlank() } ?: "Drop",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.TextPrimary
                    )
                    Text(
                        text = dropAddress.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Payment Info
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(AppColors.Background)
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        Icons.Outlined.Payments,
                        contentDescription = null,
                        tint = AppColors.Primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "Cash Payment",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.TextSecondary
                    )
                }
                Text(
                    text = "₹$fare",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.Primary
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// CANCEL BOOKING DIALOG
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
                    modifier = Modifier
                        .size(40.dp)
                        .background(AppColors.Error.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Cancel,
                        contentDescription = null,
                        tint = AppColors.Error,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Text(
                    "Cancel Booking?",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column {
                Text(
                    "Please select a reason:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextSecondary
                )

                Spacer(modifier = Modifier.height(16.dp))

                reasons.forEach { reason ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { selectedReason = reason }
                            .background(
                                if (selectedReason == reason)
                                    AppColors.Primary.copy(alpha = 0.1f)
                                else Color.Transparent
                            )
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedReason == reason,
                            onClick = { selectedReason = reason },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = AppColors.Primary
                            )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = reason,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { selectedReason?.let { onConfirm(it) } },
                enabled = selectedReason != null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.Error,
                    disabledContainerColor = AppColors.Border
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "Cancel Booking",
                    fontWeight = FontWeight.SemiBold
                )
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