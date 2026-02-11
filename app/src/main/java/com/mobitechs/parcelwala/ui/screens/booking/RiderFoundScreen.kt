// ui/screens/booking/RiderFoundScreen.kt
// ✅ POLISHED: Industry-standard UI with permission fix + waiting charge timer
package com.mobitechs.parcelwala.ui.screens.booking

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.*
import com.mobitechs.parcelwala.data.model.realtime.BookingStatusType
import com.mobitechs.parcelwala.data.model.realtime.RiderInfo
import com.mobitechs.parcelwala.data.model.request.SavedAddress
import com.mobitechs.parcelwala.ui.theme.AppColors
import com.mobitechs.parcelwala.ui.viewmodel.RiderTrackingViewModel
import com.mobitechs.parcelwala.ui.viewmodel.WaitingTimerState
import com.mobitechs.parcelwala.utils.hasLocationPermission
import kotlinx.coroutines.delay

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
    val uiState by viewModel.uiState.collectAsState()
    val waitingState by viewModel.waitingState.collectAsState()

    val isDriverArrived = uiState.currentStatus == BookingStatusType.ARRIVED

    var showContent by remember { mutableStateOf(false) }
    var showCancelDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(150)
        showContent = true
    }

    Scaffold(
        topBar = {
            Surface(shadowElevation = 2.dp, color = Color.White) {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        if (isDriverArrived) AppColors.Primary.copy(alpha = 0.1f)
                                        else AppColors.Success.copy(alpha = 0.1f),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    if (isDriverArrived) Icons.Default.LocationOn
                                    else Icons.Default.Check,
                                    contentDescription = null,
                                    tint = if (isDriverArrived) AppColors.Primary else AppColors.Success,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = if (isDriverArrived) "Driver Arrived" else "Rider Assigned",
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
                        IconButton(onClick = { }) {
                            Icon(Icons.Outlined.Share, "Share", tint = AppColors.Primary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
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
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                // ── MAP ───────────────────────────────────────────────
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

                // ── WAITING TIMER (when driver arrived) ───────────────
                AnimatedVisibility(
                    visible = showContent && waitingState.isActive,
                    enter = fadeIn() + expandVertically()
                ) {
                    WaitingChargeCard(
                        waitingState = waitingState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    )
                }

                // ── ETA CARD (hide when waiting timer active) ─────────
                AnimatedVisibility(
                    visible = showContent && !waitingState.isActive,
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

                // ── DRIVER DETAILS ────────────────────────────────────
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

                // ── OTP CARD ──────────────────────────────────────────
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

                // ── TRIP DETAILS (with waiting charge in fare) ────────
                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn() + slideInVertically { 20 }
                ) {
                    TripDetailsCard(
                        pickupAddress = pickupAddress,
                        dropAddress = dropAddress,
                        fare = fare,
                        waitingCharge = waitingState.waitingCharge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // ── BOTTOM BUTTONS ────────────────────────────────────────
            Surface(shadowElevation = 8.dp, color = Color.White) {
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
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.Primary),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = Brush.linearGradient(listOf(AppColors.Primary, AppColors.Primary))
                        )
                    ) {
                        Icon(Icons.Outlined.HeadsetMic, null, Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Support", fontWeight = FontWeight.SemiBold)
                    }
                    OutlinedButton(
                        onClick = { showCancelDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.Error),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = Brush.linearGradient(listOf(AppColors.Error, AppColors.Error))
                        )
                    ) {
                        Icon(Icons.Outlined.Close, null, Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Cancel", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }

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
// WAITING CHARGE CARD - 3 min free → ₹3/min after
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun WaitingChargeCard(
    waitingState: WaitingTimerState,
    modifier: Modifier = Modifier
) {
    val isFreeOver = waitingState.isFreeWaitingOver

    val infiniteTransition = rememberInfiniteTransition(label = "waiting_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isFreeOver) 0.5f else 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isFreeOver) 500 else 1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "waiting_pulse_alpha"
    )

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isFreeOver) AppColors.Error.copy(alpha = 0.06f)
            else AppColors.Warning.copy(alpha = 0.08f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(
            1.5.dp,
            if (isFreeOver) AppColors.Error.copy(alpha = 0.4f)
            else AppColors.Warning.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(
                            if (isFreeOver) AppColors.Error.copy(alpha = pulseAlpha)
                            else AppColors.Warning.copy(alpha = pulseAlpha),
                            CircleShape
                        )
                )
                Text(
                    text = if (isFreeOver) "Waiting charges applied" else "Driver is waiting",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isFreeOver) AppColors.Error else AppColors.Warning
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = waitingState.totalTimeFormatted,
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.TextHint
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main content
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // LEFT: Circular timer/charge
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    if (!isFreeOver) {
                        FreeWaitingCountdown(
                            freeTimeFormatted = waitingState.freeTimeFormatted,
                            progress = waitingState.freeWaitingProgress
                        )
                    } else {
                        WaitingChargeCounter(
                            charge = waitingState.waitingCharge,
                            extraMinutes = waitingState.extraMinutesCharged,
                            currentMinuteSeconds = waitingState.currentMinuteSeconds
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(70.dp)
                        .background(AppColors.Border)
                )

                // RIGHT: Info
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (!isFreeOver) {
                        Icon(Icons.Outlined.Timer, null, tint = AppColors.Warning, modifier = Modifier.size(28.dp))
                        Text(
                            "Free waiting",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = AppColors.TextPrimary,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            "Please hurry to\npickup point",
                            style = MaterialTheme.typography.labelSmall,
                            color = AppColors.TextSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp
                        )
                    } else {
                        Icon(Icons.Outlined.CurrencyRupee, null, tint = AppColors.Error, modifier = Modifier.size(28.dp))
                        Text(
                            "₹${RiderTrackingViewModel.CHARGE_PER_MINUTE}/min",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.Error,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            "Waiting charge\napplied to fare",
                            style = MaterialTheme.typography.labelSmall,
                            color = AppColors.TextSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // Bottom strip
            if (isFreeOver) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(AppColors.Error.copy(alpha = 0.08f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "₹${RiderTrackingViewModel.CHARGE_PER_MINUTE} × ${waitingState.extraMinutesCharged} min",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextSecondary
                    )
                    Text(
                        "+ ₹${waitingState.waitingCharge}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.Error
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "After 3 min, ₹${RiderTrackingViewModel.CHARGE_PER_MINUTE}/min waiting charge applies",
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
private fun FreeWaitingCountdown(freeTimeFormatted: String, progress: Float) {
    Box(contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            progress = { 1f },
            modifier = Modifier.size(80.dp),
            color = AppColors.Warning.copy(alpha = 0.15f),
            strokeWidth = 6.dp,
            strokeCap = StrokeCap.Round
        )
        CircularProgressIndicator(
            progress = { progress },
            modifier = Modifier.size(80.dp),
            color = AppColors.Warning,
            strokeWidth = 6.dp,
            strokeCap = StrokeCap.Round
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                freeTimeFormatted,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = AppColors.Warning
            )
            Text(
                "FREE",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = AppColors.Warning.copy(alpha = 0.7f),
                fontSize = 9.sp
            )
        }
    }
}

@Composable
private fun WaitingChargeCounter(charge: Int, extraMinutes: Int, currentMinuteSeconds: Int) {
    val currentMinuteProgress = currentMinuteSeconds / 60f
    Box(contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            progress = { 1f },
            modifier = Modifier.size(80.dp),
            color = AppColors.Error.copy(alpha = 0.15f),
            strokeWidth = 6.dp,
            strokeCap = StrokeCap.Round
        )
        CircularProgressIndicator(
            progress = { currentMinuteProgress },
            modifier = Modifier.size(80.dp),
            color = AppColors.Error,
            strokeWidth = 6.dp,
            strokeCap = StrokeCap.Round
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "₹$charge",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = AppColors.Error
            )
            Text(
                "${extraMinutes} min",
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.Error.copy(alpha = 0.7f),
                fontSize = 9.sp
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// INTERACTIVE MAP
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun InteractiveMapView(
    pickupAddress: SavedAddress,
    dropAddress: SavedAddress,
    riderLatitude: Double?,
    riderLongitude: Double?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(context.hasLocationPermission()) }
    var permissionDenied by remember { mutableStateOf(false) }
    var permanentlyDenied by remember { mutableStateOf(false) }
    val activity = context as? androidx.activity.ComponentActivity

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
        if (!isGranted) {
            permissionDenied = true
            permanentlyDenied = activity?.let {
                !ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.ACCESS_FINE_LOCATION)
            } ?: false
        } else {
            permissionDenied = false
            permanentlyDenied = false
        }
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val newPerm = context.hasLocationPermission()
                if (newPerm != hasPermission) {
                    hasPermission = newPerm
                    if (newPerm) { permissionDenied = false; permanentlyDenied = false }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val pickupLatLng = LatLng(pickupAddress.latitude, pickupAddress.longitude)
    val dropLatLng = LatLng(dropAddress.latitude, dropAddress.longitude)
    val riderLatLng = if (riderLatitude != null && riderLongitude != null &&
        riderLatitude != 0.0 && riderLongitude != 0.0
    ) LatLng(riderLatitude, riderLongitude) else null

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(pickupLatLng, 14f)
    }

    LaunchedEffect(riderLatLng, pickupLatLng) {
        try {
            val boundsBuilder = LatLngBounds.builder().include(pickupLatLng).include(dropLatLng)
            riderLatLng?.let { boundsBuilder.include(it) }
            cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 80), durationMs = 500)
        } catch (e: Exception) {
            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(pickupLatLng, 13f))
        }
    }

    Box(modifier = modifier) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(mapType = MapType.NORMAL, isMyLocationEnabled = hasPermission),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = true, zoomGesturesEnabled = true, scrollGesturesEnabled = true,
                tiltGesturesEnabled = true, rotationGesturesEnabled = true,
                myLocationButtonEnabled = hasPermission, mapToolbarEnabled = true, compassEnabled = true
            )
        ) {
            Marker(state = MarkerState(position = pickupLatLng), title = "Pickup",
                snippet = pickupAddress.address.take(30),
                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
            Marker(state = MarkerState(position = dropLatLng), title = "Drop",
                snippet = dropAddress.address.take(30),
                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
            riderLatLng?.let {
                Marker(state = MarkerState(position = it), title = "Driver",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
            }
        }

        if (permissionDenied && !hasPermission) {
            LocationPermissionBanner(
                isPermanentlyDenied = permanentlyDenied,
                onRequestPermission = {
                    if (permanentlyDenied) {
                        context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        })
                    } else permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                },
                onDismiss = { permissionDenied = false },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// LOCATION PERMISSION BANNER
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun LocationPermissionBanner(
    isPermanentlyDenied: Boolean,
    onRequestPermission: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth().padding(8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Outlined.LocationOff, null, tint = AppColors.Warning, modifier = Modifier.size(24.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Location access disabled", style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold, color = AppColors.TextPrimary)
                Text(
                    if (isPermanentlyDenied) "Enable in Settings for live tracking"
                    else "Allow location for better tracking",
                    style = MaterialTheme.typography.labelSmall, color = AppColors.TextSecondary
                )
            }
            TextButton(onClick = onRequestPermission,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(if (isPermanentlyDenied) "Settings" else "Allow",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold, color = AppColors.Primary)
            }
            IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Close, "Dismiss", tint = AppColors.TextHint, modifier = Modifier.size(16.dp))
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// ETA & DISTANCE CARD
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun ETADistanceCard(
    etaMinutes: Int?, distanceKm: Double?, isLive: Boolean, modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "live_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 0.4f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "pulse"
    )

    Card(
        modifier = modifier, shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isLive) AppColors.Success.copy(alpha = 0.08f) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isLive) 0.dp else 2.dp),
        border = if (isLive) BorderStroke(1.dp, AppColors.Success.copy(alpha = 0.3f)) else null
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.size(10.dp).background(
                    if (isLive) AppColors.Success.copy(alpha = pulseAlpha) else AppColors.Warning, CircleShape))
                Text(
                    if (isLive) "Live Tracking" else "Waiting for location...",
                    style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold,
                    color = if (isLive) AppColors.Success else AppColors.Warning
                )
                Spacer(modifier = Modifier.weight(1f))
                if (!isLive) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = AppColors.Warning)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Outlined.Schedule, null, tint = AppColors.Primary, modifier = Modifier.size(24.dp))
                        Text(
                            if (etaMinutes != null && etaMinutes > 0) "$etaMinutes" else "--",
                            style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = AppColors.Primary
                        )
                    }
                    Text(if (etaMinutes != null && etaMinutes > 0) "min ETA" else "ETA",
                        style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
                }
                Box(modifier = Modifier.width(1.dp).height(50.dp).background(AppColors.Border))
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Outlined.NearMe, null, tint = AppColors.Pickup, modifier = Modifier.size(24.dp))
                        Text(
                            if (distanceKm != null) String.format("%.1f", distanceKm) else "--",
                            style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = AppColors.Pickup
                        )
                    }
                    Text(if (distanceKm != null) "km away" else "Distance",
                        style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// DRIVER DETAILS CARD
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun DriverDetailsCard(rider: RiderInfo?, onCallDriver: () -> Unit, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(60.dp).clip(CircleShape)
                    .border(2.dp, AppColors.Primary.copy(alpha = 0.3f), CircleShape)
                    .background(AppColors.Primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                if (!rider?.photoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current).data(rider?.photoUrl).crossfade(true).build(),
                        contentDescription = "Driver photo", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Default.Person, null, tint = AppColors.Primary, modifier = Modifier.size(32.dp))
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    rider?.riderName?.takeIf { it.isNotBlank() } ?: "Driver",
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Icon(Icons.Default.Star, null, tint = Color(0xFFFFC107), modifier = Modifier.size(16.dp))
                        Text(rider?.rating?.let { String.format("%.1f", it) } ?: "New",
                            style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = AppColors.TextPrimary)
                    }
                    Box(modifier = Modifier.size(4.dp).background(AppColors.TextHint, CircleShape))
                    Text(rider?.totalTrips?.let { "$it trips" } ?: "New Driver",
                        style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Outlined.LocalShipping, null, tint = AppColors.Primary, modifier = Modifier.size(16.dp))
                    val vehicleText = buildString {
                        rider?.vehicleType?.takeIf { it.isNotBlank() }?.let { append(it); append(" • ") }
                        append(rider?.vehicleNumber?.takeIf { it.isNotBlank() } ?: "Vehicle")
                    }
                    Text(vehicleText, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium,
                        color = AppColors.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Box(
                modifier = Modifier.size(48.dp).shadow(4.dp, CircleShape).clip(CircleShape)
                    .background(AppColors.Pickup).clickable(onClick = onCallDriver),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Call, "Call driver", tint = Color.White, modifier = Modifier.size(22.dp))
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// OTP DISPLAY CARD
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun OTPDisplayCard(otp: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
                .background(brush = Brush.verticalGradient(listOf(
                    AppColors.Primary.copy(alpha = 0.05f), AppColors.Primary.copy(alpha = 0.1f))))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.Lock, null, tint = AppColors.Primary, modifier = Modifier.size(20.dp))
                Text("Pickup OTP", style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold, color = AppColors.TextSecondary)
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Dynamic: supports 4-digit and 6-digit OTPs
            val otpDigits = otp.filter { it.isDigit() }
            val displayLength = if (otpDigits.length >= 6) 6 else 4
            val displayOtp = otpDigits.take(displayLength).padEnd(displayLength, '-')
            val boxSize = if (displayLength >= 6) 44.dp else 52.dp
            val gapSize = if (displayLength >= 6) 8.dp else 12.dp
            val fontSize = if (displayLength >= 6) MaterialTheme.typography.headlineSmall
            else MaterialTheme.typography.headlineMedium

            Row(horizontalArrangement = Arrangement.spacedBy(gapSize)) {
                displayOtp.forEach { digit ->
                    Box(
                        modifier = Modifier.size(boxSize).shadow(4.dp, RoundedCornerShape(12.dp))
                            .clip(RoundedCornerShape(12.dp)).background(AppColors.Primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(digit.toString(), style = fontSize,
                            fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text("Share this OTP with driver at pickup", style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextHint, textAlign = TextAlign.Center)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// OTP PENDING CARD
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun OTPPendingCard(modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp, color = AppColors.Primary)
            Column(modifier = Modifier.weight(1f)) {
                Text("Generating OTP...", style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold, color = AppColors.TextPrimary)
                Text("OTP will appear here once driver confirms",
                    style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// TRIP DETAILS CARD - Shows waiting charge in fare breakdown
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun TripDetailsCard(
    pickupAddress: SavedAddress, dropAddress: SavedAddress,
    fare: Int, waitingCharge: Int = 0, modifier: Modifier = Modifier
) {
    val totalFare = fare + waitingCharge

    Card(modifier = modifier, shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            // Pickup
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier.size(12.dp).background(AppColors.Pickup, CircleShape))
                    Box(modifier = Modifier.width(2.dp).height(32.dp).background(AppColors.Border))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(pickupAddress.contactName?.takeIf { it.isNotBlank() } ?: "Pickup",
                        style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = AppColors.TextPrimary)
                    Text(pickupAddress.address, style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            // Drop
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.size(12.dp).background(AppColors.Drop, CircleShape))
                Column(modifier = Modifier.weight(1f)) {
                    Text(dropAddress.contactName?.takeIf { it.isNotBlank() } ?: "Drop",
                        style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = AppColors.TextPrimary)
                    Text(dropAddress.address, style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Payment section
            Column(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(AppColors.Background).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Base fare
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Outlined.Payments, null, tint = AppColors.Primary, modifier = Modifier.size(22.dp))
                        Text(
                            if (waitingCharge > 0) "Trip Fare" else "Cash Payment",
                            style = MaterialTheme.typography.bodyMedium, color = AppColors.TextSecondary
                        )
                    }
                    Text("₹$fare",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (waitingCharge > 0) FontWeight.SemiBold else FontWeight.Bold,
                        color = if (waitingCharge > 0) AppColors.TextPrimary else AppColors.Primary)
                }

                // Waiting charge (if any)
                if (waitingCharge > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Outlined.Timer, null, tint = AppColors.Error, modifier = Modifier.size(22.dp))
                            Text("Waiting Charge", style = MaterialTheme.typography.bodyMedium, color = AppColors.Error)
                        }
                        Text("+ ₹$waitingCharge", style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold, color = AppColors.Error)
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = AppColors.Border)

                    // Total
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Total (Cash)", style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
                        Text("₹$totalFare", style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold, color = AppColors.Primary)
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// CANCEL BOOKING DIALOG
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun CancelBookingDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var selectedReason by remember { mutableStateOf<String?>(null) }
    val reasons = listOf(
        "Driver taking too long", "Found another ride", "Plans changed",
        "Wrong pickup/drop location", "Other reason"
    )

    AlertDialog(
        onDismissRequest = onDismiss, shape = RoundedCornerShape(20.dp), containerColor = Color.White,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier.size(40.dp).background(AppColors.Error.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Outlined.Cancel, null, tint = AppColors.Error, modifier = Modifier.size(22.dp)) }
                Text("Cancel Booking?", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column {
                Text("Please select a reason:", style = MaterialTheme.typography.bodyMedium, color = AppColors.TextSecondary)
                Spacer(modifier = Modifier.height(16.dp))
                reasons.forEach { reason ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                            .clickable { selectedReason = reason }
                            .background(if (selectedReason == reason) AppColors.Primary.copy(alpha = 0.1f) else Color.Transparent)
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selectedReason == reason, onClick = { selectedReason = reason },
                            colors = RadioButtonDefaults.colors(selectedColor = AppColors.Primary))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(reason, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { selectedReason?.let { onConfirm(it) } }, enabled = selectedReason != null,
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Error, disabledContainerColor = AppColors.Border),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Cancel Booking", fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, shape = RoundedCornerShape(12.dp)) {
                Text("Go Back", color = AppColors.TextSecondary, fontWeight = FontWeight.SemiBold)
            }
        }
    )
}