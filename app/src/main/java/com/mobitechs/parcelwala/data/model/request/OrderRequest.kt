// data/model/request/OrderRequest.kt
package com.mobitechs.parcelwala.data.model.request

import com.google.gson.annotations.SerializedName


/**
 * Get orders request with filters
 */
data class GetOrdersRequest(
    @SerializedName("status")
    val status: String? = null,

    @SerializedName("page")
    val page: Int = 1,

    @SerializedName("per_page")
    val perPage: Int = 20
)