package com.mobitechs.parcelwala.ui.screens.booking

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.maps.model.LatLng
import com.mobitechs.parcelwala.data.model.request.SavedAddress
import com.mobitechs.parcelwala.data.model.response.formatRupee
import com.mobitechs.parcelwala.ui.tracking.DeliveryCompleteSheet
import com.mobitechs.parcelwala.ui.theme.AppColors
import com.mobitechs.parcelwala.ui.tracking.ConnectionBanner
import com.mobitechs.parcelwala.ui.tracking.DriverRow
import com.mobitechs.parcelwala.ui.tracking.OtpBlock
import com.mobitechs.parcelwala.ui.tracking.OtpPendingBlock
import com.mobitechs.parcelwala.ui.tracking.ShareTracking
import com.mobitechs.parcelwala.ui.tracking.StalledBanner
import com.mobitechs.parcelwala.ui.tracking.TrackingHeadline
import com.mobitechs.parcelwala.ui.tracking.TrackingMap
import com.mobitechs.parcelwala.ui.tracking.TrackingPhase
import com.mobitechs.parcelwala.ui.tracking.TrackingStepper
import com.mobitechs.parcelwala.ui.tracking.WaitingBlock
import com.mobitechs.parcelwala.ui.tracking.headlineFor
import com.mobitechs.parcelwala.ui.viewmodel.RiderTrackingViewModel

/**
 * ════════════════════════════════════════════════════════════════════════════
 * TRACKING SCREEN
 * ════════════════════════════════════════════════════════════════════════════
 *
 * Rewritten around BottomSheetScaffold. Two structural changes:
 *
 * 1. THE MAP IS NO LONGER INSIDE A verticalScroll.
 *    The old layout wrapped everything — including a 300dp GoogleMap — in a
 *    scrolling Column. A MapView inside a scrollable parent fights for drag
 *    gestures: the user tries to pan the map and the page scrolls instead, or
 *    the other way round. Full-bleed map behind a sheet solves that outright,
 *    and gives the peek/expand affordance for free.
 *
 * 2. PAYMENT IS A SHEET, NOT A DESTINATION.
 *    ARRIVED_DELIVERY used to navigate to a full-screen payment route, which
 *    destroyed the map at the most anxious moment of the trip, then popped back
 *    for the rating dialog: map → payment → map → dialog. Neither Rapido nor
 *    Porter navigate here. The map stays; payment rises over it.
 *
 * The screen computes no status booleans of its own — everything comes from
 * `viewModel.ui`, which is what stops the two journey legs from ever bleeding
 * into each other again.
 */
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
    val haptics = LocalHapticFeedback.current

    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val rider by viewModel.assignedRider.collectAsStateWithLifecycle()
    val pickupOtp by viewModel.pickupOtp.collectAsStateWithLifecycle()
    val deliveryOtp by viewModel.deliveryOtp.collectAsStateWithLifecycle()
    val waiting by viewModel.waitingState.collectAsStateWithLifecycle()
    val rating by viewModel.ratingState.collectAsStateWithLifecycle()
    val payment by viewModel.paymentState.collectAsStateWithLifecycle()

    var showCancelDialog by remember { mutableStateOf(false) }
    var recenterSignal by remember { mutableIntStateOf(0) }
    var showRecenter by remember { mutableStateOf(false) }

    // A single haptic tap on phase change. Costs nothing, feels expensive, and
    // it is the cheapest way to make an arrival register while the phone is in
    // the customer's hand but not their attention.
    LaunchedEffect(ui.phase) {
        if (ui.phase == TrackingPhase.DRIVER_WAITING || ui.phase == TrackingPhase.AT_DROP) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.PartiallyExpanded,
            skipHiddenState = true
        )
    )

    // FIX — the OTP was below the fold at the exact moment it had to be read
    // aloud. The customer had to discover that the sheet could be dragged, at
    // the one point in the trip where a driver is standing in front of them
    // waiting. The sheet now opens itself when an OTP becomes the primary
    // action, and settles back once it is no longer needed.
    LaunchedEffect(ui.phase) {
        when (ui.phase) {
            TrackingPhase.DRIVER_WAITING, TrackingPhase.AT_DROP ->
                runCatching { scaffoldState.bottomSheetState.expand() }
            else ->
                runCatching { scaffoldState.bottomSheetState.partialExpand() }
        }
    }

    val pickupLatLng = remember(pickupAddress) {
        LatLng(pickupAddress.latitude, pickupAddress.longitude)
    }
    val dropLatLng = remember(dropAddress) {
        LatLng(dropAddress.latitude, dropAddress.longitude)
    }

    val headline = headlineFor(
        phase = ui.phase,
        driverName = rider?.riderName ?: "Your driver",
        etaMinutes = ui.etaMinutes,
        distanceLabel = viewModel.formatDistance(ui.distanceKm),
        waiting = waiting,
        deliveryOtp = deliveryOtp
    )

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 300.dp,
        sheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        sheetContainerColor = Color.White,
        sheetTonalElevation = 0.dp,
        sheetContent = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 20.dp)
                    .navigationBarsPadding()
            ) {
                ConnectionBanner(
                    isConnected = ui.isConnected,
                    secondsSinceLastFix = ui.secondsSinceLastFix,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // ── The one big number ─────────────────────────────────────
                TrackingHeadline(headline)
                Spacer(Modifier.height(14.dp))
                TrackingStepper(ui.phase, Modifier.fillMaxWidth())
                Spacer(Modifier.height(16.dp))

                StalledBanner(
                    visible = ui.isDriverStalled,
                    driverName = rider?.riderName ?: "Your driver",
                    onCall = { rider?.riderPhone?.let { dial(context, it) } }
                )
                if (ui.isDriverStalled) Spacer(Modifier.height(12.dp))

                // ── Driver ─────────────────────────────────────────────────
                rider?.let {
                    DriverRow(rider = it, onCall = { dial(context, it.riderPhone) })
                    Spacer(Modifier.height(16.dp))
                }

                // ── Waiting timer, pickup phase only ───────────────────────
                AnimatedVisibility(
                    visible = ui.showWaitingTimer,
                    enter = fadeIn(tween(250)) + expandVertically(tween(250)),
                    exit = fadeOut(tween(150)) + shrinkVertically(tween(150))
                ) {
                    Column {
                        WaitingBlock(waiting)
                        Spacer(Modifier.height(14.dp))
                    }
                }

                // ── OTP. Large exactly when it must be read aloud. ─────────
                AnimatedVisibility(
                    visible = ui.showPickupOtp,
                    enter = fadeIn(tween(300)) + expandVertically(tween(300)),
                    exit = fadeOut(tween(150)) + shrinkVertically(tween(150))
                ) {
                    Column {
                        OtpBlock(
                            otp = pickupOtp.orEmpty(),
                            label = "Pickup OTP",
                            sublabel = "Share this with your driver to start the trip",
                            accent = AppColors.Primary,
                            isPrimary = ui.phase == TrackingPhase.DRIVER_WAITING
                        )
                        Spacer(Modifier.height(14.dp))
                    }
                }

                AnimatedVisibility(
                    visible = ui.showDeliveryOtp,
                    enter = fadeIn(tween(300)) + expandVertically(tween(300)),
                    exit = fadeOut(tween(150)) + shrinkVertically(tween(150))
                ) {
                    Column {
                        OtpBlock(
                            otp = deliveryOtp.orEmpty(),
                            label = "Delivery OTP",
                            sublabel = "The receiver shares this to complete delivery",
                            accent = AppColors.Drop,
                            isPrimary = ui.phase == TrackingPhase.AT_DROP
                        )
                        Spacer(Modifier.height(14.dp))
                    }
                }

                // Placeholder while the server has not sent the OTP yet — the
                // lean reconnect payload omits it, so this window is real.
                AnimatedVisibility(
                    visible = ui.isDeliveryOtpPending,
                    enter = fadeIn(tween(300)),
                    exit = fadeOut(tween(150))
                ) {
                    Column {
                        OtpPendingBlock()
                        Spacer(Modifier.height(14.dp))
                    }
                }

                JourneyBlock(
                    pickupAddress = pickupAddress,
                    dropAddress = dropAddress,
                    fare = fare,
                    waitingCharge = waiting.waitingCharge,
                    phase = ui.phase
                )
                Spacer(Modifier.height(16.dp))

                // ── Exactly one primary action per phase ───────────────────
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onContactSupport,
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.Primary),
                        border = BorderStroke(1.dp, AppColors.Primary)
                    ) {
                        Icon(Icons.Default.Headset, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Support", fontWeight = FontWeight.Bold)
                    }
                    if (ui.canCancel) {
                        OutlinedButton(
                            onClick = { showCancelDialog = true },
                            modifier = Modifier.weight(1f).height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.Drop),
                            border = BorderStroke(1.dp, AppColors.Drop)
                        ) {
                            Icon(Icons.Default.Close, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Cancel", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Say WHY cancelling is gone rather than silently removing it.
                if (!ui.canCancel && ui.phase != TrackingPhase.COMPLETING &&
                    ui.phase != TrackingPhase.SEARCHING
                ) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "This trip can no longer be cancelled — your parcel is with the driver.",
                        style = MaterialTheme.typography.labelSmall,
                        color = AppColors.TextHint,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            TrackingMap(
                phase = ui.phase,
                pickup = pickupLatLng,
                drop = dropLatLng,
                driver = ui.driverLatLng,
                bearing = ui.driverBearing,
                route = ui.activeRoute,
                vehicleType = rider?.vehicleType,
                recenterSignal = recenterSignal,
                onUserPannedMap = { showRecenter = true },
                modifier = Modifier.fillMaxSize()
            )

            // Recentre appears only after the user has panned — the camera
            // never fights them, but getting back is one tap.
            AnimatedVisibility(
                visible = showRecenter,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(Color.White, CircleShape)
                        .clickable {
                            recenterSignal++
                            showRecenter = false
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.MyLocation, "Recentre map",
                        tint = AppColors.Primary, modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Share tracking. Fixes the Share action in the old TopAppBar,
            // which was wired to an empty lambda. The person waiting at the
            // DROP end currently has no visibility at all — they call the
            // sender, who calls your support line.
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 66.dp, end = 16.dp)
                    .size(42.dp)
                    .background(Color.White, CircleShape)
                    .clickable {
                        ShareTracking.share(
                            context = context,
                            bookingId = bookingId,
                            rider = rider,
                            etaMinutes = ui.etaMinutes,
                            deliveryOtp = deliveryOtp
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Share, "Share tracking",
                    tint = AppColors.Primary, modifier = Modifier.size(19.dp)
                )
            }

            // Quiet route-loading chip. We show this INSTEAD of drawing a
            // straight line through buildings, which is what the old fallback
            // did whenever the polyline had not arrived.
            AnimatedVisibility(
                visible = ui.isRouteLoading && ui.activeRoute.isEmpty(),
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.TopStart).padding(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White)
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(13.dp),
                        strokeWidth = 2.dp,
                        color = AppColors.Primary
                    )
                    Text(
                        "Getting route…",
                        style = MaterialTheme.typography.labelSmall,
                        color = AppColors.TextSecondary
                    )
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PAYMENT — over the map, never replacing it
    // ═══════════════════════════════════════════════════════════════════════
    if (ui.showPaymentSheet) {
        PaymentOverlaySheet(
            totalFare = payment.totalFare,
            baseFare = payment.baseFare,
            waitingCharge = payment.waitingCharge,
            discount = payment.discount,
            deliveryOtp = deliveryOtp,
            onPayOnline = { viewModel.onPaymentCompleted() },
            onPayCash = { viewModel.onCashPaymentConfirmed() }
        )
    }

    if (payment.isVerifyingPayment && !rating.showRatingDialog) {
        Dialog(
            onDismissRequest = { },
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(30.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    CircularProgressIndicator(color = AppColors.Primary, strokeWidth = 3.dp)
                    Text(
                        "Confirming payment",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Just a moment…",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextSecondary
                    )
                }
            }
        }
    }

    // Rating renders last so it always sits above the spinner. onDismiss is a
    // no-op on purpose — Compose fires onDismissRequest on the first outside
    // touch, and on some devices on first composition, which used to make the
    // dialog vanish the instant it appeared.
    if (rating.showRatingDialog) {
        DeliveryCompleteSheet(
            bookingId = bookingId,
            totalPaid = rating.totalFare,
            paymentMethod = payment.paymentMethod,
            driverName = rating.driverName,
            waitingCharge = rating.waitingCharge,
            isSubmitting = rating.isSubmitting,
            onSubmit = { stars, feedback ->
                viewModel.submitRating(rating.bookingId, stars, feedback)
            },
            onSkip = { viewModel.skipRating() }
        )
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

// ═══════════════════════════════════════════════════════════════════════════
// PAYMENT OVERLAY
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Drawn as a Dialog anchored to the bottom rather than a navigation
 * destination, so the live map is still visible behind it. This is the single
 * biggest fix for the confusion at the delivery point.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaymentOverlaySheet(
    totalFare: Double,
    baseFare: Double,
    waitingCharge: Double,
    discount: Double,
    deliveryOtp: String?,
    onPayOnline: () -> Unit,
    onPayCash: () -> Unit
) {
    Dialog(
        onDismissRequest = { },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(Color.White)
                    .padding(20.dp)
                    .navigationBarsPadding()
            ) {
                Box(
                    Modifier
                        .width(38.dp).height(4.dp)
                        .background(AppColors.DragHandle, RoundedCornerShape(2.dp))
                        .align(Alignment.CenterHorizontally)
                )
                Spacer(Modifier.height(16.dp))

                Text(
                    "Amount to pay",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextSecondary
                )
                Text(
                    formatRupee(totalFare),
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary
                )
                Spacer(Modifier.height(14.dp))

                HorizontalDivider(color = AppColors.Border)
                Spacer(Modifier.height(10.dp))
                if (baseFare > 0) FareRow("Trip fare", formatRupee(baseFare))
                if (waitingCharge > 0) FareRow("Waiting charges", formatRupee(waitingCharge))
                if (discount > 0) {
                    FareRow("Discount", "− ${formatRupee(discount)}", AppColors.Pickup)
                }

                deliveryOtp?.takeIf { it.isNotBlank() }?.let {
                    Spacer(Modifier.height(14.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(AppColors.Drop.copy(alpha = 0.08f))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Delivery OTP",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.TextSecondary
                        )
                        Text(
                            it,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.Drop
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onPayCash,
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.TextPrimary),
                        border = BorderStroke(1.dp, AppColors.Border)
                    ) {
                        Text("Paid cash", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onPayOnline,
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary)
                    ) {
                        Text("Pay online", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun FareRow(label: String, amount: String, color: Color = AppColors.TextPrimary) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
        Text(
            amount,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// JOURNEY BLOCK
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun JourneyBlock(
    pickupAddress: SavedAddress,
    dropAddress: SavedAddress,
    fare: Double,
    waitingCharge: Double,
    phase: TrackingPhase
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(AppColors.Gray50)
            .padding(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(Modifier.padding(top = 5.dp).size(9.dp).background(AppColors.Pickup, CircleShape))
            Column(Modifier.weight(1f)) {
                Text(
                    pickupAddress.contactName ?: "Pickup",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.TextPrimary
                )
                Text(
                    pickupAddress.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextSecondary,
                    maxLines = 1
                )
            }
        }
        Box(
            Modifier
                .padding(start = 4.dp, top = 4.dp, bottom = 4.dp)
                .width(1.dp).height(14.dp)
                .background(AppColors.Border)
        )
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(Modifier.padding(top = 5.dp).size(9.dp).background(AppColors.Drop, CircleShape))
            Column(Modifier.weight(1f)) {
                Text(
                    dropAddress.contactName ?: "Delivery",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.TextPrimary
                )
                Text(
                    dropAddress.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextSecondary,
                    maxLines = 1
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        HorizontalDivider(color = AppColors.Border)
        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (waitingCharge > 0) "Fare + waiting" else "Total fare",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextSecondary
            )
            Text(
                formatRupee(fare + waitingCharge),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = AppColors.Primary
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// CANCEL DIALOG
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun CancelBookingDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var selected by remember { mutableStateOf<String?>(null) }
    val reasons = listOf(
        "Driver is taking too long",
        "Booked another ride",
        "My plans changed",
        "Wrong pickup location",
        "Other"
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
                        .size(38.dp)
                        .background(AppColors.Error.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Cancel, null,
                        tint = AppColors.Error, modifier = Modifier.size(20.dp)
                    )
                }
                Text("Cancel booking?", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column {
                Text(
                    "Tell us why so we can improve",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextSecondary
                )
                Spacer(Modifier.height(12.dp))
                reasons.forEach { reason ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { selected = reason }
                            .background(
                                if (selected == reason) AppColors.Primary.copy(alpha = 0.08f)
                                else Color.Transparent
                            )
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selected == reason,
                            onClick = { selected = reason },
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
                onClick = { selected?.let(onConfirm) },
                enabled = selected != null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.Error,
                    disabledContainerColor = AppColors.Border
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cancel booking", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, shape = RoundedCornerShape(12.dp)) {
                Text("Go back", color = AppColors.TextSecondary, fontWeight = FontWeight.SemiBold)
            }
        }
    )
}

private fun dial(context: android.content.Context, phone: String) {
    if (phone.isBlank()) return
    runCatching {
        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
    }
}
