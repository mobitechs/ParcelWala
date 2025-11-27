// ui/screens/account/AddressSearchScreen.kt
package com.mobitechs.parcelwala.ui.screens.account

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.google.android.gms.maps.model.LatLng
import com.mobitechs.parcelwala.data.model.request.SavedAddress
import com.mobitechs.parcelwala.data.model.response.PlaceAutocomplete
import com.mobitechs.parcelwala.ui.components.*
import com.mobitechs.parcelwala.ui.theme.AppColors
import com.mobitechs.parcelwala.ui.viewmodel.LocationSearchViewModel
import com.mobitechs.parcelwala.utils.rememberLocationPermissionState

/**
 * Address Search Screen for Account Section
 * Allows user to search for a new address to save
 *
 * Features:
 * - Search field with autocomplete
 * - Select on map option
 * - Saved addresses list
 * - Recent searches
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddressSearchScreen(
    onAddressSelected: (SavedAddress) -> Unit,
    onMapPicker: (LatLng) -> Unit,
    onBack: () -> Unit,
    viewModel: LocationSearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val permissionGranted = rememberLocationPermissionState { granted ->
        if (granted) {
            viewModel.getCurrentLocation()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Add Address",
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
            // Search Field
            SearchField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                placeholder = "Search for area, street name...",
                modifier = Modifier.padding(16.dp),
                onClear = { viewModel.updateSearchQuery("") }
            )

            // Select on Map Option
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clickable {
                        val currentLocation = uiState.selectedAddress?.let {
                            LatLng(it.latitude, it.longitude)
                        } ?: LatLng(19.0760, 72.8777) // Default to Mumbai
                        onMapPicker(currentLocation)
                    },
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = AppColors.Primary.copy(alpha = 0.05f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = null,
                        tint = AppColors.Primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Select on map",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.Primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Content
            Box(modifier = Modifier.weight(1f)) {
                when {
                    uiState.isLoading || uiState.isLoadingPredictions -> {
                        LoadingIndicator(
                            message = "Searching...",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp)
                        )
                    }
                    uiState.predictions.isNotEmpty() && uiState.searchQuery.isNotBlank() -> {
                        // Autocomplete Results
                        AutocompleteResultsList(
                            predictions = uiState.predictions,
                            onPredictionClick = { prediction ->
                                viewModel.selectPlace(prediction.placeId) { address ->
                                    onAddressSelected(address)
                                }
                            }
                        )
                    }
                    else -> {
                        // Saved Addresses and Recent
                        SavedAndRecentAddresses(
                            savedAddresses = uiState.savedAddresses,
                            recentAddresses = uiState.recentPickups,
                            onAddressClick = { address ->
                                viewModel.selectAddress(address)
                                onAddressSelected(address)
                            }
                        )
                    }
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

/**
 * Autocomplete Results List
 */
@Composable
private fun AutocompleteResultsList(
    predictions: List<PlaceAutocomplete>,
    onPredictionClick: (PlaceAutocomplete) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth()
    ) {
        items(predictions) { prediction ->
            AutocompleteItem(
                prediction = prediction,
                onClick = { onPredictionClick(prediction) }
            )
        }
    }
}

/**
 * Single Autocomplete Item
 */
@Composable
private fun AutocompleteItem(
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
        // History/Clock Icon
        Icon(
            imageVector = Icons.Default.AccessTime,
            contentDescription = null,
            tint = AppColors.TextSecondary,
            modifier = Modifier.size(24.dp)
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
                    color = AppColors.TextSecondary,
                    maxLines = 1
                )
            }
        }
    }

    HorizontalDivider(
        color = AppColors.Divider,
        thickness = 0.5.dp,
        modifier = Modifier.padding(start = 56.dp)
    )
}

/**
 * Saved and Recent Addresses List
 */
@Composable
private fun SavedAndRecentAddresses(
    savedAddresses: List<SavedAddress>,
    recentAddresses: List<SavedAddress>,
    onAddressClick: (SavedAddress) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Saved Addresses
        if (savedAddresses.isNotEmpty()) {
            items(savedAddresses) { address ->
                SavedAddressItem(
                    address = address,
                    onClick = { onAddressClick(address) }
                )
            }
        }

        // Recent Addresses
        if (recentAddresses.isNotEmpty()) {
            items(recentAddresses.filter { recent ->
                savedAddresses.none { it.addressId == recent.addressId }
            }) { address ->
                RecentAddressItem(
                    address = address,
                    onClick = { onAddressClick(address) }
                )
            }
        }
    }
}

/**
 * Saved Address Item
 */
@Composable
private fun SavedAddressItem(
    address: SavedAddress,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Heart/Favorite Icon for saved
        Icon(
            imageVector = Icons.Default.Favorite,
            contentDescription = null,
            tint = AppColors.Primary,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = address.label.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.TextPrimary
                )

                // Contact badge
                address.contactName?.let { name ->
                    Surface(
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                        color = AppColors.Background
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = AppColors.TextSecondary,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = name.take(12) + if (name.length > 12) "..." else "",
                                style = MaterialTheme.typography.labelSmall,
                                color = AppColors.TextSecondary
                            )
                        }
                    }
                }
            }

            Text(
                text = address.address,
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextSecondary,
                maxLines = 1
            )
        }
    }

    HorizontalDivider(
        color = AppColors.Divider,
        thickness = 0.5.dp,
        modifier = Modifier.padding(start = 56.dp)
    )
}

/**
 * Recent Address Item
 */
@Composable
private fun RecentAddressItem(
    address: SavedAddress,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Clock Icon for recent
        Icon(
            imageVector = Icons.Default.AccessTime,
            contentDescription = null,
            tint = AppColors.TextSecondary,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = address.label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = AppColors.TextPrimary
                )

                // Contact badge
                address.contactName?.let { name ->
                    Surface(
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                        color = AppColors.Background
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = AppColors.TextSecondary,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = name.take(12) + if (name.length > 12) "..." else "",
                                style = MaterialTheme.typography.labelSmall,
                                color = AppColors.TextSecondary
                            )
                        }
                    }
                }
            }

            Text(
                text = address.address,
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextSecondary,
                maxLines = 1
            )
        }
    }

    HorizontalDivider(
        color = AppColors.Divider,
        thickness = 0.5.dp,
        modifier = Modifier.padding(start = 56.dp)
    )
}