package com.mobitechs.parcelwala.data.api

import com.mobitechs.parcelwala.data.local.PreferencesManager
import com.mobitechs.parcelwala.utils.Constants
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OkHttp Authenticator that automatically refreshes access tokens
 * when receiving 401 Unauthorized responses.
 *
 * Flow:
 * 1. API call returns 401
 * 2. Authenticator intercepts and calls refresh token API
 * 3. On success: saves new tokens, retries original request
 * 4. On failure: clears session (user needs to login again)
 */
@Singleton
class TokenAuthenticator @Inject constructor(
    private val preferencesManager: PreferencesManager
) : Authenticator {

    companion object {
        private const val MAX_RETRY_COUNT = 2
        private const val HEADER_RETRY_COUNT = "Retry-Count"
    }

    override fun authenticate(route: Route?, response: Response): Request? {
        // Check retry count to prevent infinite loops
        val retryCount = response.request.header(HEADER_RETRY_COUNT)?.toIntOrNull() ?: 0
        if (retryCount >= MAX_RETRY_COUNT) {
            // Max retries reached, clear session and return null
            runBlocking { preferencesManager.clearAll() }
            return null
        }

        // Get current refresh token
        val refreshToken = preferencesManager.getRefreshToken() ?: run {
            // No refresh token available, clear session
            runBlocking { preferencesManager.clearAll() }
            return null
        }

        // Attempt to refresh the token
        val newAccessToken = refreshAccessToken(refreshToken)

        return if (newAccessToken != null) {
            // Retry original request with new token
            response.request.newBuilder()
                .header("Authorization", "Bearer $newAccessToken")
                .header(HEADER_RETRY_COUNT, (retryCount + 1).toString())
                .build()
        } else {
            // Refresh failed, clear session
            runBlocking { preferencesManager.clearAll() }
            null
        }
    }

    /**
     * Calls refresh token API synchronously.
     * Returns new access token on success, null on failure.
     */
    private fun refreshAccessToken(refreshToken: String): String? {
        return try {
            // Create a separate OkHttpClient without the authenticator
            // to avoid infinite loops
            val client = OkHttpClient.Builder()
                .build()

            // Build request body
            val jsonBody = JSONObject().apply {
                put("refresh_token", refreshToken)
            }.toString()

            val requestBody = jsonBody.toRequestBody("application/json".toMediaType())

            val request = okhttp3.Request.Builder()
                .url("${Constants.BASE_URL}auth/refresh-token")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                parseTokenResponse(responseBody)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Parses the refresh token API response and saves new tokens.
     */
    private fun parseTokenResponse(responseBody: String?): String? {
        return try {
            responseBody?.let { body ->
                val json = JSONObject(body)

                // Check if response is successful
                if (json.optBoolean("success", false)) {
                    val data = json.getJSONObject("data")
                    val accessToken = data.getString("access_token")
                    val newRefreshToken = data.getString("refresh_token")

                    // Save new tokens
                    runBlocking {
                        preferencesManager.saveAccessToken(accessToken)
                        preferencesManager.saveRefreshToken(newRefreshToken)
                    }

                    accessToken
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}