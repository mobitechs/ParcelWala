package com.mobitechs.parcelwala.data.repository

import com.mobitechs.parcelwala.data.api.ApiService
import com.mobitechs.parcelwala.data.local.PreferencesManager
import com.mobitechs.parcelwala.data.model.request.CompleteProfileRequest
import com.mobitechs.parcelwala.data.model.request.SendOtpRequest
import com.mobitechs.parcelwala.data.model.request.VerifyOtpRequest
import com.mobitechs.parcelwala.data.model.response.LoginData
import com.mobitechs.parcelwala.data.model.response.OtpData
import com.mobitechs.parcelwala.data.model.response.User
import com.mobitechs.parcelwala.utils.NetworkResult
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

            val request = VerifyOtpRequest(
                phoneNumber = phoneNumber,
                otp = otp,
                deviceToken = preferencesManager.getDeviceToken() ?: "",
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

        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Unknown error occurred"))
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