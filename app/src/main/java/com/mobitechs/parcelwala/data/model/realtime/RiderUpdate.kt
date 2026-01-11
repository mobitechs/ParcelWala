// data/model/realtime/RealTimeModels.kt
package com.mobitechs.parcelwala.data.model.realtime

import com.google.gson.annotations.SerializedName

/**
 * ════════════════════════════════════════════════════════════════════════════
 * REAL-TIME DATA MODELS
 * ════════════════════════════════════════════════════════════════════════════
 * These models match the backend DTOs exactly for SignalR communication
 * ════════════════════════════════════════════════════════════════════════════
 */

// ═══════════════════════════════════════════════════════════════════════════
// BOOKING STATUS UPDATE
// ═══════════════════════════════════════════════════════════════════════════
data class BookingStatusUpdate(
    @SerializedName("BookingId")
    val bookingId: Int,  // Changed to INT

    @SerializedName("BookingNumber")
    val bookingNumber: String? = null,

    @SerializedName("Status")
    val status: String,

    @SerializedName("StatusMessage")  // Changed from "Message"
    val message: String,

    @SerializedName("Timestamp")
    val timestamp: String,

    @SerializedName("DriverName")
    val driverName: String? = null,

    @SerializedName("DriverPhone")
    val driverPhone: String? = null,

    @SerializedName("VehicleNumber")
    val vehicleNumber: String? = null,

    @SerializedName("DriverRating")
    val driverRating: Double? = null,

    @SerializedName("EstimatedArrival")
    val estimatedArrival: String? = null,

    @SerializedName("CancellationReason")
    val cancellationReason: String? = null
) {
    // Create RiderInfo from individual fields
    val rider: RiderInfo?
        get() = if (!driverName.isNullOrEmpty()) {
            RiderInfo(
                riderId = "0",  // Not provided by backend
                riderName = driverName,
                riderPhone = driverPhone ?: "",
                vehicleNumber = vehicleNumber ?: "",
                vehicleType = null,
                rating = driverRating,
                totalTrips = null,
                currentLatitude = 0.0,
                currentLongitude = 0.0,
                etaMinutes = null
            )
        } else null

    val otp: String? = null  // Backend doesn't send OTP in status update

    fun getStatusType(): BookingStatusType {
        return when (status.lowercase()) {
            "searching" -> BookingStatusType.SEARCHING
            "assigned" -> BookingStatusType.RIDER_ASSIGNED
            "pickup_started" -> BookingStatusType.RIDER_ENROUTE
            "arrived_pickup" -> BookingStatusType.ARRIVED
            "pickup_completed" -> BookingStatusType.PICKED_UP
            "arrived_delivery" -> BookingStatusType.IN_TRANSIT
            "delivery_completed" -> BookingStatusType.DELIVERED
            "cancelled" -> BookingStatusType.CANCELLED
            else -> BookingStatusType.SEARCHING
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// RIDER INFORMATION
// ═══════════════════════════════════════════════════════════════════════════

data class RiderInfo(
    @SerializedName("RiderId")
    val riderId: String,

    @SerializedName("RiderName")
    val riderName: String,

    @SerializedName("RiderPhone")
    val riderPhone: String,

    @SerializedName("VehicleNumber")
    val vehicleNumber: String,

    @SerializedName("VehicleType")
    val vehicleType: String? = null,

    @SerializedName("Rating")
    val rating: Double? = null,

    @SerializedName("TotalTrips")
    val totalTrips: Int? = null,

    @SerializedName("CurrentLatitude")
    val currentLatitude: Double,

    @SerializedName("CurrentLongitude")
    val currentLongitude: Double,

    @SerializedName("EtaMinutes")
    val etaMinutes: Int? = null
)

// ═══════════════════════════════════════════════════════════════════════════
// RIDER LOCATION UPDATE
// ═══════════════════════════════════════════════════════════════════════════

data class RiderLocationUpdate(
    @SerializedName("BookingId")
    val bookingId: String,

    @SerializedName("RiderId")
    val riderId: String,

    @SerializedName("Latitude")
    val latitude: Double,

    @SerializedName("Longitude")
    val longitude: Double,

    @SerializedName("Speed")
    val speed: Double? = null,

    @SerializedName("Heading")
    val heading: Double? = null,

    @SerializedName("EtaMinutes")
    val etaMinutes: Int? = null,

    @SerializedName("DistanceMeters")
    val distanceMeters: Double? = null,

    @SerializedName("Timestamp")
    val timestamp: String
)

// ═══════════════════════════════════════════════════════════════════════════
// BOOKING STATUS TYPES
// ═══════════════════════════════════════════════════════════════════════════

enum class BookingStatusType {
    SEARCHING,          // Looking for driver
    RIDER_ASSIGNED,     // Driver accepted
    RIDER_ENROUTE,      // Driver heading to pickup
    ARRIVED,            // Driver arrived at pickup
    PICKED_UP,          // Parcel picked up (OTP verified)
    IN_TRANSIT,         // Heading to delivery
    DELIVERED,          // Successfully delivered
    CANCELLED,          // Booking cancelled
    NO_RIDER            // No driver available
}

// ═══════════════════════════════════════════════════════════════════════════
// CONNECTION STATE
// ═══════════════════════════════════════════════════════════════════════════

sealed class RealTimeConnectionState {
    object Disconnected : RealTimeConnectionState()
    object Connecting : RealTimeConnectionState()
    object Connected : RealTimeConnectionState()
    object Reconnecting : RealTimeConnectionState()
    data class Error(val message: String) : RealTimeConnectionState()
}
