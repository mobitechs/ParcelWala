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
import androidx.compose.ui.res.stringResource
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
import com.mobitechs.parcelwala.ui.components.InfoCard
import com.mobitechs.parcelwala.ui.components.RatingDialog
import com.mobitechs.parcelwala.ui.theme.AppColors
import com.mobitechs.parcelwala.ui.viewmodel.RiderTrackingViewModel
import com.mobitechs.parcelwala.ui.viewmodel.WaitingTimerState
import com.mobitechs.parcelwala.ui.viewmodel.RatingUiState
import kotlinx.coroutines.delay
import com.mobitechs.parcelwala.data.model.response.formatPrice
import com.mobitechs.parcelwala.data.model.response.formatRupee

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RiderFoundScreen(
    bookingId: String,
    pickupAddress: SavedAddress,
    dropAddress: SavedAddress,
    fare: Double,
    onCancelBooking: (String) -> Unit,
    onContactSupport: () -> Unit,
    viewModel: RiderTrackingViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    val rider by viewModel.assignedRider.collectAsState()
    val riderLocation by viewModel.riderLocation.collectAsState()
    val otp by viewModel.bookingOtp.collectAsState()
    val deliveryOtp by viewModel.deliveredOtp.collectAsState()
    val etaMinutes by viewModel.etaMinutes.collectAsState()
    val distanceKm by viewModel.distanceKm.collectAsState()
    val driverToPickupRoute by viewModel.driverToPickupRoute.collectAsState()
    val pickupToDropRoute by viewModel.pickupToDropRoute.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val waitingState by viewModel.waitingState.collectAsState()
    val ratingState by viewModel.ratingState.collectAsState()
    val paymentState by viewModel.paymentState.collectAsState()

    val currentStatus = uiState.currentStatus

    val isPrePickup = currentStatus == BookingStatusType.RIDER_ASSIGNED ||
            currentStatus == BookingStatusType.RIDER_ENROUTE ||
            currentStatus == BookingStatusType.ARRIVED
    val isDriverArrived = currentStatus == BookingStatusType.ARRIVED
    val isPostPickup = currentStatus == BookingStatusType.PICKED_UP ||
            currentStatus == BookingStatusType.IN_TRANSIT ||
            currentStatus == BookingStatusType.ARRIVED_DELIVERY
    val isDelivered = currentStatus == BookingStatusType.DELIVERED
    val isPaymentSuccess = currentStatus == BookingStatusType.PAYMENT_SUCCESS
    val shouldShowDeliveryOtp = isPostPickup && deliveryOtp != null

    var showContent by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(100); showContent = true }

    var showCancelDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = when {
                                isDelivered -> stringResource(R.string.delivery_complete)
                                isPaymentSuccess -> "Payment Confirmed"
                                isPostPickup -> stringResource(R.string.parcel_in_transit)
                                isDriverArrived -> stringResource(R.string.driver_arrived)
                                else -> stringResource(R.string.rider_assigned_title)
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.trip_label, bookingId),
                            style = MaterialTheme.typography.labelSmall,
                            color = AppColors.TextSecondary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* Share */ }) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = stringResource(R.string.share), tint = AppColors.Primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = AppColors.Background
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                // MAP
                Box(modifier = Modifier.fillMaxWidth().height(300.dp)) {
                    RiderMapView(
                        pickupAddress = pickupAddress, dropAddress = dropAddress,
                        riderLatitude = riderLocation?.latitude ?: rider?.currentLatitude,
                        riderLongitude = riderLocation?.longitude ?: rider?.currentLongitude,
                        driverToPickupRoute = driverToPickupRoute,
                        pickupToDropRoute = pickupToDropRoute,
                        isPrePickup = isPrePickup,
                        modifier = Modifier.fillMaxSize()
                    )

                    val eta = etaMinutes; val dist = distanceKm
                    if (!waitingState.isActive && ((eta != null && eta > 0) || (dist != null && dist > 0))) {
                        Card(
                            modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                if (dist != null && dist > 0) Text(text = viewModel.formatDistance(dist), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = AppColors.Primary, fontSize = 16.sp)
                                if (eta != null && eta > 0) Text(text = stringResource(R.string.eta_min_format, eta), style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary, fontSize = 12.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // WAITING TIMER CARD
                AnimatedVisibility(visible = showContent && waitingState.isActive && isDriverArrived, enter = fadeIn() + expandVertically()) {
                    Column {
                        WaitingChargeCard(waitingState = waitingState, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                // RIDER DETAILS
                AnimatedVisibility(visible = showContent && rider != null, enter = fadeIn() + slideInVertically { it / 2 }) {
                    rider?.let { r ->
                        RiderDetailsCard(
                            rider = r,
                            onCallRider = {
                                val intent = Intent(Intent.ACTION_DIAL).apply { data = Uri.parse("tel:${r.riderPhone}") }
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // PICKUP OTP CARD (pre-pickup phase)
                AnimatedVisibility(visible = showContent && otp != null && isPrePickup, enter = fadeIn() + slideInVertically { it / 2 }) {
                    otp?.let { otpCode -> OtpCard(otp = otpCode, label = stringResource(R.string.pickup_otp), sublabel = stringResource(R.string.share_otp_for_verification), modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) }
                }

                // ✅ NEW: DELIVERY OTP CARD (post-pickup phase)
                AnimatedVisibility(visible = showContent && shouldShowDeliveryOtp, enter = fadeIn() + slideInVertically { it / 2 }) {
                    deliveryOtp?.let { otpCode ->
                        OtpCard(
                            otp = otpCode,
                            label = "Delivery OTP",
                            sublabel = "Share this OTP with receiver for delivery verification",
                            accentColor = AppColors.Drop,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // JOURNEY SUMMARY
                AnimatedVisibility(visible = showContent, enter = fadeIn() + slideInVertically { it / 2 }) {
                    JourneySummaryCard(
                        pickupAddress = pickupAddress, dropAddress = dropAddress,
                        fare = fare, waitingCharge = waitingState.waitingCharge,
                        currentStatus = currentStatus,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // BOTTOM BUTTONS
            if (!isDelivered && !isPaymentSuccess) {
                Surface(shadowElevation = 8.dp, color = Color.White) {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = onContactSupport,
                            modifier = Modifier.weight(1f).height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.Primary),
                            border = BorderStroke(1.dp, AppColors.Primary)
                        ) {
                            Icon(Icons.Default.Headset, null, Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.support), fontWeight = FontWeight.Bold)
                        }
                        if (isPrePickup) {
                            OutlinedButton(
                                onClick = { showCancelDialog = true },
                                modifier = Modifier.weight(1f).height(52.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.Drop),
                                border = BorderStroke(1.dp, AppColors.Drop)
                            ) {
                                Icon(Icons.Default.Close, null, Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.cancel), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // Rating Dialog
    if (ratingState.showRatingDialog) {
        RatingDialog(
            bookingNumber = bookingId, fare = ratingState.totalFare.toInt(),
            existingCustomerRating = null, existingCustomerFeedback = null,
            driverRatingForCustomer = null, driverFeedbackForCustomer = null,
            onDismiss = { viewModel.skipRating() },
            onSubmit = { rating, feedback -> viewModel.submitRating(ratingState.bookingId, rating, feedback) },
            isSubmitting = ratingState.isSubmitting
        )
    }

    if (paymentState.isVerifyingPayment) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { /* non-dismissable */ }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(
                        color = AppColors.Primary,
                        modifier = Modifier.size(48.dp),
                        strokeWidth = 4.dp
                    )
                    Text(
                        text = "Verifying Payment",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    )
                    Text(
                        text = "Confirming payment with driver...\nPlease wait",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    // Cancel Dialog
    if (showCancelDialog) {
        CancelBookingDialog(
            onDismiss = { showCancelDialog = false },
            onConfirm = { reason -> showCancelDialog = false; onCancelBooking(reason) }
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// WAITING CHARGE CARD (unchanged)
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun WaitingChargeCard(waitingState: WaitingTimerState, modifier: Modifier = Modifier) {
    val isFreeOver = waitingState.isFreeWaitingOver
    val infiniteTransition = rememberInfiniteTransition(label = "waiting_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = if (isFreeOver) 0.5f else 0.7f,
        animationSpec = infiniteRepeatable(animation = tween(if (isFreeOver) 500 else 1000), repeatMode = RepeatMode.Reverse), label = "waiting_pulse_alpha"
    )

    Card(
        modifier = modifier, shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (isFreeOver) AppColors.Error.copy(alpha = 0.06f) else AppColors.Warning.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.5.dp, if (isFreeOver) AppColors.Error.copy(alpha = 0.4f) else AppColors.Warning.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.size(10.dp).background(if (isFreeOver) AppColors.Error.copy(alpha = pulseAlpha) else AppColors.Warning.copy(alpha = pulseAlpha), CircleShape))
                Text(
                    text = if (isFreeOver) stringResource(R.string.waiting_charges_applied) else stringResource(R.string.driver_is_waiting),
                    style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold,
                    color = if (isFreeOver) AppColors.Error else AppColors.Warning
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(text = waitingState.totalTimeFormatted, style = MaterialTheme.typography.labelSmall, color = AppColors.TextHint)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    if (!isFreeOver) FreeWaitingCountdown(freeTimeFormatted = waitingState.freeTimeFormatted, progress = waitingState.freeWaitingProgress)
                    else WaitingChargeCounter(charge = waitingState.waitingCharge, extraMinutes = waitingState.extraMinutesCharged, currentMinuteSeconds = waitingState.currentMinuteSeconds)
                }
                Box(modifier = Modifier.width(1.dp).height(70.dp).background(AppColors.Border))
                Column(modifier = Modifier.weight(1f).padding(start = 16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (!isFreeOver) {
                        Icon(Icons.Outlined.Timer, null, tint = AppColors.Warning, modifier = Modifier.size(28.dp))
                        Text(stringResource(R.string.free_waiting), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = AppColors.TextPrimary, textAlign = TextAlign.Center)
                        Text(stringResource(R.string.hurry_to_pickup), style = MaterialTheme.typography.labelSmall, color = AppColors.TextSecondary, textAlign = TextAlign.Center, lineHeight = 16.sp)
                    } else {
                        Icon(Icons.Outlined.CurrencyRupee, null, tint = AppColors.Error, modifier = Modifier.size(28.dp))
                        Text(stringResource(R.string.charge_per_min_format, formatPrice(waitingState.chargePerMinute)), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = AppColors.Error, textAlign = TextAlign.Center)
                        Text(stringResource(R.string.waiting_charge_applied_fare), style = MaterialTheme.typography.labelSmall, color = AppColors.TextSecondary, textAlign = TextAlign.Center, lineHeight = 16.sp)
                    }
                }
            }
            if (isFreeOver) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(AppColors.Error.copy(alpha = 0.08f)).padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(R.string.charge_breakdown_format, formatPrice(waitingState.chargePerMinute), waitingState.extraMinutesCharged), style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
                    Text(stringResource(R.string.plus_charge_format, formatRupee(waitingState.waitingCharge)), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = AppColors.Error)
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                Text(stringResource(R.string.free_waiting_info_format, formatPrice(waitingState.chargePerMinute)), style = MaterialTheme.typography.labelSmall, color = AppColors.TextHint, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun FreeWaitingCountdown(freeTimeFormatted: String, progress: Float) {
    Box(contentAlignment = Alignment.Center) {
        CircularProgressIndicator(progress = { 1f }, modifier = Modifier.size(80.dp), color = AppColors.Warning.copy(alpha = 0.15f), strokeWidth = 6.dp, strokeCap = StrokeCap.Round)
        CircularProgressIndicator(progress = { progress }, modifier = Modifier.size(80.dp), color = AppColors.Warning, strokeWidth = 6.dp, strokeCap = StrokeCap.Round)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(freeTimeFormatted, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = AppColors.Warning)
            Text(stringResource(R.string.free_label), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = AppColors.Warning.copy(alpha = 0.7f), fontSize = 9.sp)
        }
    }
}

@Composable
private fun WaitingChargeCounter(charge: Double, extraMinutes: Int, currentMinuteSeconds: Int) {
    val currentMinuteProgress = currentMinuteSeconds / 60f
    Box(contentAlignment = Alignment.Center) {
        CircularProgressIndicator(progress = { 1f }, modifier = Modifier.size(80.dp), color = AppColors.Error.copy(alpha = 0.15f), strokeWidth = 6.dp, strokeCap = StrokeCap.Round)
        CircularProgressIndicator(progress = { currentMinuteProgress }, modifier = Modifier.size(80.dp), color = AppColors.Error, strokeWidth = 6.dp, strokeCap = StrokeCap.Round)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(formatRupee(charge), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = AppColors.Error) // ✅ was "₹$charge"
            Text("${extraMinutes}m", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = AppColors.Error.copy(alpha = 0.7f), fontSize = 9.sp)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// HELPER COMPOSABLES
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun RiderDetailsCard(rider: RiderInfo, onCallRider: () -> Unit, modifier: Modifier = Modifier) {
    InfoCard(modifier = modifier) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(72.dp).clip(CircleShape).background(AppColors.Surface).border(2.dp, AppColors.Primary, CircleShape)) {
                if (rider.photoUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current).data(rider.photoUrl).crossfade(true).build(),
                        contentDescription = stringResource(R.string.rider_photo), contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = AppColors.TextHint, modifier = Modifier.size(40.dp).align(Alignment.Center))
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = rider.riderName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (rider.rating != null) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.Star, null, tint = Color(0xFFFFB300), modifier = Modifier.size(16.dp))
                            Text(String.format("%.1f", rider.rating), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (rider.totalTrips != null) {
                        Text("•", color = AppColors.TextHint)
                        Text(stringResource(R.string.trips_format, rider.totalTrips), style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.LocalShipping, null, tint = AppColors.Primary, modifier = Modifier.size(16.dp))
                    rider.vehicleType?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary) }
                    Text("•", color = AppColors.TextHint)
                    Text(rider.vehicleNumber, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }
            }
            IconButton(onClick = onCallRider, modifier = Modifier.size(48.dp).background(AppColors.Pickup.copy(alpha = 0.1f), CircleShape)) {
                Icon(Icons.Default.Call, stringResource(R.string.call_rider), tint = AppColors.Pickup)
            }
        }
    }
}

// ✅ UPDATED: OtpCard now accepts label, sublabel, and accentColor for reuse with delivery OTP
@Composable
private fun OtpCard(
    otp: String,
    label: String = "Pickup OTP",
    sublabel: String = "Share this OTP with driver for verification",
    accentColor: Color = AppColors.Primary,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier, shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
                .background(brush = Brush.horizontalGradient(colors = listOf(accentColor.copy(alpha = 0.05f), accentColor.copy(alpha = 0.1f))))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Lock, null, tint = accentColor, modifier = Modifier.size(20.dp))
                Text(label, style = MaterialTheme.typography.labelLarge, color = AppColors.TextSecondary)
            }
            Spacer(modifier = Modifier.height(8.dp))

            val otpDigits = otp.filter { it.isDigit() }
            val displayLength = if (otpDigits.length >= 6) 6 else 4
            val displayOtp = otpDigits.take(displayLength).padEnd(displayLength, '-')
            val boxSize = if (displayLength >= 6) 44.dp else 48.dp
            val gapSize = if (displayLength >= 6) 8.dp else 12.dp

            Row(horizontalArrangement = Arrangement.spacedBy(gapSize)) {
                displayOtp.forEach { digit ->
                    Box(
                        modifier = Modifier.size(boxSize).background(accentColor, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(digit.toString(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(sublabel, style = MaterialTheme.typography.labelSmall, color = AppColors.TextHint, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun JourneySummaryCard(
    pickupAddress: SavedAddress,
    dropAddress: SavedAddress,
    fare: Double, // ✅ Int → Double
    waitingCharge: Double = 0.0, // ✅ Int → Double
    currentStatus: BookingStatusType,
    modifier: Modifier = Modifier
) {
    val totalFare = fare + waitingCharge
    InfoCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(10.dp).background(AppColors.Pickup, CircleShape))
            Column(Modifier.weight(1f)) {
                Text(pickupAddress.contactName ?: stringResource(R.string.pickup_fallback), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text(pickupAddress.address.take(40) + if (pickupAddress.address.length > 40) "..." else "", style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary, maxLines = 1)
            }
        }
        Box(Modifier.padding(start = 4.dp, top = 4.dp, bottom = 4.dp).width(2.dp).height(16.dp).background(AppColors.Border))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(10.dp).background(AppColors.Drop, CircleShape))
            Column(Modifier.weight(1f)) {
                Text(dropAddress.contactName ?: stringResource(R.string.drop_fallback), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text(dropAddress.address.take(40) + if (dropAddress.address.length > 40) "..." else "", style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary, maxLines = 1)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Column(modifier = Modifier.fillMaxWidth().background(AppColors.Surface, RoundedCornerShape(8.dp)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Wallet, null, tint = AppColors.Primary, modifier = Modifier.size(20.dp))
                    Text(if (waitingCharge > 0.0) stringResource(R.string.trip_fare_label) else stringResource(R.string.cash_payment), style = MaterialTheme.typography.bodyMedium, color = AppColors.TextSecondary)
                }
                Text(formatRupee(fare), style = MaterialTheme.typography.titleMedium, fontWeight = if (waitingCharge > 0.0) FontWeight.SemiBold else FontWeight.Bold, color = if (waitingCharge > 0.0) AppColors.TextPrimary else AppColors.Primary)
            }
            if (waitingCharge > 0.0) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.Timer, null, tint = AppColors.Error, modifier = Modifier.size(20.dp))
                        Text(stringResource(R.string.waiting_charge_label), style = MaterialTheme.typography.bodyMedium, color = AppColors.Error)
                    }
                    Text(formatRupee(waitingCharge), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = AppColors.Error)
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = AppColors.Border)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.total_cash), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
                    Text(formatRupee(totalFare), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = AppColors.Primary)
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
        stringResource(R.string.cancel_reason_driver_slow), stringResource(R.string.cancel_reason_another_ride),
        stringResource(R.string.cancel_reason_plans_changed), stringResource(R.string.cancel_reason_wrong_location),
        stringResource(R.string.cancel_reason_other)
    )
    AlertDialog(
        onDismissRequest = onDismiss, shape = RoundedCornerShape(20.dp), containerColor = Color.White,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.size(40.dp).background(AppColors.Error.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Cancel, null, tint = AppColors.Error, modifier = Modifier.size(22.dp))
                }
                Text(stringResource(R.string.cancel_booking_title), fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column {
                Text(stringResource(R.string.select_reason), style = MaterialTheme.typography.bodyMedium, color = AppColors.TextSecondary)
                Spacer(modifier = Modifier.height(16.dp))
                reasons.forEach { reason ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { selectedReason = reason }
                            .background(if (selectedReason == reason) AppColors.Primary.copy(alpha = 0.1f) else Color.Transparent)
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selectedReason == reason, onClick = { selectedReason = reason }, colors = RadioButtonDefaults.colors(selectedColor = AppColors.Primary))
                        Spacer(modifier = Modifier.width(4.dp)); Text(reason, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { selectedReason?.let { onConfirm(it) } }, enabled = selectedReason != null,
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Error, disabledContainerColor = AppColors.Border), shape = RoundedCornerShape(12.dp)
            ) { Text(stringResource(R.string.cancel_booking_btn), fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, shape = RoundedCornerShape(12.dp)) { Text(stringResource(R.string.go_back_btn), color = AppColors.TextSecondary, fontWeight = FontWeight.SemiBold) }
        }
    )
}

// ═══════════════════════════════════════════════════════════════════════════════
// MAP
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun RiderMapView(
    pickupAddress: SavedAddress, dropAddress: SavedAddress,
    riderLatitude: Double?, riderLongitude: Double?,
    driverToPickupRoute: List<LatLng>, pickupToDropRoute: List<LatLng>,
    isPrePickup: Boolean, modifier: Modifier = Modifier
) {
    val pickupLatLng = LatLng(pickupAddress.latitude, pickupAddress.longitude)
    val dropLatLng = LatLng(dropAddress.latitude, dropAddress.longitude)
    val riderLatLng = if (riderLatitude != null && riderLongitude != null && riderLatitude != 0.0 && riderLongitude != 0.0) LatLng(riderLatitude, riderLongitude) else null
    val cameraPositionState = rememberCameraPositionState()

    LaunchedEffect(riderLatLng, pickupLatLng, isPrePickup) {
        try {
            val boundsBuilder = LatLngBounds.builder()
            if (isPrePickup) {
                boundsBuilder.include(pickupLatLng)
                if (riderLatLng != null) boundsBuilder.include(riderLatLng)
            } else {
                boundsBuilder.include(pickupLatLng); boundsBuilder.include(dropLatLng)
                if (riderLatLng != null) boundsBuilder.include(riderLatLng)
            }
            cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 120), 500)
        } catch (e: Exception) { cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(pickupLatLng, 14f)) }
    }

    GoogleMap(
        modifier = modifier.clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)),
        cameraPositionState = cameraPositionState,
        properties = MapProperties(mapType = MapType.NORMAL),
        uiSettings = MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = false, mapToolbarEnabled = false, compassEnabled = false)
    ) {
        Marker(state = MarkerState(position = pickupLatLng), title = "Pickup", icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
        Marker(state = MarkerState(position = dropLatLng), title = "Drop", icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
        riderLatLng?.let { Marker(state = MarkerState(position = it), title = "Rider", icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)) }

        if (isPrePickup) {
            if (driverToPickupRoute.isNotEmpty()) Polyline(points = driverToPickupRoute, color = AppColors.Primary, width = 12f)
            else if (riderLatLng != null) Polyline(points = listOf(riderLatLng, pickupLatLng), color = AppColors.Primary.copy(alpha = 0.4f), width = 8f, pattern = listOf(Dash(20f), Gap(10f)))
        } else {
            if (pickupToDropRoute.isNotEmpty()) Polyline(points = pickupToDropRoute, color = AppColors.Primary, width = 12f)
            else Polyline(points = listOf(pickupLatLng, dropLatLng), color = AppColors.Primary.copy(alpha = 0.4f), width = 8f, pattern = listOf(Dash(20f), Gap(10f)))
        }
    }
}