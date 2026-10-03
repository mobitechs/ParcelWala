// data/model/request/CreateBookingRequest.kt
package com.mobitechs.parcelwala.data.model.request

import com.google.gson.annotations.SerializedName

/**
 * Create Booking Request
 *
 * Includes complete fare breakdown for:
 * - Order history tracking
 * - Invoice generation
 * - Backend verification
 * - Audit trail
 */
data class CreateBookingRequest(
    // ============ VEHICLE INFO ============
    @SerializedName("vehicle_type_id")
    val vehicleTypeId: Int,

    @SerializedName("vehicle_type_name")
    val vehicleTypeName: String,

    // ============ PICKUP DETAILS ============
    @SerializedName("pickup_address")
    val pickupAddress: String,

    @SerializedName("pickup_latitude")
    val pickupLatitude: Double,

    @SerializedName("pickup_longitude")
    val pickupLongitude: Double,

    @SerializedName("pickup_contact_name")
    val pickupContactName: String,

    @SerializedName("pickup_contact_phone")
    val pickupContactPhone: String,

    @SerializedName("pickup_building_details")
    val pickupBuildingDetails: String? = null,

    @SerializedName("pickup_landmark")
    val pickupLandmark: String? = null,

    // ============ DROP DETAILS ============
    @SerializedName("drop_address")
    val dropAddress: String,

    @SerializedName("drop_latitude")
    val dropLatitude: Double,

    @SerializedName("drop_longitude")
    val dropLongitude: Double,

    @SerializedName("drop_contact_name")
    val dropContactName: String,

    @SerializedName("drop_contact_phone")
    val dropContactPhone: String,

    @SerializedName("drop_building_details")
    val dropBuildingDetails: String? = null,

    @SerializedName("drop_landmark")
    val dropLandmark: String? = null,

    // ============ GOODS DETAILS ============
    @SerializedName("goods_type_id")
    val goodsTypeId: Int? = null,

    @SerializedName("goods_type_name")
    val goodsTypeName: String? = null,

    @SerializedName("goods_weight")
    val goodsWeight: Double? = null,

    @SerializedName("goods_packages")
    val goodsPackages: Int? = null,

    @SerializedName("goods_value")
    val goodsValue: Int? = null,

    // ============ SMART SHIFTING ============
    //
    // Present only for bookings made through the "what are you moving" flow.
    // Every field is optional and the parcel flow omits all of them, so the
    // endpoint stays backwards compatible.
    //
    // WHY THE FULL ITEM LIST IS STORED, NOT JUST THE SUMMARY
    //
    // `goods_type_name` already carries a readable line ("House Shifting · Sofa
    // x1, Boxes x8") and that is what a driver reads. But a text field cannot be
    // queried, and the numbers behind the recommendation are the only way to
    // ever answer the question that matters: did we send the right vehicle?
    // Storing the rows — with the volume and weight each estimate was built from
    // — lets the backend compare what was predicted against what turned up, and
    // tune the catalog from real trips instead of guesses.
    //
    // See the backend documentation for the storage contract.

    /** "moving" for a Smart Shifting booking; absent for an ordinary parcel. */
    @SerializedName("booking_source")
    val bookingSource: String? = null,

    /** One row per item the customer selected. */
    @SerializedName("moving_items")
    val movingItems: List<com.mobitechs.parcelwala.data.model.moving.MovingBookingItem>? = null,

    /** Total estimated volume in cubic feet, after packing allowance. */
    @SerializedName("estimated_volume_cft")
    val estimatedVolumeCft: Double? = null,

    /** SMALL | MEDIUM | LARGE | VERY_LARGE | EXTRA_LARGE — the band shown to the customer. */
    @SerializedName("load_size")
    val loadSize: String? = null,

    /** True when the load contains items needing two people / a wide door. */
    @SerializedName("has_bulky_items")
    val hasBulkyItems: Boolean? = null,

    /** True when the load contains food, glass or electronics. */
    @SerializedName("has_fragile_items")
    val hasFragileItems: Boolean? = null,

    /** Loading helpers the app suggested. 0 when the driver can manage alone. */
    @SerializedName("suggested_helpers")
    val suggestedHelpers: Int? = null,

    /** The vehicle the app recommended, before any customer override. */
    @SerializedName("recommended_vehicle_type_id")
    val recommendedVehicleTypeId: Int? = null,

    /**
     * FALSE when the load does not fit in one trip.
     *
     * The customer is never told this — see the note on
     * `VehicleRecommendation.fitsInOneTrip`. OPERATIONS SHOULD ACT ON IT: flag
     * the booking for a call before a driver is dispatched.
     */
    @SerializedName("fits_in_one_trip")
    val fitsInOneTrip: Boolean? = null,

    // ============ FARE CALCULATION DETAILS ============
    @SerializedName("distance_km")
    val distanceKm: Double,

    @SerializedName("estimated_duration_minutes")
    val estimatedDurationMinutes: Int,

    @SerializedName("base_fare")
    val baseFare: Double,

    @SerializedName("free_distance_km")
    val freeDistanceKm: Double,

    @SerializedName("chargeable_distance_km")
    val chargeableDistanceKm: Double,

    @SerializedName("distance_fare")
    val distanceFare: Double,

    @SerializedName("platform_fee")
    val platformFee: Double,

    @SerializedName("loading_charges")
    val loadingCharges: Double = 0.0,

    @SerializedName("waiting_charges")
    val waitingCharges: Double = 0.0,

    @SerializedName("toll_charges")
    val tollCharges: Double = 0.0,

    @SerializedName("surge_multiplier")
    val surgeMultiplier: Double = 1.0,

    @SerializedName("surge_amount")
    val surgeAmount: Double = 0.0,

    @SerializedName("sub_total")
    val subTotal: Double,

    @SerializedName("gst_percentage")
    val gstPercentage: Double,

    @SerializedName("gst_amount")
    val gstAmount: Double,

    @SerializedName("fare_before_discount")
    val fareBeforeDiscount: Double,  // roundedFare from API

    // ============ COUPON/DISCOUNT DETAILS ============
    @SerializedName("coupon_id")
    val couponId: Int? = null,

    @SerializedName("coupon_code")
    val couponCode: String? = null,

    @SerializedName("coupon_discount_type")
    val couponDiscountType: String? = null,  // "percentage" or "fixed"

    @SerializedName("coupon_discount_value")
    val couponDiscountValue: Int? = null,  // Original coupon value (e.g., 20 for 20%)

    @SerializedName("coupon_discount_amount")
    val couponDiscountAmount: Int = 0,  // Actual discount applied in rupees

    // ============ FINAL AMOUNT ============
    @SerializedName("final_fare")
    val finalFare: Double,  // Amount after all discounts

    // ============ PAYMENT & OTHER ============
    @SerializedName("payment_method")
    val paymentMethod: String = "Cash",

    @SerializedName("gstin")
    val gstin: String? = null,

    // ============ METADATA ============
    @SerializedName("free_loading_time_mins")
    val freeLoadingTimeMins: Int = 25,

    @SerializedName("currency")
    val currency: String = "INR"
)

object CreateBookingRequestBuilder {

    fun build(
        fareDetails: com.mobitechs.parcelwala.data.model.response.FareDetails,
        pickupAddress: SavedAddress,
        dropAddress: SavedAddress,
        goodsTypeId: Int?,
        goodsTypeName: String?,
        goodsWeight: Double?,
        goodsPackages: Int?,
        goodsValue: Int?,
        couponId: Int?,
        couponCode: String?,
        couponDiscountType: String?,
        couponDiscountValue: Int?,
        couponDiscountAmount: Int,
        paymentMethod: String,
        gstin: String?,
        // ✅ NEW: Optional road distance/ETA from Google Directions API
        roadDistanceKm: Double? = null,
        roadDurationMinutes: Int? = null,
        // ── Smart Shifting. All null for an ordinary parcel booking. ──────
        movingItems: List<com.mobitechs.parcelwala.data.model.moving.MovingBookingItem>? = null,
        estimatedVolumeCft: Double? = null,
        loadSize: String? = null,
        hasBulkyItems: Boolean? = null,
        hasFragileItems: Boolean? = null,
        suggestedHelpers: Int? = null,
        recommendedVehicleTypeId: Int? = null,
        fitsInOneTrip: Boolean? = null
    ): CreateBookingRequest {

        val fareBeforeDiscount = fareDetails.roundedFare
        val finalFare = fareBeforeDiscount - couponDiscountAmount

        // ✅ Use road distance if available, fallback to fare API distance
        val distanceKm = roadDistanceKm ?: fareDetails.distanceKm
        val durationMinutes = roadDurationMinutes ?: fareDetails.estimatedDurationMinutes

        return CreateBookingRequest(
            // Vehicle Info
            vehicleTypeId = fareDetails.vehicleTypeId,
            vehicleTypeName = fareDetails.vehicleTypeName,

            // Pickup Details
            pickupAddress = pickupAddress.address,
            pickupLatitude = pickupAddress.latitude,
            pickupLongitude = pickupAddress.longitude,
            pickupContactName = pickupAddress.contactName ?: "",
            pickupContactPhone = pickupAddress.contactPhone ?: "",
            pickupBuildingDetails = pickupAddress.buildingDetails,
            pickupLandmark = pickupAddress.landmark,

            // Drop Details
            dropAddress = dropAddress.address,
            dropLatitude = dropAddress.latitude,
            dropLongitude = dropAddress.longitude,
            dropContactName = dropAddress.contactName ?: "",
            dropContactPhone = dropAddress.contactPhone ?: "",
            dropBuildingDetails = dropAddress.buildingDetails,
            dropLandmark = dropAddress.landmark,

            // Goods Details
            goodsTypeId = goodsTypeId,
            goodsTypeName = goodsTypeName,
            goodsWeight = goodsWeight,
            goodsPackages = goodsPackages,
            goodsValue = goodsValue,

            // Smart Shifting. `booking_source` is derived rather than passed:
            // an item list IS the definition of a moving booking, so the two
            // cannot drift out of agreement.
            bookingSource = if (!movingItems.isNullOrEmpty()) "moving" else null,
            movingItems = movingItems?.takeIf { it.isNotEmpty() },
            estimatedVolumeCft = estimatedVolumeCft,
            loadSize = loadSize,
            hasBulkyItems = hasBulkyItems,
            hasFragileItems = hasFragileItems,
            suggestedHelpers = suggestedHelpers,
            recommendedVehicleTypeId = recommendedVehicleTypeId,
            fitsInOneTrip = fitsInOneTrip,

            // ✅ Fare Calculation - uses road distance when available
            distanceKm = distanceKm,
            estimatedDurationMinutes = durationMinutes,
            baseFare = fareDetails.baseFare,
            freeDistanceKm = fareDetails.freeDistanceKm,
            chargeableDistanceKm = fareDetails.chargeableDistanceKm,
            distanceFare = fareDetails.distanceFare,
            platformFee = fareDetails.platformFee,
            loadingCharges = fareDetails.loadingCharges,
            waitingCharges = fareDetails.waitingCharges,
            tollCharges = fareDetails.tollCharges,
            surgeMultiplier = fareDetails.surgeMultiplier,
            surgeAmount = fareDetails.surgeAmount,
            subTotal = fareDetails.subTotal,
            gstPercentage = fareDetails.gstPercentage,
            gstAmount = fareDetails.gstAmount,
            fareBeforeDiscount = fareBeforeDiscount,

            // Coupon Details
            couponId = couponId,
            couponCode = couponCode,
            couponDiscountType = couponDiscountType,
            couponDiscountValue = couponDiscountValue,
            couponDiscountAmount = couponDiscountAmount,

            // Final Amount
            finalFare = finalFare,

            // Payment & Other
            paymentMethod = paymentMethod,
            gstin = gstin,

            // Metadata
            freeLoadingTimeMins = fareDetails.freeLoadingTimeMins ?: 25,
            currency = fareDetails.currency
        )
    }
}

/**
 * Fare calculation request
 * When distance_km and estimated_duration_minutes are provided (from Google Directions),
 * backend uses these values instead of calculating its own (straight-line) distance.
 * This ensures fare calculation uses accurate road distance.
 */
data class CalculateFareRequest(
    @SerializedName("pickup_latitude")
    val pickupLatitude: Double,

    @SerializedName("pickup_longitude")
    val pickupLongitude: Double,

    @SerializedName("drop_latitude")
    val dropLatitude: Double,

    @SerializedName("drop_longitude")
    val dropLongitude: Double,

    // Optional: Road distance from Google Directions API
    // If provided, backend uses this instead of calculating its own distance
    @SerializedName("distance_km")
    val distanceKm: Double? = null,

    // If provided, backend uses this instead of calculating its own duration
    @SerializedName("estimated_duration_minutes")
    val estimatedDurationMinutes: Int? = null
)


/**
 * Validate Coupon Request
 */
data class ValidateCouponRequest(
    @SerializedName("coupon_code")
    val couponCode: String,

    @SerializedName("order_value")
    val orderValue: Int
)