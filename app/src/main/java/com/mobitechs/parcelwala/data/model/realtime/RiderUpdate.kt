package com.mobitechs.parcelwala.data.model.realtime

data class BookingStatusUpdate(
    val bookingId: Int,
    val bookingNumber: String? = null,
    val status: String? = null,
    val statusMessage: String? = null,
    val messageAlt: String? = null,
    val timestamp: String? = null,
    val driverId: Int? = null,
    val driverName: String? = null,
    val driverPhone: String? = null,
    val driverRating: Double? = null,
    val driverPhoto: String? = null,
    val vehicleNumber: String? = null,
    val vehicleType: String? = null,
    val vehiclePhoto: String? = null,
    val pickupOtp: String? = null,
    val deliveredOtp: String? = null,
    val estimatedArrival: String? = null,
    val etaMinutes: Int? = null,
    val driverLatitude: Double? = null,
    val driverLongitude: Double? = null,
    val cancellationReason: String? = null,
    val cancelledBy: String? = null,
    val baseFare: Double? = null,
    val distanceFare: Double? = null,
    val loadingCharges: Double? = null,
    val waitingCharges: Double? = null,
    val tollCharges: Double? = null,
    val platformFee: Double? = null,
    val surgeMultiplier: Double? = null,
    val surgeAmount: Double? = null,
    val subTotal: Double? = null,
    val gstPercentage: Double? = null,
    val gstAmount: Double? = null,
    val couponDiscount: Double? = null,
    val roundedFare: Double? = null,
    val totalFare: Double? = null,
    val paymentStatus: String? = null,
    val paymentMethod: String? = "cash",
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
        } catch (e: Exception) {
            null
        }
    }

    fun getStatusType(): BookingStatusType {
        if (status.isNullOrEmpty()) return BookingStatusType.SEARCHING
        return when (status.lowercase()) {
            "searching" -> BookingStatusType.SEARCHING
            "assigned" -> BookingStatusType.RIDER_ASSIGNED
            "heading_to_pickup", "pickup_started" -> BookingStatusType.RIDER_ENROUTE
            "arrived_pickup" -> BookingStatusType.ARRIVED
            "pickup_completed" -> BookingStatusType.PICKED_UP
            "heading_to_drop", "in_transit" -> BookingStatusType.IN_TRANSIT
            "arrived_delivery" -> BookingStatusType.ARRIVED_DELIVERY
            "payment_success" -> BookingStatusType.PAYMENT_SUCCESS
            "delivery_completed", "completed" -> BookingStatusType.DELIVERED
            "cancelled" -> BookingStatusType.CANCELLED
            "no_rider", "no_driver" -> BookingStatusType.NO_RIDER
            else -> BookingStatusType.SEARCHING
        }
    }
}

data class AdditionalBookingData(
    val pickupAddress: String? = null,
    val dropAddress: String? = null,
    val distance: Double? = null,
    val fare: Double? = null
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
    PICKED_UP, IN_TRANSIT, ARRIVED_DELIVERY, PAYMENT_SUCCESS, DELIVERED,
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

/**
 * FIX #27 — merge, never replace.
 *
 * THE BUG
 * Two different server paths send BookingStatusUpdate with different levels of
 * detail:
 *
 *   NotifyBookingStatusChangeAsync  → full payload, includes DeliveredOtp
 *   JoinBookingChannel              → GetBookingStatusAsync, a leaner payload
 *                                     with deliveredOtp = null
 *
 * The connection in these logs was dropping every 20-30 seconds. Each reconnect
 * re-joined the booking channel, which pushed the lean payload, which the app
 * applied wholesale — wiping the delivery OTP the customer had just been given:
 *
 *   15:44:24  status=pickup_completed  deliveredOtp=null   ← rejoin, overwrote it
 *   15:45:13  status=pickup_completed  deliveredOtp=1217   ← full payload, back again
 *
 * That flicker is exactly the "OTP takes time to appear" glitch.
 *
 * THE FIX
 * A null field in a newer update means "not included", not "cleared". Only
 * bookingId, status and timestamp are taken unconditionally — everything else
 * keeps the last value we actually know.
 *
 * This makes the customer screen immune to whichever payload shape arrives, and
 * to any ordering between them.
 */
fun BookingStatusUpdate.mergedWith(incoming: BookingStatusUpdate): BookingStatusUpdate {
    // A different booking is not a merge — take the new one whole.
    if (incoming.bookingId != this.bookingId) return incoming

    return incoming.copy(
        bookingNumber = incoming.bookingNumber ?: this.bookingNumber,
        statusMessage = incoming.statusMessage ?: this.statusMessage,
        messageAlt = incoming.messageAlt ?: this.messageAlt,
        driverId = incoming.driverId ?: this.driverId,
        driverName = incoming.driverName ?: this.driverName,
        driverPhone = incoming.driverPhone ?: this.driverPhone,
        driverRating = incoming.driverRating ?: this.driverRating,
        driverPhoto = incoming.driverPhoto ?: this.driverPhoto,
        vehicleNumber = incoming.vehicleNumber ?: this.vehicleNumber,
        vehicleType = incoming.vehicleType ?: this.vehicleType,
        vehiclePhoto = incoming.vehiclePhoto ?: this.vehiclePhoto,
        pickupOtp = incoming.pickupOtp ?: this.pickupOtp,
        deliveredOtp = incoming.deliveredOtp ?: this.deliveredOtp,
        estimatedArrival = incoming.estimatedArrival ?: this.estimatedArrival,
        etaMinutes = incoming.etaMinutes ?: this.etaMinutes,
        driverLatitude = incoming.driverLatitude ?: this.driverLatitude,
        driverLongitude = incoming.driverLongitude ?: this.driverLongitude,
        cancellationReason = incoming.cancellationReason ?: this.cancellationReason,
        cancelledBy = incoming.cancelledBy ?: this.cancelledBy,
        baseFare = incoming.baseFare ?: this.baseFare,
        distanceFare = incoming.distanceFare ?: this.distanceFare,
        loadingCharges = incoming.loadingCharges ?: this.loadingCharges,
        waitingCharges = incoming.waitingCharges ?: this.waitingCharges,
        tollCharges = incoming.tollCharges ?: this.tollCharges,
        platformFee = incoming.platformFee ?: this.platformFee,
        surgeMultiplier = incoming.surgeMultiplier ?: this.surgeMultiplier,
        surgeAmount = incoming.surgeAmount ?: this.surgeAmount,
        subTotal = incoming.subTotal ?: this.subTotal,
        gstPercentage = incoming.gstPercentage ?: this.gstPercentage,
        gstAmount = incoming.gstAmount ?: this.gstAmount,
        couponDiscount = incoming.couponDiscount ?: this.couponDiscount,
        roundedFare = incoming.roundedFare ?: this.roundedFare,
        totalFare = incoming.totalFare ?: this.totalFare,
        paymentStatus = incoming.paymentStatus ?: this.paymentStatus,
        paymentMethod = incoming.paymentMethod ?: this.paymentMethod,
        additionalData = incoming.additionalData ?: this.additionalData
        // message, distanceKm and rider are intentionally absent: they are
        // computed getters in the class body, not constructor parameters, so
        // copy() has no arguments for them. Nothing is lost — each is derived
        // from fields the merge above already preserves:
        //   message    ← statusMessage / messageAlt
        //   distanceKm ← additionalData.distance
        //   rider      ← driverName, driverPhone, vehicleNumber, lat/lng, etc.
    )
}