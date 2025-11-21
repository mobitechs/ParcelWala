package com.mobitechs.parcelwala.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobitechs.parcelwala.data.model.request.SavedAddress
import com.mobitechs.parcelwala.data.model.response.PlaceAutocomplete
import com.mobitechs.parcelwala.data.repository.BookingRepository
import com.mobitechs.parcelwala.data.service.LocationService
import com.mobitechs.parcelwala.utils.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Location Search Screen
 * Manages saved addresses, recent pickups, and Google Places autocomplete
 */
@OptIn(FlowPreview::class)
@HiltViewModel
class LocationSearchViewModel @Inject constructor(
    private val bookingRepository: BookingRepository,
    private val locationService: LocationService
) : ViewModel() {

    private val _uiState = MutableStateFlow(LocationSearchUiState())
    val uiState: StateFlow<LocationSearchUiState> = _uiState.asStateFlow()

    // âœ… Debounced search query
    private val searchQueryFlow = MutableStateFlow("")

    init {
        loadSavedAddresses()
        setupAutocomplete()
    }

    /**
     * âœ… Setup autocomplete with debounce
     */
    private fun setupAutocomplete() {
        viewModelScope.launch {
            searchQueryFlow
                .debounce(300) // Wait 300ms after user stops typing
                .filter { it.length >= 3 } // Only search if query is 3+ characters
                .distinctUntilChanged()
                .collect { query ->
                    searchPlaces(query)
                }
        }
    }

    /**
     * Load saved addresses
     */
    private fun loadSavedAddresses() {
        viewModelScope.launch {
            bookingRepository.getSavedAddresses().collect { result ->
                when (result) {
                    is NetworkResult.Loading -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                    is NetworkResult.Success -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                savedAddresses = result.data ?: emptyList(),
                                recentPickups = result.data ?: emptyList(),
                                error = null
                            )
                        }
                    }
                    is NetworkResult.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = result.message
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * âœ… Update search query and trigger autocomplete
     */
    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchQueryFlow.value = query

        // Clear predictions if query is empty
        if (query.isEmpty()) {
            _uiState.update { it.copy(predictions = emptyList()) }
        }
    }

    /**
     * âœ… Search places with Google Places API
     */
    private fun searchPlaces(query: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoadingPredictions = true) }

                // Get current location for bias (optional)
                val currentLocation = try {
                    locationService.getCurrentLocation()?.let { location ->
                        com.google.android.gms.maps.model.LatLng(
                            location.latitude,
                            location.longitude
                        )
                    }
                } catch (e: Exception) {
                    null
                }

                // Search with Places API
                val predictions = locationService.searchPlaces(
                    query = query,
                    biasLocation = currentLocation
                )

                _uiState.update {
                    it.copy(
                        predictions = predictions,
                        isLoadingPredictions = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingPredictions = false,
                        error = "Failed to search places: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * âœ… Get current location
     */
    fun getCurrentLocation() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }

                val location = locationService.getCurrentLocation()
                if (location != null) {
                    // Get address from coordinates
                    val address = locationService.getAddressFromLocation(
                        latitude = location.latitude,
                        longitude = location.longitude
                    )

                    // Create SavedAddress from current location
                    val currentLocationAddress = SavedAddress(
                        addressId = "current",
                        addressType="",
                        landmark="",
                        label = "Current Location",
                        address = address ?: "Unknown location",
                        latitude = location.latitude,
                        longitude = location.longitude,
                        contactName = null,
                        contactPhone = null,
                        isDefault = false
                    )

                    _uiState.update {
                        it.copy(
                            selectedAddress = currentLocationAddress,
                            isLoading = false,
                            error = null
                        )
                    }
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
     * âœ… Select place from autocomplete
     */
    fun selectPlace(placeId: String, onSuccess: (SavedAddress) -> Unit) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }

                val placeDetails = locationService.getPlaceDetails(placeId)
                if (placeDetails != null) {
                    val address = SavedAddress(
                        addressId = placeDetails.placeId,
                        addressType="",
                        landmark="",
                        label = placeDetails.name,
                        address = placeDetails.address,
                        latitude = placeDetails.latitude,
                        longitude = placeDetails.longitude,
                        contactName = null,
                        contactPhone = null,
                        isDefault = false
                    )

                    _uiState.update {
                        it.copy(
                            selectedAddress = address,
                            isLoading = false,
                            error = null
                        )
                    }

                    onSuccess(address)
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Unable to get place details"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to select place: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Select address from saved or recent
     */
    fun selectAddress(address: SavedAddress) {
        _uiState.update { it.copy(selectedAddress = address) }
    }

    /**
     * Clear error
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

/**
 * UI State for Location Search Screen
 */
data class LocationSearchUiState(
    val isLoading: Boolean = false,
    val isLoadingPredictions: Boolean = false,
    val searchQuery: String = "",
    val predictions: List<PlaceAutocomplete> = emptyList(), // âœ… Autocomplete results
    val savedAddresses: List<SavedAddress> = emptyList(),
    val recentPickups: List<SavedAddress> = emptyList(),
    val selectedAddress: SavedAddress? = null,
    val error: String? = null
)