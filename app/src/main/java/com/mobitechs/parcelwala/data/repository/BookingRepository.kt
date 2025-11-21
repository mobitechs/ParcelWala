package com.mobitechs.parcelwala.data.repository


// data/repository/BookingRepository.kt

import com.mobitechs.parcelwala.data.api.ApiService
import com.mobitechs.parcelwala.data.mock.MockBookingData
import com.mobitechs.parcelwala.data.model.request.*
import com.mobitechs.parcelwala.data.model.response.*
import com.mobitechs.parcelwala.utils.NetworkResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Repository for booking operations
 * Handles both mock and real API calls
 */
class BookingRepository @Inject constructor(
    private val apiService: ApiService
) {

    companion object {
        private const val USE_MOCK_DATA = true // ← Change to false when API ready
    }

    /**
     * Get available vehicle types
     */
    fun getVehicleTypes(): Flow<NetworkResult<List<VehicleTypeResponse>>> = flow {
        emit(NetworkResult.Loading())

        try {
            if (USE_MOCK_DATA) {
                delay(800) // Simulate network delay
                val mockData = MockBookingData.getVehicleTypes()
                emit(NetworkResult.Success(mockData))
            } else {
                val response = apiService.getVehicleTypes()
                if (response.success) {
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
     * Get saved addresses
     */
    fun getSavedAddresses(): Flow<NetworkResult<List<SavedAddress>>> = flow {
        emit(NetworkResult.Loading())

        try {
            if (USE_MOCK_DATA) {
                delay(600)
                val mockData = MockBookingData.getSavedAddresses()
                emit(NetworkResult.Success(mockData))
            } else {
                val response = apiService.getSavedAddresses()
                if (response.success && response.data != null) {
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
                var randomNo =(100..999).random()
                emit(NetworkResult.Success(address.copy(addressId = randomNo    .toString())))
            } else {
                val response = apiService.saveAddress(address)
                if (response.success && response.data != null) {
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
                emit(NetworkResult.Success(Unit))
            } else {
                val response = apiService.deleteAddress(addressId)
                if (response.success) {
                    emit(NetworkResult.Success(Unit))
                } else {
                    emit(NetworkResult.Error(response.message ?: "Failed to delete address"))
                }
            }
        } catch (e: Exception) {
            emit(NetworkResult.Error(e.message ?: "Network error"))
        }
    }

    /**
     * Calculate fare
     */
    fun calculateFare(request: CalculateFareRequest): Flow<NetworkResult<FareDetails>> = flow {
        emit(NetworkResult.Loading())

        try {
            if (USE_MOCK_DATA) {
                delay(700)
                // Calculate mock distance (simplified)
                val distanceKm = 8.5 // Mock distance
                val mockFare = MockBookingData.calculateFare(request.vehicleTypeId, distanceKm)
                emit(NetworkResult.Success(mockFare))
            } else {
                val response = apiService.calculateFare(request)
                if (response.success) {
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
                // Return empty list for now
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
}