// data/mock/MockRealTimeData.kt
package com.mobitechs.parcelwala.data.mock

import com.mobitechs.parcelwala.data.model.realtime.BookingStatusType
import com.mobitechs.parcelwala.data.model.realtime.BookingStatusUpdate
import com.mobitechs.parcelwala.data.model.realtime.RiderInfo
import com.mobitechs.parcelwala.data.model.realtime.RiderLocationUpdate
import kotlin.random.Random

/**
 * ════════════════════════════════════════════════════════════════════════════
 * MOCK REAL-TIME DATA
 * ════════════════════════════════════════════════════════════════════════════
 *
 * ⚠️ DELETE THIS ENTIRE FILE WHEN BACKEND IS READY ⚠️
 *
 * This file simulates backend SignalR responses for testing.
 * When real API is ready:
 * 1. Set Constants.USE_MOCK_DATA = false
 * 2. Delete this file
 *
 * ════════════════════════════════════════════════════════════════════════════
 */
object MockRealTimeData {

    // ═══════════════════════════════════════════════════════════════════════
    // MOCK RIDERS
    // ═══════════════════════════════════════════════════════════════════════

    private val mockRiders = listOf(
        RiderInfo(
            riderId = 1001,
            name = "Ramesh Kumar",
            phone = "9876543210",
            photo = "https://randomuser.me/api/portraits/men/32.jpg",
            vehicleNumber = "MH12AB1234",
            vehicleType = "Tata Ace",
            vehicleModel = "Tata Ace Gold",
            rating = 4.8,
            totalTrips = 342,
            etaMinutes = 8
        ),
        RiderInfo(
            riderId = 1002,
            name = "Suresh Yadav",
            phone = "9876543211",
            photo = "https://randomuser.me/api/portraits/men/45.jpg",
            vehicleNumber = "MH14CD5678",
            vehicleType = "Mahindra Bolero",
            vehicleModel = "Bolero Pickup",
            rating = 4.6,
            totalTrips = 215,
            etaMinutes = 12
        ),
        RiderInfo(
            riderId = 1003,
            name = "Vijay Sharma",
            phone = "9876543212",
            photo = "https://randomuser.me/api/portraits/men/67.jpg",
            vehicleNumber = "MH04EF9012",
            vehicleType = "Tata 407",
            vehicleModel = "Tata 407 Turbo",
            rating = 4.9,
            totalTrips = 567,
            etaMinutes = 5
        ),
        RiderInfo(
            riderId = 1004,
            name = "Prakash Patil",
            phone = "9876543213",
            photo = "https://randomuser.me/api/portraits/men/22.jpg",
            vehicleNumber = "MH02GH3456",
            vehicleType = "Ashok Leyland Dost",
            vehicleModel = "Dost Plus",
            rating = 4.5,
            totalTrips = 128,
            etaMinutes = 10
        )
    )

    // ═══════════════════════════════════════════════════════════════════════
    // PUBLIC METHODS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Get a random rider with random ETA
     */
    fun getRandomRider(): RiderInfo {
        return mockRiders.random().copy(
            etaMinutes = Random.nextInt(5, 15)
        )
    }

    /**
     * Generate 4-digit OTP
     */
    fun generateOtp(): String {
        return (1000..9999).random().toString()
    }

    /**
     * Create SEARCHING status update
     */
    fun createSearchingUpdate(bookingId: String): BookingStatusUpdate {
        return BookingStatusUpdate(
            bookingId = bookingId,
            status = BookingStatusType.SEARCHING.value,
            message = "Looking for nearby riders...",
            updatedAt = System.currentTimeMillis().toString()
        )
    }

    /**
     * Create RIDER_ASSIGNED status update
     */
    fun createRiderAssignedUpdate(bookingId: String): BookingStatusUpdate {
        val rider = getRandomRider()
        val otp = generateOtp()

        return BookingStatusUpdate(
            bookingId = bookingId,
            status = BookingStatusType.RIDER_ASSIGNED.value,
            message = "${rider.name} is coming to pickup your parcel",
            rider = rider,
            otp = otp,
            updatedAt = System.currentTimeMillis().toString()
        )
    }

    /**
     * Create RIDER_ENROUTE status update
     */
    fun createRiderEnrouteUpdate(bookingId: String, rider: RiderInfo): BookingStatusUpdate {
        return BookingStatusUpdate(
            bookingId = bookingId,
            status = BookingStatusType.RIDER_ENROUTE.value,
            message = "${rider.name} is on the way to pickup location",
            rider = rider,
            updatedAt = System.currentTimeMillis().toString()
        )
    }

    /**
     * Create ARRIVED status update
     */
    fun createArrivedUpdate(bookingId: String, rider: RiderInfo): BookingStatusUpdate {
        return BookingStatusUpdate(
            bookingId = bookingId,
            status = BookingStatusType.ARRIVED.value,
            message = "${rider.name} has arrived at pickup location",
            rider = rider,
            updatedAt = System.currentTimeMillis().toString()
        )
    }

    /**
     * Create IN_TRANSIT status update
     */
    fun createInTransitUpdate(bookingId: String, rider: RiderInfo): BookingStatusUpdate {
        return BookingStatusUpdate(
            bookingId = bookingId,
            status = BookingStatusType.IN_TRANSIT.value,
            message = "Parcel picked up! Heading to destination",
            rider = rider,
            updatedAt = System.currentTimeMillis().toString()
        )
    }

    /**
     * Create DELIVERED status update
     */
    fun createDeliveredUpdate(bookingId: String, rider: RiderInfo): BookingStatusUpdate {
        return BookingStatusUpdate(
            bookingId = bookingId,
            status = BookingStatusType.DELIVERED.value,
            message = "Parcel delivered successfully!",
            rider = rider,
            updatedAt = System.currentTimeMillis().toString()
        )
    }

    /**
     * Create NO_RIDER status update
     */
    fun createNoRiderUpdate(bookingId: String): BookingStatusUpdate {
        return BookingStatusUpdate(
            bookingId = bookingId,
            status = BookingStatusType.NO_RIDER.value,
            message = "No riders available in your area. Please try again.",
            updatedAt = System.currentTimeMillis().toString()
        )
    }

    /**
     * Create CANCELLED status update
     */
    fun createCancelledUpdate(bookingId: String, reason: String): BookingStatusUpdate {
        return BookingStatusUpdate(
            bookingId = bookingId,
            status = BookingStatusType.CANCELLED.value,
            message = reason,
            updatedAt = System.currentTimeMillis().toString()
        )
    }

    /**
     * Generate location update for rider moving towards destination
     * @param progress 0.0 to 1.0 (0 = start, 1 = arrived)
     */
    fun generateLocationUpdate(
        bookingId: String,
        rider: RiderInfo,
        startLat: Double,
        startLng: Double,
        endLat: Double,
        endLng: Double,
        progress: Float
    ): RiderLocationUpdate {
        // Interpolate position
        val currentLat = startLat + (endLat - startLat) * progress
        val currentLng = startLng + (endLng - startLng) * progress

        // Calculate remaining ETA based on progress
        val totalEta = rider.etaMinutes
        val remainingEta = (totalEta * (1 - progress)).toInt().coerceAtLeast(1)

        // Calculate remaining distance (approximate)
        val totalDistanceKm = calculateDistance(startLat, startLng, endLat, endLng)
        val remainingDistanceKm = totalDistanceKm * (1 - progress)

        return RiderLocationUpdate(
            bookingId = bookingId,
            riderId = rider.riderId,
            latitude = currentLat,
            longitude = currentLng,
            heading = Random.nextFloat() * 360,
            speed = Random.nextFloat() * 30 + 15, // 15-45 km/h
            etaMinutes = remainingEta,
            distanceKm = remainingDistanceKm,
            updatedAt = System.currentTimeMillis().toString()
        )
    }

    /**
     * Calculate distance between two points (Haversine formula)
     */
    private fun calculateDistance(
        lat1: Double, lng1: Double,
        lat2: Double, lng2: Double
    ): Double {
        val r = 6371 // Earth's radius in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
                kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) *
                kotlin.math.sin(dLng / 2) * kotlin.math.sin(dLng / 2)
        val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
        return r * c
    }
}
