package com.mobitechs.parcelwala.data.mock

import com.mobitechs.parcelwala.data.model.response.*

/**
 * Mock API Responses
 * When API is ready, just change USE_MOCK_DATA = false in AuthRepository
 */
object MockData {

    const val MASTER_OTP = "123456"

    /**
     * Mock Send OTP Response
     */
    fun getSendOtpResponse(): ApiResponse<OtpData> {
        return ApiResponse(
            success = true,
            message = "OTP sent successfully",
            data = OtpData(
                otpId = "mock_otp_${System.currentTimeMillis()}",
                expiresIn = 300,
                canResendAfter = 60
            )
        )
    }

    /**
     * Mock Verify OTP Response - New User
     */
    fun getVerifyOtpResponseNewUser(phoneNumber: String): ApiResponse<LoginData> {
        return ApiResponse(
            success = true,
            message = "Login successful",
            data = LoginData(
                user = User(
                    userId = (1000..9999).random(),
                    customerId = (2000..9999).random(),
                    phoneNumber = phoneNumber,
                    fullName = null,
                    email = null,
                    profileImage = null,
                    isNewUser = true,
                    walletBalance = 0.0,
                    referralCode = null
                ),
                tokens = AuthTokens(
                    accessToken = "mock_access_token_${System.currentTimeMillis()}",
                    refreshToken = "mock_refresh_token_${System.currentTimeMillis()}",
                    expiresIn = 3600
                )
            )
        )
    }

    /**
     * Mock Verify OTP Response - Existing User
     */
    fun getVerifyOtpResponseExistingUser(phoneNumber: String, userName: String): ApiResponse<LoginData> {
        return ApiResponse(
            success = true,
            message = "Login successful",
            data = LoginData(
                user = User(
                    userId = 1001,
                    customerId = 2001,
                    phoneNumber = phoneNumber,
                    fullName = userName,
                    email = "user@example.com",
                    profileImage = null,
                    isNewUser = false,
                    walletBalance = 150.0,
                    referralCode = "REFER123"
                ),
                tokens = AuthTokens(
                    accessToken = "mock_access_token_${System.currentTimeMillis()}",
                    refreshToken = "mock_refresh_token_${System.currentTimeMillis()}",
                    expiresIn = 3600
                )
            )
        )
    }

    /**
     * Mock Complete Profile Response
     */
    fun getCompleteProfileResponse(phoneNumber: String, fullName: String, email: String?): ApiResponse<User> {
        return ApiResponse(
            success = true,
            message = "Profile updated successfully",
            data = User(
                userId = 1001,
                customerId = 2001,
                phoneNumber = phoneNumber,
                fullName = fullName,
                email = email,
                profileImage = null,
                isNewUser = false,
                walletBalance = 0.0,
                referralCode = generateReferralCode()
            )
        )
    }

    /**
     * Mock Error Response
     */
    fun getErrorResponse(message: String): ApiResponse<Nothing> {
        return ApiResponse(
            success = false,
            message = message,
            data = null
        )
    }

    private fun generateReferralCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..8).map { chars.random() }.joinToString("")
    }
}