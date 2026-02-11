// ui/viewmodel/RiderTrackingViewModel.kt
// ✅ UPDATED: With waiting timer, charge calculation & progress notifications
package com.mobitechs.parcelwala.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobitechs.parcelwala.data.manager.ActiveBookingManager
import com.mobitechs.parcelwala.data.manager.BookingStatus
import com.mobitechs.parcelwala.data.model.realtime.BookingStatusType
import com.mobitechs.parcelwala.data.model.realtime.BookingStatusUpdate
import com.mobitechs.parcelwala.data.model.realtime.RealTimeConnectionState
import com.mobitechs.parcelwala.data.model.realtime.RiderInfo
import com.mobitechs.parcelwala.data.model.realtime.RiderLocationUpdate
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

/**
 * ════════════════════════════════════════════════════════════════════════════
 * RIDER TRACKING VIEWMODEL - ENHANCED
 * ════════════════════════════════════════════════════════════════════════════
 *
 * ✅ Features:
 *    - Push notifications for all status changes
 *    - Progress bar notification for ETA/distance tracking
 *    - Real-time ETA and distance tracking
 *    - Proper null handling throughout
 *    - 🆕 Waiting timer with 3-min free period + ₹3/min charge
 *
 * ════════════════════════════════════════════════════════════════════════════
 */
@HiltViewModel
class RiderTrackingViewModel @Inject constructor(
    private val realTimeRepository: RealTimeRepository,
    private val activeBookingManager: ActiveBookingManager,
    private val notificationHelper: BookingNotificationHelper
) : ViewModel() {

    companion object {
        private const val TAG = "RiderTrackingVM"
        // For progress calculation - initial max distance
        private const val INITIAL_MAX_DISTANCE_METERS = 10000.0 // 10 km

        // ── Waiting Charge Constants ──────────────────────────────
        const val FREE_WAITING_SECONDS = 180       // 3 minutes free
        const val CHARGE_PER_MINUTE = 3            // ₹3 per minute after free period
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

    // ETA and Distance StateFlows for UI
    private val _etaMinutes = MutableStateFlow<Int?>(null)
    val etaMinutes: StateFlow<Int?> = _etaMinutes.asStateFlow()

    private val _distanceKm = MutableStateFlow<Double?>(null)
    val distanceKm: StateFlow<Double?> = _distanceKm.asStateFlow()

    // ── Waiting Timer State ──────────────────────────────────────────
    private val _waitingState = MutableStateFlow(WaitingTimerState())
    val waitingState: StateFlow<WaitingTimerState> = _waitingState.asStateFlow()

    private var waitingTimerJob: Job? = null

    val connectionState: StateFlow<RealTimeConnectionState> = realTimeRepository.connectionState

    private val _navigationEvent = MutableSharedFlow<RiderTrackingNavigationEvent>()
    val navigationEvent: SharedFlow<RiderTrackingNavigationEvent> = _navigationEvent.asSharedFlow()

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    // Track initial distance for progress calculation
    private var initialDistanceMeters: Double? = null
    private var lastNotifiedEta: Int? = null

    // ═══════════════════════════════════════════════════════════════════════
    // INITIALIZATION
    // ═══════════════════════════════════════════════════════════════════════

    init {
        observeRealTimeUpdates()
    }


    private fun observeRealTimeUpdates() {
        // Observe booking status updates
        viewModelScope.launch {
            realTimeRepository.bookingUpdates.collect { update ->
                handleBookingStatusUpdate(update)
            }
        }

        // Observe rider location updates
        viewModelScope.launch {
            realTimeRepository.riderLocationUpdates.collect { location ->
                handleRiderLocationUpdate(location)
            }
        }

        // Observe booking cancelled
        viewModelScope.launch {
            realTimeRepository.bookingCancelled.collect { notification ->
                Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                Log.d(TAG, "📥 BOOKING CANCELLED NOTIFICATION")
                Log.d(TAG, "  CancelledBy: ${notification.cancelledBy ?: "null"}")
                Log.d(TAG, "  Reason: ${notification.reason ?: "null"}")
                Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

                notificationHelper.showBookingCancelledNotification(
                    bookingId = notification.bookingId.toString(),
                    reason = notification.reason,
                    cancelledBy = notification.cancelledBy
                )

                stopWaitingTimer()
                activeBookingManager.updateStatus(BookingStatus.CANCELLED)
                realTimeRepository.disconnect()

                _toastMessage.emit(notification.message)
                _navigationEvent.emit(RiderTrackingNavigationEvent.BookingCancelled(notification.message))
            }
        }

        // Observe connection state
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

        // Observe errors
        viewModelScope.launch {
            realTimeRepository.errors.collect { error ->
                Log.e(TAG, "❌ SignalR Error: ${error.message}")
                _toastMessage.emit(error.message)
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // WAITING TIMER - 3 min free, then ₹3/min
    // ═══════════════════════════════════════════════════════════════════════

    private fun startWaitingTimer() {
        // Don't start if already running
        if (waitingTimerJob?.isActive == true) {
            Log.d(TAG, "⏱️ Waiting timer already running, skipping")
            return
        }

        Log.d(TAG, "⏱️ Starting waiting timer - 3 min free, then ₹$CHARGE_PER_MINUTE/min")

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
                delay(1000L) // Tick every second
                elapsedSeconds++

                val freeRemaining = (FREE_WAITING_SECONDS - elapsedSeconds).coerceAtLeast(0)
                val isFreeOver = elapsedSeconds > FREE_WAITING_SECONDS

                // Calculate charge: only for full minutes after free period
                val extraSeconds = if (isFreeOver) elapsedSeconds - FREE_WAITING_SECONDS else 0
                val extraMinutes = extraSeconds / 60  // Only charge for complete minutes
                val charge = extraMinutes * CHARGE_PER_MINUTE

                _waitingState.value = WaitingTimerState(
                    isActive = true,
                    totalWaitingSeconds = elapsedSeconds,
                    freeSecondsRemaining = freeRemaining,
                    isFreeWaitingOver = isFreeOver,
                    extraMinutesCharged = extraMinutes,
                    waitingCharge = charge,
                    // Seconds into the current (next) chargeable minute
                    currentMinuteSeconds = if (isFreeOver) extraSeconds % 60 else 0
                )

                // Log every 30 seconds
                if (elapsedSeconds % 30 == 0) {
                    Log.d(TAG, "⏱️ Waiting: ${elapsedSeconds}s | Free: ${freeRemaining}s | Charge: ₹$charge ($extraMinutes min)")
                }

                // Log when free period ends
                if (elapsedSeconds == FREE_WAITING_SECONDS) {
                    Log.d(TAG, "⚠️ FREE WAITING PERIOD OVER - Charges starting now!")
                }
            }
        }
    }

    private fun stopWaitingTimer() {
        waitingTimerJob?.cancel()
        waitingTimerJob = null

        val finalState = _waitingState.value
        if (finalState.isActive) {
            Log.d(TAG, "⏱️ Waiting timer stopped - Total: ${finalState.totalWaitingSeconds}s | Charge: ₹${finalState.waitingCharge}")
        }

        _waitingState.update { it.copy(isActive = false) }
    }

    /**
     * Get the final waiting charge to add to fare
     * Call this when parcel is picked up to get the total waiting charge
     */
    fun getFinalWaitingCharge(): Int {
        return _waitingState.value.waitingCharge
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
        _waitingState.value = WaitingTimerState()
        initialDistanceMeters = null
        lastNotifiedEta = null
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
        Log.d(TAG, "  Message: ${update.message ?: "null"}")
        Log.d(TAG, "  Driver: ${update.driverName ?: "null"}")
        Log.d(TAG, "  Phone: ${update.driverPhone ?: "null"}")
        Log.d(TAG, "  Vehicle: ${update.vehicleNumber ?: "null"}")
        Log.d(TAG, "  VehicleType: ${update.vehicleType ?: "null"}")
        Log.d(TAG, "  Rating: ${update.driverRating ?: "null"}")
        Log.d(TAG, "  OTP: ${update.otp ?: "null"}")
        Log.d(TAG, "  DeliveryOTP: ${update.deliveryOtp ?: "null"}")
        Log.d(TAG, "  ETA: ${update.etaMinutes ?: "null"} min")
        Log.d(TAG, "  Lat: ${update.driverLatitude ?: "null"}")
        Log.d(TAG, "  Lng: ${update.driverLongitude ?: "null"}")
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

                BookingStatusType.RIDER_ASSIGNED -> {
                    handleDriverAssigned(update)
                }

                BookingStatusType.RIDER_ENROUTE -> {
                    activeBookingManager.updateStatus(BookingStatus.RIDER_EN_ROUTE)
                    _navigationEvent.emit(
                        RiderTrackingNavigationEvent.RiderEnroute(update.bookingId.toString())
                    )
                }

                BookingStatusType.ARRIVED -> {
                    handleDriverArrived(update)
                }

                BookingStatusType.PICKED_UP -> {
                    handleParcelPickedUp(update)
                }

                BookingStatusType.IN_TRANSIT -> {
                    activeBookingManager.updateStatus(BookingStatus.IN_TRANSIT)
                }

                BookingStatusType.ARRIVED_DELIVERY -> {
                    handleArrivedAtDelivery(update)
                }

                BookingStatusType.DELIVERED -> {
                    handleDeliveryCompleted(update)
                }

                BookingStatusType.NO_RIDER -> {
                    activeBookingManager.updateStatus(BookingStatus.SEARCH_TIMEOUT)
                    _uiState.update { it.copy(isNoRiderAvailable = true) }
                    _navigationEvent.emit(
                        RiderTrackingNavigationEvent.NoRiderAvailable(
                            message = update.message ?: "No riders available"
                        )
                    )
                }

                BookingStatusType.CANCELLED -> {
                    handleCancelled(update)
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // STATUS HANDLERS WITH NOTIFICATIONS
    // ═══════════════════════════════════════════════════════════════════════

    private suspend fun handleDriverAssigned(update: BookingStatusUpdate) {
        // Create rider info from update
        val rider = update.rider

        if (rider != null) {
            _assignedRider.value = rider
            Log.d(TAG, "✅ Rider assigned: ${rider.riderName}")
        } else {
            // Create a minimal RiderInfo from available data
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

        // Store OTP
        _bookingOtp.value = update.otp
        Log.d(TAG, "📝 OTP stored: ${update.otp ?: "null"}")

        // Store ETA
        _etaMinutes.value = update.etaMinutes ?: rider?.etaMinutes
        Log.d(TAG, "⏱️ ETA stored: ${_etaMinutes.value ?: "null"}")

        activeBookingManager.updateStatus(BookingStatus.RIDER_ASSIGNED)

        // Show notification
        notificationHelper.showDriverAssignedNotification(
            bookingId = update.bookingId.toString(),
            driverName = update.driverName ?: rider?.riderName,
            vehicleNumber = update.vehicleNumber ?: rider?.vehicleNumber,
            vehicleType = update.vehicleType ?: rider?.vehicleType,
            otp = update.otp,
            etaMinutes = update.etaMinutes ?: rider?.etaMinutes
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

        // Cancel progress notification
        notificationHelper.cancelNotification(BookingNotificationHelper.NOTIFICATION_TRACKING_PROGRESS)

        // ⏱️ START WAITING TIMER when driver arrives
        startWaitingTimer()

        notificationHelper.showDriverArrivedNotification(
            bookingId = update.bookingId.toString(),
            driverName = _assignedRider.value?.riderName ?: update.driverName,
            otp = _bookingOtp.value ?: update.otp
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

        // ⏱️ STOP WAITING TIMER when parcel is picked up
        val finalCharge = getFinalWaitingCharge()
        stopWaitingTimer()
        Log.d(TAG, "💰 Final waiting charge at pickup: ₹$finalCharge")

        update.deliveryOtp?.let { _deliveryOtp.value = it }

        val dropAddress = activeBookingManager.activeBooking.value?.dropAddress?.address

        notificationHelper.showParcelPickedUpNotification(
            bookingId = update.bookingId.toString(),
            dropAddress = dropAddress
        )

        _toastMessage.emit("Parcel picked up!")
        _navigationEvent.emit(
            RiderTrackingNavigationEvent.ParcelPickedUp(update.bookingId.toString())
        )
    }

    private suspend fun handleArrivedAtDelivery(update: BookingStatusUpdate) {
        activeBookingManager.updateStatus(BookingStatus.IN_TRANSIT)

        notificationHelper.showArrivedAtDeliveryNotification(
            bookingId = update.bookingId.toString(),
            deliveryOtp = _deliveryOtp.value ?: update.deliveryOtp
        )

        _toastMessage.emit("Rider arrived at delivery location!")
    }

    private suspend fun handleDeliveryCompleted(update: BookingStatusUpdate) {
        activeBookingManager.updateStatus(BookingStatus.DELIVERED)

        stopWaitingTimer()

        val fare = activeBookingManager.activeBooking.value?.fare

        notificationHelper.showDeliveryCompletedNotification(
            bookingId = update.bookingId.toString(),
            fare = fare
        )

        realTimeRepository.disconnect()
        _toastMessage.emit("Delivery completed!")
        _navigationEvent.emit(
            RiderTrackingNavigationEvent.Delivered(update.bookingId.toString())
        )
    }

    private suspend fun handleCancelled(update: BookingStatusUpdate) {
        activeBookingManager.updateStatus(BookingStatus.CANCELLED)

        stopWaitingTimer()

        notificationHelper.showBookingCancelledNotification(
            bookingId = update.bookingId.toString(),
            reason = update.cancellationReason,
            cancelledBy = update.cancelledBy
        )

        realTimeRepository.disconnect()
        _toastMessage.emit(update.message ?: "Booking cancelled")
        _navigationEvent.emit(
            RiderTrackingNavigationEvent.BookingCancelled(
                update.message ?: "Booking cancelled"
            )
        )
    }

    // ═══════════════════════════════════════════════════════════════════════
    // LOCATION UPDATE HANDLER - With Progress Notification
    // ═══════════════════════════════════════════════════════════════════════

    private fun handleRiderLocationUpdate(location: RiderLocationUpdate) {
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "📍 LOCATION UPDATE RECEIVED")
        Log.d(TAG, "  Lat: ${location.latitude}")
        Log.d(TAG, "  Lng: ${location.longitude}")
        Log.d(TAG, "  ETA: ${location.etaMinutes ?: "null"} min")
        Log.d(TAG, "  Distance: ${location.distanceMeters ?: "null"} m")
        Log.d(TAG, "  Status: ${location.status ?: "null"}")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        _riderLocation.value = location

        // Update ETA
        location.etaMinutes?.let { eta ->
            _etaMinutes.value = eta
        }

        // Update distance
        location.distanceMeters?.let { meters ->
            _distanceKm.value = meters / 1000.0

            // Store initial distance for progress calculation
            if (initialDistanceMeters == null) {
                initialDistanceMeters = meters.coerceAtLeast(1000.0) // Minimum 1km for calculation
                Log.d(TAG, "📏 Initial distance set: ${initialDistanceMeters}m")
            }
        }

        // Update rider info with new location
        _assignedRider.update { rider ->
            rider?.copy(
                currentLatitude = location.latitude,
                currentLongitude = location.longitude,
                etaMinutes = location.etaMinutes ?: rider.etaMinutes
            )
        }

        // Update progress notification (only during driver en-route to pickup)
        val currentStatus = _uiState.value.currentStatus
        if (currentStatus == BookingStatusType.RIDER_ASSIGNED ||
            currentStatus == BookingStatusType.RIDER_ENROUTE) {

            location.distanceMeters?.let { distance ->
                val bookingId = _uiState.value.currentBookingId ?: return@let
                val driverName = _assignedRider.value?.riderName
                val maxDistance = initialDistanceMeters ?: INITIAL_MAX_DISTANCE_METERS

                notificationHelper.showDriverTrackingProgress(
                    bookingId = bookingId,
                    driverName = driverName,
                    distanceMeters = distance,
                    etaMinutes = location.etaMinutes,
                    maxDistanceMeters = maxDistance
                )
            }
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
    val extraMinutesCharged: Int = 0,        // Complete minutes after free period
    val waitingCharge: Int = 0,              // Total charge in ₹
    val currentMinuteSeconds: Int = 0        // Seconds into current chargeable minute (0-59)
) {
    /** Formatted free time remaining "2:45" */
    val freeTimeFormatted: String
        get() {
            val min = freeSecondsRemaining / 60
            val sec = freeSecondsRemaining % 60
            return "%d:%02d".format(min, sec)
        }

    /** Formatted total waiting time "5:30" */
    val totalTimeFormatted: String
        get() {
            val min = totalWaitingSeconds / 60
            val sec = totalWaitingSeconds % 60
            return "%d:%02d".format(min, sec)
        }

    /** Progress for circular indicator (0f to 1f) during free period */
    val freeWaitingProgress: Float
        get() = if (RiderTrackingViewModel.FREE_WAITING_SECONDS > 0) {
            1f - (freeSecondsRemaining.toFloat() / RiderTrackingViewModel.FREE_WAITING_SECONDS)
        } else 1f
}

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