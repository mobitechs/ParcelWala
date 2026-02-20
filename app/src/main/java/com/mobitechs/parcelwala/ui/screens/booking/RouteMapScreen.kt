package com.mobitechs.parcelwala.ui.screens.booking

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.*
import com.google.maps.android.compose.*
import com.mobitechs.parcelwala.R
import com.mobitechs.parcelwala.data.repository.RouteInfo
import com.mobitechs.parcelwala.ui.theme.AppColors
import com.mobitechs.parcelwala.ui.viewmodel.RouteUiState
import com.mobitechs.parcelwala.ui.viewmodel.RouteViewModel

@Composable
fun RouteMapScreen(
    pickupLatLng: LatLng,
    dropLatLng: LatLng,
    pickupAddress: String,
    dropAddress: String,
    viewModel: RouteViewModel = hiltViewModel()
) {
    val routeState by viewModel.routeState.collectAsState()

    LaunchedEffect(pickupLatLng, dropLatLng) {
        viewModel.calculateRoute(pickupLatLng, dropLatLng)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        RouteMap(
            pickupLatLng = pickupLatLng,
            dropLatLng = dropLatLng,
            pickupAddress = pickupAddress,
            dropAddress = dropAddress,
            routeState = routeState
        )

        when (val state = routeState) {
            is RouteUiState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            is RouteUiState.Success -> {
                RouteInfoCard(
                    routeInfo = state.routeInfo,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                )
            }
            is RouteUiState.Error -> {
                ErrorCard(
                    message = state.message,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                )
            }
            else -> {}
        }
    }
}

@Composable
fun RouteMap(
    pickupLatLng: LatLng,
    dropLatLng: LatLng,
    pickupAddress: String,
    dropAddress: String,
    routeState: RouteUiState
) {
    val pickupLabel = stringResource(R.string.label_pickup)
    val dropLabel = stringResource(R.string.label_drop)

    val boundsBuilder = LatLngBounds.builder()
        .include(pickupLatLng)
        .include(dropLatLng)

    if (routeState is RouteUiState.Success) {
        routeState.routeInfo.polylinePoints.forEach { point ->
            boundsBuilder.include(point)
        }
    }

    val bounds = boundsBuilder.build()
    val cameraPositionState = rememberCameraPositionState()

    LaunchedEffect(routeState) {
        if (routeState is RouteUiState.Success || routeState is RouteUiState.Idle) {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngBounds(bounds, 100)
            )
        }
    }

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        properties = MapProperties(
            mapType = MapType.NORMAL,
            isMyLocationEnabled = false
        ),
        uiSettings = MapUiSettings(
            zoomControlsEnabled = false,
            myLocationButtonEnabled = false
        )
    ) {
        Marker(
            state = MarkerState(position = pickupLatLng),
            title = pickupLabel,
            snippet = pickupAddress,
            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
        )

        Marker(
            state = MarkerState(position = dropLatLng),
            title = dropLabel,
            snippet = dropAddress,
            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
        )

        if (routeState is RouteUiState.Success) {
            Polyline(
                points = routeState.routeInfo.polylinePoints,
                color = AppColors.Blue,
                width = 12f,
                jointType = JointType.ROUND,
                startCap = RoundCap(),
                endCap = RoundCap()
            )

            Polyline(
                points = routeState.routeInfo.polylinePoints,
                color = AppColors.RouteShadow,
                width = 16f,
                jointType = JointType.ROUND,
                startCap = RoundCap(),
                endCap = RoundCap(),
                zIndex = -1f
            )
        }
    }
}

@Composable
fun RouteInfoCard(
    routeInfo: RouteInfo,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = routeInfo.distanceText,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.label_distance_label),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(40.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = routeInfo.durationText,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.label_duration_label),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ErrorCard(
    message: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.onErrorContainer
        )
    }
}