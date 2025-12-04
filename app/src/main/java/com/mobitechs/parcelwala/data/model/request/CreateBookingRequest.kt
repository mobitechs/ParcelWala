// data/model/request/CreateBookingRequest.kt
package com.mobitechs.parcelwala.data.model.request

import com.google.gson.annotations.SerializedName

/**
 * Create booking request
 */
data class CreateBookingRequest(
    @SerializedName("vehicle_type_id")
    val vehicleTypeId: Int,

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

    @SerializedName("goods_type_id")
    val goodsTypeId: Int? = null,

    @SerializedName("goods_weight")
    val goodsWeight: Double? = null,

    @SerializedName("goods_packages")
    val goodsPackages: Int? = null,

    @SerializedName("goods_value")
    val goodsValue: Int? = null,

    @SerializedName("payment_method")
    val paymentMethod: String = "Cash",

    @SerializedName("coupon_code")
    val couponCode: String? = null,

    @SerializedName("gstin")
    val gstin: String? = null
)

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