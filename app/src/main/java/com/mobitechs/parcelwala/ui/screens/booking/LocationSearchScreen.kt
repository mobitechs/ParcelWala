package com.mobitechs.parcelwala.ui.screens.booking

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
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
import java.util.Locale

// ── Local colour tokens ───────────────────────────────────────────────────────
private val SearchBarBg     = Color.White.copy(alpha = 0.13f)
private val SearchBarBorder = Color.White.copy(alpha = 0.25f)
private val HeaderText      = Color.White
private val HeaderTextMuted = Color.White.copy(alpha = 0.60f)

// ══════════════════════════════════════════════════════════════════════════════
// LocationSearchScreen
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

    // ── Speech-to-text launcher ──────────────────────────────────────────────
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            matches?.firstOrNull()?.let { spokenText ->
                viewModel.updateSearchQuery(spokenText)
            }
        }
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
                onBack         = onBack,
                onMicClick     = {
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(
                            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                        )
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                        putExtra(RecognizerIntent.EXTRA_PROMPT, "Search for a location…")
                    }
                    speechLauncher.launch(intent)
                }
            )
        },
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
                            predictions       = uiState.predictions,
                            onPredictionClick = { prediction ->
                                viewModel.selectPlace(prediction.placeId) { address ->
                                    onAddressSelected(address)
                                }
                            }
                        )
                    }

                    else -> {
                        DefaultContentList(
                            uiState              = uiState,
                            onUseCurrentLocation = {
                                // Request permission if needed, then fetch + navigate
                                if (permissionGranted.value) {
                                    viewModel.getCurrentLocation()
                                    // Once location is fetched, selectedAddress is populated
                                    uiState.selectedAddress?.let { onAddressSelected(it) }
                                } else {
                                    // Trigger permission request (callback above handles the rest)
                                    viewModel.getCurrentLocation()
                                }
                            },
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

            // ── Error card ─────────────────────────────────────────────────
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
// CHANGES:
//   • Added statusBarHeight top padding so header doesn't crowd the status bar
//   • Back icon changed to ArrowBack (← standard Android back)
//   • Mic icon added at end of search bar; shows when query is empty,
//     replaces MyLocation GPS icon (GPS moved to bottom bar only)
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun LocationSearchTopBar(
    locationType: String,
    query: String,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    focusRequester: FocusRequester,
    onBack: () -> Unit,
    onMicClick: () -> Unit
) {
    val isPickup = locationType == "pickup"

    // ── Respect the status bar height so content isn't clipped ──────────────
    // GradientTopBarWrapper adds a Spacer for status bar, but we still add
    // a small extra top gap here for breathing room.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            // top = 8.dp gives comfortable gap below the status bar spacer
            .padding(top = 8.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── Title row ──────────────────────────────────────────────────────
        Row(
            modifier          = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ← Standard back button
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.14f))
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = Icons.Default.ArrowBack,   // ← FIXED
                    contentDescription = stringResource(R.string.back),
                    tint               = HeaderText,
                    modifier           = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text  = if (isPickup)
                        stringResource(R.string.label_pickup_location)
                    else
                        stringResource(R.string.label_drop_location),
                    // titleMedium ≈ 16sp Bold — matches standard toolbar title size
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight    = FontWeight.Bold,
                        letterSpacing = (-0.3).sp
                    ),
                    color = HeaderText
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

                // ── Trailing icon: ✕ while typing, 🎤 mic when idle ────────
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
                    // Mic icon — tap to speak
                    Icon(
                        imageVector        = Icons.Default.Mic,
                        contentDescription = "Search by voice",
                        tint               = Color.White.copy(alpha = 0.85f),
                        modifier           = Modifier
                            .size(20.dp)
                            .clickable { onMicClick() }
                    )
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// DefaultContentList
// CHANGE: Added "Use Current Location" as the very first item above history.
// On click it triggers location fetch and passes the address to the next screen.
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun DefaultContentList(
    uiState: com.mobitechs.parcelwala.ui.viewmodel.LocationSearchUiState,
    onUseCurrentLocation: () -> Unit,
    onHistoryClick: (SearchHistory) -> Unit,
    onClearHistory: () -> Unit,
    onAddressClick: (SavedAddress) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {

        // ── Use Current Location ───────────────────────────────────────────
        item {
            UseCurrentLocationRow(onClick = onUseCurrentLocation)
            HorizontalDivider(
                color     = AppColors.Divider,
                thickness = 0.5.dp,
                modifier  = Modifier.padding(horizontal = 16.dp)
            )
        }

        // ── Search history ─────────────────────────────────────────────────
        if (uiState.searchHistory.isNotEmpty()) {
            item {
                FlatSectionLabel(
                    title = stringResource(R.string.label_recent_searches),
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
                    history     = history,
                    onClick     = { onHistoryClick(history) },
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
// UseCurrentLocationRow
// Prominent teal/primary row at the top of the list.
// Tapping fetches GPS location and auto-navigates to the confirm screen.
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun UseCurrentLocationRow(onClick: () -> Unit) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Filled primary circle with GPS icon
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(AppColors.Primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = Icons.Default.MyLocation,
                contentDescription = null,
                tint               = AppColors.Primary,
                modifier           = Modifier.size(20.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = "Use Current Location",
                style      = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                fontWeight = FontWeight.SemiBold,
                color      = AppColors.Primary
            )
            Text(
                text  = "Fetches your GPS location automatically",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = AppColors.TextSecondary
            )
        }

        Icon(
            imageVector        = Icons.Default.MyLocation,
            contentDescription = null,
            tint               = AppColors.Primary.copy(alpha = 0.50f),
            modifier           = Modifier.size(16.dp)
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Autocomplete results list
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
// Row composables
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
        "home"         -> Color(0xFFECFDF5) to Color(0xFF059669)
        "work", "shop" -> Color(0xFFFEF9C3) to Color(0xFFD97706)
        else           -> AppColors.Background to AppColors.Primary
    }
    val icon = when (type) {
        "home"         -> Icons.Default.Place
        "work", "shop" -> Icons.Default.Place
        else           -> Icons.Default.Place
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

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFFECFDF5)
            ) {
                Text(
                    text       = "Saved",
                    style      = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color      = Color(0xFF15803D),
                    fontWeight = FontWeight.Bold,
                    modifier   = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
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
// Section label
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
// IconCircle
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
// BottomActionBar
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