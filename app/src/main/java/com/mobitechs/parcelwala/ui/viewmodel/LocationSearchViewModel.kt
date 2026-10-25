// ui/viewmodel/LocationSearchViewModel.kt - Updated
package com.mobitechs.parcelwala.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.common.api.ApiException
import com.google.android.libraries.places.api.net.PlacesStatusCodes
import com.mobitechs.parcelwala.BuildConfig
import com.mobitechs.parcelwala.data.local.PreferencesManager
import com.mobitechs.parcelwala.data.model.request.SavedAddress
import com.mobitechs.parcelwala.data.model.request.SearchHistory
import com.mobitechs.parcelwala.data.model.response.PlaceAutocomplete
import com.mobitechs.parcelwala.data.repository.BookingRepository
import com.mobitechs.parcelwala.data.service.LocationService
import com.mobitechs.parcelwala.utils.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.update
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
            _uiState.update { it.copy(predictions = emptyList(), searchError = null) }
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
                        error = null,
                        searchError = null
                    )
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _uiState.update {
                    it.copy(
                        isLoadingPredictions = false,
                        predictions = emptyList(),
                        error = "Failed to search places: ${e.message}",
                        searchError = describePlacesFailure(e)
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
                if (e is kotlinx.coroutines.CancellationException) throw e
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to select place: ${e.message}",
                        searchError = describePlacesFailure(e)
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
    val error: String? = null,
    /**
     * Why the last address search or place lookup failed, ready to show.
     *
     * Kept apart from [error], which GPS also writes to: the picker shows this
     * one in place of "No places found", and a GPS hiccup must not appear
     * there as if the search had failed.
     */
    val searchError: String? = null
)

/**
 * Turn a Places failure into one sentence for the screen.
 *
 * Before, every failure — a missing key, a key Google rejects, no network —
 * left the list empty under "No places found. Try a nearby landmark", which
 * reads as "there is no such place" rather than "search is broken". Debug builds
 * append Google's own reason (e.g. "You must enable Billing on the Google Cloud
 * Project…") so a developer sees the real cause on the screen, not only in
 * Logcat.
 */
internal fun describePlacesFailure(e: Throwable): String {
    Log.e("ParcelWalaMaps", "Place search failed", e)
    val reason = e.message.orEmpty()
    val isKeyProblem = (e as? ApiException)?.statusCode == PlacesStatusCodes.REQUEST_DENIED ||
        reason.contains("MAPS_API_KEY") ||
        reason.contains("REQUEST_DENIED", ignoreCase = true) ||
        reason.contains("billing", ignoreCase = true) ||
        reason.contains("API key", ignoreCase = true) ||
        reason.contains("not authorized", ignoreCase = true)
    val friendly = if (isKeyProblem) {
        "Address search isn't available right now. Please try again later."
    } else {
        "Couldn't search addresses. Check your internet connection and try again."
    }
    return if (BuildConfig.DEBUG && reason.isNotBlank()) "$friendly\n\nDebug: $reason" else friendly
}