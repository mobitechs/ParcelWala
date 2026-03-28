package com.mobitechs.parcelwala.data.manager

import android.util.Log
import com.google.gson.Gson
import com.mobitechs.parcelwala.data.local.PreferencesManager
import com.mobitechs.parcelwala.data.model.realtime.BookingStatusUpdate
import com.mobitechs.parcelwala.data.model.request.SavedAddress
import com.mobitechs.parcelwala.data.model.response.FareDetails
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "ActiveBookingManager"

@Singleton
class ActiveBookingManager @Inject constructor(
    private val preferencesManager: PreferencesManager
) {

    companion object {
        const val SEARCH_TIMEOUT_MS = 3 * 60 * 1000L
        private const val MAX_BOOKING_AGE_MS = 6 * 60 * 60 * 1000L
    }

    private val gson = Gson()

    private val _activeBooking = MutableStateFlow<ActiveBooking?>(null)
    val activeBooking: StateFlow<ActiveBooking?> = _activeBooking.asStateFlow()

    // ═══════════════════════════════════════════════════════════════════════
    // INIT
    // ═══════════════════════════════════════════════════════════════════════

    init {
        restoreActiveBooking()
    }

    private fun restoreActiveBooking() {
        try {
            val json = preferencesManager.getActiveBooking() ?: run {
                Log.d(TAG, "📦 No stored booking to restore")
                return
            }

            val booking = gson.fromJson(json, ActiveBooking::class.java) ?: run {
                Log.w(TAG, "⚠️ Failed to parse stored booking, clearing")
                preferencesManager.clearActiveBooking()
                return
            }

            if (booking.status == BookingStatus.DELIVERED || booking.status == BookingStatus.CANCELLED) {
                Log.d(TAG, "🗑️ Stored booking is ${booking.status}, clearing")
                preferencesManager.clearActiveBooking()
                return
            }

            val age = System.currentTimeMillis() - booking.createdAt
            if (age > MAX_BOOKING_AGE_MS) {
                Log.d(TAG, "🗑️ Stored booking is ${age / 3600000}h old, clearing")
                preferencesManager.clearActiveBooking()
                return
            }

            _activeBooking.value = booking
            Log.d(TAG, "✅ RESTORED booking: #${booking.bookingId} | Status: ${booking.status} | Age: ${age / 60000}min")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to restore active booking: ${e.message}", e)
            preferencesManager.clearActiveBooking()
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PERSIST
    // ═══════════════════════════════════════════════════════════════════════

    private fun persistBooking(booking: ActiveBooking?) {
        if (booking != null) {
            try {
                preferencesManager.saveActiveBooking(gson.toJson(booking))
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to persist booking: ${e.message}", e)
            }
        } else {
            preferencesManager.clearActiveBooking()
        }
    }

    private fun updateAndPersist(booking: ActiveBooking?) {
        _activeBooking.value = booking
        persistBooking(booking)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PUBLIC METHODS
    // ═══════════════════════════════════════════════════════════════════════

    fun setActiveBooking(
        bookingId: String,
        pickupAddress: SavedAddress,
        dropAddress: SavedAddress,
        fareDetails: FareDetails,
        fare: Double,
        status: BookingStatus = BookingStatus.SEARCHING,
        paymentMethod: String = "cash"
    ) {
        val currentTime = System.currentTimeMillis()
        val booking = ActiveBooking(
            bookingId = bookingId,
            pickupAddress = pickupAddress,
            dropAddress = dropAddress,
            fareDetails = fareDetails,
            fare = fare,
            status = status,
            createdAt = currentTime,
            searchStartTime = currentTime,
            searchAttempts = 1,
            paymentMethod = paymentMethod,
            waitingChargePerMin = fareDetails.waitingChargePerMin,
            freeWaitingTimeMins = fareDetails.resolvedFreeWaitingMins
        )
        updateAndPersist(booking)
        Log.d(TAG, "📦 Active booking SET: #$bookingId | waitCharge/min=₹${booking.waitingChargePerMin} | freeWait=${booking.freeWaitingTimeMins}min")
    }

    /**
     * Full state replace from SignalR BookingStatusUpdate.
     * Every field from the server overwrites the local copy — no stale data.
     */
    fun updateFromSignalR(update: BookingStatusUpdate) {
        val current = _activeBooking.value ?: return
        val newStatus = mapServerStatus(update.status)

        val updated = current.copy(
            status = newStatus,
            fare = update.totalFare ?: update.roundedFare ?: current.fare,
            paymentMethod = update.paymentMethod ?: current.paymentMethod,
            lastSignalRUpdate = update
        )
        updateAndPersist(updated)
        Log.d(TAG, "🔄 Booking updated from SignalR | status=$newStatus | fare=${updated.fare}")
    }

    /**
     * Lightweight status-only update — kept for edge cases (driver cancel retry, etc.)
     */
    fun updateStatus(status: BookingStatus) {
        val updated = _activeBooking.value?.copy(status = status) ?: return
        updateAndPersist(updated)
        Log.d(TAG, "📊 Status → $status")
    }

    fun retrySearch() {
        val updated = _activeBooking.value?.copy(
            searchStartTime = System.currentTimeMillis(),
            searchAttempts = (_activeBooking.value?.searchAttempts ?: 0) + 1,
            status = BookingStatus.SEARCHING,
            lastSignalRUpdate = null
        )
        updateAndPersist(updated)
        Log.d(TAG, "🔄 Retry search: attempt ${updated?.searchAttempts}")
    }

    fun clearActiveBooking() {
        updateAndPersist(null)
        Log.d(TAG, "🗑️ Active booking CLEARED")
    }

    fun hasActiveBooking(): Boolean = _activeBooking.value != null

    fun isSearching(): Boolean = _activeBooking.value?.status == BookingStatus.SEARCHING

    fun getRemainingSearchTime(): Long {
        val booking = _activeBooking.value ?: return 0L
        val elapsed = System.currentTimeMillis() - booking.searchStartTime
        return maxOf(0L, SEARCH_TIMEOUT_MS - elapsed)
    }

    fun isSearchTimedOut(): Boolean {
        val booking = _activeBooking.value ?: return false
        if (booking.status != BookingStatus.SEARCHING) return false
        return getRemainingSearchTime() <= 0
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ═══════════════════════════════════════════════════════════════════════

    private fun mapServerStatus(serverStatus: String?): BookingStatus {
        return when (serverStatus?.lowercase()?.trim()) {
            "searching"                                      -> BookingStatus.SEARCHING
            "assigned"                                       -> BookingStatus.RIDER_ASSIGNED
            "heading_to_pickup", "pickup_started",
            "arriving", "driver_arriving", "arrived_pickup" -> BookingStatus.RIDER_EN_ROUTE
            "pickup_completed", "picked_up"                 -> BookingStatus.PICKED_UP
            "heading_to_drop", "in_transit",
            "in_progress", "in progress"                    -> BookingStatus.IN_TRANSIT
            "arrived_delivery"                              -> BookingStatus.ARRIVED_DELIVERY
            "payment_success"                               -> BookingStatus.PAYMENT_SUCCESS
            "delivery_completed", "completed"               -> BookingStatus.DELIVERED
            "cancelled"                                     -> BookingStatus.CANCELLED
            "no_rider", "no_driver"                         -> BookingStatus.SEARCH_TIMEOUT
            else                                            -> _activeBooking.value?.status ?: BookingStatus.SEARCHING
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// DATA CLASSES
// ═══════════════════════════════════════════════════════════════════════════

data class ActiveBooking(
    val bookingId: String,
    val pickupAddress: SavedAddress,
    val dropAddress: SavedAddress,
    val fareDetails: FareDetails,
    val fare: Double,
    val status: BookingStatus,
    val createdAt: Long,
    val searchStartTime: Long = createdAt,
    val searchAttempts: Int = 1,
    val paymentMethod: String = "cash",
    val waitingChargePerMin: Double = FareDetails.DEFAULT_CHARGE_PER_MIN,
    val freeWaitingTimeMins: Int = FareDetails.DEFAULT_FREE_WAITING_MINS,
    // Full latest server state — survives app restarts via SharedPreferences
    val lastSignalRUpdate: BookingStatusUpdate? = null
) {
    val freeWaitingSeconds: Int get() = freeWaitingTimeMins * 60
}

enum class BookingStatus {
    SEARCHING,
    SEARCH_TIMEOUT,
    RIDER_ASSIGNED,
    RIDER_EN_ROUTE,
    PICKED_UP,
    IN_TRANSIT,
    ARRIVED_DELIVERY,
    PAYMENT_SUCCESS,
    DELIVERED,
    CANCELLED
}