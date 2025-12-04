// ui/viewmodel/HomeViewModel.kt
package com.mobitechs.parcelwala.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobitechs.parcelwala.data.manager.ActiveBooking
import com.mobitechs.parcelwala.data.manager.ActiveBookingManager
import com.mobitechs.parcelwala.data.model.response.VehicleTypeResponse
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
 * ViewModel for Home Screen
 * Manages vehicle types, pickup location, and active booking state
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val bookingRepository: BookingRepository,
    private val activeBookingManager: ActiveBookingManager
) : ViewModel() {

    // UI State
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // Active booking state - exposed from ActiveBookingManager
    val activeBooking: StateFlow<ActiveBooking?> = activeBookingManager.activeBooking

    init {
        loadVehicleTypes()
    }

    /**
     * Load vehicle types from repository
     */
    private fun loadVehicleTypes() {
        viewModelScope.launch {
            bookingRepository.getVehicleTypes().collect { result ->
                when (result) {
                    is NetworkResult.Loading -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                    is NetworkResult.Success -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                vehicleTypes = result.data ?: emptyList(),
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
     * Set pickup location
     */
    fun setPickupLocation(address: String) {
        _uiState.update { it.copy(pickupLocation = address) }
    }

    /**
     * Check if there's an active booking
     */
    fun hasActiveBooking(): Boolean = activeBookingManager.hasActiveBooking()

    /**
     * Retry search for rider
     * Resets the search timer and increments attempt count
     */
    fun retrySearch() {
        activeBookingManager.retrySearch()
    }

    /**
     * Refresh data
     */
    fun refresh() {
        loadVehicleTypes()
    }

    /**
     * Clear error
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

/**
 * UI State for Home Screen
 */
data class HomeUiState(
    val isLoading: Boolean = false,
    val vehicleTypes: List<VehicleTypeResponse> = emptyList(),
    val pickupLocation: String = "Home - Narayan Smruti, Gandhi Nagar Internal...",
    val error: String? = null
)