package com.mobitechs.parcelwala.data.api

import com.mobitechs.parcelwala.data.model.request.*
import com.mobitechs.parcelwala.data.model.response.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @POST("auth/customer/send-otp")
    suspend fun sendOtp(
        @Body request: SendOtpRequest
    ): Response<ApiResponse<OtpData>>

    @POST("auth/customer/verify-otp")
    suspend fun verifyOtp(
        @Body request: VerifyOtpRequest
    ): Response<ApiResponse<LoginData>>

    @POST("auth/refresh-token")
    suspend fun refreshToken(
        @Body request: Map<String, String>
    ): Response<ApiResponse<AuthTokens>>

    @POST("auth/logout")
    suspend fun logout(
        @Body request: Map<String, String>
    ): Response<ApiResponse<Unit>>

    @PUT("auth/customer/complete-profile")
    suspend fun completeProfile(
        @Body request: CompleteProfileRequest
    ): Response<ApiResponse<User>>



    /**
     * Get available vehicle types
     */

    @GET("vehicles/types")
    suspend fun getVehicleTypes(): ApiResponse<List<VehicleTypeResponse>>

    /**
     * Get Goods Types
     */
    @GET("goods/types")
    suspend fun getGoodsTypes(): ApiResponse<List<GoodsTypeResponse>>

    /**
     * Get Restricted Items
     */
    @GET("items/restricted")
    suspend fun getRestrictedItems(): ApiResponse<List<RestrictedItemResponse>>

    /**
     * Get Available Coupons
     */
    @GET("coupons/available")
    suspend fun getAvailableCoupons(): ApiResponse<List<CouponResponse>>

    /**
     * Validate Coupon
     */
    @POST("coupons/validate")
    suspend fun validateCoupon(@Body request: ValidateCouponRequest): ApiResponse<CouponResponse>


//    @GET("customer/vehicle-types")
//    suspend fun getVehicleTypes(): VehicleTypeListResponse

    /**
     * Get saved addresses
     */
    @GET("customer/addresses")
    suspend fun getSavedAddresses(): ApiResponse<List<SavedAddress>>

    /**
     * Save new address
     */
    @POST("customer/addresses")
    suspend fun saveAddress(@Body address: SavedAddress): ApiResponse<SavedAddress>

    /**
     * Delete saved address
     */
    @DELETE("customer/addresses/{addressId}")
    suspend fun deleteAddress(@Path("addressId") addressId: Int): ApiResponse<Unit>

    /**
     * Calculate fare
     */
    @POST("bookings/calculate-fare")
    suspend fun calculateFare(@Body request: CalculateFareRequest): FareCalculationResponse

    /**
     * Create new booking
     */
    @POST("bookings")
    suspend fun createBooking(@Body request: CreateBookingRequest): CreateBookingResponse

    /**
     * Get booking details
     */
    @GET("bookings/{bookingId}")
    suspend fun getBookingDetails(@Path("bookingId") bookingId: Int): ApiResponse<BookingResponse>

    /**
     * Get my bookings list
     */
    @GET("customer/bookings")
    suspend fun getMyBookings(
        @Query("status") status: String? = null,
        @Query("page") page: Int = 1
    ): ApiResponse<List<BookingResponse>>

    /**
     * Cancel booking
     */
    @POST("bookings/{bookingId}/cancel")
    suspend fun cancelBooking(
        @Path("bookingId") bookingId: Int,
        @Body reason: Map<String, String>
    ): ApiResponse<BookingResponse>


}