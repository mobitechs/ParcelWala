// ui/screens/booking/MapPickerScreen.kt
package com.mobitechs.parcelwala.ui.screens.booking

import android.location.Geocoder
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.mobitechs.parcelwala.R
import com.mobitechs.parcelwala.data.model.request.SavedAddress
import com.mobitechs.parcelwala.ui.components.PrimaryButton
import com.mobitechs.parcelwala.ui.theme.AppColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapPickerScreen(
    initialLocation: LatLng,
    onLocationSelected: (SavedAddress) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialLocation, 16f)
    }

    val moveMapHint = stringResource(R.string.label_move_map_hint)
    val selectedLocationLabel = stringResource(R.string.label_selected_location)
    val unknownLocationText = stringResource(R.string.label_unknown_location)
    val unableGetAddressText = stringResource(R.string.label_unable_get_address)

    var selectedLocation by remember { mutableStateOf(initialLocation) }
    var currentAddress by remember { mutableStateOf(moveMapHint) }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(cameraPositionState.isMoving) {
        if (!cameraPositionState.isMoving) {
            selectedLocation = cameraPositionState.position.target
            isLoading = true
            scope.launch {
                val address = getAddressFromLatLng(
                    context = context,
                    latLng = selectedLocation,
                    unknownText = unknownLocationText,
                    errorText = unableGetAddressText
                )
                currentAddress = address
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        isLoading = true
        val address = getAddressFromLatLng(
            context, initialLocation,
            unknownText = unknownLocationText,
            errorText = unableGetAddressText
        )
        currentAddress = address
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.title_select_location),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.content_desc_back),
                            tint = AppColors.TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppColors.Surface
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = true,
                    myLocationButtonEnabled = false
                )
            )

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = stringResource(R.string.content_desc_pin),
                    tint = AppColors.Primary,
                    modifier = Modifier
                        .size(48.dp)
                        .offset(y = (-24).dp)
                )
            }

            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    color = AppColors.Primary.copy(alpha = 0.1f),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = AppColors.Primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = selectedLocationLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = AppColors.TextSecondary
                            )
                            if (isLoading) {
                                Text(
                                    text = stringResource(R.string.label_getting_address),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = AppColors.TextHint
                                )
                            } else {
                                Text(
                                    text = currentAddress,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = AppColors.TextPrimary,
                                    maxLines = 2
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = AppColors.Background
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = stringResource(R.string.label_latitude),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AppColors.TextHint
                                )
                                Text(
                                    text = String.format("%.6f", selectedLocation.latitude),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AppColors.TextPrimary
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = stringResource(R.string.label_longitude),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AppColors.TextHint
                                )
                                Text(
                                    text = String.format("%.6f", selectedLocation.longitude),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AppColors.TextPrimary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    PrimaryButton(
                        text = stringResource(R.string.label_confirm_location),
                        onClick = {
                            val savedAddress = SavedAddress(
                                addressId = "map_${System.currentTimeMillis()}",
                                addressType = "Other",
                                label = selectedLocationLabel,
                                address = currentAddress,
                                landmark = null,
                                latitude = selectedLocation.latitude,
                                longitude = selectedLocation.longitude,
                                contactName = null,
                                contactPhone = null,
                                isDefault = false,
                                buildingDetails = null,
                                pincode = null
                            )
                            onLocationSelected(savedAddress)
                        },
                        icon = Icons.Default.Check,
                        enabled = !isLoading && currentAddress.isNotEmpty() && currentAddress != moveMapHint,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            FloatingActionButton(
                onClick = {
                    // TODO: Get current location and move camera
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
                containerColor = AppColors.Surface
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = stringResource(R.string.content_desc_my_location),
                    tint = AppColors.Primary
                )
            }
        }
    }
}

private suspend fun getAddressFromLatLng(
    context: android.content.Context,
    latLng: LatLng,
    unknownText: String = "Unknown location",
    errorText: String = "Unable to get address"
): String {
    return withContext(Dispatchers.IO) {
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(
                latLng.latitude,
                latLng.longitude,
                1
            )
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                buildString {
                    address.getAddressLine(0)?.let { append(it) }
                }
            } else {
                unknownText
            }
        } catch (e: Exception) {
            errorText
        }
    }
}