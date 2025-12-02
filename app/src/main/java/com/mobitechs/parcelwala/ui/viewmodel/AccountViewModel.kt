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

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val bookingRepository: BookingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountUiState())
    val uiState: StateFlow<AccountUiState> = _uiState.asStateFlow()

    private val _savedAddresses = MutableStateFlow<List<SavedAddress>>(emptyList())
    val savedAddresses: StateFlow<List<SavedAddress>> = _savedAddresses.asStateFlow()

    init {
        loadUserProfile()
        loadSavedAddresses()
    }

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

    fun updateProfile(firstName: String, lastName: String, email: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
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

    fun clearProfileUpdateSuccess() {
        _uiState.update { it.copy(profileUpdateSuccess = false) }
    }

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
                        _uiState.update {
                            it.copy(
                                isSavingAddress = false,
                                addressSaveSuccess = true,
                                error = null
                            )
                        }
                        // ✅ FIX 3: Reload addresses after save
                        loadSavedAddresses()
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
            bookingRepository.updateAddress(address).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> {
                        _uiState.update { it.copy(isSavingAddress = true) }
                    }
                    is NetworkResult.Success -> {
                        _uiState.update {
                            it.copy(
                                isSavingAddress = false,
                                addressSaveSuccess = true,
                                error = null
                            )
                        }
                        // ✅ FIX 3: Reload addresses after update
                        loadSavedAddresses()
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
            bookingRepository.deleteAddress(addressId).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> {
                        _uiState.update { it.copy(isDeletingAddress = true) }
                    }
                    is NetworkResult.Success -> {
                        // Remove from local list immediately
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

    fun clearAddressSaveSuccess() {
        _uiState.update { it.copy(addressSaveSuccess = false) }
    }

    fun clearAddressDeleteSuccess() {
        _uiState.update { it.copy(addressDeleteSuccess = false) }
    }

    fun saveGSTIN(gstin: String, companyName: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSavingGST = true) }

            try {
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

    fun clearGSTSaveSuccess() {
        _uiState.update { it.copy(gstSaveSuccess = false) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun logout(onLogoutComplete: () -> Unit) {
        viewModelScope.launch {
            preferencesManager.clearAll()
            onLogoutComplete()
        }
    }
}

data class AccountUiState(
    val isLoading: Boolean = false,
    val isLoadingAddresses: Boolean = false,
    val isSavingAddress: Boolean = false,
    val isDeletingAddress: Boolean = false,
    val isSavingGST: Boolean = false,
    val user: User? = null,
    val userName: String? = null,
    val email: String? = null,
    val isEmailVerified: Boolean = false,
    val phoneNumber: String? = null,
    val profileImage: String? = null,
    val walletBalance: Double = 0.0,
    val referralCode: String? = null,
    val gstin: String? = null,
    val companyName: String? = null,
    val profileUpdateSuccess: Boolean = false,
    val addressSaveSuccess: Boolean = false,
    val addressDeleteSuccess: Boolean = false,
    val gstSaveSuccess: Boolean = false,
    val error: String? = null
)