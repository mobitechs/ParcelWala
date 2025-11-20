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

    @SerializedName("pickup_landmark")
    val pickupLandmark: String? = null,

    @SerializedName("pickup_flat_building")
    val pickupFlatBuilding: String? = null,

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

    @SerializedName("drop_landmark")
    val dropLandmark: String? = null,

    @SerializedName("drop_flat_building")
    val dropFlatBuilding: String? = null,

    @SerializedName("drop_contact_name")
    val dropContactName: String,

    @SerializedName("drop_contact_phone")
    val dropContactPhone: String,

    @SerializedName("goods_type")
    val goodsType: String? = null,

    @SerializedName("goods_weight")
    val goodsWeight: Double? = null,

    @SerializedName("special_instructions")
    val specialInstructions: String? = null,

    @SerializedName("payment_method")
    val paymentMethod: String = "cash", // cash, card, wallet, upi

    @SerializedName("promo_code")
    val promoCode: String? = null
)

/**
 * Fare calculation request
 */
data class CalculateFareRequest(
    @SerializedName("vehicle_type_id")
    val vehicleTypeId: Int,

    @SerializedName("pickup_latitude")
    val pickupLatitude: Double,

    @SerializedName("pickup_longitude")
    val pickupLongitude: Double,

    @SerializedName("drop_latitude")
    val dropLatitude: Double,

    @SerializedName("drop_longitude")
    val dropLongitude: Double,

    @SerializedName("promo_code")
    val promoCode: String? = null
)