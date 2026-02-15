package com.mobitechs.parcelwala.ui.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobitechs.parcelwala.data.manager.RazorpayManager
import com.mobitechs.parcelwala.data.model.request.CreateOrderResponse
import com.mobitechs.parcelwala.data.model.request.CreatePaymentOrderRequest
import com.mobitechs.parcelwala.data.model.request.TransactionResponse
import com.mobitechs.parcelwala.data.model.request.VerifyPaymentRequest
import com.mobitechs.parcelwala.data.model.request.VerifyPaymentResponse
import com.mobitechs.parcelwala.data.model.request.WalletTopupOrderRequest
import com.mobitechs.parcelwala.data.model.request.WalletTopupVerifyRequest
import com.mobitechs.parcelwala.data.repository.PaymentRepository
import com.mobitechs.parcelwala.utils.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Payment UI State
 */
data class PaymentUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val walletBalance: Double = 0.0,
    val transactions: List<TransactionResponse> = emptyList(),
    val isTransactionsLoading: Boolean = false,

    // Payment flow state
    val currentBookingId: Int? = null,
    val currentAmount: Double = 0.0,
    val currentPaymentMethod: String = "upi",
    val currentOrderResponse: CreateOrderResponse? = null,

    // Wallet topup state
    val topupAmount: Double = 0.0,
    val topupOrderResponse: CreateOrderResponse? = null
)

/**
 * Payment navigation events
 */
sealed class PaymentEvent {
    data class OpenRazorpayCheckout(
        val orderResponse: CreateOrderResponse,
        val isWalletTopup: Boolean = false
    ) : PaymentEvent()

    data class PaymentSuccess(
        val response: VerifyPaymentResponse
    ) : PaymentEvent()

    data class PaymentFailure(
        val message: String
    ) : PaymentEvent()

    data class WalletTopupSuccess(
        val newBalance: Double,
        val amount: Double
    ) : PaymentEvent()
}

@HiltViewModel
class PaymentViewModel @Inject constructor(
    private val paymentRepository: PaymentRepository,
    private val razorpayManager: RazorpayManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(PaymentUiState())
    val uiState: StateFlow<PaymentUiState> = _uiState.asStateFlow()

    private val _paymentEvent = MutableSharedFlow<PaymentEvent>()
    val paymentEvent: SharedFlow<PaymentEvent> = _paymentEvent.asSharedFlow()

    init {
        loadWalletBalance()
    }

    // ============================================================
    // BOOKING PAYMENT FLOW
    // ============================================================

    /**
     * Step 1: Initiate payment for a booking
     * Creates order on backend → Opens Razorpay checkout
     */
    fun initiateBookingPayment(
        bookingId: Int,
        amount: Double,
        paymentMethod: String
    ) {
        _uiState.update {
            it.copy(
                currentBookingId = bookingId,
                currentAmount = amount,
                currentPaymentMethod = paymentMethod
            )
        }

        viewModelScope.launch {
            paymentRepository.createPaymentOrder(
                bookingId = bookingId,
                amount = amount,
                paymentMethod = paymentMethod
            ).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> {
                        _uiState.update { it.copy(isLoading = true, error = null) }
                    }
                    is NetworkResult.Success -> {
                        result.data?.let { orderResponse ->
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    currentOrderResponse = orderResponse
                                )
                            }
                            // Emit event to open Razorpay checkout
                            _paymentEvent.emit(
                                PaymentEvent.OpenRazorpayCheckout(orderResponse)
                            )
                        }
                    }
                    is NetworkResult.Error -> {
                        _uiState.update {
                            it.copy(isLoading = false, error = result.message)
                        }
                        _paymentEvent.emit(
                            PaymentEvent.PaymentFailure(
                                result.message ?: "Failed to create payment order"
                            )
                        )
                    }
                }
            }
        }
    }

    /**
     * Step 2: Open Razorpay Checkout SDK
     * Called from Activity after receiving OpenRazorpayCheckout event
     */
    fun openRazorpayCheckout(activity: Activity, orderResponse: CreateOrderResponse) {
        razorpayManager.openCheckout(
            activity = activity,
            orderResponse = orderResponse,
            description = "Booking Payment - ₹${orderResponse.amount}"
        )
    }

    /**
     * Step 3: Handle Razorpay success callback
     * Verify payment on backend
     */
    fun onRazorpayPaymentSuccess(
        razorpayPaymentId: String,
        razorpayOrderId: String,
        razorpaySignature: String
    ) {
        val state = _uiState.value
        val bookingId = state.currentBookingId ?: return

        viewModelScope.launch {
            paymentRepository.verifyPayment(
                bookingId = bookingId,
                razorpayPaymentId = razorpayPaymentId,
                razorpayOrderId = razorpayOrderId,
                razorpaySignature = razorpaySignature,
                paymentMethod = state.currentPaymentMethod
            ).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                    is NetworkResult.Success -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = null,
                                currentOrderResponse = null
                            )
                        }
                        result.data?.let {
                            _paymentEvent.emit(PaymentEvent.PaymentSuccess(it))
                        }
                    }
                    is NetworkResult.Error -> {
                        _uiState.update {
                            it.copy(isLoading = false, error = result.message)
                        }
                        _paymentEvent.emit(
                            PaymentEvent.PaymentFailure(
                                result.message ?: "Payment verification failed"
                            )
                        )
                    }
                }
            }
        }
    }

    /**
     * Handle Razorpay failure callback
     */
    fun onRazorpayPaymentFailure(errorCode: Int, errorMessage: String?) {
        val message = when (errorCode) {
            Checkout.PAYMENT_CANCELED -> "Payment cancelled"
            Checkout.NETWORK_ERROR -> "Network error. Please check your connection"
            Checkout.INVALID_OPTIONS -> "Invalid payment options"
            else -> errorMessage ?: "Payment failed"
        }

        _uiState.update { it.copy(isLoading = false, error = message) }

        viewModelScope.launch {
            _paymentEvent.emit(PaymentEvent.PaymentFailure(message))
        }
    }

    // ============================================================
    // WALLET TOPUP FLOW
    // ============================================================

    fun initiateWalletTopup(amount: Double) {
        _uiState.update { it.copy(topupAmount = amount) }

        viewModelScope.launch {
            paymentRepository.createWalletTopupOrder(amount).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> {
                        _uiState.update { it.copy(isLoading = true, error = null) }
                    }
                    is NetworkResult.Success -> {
                        result.data?.let { orderResponse ->
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    topupOrderResponse = orderResponse
                                )
                            }
                            _paymentEvent.emit(
                                PaymentEvent.OpenRazorpayCheckout(
                                    orderResponse = orderResponse,
                                    isWalletTopup = true
                                )
                            )
                        }
                    }
                    is NetworkResult.Error -> {
                        _uiState.update {
                            it.copy(isLoading = false, error = result.message)
                        }
                    }
                }
            }
        }
    }

    fun onWalletTopupSuccess(
        razorpayPaymentId: String,
        razorpayOrderId: String,
        razorpaySignature: String
    ) {
        val amount = _uiState.value.topupAmount

        viewModelScope.launch {
            paymentRepository.verifyWalletTopup(
                amount = amount,
                razorpayPaymentId = razorpayPaymentId,
                razorpayOrderId = razorpayOrderId,
                razorpaySignature = razorpaySignature
            ).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> _uiState.update { it.copy(isLoading = true) }
                    is NetworkResult.Success -> {
                        result.data?.let { response ->
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    walletBalance = response.newBalance,
                                    topupOrderResponse = null
                                )
                            }
                            _paymentEvent.emit(
                                PaymentEvent.WalletTopupSuccess(
                                    newBalance = response.newBalance,
                                    amount = response.amount
                                )
                            )
                        }
                    }
                    is NetworkResult.Error -> {
                        _uiState.update { it.copy(isLoading = false, error = result.message) }
                    }
                }
            }
        }
    }

    // ============================================================
    // WALLET & TRANSACTIONS
    // ============================================================

    fun loadWalletBalance() {
        viewModelScope.launch {
            paymentRepository.getWalletBalance().collect { result ->
                when (result) {
                    is NetworkResult.Success -> {
                        _uiState.update {
                            it.copy(walletBalance = result.data?.balance ?: 0.0)
                        }
                    }
                    else -> { /* silently fail for balance */ }
                }
            }
        }
    }

    fun loadTransactions(page: Int = 1, type: String? = null) {
        viewModelScope.launch {
            paymentRepository.getTransactionHistory(page, type).collect { result ->
                when (result) {
                    is NetworkResult.Loading -> {
                        _uiState.update { it.copy(isTransactionsLoading = true) }
                    }
                    is NetworkResult.Success -> {
                        _uiState.update {
                            it.copy(
                                isTransactionsLoading = false,
                                transactions = result.data?.transactions ?: emptyList()
                            )
                        }
                    }
                    is NetworkResult.Error -> {
                        _uiState.update {
                            it.copy(isTransactionsLoading = false, error = result.message)
                        }
                    }
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    // Razorpay Checkout error codes
    private object Checkout {
        const val PAYMENT_CANCELED = 2
        const val NETWORK_ERROR = 0
        const val INVALID_OPTIONS = 1
    }



    /**
     * ⚠️ TEST MODE - Opens Razorpay directly without backend
     * Remove this when backend is ready
     */
    fun testWalletTopup(activity: Activity, amount: Double) {
        _uiState.update { it.copy(topupAmount = amount, isLoading = false) }

        val testOrder = CreateOrderResponse(
            orderId = "",
            amount = amount,
            amountInPaise = (amount * 100).toInt(),
            currency = "INR",
            receipt = "test_${System.currentTimeMillis()}",
            status = "created",
            razorpayKeyId = RazorpayManager.TEST_KEY_ID,
            customerName = "Test User",
            customerPhone = "9876543210",
            customerEmail = "test@parcelwala.com"
        )

        // Save so callback knows it's a topup
        _uiState.update { it.copy(topupOrderResponse = testOrder) }

        // Open Razorpay checkout directly
        razorpayManager.openCheckout(activity, testOrder, "Wallet Topup - ₹$amount")
    }

}