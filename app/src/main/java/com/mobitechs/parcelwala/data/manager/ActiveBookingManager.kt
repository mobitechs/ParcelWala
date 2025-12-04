// data/manager/ActiveBookingManager.kt
package com.mobitechs.parcelwala.data.manager

import com.mobitechs.parcelwala.data.model.request.SavedAddress
import com.mobitechs.parcelwala.data.model.response.FareDetails
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ActiveBookingManager - Singleton to manage active booking state across the app
 * Tracks ongoing bookings with search timeout functionality (3 minutes)
 */
@Singleton
class ActiveBookingManager @Inject constructor() {

    companion object {
        // Search timeout duration: 3 minutes in milliseconds
        const val SEARCH_TIMEOUT_MS = 3 * 60 * 1000L // 180,000ms = 3 minutes
    }

    private val _activeBooking = MutableStateFlow<ActiveBooking?>(null)
    val activeBooking: StateFlow<ActiveBooking?> = _activeBooking.asStateFlow()

    /**
     * Set active booking when booking is confirmed
     */
    fun setActiveBooking(
        bookingId: String,
        pickupAddress: SavedAddress,
        dropAddress: SavedAddress,
        fareDetails: FareDetails,
        fare: Int,
        status: BookingStatus = BookingStatus.SEARCHING
    ) {
        val currentTime = System.currentTimeMillis()
        _activeBooking.value = ActiveBooking(
            bookingId = bookingId,
            pickupAddress = pickupAddress,
            dropAddress = dropAddress,
            fareDetails = fareDetails,
            fare = fare,
            status = status,
            createdAt = currentTime,
            searchStartTime = currentTime,  // Track when search started
            searchAttempts = 1              // First attempt
        )
    }

    /**
     * Update booking status
     */
    fun updateStatus(status: BookingStatus) {
        _activeBooking.value = _activeBooking.value?.copy(status = status)
    }

    /**
     * Retry search - resets the search timer and increments attempt count
     */
    fun retrySearch() {
        _activeBooking.value = _activeBooking.value?.copy(
            searchStartTime = System.currentTimeMillis(),
            searchAttempts = (_activeBooking.value?.searchAttempts ?: 0) + 1,
            status = BookingStatus.SEARCHING
        )
    }

    /**
     * Clear active booking (when completed or cancelled)
     */
    fun clearActiveBooking() {
        _activeBooking.value = null
    }

    /**
     * Check if there's an active booking
     */
    fun hasActiveBooking(): Boolean = _activeBooking.value != null

    /**
     * Check if currently searching for rider
     */
    fun isSearching(): Boolean = _activeBooking.value?.status == BookingStatus.SEARCHING

    /**
     * Get remaining search time in milliseconds
     */
    fun getRemainingSearchTime(): Long {
        val booking = _activeBooking.value ?: return 0L
        val elapsed = System.currentTimeMillis() - booking.searchStartTime
        return maxOf(0L, SEARCH_TIMEOUT_MS - elapsed)
    }

    /**
     * Check if search has timed out
     */
    fun isSearchTimedOut(): Boolean {
        val booking = _activeBooking.value ?: return false
        if (booking.status != BookingStatus.SEARCHING) return false
        return getRemainingSearchTime() <= 0
    }
}

/**
 * Active Booking Data Class
 * Represents an ongoing booking with all details
 */
data class ActiveBooking(
    val bookingId: String,
    val pickupAddress: SavedAddress,
    val dropAddress: SavedAddress,
    val fareDetails: FareDetails,
    val fare: Int,
    val status: BookingStatus,
    val createdAt: Long,
    val searchStartTime: Long = createdAt,  // When the current search attempt started
    val searchAttempts: Int = 1             // Number of search attempts
)

/**
 * Booking Status Enum
 * Tracks the lifecycle of a booking
 */
enum class BookingStatus {
    SEARCHING,          // Looking for a driver
    SEARCH_TIMEOUT,     // Search timed out (3 minutes)
    RIDER_ASSIGNED,     // Driver accepted
    RIDER_EN_ROUTE,     // Driver on the way to pickup
    PICKED_UP,          // Goods collected
    IN_TRANSIT,         // On the way to drop
    DELIVERED,          // Successfully delivered
    CANCELLED           // Booking cancelled
}