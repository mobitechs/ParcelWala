package com.mobitechs.parcelwala.ui.viewmodel

// ui/viewmodel/LocationSearchViewModel.kt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobitechs.parcelwala.data.model.request.SavedAddress
import com.mobitechs.parcelwala.data.repository.BookingRepository
import com.mobitechs.parcelwala.utils.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Location Search Screen
 * Manages saved addresses and recent pickups
 */
@HiltViewModel
class LocationSearchViewModel @Inject constructor(
    private val bookingRepository: BookingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LocationSearchUiState())
    val uiState: StateFlow<LocationSearchUiState> = _uiState.asStateFlow()

    init {
        loadSavedAddresses()
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
     * Update search query
     */
    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        // TODO: Implement search/autocomplete
    }

    /**
     * Select address
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
    val searchQuery: String = "",
    val savedAddresses: List<SavedAddress> = emptyList(),
    val recentPickups: List<SavedAddress> = emptyList(),
    val selectedAddress: SavedAddress? = null,
    val error: String? = null
)