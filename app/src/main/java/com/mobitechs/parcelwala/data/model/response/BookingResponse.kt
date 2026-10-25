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
    val fare: Double, // ✅ Int → Double

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
 * Fare details for a vehicle type.
 *
 * ✅ All price/fare fields are Double for precision.
 * ✅ waitingChargePerMin & freeWaitingTimeMins drive the dynamic waiting timer.
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

    // Fare Calculation — all Double
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

    @SerializedName("free_waiting_time_mins")
    val freeWaitingTimeMins: Int? = null,

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
    val roundedFare: Double, // ✅ Int → Double

    @SerializedName("estimated_duration_minutes")
    val estimatedDurationMinutes: Int,

    @SerializedName("waitingChargePerMin")
    val waitingChargePerMin: Double, // ✅ Int → Double (per-min charge for waiting timer)

    @SerializedName("freeWaitingTimeMins")
    val freeWaitingTimeMinsAlt: Int? = null, // alternate JSON key from API

    /**
     * Vehicle photo. Read from either key: the fare API has sent `imageUrl`,
     * `/vehicles/types` sends `image_url`, and accepting both means the backend
     * can align the two without an app release.
     *
     * Nullable because it IS optional in practice — Gson ignores Kotlin
     * non-null types and would hand back null for a missing key anyway.
     */
    @SerializedName(value = "imageUrl", alternate = ["image_url"])
    val imageUrl: String? = null,

    @SerializedName("currency")
    val currency: String = "INR",

    @SerializedName("promo_applied")
    val promoApplied: String? = "",

    @SerializedName("fare_breakdown")
    val fareBreakdown: List<FareBreakdownItem> = emptyList(),

    /**
     * Variant of this vehicle type, when the fare API sends it. The app also
     * fills these from `/vehicles/types` by id, so they are optional here.
     */
    @SerializedName(
        value = "vehicle_sub_type",
        alternate = ["vehicle_subtype", "vehicleSubType", "VehicleSubType", "sub_type", "subType"]
    )
    val vehicleSubType: String? = null,

    @SerializedName(
        value = "vehicle_body_type",
        alternate = ["vehicle_bodytype", "vehicleBodyType", "VehicleBodyType", "body_type", "bodyType"]
    )
    val vehicleBodyType: String? = null
) {
    companion object {
        const val DEFAULT_FREE_WAITING_MINS = 3
        const val DEFAULT_CHARGE_PER_MIN = 3.0
    }

    /** Resolved free waiting time — checks both JSON key variants, defaults to 3 min */
    val resolvedFreeWaitingMins: Int
        get() = freeWaitingTimeMins ?: freeWaitingTimeMinsAlt ?: DEFAULT_FREE_WAITING_MINS

    fun getDisplayFare(): String = formatRupee(roundedFare)
    fun getEtaText(): String = "$estimatedDurationMinutes mins"
    fun hasSurgePricing(): Boolean = surgeMultiplier > 1.0
    fun getSurgePercentage(): Int = ((surgeMultiplier - 1.0) * 100).toInt()
    fun getEffectiveFare(): Double {
        // return the final fare used for the strikethrough base
        // e.g. return if (discount > 0) totalFare else totalFare
        return totalFare
    }
}


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
)

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

// ═══════════════════════════════════════════════════════════════════════
// PRICE FORMATTING UTILITIES
// ═══════════════════════════════════════════════════════════════════════

/** Format price: show decimals only when fractional (150.0 → "150", 150.5 → "150.50") */
fun formatPrice(amount: Double): String =
    if (amount == amount.toLong().toDouble()) amount.toLong().toString()
    else String.format("%.2f", amount)

/** Format price with ₹ symbol */
fun formatRupee(amount: Double): String = "₹${formatPrice(amount)}"