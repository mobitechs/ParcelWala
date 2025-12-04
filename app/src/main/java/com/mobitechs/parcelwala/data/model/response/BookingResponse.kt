// data/model/response/BookingResponse.kt
package com.mobitechs.parcelwala.data.model.response

import com.google.gson.annotations.SerializedName

/**
 * Booking response from API
 */
data class BookingResponse(
    @SerializedName("booking_id")
    val bookingId: Int,

    @SerializedName("booking_number")
    val bookingNumber: String,

    @SerializedName("vehicle_type")
    val vehicleType: String,

    @SerializedName("pickup_address")
    val pickupAddress: String,

    @SerializedName("drop_address")
    val dropAddress: String,

    @SerializedName("status")
    val status: String,

    @SerializedName("fare")
    val fare: Int,

    @SerializedName("distance")
    val distance: Double,

    @SerializedName("created_at")
    val createdAt: String,

    @SerializedName("driver_name")
    val driverName: String? = null,

    @SerializedName("driver_phone")
    val driverPhone: String? = null,

    @SerializedName("vehicle_number")
    val vehicleNumber: String? = null
)

/**
 * Driver information
 */
data class DriverInfo(
    @SerializedName("driver_id")
    val driverId: Int,

    @SerializedName("name")
    val name: String,

    @SerializedName("phone")
    val phone: String,

    @SerializedName("profile_image")
    val profileImage: String?,

    @SerializedName("rating")
    val rating: Double,

    @SerializedName("vehicle_number")
    val vehicleNumber: String?,

    @SerializedName("current_latitude")
    val currentLatitude: Double?,

    @SerializedName("current_longitude")
    val currentLongitude: Double?
)

/**
 * Fare calculation response wrapper
 * Returns List<FareDetails> for all vehicle types
 */
data class FareCalculationResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("data")
    val data: List<FareDetails>,

    @SerializedName("message")
    val message: String?
)

/**
 * Fare details for a vehicle type
 * Includes vehicle info + calculated fare
 */
data class FareDetails(
    // Vehicle Information
    @SerializedName("vehicle_type_id")
    val vehicleTypeId: Int,

    @SerializedName("vehicle_type_name")
    val vehicleTypeName: String,

    @SerializedName("vehicle_type_description")
    val vehicleTypeDescription: String,

    @SerializedName("vehicle_type_icon")
    val vehicleTypeIcon: String,

    @SerializedName("capacity")
    val capacity: String,

    // Fare Calculation
    @SerializedName("base_fare")
    val baseFare: Double,

    @SerializedName("distance_km")
    val distanceKm: Double,

    @SerializedName("free_distance_km")
    val freeDistanceKm: Double = 0.0,

    @SerializedName("chargeable_distance_km")
    val chargeableDistanceKm: Double,

    @SerializedName("distance_fare")
    val distanceFare: Double,

    @SerializedName("loading_charges")
    val loadingCharges: Double = 0.0,

    @SerializedName("free_loading_time_mins")
    val freeLoadingTimeMins: Int? = 25,

    @SerializedName("waiting_charges")
    val waitingCharges: Double = 0.0,

    @SerializedName("toll_charges")
    val tollCharges: Double = 0.0,

    @SerializedName("platform_fee")
    val platformFee: Double = 0.0,

    @SerializedName("surge_multiplier")
    val surgeMultiplier: Double = 1.0,

    @SerializedName("surge_amount")
    val surgeAmount: Double = 0.0,

    @SerializedName("sub_total")
    val subTotal: Double,

    @SerializedName("gst_percentage")
    val gstPercentage: Double = 5.0,

    @SerializedName("gst_amount")
    val gstAmount: Double,

    @SerializedName("discount")
    val discount: Double = 0.0,

    @SerializedName("total_fare")
    val totalFare: Double,

    @SerializedName("rounded_fare")
    val roundedFare: Int,

    @SerializedName("estimated_duration_minutes")
    val estimatedDurationMinutes: Int,

    @SerializedName("currency")
    val currency: String = "INR",

    @SerializedName("promo_applied")
//    val promoApplied: PromoInfo? = null,
    val promoApplied: String? = "",

    @SerializedName("fare_breakdown")
    val fareBreakdown: List<FareBreakdownItem> = emptyList()
) {
    fun getDisplayFare(): String = "₹$roundedFare"
    fun getEtaText(): String = "$estimatedDurationMinutes mins"
    fun hasSurgePricing(): Boolean = surgeMultiplier > 1.0
    fun getSurgePercentage(): Int = ((surgeMultiplier - 1.0) * 100).toInt()
}

/**
 * Promo Info
 */
data class PromoInfo(
    @SerializedName("promo_code")
    val promoCode: String,

    @SerializedName("discount_amount")
    val discountAmount: Int,

    @SerializedName("discount_type")
    val discountType: String
)

/**
 * Fare Breakdown Item
 */
data class FareBreakdownItem(
    @SerializedName("label")
    val label: String,

    @SerializedName("value")
    val value: Double,

    @SerializedName("type")
    val type: String
) {
    fun getFormattedValue(): String {
        return when {
            type == "discount" && value > 0 -> "-₹${value.toInt()}"
            else -> "₹${value.toInt()}"
        }
    }
}

/**
 * Create booking response wrapper
 */
data class CreateBookingResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("data")
    val data: BookingResponse,

    @SerializedName("message")
    val message: String?
)