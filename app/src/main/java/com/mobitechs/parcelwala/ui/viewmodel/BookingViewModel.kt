package com.mobitechs.parcelwala.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.mobitechs.parcelwala.data.local.PreferencesManager
import com.mobitechs.parcelwala.data.model.request.*
import com.mobitechs.parcelwala.data.repository.BookingRepository
import com.mobitechs.parcelwala.ui.components.VehicleType
import com.mobitechs.parcelwala.ui.screens.booking.GoodsType
import com.mobitechs.parcelwala.utils.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Booking ViewModel
 * Manages complete booking flow state
 */
@HiltViewModel
class BookingViewModel @Inject constructor(
    private val repository: BookingRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(BookingUiState())
    val uiState: StateFlow<BookingUiState> = _uiState.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<BookingNavigationEvent>()
    val navigationEvent: SharedFlow<BookingNavigationEvent> = _navigationEvent.asSharedFlow()

    init {
        loadSavedAddresses()
    }

    /**
     * Set Pickup Address
     */
    fun setPickupAddress(address: SavedAddress) {
        _uiState.update { it.copy(pickupAddress = address) }
        calculateFareIfReady()
    }

    /**
     * Set Drop Address
     */
    fun setDropAddress(address: SavedAddress) {
        _uiState.update { it.copy(dropAddress = address) }
        calculateFareIfReady()
    }

    /**
     * Set Selected Vehicle
     */
    fun setSelectedVehicle(vehicle: VehicleType) {
        _uiState.update { it.copy(selectedVehicle = vehicle) }
        calculateFareIfReady()
    }

    /**
     * Set Goods Type
     */
    fun setGoodsType(goodsType: GoodsType) {
        _uiState.update { it.copy(goodsType = goodsType) }
    }

    /**
     * Apply Coupon
     */
    fun applyCoupon(couponCode: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                // Validate coupon - mock implementation
                val discount = validateCoupon(couponCode)
                _uiState.update {
                    it.copy(
                        appliedCoupon = couponCode,
                        discount = discount,
                        isLoading = false
                    )
                }
                calculateFinalFare()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        error = e.message ?: "Invalid coupon code",
                        isLoading = false
                    )
                }
            }
        }
    }

    /**
     * Remove Coupon
     */
    fun removeCoupon() {
        _uiState.update {
            it.copy(
                appliedCoupon = null,
                discount = 0
            )
        }
        calculateFinalFare()
    }

    /**
     * Set Payment Method
     */
    fun setPaymentMethod(method: String) {
        _uiState.update { it.copy(paymentMethod = method) }
    }

    /**
     * Add GSTIN
     */
    fun addGSTIN(gstin: String) {
        _uiState.update { it.copy(gstin = gstin) }
    }

    /**
     * Confirm Booking
     */
    fun confirmBooking() {
        viewModelScope.launch {
            val state = _uiState.value

            // Validate booking data
            if (state.pickupAddress == null || state.dropAddress == null || state.selectedVehicle == null) {
                _uiState.update { it.copy(error = "Please complete all booking details") }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true) }

            try {
                val bookingRequest = createBookingRequest(state)

                repository.createBooking(bookingRequest).collect { result ->
                    when (result) {
                        is NetworkResult.Loading -> {
                            _uiState.update { it.copy(isLoading = true) }
                        }
                        is NetworkResult.Success -> {
                            val booking = result.data
                            _uiState.update {
                                it.copy(
                                    bookingId = booking?.bookingId?.toString(),
                                    bookingStatus = BookingStatus.SEARCHING_RIDER,
                                    isLoading = false
                                )
                            }
                            // Navigate to searching rider screen
                            _navigationEvent.emit(
                                BookingNavigationEvent.NavigateToSearchingRider(
                                    booking?.bookingId?.toString() ?: "0"
                                )
                            )
                        }
                        is NetworkResult.Error -> {
                            _uiState.update {
                                it.copy(
                                    error = result.message ?: "Failed to create booking",
                                    isLoading = false
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        error = e.message ?: "Failed to create booking",
                        isLoading = false
                    )
                }
            }
        }
    }

    /**
     * Cancel Booking with Reason
     */
    fun cancelBooking(reason: String) {
        viewModelScope.launch {
            val bookingId = _uiState.value.bookingId?.toIntOrNull() ?: return@launch

            _uiState.update { it.copy(isLoading = true) }

            try {
                repository.cancelBooking(bookingId, reason).collect { result ->
                    when (result) {
                        is NetworkResult.Loading -> {
                            _uiState.update { it.copy(isLoading = true) }
                        }
                        is NetworkResult.Success -> {
                            _uiState.update {
                                it.copy(
                                    bookingStatus = BookingStatus.CANCELLED,
                                    isLoading = false
                                )
                            }
                            _navigationEvent.emit(BookingNavigationEvent.NavigateToHome)
                        }
                        is NetworkResult.Error -> {
                            _uiState.update {
                                it.copy(
                                    error = result.message ?: "Failed to cancel booking",
                                    isLoading = false
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        error = e.message ?: "Failed to cancel booking",
                        isLoading = false
                    )
                }
            }
        }
    }

    /**
     * Load Saved Addresses
     */
    private fun loadSavedAddresses() {
        viewModelScope.launch {
            repository.getSavedAddresses().collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        _uiState.update {
                            it.copy(savedAddresses = result.data ?: emptyList())
                        }
                    }
                    is NetworkResult.Error -> {
                        // Silently fail, user can still enter addresses manually
                    }
                    is NetworkResult.Loading -> {
                        // Do nothing
                    }
                }
            }
        }
    }

    /**
     * Calculate Fare if all data is ready
     */
    private fun calculateFareIfReady() {
        val state = _uiState.value
        val pickup = state.pickupAddress
        val drop = state.dropAddress
        val vehicle = state.selectedVehicle

        if (pickup != null && drop != null && vehicle != null) {
            calculateFare(vehicle, pickup, drop)
        }
    }

    /**
     * Calculate Fare based on distance and vehicle
     */
    private fun calculateFare(
        vehicle: VehicleType,
        pickup: SavedAddress,
        drop: SavedAddress
    ) {
        viewModelScope.launch {
            val fareRequest = CalculateFareRequest(
                vehicleTypeId = vehicle.id.toIntOrNull() ?: 1,
                pickupLatitude = pickup.latitude ?: 0.0,
                pickupLongitude = pickup.longitude ?: 0.0,
                dropLatitude = drop.latitude ?: 0.0,
                dropLongitude = drop.longitude ?: 0.0,
                promoCode = _uiState.value.appliedCoupon
            )

            repository.calculateFare(fareRequest).collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        val fareDetails = result.data
                        _uiState.update {
                            it.copy(
                                baseFare = fareDetails?.totalFare?.toInt() ?: vehicle.price,
                                distance = fareDetails?.distanceKm ?: 0.0
                            )
                        }
                        calculateFinalFare()
                    }
                    is NetworkResult.Error -> {
                        // Use vehicle base price as fallback
                        _uiState.update {
                            it.copy(baseFare = vehicle.price)
                        }
                        calculateFinalFare()
                    }
                    is NetworkResult.Loading -> {
                        // Do nothing
                    }
                }
            }
        }
    }

    /**
     * Calculate Final Fare after discounts
     */
    private fun calculateFinalFare() {
        val state = _uiState.value
        val finalFare = state.baseFare - state.discount
        _uiState.update { it.copy(finalFare = finalFare.coerceAtLeast(0)) }
    }

    /**
     * Validate Coupon (Mock implementation)
     */
    private suspend fun validateCoupon(couponCode: String): Int {
        // Mock implementation - replace with actual API call
        return when (couponCode.uppercase()) {
            "FIRST50" -> 50
            "SAVE100" -> 100
            "WEEKEND20" -> 20
            else -> throw Exception("Invalid coupon code")
        }
    }

    /**
     * Create Booking Request from UI State
     */
    private fun createBookingRequest(state: BookingUiState): CreateBookingRequest {
        val pickup = state.pickupAddress!!
        val drop = state.dropAddress!!

        return CreateBookingRequest(
            vehicleTypeId = state.selectedVehicle?.id?.toIntOrNull() ?: 1,
            pickupAddress = pickup.address,
            pickupLatitude = pickup.latitude ?: 0.0,
            pickupLongitude = pickup.longitude ?: 0.0,
            pickupLandmark = pickup.landmark,
            pickupFlatBuilding = null,
            pickupContactName = pickup.contactName ?: "",
            pickupContactPhone = pickup.contactPhone ?: "",
            dropAddress = drop.address,
            dropLatitude = drop.latitude ?: 0.0,
            dropLongitude = drop.longitude ?: 0.0,
            dropLandmark = drop.landmark,
            dropFlatBuilding = null,
            dropContactName = drop.contactName ?: "",
            dropContactPhone = drop.contactPhone ?: "",
            goodsType = state.goodsType?.name,
            goodsWeight = state.goodsType?.weight,
            specialInstructions = null,
            paymentMethod = state.paymentMethod.lowercase(),
            promoCode = state.appliedCoupon
        )
    }

    /**
     * Clear Error
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Reset Booking State
     */
    fun resetBooking() {
        _uiState.update { BookingUiState() }
    }
}

/**
 * Booking UI State
 */
data class BookingUiState(
    val pickupAddress: SavedAddress? = null,
    val dropAddress: SavedAddress? = null,
    val selectedVehicle: VehicleType? = null,
    val goodsType: GoodsType? = null,
    val paymentMethod: String = "Cash",
    val appliedCoupon: String? = null,
    val gstin: String? = null,

    val savedAddresses: List<SavedAddress> = emptyList(),

    val baseFare: Int = 0,
    val discount: Int = 0,
    val finalFare: Int = 0,
    val distance: Double = 0.0,

    val bookingId: String? = null,
    val bookingStatus: BookingStatus = BookingStatus.NOT_STARTED,

    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * Booking Status
 */
enum class BookingStatus {
    NOT_STARTED,
    SEARCHING_RIDER,
    RIDER_FOUND,
    IN_TRANSIT,
    COMPLETED,
    CANCELLED
}

/**
 * Booking Navigation Events
 */
sealed class BookingNavigationEvent {
    data class NavigateToSearchingRider(val bookingId: String) : BookingNavigationEvent()
    object NavigateToHome : BookingNavigationEvent()
}