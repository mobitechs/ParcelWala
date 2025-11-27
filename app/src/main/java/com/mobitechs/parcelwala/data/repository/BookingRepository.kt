// data/repository/BookingRepository.kt
package com.mobitechs.parcelwala.data.repository

import com.mobitechs.parcelwala.data.api.ApiService
import com.mobitechs.parcelwala.data.mock.MockAccountData
import com.mobitechs.parcelwala.data.mock.MockBookingData
import com.mobitechs.parcelwala.data.model.request.*
import com.mobitechs.parcelwala.data.model.response.*
import com.mobitechs.parcelwala.utils.Constants.USE_MOCK_DATA
import com.mobitechs.parcelwala.utils.NetworkResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for booking operations
 * Handles both mock and real API calls with caching
 *
 * Features:
 * - Vehicle types management
 * - Goods types management
 * - Coupon validation
 * - Address CRUD operations
 * - Fare calculation
 * - Booking creation and management
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
        cachedVehicleTypes = null
        cachedGoodsTypes = null
        cachedRestrictedItems = null
        cachedCoupons = null
        cachedSavedAddresses = null
        cacheTimestamp = 0L
    }

    // ============ STATIC DATA APIs ============

    /**
     * Get available vehicle types (with caching)
     */
    fun getVehicleTypes(forceRefresh: Boolean = false): Flow<NetworkResult<List<VehicleTypeResponse>>> = flow {
        emit(NetworkResult.Loading())

        try {
            // Return cached data if available and valid
            if (!forceRefresh && isCacheValid() && cachedVehicleTypes != null) {
                emit(NetworkResult.Success(cachedVehicleTypes!!))
                return@flow
            }

            if (USE_MOCK_DATA) {
                delay(800) // Simulate network delay
                val mockData = MockBookingData.getVehicleTypes()
                cachedVehicleTypes = mockData
                cacheTimestamp = System.currentTimeMillis()
                emit(NetworkResult.Success(mockData))
            } else {
                val response = apiService.getVehicleTypes()
                if (response.success && response.data != null) {
                    cachedVehicleTypes = response.data
                    cacheTimestamp = System.currentTimeMillis()
                    emit(NetworkResult.Success(response.data))
                } else {
                    emit(NetworkResult.Error(response.message ?: "Failed to load vehicle types"))
                }
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Network error"))
        }
    }

    /**
     * Get goods types (with caching)
     */
    fun getGoodsTypes(forceRefresh: Boolean = false): Flow<NetworkResult<List<GoodsTypeResponse>>> = flow {
        emit(NetworkResult.Loading())

        try {
            if (!forceRefresh && isCacheValid() && cachedGoodsTypes != null) {
                emit(NetworkResult.Success(cachedGoodsTypes!!))
                return@flow
            }

            if (USE_MOCK_DATA) {
                delay(600)
                val mockData = MockBookingData.getGoodsTypes()
                cachedGoodsTypes = mockData
                cacheTimestamp = System.currentTimeMillis()
                emit(NetworkResult.Success(mockData))
            } else {
                val response = apiService.getGoodsTypes()
                if (response.success && response.data != null) {
                    cachedGoodsTypes = response.data
                    cacheTimestamp = System.currentTimeMillis()
                    emit(NetworkResult.Success(response.data))
                } else {
                    emit(NetworkResult.Error(response.message ?: "Failed to load goods types"))
                }
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Network error"))
        }
    }

    /**
     * Get restricted items (with caching)
     */
    fun getRestrictedItems(forceRefresh: Boolean = false): Flow<NetworkResult<List<RestrictedItemResponse>>> = flow {
        emit(NetworkResult.Loading())

        try {
            if (!forceRefresh && isCacheValid() && cachedRestrictedItems != null) {
                emit(NetworkResult.Success(cachedRestrictedItems!!))
                return@flow
            }

            if (USE_MOCK_DATA) {
                delay(500)
                val mockData = MockBookingData.getRestrictedItems()
                cachedRestrictedItems = mockData
                cacheTimestamp = System.currentTimeMillis()
                emit(NetworkResult.Success(mockData))
            } else {
                val response = apiService.getRestrictedItems()
                if (response.success && response.data != null) {
                    cachedRestrictedItems = response.data
                    cacheTimestamp = System.currentTimeMillis()
                    emit(NetworkResult.Success(response.data))
                } else {
                    emit(NetworkResult.Error(response.message ?: "Failed to load restricted items"))
                }
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Network error"))
        }
    }

    /**
     * Get available coupons (with caching)
     */
    fun getAvailableCoupons(forceRefresh: Boolean = false): Flow<NetworkResult<List<CouponResponse>>> = flow {
        emit(NetworkResult.Loading())

        try {
            if (!forceRefresh && isCacheValid() && cachedCoupons != null) {
                emit(NetworkResult.Success(cachedCoupons!!))
                return@flow
            }

            if (USE_MOCK_DATA) {
                delay(700)
                val mockData = MockBookingData.getAvailableCoupons()
                cachedCoupons = mockData
                cacheTimestamp = System.currentTimeMillis()
                emit(NetworkResult.Success(mockData))
            } else {
                val response = apiService.getAvailableCoupons()
                if (response.success && response.data != null) {
                    cachedCoupons = response.data
                    cacheTimestamp = System.currentTimeMillis()
                    emit(NetworkResult.Success(response.data))
                } else {
                    emit(NetworkResult.Error(response.message ?: "Failed to load coupons"))
                }
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Network error"))
        }
    }

    /**
     * Validate coupon
     */
    fun validateCoupon(code: String, orderValue: Int): Flow<NetworkResult<CouponResponse>> = flow {
        emit(NetworkResult.Loading())

        try {
            if (USE_MOCK_DATA) {
                delay(500)
                val coupon = MockBookingData.validateCoupon(code, orderValue)
                if (coupon != null) {
                    emit(NetworkResult.Success(coupon))
                } else {
                    emit(NetworkResult.Error("Invalid coupon code"))
                }
            } else {
                val request = ValidateCouponRequest(code, orderValue)
                val response = apiService.validateCoupon(request)
                if (response.success && response.data != null) {
                    emit(NetworkResult.Success(response.data))
                } else {
                    emit(NetworkResult.Error(response.message ?: "Invalid coupon code"))
                }
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Network error"))
        }
    }

    // ============ SAVED ADDRESSES APIs ============

    /**
     * Get saved addresses
     * Returns cached addresses if available, otherwise fetches from API/Mock
     */
    fun getSavedAddresses(): Flow<NetworkResult<List<SavedAddress>>> = flow {
        emit(NetworkResult.Loading())

        try {
            if (USE_MOCK_DATA) {
                delay(600)

                // Initialize cache from mock data if empty
                if (cachedSavedAddresses == null) {
                    cachedSavedAddresses = MockAccountData.getSavedAddresses().toMutableList()
                }

                emit(NetworkResult.Success(cachedSavedAddresses!!.toList()))
            } else {
                val response = apiService.getSavedAddresses()
                if (response.success && response.data != null) {
                    cachedSavedAddresses = response.data.toMutableList()
                    emit(NetworkResult.Success(response.data))
                } else {
                    emit(NetworkResult.Error(response.message ?: "Failed to load addresses"))
                }
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Network error"))
        }
    }

    /**
     * Save new address
     */
    fun saveAddress(address: SavedAddress): Flow<NetworkResult<SavedAddress>> = flow {
        emit(NetworkResult.Loading())

        try {
            if (USE_MOCK_DATA) {
                delay(500)

                // Generate new ID for new addresses
                val newAddress = if (address.addressId.startsWith("addr_") ||
                    address.addressId.startsWith("map_") ||
                    address.addressId == "0" ||
                    address.addressId.isBlank()) {
                    address.copy(addressId = MockAccountData.generateAddressId())
                } else {
                    address
                }

                // Add to cache
                if (cachedSavedAddresses == null) {
                    cachedSavedAddresses = mutableListOf()
                }

                // Check if updating existing or adding new
                val existingIndex = cachedSavedAddresses!!.indexOfFirst { it.addressId == newAddress.addressId }
                if (existingIndex >= 0) {
                    cachedSavedAddresses!![existingIndex] = newAddress
                } else {
                    cachedSavedAddresses!!.add(newAddress)
                }

                emit(NetworkResult.Success(newAddress))
            } else {
                val response = apiService.saveAddress(address)
                if (response.success && response.data != null) {
                    // Update cache
                    if (cachedSavedAddresses == null) {
                        cachedSavedAddresses = mutableListOf()
                    }
                    val existingIndex = cachedSavedAddresses!!.indexOfFirst { it.addressId == response.data.addressId }
                    if (existingIndex >= 0) {
                        cachedSavedAddresses!![existingIndex] = response.data
                    } else {
                        cachedSavedAddresses!!.add(response.data)
                    }

                    emit(NetworkResult.Success(response.data))
                } else {
                    emit(NetworkResult.Error(response.message ?: "Failed to save address"))
                }
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Network error"))
        }
    }

    /**
     * Delete saved address
     */
    fun deleteAddress(addressId: Int): Flow<NetworkResult<Unit>> = flow {
        emit(NetworkResult.Loading())

        try {
            if (USE_MOCK_DATA) {
                delay(400)

                // Remove from cache
                cachedSavedAddresses?.removeIf { it.addressId == addressId.toString() }

                emit(NetworkResult.Success(Unit))
            } else {
                val response = apiService.deleteAddress(addressId)
                if (response.success) {
                    // Update cache
                    cachedSavedAddresses?.removeIf { it.addressId == addressId.toString() }

                    emit(NetworkResult.Success(Unit))
                } else {
                    emit(NetworkResult.Error(response.message ?: "Failed to delete address"))
                }
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Network error"))
        }
    }

    // ============ BOOKING APIs ============

    /**
     * Calculate fare
     */
    fun calculateFare(request: CalculateFareRequest): Flow<NetworkResult<FareDetails>> = flow {
        emit(NetworkResult.Loading())

        try {
            if (USE_MOCK_DATA) {
                delay(700)
                val distanceKm = 8.5 // Mock distance
                val mockFare = MockBookingData.calculateFare(request.vehicleTypeId, distanceKm)
                emit(NetworkResult.Success(mockFare))
            } else {
                val response = apiService.calculateFare(request)
                if (response.success && response.data != null) {
                    emit(NetworkResult.Success(response.data))
                } else {
                    emit(NetworkResult.Error(response.message ?: "Failed to calculate fare"))
                }
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Network error"))
        }
    }

    /**
     * Create new booking
     */
    fun createBooking(request: CreateBookingRequest): Flow<NetworkResult<BookingResponse>> = flow {
        emit(NetworkResult.Loading())

        try {
            if (USE_MOCK_DATA) {
                delay(1000)
                val mockBooking = MockBookingData.createBooking(
                    vehicleTypeId = request.vehicleTypeId,
                    pickupAddress = request.pickupAddress,
                    dropAddress = request.dropAddress
                )
                emit(NetworkResult.Success(mockBooking))
            } else {
                val response = apiService.createBooking(request)
                if (response.success && response.data != null) {
                    emit(NetworkResult.Success(response.data))
                } else {
                    emit(NetworkResult.Error(response.message ?: "Failed to create booking"))
                }
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Network error"))
        }
    }

    /**
     * Get my bookings
     */
    fun getMyBookings(status: String? = null): Flow<NetworkResult<List<BookingResponse>>> = flow {
        emit(NetworkResult.Loading())

        try {
            if (USE_MOCK_DATA) {
                delay(800)
                emit(NetworkResult.Success(emptyList()))
            } else {
                val response = apiService.getMyBookings(status = status)
                if (response.success && response.data != null) {
                    emit(NetworkResult.Success(response.data))
                } else {
                    emit(NetworkResult.Error(response.message ?: "Failed to load bookings"))
                }
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Network error"))
        }
    }

    /**
     * Get booking details
     */
    fun getBookingDetails(bookingId: Int): Flow<NetworkResult<BookingResponse>> = flow {
        emit(NetworkResult.Loading())

        try {
            if (USE_MOCK_DATA) {
                delay(600)
                val mockBooking = MockBookingData.createBooking(1, "Pickup Location", "Drop Location")
                emit(NetworkResult.Success(mockBooking.copy(bookingId = bookingId)))
            } else {
                val response = apiService.getBookingDetails(bookingId)
                if (response.success && response.data != null) {
                    emit(NetworkResult.Success(response.data))
                } else {
                    emit(NetworkResult.Error(response.message ?: "Failed to load booking details"))
                }
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Network error"))
        }
    }

    /**
     * Cancel booking with reason
     */
    fun cancelBooking(bookingId: Int, reason: String): Flow<NetworkResult<Unit>> = flow {
        emit(NetworkResult.Loading())

        try {
            if (USE_MOCK_DATA) {
                delay(500)
                emit(NetworkResult.Success(Unit))
            } else {
                val reasonMap = mapOf("reason" to reason)
                val response = apiService.cancelBooking(bookingId, reasonMap)
                if (response.success) {
                    emit(NetworkResult.Success(Unit))
                } else {
                    emit(NetworkResult.Error(response.message ?: "Failed to cancel booking"))
                }
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Network error"))
        }
    }
}