// ui/viewmodel/RiderTrackingViewModel.kt
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
import dagger.hilt.android.lifecycle.HiltViewModel
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
 * RIDER TRACKING VIEWMODEL
 * ════════════════════════════════════════════════════════════════════════════
 *
 * Manages real-time rider tracking state:
 * - Connection to real-time service
 * - Booking status updates
 * - Rider location updates
 * - Navigation events
 *
 * ════════════════════════════════════════════════════════════════════════════
 */
@HiltViewModel
class RiderTrackingViewModel @Inject constructor(
    private val realTimeRepository: RealTimeRepository,
    private val activeBookingManager: ActiveBookingManager
) : ViewModel() {

    companion object {
        private const val TAG = "RiderTrackingVM"
    }

    // ═══════════════════════════════════════════════════════════════════════
    // UI STATE
    // ═══════════════════════════════════════════════════════════════════════

    private val _uiState = MutableStateFlow(RiderTrackingUiState())
    val uiState: StateFlow<RiderTrackingUiState> = _uiState.asStateFlow()

    // ═══════════════════════════════════════════════════════════════════════
    // RIDER STATE
    // ═══════════════════════════════════════════════════════════════════════

    private val _assignedRider = MutableStateFlow<RiderInfo?>(null)
    val assignedRider: StateFlow<RiderInfo?> = _assignedRider.asStateFlow()

    private val _riderLocation = MutableStateFlow<RiderLocationUpdate?>(null)
    val riderLocation: StateFlow<RiderLocationUpdate?> = _riderLocation.asStateFlow()

    private val _bookingOtp = MutableStateFlow<String?>(null)
    val bookingOtp: StateFlow<String?> = _bookingOtp.asStateFlow()

    // ═══════════════════════════════════════════════════════════════════════
    // CONNECTION STATE
    // ═══════════════════════════════════════════════════════════════════════

    val connectionState: StateFlow<RealTimeConnectionState> = realTimeRepository.connectionState

    // ═══════════════════════════════════════════════════════════════════════
    // NAVIGATION EVENTS
    // ═══════════════════════════════════════════════════════════════════════

    private val _navigationEvent = MutableSharedFlow<RiderTrackingNavigationEvent>()
    val navigationEvent: SharedFlow<RiderTrackingNavigationEvent> = _navigationEvent.asSharedFlow()

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
                    else -> { }
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PUBLIC METHODS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Connect to real-time service for booking updates
     */
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

    /**
     * Disconnect from real-time service
     */
    fun disconnect() {
        Log.d(TAG, "🔌 Disconnecting...")
        realTimeRepository.disconnect()
        clearState()
    }

    /**
     * Retry search for rider
     */
    fun retrySearch() {
        val bookingId = _uiState.value.currentBookingId ?: return
        val activeBooking = activeBookingManager.activeBooking.value ?: return

        Log.d(TAG, "🔄 Retrying search...")

        // Reset state
        _uiState.update {
            it.copy(
                currentStatus = BookingStatusType.SEARCHING,
                statusMessage = "Looking for nearby riders...",
                isNoRiderAvailable = false
            )
        }

        // Reconnect
        realTimeRepository.connectAndSubscribe(
            bookingId = bookingId,
            pickupLatitude = activeBooking.pickupAddress.latitude,
            pickupLongitude = activeBooking.pickupAddress.longitude
        )

        // Update active booking manager
        activeBookingManager.retrySearch()
    }

    /**
     * Cancel booking
     */
    fun cancelBooking(reason: String) {
        viewModelScope.launch {
            Log.d(TAG, "❌ Cancelling booking: $reason")
            realTimeRepository.disconnect()
            activeBookingManager.clearActiveBooking()
            _navigationEvent.emit(RiderTrackingNavigationEvent.BookingCancelled(reason))
        }
    }

    /**
     * Clear all state
     */
    fun clearState() {
        _assignedRider.value = null
        _riderLocation.value = null
        _bookingOtp.value = null
        _uiState.value = RiderTrackingUiState()
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PRIVATE METHODS
    // ═══════════════════════════════════════════════════════════════════════

    private fun handleBookingStatusUpdate(update: BookingStatusUpdate) {
        val status = update.getStatusType()
        Log.d(TAG, "📥 Status update: $status")

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
                    update.rider?.let { rider ->
                        _assignedRider.value = rider
                        _bookingOtp.value = update.otp
                        activeBookingManager.updateStatus(BookingStatus.RIDER_ASSIGNED)

                        _navigationEvent.emit(
                            RiderTrackingNavigationEvent.RiderAssigned(
                                bookingId = update.bookingId,
                                rider = rider,
                                otp = update.otp
                            )
                        )
                    }
                }

                BookingStatusType.RIDER_ENROUTE -> {
                    // Maps to RIDER_EN_ROUTE in your BookingStatus enum (with underscore)
                    activeBookingManager.updateStatus(BookingStatus.RIDER_EN_ROUTE)
                    _navigationEvent.emit(
                        RiderTrackingNavigationEvent.RiderEnroute(update.bookingId)
                    )
                }

                BookingStatusType.ARRIVED -> {
                    // No ARRIVED in BookingStatus enum - keep as RIDER_EN_ROUTE
                    activeBookingManager.updateStatus(BookingStatus.RIDER_EN_ROUTE)
                    _navigationEvent.emit(
                        RiderTrackingNavigationEvent.RiderArrived(
                            bookingId = update.bookingId,
                            message = update.message ?: "Rider has arrived!"
                        )
                    )
                }

                BookingStatusType.PICKED_UP, BookingStatusType.IN_TRANSIT -> {
                    activeBookingManager.updateStatus(BookingStatus.IN_TRANSIT)
                    _navigationEvent.emit(
                        RiderTrackingNavigationEvent.ParcelPickedUp(update.bookingId)
                    )
                }

                BookingStatusType.DELIVERED -> {
                    activeBookingManager.updateStatus(BookingStatus.DELIVERED)
                    realTimeRepository.disconnect()
                    _navigationEvent.emit(
                        RiderTrackingNavigationEvent.Delivered(update.bookingId)
                    )
                }

                BookingStatusType.NO_RIDER -> {
                    // Maps to SEARCH_TIMEOUT in your BookingStatus enum
                    activeBookingManager.updateStatus(BookingStatus.SEARCH_TIMEOUT)
                    _uiState.update { it.copy(isNoRiderAvailable = true) }
                    _navigationEvent.emit(
                        RiderTrackingNavigationEvent.NoRiderAvailable(
                            message = update.message ?: "No riders available"
                        )
                    )
                }

                BookingStatusType.CANCELLED -> {
                    activeBookingManager.updateStatus(BookingStatus.CANCELLED)
                    realTimeRepository.disconnect()
                    _navigationEvent.emit(
                        RiderTrackingNavigationEvent.BookingCancelled(
                            update.message ?: "Booking cancelled"
                        )
                    )
                }
            }
        }
    }

    private fun handleRiderLocationUpdate(location: RiderLocationUpdate) {
        _riderLocation.value = location

        // Update rider info with new location
        _assignedRider.update { rider ->
            rider?.copy(
                currentLatitude = location.latitude,
                currentLongitude = location.longitude,
                etaMinutes = location.etaMinutes
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        realTimeRepository.disconnect()
    }
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