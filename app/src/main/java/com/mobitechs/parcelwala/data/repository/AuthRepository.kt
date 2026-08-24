package com.mobitechs.parcelwala.data.repository

import com.mobitechs.parcelwala.data.api.ApiService
import com.mobitechs.parcelwala.data.api.apiCall
import com.mobitechs.parcelwala.data.api.apiFlow
import com.mobitechs.parcelwala.data.local.PreferencesManager
import com.mobitechs.parcelwala.data.model.request.CompleteProfileRequest
import com.mobitechs.parcelwala.data.model.request.SendOtpRequest
import com.mobitechs.parcelwala.data.model.request.VerifyOtpRequest
import com.mobitechs.parcelwala.data.model.response.LoginData
import com.mobitechs.parcelwala.data.model.response.OtpData
import com.mobitechs.parcelwala.data.model.response.User
import com.mobitechs.parcelwala.utils.NetworkResult
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Auth calls.
 *
 * FIX — every method here used to hand-roll its own try/catch and throw the error
 * body away:
 *
 *     } else {
 *         emit(NetworkResult.Error("Network error"))
 *     }
 *
 * The server was telling us exactly which field failed and why, and we replaced it
 * with two words that were also inaccurate: the network was fine, the request wasn't.
 *
 * Everything now goes through apiCall(), which parses the body (ASP.NET problem
 * details, our own {success, message} envelope, plain text, HTML error pages) and
 * keeps the per-field validation map so screens can underline the exact input the
 * server rejected.
 */
class AuthRepository @Inject constructor(
    private val apiService: ApiService,
    private val preferencesManager: PreferencesManager
) {

    fun sendOtp(phoneNumber: String): Flow<NetworkResult<OtpData>> = apiFlow {
        apiCall {
            apiService.sendOtp(
                SendOtpRequest(
                    phoneNumber = phoneNumber.trim(),
                    countryCode = "+91",
                    purpose = "login"
                )
            )
        }
    }

    fun verifyOtp(
        phoneNumber: String,
        otp: String
    ): Flow<NetworkResult<LoginData>> = apiFlow {
        apiCall {
            apiService.verifyOtp(
                VerifyOtpRequest(
                    phoneNumber = phoneNumber.trim(),
                    otp = otp.trim(),
                    deviceToken = preferencesManager.getDeviceToken() ?: "",
                    deviceType = "android"
                )
            )
        }.also { result ->
            if (result is NetworkResult.Success) {
                result.data?.let { saveUserSession(it) }
            }
        }
    }

    fun completeProfile(
        fullName: String,
        email: String?,
        referralCode: String?
    ): Flow<NetworkResult<User>> = apiFlow {
        apiCall {
            apiService.completeProfile(
                CompleteProfileRequest(
                    fullName = fullName.trim(),
                    email = email?.trim()?.takeIf { it.isNotEmpty() },
                    referralCode = referralCode?.trim()?.takeIf { it.isNotEmpty() }
                )
            )
        }.also { result ->
            if (result is NetworkResult.Success) {
                result.data?.let { preferencesManager.saveUser(it) }
            }
        }
    }

    fun logout(): Flow<NetworkResult<Unit>> = apiFlow {
        val refreshToken = preferencesManager.getRefreshToken().orEmpty()
        val result = apiCall<Unit> {
            apiService.logout(mapOf("refresh_token" to refreshToken))
        }
        // Whether or not the server acknowledged it, the session is over on this
        // device. Never leave a user stuck logged in because logout got a 500.
        clearUserSession()
        if (result is NetworkResult.Error) NetworkResult.Success(Unit) else result
    }

    private suspend fun saveUserSession(loginData: LoginData) {
        preferencesManager.saveAccessToken(loginData.tokens.accessToken)
        preferencesManager.saveRefreshToken(loginData.tokens.refreshToken)
        preferencesManager.saveUser(loginData.user)
    }

    private suspend fun clearUserSession() {
        preferencesManager.clearAll()
    }

    fun isUserLoggedIn(): Boolean = preferencesManager.getAccessToken() != null

    fun getUserData(): User? = preferencesManager.getUser()
}
