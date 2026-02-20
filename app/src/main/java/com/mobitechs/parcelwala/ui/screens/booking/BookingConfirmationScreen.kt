// ui/screens/booking/BookingConfirmationScreen.kt
package com.mobitechs.parcelwala.ui.screens.booking

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.TripOrigin
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mobitechs.parcelwala.R
import com.mobitechs.parcelwala.data.model.request.SavedAddress
import com.mobitechs.parcelwala.data.model.response.FareDetails
import com.mobitechs.parcelwala.ui.components.AddressesCard
import com.mobitechs.parcelwala.ui.components.EmptyState
import com.mobitechs.parcelwala.ui.components.InfoCard
import com.mobitechs.parcelwala.ui.components.PrimaryButton
import com.mobitechs.parcelwala.ui.theme.AppColors
import com.mobitechs.parcelwala.ui.viewmodel.BookingViewModel

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

    val vehicleFares by viewModel.vehicleFares.collectAsState()
    val isFareLoading by viewModel.isFareLoading.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(pickupAddress, dropAddress) {
        if (pickupAddress != null && dropAddress != null &&
            pickupAddress.latitude != 0.0 && dropAddress.latitude != 0.0
        ) {
            viewModel.setPickupAddress(pickupAddress)
            viewModel.setDropAddress(dropAddress)
        }
    }

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
            title = stringResource(R.string.label_missing_locations),
            subtitle = stringResource(R.string.label_select_pickup_drop),
            actionText = stringResource(R.string.label_go_back),
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
                            stringResource(R.string.title_select_vehicle),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (isPrefilledFromOrder) {
                            Text(
                                stringResource(R.string.label_booking_from_previous),
                                style = MaterialTheme.typography.labelSmall,
                                color = AppColors.Primary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, stringResource(R.string.content_desc_back), tint = AppColors.TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.Surface)
            )
        },
        containerColor = AppColors.Background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (isPrefilledFromOrder) {
                        BookAgainBanner(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                    CompactJourneySummary(
                        pickupAddress = pickupAddress,
                        dropAddress = dropAddress,
                        distanceKm = vehicleFares.firstOrNull()?.distanceKm,
                        estimatedMins = vehicleFares.firstOrNull()?.estimatedDurationMinutes,
                        onViewDetails = { showLocationDetails = true },
                        onEditPickup = onEditPickup,
                        onEditDrop = onEditDrop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                stringResource(R.string.label_choose_your_ride),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = AppColors.TextPrimary
                            )
                            Text(
                                if (preSelectedVehicleId != null) stringResource(R.string.label_pre_selected_hint) else stringResource(R.string.label_select_vehicle_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = AppColors.TextSecondary
                            )
                        }
                        if (!isFareLoading) {
                            IconButton(onClick = { viewModel.calculateFaresForAllVehicles() }) {
                                Icon(Icons.Default.Refresh, stringResource(R.string.content_desc_refresh), tint = AppColors.Primary)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    AnimatedVisibility(
                        visible = isFareLoading,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        FareLoadingIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp)
                        )
                    }

                    AnimatedVisibility(
                        visible = !isFareLoading && vehicleFares.isNotEmpty(),
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
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
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = !isFareLoading && vehicleFares.isEmpty() && uiState.hasFaresLoaded,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        EmptyState(
                            icon = Icons.Default.DirectionsCar,
                            title = stringResource(R.string.label_no_vehicles),
                            subtitle = stringResource(R.string.label_no_vehicles_subtitle),
                            actionText = stringResource(R.string.label_retry),
                            onAction = { viewModel.calculateFaresForAllVehicles() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    InfoCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.Timer,
                                null,
                                tint = AppColors.Primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                stringResource(R.string.label_fare_info),
                                style = MaterialTheme.typography.bodyMedium,
                                color = AppColors.TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(100.dp))
                }

                BottomFareActionBar(
                    selectedFareDetails = selectedFareDetails,
                    onProceed = { selectedFareDetails?.let { onVehicleSelected(it) } },
                    isLoading = isFareLoading
                )
            }

            uiState.error?.let { error ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .padding(bottom = 80.dp),
                    action = { TextButton(onClick = { viewModel.clearError() }) { Text(stringResource(R.string.label_dismiss)) } }
                ) { Text(error) }
            }
        }
    }

    if (showLocationDetails) {
        ModalBottomSheet(
            onDismissRequest = { showLocationDetails = false },
            containerColor = AppColors.Surface
        ) {
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
        colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        AddressesCard(
            pickupAddress.contactName, pickupAddress.contactPhone, pickupAddress.address,
            dropAddress.contactName, dropAddress.contactPhone, dropAddress.address
        )
    }
}

@Composable
private fun VehicleFareCard(
    fareDetails: FareDetails, isSelected: Boolean, isPreSelected: Boolean = false,
    onSelect: () -> Unit, modifier: Modifier = Modifier
) {
    val alpha by animateFloatAsState(targetValue = if (isSelected) 1f else 0.9f, label = "alpha")

    Card(
        modifier = modifier
            .alpha(alpha)
            .clickable(onClick = onSelect),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
        border = when {
            isSelected -> BorderStroke(2.dp, AppColors.Primary)
            isPreSelected -> BorderStroke(2.dp, AppColors.Primary.copy(alpha = 0.4f))
            else -> BorderStroke(1.dp, AppColors.Border)
        },
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 6.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        color = if (isSelected) AppColors.Primary.copy(alpha = 0.15f) else AppColors.Background,
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(fareDetails.vehicleTypeIcon, style = MaterialTheme.typography.headlineMedium)
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        fareDetails.vehicleTypeName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) AppColors.Primary else AppColors.TextPrimary
                    )
                    if (isPreSelected) {
                        Surface(
                            color = AppColors.Primary.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                stringResource(R.string.label_previous),
                                style = MaterialTheme.typography.labelSmall,
                                color = AppColors.Primary,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (fareDetails.hasSurgePricing()) {
                        Surface(color = AppColors.PrimaryLight, shape = RoundedCornerShape(4.dp)) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    Icons.Default.TrendingUp,
                                    stringResource(R.string.label_surge),
                                    tint = AppColors.OrangeDark,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    "${fareDetails.getSurgePercentage()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AppColors.OrangeDark,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    fareDetails.capacity,
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextSecondary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.Timer,
                        null,
                        tint = AppColors.TextHint,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        fareDetails.getEtaText(),
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextHint
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    fareDetails.getDisplayFare(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) AppColors.Primary else AppColors.TextPrimary
                )
                if (fareDetails.discount > 0) {
                    Text(
                        "₹${fareDetails.totalFare.toInt()}",
                        style = MaterialTheme.typography.bodySmall.copy(textDecoration = TextDecoration.LineThrough),
                        color = AppColors.TextHint
                    )
                }
                if (fareDetails.tollCharges > 0) {
                    Text(
                        stringResource(R.string.label_incl_toll),
                        style = MaterialTheme.typography.labelSmall,
                        color = AppColors.TextHint
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))
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

@Composable
private fun FareLoadingIndicator(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(color = AppColors.Primary, modifier = Modifier.size(48.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            stringResource(R.string.label_calculating_fares),
            style = MaterialTheme.typography.bodyLarge,
            color = AppColors.TextSecondary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            stringResource(R.string.label_finding_best_prices),
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.TextHint
        )
    }
}

@Composable
private fun BottomFareActionBar(
    selectedFareDetails: FareDetails?,
    onProceed: () -> Unit,
    isLoading: Boolean
) {
    Surface(color = AppColors.Surface, shadowElevation = 12.dp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            selectedFareDetails?.let { fare ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            fare.vehicleTypeName,
                            style = MaterialTheme.typography.labelMedium,
                            color = AppColors.TextSecondary
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                fare.getDisplayFare(),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = AppColors.Primary
                            )
                            if (fare.discount > 0) {
                                Surface(
                                    color = AppColors.Pickup.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        stringResource(R.string.label_discount_off, fare.discount.toInt()),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AppColors.Pickup,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                    PrimaryButton(
                        text = stringResource(R.string.label_proceed),
                        onClick = onProceed,
                        icon = Icons.Default.ArrowForward,
                        modifier = Modifier.width(150.dp),
                        enabled = !isLoading
                    )
                }
            } ?: run {
                Text(
                    if (isLoading) stringResource(R.string.label_calculating_fares) else stringResource(R.string.label_select_vehicle_to_proceed),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextHint,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun BookAgainBanner(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.Primary.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.Refresh,
                stringResource(R.string.content_desc_book_again),
                tint = AppColors.Primary,
                modifier = Modifier.size(24.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.label_booking_again),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.Primary
                )
                Text(
                    stringResource(R.string.label_details_prefilled),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextSecondary
                )
            }
        }
    }
}

@Composable
private fun LocationDetailsBottomSheet(
    pickupAddress: SavedAddress,
    dropAddress: SavedAddress,
    distanceKm: Double?,
    estimatedMins: Int?,
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
                stringResource(R.string.label_trip_details),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    Icons.Default.Close,
                    stringResource(R.string.content_desc_close),
                    tint = AppColors.TextSecondary
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        LocationDetailItem(pickupAddress, stringResource(R.string.label_pickup), AppColors.Pickup)
        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = AppColors.Divider)
        Spacer(modifier = Modifier.height(16.dp))
        LocationDetailItem(dropAddress, stringResource(R.string.label_drop), AppColors.Drop)
        Spacer(modifier = Modifier.height(24.dp))
        if (distanceKm != null || estimatedMins != null) {
            InfoCard {
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    distanceKm?.let {
                        InfoStatItem(
                            Icons.Default.Route,
                            String.format("%.1f km", it),
                            stringResource(R.string.label_distance)
                        )
                    }
                    estimatedMins?.let {
                        InfoStatItem(
                            Icons.Default.Timer,
                            "$it mins",
                            stringResource(R.string.label_est_time)
                        )
                    }
                    InfoStatItem(Icons.Default.Speed, stringResource(R.string.label_fastest), stringResource(R.string.label_route))
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
            Icon(
                if (type == stringResource(R.string.label_pickup)) Icons.Default.TripOrigin else Icons.Default.LocationOn,
                null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                type,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            address.address,
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.TextPrimary
        )
        address.contactName?.let { name ->
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "$name • ${address.contactPhone ?: ""}",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextSecondary
            )
        }
    }
}

@Composable
private fun InfoStatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, label, tint = AppColors.Primary, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = AppColors.TextPrimary
        )
        Text(label, style = MaterialTheme.typography.labelSmall, color = AppColors.TextSecondary)
    }
}