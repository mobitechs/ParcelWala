package com.mobitechs.parcelwala.ui.screens.account

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.maps.model.LatLng
import com.mobitechs.parcelwala.R
import com.mobitechs.parcelwala.data.model.request.SavedAddress
import com.mobitechs.parcelwala.data.model.response.PlaceAutocomplete
import com.mobitechs.parcelwala.ui.components.AppTopBar
import com.mobitechs.parcelwala.ui.components.ErrorMessageCard
import com.mobitechs.parcelwala.ui.components.LoadingIndicator
import com.mobitechs.parcelwala.ui.components.StatusBarScaffold
import com.mobitechs.parcelwala.ui.theme.AppColors
import com.mobitechs.parcelwala.ui.viewmodel.LocationSearchViewModel
import com.mobitechs.parcelwala.utils.rememberLocationPermissionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddressSearchScreen(
    onAddressSelected: (SavedAddress) -> Unit,
    onMapPicker: (LatLng) -> Unit,
    onBack: () -> Unit,
    viewModel: LocationSearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    rememberLocationPermissionState { granted ->
        if (granted) viewModel.getCurrentLocation()
    }

    StatusBarScaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.add_address),
                onBack = onBack,
                // ── Search bar + map pill both inside the gradient ─────────
                extraContent = {
                    SearchAndMapContent(
                        query = uiState.searchQuery,
                        onQueryChange = { viewModel.updateSearchQuery(it) },
                        onMapPicker = {
                            val currentLocation = uiState.selectedAddress?.let {
                                LatLng(it.latitude, it.longitude)
                            } ?: LatLng(19.0760, 72.8777)
                            onMapPicker(currentLocation)
                        }
                    )
                }
            )
        },
        containerColor = AppColors.Background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                when {
                    uiState.isLoading || uiState.isLoadingPredictions -> {
                        LoadingIndicator(
                            message = stringResource(R.string.label_searching),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp)
                        )
                    }

                    uiState.predictions.isNotEmpty() && uiState.searchQuery.isNotBlank() -> {
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

// ══════════════════════════════════════════════════════════════════════════════
// SearchAndMapContent
// Sits in AppTopBar's extraContent — gradient shows through from
// GradientTopBarWrapper. Frosted search bar + transparent map pill.
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun SearchAndMapContent(
    query: String,
    onQueryChange: (String) -> Unit,
    onMapPicker: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // ── Frosted search bar ─────────────────────────────────────────────
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = Color.White.copy(alpha = 0.15f),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.30f)
            ),
            onClick = { /* focuses keyboard */ }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.70f),
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = if (query.isBlank())
                        stringResource(R.string.search_area_placeholder)
                    else query,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (query.isBlank())
                        Color.White.copy(alpha = 0.55f)
                    else Color.White,
                    modifier = Modifier.weight(1f)
                )
                // GPS / current location icon
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // ── Map pill centered ──────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Surface(
                onClick = onMapPicker,
                shape = RoundedCornerShape(20.dp),
                color = Color.White.copy(alpha = 0.18f),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.35f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Map,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = stringResource(R.string.select_on_map),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// AutocompleteResultsList
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun AutocompleteResultsList(
    predictions: List<PlaceAutocomplete>,
    onPredictionClick: (PlaceAutocomplete) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxWidth()) {
        items(predictions) { prediction ->
            AutocompleteItem(
                prediction = prediction,
                onClick = { onPredictionClick(prediction) }
            )
        }
    }
}

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
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(AppColors.Background),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AccessTime,
                contentDescription = null,
                tint = AppColors.TextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = prediction.primaryText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = AppColors.TextPrimary
            )
            prediction.secondaryText?.let { secondary ->
                Text(
                    text = secondary,
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextSecondary,
                    maxLines = 1
                )
            }
        }
    }
    HorizontalDivider(
        color = AppColors.Divider,
        thickness = 0.5.dp,
        modifier = Modifier.padding(start = 66.dp, end = 16.dp)
    )
}

// ══════════════════════════════════════════════════════════════════════════════
// SavedAndRecentAddresses
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun SavedAndRecentAddresses(
    savedAddresses: List<SavedAddress>,
    recentAddresses: List<SavedAddress>,
    onAddressClick: (SavedAddress) -> Unit
) {
    val filteredRecent = recentAddresses.filter { recent ->
        savedAddresses.none { it.addressId == recent.addressId }
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // ── Saved addresses group ──────────────────────────────────────────
        if (savedAddresses.isNotEmpty()) {
            item { SectionLabel(text = stringResource(R.string.saved_addresses)) }
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    shadowElevation = 1.dp
                ) {
                    Column {
                        savedAddresses.forEachIndexed { index, address ->
                            AddressListRow(
                                address = address,
                                isSaved = true,
                                onClick = { onAddressClick(address) }
                            )
                            if (index < savedAddresses.lastIndex) {
                                HorizontalDivider(
                                    color = AppColors.Divider,
                                    thickness = 0.5.dp,
                                    modifier = Modifier.padding(start = 62.dp)
                                )
                            }
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(12.dp)) }
        }

        // ── Recent addresses group ─────────────────────────────────────────
        if (filteredRecent.isNotEmpty()) {
            item { SectionLabel(text = stringResource(R.string.label_recent_searches)) }
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    shadowElevation = 1.dp
                ) {
                    Column {
                        filteredRecent.forEachIndexed { index, address ->
                            AddressListRow(
                                address = address,
                                isSaved = false,
                                onClick = { onAddressClick(address) }
                            )
                            if (index < filteredRecent.lastIndex) {
                                HorizontalDivider(
                                    color = AppColors.Divider,
                                    thickness = 0.5.dp,
                                    modifier = Modifier.padding(start = 62.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(
            letterSpacing = 0.5.sp,
            fontSize = 10.sp
        ),
        fontWeight = FontWeight.SemiBold,
        color = AppColors.TextSecondary,
        modifier = Modifier.padding(
            start = 20.dp, end = 16.dp,
            top = 8.dp, bottom = 6.dp
        )
    )
}

@Composable
private fun AddressListRow(
    address: SavedAddress,
    isSaved: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Icon
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(
                    if (isSaved) AppColors.Primary.copy(alpha = 0.10f)
                    else AppColors.Background
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isSaved) Icons.Default.Favorite else Icons.Default.AccessTime,
                contentDescription = null,
                tint = if (isSaved) AppColors.Primary else AppColors.TextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }

        // Label + address
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (isSaved)
                        address.label.replaceFirstChar { it.uppercase() }
                    else address.label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSaved) FontWeight.SemiBold else FontWeight.Medium,
                    color = AppColors.TextPrimary
                )
                address.contactName?.takeIf { it.isNotEmpty() }?.let { name ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = AppColors.Background
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = AppColors.TextSecondary,
                                modifier = Modifier.size(10.dp)
                            )
                            Text(
                                text = name.take(12) + if (name.length > 12) "…" else "",
                                style = MaterialTheme.typography.labelSmall,
                                color = AppColors.TextSecondary,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
            Text(
                text = address.address,
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextSecondary,
                maxLines = 1
            )
        }
    }
}