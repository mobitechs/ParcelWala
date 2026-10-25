package com.mobitechs.parcelwala.data.model.request

import com.google.gson.annotations.SerializedName

/**
 * `POST auth/customer/send-otp`.
 *
 * Carries EITHER [phoneNumber] (+ [countryCode]) OR [email], never both — see
 * backend-docs/EMAIL_LOGIN_API.md. Null fields are left out of the JSON (the
 * app's Gson does not serialize nulls), so an email request has no phone keys.
 * Build it through [LoginIdentifier.toSendOtpRequest] rather than by hand.
 */
data class SendOtpRequest(
    @SerializedName("phone_number")
    val phoneNumber: String? = null,

    @SerializedName("country_code")
    val countryCode: String? = null,

    @SerializedName("email")
    val email: String? = null,

    @SerializedName("purpose")
    val purpose: String = "login"
)

/** `POST auth/customer/verify-otp` — same either/or rule as [SendOtpRequest]. */
data class VerifyOtpRequest(
    @SerializedName("phone_number")
    val phoneNumber: String? = null,

    @SerializedName("email")
    val email: String? = null,

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
/** How the customer chose to log in on the Mobile | Email toggle. */
enum class LoginMethod { PHONE, EMAIL }

/**
 * What the customer logs in with: a 10-digit mobile number or an email address.
 *
 * One value carried from the login screen through the OTP screen (as a route
 * argument) so send, resend and verify always address the same identifier the
 * same way. [normalized] is what goes on the wire: digits only for a phone,
 * trimmed and lower-cased for an email — the backend compares emails
 * lower-cased, so "Name@Example.com" and "name@example.com" are one account.
 */
data class LoginIdentifier(
    val method: LoginMethod,
    val value: String
) {
    val isEmail: Boolean get() = method == LoginMethod.EMAIL

    val normalized: String
        get() = when (method) {
            LoginMethod.PHONE -> value.filter { it.isDigit() }
            LoginMethod.EMAIL -> value.trim().lowercase()
        }

    fun toSendOtpRequest(): SendOtpRequest = when (method) {
        LoginMethod.PHONE -> SendOtpRequest(phoneNumber = normalized, countryCode = "+91")
        LoginMethod.EMAIL -> SendOtpRequest(email = normalized)
    }

    fun toVerifyOtpRequest(otp: String, deviceToken: String): VerifyOtpRequest = when (method) {
        LoginMethod.PHONE -> VerifyOtpRequest(phoneNumber = normalized, otp = otp, deviceToken = deviceToken)
        LoginMethod.EMAIL -> VerifyOtpRequest(email = normalized, otp = otp, deviceToken = deviceToken)
    }

    companion object {
        fun phone(number: String) = LoginIdentifier(LoginMethod.PHONE, number)
        fun email(address: String) = LoginIdentifier(LoginMethod.EMAIL, address)

        /** From the OTP route's `method` argument; anything unrecognised is a phone. */
        fun fromRoute(method: String?, value: String) = LoginIdentifier(
            method = if (method.equals(LoginMethod.EMAIL.name, ignoreCase = true)) LoginMethod.EMAIL
            else LoginMethod.PHONE,
            value = value
        )
    }
}
