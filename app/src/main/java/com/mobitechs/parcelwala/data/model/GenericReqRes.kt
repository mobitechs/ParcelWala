package com.mobitechs.parcelwala.data.model

import com.google.gson.annotations.SerializedName




data class GenericResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("message")
    val message: String?,
    @SerializedName("data")
    val data: Any?
)



data class Pagination(
    @SerializedName("current_page")
    val currentPage: Int?,
    @SerializedName("total_pages")
    val totalPages: Int?,
    @SerializedName("total_items")
    val totalItems: Int?,
    @SerializedName("items_per_page")
    val itemsPerPage: Int?
)