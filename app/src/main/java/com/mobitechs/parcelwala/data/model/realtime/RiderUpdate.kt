// data/model/realtime/RealTimeModels.kt
// ONLY CHANGE: Added @SerializedName("PickupOtp") for otp field
package com.mobitechs.parcelwala.data.model.realtime

import com.google.gson.annotations.SerializedName

data class BookingStatusUpdate(
    @SerializedName("BookingId")
    val bookingId: Int,

    @SerializedName("BookingNumber")
    val bookingNumber: String? = null,

    @SerializedName("Status")
    val status: String?=null,

    @SerializedName("StatusMessage")
    val message: String? = null,

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

    @SerializedName("PickupOtp")  // ← ADDED THIS
    val otp: String? = null,

    @SerializedName("EstimatedArrival")
    val estimatedArrival: String? = null,

    @SerializedName("CancellationReason")
    val cancellationReason: String? = null
) {
    val rider: RiderInfo?
        get() = if (!driverName.isNullOrEmpty()) {
            RiderInfo(
                riderId = "0",
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

    fun getStatusType(): BookingStatusType {
        if (status.isNullOrEmpty()) {
            return BookingStatusType.SEARCHING
        }
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

enum class BookingStatusType {
    SEARCHING,
    RIDER_ASSIGNED,
    RIDER_ENROUTE,
    ARRIVED,
    PICKED_UP,
    IN_TRANSIT,
    DELIVERED,
    CANCELLED,
    NO_RIDER
}

sealed class RealTimeConnectionState {
    object Disconnected : RealTimeConnectionState()
    object Connecting : RealTimeConnectionState()
    object Connected : RealTimeConnectionState()
    object Reconnecting : RealTimeConnectionState()
    data class Error(val message: String) : RealTimeConnectionState()
}