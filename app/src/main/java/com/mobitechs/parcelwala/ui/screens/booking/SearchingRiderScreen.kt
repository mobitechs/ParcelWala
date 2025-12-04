// ui/screens/booking/SearchingRiderScreen.kt
package com.mobitechs.parcelwala.ui.screens.booking

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.*
import com.google.maps.android.compose.*
import com.mobitechs.parcelwala.data.model.request.SavedAddress
import com.mobitechs.parcelwala.data.model.response.FareDetails  // ✅ Changed import
import com.mobitechs.parcelwala.data.repository.RouteInfo
import com.mobitechs.parcelwala.ui.components.InfoCard
import com.mobitechs.parcelwala.ui.theme.AppColors
import com.mobitechs.parcelwala.ui.viewmodel.BookingViewModel

/**
 * Searching for Rider Screen
 * Shows map with route in top 50%, booking details below
 * Final screen before driver is assigned
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchingRiderScreen(
    bookingId: String,
    pickupAddress: SavedAddress,
    dropAddress: SavedAddress,
    selectedFareDetails: FareDetails,  // ✅ Changed from VehicleTypeResponse
    fare: Int,
    onRiderFound: () -> Unit,
    onContactSupport: () -> Unit,
    onViewDetails: () -> Unit,
    viewModel: BookingViewModel = hiltViewModel()
) {
    var showCancelSheet by remember { mutableStateOf(false) }

    // Observe route info from ViewModel
    val routeInfo by viewModel.routeInfo.collectAsState()
    val isRouteLoading by viewModel.isRouteLoading.collectAsState()

    // Calculate route when screen loads
    LaunchedEffect(pickupAddress, dropAddress) {
        if (pickupAddress.latitude != 0.0 && pickupAddress.longitude != 0.0 &&
            dropAddress.latitude != 0.0 && dropAddress.longitude != 0.0
        ) {
            viewModel.calculateRoute(
                pickupLat = pickupAddress.latitude,
                pickupLng = pickupAddress.longitude,
                dropLat = dropAddress.latitude,
                dropLng = dropAddress.longitude
            )
        }
    }

    // Animation for pulse effect (for searching indicator)
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
                            // Animated dot
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .scale(pulseScale)
                                    .background(AppColors.Primary, CircleShape)
                            )
                            Text(
                                text = "Finding your rider...",
                                style = MaterialTheme.typography.labelSmall,
                                color = AppColors.Primary
                            )
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        containerColor = AppColors.Background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // ============ TOP 45%: MAP WITH ROUTE ============
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.45f)
            ) {
                RouteMapView(
                    pickupAddress = pickupAddress,
                    dropAddress = dropAddress,
                    routeInfo = routeInfo,
                    isLoading = isRouteLoading,
                    modifier = Modifier.fillMaxSize()
                )

                // Distance & Time Overlay Card
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
                            // Distance
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Route,
                                    contentDescription = null,
                                    tint = AppColors.Primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = route.distanceText,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = AppColors.TextPrimary
                                )
                            }

                            // Divider
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(24.dp)
                                    .background(AppColors.Border)
                            )

                            // Duration
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = null,
                                    tint = AppColors.Primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = route.durationText,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = AppColors.TextPrimary
                                )
                            }
                        }
                    }
                }

                // Loading indicator for route
                if (isRouteLoading) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .background(
                                color = Color.White.copy(alpha = 0.8f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = AppColors.Primary
                            )
                            Text(
                                text = "Loading route...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = AppColors.TextSecondary
                            )
                        }
                    }
                }
            }

            // ============ BOTTOM 55%: SCROLLABLE DETAILS ============
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // Searching Status Card
                SearchingStatusCard(
                    pulseScale = pulseScale,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Journey Details Card
                JourneyDetailsCard(
                    pickupAddress = pickupAddress,
                    dropAddress = dropAddress,
                    onViewDetails = onViewDetails,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Vehicle & Payment Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Vehicle Info Card - ✅ Changed to use FareDetails
                    VehicleInfoCompactCard(
                        fareDetails = selectedFareDetails,
                        modifier = Modifier.weight(1f)
                    )

                    // Payment Card
                    PaymentCompactCard(
                        paymentMethod = "Cash",
                        amount = fare,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Support & Cancel Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Contact Support Button
                    OutlinedButton(
                        onClick = onContactSupport,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = AppColors.Primary
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.Primary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Headset,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Support",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Cancel Trip Button
                    OutlinedButton(
                        onClick = { showCancelSheet = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = AppColors.Drop
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.Drop)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Cancel",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Cancel Reason Bottom Sheet
    if (showCancelSheet) {
        CancellationReasonBottomSheet(
            onDismiss = { showCancelSheet = false },
            onConfirmCancel = { reason ->
                viewModel.cancelBooking(reason)
                showCancelSheet = false
            }
        )
    }
}

/**
 * Route Map View with Polyline
 */
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

    // Calculate bounds
    val boundsBuilder = remember(pickupLatLng, dropLatLng, routeInfo) {
        LatLngBounds.builder()
            .include(pickupLatLng)
            .include(dropLatLng)
            .apply {
                routeInfo?.polylinePoints?.forEach { point ->
                    include(point)
                }
            }
    }

    val cameraPositionState = rememberCameraPositionState()

    // Animate camera to show full route
    LaunchedEffect(routeInfo, pickupLatLng, dropLatLng) {
        try {
            val bounds = boundsBuilder.build()
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngBounds(bounds, 80),
                durationMs = 1000
            )
        } catch (e: Exception) {
            // Fallback to center between points
            val centerLat = (pickupLatLng.latitude + dropLatLng.latitude) / 2
            val centerLng = (pickupLatLng.longitude + dropLatLng.longitude) / 2
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(LatLng(centerLat, centerLng), 13f)
            )
        }
    }

    GoogleMap(
        modifier = modifier.clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)),
        cameraPositionState = cameraPositionState,
        properties = MapProperties(
            mapType = MapType.NORMAL,
            isMyLocationEnabled = false
        ),
        uiSettings = MapUiSettings(
            zoomControlsEnabled = false,
            myLocationButtonEnabled = false,
            mapToolbarEnabled = false,
            compassEnabled = false
        )
    ) {
        // Pickup Marker (Green)
        Marker(
            state = MarkerState(position = pickupLatLng),
            title = "Pickup",
            snippet = pickupAddress.address.take(50),
            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
        )

        // Drop Marker (Red)
        Marker(
            state = MarkerState(position = dropLatLng),
            title = "Drop",
            snippet = dropAddress.address.take(50),
            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
        )

        // Draw Route Polyline
        routeInfo?.let { route ->
            // Shadow/border polyline (darker, thicker)
            Polyline(
                points = route.polylinePoints,
                color = Color(0xFF1565C0),
                width = 14f,
                jointType = JointType.ROUND,
                startCap = RoundCap(),
                endCap = RoundCap(),
                zIndex = 0f
            )

            // Main route polyline
            Polyline(
                points = route.polylinePoints,
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

/**
 * Compact Searching Status Card
 */
@Composable
private fun SearchingStatusCard(
    pulseScale: Float,
    modifier: Modifier = Modifier
) {
    InfoCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Animated Pulse Circle
            Box(
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                // Outer pulse
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .scale(pulseScale)
                        .alpha(0.3f)
                        .background(AppColors.Primary, CircleShape)
                )
                // Inner circle
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(AppColors.Primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalShipping,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Searching for drivers nearby",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary
                )
                Text(
                    text = "Usually takes 1-3 minutes",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextSecondary
                )
            }
        }
    }
}

/**
 * Compact Journey Details Card
 */
@Composable
private fun JourneyDetailsCard(
    pickupAddress: SavedAddress,
    dropAddress: SavedAddress,
    onViewDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    InfoCard(modifier = modifier) {
        // Pickup
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(AppColors.Pickup, CircleShape)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = pickupAddress.contactName ?: "Pickup",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary
                )
                Text(
                    text = pickupAddress.address.take(45) + if (pickupAddress.address.length > 45) "..." else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextSecondary,
                    maxLines = 1
                )
            }
        }

        // Connector
        Box(
            modifier = Modifier
                .padding(start = 4.dp, top = 4.dp, bottom = 4.dp)
                .width(2.dp)
                .height(16.dp)
                .background(AppColors.Border)
        )

        // Drop
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(AppColors.Drop, CircleShape)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = dropAddress.contactName ?: "Drop",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary
                )
                Text(
                    text = dropAddress.address.take(45) + if (dropAddress.address.length > 45) "..." else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextSecondary,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Compact Vehicle Info Card - ✅ Updated to use FareDetails
 */
@Composable
private fun VehicleInfoCompactCard(
    fareDetails: FareDetails,
    modifier: Modifier = Modifier
) {
    InfoCard(modifier = modifier) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = fareDetails.vehicleTypeIcon ?: "🚚",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = fareDetails.vehicleTypeName ?: "Vehicle",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary
            )
            Text(
                text = fareDetails.capacity ?: "",
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.TextSecondary
            )
        }
    }
}

/**
 * Compact Payment Card
 */
@Composable
private fun PaymentCompactCard(
    paymentMethod: String,
    amount: Int,
    modifier: Modifier = Modifier
) {
    InfoCard(modifier = modifier) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "₹$amount",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = AppColors.Primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Wallet,
                    contentDescription = null,
                    tint = AppColors.Pickup,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = paymentMethod,
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextSecondary
                )
            }
        }
    }
}

/**
 * Cancellation Reason Bottom Sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CancellationReasonBottomSheet(
    onDismiss: () -> Unit,
    onConfirmCancel: (String) -> Unit
) {
    var selectedReason by remember { mutableStateOf<String?>(null) }
    var otherReason by remember { mutableStateOf("") }
    var showConfirmDialog by remember { mutableStateOf(false) }

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val cancellationReasons = listOf(
        CancellationReason("driver_delayed", "Driver is taking too long", Icons.Default.Timer),
        CancellationReason("change_plans", "Change of plans", Icons.Default.EventBusy),
        CancellationReason("wrong_address", "Wrong pickup/drop location", Icons.Default.LocationOff),
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
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .background(AppColors.Border, RoundedCornerShape(2.dp))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight(0.9f)
                .fillMaxWidth()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Cancel Trip?",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    )
                    Text(
                        text = "Please select a reason",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.TextSecondary
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, "Close", tint = AppColors.TextSecondary)
                }
            }

            // Scrollable Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                cancellationReasons.forEach { reason ->
                    CancellationReasonItem(
                        reason = reason,
                        isSelected = selectedReason == reason.id,
                        onClick = {
                            selectedReason = reason.id
                            if (reason.id != "other") {
                                otherReason = ""
                                keyboardController?.hide()
                                focusManager.clearFocus()
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Other Reason TextField
                if (selectedReason == "other") {
                    Spacer(modifier = Modifier.height(8.dp))
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
                        keyboardActions = KeyboardActions(onDone = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                        }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AppColors.Primary,
                            focusedLabelColor = AppColors.Primary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Warning
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
                                text = "Cancellation Policy",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = AppColors.TextPrimary
                            )
                            Text(
                                text = "Frequent cancellations may result in temporary suspension.",
                                style = MaterialTheme.typography.bodySmall,
                                color = AppColors.TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Bottom Buttons
            Surface(color = Color.White, shadowElevation = 8.dp) {
                Row(
                    modifier = Modifier
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
                        border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.Primary),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.Primary)
                    ) {
                        Text("Keep Trip", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            if (selectedReason != null &&
                                (selectedReason != "other" || otherReason.isNotBlank())
                            ) {
                                showConfirmDialog = true
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.Drop,
                            disabledContainerColor = AppColors.Drop.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        enabled = selectedReason != null &&
                                (selectedReason != "other" || otherReason.isNotBlank())
                    ) {
                        Text("Cancel Trip", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Confirmation Dialog
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            icon = { Icon(Icons.Default.ErrorOutline, null, tint = AppColors.Drop, modifier = Modifier.size(32.dp)) },
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
                        val reason = if (selectedReason == "other") {
                            otherReason.ifBlank { "Other" }
                        } else {
                            cancellationReasons.find { it.id == selectedReason }?.title ?: "Cancelled"
                        }
                        onConfirmCancel(reason)
                        showConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Drop)
                ) {
                    Text("Yes, Cancel")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("No, Keep It", color = AppColors.Primary)
                }
            },
            containerColor = Color.White
        )
    }
}

/**
 * Cancellation Reason Item
 */
@Composable
private fun CancellationReasonItem(
    reason: CancellationReason,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) AppColors.Primary else AppColors.Border
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = reason.icon,
                contentDescription = null,
                tint = if (isSelected) AppColors.Primary else AppColors.TextSecondary,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = reason.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) AppColors.Primary else AppColors.TextPrimary,
                modifier = Modifier.weight(1f)
            )
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = AppColors.Primary,
                    unselectedColor = AppColors.Border
                )
            )
        }
    }
}

data class CancellationReason(
    val id: String,
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)