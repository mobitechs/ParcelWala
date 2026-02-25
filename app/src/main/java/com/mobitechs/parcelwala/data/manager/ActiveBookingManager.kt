// data/manager/ActiveBookingManager.kt
// ✅ UPDATED: Auto-persists to SharedPreferences on every state change
// ✅ UPDATED: Restores active booking on app restart (crash recovery)
// ✅ UPDATED: Stale booking auto-cleanup (bookings older than 6 hours)
package com.mobitechs.parcelwala.data.manager

import android.util.Log
import com.google.gson.Gson
import com.mobitechs.parcelwala.data.local.PreferencesManager
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
        const val SEARCH_TIMEOUT_MS = 3 * 60 * 1000L // 3 minutes

        // ✅ Auto-expire stale bookings older than this (6 hours)
        private const val MAX_BOOKING_AGE_MS = 6 * 60 * 60 * 1000L
    }

    private val gson = Gson()

    private val _activeBooking = MutableStateFlow<ActiveBooking?>(null)
    val activeBooking: StateFlow<ActiveBooking?> = _activeBooking.asStateFlow()

    // ═══════════════════════════════════════════════════════════════════════
    // INIT — Restore persisted booking on app start
    // ═══════════════════════════════════════════════════════════════════════

    init {
        restoreActiveBooking()
    }

    /**
     * ✅ Restore active booking from SharedPreferences.
     * Called automatically when ActiveBookingManager is created (on app start).
     * Cleans up stale or terminal-state bookings.
     */
    private fun restoreActiveBooking() {
        try {
            val json = preferencesManager.getActiveBooking()
            if (json == null) {
                Log.d(TAG, "📦 No stored booking to restore")
                return
            }

            val booking = gson.fromJson(json, ActiveBooking::class.java)
            if (booking == null) {
                Log.w(TAG, "⚠️ Failed to parse stored booking, clearing")
                preferencesManager.clearActiveBooking()
                return
            }

            // ✅ Don't restore terminal-state bookings
            if (booking.status == BookingStatus.DELIVERED || booking.status == BookingStatus.CANCELLED) {
                Log.d(TAG, "🗑️ Stored booking is ${booking.status}, clearing")
                preferencesManager.clearActiveBooking()
                return
            }

            // ✅ Don't restore stale bookings (older than 6 hours)
            val age = System.currentTimeMillis() - booking.createdAt
            if (age > MAX_BOOKING_AGE_MS) {
                Log.d(TAG, "🗑️ Stored booking is ${age / 3600000}h old, clearing")
                preferencesManager.clearActiveBooking()
                return
            }

            // ✅ Restore the booking!
            _activeBooking.value = booking
            Log.d(TAG, "✅ RESTORED booking: #${booking.bookingId} | Status: ${booking.status} | Age: ${age / 60000}min")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to restore active booking: ${e.message}", e)
            preferencesManager.clearActiveBooking()
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PERSIST — Save to SharedPreferences on every change
    // ═══════════════════════════════════════════════════════════════════════

    private fun persistBooking(booking: ActiveBooking?) {
        if (booking != null) {
            try {
                val json = gson.toJson(booking)
                preferencesManager.saveActiveBooking(json)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to persist booking: ${e.message}", e)
            }
        } else {
            preferencesManager.clearActiveBooking()
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PUBLIC METHODS (same interface, now with auto-persist)
    // ═══════════════════════════════════════════════════════════════════════

    fun setActiveBooking(
        bookingId: String,
        pickupAddress: SavedAddress,
        dropAddress: SavedAddress,
        fareDetails: FareDetails,
        fare: Int,
        status: BookingStatus = BookingStatus.SEARCHING
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
            searchAttempts = 1
        )
        _activeBooking.value = booking
        persistBooking(booking)
        Log.d(TAG, "📦 Active booking SET: #$bookingId")
    }

    fun updateStatus(status: BookingStatus) {
        val updated = _activeBooking.value?.copy(status = status)
        _activeBooking.value = updated
        persistBooking(updated)
        Log.d(TAG, "📊 Status → $status")
    }

    fun retrySearch() {
        val updated = _activeBooking.value?.copy(
            searchStartTime = System.currentTimeMillis(),
            searchAttempts = (_activeBooking.value?.searchAttempts ?: 0) + 1,
            status = BookingStatus.SEARCHING
        )
        _activeBooking.value = updated
        persistBooking(updated)
        Log.d(TAG, "🔄 Retry search: attempt ${updated?.searchAttempts}")
    }

    fun clearActiveBooking() {
        _activeBooking.value = null
        preferencesManager.clearActiveBooking()
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
}

data class ActiveBooking(
    val bookingId: String,
    val pickupAddress: SavedAddress,
    val dropAddress: SavedAddress,
    val fareDetails: FareDetails,
    val fare: Int,
    val status: BookingStatus,
    val createdAt: Long,
    val searchStartTime: Long = createdAt,
    val searchAttempts: Int = 1,
    val paymentMethod: String = "cash",
)

enum class BookingStatus {
    SEARCHING,
    SEARCH_TIMEOUT,
    RIDER_ASSIGNED,
    RIDER_EN_ROUTE,
    PICKED_UP,
    IN_TRANSIT,
    DELIVERED,
    CANCELLED
}