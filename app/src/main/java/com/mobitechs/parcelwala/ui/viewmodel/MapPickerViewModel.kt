package com.mobitechs.parcelwala.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.mobitechs.parcelwala.data.service.LocationService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Map Picker Screen
 * Handles reverse geocoding and location updates
 */
@HiltViewModel
class MapPickerViewModel @Inject constructor(
    private val locationService: LocationService
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapPickerUiState())
    val uiState: StateFlow<MapPickerUiState> = _uiState.asStateFlow()

    /**
     * âœ… Update location and perform reverse geocoding
     */
    fun updateLocation(latLng: LatLng) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, selectedLocation = latLng) }

                // Reverse geocode to get address
                val address = locationService.getAddressFromLocation(
                    latitude = latLng.latitude,
                    longitude = latLng.longitude
                )

                _uiState.update {
                    it.copy(
                        address = address ?: "Unknown location",
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to get address: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * âœ… Get current location
     */
    fun getCurrentLocation(onSuccess: (LatLng) -> Unit) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }

                val location = locationService.getCurrentLocation()
                if (location != null) {
                    val latLng = LatLng(location.latitude, location.longitude)

                    _uiState.update {
                        it.copy(
                            selectedLocation = latLng,
                            isLoading = false,
                            error = null
                        )
                    }

                    onSuccess(latLng)
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Unable to get current location"
                        )
                    }
                }
            } catch (e: SecurityException) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Location permission not granted"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to get location: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

/**
 * UI State for Map Picker Screen
 */
data class MapPickerUiState(
    val isLoading: Boolean = false,
    val selectedLocation: LatLng? = null,
    val address: String = "",
    val error: String? = null
)