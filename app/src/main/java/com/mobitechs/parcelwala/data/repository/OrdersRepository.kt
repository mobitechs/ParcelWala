// data/repository/OrdersRepository.kt
package com.mobitechs.parcelwala.data.repository

import android.util.Log
import com.mobitechs.parcelwala.data.api.ApiService
import com.mobitechs.parcelwala.data.mock.MockOrdersData
import com.mobitechs.parcelwala.data.model.SubmitRatingRequest
import com.mobitechs.parcelwala.data.model.response.OrderResponse
import com.mobitechs.parcelwala.data.repository.BookingRepository.Companion.TAG
import com.mobitechs.parcelwala.utils.Constants.USE_MOCK_DATA
import com.mobitechs.parcelwala.utils.NetworkResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for order operations
 * Handles both mock and real API calls with caching
 */
@Singleton
class OrdersRepository @Inject constructor(
    private val apiService: ApiService
) {

    // ============ IN-MEMORY CACHE ============
    private var cachedOrders: List<OrderResponse>? = null
    private var cacheTimestamp: Long = 0L
    private val CACHE_DURATION = 5 * 60 * 1000L // 5 minutes

    /**
     * Check if cache is valid
     */
    private fun isCacheValid(): Boolean {
        return System.currentTimeMillis() - cacheTimestamp < CACHE_DURATION
    }

    /**
     * Clear all cached data
     */
    fun clearCache() {
        cachedOrders = null
        cacheTimestamp = 0L
    }

    // ============ ORDER APIs ============

    /**
     * Get user's bookings/orders (with caching)
     */
    fun getMyBookings(
        status: String? = null,
        forceRefresh: Boolean = false
    ): Flow<NetworkResult<List<OrderResponse>>> = flow {
        emit(NetworkResult.Loading())

        try {
            val response = apiService.getMyOrders(status = status)
            if (response.success && response.data != null) {
                cachedOrders = response.data
                cacheTimestamp = System.currentTimeMillis()
                emit(NetworkResult.Success(response.data))
            } else {
                emit(NetworkResult.Error(response.message ?: "Failed to load orders"))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Network error"))
        }
    }

    /**
     * Get booking details by ID
     */
    fun getBookingDetails(bookingId: Int): Flow<NetworkResult<OrderResponse>> = flow {
        emit(NetworkResult.Loading())

        try {
            if (USE_MOCK_DATA) {
                delay(500) // Simulate network delay
                val order = MockOrdersData.getMockOrderById(bookingId)
                if (order != null) {
                    emit(NetworkResult.Success(order))
                } else {
                    emit(NetworkResult.Error("Order not found"))
                }
            } else {
                val response = apiService.getOrderDetails(bookingId)
                if (response.success && response.data != null) {
                    emit(NetworkResult.Success(response.data))
                } else {
                    emit(NetworkResult.Error(response.message ?: "Failed to load order details"))
                }
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Network error"))
        }
    }



    /**
     * Filter orders by status
     */
    private fun filterOrders(orders: List<OrderResponse>, status: String?): List<OrderResponse> {
        if (status.isNullOrEmpty()) return orders
        return orders.filter { it.status == status }
    }



    suspend fun submitRating(
        bookingId: String,
        rating: Int,
        feedback: String?
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Submitting rating for booking: $bookingId, Rating: $rating")

            val response = apiService.submitRating(
                bookingId = bookingId,
                request = SubmitRatingRequest(
                    rating = rating,
                    feedback = feedback
                )
            )

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                Log.d(TAG, "Rating submitted: ${body.message}")
                Result.success(body.success)
            } else {
                val error = response.errorBody()?.string() ?: "Failed to submit rating"
                Log.e(TAG, "Submit rating failed: $error")
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Submit rating exception", e)
            Result.failure(e)
        }
    }
}