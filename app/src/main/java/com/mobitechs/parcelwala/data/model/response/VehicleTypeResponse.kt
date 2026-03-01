// data/model/response/VehicleTypeResponse.kt
package com.mobitechs.parcelwala.data.model.response

import com.google.gson.annotations.SerializedName

/**
 * Vehicle Type Response
 */
data class VehicleTypeResponse(
    @SerializedName("vehicle_type_id")
    val vehicleTypeId: Int,

    @SerializedName("name")
    val name: String,

    @SerializedName("icon")
    val icon: String,

    @SerializedName("description")
    val description: String,

    @SerializedName("capacity")
    val capacity: String,

    @SerializedName("base_price")
    val basePrice: Int,

    @SerializedName("free_distance_km")
    val freeDistanceKm: Double = 2.0, // First 2 km included in base fare

    @SerializedName("price_per_km")
    val pricePerKm: Double,

    @SerializedName("platform_fee")
    val platformFee: Int = 10, // Fixed platform fee

    @SerializedName("waiting_charge_per_min")
    val waitingChargePerMin: Double = 2.0, // ₹2 per minute after free time

    @SerializedName("free_waiting_time_mins")
    val freeWaitingTimeMins: Int = 10, // 25 mins free loading/unloading

    @SerializedName("min_fare")
    val minFare: Int = 50, // Minimum fare guarantee

    @SerializedName("max_capacity_kg")
    val maxCapacityKg: Int,

    @SerializedName("dimensions")
    val dimensions: String? = null,

    @SerializedName("is_available")
    val isAvailable: Boolean = true,

    @SerializedName("image_url")
    val imageUrl: String? = null,

    @SerializedName("surge_enabled")
    val surgeEnabled: Boolean = false
)


/**
 * Goods Type Response
 */
data class GoodsTypeResponse(
    @SerializedName("goodsTypeId")  // Changed from "goods_type_id"
    val goodsTypeId: Int,

    @SerializedName("name")
    val name: String,

    @SerializedName("icon")
    val icon: String,

    @SerializedName("defaultWeight")  // Changed from "default_weight"
    val defaultWeight: Double,

    @SerializedName("defaultPackages")  // Changed from "default_packages"
    val defaultPackages: Int,

    @SerializedName("defaultValue")  // Changed from "default_value"
    val defaultValue: Int,

    @SerializedName("isActive")  // Changed from "is_active"
    val isActive: Boolean = true
)

/**
 * Restricted Item Response
 */
data class RestrictedItemResponse(
    @SerializedName("item_id")
    val itemId: Int,

    @SerializedName("name")
    val name: String,

    @SerializedName("description")
    val description: String? = null,

    @SerializedName("category")
    val category: String? = null
)

/**
 * Coupon Response
 */
data class CouponResponse(
    @SerializedName("couponId")
    val couponId: Int,

    @SerializedName("code")
    val code: String,

    @SerializedName("title")
    val title: String,

    @SerializedName("description")
    val description: String,

    @SerializedName("discountType")  // Changed from "discount_type"
    val discountType: String,

    @SerializedName("discountValue")  // Changed from "discount_value"
    val discountValue: Int,

    @SerializedName("minOrderValue")  // Changed from "min_order_value"
    val minOrderValue: Int,

    @SerializedName("maxDiscount")  // Changed from "max_discount"
    val maxDiscount: Int? = null,

    @SerializedName("terms")
    val terms: String,

    @SerializedName("expiryDate")  // Changed from "expiry_date"
    val expiryDate: String? = null,

    @SerializedName("isActive")  // Changed from "is_active"
    val isActive: Boolean = true,

    @SerializedName("usageLimit")  // Changed from "usage_limit"
    val usageLimit: Int? = null,

    @SerializedName("userUsageCount")  // Changed from "user_usage_count"
    val userUsageCount: Int = 0
)
