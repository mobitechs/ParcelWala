// data/model/request/SavedAddress.kt
package com.mobitechs.parcelwala.data.model.request

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

/**
 * Saved Address Model
 * Used for both booking flow (pickup/drop addresses) and account saved addresses
 *
 * This model supports:
 * - Booking flow: pickup and drop location details
 * - Account flow: saved addresses for quick selection
 * - Book Again: prefilling from previous orders
 */
@Parcelize
data class SavedAddress(
    @SerializedName("address_id")
    val addressId: String,

    @SerializedName("address_type")
    val addressType: String = "other", // "home", "shop", "other"

    @SerializedName("label")
    val label: String,

    @SerializedName("address")
    val address: String,

    @SerializedName("landmark")
    val landmark: String? = null,

    @SerializedName("latitude")
    val latitude: Double,

    @SerializedName("longitude")
    val longitude: Double,

    @SerializedName("contact_name")
    val contactName: String? = null,

    @SerializedName("contact_phone")
    val contactPhone: String? = null,

    @SerializedName("is_default")
    val isDefault: Boolean = false,

    // Additional address details for more precise location
    @SerializedName("flat_number")
    val flatNumber: String? = null,

    @SerializedName("building_name")
    val buildingName: String? = null,

    @SerializedName("pincode")
    val pincode: String? = null
) : Parcelable {}