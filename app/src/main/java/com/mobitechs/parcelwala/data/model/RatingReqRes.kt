package com.mobitechs.parcelwala.data.model



data class SubmitRatingRequest(
    val rating: Int,      // 1-5
    val feedback: String? = null
)

data class RatingListResponse(
    val success: Boolean,
    val message: String?,
    val data: List<RatingListItems>,
    val pagination: Pagination?,
)

data class RatingListItems(
    val bookingNumber: String,
    val driverName: String,
    val feedback: String,
    val ratedAt: String,
    val rating: Int,
    val ratingId: Int
)


data class RatingSubmitResponse(
    val data: RatingSubmitDetails,
    val message: String,
    val success: Boolean
)

data class RatingSubmitDetails(
    val bookingId: Int,
    val bookingNumber: String,
    val driverName: String,
    val driverNewRating: String,
    val driverTotalRatings: String,
    val feedback: String,
    val ratedAt: String,
    val rating: Int,
    val ratingId: Int
)



