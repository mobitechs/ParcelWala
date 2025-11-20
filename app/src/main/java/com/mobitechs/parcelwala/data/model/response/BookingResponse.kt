// data/model/response/BookingResponse.kt
package com.mobitechs.parcelwala.data.model.response

import com.google.gson.annotations.SerializedName

/**
 * Booking response from API
 */
data class BookingResponse(
    @SerializedName("booking_id")
    val bookingId: Int,

    @SerializedName("booking_number")
    val bookingNumber: String,

    @SerializedName("status")
    val status: String, // pending, confirmed, driver_assigned, in_progress, completed, cancelled

    @SerializedName("payment_status")
    val paymentStatus: String, // pending, paid, refunded

    @SerializedName("vehicle_type")
    val vehicleType: VehicleTypeResponse,

    @SerializedName("driver")
    val driver: DriverInfo?,

    @SerializedName("pickup_address")
    val pickupAddress: String,

    @SerializedName("pickup_latitude")
    val pickupLatitude: Double,

    @SerializedName("pickup_longitude")
    val pickupLongitude: Double,

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

    @SerializedName("drop_contact_name")
    val dropContactName: String,

    @SerializedName("drop_contact_phone")
    val dropContactPhone: String,

    @SerializedName("total_fare")
    val totalFare: Double,

    @SerializedName("distance_km")
    val distanceKm: Double,

    @SerializedName("estimated_duration_minutes")
    val estimatedDurationMinutes: Int,

    @SerializedName("created_at")
    val createdAt: String,

    @SerializedName("updated_at")
    val updatedAt: String
)

/**
 * Driver information
 */
data class DriverInfo(
    @SerializedName("driver_id")
    val driverId: Int,

    @SerializedName("name")
    val name: String,

    @SerializedName("phone")
    val phone: String,

    @SerializedName("profile_image")
    val profileImage: String?,

    @SerializedName("rating")
    val rating: Double,

    @SerializedName("vehicle_number")
    val vehicleNumber: String?,

    @SerializedName("current_latitude")
    val currentLatitude: Double?,

    @SerializedName("current_longitude")
    val currentLongitude: Double?
)

/**
 * Fare calculation response
 */
data class FareCalculationResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("data")
    val data: FareDetails,

    @SerializedName("message")
    val message: String?
)

data class FareDetails(
    @SerializedName("base_fare")
    val baseFare: Double,

    @SerializedName("distance_km")
    val distanceKm: Double,

    @SerializedName("distance_fare")
    val distanceFare: Double,

    @SerializedName("sub_total")
    val subTotal: Double,

    @SerializedName("gst")
    val gst: Double,

    @SerializedName("discount")
    val discount: Double = 0.0,

    @SerializedName("total_fare")
    val totalFare: Double,

    @SerializedName("estimated_duration_minutes")
    val estimatedDurationMinutes: Int,

    @SerializedName("promo_applied")
    val promoApplied: PromoInfo?
)

data class PromoInfo(
    @SerializedName("code")
    val code: String,

    @SerializedName("discount")
    val discount: Double,

    @SerializedName("message")
    val message: String
)

/**
 * Create booking response wrapper
 */
data class CreateBookingResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("data")
    val data: BookingResponse,

    @SerializedName("message")
    val message: String?
)