// ui/viewmodel/RiderTrackingViewModel.kt
// ✅ UPDATED: Full lifecycle tracking - Assigned → Arrived → PickedUp → InTransit → Delivered
// ✅ UPDATED: Phase-aware map (driver→pickup pre-pickup, driver→drop post-pickup)
// ✅ UPDATED: Waiting timer, rating integration, cancel logic per phase
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
import com.mobitechs.parcelwala.data.repository.BookingRepository
import com.mobitechs.parcelwala.data.repository.DirectionsRepository
import com.mobitechs.parcelwala.data.repository.RealTimeRepository
import com.mobitechs.parcelwala.utils.BookingNotificationHelper
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
        private const val INITIAL_MAX_DISTANCE_METERS = 10000.0

        // Waiting Charge Constants
        const val FREE_WAITING_SECONDS = 180
        const val CHARGE_PER_MINUTE = 3
    }

    // ═══════════════════════════════════════════════════════════════════════
    // UI STATE
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
    val deliveryOtp: StateFlow<String?> = _deliveryOtp.asStateFlow()

    private val _etaMinutes = MutableStateFlow<Int?>(null)
    val etaMinutes: StateFlow<Int?> = _etaMinutes.asStateFlow()

    private val _distanceKm = MutableStateFlow<Double?>(null)
    val distanceKm: StateFlow<Double?> = _distanceKm.asStateFlow()

    // Route Polylines
    private val _driverToPickupRoute = MutableStateFlow<List<LatLng>>(emptyList())
    val driverToPickupRoute: StateFlow<List<LatLng>> = _driverToPickupRoute.asStateFlow()

    private val _pickupToDropRoute = MutableStateFlow<List<LatLng>>(emptyList())
    val pickupToDropRoute: StateFlow<List<LatLng>> = _pickupToDropRoute.asStateFlow()

    private var routeFetchJob: Job? = null

    // Waiting Timer State
    private val _waitingState = MutableStateFlow(WaitingTimerState())
    val waitingState: StateFlow<WaitingTimerState> = _waitingState.asStateFlow()

    // Rating State
    private val _ratingState = MutableStateFlow(RatingUiState())
    val ratingState: StateFlow<RatingUiState> = _ratingState.asStateFlow()

    private var waitingTimerJob: Job? = null

    val connectionState: StateFlow<RealTimeConnectionState> = realTimeRepository.connectionState

    private val _navigationEvent = MutableSharedFlow<RiderTrackingNavigationEvent>()
    val navigationEvent: SharedFlow<RiderTrackingNavigationEvent> = _navigationEvent.asSharedFlow()

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    private var initialDistanceMeters: Double? = null
    private var lastNotifiedEta: Int? = null
    private var lastRouteFetchTime = 0L
    private val ROUTE_FETCH_INTERVAL = 30_000L
    private var hasServerEta = false

    // ═══════════════════════════════════════════════════════════════════════
    // COMPUTED PROPERTIES FOR UI
    // ═══════════════════════════════════════════════════════════════════════

    /** Whether we're in pre-pickup phase (driver heading to pickup) */
    val isPrePickupPhase: Boolean
        get() {
            val status = _uiState.value.currentStatus
            return status == BookingStatusType.RIDER_ASSIGNED ||
                    status == BookingStatusType.RIDER_ENROUTE ||
                    status == BookingStatusType.ARRIVED
        }

    /** Whether parcel has been picked up (driver heading to drop) */
    val isPostPickupPhase: Boolean
        get() {
            val status = _uiState.value.currentStatus
            return status == BookingStatusType.PICKED_UP ||
                    status == BookingStatusType.IN_TRANSIT ||
                    status == BookingStatusType.ARRIVED_DELIVERY
        }

    /** Whether booking can be cancelled (only before pickup) */
    val canCancel: Boolean
        get() = isPrePickupPhase

    /** Whether OTP should be shown (only before pickup, not after) */
    val showPickupOtp: Boolean
        get() = isPrePickupPhase && _bookingOtp.value != null

    /** Whether trip is completed */
    val isDelivered: Boolean
        get() = _uiState.value.currentStatus == BookingStatusType.DELIVERED

    // ═══════════════════════════════════════════════════════════════════════
    // INITIALIZATION
    // ═══════════════════════════════════════════════════════════════════════

    init {
        observeRealTimeUpdates()
    }

    private fun observeRealTimeUpdates() {
        viewModelScope.launch {
            realTimeRepository.bookingUpdates.collect { update ->
                handleBookingStatusUpdate(update)
            }
        }

        viewModelScope.launch {
            realTimeRepository.riderLocationUpdates.collect { location ->
                handleRiderLocationUpdate(location)
            }
        }

        viewModelScope.launch {
            realTimeRepository.bookingCancelled.collect { notification ->
                Log.d(TAG, "📥 BOOKING CANCELLED: ${notification.cancelledBy} - ${notification.reason}")

                notificationHelper.showStickyStatusNotification(
                    bookingId = notification.bookingId.toString(),
                    title = "Booking Cancelled",
                    body = buildString {
                        when (notification.cancelledBy?.lowercase()) {
                            "driver" -> append("Driver cancelled the booking")
                            "system" -> append("Booking was cancelled by system")
                            else -> append("Booking has been cancelled")
                        }
                        notification.reason?.takeIf { it.isNotBlank() }?.let {
                            append("\nReason: $it")
                        }
                    },
                    isFinal = true
                )

                stopWaitingTimer()
                activeBookingManager.clearActiveBooking()
                realTimeRepository.disconnect()

                _toastMessage.emit(notification.message)
                _navigationEvent.emit(RiderTrackingNavigationEvent.BookingCancelled(notification.message))
            }
        }

        viewModelScope.launch {
            realTimeRepository.connectionState.collect { state ->
                when (state) {
                    is RealTimeConnectionState.Connected -> {
                        _uiState.update { it.copy(connectionError = null) }
                    }
                    is RealTimeConnectionState.Error -> {
                        _uiState.update { it.copy(connectionError = state.message) }
                    }
                    else -> {}
                }
            }
        }

        viewModelScope.launch {
            realTimeRepository.errors.collect { error ->
                Log.e(TAG, "❌ SignalR Error: ${error.message}")
                _toastMessage.emit(error.message)
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // WAITING TIMER
    // ═══════════════════════════════════════════════════════════════════════

    private fun startWaitingTimer() {
        if (waitingTimerJob?.isActive == true) return

        Log.d(TAG, "⏱️ Starting waiting timer")

        _waitingState.value = WaitingTimerState(
            isActive = true,
            totalWaitingSeconds = 0,
            freeSecondsRemaining = FREE_WAITING_SECONDS,
            isFreeWaitingOver = false,
            extraMinutesCharged = 0,
            waitingCharge = 0
        )

        waitingTimerJob = viewModelScope.launch {
            var elapsedSeconds = 0
            while (true) {
                delay(1000L)
                elapsedSeconds++

                val freeRemaining = (FREE_WAITING_SECONDS - elapsedSeconds).coerceAtLeast(0)
                val isFreeOver = elapsedSeconds > FREE_WAITING_SECONDS
                val extraSeconds = if (isFreeOver) elapsedSeconds - FREE_WAITING_SECONDS else 0
                val extraMinutes = extraSeconds / 60
                val charge = extraMinutes * CHARGE_PER_MINUTE

                _waitingState.value = WaitingTimerState(
                    isActive = true,
                    totalWaitingSeconds = elapsedSeconds,
                    freeSecondsRemaining = freeRemaining,
                    isFreeWaitingOver = isFreeOver,
                    extraMinutesCharged = extraMinutes,
                    waitingCharge = charge,
                    currentMinuteSeconds = if (isFreeOver) extraSeconds % 60 else 0
                )

                if (elapsedSeconds % 30 == 0) {
                    Log.d(TAG, "⏱️ Waiting: ${elapsedSeconds}s | Free: ${freeRemaining}s | Charge: ₹$charge")
                }
            }
        }
    }

    private fun stopWaitingTimer() {
        waitingTimerJob?.cancel()
        waitingTimerJob = null
        val finalState = _waitingState.value
        if (finalState.isActive) {
            Log.d(TAG, "⏱️ Timer stopped - Total: ${finalState.totalWaitingSeconds}s | Charge: ₹${finalState.waitingCharge}")
        }
        _waitingState.update { it.copy(isActive = false) }
    }

    fun getFinalWaitingCharge(): Int = _waitingState.value.waitingCharge

    fun getTotalFare(): Int {
        val baseFare = activeBookingManager.activeBooking.value?.fare ?: 0
        return baseFare + _waitingState.value.waitingCharge
    }

    // ═══════════════════════════════════════════════════════════════════════
    // RATING
    // ═══════════════════════════════════════════════════════════════════════

    fun submitRating(bookingId: String, rating: Int, feedback: String?) {
        viewModelScope.launch {
            _ratingState.update { it.copy(isSubmitting = true) }
            try {
                val result = bookingRepository.submitRating(bookingId, rating, feedback)
                result.onSuccess {
                    _ratingState.update { it.copy(isSubmitting = false, isSubmitted = true) }
                    Log.d(TAG, "⭐ Rating submitted: $rating stars")

                    delay(2000)
                    _ratingState.value = RatingUiState()
                    activeBookingManager.clearActiveBooking()
                    _navigationEvent.emit(RiderTrackingNavigationEvent.NavigateToHome)
                }.onFailure { e ->
                    Log.e(TAG, "⭐ Rating failed: ${e.message}")
                    if (e.message?.contains("already rated", ignoreCase = true) == true) {
                        _ratingState.value = RatingUiState()
                        activeBookingManager.clearActiveBooking()
                        _navigationEvent.emit(RiderTrackingNavigationEvent.NavigateToHome)
                    } else {
                        _ratingState.update { it.copy(isSubmitting = false, error = e.message) }
                    }
                }
            } catch (e: Exception) {
                _ratingState.update { it.copy(isSubmitting = false, error = e.message) }
            }
        }
    }

    fun skipRating() {
        viewModelScope.launch {
            _ratingState.value = RatingUiState()
            activeBookingManager.clearActiveBooking()
            _navigationEvent.emit(RiderTrackingNavigationEvent.NavigateToHome)
        }
    }

    fun dismissRating() {
        _ratingState.value = RatingUiState()
    }

    fun onRatingCompleted() {
        viewModelScope.launch {
            _ratingState.value = RatingUiState()
            activeBookingManager.clearActiveBooking()
            _navigationEvent.emit(RiderTrackingNavigationEvent.NavigateToHome)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PUBLIC METHODS
    // ═══════════════════════════════════════════════════════════════════════

    fun connectToBooking(
        bookingId: String,
        pickupLatitude: Double,
        pickupLongitude: Double
    ) {
        Log.d(TAG, "📡 Connecting to booking: $bookingId")
        _uiState.update { it.copy(currentBookingId = bookingId) }

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
            it.copy(
                currentStatus = BookingStatusType.SEARCHING,
                statusMessage = "Looking for nearby riders...",
                isNoRiderAvailable = false
            )
        }

        realTimeRepository.connectAndSubscribe(
            bookingId = bookingId,
            pickupLatitude = activeBooking.pickupAddress.latitude,
            pickupLongitude = activeBooking.pickupAddress.longitude
        )
        activeBookingManager.retrySearch()
    }

    fun cancelBooking(reason: String) {
        viewModelScope.launch {
            Log.d(TAG, "❌ Cancelling booking: $reason")
            stopWaitingTimer()

            val bookingId = _uiState.value.currentBookingId?.toIntOrNull()
                ?: activeBookingManager.activeBooking.value?.bookingId?.filter { it.isDigit() }?.toIntOrNull()

            if (bookingId != null && realTimeRepository.isConnected()) {
                realTimeRepository.cancelBooking(bookingId, reason)
            }

            notificationHelper.cancelAllNotifications()
            realTimeRepository.disconnect()
            activeBookingManager.clearActiveBooking()
            _navigationEvent.emit(RiderTrackingNavigationEvent.BookingCancelled(reason))
        }
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
        routeFetchJob?.cancel()
        _waitingState.value = WaitingTimerState()
        _ratingState.value = RatingUiState()
        initialDistanceMeters = null
        lastNotifiedEta = null
        lastRouteFetchTime = 0L
        hasServerEta = false
        waitingTimerJob = null
        _uiState.value = RiderTrackingUiState()
    }

    // ═══════════════════════════════════════════════════════════════════════
    // STATUS UPDATE HANDLER
    // ═══════════════════════════════════════════════════════════════════════

    private fun handleBookingStatusUpdate(update: BookingStatusUpdate) {
        val status = update.getStatusType()

        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "📥 STATUS UPDATE: $status")
        Log.d(TAG, "  Driver: ${update.driverName ?: "null"} | ETA: ${update.etaMinutes ?: update.rider?.etaMinutes ?: "null"}min")
        Log.d(TAG, "  Lat: ${update.driverLatitude} | Lng: ${update.driverLongitude}")
        Log.d(TAG, "  OTP: ${update.otp} | EstArrival: ${update.estimatedArrival} | BookingDist: ${update.distanceKm}")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        _uiState.update {
            it.copy(
                currentStatus = status,
                statusMessage = update.message
            )
        }

        viewModelScope.launch {
            when (status) {
                BookingStatusType.SEARCHING -> {
                    activeBookingManager.updateStatus(BookingStatus.SEARCHING)
                }
                BookingStatusType.RIDER_ASSIGNED -> handleDriverAssigned(update)
                BookingStatusType.RIDER_ENROUTE -> {
                    activeBookingManager.updateStatus(BookingStatus.RIDER_EN_ROUTE)
                    notificationHelper.showStickyStatusNotification(
                        bookingId = update.bookingId.toString(),
                        title = "Driver on the way",
                        body = buildString {
                            append(_assignedRider.value?.riderName ?: "Driver")
                            append(" is heading to pickup")
                            _etaMinutes.value?.let { if (it > 0) append("\n⏱️ ~$it min away") }
                        }
                    )
                    _navigationEvent.emit(
                        RiderTrackingNavigationEvent.RiderEnroute(update.bookingId.toString())
                    )
                }
                BookingStatusType.ARRIVED -> handleDriverArrived(update)
                BookingStatusType.PICKED_UP -> handleParcelPickedUp(update)
                BookingStatusType.IN_TRANSIT -> {
                    activeBookingManager.updateStatus(BookingStatus.IN_TRANSIT)
                    notificationHelper.showStickyStatusNotification(
                        bookingId = update.bookingId.toString(),
                        title = "Parcel in transit",
                        body = "Your parcel is on the way to delivery"
                    )
                }
                BookingStatusType.ARRIVED_DELIVERY -> handleArrivedAtDelivery(update)
                BookingStatusType.DELIVERED -> handleDeliveryCompleted(update)
                BookingStatusType.NO_RIDER -> {
                    activeBookingManager.updateStatus(BookingStatus.SEARCH_TIMEOUT)
                    _uiState.update { it.copy(isNoRiderAvailable = true) }
                    _navigationEvent.emit(
                        RiderTrackingNavigationEvent.NoRiderAvailable(
                            message = update.message ?: "No riders available"
                        )
                    )
                }
                BookingStatusType.CANCELLED -> handleCancelled(update)
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // STATUS HANDLERS
    // ═══════════════════════════════════════════════════════════════════════

    private suspend fun handleDriverAssigned(update: BookingStatusUpdate) {
        val rider = update.rider

        if (rider != null) {
            _assignedRider.value = rider
            Log.d(TAG, "✅ Rider assigned: ${rider.riderName}")
        } else {
            Log.w(TAG, "⚠️ No rider object, creating from update fields")
            _assignedRider.value = RiderInfo(
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

        _bookingOtp.value = update.otp

        // Calculate DRIVER → PICKUP distance
        val driverLat = update.driverLatitude ?: rider?.currentLatitude ?: 0.0
        val driverLng = update.driverLongitude ?: rider?.currentLongitude ?: 0.0
        val pickupLat = activeBookingManager.activeBooking.value?.pickupAddress?.latitude ?: 0.0
        val pickupLng = activeBookingManager.activeBooking.value?.pickupAddress?.longitude ?: 0.0

        if (driverLat != 0.0 && driverLng != 0.0 && pickupLat != 0.0 && pickupLng != 0.0) {
            val distMeters = haversineDistance(driverLat, driverLng, pickupLat, pickupLng)
            _distanceKm.value = distMeters / 1000.0
            initialDistanceMeters = distMeters
            Log.d(TAG, "📏 Driver→Pickup distance: ${formatDistance(distMeters / 1000.0)}")

            val serverEta = update.etaMinutes ?: rider?.etaMinutes
            if (serverEta != null && serverEta > 0) {
                _etaMinutes.value = serverEta
            } else {
                val estimatedEta = ((distMeters / 1000.0) / 25.0 * 60.0).toInt().coerceAtLeast(1)
                _etaMinutes.value = estimatedEta
            }

            fetchRoute(driverLat, driverLng, pickupLat, pickupLng, isDriverToPickup = true)
        } else {
            val serverEta = update.etaMinutes ?: rider?.etaMinutes
            _etaMinutes.value = serverEta
        }

        // Pre-fetch pickup → drop route for later use
        val dropLat = activeBookingManager.activeBooking.value?.dropAddress?.latitude ?: 0.0
        val dropLng = activeBookingManager.activeBooking.value?.dropAddress?.longitude ?: 0.0
        if (pickupLat != 0.0 && pickupLng != 0.0 && dropLat != 0.0 && dropLng != 0.0) {
            fetchRoute(pickupLat, pickupLng, dropLat, dropLng, isDriverToPickup = false)
        }

        activeBookingManager.updateStatus(BookingStatus.RIDER_ASSIGNED)

        val eta = _etaMinutes.value
        notificationHelper.showStickyStatusNotification(
            bookingId = update.bookingId.toString(),
            title = "Driver Assigned!",
            body = buildString {
                append(update.driverName ?: rider?.riderName ?: "Your driver")
                append(" is on the way")
                eta?.let { if (it > 0) append("\n⏱️ Arriving in ~$it min") }
                (update.vehicleType ?: rider?.vehicleType)?.let { append("\n🚚 $it") }
                (update.vehicleNumber ?: rider?.vehicleNumber)?.let { append(" • $it") }
                update.otp?.let { append("\n🔐 Pickup OTP: $it") }
            }
        )

        _navigationEvent.emit(
            RiderTrackingNavigationEvent.RiderAssigned(
                bookingId = update.bookingId.toString(),
                rider = _assignedRider.value!!,
                otp = update.otp
            )
        )
    }

    private suspend fun handleDriverArrived(update: BookingStatusUpdate) {
        activeBookingManager.updateStatus(BookingStatus.RIDER_EN_ROUTE)

        // START WAITING TIMER
        startWaitingTimer()

        // Clear ETA/distance since driver is at pickup
        _etaMinutes.value = 0
        _distanceKm.value = 0.0

        notificationHelper.showStickyStatusNotification(
            bookingId = update.bookingId.toString(),
            title = "📍 Driver Has Arrived!",
            body = buildString {
                append(_assignedRider.value?.riderName ?: update.driverName ?: "Your driver")
                append(" is at your pickup location")
                (_bookingOtp.value ?: update.otp)?.let { append("\n🔐 Share OTP: $it") }
            }
        )

        _toastMessage.emit(update.message ?: "Rider has arrived at pickup!")
        _navigationEvent.emit(
            RiderTrackingNavigationEvent.RiderArrived(
                bookingId = update.bookingId.toString(),
                message = update.message ?: "Rider has arrived!"
            )
        )
    }

    private suspend fun handleParcelPickedUp(update: BookingStatusUpdate) {
        activeBookingManager.updateStatus(BookingStatus.IN_TRANSIT)

        // STOP WAITING TIMER
        val finalCharge = getFinalWaitingCharge()
        stopWaitingTimer()
        Log.d(TAG, "💰 Final waiting charge at pickup: ₹$finalCharge")

        update.deliveryOtp?.let { _deliveryOtp.value = it }

        // Clear pickup OTP - no longer needed
        // _bookingOtp remains for reference but showPickupOtp will return false

        // Reset distance tracking for drop phase
        initialDistanceMeters = null

        val pickupLat = activeBookingManager.activeBooking.value?.pickupAddress?.latitude ?: 0.0
        val pickupLng = activeBookingManager.activeBooking.value?.pickupAddress?.longitude ?: 0.0
        val dropLat = activeBookingManager.activeBooking.value?.dropAddress?.latitude ?: 0.0
        val dropLng = activeBookingManager.activeBooking.value?.dropAddress?.longitude ?: 0.0

        if (pickupLat != 0.0 && dropLat != 0.0) {
            val distMeters = haversineDistance(pickupLat, pickupLng, dropLat, dropLng)
            _distanceKm.value = distMeters / 1000.0
            _etaMinutes.value = ((distMeters / 1000.0) / 25.0 * 60.0).toInt().coerceAtLeast(1)
            initialDistanceMeters = distMeters

            // Fetch road route for pickup→drop
            fetchRoute(pickupLat, pickupLng, dropLat, dropLng, isDriverToPickup = false)
        } else {
            _etaMinutes.value = null
            _distanceKm.value = null
        }

        val dropAddress = activeBookingManager.activeBooking.value?.dropAddress?.address

        notificationHelper.showStickyStatusNotification(
            bookingId = update.bookingId.toString(),
            title = "📦 Parcel Picked Up!",
            body = buildString {
                append("Your parcel is on the way to delivery")
                dropAddress?.let {
                    val shortAddr = if (it.length > 50) it.take(50) + "..." else it
                    append("\n📍 To: $shortAddr")
                }
            }
        )

        _toastMessage.emit("Parcel picked up!")
        _navigationEvent.emit(
            RiderTrackingNavigationEvent.ParcelPickedUp(update.bookingId.toString())
        )
    }

    private suspend fun handleArrivedAtDelivery(update: BookingStatusUpdate) {
        activeBookingManager.updateStatus(BookingStatus.IN_TRANSIT)

        notificationHelper.showStickyStatusNotification(
            bookingId = update.bookingId.toString(),
            title = "🏠 Arriving at Delivery!",
            body = buildString {
                append("Driver has arrived at delivery location")
                (_deliveryOtp.value ?: update.deliveryOtp)?.let { append("\n🔐 Delivery OTP: $it") }
            }
        )

        _toastMessage.emit("Rider arrived at delivery location!")
    }

    private suspend fun handleDeliveryCompleted(update: BookingStatusUpdate) {
        activeBookingManager.updateStatus(BookingStatus.DELIVERED)
        stopWaitingTimer()

        val baseFare = activeBookingManager.activeBooking.value?.fare ?: 0
        val waitingCharge = _waitingState.value.waitingCharge
        val totalFare = baseFare + waitingCharge

        notificationHelper.showStickyStatusNotification(
            bookingId = update.bookingId.toString(),
            title = "✅ Delivery Completed!",
            body = buildString {
                append("Your parcel has been delivered successfully!")
                append("\n💰 Total: ₹$totalFare")
                if (waitingCharge > 0) {
                    append(" (incl. ₹$waitingCharge waiting)")
                }
                append("\n⭐ Rate your experience")
            },
            isFinal = true
        )

        realTimeRepository.disconnect()
        _toastMessage.emit("Delivery completed!")

        // Show rating dialog
        _ratingState.update {
            it.copy(
                showRatingDialog = true,
                bookingId = update.bookingId.toString(),
                driverName = _assignedRider.value?.riderName ?: "Driver",
                driverPhoto = _assignedRider.value?.photoUrl,
                vehicleType = _assignedRider.value?.vehicleType,
                totalFare = totalFare,
                waitingCharge = waitingCharge
            )
        }

        _navigationEvent.emit(
            RiderTrackingNavigationEvent.Delivered(update.bookingId.toString())
        )
    }

    private suspend fun handleCancelled(update: BookingStatusUpdate) {
        stopWaitingTimer()

        notificationHelper.showStickyStatusNotification(
            bookingId = update.bookingId.toString(),
            title = "❌ Booking Cancelled",
            body = buildString {
                when (update.cancelledBy?.lowercase()) {
                    "driver" -> append("Driver cancelled the booking")
                    "system" -> append("Booking was cancelled by system")
                    "customer" -> append("You cancelled the booking")
                    else -> append("Booking has been cancelled")
                }
                update.cancellationReason?.takeIf { it.isNotBlank() }?.let {
                    append("\nReason: $it")
                }
            },
            isFinal = true
        )

        activeBookingManager.clearActiveBooking()
        realTimeRepository.disconnect()
        _toastMessage.emit(update.message ?: "Booking cancelled")
        _navigationEvent.emit(
            RiderTrackingNavigationEvent.BookingCancelled(update.message ?: "Booking cancelled")
        )
    }

    // ═══════════════════════════════════════════════════════════════════════
    // LOCATION UPDATE HANDLER
    // ═══════════════════════════════════════════════════════════════════════

    private fun handleRiderLocationUpdate(location: RiderLocationUpdate) {
        val isPrePickup = isPrePickupPhase

        val serverDistMeters = location.getRelevantDistanceMeters(isPrePickup)

        Log.d(TAG, "📍 LOCATION: ${location.latitude},${location.longitude} | ETA: ${location.etaMinutes} | Dist: ${serverDistMeters?.toInt()}m | toPickup: ${location.distanceToPickupKm} | toDrop: ${location.distanceToDropKm}")

        _riderLocation.value = location

        val currentStatus = _uiState.value.currentStatus
        val targetLat: Double
        val targetLng: Double

        if (isPrePickup) {
            targetLat = activeBookingManager.activeBooking.value?.pickupAddress?.latitude ?: 0.0
            targetLng = activeBookingManager.activeBooking.value?.pickupAddress?.longitude ?: 0.0
        } else {
            targetLat = activeBookingManager.activeBooking.value?.dropAddress?.latitude ?: 0.0
            targetLng = activeBookingManager.activeBooking.value?.dropAddress?.longitude ?: 0.0
        }

        val calcDistKm = if (targetLat != 0.0 && targetLng != 0.0) {
            haversineDistance(location.latitude, location.longitude, targetLat, targetLng) / 1000.0
        } else null

        val distanceKmValue = if (serverDistMeters != null && serverDistMeters > 0) {
            serverDistMeters / 1000.0
        } else {
            calcDistKm
        }

        distanceKmValue?.let {
            _distanceKm.value = it
            if (initialDistanceMeters == null) {
                initialDistanceMeters = (it * 1000.0).coerceAtLeast(1000.0)
            }
        }

        val calculatedEtaMin = distanceKmValue?.let {
            ((it / 25.0) * 60.0).toInt().coerceAtLeast(1)
        }

        val serverEta = location.etaMinutes

        if (serverEta != null && serverEta > 0) {
            val actualDistKm = _distanceKm.value ?: distanceKmValue
            if (actualDistKm != null && actualDistKm > 0) {
                val minReasonableEta = (actualDistKm / 40.0 * 60.0).toInt().coerceAtLeast(1)
                if (serverEta >= minReasonableEta) {
                    _etaMinutes.value = serverEta
                    hasServerEta = true
                } else {
                    _etaMinutes.value = calculatedEtaMin
                    hasServerEta = false
                }
            } else {
                _etaMinutes.value = serverEta
                hasServerEta = true
            }
        } else {
            hasServerEta = false
            _etaMinutes.value = calculatedEtaMin
        }

        // Update route polyline with latest driver position (throttled)
        if (isPrePickup && currentStatus != BookingStatusType.ARRIVED) {
            if (targetLat != 0.0 && targetLng != 0.0) {
                fetchRouteThrottled(location.latitude, location.longitude, targetLat, targetLng, isDriverToPickup = true)
            }
        } else if (isPostPickupPhase) {
            if (targetLat != 0.0 && targetLng != 0.0) {
                fetchRouteThrottled(location.latitude, location.longitude, targetLat, targetLng, isDriverToPickup = false)
            }
        }

        // Update rider info
        _assignedRider.update { rider ->
            rider?.copy(
                currentLatitude = location.latitude,
                currentLongitude = location.longitude,
                etaMinutes = location.etaMinutes ?: rider.etaMinutes
            )
        }

        // Update notification
        if (isPrePickup && currentStatus != BookingStatusType.ARRIVED) {
            val driverName = _assignedRider.value?.riderName
            val bookingId = _uiState.value.currentBookingId ?: return
            val distKm = _distanceKm.value
            val eta = _etaMinutes.value

            notificationHelper.showStickyStatusNotification(
                bookingId = bookingId,
                title = "🚗 ${driverName ?: "Driver"} is on the way",
                body = buildString {
                    distKm?.let { append("📍 ${formatDistance(it)} away") }
                    eta?.let { if (it > 0) append(" • ~$it min") }
                    _bookingOtp.value?.let { append("\n🔐 OTP: $it") }
                },
                isSilent = true
            )
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ROUTE FETCHING
    // ═══════════════════════════════════════════════════════════════════════

    private fun fetchRoute(
        fromLat: Double, fromLng: Double,
        toLat: Double, toLng: Double,
        isDriverToPickup: Boolean
    ) {
        routeFetchJob?.cancel()
        routeFetchJob = viewModelScope.launch {
            try {
                val result = directionsRepository.getRouteInfo(fromLat, fromLng, toLat, toLng)
                result.onSuccess { routeInfo ->
                    if (isDriverToPickup) {
                        _driverToPickupRoute.value = routeInfo.polylinePoints
                    } else {
                        _pickupToDropRoute.value = routeInfo.polylinePoints
                    }
                    Log.d(TAG, "🗺️ Route fetched: ${routeInfo.polylinePoints.size} points (${if (isDriverToPickup) "driver→pickup" else "pickup→drop"})")

                    if (!hasServerEta) {
                        _distanceKm.value = routeInfo.distanceMeters / 1000.0
                        _etaMinutes.value = (routeInfo.durationSeconds / 60.0).toInt().coerceAtLeast(1)
                    }
                }.onFailure { e ->
                    Log.w(TAG, "⚠️ Route fetch failed: ${e.message}")
                }
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ Route fetch error: ${e.message}")
            }
        }
    }

    private fun fetchRouteThrottled(
        fromLat: Double, fromLng: Double,
        toLat: Double, toLng: Double,
        isDriverToPickup: Boolean
    ) {
        val now = System.currentTimeMillis()
        if (now - lastRouteFetchTime > ROUTE_FETCH_INTERVAL) {
            lastRouteFetchTime = now
            fetchRoute(fromLat, fromLng, toLat, toLng, isDriverToPickup)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // UTILITY
    // ═══════════════════════════════════════════════════════════════════════

    private fun haversineDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val R = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLng / 2) * Math.sin(dLng / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return R * c
    }

    fun formatDistance(distanceKm: Double?): String {
        if (distanceKm == null) return ""
        val meters = (distanceKm * 1000).toInt()
        return if (meters < 1000) {
            "${meters} m"
        } else {
            String.format("%.1f km", distanceKm)
        }
    }

    /** Get status display text for the bottom sheet header */
    fun getStatusDisplayText(): String {
        return when (_uiState.value.currentStatus) {
            BookingStatusType.RIDER_ASSIGNED, BookingStatusType.RIDER_ENROUTE -> "Driver is on the way"
            BookingStatusType.ARRIVED -> "Driver has arrived"
            BookingStatusType.PICKED_UP -> "Parcel picked up"
            BookingStatusType.IN_TRANSIT -> "On the way to delivery"
            BookingStatusType.ARRIVED_DELIVERY -> "Arrived at delivery"
            BookingStatusType.DELIVERED -> "Delivery completed"
            else -> "Tracking your delivery"
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopWaitingTimer()
        realTimeRepository.disconnect()
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// WAITING TIMER STATE
// ═══════════════════════════════════════════════════════════════════════════

data class WaitingTimerState(
    val isActive: Boolean = false,
    val totalWaitingSeconds: Int = 0,
    val freeSecondsRemaining: Int = RiderTrackingViewModel.FREE_WAITING_SECONDS,
    val isFreeWaitingOver: Boolean = false,
    val extraMinutesCharged: Int = 0,
    val waitingCharge: Int = 0,
    val currentMinuteSeconds: Int = 0
) {
    val freeTimeFormatted: String
        get() {
            val min = freeSecondsRemaining / 60
            val sec = freeSecondsRemaining % 60
            return "%d:%02d".format(min, sec)
        }

    val totalTimeFormatted: String
        get() {
            val min = totalWaitingSeconds / 60
            val sec = totalWaitingSeconds % 60
            return "%d:%02d".format(min, sec)
        }

    val freeWaitingProgress: Float
        get() = if (RiderTrackingViewModel.FREE_WAITING_SECONDS > 0) {
            1f - (freeSecondsRemaining.toFloat() / RiderTrackingViewModel.FREE_WAITING_SECONDS)
        } else 1f
}

// ═══════════════════════════════════════════════════════════════════════════
// RATING UI STATE
// ═══════════════════════════════════════════════════════════════════════════

data class RatingUiState(
    val showRatingDialog: Boolean = false,
    val bookingId: String = "",
    val driverName: String = "",
    val driverPhoto: String? = null,
    val vehicleType: String? = null,
    val totalFare: Int = 0,
    val waitingCharge: Int = 0,
    val isSubmitting: Boolean = false,
    val isSubmitted: Boolean = false,
    val error: String? = null
)

// ═══════════════════════════════════════════════════════════════════════════
// UI STATE
// ═══════════════════════════════════════════════════════════════════════════

data class RiderTrackingUiState(
    val currentBookingId: String? = null,
    val currentStatus: BookingStatusType = BookingStatusType.SEARCHING,
    val statusMessage: String? = null,
    val isNoRiderAvailable: Boolean = false,
    val connectionError: String? = null
)

// ═══════════════════════════════════════════════════════════════════════════
// NAVIGATION EVENTS
// ═══════════════════════════════════════════════════════════════════════════

sealed class RiderTrackingNavigationEvent {
    data class RiderAssigned(
        val bookingId: String,
        val rider: RiderInfo,
        val otp: String?
    ) : RiderTrackingNavigationEvent()

    data class RiderEnroute(val bookingId: String) : RiderTrackingNavigationEvent()

    data class RiderArrived(
        val bookingId: String,
        val message: String
    ) : RiderTrackingNavigationEvent()

    data class ParcelPickedUp(val bookingId: String) : RiderTrackingNavigationEvent()

    data class Delivered(val bookingId: String) : RiderTrackingNavigationEvent()

    data class NoRiderAvailable(val message: String) : RiderTrackingNavigationEvent()

    data class BookingCancelled(val reason: String) : RiderTrackingNavigationEvent()

    object NavigateToHome : RiderTrackingNavigationEvent()
}