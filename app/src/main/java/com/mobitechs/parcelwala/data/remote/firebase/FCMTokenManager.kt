package com.mobitechs.parcelwala.data.remote.firebase



import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.mobitechs.parcelwala.data.local.PreferencesManager
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages FCM token retrieval and storage
 */
@Singleton
class FCMTokenManager @Inject constructor(
    private val preferencesManager: PreferencesManager
) {
    companion object {
        private const val TAG = "FCMTokenManager"
    }

    /**
     * Get current FCM token
     * Returns cached token or fetches new one
     */
    suspend fun getToken(): String? {
        return try {
            // Try to get cached token first
            var token = preferencesManager.getDeviceToken()

            if (token.isNullOrEmpty()) {
                // Fetch new token from Firebase
                token = FirebaseMessaging.getInstance().token.await()
                preferencesManager.saveDeviceToken(token)
                Log.d(TAG, "New FCM token obtained: $token")
            }

            token
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get FCM token", e)
            null
        }
    }

    /**
     * Force refresh FCM token
     */
    suspend fun refreshToken(): String? {
        return try {
            // Delete existing token
            FirebaseMessaging.getInstance().deleteToken().await()

            // Get new token
            val newToken = FirebaseMessaging.getInstance().token.await()
            preferencesManager.saveDeviceToken(newToken)
            Log.d(TAG, "FCM token refreshed: $newToken")

            newToken
        } catch (e: Exception) {
            Log.e(TAG, "Failed to refresh FCM token", e)
            null
        }
    }

    /**
     * Subscribe to a topic (e.g., for broadcast notifications)
     */
    suspend fun subscribeToTopic(topic: String): Boolean {
        return try {
            FirebaseMessaging.getInstance().subscribeToTopic(topic).await()
            Log.d(TAG, "Subscribed to topic: $topic")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to subscribe to topic: $topic", e)
            false
        }
    }

    /**
     * Unsubscribe from a topic
     */
    suspend fun unsubscribeFromTopic(topic: String): Boolean {
        return try {
            FirebaseMessaging.getInstance().unsubscribeFromTopic(topic).await()
            Log.d(TAG, "Unsubscribed from topic: $topic")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unsubscribe from topic: $topic", e)
            false
        }
    }
}