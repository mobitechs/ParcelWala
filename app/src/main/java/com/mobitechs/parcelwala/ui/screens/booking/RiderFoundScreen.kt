// ui/screens/booking/RiderFoundScreen.kt
package com.mobitechs.parcelwala.ui.screens.booking

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.*
import com.mobitechs.parcelwala.data.model.realtime.RiderInfo
import com.mobitechs.parcelwala.data.model.request.SavedAddress
import com.mobitechs.parcelwala.ui.components.InfoCard
import com.mobitechs.parcelwala.ui.theme.AppColors
import com.mobitechs.parcelwala.ui.viewmodel.RiderTrackingViewModel
import kotlinx.coroutines.delay

/**
 * ════════════════════════════════════════════════════════════════════════════
 * RIDER FOUND SCREEN
 * ════════════════════════════════════════════════════════════════════════════
 *
 * Displayed when a rider is assigned to the booking.
 * Shows:
 * - Rider details (photo, name, rating, vehicle)
 * - OTP for pickup verification
 * - Live ETA updates
 * - Map with rider location
 * - Call/Message rider buttons
 * - Cancel option (calls API via callback)
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
    onCancelBooking: (String) -> Unit,  // ✅ Changed: Now takes reason string
    onContactSupport: () -> Unit,
    viewModel: RiderTrackingViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    // State from ViewModel
    val rider by viewModel.assignedRider.collectAsState()
    val riderLocation by viewModel.riderLocation.collectAsState()
    val otp by viewModel.bookingOtp.collectAsState()

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
                            text = "Rider Assigned! 🎉",
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
                .verticalScroll(rememberScrollState())
        ) {
            // MAP WITH RIDER LOCATION
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            ) {
                RiderMapView(
                    pickupAddress = pickupAddress,
                    riderLatitude = riderLocation?.latitude ?: rider?.currentLatitude,
                    riderLongitude = riderLocation?.longitude ?: rider?.currentLongitude,
                    modifier = Modifier.fillMaxSize()
                )

                // ETA Overlay
                rider?.let { r ->
                    val etaMinutes = riderLocation?.etaMinutes ?: r.etaMinutes
                    if (etaMinutes > 0) {
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
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = null,
                                    tint = AppColors.Primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Column {
                                    Text(
                                        text = "$etaMinutes min",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = AppColors.Primary
                                    )
                                    Text(
                                        text = "arriving at pickup",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AppColors.TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

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
                                data = Uri.parse("tel:${r.phone}")
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

            // JOURNEY SUMMARY
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn() + slideInVertically { it / 2 }
            ) {
                JourneySummaryCard(
                    pickupAddress = pickupAddress,
                    dropAddress = dropAddress,
                    fare = fare,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ACTION BUTTONS
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
                    border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.Primary)
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
                    border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.Drop)
                ) {
                    Icon(Icons.Default.Close, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Cancel", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Cancel Confirmation Dialog
    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFFF9800),
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text("Cancel Trip?", fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "A rider has already been assigned. Are you sure you want to cancel?",
                    color = AppColors.TextSecondary,
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCancelDialog = false
                        onCancelBooking("Cancelled after rider assigned")  // ✅ Pass reason
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Drop)
                ) {
                    Text("Yes, Cancel")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text("Keep Trip", color = AppColors.Primary)
                }
            },
            containerColor = Color.White
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// HELPER COMPOSABLES
// ═══════════════════════════════════════════════════════════════════════════

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
                if (rider.photo != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(rider.photo)
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
                        modifier = Modifier.size(40.dp).align(Alignment.Center)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = rider.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Star, null, tint = Color(0xFFFFB300), modifier = Modifier.size(16.dp))
                        Text(String.format("%.1f", rider.rating), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                    Text("•", color = AppColors.TextHint)
                    Text("${rider.totalTrips} trips", style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.LocalShipping, null, tint = AppColors.Primary, modifier = Modifier.size(16.dp))
                    Text(rider.vehicleType, style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
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
                        colors = listOf(AppColors.Primary.copy(alpha = 0.05f), AppColors.Primary.copy(alpha = 0.1f))
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

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                otp.forEach { digit ->
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(AppColors.Primary, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(digit.toString(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.White)
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
    modifier: Modifier = Modifier
) {
    InfoCard(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(Modifier.size(10.dp).background(AppColors.Pickup, CircleShape))
            Column(Modifier.weight(1f)) {
                Text(pickupAddress.contactName ?: "Pickup", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text(pickupAddress.address.take(40) + if (pickupAddress.address.length > 40) "..." else "", style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary, maxLines = 1)
            }
        }

        Box(Modifier.padding(start = 4.dp, top = 4.dp, bottom = 4.dp).width(2.dp).height(16.dp).background(AppColors.Border))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(Modifier.size(10.dp).background(AppColors.Drop, CircleShape))
            Column(Modifier.weight(1f)) {
                Text(dropAddress.contactName ?: "Drop", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text(dropAddress.address.take(40) + if (dropAddress.address.length > 40) "..." else "", style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary, maxLines = 1)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth().background(AppColors.Surface, RoundedCornerShape(8.dp)).padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Wallet, null, tint = AppColors.Primary, modifier = Modifier.size(20.dp))
                Text("Cash Payment", style = MaterialTheme.typography.bodyMedium, color = AppColors.TextSecondary)
            }
            Text("₹$fare", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = AppColors.Primary)
        }
    }
}

@Composable
private fun RiderMapView(
    pickupAddress: SavedAddress,
    riderLatitude: Double?,
    riderLongitude: Double?,
    modifier: Modifier = Modifier
) {
    val pickupLatLng = LatLng(pickupAddress.latitude, pickupAddress.longitude)
    val riderLatLng = if (riderLatitude != null && riderLongitude != null) LatLng(riderLatitude, riderLongitude) else null
    val cameraPositionState = rememberCameraPositionState()

    LaunchedEffect(riderLatLng, pickupLatLng) {
        try {
            if (riderLatLng != null) {
                val bounds = LatLngBounds.builder().include(pickupLatLng).include(riderLatLng).build()
                cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(bounds, 100), 500)
            } else {
                cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(pickupLatLng, 15f))
            }
        } catch (e: Exception) {
            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(pickupLatLng, 14f))
        }
    }

    GoogleMap(
        modifier = modifier.clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)),
        cameraPositionState = cameraPositionState,
        properties = MapProperties(mapType = MapType.NORMAL),
        uiSettings = MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = false, mapToolbarEnabled = false, compassEnabled = false)
    ) {
        Marker(MarkerState(pickupLatLng).toString(), title = "Pickup", icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
        riderLatLng?.let { Marker(MarkerState(it).toString(), title = "Rider", icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)) }
    }
}