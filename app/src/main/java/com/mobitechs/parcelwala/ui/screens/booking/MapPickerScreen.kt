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
import androidx.compose.ui.text.style.TextOverflow
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
import java.util.*

/**
 * Map Picker Screen
 * Full-screen map for selecting precise location
 *
 * Features:
 * - Full-screen Google Map
 * - Center pin indicator
 * - Address display at bottom
 * - Confirm button
 * - ✅ Pinch-to-zoom support
 * - ✅ All map gestures enabled
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
        position = CameraPosition.fromLatLngZoom(initialLocation, 17f)
    }

    // Current address state
    var currentAddress by remember { mutableStateOf("Move map to select location") }
    var currentLocationName by remember { mutableStateOf("Selected Location") }
    var isLoadingAddress by remember { mutableStateOf(false) }
    var selectedLatLng by remember { mutableStateOf(initialLocation) }

    // ✅ Map UI Settings with all gestures enabled
    val mapUiSettings = remember {
        MapUiSettings(
            zoomControlsEnabled = true,           // Show +/- buttons
            zoomGesturesEnabled = true,           // ✅ Enable pinch-to-zoom
            scrollGesturesEnabled = true,         // ✅ Enable pan/scroll
            tiltGesturesEnabled = true,           // ✅ Enable tilt
            rotationGesturesEnabled = true,       // ✅ Enable rotation
            scrollGesturesEnabledDuringRotateOrZoom = true, // ✅ Allow scroll during zoom
            compassEnabled = true,                // Show compass
            myLocationButtonEnabled = false       // We'll add our own
        )
    }

    // Map properties
    val mapProperties = remember {
        MapProperties(
            isMyLocationEnabled = false,          // We handle this manually
            mapType = MapType.NORMAL
        )
    }

    // Geocode function
    fun geocodeLocation(latLng: LatLng) {
        isLoadingAddress = true
        scope.launch {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                val addresses = withContext(Dispatchers.IO) {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
                }

                if (!addresses.isNullOrEmpty()) {
                    val address = addresses[0]
                    currentAddress = address.getAddressLine(0) ?: "Unknown address"
                    currentLocationName = address.featureName
                        ?: address.subLocality
                                ?: address.locality
                                ?: "Selected Location"
                } else {
                    currentAddress = "Unable to determine address"
                    currentLocationName = "Selected Location"
                }
            } catch (e: Exception) {
                currentAddress = "Unable to determine address"
                currentLocationName = "Selected Location"
            } finally {
                isLoadingAddress = false
            }
        }
    }

    // Update address when camera stops moving
    LaunchedEffect(cameraPositionState.isMoving) {
        if (!cameraPositionState.isMoving) {
            selectedLatLng = cameraPositionState.position.target
            geocodeLocation(selectedLatLng)
        }
    }

    // Initial geocode
    LaunchedEffect(Unit) {
        geocodeLocation(initialLocation)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // ============ GOOGLE MAP ============
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            uiSettings = mapUiSettings,      // ✅ Apply UI settings with gestures
            properties = mapProperties
        )

        // ============ CENTER PIN (Fixed position) ============
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 48.dp), // Offset for pin shadow
            contentAlignment = Alignment.Center
        ) {
            // Pin shadow
            Box(
                modifier = Modifier
                    .offset(y = 24.dp)
                    .size(12.dp, 6.dp)
                    .background(
                        color = Color.Black.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(50)
                    )
            )

            // Pin icon
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = "Location pin",
                tint = AppColors.Primary,
                modifier = Modifier.size(48.dp)
            )
        }

        // ============ TOP BAR ============
        TopAppBar(
            title = { Text("Select Location") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.White.copy(alpha = 0.95f)
            ),
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // ============ MY LOCATION BUTTON ============
        FloatingActionButton(
            onClick = {
                // Move camera to initial location
                scope.launch {
                    cameraPositionState.animate(
                        CameraUpdateFactory.newLatLngZoom(initialLocation, 17f)
                    )
                }
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 80.dp, end = 16.dp)
                .size(48.dp),
            containerColor = Color.White,
            contentColor = AppColors.Primary,
            shape = CircleShape
        ) {
            Icon(
                imageVector = Icons.Default.MyLocation,
                contentDescription = "My Location",
                modifier = Modifier.size(24.dp)
            )
        }

        // ============ BOTTOM CARD ============
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Address Info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    // Location Icon
                    Surface(
                        modifier = Modifier.size(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = AppColors.Primary.copy(alpha = 0.1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = null,
                            tint = AppColors.Primary,
                            modifier = Modifier
                                .padding(10.dp)
                                .size(24.dp)
                        )
                    }

                    // Address Text
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentLocationName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        if (isLoadingAddress) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    strokeWidth = 2.dp,
                                    color = AppColors.Primary
                                )
                                Text(
                                    text = "Getting address...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AppColors.TextHint
                                )
                            }
                        } else {
                            Text(
                                text = currentAddress,
                                style = MaterialTheme.typography.bodySmall,
                                color = AppColors.TextSecondary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Confirm Button
                PrimaryButton(
                    text = "Confirm Location",
                    onClick = {
                        val savedAddress = SavedAddress(
                            addressId = "map_${System.currentTimeMillis()}",
                            addressType = "other",
                            label = currentLocationName,
                            address = currentAddress,
                            landmark = null,
                            latitude = selectedLatLng.latitude,
                            longitude = selectedLatLng.longitude,
                            contactName = null,
                            contactPhone = null,
                            isDefault = false
                        )
                        onLocationSelected(savedAddress)
                    },
                    enabled = !isLoadingAddress && currentAddress != "Move map to select location",
                    modifier = Modifier.fillMaxWidth()
                )

                // Bottom safe area padding
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // ============ ZOOM HINT (optional - shows on first load) ============
        // You can add a hint to show users they can pinch to zoom
    }
}