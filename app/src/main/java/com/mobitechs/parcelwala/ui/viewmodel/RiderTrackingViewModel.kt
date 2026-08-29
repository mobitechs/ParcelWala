package com.mobitechs.parcelwala.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.mobitechs.parcelwala.data.manager.ActiveBookingManager
import com.mobitechs.parcelwala.data.model.realtime.BookingStatusType
import com.mobitechs.parcelwala.data.model.realtime.BookingStatusUpdate
import com.mobitechs.parcelwala.data.model.realtime.RealTimeConnectionState
import com.mobitechs.parcelwala.data.model.realtime.RiderInfo
import com.mobitechs.parcelwala.data.model.realtime.RiderLocationUpdate
import com.mobitechs.parcelwala.data.model.response.FareDetails
import com.mobitechs.parcelwala.data.repository.BookingRepository
import com.mobitechs.parcelwala.data.repository.DirectionsRepository
import com.mobitechs.parcelwala.data.repository.RealTimeRepository
import com.mobitechs.parcelwala.ui.tracking.Leg
import com.mobitechs.parcelwala.ui.tracking.MapFocus
import com.mobitechs.parcelwala.ui.tracking.MapGeometry
import com.mobitechs.parcelwala.ui.tracking.TrackingPhase
import com.mobitechs.parcelwala.ui.tracking.TrackingUiModel
import com.mobitechs.parcelwala.utils.BookingNotificationHelper
import com.mobitechs.parcelwala.utils.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

/**
 * ════════════════════════════════════════════════════════════════════════════
 * RIDER TRACKING VIEWMODEL
 * ════════════════════════════════════════════════════════════════════════════
 *
 * WHAT CHANGED FROM THE PREVIOUS VERSION
 *
 * FIX A — per-leg state. There used to be one _etaMinutes, one _distanceKm and
 *   one routeFetchJob shared by the driver→pickup and pickup→drop legs.
 *   fetchRoute() opened with `routeFetchJob?.cancel()`, so calling it twice in
 *   handleDriverAssigned() meant the second call killed the first, then wrote
 *   the PICKUP→DROP distance into the number the customer was reading while the
 *   driver was still on the way to pickup. Legs now own separate state, jobs
 *   and throttles; the UI only ever reads the ACTIVE leg.
 *
 * FIX B — no straight-line fallback. The driver→pickup polyline used to never
 *   arrive (see FIX A), so the map fell through to a dashed straight line drawn
 *   through buildings. We now expose isRouteLoading instead and draw nothing.
 *
 * FIX C — waiting timer anchored to the server. It used to count up from the
 *   moment the ARRIVED event was RECEIVED. A 40-second socket drop silently
 *   lost 40 seconds of billable waiting, and a cold start reset it to zero.
 *   It is now computed from a persisted arrival timestamp, so it survives
 *   backgrounding, doze and process death.
 *
 * FIX D — the timer job is actually cancelled. clearState() used to do
 *   `waitingTimerJob = null` with no cancel(), leaking a coroutine that kept
 *   writing to _waitingState. On a retry you got two timers racing on one
 *   StateFlow and the displayed time flickered between two values.
 *
 * FIX E — blank OTPs never reach the UI. The screen checked `!= null`, but the
 *   OTP card pads with '-', so an empty-string payload rendered a card of
 *   dashes. OTPs are validated here, at the source.
 *
 * FIX F — payment no longer navigates away. ARRIVED_DELIVERY used to push a
 *   full-screen payment route, destroying the map at the most anxious moment
 *   of the trip, then popped back for the rating. It is now a sheet flag.
 *
 * FIX G — bad GPS is filtered, routes refetch on deviation rather than a timer,
 *   and route fetching stops entirely once the driver has arrived.
 */
@HiltViewModel
class RiderTrackingViewModel @Inject constructor(
    private val realTimeRepository: RealTimeRepository,
    private val activeBookingManager: ActiveBookingManager,
    private val notificationHelper: BookingNotificationHelper,
    private val bookingRepository: BookingRepository,
    private val directionsRepository: DirectionsRepository
) : ViewModel() {

    companion object {
        private const val TAG = "PW-Tracking"

        /**
         * ═══════════════════════════════════════════════════════════════════
         * SHARED CONNECTION OWNERSHIP — fixes the permanent "Reconnecting…"
         * ═══════════════════════════════════════════════════════════════════
         *
         * `booking_flow` and `active_booking_flow` each scope their OWN
         * RiderTrackingViewModel. Opening a live booking from Home therefore
         * creates a second instance while the first is still alive, and both
         * call connectToBooking() on the SAME singleton RealTimeRepository.
         *
         * Two things then went wrong:
         *
         *  1. The second instance tore down a perfectly good connection and
         *     rebuilt it, so the customer saw "Reconnecting…" for no reason.
         *  2. Worse, when the first graph was popped its onCleared() called
         *     realTimeRepository.disconnect() — killing the connection the
         *     SECOND instance was actively using. That is why the banner in the
         *     screenshots stayed up while data was clearly still arriving: the
         *     socket was being closed underneath the screen that owned it.
         *
         * Ownership is a token. Connecting claims it; only the current holder
         * may disconnect. A superseded instance shutting down leaves the live
         * connection alone.
         */
        private val connectionOwner = java.util.concurrent.atomic.AtomicReference<String?>(null)
        private val connectedBookingId = java.util.concurrent.atomic.AtomicReference<String?>(null)

        /** Hard floor between route refetches, even if the driver deviates. */
        private const val MIN_ROUTE_REFETCH_MS = 20_000L

        /** Refetch anyway after this long, to pick up traffic changes. */
        private const val MAX_ROUTE_AGE_MS = 120_000L

        private const val ASSUMED_SPEED_KMH = 25.0

        /** No movement beyond this within STALL_WINDOW_MS means the driver is stuck. */
        private const val STALL_DISTANCE_M = 50.0
        private const val STALL_WINDOW_MS = 180_000L
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PUBLIC STATE — the screen reads `ui` for everything structural
    // ═══════════════════════════════════════════════════════════════════════

    private val _ui = MutableStateFlow(TrackingUiModel())
    val ui: StateFlow<TrackingUiModel> = _ui.asStateFlow()

    private val _assignedRider = MutableStateFlow<RiderInfo?>(null)
    val assignedRider: StateFlow<RiderInfo?> = _assignedRider.asStateFlow()

    private val _pickupOtp = MutableStateFlow<String?>(null)
    val pickupOtp: StateFlow<String?> = _pickupOtp.asStateFlow()

    private val _deliveryOtp = MutableStateFlow<String?>(null)
    val deliveryOtp: StateFlow<String?> = _deliveryOtp.asStateFlow()

    private val _waitingState = MutableStateFlow(WaitingTimerState())
    val waitingState: StateFlow<WaitingTimerState> = _waitingState.asStateFlow()

    private val _ratingState = MutableStateFlow(RatingUiState())
    val ratingState: StateFlow<RatingUiState> = _ratingState.asStateFlow()

    private val _paymentState = MutableStateFlow(PostDeliveryPaymentState())
    val paymentState: StateFlow<PostDeliveryPaymentState> = _paymentState.asStateFlow()

    val connectionState: StateFlow<RealTimeConnectionState> = realTimeRepository.connectionState

    private val _navigationEvent = MutableSharedFlow<RiderTrackingNavigationEvent>()
    val navigationEvent: SharedFlow<RiderTrackingNavigationEvent> = _navigationEvent.asSharedFlow()

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    private val _rebookRequested = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val rebookRequested: SharedFlow<Unit> = _rebookRequested.asSharedFlow()

    // ═══════════════════════════════════════════════════════════════════════
    // INTERNAL — per-leg, never shared
    // ═══════════════════════════════════════════════════════════════════════

    private val legEta = mutableMapOf<Leg, Int>()
    private val legDistanceKm = mutableMapOf<Leg, Double>()
    private val legRoute = mutableMapOf<Leg, List<LatLng>>()
    private val legJob = mutableMapOf<Leg, Job>()
    private val legFetchedAt = mutableMapOf<Leg, Long>()
    private val legHasServerEta = mutableMapOf<Leg, Boolean>()

    /** Distance at the moment each leg began — the denominator for progress. */
    private val legStartDistanceKm = mutableMapOf<Leg, Double>()

    private var waitingTimerJob: Job? = null
    private var stallWatchJob: Job? = null

    private var currentStatus: BookingStatusType = BookingStatusType.SEARCHING
    private var currentBookingId: String? = null

    /** Identity for connection ownership. See the companion object. */
    private val instanceToken: String = java.util.UUID.randomUUID().toString()

    private var lastFix: LatLng? = null
    private var lastFixAtMs: Long = 0L
    private var lastMovedAtMs: Long = 0L
    private var lastMovedFrom: LatLng? = null
    private var smoothedBearing: Float = 0f

    /** Last content posted to the ongoing tracking notification. See publish site. */
    private var lastNotificationSignature: String? = null

    private var cachedBookingFare = 0.0
    private var freeWaitingSeconds: Int = FareDetails.DEFAULT_FREE_WAITING_MINS * 60
    private var chargePerMinute: Double = FareDetails.DEFAULT_CHARGE_PER_MIN

    init {
        observeRealTimeUpdates()
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CONNECTION
    // ═══════════════════════════════════════════════════════════════════════

    fun connectToBooking(bookingId: String, pickupLatitude: Double, pickupLongitude: Double) {
        Log.d(TAG, "📡 Connecting to booking $bookingId")

        // FIX — do NOT wipe state when we are already on this booking.
        //
        // SearchingRiderScreen and RiderFoundScreen both call connectToBooking()
        // from a LaunchedEffect, and inside one nav graph they share this
        // ViewModel. Moving from searching -> rider_found therefore called this
        // a second time, and the unconditional clearState() threw away the
        // rider, both OTPs and the waiting-timer anchor. Whether the customer
        // got them back depended entirely on the next payload being the FULL
        // one rather than the lean rejoin payload — which is a coin flip, and
        // exactly the "delivery OTP never appeared" symptom.
        val isSameBooking = currentBookingId == bookingId
        if (!isSameBooking) {
            clearState()
        } else {
            Log.d(TAG, "♻️ Same booking, preserving rider / OTP / timer state")
        }

        currentBookingId = bookingId
        _ui.update { it.copy(bookingId = bookingId) }

        activeBookingManager.activeBooking.value?.let { booking ->
            if (booking.fare > 0) cachedBookingFare = booking.fare
            _paymentState.update { it.copy(paymentMethod = booking.paymentMethod) }
            freeWaitingSeconds = booking.freeWaitingSeconds
            chargePerMinute = booking.waitingChargePerMin

            // Restore the last known server state before SignalR delivers
            // anything, so a cold start does not show an empty screen.
            booking.lastSignalRUpdate?.let { cached ->
                Log.d(TAG, "🔁 Restoring from persisted state: ${cached.status}")
                handleBookingStatusUpdate(cached)
            }
        }

        // Only rebuild the socket if we are not already subscribed to this
        // exact booking. Re-subscribing to a healthy connection is what made
        // the reconnect banner flash every time the screen was re-entered.
        val alreadyLive = connectedBookingId.get() == bookingId &&
                realTimeRepository.connectionState.value is RealTimeConnectionState.Connected

        connectionOwner.set(instanceToken)
        connectedBookingId.set(bookingId)

        if (alreadyLive) {
            Log.d(TAG, "♻️ Reusing live connection for $bookingId")
            _ui.update { it.copy(isConnected = true) }
        } else {
            realTimeRepository.connectAndSubscribe(bookingId = bookingId)
        }
    }

    fun disconnect() {
        stopWaitingTimer()
        releaseConnection()
        clearState()
    }

    /**
     * Close the shared socket ONLY if this instance still owns it. A superseded
     * instance being cleared must not disconnect the screen that took over.
     */
    private fun releaseConnection() {
        if (connectionOwner.compareAndSet(instanceToken, null)) {
            connectedBookingId.set(null)
            realTimeRepository.disconnect()
        } else {
            Log.d(TAG, "⏭️ Not the connection owner, leaving socket open")
        }
    }

    private fun observeRealTimeUpdates() {
        viewModelScope.launch {
            realTimeRepository.bookingUpdates.collect { handleBookingStatusUpdate(it) }
        }
        viewModelScope.launch {
            realTimeRepository.riderLocationUpdates.collect { handleRiderLocationUpdate(it) }
        }
        viewModelScope.launch {
            // FIX — connection state used to be stored in uiState.connectionError
            // and then never rendered anywhere, so a silent socket drop just
            // froze the marker with no explanation to the customer.
            realTimeRepository.connectionState.collect { state ->
                _ui.update { it.copy(isConnected = state is RealTimeConnectionState.Connected) }
            }
        }
        viewModelScope.launch {
            realTimeRepository.errors.collect { _toastMessage.emit(it.message) }
        }
        // PERFORMANCE — "last updated Ns ago".
        //
        // This used to be an unconditional `while (isActive) { delay(1000) }`
        // that wrote a new value into _ui every second for the entire life of
        // the ViewModel — including the whole time the connection was perfectly
        // healthy, and including when no booking was being tracked at all.
        // TrackingUiModel is the screen's single source of truth, so each of
        // those writes recomposed the map screen once per second for a number
        // that ConnectionBanner only ever renders while DISCONNECTED.
        //
        // The counter now runs only while the banner is actually on screen, and
        // the field is reset on reconnect so it never shows a stale age.
        viewModelScope.launch {
            realTimeRepository.connectionState.collect { state ->
                if (state is RealTimeConnectionState.Connected) {
                    if (_ui.value.secondsSinceLastFix != 0) {
                        _ui.update { it.copy(secondsSinceLastFix = 0) }
                    }
                    return@collect
                }
                while (isActive &&
                    realTimeRepository.connectionState.value !is RealTimeConnectionState.Connected
                ) {
                    if (lastFixAtMs > 0) {
                        val secs = ((System.currentTimeMillis() - lastFixAtMs) / 1000).toInt()
                        _ui.update { it.copy(secondsSinceLastFix = secs) }
                    }
                    delay(1_000)
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // STATUS UPDATES
    // ═══════════════════════════════════════════════════════════════════════

    private fun handleBookingStatusUpdate(update: BookingStatusUpdate) {
        // Defence in depth — this screen only ever reacts to the booking it was
        // opened for, even if a stale event leaks past the repository guard.
        val expected = currentBookingId.asBookingId()
        if (expected != null && update.bookingId != expected) {
            Log.w(TAG, "⚠️ Ignoring update for ${update.bookingId}, tracking $expected")
            return
        }

        val status = update.getStatusType()
        currentStatus = status
        activeBookingManager.updateFromSignalR(update)

        restoreRiderIfNeeded(update)
        cacheFareIfAvailable(update)
        setPickupOtp(update.pickupOtp)
        setDeliveryOtp(update.deliveredOtp)

        viewModelScope.launch {
            when (status) {
                BookingStatusType.SEARCHING -> Unit
                BookingStatusType.RIDER_ASSIGNED -> handleDriverAssigned(update)
                BookingStatusType.RIDER_ENROUTE -> handleRiderEnroute(update)
                BookingStatusType.ARRIVED -> handleDriverArrived(update)
                BookingStatusType.PICKED_UP -> handleParcelPickedUp(update)
                BookingStatusType.IN_TRANSIT -> handleInTransit(update)
                BookingStatusType.ARRIVED_DELIVERY -> handleArrivedAtDelivery(update)
                BookingStatusType.PAYMENT_SUCCESS -> handlePaymentSuccess(update)
                BookingStatusType.DELIVERED -> handleDeliveryCompleted(update)
                BookingStatusType.NO_RIDER -> handleNoRider(update)
                BookingStatusType.CANCELLED -> handleCancelled(update)
            }
            publish()
        }
        publish()
    }

    private suspend fun handleDriverAssigned(update: BookingStatusUpdate) {
        val rider = resolveRiderFromUpdate(update)
        _assignedRider.value = rider

        val driverLat = update.driverLatitude ?: rider.currentLatitude
        val driverLng = update.driverLongitude ?: rider.currentLongitude
        val driver = LatLng(driverLat, driverLng)
        val pickup = pickupLatLng()
        val drop = dropLatLng()

        // Seed the driver→pickup leg from what the server already told us, so
        // the customer sees a number immediately rather than a blank.
        if (MapGeometry.isValid(driver) && pickup != null) {
            lastFix = driver
            lastFixAtMs = System.currentTimeMillis()
            val metres = MapGeometry.distanceMeters(driver, pickup)
            legDistanceKm[Leg.DRIVER_TO_PICKUP] = metres / 1000.0
            legEta[Leg.DRIVER_TO_PICKUP] =
                update.etaMinutes ?: rider.etaMinutes ?: etaFromMetres(metres)
            fetchRoute(driver, pickup, Leg.DRIVER_TO_PICKUP, force = true)
        }

        // Pre-fetch the trip leg for its POLYLINE ONLY. Because legs no longer
        // share state, this can no longer overwrite the numbers above — which
        // was the entire "wrong KM/time before the ride starts" bug.
        if (pickup != null && drop != null) {
            fetchRoute(pickup, drop, Leg.PICKUP_TO_DROP, force = true)
        }

        if (cachedBookingFare <= 0.0) {
            activeBookingManager.activeBooking.value?.fare
                ?.takeIf { it > 0 }?.let { cachedBookingFare = it }
        }

        notify(
            update.bookingId, "Driver assigned",
            buildString {
                append(rider.riderName).append(" is on the way")
                legEta[Leg.DRIVER_TO_PICKUP]?.let { if (it > 0) append("\n⏱️ Arriving in ~$it min") }
                rider.vehicleType?.let { append("\n🚚 $it") }
                _pickupOtp.value?.let { append("\n🔐 Pickup OTP: $it") }
            }
        )
        _navigationEvent.emit(
            RiderTrackingNavigationEvent.RiderAssigned(
                update.bookingId.toString(), rider, _pickupOtp.value
            )
        )
        startStallWatch()
    }

    private suspend fun handleRiderEnroute(update: BookingStatusUpdate) {
        notify(
            update.bookingId, "Driver on the way",
            buildString {
                append(riderName).append(" is heading to pickup")
                legEta[Leg.DRIVER_TO_PICKUP]?.let { if (it > 0) append("\n⏱️ ~$it min away") }
            }
        )
        _navigationEvent.emit(
            RiderTrackingNavigationEvent.RiderEnroute(update.bookingId.toString())
        )
        startStallWatch()
    }

    private suspend fun handleDriverArrived(update: BookingStatusUpdate) {
        stopStallWatch()

        // FIX C — anchor to the server's arrival time, not "now". Order matters:
        // a previously persisted anchor always wins, so reconnects and cold
        // starts never restart the clock.
        val arrivedAt = activeBookingManager.activeBooking.value?.arrivedAtPickupMs
            ?: parseTimestamp(update.timestamp)
            ?: System.currentTimeMillis()
        activeBookingManager.markArrivedAtPickup(arrivedAt)
        startWaitingTimer(arrivedAt)

        // The driver is here — there is no meaningful distance left on this leg.
        legEta[Leg.DRIVER_TO_PICKUP] = 0
        legDistanceKm[Leg.DRIVER_TO_PICKUP] = 0.0
        legJob[Leg.DRIVER_TO_PICKUP]?.cancel()

        notify(
            update.bookingId, "📍 Driver has arrived",
            buildString {
                append(riderName).append(" is at your pickup location")
                _pickupOtp.value?.let { append("\n🔐 Share OTP: $it") }
            }
        )
        _toastMessage.emit(update.message ?: "Rider has arrived at pickup")
        _navigationEvent.emit(
            RiderTrackingNavigationEvent.RiderArrived(
                update.bookingId.toString(), update.message ?: "Rider has arrived"
            )
        )
    }

    private suspend fun handleParcelPickedUp(update: BookingStatusUpdate) {
        val finalCharge = _waitingState.value.waitingCharge
        stopWaitingTimer()
        Log.d(TAG, "💰 Final waiting charge at pickup: ₹$finalCharge")

        val pickup = pickupLatLng()
        val drop = dropLatLng()
        if (pickup != null && drop != null) {
            val metres = MapGeometry.distanceMeters(pickup, drop)
            legDistanceKm[Leg.PICKUP_TO_DROP] = metres / 1000.0
            legEta[Leg.PICKUP_TO_DROP] = etaFromMetres(metres)
            fetchRoute(lastFix ?: pickup, drop, Leg.PICKUP_TO_DROP, force = true)
        }

        notify(
            update.bookingId, "📦 Parcel picked up",
            buildString {
                append("Your parcel is on the way to delivery")
                _deliveryOtp.value?.let { append("\n🔐 Delivery OTP: $it") }
            }
        )
        _toastMessage.emit("Parcel picked up")
        _navigationEvent.emit(
            RiderTrackingNavigationEvent.ParcelPickedUp(update.bookingId.toString())
        )
        startStallWatch()
    }

    private fun handleInTransit(update: BookingStatusUpdate) {
        notify(update.bookingId, "Parcel in transit", "Your parcel is on the way to delivery")
    }

    private suspend fun handleArrivedAtDelivery(update: BookingStatusUpdate) {
        stopStallWatch()
        // FIX G — stop burning Directions quota on a zero-length route. The old
        // code kept fetching every 30 s because ARRIVED_DELIVERY was inside
        // POST_PICKUP_STATUSES, and the degenerate polyline it got back is part
        // of what made the map look broken at the drop.
        legJob[Leg.PICKUP_TO_DROP]?.cancel()

        val fare = extractFare(update)
        val method = update.paymentMethod
            ?: activeBookingManager.activeBooking.value?.paymentMethod
            ?: "cash"

        // FIX F — a flag, NOT a navigation event. The map stays alive behind a
        // sheet instead of being destroyed and rebuilt.
        _paymentState.update {
            it.copy(
                showPaymentScreen = true,
                bookingId = update.bookingId.toString(),
                baseFare = fare.baseFare,
                waitingCharge = fare.waitingCharge,
                platformFee = fare.platformFee,
                gst = fare.gst,
                discount = fare.discount,
                totalFare = fare.totalFare,
                driverName = riderName,
                paymentMethod = method
            )
        }

        notify(
            update.bookingId, "🏠 Driver arrived at delivery",
            buildString {
                append("Driver has arrived at the delivery location")
                _deliveryOtp.value?.let { append("\n🔐 Delivery OTP: $it") }
                if (fare.totalFare > 0) append("\n💰 Total: ₹${fare.totalFare}")
            }
        )
        _toastMessage.emit("Rider arrived at the delivery location")
    }

    private suspend fun handlePaymentSuccess(update: BookingStatusUpdate) {
        _paymentState.update {
            it.copy(showPaymentScreen = false, isPaymentCompleted = true, isVerifyingPayment = true)
        }
        notify(update.bookingId, "💳 Payment successful", "Payment confirmed. Completing delivery…")
        _toastMessage.emit("Payment confirmed")
    }

    private suspend fun handleDeliveryCompleted(update: BookingStatusUpdate) {
        stopWaitingTimer()
        stopStallWatch()
        _paymentState.update {
            it.copy(showPaymentScreen = false, isVerifyingPayment = false, isPaymentCompleted = true)
        }

        val fare = extractFare(update)
        val total = fare.totalFare.takeIf { it > 0 } ?: _paymentState.value.totalFare
        val waiting = fare.waitingCharge.takeIf { fare.totalFare > 0 }
            ?: _paymentState.value.waitingCharge

        notify(
            update.bookingId, "✅ Delivery completed",
            buildString {
                append("Your parcel has been delivered")
                if (total > 0) append("\n💰 Total: ₹$total")
            },
            isFinal = true
        )
        _toastMessage.emit("Delivery completed")

        _ratingState.update {
            it.copy(
                showRatingDialog = true,
                bookingId = update.bookingId.toString(),
                driverName = riderName,
                driverPhoto = _assignedRider.value?.photoUrl,
                vehicleType = _assignedRider.value?.vehicleType,
                totalFare = total,
                waitingCharge = waiting
            )
        }
        releaseConnection()
    }

    private suspend fun handleNoRider(update: BookingStatusUpdate) {
        _navigationEvent.emit(
            RiderTrackingNavigationEvent.NoRiderAvailable(update.message ?: "No riders available")
        )
    }

    private suspend fun handleCancelled(update: BookingStatusUpdate) {
        stopWaitingTimer()
        stopStallWatch()
        if (update.cancelledBy?.lowercase() == "driver") {
            handleDriverCancelled(update)
        } else {
            handleCustomerOrSystemCancelled(update)
        }
    }

    private suspend fun handleDriverCancelled(update: BookingStatusUpdate) {
        notify(
            update.bookingId, "Driver cancelled",
            buildString {
                append("Driver cancelled the booking")
                update.cancellationReason?.takeIf { it.isNotBlank() }?.let { append("\nReason: $it") }
                append("\nSearching for another driver…")
            }
        )
        _assignedRider.value = null
        _pickupOtp.value = null
        _deliveryOtp.value = null
        clearLegs()
        lastFix = null
        currentStatus = BookingStatusType.SEARCHING
        activeBookingManager.retrySearch()

        val msg = update.message ?: "Driver cancelled, searching for another driver"
        _toastMessage.emit(msg)
        _navigationEvent.emit(RiderTrackingNavigationEvent.DriverCancelledRetrySearch(msg))
    }

    private suspend fun handleCustomerOrSystemCancelled(update: BookingStatusUpdate) {
        val label = when (update.cancelledBy?.lowercase()) {
            "system" -> "Booking was cancelled by system"
            "customer" -> "You cancelled the booking"
            else -> "Booking has been cancelled"
        }
        notify(
            update.bookingId, "❌ Booking cancelled",
            buildString {
                append(label)
                update.cancellationReason?.takeIf { it.isNotBlank() }?.let { append("\nReason: $it") }
            },
            isFinal = true
        )
        activeBookingManager.clearActiveBooking()
        releaseConnection()
        val msg = update.message ?: "Booking cancelled"
        _toastMessage.emit(msg)
        _navigationEvent.emit(RiderTrackingNavigationEvent.BookingCancelled(msg))
    }

    // ═══════════════════════════════════════════════════════════════════════
    // LOCATION UPDATES
    // ═══════════════════════════════════════════════════════════════════════

    private fun handleRiderLocationUpdate(location: RiderLocationUpdate) {
        val incoming = LatLng(location.latitude, location.longitude)
        val now = System.currentTimeMillis()

        // FIX G — one bad fix used to teleport the marker across the city and
        // trigger a route refetch to nowhere.
        if (!MapGeometry.isPlausibleFix(lastFix, lastFixAtMs, incoming, now, location.accuracy)) {
            Log.w(TAG, "⚠️ Dropped implausible fix ${location.latitude},${location.longitude}")
            return
        }

        smoothedBearing = MapGeometry.smoothBearing(
            previous = smoothedBearing,
            incoming = location.heading
                ?: lastFix?.let { MapGeometry.bearingBetween(it, incoming).toDouble() },
            speedMs = location.speed
        )

        // Stall detection anchor — only reset when the driver genuinely moves.
        if (lastMovedFrom == null ||
            MapGeometry.distanceMeters(lastMovedFrom!!, incoming) > STALL_DISTANCE_M
        ) {
            lastMovedFrom = incoming
            lastMovedAtMs = now
        }

        lastFix = incoming
        lastFixAtMs = now

        val phase = TrackingPhase.from(currentStatus)
        val leg = Leg.activeFor(phase)
        if (leg != null) {
            val target = if (leg == Leg.DRIVER_TO_PICKUP) pickupLatLng() else dropLatLng()
            if (target != null) {
                val serverMetres = location.getRelevantDistanceMeters(leg == Leg.DRIVER_TO_PICKUP)
                val metres = serverMetres?.takeIf { it > 0 }
                    ?: MapGeometry.distanceMeters(incoming, target)
                val km = metres / 1000.0
                legDistanceKm[leg] = km
                legStartDistanceKm.putIfAbsent(leg, km)
                updateEtaForLeg(leg, location.etaMinutes, metres)

                // FIX G — refetch on DEVIATION, not on a 30-second clock. Most of
                // the time the driver is following the line we already have, so
                // this cuts Directions calls by roughly an order of magnitude.
                if (phase == TrackingPhase.DRIVER_COMING ||
            phase == TrackingPhase.IN_TRANSIT ||
            phase == TrackingPhase.AT_DROP
        ) {
                    val route = legRoute[leg].orEmpty()
                    val age = now - (legFetchedAt[leg] ?: 0L)
                    val deviated = MapGeometry.hasLeftRoute(route, incoming)
                    if ((deviated && age > MIN_ROUTE_REFETCH_MS) || age > MAX_ROUTE_AGE_MS) {
                        fetchRoute(incoming, target, leg)
                    }
                }
            }
        }

        // NOTE: we deliberately do NOT copy() the live position onto
        // _assignedRider any more. That produced a second StateFlow emission and
        // a full recomposition on every 3-second ping, for data the map already
        // receives via ui.driverLatLng.

        // Ongoing notification with a live progress bar. Most tracking attention
        // happens on the lock screen, not in the app — the customer books, locks
        // the phone and waits. This is the tracking screen for that stretch.
        if (phase == TrackingPhase.DRIVER_COMING || phase == TrackingPhase.IN_TRANSIT) {
            currentBookingId?.let { id ->
                val title = when (phase) {
                    TrackingPhase.DRIVER_COMING ->
                        legEta[Leg.DRIVER_TO_PICKUP]?.takeIf { it > 0 }
                            ?.let { "🚗 Arriving in $it min" }
                            ?: "🚗 $riderName is on the way"
                    TrackingPhase.AT_DROP ->
                        _deliveryOtp.value?.takeIf { it.isNotBlank() }
                            ?.let { "🔐 Share OTP $it to complete" }
                            ?: "🏠 Driver has arrived at delivery"
                    else ->
                        legEta[Leg.PICKUP_TO_DROP]?.takeIf { it > 0 }
                            ?.let { "📦 $it min to delivery" }
                            ?: "📦 Parcel in transit"
                }
                val body = buildString {
                    // OTP FIRST. The notification is where most tracking
                    // attention actually lives, and the collapsed shade
                    // shows only the first line — so the OTP has to be on
                    // it, not below a distance the customer can already see.
                    val otp = if (phase == TrackingPhase.DRIVER_COMING) {
                        _pickupOtp.value
                    } else {
                        _deliveryOtp.value
                    }
                    val otpLabel = if (phase == TrackingPhase.DRIVER_COMING) {
                        "Pickup OTP"
                    } else {
                        "Delivery OTP"
                    }
                    if (!otp.isNullOrBlank()) append("🔐 $otpLabel: $otp\n")
                    legDistanceKm[leg ?: Leg.DRIVER_TO_PICKUP]?.let {
                        append("📍 ${formatDistance(it)} remaining")
                    }
                }
                val progress = if (phase == TrackingPhase.AT_DROP) 100
                else legProgressPercent(leg)

                // PERFORMANCE — this used to rebuild and re-post the ongoing
                // notification on EVERY location ping, i.e. every ~3 seconds for
                // the whole trip. Each post is a synchronous binder call into
                // system_server plus a full RemoteViews inflate, all on the main
                // thread, and almost every one of them rendered text identical to
                // the notification already on screen: the distance line only
                // changes at 100 m granularity and the ETA only at whole minutes.
                //
                // Posting only when the visible content actually differs cuts
                // this by roughly 90% with no change to what the customer sees.
                val signature = "$title|$body|$progress"
                if (signature != lastNotificationSignature) {
                    lastNotificationSignature = signature
                    notificationHelper.showTrackingProgressNotification(
                        bookingId = id,
                        title = title,
                        body = body,
                        progressPercent = progress
                    )
                }
            }
        }

        publish()
    }

    private fun updateEtaForLeg(leg: Leg, serverEta: Int?, metres: Double) {
        val calculated = etaFromMetres(metres)
        if (serverEta != null && serverEta > 0) {
            // Sanity-check the server against physics — an ETA implying more
            // than 40 km/h through a city is optimistic enough to mistrust.
            val floor = ((metres / 1000.0) / 40.0 * 60.0).toInt().coerceAtLeast(1)
            if (serverEta >= floor) {
                legEta[leg] = serverEta
                legHasServerEta[leg] = true
                return
            }
        }
        legHasServerEta[leg] = false
        legEta[leg] = calculated
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ROUTES — one job, one throttle, one polyline PER LEG
    // ═══════════════════════════════════════════════════════════════════════

    private fun fetchRoute(from: LatLng, to: LatLng, leg: Leg, force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!force && now - (legFetchedAt[leg] ?: 0L) < MIN_ROUTE_REFETCH_MS) return

        // Cancels only THIS leg. The old code cancelled whichever fetch happened
        // to be in flight, which is how the driver→pickup polyline kept getting
        // killed by the pickup→drop prefetch.
        legJob[leg]?.cancel()
        legFetchedAt[leg] = now

        legJob[leg] = viewModelScope.launch {
            if (legRoute[leg].isNullOrEmpty()) {
                _ui.update { it.copy(isRouteLoading = true) }
            }
            directionsRepository.getRouteInfo(
                from.latitude, from.longitude, to.latitude, to.longitude
            )
                .onSuccess { info ->
                    legRoute[leg] = info.polylinePoints
                    legDistanceKm[leg] = info.distanceMeters / 1000.0
                    if (legHasServerEta[leg] != true) {
                        legEta[leg] = (info.durationSeconds / 60).coerceAtLeast(1)
                    }
                }
                .onFailure { Log.w(TAG, "⚠️ Route fetch failed for $leg: ${it.message}") }
            _ui.update { it.copy(isRouteLoading = false) }
            publish()
        }
    }

    private fun clearLegs() {
        legJob.values.forEach { it.cancel() }
        legJob.clear()
        legEta.clear()
        legDistanceKm.clear()
        legRoute.clear()
        legFetchedAt.clear()
        legHasServerEta.clear()
        legStartDistanceKm.clear()
    }

    // ═══════════════════════════════════════════════════════════════════════
    // WAITING TIMER
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * FIX C — computed from a wall-clock anchor rather than an incrementing
     * counter. Correct across backgrounding, doze, reconnects and process death,
     * and it always agrees with what the server will bill.
     */
    private fun startWaitingTimer(arrivedAtEpochMs: Long) {
        if (waitingTimerJob?.isActive == true) return
        Log.d(TAG, "⏱️ Waiting timer anchored at $arrivedAtEpochMs")

        waitingTimerJob = viewModelScope.launch {
            while (isActive) {
                val elapsed = ((System.currentTimeMillis() - arrivedAtEpochMs) / 1000)
                    .toInt().coerceAtLeast(0)
                val freeRemaining = (freeWaitingSeconds - elapsed).coerceAtLeast(0)
                val isFreeOver = elapsed > freeWaitingSeconds
                val extraSeconds = if (isFreeOver) elapsed - freeWaitingSeconds else 0
                val extraMinutes = extraSeconds / 60

                _waitingState.value = WaitingTimerState(
                    isActive = true,
                    totalWaitingSeconds = elapsed,
                    freeSecondsRemaining = freeRemaining,
                    isFreeWaitingOver = isFreeOver,
                    extraMinutesCharged = extraMinutes,
                    waitingCharge = extraMinutes * chargePerMinute,
                    currentMinuteSeconds = if (isFreeOver) extraSeconds % 60 else 0,
                    chargePerMinute = chargePerMinute,
                    totalFreeSeconds = freeWaitingSeconds
                )
                publish()
                delay(1_000)
            }
        }
    }

    private fun stopWaitingTimer() {
        // FIX D — the old code assigned null WITHOUT cancelling, leaking the
        // coroutine. Two timers then raced on one StateFlow and the displayed
        // time flickered between two values.
        waitingTimerJob?.cancel()
        waitingTimerJob = null
        _waitingState.update { it.copy(isActive = false) }
    }

    fun getFinalWaitingCharge(): Double = _waitingState.value.waitingCharge

    fun getTotalFare(): Double =
        (activeBookingManager.activeBooking.value?.fare ?: 0.0) + _waitingState.value.waitingCharge

    // ═══════════════════════════════════════════════════════════════════════
    // STALL DETECTION
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Silence while the driver is stuck is a major driver of support calls. If
     * nothing has moved 50 m in 3 minutes we say so and offer a call button,
     * rather than letting the customer stare at a frozen marker.
     */
    private fun startStallWatch() {
        if (stallWatchJob?.isActive == true) return
        lastMovedAtMs = System.currentTimeMillis()
        stallWatchJob = viewModelScope.launch {
            while (isActive) {
                delay(15_000)
                val stalled = lastMovedAtMs > 0 &&
                        System.currentTimeMillis() - lastMovedAtMs > STALL_WINDOW_MS
                _ui.update { it.copy(isDriverStalled = stalled) }
            }
        }
    }

    private fun stopStallWatch() {
        stallWatchJob?.cancel()
        stallWatchJob = null
        _ui.update { it.copy(isDriverStalled = false) }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PUBLISH — one emission, one recomposition
    // ═══════════════════════════════════════════════════════════════════════

    private fun publish() {
        val phase = TrackingPhase.from(currentStatus)
        val leg = Leg.activeFor(phase)

        _ui.update { prev ->
            prev.copy(
                phase = phase,
                bookingId = currentBookingId.orEmpty(),
                // The ACTIVE leg only. This is the guarantee that the trip
                // distance can never be shown while the driver is still on the
                // way to pickup.
                etaMinutes = leg?.let { legEta[it] },
                distanceKm = leg?.let { legDistanceKm[it] },
                mapFocus = when (phase) {
                    TrackingPhase.DRIVER_COMING -> MapFocus.DRIVER_AND_PICKUP
                    TrackingPhase.DRIVER_WAITING -> MapFocus.PICKUP_CLOSE
                    TrackingPhase.IN_TRANSIT -> MapFocus.DRIVER_AND_DROP
                    TrackingPhase.AT_DROP -> MapFocus.DROP_CLOSE
                    else -> MapFocus.WHOLE_TRIP
                },
                activeRoute = leg?.let { legRoute[it] }.orEmpty(),
                driverLatLng = lastFix,
                driverBearing = smoothedBearing,
                showPickupOtp = phase.atOrBefore(TrackingPhase.DRIVER_WAITING) &&
                        phase != TrackingPhase.SEARCHING &&
                        !_pickupOtp.value.isNullOrBlank(),
                showDeliveryOtp = phase.atOrAfter(TrackingPhase.IN_TRANSIT) &&
                        phase != TrackingPhase.COMPLETING &&
                        !_deliveryOtp.value.isNullOrBlank(),
                isDeliveryOtpPending = phase.atOrAfter(TrackingPhase.IN_TRANSIT) &&
                        phase != TrackingPhase.COMPLETING &&
                        _deliveryOtp.value.isNullOrBlank(),
                showWaitingTimer = phase == TrackingPhase.DRIVER_WAITING &&
                        _waitingState.value.isActive,
                showPaymentSheet = _paymentState.value.showPaymentScreen,
                canCancel = phase == TrackingPhase.DRIVER_COMING ||
                        phase == TrackingPhase.DRIVER_WAITING
            )
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PAYMENT / RATING / CANCEL
    // ═══════════════════════════════════════════════════════════════════════

    fun onPaymentCompleted() = sendPaymentConfirmation("💳 Online")
    fun onCashPaymentConfirmed() = sendPaymentConfirmation("💵 Cash")

    private fun sendPaymentConfirmation(tag: String) {
        viewModelScope.launch {
            val bookingId = _paymentState.value.bookingId
            _paymentState.update {
                it.copy(
                    showPaymentScreen = false,
                    isPaymentCompleted = true,
                    isVerifyingPayment = true
                )
            }
            publish()
            val id = bookingId.asBookingId() ?: return@launch
            realTimeRepository.updateBookingStatus(
                id, Constants.SignalREvents.STATUS_PAYMENT_SUCCESS
            ).onFailure { e ->
                Log.e(TAG, "$tag payment_success failed: ${e.message}")
                _toastMessage.emit("Retrying payment confirmation…")
            }
        }
    }

    fun submitRating(bookingId: String, rating: Int, feedback: String?) {
        _ratingState.update { it.copy(showRatingDialog = false, isSubmitting = true) }
        viewModelScope.launch {
            runCatching { bookingRepository.submitRating(bookingId, rating, feedback) }
            navigateHomeAfterCompletion()
        }
    }

    fun skipRating() {
        _ratingState.update { it.copy(showRatingDialog = false) }
        viewModelScope.launch { navigateHomeAfterCompletion() }
    }

    private suspend fun navigateHomeAfterCompletion() {
        activeBookingManager.clearActiveBooking()
        _ratingState.value = RatingUiState()
        _navigationEvent.emit(RiderTrackingNavigationEvent.NavigateToHome)
    }

    fun cancelBooking(reason: String) {
        viewModelScope.launch {
            val id = currentBookingId.asBookingId()
            if (id != null && id > 0) {
                realTimeRepository.cancelBooking(id, reason).onFailure { e ->
                    activeBookingManager.clearActiveBooking()
                    releaseConnection()
                    _navigationEvent.emit(
                        RiderTrackingNavigationEvent.BookingCancelled(
                            e.message ?: "Booking cancelled"
                        )
                    )
                }
            } else {
                activeBookingManager.clearActiveBooking()
                releaseConnection()
                _navigationEvent.emit(
                    RiderTrackingNavigationEvent.BookingCancelled("Booking cancelled")
                )
            }
        }
    }

    /** A retry is a NEW booking — never re-subscribe to the expired one. */
    fun retrySearch() {
        releaseConnection()
        activeBookingManager.clearActiveBooking()
        currentBookingId = null
        currentStatus = BookingStatusType.SEARCHING
        publish()
        viewModelScope.launch { _rebookRequested.emit(Unit) }
    }

    fun clearState() {
        clearLegs()
        stopWaitingTimer()
        stopStallWatch()
        _assignedRider.value = null
        _pickupOtp.value = null
        _deliveryOtp.value = null
        _waitingState.value = WaitingTimerState()
        _ratingState.value = RatingUiState()
        _paymentState.value = PostDeliveryPaymentState()
        _ui.value = TrackingUiModel()
        currentStatus = BookingStatusType.SEARCHING
        currentBookingId = null
        lastFix = null
        lastFixAtMs = 0L
        lastMovedFrom = null
        lastMovedAtMs = 0L
        smoothedBearing = 0f
        lastNotificationSignature = null
        cachedBookingFare = 0.0
        freeWaitingSeconds = FareDetails.DEFAULT_FREE_WAITING_MINS * 60
        chargePerMinute = FareDetails.DEFAULT_CHARGE_PER_MIN
    }

    // ═══════════════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════════════

    private val riderName: String get() = _assignedRider.value?.riderName ?: "Driver"

    /**
     * FIX E — validate at the source. The screen used to check `!= null` while
     * OtpCard padded short strings with '-', so an empty payload rendered a card
     * of dashes with no OTP in it. A partial value is also rejected rather than
     * shown, and a good value is never overwritten by a later lean payload.
     */
    private fun setPickupOtp(raw: String?) {
        val clean = raw?.trim()?.filter { it.isDigit() }.orEmpty()
        if (clean.length >= 4) _pickupOtp.value = clean
    }

    private fun setDeliveryOtp(raw: String?) {
        val clean = raw?.trim()?.filter { it.isDigit() }.orEmpty()
        if (clean.length >= 4) _deliveryOtp.value = clean
    }

    private fun pickupLatLng(): LatLng? =
        activeBookingManager.activeBooking.value?.pickupAddress
            ?.let { LatLng(it.latitude, it.longitude) }
            ?.takeIf { MapGeometry.isValid(it) }

    private fun dropLatLng(): LatLng? =
        activeBookingManager.activeBooking.value?.dropAddress
            ?.let { LatLng(it.latitude, it.longitude) }
            ?.takeIf { MapGeometry.isValid(it) }

    /**
     * Journey completion for the active leg, 0-100.
     *
     * Anchored to the distance we saw when the leg STARTED, because there is no
     * other honest denominator — the route length changes as the driver
     * deviates, and using the current route would make the bar jump backwards.
     * Returns -1 (indeterminate) until we have an anchor.
     */
    private fun legProgressPercent(leg: Leg?): Int {
        if (leg == null) return -1
        val start = legStartDistanceKm[leg] ?: return -1
        val now = legDistanceKm[leg] ?: return -1
        if (start <= 0.0) return -1
        val done = ((start - now) / start * 100).toInt()
        return done.coerceIn(0, 100)
    }

    private fun etaFromMetres(metres: Double): Int =
        ((metres / 1000.0) / ASSUMED_SPEED_KMH * 60.0).toInt().coerceAtLeast(1)

    /**
     * Locale-explicit. The old code called String.format without a Locale, which
     * emits "1,2 km" on devices set to e.g. German and breaks parsing.
     */
    fun formatDistance(distanceKm: Double?): String {
        if (distanceKm == null) return ""
        val metres = (distanceKm * 1000).toInt()
        return if (metres < 1000) "$metres m"
        else String.format(Locale.getDefault(), "%.1f km", distanceKm)
    }

    private fun parseTimestamp(raw: String?): Long? {
        if (raw.isNullOrBlank()) return null
        return runCatching {
            val fmt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            fmt.timeZone = java.util.TimeZone.getTimeZone("UTC")
            val cleaned = raw.substringBefore("Z").substringBefore(".")
            fmt.parse(cleaned)?.time
        }.getOrNull()
    }

    /** Booking ids arrive from SignalR as "26.0", which toIntOrNull() rejects. */
    private fun String?.asBookingId(): Int? {
        val t = this?.trim().orEmpty()
        if (t.isEmpty()) return null
        return t.toIntOrNull() ?: t.toDoubleOrNull()?.takeIf { it > 0 }?.toInt()
    }

    private fun restoreRiderIfNeeded(update: BookingStatusUpdate) {
        if (_assignedRider.value != null || update.driverName.isNullOrEmpty()) return
        _assignedRider.value = resolveRiderFromUpdate(update)
    }

    private fun resolveRiderFromUpdate(update: BookingStatusUpdate): RiderInfo =
        update.rider ?: RiderInfo(
            riderId = update.driverId?.toString() ?: "0",
            riderName = update.driverName ?: "Driver",
            riderPhone = update.driverPhone ?: "",
            vehicleNumber = update.vehicleNumber ?: "",
            vehicleType = update.vehicleType,
            rating = update.driverRating,
            totalTrips = null,
            currentLatitude = update.driverLatitude ?: 0.0,
            currentLongitude = update.driverLongitude ?: 0.0,
            etaMinutes = update.etaMinutes,
            photoUrl = update.driverPhoto
        )

    private fun cacheFareIfAvailable(update: BookingStatusUpdate) {
        val fare = extractFare(update)
        if (fare.totalFare <= 0) return
        _paymentState.update {
            it.copy(
                baseFare = fare.baseFare,
                waitingCharge = fare.waitingCharge,
                platformFee = fare.platformFee,
                gst = fare.gst,
                discount = fare.discount,
                totalFare = fare.totalFare
            )
        }
        if (cachedBookingFare == 0.0) cachedBookingFare = fare.baseFare
    }

    private fun extractFare(update: BookingStatusUpdate): FareBreakdown {
        val total = update.roundedFare
            ?: update.totalFare
            ?: update.subTotal
            ?: update.baseFare
            ?: update.additionalData?.fare
            ?: cachedBookingFare.takeIf { it > 0.0 }
            ?: activeBookingManager.activeBooking.value?.fare
            ?: 0.0
        return FareBreakdown(
            baseFare = update.baseFare ?: 0.0,
            waitingCharge = update.waitingCharges ?: 0.0,
            platformFee = update.platformFee ?: 0.0,
            gst = update.gstAmount ?: 0.0,
            discount = update.couponDiscount ?: 0.0,
            totalFare = total
        )
    }

    private fun notify(
        bookingId: Int,
        title: String,
        body: String,
        isFinal: Boolean = false,
        isSilent: Boolean = false
    ) = notificationHelper.showStickyStatusNotification(
        bookingId = bookingId.toString(),
        title = title,
        body = body,
        isFinal = isFinal,
        isSilent = isSilent
    )

    override fun onCleared() {
        super.onCleared()
        stopWaitingTimer()
        stopStallWatch()
        clearLegs()
        releaseConnection()
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// DATA CLASSES
// ═══════════════════════════════════════════════════════════════════════════

data class FareBreakdown(
    val baseFare: Double = 0.0,
    val waitingCharge: Double = 0.0,
    val platformFee: Double = 0.0,
    val gst: Double = 0.0,
    val discount: Double = 0.0,
    val totalFare: Double = 0.0
)

data class WaitingTimerState(
    val isActive: Boolean = false,
    val totalWaitingSeconds: Int = 0,
    val freeSecondsRemaining: Int = FareDetails.DEFAULT_FREE_WAITING_MINS * 60,
    val isFreeWaitingOver: Boolean = false,
    val extraMinutesCharged: Int = 0,
    val waitingCharge: Double = 0.0,
    val currentMinuteSeconds: Int = 0,
    val chargePerMinute: Double = FareDetails.DEFAULT_CHARGE_PER_MIN,
    val totalFreeSeconds: Int = FareDetails.DEFAULT_FREE_WAITING_MINS * 60
) {
    val freeTimeFormatted: String
        get() = String.format(
            Locale.US, "%d:%02d", freeSecondsRemaining / 60, freeSecondsRemaining % 60
        )
    val totalTimeFormatted: String
        get() = String.format(
            Locale.US, "%d:%02d", totalWaitingSeconds / 60, totalWaitingSeconds % 60
        )
    val freeWaitingProgress: Float
        get() = if (totalFreeSeconds > 0) {
            1f - (freeSecondsRemaining.toFloat() / totalFreeSeconds)
        } else 1f
}

data class RatingUiState(
    val showRatingDialog: Boolean = false,
    val bookingId: String = "",
    val driverName: String = "",
    val driverPhoto: String? = null,
    val vehicleType: String? = null,
    val totalFare: Double = 0.0,
    val waitingCharge: Double = 0.0,
    val isSubmitting: Boolean = false
)

data class PostDeliveryPaymentState(
    val showPaymentScreen: Boolean = false,
    val bookingId: String = "",
    val baseFare: Double = 0.0,
    val waitingCharge: Double = 0.0,
    val platformFee: Double = 0.0,
    val gst: Double = 0.0,
    val discount: Double = 0.0,
    val totalFare: Double = 0.0,
    val driverName: String = "",
    val paymentMethod: String = "cash",
    val isPaymentCompleted: Boolean = false,
    val isVerifyingPayment: Boolean = false
)

sealed class RiderTrackingNavigationEvent {
    data class RiderAssigned(
        val bookingId: String, val rider: RiderInfo, val otp: String?
    ) : RiderTrackingNavigationEvent()

    data class RiderEnroute(val bookingId: String) : RiderTrackingNavigationEvent()
    data class RiderArrived(val bookingId: String, val message: String) : RiderTrackingNavigationEvent()
    data class ParcelPickedUp(val bookingId: String) : RiderTrackingNavigationEvent()
    data class Delivered(val bookingId: String) : RiderTrackingNavigationEvent()
    data class NoRiderAvailable(val message: String) : RiderTrackingNavigationEvent()
    data class BookingCancelled(val reason: String) : RiderTrackingNavigationEvent()
    data class DriverCancelledRetrySearch(val message: String) : RiderTrackingNavigationEvent()
    data class PaymentConfirmed(val bookingId: String) : RiderTrackingNavigationEvent()
    object NavigateToHome : RiderTrackingNavigationEvent()
}
