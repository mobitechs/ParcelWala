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

//    private val mockRiders = listOf(
//        RiderInfo(
//            riderId = "1001",
//            riderName = "Ramesh Kumar",
//            riderPhone = "9876543210",
//            vehicleNumber = "MH12AB1234",
//            vehicleType = "Tata Ace",
//            rating = 4.8,
//            totalTrips = 342,
//            currentLatitude = 19.0760,
//            currentLongitude = 72.8777,
//            etaMinutes = 8
//        ),
//        RiderInfo(
//            riderId = "1002",
//            riderName = "Suresh Yadav",
//            riderPhone = "9876543211",
//            vehicleNumber = "MH14CD5678",
//            vehicleType = "Mahindra Bolero",
//            rating = 4.6,
//            totalTrips = 215,
//            currentLatitude = 19.2183,
//            currentLongitude = 72.9781,
//            etaMinutes = 12
//        ),
//        RiderInfo(
//            riderId = "1003",
//            riderName = "Vijay Sharma",
//            riderPhone = "9876543212",
//            vehicleNumber = "MH04EF9012",
//            vehicleType = "Tata 407",
//            rating = 4.9,
//            totalTrips = 567,
//            currentLatitude = 19.0330,
//            currentLongitude = 73.0297,
//            etaMinutes = 5
//        ),
//        RiderInfo(
//            riderId = "1004",
//            riderName = "Prakash Patil",
//            riderPhone = "9876543213",
//            vehicleNumber = "MH02GH3456",
//            vehicleType = "Ashok Leyland Dost",
//            rating = 4.5,
//            totalTrips = 128,
//            currentLatitude = 19.0896,
//            currentLongitude = 72.8656,
//            etaMinutes = 10
//        )
//    )
//
//
//    // ═══════════════════════════════════════════════════════════════════════
//    // PUBLIC METHODS
//    // ═══════════════════════════════════════════════════════════════════════
//
//    /**
//     * Get a random rider with random ETA
//     */
//    fun getRandomRider(): RiderInfo {
//        return mockRiders.random().copy(
//            etaMinutes = Random.nextInt(5, 15)
//        )
//    }
//
//    /**
//     * Generate 4-digit OTP
//     */
//    fun generateOtp(): String {
//        return (1000..9999).random().toString()
//    }
//
//    /**
//     * Create SEARCHING status update
//     */
//    fun createSearchingUpdate(bookingId: String): BookingStatusUpdate {
//        return BookingStatusUpdate(
//            bookingId = bookingId,
//            status = BookingStatusType.SEARCHING.name,
//            message = "Looking for nearby riders...",
//            timestamp = System.currentTimeMillis().toString()
//        )
//    }
//
//    /**
//     * Create RIDER_ASSIGNED status update
//     */
//    fun createRiderAssignedUpdate(bookingId: String): BookingStatusUpdate {
//        val rider = getRandomRider()
//        val otp = generateOtp()
//
//        return BookingStatusUpdate(
//            bookingId = bookingId,
//            status = BookingStatusType.RIDER_ASSIGNED.name,
//            message = "${rider.riderName} is coming to pick up your parcel",
//            rider = rider,
//            otp = otp,
//            timestamp = System.currentTimeMillis().toString()
//        )
//    }
//
//
//    /**
//     * Create RIDER_ENROUTE status update
//     */
//    fun createRiderEnrouteUpdate(
//        bookingId: String,
//        rider: RiderInfo
//    ): BookingStatusUpdate {
//        return BookingStatusUpdate(
//            bookingId = bookingId,
//            status = BookingStatusType.RIDER_ENROUTE.name,
//            message = "${rider.riderName} is on the way to pickup location",
//            rider = rider,
//            timestamp = System.currentTimeMillis().toString()
//        )
//    }
//
//
//    /**
//     * Create ARRIVED status update
//     */
//    fun createArrivedUpdate(
//        bookingId: String,
//        rider: RiderInfo
//    ): BookingStatusUpdate {
//        return BookingStatusUpdate(
//            bookingId = bookingId,
//            status = BookingStatusType.ARRIVED.name,
//            message = "${rider.riderName} has arrived at pickup location",
//            rider = rider,
//            timestamp = System.currentTimeMillis().toString()
//        )
//    }
//
//
//    /**
//     * Create IN_TRANSIT status update
//     */
//    fun createInTransitUpdate(
//        bookingId: String,
//        rider: RiderInfo
//    ): BookingStatusUpdate {
//        return BookingStatusUpdate(
//            bookingId = bookingId,
//            status = BookingStatusType.IN_TRANSIT.name,
//            message = "Parcel picked up! Heading to destination",
//            rider = rider,
//            timestamp = System.currentTimeMillis().toString()
//        )
//    }
//
//
//    /**
//     * Create DELIVERED status update
//     */
//    fun createDeliveredUpdate(
//        bookingId: String,
//        rider: RiderInfo
//    ): BookingStatusUpdate {
//        return BookingStatusUpdate(
//            bookingId = bookingId,
//            status = BookingStatusType.DELIVERED.name,
//            message = "Parcel delivered successfully!",
//            rider = rider,
//            timestamp = System.currentTimeMillis().toString()
//        )
//    }
//
//
//    /**
//     * Create NO_RIDER status update
//     */
//    fun createNoRiderUpdate(bookingId: String): BookingStatusUpdate {
//        return BookingStatusUpdate(
//            bookingId = bookingId,
//            status = BookingStatusType.NO_RIDER.name,
//            message = "No riders available in your area. Please try again.",
//            timestamp = System.currentTimeMillis().toString()
//        )
//    }
//
//
//    /**
//     * Create CANCELLED status update
//     */
//    fun createCancelledUpdate(
//        bookingId: String,
//        reason: String
//    ): BookingStatusUpdate {
//        return BookingStatusUpdate(
//            bookingId = bookingId,
//            status = BookingStatusType.CANCELLED.name,
//            message = reason,
//            timestamp = System.currentTimeMillis().toString()
//        )
//    }
//
//    /**
//     * Generate location update for rider moving towards destination
//     * @param progress 0.0 to 1.0 (0 = start, 1 = arrived)
//     */
//    fun generateLocationUpdate(
//        bookingId: String,
//        rider: RiderInfo,
//        startLat: Double,
//        startLng: Double,
//        endLat: Double,
//        endLng: Double,
//        progress: Float
//    ): RiderLocationUpdate {
//
//        val currentLat = startLat + (endLat - startLat) * progress
//        val currentLng = startLng + (endLng - startLng) * progress
//
//        val totalEta = rider.etaMinutes ?: 10
//        val remainingEta = (totalEta * (1 - progress)).toInt().coerceAtLeast(1)
//
//        val totalDistanceMeters =
//            calculateDistance(startLat, startLng, endLat, endLng) * 1000
//
//        val remainingDistanceMeters =
//            totalDistanceMeters * (1 - progress)
//
//        return RiderLocationUpdate(
//            bookingId = bookingId,
//            riderId = rider.riderId,
//            latitude = currentLat,
//            longitude = currentLng,
//            speed = (Random.nextDouble() * 30) + 15,   // 15–45 km/h
//            heading = Random.nextDouble() * 360,       // degrees
//            etaMinutes = remainingEta,
//            distanceMeters = remainingDistanceMeters,
//            timestamp = System.currentTimeMillis().toString()
//        )
//    }
//
//
//
//    /**
//     * Calculate distance between two points (Haversine formula)
//     */
//    private fun calculateDistance(
//        lat1: Double, lng1: Double,
//        lat2: Double, lng2: Double
//    ): Double {
//        val r = 6371 // Earth's radius in km
//        val dLat = Math.toRadians(lat2 - lat1)
//        val dLng = Math.toRadians(lng2 - lng1)
//        val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
//                kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) *
//                kotlin.math.sin(dLng / 2) * kotlin.math.sin(dLng / 2)
//        val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
//        return r * c
//    }
}
