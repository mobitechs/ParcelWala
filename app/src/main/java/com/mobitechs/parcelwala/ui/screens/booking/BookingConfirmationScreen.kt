// ui/screens/booking/BookingConfirmationScreen.kt
package com.mobitechs.parcelwala.ui.screens.booking

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mobitechs.parcelwala.data.model.request.SavedAddress
import com.mobitechs.parcelwala.data.model.response.FareDetails
import com.mobitechs.parcelwala.ui.components.*
import com.mobitechs.parcelwala.ui.theme.AppColors
import com.mobitechs.parcelwala.ui.viewmodel.BookingViewModel

/**
 * Booking Confirmation Screen
 * Shows pickup/drop locations and vehicle selection with CALCULATED FARES
 *
 * Flow (Like Ola/Uber):
 * 1. User enters pickup & drop → API calculates fares for all vehicle types
 * 2. Shows list of vehicles with their actual calculated fares
 * 3. User selects a vehicle → proceeds to review screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingConfirmationScreen(
    pickupAddress: SavedAddress?,
    dropAddress: SavedAddress?,
    preSelectedVehicleId: Int? = null,
    isPrefilledFromOrder: Boolean = false,
    onVehicleSelected: (FareDetails) -> Unit,
    onEditPickup: () -> Unit,
    onEditDrop: () -> Unit,
    onChangePickup: () -> Unit,
    onChangeDrop: () -> Unit,
    onBack: () -> Unit,
    viewModel: BookingViewModel = hiltViewModel()
) {
    var selectedFareDetails by remember { mutableStateOf<FareDetails?>(null) }
    var showLocationDetails by remember { mutableStateOf(false) }
    var hasInitializedPreSelection by remember { mutableStateOf(false) }

    // Observe from ViewModel
    val vehicleFares by viewModel.vehicleFares.collectAsState()
    val isFareLoading by viewModel.isFareLoading.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    // Calculate fares when addresses are available
    LaunchedEffect(pickupAddress, dropAddress) {
        if (pickupAddress != null && dropAddress != null &&
            pickupAddress.latitude != 0.0 && dropAddress.latitude != 0.0
        ) {
            viewModel.setPickupAddress(pickupAddress)
            viewModel.setDropAddress(dropAddress)
        }
    }

    // Auto-select for Book Again flow
    LaunchedEffect(vehicleFares, preSelectedVehicleId) {
        if (!hasInitializedPreSelection && vehicleFares.isNotEmpty() && preSelectedVehicleId != null) {
            vehicleFares.find { it.vehicleTypeId == preSelectedVehicleId }?.let {
                selectedFareDetails = it
                hasInitializedPreSelection = true
            }
        }
    }

    if (pickupAddress == null || dropAddress == null) {
        EmptyState(
            icon = Icons.Default.ErrorOutline,
            title = "Missing Locations",
            subtitle = "Please select pickup and drop locations",
            actionText = "Go Back",
            onAction = onBack,
            modifier = Modifier.fillMaxSize().padding(32.dp)
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Select Vehicle", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        if (isPrefilledFromOrder) {
                            Text("Booking from previous order", style = MaterialTheme.typography.labelSmall, color = AppColors.Primary)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = AppColors.TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = AppColors.Background
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Column(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
                ) {
                    // Book Again Banner
                    if (isPrefilledFromOrder) {
                        BookAgainBanner(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp))
                    }

                    // Compact Journey Summary
                    CompactJourneySummary(
                        pickupAddress = pickupAddress,
                        dropAddress = dropAddress,
                        distanceKm = vehicleFares.firstOrNull()?.distanceKm,
                        estimatedMins = vehicleFares.firstOrNull()?.estimatedDurationMinutes,
                        onViewDetails = { showLocationDetails = true },
                        onEditPickup = onEditPickup,
                        onEditDrop = onEditDrop,
                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Section Header
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Choose Your Ride", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
                            Text(
                                if (preSelectedVehicleId != null) "Pre-selected from your previous order" else "Select the right vehicle for your delivery",
                                style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary
                            )
                        }
                        if (!isFareLoading) {
                            IconButton(onClick = { viewModel.calculateFaresForAllVehicles() }) {
                                Icon(Icons.Default.Refresh, "Refresh", tint = AppColors.Primary)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Loading State
                    AnimatedVisibility(visible = isFareLoading, enter = fadeIn(), exit = fadeOut()) {
                        FareLoadingIndicator(modifier = Modifier.fillMaxWidth().padding(32.dp))
                    }

                    // Vehicle Fares List
                    AnimatedVisibility(visible = !isFareLoading && vehicleFares.isNotEmpty(), enter = fadeIn(), exit = fadeOut()) {
                        Column {
                            vehicleFares.forEach { fareDetails ->
                                VehicleFareCard(
                                    fareDetails = fareDetails,
                                    isSelected = selectedFareDetails?.vehicleTypeId == fareDetails.vehicleTypeId,
                                    isPreSelected = preSelectedVehicleId == fareDetails.vehicleTypeId,
                                    onSelect = {
                                        selectedFareDetails = fareDetails
                                        viewModel.selectFareDetails(fareDetails)
                                    },
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    // Empty State
                    AnimatedVisibility(visible = !isFareLoading && vehicleFares.isEmpty() && uiState.hasFaresLoaded, enter = fadeIn(), exit = fadeOut()) {
                        EmptyState(
                            icon = Icons.Default.DirectionsCar,
                            title = "No Vehicles Available",
                            subtitle = "Please try again or choose different locations",
                            actionText = "Retry",
                            onAction = { viewModel.calculateFaresForAllVehicles() },
                            modifier = Modifier.fillMaxWidth().padding(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Info Card
                    InfoCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Default.Timer, null, tint = AppColors.Primary, modifier = Modifier.size(24.dp))
                            Text("Fare includes 25 mins of free loading/unloading time", style = MaterialTheme.typography.bodyMedium, color = AppColors.TextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(100.dp))
                }

                // Bottom Action Bar
                BottomFareActionBar(
                    selectedFareDetails = selectedFareDetails,
                    onProceed = { selectedFareDetails?.let { onVehicleSelected(it) } },
                    isLoading = isFareLoading
                )
            }

            // Error Snackbar
            uiState.error?.let { error ->
                Snackbar(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp).padding(bottom = 80.dp),
                    action = { TextButton(onClick = { viewModel.clearError() }) { Text("Dismiss") } }
                ) { Text(error) }
            }
        }
    }

    // Location Details Bottom Sheet
    if (showLocationDetails) {
        ModalBottomSheet(onDismissRequest = { showLocationDetails = false }, containerColor = Color.White) {
            LocationDetailsBottomSheet(
                pickupAddress = pickupAddress,
                dropAddress = dropAddress,
                distanceKm = vehicleFares.firstOrNull()?.distanceKm,
                estimatedMins = vehicleFares.firstOrNull()?.estimatedDurationMinutes,
                onDismiss = { showLocationDetails = false }
            )
        }
    }
}

@Composable
private fun CompactJourneySummary(
    pickupAddress: SavedAddress, dropAddress: SavedAddress,
    distanceKm: Double?, estimatedMins: Int?,
    onViewDetails: () -> Unit, onEditPickup: () -> Unit, onEditDrop: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            // Pickup Row
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(12.dp).background(AppColors.Pickup, CircleShape))
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("PICKUP", style = MaterialTheme.typography.labelSmall, color = AppColors.TextHint, letterSpacing = 1.sp)
                    Text(pickupAddress.address, style = MaterialTheme.typography.bodyMedium, color = AppColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                IconButton(onClick = onEditPickup, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, "Edit", tint = AppColors.Primary, modifier = Modifier.size(18.dp))
                }
            }

            Box(modifier = Modifier.padding(start = 5.dp).width(2.dp).height(20.dp).background(AppColors.Border))

            // Drop Row
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(12.dp).background(AppColors.Drop, CircleShape))
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("DROP", style = MaterialTheme.typography.labelSmall, color = AppColors.TextHint, letterSpacing = 1.sp)
                    Text(dropAddress.address, style = MaterialTheme.typography.bodyMedium, color = AppColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                IconButton(onClick = onEditDrop, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, "Edit", tint = AppColors.Primary, modifier = Modifier.size(18.dp))
                }
            }

            // Distance & Time
            if (distanceKm != null || estimatedMins != null) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = AppColors.Divider)
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                    distanceKm?.let {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Route, null, tint = AppColors.Primary, modifier = Modifier.size(18.dp))
                            Text(String.format("%.1f km", it), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = AppColors.TextPrimary)
                        }
                    }
                    estimatedMins?.let {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Timer, null, tint = AppColors.Primary, modifier = Modifier.size(18.dp))
                            Text("$it mins", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = AppColors.TextPrimary)
                        }
                    }
                    TextButton(onClick = onViewDetails, contentPadding = PaddingValues(horizontal = 8.dp)) {
                        Text("Details", style = MaterialTheme.typography.labelMedium, color = AppColors.Primary)
                        Icon(Icons.Default.ChevronRight, null, tint = AppColors.Primary, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun VehicleFareCard(
    fareDetails: FareDetails, isSelected: Boolean, isPreSelected: Boolean = false,
    onSelect: () -> Unit, modifier: Modifier = Modifier
) {
    val alpha by animateFloatAsState(targetValue = if (isSelected) 1f else 0.9f, label = "alpha")

    Card(
        modifier = modifier.alpha(alpha).clickable(onClick = onSelect),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (isSelected) AppColors.Primary.copy(alpha = 0.05f) else Color.White),
        border = when {
            isSelected -> BorderStroke(2.dp, AppColors.Primary)
            isPreSelected -> BorderStroke(2.dp, AppColors.Primary.copy(alpha = 0.4f))
            else -> BorderStroke(1.dp, AppColors.Border)
        },
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 6.dp else 2.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            // Vehicle Icon
            Box(
                modifier = Modifier.size(56.dp).background(
                    color = if (isSelected) AppColors.Primary.copy(alpha = 0.15f) else AppColors.Background,
                    shape = RoundedCornerShape(12.dp)
                ),
                contentAlignment = Alignment.Center
            ) {
                Text(fareDetails.vehicleTypeIcon, style = MaterialTheme.typography.headlineMedium)
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Vehicle Info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(fareDetails.vehicleTypeName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (isSelected) AppColors.Primary else AppColors.TextPrimary)
                    if (isPreSelected) {
                        Surface(color = AppColors.Primary.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp)) {
                            Text("Previous", style = MaterialTheme.typography.labelSmall, color = AppColors.Primary, fontWeight = FontWeight.Medium, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                    if (fareDetails.hasSurgePricing()) {
                        Surface(color = Color(0xFFFFE0B2), shape = RoundedCornerShape(4.dp)) {
                            Row(modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                Icon(Icons.Default.TrendingUp, "Surge", tint = Color(0xFFE65100), modifier = Modifier.size(12.dp))
                                Text("${fareDetails.getSurgePercentage()}%", style = MaterialTheme.typography.labelSmall, color = Color(0xFFE65100), fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(fareDetails.capacity, style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.Timer, null, tint = AppColors.TextHint, modifier = Modifier.size(14.dp))
                    Text(fareDetails.getEtaText(), style = MaterialTheme.typography.bodySmall, color = AppColors.TextHint)
                }
            }

            // Fare
            Column(horizontalAlignment = Alignment.End) {
                Text(fareDetails.getDisplayFare(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = if (isSelected) AppColors.Primary else AppColors.TextPrimary)
                if (fareDetails.discount > 0) {
                    Text("₹${fareDetails.totalFare.toInt()}", style = MaterialTheme.typography.bodySmall.copy(textDecoration = TextDecoration.LineThrough), color = AppColors.TextHint)
                }
                if (fareDetails.tollCharges > 0) {
                    Text("incl. toll", style = MaterialTheme.typography.labelSmall, color = AppColors.TextHint)
                }
            }

            Spacer(modifier = Modifier.width(8.dp))
            RadioButton(selected = isSelected, onClick = onSelect, colors = RadioButtonDefaults.colors(selectedColor = AppColors.Primary, unselectedColor = AppColors.Border))
        }
    }
}

@Composable
private fun FareLoadingIndicator(modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        CircularProgressIndicator(color = AppColors.Primary, modifier = Modifier.size(48.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text("Calculating fares...", style = MaterialTheme.typography.bodyLarge, color = AppColors.TextSecondary)
        Spacer(modifier = Modifier.height(4.dp))
        Text("Finding the best prices for you", style = MaterialTheme.typography.bodySmall, color = AppColors.TextHint)
    }
}

@Composable
private fun BottomFareActionBar(selectedFareDetails: FareDetails?, onProceed: () -> Unit, isLoading: Boolean) {
    Surface(color = Color.White, shadowElevation = 12.dp) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            selectedFareDetails?.let { fare ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(fare.vehicleTypeName, style = MaterialTheme.typography.labelMedium, color = AppColors.TextSecondary)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(fare.getDisplayFare(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = AppColors.Primary)
                            if (fare.discount > 0) {
                                Surface(color = AppColors.Pickup.copy(alpha = 0.1f), shape = RoundedCornerShape(4.dp)) {
                                    Text("₹${fare.discount.toInt()} off", style = MaterialTheme.typography.labelSmall, color = AppColors.Pickup, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                        }
                    }
                    PrimaryButton(text = "Proceed", onClick = onProceed, icon = Icons.Default.ArrowForward, modifier = Modifier.width(150.dp), enabled = !isLoading)
                }
            } ?: run {
                Text(if (isLoading) "Calculating fares..." else "Select a vehicle to proceed", style = MaterialTheme.typography.bodyMedium, color = AppColors.TextHint, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun BookAgainBanner(modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = AppColors.Primary.copy(alpha = 0.1f))) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Default.Refresh, "Book Again", tint = AppColors.Primary, modifier = Modifier.size(24.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Booking Again", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = AppColors.Primary)
                Text("Details pre-filled from your previous order", style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
            }
        }
    }
}

@Composable
private fun LocationDetailsBottomSheet(pickupAddress: SavedAddress, dropAddress: SavedAddress, distanceKm: Double?, estimatedMins: Int?, onDismiss: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Trip Details", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
            IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Close", tint = AppColors.TextSecondary) }
        }
        Spacer(modifier = Modifier.height(24.dp))
        LocationDetailItem(pickupAddress, "Pickup", AppColors.Pickup)
        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = AppColors.Divider)
        Spacer(modifier = Modifier.height(16.dp))
        LocationDetailItem(dropAddress, "Drop", AppColors.Drop)
        Spacer(modifier = Modifier.height(24.dp))
        if (distanceKm != null || estimatedMins != null) {
            InfoCard {
                Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                    distanceKm?.let { InfoStatItem(Icons.Default.Route, String.format("%.1f km", it), "Distance") }
                    estimatedMins?.let { InfoStatItem(Icons.Default.Timer, "$it mins", "Est. Time") }
                    InfoStatItem(Icons.Default.Speed, "Fastest", "Route")
                }
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun LocationDetailItem(address: SavedAddress, type: String, color: Color) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(if (type == "Pickup") Icons.Default.TripOrigin else Icons.Default.LocationOn, null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(type, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = color)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(address.address, style = MaterialTheme.typography.bodyMedium, color = AppColors.TextPrimary)
        address.contactName?.let { name ->
            Spacer(modifier = Modifier.height(4.dp))
            Text("$name • ${address.contactPhone ?: ""}", style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
        }
    }
}

@Composable
private fun InfoStatItem(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, label, tint = AppColors.Primary, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
        Text(label, style = MaterialTheme.typography.labelSmall, color = AppColors.TextSecondary)
    }
}