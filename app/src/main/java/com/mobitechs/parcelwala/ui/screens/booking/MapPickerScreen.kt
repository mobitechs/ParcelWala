package com.mobitechs.parcelwala.ui.screens.booking

import android.location.Geocoder
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState
import com.mobitechs.parcelwala.R
import com.mobitechs.parcelwala.data.model.request.SavedAddress
import com.mobitechs.parcelwala.ui.components.PrimaryButton
import com.mobitechs.parcelwala.ui.components.StatusBarScaffold
import com.mobitechs.parcelwala.ui.theme.AppColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

// ── Local colour tokens (header palette) ─────────────────────────────────────
private val HeaderBg          = AppColors.PrimaryDeep        // same deep navy as rest of app
private val HeaderText        = Color.White
private val HeaderTextMuted   = Color.White.copy(alpha = 0.60f)
private val IndicatorBg       = Color.White.copy(alpha = 0.12f)
private val IndicatorBorder   = Color.White.copy(alpha = 0.22f)

// ══════════════════════════════════════════════════════════════════════════════
// MapPickerScreen  —  Variation 3: dark header + live address indicator + split
// ══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapPickerScreen(
    initialLocation: LatLng,
    onLocationSelected: (SavedAddress) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialLocation, 16f)
    }

    // String resources captured before coroutine context
    val moveMapHint       = stringResource(R.string.label_move_map_hint)
    val selectedLocLabel  = stringResource(R.string.selected_location)
    val unknownLocText    = stringResource(R.string.label_unknown_location)
    val unableAddrText    = stringResource(R.string.label_unable_get_address)

    var selectedLocation  by remember { mutableStateOf(initialLocation) }
    var currentAddress    by remember { mutableStateOf(moveMapHint) }
    var isLoading         by remember { mutableStateOf(false) }

    // Geocode whenever the camera stops moving
    LaunchedEffect(cameraPositionState.isMoving) {
        if (!cameraPositionState.isMoving) {
            selectedLocation = cameraPositionState.position.target
            isLoading = true
            scope.launch {
                currentAddress = getAddressFromLatLng(
                    context    = context,
                    latLng     = selectedLocation,
                    unknownText = unknownLocText,
                    errorText  = unableAddrText
                )
                isLoading = false
            }
        }
    }

    // Geocode the initial location on first load
    LaunchedEffect(Unit) {
        isLoading = true
        currentAddress = getAddressFromLatLng(
            context, initialLocation,
            unknownText = unknownLocText,
            errorText   = unableAddrText
        )
        isLoading = false
    }

    // StatusBarScaffold gives us the gradient wrapper consistent with the app
    StatusBarScaffold(
        topBar = {
            MapPickerTopBar(
                currentAddress = currentAddress,
                isLoading      = isLoading,
                moveMapHint    = moveMapHint,
                onBack         = onBack
            )
        },
        containerColor = AppColors.Background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            // ── Map fills the remaining space ──────────────────────────────
            Box(modifier = Modifier.weight(1f)) {

                GoogleMap(
                    modifier            = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    uiSettings          = MapUiSettings(
                        zoomControlsEnabled     = false,
                        myLocationButtonEnabled = false
                    )
                )

                // ── Fixed centre pin ───────────────────────────────────────
                // Floats above the map centre; does NOT move with the camera
                Box(
                    modifier         = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // Pin head
                        Box(
                            modifier         = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(AppColors.PrimaryDeep),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector        = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint               = Color.White,
                                modifier           = Modifier.size(24.dp)
                            )
                        }
                        // Pin stem
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(12.dp)
                                .background(AppColors.PrimaryDeep)
                        )
                        // Shadow
                        Box(
                            modifier = Modifier
                                .width(10.dp)
                                .height(3.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.15f))
                        )
                    }
                }

                // ── GPS FAB ────────────────────────────────────────────────
                FloatingActionButton(
                    onClick         = {
                        scope.launch {
                            cameraPositionState.animate(
                                CameraUpdateFactory.newLatLngZoom(initialLocation, 16f),
                                durationMs = 400
                            )
                        }
                    },
                    modifier        = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp),
                    containerColor  = Color.White,
                    contentColor    = AppColors.Primary,
                    elevation       = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 3.dp
                    ),
                    shape           = CircleShape
                ) {
                    Icon(
                        imageVector        = Icons.Default.MyLocation,
                        contentDescription = stringResource(R.string.content_desc_my_location),
                        modifier           = Modifier.size(20.dp)
                    )
                }
            }

            // ── Bottom white panel ─────────────────────────────────────────
            MapPickerBottomPanel(
                address          = currentAddress,
                latitude         = selectedLocation.latitude,
                longitude        = selectedLocation.longitude,
                isLoading        = isLoading,
                moveMapHint      = moveMapHint,
                selectedLocLabel = selectedLocLabel,
                onConfirm        = {
                    onLocationSelected(
                        SavedAddress(
                            addressId       = "map_${System.currentTimeMillis()}",
                            addressType     = "Other",
                            label           = selectedLocLabel,
                            address         = currentAddress,
                            landmark        = null,
                            latitude        = selectedLocation.latitude,
                            longitude       = selectedLocation.longitude,
                            contactName     = null,
                            contactPhone    = null,
                            isDefault       = false,
                            buildingDetails = null,
                            pincode         = null
                        )
                    )
                }
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// MapPickerTopBar
// Sits inside GradientTopBarWrapper. Shows back button + title on the first
// row, then a frosted live-address indicator bar below that updates as the
// pin moves.
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun MapPickerTopBar(
    currentAddress: String,
    isLoading: Boolean,
    moveMapHint: String,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ── Back + title row ───────────────────────────────────────────────
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick  = onBack,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.12f))
            ) {
                Icon(
                    imageVector        = Icons.Default.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    tint               = HeaderText,
                    modifier           = Modifier.size(20.dp)
                )
            }
            Text(
                text       = stringResource(R.string.title_select_location),
                style      = MaterialTheme.typography.titleMedium.copy(
                    fontWeight    = FontWeight.Bold,
                    letterSpacing = (-0.2).sp
                ),
                color      = HeaderText
            )
        }

        // ── Live address indicator ─────────────────────────────────────────
        // Shows a pulsing green dot + address text while geocoding resolves.
        // Crossfades between loading and resolved states.
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape    = RoundedCornerShape(12.dp),
            color    = IndicatorBg,
            border   = androidx.compose.foundation.BorderStroke(1.dp, IndicatorBorder)
        ) {
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Status dot — spins while loading, solid green when resolved
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(12.dp),
                        color       = Color.White.copy(alpha = 0.70f),
                        strokeWidth = 1.5.dp
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF34D399)) // emerald-400
                    )
                }

                // Address text — crossfades when it changes
                AnimatedContent(
                    targetState = currentAddress,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    modifier    = Modifier.weight(1f),
                    label       = "address"
                ) { addr ->
                    Text(
                        text     = addr,
                        style    = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color    = if (addr == moveMapHint) HeaderTextMuted else HeaderText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Refresh icon hint — shows when not loading
                if (!isLoading && currentAddress != moveMapHint) {
                    Icon(
                        imageVector        = Icons.Default.Refresh,
                        contentDescription = null,
                        tint               = HeaderTextMuted,
                        modifier           = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// MapPickerBottomPanel
// White panel pinned to the bottom. Shows:
//   • "Pinned location" section label
//   • Resolved address with location icon
//   • Lat / Lng in two outlined boxes
//   • Confirm button (disabled while loading or on the move-hint placeholder)
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun MapPickerBottomPanel(
    address: String,
    latitude: Double,
    longitude: Double,
    isLoading: Boolean,
    moveMapHint: String,
    selectedLocLabel: String,
    onConfirm: () -> Unit
) {
    Surface(
        color           = Color.White,
        shadowElevation = 12.dp,
        modifier        = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ── Section label ──────────────────────────────────────────────
            Text(
                text       = "Pinned location".uppercase(),
                style      = MaterialTheme.typography.labelSmall.copy(
                    fontSize      = 9.sp,
                    letterSpacing = 0.7.sp
                ),
                fontWeight = FontWeight.Bold,
                color      = AppColors.TextSecondary
            )

            // ── Address row ────────────────────────────────────────────────
            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Tinted icon circle
                Box(
                    modifier         = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(AppColors.Primary.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector        = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint               = AppColors.Primary,
                        modifier           = Modifier.size(18.dp)
                    )
                }
                Column(
                    modifier            = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (isLoading) {
                        Text(
                            text  = stringResource(R.string.label_getting_address),
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = AppColors.TextSecondary
                        )
                    } else {
                        Text(
                            text       = address.ifEmpty { selectedLocLabel },
                            style      = MaterialTheme.typography.bodyMedium.copy(
                                fontSize   = 13.sp,
                                lineHeight = 18.sp
                            ),
                            fontWeight = FontWeight.SemiBold,
                            color      = if (address == moveMapHint) AppColors.TextSecondary
                            else AppColors.TextPrimary,
                            maxLines   = 2,
                            overflow   = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // ── Lat / Lng coordinate boxes ─────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CoordBox(
                    label    = stringResource(R.string.label_latitude),
                    value    = String.format("%.6f", latitude),
                    modifier = Modifier.weight(1f)
                )
                CoordBox(
                    label    = stringResource(R.string.label_longitude),
                    value    = String.format("%.6f", longitude),
                    modifier = Modifier.weight(1f)
                )
            }

            // ── Confirm CTA ────────────────────────────────────────────────
            PrimaryButton(
                text     = stringResource(R.string.label_confirm_location),
                onClick  = onConfirm,
                icon     = Icons.Default.Check,
                enabled  = !isLoading
                        && address.isNotEmpty()
                        && address != moveMapHint,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// CoordBox — labelled lat or lng value in an outlined rounded box
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun CoordBox(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape    = RoundedCornerShape(10.dp),
        color    = AppColors.Background,
        border   = androidx.compose.foundation.BorderStroke(0.5.dp, AppColors.Divider)
    ) {
        Column(
            modifier            = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text  = label.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize      = 8.sp,
                    letterSpacing = 0.5.sp
                ),
                color = AppColors.TextSecondary
            )
            Text(
                text       = value,
                style      = MaterialTheme.typography.bodySmall.copy(
                    fontSize   = 12.sp,
                    fontWeight = FontWeight.Bold
                ),
                color      = AppColors.TextPrimary
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// getAddressFromLatLng — reverse-geocode on IO dispatcher
// ══════════════════════════════════════════════════════════════════════════════

private suspend fun getAddressFromLatLng(
    context: android.content.Context,
    latLng: LatLng,
    unknownText: String = "Unknown location",
    errorText: String   = "Unable to get address"
): String = withContext(Dispatchers.IO) {
    try {
        val geocoder  = Geocoder(context, Locale.getDefault())
        @Suppress("DEPRECATION")
        val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
        if (!addresses.isNullOrEmpty()) {
            buildString { addresses[0].getAddressLine(0)?.let { append(it) } }
        } else unknownText
    } catch (e: Exception) {
        errorText
    }
}