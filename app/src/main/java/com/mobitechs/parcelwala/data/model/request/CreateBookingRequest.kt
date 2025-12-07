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
    val fareBeforeDiscount: Int,  // roundedFare from API

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
    val finalFare: Int,  // Amount after all discounts

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

/**
 * Builder helper to create request from FareDetails and UI state
 */
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
        gstin: String?
    ): CreateBookingRequest {

        val fareBeforeDiscount = fareDetails.roundedFare
        val finalFare = fareBeforeDiscount - couponDiscountAmount

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

            // Fare Calculation
            distanceKm = fareDetails.distanceKm,
            estimatedDurationMinutes = fareDetails.estimatedDurationMinutes,
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
 */
data class CalculateFareRequest(
//    @SerializedName("vehicle_type_id")
//    val vehicleTypeId: Int,

    @SerializedName("pickup_latitude")
    val pickupLatitude: Double,

    @SerializedName("pickup_longitude")
    val pickupLongitude: Double,

    @SerializedName("drop_latitude")
    val dropLatitude: Double,

    @SerializedName("drop_longitude")
    val dropLongitude: Double
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