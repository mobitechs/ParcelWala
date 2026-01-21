// ui/viewmodel/LocationSearchViewModel.kt - Updated
package com.mobitechs.parcelwala.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobitechs.parcelwala.data.local.PreferencesManager
import com.mobitechs.parcelwala.data.model.request.SavedAddress
import com.mobitechs.parcelwala.data.model.request.SearchHistory
import com.mobitechs.parcelwala.data.model.response.PlaceAutocomplete
import com.mobitechs.parcelwala.data.repository.BookingRepository
import com.mobitechs.parcelwala.data.service.LocationService
import com.mobitechs.parcelwala.utils.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class LocationSearchViewModel @Inject constructor(
    private val bookingRepository: BookingRepository,
    private val locationService: LocationService,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(LocationSearchUiState())
    val uiState: StateFlow<LocationSearchUiState> = _uiState.asStateFlow()

    private val searchQueryFlow = MutableStateFlow("")

    init {
        loadSavedAddresses()
        loadSearchHistory()
        setupAutocomplete()
    }

    private fun setupAutocomplete() {
        viewModelScope.launch {
            searchQueryFlow
                .debounce(300)
                .filter { it.length >= 3 }
                .distinctUntilChanged()
                .collect { query ->
                    searchPlaces(query)
                }
        }
    }

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
     * ✅ Load search history from last 3 days
     */
    private fun loadSearchHistory() {
        viewModelScope.launch {
            val history = preferencesManager.getSearchHistory()
            _uiState.update { it.copy(searchHistory = history) }
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchQueryFlow.value = query

        if (query.isEmpty()) {
            _uiState.update { it.copy(predictions = emptyList()) }
        }
    }

    private fun searchPlaces(query: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoadingPredictions = true) }

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

    fun getCurrentLocation() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }

                val location = locationService.getCurrentLocation()
                if (location != null) {
                    val address = locationService.getAddressFromLocation(
                        latitude = location.latitude,
                        longitude = location.longitude
                    )

                    val currentLocationAddress = SavedAddress(
                        addressId = "current",
                        addressType = "",
                        landmark = "",
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

    fun selectPlace(placeId: String, onSuccess: (SavedAddress) -> Unit) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }

                val placeDetails = locationService.getPlaceDetails(placeId)
                if (placeDetails != null) {
                    val address = SavedAddress(
                        addressId = placeDetails.placeId,
                        addressType = "",
                        landmark = "",
                        label = placeDetails.name,
                        address = placeDetails.address,
                        latitude = placeDetails.latitude,
                        longitude = placeDetails.longitude,
                        contactName = null,
                        contactPhone = null,
                        isDefault = false
                    )

                    // ✅ Save to search history
                    saveToSearchHistory(address)

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

    fun selectAddress(address: SavedAddress) {
        // ✅ Save to search history when selecting from saved/recent addresses
        saveToSearchHistory(address)
        _uiState.update { it.copy(selectedAddress = address) }
    }

    /**
     * ✅ Save address to search history
     */
    private fun saveToSearchHistory(address: SavedAddress) {
        val searchHistory = SearchHistory(
            address = address.address,
            latitude = address.latitude ?: 0.0,
            longitude = address.longitude ?: 0.0,
            timestamp = System.currentTimeMillis(),
            label = address.label
        )
        preferencesManager.saveSearchHistory(searchHistory)

        // Reload history
        loadSearchHistory()
    }

    /**
     * ✅ Select from search history
     */
    fun selectFromHistory(history: SearchHistory, onSuccess: (SavedAddress) -> Unit) {
        val address = SavedAddress(
            addressId = "history_${history.timestamp}",
            addressType = "",
            landmark = "",
            label = history.label.ifEmpty { "Recent Location" },
            address = history.address,
            latitude = history.latitude,
            longitude = history.longitude,
            contactName = null,
            contactPhone = null,
            isDefault = false
        )

        selectAddress(address)
        onSuccess(address)
    }

    /**
     * ✅ Clear search history
     */
    fun clearSearchHistory() {
        preferencesManager.clearSearchHistory()
        _uiState.update { it.copy(searchHistory = emptyList()) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

data class LocationSearchUiState(
    val isLoading: Boolean = false,
    val isLoadingPredictions: Boolean = false,
    val searchQuery: String = "",
    val predictions: List<PlaceAutocomplete> = emptyList(),
    val savedAddresses: List<SavedAddress> = emptyList(),
    val recentPickups: List<SavedAddress> = emptyList(),
    val searchHistory: List<SearchHistory> = emptyList(), // ✅ Added
    val selectedAddress: SavedAddress? = null,
    val error: String? = null
)