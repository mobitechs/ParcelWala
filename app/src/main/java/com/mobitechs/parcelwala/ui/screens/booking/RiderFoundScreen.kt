// ui/screens/booking/RiderFoundScreen.kt
// ✅ FIXED: Issue 3 (route polyline, m/km format, Marker syntax)
// ✅ FIXED: Issue 4 (small ETA card bottom-right, larger map)
// ✅ ADDED: Waiting timer UI (3 min free → ₹3/min after driver arrives)
package com.mobitechs.parcelwala.ui.screens.booking

import android.content.Intent
import android.net.Uri
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
import com.mobitechs.parcelwala.data.model.realtime.BookingStatusType
import com.mobitechs.parcelwala.data.model.realtime.RiderInfo
import com.mobitechs.parcelwala.data.model.request.SavedAddress
import com.mobitechs.parcelwala.ui.components.InfoCard
import com.mobitechs.parcelwala.ui.theme.AppColors
import com.mobitechs.parcelwala.ui.viewmodel.RiderTrackingViewModel
import com.mobitechs.parcelwala.ui.viewmodel.WaitingTimerState
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

    // State from ViewModel
    val rider by viewModel.assignedRider.collectAsState()
    val riderLocation by viewModel.riderLocation.collectAsState()
    val otp by viewModel.bookingOtp.collectAsState()
    val etaMinutes by viewModel.etaMinutes.collectAsState()
    val distanceKm by viewModel.distanceKm.collectAsState()
    val driverToPickupRoute by viewModel.driverToPickupRoute.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val waitingState by viewModel.waitingState.collectAsState()

    val isDriverArrived = uiState.currentStatus == BookingStatusType.ARRIVED

    // Animation states
    var showContent by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(100)
        showContent = true
    }

    // Cancel confirmation dialog
    var showCancelDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isDriverArrived) "Driver Arrived!" else "Rider Assigned!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Trip $bookingId",
                            style = MaterialTheme.typography.labelSmall,
                            color = AppColors.TextSecondary
                        )
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
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                // ✅ Issue 4: LARGER MAP (300dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                ) {
                    // ✅ Issue 3: Map with route polyline
                    RiderMapView(
                        pickupAddress = pickupAddress,
                        riderLatitude = riderLocation?.latitude ?: rider?.currentLatitude,
                        riderLongitude = riderLocation?.longitude ?: rider?.currentLongitude,
                        routePoints = driverToPickupRoute,
                        modifier = Modifier.fillMaxSize()
                    )

                    // ✅ Issue 4: Small ETA+Distance card at BOTTOM-END (right)
                    // Only show when waiting timer is NOT active
                    val eta = etaMinutes
                    val dist = distanceKm
                    if (!waitingState.isActive && ((eta != null && eta > 0) || (dist != null && dist > 0))) {
                        Card(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(12.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // ✅ Issue 3: Distance in m/km format
                                if (dist != null && dist > 0) {
                                    Text(
                                        text = viewModel.formatDistance(dist),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = AppColors.Primary,
                                        fontSize = 16.sp
                                    )
                                }
                                if (eta != null && eta > 0) {
                                    Text(
                                        text = "~$eta min",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = AppColors.TextSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ── WAITING TIMER CARD (when driver has arrived) ──────────
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
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // RIDER DETAILS CARD
                AnimatedVisibility(
                    visible = showContent && rider != null,
                    enter = fadeIn() + slideInVertically { it / 2 }
                ) {
                    rider?.let { r ->
                        RiderDetailsCard(
                            rider = r,
                            onCallRider = {
                                val intent = Intent(Intent.ACTION_DIAL).apply {
                                    data = Uri.parse("tel:${r.riderPhone}")
                                }
                                context.startActivity(intent)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // OTP CARD
                AnimatedVisibility(
                    visible = showContent && otp != null,
                    enter = fadeIn() + slideInVertically { it / 2 }
                ) {
                    otp?.let { otpCode ->
                        OtpCard(
                            otp = otpCode,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // JOURNEY SUMMARY (with waiting charge in fare breakdown)
                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn() + slideInVertically { it / 2 }
                ) {
                    JourneySummaryCard(
                        pickupAddress = pickupAddress,
                        dropAddress = dropAddress,
                        fare = fare,
                        waitingCharge = waitingState.waitingCharge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // ── BOTTOM BUTTONS (fixed at bottom) ──────────────────────
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
                        border = BorderStroke(1.dp, AppColors.Primary)
                    ) {
                        Icon(Icons.Default.Headset, null, Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Support", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { showCancelDialog = true },
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
            }
        }
    }

    // Cancel Confirmation Dialog
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
// HELPER COMPOSABLES
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun RiderDetailsCard(
    rider: RiderInfo,
    onCallRider: () -> Unit,
    modifier: Modifier = Modifier
) {
    InfoCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Photo
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(AppColors.Surface)
                    .border(2.dp, AppColors.Primary, CircleShape)
            ) {
                if (rider.photoUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(rider.photoUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Rider photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = AppColors.TextHint,
                        modifier = Modifier
                            .size(40.dp)
                            .align(Alignment.Center)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = rider.riderName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (rider.rating != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Star, null, tint = Color(0xFFFFB300), modifier = Modifier.size(16.dp))
                            Text(String.format("%.1f", rider.rating), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (rider.totalTrips != null) {
                        Text("•", color = AppColors.TextHint)
                        Text("${rider.totalTrips} trips", style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.LocalShipping, null, tint = AppColors.Primary, modifier = Modifier.size(16.dp))
                    rider.vehicleType?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary) }
                    Text("•", color = AppColors.TextHint)
                    Text(rider.vehicleNumber, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }
            }

            // Call Button
            IconButton(
                onClick = onCallRider,
                modifier = Modifier
                    .size(48.dp)
                    .background(AppColors.Pickup.copy(alpha = 0.1f), CircleShape)
            ) {
                Icon(Icons.Default.Call, "Call rider", tint = AppColors.Pickup)
            }
        }
    }
}

@Composable
private fun OtpCard(otp: String, modifier: Modifier = Modifier) {
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
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            AppColors.Primary.copy(alpha = 0.05f),
                            AppColors.Primary.copy(alpha = 0.1f)
                        )
                    )
                )
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Lock, null, tint = AppColors.Primary, modifier = Modifier.size(20.dp))
                Text("Pickup OTP", style = MaterialTheme.typography.labelLarge, color = AppColors.TextSecondary)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Dynamic: supports 4-digit and 6-digit OTPs
            val otpDigits = otp.filter { it.isDigit() }
            val displayLength = if (otpDigits.length >= 6) 6 else 4
            val displayOtp = otpDigits.take(displayLength).padEnd(displayLength, '-')
            val boxSize = if (displayLength >= 6) 44.dp else 48.dp
            val gapSize = if (displayLength >= 6) 8.dp else 12.dp

            Row(horizontalArrangement = Arrangement.spacedBy(gapSize)) {
                displayOtp.forEach { digit ->
                    Box(
                        modifier = Modifier
                            .size(boxSize)
                            .background(AppColors.Primary, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            digit.toString(),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Share this OTP with rider for pickup verification",
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.TextHint,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun JourneySummaryCard(
    pickupAddress: SavedAddress,
    dropAddress: SavedAddress,
    fare: Int,
    waitingCharge: Int = 0,
    modifier: Modifier = Modifier
) {
    val totalFare = fare + waitingCharge

    InfoCard(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(Modifier.size(10.dp).background(AppColors.Pickup, CircleShape))
            Column(Modifier.weight(1f)) {
                Text(
                    pickupAddress.contactName ?: "Pickup",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    pickupAddress.address.take(40) + if (pickupAddress.address.length > 40) "..." else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextSecondary,
                    maxLines = 1
                )
            }
        }

        Box(
            Modifier
                .padding(start = 4.dp, top = 4.dp, bottom = 4.dp)
                .width(2.dp)
                .height(16.dp)
                .background(AppColors.Border)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(Modifier.size(10.dp).background(AppColors.Drop, CircleShape))
            Column(Modifier.weight(1f)) {
                Text(
                    dropAddress.contactName ?: "Drop",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    dropAddress.address.take(40) + if (dropAddress.address.length > 40) "..." else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextSecondary,
                    maxLines = 1
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Payment section with waiting charge breakdown
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppColors.Surface, RoundedCornerShape(8.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Base fare
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Wallet, null, tint = AppColors.Primary, modifier = Modifier.size(20.dp))
                    Text(
                        if (waitingCharge > 0) "Trip Fare" else "Cash Payment",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.TextSecondary
                    )
                }
                Text(
                    "₹$fare",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (waitingCharge > 0) FontWeight.SemiBold else FontWeight.Bold,
                    color = if (waitingCharge > 0) AppColors.TextPrimary else AppColors.Primary
                )
            }

            // Waiting charge (if any)
            if (waitingCharge > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Outlined.Timer, null, tint = AppColors.Error, modifier = Modifier.size(20.dp))
                        Text("Waiting Charge", style = MaterialTheme.typography.bodyMedium, color = AppColors.Error)
                    }
                    Text(
                        "+ ₹$waitingCharge",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.Error
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = AppColors.Border)

                // Total
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Total (Cash)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    )
                    Text(
                        "₹$totalFare",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.Primary
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// CANCEL BOOKING DIALOG (with reason selection)
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun CancelBookingDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var selectedReason by remember { mutableStateOf<String?>(null) }
    val reasons = listOf(
        "Driver taking too long", "Found another ride", "Plans changed",
        "Wrong pickup/drop location", "Other reason"
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
                    Icon(Icons.Outlined.Cancel, null, tint = AppColors.Error, modifier = Modifier.size(22.dp))
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
                Spacer(modifier = Modifier.height(16.dp))
                reasons.forEach { reason ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { selectedReason = reason }
                            .background(
                                if (selectedReason == reason) AppColors.Primary.copy(alpha = 0.1f)
                                else Color.Transparent
                            )
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedReason == reason,
                            onClick = { selectedReason = reason },
                            colors = RadioButtonDefaults.colors(selectedColor = AppColors.Primary)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
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
                    containerColor = AppColors.Error,
                    disabledContainerColor = AppColors.Border
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cancel Booking", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, shape = RoundedCornerShape(12.dp)) {
                Text("Go Back", color = AppColors.TextSecondary, fontWeight = FontWeight.SemiBold)
            }
        }
    )
}

// ═══════════════════════════════════════════════════════════════════════════════
// MAP WITH ROUTE POLYLINE
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * ✅ Issue 3: Map with route polyline + proper zoom + fixed Marker syntax
 */
@Composable
private fun RiderMapView(
    pickupAddress: SavedAddress,
    riderLatitude: Double?,
    riderLongitude: Double?,
    routePoints: List<LatLng>,
    modifier: Modifier = Modifier
) {
    val pickupLatLng = LatLng(pickupAddress.latitude, pickupAddress.longitude)
    val riderLatLng = if (riderLatitude != null && riderLongitude != null &&
        riderLatitude != 0.0 && riderLongitude != 0.0
    ) LatLng(riderLatitude, riderLongitude) else null

    val cameraPositionState = rememberCameraPositionState()

    // Animate camera to fit both markers
    LaunchedEffect(riderLatLng, pickupLatLng) {
        try {
            if (riderLatLng != null) {
                val bounds = LatLngBounds.builder()
                    .include(pickupLatLng)
                    .include(riderLatLng)
                    .build()
                // padding 120 for markers + small ETA card at corner
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngBounds(bounds, 120), 500
                )
            } else {
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngZoom(pickupLatLng, 15f)
                )
            }
        } catch (e: Exception) {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(pickupLatLng, 14f)
            )
        }
    }

    GoogleMap(
        modifier = modifier.clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)),
        cameraPositionState = cameraPositionState,
        properties = MapProperties(mapType = MapType.NORMAL),
        uiSettings = MapUiSettings(
            zoomControlsEnabled = false,
            myLocationButtonEnabled = false,
            mapToolbarEnabled = false,
            compassEnabled = false
        )
    ) {
        // ✅ Fixed Marker syntax: state = MarkerState(position = ...)
        Marker(
            state = MarkerState(position = pickupLatLng),
            title = "Pickup",
            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
        )

        riderLatLng?.let {
            Marker(
                state = MarkerState(position = it),
                title = "Rider",
                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
            )
        }

        // ✅ Issue 3: Route polyline (actual road route)
        if (routePoints.isNotEmpty()) {
            Polyline(
                points = routePoints,
                color = AppColors.Primary,
                width = 12f
            )
        } else if (riderLatLng != null) {
            // Fallback: dashed straight line while route is loading
            Polyline(
                points = listOf(riderLatLng, pickupLatLng),
                color = AppColors.Primary.copy(alpha = 0.4f),
                width = 8f,
                pattern = listOf(Dash(20f), Gap(10f))
            )
        }
    }
}