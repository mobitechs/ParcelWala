// data/model/request/SavedAddress.kt
package com.mobitechs.parcelwala.data.model.request

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

/**
 * Saved address from user
 */
@Parcelize
data class SavedAddress(
    @SerializedName("address_id")
    val addressId: String,

    @SerializedName("address_type")
    val addressType: String, // "home", "shop", "other"

    @SerializedName("label")
    val label: String,

    @SerializedName("address")
    val address: String,

    @SerializedName("landmark")
    val landmark: String?,

    @SerializedName("latitude")
    val latitude: Double,

    @SerializedName("longitude")
    val longitude: Double,

    @SerializedName("contact_name")
    val contactName: String?,

    @SerializedName("contact_phone")
    val contactPhone: String?,

    @SerializedName("is_default")
    val isDefault: Boolean = false
) : Parcelable