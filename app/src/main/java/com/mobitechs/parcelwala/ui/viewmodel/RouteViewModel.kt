package com.mobitechs.parcelwala.ui.viewmodel



import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.mobitechs.parcelwala.data.repository.DirectionsRepository
import com.mobitechs.parcelwala.data.repository.RouteInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RouteViewModel @Inject constructor(
    private val directionsRepository: DirectionsRepository
) : ViewModel() {

    private val _routeState = MutableStateFlow<RouteUiState>(RouteUiState.Idle)
    val routeState: StateFlow<RouteUiState> = _routeState.asStateFlow()

    /**
     * Calculate route between pickup and drop locations
     */
    fun calculateRoute(
        pickupLatLng: LatLng,
        dropLatLng: LatLng
    ) {
        viewModelScope.launch {
            _routeState.value = RouteUiState.Loading

            directionsRepository.getRouteInfo(
                pickupLat = pickupLatLng.latitude,
                pickupLng = pickupLatLng.longitude,
                dropLat = dropLatLng.latitude,
                dropLng = dropLatLng.longitude
            ).onSuccess { routeInfo ->
                _routeState.value = RouteUiState.Success(routeInfo)
            }.onFailure { error ->
                _routeState.value = RouteUiState.Error(
                    error.message ?: "Failed to calculate route"
                )
            }
        }
    }

    fun clearRoute() {
        _routeState.value = RouteUiState.Idle
    }
}

sealed class RouteUiState {
    data object Idle : RouteUiState()
    data object Loading : RouteUiState()
    data class Success(val routeInfo: RouteInfo) : RouteUiState()
    data class Error(val message: String) : RouteUiState()
}