// data/model/response/VehicleTypeResponse.kt
package com.mobitechs.parcelwala.data.model.response

import com.google.gson.annotations.SerializedName

/**
 * Vehicle type from API
 */
data class VehicleTypeResponse(
    @SerializedName("vehicle_type_id")
    val vehicleTypeId: Int,

    @SerializedName("vehicle_name")
    val vehicleName: String,

    @SerializedName("display_name")
    val displayName: String,

    @SerializedName("description")
    val description: String?,

    @SerializedName("image_url")
    val imageUrl: String?,

    @SerializedName("capacity_kg")
    val capacityKg: Double?,

    @SerializedName("base_fare")
    val baseFare: Double,

    @SerializedName("per_km_rate")
    val perKmRate: Double,

    @SerializedName("is_active")
    val isActive: Boolean,

    @SerializedName("features")
    val features: List<String>?
)

/**
 * List response wrapper
 */
data class VehicleTypeListResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("data")
    val data: List<VehicleTypeResponse>,

    @SerializedName("message")
    val message: String?
)