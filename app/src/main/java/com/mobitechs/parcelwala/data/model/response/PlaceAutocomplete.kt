package com.mobitechs.parcelwala.data.model.response

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Place autocomplete prediction model
 */
@Parcelize
data class PlaceAutocomplete(
    val placeId: String,
    val primaryText: String,
    val secondaryText: String?,
    val fullText: String
) : Parcelable

/**
 * Place details with coordinates
 */
@Parcelize
data class PlaceDetails(
    val placeId: String,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val types: List<String> = emptyList()
) : Parcelable

/**
 * Location model
 */
@Parcelize
data class Location(
    val latitude: Double,
    val longitude: Double,
    val address: String = "",
    val name: String = ""
) : Parcelable