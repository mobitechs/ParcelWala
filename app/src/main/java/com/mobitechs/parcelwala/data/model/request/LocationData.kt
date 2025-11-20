// data/model/request/LocationData.kt
package com.mobitechs.parcelwala.data.model.request

import com.google.gson.annotations.SerializedName

/**
 * Location data for booking
 */
data class LocationData(
    @SerializedName("latitude")
    val latitude: Double,

    @SerializedName("longitude")
    val longitude: Double,

    @SerializedName("address")
    val address: String,

    @SerializedName("landmark")
    val landmark: String? = null,

    @SerializedName("city")
    val city: String? = null,

    @SerializedName("pincode")
    val pincode: String? = null
)
