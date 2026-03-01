package com.mobitechs.parcelwala.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.mobitechs.parcelwala.data.manager.ActiveBookingManager
import com.mobitechs.parcelwala.data.manager.BookingStatus
import com.mobitechs.parcelwala.data.model.realtime.BookingStatusType
import com.mobitechs.parcelwala.data.model.realtime.BookingStatusUpdate
import com.mobitechs.parcelwala.data.model.realtime.RealTimeConnectionState
import com.mobitechs.parcelwala.data.model.realtime.RiderInfo
import com.mobitechs.parcelwala.data.model.realtime.RiderLocationUpdate
import com.mobitechs.parcelwala.data.model.response.FareDetails
import com.mobitechs.parcelwala.data.model.response.formatRupee
import com.mobitechs.parcelwala.data.repository.BookingRepository
import com.mobitechs.parcelwala.data.repository.DirectionsRepository
import com.mobitechs.parcelwala.data.repository.RealTimeRepository
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
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RiderTrackingViewModel @Inject constructor(
    private val realTimeRepository: RealTimeRepository,
    private val activeBookingManager: ActiveBookingManager,
    private val notificationHelper: BookingNotificationHelper,
    private val bookingRepository: BookingRepository,
    private val directionsRepository: DirectionsRepository
) : ViewModel() {

    companion object {
        private const val TAG = "RiderTrackingVM"
        private const val ROUTE_FETCH_INTERVAL_MS = 30_000L
        private const val ASSUMED_SPEED_KMH = 25.0
        // ✅ Removed FREE_WAITING_SECONDS and CHARGE_PER_MINUTE — now dynamic from FareDetails API
    }

    // ═══════════════════════════════════════════════════════════════════════
    // STATE FLOWS
    // ═══════════════════════════════════════════════════════════════════════

    private val _uiState = MutableStateFlow(RiderTrackingUiState())
    val uiState: StateFlow<RiderTrackingUiState> = _uiState.asStateFlow()

    private val _assignedRider = MutableStateFlow<RiderInfo?>(null)
    val assignedRider: StateFlow<RiderInfo?> = _assignedRider.asStateFlow()

    private val _riderLocation = MutableStateFlow<RiderLocationUpdate?>(null)
    val riderLocation: StateFlow<RiderLocationUpdate?> = _riderLocation.asStateFlow()

    private val _bookingOtp = MutableStateFlow<String?>(null)
    val bookingOtp: StateFlow<String?> = _bookingOtp.asStateFlow()

    private val _deliveryOtp = MutableStateFlow<String?>(null)
    val deliveredOtp: StateFlow<String?> = _deliveryOtp.asStateFlow()

    private val _etaMinutes = MutableStateFlow<Int?>(null)
    val etaMinutes: StateFlow<Int?> = _etaMinutes.asStateFlow()

    private val _distanceKm = MutableStateFlow<Double?>(null)
    val distanceKm: StateFlow<Double?> = _distanceKm.asStateFlow()

    private val _driverToPickupRoute = MutableStateFlow<List<LatLng>>(emptyList())
    val driverToPickupRoute: StateFlow<List<LatLng>> = _driverToPickupRoute.asStateFlow()

    private val _pickupToDropRoute = MutableStateFlow<List<LatLng>>(emptyList())
    val pickupToDropRoute: StateFlow<List<LatLng>> = _pickupToDropRoute.asStateFlow()

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

    // ═══════════════════════════════════════════════════════════════════════
    // INTERNAL STATE
    // ═══════════════════════════════════════════════════════════════════════

    private var routeFetchJob: Job? = null
    private var waitingTimerJob: Job? = null
    private var initialDistanceMeters: Double? = null
    private var lastRouteFetchTime = 0L
    private var hasServerEta = false
    private var cachedBookingFare = 0.0

    private var freeWaitingSeconds: Int = FareDetails.DEFAULT_FREE_WAITING_MINS * 60
    private var chargePerMinute: Double = FareDetails.DEFAULT_CHARGE_PER_MIN


    // ═══════════════════════════════════════════════════════════════════════
    // COMPUTED PROPERTIES
    // ═══════════════════════════════════════════════════════════════════════

    private val currentStatus: BookingStatusType
        get() = _uiState.value.currentStatus

    val isPrePickupPhase: Boolean
        get() = currentStatus in PRE_PICKUP_STATUSES

    val isPostPickupPhase: Boolean
        get() = currentStatus in POST_PICKUP_STATUSES

    val canCancel: Boolean
        get() = isPrePickupPhase

    val showPickupOtp: Boolean
        get() = isPrePickupPhase && _bookingOtp.value != null

    val showDeliveryOtp: Boolean
        get() = isPostPickupPhase && _deliveryOtp.value != null

    val isDelivered: Boolean
        get() = currentStatus == BookingStatusType.DELIVERED

    // ═══════════════════════════════════════════════════════════════════════
    // INITIALIZATION
    // ═══════════════════════════════════════════════════════════════════════

    init {
        observeRealTimeUpdates()
    }

    private fun observeRealTimeUpdates() {
        collectFlow { realTimeRepository.bookingUpdates.collect { handleBookingStatusUpdate(it) } }
        collectFlow { realTimeRepository.riderLocationUpdates.collect { handleRiderLocationUpdate(it) } }
        collectFlow {
            realTimeRepository.bookingCancelled.collect { notification ->
                Log.d(TAG, "📥 BOOKING_CANCELLED: cancelledBy=${notification.cancelledBy} | reason=${notification.reason}")
            }
        }
        collectFlow {
            realTimeRepository.connectionState.collect { state ->
                when (state) {
                    is RealTimeConnectionState.Connected -> _uiState.update { it.copy(connectionError = null) }
                    is RealTimeConnectionState.Error -> _uiState.update { it.copy(connectionError = state.message) }
                    else -> {}
                }
            }
        }
        collectFlow {
            realTimeRepository.errors.collect { error ->
                Log.e(TAG, "❌ SignalR Error: ${error.message}")
                _toastMessage.emit(error.message)
            }
        }
    }

    /** Shorthand to launch a flow collection in viewModelScope */
    private fun collectFlow(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // FARE EXTRACTION
    // roundedFare = final customer amount (includes waiting, GST, discounts)
    // ═══════════════════════════════════════════════════════════════════════

    private fun extractFareFromUpdate(update: BookingStatusUpdate): FareBreakdown {
        val finalFare = update.roundedFare
            ?: update.totalFare
            ?: update.subTotal
            ?: update.baseFare
            ?: update.additionalData?.fare
            ?: cachedBookingFare.takeIf { it > 0.0 }
            ?: activeBookingManager.activeBooking.value?.fare
            ?: 0.0

        return FareBreakdown(
            baseFare = update.baseFare ?: 0.0,
            waitingCharge = update.waitingCharges?: 0.0,
            platformFee = update.platformFee ?: 0.0,
            gst = update.gstAmount ?: 0.0,
            discount = update.couponDiscount ?: 0.0,
            totalFare = finalFare
        )
    }

    /** Cache fare from any status update that carries fare data */
    private fun cacheFareIfAvailable(update: BookingStatusUpdate) {
        val fare = extractFareFromUpdate(update)
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
        Log.d(TAG, "💰 Cached fare: ₹${fare.totalFare}")
    }

    // ═══════════════════════════════════════════════════════════════════════
    // WAITING TIMER
    // ═══════════════════════════════════════════════════════════════════════

    private fun startWaitingTimer() {
        if (waitingTimerJob?.isActive == true) return
        Log.d(TAG, "⏱️ Starting waiting timer | Free: ${freeWaitingSeconds}s | Charge: ₹$chargePerMinute/min")

        _waitingState.value = WaitingTimerState(
            isActive = true,
            freeSecondsRemaining = freeWaitingSeconds,
            chargePerMinute = chargePerMinute,
            totalFreeSeconds = freeWaitingSeconds
        )

        waitingTimerJob = viewModelScope.launch {
            var elapsed = 0
            while (true) {
                delay(1000L)
                elapsed++
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
            }
        }
    }

    private fun stopWaitingTimer() {
        val finalState = _waitingState.value
        if (finalState.isActive) {
            Log.d(TAG, "⏱️ Timer stopped — Total: ${finalState.totalWaitingSeconds}s | Charge: ₹${finalState.waitingCharge}")
        }
        waitingTimerJob?.cancel()
        waitingTimerJob = null
        _waitingState.update { it.copy(isActive = false) }
    }

    fun getFinalWaitingCharge(): Double = _waitingState.value.waitingCharge

    fun getTotalFare(): Double {
        val baseFare = activeBookingManager.activeBooking.value?.fare ?: 0.0
        return baseFare + _waitingState.value.waitingCharge
    }

    // ═══════════════════════════════════════════════════════════════════════
    // POST-DELIVERY PAYMENT
    // ═══════════════════════════════════════════════════════════════════════

    fun onPaymentCompleted() {
        sendPaymentConfirmation("💳 Online")
    }

    fun onCashPaymentConfirmed() {
        sendPaymentConfirmation("💵 Cash")
    }

    private fun sendPaymentConfirmation(tag: String) {
        viewModelScope.launch {
            val bookingId = _paymentState.value.bookingId
            Log.d(TAG, "$tag payment confirmed for booking $bookingId")

            _paymentState.update {
                it.copy(showPaymentScreen = false, isPaymentCompleted = true, isVerifyingPayment = true)
            }

            val bookingIdInt = bookingId.toIntOrNull() ?: return@launch
            realTimeRepository.updateBookingStatus(bookingIdInt, Constants.SignalREvents.STATUS_PAYMENT_SUCCESS)
                .onSuccess { Log.d(TAG, "$tag SignalR: payment_success sent") }
                .onFailure { e ->
                    Log.e(TAG, "$tag SignalR: Failed to send payment_success: ${e.message}")
                    if (tag.contains("Cash")) _toastMessage.emit("Retrying payment confirmation...")
                }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // RATING
    // ═══════════════════════════════════════════════════════════════════════

    fun submitRating(bookingId: String, rating: Int, feedback: String?) {
        viewModelScope.launch {
            _ratingState.update { it.copy(isSubmitting = true) }
            try {
                bookingRepository.submitRating(bookingId, rating, feedback)
                    .onSuccess {
                        _ratingState.update { it.copy(isSubmitting = false, isSubmitted = true) }
                        Log.d(TAG, "⭐ Rating submitted: $rating stars")
                        delay(2000)
                        navigateHomeAfterCompletion()
                    }
                    .onFailure { e ->
                        Log.e(TAG, "⭐ Rating failed: ${e.message}")
                        if (e.message?.contains("already rated", ignoreCase = true) == true) {
                            navigateHomeAfterCompletion()
                        } else {
                            _ratingState.update { it.copy(isSubmitting = false, error = e.message) }
                        }
                    }
            } catch (e: Exception) {
                _ratingState.update { it.copy(isSubmitting = false, error = e.message) }
            }
        }
    }

    fun skipRating() { viewModelScope.launch { navigateHomeAfterCompletion() } }
    fun dismissRating() { _ratingState.value = RatingUiState() }
    fun onRatingCompleted() { viewModelScope.launch { navigateHomeAfterCompletion() } }

    private suspend fun navigateHomeAfterCompletion() {
        _ratingState.value = RatingUiState()
        activeBookingManager.clearActiveBooking()
        _navigationEvent.emit(RiderTrackingNavigationEvent.NavigateToHome)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PUBLIC METHODS
    // ═══════════════════════════════════════════════════════════════════════

    fun connectToBooking(bookingId: String, pickupLatitude: Double, pickupLongitude: Double) {
        Log.d(TAG, "📡 Connecting to booking: $bookingId")
        _uiState.update { it.copy(currentBookingId = bookingId) }

        activeBookingManager.activeBooking.value?.let { booking ->
            if (booking.fare > 0) {
                cachedBookingFare = booking.fare
                Log.d(TAG, "💰 Cached booking fare: ${formatRupee(booking.fare)}")
            }
            _paymentState.update { it.copy(paymentMethod = booking.paymentMethod) }

            // ✅ NEW: Load dynamic waiting timer config from ActiveBooking
            freeWaitingSeconds = booking.freeWaitingSeconds
            chargePerMinute = booking.waitingChargePerMin
            Log.d(TAG, "⏱️ Waiting config: free=${booking.freeWaitingTimeMins}min | charge=₹${booking.waitingChargePerMin}/min")
        }

        realTimeRepository.connectAndSubscribe(
            bookingId = bookingId,
            pickupLatitude = pickupLatitude,
            pickupLongitude = pickupLongitude
        )
    }

    fun disconnect() {
        Log.d(TAG, "🔌 Disconnecting...")
        stopWaitingTimer()
        realTimeRepository.disconnect()
        clearState()
    }

    fun retrySearch() {
        val bookingId = _uiState.value.currentBookingId ?: return
        val activeBooking = activeBookingManager.activeBooking.value ?: return
        Log.d(TAG, "🔄 Retrying search...")

        _uiState.update {
            it.copy(currentStatus = BookingStatusType.SEARCHING, statusMessage = "Looking for nearby riders...", isNoRiderAvailable = false)
        }
        realTimeRepository.connectAndSubscribe(
            bookingId = bookingId, pickupLatitude= activeBooking.pickupAddress.latitude, pickupLongitude= activeBooking.pickupAddress.longitude)
        activeBookingManager.retrySearch()
    }

    fun cancelBooking(reason: String) {
        viewModelScope.launch {
            val bookingId = _uiState.value.currentBookingId?.toIntOrNull()
            Log.d(TAG, "🚫 cancelBooking() — bookingId=$bookingId, reason=$reason")

            if (bookingId != null && bookingId > 0) {
                realTimeRepository.cancelBooking(bookingId, reason)
                    .onSuccess { Log.d(TAG, "✅ Cancel SUCCESS for booking $bookingId") }
                    .onFailure { error ->
                        Log.e(TAG, "❌ Cancel FAILED: ${error.message}")
                        cleanupAndNavigate(error.message ?: "Booking cancelled")
                    }
            } else {
                cleanupAndNavigate("Booking cancelled")
            }
        }
    }

    /** Common cleanup for cancellation failures and invalid booking IDs */
    private suspend fun cleanupAndNavigate(reason: String) {
        activeBookingManager.clearActiveBooking()
        realTimeRepository.disconnect()
        _navigationEvent.emit(RiderTrackingNavigationEvent.BookingCancelled(reason))
    }

    fun clearState() {
        _assignedRider.value = null
        _riderLocation.value = null
        _bookingOtp.value = null
        _deliveryOtp.value = null
        _etaMinutes.value = null
        _distanceKm.value = null
        _driverToPickupRoute.value = emptyList()
        _pickupToDropRoute.value = emptyList()
        _waitingState.value = WaitingTimerState()
        _ratingState.value = RatingUiState()
        _paymentState.value = PostDeliveryPaymentState()
        _uiState.value = RiderTrackingUiState()
        routeFetchJob?.cancel()
        waitingTimerJob = null
        cachedBookingFare = 0.0
        freeWaitingSeconds = FareDetails.DEFAULT_FREE_WAITING_MINS * 60
        chargePerMinute = FareDetails.DEFAULT_CHARGE_PER_MIN
        initialDistanceMeters = null
        lastRouteFetchTime = 0L
        hasServerEta = false
    }

    // ═══════════════════════════════════════════════════════════════════════
    // STATUS UPDATE HANDLER
    // ═══════════════════════════════════════════════════════════════════════

    private fun handleBookingStatusUpdate(update: BookingStatusUpdate) {
        val status = update.getStatusType()
        logStatusUpdate(update, status)

        restoreRiderIfNeeded(update)
        cacheFareIfAvailable(update)
        _uiState.update { it.copy(currentStatus = status, statusMessage = update.message) }

        viewModelScope.launch {
            when (status) {
                BookingStatusType.SEARCHING -> activeBookingManager.updateStatus(BookingStatus.SEARCHING)
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
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // STATUS HANDLERS
    // ═══════════════════════════════════════════════════════════════════════

    private suspend fun handleDriverAssigned(update: BookingStatusUpdate) {
        val rider = resolveRiderFromUpdate(update)
        _assignedRider.value = rider
        _bookingOtp.value = update.pickupOtp

        val driverLat = update.driverLatitude ?: rider.currentLatitude
        val driverLng = update.driverLongitude ?: rider.currentLongitude
        val pickup = getPickupLatLng()
        val drop = getDropLatLng()

        if (isValidLatLng(driverLat, driverLng) && isValidLatLng(pickup)) {
            val distMeters = haversineDistance(driverLat, driverLng, pickup.first, pickup.second)
            _distanceKm.value = distMeters / 1000.0
            initialDistanceMeters = distMeters
            _etaMinutes.value = update.etaMinutes ?: rider.etaMinutes ?: estimateEtaFromDistance(distMeters)
            fetchRoute(driverLat, driverLng, pickup.first, pickup.second, isDriverToPickup = true)
        } else {
            _etaMinutes.value = update.etaMinutes ?: rider.etaMinutes
        }

        if (isValidLatLng(pickup) && isValidLatLng(drop)) {
            fetchRoute(pickup.first, pickup.second, drop.first, drop.second, isDriverToPickup = false)
        }

        activeBookingManager.updateStatus(BookingStatus.RIDER_ASSIGNED)
        cacheBookingFareIfNeeded()

        showNotification(
            update.bookingId, "Driver Assigned!",
            buildString {
                append(rider.riderName); append(" is on the way")
                _etaMinutes.value?.let { if (it > 0) append("\n⏱️ Arriving in ~$it min") }
                rider.vehicleType?.let { append("\n🚚 $it") }
                rider.vehicleNumber.takeIf { it.isNotEmpty() }?.let { append(" • $it") }
                update.pickupOtp?.let { append("\n🔐 Pickup OTP: $it") }
            }
        )

        _navigationEvent.emit(
            RiderTrackingNavigationEvent.RiderAssigned(update.bookingId.toString(), rider, update.pickupOtp)
        )
    }

    private suspend fun handleRiderEnroute(update: BookingStatusUpdate) {
        activeBookingManager.updateStatus(BookingStatus.RIDER_EN_ROUTE)
        showNotification(
            update.bookingId, "Driver on the way",
            buildString {
                append(riderName); append(" is heading to pickup")
                _etaMinutes.value?.let { if (it > 0) append("\n⏱️ ~$it min away") }
            }
        )
        _navigationEvent.emit(RiderTrackingNavigationEvent.RiderEnroute(update.bookingId.toString()))
    }

    private suspend fun handleDriverArrived(update: BookingStatusUpdate) {
        activeBookingManager.updateStatus(BookingStatus.RIDER_EN_ROUTE)
        startWaitingTimer()
        _etaMinutes.value = 0
        _distanceKm.value = 0.0

        showNotification(
            update.bookingId, "📍 Driver Has Arrived!",
            buildString {
                append(riderName); append(" is at your pickup location")
                pickupOtpDisplay?.let { append("\n🔐 Share OTP: $it") }
            }
        )
        _toastMessage.emit(update.message ?: "Rider has arrived at pickup!")
        _navigationEvent.emit(
            RiderTrackingNavigationEvent.RiderArrived(update.bookingId.toString(), update.message ?: "Rider has arrived!")
        )
    }

    private suspend fun handleParcelPickedUp(update: BookingStatusUpdate) {
        activeBookingManager.updateStatus(BookingStatus.IN_TRANSIT)
        val finalCharge = getFinalWaitingCharge()
        stopWaitingTimer()
        Log.d(TAG, "💰 Final waiting charge at pickup: ₹$finalCharge")

        update.deliveredOtp?.let { _deliveryOtp.value = it }

        initialDistanceMeters = null
        val pickup = getPickupLatLng()
        val drop = getDropLatLng()

        if (isValidLatLng(pickup) && isValidLatLng(drop)) {
            val distMeters = haversineDistance(pickup.first, pickup.second, drop.first, drop.second)
            _distanceKm.value = distMeters / 1000.0
            _etaMinutes.value = estimateEtaFromDistance(distMeters)
            initialDistanceMeters = distMeters
            fetchRoute(pickup.first, pickup.second, drop.first, drop.second, isDriverToPickup = false)
        } else {
            _etaMinutes.value = null
            _distanceKm.value = null
        }

        val dropAddress = activeBookingManager.activeBooking.value?.dropAddress?.address
        showNotification(
            update.bookingId, "📦 Parcel Picked Up!",
            buildString {
                append("Your parcel is on the way to delivery")
                dropAddress?.let { append("\n📍 To: ${it.take(50)}${if (it.length > 50) "..." else ""}") }
                _deliveryOtp.value?.let { append("\n🔐 Delivery OTP: $it") }
            }
        )
        _toastMessage.emit("Parcel picked up!")
        _navigationEvent.emit(RiderTrackingNavigationEvent.ParcelPickedUp(update.bookingId.toString()))
    }

    private suspend fun handleInTransit(update: BookingStatusUpdate) {
        activeBookingManager.updateStatus(BookingStatus.IN_TRANSIT)
        showNotification(update.bookingId, "Parcel in transit", "Your parcel is on the way to delivery")
    }

    private suspend fun handleArrivedAtDelivery(update: BookingStatusUpdate) {
        activeBookingManager.updateStatus(BookingStatus.ARRIVED_DELIVERY)
        update.deliveredOtp?.let { _deliveryOtp.value = it }

        val fare = extractFareFromUpdate(update)
        val paymentMethod = update.paymentMethod
            ?: activeBookingManager.activeBooking.value?.paymentMethod
            ?: "Cash"

        showNotification(
            update.bookingId, "🏠 Driver Arrived at Delivery!",
            buildString {
                append("Driver has arrived at delivery location")
                (_deliveryOtp.value ?: update.deliveredOtp)?.let { append("\n🔐 Delivery OTP: $it") }
                if (fare.totalFare > 0) append("\n💰 Total: ₹${fare.totalFare}")
                append("\n💳 Complete payment to proceed")
            }
        )
        _toastMessage.emit("Rider arrived at delivery location!")

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
                paymentMethod = paymentMethod
            )
        }

        _navigationEvent.emit(
            RiderTrackingNavigationEvent.ShowPaymentScreen(
                bookingId = update.bookingId.toString(),
                roundedFare = fare.totalFare,       // roundedFare = final customer amount
                waitingCharge = fare.waitingCharge,
                discount = fare.discount,
                driverName = riderName,
                paymentMethod = paymentMethod
            )
        )
    }

    private suspend fun handlePaymentSuccess(update: BookingStatusUpdate) {
        activeBookingManager.updateStatus(BookingStatus.PAYMENT_SUCCESS)
        Log.d(TAG, "💳 PAYMENT_SUCCESS received for booking ${update.bookingId}")

        _paymentState.update {
            it.copy(showPaymentScreen = false, isPaymentCompleted = true, isVerifyingPayment = true)
        }

        showNotification(update.bookingId, "💳 Payment Successful!", "Payment confirmed. Waiting for delivery confirmation...")
        _toastMessage.emit("Payment confirmed!")
    }

    private suspend fun handleDeliveryCompleted(update: BookingStatusUpdate) {
        activeBookingManager.updateStatus(BookingStatus.DELIVERED)
        stopWaitingTimer()
        _paymentState.update { it.copy(isVerifyingPayment = false) }

        val fare = extractFareFromUpdate(update)
        val totalFare = fare.totalFare.takeIf { it > 0 } ?: _paymentState.value.totalFare
        val waitingCharge = fare.waitingCharge.takeIf { fare.totalFare > 0 } ?: _paymentState.value.waitingCharge

        showNotification(
            update.bookingId, "✅ Delivery Completed!",
            buildString {
                append("Your parcel has been delivered successfully!")
                if (totalFare > 0) append("\n💰 Total: ₹$totalFare")
            },
            isFinal = true
        )

        realTimeRepository.disconnect()
        _toastMessage.emit("Delivery completed!")

        _ratingState.update {
            it.copy(
                showRatingDialog = true,
                bookingId = update.bookingId.toString(),
                driverName = riderName,
                driverPhoto = _assignedRider.value?.photoUrl,
                vehicleType = _assignedRider.value?.vehicleType,
                totalFare = totalFare,
                waitingCharge = waitingCharge
            )
        }
    }

    private suspend fun handleNoRider(update: BookingStatusUpdate) {
        activeBookingManager.updateStatus(BookingStatus.SEARCH_TIMEOUT)
        _uiState.update { it.copy(isNoRiderAvailable = true) }
        _navigationEvent.emit(
            RiderTrackingNavigationEvent.NoRiderAvailable(update.message ?: "No riders available")
        )
    }

    private suspend fun handleCancelled(update: BookingStatusUpdate) {
        Log.d(TAG, "🚫 handleCancelled: cancelledBy=${update.cancelledBy} | reason=${update.cancellationReason}")
        stopWaitingTimer()

        if (update.cancelledBy?.lowercase() == "driver") {
            handleDriverCancelled(update)
        } else {
            handleCustomerOrSystemCancelled(update)
        }
    }

    private suspend fun handleDriverCancelled(update: BookingStatusUpdate) {
        Log.d(TAG, "📋 Driver cancelled, re-entering search mode")

        showNotification(
            update.bookingId, "Driver Cancelled",
            buildString {
                append("Driver cancelled the booking")
                update.cancellationReason?.takeIf { it.isNotBlank() }?.let { append("\nReason: $it") }
                append("\nSearching for another driver...")
            }
        )

        // Reset rider-related state only
        _assignedRider.value = null
        _riderLocation.value = null
        _bookingOtp.value = null
        _deliveryOtp.value = null
        _etaMinutes.value = null
        _distanceKm.value = null
        _driverToPickupRoute.value = emptyList()
        initialDistanceMeters = null
        hasServerEta = false

        _uiState.update {
            it.copy(currentStatus = BookingStatusType.SEARCHING, statusMessage = "Driver cancelled. Searching for another driver...")
        }
        activeBookingManager.retrySearch()

        val msg = update.message ?: "Driver cancelled, searching for another driver"
        _toastMessage.emit(msg)
        _navigationEvent.emit(RiderTrackingNavigationEvent.DriverCancelledRetrySearch(msg))
    }

    private suspend fun handleCustomerOrSystemCancelled(update: BookingStatusUpdate) {
        Log.d(TAG, "📋 Customer/system cancelled, navigating home")

        val cancelLabel = when (update.cancelledBy?.lowercase()) {
            "system" -> "Booking was cancelled by system"
            "customer" -> "You cancelled the booking"
            else -> "Booking has been cancelled"
        }

        showNotification(
            update.bookingId, "❌ Booking Cancelled",
            buildString {
                append(cancelLabel)
                update.cancellationReason?.takeIf { it.isNotBlank() }?.let { append("\nReason: $it") }
            },
            isFinal = true
        )

        activeBookingManager.updateStatus(BookingStatus.CANCELLED)
        activeBookingManager.clearActiveBooking()
        realTimeRepository.disconnect()

        val msg = update.message ?: "Booking cancelled"
        _toastMessage.emit(msg)
        _navigationEvent.emit(RiderTrackingNavigationEvent.BookingCancelled(msg))
    }

    // ═══════════════════════════════════════════════════════════════════════
    // LOCATION UPDATE HANDLER
    // ═══════════════════════════════════════════════════════════════════════

    private fun handleRiderLocationUpdate(location: RiderLocationUpdate) {
        val isPrePickup = isPrePickupPhase
        Log.d(TAG, "📍 LOCATION: ${location.latitude},${location.longitude} | ETA: ${location.etaMinutes}")

        _riderLocation.value = location

        val target = if (isPrePickup) getPickupLatLng() else getDropLatLng()
        val serverDistMeters = location.getRelevantDistanceMeters(isPrePickup)

        // Calculate distance
        val calcDistKm = if (isValidLatLng(target)) {
            haversineDistance(location.latitude, location.longitude, target.first, target.second) / 1000.0
        } else null

        val distanceKmValue = if (serverDistMeters != null && serverDistMeters > 0) {
            serverDistMeters / 1000.0
        } else calcDistKm

        distanceKmValue?.let {
            _distanceKm.value = it
            if (initialDistanceMeters == null) initialDistanceMeters = (it * 1000.0).coerceAtLeast(1000.0)
        }

        // Calculate ETA
        updateEta(location.etaMinutes, distanceKmValue)

        // Update route (throttled)
        if (isValidLatLng(target)) {
            val shouldFetchRoute = when {
                isPrePickup && currentStatus != BookingStatusType.ARRIVED -> true
                isPostPickupPhase -> true
                else -> false
            }
            if (shouldFetchRoute) {
                fetchRouteThrottled(location.latitude, location.longitude, target.first, target.second, isDriverToPickup = isPrePickup)
            }
        }

        // Update rider position
        _assignedRider.update { rider ->
            rider?.copy(
                currentLatitude = location.latitude,
                currentLongitude = location.longitude,
                etaMinutes = location.etaMinutes ?: rider.etaMinutes
            )
        }

        // Update notification (pre-pickup only, not arrived)
        if (isPrePickup && currentStatus != BookingStatusType.ARRIVED) {
            val bookingId = _uiState.value.currentBookingId ?: return
            notificationHelper.showStickyStatusNotification(
                bookingId = bookingId,
                title = "🚗 $riderName is on the way",
                body = buildString {
                    _distanceKm.value?.let { append("📍 ${formatDistance(it)} away") }
                    _etaMinutes.value?.let { if (it > 0) append(" • ~$it min") }
                    _bookingOtp.value?.let { append("\n🔐 OTP: $it") }
                },
                isSilent = true
            )
        }
    }

    private fun updateEta(serverEta: Int?, distanceKmValue: Double?) {
        val calculatedEta = distanceKmValue?.let { estimateEtaFromDistanceKm(it) }

        if (serverEta != null && serverEta > 0) {
            val actualDistKm = _distanceKm.value ?: distanceKmValue
            val minReasonableEta = actualDistKm?.let { ((it / 40.0) * 60.0).toInt().coerceAtLeast(1) }

            if (minReasonableEta == null || serverEta >= minReasonableEta) {
                _etaMinutes.value = serverEta
                hasServerEta = true
            } else {
                _etaMinutes.value = calculatedEta
                hasServerEta = false
            }
        } else {
            hasServerEta = false
            _etaMinutes.value = calculatedEta
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ROUTE FETCHING
    // ═══════════════════════════════════════════════════════════════════════

    private fun fetchRoute(fromLat: Double, fromLng: Double, toLat: Double, toLng: Double, isDriverToPickup: Boolean) {
        routeFetchJob?.cancel()
        routeFetchJob = viewModelScope.launch {
            try {
                directionsRepository.getRouteInfo(fromLat, fromLng, toLat, toLng)
                    .onSuccess { routeInfo ->
                        if (isDriverToPickup) _driverToPickupRoute.value = routeInfo.polylinePoints
                        else _pickupToDropRoute.value = routeInfo.polylinePoints

                        if (!hasServerEta) {
                            _distanceKm.value = routeInfo.distanceMeters / 1000.0
                            _etaMinutes.value = (routeInfo.durationSeconds / 60.0).toInt().coerceAtLeast(1)
                        }
                    }
                    .onFailure { e -> Log.w(TAG, "⚠️ Route fetch failed: ${e.message}") }
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ Route fetch error: ${e.message}")
            }
        }
    }

    private fun fetchRouteThrottled(fromLat: Double, fromLng: Double, toLat: Double, toLng: Double, isDriverToPickup: Boolean) {
        val now = System.currentTimeMillis()
        if (now - lastRouteFetchTime > ROUTE_FETCH_INTERVAL_MS) {
            lastRouteFetchTime = now
            fetchRoute(fromLat, fromLng, toLat, toLng, isDriverToPickup)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // COMMON HELPERS
    // ═══════════════════════════════════════════════════════════════════════

    /** Shorthand for current rider name */
    private val riderName: String
        get() = _assignedRider.value?.riderName ?: "Driver"

    /** Shorthand for pickup OTP display */
    private val pickupOtpDisplay: String?
        get() = _bookingOtp.value

    private fun getPickupLatLng(): Pair<Double, Double> {
        val addr = activeBookingManager.activeBooking.value?.pickupAddress
        return Pair(addr?.latitude ?: 0.0, addr?.longitude ?: 0.0)
    }

    private fun getDropLatLng(): Pair<Double, Double> {
        val addr = activeBookingManager.activeBooking.value?.dropAddress
        return Pair(addr?.latitude ?: 0.0, addr?.longitude ?: 0.0)
    }

    private fun isValidLatLng(lat: Double, lng: Double): Boolean = lat != 0.0 && lng != 0.0
    private fun isValidLatLng(pair: Pair<Double, Double>): Boolean = pair.first != 0.0 && pair.second != 0.0

    private fun estimateEtaFromDistance(distMeters: Double): Int =
        ((distMeters / 1000.0) / ASSUMED_SPEED_KMH * 60.0).toInt().coerceAtLeast(1)

    private fun estimateEtaFromDistanceKm(distKm: Double): Int =
        ((distKm / ASSUMED_SPEED_KMH) * 60.0).toInt().coerceAtLeast(1)

    private fun resolveRiderFromUpdate(update: BookingStatusUpdate): RiderInfo {
        return update.rider ?: RiderInfo(
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
    }

    private fun restoreRiderIfNeeded(update: BookingStatusUpdate) {
        if (_assignedRider.value != null || update.driverName.isNullOrEmpty()) return
        Log.d(TAG, "🔄 Restoring rider from status update: ${update.driverName}")
        _assignedRider.value = resolveRiderFromUpdate(update)
        update.pickupOtp?.let { _bookingOtp.value = it }
        update.deliveredOtp?.let { _deliveryOtp.value = it }
    }

    private fun cacheBookingFareIfNeeded() {
        if (cachedBookingFare > 0) return
        activeBookingManager.activeBooking.value?.fare?.takeIf { it > 0 }?.let {
            cachedBookingFare = it
            Log.d(TAG, "💰 Cached booking fare: ₹$it")
        }
    }

    private fun showNotification(
        bookingId: Int, title: String, body: String,
        isFinal: Boolean = false, isSilent: Boolean = false
    ) {
        notificationHelper.showStickyStatusNotification(
            bookingId = bookingId.toString(),
            title = title,
            body = body,
            isFinal = isFinal,
            isSilent = isSilent
        )
    }

    private fun logStatusUpdate(update: BookingStatusUpdate, status: BookingStatusType) {
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "📥 STATUS: $status | Driver: ${update.driverName} | ETA: ${update.etaMinutes}min")
        Log.d(TAG, "  Fare: total=${update.totalFare} | rounded=${update.roundedFare} | base=${update.baseFare} | waiting=${update.waitingCharges}")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    }

    private fun Int.toDoubleOrZero(): Double = this.toDouble()

    fun formatDistance(distanceKm: Double?): String {
        if (distanceKm == null) return ""
        val meters = (distanceKm * 1000).toInt()
        return if (meters < 1000) "$meters m" else String.format("%.1f km", distanceKm)
    }

    fun getStatusDisplayText(): String = when (currentStatus) {
        BookingStatusType.RIDER_ASSIGNED, BookingStatusType.RIDER_ENROUTE -> "Driver is on the way"
        BookingStatusType.ARRIVED -> "Driver has arrived"
        BookingStatusType.PICKED_UP -> "Parcel picked up"
        BookingStatusType.IN_TRANSIT -> "On the way to delivery"
        BookingStatusType.ARRIVED_DELIVERY -> "Arrived at delivery"
        BookingStatusType.PAYMENT_SUCCESS -> "Payment confirmed"
        BookingStatusType.DELIVERED -> "Delivery completed"
        else -> "Tracking your delivery"
    }

    private fun haversineDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val R = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLng / 2) * Math.sin(dLng / 2)
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    }

    override fun onCleared() {
        super.onCleared()
        stopWaitingTimer()
        realTimeRepository.disconnect()
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// CONSTANTS
// ═══════════════════════════════════════════════════════════════════════════

private val PRE_PICKUP_STATUSES = setOf(
    BookingStatusType.RIDER_ASSIGNED,
    BookingStatusType.RIDER_ENROUTE,
    BookingStatusType.ARRIVED
)

private val POST_PICKUP_STATUSES = setOf(
    BookingStatusType.PICKED_UP,
    BookingStatusType.IN_TRANSIT,
    BookingStatusType.ARRIVED_DELIVERY
)

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
)data class WaitingTimerState(
    val isActive: Boolean = false,
    val totalWaitingSeconds: Int = 0,
    val freeSecondsRemaining: Int = FareDetails.DEFAULT_FREE_WAITING_MINS * 60,
    val isFreeWaitingOver: Boolean = false,
    val extraMinutesCharged: Int = 0,
    val waitingCharge: Double = 0.0, // ✅ Int → Double
    val currentMinuteSeconds: Int = 0,
    // ✅ Dynamic config from API
    val chargePerMinute: Double = FareDetails.DEFAULT_CHARGE_PER_MIN,
    val totalFreeSeconds: Int = FareDetails.DEFAULT_FREE_WAITING_MINS * 60
) {
    val freeTimeFormatted: String
        get() = "%d:%02d".format(freeSecondsRemaining / 60, freeSecondsRemaining % 60)
    val totalTimeFormatted: String
        get() = "%d:%02d".format(totalWaitingSeconds / 60, totalWaitingSeconds % 60)
    val freeWaitingProgress: Float
        get() = if (totalFreeSeconds > 0) 1f - (freeSecondsRemaining.toFloat() / totalFreeSeconds) else 1f
}

data class RatingUiState(
    val showRatingDialog: Boolean = false,
    val bookingId: String = "",
    val driverName: String = "",
    val driverPhoto: String? = null,
    val vehicleType: String? = null,
    val totalFare: Double = 0.0, // ✅ Int → Double
    val waitingCharge: Double = 0.0, // ✅ Int → Double
    val isSubmitting: Boolean = false,
    val isSubmitted: Boolean = false,
    val error: String? = null
)

data class PostDeliveryPaymentState(
    val showPaymentScreen: Boolean = false,
    val bookingId: String = "",
    val baseFare: Double = 0.0, // ✅ Int → Double
    val waitingCharge: Double = 0.0, // ✅ Int → Double
    val platformFee: Double = 0.0, // ✅ Int → Double
    val gst: Double = 0.0, // ✅ Int → Double
    val discount: Double = 0.0, // ✅ Int → Double
    val totalFare: Double = 0.0, // ✅ Int → Double
    val driverName: String = "",
    val paymentMethod: String = "cash",
    val isPaymentCompleted: Boolean = false,
    val isVerifyingPayment: Boolean = false
)

data class RiderTrackingUiState(
    val currentBookingId: String? = null,
    val currentStatus: BookingStatusType = BookingStatusType.SEARCHING,
    val statusMessage: String? = null,
    val isNoRiderAvailable: Boolean = false,
    val connectionError: String? = null
)

sealed class RiderTrackingNavigationEvent {
    data class RiderAssigned(val bookingId: String, val rider: RiderInfo, val otp: String?) : RiderTrackingNavigationEvent()
    data class RiderEnroute(val bookingId: String) : RiderTrackingNavigationEvent()
    data class RiderArrived(val bookingId: String, val message: String) : RiderTrackingNavigationEvent()
    data class ParcelPickedUp(val bookingId: String) : RiderTrackingNavigationEvent()
    data class Delivered(val bookingId: String) : RiderTrackingNavigationEvent()
    data class NoRiderAvailable(val message: String) : RiderTrackingNavigationEvent()
    data class BookingCancelled(val reason: String) : RiderTrackingNavigationEvent()
    data class DriverCancelledRetrySearch(val message: String) : RiderTrackingNavigationEvent()
    data class ShowPaymentScreen(
        val bookingId: String,
        val roundedFare: Double,      // ✅ final amount customer pays
        val waitingCharge: Double,
        val discount: Double,          // ✅ coupon discount (0.0 if none)
        val driverName: String,
        val paymentMethod: String
    ) : RiderTrackingNavigationEvent()
    object NavigateToHome : RiderTrackingNavigationEvent()
}