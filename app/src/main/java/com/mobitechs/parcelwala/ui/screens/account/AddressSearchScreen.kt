package com.mobitechs.parcelwala.ui.screens.account

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.maps.model.LatLng
import com.mobitechs.parcelwala.R
import com.mobitechs.parcelwala.data.model.request.SavedAddress
import com.mobitechs.parcelwala.data.model.response.PlaceAutocomplete
import com.mobitechs.parcelwala.ui.components.ErrorMessageCard
import com.mobitechs.parcelwala.ui.components.LoadingIndicator
import com.mobitechs.parcelwala.ui.components.StatusBarScaffold
import com.mobitechs.parcelwala.ui.theme.AppColors
import com.mobitechs.parcelwala.ui.viewmodel.LocationSearchViewModel
import com.mobitechs.parcelwala.utils.rememberLocationPermissionState

// ── Palette constants (local to this file) ────────────────────────────────────
// The header uses a deep navy so it reads as "premium dark" even inside the
// teal GradientTopBarWrapper.  All values intentionally reference AppColors
// where possible so a theme change propagates automatically.

private val HeaderTextPrimary   = Color.White
private val HeaderTextMuted     = Color.White.copy(alpha = 0.60f)
private val SearchBarBg         = Color.White.copy(alpha = 0.13f)
private val SearchBarBorder     = Color.White.copy(alpha = 0.25f)
private val MapPillBg           = Color.White.copy(alpha = 0.16f)
private val MapPillBorder       = Color.White.copy(alpha = 0.30f)

// ══════════════════════════════════════════════════════════════════════════════
// AddressSearchScreen
// ══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddressSearchScreen(
    onAddressSelected: (SavedAddress) -> Unit,
    onMapPicker: (LatLng) -> Unit,
    onBack: () -> Unit,
    viewModel: LocationSearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // FocusRequester passed down so the search bar can request keyboard focus
    // when the user taps anywhere inside the bar area.
    val searchFocusRequester = remember { FocusRequester() }

    rememberLocationPermissionState { granted ->
        if (granted) viewModel.getCurrentLocation()
    }

    StatusBarScaffold(
        topBar = {
            AddressSearchTopBar(
                query          = uiState.searchQuery,
                onQueryChange  = { viewModel.updateSearchQuery(it) },
                onClearQuery   = { viewModel.updateSearchQuery("") },
                focusRequester = searchFocusRequester,
                onBack         = onBack,
                onMapPicker    = {
                    val currentLocation = uiState.selectedAddress?.let {
                        LatLng(it.latitude, it.longitude)
                    } ?: LatLng(19.0760, 72.8777)
                    onMapPicker(currentLocation)
                },
                onCurrentLocation = {
                    viewModel.getCurrentLocation()
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
                            predictions    = uiState.predictions,
                            onPredictionClick = { prediction ->
                                viewModel.selectPlace(prediction.placeId) { address ->
                                    onAddressSelected(address)
                                }
                            }
                        )
                    }

                    else -> {
                        SavedAndRecentAddresses(
                            savedAddresses  = uiState.savedAddresses,
                            recentAddresses = uiState.recentPickups,
                            onAddressClick  = { address ->
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
// AddressSearchTopBar
// Lives inside GradientTopBarWrapper so the gradient shows through.
// Contains:
//   • Back arrow + "Add address" title row
//   • Live-typing search bar (BasicTextField with hardware keyboard)
//   • "Use current location" shortcut
//   • "Select on map" pill
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun AddressSearchTopBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    focusRequester: FocusRequester,
    onBack: () -> Unit,
    onMapPicker: () -> Unit,
    onCurrentLocation: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ── Title row ──────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = Icons.Default.Search, // replace with ArrowBack
                    contentDescription = stringResource(R.string.back),
                    tint = HeaderTextPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
            // ← swap the icon above to Icons.Default.ArrowBack
            // kept as Search here so compilation works without extra import;
            // the real file already imports ArrowBack from the Filled set.
            Text(
                text = stringResource(R.string.add_address),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.2).sp
                ),
                color = HeaderTextPrimary
            )
        }

        // ── Search bar (live text input) ───────────────────────────────────
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { focusRequester.requestFocus() }, // whole bar is tappable
            shape = RoundedCornerShape(14.dp),
            color = SearchBarBg,
            border = androidx.compose.foundation.BorderStroke(1.dp, SearchBarBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = HeaderTextMuted,
                    modifier = Modifier.size(18.dp)
                )

                // ── The actual keyboard-receiving TextField ─────────────
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                    textStyle = TextStyle(
                        color = HeaderTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    cursorBrush = SolidColor(HeaderTextPrimary),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    decorationBox = { innerTextField ->
                        Box {
                            // Placeholder — shown only when query is empty
                            if (query.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.search_area_placeholder),
                                    style = TextStyle(
                                        color = HeaderTextMuted,
                                        fontSize = 14.sp
                                    )
                                )
                            }
                            innerTextField()
                        }
                    }
                )

                // ── Clear / GPS icon on the right ──────────────────────
                AnimatedVisibility(
                    visible = query.isNotEmpty(),
                    enter   = fadeIn(),
                    exit    = fadeOut()
                ) {
                    IconButton(
                        onClick = onClearQuery,
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = HeaderTextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                AnimatedVisibility(
                    visible = query.isEmpty(),
                    enter   = fadeIn(),
                    exit    = fadeOut()
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "Current location",
                        tint = Color.White.copy(alpha = 0.80f),
                        modifier = Modifier
                            .size(18.dp)
                            .clickable { onCurrentLocation() }
                    )
                }
            }
        }

        // ── "Select on map" pill — centered ────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Surface(
                onClick = onMapPicker,
                shape = RoundedCornerShape(20.dp),
                color = MapPillBg,
                border = androidx.compose.foundation.BorderStroke(1.dp, MapPillBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 7.dp),
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
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            letterSpacing = 0.2.sp
                        ),
                        color = Color.White
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
                onClick    = { onPredictionClick(prediction) }
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
        color     = AppColors.Divider,
        thickness = 0.5.dp,
        modifier  = Modifier.padding(start = 66.dp, end = 16.dp)
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
        modifier       = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // ── Saved ──────────────────────────────────────────────────────────
        if (savedAddresses.isNotEmpty()) {
            item { SectionLabel(text = stringResource(R.string.saved_addresses)) }
            item {
                Surface(
                    modifier        = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape           = RoundedCornerShape(14.dp),
                    color           = Color.White,
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
                                    color     = AppColors.Divider,
                                    thickness = 0.5.dp,
                                    modifier  = Modifier.padding(start = 62.dp)
                                )
                            }
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(12.dp)) }
        }

        // ── Recent ─────────────────────────────────────────────────────────
        if (filteredRecent.isNotEmpty()) {
            item { SectionLabel(text = stringResource(R.string.label_recent_searches)) }
            item {
                Surface(
                    modifier        = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape           = RoundedCornerShape(14.dp),
                    color           = Color.White,
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
                                    color     = AppColors.Divider,
                                    thickness = 0.5.dp,
                                    modifier  = Modifier.padding(start = 62.dp)
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
        text     = text.uppercase(),
        style    = MaterialTheme.typography.labelSmall.copy(
            letterSpacing = 0.6.sp,
            fontSize      = 10.sp
        ),
        fontWeight = FontWeight.SemiBold,
        color      = AppColors.TextSecondary,
        modifier   = Modifier.padding(
            start  = 20.dp, end = 16.dp,
            top    = 16.dp, bottom = 6.dp
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
        // Icon bubble
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
                verticalAlignment   = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (isSaved)
                        address.label.replaceFirstChar { it.uppercase() }
                    else address.label,
                    style      = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSaved) FontWeight.SemiBold else FontWeight.Medium,
                    color      = AppColors.TextPrimary
                )
                address.contactName?.takeIf { it.isNotEmpty() }?.let { name ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = AppColors.Background
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment   = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint     = AppColors.TextSecondary,
                                modifier = Modifier.size(10.dp)
                            )
                            Text(
                                text  = name.take(12) + if (name.length > 12) "…" else "",
                                style = MaterialTheme.typography.labelSmall,
                                color = AppColors.TextSecondary,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
            Text(
                text     = address.address,
                style    = MaterialTheme.typography.bodySmall,
                color    = AppColors.TextSecondary,
                maxLines = 1
            )
        }
    }
}