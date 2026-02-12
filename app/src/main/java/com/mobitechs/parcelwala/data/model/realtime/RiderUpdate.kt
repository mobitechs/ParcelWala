// data/model/realtime/RealTimeModels.kt
// ✅ No changes from previous version - already correct
// - distanceToPickupKm/distanceToDropKm fields present
// - getRelevantDistanceMeters() helper present
// - AdditionalBookingData model present
// - estimatedArrivalToMinutes() present
package com.mobitechs.parcelwala.data.model.realtime

import com.google.gson.annotations.SerializedName

// ════════════════════════════════════════════════════════════════════════════
// BOOKING STATUS UPDATE
// ════════════════════════════════════════════════════════════════════════════

data class BookingStatusUpdate(
    @SerializedName("bookingId")
    val bookingId: Int,

    @SerializedName("bookingNumber")
    val bookingNumber: String? = null,

    @SerializedName("status")
    val status: String? = null,

    @SerializedName("statusMessage")
    val statusMessage: String? = null,

    @SerializedName("message")
    val messageAlt: String? = null,

    @SerializedName("timestamp")
    val timestamp: String? = null,

    @SerializedName("driverId")
    val driverId: Int? = null,

    @SerializedName("driverName")
    val driverName: String? = null,

    @SerializedName("driverPhone")
    val driverPhone: String? = null,

    @SerializedName("driverRating")
    val driverRating: Double? = null,

    @SerializedName("driverPhoto")
    val driverPhoto: String? = null,

    @SerializedName("vehicleNumber")
    val vehicleNumber: String? = null,

    @SerializedName("vehicleType")
    val vehicleType: String? = null,

    @SerializedName("pickupOtp")
    val otp: String? = null,

    @SerializedName("deliveryOtp")
    val deliveryOtp: String? = null,

    @SerializedName("estimatedArrival")
    val estimatedArrival: String? = null,

    @SerializedName("etaMinutes")
    val etaMinutes: Int? = null,

    @SerializedName("driverLatitude")
    val driverLatitude: Double? = null,

    @SerializedName("driverLongitude")
    val driverLongitude: Double? = null,

    @SerializedName("cancellationReason")
    val cancellationReason: String? = null,

    @SerializedName("cancelledBy")
    val cancelledBy: String? = null,

    @SerializedName("totalFare")
    val totalFare: Int? = null,

    @SerializedName("waitingCharge")
    val waitingCharge: Int? = null,

    @SerializedName("additionalData")
    val additionalData: AdditionalBookingData? = null
) {
    val message: String?
        get() = statusMessage ?: messageAlt

    val distanceKm: Double?
        get() = additionalData?.distance

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
                etaMinutes = etaMinutes ?: estimatedArrivalToMinutes(),
                photoUrl = driverPhoto
            )
        } else null

    private fun estimatedArrivalToMinutes(): Int? {
        if (estimatedArrival.isNullOrEmpty()) return null
        return try {
            val format = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
            format.timeZone = java.util.TimeZone.getTimeZone("UTC")
            val cleaned = estimatedArrival.substringBefore("Z").let {
                if (it.contains(".")) it.substringBefore(".") else it
            }
            val arrivalTime = format.parse(cleaned)?.time ?: return null
            val now = System.currentTimeMillis()
            val diffMinutes = ((arrivalTime - now) / 60000).toInt()
            if (diffMinutes > 0) diffMinutes else 1
        } catch (e: Exception) { null }
    }

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

data class AdditionalBookingData(
    @SerializedName("pickupAddress") val pickupAddress: String? = null,
    @SerializedName("dropAddress") val dropAddress: String? = null,
    @SerializedName("distance") val distance: Double? = null,
    @SerializedName("fare") val fare: Double? = null
)

// ════════════════════════════════════════════════════════════════════════════
// BOOKING CANCELLED NOTIFICATION
// ════════════════════════════════════════════════════════════════════════════

data class BookingCancelledNotification(
    val bookingId: Int,
    val cancelledBy: String? = null,
    val reason: String? = null,
    val message: String = "Booking cancelled",
    val refundAmount: Double? = null,
    val timestamp: String? = null
)

// ════════════════════════════════════════════════════════════════════════════
// RIDER INFO
// ════════════════════════════════════════════════════════════════════════════

data class RiderInfo(
    val riderId: String,
    val riderName: String,
    val riderPhone: String,
    val vehicleNumber: String,
    val vehicleType: String? = null,
    val rating: Double? = null,
    val totalTrips: Int? = null,
    val currentLatitude: Double,
    val currentLongitude: Double,
    val etaMinutes: Int? = null,
    val photoUrl: String? = null
)

// ════════════════════════════════════════════════════════════════════════════
// RIDER LOCATION UPDATE
// ════════════════════════════════════════════════════════════════════════════

data class RiderLocationUpdate(
    val bookingId: String,
    val riderId: String? = null,
    val driverId: Int? = null,
    val latitude: Double,
    val longitude: Double,
    val distanceToPickupKm: Double? = null,
    val distanceToDropKm: Double? = null,
    val speed: Double? = null,
    val heading: Double? = null,
    val etaMinutes: Int? = null,
    val distanceMeters: Double? = null,
    val status: String? = null,
    val timestamp: String? = null
) {
    val driverIdString: String
        get() = riderId ?: driverId?.toString() ?: "0"

    /**
     * Get relevant distance in METERS based on current phase
     * Priority: distanceMeters (road, from driver) > distanceToPickup/Drop (straight-line, from backend)
     */
    fun getRelevantDistanceMeters(isPrePickup: Boolean): Double? {
        if (distanceMeters != null && distanceMeters > 0) return distanceMeters
        val distKm = if (isPrePickup) distanceToPickupKm else distanceToDropKm
        return distKm?.let { it * 1000.0 }
    }
}

// ════════════════════════════════════════════════════════════════════════════
// ENUMS & SEALED CLASSES
// ════════════════════════════════════════════════════════════════════════════

enum class BookingStatusType {
    SEARCHING, RIDER_ASSIGNED, RIDER_ENROUTE, ARRIVED,
    PICKED_UP, IN_TRANSIT, ARRIVED_DELIVERY, DELIVERED,
    CANCELLED, NO_RIDER
}

sealed class RealTimeConnectionState {
    object Disconnected : RealTimeConnectionState()
    object Connecting : RealTimeConnectionState()
    object Connected : RealTimeConnectionState()
    object Reconnecting : RealTimeConnectionState()
    data class Error(val message: String) : RealTimeConnectionState()
    val isConnected: Boolean get() = this is Connected
}

data class SignalRError(
    val message: String,
    val code: String? = null,
    val errorCode: String? = null
)