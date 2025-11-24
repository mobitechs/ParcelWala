// ui/viewmodel/AccountViewModel.kt
package com.mobitechs.parcelwala.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Account ViewModel
 */
@HiltViewModel
class AccountViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(AccountUiState())
    val uiState: StateFlow<AccountUiState> = _uiState.asStateFlow()

    init {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            // Mock user data - Replace with actual API call
            _uiState.update {
                it.copy(
                    userName = "Pratik Sonawane",
                    email = "sonawane.ptk@gmail.com",
                    isEmailVerified = false,
                    phoneNumber = "+91 8655883062",
                    profileImage = null,
                    walletBalance = 0.0,
                    referralCode = "PRATIK2024"
                )
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            // Clear user session
            // Navigate to login
        }
    }
}

data class AccountUiState(
    val isLoading: Boolean = false,
    val userName: String? = null,
    val email: String? = null,
    val isEmailVerified: Boolean = false,
    val phoneNumber: String? = null,
    val profileImage: String? = null,
    val walletBalance: Double = 0.0,
    val referralCode: String? = null,
    val error: String? = null
)