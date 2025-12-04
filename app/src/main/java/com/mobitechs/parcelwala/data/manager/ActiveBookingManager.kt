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
 * Active Booking Manager
 * Singleton that tracks active bookings across the app
 * Used to show "Searching for rider" card on HomeScreen
 */
@Singleton
class ActiveBookingManager @Inject constructor() {

    private val _activeBooking = MutableStateFlow<ActiveBooking?>(null)
    val activeBooking: StateFlow<ActiveBooking?> = _activeBooking.asStateFlow()

    /**
     * Set active booking when booking is created and searching for rider
     */
    fun setActiveBooking(
        bookingId: String,
        pickupAddress: SavedAddress,
        dropAddress: SavedAddress,
        fareDetails: FareDetails,
        fare: Int,
        status: BookingStatus = BookingStatus.SEARCHING
    ) {
        _activeBooking.value = ActiveBooking(
            bookingId = bookingId,
            pickupAddress = pickupAddress,
            dropAddress = dropAddress,
            fareDetails = fareDetails,
            fare = fare,
            status = status,
            createdAt = System.currentTimeMillis()
        )
    }

    /**
     * Update booking status
     */
    fun updateStatus(status: BookingStatus) {
        _activeBooking.value = _activeBooking.value?.copy(status = status)
    }

    /**
     * Clear active booking (when completed, cancelled, or expired)
     */
    fun clearActiveBooking() {
        _activeBooking.value = null
    }

    /**
     * Check if there's an active booking
     */
    fun hasActiveBooking(): Boolean = _activeBooking.value != null

    /**
     * Check if booking is in searching state
     */
    fun isSearching(): Boolean = _activeBooking.value?.status == BookingStatus.SEARCHING
}

/**
 * Active Booking Data
 */
data class ActiveBooking(
    val bookingId: String,
    val pickupAddress: SavedAddress,
    val dropAddress: SavedAddress,
    val fareDetails: FareDetails,
    val fare: Int,
    val status: BookingStatus,
    val createdAt: Long
)

/**
 * Booking Status
 */
enum class BookingStatus {
    SEARCHING,          // Looking for rider
    RIDER_ASSIGNED,     // Rider found
    RIDER_EN_ROUTE,     // Rider coming to pickup
    PICKED_UP,          // Goods picked up
    IN_TRANSIT,         // On the way to drop
    DELIVERED,          // Delivered
    CANCELLED           // Cancelled
}