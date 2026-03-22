// ui/screens/booking/BookingConfirmationScreen.kt
package com.mobitechs.parcelwala.ui.screens.booking

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.TripOrigin
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mobitechs.parcelwala.R
import com.mobitechs.parcelwala.data.model.request.SavedAddress
import com.mobitechs.parcelwala.data.model.response.FareDetails
import com.mobitechs.parcelwala.ui.components.AppTopBar
import com.mobitechs.parcelwala.ui.components.EmptyState
import com.mobitechs.parcelwala.ui.components.PrimaryButton
import com.mobitechs.parcelwala.ui.components.StatusBarScaffold
import com.mobitechs.parcelwala.ui.theme.AppColors
import com.mobitechs.parcelwala.ui.viewmodel.BookingViewModel

// ─────────────────────────────────────────────────────────────────────────────
// Screen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun BookingConfirmationScreen(
    pickupAddress: SavedAddress?,
    dropAddress: SavedAddress?,
    preSelectedVehicleId: Int? = null,
    isPrefilledFromOrder: Boolean = false,
    onVehicleSelected: (FareDetails) -> Unit,
    onEditPickup: () -> Unit,
    onEditDrop: () -> Unit,
    onBack: () -> Unit,
    viewModel: BookingViewModel = hiltViewModel()
) {
    var selectedFare by remember { mutableStateOf<FareDetails?>(null) }
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
                selectedFare = it
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
            modifier = Modifier.fillMaxSize().padding(32.dp)
        )
        return
    }

    StatusBarScaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.title_select_vehicle),
                onBack = onBack
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

                // ── Scrollable body ───────────────────────────────────────
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 4.dp)
                ) {

                    // Journey summary card
                    JourneyCard(
                        pickupAddress = pickupAddress,
                        dropAddress = dropAddress,
                        distanceKm = vehicleFares.firstOrNull()?.distanceKm,
                        estimatedMins = vehicleFares.firstOrNull()?.estimatedDurationMinutes,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    )

                    // Section header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.label_choose_your_ride),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = AppColors.TextSecondary,
                            letterSpacing = androidx.compose.ui.unit.TextUnit(0.4f, androidx.compose.ui.unit.TextUnitType.Sp)
                        )
                        if (!isFareLoading) {
                            IconButton(
                                onClick = { viewModel.calculateFaresForAllVehicles() },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = stringResource(R.string.content_desc_refresh),
                                    tint = AppColors.Primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Loading
                    AnimatedVisibility(visible = isFareLoading, enter = fadeIn(), exit = fadeOut()) {
                        FareLoadingState(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(40.dp)
                        )
                    }

                    // Vehicle cards
                    AnimatedVisibility(
                        visible = !isFareLoading && vehicleFares.isNotEmpty(),
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            vehicleFares.forEach { fare ->
                                VehicleCard(
                                    fareDetails = fare,
                                    isSelected = selectedFare?.vehicleTypeId == fare.vehicleTypeId,
                                    onSelect = {
                                        selectedFare = fare
                                        viewModel.selectFareDetails(fare)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp)
                                )
                            }
                        }
                    }

                    // Empty state
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

                    Spacer(modifier = Modifier.height(100.dp))
                }

                // ── Bottom action bar ─────────────────────────────────────
                BottomActionBar(
                    selectedFare = selectedFare,
                    isLoading = isFareLoading,
                    onProceed = { selectedFare?.let { onVehicleSelected(it) } }
                )
            }

            // Snackbar
            uiState.error?.let { error ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .padding(bottom = 80.dp),
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text(stringResource(R.string.label_dismiss))
                        }
                    }
                ) { Text(error) }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Journey card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun JourneyCard(
    pickupAddress: SavedAddress,
    dropAddress: SavedAddress,
    distanceKm: Double?,
    estimatedMins: Int?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
        border = BorderStroke(0.5.dp, AppColors.Border),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            // Pickup row
            JourneyStop(
                address = pickupAddress,
                isPickup = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            )

            HorizontalDivider(
                color = AppColors.Divider,
                thickness = 0.5.dp,
                modifier = Modifier.padding(horizontal = 14.dp)
            )

            // Drop row
            JourneyStop(
                address = dropAddress,
                isPickup = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            )

            // Footer chips — distance + time only
            if (distanceKm != null || estimatedMins != null) {
                HorizontalDivider(color = AppColors.Divider, thickness = 0.5.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppColors.Background)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    distanceKm?.let {
                        JourneyChip(text = String.format("%.1f km", it))
                    }
                    estimatedMins?.let {
                        JourneyChip(text = "$it mins")
                    }
                }
            }
        }
    }
}

@Composable
private fun JourneyStop(
    address: SavedAddress,
    isPickup: Boolean,
    modifier: Modifier = Modifier
) {
    val accentColor = if (isPickup) AppColors.Pickup else AppColors.Drop
    val label = if (isPickup) "PICKUP" else "DROP"

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Dot icon
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .size(22.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPickup) Icons.Default.TripOrigin else Icons.Default.LocationOn,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(12.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = accentColor,
                letterSpacing = androidx.compose.ui.unit.TextUnit(0.4f, androidx.compose.ui.unit.TextUnitType.Sp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            val contactDisplay = listOfNotNull(
                address.contactName?.takeIf { it.isNotBlank() },
                address.contactPhone?.takeIf { it.isNotBlank() }
            ).joinToString(" · ")
            if (contactDisplay.isNotBlank()) {
                Text(
                    text = contactDisplay,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = AppColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
            }
            Text(
                text = address.address,
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = androidx.compose.ui.unit.TextUnit(16f, androidx.compose.ui.unit.TextUnitType.Sp)
            )
        }
    }
}

@Composable
private fun JourneyChip(text: String) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = AppColors.Surface,
        border = BorderStroke(0.5.dp, AppColors.Border),
        tonalElevation = 0.dp
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = AppColors.TextSecondary,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Vehicle card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun VehicleCard(
    fareDetails: FareDetails,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strikePrice = (fareDetails.getEffectiveFare() * 1.1).toInt()

    Card(
        modifier = modifier.clickable(onClick = onSelect),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
        border = if (isSelected)
            BorderStroke(1.5.dp, AppColors.Primary)
        else
            BorderStroke(0.5.dp, AppColors.Border),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            // Top row: emoji · name · price
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Vehicle emoji
                Text(
                    text = fareDetails.vehicleTypeIcon,
                    style = MaterialTheme.typography.headlineSmall
                )

                // Name
                Text(
                    text = fareDetails.vehicleTypeName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = if (isSelected) AppColors.Primary else AppColors.TextPrimary,
                    modifier = Modifier.weight(1f)
                )

                // Price column
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = fareDetails.getDisplayFare(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (isSelected) AppColors.Primary else AppColors.TextPrimary
                    )
                    Text(
                        text = "₹$strikePrice",
                        style = MaterialTheme.typography.bodySmall.copy(
                            textDecoration = TextDecoration.LineThrough
                        ),
                        color = AppColors.TextHint
                    )
                }
            }

            // Detail row: ETA · weight
            HorizontalDivider(
                color = AppColors.Divider,
                thickness = 0.5.dp,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                VehicleDetailItem(
                    icon = Icons.Default.Timer,
                    text = fareDetails.getEtaText()
                )
                VehicleDetailItem(
                    icon = Icons.Default.TrendingUp, // replace with your weight icon if available
                    text = fareDetails.capacity  // e.g. "Upto 10 kg"
                )
            }
        }
    }
}

@Composable
private fun VehicleDetailItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = AppColors.TextHint,
            modifier = Modifier.size(13.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.TextSecondary
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Bottom action bar
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BottomActionBar(
    selectedFare: FareDetails?,
    isLoading: Boolean,
    onProceed: () -> Unit
) {
    Surface(
        color = AppColors.Surface,
        shadowElevation = 12.dp,
        tonalElevation = 0.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            if (selectedFare != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = selectedFare.vehicleTypeName,
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.TextSecondary
                        )
                        Text(
                            text = selectedFare.getDisplayFare(),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Medium,
                            color = AppColors.Primary
                        )
                    }
                    PrimaryButton(
                        text = stringResource(R.string.label_proceed),
                        onClick = onProceed,
                        icon = Icons.Default.ArrowForward,
                        modifier = Modifier.width(148.dp),
                        enabled = !isLoading
                    )
                }
            } else {
                Text(
                    text = if (isLoading)
                        stringResource(R.string.label_calculating_fares)
                    else
                        stringResource(R.string.label_select_vehicle_to_proceed),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextHint,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Loading state
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun FareLoadingState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            color = AppColors.Primary,
            modifier = Modifier.size(40.dp),
            strokeWidth = 3.dp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.label_calculating_fares),
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.TextSecondary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.label_finding_best_prices),
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.TextHint
        )
    }
}