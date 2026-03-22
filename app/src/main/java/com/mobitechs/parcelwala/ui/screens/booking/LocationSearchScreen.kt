package com.mobitechs.parcelwala.ui.screens.booking

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NorthWest
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.maps.model.LatLng
import com.mobitechs.parcelwala.R
import com.mobitechs.parcelwala.data.model.request.SavedAddress
import com.mobitechs.parcelwala.data.model.request.SearchHistory
import com.mobitechs.parcelwala.data.model.response.PlaceAutocomplete
import com.mobitechs.parcelwala.ui.components.ErrorMessageCard
import com.mobitechs.parcelwala.ui.components.LoadingIndicator
import com.mobitechs.parcelwala.ui.components.StatusBarScaffold
import com.mobitechs.parcelwala.ui.theme.AppColors
import com.mobitechs.parcelwala.ui.viewmodel.LocationSearchViewModel
import com.mobitechs.parcelwala.utils.DateTimeUtils
import com.mobitechs.parcelwala.utils.rememberLocationPermissionState
import kotlinx.coroutines.delay

// ── Local colour tokens ───────────────────────────────────────────────────────
private val SearchBarBg     = Color.White.copy(alpha = 0.13f)
private val SearchBarBorder = Color.White.copy(alpha = 0.25f)
private val HeaderText      = Color.White
private val HeaderTextMuted = Color.White.copy(alpha = 0.60f)

// ══════════════════════════════════════════════════════════════════════════════
// LocationSearchScreen  —  Variation 2: dark header + all-white flat list
// ══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationSearchScreen(
    locationType: String,
    onAddressSelected: (SavedAddress) -> Unit,
    onMapPicker: (LatLng) -> Unit,
    onBack: () -> Unit,
    viewModel: LocationSearchViewModel = hiltViewModel()
) {
    val uiState        by viewModel.uiState.collectAsState()
    val focusRequester  = remember { FocusRequester() }

    val permissionGranted = rememberLocationPermissionState { granted ->
        if (granted) viewModel.getCurrentLocation()
    }

    // Auto-focus the search field so the keyboard opens immediately
    LaunchedEffect(Unit) {
        delay(100)
        focusRequester.requestFocus()
    }

    StatusBarScaffold(
        topBar = {
            LocationSearchTopBar(
                locationType   = locationType,
                query          = uiState.searchQuery,
                onQueryChange  = { viewModel.updateSearchQuery(it) },
                onClearQuery   = { viewModel.updateSearchQuery("") },
                focusRequester = focusRequester,
                onBack         = onBack
            )
        },
        // White body — the dark header provides all the contrast needed
        containerColor = Color.White
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            // ── Main content area ──────────────────────────────────────────
            Box(modifier = Modifier.weight(1f)) {
                when {
                    uiState.isLoading || uiState.isLoadingPredictions -> {
                        LoadingIndicator(
                            message  = stringResource(R.string.label_searching),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp)
                        )
                    }

                    uiState.predictions.isNotEmpty() && uiState.searchQuery.isNotBlank() -> {
                        AutocompleteResultsList(
                            predictions     = uiState.predictions,
                            onPredictionClick = { prediction ->
                                viewModel.selectPlace(prediction.placeId) { address ->
                                    onAddressSelected(address)
                                }
                            }
                        )
                    }

                    else -> {
                        DefaultContentList(
                            uiState         = uiState,
                            onHistoryClick  = { history ->
                                viewModel.selectFromHistory(history) { address ->
                                    onAddressSelected(address)
                                }
                            },
                            onClearHistory  = { viewModel.clearSearchHistory() },
                            onAddressClick  = { address ->
                                viewModel.selectAddress(address)
                                onAddressSelected(address)
                            }
                        )
                    }
                }
            }

            // ── Error card (non-blocking, sits above the bottom bar) ───────
            uiState.error?.let { error ->
                ErrorMessageCard(
                    message  = error,
                    onRetry  = { viewModel.clearError() },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // ── Bottom action bar ──────────────────────────────────────────
            BottomActionBar(
                onCurrentLocation = {
                    if (permissionGranted.value) {
                        viewModel.getCurrentLocation()
                        uiState.selectedAddress?.let { onAddressSelected(it) }
                    }
                },
                onMapPicker = {
                    val latLng = uiState.selectedAddress?.let {
                        LatLng(it.latitude, it.longitude)
                    } ?: LatLng(19.0760, 72.8777)
                    onMapPicker(latLng)
                }
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// LocationSearchTopBar
// Lives inside GradientTopBarWrapper — gradient shows through.
// Title + subtitle + live BasicTextField search bar.
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun LocationSearchTopBar(
    locationType: String,
    query: String,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    focusRequester: FocusRequester,
    onBack: () -> Unit
) {
    val isPickup = locationType == "pickup"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ── Title row ──────────────────────────────────────────────────────
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Back button — circle ghost
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.12f))
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = Icons.Default.NorthWest, // swap to ArrowBack
                    contentDescription = stringResource(R.string.back),
                    tint               = HeaderText,
                    modifier           = Modifier.size(16.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = if (isPickup)
                        stringResource(R.string.label_pickup_location)
                    else
                        stringResource(R.string.label_drop_location),
                    style      = MaterialTheme.typography.titleMedium.copy(
                        fontWeight    = FontWeight.Bold,
                        letterSpacing = (-0.2).sp
                    ),
                    color      = HeaderText
                )
                Text(
                    text  = stringResource(R.string.search_area_placeholder),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = HeaderTextMuted
                )
            }
        }

        // ── Live search bar ────────────────────────────────────────────────
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { focusRequester.requestFocus() },
            shape  = RoundedCornerShape(14.dp),
            color  = SearchBarBg,
            border = androidx.compose.foundation.BorderStroke(1.dp, SearchBarBorder)
        ) {
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector        = Icons.Default.Search,
                    contentDescription = null,
                    tint               = HeaderTextMuted,
                    modifier           = Modifier.size(18.dp)
                )

                BasicTextField(
                    value         = query,
                    onValueChange = onQueryChange,
                    modifier      = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                    textStyle     = TextStyle(
                        color      = HeaderText,
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    cursorBrush   = SolidColor(HeaderText),
                    singleLine    = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    decorationBox = { inner ->
                        Box {
                            if (query.isEmpty()) {
                                Text(
                                    text  = if (locationType == "pickup")
                                        stringResource(R.string.label_search_pickup)
                                    else
                                        stringResource(R.string.label_search_drop),
                                    style = TextStyle(
                                        color    = HeaderTextMuted,
                                        fontSize = 14.sp
                                    )
                                )
                            }
                            inner()
                        }
                    }
                )

                // Clear icon when typing; GPS icon when idle
                if (query.isNotEmpty()) {
                    Icon(
                        imageVector        = Icons.Default.Close,
                        contentDescription = "Clear",
                        tint               = HeaderTextMuted,
                        modifier           = Modifier
                            .size(18.dp)
                            .clickable { onClearQuery() }
                    )
                } else {
                    Icon(
                        imageVector        = Icons.Default.MyLocation,
                        contentDescription = null,
                        tint               = Color.White.copy(alpha = 0.80f),
                        modifier           = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// DefaultContentList
// Shows history → saved addresses → recent pickups as a single flat
// divider-separated list. Section labels act as anchors.
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun DefaultContentList(
    uiState: com.mobitechs.parcelwala.ui.viewmodel.LocationSearchUiState,
    onHistoryClick: (SearchHistory) -> Unit,
    onClearHistory: () -> Unit,
    onAddressClick: (SavedAddress) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {

        // ── Search history ─────────────────────────────────────────────────
        if (uiState.searchHistory.isNotEmpty()) {
            item {
                FlatSectionLabel(
                    title       = stringResource(R.string.label_recent_searches),
                    trailingAction = {
                        TextButton(onClick = onClearHistory) {
                            Text(
                                text  = stringResource(R.string.label_clear),
                                style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp),
                                color = AppColors.Drop
                            )
                        }
                    }
                )
            }
            itemsIndexed(uiState.searchHistory.take(5)) { index, history ->
                HistoryRow(
                    history  = history,
                    onClick  = { onHistoryClick(history) },
                    showDivider = index < minOf(uiState.searchHistory.size, 5) - 1
                )
            }
            item { Spacer(modifier = Modifier.height(8.dp)) }
        }

        // ── Saved addresses ────────────────────────────────────────────────
        if (uiState.savedAddresses.isNotEmpty()) {
            item {
                FlatSectionLabel(title = stringResource(R.string.label_saved_addresses))
            }
            itemsIndexed(uiState.savedAddresses.take(3)) { index, address ->
                SavedAddressRow(
                    address     = address,
                    onClick     = { onAddressClick(address) },
                    showDivider = index < minOf(uiState.savedAddresses.size, 3) - 1
                )
            }
            item { Spacer(modifier = Modifier.height(8.dp)) }
        }

        // ── Recent pickups ─────────────────────────────────────────────────
        if (uiState.recentPickups.isNotEmpty()) {
            item {
                FlatSectionLabel(title = stringResource(R.string.label_recent_pickups))
            }
            itemsIndexed(uiState.recentPickups.take(4)) { index, pickup ->
                RecentPickupRow(
                    address     = pickup,
                    onClick     = { onAddressClick(pickup) },
                    showDivider = index < minOf(uiState.recentPickups.size, 4) - 1
                )
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Autocomplete results list (shown while user is typing)
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun AutocompleteResultsList(
    predictions: List<PlaceAutocomplete>,
    onPredictionClick: (PlaceAutocomplete) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        itemsIndexed(predictions) { index, prediction ->
            AutocompleteRow(
                prediction  = prediction,
                onClick     = { onPredictionClick(prediction) },
                showDivider = index < predictions.lastIndex
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Row composables — all flat (no card wrapper), divider-separated
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun HistoryRow(
    history: SearchHistory,
    onClick: () -> Unit,
    showDivider: Boolean
) {
    Column {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Clock icon in a neutral circle
            IconCircle(
                icon = Icons.Default.History,
                bg   = AppColors.Background,
                tint = AppColors.TextSecondary
            )

            Column(modifier = Modifier.weight(1f)) {
                if (history.label.isNotEmpty()) {
                    Text(
                        text       = history.label,
                        style      = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                        fontWeight = FontWeight.SemiBold,
                        color      = AppColors.TextPrimary,
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis
                    )
                }
                // Address + relative time merged into one subtitle line
                val subtitle = buildString {
                    append(history.address)
                    val rel = DateTimeUtils.formatRelativeTime(history.timestamp)
                    if (rel.isNotEmpty()) append(" · $rel")
                }
                Text(
                    text     = subtitle,
                    style    = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color    = AppColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // NW arrow — "use this result" affordance
            Icon(
                imageVector        = Icons.Default.NorthWest,
                contentDescription = null,
                tint               = AppColors.TextSecondary.copy(alpha = 0.40f),
                modifier           = Modifier.size(16.dp)
            )
        }
        if (showDivider) {
            HorizontalDivider(
                color     = AppColors.Divider,
                thickness = 0.5.dp,
                modifier  = Modifier.padding(start = 60.dp, end = 16.dp)
            )
        }
    }
}

@Composable
private fun SavedAddressRow(
    address: SavedAddress,
    onClick: () -> Unit,
    showDivider: Boolean
) {
    val type = address.addressType.lowercase()
    val (bg, tint) = when (type) {
        "home"        -> Color(0xFFECFDF5) to Color(0xFF059669)
        "work", "shop"-> Color(0xFFFEF9C3) to Color(0xFFD97706)
        else          -> AppColors.Background to AppColors.Primary
    }
    val icon = when (type) {
        "home"        -> Icons.Default.Place   // swap to Icons.Default.Home
        "work", "shop"-> Icons.Default.Place   // swap to Icons.Default.Store
        else          -> Icons.Default.Place
    }

    Column {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconCircle(icon = icon, bg = bg, tint = tint)

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = address.label.replaceFirstChar { it.uppercase() },
                    style      = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                    fontWeight = FontWeight.SemiBold,
                    color      = AppColors.TextPrimary,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis
                )
                Text(
                    text     = address.address,
                    style    = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color    = AppColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // "Saved" green badge
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFFECFDF5)
            ) {
                Text(
                    text     = "Saved",
                    style    = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color    = Color(0xFF15803D),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                )
            }
        }
        if (showDivider) {
            HorizontalDivider(
                color     = AppColors.Divider,
                thickness = 0.5.dp,
                modifier  = Modifier.padding(start = 60.dp, end = 16.dp)
            )
        }
    }
}

@Composable
private fun RecentPickupRow(
    address: SavedAddress,
    onClick: () -> Unit,
    showDivider: Boolean
) {
    Column {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconCircle(
                icon = Icons.Default.History,
                bg   = AppColors.Background,
                tint = AppColors.TextSecondary
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = address.label.ifEmpty { address.contactName ?: "Pickup" },
                    style      = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                    fontWeight = FontWeight.SemiBold,
                    color      = AppColors.TextPrimary,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis
                )
                Text(
                    text     = address.address,
                    style    = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color    = AppColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                // Contact name + phone if available
                address.contactName?.takeIf { it.isNotEmpty() }?.let { name ->
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector        = Icons.Default.Person,
                            contentDescription = null,
                            tint               = AppColors.TextSecondary.copy(alpha = 0.50f),
                            modifier           = Modifier.size(11.dp)
                        )
                        Text(
                            text  = "$name • ${address.contactPhone ?: ""}".trimEnd(' ', '•'),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = AppColors.TextSecondary
                        )
                    }
                }
            }

            Icon(
                imageVector        = Icons.Default.NorthWest,
                contentDescription = null,
                tint               = AppColors.TextSecondary.copy(alpha = 0.40f),
                modifier           = Modifier.size(16.dp)
            )
        }
        if (showDivider) {
            HorizontalDivider(
                color     = AppColors.Divider,
                thickness = 0.5.dp,
                modifier  = Modifier.padding(start = 60.dp, end = 16.dp)
            )
        }
    }
}

@Composable
private fun AutocompleteRow(
    prediction: PlaceAutocomplete,
    onClick: () -> Unit,
    showDivider: Boolean
) {
    Column {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconCircle(
                icon = Icons.Default.Place,
                bg   = AppColors.Primary.copy(alpha = 0.08f),
                tint = AppColors.Primary
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = prediction.primaryText,
                    style      = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                    fontWeight = FontWeight.SemiBold,
                    color      = AppColors.TextPrimary,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis
                )
                prediction.secondaryText?.let { secondary ->
                    Text(
                        text     = secondary,
                        style    = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color    = AppColors.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Icon(
                imageVector        = Icons.Default.NorthWest,
                contentDescription = null,
                tint               = AppColors.TextSecondary.copy(alpha = 0.40f),
                modifier           = Modifier.size(16.dp)
            )
        }
        if (showDivider) {
            HorizontalDivider(
                color     = AppColors.Divider,
                thickness = 0.5.dp,
                modifier  = Modifier.padding(start = 60.dp, end = 16.dp)
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Section label — flat, no card, with optional trailing action
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun FlatSectionLabel(
    title: String,
    trailingAction: @Composable (() -> Unit)? = null
) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 16.dp, bottom = 2.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text  = title.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize      = 9.sp,
                letterSpacing = 0.7.sp
            ),
            fontWeight = FontWeight.Bold,
            color      = AppColors.TextSecondary
        )
        trailingAction?.invoke()
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// IconCircle — reusable tinted circle icon used by all row types
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun IconCircle(
    icon: ImageVector,
    bg: Color,
    tint: Color
) {
    Box(
        modifier         = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = null,
            tint               = tint,
            modifier           = Modifier.size(18.dp)
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// BottomActionBar — Current location + On map buttons above system nav
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun BottomActionBar(
    onCurrentLocation: () -> Unit,
    onMapPicker: () -> Unit
) {
    Surface(
        color           = Color.White,
        shadowElevation = 8.dp,
        modifier        = Modifier.navigationBarsPadding()
    ) {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Current location — outlined secondary style
            Surface(
                onClick  = onCurrentLocation,
                modifier = Modifier.weight(1f),
                shape    = RoundedCornerShape(12.dp),
                color    = AppColors.Background,
                border   = androidx.compose.foundation.BorderStroke(
                    0.5.dp, AppColors.Divider
                )
            ) {
                Row(
                    modifier              = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 11.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector        = Icons.Default.MyLocation,
                        contentDescription = null,
                        tint               = AppColors.Primary,
                        modifier           = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text       = stringResource(R.string.label_current),
                        style      = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                        fontWeight = FontWeight.SemiBold,
                        color      = AppColors.TextPrimary
                    )
                }
            }

            // On map — filled primary style
            Surface(
                onClick  = onMapPicker,
                modifier = Modifier.weight(1f),
                shape    = RoundedCornerShape(12.dp),
                color    = AppColors.Primary
            ) {
                Row(
                    modifier              = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 11.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector        = Icons.Default.Place,
                        contentDescription = null,
                        tint               = Color.White,
                        modifier           = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text       = stringResource(R.string.label_on_map),
                        style      = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                        fontWeight = FontWeight.SemiBold,
                        color      = Color.White
                    )
                }
            }
        }
    }
}