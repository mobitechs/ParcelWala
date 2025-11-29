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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.mobitechs.parcelwala.data.model.request.SavedAddress
import com.mobitechs.parcelwala.ui.components.PrimaryButton
import com.mobitechs.parcelwala.ui.theme.AppColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Map Picker Screen
 * Allows user to select location by moving map
 * Creates SavedAddress with proper latitude/longitude
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapPickerScreen(
    initialLocation: LatLng,
    onLocationSelected: (SavedAddress) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Camera position state
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialLocation, 16f)
    }

    // Current selected location (center of map)
    var selectedLocation by remember { mutableStateOf(initialLocation) }
    var currentAddress by remember { mutableStateOf("Move map to select location") }
    var isLoading by remember { mutableStateOf(false) }

    // Update selected location when camera moves
    LaunchedEffect(cameraPositionState.isMoving) {
        if (!cameraPositionState.isMoving) {
            selectedLocation = cameraPositionState.position.target
            // Reverse geocode to get address
            isLoading = true
            scope.launch {
                val address = getAddressFromLatLng(
                    context = context,
                    latLng = selectedLocation
                )
                currentAddress = address
                isLoading = false
            }
        }
    }

    // Initial geocoding
    LaunchedEffect(Unit) {
        isLoading = true
        val address = getAddressFromLatLng(context, initialLocation)
        currentAddress = address
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Select Location",
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
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Google Map
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = true,
                    myLocationButtonEnabled = false
                )
            )

            // Center Pin Marker (fixed at center)
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Pin",
                    tint = AppColors.Primary,
                    modifier = Modifier
                        .size(48.dp)
                        .offset(y = (-24).dp)  // Offset to place pin tip at center
                )
            }

            // Bottom Card with address and confirm button
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Location indicator
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
                                text = "Selected Location",
                                style = MaterialTheme.typography.labelMedium,
                                color = AppColors.TextSecondary
                            )
                            if (isLoading) {
                                Text(
                                    text = "Getting address...",
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

                    // Coordinates display - IMPORTANT for verification
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
                                    text = "Latitude",
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
                                    text = "Longitude",
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

                    // Confirm Button
                    PrimaryButton(
                        text = "Confirm Location",
                        onClick = {
                            // ✅ Create SavedAddress with PROPER latitude and longitude
                            val savedAddress = SavedAddress(
                                addressId = "map_${System.currentTimeMillis()}",
                                addressType = "other",
                                label = "Selected Location",
                                address = currentAddress,
                                landmark = null,
                                latitude = selectedLocation.latitude,    // ✅ IMPORTANT
                                longitude = selectedLocation.longitude,  // ✅ IMPORTANT
                                contactName = null,
                                contactPhone = null,
                                isDefault = false,
                                flatNumber = null,
                                buildingName = null,
                                pincode = null
                            )
                            onLocationSelected(savedAddress)
                        },
                        icon = Icons.Default.Check,
                        enabled = !isLoading && currentAddress.isNotEmpty() && currentAddress != "Move map to select location",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // My Location Button
            FloatingActionButton(
                onClick = {
                    // TODO: Get current location and move camera
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
                containerColor = Color.White
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "My Location",
                    tint = AppColors.Primary
                )
            }
        }
    }
}

/**
 * Get address string from LatLng using Geocoder
 */
private suspend fun getAddressFromLatLng(
    context: android.content.Context,
    latLng: LatLng
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
                "Unknown location"
            }
        } catch (e: Exception) {
            "Unable to get address"
        }
    }
}