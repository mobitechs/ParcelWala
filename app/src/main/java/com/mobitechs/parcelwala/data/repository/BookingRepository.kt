// data/repository/BookingRepository.kt
package com.mobitechs.parcelwala.data.repository

import android.util.Log
import com.mobitechs.parcelwala.data.api.ApiService
import com.mobitechs.parcelwala.data.mock.MockAccountData
import com.mobitechs.parcelwala.data.mock.MockBookingData
import com.mobitechs.parcelwala.data.model.SubmitRatingRequest
import com.mobitechs.parcelwala.data.model.request.CalculateFareRequest
import com.mobitechs.parcelwala.data.model.request.CreateBookingRequest
import com.mobitechs.parcelwala.data.model.request.SavedAddress
import com.mobitechs.parcelwala.data.model.request.ValidateCouponRequest
import com.mobitechs.parcelwala.data.model.response.BookingResponse
import com.mobitechs.parcelwala.data.model.response.CouponResponse
import com.mobitechs.parcelwala.data.model.response.FareDetails
import com.mobitechs.parcelwala.data.model.response.GoodsTypeResponse
import com.mobitechs.parcelwala.data.model.response.RestrictedItemResponse
import com.mobitechs.parcelwala.data.model.response.VehicleTypeResponse
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
 * Repository for booking operations
 * Handles both mock and real API calls with caching
 */
@Singleton
class BookingRepository @Inject constructor(
    private val apiService: ApiService
) {

    // ============ IN-MEMORY CACHE ============
    private var cachedVehicleTypes: List<VehicleTypeResponse>? = null
    private var cachedGoodsTypes: List<GoodsTypeResponse>? = null
    private var cachedRestrictedItems: List<RestrictedItemResponse>? = null
    private var cachedCoupons: List<CouponResponse>? = null
    private var cachedSavedAddresses: MutableList<SavedAddress>? = null
    private var cacheTimestamp: Long = 0L
    private val CACHE_DURATION = 30 * 60 * 1000L // 30 minutes

    private fun isCacheValid(): Boolean {
        return System.currentTimeMillis() - cacheTimestamp < CACHE_DURATION
    }

    companion object {
        const val TAG = "BookingRepository"
    }

    fun clearCache() {
        cachedVehicleTypes = null
        cachedGoodsTypes = null
        cachedRestrictedItems = null
        cachedCoupons = null
        cachedSavedAddresses = null
        cacheTimestamp = 0L
    }

    // ============ STATIC DATA APIs ============

    fun getVehicleTypes(forceRefresh: Boolean = false): Flow<NetworkResult<List<VehicleTypeResponse>>> =
        flow {
            emit(NetworkResult.Loading())

            try {
                if (!forceRefresh && isCacheValid() && cachedVehicleTypes != null) {
                    emit(NetworkResult.Success(cachedVehicleTypes!!))
                    return@flow
                }

                val response = apiService.getVehicleTypes()
                if (response.success && response.data != null) {
                    cachedVehicleTypes = response.data
                    cacheTimestamp = System.currentTimeMillis()
                    emit(NetworkResult.Success(response.data))
                } else {
                    emit(
                        NetworkResult.Error(
                            response.message ?: "Failed to load vehicle types"
                        )
                    )
                }
            } catch (e: Exception) {
                emit(NetworkResult.Error(e.message ?: "Network error"))
            }
        }

    fun getGoodsTypes(forceRefresh: Boolean = false): Flow<NetworkResult<List<GoodsTypeResponse>>> =
        flow {
            emit(NetworkResult.Loading())

            try {
                if (!forceRefresh && isCacheValid() && cachedGoodsTypes != null) {
                    emit(NetworkResult.Success(cachedGoodsTypes!!))
                    return@flow
                }

                val response = apiService.getGoodsTypes()
                if (response.success && response.data != null) {
                    cachedGoodsTypes = response.data
                    cacheTimestamp = System.currentTimeMillis()
                    emit(NetworkResult.Success(response.data))
                } else {
                    emit(NetworkResult.Error(response.message ?: "Failed to load goods types"))
                }
            } catch (e: Exception) {
                emit(NetworkResult.Error(e.message ?: "Network error"))
            }
        }

    fun getRestrictedItems(forceRefresh: Boolean = false): Flow<NetworkResult<List<RestrictedItemResponse>>> =
        flow {
            emit(NetworkResult.Loading())

            try {
                if (!forceRefresh && isCacheValid() && cachedRestrictedItems != null) {
                    emit(NetworkResult.Success(cachedRestrictedItems!!))
                    return@flow
                }

                val response = apiService.getRestrictedItems()
                if (response.success && response.data != null) {
                    cachedRestrictedItems = response.data
                    cacheTimestamp = System.currentTimeMillis()
                    emit(NetworkResult.Success(response.data))
                } else {
                    emit(
                        NetworkResult.Error(
                            response.message ?: "Failed to load restricted items"
                        )
                    )
                }
            } catch (e: Exception) {
                emit(NetworkResult.Error(e.message ?: "Network error"))
            }
        }

    fun getAvailableCoupons(forceRefresh: Boolean = false): Flow<NetworkResult<List<CouponResponse>>> =
        flow {
            emit(NetworkResult.Loading())

            try {
                if (!forceRefresh && isCacheValid() && cachedCoupons != null) {
                    emit(NetworkResult.Success(cachedCoupons!!))
                    return@flow
                }

                val response = apiService.getAvailableCoupons()
                if (response.success && response.data != null) {
                    cachedCoupons = response.data
                    cacheTimestamp = System.currentTimeMillis()
                    emit(NetworkResult.Success(response.data))
                } else {
                    emit(NetworkResult.Error(response.message ?: "Failed to load coupons"))
                }
            } catch (e: Exception) {
                emit(NetworkResult.Error(e.message ?: "Network error"))
            }
        }

    fun validateCoupon(code: String, orderValue: Int): Flow<NetworkResult<CouponResponse>> = flow {
        emit(NetworkResult.Loading())

        try {
            val request = ValidateCouponRequest(code, orderValue)
            val response = apiService.validateCoupon(request)
            if (response.success && response.data != null) {
                emit(NetworkResult.Success(response.data))
            } else {
                emit(NetworkResult.Error(response.message ?: "Invalid coupon code"))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Network error"))
        }
    }

    // ============ SAVED ADDRESSES APIs ============

    fun getSavedAddresses(): Flow<NetworkResult<List<SavedAddress>>> = flow {
        emit(NetworkResult.Loading())

        try {
            val response = apiService.getSavedAddresses()
            if (response.success && response.data != null) {
                cachedSavedAddresses = response.data.toMutableList()
                emit(NetworkResult.Success(response.data))
            } else {
                emit(NetworkResult.Error(response.message ?: "Failed to load addresses"))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Network error"))
        }
    }

    fun saveAddress(address: SavedAddress): Flow<NetworkResult<SavedAddress>> = flow {
        emit(NetworkResult.Loading())

        try {
            val response = apiService.saveAddress(address)
            if (response.success && response.data != null) {
                cachedSavedAddresses?.add(response.data)
                emit(NetworkResult.Success(response.data))
            } else {
                emit(NetworkResult.Error(response.message ?: "Failed to save address"))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Network error"))
        }
    }

    fun updateAddress(address: SavedAddress): Flow<NetworkResult<SavedAddress>> = flow {
        emit(NetworkResult.Loading())

        try {
            val response = apiService.updateAddress(address.addressId, address)
            if (response.success && response.data != null) {
                val existingIndex = cachedSavedAddresses?.indexOfFirst {
                    it.addressId == response.data.addressId
                } ?: -1
                if (existingIndex >= 0) {
                    cachedSavedAddresses!![existingIndex] = response.data
                }
                emit(NetworkResult.Success(response.data))
            } else {
                emit(NetworkResult.Error(response.message ?: "Failed to update address"))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Network error"))
        }
    }

    fun deleteAddress(addressId: String): Flow<NetworkResult<Unit>> = flow {
        emit(NetworkResult.Loading())

        try {
            val response = apiService.deleteAddress(addressId)
            if (response.success) {
                cachedSavedAddresses?.removeIf { it.addressId == addressId }
                emit(NetworkResult.Success(Unit))
            } else {
                emit(NetworkResult.Error(response.message ?: "Failed to delete address"))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Network error"))
        }
    }

    // ============ FARE CALCULATION APIs ============

    /**
     * Calculate fares for ALL vehicle types based on pickup/drop locations
     * Returns List<FareDetails> - one for each vehicle type
     */
    fun calculateFaresForAllVehicles(
        request: CalculateFareRequest
    ): Flow<NetworkResult<List<FareDetails>>> = flow {
        emit(NetworkResult.Loading())

        try {
            val response = apiService.calculateFare(request)
            if (response.success && response.data.isNotEmpty()) {
                emit(NetworkResult.Success(response.data))
            } else {
                emit(NetworkResult.Error(response.message ?: "Failed to calculate fares"))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Network error"))
        }
    }


    fun createBooking(request: CreateBookingRequest): Flow<NetworkResult<BookingResponse>> = flow {
        emit(NetworkResult.Loading())

        try {
            val response = apiService.createBooking(request)
            if (response.success) {
                emit(NetworkResult.Success(response.data))
            } else {
                emit(NetworkResult.Error(response.message ?: "Failed to create booking"))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Network error"))
        }
    }


    fun cancelBooking(bookingId: Int, reason: String): Flow<NetworkResult<Unit>> = flow {
        emit(NetworkResult.Loading())

        try {
            val reasonMap = mapOf("reason" to reason)
            val response = apiService.cancelBooking(bookingId, reasonMap)
            if (response.success) {
                emit(NetworkResult.Success(Unit))
            } else {
                emit(NetworkResult.Error(response.message ?: "Failed to cancel booking"))
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Network error"))
        }
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