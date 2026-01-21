package com.mobitechs.parcelwala.ui.screens.booking

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.maps.model.LatLng
import com.mobitechs.parcelwala.data.model.request.SavedAddress
import com.mobitechs.parcelwala.data.model.response.PlaceAutocomplete
import com.mobitechs.parcelwala.ui.components.*
import com.mobitechs.parcelwala.ui.theme.AppColors
import com.mobitechs.parcelwala.ui.viewmodel.LocationSearchViewModel
import com.mobitechs.parcelwala.utils.rememberLocationPermissionState
import kotlinx.coroutines.delay

/**
 * Location Search Screen with Reusable Components
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationSearchScreen(
    locationType: String,
    onAddressSelected: (SavedAddress) -> Unit,
    onMapPicker: (LatLng) -> Unit,
    onBack: () -> Unit,
    viewModel: LocationSearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusRequester = remember { FocusRequester() }

    val permissionGranted = rememberLocationPermissionState { granted ->
        if (granted) {
            viewModel.getCurrentLocation()
        }
    }

    // ✅ Auto-focus on search field when screen opens
    LaunchedEffect(Unit) {
        delay(100) // Small delay for smooth animation
        focusRequester.requestFocus()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (locationType == "pickup") "Pickup Location" else "Drop Location",
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
            // Search Field with auto-focus
            SearchField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                placeholder = when (locationType) {
                    "pickup" -> "Search pickup location..."
                    "drop" -> "Search drop location..."
                    else -> "Search location..."
                },
                modifier = Modifier
                    .padding(16.dp)
                    .focusRequester(focusRequester),
                onClear = { viewModel.updateSearchQuery("") }
            )

            // Content
            Box(modifier = Modifier.weight(1f)) {
                if (uiState.isLoading) {
                    LoadingIndicator(
                        message = "Searching...",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp)
                    )
                } else if (uiState.predictions.isNotEmpty() && uiState.searchQuery.isNotBlank()) {
                    AutocompleteResults(
                        predictions = uiState.predictions,
                        onPredictionClick = { prediction ->
                            viewModel.selectPlace(prediction.placeId) { address ->
                                onAddressSelected(address)
                            }
                        }
                    )
                } else {
                    LazyColumn {
                        if (uiState.savedAddresses.isNotEmpty()) {
                            item {
                                SavedAddressesSection(
                                    addresses = uiState.savedAddresses,
                                    onAddressClick = { address ->
                                        viewModel.selectAddress(address)
                                        onAddressSelected(address)
                                    }
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }

                        if (uiState.recentPickups.isNotEmpty()) {
                            item {
                                RecentPickupsSection(
                                    pickups = uiState.recentPickups,
                                    onPickupClick = { address ->
                                        viewModel.selectAddress(address)
                                        onAddressSelected(address)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Actions
            Surface(
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SecondaryButton(
                        text = "Current",
                        onClick = {
                            if (permissionGranted.value) {
                                viewModel.getCurrentLocation()
                                uiState.selectedAddress?.let { address ->
                                    onAddressSelected(address)
                                }
                            }
                        },
                        icon = Icons.Default.MyLocation,
                        modifier = Modifier.weight(1f)
                    )

                    SecondaryButton(
                        text = "On Map",
                        onClick = {
                            val currentLocation = uiState.selectedAddress?.let {
                                LatLng(it.latitude ?: 19.0760, it.longitude ?: 72.8777)
                            } ?: LatLng(19.0760, 72.8777)
                            onMapPicker(currentLocation)
                        },
                        icon = Icons.Default.Place,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Error Message
            uiState.error?.let { error ->
                ErrorMessageCard(
                    message = error,
                    onRetry = { viewModel.clearError() },
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Composable
private fun AutocompleteResults(
    predictions: List<PlaceAutocomplete>,
    onPredictionClick: (PlaceAutocomplete) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxWidth()) {
        items(predictions) { prediction ->
            PredictionItem(
                prediction = prediction,
                onClick = { onPredictionClick(prediction) }
            )
        }
    }
}

@Composable
private fun PredictionItem(
    prediction: PlaceAutocomplete,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LocationTypeIcon(
            locationType = "search",
            size = 40.dp
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = prediction.primaryText,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = AppColors.TextPrimary
            )
            prediction.secondaryText?.let { secondary ->
                Text(
                    text = secondary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextSecondary
                )
            }
        }

        Icon(
            imageVector = Icons.Default.ArrowForward,
            contentDescription = "Select",
            tint = AppColors.TextHint,
            modifier = Modifier.size(20.dp)
        )
    }

    HorizontalDivider(color = AppColors.Divider, thickness = 0.5.dp)
}

@Composable
private fun SavedAddressesSection(
    addresses: List<SavedAddress>,
    onAddressClick: (SavedAddress) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        SectionHeader(
            text = "Saved Addresses",
            modifier = Modifier.padding(vertical = 8.dp)
        )

        addresses.take(2).forEach { address ->
            AddressItem(
                address = address,
                onClick = { onAddressClick(address) }
            )
        }
    }
}

@Composable
private fun RecentPickupsSection(
    pickups: List<SavedAddress>,
    onPickupClick: (SavedAddress) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        SectionHeader(
            text = "Recent Pickups",
            modifier = Modifier.padding(vertical = 8.dp)
        )

        pickups.take(3).forEach { pickup ->
            AddressItem(
                address = pickup,
                onClick = { onPickupClick(pickup) }
            )
        }
    }
}

@Composable
private fun AddressItem(
    address: SavedAddress,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.Default.AccessTime,
            contentDescription = "Recent",
            modifier = Modifier.size(24.dp),
            tint = AppColors.TextSecondary
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = address.label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = AppColors.TextPrimary
            )
            Text(
                text = address.address,
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextSecondary,
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

        IconButton(onClick = { /* TODO: Save address */ }) {
            Icon(
                imageVector = Icons.Default.FavoriteBorder,
                contentDescription = "Save",
                tint = AppColors.Primary
            )
        }
    }

    HorizontalDivider(color = AppColors.Divider, thickness = 0.5.dp)
}