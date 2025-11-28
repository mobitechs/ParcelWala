package com.mobitechs.parcelwala.data.remote.firebase


import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.mobitechs.parcelwala.MainActivity
import com.mobitechs.parcelwala.R
import com.mobitechs.parcelwala.data.local.PreferencesManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Firebase Cloud Messaging Service
 * Handles incoming push notifications and token refresh
 */
@AndroidEntryPoint
class ParcelWalaFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var preferencesManager: PreferencesManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "FCMService"
        const val CHANNEL_ID_BOOKING = "booking_notifications"
        const val CHANNEL_ID_PROMOTION = "promotion_notifications"
        const val CHANNEL_ID_GENERAL = "general_notifications"
    }

    /**
     * Called when FCM token is refreshed
     * Store new token locally and send to server if user is logged in
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token: $token")

        serviceScope.launch {
            // Save token locally
            preferencesManager.saveDeviceToken(token)

            // If user is logged in, update token on server
            if (preferencesManager.isLoggedIn()) {
                sendTokenToServer(token)
            }
        }
    }

    /**
     * Called when a message is received
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "Message received from: ${remoteMessage.from}")

        // Handle data payload
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Data payload: ${remoteMessage.data}")
            handleDataMessage(remoteMessage.data)
        }

        // Handle notification payload (when app is in foreground)
        remoteMessage.notification?.let { notification ->
            Log.d(TAG, "Notification: ${notification.title} - ${notification.body}")
            showNotification(
                title = notification.title ?: "Parcel Wala",
                body = notification.body ?: "",
                data = remoteMessage.data
            )
        }
    }

    /**
     * Handle data-only messages
     */
    private fun handleDataMessage(data: Map<String, String>) {
        val notificationType = data["type"] ?: "general"
        val title = data["title"] ?: "Parcel Wala"
        val message = data["message"] ?: ""
        val bookingId = data["booking_id"]
        val deepLink = data["deep_link"]

        // Show notification based on type
        showNotification(
            title = title,
            body = message,
            data = data,
            channelId = getChannelForType(notificationType)
        )
    }

    /**
     * Display notification
     */
    private fun showNotification(
        title: String,
        body: String,
        data: Map<String, String>,
        channelId: String = CHANNEL_ID_GENERAL
    ) {
        // Create intent for notification tap
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)

            // Pass notification data
            data.forEach { (key, value) ->
                putExtra(key, value)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build notification
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification) // Add this icon to drawable
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel for Android O+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannels(notificationManager)
        }

        // Show notification
        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }

    /**
     * Create notification channels (Android 8.0+)
     */
    private fun createNotificationChannels(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channels = listOf(
                NotificationChannel(
                    CHANNEL_ID_BOOKING,
                    "Booking Updates",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifications about your booking status"
                    enableVibration(true)
                },
                NotificationChannel(
                    CHANNEL_ID_PROMOTION,
                    "Offers & Promotions",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Special offers and promotional messages"
                },
                NotificationChannel(
                    CHANNEL_ID_GENERAL,
                    "General",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "General notifications"
                }
            )

            channels.forEach { channel ->
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    /**
     * Get appropriate channel based on notification type
     */
    private fun getChannelForType(type: String): String {
        return when (type.lowercase()) {
            "booking", "driver_assigned", "trip_started", "trip_completed" -> CHANNEL_ID_BOOKING
            "promotion", "offer" -> CHANNEL_ID_PROMOTION
            else -> CHANNEL_ID_GENERAL
        }
    }

    /**
     * Send FCM token to server
     */
    private suspend fun sendTokenToServer(token: String) {
        // TODO: Implement API call to update device token
        // This should call your backend API to register the new token
        Log.d(TAG, "Token should be sent to server: $token")
    }
}