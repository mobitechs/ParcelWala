package com.mobitechs.parcelwala.data.manager

import android.util.Log
import com.google.gson.Gson
import com.mobitechs.parcelwala.data.local.PreferencesManager
import com.mobitechs.parcelwala.data.model.realtime.BookingStatusUpdate
import com.mobitechs.parcelwala.data.model.request.SavedAddress
import com.mobitechs.parcelwala.data.model.response.FareDetails
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "PW-ActiveBooking"

@Singleton
class ActiveBookingManager @Inject constructor(
    private val preferencesManager: PreferencesManager
) {

    companion object {
        const val SEARCH_TIMEOUT_MS = 3 * 60 * 1000L
        private const val MAX_BOOKING_AGE_MS = 6 * 60 * 60 * 1000L
    }

    private val gson = Gson()

    /** Writes JSON + SharedPreferences off the main thread. See persistBooking(). */
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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

    /**
     * PERFORMANCE — this used to run on whichever thread called it, which for
     * updateFromSignalR() is the main thread (RiderTrackingViewModel handles
     * updates in viewModelScope). Serialising an ActiveBooking is not cheap:
     * it embeds the entire lastSignalRUpdate payload, so every status change
     * did a reflective Gson walk over that whole object graph on the UI thread
     * — visible as a hitch at exactly the moments the screen was also animating
     * a state transition.
     *
     * The in-memory StateFlow is still updated synchronously, so readers never
     * see a lag; only the JSON encoding and the SharedPreferences write move
     * off the main thread.
     */
    private fun persistBooking(booking: ActiveBooking?) {
        if (booking == null) {
            preferencesManager.clearActiveBooking()
            return
        }
        ioScope.launch {
            try {
                preferencesManager.saveActiveBooking(gson.toJson(booking))
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to persist booking: ${e.message}", e)
            }
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
            // FIX — the customer saw ₹126.57 while the driver saw ₹130.
            //
            // totalFare is the raw computed amount; roundedFare is what is
            // actually charged and what the driver app displays. Preferring
            // totalFare meant the two apps disagreed about the price of the same
            // trip, which is the kind of thing that turns into a refund request.
            // roundedFare wins wherever it exists.
            fare = update.roundedFare ?: update.totalFare ?: current.fare,
            paymentMethod = update.paymentMethod ?: current.paymentMethod,
            lastSignalRUpdate = update
        )
        updateAndPersist(updated)
        Log.d(TAG, "🔄 Booking updated from SignalR | status=$newStatus | fare=${updated.fare}")
    }

    /**
     * Persist the moment the driver reached the pickup point.
     *
     * WHY THIS EXISTS
     * The waiting timer used to count up from the instant the ARRIVED event was
     * RECEIVED. A 40-second socket drop silently lost 40 seconds of billable
     * waiting, and a cold start reset the customer's timer to zero while the
     * server kept counting. Anchoring to a persisted wall-clock timestamp makes
     * the displayed time survive backgrounding, doze and process death — and
     * always agree with what the server will bill.
     *
     * WRITE-ONCE. A later ARRIVED (a reconnect re-pushing the same status) must
     * never restart the clock, so an existing anchor always wins.
     */
    fun markArrivedAtPickup(epochMs: Long) {
        val current = _activeBooking.value ?: return
        if (current.arrivedAtPickupMs != null) return
        updateAndPersist(current.copy(arrivedAtPickupMs = epochMs))
        Log.d(TAG, "⏱️ Arrival anchored at $epochMs")
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
    /**
     * Wall-clock time the driver reached pickup. The waiting timer is computed
     * from this rather than an incrementing counter, so it stays correct across
     * reconnects and process death. Null until the driver arrives.
     */
    val arrivedAtPickupMs: Long? = null,
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