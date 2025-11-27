// ui/viewmodel/AccountViewModel.kt
package com.mobitechs.parcelwala.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobitechs.parcelwala.data.local.PreferencesManager
import com.mobitechs.parcelwala.data.model.request.SavedAddress
import com.mobitechs.parcelwala.data.model.response.User
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
 * Account ViewModel
 * Manages user profile, saved addresses, and GST details
 *
 * Features:
 * - User profile management (view/edit)
 * - Saved addresses CRUD operations
 * - GST details management
 * - Logout functionality
 */
@HiltViewModel
class AccountViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val bookingRepository: BookingRepository
) : ViewModel() {

    // ============ UI STATE ============
    private val _uiState = MutableStateFlow(AccountUiState())
    val uiState: StateFlow<AccountUiState> = _uiState.asStateFlow()

    // ============ SAVED ADDRESSES STATE ============
    private val _savedAddresses = MutableStateFlow<List<SavedAddress>>(emptyList())
    val savedAddresses: StateFlow<List<SavedAddress>> = _savedAddresses.asStateFlow()

    init {
        loadUserProfile()
        loadSavedAddresses()
    }

    // ============ USER PROFILE OPERATIONS ============

    /**
     * Load user profile from preferences
     */
    private fun loadUserProfile() {
        viewModelScope.launch {
            val user = preferencesManager.getUser()
            _uiState.update {
                it.copy(
                    user = user,
                    userName = user?.fullName,
                    email = user?.email,
                    isEmailVerified = user?.email != null,
                    phoneNumber = user?.phoneNumber,
                    profileImage = user?.profileImage,
                    walletBalance = user?.walletBalance ?: 0.0,
                    referralCode = user?.referralCode
                )
            }
        }
    }

    /**
     * Update user profile
     */
    fun updateProfile(
        firstName: String,
        lastName: String,
        email: String
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                // In real implementation, call API to update profile
                // For now, update local state
                val fullName = "$firstName $lastName".trim()
                val currentUser = _uiState.value.user

                if (currentUser != null) {
                    val updatedUser = currentUser.copy(
                        fullName = fullName,
                        email = email.ifBlank { null }
                    )
                    preferencesManager.saveUser(updatedUser)

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            user = updatedUser,
                            userName = fullName,
                            email = email.ifBlank { null },
                            profileUpdateSuccess = true,
                            error = null
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to update profile: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Clear profile update success flag
     */
    fun clearProfileUpdateSuccess() {
        _uiState.update { it.copy(profileUpdateSuccess = false) }
    }

    // ============ SAVED ADDRESSES OPERATIONS ============

    /**
     * Load saved addresses from repository
     */
    fun loadSavedAddresses() {
        viewModelScope.launch {
            bookingRepository.getSavedAddresses().collect { result ->
                when (result) {
                    is NetworkResult.Loading -> {
                        _uiState.update { it.copy(isLoadingAddresses = true) }
                    }
                    is NetworkResult.Success -> {
                        _savedAddresses.value = result.data ?: emptyList()
                        _uiState.update {
                            it.copy(
                                isLoadingAddresses = false,
                                error = null
                            )
                        }
                    }
                    is NetworkResult.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoadingAddresses = false,
                                error = result.message
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Save new address
     */
    fun saveAddress(address: SavedAddress) {
        viewModelScope.launch {
            bookingRepository.saveAddress(address).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> {
                        _uiState.update { it.copy(isSavingAddress = true) }
                    }
                    is NetworkResult.Success -> {
                        // Add to local list
                        result.data?.let { savedAddress ->
                            _savedAddresses.update { currentList ->
                                currentList + savedAddress
                            }
                        }
                        _uiState.update {
                            it.copy(
                                isSavingAddress = false,
                                addressSaveSuccess = true,
                                error = null
                            )
                        }
                    }
                    is NetworkResult.Error -> {
                        _uiState.update {
                            it.copy(
                                isSavingAddress = false,
                                error = result.message
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Update existing address
     */
    fun updateAddress(address: SavedAddress) {
        viewModelScope.launch {
            bookingRepository.saveAddress(address).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> {
                        _uiState.update { it.copy(isSavingAddress = true) }
                    }
                    is NetworkResult.Success -> {
                        // Update in local list
                        _savedAddresses.update { currentList ->
                            currentList.map {
                                if (it.addressId == address.addressId) address else it
                            }
                        }
                        _uiState.update {
                            it.copy(
                                isSavingAddress = false,
                                addressSaveSuccess = true,
                                error = null
                            )
                        }
                    }
                    is NetworkResult.Error -> {
                        _uiState.update {
                            it.copy(
                                isSavingAddress = false,
                                error = result.message
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Delete saved address
     */
    fun deleteAddress(addressId: String) {
        viewModelScope.launch {
            val addressIdInt = addressId.toIntOrNull() ?: return@launch

            bookingRepository.deleteAddress(addressIdInt).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> {
                        _uiState.update { it.copy(isDeletingAddress = true) }
                    }
                    is NetworkResult.Success -> {
                        // Remove from local list
                        _savedAddresses.update { currentList ->
                            currentList.filter { it.addressId != addressId }
                        }
                        _uiState.update {
                            it.copy(
                                isDeletingAddress = false,
                                addressDeleteSuccess = true,
                                error = null
                            )
                        }
                    }
                    is NetworkResult.Error -> {
                        _uiState.update {
                            it.copy(
                                isDeletingAddress = false,
                                error = result.message
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Clear address save success flag
     */
    fun clearAddressSaveSuccess() {
        _uiState.update { it.copy(addressSaveSuccess = false) }
    }

    /**
     * Clear address delete success flag
     */
    fun clearAddressDeleteSuccess() {
        _uiState.update { it.copy(addressDeleteSuccess = false) }
    }

    // ============ GST OPERATIONS ============

    /**
     * Save GST details
     */
    fun saveGSTIN(gstin: String, companyName: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSavingGST = true) }

            try {
                // In real implementation, call API to save GSTIN
                // For now, update local state
                _uiState.update {
                    it.copy(
                        isSavingGST = false,
                        gstin = gstin,
                        companyName = companyName,
                        gstSaveSuccess = true,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSavingGST = false,
                        error = "Failed to save GST: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Clear GST save success flag
     */
    fun clearGSTSaveSuccess() {
        _uiState.update { it.copy(gstSaveSuccess = false) }
    }

    // ============ COMMON OPERATIONS ============

    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Logout user
     */
    fun logout(onLogoutComplete: () -> Unit) {
        viewModelScope.launch {
            preferencesManager.clearAll()
            onLogoutComplete()
        }
    }
}

/**
 * UI State for Account Screen
 */
data class AccountUiState(
    // Loading states
    val isLoading: Boolean = false,
    val isLoadingAddresses: Boolean = false,
    val isSavingAddress: Boolean = false,
    val isDeletingAddress: Boolean = false,
    val isSavingGST: Boolean = false,

    // User profile
    val user: User? = null,
    val userName: String? = null,
    val email: String? = null,
    val isEmailVerified: Boolean = false,
    val phoneNumber: String? = null,
    val profileImage: String? = null,
    val walletBalance: Double = 0.0,
    val referralCode: String? = null,

    // GST details
    val gstin: String? = null,
    val companyName: String? = null,

    // Success flags
    val profileUpdateSuccess: Boolean = false,
    val addressSaveSuccess: Boolean = false,
    val addressDeleteSuccess: Boolean = false,
    val gstSaveSuccess: Boolean = false,

    // Error
    val error: String? = null
)