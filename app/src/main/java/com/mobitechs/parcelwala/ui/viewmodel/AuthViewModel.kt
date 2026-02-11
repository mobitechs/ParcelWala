package com.mobitechs.parcelwala.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobitechs.parcelwala.data.model.response.LoginData
import com.mobitechs.parcelwala.data.model.response.OtpData
import com.mobitechs.parcelwala.data.repository.AuthRepository
import com.mobitechs.parcelwala.utils.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val otpSent: Boolean = false,
    val otpData: OtpData? = null,
    val loginSuccess: Boolean = false,
    val loginData: LoginData? = null,
    val isNewUser: Boolean = false,
    val profileCompleted: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    companion object {
        private const val KEY_PHONE_NUMBER = "phone_number"
    }

    private var currentPhoneNumber: String
        get() = savedStateHandle[KEY_PHONE_NUMBER] ?: ""
        set(value) { savedStateHandle[KEY_PHONE_NUMBER] = value }

    fun sendOtp(phoneNumber: String) {
        if (!isValidPhoneNumber(phoneNumber)) {
            _uiState.update { it.copy(error = "Please enter a valid phone number") }
            return
        }

        currentPhoneNumber = phoneNumber

        viewModelScope.launch {
            authRepository.sendOtp(phoneNumber).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> {
                        _uiState.update { it.copy(isLoading = true, error = null) }
                    }
                    is NetworkResult.Success -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                otpSent = true,
                                otpData = result.data,
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
     * Verify OTP - ✅ FIXED: Changed from 6-digit to 4-digit validation
     */
    fun verifyOtp(otp: String, phoneNumber: String? = null) {
        if (otp.length != 4) {  // ✅ FIXED: Was 6, now 4
            _uiState.update { it.copy(error = "Please enter a valid 4-digit OTP") }  // ✅ FIXED
            return
        }

        val phone = phoneNumber ?: currentPhoneNumber

        if (phone.isBlank()) {
            _uiState.update { it.copy(error = "Phone number not found. Please go back and try again.") }
            return
        }

        viewModelScope.launch {
            authRepository.verifyOtp(phone, otp).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> {
                        _uiState.update { it.copy(isLoading = true, error = null) }
                    }
                    is NetworkResult.Success -> {
                        val isNewUser = result.data?.user?.isNewUser ?: false
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                loginSuccess = true,
                                loginData = result.data,
                                isNewUser = isNewUser,
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

    fun completeProfile(
        fullName: String,
        email: String?,
        referralCode: String?
    ) {
        if (fullName.isBlank()) {
            _uiState.update { it.copy(error = "Please enter your name") }
            return
        }

        if (!email.isNullOrBlank() && !isValidEmail(email)) {
            _uiState.update { it.copy(error = "Please enter a valid email") }
            return
        }

        viewModelScope.launch {
            authRepository.completeProfile(fullName, email, referralCode).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> {
                        _uiState.update { it.copy(isLoading = true, error = null) }
                    }
                    is NetworkResult.Success -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                profileCompleted = true,
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

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun resetOtpState() {
        _uiState.update { it.copy(otpSent = false, otpData = null) }
    }

    fun setPhoneNumber(phoneNumber: String) {
        currentPhoneNumber = phoneNumber
    }

    private fun isValidPhoneNumber(phone: String): Boolean {
        val phoneRegex = "^[6-9]\\d{9}$".toRegex()
        return phone.matches(phoneRegex)
    }

    private fun isValidEmail(email: String): Boolean {
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
        return email.matches(emailRegex)
    }
}