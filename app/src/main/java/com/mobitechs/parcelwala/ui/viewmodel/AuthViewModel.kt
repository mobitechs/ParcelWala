package com.mobitechs.parcelwala.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobitechs.parcelwala.data.model.request.LoginIdentifier
import com.mobitechs.parcelwala.data.model.request.LoginMethod
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
import java.io.File
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
        private const val KEY_LOGIN_METHOD = "login_method"
    }

    private var currentPhoneNumber: String
        get() = savedStateHandle[KEY_PHONE_NUMBER] ?: ""
        set(value) {
            savedStateHandle[KEY_PHONE_NUMBER] = value
        }

    /**
     * The identifier this login is for — mobile or email. Stored in the
     * SavedStateHandle beside the value so a process death between "send" and
     * "verify" does not turn an email login back into a phone one.
     */
    private var currentIdentifier: LoginIdentifier
        get() = LoginIdentifier.fromRoute(savedStateHandle[KEY_LOGIN_METHOD], currentPhoneNumber)
        set(value) {
            savedStateHandle[KEY_LOGIN_METHOD] = value.method.name
            currentPhoneNumber = value.value
        }

    /**
     * Complete profile asks for a MOBILE NUMBER (required) instead of an email
     * when the customer signed up by email. Read once from the saved user,
     * which verify-otp stored moments before this screen opened.
     */
    val isEmailSignUp: Boolean
        get() = authRepository.isEmailSignUpWithoutPhone()

    /** The email this customer signed in with, for the "Signed in with …" line. */
    val signedInEmail: String?
        get() = authRepository.getUserData()?.email?.takeIf { it.isNotBlank() }

    fun sendOtp(phoneNumber: String) = sendOtp(LoginIdentifier.phone(phoneNumber))

    /** Send a code by SMS or email; validation matches the toggle's field. */
    fun sendOtp(identifier: LoginIdentifier) {
        val invalid = when (identifier.method) {
            LoginMethod.PHONE -> if (isValidPhoneNumber(identifier.value)) null
            else "Please enter a valid phone number"
            LoginMethod.EMAIL -> Validators.emailRequired(identifier.value)
        }
        if (invalid != null) {
            _uiState.update { it.copy(error = invalid) }
            return
        }

        currentIdentifier = identifier

        viewModelScope.launch {
            authRepository.sendOtp(identifier).collect { result ->
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
    fun verifyOtp(otp: String, phoneNumber: String? = null) =
        verifyOtp(otp, phoneNumber?.let { LoginIdentifier.phone(it) } ?: currentIdentifier)

    /** Verify a 4-digit code for the mobile number or email it was sent to. */
    fun verifyOtp(otp: String, identifier: LoginIdentifier) {
        if (otp.length != 4) {  // ✅ FIXED: Was 6, now 4
            _uiState.update { it.copy(error = "Please enter a valid 4-digit OTP") }  // ✅ FIXED
            return
        }

        if (identifier.value.isBlank()) {
            val what = if (identifier.isEmail) "Email" else "Phone number"
            _uiState.update { it.copy(error = "$what not found. Please go back and try again.") }
            return
        }

        viewModelScope.launch {
            authRepository.verifyOtp(identifier, otp).collect { result ->
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
        referralCode: String?,
        profilePhoto: File? = null,
        /** Required (and only sent) for email sign-ups. */
        phoneNumber: String? = null
    ) {
        Validators.fullName(fullName)?.let { message ->
            _uiState.update { it.copy(error = message, fieldErrors = mapOf("full_name" to listOf(message))) }
            return
        }
        Validators.emailOptional(email)?.let { message ->
            _uiState.update { it.copy(error = message, fieldErrors = mapOf("email" to listOf(message))) }
            return
        }
        if (isEmailSignUp) {
            Validators.mobile(phoneNumber)?.let { message ->
                _uiState.update { it.copy(error = message, fieldErrors = mapOf("phone_number" to listOf(message))) }
                return
            }
        }

        viewModelScope.launch {
            authRepository.completeProfile(
                fullName = fullName,
                email = email,
                referralCode = referralCode,
                profilePhoto = profilePhoto,
                phoneNumber = phoneNumber.takeIf { isEmailSignUp }
            ).collect { result ->
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

//    fun completeProfile(
//        fullName: String,
//        email: String?,
//        referralCode: String?
//    ) {
//        // Client-side rules mirror the backend's, so an obviously bad value never
//        // costs a round trip. The screen already gates its button on the same rules;
//        // this is the backstop for programmatic callers.
//        Validators.fullName(fullName)?.let { message ->
//            _uiState.update { it.copy(error = message, fieldErrors = mapOf("full_name" to listOf(message))) }
//            return
//        }
//        Validators.emailOptional(email)?.let { message ->
//            _uiState.update { it.copy(error = message, fieldErrors = mapOf("email" to listOf(message))) }
//            return
//        }
//
//        viewModelScope.launch {
//            authRepository.completeProfile(fullName, email, referralCode).collect { result ->
//                when (result) {
//                    is NetworkResult.Loading -> {
//                        _uiState.update {
//                            it.copy(isLoading = true, error = null, fieldErrors = emptyMap())
//                        }
//                    }
//
//                    is NetworkResult.Success -> {
//                        _uiState.update {
//                            it.copy(
//                                isLoading = false,
//                                profileCompleted = true,
//                                error = null,
//                                fieldErrors = emptyMap()
//                            )
//                        }
//                    }
//
//                    is NetworkResult.Error -> {
//                        // Carry the server's per-field map through so the screen can
//                        // underline the exact input that was rejected.
//                        _uiState.update {
//                            it.copy(
//                                isLoading = false,
//                                error = result.message,
//                                fieldErrors = result.fieldErrors
//                            )
//                        }
//                    }
//                }
//            }
//        }
//    }

    fun clearError() {
        _uiState.update { it.copy(error = null, fieldErrors = emptyMap()) }
    }

    fun resetOtpState() {
        _uiState.update { it.copy(otpSent = false, otpData = null) }
    }

    fun setPhoneNumber(phoneNumber: String) {
        currentIdentifier = LoginIdentifier.phone(phoneNumber)
    }

    /** The OTP screen hands over what the code was sent to — phone or email. */
    fun setIdentifier(identifier: LoginIdentifier) {
        currentIdentifier = identifier
    }

    // Phone and email rules now live in Validators so the screens, the ViewModel
    // and the backend all agree on what "valid" means.
    private fun isValidPhoneNumber(phone: String): Boolean = Validators.mobile(phone) == null
}