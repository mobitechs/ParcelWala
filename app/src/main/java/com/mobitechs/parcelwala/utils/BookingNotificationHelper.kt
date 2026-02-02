// utils/BookingNotificationHelper.kt
// ✅ ENHANCED: With progress bar for distance/ETA tracking
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
 * BOOKING NOTIFICATION HELPER - ENHANCED
 * ════════════════════════════════════════════════════════════════════════════
 *
 * Features:
 * ✅ Driver Assigned - with name, vehicle, OTP, ETA
 * ✅ Driver Arrived at Pickup - with OTP reminder
 * ✅ Parcel Picked Up - with destination
 * ✅ Driver Arrived at Delivery
 * ✅ Delivery Completed - with fare
 * ✅ Booking Cancelled - with reason
 * ✅ Progress Bar for ETA/Distance tracking
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
        const val CHANNEL_DRIVER_TRACKING = "driver_tracking_channel"

        // Notification IDs
        const val NOTIFICATION_DRIVER_ASSIGNED = 2001
        const val NOTIFICATION_DRIVER_ARRIVED = 2002
        const val NOTIFICATION_PICKED_UP = 2003
        const val NOTIFICATION_ARRIVED_DELIVERY = 2004
        const val NOTIFICATION_DELIVERED = 2005
        const val NOTIFICATION_CANCELLED = 2006
        const val NOTIFICATION_TRACKING_PROGRESS = 2007

        // Max distance for progress calculation (5 km = 5000 meters)
        private const val MAX_DISTANCE_METERS = 5000.0
    }

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // High priority channel for status updates
            val statusChannel = NotificationChannel(
                CHANNEL_BOOKING_STATUS,
                "Booking Status",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Important updates about your booking"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 250, 250)
                enableLights(true)
                lightColor = 0xFFFF6B35.toInt() // Orange
            }

            // Lower priority for tracking updates (with progress bar)
            val trackingChannel = NotificationChannel(
                CHANNEL_DRIVER_TRACKING,
                "Driver Tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Real-time driver location and ETA updates"
                setShowBadge(false)
                enableVibration(false)
            }

            notificationManager.createNotificationChannel(statusChannel)
            notificationManager.createNotificationChannel(trackingChannel)
            Log.d(TAG, "✅ Notification channels created")
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 1️⃣ DRIVER ASSIGNED NOTIFICATION
    // ═══════════════════════════════════════════════════════════════════════

    fun showDriverAssignedNotification(
        bookingId: String,
        driverName: String?,
        vehicleNumber: String?,
        vehicleType: String?,
        otp: String?,
        etaMinutes: Int?
    ) {
        Log.d(TAG, "📱 Showing Driver Assigned notification")
        Log.d(TAG, "  Driver: $driverName, Vehicle: $vehicleNumber, OTP: $otp, ETA: $etaMinutes")

        val title = "🚗 Driver Assigned!"
        val body = buildString {
            append(driverName ?: "Your driver")
            append(" is on the way")
            etaMinutes?.let { if (it > 0) append("\n⏱️ Arriving in ~$it min") }
            vehicleType?.let { append("\n🚚 $it") }
            vehicleNumber?.let { append(" • $it") }
            otp?.let { append("\n\n🔐 Pickup OTP: $it") }
        }

        showNotification(
            notificationId = NOTIFICATION_DRIVER_ASSIGNED,
            channelId = CHANNEL_BOOKING_STATUS,
            title = title,
            body = body,
            bookingId = bookingId,
            priority = NotificationCompat.PRIORITY_HIGH,
            autoCancel = false, // Keep showing until pickup
            ongoing = true      // Can't be swiped away
        )

        vibrate(longArrayOf(0, 300, 200, 300))
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 2️⃣ DRIVER TRACKING WITH PROGRESS BAR
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Shows/updates the driver tracking notification with progress bar
     * @param distanceMeters Current distance from driver to pickup (in meters)
     * @param etaMinutes Estimated time of arrival (in minutes)
     * @param maxDistanceMeters The starting distance (for progress calculation)
     */
    fun showDriverTrackingProgress(
        bookingId: String,
        driverName: String?,
        distanceMeters: Double,
        etaMinutes: Int?,
        maxDistanceMeters: Double = MAX_DISTANCE_METERS
    ) {
        // Calculate progress (0 = far away, 100 = arrived)
        val progress = ((1 - (distanceMeters / maxDistanceMeters).coerceIn(0.0, 1.0)) * 100).toInt()
        val distanceKm = distanceMeters / 1000.0

        val title = "🚗 ${driverName ?: "Driver"} is on the way"
        val body = buildString {
            append("📍 ${String.format("%.1f", distanceKm)} km away")
            etaMinutes?.let { if (it > 0) append(" • ~$it min") }
        }

        val intent = createPendingIntent(bookingId)

        val builder = NotificationCompat.Builder(context, CHANNEL_DRIVER_TRACKING)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setProgress(100, progress, false) // ✅ Progress bar
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(intent)
            .setOngoing(true)   // Can't be swiped
            .setAutoCancel(false)
            .setSilent(true)    // No sound for updates
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setOnlyAlertOnce(true) // Don't make sound on every update

        notificationManager.notify(NOTIFICATION_TRACKING_PROGRESS, builder.build())
        Log.d(TAG, "📍 Tracking notification: ${String.format("%.1f", distanceKm)}km, ETA: ${etaMinutes}min, Progress: $progress%")
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 3️⃣ DRIVER ARRIVED NOTIFICATION
    // ═══════════════════════════════════════════════════════════════════════

    fun showDriverArrivedNotification(
        bookingId: String,
        driverName: String?,
        otp: String?
    ) {
        Log.d(TAG, "📱 Showing Driver Arrived notification")

        // Cancel the tracking progress notification
        cancelNotification(NOTIFICATION_TRACKING_PROGRESS)

        val title = "📍 Driver Has Arrived!"
        val body = buildString {
            append(driverName ?: "Your driver")
            append(" is at your pickup location")
            otp?.let { append("\n\n🔐 Share OTP: $it") }
        }

        showNotification(
            notificationId = NOTIFICATION_DRIVER_ARRIVED,
            channelId = CHANNEL_BOOKING_STATUS,
            title = title,
            body = body,
            bookingId = bookingId,
            priority = NotificationCompat.PRIORITY_HIGH,
            autoCancel = true
        )

        // Strong vibration for arrival
        vibrate(longArrayOf(0, 500, 200, 500))
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 4️⃣ PARCEL PICKED UP NOTIFICATION
    // ═══════════════════════════════════════════════════════════════════════

    fun showParcelPickedUpNotification(
        bookingId: String,
        dropAddress: String?
    ) {
        Log.d(TAG, "📱 Showing Parcel Picked Up notification")

        // Cancel driver assigned notification
        cancelNotification(NOTIFICATION_DRIVER_ASSIGNED)

        val title = "📦 Parcel Picked Up!"
        val body = buildString {
            append("Your parcel is on the way to delivery")
            dropAddress?.let {
                val shortAddress = if (it.length > 50) it.take(50) + "..." else it
                append("\n📍 To: $shortAddress")
            }
        }

        showNotification(
            notificationId = NOTIFICATION_PICKED_UP,
            channelId = CHANNEL_BOOKING_STATUS,
            title = title,
            body = body,
            bookingId = bookingId,
            priority = NotificationCompat.PRIORITY_DEFAULT,
            autoCancel = true
        )

        vibrate(longArrayOf(0, 200, 100, 200))
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 5️⃣ ARRIVED AT DELIVERY NOTIFICATION
    // ═══════════════════════════════════════════════════════════════════════

    fun showArrivedAtDeliveryNotification(
        bookingId: String,
        deliveryOtp: String?
    ) {
        Log.d(TAG, "📱 Showing Arrived at Delivery notification")

        val title = "🏠 Arriving at Delivery!"
        val body = buildString {
            append("Driver has arrived at delivery location")
            deliveryOtp?.let { append("\n🔐 Delivery OTP: $it") }
        }

        showNotification(
            notificationId = NOTIFICATION_ARRIVED_DELIVERY,
            channelId = CHANNEL_BOOKING_STATUS,
            title = title,
            body = body,
            bookingId = bookingId,
            priority = NotificationCompat.PRIORITY_HIGH,
            autoCancel = true
        )

        vibrate(longArrayOf(0, 300, 200, 300))
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 6️⃣ DELIVERY COMPLETED NOTIFICATION
    // ═══════════════════════════════════════════════════════════════════════

    fun showDeliveryCompletedNotification(
        bookingId: String,
        fare: Int?
    ) {
        Log.d(TAG, "📱 Showing Delivery Completed notification")

        // Cancel all ongoing notifications
        cancelNotification(NOTIFICATION_DRIVER_ASSIGNED)
        cancelNotification(NOTIFICATION_TRACKING_PROGRESS)
        cancelNotification(NOTIFICATION_PICKED_UP)

        val title = "✅ Delivery Completed!"
        val body = buildString {
            append("Your parcel has been delivered successfully!")
            fare?.let { append("\n💰 Total: ₹$it") }
            append("\n\n⭐ Rate your experience")
        }

        showNotification(
            notificationId = NOTIFICATION_DELIVERED,
            channelId = CHANNEL_BOOKING_STATUS,
            title = title,
            body = body,
            bookingId = bookingId,
            priority = NotificationCompat.PRIORITY_HIGH,
            autoCancel = true
        )

        // Success vibration pattern
        vibrate(longArrayOf(0, 100, 100, 100, 100, 300))
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 7️⃣ BOOKING CANCELLED NOTIFICATION
    // ═══════════════════════════════════════════════════════════════════════

    fun showBookingCancelledNotification(
        bookingId: String,
        reason: String?,
        cancelledBy: String?
    ) {
        Log.d(TAG, "📱 Showing Booking Cancelled notification")
        Log.d(TAG, "  Reason: $reason, CancelledBy: $cancelledBy")

        // Cancel all ongoing notifications
        cancelAllNotifications()

        val title = "❌ Booking Cancelled"
        val body = buildString {
            when (cancelledBy?.lowercase()) {
                "driver" -> append("Driver cancelled the booking")
                "system" -> append("Booking was cancelled by system")
                "customer" -> append("You cancelled the booking")
                else -> append("Booking has been cancelled")
            }
            reason?.takeIf { it.isNotBlank() }?.let {
                append("\nReason: $it")
            }
        }

        showNotification(
            notificationId = NOTIFICATION_CANCELLED,
            channelId = CHANNEL_BOOKING_STATUS,
            title = title,
            body = body,
            bookingId = bookingId,
            priority = NotificationCompat.PRIORITY_HIGH,
            autoCancel = true
        )

        vibrate(longArrayOf(0, 500))
    }

    // ═══════════════════════════════════════════════════════════════════════
    // HELPER METHODS
    // ═══════════════════════════════════════════════════════════════════════

    private fun showNotification(
        notificationId: Int,
        channelId: String,
        title: String,
        body: String,
        bookingId: String,
        priority: Int = NotificationCompat.PRIORITY_DEFAULT,
        autoCancel: Boolean = true,
        ongoing: Boolean = false
    ) {
        val pendingIntent = createPendingIntent(bookingId)

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body.lines().firstOrNull() ?: body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(priority)
            .setContentIntent(pendingIntent)
            .setAutoCancel(autoCancel)
            .setOngoing(ongoing)
            .setCategory(NotificationCompat.CATEGORY_STATUS)

        if (priority >= NotificationCompat.PRIORITY_HIGH) {
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            builder.setSound(soundUri)
        } else {
            builder.setSilent(true)
        }

        notificationManager.notify(notificationId, builder.build())
        Log.d(TAG, "✅ Notification shown: $title")
    }

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

    /**
     * Cancel all booking-related notifications
     */
    fun cancelAllNotifications() {
        notificationManager.cancel(NOTIFICATION_DRIVER_ASSIGNED)
        notificationManager.cancel(NOTIFICATION_DRIVER_ARRIVED)
        notificationManager.cancel(NOTIFICATION_PICKED_UP)
        notificationManager.cancel(NOTIFICATION_ARRIVED_DELIVERY)
        notificationManager.cancel(NOTIFICATION_DELIVERED)
        notificationManager.cancel(NOTIFICATION_CANCELLED)
        notificationManager.cancel(NOTIFICATION_TRACKING_PROGRESS)
        Log.d(TAG, "🗑️ All notifications cancelled")
    }

    /**
     * Cancel a specific notification
     */
    fun cancelNotification(notificationId: Int) {
        notificationManager.cancel(notificationId)
        Log.d(TAG, "🗑️ Notification $notificationId cancelled")
    }
}