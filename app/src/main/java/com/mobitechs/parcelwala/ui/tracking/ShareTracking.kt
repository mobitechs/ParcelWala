package com.mobitechs.parcelwala.ui.tracking

import android.content.Context
import android.content.Intent
import com.mobitechs.parcelwala.data.model.realtime.RiderInfo

/**
 * ════════════════════════════════════════════════════════════════════════════
 * SHARE TRACKING
 * ════════════════════════════════════════════════════════════════════════════
 *
 * The Share icon in the old TopAppBar was wired to `/* Share */` — an empty
 * lambda. Tapping it did nothing at all.
 *
 * This matters more for a parcel service than for a taxi: the person waiting at
 * the DROP end currently has zero visibility. They cannot see where the driver
 * is, they do not know when to come downstairs, and they call the sender, who
 * calls your support line. A share link moves that whole conversation out of
 * your support queue.
 */
object ShareTracking {

    /**
     * Base URL for the public tracking page.
     *
     * TODO(server): this page does not exist yet. Until it does, the share text
     * still carries the driver, vehicle and OTP, which is genuinely useful on
     * its own — but the link is the part that removes the phone calls, so it is
     * worth building.
     */
    private const val TRACK_BASE_URL = "https://parcelwala.in/track"

    fun linkFor(bookingId: String): String = "$TRACK_BASE_URL/$bookingId"

    /**
     * Compose the share text.
     *
     * Deliberately includes the delivery OTP. The receiver is the person who
     * has to read it out, so making them wait for the sender to relay it is
     * pure friction — and the sender is going to text it to them anyway, less
     * securely and with more typos.
     */
    fun buildMessage(
        bookingId: String,
        rider: RiderInfo?,
        etaMinutes: Int?,
        deliveryOtp: String?
    ): String = buildString {
        append("Track your ParcelWala delivery")
        rider?.let {
            append("\n\nDriver: ").append(it.riderName)
            if (it.vehicleNumber.isNotBlank()) append(" · ").append(it.vehicleNumber)
        }
        etaMinutes?.takeIf { it > 0 }?.let { append("\nArriving in about ").append(it).append(" min") }
        deliveryOtp?.takeIf { it.isNotBlank() }?.let {
            append("\nDelivery OTP: ").append(it)
        }
        append("\n\n").append(linkFor(bookingId))
    }

    fun share(
        context: Context,
        bookingId: String,
        rider: RiderInfo?,
        etaMinutes: Int?,
        deliveryOtp: String?
    ) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "ParcelWala delivery tracking")
            putExtra(
                Intent.EXTRA_TEXT,
                buildMessage(bookingId, rider, etaMinutes, deliveryOtp)
            )
        }
        runCatching {
            context.startActivity(Intent.createChooser(intent, "Share tracking"))
        }
    }
}
