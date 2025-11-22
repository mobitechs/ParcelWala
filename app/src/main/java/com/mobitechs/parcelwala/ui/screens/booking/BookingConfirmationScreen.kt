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
import com.mobitechs.parcelwala.ui.components.*
import com.mobitechs.parcelwala.ui.theme.AppColors
import com.mobitechs.parcelwala.ui.viewmodel.BookingViewModel

/**
 * Booking Confirmation Screen
 * Shows pickup/drop locations and vehicle selection
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingConfirmationScreen(
    pickupAddress: SavedAddress?,
    dropAddress: SavedAddress?,
    onVehicleSelected: (VehicleType) -> Unit,
    onChangePickup: () -> Unit,
    onChangeDrop: () -> Unit,
    onBack: () -> Unit,
    viewModel: BookingViewModel = hiltViewModel()
) {
    var selectedVehicle by remember { mutableStateOf<VehicleType?>(null) }
    var showLocationDetails by remember { mutableStateOf(false) }

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
                    Text(
                        text = "Confirm Booking",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Scrollable Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                // Journey Summary Card
                JourneySummaryCard(
                    pickupAddress = pickupAddress,
                    dropAddress = dropAddress,
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
                    subtitle = "Choose the right vehicle for your delivery",
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Vehicle Options
                availableVehicles.forEach { vehicle ->
                    VehicleOptionCard(
                        vehicle = vehicle,
                        isSelected = selectedVehicle == vehicle,
                        onSelect = { selectedVehicle = vehicle },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    )
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
                                    text = "₹${vehicle.price}",
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
 * Journey Summary Card with Pickup and Drop
 */
@Composable
private fun JourneySummaryCard(
    pickupAddress: SavedAddress,
    dropAddress: SavedAddress,
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
            onChangeLocation = onChangePickup
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Journey Connector
        JourneyConnector(modifier = Modifier.padding(start = 20.dp))

        Spacer(modifier = Modifier.height(12.dp))

        // Drop Location
        LocationRow(
            address = dropAddress,
            locationType = "drop",
            onChangeLocation = onChangeDrop
        )
    }
}

/**
 * Location Row with Icon and Address
 */
@Composable
private fun LocationRow(
    address: SavedAddress,
    locationType: String,
    onChangeLocation: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        LocationTypeIcon(
            locationType = locationType,
            size = 40.dp
        )

        Spacer(modifier = Modifier.width(12.dp))

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
            address.contactName?.let { name ->
                Spacer(modifier = Modifier.height(4.dp))
                LabeledIcon(
                    icon = Icons.Default.Person,
                    text = "$name • ${address.contactPhone}"
                )
            }
        }

        IconButton(onClick = onChangeLocation) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Change",
                tint = AppColors.Primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Vehicle Option Card with Border Selection
 */
@Composable
private fun VehicleOptionCard(
    vehicle: VehicleType,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onSelect),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        border = if (isSelected) {
            BorderStroke(2.dp, AppColors.Primary)
        } else {
            BorderStroke(1.dp, AppColors.Border)
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
                Text(
                    text = vehicle.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary
                )
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
                    text = "₹${vehicle.price}",
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