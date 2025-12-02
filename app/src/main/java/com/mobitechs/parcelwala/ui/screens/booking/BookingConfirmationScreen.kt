// ui/screens/booking/BookingConfirmationScreen.kt
package com.mobitechs.parcelwala.ui.screens.booking

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mobitechs.parcelwala.data.model.request.SavedAddress
import com.mobitechs.parcelwala.data.model.response.VehicleTypeResponse
import com.mobitechs.parcelwala.ui.components.*
import com.mobitechs.parcelwala.ui.theme.AppColors
import com.mobitechs.parcelwala.ui.viewmodel.BookingViewModel

/**
 * Booking Confirmation Screen
 * Shows pickup/drop locations and vehicle selection
 * Supports Book Again flow with pre-filled data
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingConfirmationScreen(
    pickupAddress: SavedAddress?,
    dropAddress: SavedAddress?,
    preSelectedVehicleId: Int? = null,
    isPrefilledFromOrder: Boolean = false,
    onVehicleSelected: (VehicleTypeResponse) -> Unit,
    onEditPickup: () -> Unit,
    onEditDrop: () -> Unit,
    onChangePickup: () -> Unit,
    onChangeDrop: () -> Unit,
    onBack: () -> Unit,
    viewModel: BookingViewModel = hiltViewModel()
) {
    var selectedVehicle by remember { mutableStateOf<VehicleTypeResponse?>(null) }
    var showLocationDetails by remember { mutableStateOf(false) }
    var hasInitializedPreSelection by remember { mutableStateOf(false) }

    // Observe vehicle types from ViewModel
    val vehicleTypes by viewModel.vehicleTypes.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    // Load vehicle types on first composition
    LaunchedEffect(Unit) {
        if (vehicleTypes.isEmpty()) {
            viewModel.loadVehicleTypes()
        }
    }

    // Auto-select vehicle for Book Again flow
    LaunchedEffect(vehicleTypes, preSelectedVehicleId) {
        if (!hasInitializedPreSelection &&
            vehicleTypes.isNotEmpty() &&
            preSelectedVehicleId != null
        ) {
            val preSelected = vehicleTypes.find { it.vehicleTypeId == preSelectedVehicleId }
            if (preSelected != null) {
                selectedVehicle = preSelected
                hasInitializedPreSelection = true
            }
        }
    }

    if (pickupAddress == null || dropAddress == null) {
        // Show error state
        EmptyState(
            icon = Icons.Default.ErrorOutline,
            title = "Missing Locations",
            subtitle = "Please select pickup and drop locations",
            actionText = "Go Back",
            onAction = onBack,
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Confirm Booking",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (isPrefilledFromOrder) {
                            Text(
                                text = "Booking from previous order",
                                style = MaterialTheme.typography.labelSmall,
                                color = AppColors.Primary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = AppColors.TextPrimary
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading && vehicleTypes.isEmpty()) {
                // Show loading state
                LoadingIndicator(
                    message = "Loading vehicles...",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp)
                )
            } else {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Scrollable Content
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Book Again Banner
                        if (isPrefilledFromOrder) {
                            BookAgainBanner(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }

                        // Journey Summary Card
                        JourneySummaryCard(
                            pickupAddress = pickupAddress,
                            dropAddress = dropAddress,
                            onEditPickup = onEditPickup,
                            onEditDrop = onEditDrop,
                            onChangePickup = onChangePickup,
                            onChangeDrop = onChangeDrop,
                            onViewDetails = { showLocationDetails = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Select Vehicle Section
                        SectionHeader(
                            text = "Select Vehicle",
                            subtitle = if (preSelectedVehicleId != null)
                                "Pre-selected from your previous order"
                            else
                                "Choose the right vehicle for your delivery",
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Vehicle Options from API
                        if (vehicleTypes.isEmpty()) {
                            // Empty state
                            EmptyState(
                                icon = Icons.Default.DirectionsCar,
                                title = "No Vehicles Available",
                                subtitle = "Please try again later",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp)
                            )
                        } else {
                            vehicleTypes.forEach { vehicle ->
                                VehicleOptionCard(
                                    vehicle = vehicle,
                                    isSelected = selectedVehicle?.vehicleTypeId == vehicle.vehicleTypeId,
                                    isPreSelected = preSelectedVehicleId == vehicle.vehicleTypeId,
                                    onSelect = { selectedVehicle = vehicle },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Information Card
                        InfoCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = AppColors.Primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = "Fare includes 25 mins of free loading/unloading time",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = AppColors.TextSecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(80.dp))
                    }

                    // Bottom Button
                    Surface(
                        color = Color.White,
                        shadowElevation = 8.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            selectedVehicle?.let { vehicle ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Estimated Fare",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = AppColors.TextSecondary
                                        )
                                        Text(
                                            text = "₹${vehicle.basePrice}",
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = AppColors.Primary
                                        )
                                    }

                                    PrimaryButton(
                                        text = "Proceed",
                                        onClick = { onVehicleSelected(vehicle) },
                                        icon = Icons.Default.ArrowForward,
                                        modifier = Modifier.width(150.dp)
                                    )
                                }
                            } ?: run {
                                Text(
                                    text = "Please select a vehicle to proceed",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = AppColors.TextHint,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }

            // Show error snackbar
            uiState.error?.let { error ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("Dismiss")
                        }
                    }
                ) {
                    Text(error)
                }
            }
        }
    }

    // Location Details Bottom Sheet
    if (showLocationDetails) {
        ModalBottomSheet(
            onDismissRequest = { showLocationDetails = false },
            containerColor = Color.White
        ) {
            LocationDetailsBottomSheet(
                pickupAddress = pickupAddress,
                dropAddress = dropAddress,
                onDismiss = { showLocationDetails = false }
            )
        }
    }
}

/**
 * Book Again Banner
 */
@Composable
private fun BookAgainBanner(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = AppColors.Primary.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Book Again",
                tint = AppColors.Primary,
                modifier = Modifier.size(24.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Booking Again",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.Primary
                )
                Text(
                    text = "Details pre-filled from your previous order. You can modify if needed.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextSecondary
                )
            }
        }
    }
}

/**
 * Journey Summary Card with Pickup and Drop
 */
@Composable
private fun JourneySummaryCard(
    pickupAddress: SavedAddress,
    dropAddress: SavedAddress,
    onEditPickup: () -> Unit,
    onEditDrop: () -> Unit,
    onChangePickup: () -> Unit,
    onChangeDrop: () -> Unit,
    onViewDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    InfoCard(
        modifier = modifier,
        elevation = 4.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Journey Details",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary
            )

            TextButton(onClick = onViewDetails) {
                Text(
                    text = "View Details",
                    color = AppColors.Primary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = AppColors.Primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Pickup Location
        LocationRow(
            address = pickupAddress,
            locationType = "pickup",
            onEditDetails = onEditPickup,
            onChangeLocation = onChangePickup
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Journey Connector
        Box(
            modifier = Modifier
                .padding(start = 20.dp)
                .width(2.dp)
                .height(24.dp)
                .background(AppColors.Border)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Drop Location
        LocationRow(
            address = dropAddress,
            locationType = "drop",
            onEditDetails = onEditDrop,
            onChangeLocation = onChangeDrop
        )
    }
}

/**
 * Location Row with Icon, Address, Edit and Change buttons
 */
@Composable
private fun LocationRow(
    address: SavedAddress,
    locationType: String,
    onEditDetails: () -> Unit,
    onChangeLocation: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        // Location Type Icon
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = if (locationType == "pickup")
                        AppColors.Pickup.copy(alpha = 0.1f)
                    else
                        AppColors.Drop.copy(alpha = 0.1f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (locationType == "pickup")
                    Icons.Default.TripOrigin
                else
                    Icons.Default.LocationOn,
                contentDescription = null,
                tint = if (locationType == "pickup") AppColors.Pickup else AppColors.Drop,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Address Info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (locationType == "pickup") "Pickup" else "Drop",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (locationType == "pickup") AppColors.Pickup else AppColors.Drop
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = address.address,
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextPrimary,
                maxLines = 2
            )
            // Show contact info if available
            address.contactName?.let { name ->
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = AppColors.TextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$name • ${address.contactPhone ?: ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextSecondary
                    )
                }
            }
            // Show coordinates if available
            if (address.latitude != 0.0 && address.longitude != 0.0) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "📍 ${String.format("%.4f", address.latitude)}, ${String.format("%.4f", address.longitude)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.TextHint
                )
            }
        }

        // Edit button - Edit details (name, phone, flat, etc.)
        IconButton(
            onClick = onEditDetails,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Edit Details",
                tint = AppColors.Primary,
                modifier = Modifier.size(20.dp)
            )
        }

        // Change button - Change location
        IconButton(
            onClick = onChangeLocation,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MyLocation,
                contentDescription = "Change Location",
                tint = AppColors.TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Vehicle Option Card
 */
@Composable
private fun VehicleOptionCard(
    vehicle: VehicleTypeResponse,
    isSelected: Boolean,
    isPreSelected: Boolean = false,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onSelect),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        border = when {
            isSelected -> BorderStroke(2.dp, AppColors.Primary)
            isPreSelected && !isSelected -> BorderStroke(2.dp, AppColors.Primary.copy(alpha = 0.5f))
            else -> BorderStroke(1.dp, AppColors.Border)
        },
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Vehicle Icon
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(
                        color = if (isSelected)
                            AppColors.Primary.copy(alpha = 0.1f)
                        else
                            AppColors.Background,
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = vehicle.icon,
                    style = MaterialTheme.typography.displaySmall
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Vehicle Info
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = vehicle.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    )
                    // "Previous" badge for pre-selected vehicle
                    if (isPreSelected) {
                        Surface(
                            color = AppColors.Primary.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "Previous",
                                style = MaterialTheme.typography.labelSmall,
                                color = AppColors.Primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = vehicle.capacity,
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextSecondary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = vehicle.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextHint
                )
            }

            // Price
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "₹${vehicle.basePrice}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) AppColors.Primary else AppColors.TextPrimary
                )
                Text(
                    text = "Est. fare",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.TextHint
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Selection Indicator
            RadioButton(
                selected = isSelected,
                onClick = onSelect,
                colors = RadioButtonDefaults.colors(
                    selectedColor = AppColors.Primary,
                    unselectedColor = AppColors.Border
                )
            )
        }
    }
}

/**
 * Location Details Bottom Sheet
 */
@Composable
private fun LocationDetailsBottomSheet(
    pickupAddress: SavedAddress,
    dropAddress: SavedAddress,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Location Details",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary
            )

            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = AppColors.TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Pickup Details
        LocationDetailItem(
            address = pickupAddress,
            type = "Pickup",
            color = AppColors.Pickup,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        HorizontalDivider(color = AppColors.Divider)

        Spacer(modifier = Modifier.height(16.dp))

        // Drop Details
        LocationDetailItem(
            address = dropAddress,
            type = "Drop",
            color = AppColors.Drop,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Distance Info (Mock data)
        InfoCard {
            Row(
                horizontalArrangement = Arrangement.SpaceAround,
                modifier = Modifier.fillMaxWidth()
            ) {
                InfoItem(
                    icon = Icons.Default.Route,
                    label = "Distance",
                    value = "8.5 km"
                )
                InfoItem(
                    icon = Icons.Default.Timer,
                    label = "Est. Time",
                    value = "25 mins"
                )
                InfoItem(
                    icon = Icons.Default.Speed,
                    label = "Route",
                    value = "Fastest"
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

/**
 * Location Detail Item for Bottom Sheet
 */
@Composable
private fun LocationDetailItem(
    address: SavedAddress,
    type: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (type == "Pickup") Icons.Default.TripOrigin else Icons.Default.LocationOn,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = type,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = address.address,
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.TextPrimary
        )
        address.contactName?.let { name ->
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$name • ${address.contactPhone ?: ""}",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextSecondary
            )
        }
        // Show full address details if available
        val details = listOfNotNull(
            address.buildingDetails,
            address.landmark?.let { "Near $it" },
            address.pincode
        )
        if (details.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = details.joinToString(", "),
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextHint
            )
        }
        // Show coordinates
        if (address.latitude != 0.0 && address.longitude != 0.0) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Lat: ${address.latitude}, Lng: ${address.longitude}",
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.TextHint
            )
        }
    }
}

/**
 * Info Item for Bottom Sheet
 */
@Composable
private fun InfoItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = AppColors.Primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = AppColors.TextPrimary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = AppColors.TextSecondary
        )
    }
}