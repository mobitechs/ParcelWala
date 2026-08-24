package com.mobitechs.parcelwala.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobitechs.parcelwala.data.model.response.LoginData
import com.mobitechs.parcelwala.data.model.response.OtpData
import com.mobitechs.parcelwala.data.repository.AuthRepository
import com.mobitechs.parcelwala.utils.NetworkResult
import com.mobitechs.parcelwala.utils.Validators
import com.mobitechs.parcelwala.utils.fieldErrors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    /**
     * Per-field validation errors straight from the server, e.g.
     *   { "full_name": ["Full name is required", "Full name must be at least 3 characters"] }
     * The screen maps these onto the matching inputs instead of showing a popup.
     */
    val fieldErrors: Map<String, List<String>> = emptyMap(),
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
        set(value) {
            savedStateHandle[KEY_PHONE_NUMBER] = value
        }

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
        // Client-side rules mirror the backend's, so an obviously bad value never
        // costs a round trip. The screen already gates its button on the same rules;
        // this is the backstop for programmatic callers.
        Validators.fullName(fullName)?.let { message ->
            _uiState.update { it.copy(error = message, fieldErrors = mapOf("full_name" to listOf(message))) }
            return
        }
        Validators.emailOptional(email)?.let { message ->
            _uiState.update { it.copy(error = message, fieldErrors = mapOf("email" to listOf(message))) }
            return
        }

        viewModelScope.launch {
            authRepository.completeProfile(fullName, email, referralCode).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> {
                        _uiState.update {
                            it.copy(isLoading = true, error = null, fieldErrors = emptyMap())
                        }
                    }

                    is NetworkResult.Success -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                profileCompleted = true,
                                error = null,
                                fieldErrors = emptyMap()
                            )
                        }
                    }

                    is NetworkResult.Error -> {
                        // Carry the server's per-field map through so the screen can
                        // underline the exact input that was rejected.
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = result.message,
                                fieldErrors = result.fieldErrors
                            )
                        }
                    }
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null, fieldErrors = emptyMap()) }
    }

    fun resetOtpState() {
        _uiState.update { it.copy(otpSent = false, otpData = null) }
    }

    fun setPhoneNumber(phoneNumber: String) {
        currentPhoneNumber = phoneNumber
    }

    // Phone and email rules now live in Validators so the screens, the ViewModel
    // and the backend all agree on what "valid" means.
    private fun isValidPhoneNumber(phone: String): Boolean = Validators.mobile(phone) == null
}