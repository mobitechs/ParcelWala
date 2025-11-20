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
)