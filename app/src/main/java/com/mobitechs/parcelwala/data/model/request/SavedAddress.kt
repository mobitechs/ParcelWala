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
    val addressId: String = "",

    @SerializedName("address_type")
    val addressType: String = "Other", // "home", "shop", "Other"

    @SerializedName("label")
    val label: String = "Other", // For "Other" type, stores custom label; Otherwise stores type name

    @SerializedName("address")
    val address: String = "", // Complete address from map/search

    @SerializedName("landmark")
    val landmark: String? = null,

    @SerializedName("latitude")
    val latitude: Double = 0.0,

    @SerializedName("longitude")
    val longitude: Double = 0.0,

    @SerializedName("contact_name")
    val contactName: String? = null,

    @SerializedName("contact_phone")
    val contactPhone: String? = null,

    @SerializedName("is_default")
    val isDefault: Boolean = false,

    // Combined building details field (Flat no, Building name)
    @SerializedName("building_details")
    val buildingDetails: String? = null,

    @SerializedName("pincode")
    val pincode: String? = null
) : Parcelable {

    /**
     * Returns display label for UI
     * For "Other" type: "Other (custom label)"
     * For home/shop: "Home" or "Shop"
     */
    fun getDisplayLabel(): String {
        return when (addressType) {
            "Home", "home" -> "Home"
            "Shop", "shop" -> "Shop"
            "Other", "other" -> {
                if (label.isNotBlank() &&
                    !label.equals("other", ignoreCase = true) &&
                    !label.equals("selected location", ignoreCase = true)) {
                    "Other ($label)"
                } else {
                    "Other"
                }
            }
            else -> label.ifBlank { "Address" }
        }
    }
}