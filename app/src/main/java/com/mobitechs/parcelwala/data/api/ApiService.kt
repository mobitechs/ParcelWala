package com.mobitechs.parcelwala.data.api

import com.mobitechs.parcelwala.data.model.DistanceMatrixRequest
import com.mobitechs.parcelwala.data.model.DistanceMatrixResponse
import com.mobitechs.parcelwala.data.model.GoogleDirectionsResponse
import com.mobitechs.parcelwala.data.model.RatingSubmitResponse
import com.mobitechs.parcelwala.data.model.SubmitRatingRequest
import com.mobitechs.parcelwala.data.model.request.CalculateFareRequest
import com.mobitechs.parcelwala.data.model.request.CompleteProfileRequest
import com.mobitechs.parcelwala.data.model.request.CreateBookingRequest
import com.mobitechs.parcelwala.data.model.request.*
import com.mobitechs.parcelwala.data.model.request.SavedAddress
import com.mobitechs.parcelwala.data.model.request.SendOtpRequest
import com.mobitechs.parcelwala.data.model.request.ValidateCouponRequest
import com.mobitechs.parcelwala.data.model.request.VerifyOtpRequest
import com.mobitechs.parcelwala.data.model.request.WalletTopupOrderRequest
import com.mobitechs.parcelwala.data.model.request.WalletTopupVerifyRequest
import com.mobitechs.parcelwala.data.model.response.ApiResponse
import com.mobitechs.parcelwala.data.model.response.AuthTokens
import com.mobitechs.parcelwala.data.model.response.BookingResponse
import com.mobitechs.parcelwala.data.model.response.CouponResponse
import com.mobitechs.parcelwala.data.model.response.CreateBookingResponse
import com.mobitechs.parcelwala.data.model.response.FareCalculationResponse
import com.mobitechs.parcelwala.data.model.response.GoodsTypeResponse
import com.mobitechs.parcelwala.data.model.response.LoginData
import com.mobitechs.parcelwala.data.model.response.OrderDetailsResponse
import com.mobitechs.parcelwala.data.model.response.OrdersListResponse
import com.mobitechs.parcelwala.data.model.response.OtpData
import com.mobitechs.parcelwala.data.model.response.RestrictedItemResponse
import com.mobitechs.parcelwala.data.model.response.User
import com.mobitechs.parcelwala.data.model.response.VehicleTypeResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    @POST("auth/customer/send-otp")  // working
    suspend fun sendOtp(
        @Body request: SendOtpRequest
    ): Response<ApiResponse<OtpData>>

    @POST("auth/customer/verify-otp")
    suspend fun verifyOtp(
        @Body request: VerifyOtpRequest
    ): Response<ApiResponse<LoginData>>


    @PUT("customer/complete-profile")
    suspend fun completeProfile(
        @Body request: CompleteProfileRequest
    ): Response<ApiResponse<User>>


    @POST("auth/refresh-token")
    suspend fun refreshToken(
        @Body request: Map<String, String>
    ): Response<ApiResponse<AuthTokens>>

    @POST("auth/logout")
    suspend fun logout(
        @Body request: Map<String, String>
    ): Response<ApiResponse<Unit>>


    //Get available vehicle types
    @GET("vehicles/types")  // working
    suspend fun getVehicleTypes(): ApiResponse<List<VehicleTypeResponse>>

    // Get Goods Types
    @GET("goods/types") // working
    suspend fun getGoodsTypes(): ApiResponse<List<GoodsTypeResponse>>

    //    Get Restricted Items
    @GET("items/restricted")    // working
    suspend fun getRestrictedItems(): ApiResponse<List<RestrictedItemResponse>>


    //  Get saved addresses
    @GET("customer/addresses")  // working
    suspend fun getSavedAddresses(): ApiResponse<List<SavedAddress>>

    //    Save new address
    @POST("customer/addresses") // working
    suspend fun saveAddress(@Body address: SavedAddress): ApiResponse<SavedAddress>

    @PUT("customer/addresses/{addressId}")  // working
    suspend fun updateAddress(
        @Path("addressId") addressId: String,
        @Body address: SavedAddress
    ): ApiResponse<SavedAddress>

    //    Delete saved address
    @DELETE("customer/addresses/{addressId}")   // working
    suspend fun deleteAddress(@Path("addressId") addressId: String): ApiResponse<Unit>


    //    Get Available Coupons
    @GET("coupons/available")
    suspend fun getAvailableCoupons(): ApiResponse<List<CouponResponse>>


    //    Validate Coupon
    @POST("coupons/validate")
    suspend fun validateCoupon(@Body request: ValidateCouponRequest): ApiResponse<CouponResponse>

    //    Calculate fare
    @POST("bookings/calculate-fare")
    suspend fun calculateFare(@Body request: CalculateFareRequest): FareCalculationResponse

    //    Create new booking
    @POST("bookings")
    suspend fun createBooking(@Body request: CreateBookingRequest): CreateBookingResponse

    //    Cancel booking
    @POST("bookings/{bookingId}/cancel")
    suspend fun cancelBooking(
        @Path("bookingId") bookingId: Int,
        @Body reason: Map<String, String>
    ): ApiResponse<BookingResponse>


    //    @GET("orders")
    @GET("customer/bookings")
    suspend fun getMyOrders(
        @Query("status") status: String? = null,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): OrdersListResponse


    //    Get single order details
//    @GET("orders/{orderId}")
    @GET("bookings/{bookingId}")
    suspend fun getOrderDetails(
        @Path("orderId") orderId: Int
    ): OrderDetailsResponse

//--------------------------------------------------------------------------------------------------------

    // Option 1: Call your backend API
    @POST("bookings/distance-matrix")
    suspend fun getDistanceMatrix(
        @Header("Authorization") token: String,
        @Body request: DistanceMatrixRequest
    ): Response<DistanceMatrixResponse>

    // Option 2: Call Google Directions API directly
    @GET("https://maps.googleapis.com/maps/api/directions/json")
    suspend fun getDirections(
        @Query("origin") origin: String,           // "lat,lng"
        @Query("destination") destination: String,  // "lat,lng"
        @Query("mode") mode: String = "driving",
        @Query("key") apiKey: String
    ): Response<GoogleDirectionsResponse>

    @POST("bookings/{bookingId}/rating")
    suspend fun submitRating(
        @Path("bookingId") bookingId: String,
        @Body request: SubmitRatingRequest
    ): Response<RatingSubmitResponse>



    // ============ PAYMENT ENDPOINTS ============

    // Create Razorpay order for booking payment
    @POST("payments/create-order")
    suspend fun createPaymentOrder(
        @Body request: CreatePaymentOrderRequest
    ): ApiResponse<CreateOrderResponse>

    // Verify payment after Razorpay checkout
    @POST("payments/verify")
    suspend fun verifyPayment(
        @Body request: VerifyPaymentRequest
    ): ApiResponse<VerifyPaymentResponse>



    // Get transaction history
    @GET("customer/transactions")
    suspend fun getTransactions(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
        @Query("type") type: String? = null
    ): ApiResponse<TransactionListResponse>

// ============ WALLET ENDPOINTS ============

    // Get wallet balance
    @GET("customer/wallet")
    suspend fun getWalletBalance(): ApiResponse<WalletBalanceResponse>

    // Create wallet topup order
    @POST("payments/wallet/create-order")
    suspend fun createWalletTopupOrder(
        @Body request: WalletTopupOrderRequest
    ): ApiResponse<CreateOrderResponse>

    // Verify wallet topup
    @POST("payments/wallet/verify")
    suspend fun verifyWalletTopup(
        @Body request: WalletTopupVerifyRequest
    ): ApiResponse<WalletTopupResponse>



}

