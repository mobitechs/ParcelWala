// data/model/realtime/RiderUpdate.kt
package com.mobitechs.parcelwala.data.model.realtime

import com.google.gson.annotations.SerializedName

/**
 * ════════════════════════════════════════════════════════════════════════════
 * REAL-TIME DATA MODELS
 * ════════════════════════════════════════════════════════════════════════════
 * Used for SignalR communication between app and server
 */

/**
 * Booking Status Types
 */
enum class BookingStatusType(val value: String) {
    SEARCHING("searching"),
    RIDER_ASSIGNED("rider_assigned"),
    RIDER_ENROUTE("rider_enroute"),
    ARRIVED("arrived"),
    PICKED_UP("picked_up"),
    IN_TRANSIT("in_transit"),
    DELIVERED("delivered"),
    CANCELLED("cancelled"),
    NO_RIDER("no_rider_available");

    companion object {
        fun fromString(value: String): BookingStatusType {
            return entries.find { it.value.equals(value, ignoreCase = true) } ?: SEARCHING
        }
    }
}

/**
 * Rider Information
 * Received when rider is assigned to booking
 */
data class RiderInfo(
    @SerializedName("rider_id")
    val riderId: Int,

    @SerializedName("name")
    val name: String,

    @SerializedName("phone")
    val phone: String,

    @SerializedName("photo")
    val photo: String? = null,

    @SerializedName("vehicle_number")
    val vehicleNumber: String,

    @SerializedName("vehicle_type")
    val vehicleType: String,

    @SerializedName("vehicle_model")
    val vehicleModel: String? = null,

    @SerializedName("rating")
    val rating: Double = 0.0,

    @SerializedName("total_trips")
    val totalTrips: Int = 0,

    @SerializedName("current_latitude")
    val currentLatitude: Double? = null,

    @SerializedName("current_longitude")
    val currentLongitude: Double? = null,

    @SerializedName("eta_minutes")
    val etaMinutes: Int = 0
)

/**
 * Rider Location Update
 * Received periodically when rider is moving
 */
data class RiderLocationUpdate(
    @SerializedName("booking_id")
    val bookingId: String,

    @SerializedName("rider_id")
    val riderId: Int,

    @SerializedName("latitude")
    val latitude: Double,

    @SerializedName("longitude")
    val longitude: Double,

    @SerializedName("heading")
    val heading: Float = 0f,

    @SerializedName("speed")
    val speed: Float = 0f,

    @SerializedName("eta_minutes")
    val etaMinutes: Int = 0,

    @SerializedName("distance_km")
    val distanceKm: Double = 0.0,

    @SerializedName("updated_at")
    val updatedAt: String? = null
)

/**
 * Booking Status Update
 * Main update received from SignalR for booking state changes
 */
data class BookingStatusUpdate(
    @SerializedName("booking_id")
    val bookingId: String,

    @SerializedName("status")
    val status: String,

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("rider")
    val rider: RiderInfo? = null,

    @SerializedName("otp")
    val otp: String? = null,

    @SerializedName("updated_at")
    val updatedAt: String? = null
) {
    fun getStatusType(): BookingStatusType = BookingStatusType.fromString(status)
}

/**
 * Real-Time Connection State
 */
sealed class RealTimeConnectionState {
    object Disconnected : RealTimeConnectionState()
    object Connecting : RealTimeConnectionState()
    object Connected : RealTimeConnectionState()
    data class Error(val message: String) : RealTimeConnectionState()
    object Reconnecting : RealTimeConnectionState()
}
