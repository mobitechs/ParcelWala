// data/model/realtime/RealTimeModels.kt
package com.mobitechs.parcelwala.data.model.realtime

import com.google.gson.annotations.SerializedName

// ════════════════════════════════════════════════════════════════════════════
// BOOKING STATUS UPDATE
// ════════════════════════════════════════════════════════════════════════════

data class BookingStatusUpdate(
    @SerializedName("BookingId")
    val bookingId: Int,

    @SerializedName("BookingNumber")
    val bookingNumber: String? = null,

    @SerializedName("Status")
    val status: String? = null,

    @SerializedName("StatusMessage")
    val statusMessage: String? = null,

    @SerializedName("Message")
    val messageAlt: String? = null,

    @SerializedName("Timestamp")
    val timestamp: String? = null,

    // Driver Info
    @SerializedName("DriverId")
    val driverId: Int? = null,

    @SerializedName("DriverName")
    val driverName: String? = null,

    @SerializedName("DriverPhone")
    val driverPhone: String? = null,

    @SerializedName("VehicleNumber")
    val vehicleNumber: String? = null,

    @SerializedName("VehicleType")
    val vehicleType: String? = null,

    @SerializedName("DriverRating")
    val driverRating: Double? = null,

    @SerializedName("DriverPhoto")
    val driverPhoto: String? = null,

    // OTP
    @SerializedName("PickupOtp")
    val otp: String? = null,

    @SerializedName("DeliveryOtp")
    val deliveryOtp: String? = null,

    // ETA
    @SerializedName("EstimatedArrival")
    val estimatedArrival: String? = null,

    @SerializedName("EtaMinutes")
    val etaMinutes: Int? = null,

    // Location
    @SerializedName("DriverLatitude")
    val driverLatitude: Double? = null,

    @SerializedName("DriverLongitude")
    val driverLongitude: Double? = null,

    // Cancellation
    @SerializedName("CancellationReason")
    val cancellationReason: String? = null,

    @SerializedName("CancelledBy")
    val cancelledBy: String? = null
) {
    val message: String?
        get() = statusMessage ?: messageAlt

    val rider: RiderInfo?
        get() = if (!driverName.isNullOrEmpty()) {
            RiderInfo(
                riderId = driverId?.toString() ?: "0",
                riderName = driverName,
                riderPhone = driverPhone ?: "",
                vehicleNumber = vehicleNumber ?: "",
                vehicleType = vehicleType,
                rating = driverRating,
                totalTrips = null,
                currentLatitude = driverLatitude ?: 0.0,
                currentLongitude = driverLongitude ?: 0.0,
                etaMinutes = etaMinutes,
                photoUrl = driverPhoto
            )
        } else null

    fun getStatusType(): BookingStatusType {
        if (status.isNullOrEmpty()) return BookingStatusType.SEARCHING

        return when (status.lowercase()) {
            "searching" -> BookingStatusType.SEARCHING
            "assigned" -> BookingStatusType.RIDER_ASSIGNED
            "heading_to_pickup", "pickup_started" -> BookingStatusType.RIDER_ENROUTE
            "arrived_at_pickup", "arrived_pickup", "arrived" -> BookingStatusType.ARRIVED
            "picked_up", "pickup_completed" -> BookingStatusType.PICKED_UP
            "heading_to_drop", "in_transit" -> BookingStatusType.IN_TRANSIT
            "arrived_at_delivery", "arrived_delivery" -> BookingStatusType.ARRIVED_DELIVERY
            "delivered", "delivery_completed", "completed" -> BookingStatusType.DELIVERED
            "cancelled" -> BookingStatusType.CANCELLED
            "no_rider", "no_driver" -> BookingStatusType.NO_RIDER
            else -> BookingStatusType.SEARCHING
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
// BOOKING CANCELLED NOTIFICATION
// ════════════════════════════════════════════════════════════════════════════

data class BookingCancelledNotification(
    @SerializedName("BookingId")
    val bookingId: Int,

    @SerializedName("CancelledBy")
    val cancelledBy: String? = null,

    @SerializedName("Reason")
    val reason: String? = null,

    @SerializedName("Message")
    val message: String = "Booking cancelled",

    @SerializedName("RefundAmount")
    val refundAmount: Double? = null,

    @SerializedName("Timestamp")
    val timestamp: String? = null
)

// ════════════════════════════════════════════════════════════════════════════
// RIDER INFO
// ════════════════════════════════════════════════════════════════════════════

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
    val etaMinutes: Int? = null,

    @SerializedName("PhotoUrl")
    val photoUrl: String? = null
)

// ════════════════════════════════════════════════════════════════════════════
// RIDER LOCATION UPDATE
// ════════════════════════════════════════════════════════════════════════════

data class RiderLocationUpdate(
    @SerializedName("BookingId")
    val bookingId: String,

    @SerializedName("RiderId")
    val riderId: String? = null,

    @SerializedName("DriverId")
    val driverId: Int? = null,

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

    @SerializedName("Status")
    val status: String? = null,

    @SerializedName("Timestamp")
    val timestamp: String? = null
) {
    val driverIdString: String
        get() = riderId ?: driverId?.toString() ?: "0"
}

// ════════════════════════════════════════════════════════════════════════════
// BOOKING STATUS TYPE
// ════════════════════════════════════════════════════════════════════════════

enum class BookingStatusType {
    SEARCHING,
    RIDER_ASSIGNED,
    RIDER_ENROUTE,
    ARRIVED,
    PICKED_UP,
    IN_TRANSIT,
    ARRIVED_DELIVERY,
    DELIVERED,
    CANCELLED,
    NO_RIDER
}

// ════════════════════════════════════════════════════════════════════════════
// CONNECTION STATE
// ════════════════════════════════════════════════════════════════════════════

sealed class RealTimeConnectionState {
    object Disconnected : RealTimeConnectionState()
    object Connecting : RealTimeConnectionState()
    object Connected : RealTimeConnectionState()
    object Reconnecting : RealTimeConnectionState()
    data class Error(val message: String) : RealTimeConnectionState()

    val isConnected: Boolean
        get() = this is Connected
}

// ════════════════════════════════════════════════════════════════════════════
// SIGNALR ERROR
// ════════════════════════════════════════════════════════════════════════════

data class SignalRError(
    @SerializedName("Message")
    val message: String,

    @SerializedName("Code")
    val code: String? = null,

    @SerializedName("ErrorCode")
    val errorCode: String? = null
)