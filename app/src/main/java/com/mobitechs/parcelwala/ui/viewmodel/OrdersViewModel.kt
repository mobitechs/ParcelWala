// ui/viewmodel/OrdersViewModel.kt
package com.mobitechs.parcelwala.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobitechs.parcelwala.data.model.response.OrderResponse
import com.mobitechs.parcelwala.data.repository.OrdersRepository
import com.mobitechs.parcelwala.utils.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Orders ViewModel
 * Handles order list operations only
 * No need to fetch order details - we pass the object directly
 */
@HiltViewModel
class OrdersViewModel @Inject constructor(
    private val ordersRepository: OrdersRepository
) : ViewModel() {

    // UI State
    private val _uiState = MutableStateFlow(OrdersUiState())
    val uiState: StateFlow<OrdersUiState> = _uiState.asStateFlow()

    // Selected Tab
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    init {
        loadOrders()
    }

    /**
     * Load orders from API/Mock
     */
    fun loadOrders(status: String? = null, forceRefresh: Boolean = false) {
        viewModelScope.launch {
            ordersRepository.getMyBookings(
                status = status,
                forceRefresh = forceRefresh
            ).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> {
                        _uiState.update { it.copy(isLoading = true, error = null) }
                    }
                    is NetworkResult.Success -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                orders = result.data ?: emptyList(),
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
     * Refresh orders
     */
    fun refreshOrders() {
        val status = getStatusForTab(_selectedTab.value)
        loadOrders(status = status, forceRefresh = true)
    }

    /**
     * Handle tab selection
     */
    fun onTabSelected(index: Int) {
        _selectedTab.value = index
        val status = getStatusForTab(index)
        loadOrders(status = status)
    }

    /**
     * Get status filter for tab index
     */
    private fun getStatusForTab(tabIndex: Int): String? {
        return when (tabIndex) {
            0 -> null // All/Past orders
            1 -> "Scheduled" // Scheduled orders
            else -> null
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

/**
 * Orders UI State - simplified
 */
data class OrdersUiState(
    val isLoading: Boolean = false,
    val orders: List<OrderResponse> = emptyList(),
    val error: String? = null
)