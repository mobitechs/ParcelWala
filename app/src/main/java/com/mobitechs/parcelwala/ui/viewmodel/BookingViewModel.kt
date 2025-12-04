// ui/viewmodel/BookingViewModel.kt
package com.mobitechs.parcelwala.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobitechs.parcelwala.data.model.request.CalculateFareRequest
import com.mobitechs.parcelwala.data.model.request.CreateBookingRequest
import com.mobitechs.parcelwala.data.model.request.SavedAddress
import com.mobitechs.parcelwala.data.model.response.CouponResponse
import com.mobitechs.parcelwala.data.model.response.FareDetails
import com.mobitechs.parcelwala.data.model.response.GoodsTypeResponse
import com.mobitechs.parcelwala.data.model.response.OrderResponse
import com.mobitechs.parcelwala.data.model.response.RestrictedItemResponse
import com.mobitechs.parcelwala.data.model.response.VehicleTypeResponse
import com.mobitechs.parcelwala.data.repository.BookingRepository
import com.mobitechs.parcelwala.data.repository.DirectionsRepository
import com.mobitechs.parcelwala.data.repository.RouteInfo
import com.mobitechs.parcelwala.utils.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
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
 * Booking ViewModel
 * Handles all booking-related operations
 *
 * NEW FLOW (Like Ola/Uber):
 * 1. User selects pickup & drop locations
 * 2. Call calculateFaresForAllVehicles API
 * 3. Show list of vehicles with calculated fares
 * 4. User selects a vehicle → proceed to review
 */
@HiltViewModel
class BookingViewModel @Inject constructor(
    private val bookingRepository: BookingRepository,
    private val directionsRepository: DirectionsRepository
) : ViewModel() {

    // ============ UI STATE ============
    private val _uiState = MutableStateFlow(BookingUiState())
    val uiState: StateFlow<BookingUiState> = _uiState.asStateFlow()

    // ============ VEHICLE FARES - List<FareDetails> ============
    private val _vehicleFares = MutableStateFlow<List<FareDetails>>(emptyList())
    val vehicleFares: StateFlow<List<FareDetails>> = _vehicleFares.asStateFlow()

    private val _selectedFareDetails = MutableStateFlow<FareDetails?>(null)
    val selectedFareDetails: StateFlow<FareDetails?> = _selectedFareDetails.asStateFlow()

    private val _isFareLoading = MutableStateFlow(false)
    val isFareLoading: StateFlow<Boolean> = _isFareLoading.asStateFlow()

    private var fareCalculationJob: Job? = null

    // ============ STATIC DATA ============
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

    // ============ ROUTE INFO ============
    private val _routeInfo = MutableStateFlow<RouteInfo?>(null)
    val routeInfo: StateFlow<RouteInfo?> = _routeInfo.asStateFlow()

    private val _isRouteLoading = MutableStateFlow(false)
    val isRouteLoading: StateFlow<Boolean> = _isRouteLoading.asStateFlow()

    init {
        loadGoodsTypes()
    }

    // ============ FARE CALCULATION ============

    /**
     * Calculate fares for all vehicle types
     * Called when both pickup and drop are set
     */
    fun calculateFaresForAllVehicles() {
        val pickup = _uiState.value.pickupAddress
        val drop = _uiState.value.dropAddress

        if (pickup == null || drop == null) {
            _uiState.update { it.copy(error = "Please select pickup and drop locations") }
            return
        }

        if (pickup.latitude == 0.0 || drop.latitude == 0.0) {
            _uiState.update { it.copy(error = "Invalid location coordinates") }
            return
        }

        fareCalculationJob?.cancel()
        fareCalculationJob = viewModelScope.launch {
            _isFareLoading.value = true
            _uiState.update { it.copy(isLoading = true, error = null) }

            val request = CalculateFareRequest(
//                vehicleTypeId = 0, // 0 = all vehicles
                pickupLatitude = pickup.latitude,
                pickupLongitude = pickup.longitude,
                dropLatitude = drop.latitude,
                dropLongitude = drop.longitude
            )

            bookingRepository.calculateFaresForAllVehicles(request).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> {
                        _isFareLoading.value = true
                    }
                    is NetworkResult.Success -> {
                        _vehicleFares.value = result.data ?: emptyList()
                        _isFareLoading.value = false
                        _uiState.update { it.copy(isLoading = false, error = null, hasFaresLoaded = true) }

                        // Auto-select for Book Again
                        _uiState.value.preferredVehicleTypeId?.let { prefId ->
                            selectFareDetailsById(prefId)
                        }
                    }
                    is NetworkResult.Error -> {
                        _isFareLoading.value = false
                        _vehicleFares.value = emptyList()
                        _uiState.update { it.copy(isLoading = false, error = result.message, hasFaresLoaded = false) }
                    }
                }
            }
        }
    }

    /**
     * Select a fare details (vehicle with calculated fare)
     */
    fun selectFareDetails(fareDetails: FareDetails) {
        _selectedFareDetails.value = fareDetails
        _uiState.update {
            it.copy(
                selectedVehicleId = fareDetails.vehicleTypeId,
                baseFare = fareDetails.roundedFare,
                finalFare = calculateFinalFare(fareDetails.roundedFare)
            )
        }
    }

    private fun selectFareDetailsById(vehicleTypeId: Int) {
        _vehicleFares.value.find { it.vehicleTypeId == vehicleTypeId }?.let {
            selectFareDetails(it)
        }
    }

    fun clearVehicleFares() {
        _vehicleFares.value = emptyList()
        _selectedFareDetails.value = null
        _uiState.update { it.copy(hasFaresLoaded = false, selectedVehicleId = null) }
    }

    // ============ LOAD STATIC DATA ============

    fun loadVehicleTypes(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            bookingRepository.getVehicleTypes(forceRefresh).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> _uiState.update { it.copy(isLoading = true) }
                    is NetworkResult.Success -> {
                        _vehicleTypes.value = result.data ?: emptyList()
                        _uiState.update { it.copy(isLoading = false, error = null) }
                    }
                    is NetworkResult.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
            }
        }
    }

    fun loadGoodsTypes(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            bookingRepository.getGoodsTypes(forceRefresh).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> _uiState.update { it.copy(isLoading = true) }
                    is NetworkResult.Success -> {
                        _goodsTypes.value = result.data ?: emptyList()
                        _uiState.update { it.copy(isLoading = false, error = null) }
                    }
                    is NetworkResult.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
            }
        }
    }

    fun loadRestrictedItems(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            bookingRepository.getRestrictedItems(forceRefresh).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> _uiState.update { it.copy(isLoading = true) }
                    is NetworkResult.Success -> {
                        _restrictedItems.value = result.data ?: emptyList()
                        _uiState.update { it.copy(isLoading = false, error = null) }
                    }
                    is NetworkResult.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
            }
        }
    }

    fun loadAvailableCoupons(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            bookingRepository.getAvailableCoupons(forceRefresh).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> _uiState.update { it.copy(isLoading = true) }
                    is NetworkResult.Success -> {
                        _availableCoupons.value = result.data ?: emptyList()
                        _uiState.update { it.copy(isLoading = false, error = null) }
                    }
                    is NetworkResult.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
            }
        }
    }

    // ============ BOOKING FLOW ============

    fun setPickupAddress(address: SavedAddress) {
        _uiState.update { it.copy(pickupAddress = address) }
        clearVehicleFares()
        if (_uiState.value.dropAddress != null) {
            calculateFaresForAllVehicles()
        }
    }

    fun setDropAddress(address: SavedAddress) {
        _uiState.update { it.copy(dropAddress = address) }
        clearVehicleFares()
        if (_uiState.value.pickupAddress != null) {
            calculateFaresForAllVehicles()
        }
    }

    fun setSelectedVehicle(vehicleType: VehicleTypeResponse) {
        _uiState.update {
            it.copy(
                selectedVehicleId = vehicleType.vehicleTypeId,
                baseFare = vehicleType.basePrice,
                finalFare = calculateFinalFare(vehicleType.basePrice)
            )
        }
    }

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

    fun applyCoupon(couponCode: String) {
        viewModelScope.launch {
            val orderValue = _uiState.value.baseFare
            bookingRepository.validateCoupon(couponCode, orderValue).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> _uiState.update { it.copy(isLoading = true) }
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
                    is NetworkResult.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
            }
        }
    }

    fun removeCoupon() {
        _uiState.update {
            it.copy(appliedCoupon = null, discount = 0, finalFare = calculateFinalFare(it.baseFare))
        }
    }

    fun setPaymentMethod(method: String) {
        _uiState.update { it.copy(paymentMethod = method) }
    }

    fun addGSTIN(gstin: String) {
        _uiState.update { it.copy(gstin = gstin) }
    }

    private fun calculateDiscount(coupon: CouponResponse, orderValue: Int): Int {
        return when (coupon.discountType) {
            "percentage" -> {
                val discount = (orderValue * coupon.discountValue / 100)
                coupon.maxDiscount?.let { minOf(discount, it) } ?: discount
            }
            "fixed" -> coupon.discountValue
            else -> 0
        }
    }

    private fun calculateFinalFare(baseFare: Int): Int {
        return maxOf(0, baseFare - _uiState.value.discount)
    }

    fun calculateRoute(pickupLat: Double, pickupLng: Double, dropLat: Double, dropLng: Double) {
        viewModelScope.launch {
            _isRouteLoading.value = true
            directionsRepository.getRouteInfo(pickupLat, pickupLng, dropLat, dropLng)
                .onSuccess { _routeInfo.value = it }
                .onFailure { _uiState.update { s -> s.copy(error = "Failed to calculate route") } }
            _isRouteLoading.value = false
        }
    }

    fun clearRouteInfo() {
        _routeInfo.value = null
    }

    fun confirmBooking() {
        viewModelScope.launch {
            val state = _uiState.value
            val selectedFare = _selectedFareDetails.value

            if (state.pickupAddress == null || state.dropAddress == null) {
                _uiState.update { it.copy(error = "Please select pickup and drop locations") }
                return@launch
            }

            val vehicleTypeId = selectedFare?.vehicleTypeId ?: state.selectedVehicleId
            if (vehicleTypeId == null) {
                _uiState.update { it.copy(error = "Please select a vehicle") }
                return@launch
            }

            val request = CreateBookingRequest(
                vehicleTypeId = vehicleTypeId,
                pickupAddress = state.pickupAddress.address,
                pickupLatitude = state.pickupAddress.latitude,
                pickupLongitude = state.pickupAddress.longitude,
                pickupContactName = state.pickupAddress.contactName ?: "",
                pickupContactPhone = state.pickupAddress.contactPhone ?: "",
                dropAddress = state.dropAddress.address,
                dropLatitude = state.dropAddress.latitude,
                dropLongitude = state.dropAddress.longitude,
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
                    is NetworkResult.Loading -> _uiState.update { it.copy(isLoading = true) }
                    is NetworkResult.Success -> {
                        result.data?.let { booking ->
                            _uiState.update { it.copy(isLoading = false, error = null) }
                            _navigationEvent.emit(BookingNavigationEvent.NavigateToSearchingRider(booking.bookingNumber))
                        }
                    }
                    is NetworkResult.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
            }
        }
    }

    fun cancelBooking(reason: String) {
        viewModelScope.launch {
            val bookingId = 12345
            bookingRepository.cancelBooking(bookingId, reason).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> _uiState.update { it.copy(isLoading = true) }
                    is NetworkResult.Success -> {
                        _uiState.update { it.copy(isLoading = false, error = null) }
                        _navigationEvent.emit(BookingNavigationEvent.NavigateToHome)
                    }
                    is NetworkResult.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun setPendingAddress(address: SavedAddress) {
        _uiState.update { it.copy(pendingAddress = address) }
    }

    fun clearPendingAddress() {
        _uiState.update { it.copy(pendingAddress = null) }
    }

    fun resetBooking() {
        fareCalculationJob?.cancel()
        _vehicleFares.value = emptyList()
        _selectedFareDetails.value = null
        _uiState.value = BookingUiState()
    }

    // ============ BOOK AGAIN ============

    fun prefillFromOrder(order: OrderResponse) {
        viewModelScope.launch {
            val pickupAddress = SavedAddress(
                addressId = "pickup_${order.bookingId}",
                addressType = "Other",
                label = order.pickupContactName ?: "Pickup",
                address = order.pickupAddress,
                latitude = order.pickupLatitude ?: 0.0,
                longitude = order.pickupLongitude ?: 0.0,
                contactName = order.pickupContactName,
                contactPhone = order.pickupContactPhone,
                isDefault = false
            )

            val dropAddress = SavedAddress(
                addressId = "drop_${order.bookingId}",
                addressType = "Other",
                label = order.dropContactName ?: "Drop",
                address = order.dropAddress,
                latitude = order.dropLatitude ?: 0.0,
                longitude = order.dropLongitude ?: 0.0,
                contactName = order.dropContactName,
                contactPhone = order.dropContactPhone,
                isDefault = false
            )

            _uiState.update {
                it.copy(
                    pickupAddress = pickupAddress,
                    dropAddress = dropAddress,
                    selectedGoodsTypeId = order.goodsTypeId,
                    isBookAgain = true,
                    originalOrderId = order.bookingId,
                    preferredVehicleTypeId = order.vehicleTypeId
                )
            }

            if ((order.pickupLatitude ?: 0.0) != 0.0 && (order.dropLatitude ?: 0.0) != 0.0) {
                calculateFaresForAllVehicles()
            }
        }
    }
}

/**
 * Booking UI State
 */
data class BookingUiState(
    val pickupAddress: SavedAddress? = null,
    val dropAddress: SavedAddress? = null,
    val pendingAddress: SavedAddress? = null,
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
    val error: String? = null,
    val hasFaresLoaded: Boolean = false,
    val isBookAgain: Boolean = false,
    val originalOrderId: Int? = null,
    val preferredVehicleTypeId: Int? = null
)

sealed class BookingNavigationEvent {
    data class NavigateToSearchingRider(val bookingId: String) : BookingNavigationEvent()
    object NavigateToHome : BookingNavigationEvent()
}