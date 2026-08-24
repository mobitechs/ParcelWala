package com.mobitechs.parcelwala.data.model.response

import com.google.gson.annotations.SerializedName

// ─────────────────────────────────────────────────────────────────────────────
// FIX — camelCase alternates on every snake_case field.
//
// The backend does not agree with itself on casing. complete-profile rejected
// `full_name` as "required" for a non-empty value, which only happens when the
// key never bound — it wanted `fullName`. The same mismatch on the response side
// is why User.fullName came back null, and why the "I'm the sender" checkbox
// filled the phone number but left the name blank: there was no name to fill.
//
// Gson tries the primary name first and then each alternate, so this is safe
// either way and fixes nothing that already worked.
// ─────────────────────────────────────────────────────────────────────────────
data class ApiResponse<T>(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("message")
    val message: String?,

    @SerializedName("data")
    val data: T?
)

data class OtpData(
    @SerializedName("otp_id", alternate = ["otpId", "OtpId"])
    val otpId: String,

    @SerializedName("expires_in", alternate = ["expiresIn", "ExpiresIn"])
    val expiresIn: Int,

    @SerializedName("can_resend_after", alternate = ["canResendAfter", "CanResendAfter"])
    val canResendAfter: Int,

    @SerializedName("otp")
    val otp: String? = null
)

data class User(
    @SerializedName("user_id", alternate = ["userId", "UserId"])
    val userId: Int,

    @SerializedName("customer_id", alternate = ["customerId", "CustomerId"])
    val customerId: Int,

    @SerializedName("phone_number", alternate = ["phoneNumber", "PhoneNumber"])
    val phoneNumber: String,

    @SerializedName("full_name", alternate = ["fullName", "FullName"])
    val fullName: String?,

    @SerializedName("email")
    val email: String?,

    @SerializedName("profile_image", alternate = ["profileImage", "ProfileImage"])
    val profileImage: String?,

    @SerializedName("is_new_user", alternate = ["isNewUser", "IsNewUser"])
    val isNewUser: Boolean,

    @SerializedName("wallet_balance", alternate = ["walletBalance", "WalletBalance"])
    val walletBalance: Double,

    @SerializedName("referral_code", alternate = ["referralCode", "ReferralCode"])
    val referralCode: String?
)

data class AuthTokens(
    @SerializedName("access_token", alternate = ["accessToken", "AccessToken"])
    val accessToken: String,

    @SerializedName("refresh_token", alternate = ["refreshToken", "RefreshToken"])
    val refreshToken: String,

    @SerializedName("expires_in", alternate = ["expiresIn", "ExpiresIn"])
    val expiresIn: Int
)

data class LoginData(
    @SerializedName("user")
    val user: User,

    @SerializedName("tokens")
    val tokens: AuthTokens
)