// utils/BookingNotificationHelper.kt
// ✅ FIXED: Single sticky notification pattern - replaces instead of stacking
package com.mobitechs.parcelwala.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.mobitechs.parcelwala.MainActivity
import com.mobitechs.parcelwala.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ════════════════════════════════════════════════════════════════════════════
 * BOOKING NOTIFICATION HELPER - FIXED
 * ════════════════════════════════════════════════════════════════════════════
 *
 * ✅ FIX: Uses a SINGLE notification ID that gets UPDATED (not stacked)
 *    - Each status update replaces the previous notification
 *    - Only the latest status is visible to the user
 *    - Final notifications (delivered, cancelled) are auto-dismissible
 *
 * ════════════════════════════════════════════════════════════════════════════
 */
@Singleton
class BookingNotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "BookingNotification"

        // Channel IDs
        const val CHANNEL_BOOKING_STATUS = "booking_status_channel"

        // ✅ FIX: SINGLE notification ID for all booking status updates
        // This ensures each new status REPLACES the previous notification
        const val NOTIFICATION_BOOKING_STATUS = 2001

        // Legacy IDs - kept for cancellation compatibility
        const val NOTIFICATION_DRIVER_ASSIGNED = 2001
        const val NOTIFICATION_DRIVER_ARRIVED = 2001
        const val NOTIFICATION_PICKED_UP = 2001
        const val NOTIFICATION_ARRIVED_DELIVERY = 2001
        const val NOTIFICATION_DELIVERED = 2001
        const val NOTIFICATION_CANCELLED = 2001
        const val NOTIFICATION_TRACKING_PROGRESS = 2007
    }

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val statusChannel = NotificationChannel(
                CHANNEL_BOOKING_STATUS,
                "Booking Status",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Important updates about your booking"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 250, 250)
                enableLights(true)
                lightColor = 0xFFFF6B35.toInt()
            }

            notificationManager.createNotificationChannel(statusChannel)
            Log.d(TAG, "✅ Notification channel created")
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ✅ PRIMARY METHOD: Single Sticky Status Notification
    // Each call REPLACES the previous notification
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Shows/updates the single sticky booking notification.
     * This REPLACES any previous booking notification.
     *
     * @param bookingId Booking ID for deep-link
     * @param title Notification title (e.g., "Driver Assigned!")
     * @param body Notification body text
     * @param isFinal If true, notification is auto-dismissible (for delivered/cancelled)
     * @param isSilent If true, no sound/vibration (for location updates)
     */
    fun showStickyStatusNotification(
        bookingId: String,
        title: String,
        body: String,
        isFinal: Boolean = false,
        isSilent: Boolean = false
    ) {
        val pendingIntent = createPendingIntent(bookingId)

        val builder = NotificationCompat.Builder(context, CHANNEL_BOOKING_STATUS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body.lines().firstOrNull() ?: body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(if (isSilent) NotificationCompat.PRIORITY_LOW else NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(isFinal)           // Auto-dismiss only for final states
            .setOngoing(!isFinal)             // Sticky for in-progress states
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setOnlyAlertOnce(isSilent)       // Don't re-alert for silent updates

        if (!isSilent) {
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            builder.setSound(soundUri)
            vibrate(longArrayOf(0, 300, 200, 300))
        } else {
            builder.setSilent(true)
        }

        // ✅ KEY FIX: Always use the SAME notification ID
        // This ensures the new notification REPLACES the old one
        notificationManager.notify(NOTIFICATION_BOOKING_STATUS, builder.build())
        Log.d(TAG, "📱 Notification updated: $title")
    }

    // ═══════════════════════════════════════════════════════════════════════
    // LEGACY METHODS - Now delegate to showStickyStatusNotification
    // Kept for backward compatibility
    // ═══════════════════════════════════════════════════════════════════════

    fun showDriverAssignedNotification(
        bookingId: String,
        driverName: String?,
        vehicleNumber: String?,
        vehicleType: String?,
        otp: String?,
        etaMinutes: Int?
    ) {
        showStickyStatusNotification(
            bookingId = bookingId,
            title = "🚗 Driver Assigned!",
            body = buildString {
                append(driverName ?: "Your driver")
                append(" is on the way")
                etaMinutes?.let { if (it > 0) append("\n⏱️ Arriving in ~$it min") }
                vehicleType?.let { append("\n🚚 $it") }
                vehicleNumber?.let { append(" • $it") }
                otp?.let { append("\n\n🔐 Pickup OTP: $it") }
            }
        )
    }

    fun showDriverTrackingProgress(
        bookingId: String,
        driverName: String?,
        distanceMeters: Double,
        etaMinutes: Int?,
        maxDistanceMeters: Double = 5000.0
    ) {
        val distanceKm = distanceMeters / 1000.0
        showStickyStatusNotification(
            bookingId = bookingId,
            title = "🚗 ${driverName ?: "Driver"} is on the way",
            body = buildString {
                append("📍 ${String.format("%.1f", distanceKm)} km away")
                etaMinutes?.let { if (it > 0) append(" • ~$it min") }
            },
            isSilent = true
        )
    }

    fun showDriverArrivedNotification(
        bookingId: String,
        driverName: String?,
        otp: String?
    ) {
        showStickyStatusNotification(
            bookingId = bookingId,
            title = "📍 Driver Has Arrived!",
            body = buildString {
                append(driverName ?: "Your driver")
                append(" is at your pickup location")
                otp?.let { append("\n\n🔐 Share OTP: $it") }
            }
        )
    }

    fun showParcelPickedUpNotification(
        bookingId: String,
        dropAddress: String?
    ) {
        showStickyStatusNotification(
            bookingId = bookingId,
            title = "📦 Parcel Picked Up!",
            body = buildString {
                append("Your parcel is on the way to delivery")
                dropAddress?.let {
                    val shortAddress = if (it.length > 50) it.take(50) + "..." else it
                    append("\n📍 To: $shortAddress")
                }
            }
        )
    }

    fun showArrivedAtDeliveryNotification(
        bookingId: String,
        deliveredOtp: String?
    ) {
        showStickyStatusNotification(
            bookingId = bookingId,
            title = "🏠 Arriving at Delivery!",
            body = buildString {
                append("Driver has arrived at delivery location")
                deliveredOtp?.let { append("\n🔐 Delivery OTP: $it") }
            }
        )
    }

    fun showDeliveryCompletedNotification(
        bookingId: String,
        fare: Int?
    ) {
        showStickyStatusNotification(
            bookingId = bookingId,
            title = "✅ Delivery Completed!",
            body = buildString {
                append("Your parcel has been delivered successfully!")
                fare?.let { append("\n💰 Total: ₹$it") }
                append("\n\n⭐ Rate your experience")
            },
            isFinal = true
        )
    }

    fun showBookingCancelledNotification(
        bookingId: String,
        reason: String?,
        cancelledBy: String?
    ) {
        showStickyStatusNotification(
            bookingId = bookingId,
            title = "❌ Booking Cancelled",
            body = buildString {
                when (cancelledBy?.lowercase()) {
                    "driver" -> append("Driver cancelled the booking")
                    "system" -> append("Booking was cancelled by system")
                    "customer" -> append("You cancelled the booking")
                    else -> append("Booking has been cancelled")
                }
                reason?.takeIf { it.isNotBlank() }?.let {
                    append("\nReason: $it")
                }
            },
            isFinal = true
        )
    }

    // ═══════════════════════════════════════════════════════════════════════
    // HELPER METHODS
    // ═══════════════════════════════════════════════════════════════════════

    private fun createPendingIntent(bookingId: String): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("booking_id", bookingId)
            putExtra("navigate_to", "booking_tracking")
        }

        return PendingIntent.getActivity(
            context,
            bookingId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun vibrate(pattern: LongArray) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                val vibrator = vibratorManager.defaultVibrator
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(pattern, -1)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Vibration error: ${e.message}")
        }
    }

    fun cancelAllNotifications() {
        notificationManager.cancel(NOTIFICATION_BOOKING_STATUS)
        notificationManager.cancel(NOTIFICATION_TRACKING_PROGRESS)
        Log.d(TAG, "🗑️ All notifications cancelled")
    }

    fun cancelNotification(notificationId: Int) {
        notificationManager.cancel(notificationId)
        Log.d(TAG, "🗑️ Notification $notificationId cancelled")
    }
}