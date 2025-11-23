// ui/viewmodel/BookingViewModel.kt
package com.mobitechs.parcelwala.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobitechs.parcelwala.data.model.request.*
import com.mobitechs.parcelwala.data.model.response.*
import com.mobitechs.parcelwala.data.repository.BookingRepository
import com.mobitechs.parcelwala.utils.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Booking ViewModel
 * Handles all booking-related operations with API integration
 */
@HiltViewModel
class BookingViewModel @Inject constructor(
    private val bookingRepository: BookingRepository
) : ViewModel() {

    // ============ UI STATE ============
    private val _uiState = MutableStateFlow(BookingUiState())
    val uiState: StateFlow<BookingUiState> = _uiState.asStateFlow()

    // ============ STATIC DATA STATE ============
    private val _vehicleTypes = MutableStateFlow<List<VehicleTypeResponse>>(emptyList())
    val vehicleTypes: StateFlow<List<VehicleTypeResponse>> = _vehicleTypes.asStateFlow()

    private val _goodsTypes = MutableStateFlow<List<GoodsTypeResponse>>(emptyList())
    val goodsTypes: StateFlow<List<GoodsTypeResponse>> = _goodsTypes.asStateFlow()

    private val _restrictedItems = MutableStateFlow<List<RestrictedItemResponse>>(emptyList())
    val restrictedItems: StateFlow<List<RestrictedItemResponse>> = _restrictedItems.asStateFlow()

    private val _availableCoupons = MutableStateFlow<List<CouponResponse>>(emptyList())
    val availableCoupons: StateFlow<List<CouponResponse>> = _availableCoupons.asStateFlow()

    // ============ NAVIGATION EVENTS ============
    private val _navigationEvent = MutableSharedFlow<BookingNavigationEvent>()
    val navigationEvent: SharedFlow<BookingNavigationEvent> = _navigationEvent.asSharedFlow()

    init {
        // Load static data on initialization
        loadVehicleTypes()
        loadGoodsTypes()
    }

    // ============ LOAD STATIC DATA ============

    /**
     * Load vehicle types from API/Mock
     */
    fun loadVehicleTypes(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            bookingRepository.getVehicleTypes(forceRefresh).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                    is NetworkResult.Success -> {
                        _vehicleTypes.value = result.data ?: emptyList()
                        _uiState.update { it.copy(isLoading = false, error = null) }
                    }
                    is NetworkResult.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = result.message ?: "Failed to load vehicle types"
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Load goods types from API/Mock
     */
    fun loadGoodsTypes(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            bookingRepository.getGoodsTypes(forceRefresh).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                    is NetworkResult.Success -> {
                        _goodsTypes.value = result.data ?: emptyList()
                        _uiState.update { it.copy(isLoading = false, error = null) }
                    }
                    is NetworkResult.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = result.message ?: "Failed to load goods types"
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Load restricted items from API/Mock
     */
    fun loadRestrictedItems(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            bookingRepository.getRestrictedItems(forceRefresh).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                    is NetworkResult.Success -> {
                        _restrictedItems.value = result.data ?: emptyList()
                        _uiState.update { it.copy(isLoading = false, error = null) }
                    }
                    is NetworkResult.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = result.message ?: "Failed to load restricted items"
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Load available coupons from API/Mock
     */
    fun loadAvailableCoupons(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            bookingRepository.getAvailableCoupons(forceRefresh).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                    is NetworkResult.Success -> {
                        _availableCoupons.value = result.data ?: emptyList()
                        _uiState.update { it.copy(isLoading = false, error = null) }
                    }
                    is NetworkResult.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = result.message ?: "Failed to load coupons"
                            )
                        }
                    }
                }
            }
        }
    }

    // ============ BOOKING FLOW ACTIONS ============

    /**
     * Set pickup address
     */
    fun setPickupAddress(address: SavedAddress) {
        _uiState.update { it.copy(pickupAddress = address) }
    }

    /**
     * Set drop address
     */
    fun setDropAddress(address: SavedAddress) {
        _uiState.update { it.copy(dropAddress = address) }
        // Calculate fare when both addresses are set
        if (_uiState.value.pickupAddress != null) {
            calculateFare()
        }
    }

    /**
     * Set selected vehicle
     */
    fun setSelectedVehicle(vehicleType: VehicleTypeResponse) {
        _uiState.update {
            it.copy(
                selectedVehicleId = vehicleType.vehicleTypeId,
                baseFare = vehicleType.basePrice,
                finalFare = calculateFinalFare(vehicleType.basePrice)
            )
        }
    }

    /**
     * Set goods type
     */
    fun setGoodsType(goodsType: GoodsTypeResponse) {
        _uiState.update {
            it.copy(
                selectedGoodsTypeId = goodsType.goodsTypeId,
                goodsWeight = goodsType.defaultWeight,
                goodsPackages = goodsType.defaultPackages,
                goodsValue = goodsType.defaultValue
            )
        }
    }

    /**
     * Apply coupon
     */
    fun applyCoupon(couponCode: String) {
        viewModelScope.launch {
            val orderValue = _uiState.value.baseFare
            bookingRepository.validateCoupon(couponCode, orderValue).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                    is NetworkResult.Success -> {
                        result.data?.let { coupon ->
                            val discount = calculateDiscount(coupon, orderValue)
                            _uiState.update {
                                it.copy(
                                    appliedCoupon = coupon.code,
                                    discount = discount,
                                    finalFare = calculateFinalFare(orderValue),
                                    isLoading = false,
                                    error = null
                                )
                            }
                        }
                    }
                    is NetworkResult.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = result.message ?: "Invalid coupon"
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Remove applied coupon
     */
    fun removeCoupon() {
        _uiState.update {
            it.copy(
                appliedCoupon = null,
                discount = 0,
                finalFare = calculateFinalFare(it.baseFare)
            )
        }
    }

    /**
     * Set payment method
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
     * Calculate fare based on pickup and drop locations
     */
    private fun calculateFare() {
        viewModelScope.launch {
            val pickup = _uiState.value.pickupAddress
            val drop = _uiState.value.dropAddress

            if (pickup == null || drop == null) return@launch

            val request = CalculateFareRequest(
                vehicleTypeId = _uiState.value.selectedVehicleId ?: 1,
                pickupLatitude = pickup.latitude ?: 0.0,
                pickupLongitude = pickup.longitude ?: 0.0,
                dropLatitude = drop.latitude ?: 0.0,
                dropLongitude = drop.longitude ?: 0.0
            )

            bookingRepository.calculateFare(request).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                    is NetworkResult.Success -> {
                        result.data?.let { fareDetails ->
                            _uiState.update {
                                it.copy(
                                    baseFare = fareDetails.totalFare.toInt(),
                                    finalFare = calculateFinalFare(fareDetails.totalFare.toInt()),
                                    isLoading = false,
                                    error = null
                                )
                            }
                        }
                    }
                    is NetworkResult.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = result.message ?: "Failed to calculate fare"
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Calculate discount based on coupon
     */
    private fun calculateDiscount(coupon: CouponResponse, orderValue: Int): Int {
        return when (coupon.discountType) {
            "percentage" -> {
                val discount = (orderValue * coupon.discountValue / 100)
                if (coupon.maxDiscount != null) {
                    minOf(discount, coupon.maxDiscount)
                } else {
                    discount
                }
            }
            "fixed" -> coupon.discountValue
            else -> 0
        }
    }

    /**
     * Calculate final fare after discount
     */
    private fun calculateFinalFare(baseFare: Int): Int {
        val discount = _uiState.value.discount
        return maxOf(0, baseFare - discount)
    }

    /**
     * Confirm booking and create order
     */
    fun confirmBooking() {
        viewModelScope.launch {
            val state = _uiState.value

            if (state.pickupAddress == null || state.dropAddress == null || state.selectedVehicleId == null) {
                _uiState.update { it.copy(error = "Missing required information") }
                return@launch
            }

            val request = CreateBookingRequest(
                vehicleTypeId = state.selectedVehicleId,
                pickupAddress = state.pickupAddress.address,
                pickupLatitude = state.pickupAddress.latitude ?: 0.0,
                pickupLongitude = state.pickupAddress.longitude ?: 0.0,
                pickupContactName = state.pickupAddress.contactName ?: "",
                pickupContactPhone = state.pickupAddress.contactPhone ?: "",
                dropAddress = state.dropAddress.address,
                dropLatitude = state.dropAddress.latitude ?: 0.0,
                dropLongitude = state.dropAddress.longitude ?: 0.0,
                dropContactName = state.dropAddress.contactName ?: "",
                dropContactPhone = state.dropAddress.contactPhone ?: "",
                goodsTypeId = state.selectedGoodsTypeId,
                goodsWeight = state.goodsWeight,
                goodsPackages = state.goodsPackages,
                goodsValue = state.goodsValue,
                paymentMethod = state.paymentMethod,
                couponCode = state.appliedCoupon,
                gstin = state.gstin
            )

            bookingRepository.createBooking(request).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                    is NetworkResult.Success -> {
                        result.data?.let { booking ->
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    error = null
                                )
                            }
                            // Navigate to searching rider screen
                            _navigationEvent.emit(
                                BookingNavigationEvent.NavigateToSearchingRider(
                                    booking.bookingNumber
                                )
                            )
                        }
                    }
                    is NetworkResult.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = result.message ?: "Failed to create booking"
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Cancel booking with reason
     */
    fun cancelBooking(reason: String) {
        viewModelScope.launch {
            // Get booking ID from navigation (would be passed in real scenario)
            val bookingId = 12345 // This should come from the current booking

            bookingRepository.cancelBooking(bookingId, reason).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                    is NetworkResult.Success -> {
                        _uiState.update {
                            it.copy(isLoading = false, error = null)
                        }
                        // Navigate back to home
                        _navigationEvent.emit(BookingNavigationEvent.NavigateToHome)
                    }
                    is NetworkResult.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = result.message ?: "Failed to cancel booking"
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Reset booking state
     */
    fun resetBooking() {
        _uiState.value = BookingUiState()
    }
}

/**
 * Booking UI State
 */
data class BookingUiState(
    val pickupAddress: SavedAddress? = null,
    val dropAddress: SavedAddress? = null,
    val selectedVehicleId: Int? = null,
    val selectedGoodsTypeId: Int? = null,
    val goodsWeight: Double? = null,
    val goodsPackages: Int? = null,
    val goodsValue: Int? = null,
    val baseFare: Int = 0,
    val discount: Int = 0,
    val finalFare: Int = 0,
    val appliedCoupon: String? = null,
    val paymentMethod: String = "Cash",
    val gstin: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * Navigation Events
 */
sealed class BookingNavigationEvent {
    data class NavigateToSearchingRider(val bookingId: String) : BookingNavigationEvent()
    object NavigateToHome : BookingNavigationEvent()
}
