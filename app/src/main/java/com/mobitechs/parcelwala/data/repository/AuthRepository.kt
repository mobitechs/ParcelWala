package com.mobitechs.parcelwala.data.repository

import com.mobitechs.parcelwala.data.api.ApiService
import com.mobitechs.parcelwala.data.local.PreferencesManager
import com.mobitechs.parcelwala.data.mock.MockData
import com.mobitechs.parcelwala.data.model.request.*
import com.mobitechs.parcelwala.data.model.response.*
import com.mobitechs.parcelwala.utils.Constants.USE_MOCK_DATA
import com.mobitechs.parcelwala.utils.NetworkResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class AuthRepository @Inject constructor(
    private val apiService: ApiService,
    private val preferencesManager: PreferencesManager
) {

    companion object {

        private const val MOCK_DELAY = 1500L // Simulate network delay
    }

    fun sendOtp(phoneNumber: String): Flow<NetworkResult<OtpData>> = flow {
        emit(NetworkResult.Loading())

        try {
            if (USE_MOCK_DATA) {
                // ========== MOCK MODE ==========
                delay(MOCK_DELAY)
                val mockResponse = MockData.getSendOtpResponse()

                if (mockResponse.success && mockResponse.data != null) {
                    emit(NetworkResult.Success(mockResponse.data))
                } else {
                    emit(NetworkResult.Error(mockResponse.message ?: "Failed to send OTP"))
                }
                // ================================
            } else {
                // ========== REAL API ==========
                val request = SendOtpRequest(
                    phoneNumber = phoneNumber,
                    countryCode = "+91",
                    purpose = "login"
                )

                val response = apiService.sendOtp(request)

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.success == true && body.data != null) {
                        emit(NetworkResult.Success(body.data))
                    } else {
                        emit(NetworkResult.Error(body?.message ?: "Failed to send OTP"))
                    }
                } else {
                    emit(NetworkResult.Error("Network error: ${response.code()}"))
                }
                // ==============================
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Unknown error occurred"))
        }
    }

    fun verifyOtp(
        phoneNumber: String,
        otp: String
    ): Flow<NetworkResult<LoginData>> = flow {
        emit(NetworkResult.Loading())

        try {

            if (USE_MOCK_DATA) {
                // ========== MOCK MODE ==========
                delay(MOCK_DELAY)

                // Check master OTP
                if (otp != MockData.MASTER_OTP) {
                    emit(NetworkResult.Error("Invalid OTP. Use: ${MockData.MASTER_OTP}"))
                    return@flow
                }

                // Check if existing user
                val existingUser = preferencesManager.getUser()

                val mockResponse = if (existingUser != null) {
                    MockData.getVerifyOtpResponseExistingUser(phoneNumber, existingUser.fullName ?: "User")
                } else {
                    MockData.getVerifyOtpResponseNewUser(phoneNumber)
                }

                if (mockResponse.success && mockResponse.data != null) {
                    saveUserSession(mockResponse.data)
                    emit(NetworkResult.Success(mockResponse.data))
                } else {
                    emit(NetworkResult.Error(mockResponse.message ?: "OTP verification failed"))
                }
                // ================================
            } else {

                // ========== REAL API ==========
                val request = VerifyOtpRequest(
                    phoneNumber = phoneNumber,
                    otp = otp,
                    deviceToken =  preferencesManager.getDeviceToken() ?: "",
                    deviceType = "android"
                )

                val response = apiService.verifyOtp(request)

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.success == true && body.data != null) {
                        saveUserSession(body.data)
                        emit(NetworkResult.Success(body.data))
                    } else {
                        emit(NetworkResult.Error(body?.message ?: "OTP verification failed"))
                    }
                } else {
                    emit(NetworkResult.Error("Invalid OTP or network error"))
                }
                // ==============================
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Unknown error occurred"))
        }
    }

    fun completeProfile(
        fullName: String,
        email: String?,
        referralCode: String?
    ): Flow<NetworkResult<User>> = flow {
        emit(NetworkResult.Loading())

        try {
            val phoneNumber = preferencesManager.getUser()?.phoneNumber ?: ""

            if (USE_MOCK_DATA) {
                // ========== MOCK MODE ==========
                delay(MOCK_DELAY)

                if (fullName.isBlank()) {
                    emit(NetworkResult.Error("Please enter your name"))
                    return@flow
                }

                val mockResponse = MockData.getCompleteProfileResponse(phoneNumber, fullName, email)

                if (mockResponse.success && mockResponse.data != null) {
                    preferencesManager.saveUser(mockResponse.data)
                    emit(NetworkResult.Success(mockResponse.data))
                } else {
                    emit(NetworkResult.Error(mockResponse.message ?: "Failed to update profile"))
                }
                // ================================
            } else {
                // ========== REAL API ==========
                val request = CompleteProfileRequest(
                    fullName = fullName,
                    email = email,
                    referralCode = referralCode
                )

                val response = apiService.completeProfile(request)

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.success == true && body.data != null) {
                        preferencesManager.saveUser(body.data)
                        emit(NetworkResult.Success(body.data))
                    } else {
                        emit(NetworkResult.Error(body?.message ?: "Failed to update profile"))
                    }
                } else {
                    emit(NetworkResult.Error("Network error"))
                }
                // ==============================
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Unknown error occurred"))
        }
    }

    suspend fun logout() {
        try {
            if (!USE_MOCK_DATA) {
                val deviceToken = preferencesManager.getDeviceToken()
                apiService.logout(mapOf("device_token" to deviceToken.orEmpty()))
            }
        } catch (e: Exception) {
            // Ignore error, clear local session anyway
        } finally {
            clearUserSession()
        }
    }

    private suspend fun saveUserSession(loginData: LoginData) {
        preferencesManager.saveAccessToken(loginData.tokens.accessToken)
        preferencesManager.saveRefreshToken(loginData.tokens.refreshToken)
        preferencesManager.saveUser(loginData.user)
    }

    private suspend fun clearUserSession() {
        preferencesManager.clearAll()
    }

    fun isUserLoggedIn(): Boolean {
        return preferencesManager.getAccessToken() != null
    }

    fun getUserData(): User? {
        return preferencesManager.getUser()
    }
}