package com.mobitechs.parcelwala.data.model.response

import com.google.gson.annotations.SerializedName

/**
 * Orders List Response - wrapper for list API
 */
data class OrdersListResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("message")
    val message: String?,

    @SerializedName("data")
    val data: List<OrderResponse>?
)

/**
 * Order Details Response - wrapper for single order API
 */
data class OrderDetailsResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("message")
    val message: String?,

    @SerializedName("data")
    val data: OrderResponse?
)

/**
 * Order Response - Main data model
 */
data class OrderResponse(

    @SerializedName("booking_id")
    val bookingId: Int,

    @SerializedName("booking_number")
    val bookingNumber: String,

    @SerializedName("vehicle_type")
    val vehicleType: String,

    @SerializedName("vehicle_type_id")
    val vehicleTypeId: Int? = null,

    @SerializedName("pickup_address")
    val pickupAddress: String,

    @SerializedName("pickup_latitude")
    val pickupLatitude: Double? = null,

    @SerializedName("pickup_longitude")
    val pickupLongitude: Double? = null,

    @SerializedName("pickup_contact_name")
    val pickupContactName: String? = null,

    @SerializedName("pickup_contact_phone")
    val pickupContactPhone: String? = null,

    @SerializedName("drop_address")
    val dropAddress: String,

    @SerializedName("drop_latitude")
    val dropLatitude: Double? = null,

    @SerializedName("drop_longitude")
    val dropLongitude: Double? = null,

    @SerializedName("drop_contact_name")
    val dropContactName: String? = null,

    @SerializedName("drop_contact_phone")
    val dropContactPhone: String? = null,

    @SerializedName("goods_type_id")
    val goodsTypeId: Int? = null,

    @SerializedName("goods_type")
    val goodsType: String? = null,

    @SerializedName("goods_weight")
    val goodsWeight: Double? = null,

    @SerializedName("goods_packages")
    val goodsPackages: Int? = null,

    @SerializedName("goods_value")
    val goodsValue: Int? = null,

    @SerializedName("status")
    val status: String,

    @SerializedName("fare")
    val fare: Double,

    @SerializedName("base_fare")
    val baseFare: Double? = null,

    @SerializedName("distance")
    val distance: Double? = null,

    @SerializedName("discount_amount")
    val discountAmount: Double? = null,

    @SerializedName("coupon_code")
    val couponCode: String? = null,

    @SerializedName("payment_method")
    val paymentMethod: String? = null,

    @SerializedName("payment_status")
    val paymentStatus: String? = null,

    @SerializedName("driver_id")
    val driverId: Int? = null,

    @SerializedName("driver_name")
    val driverName: String? = null,

    @SerializedName("driver_phone")
    val driverPhone: String? = null,

    @SerializedName("driver_photo")
    val driverPhoto: String? = null,

    @SerializedName("driver_rating")
    val driverRating: Double? = null,

    @SerializedName("vehicle_number")
    val vehicleNumber: String? = null,

    @SerializedName("vehicle_model")
    val vehicleModel: String? = null,

    @SerializedName("otp")
    val otp: String? = null,

    @SerializedName("pickup_time")
    val pickupTime: String? = null,

    @SerializedName("delivery_time")
    val deliveryTime: String? = null,

    @SerializedName("estimated_time")
    val estimatedTime: String? = null,

    @SerializedName("instructions")
    val instructions: String? = null,

    @SerializedName("cancellation_reason")
    val cancellationReason: String? = null,

    @SerializedName("cancelled_by")
    val cancelledBy: String? = null,

    @SerializedName("rating")
    val rating: Int? = null,

    @SerializedName("review")
    val review: String? = null,

    @SerializedName("gstin")
    val gstin: String? = null,

    // Important fix → JSON key is "createdAt"
    @SerializedName("createdAt")
    val createdAt: String,

    @SerializedName("updated_at")
    val updatedAt: String? = null
)
