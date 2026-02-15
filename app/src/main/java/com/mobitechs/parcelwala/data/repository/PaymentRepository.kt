package com.mobitechs.parcelwala.data.repository

import com.mobitechs.parcelwala.data.api.ApiService
import com.mobitechs.parcelwala.data.model.request.CreateOrderResponse
import com.mobitechs.parcelwala.data.model.request.CreatePaymentOrderRequest
import com.mobitechs.parcelwala.data.model.request.TransactionListResponse
import com.mobitechs.parcelwala.data.model.request.TransactionResponse
import com.mobitechs.parcelwala.data.model.request.VerifyPaymentRequest
import com.mobitechs.parcelwala.data.model.request.VerifyPaymentResponse
import com.mobitechs.parcelwala.data.model.request.WalletBalanceResponse
import com.mobitechs.parcelwala.data.model.request.WalletTopupOrderRequest
import com.mobitechs.parcelwala.data.model.request.WalletTopupResponse
import com.mobitechs.parcelwala.data.model.request.WalletTopupVerifyRequest
import com.mobitechs.parcelwala.utils.NetworkResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PaymentRepository @Inject constructor(
    private val apiService: ApiService
) {

    // ============ BOOKING PAYMENT ============

    /**
     * Step 1: Create Razorpay order on backend
     * Backend creates order with Razorpay API and returns order_id
     */
    fun createPaymentOrder(
        bookingId: Int,
        amount: Double,
        paymentMethod: String
    ): Flow<NetworkResult<CreateOrderResponse>> = flow {
        emit(NetworkResult.Loading())
        try {
            val request = CreatePaymentOrderRequest(
                bookingId = bookingId,
                amount = amount,
                paymentMethod = paymentMethod
            )
            val response = apiService.createPaymentOrder(request)

            if (response.success && response.data != null) {
                emit(NetworkResult.Success(response.data))
            } else {
                emit(NetworkResult.Error(response.message ?: "Failed to create payment order"))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Network error"))
        }
    }

    /**
     * Step 3: Verify payment on backend after Razorpay checkout
     * Backend verifies signature and updates transaction
     */
    fun verifyPayment(
        bookingId: Int,
        razorpayPaymentId: String,
        razorpayOrderId: String,
        razorpaySignature: String,
        paymentMethod: String
    ): Flow<NetworkResult<VerifyPaymentResponse>> = flow {
        emit(NetworkResult.Loading())
        try {
            val request = VerifyPaymentRequest(
                bookingId = bookingId,
                razorpayPaymentId = razorpayPaymentId,
                razorpayOrderId = razorpayOrderId,
                razorpaySignature = razorpaySignature,
                paymentMethod = paymentMethod
            )
            val response = apiService.verifyPayment(request)

            if (response.success && response.data != null) {
                emit(NetworkResult.Success(response.data))
            } else {
                emit(NetworkResult.Error(response.message ?: "Payment verification failed"))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Network error"))
        }
    }

    // ============ WALLET ============

    fun getWalletBalance(): Flow<NetworkResult<WalletBalanceResponse>> = flow {
        emit(NetworkResult.Loading())
        try {
            val response = apiService.getWalletBalance()
            if (response.success && response.data != null) {
                emit(NetworkResult.Success(response.data))
            } else {
                emit(NetworkResult.Error(response.message ?: "Failed to get wallet balance"))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Network error"))
        }
    }

    fun createWalletTopupOrder(amount: Double): Flow<NetworkResult<CreateOrderResponse>> = flow {
        emit(NetworkResult.Loading())
        try {
            val request = WalletTopupOrderRequest(amount = amount)
            val response = apiService.createWalletTopupOrder(request)
            if (response.success && response.data != null) {
                emit(NetworkResult.Success(response.data))
            } else {
                emit(NetworkResult.Error(response.message ?: "Failed to create topup order"))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Network error"))
        }
    }

    fun verifyWalletTopup(
        amount: Double,
        razorpayPaymentId: String,
        razorpayOrderId: String,
        razorpaySignature: String
    ): Flow<NetworkResult<WalletTopupResponse>> = flow {
        emit(NetworkResult.Loading())
        try {
            val request = WalletTopupVerifyRequest(
                amount = amount,
                razorpayPaymentId = razorpayPaymentId,
                razorpayOrderId = razorpayOrderId,
                razorpaySignature = razorpaySignature
            )
            val response = apiService.verifyWalletTopup(request)
            if (response.success && response.data != null) {
                emit(NetworkResult.Success(response.data))
            } else {
                emit(NetworkResult.Error(response.message ?: "Topup verification failed"))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Network error"))
        }
    }

    // ============ TRANSACTIONS ============

    fun getTransactionHistory(
        page: Int = 1,
        type: String? = null
    ): Flow<NetworkResult<TransactionListResponse>> = flow {
        emit(NetworkResult.Loading())
        try {
            val response = apiService.getTransactions(page = page, type = type)
            if (response.success && response.data != null) {
                emit(NetworkResult.Success(response.data))
            } else {
                emit(NetworkResult.Error(response.message ?: "Failed to load transactions"))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Network error"))
        }
    }
}