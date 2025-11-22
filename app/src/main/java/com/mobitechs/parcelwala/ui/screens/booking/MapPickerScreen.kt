package com.mobitechs.parcelwala.ui.screens.booking

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.mobitechs.parcelwala.data.model.request.SavedAddress
import com.mobitechs.parcelwala.ui.components.*
import com.mobitechs.parcelwala.ui.theme.AppColors
import com.mobitechs.parcelwala.ui.viewmodel.MapPickerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapPickerScreen(
    initialLocation: LatLng,
    onLocationSelected: (SavedAddress) -> Unit,
    onBack: () -> Unit,
    viewModel: MapPickerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialLocation, 15f)
    }

    LaunchedEffect(cameraPositionState.isMoving) {
        if (!cameraPositionState.isMoving) {
            val position = cameraPositionState.position.target
            viewModel.updateLocation(position)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.updateLocation(initialLocation)
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
        },
        containerColor = AppColors.Background
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
                properties = MapProperties(isMyLocationEnabled = false),
                uiSettings = MapUiSettings(
                    myLocationButtonEnabled = false,
                    zoomControlsEnabled = false,
                    compassEnabled = true,
                    mapToolbarEnabled = false
                )
            )

            // Center Pin
            Icon(
                imageVector = Icons.Default.Place,
                contentDescription = "Pin",
                modifier = Modifier
                    .size(48.dp)
                    .align(Alignment.Center)
                    .offset(y = (-24).dp),
                tint = AppColors.Drop
            )

            // My Location Button
            IconButtonWithBackground(
                icon = Icons.Default.MyLocation,
                contentDescription = "My location",
                onClick = {
                    viewModel.getCurrentLocation { location ->
                        cameraPositionState.position = CameraPosition.fromLatLngZoom(location, 15f)
                    }
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
                size = 48.dp
            )

            // Address Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(
                        start = 16.dp,
                        end = 16.dp,
                        bottom = 88.dp
                    ),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Selected Location",
                        style = MaterialTheme.typography.labelMedium,
                        color = AppColors.TextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    if (uiState.isLoading) {
                        LoadingIndicator(message = "Getting address...")
                    } else {
                        Text(
                            text = uiState.address.ifEmpty { "Unknown location" },
                            style = MaterialTheme.typography.bodyLarge,
                            color = AppColors.TextPrimary
                        )
                    }

                    Text(
                        text = "Lat: ${"%.6f".format(cameraPositionState.position.target.latitude)}, " +
                                "Lng: ${"%.6f".format(cameraPositionState.position.target.longitude)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextHint,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // Confirm Button
            PrimaryButton(
                text = "Confirm Location",
                onClick = {
                    val location = cameraPositionState.position.target
                    val address = SavedAddress(
                        addressId = "map_${System.currentTimeMillis()}",
                        label = "Selected Location",
                        addressType = "",
                        landmark = "",
                        address = uiState.address,
                        latitude = location.latitude,
                        longitude = location.longitude,
                        contactName = null,
                        contactPhone = null,
                        isDefault = false
                    )
                    onLocationSelected(address)
                },
                icon = Icons.Default.Check,
                enabled = uiState.address.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            )

            // Error
            uiState.error?.let { error ->
                ErrorMessageCard(
                    message = error,
                    onRetry = { viewModel.clearError() },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(16.dp)
                )
            }
        }
    }
}