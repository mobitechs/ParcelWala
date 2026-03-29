package com.mobitechs.parcelwala.data.api

import com.mobitechs.parcelwala.data.local.PreferencesManager
import com.mobitechs.parcelwala.utils.Constants
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OkHttp Authenticator that automatically refreshes access tokens
 * when receiving 401 Unauthorized responses.
 *
 * FLOW:
 * 1. Any API call returns 401 Unauthorized
 * 2. OkHttp automatically triggers authenticate()
 * 3. synchronized(lock) ensures only ONE thread refreshes at a time
 * 4. If another thread already refreshed → reuse the new token directly (no double call)
 * 5. On refresh SUCCESS → save new tokens → retry original request with new token
 * 6. On refresh FAILURE → clearAll() → return null → user gets logged out
 *
 * RACE CONDITION PROTECTION:
 * If 3 API calls all get 401 simultaneously:
 *   - Thread 1 enters lock → refreshes → saves new tokens
 *   - Thread 2 & 3 wait → enter lock → detect token already changed → reuse new token
 *   - Result: only 1 refresh call is made ✅
 */
@Singleton
class TokenAuthenticator @Inject constructor(
    private val preferencesManager: PreferencesManager
) : Authenticator {

    companion object {
        // Only retry once — the refresh either works or it doesn't
        private const val MAX_RETRY_COUNT = 1
        private const val HEADER_RETRY_COUNT = "Retry-Count"
    }

    // Mutex — ensures only one thread calls the refresh endpoint at a time
    private val lock = Any()

    override fun authenticate(route: Route?, response: Response): Request? {

        // ── Step 1: Check retry count ──────────────────────────────────────
        // Prevents infinite loop: if we already retried once and still get 401,
        // the refresh token itself is invalid → log out.
        val retryCount = response.request.header(HEADER_RETRY_COUNT)?.toIntOrNull() ?: 0
        if (retryCount >= MAX_RETRY_COUNT) {
            runBlocking { preferencesManager.clearAll() }
            return null
        }

        // ── Step 2: Enter synchronized block ──────────────────────────────
        // Only one thread at a time can refresh. Others will wait here.
        synchronized(lock) {

            // ── Step 3: Check if another thread already refreshed ──────────
            // Compare the token that FAILED with what's currently saved.
            // If they differ, refresh already happened — just retry with new token.
            val savedToken   = preferencesManager.getAccessToken()
            val failedToken  = response.request
                .header("Authorization")
                ?.removePrefix("Bearer ")
                ?.trim()

            if (savedToken != null && savedToken != failedToken) {
                // Another thread already refreshed successfully.
                // No need to call refresh API again — reuse the saved token.
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $savedToken")
                    .header(HEADER_RETRY_COUNT, (retryCount + 1).toString())
                    .build()
            }

            // ── Step 4: Get refresh token ──────────────────────────────────
            val refreshToken = preferencesManager.getRefreshToken()
            if (refreshToken.isNullOrBlank()) {
                // No refresh token stored → session is invalid → log out
                runBlocking { preferencesManager.clearAll() }
                return null
            }

            // ── Step 5: Call refresh endpoint ─────────────────────────────
            val newAccessToken = refreshAccessToken(refreshToken)

            return if (newAccessToken != null) {
                // ── Step 6a: Refresh succeeded → retry original request ────
                response.request.newBuilder()
                    .header("Authorization", "Bearer $newAccessToken")
                    .header(HEADER_RETRY_COUNT, (retryCount + 1).toString())
                    .build()
            } else {
                // ── Step 6b: Refresh failed → clear session → log out ──────
                runBlocking { preferencesManager.clearAll() }
                null
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // refreshAccessToken
    // Uses a SEPARATE bare OkHttpClient (no interceptors, no authenticator)
    // to avoid recursive 401 loops.
    // ══════════════════════════════════════════════════════════════════════

    private fun refreshAccessToken(refreshToken: String): String? {
        return try {
            // Bare client — no auth interceptor, no TokenAuthenticator
            // so a failed refresh doesn't trigger another authenticate() call
            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()

            val jsonBody = JSONObject().apply {
                put("refresh_token", refreshToken)
            }.toString()

            val request = Request.Builder()
                .url("${Constants.BASE_URL}auth/refresh-token")
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                parseAndSaveTokens(response.body?.string())
            } else {
                // Server rejected the refresh token (expired / revoked)
                null
            }
        } catch (e: Exception) {
            // Network error, timeout, JSON parse error, etc.
            e.printStackTrace()
            null
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // parseAndSaveTokens
    // Parses the refresh response JSON, saves both tokens, returns new
    // access token so authenticate() can immediately retry the request.
    // ══════════════════════════════════════════════════════════════════════

    private fun parseAndSaveTokens(responseBody: String?): String? {
        return try {
            if (responseBody.isNullOrBlank()) return null

            val json = JSONObject(responseBody)

            // Check API-level success flag
            if (!json.optBoolean("success", false)) return null

            val data         = json.getJSONObject("data")
            val accessToken  = data.getString("access_token")
            val newRefresh   = data.getString("refresh_token")

            // Persist both tokens synchronously inside the lock
            runBlocking {
                preferencesManager.saveAccessToken(accessToken)
                preferencesManager.saveRefreshToken(newRefresh)
            }

            accessToken
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}