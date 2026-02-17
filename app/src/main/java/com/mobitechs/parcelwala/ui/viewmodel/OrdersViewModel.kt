// ui/viewmodel/OrdersViewModel.kt
package com.mobitechs.parcelwala.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobitechs.parcelwala.data.manager.ActiveBookingManager
import com.mobitechs.parcelwala.data.model.response.OrderResponse
import com.mobitechs.parcelwala.data.repository.OrdersRepository
import com.mobitechs.parcelwala.utils.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject


sealed class RatingSubmitState {
    object Idle : RatingSubmitState()
    object Submitting : RatingSubmitState()
    data class Success(val message: String) : RatingSubmitState()
    data class Error(val message: String) : RatingSubmitState()
}


@HiltViewModel
class OrdersViewModel @Inject constructor(
    private val ordersRepository: OrdersRepository,
    private val activeBookingManager: ActiveBookingManager
) : ViewModel() {

    // UI State
    private val _uiState = MutableStateFlow(OrdersUiState())
    val uiState: StateFlow<OrdersUiState> = _uiState.asStateFlow()

    private val _ratingSubmitState = MutableStateFlow<RatingSubmitState>(RatingSubmitState.Idle)
    val ratingSubmitState: StateFlow<RatingSubmitState> = _ratingSubmitState.asStateFlow()


    // Selected filter key (null = All)
    private val _selectedFilter = MutableStateFlow<String?>(null)
    val selectedFilter: StateFlow<String?> = _selectedFilter.asStateFlow()

    /**
     * Exposes whether there's an active booking.
     * Used by OrdersScreen to block "Book Again" action.
     */
    val hasActiveBooking: StateFlow<Boolean> = activeBookingManager.activeBooking
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // All orders cache (for client-side filtering)
    private var allOrders: List<OrderResponse> = emptyList()

    init {
        loadOrders()
    }

    /**
     * Load all orders from API/Mock
     */
    private fun loadOrders(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            ordersRepository.getMyBookings(
                status = null, // Always fetch all, filter client-side
                forceRefresh = forceRefresh
            ).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> {
                        _uiState.update { it.copy(isLoading = true, error = null) }
                    }
                    is NetworkResult.Success -> {
                        allOrders = result.data ?: emptyList()
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                orders = applyFilter(allOrders, _selectedFilter.value),
                                error = null
                            )
                        }
                    }
                    is NetworkResult.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = result.message ?: "Failed to load orders"
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Handle filter chip selection
     */
    fun onFilterSelected(filterKey: String?) {
        _selectedFilter.value = filterKey
        _uiState.update {
            it.copy(orders = applyFilter(allOrders, filterKey))
        }
    }

    /**
     * Apply filter to orders list.
     * Uses .lowercase() comparison to be safe regardless of backend casing.
     */
    private fun applyFilter(orders: List<OrderResponse>, filterKey: String?): List<OrderResponse> {
        if (filterKey == null) return orders

        return when (filterKey) {
            "searching" -> orders.filter {
                it.status.lowercase() == "searching"
            }
            "active" -> orders.filter {
                it.status.lowercase() in listOf(
                    "in_progress", "in progress", "assigned",
                    "arriving", "driver_arriving", "picked_up"
                )
            }
            "completed" -> orders.filter {
                it.status.lowercase() in listOf("delivery_completed", "completed")
            }
            "cancelled" -> orders.filter {
                it.status.lowercase() == "cancelled"
            }
            else -> orders
        }
    }

    /**
     * Refresh orders (pull-to-refresh)
     */
    fun refreshOrders() {
        loadOrders(forceRefresh = true)
    }

    fun submitRating(bookingId: Int, rating: Int, review: String) {
        viewModelScope.launch {
            _ratingSubmitState.value = RatingSubmitState.Submitting
            try {
                val result = ordersRepository.submitRating(bookingId.toString(), rating, review)

                result.onSuccess {
                    // Update local cache so UI reflects immediately
                    val updatedOrders = allOrders.map { order ->
                        if (order.bookingId == bookingId) {
                            order.copy(
                                rating = rating,
                                review = review.ifBlank { null }
                            )
                        } else {
                            order
                        }
                    }
                    allOrders = updatedOrders
                    _uiState.update {
                        it.copy(orders = applyFilter(updatedOrders, _selectedFilter.value))
                    }
                    _ratingSubmitState.value = RatingSubmitState.Success("Rating submitted successfully!")
                }.onFailure { e ->
                    if (e.message?.contains("already rated", ignoreCase = true) == true) {
                        // Already rated — still update local state to hide the Rate button
                        val updatedOrders = allOrders.map { order ->
                            if (order.bookingId == bookingId) {
                                order.copy(rating = rating, review = review.ifBlank { null })
                            } else order
                        }
                        allOrders = updatedOrders
                        _uiState.update {
                            it.copy(orders = applyFilter(updatedOrders, _selectedFilter.value))
                        }
                        _ratingSubmitState.value = RatingSubmitState.Success("Rating already submitted")
                    } else {
                        _ratingSubmitState.value = RatingSubmitState.Error(
                            e.message ?: "Failed to submit rating"
                        )
                    }
                }
            } catch (e: Exception) {
                _ratingSubmitState.value = RatingSubmitState.Error(
                    e.message ?: "Something went wrong"
                )
            }
        }
    }

    fun clearRatingState() {
        _ratingSubmitState.value = RatingSubmitState.Idle
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

/**
 * Orders UI State
 */
data class OrdersUiState(
    val isLoading: Boolean = false,
    val orders: List<OrderResponse> = emptyList(),
    val error: String? = null
)