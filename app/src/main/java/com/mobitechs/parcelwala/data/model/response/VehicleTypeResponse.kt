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

    // ═══════════════════════════════════════════════════════════════════════
    // LOADING SPACE — added for Smart Shifting
    // ═══════════════════════════════════════════════════════════════════════
    //
    // Household goods "cube out" long before they "weigh out": a Tata Ace is
    // rated for 750 kg but holds ~80 cubic feet, and two sofas fill it at under
    // 150 kg. Sizing a move on `max_capacity_kg` alone therefore recommends
    // vehicles the goods physically cannot fit into.
    //
    // All FOUR fields are optional and the app degrades gracefully without them
    // (it parses the `capacity` string, then falls back to a vehicle-name
    // table), so shipping the client does not block on the backend. But every
    // fallback is a guess about the operator's own fleet — send these and the
    // recommendation becomes exact.
    //
    // See the backend documentation for the full contract.

    /**
     * Usable loading volume in cubic feet. THE MOST IMPORTANT ONE.
     *
     * If only one field can be added, add this. It short-circuits every fallback
     * in `MovingRecommendationEngine.toLoadProfile`.
     */
    @SerializedName("capacity_cft")
    val capacityCft: Double? = null,

    /** Loading deck length in feet. */
    @SerializedName("deck_length_ft")
    val deckLengthFt: Double? = null,

    /** Loading deck width in feet. */
    @SerializedName("deck_width_ft")
    val deckWidthFt: Double? = null,

    /**
     * Usable loading height in feet.
     *
     * For an open body this is the height goods can be stacked to, NOT the side
     * rail height — the app already discounts the deck box by 15% for the fact
     * that nothing is packed to the very top.
     */
    @SerializedName("deck_height_ft")
    val deckHeightFt: Double? = null,

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
