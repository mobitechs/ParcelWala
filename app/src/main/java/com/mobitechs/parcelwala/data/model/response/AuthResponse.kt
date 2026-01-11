package com.mobitechs.parcelwala.data.model.response

import com.google.gson.annotations.SerializedName

data class ApiResponse<T>(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("message")
    val message: String?,

    @SerializedName("data")
    val data: T?
)

data class OtpData(
    @SerializedName("otp_id")
    val otpId: String,

    @SerializedName("expires_in")
    val expiresIn: Int,

    @SerializedName("can_resend_after")
    val canResendAfter: Int,

    @SerializedName("otp")
    val otp: String? = null
)

data class User(
    @SerializedName("user_id")
    val userId: Int,

    @SerializedName("customer_id")
    val customerId: Int,

    @SerializedName("phone_number")
    val phoneNumber: String,

    @SerializedName("full_name")
    val fullName: String?,

    @SerializedName("email")
    val email: String?,

    @SerializedName("profile_image")
    val profileImage: String?,

    @SerializedName("is_new_user")
    val isNewUser: Boolean,

    @SerializedName("wallet_balance")
    val walletBalance: Double,

    @SerializedName("referral_code")
    val referralCode: String?
)

data class AuthTokens(
    @SerializedName("access_token")
    val accessToken: String,

    @SerializedName("refresh_token")
    val refreshToken: String,

    @SerializedName("expires_in")
    val expiresIn: Int
)

data class LoginData(
    @SerializedName("user")
    val user: User,

    @SerializedName("tokens")
    val tokens: AuthTokens
)