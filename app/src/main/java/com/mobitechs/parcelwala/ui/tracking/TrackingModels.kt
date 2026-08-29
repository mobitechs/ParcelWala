package com.mobitechs.parcelwala.ui.tracking

import com.mobitechs.parcelwala.data.model.realtime.BookingStatusType

/**
 * ════════════════════════════════════════════════════════════════════════════
 * TRACKING PHASE — the single source of truth for the tracking screen.
 * ════════════════════════════════════════════════════════════════════════════
 *
 * THE PROBLEM THIS REPLACES
 *
 * The screen used to derive its behaviour from five loose booleans computed
 * inline (isPrePickup, isDriverArrived, isPostPickup, isDelivered,
 * isPaymentSuccess), while the ViewModel kept ONE _etaMinutes, ONE _distanceKm
 * and ONE routeFetchJob shared between two completely different journey legs.
 *
 * That sharing caused three visible bugs:
 *   - the pickup→drop ETA was shown while the driver was still coming to pickup
 *   - the driver→pickup polyline never arrived, so a straight line was drawn
 *     through buildings
 *   - the map stayed framed for the whole trip after the driver reached the drop
 *
 * Every phase below declares exactly what the map frames, which leg owns the
 * live numbers, and which sheet content is visible. Nothing is computed twice.
 */
enum class TrackingPhase {
    /** No driver assigned yet. */
    SEARCHING,

    /** Driver assigned / enroute. Map frames driver → pickup. */
    DRIVER_COMING,

    /** Driver has reached pickup. Map zooms to pickup. Waiting timer is live. */
    DRIVER_WAITING,

    /** Parcel collected, moving to drop. Map frames driver → drop. */
    IN_TRANSIT,

    /** Driver reached the delivery point. Map zooms to drop. Payment is due. */
    AT_DROP,

    /** Payment sent / delivered. Map is static, rating takes over. */
    COMPLETING;

    /** Ordinal comparison used by the map to decide which markers to show. */
    fun atOrBefore(other: TrackingPhase) = this.ordinal <= other.ordinal

    fun atOrAfter(other: TrackingPhase) = this.ordinal >= other.ordinal

    companion object {
        fun from(status: BookingStatusType): TrackingPhase = when (status) {
            BookingStatusType.SEARCHING -> SEARCHING
            BookingStatusType.RIDER_ASSIGNED,
            BookingStatusType.RIDER_ENROUTE -> DRIVER_COMING
            BookingStatusType.ARRIVED -> DRIVER_WAITING
            BookingStatusType.PICKED_UP,
            BookingStatusType.IN_TRANSIT -> IN_TRANSIT
            BookingStatusType.ARRIVED_DELIVERY -> AT_DROP
            BookingStatusType.PAYMENT_SUCCESS,
            BookingStatusType.DELIVERED -> COMPLETING
            BookingStatusType.CANCELLED,
            BookingStatusType.NO_RIDER -> SEARCHING
        }
    }
}

/**
 * A journey leg. Each leg owns its OWN eta, distance, polyline and fetch job.
 * This is the fix for the "wrong KM/time before the ride starts" bug — the
 * pickup→drop leg can be pre-fetched without ever touching the numbers the
 * customer is currently looking at.
 */
enum class Leg {
    DRIVER_TO_PICKUP,
    PICKUP_TO_DROP;

    companion object {
        /**
         * Which leg the customer is actually travelling right now.
         * null means no live leg — at the drop or completing, there is no
         * meaningful "distance remaining", so the UI shows none.
         */
        fun activeFor(phase: TrackingPhase): Leg? = when (phase) {
            TrackingPhase.DRIVER_COMING,
            TrackingPhase.DRIVER_WAITING -> DRIVER_TO_PICKUP
            TrackingPhase.IN_TRANSIT -> PICKUP_TO_DROP
            else -> null
        }
    }
}

/** How the camera should frame the map for the current phase. */
enum class MapFocus {
    DRIVER_AND_PICKUP,
    PICKUP_CLOSE,
    DRIVER_AND_DROP,
    DROP_CLOSE,
    WHOLE_TRIP
}

/**
 * Everything the tracking screen renders, in one object.
 *
 * The screen is a pure function of this. It computes no booleans of its own,
 * which is what stops the two legs from ever bleeding into each other again.
 */
data class TrackingUiModel(
    val phase: TrackingPhase = TrackingPhase.SEARCHING,
    val bookingId: String = "",

    // ── The one big number ────────────────────────────────────────────────
    /** Live ETA for the ACTIVE leg only. Null when no leg is active. */
    val etaMinutes: Int? = null,
    /** Live distance for the ACTIVE leg only. Null when no leg is active. */
    val distanceKm: Double? = null,

    // ── Map ───────────────────────────────────────────────────────────────
    val mapFocus: MapFocus = MapFocus.WHOLE_TRIP,
    /** Polyline for the active leg. Empty while loading — never a straight line. */
    val activeRoute: List<com.google.android.gms.maps.model.LatLng> = emptyList(),
    val isRouteLoading: Boolean = false,
    val driverLatLng: com.google.android.gms.maps.model.LatLng? = null,
    val driverBearing: Float = 0f,

    // ── Sheet content ─────────────────────────────────────────────────────
    val showPickupOtp: Boolean = false,
    val showDeliveryOtp: Boolean = false,
    /** True once we are post-pickup but the OTP has not arrived yet. */
    val isDeliveryOtpPending: Boolean = false,
    val showWaitingTimer: Boolean = false,
    val showPaymentSheet: Boolean = false,
    val canCancel: Boolean = false,

    // ── Connection / stall awareness ──────────────────────────────────────
    val isConnected: Boolean = true,
    val secondsSinceLastFix: Int = 0,
    /** Driver has not moved 50 m in 3 minutes while enroute. */
    val isDriverStalled: Boolean = false
) {
    val activeLeg: Leg? get() = Leg.activeFor(phase)

    /** True while we have a driver on the map and a live leg to show. */
    val hasLiveNumbers: Boolean
        get() = activeLeg != null && (etaMinutes != null || distanceKm != null)
}
