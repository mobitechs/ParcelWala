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

    private val _assignedRider = MutableStateFlow<RiderInfo?>(null)
    val assignedRider: StateFlow<RiderInfo?> = _assignedRider.asStateFlow()

    private val _riderLocation = MutableStateFlow<RiderLocationUpdate?>(null)
    val riderLocation: StateFlow<RiderLocationUpdate?> = _riderLocation.asStateFlow()

    private val _bookingOtp = MutableStateFlow<String?>(null)
    val bookingOtp: StateFlow<String?> = _bookingOtp.asStateFlow()

    val connectionState: StateFlow<RealTimeConnectionState> = realTimeRepository.connectionState

    private val _navigationEvent = MutableSharedFlow<RiderTrackingNavigationEvent>()
    val navigationEvent: SharedFlow<RiderTrackingNavigationEvent> = _navigationEvent.asSharedFlow()

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

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
                Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                Log.d(TAG, "📥 BOOKING CANCELLED NOTIFICATION")
                Log.d(TAG, "Cancelled by: ${notification.cancelledBy}")
                Log.d(TAG, "Reason: ${notification.reason}")
                Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

                activeBookingManager.updateStatus(BookingStatus.CANCELLED)
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

            val bookingId = _uiState.value.currentBookingId?.toIntOrNull()
                ?: activeBookingManager.activeBooking.value?.bookingId?.filter { it.isDigit() }?.toIntOrNull()

            if (bookingId != null && realTimeRepository.isConnected()) {
                realTimeRepository.cancelBooking(bookingId, reason)
            }

            realTimeRepository.disconnect()
            activeBookingManager.clearActiveBooking()
            _navigationEvent.emit(RiderTrackingNavigationEvent.BookingCancelled(reason))
        }
    }

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
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "📥 STATUS UPDATE: $status")
        Log.d(TAG, "Message: ${update.message}")
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
                    update.rider?.let { rider ->
                        _assignedRider.value = rider
                        _bookingOtp.value = update.otp
                        activeBookingManager.updateStatus(BookingStatus.RIDER_ASSIGNED)

                        _navigationEvent.emit(
                            RiderTrackingNavigationEvent.RiderAssigned(
                                bookingId = update.bookingId.toString(),
                                rider = rider,
                                otp = update.otp
                            )
                        )
                    }
                }

                BookingStatusType.RIDER_ENROUTE -> {
                    activeBookingManager.updateStatus(BookingStatus.RIDER_EN_ROUTE)
                    _navigationEvent.emit(
                        RiderTrackingNavigationEvent.RiderEnroute(update.bookingId.toString())
                    )
                }

                BookingStatusType.ARRIVED -> {
                    activeBookingManager.updateStatus(BookingStatus.RIDER_EN_ROUTE)
                    _toastMessage.emit(update.message ?: "Rider has arrived at pickup!")
                    _navigationEvent.emit(
                        RiderTrackingNavigationEvent.RiderArrived(
                            bookingId = update.bookingId.toString(),
                            message = update.message ?: "Rider has arrived!"
                        )
                    )
                }

                BookingStatusType.PICKED_UP -> {
                    activeBookingManager.updateStatus(BookingStatus.IN_TRANSIT)
                    _toastMessage.emit("Parcel picked up!")
                    _navigationEvent.emit(
                        RiderTrackingNavigationEvent.ParcelPickedUp(update.bookingId.toString())
                    )
                }

                BookingStatusType.IN_TRANSIT -> {
                    activeBookingManager.updateStatus(BookingStatus.IN_TRANSIT)
                }

                BookingStatusType.ARRIVED_DELIVERY -> {
                    activeBookingManager.updateStatus(BookingStatus.IN_TRANSIT)
                    _toastMessage.emit("Rider arrived at delivery location!")
                }

                BookingStatusType.DELIVERED -> {
                    activeBookingManager.updateStatus(BookingStatus.DELIVERED)
                    realTimeRepository.disconnect()
                    _toastMessage.emit("Delivery completed!")
                    _navigationEvent.emit(
                        RiderTrackingNavigationEvent.Delivered(update.bookingId.toString())
                    )
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
                    activeBookingManager.updateStatus(BookingStatus.CANCELLED)
                    realTimeRepository.disconnect()
                    _toastMessage.emit(update.message ?: "Booking cancelled")
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