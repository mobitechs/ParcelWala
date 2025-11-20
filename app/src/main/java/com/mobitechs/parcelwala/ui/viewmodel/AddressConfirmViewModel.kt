package com.mobitechs.parcelwala.ui.viewmodel

// ui/viewmodel/AddressConfirmViewModel.kt

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
 * ViewModel for Address Confirmation Screen
 * Manages address details and validation
 */
@HiltViewModel
class AddressConfirmViewModel @Inject constructor(
    private val bookingRepository: BookingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddressConfirmUiState())
    val uiState: StateFlow<AddressConfirmUiState> = _uiState.asStateFlow()

    /**
     * Initialize with address
     */
    fun initializeWithAddress(address: SavedAddress) {
        _uiState.update {
            it.copy(
                address = address.address,
                latitude = address.latitude,
                longitude = address.longitude,
                flatBuilding = "",
                senderName = address.contactName ?: "",
                senderMobile = address.contactPhone ?: "",
                saveAs = address.addressType
            )
        }
    }

    /**
     * Update address
     */
    fun updateAddress(address: String) {
        _uiState.update { it.copy(address = address) }
    }

    /**
     * Update flat/building
     */
    fun updateFlatBuilding(value: String) {
        _uiState.update { it.copy(flatBuilding = value) }
    }

    /**
     * Update sender name
     */
    fun updateSenderName(name: String) {
        _uiState.update { it.copy(senderName = name) }
    }

    /**
     * Update sender mobile
     */
    fun updateSenderMobile(mobile: String) {
        _uiState.update { it.copy(senderMobile = mobile) }
    }

    /**
     * Use user's mobile number
     */
    fun useMyMobile(userMobile: String) {
        _uiState.update { it.copy(senderMobile = userMobile) }
    }

    /**
     * Set save as type
     */
    fun setSaveAs(type: String) {
        _uiState.update { it.copy(saveAs = type) }
    }

    /**
     * Set save label
     */
    fun setSaveLabel(label: String) {
        _uiState.update { it.copy(saveLabel = label) }
    }

    /**
     * Validate and confirm
     */
    fun confirmAddress(onSuccess: (SavedAddress) -> Unit) {
        val state = _uiState.value

        // Validation
        if (state.senderName.isBlank()) {
            _uiState.update { it.copy(error = "Please enter sender name") }
            return
        }

        if (state.senderMobile.isBlank()) {
            _uiState.update { it.copy(error = "Please enter sender mobile number") }
            return
        }

        if (state.senderMobile.length != 10) {
            _uiState.update { it.copy(error = "Please enter a valid 10-digit mobile number") }
            return
        }

        // Create SavedAddress object
        val savedAddress = SavedAddress(
            addressId = 0,
            addressType = state.saveAs,
            label = state.saveLabel.ifEmpty { state.saveAs.capitalize() },
            address = state.address,
            landmark = null,
            latitude = state.latitude,
            longitude = state.longitude,
            contactName = state.senderName,
            contactPhone = state.senderMobile,
            isDefault = false
        )

        onSuccess(savedAddress)
    }

    /**
     * Clear error
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

/**
 * UI State for Address Confirmation Screen
 */
data class AddressConfirmUiState(
    val address: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val flatBuilding: String = "",
    val senderName: String = "",
    val senderMobile: String = "",
    val saveAs: String = "home", // home, shop, other
    val saveLabel: String = "",
    val error: String? = null
)