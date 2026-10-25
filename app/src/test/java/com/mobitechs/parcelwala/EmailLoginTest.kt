package com.mobitechs.parcelwala

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.mobitechs.parcelwala.data.model.request.LoginIdentifier
import com.mobitechs.parcelwala.data.model.request.LoginMethod
import com.mobitechs.parcelwala.data.model.response.ApiResponse
import com.mobitechs.parcelwala.data.model.response.LoginData
import com.google.gson.reflect.TypeToken
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Email login: the request bodies the app sends must match
 * backend-docs/EMAIL_LOGIN_API.md exactly — either `phone_number` or `email`,
 * never both — and phone login must send what it always sent.
 */
class EmailLoginTest {

    // The app's Retrofit uses GsonConverterFactory.create(), i.e. a default Gson.
    private val gson = Gson()

    private fun json(any: Any) = JsonParser.parseString(gson.toJson(any)).asJsonObject

    @Test
    fun emailSendOtpCarriesOnlyEmailAndPurpose() {
        val body = json(LoginIdentifier.email("  Name@Example.COM ").toSendOtpRequest())
        assertEquals("name@example.com", body["email"].asString)
        assertEquals("login", body["purpose"].asString)
        assertFalse(body.has("phone_number"))
        assertFalse(body.has("country_code"))
    }

    @Test
    fun phoneSendOtpIsUnchanged() {
        val body = json(LoginIdentifier.phone("98765 43210").toSendOtpRequest())
        assertEquals("9876543210", body["phone_number"].asString)
        assertEquals("+91", body["country_code"].asString)
        assertEquals("login", body["purpose"].asString)
        assertFalse(body.has("email"))
    }

    @Test
    fun verifyOtpUsesTheSameIdentifier() {
        val email = json(LoginIdentifier.email("name@example.com").toVerifyOtpRequest("4821", "fcm"))
        assertEquals("name@example.com", email["email"].asString)
        assertEquals("4821", email["otp"].asString)
        assertEquals("android", email["device_type"].asString)
        assertFalse(email.has("phone_number"))

        val phone = json(LoginIdentifier.phone("9876543210").toVerifyOtpRequest("4821", "fcm"))
        assertEquals("9876543210", phone["phone_number"].asString)
        assertFalse(phone.has("email"))
    }

    @Test
    fun routeArgumentsRoundTrip() {
        assertEquals(LoginMethod.EMAIL, LoginIdentifier.fromRoute("EMAIL", "a@b.co").method)
        assertEquals(LoginMethod.PHONE, LoginIdentifier.fromRoute("PHONE", "9876543210").method)
        // Old links / missing argument fall back to phone, the pre-toggle behaviour.
        assertEquals(LoginMethod.PHONE, LoginIdentifier.fromRoute(null, "9876543210").method)
        assertTrue(LoginIdentifier.fromRoute("email", "a@b.co").isEmail)
    }

    @Test
    fun emailSignUpUserWithNullPhoneParses() {
        val type = object : TypeToken<ApiResponse<LoginData>>() {}.type
        val response: ApiResponse<LoginData> = gson.fromJson(
            """{ "success": true, "message": "Login successful", "data": {
                 "user": { "user_id": 1042, "customer_id": 877, "phone_number": null,
                           "full_name": null, "email": "name@example.com", "profile_image": null,
                           "is_new_user": true, "wallet_balance": 0, "referral_code": "PW8K2Q" },
                 "tokens": { "access_token": "a", "refresh_token": "r", "expires_in": 3600 } } }""",
            type
        )
        val user = response.data!!.user
        assertNull(user.phoneNumber)
        assertEquals("name@example.com", user.email)
        assertTrue(user.isNewUser)
    }
}
