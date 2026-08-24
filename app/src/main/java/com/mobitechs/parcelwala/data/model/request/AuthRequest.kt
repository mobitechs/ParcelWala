package com.mobitechs.parcelwala.data.model.request

import com.google.gson.annotations.SerializedName

data class SendOtpRequest(
    @SerializedName("phone_number")
    val phoneNumber: String,

    @SerializedName("country_code")
    val countryCode: String = "+91",

    @SerializedName("purpose")
    val purpose: String = "login"
)

data class VerifyOtpRequest(
    @SerializedName("phone_number")
    val phoneNumber: String,

    @SerializedName("otp")
    val otp: String,

    @SerializedName("device_token")
    val deviceToken: String? = null,

    @SerializedName("device_type")
    val deviceType: String = "android"
)

data class CompleteProfileRequest(
    @SerializedName("full_name")
    val fullName: String,

    @SerializedName("email")
    val email: String?,

    @SerializedName("referral_code")
    val referralCode: String?
) {
    // ─────────────────────────────────────────────────────────────────────────
    // ⚠️ TEMPORARY — remove once the backend contract is confirmed.
    //
    // The server rejected a perfectly valid name with BOTH
    //   "Full name is required" AND "Full name must be at least 3 characters"
    // at the same time. Neither can be true for "Pratik sonawane", so the property
    // bound as null — meaning the JSON key never matched.
    //
    // ASP.NET's model binder is case-insensitive but does NOT map snake_case, so
    // `full_name` never reaches a `FullName` property. It wants camelCase.
    //
    // Sending both spellings works whichever way the DTO is defined, because
    // ASP.NET ignores properties it doesn't recognise. Ask the backend team which
    // casing that endpoint expects, then delete the two fields below.
    // ─────────────────────────────────────────────────────────────────────────

    @SerializedName("fullName")
    val fullNameCamel: String = fullName

    @SerializedName("referralCode")
    val referralCodeCamel: String? = referralCode
}