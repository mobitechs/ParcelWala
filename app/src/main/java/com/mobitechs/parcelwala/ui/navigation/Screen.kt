// ui/navigation/Screen.kt
package com.mobitechs.parcelwala.ui.navigation

import com.mobitechs.parcelwala.data.model.request.SavedAddress

/**
 * Navigation Screen Routes
 * Defines all navigation destinations in the app
 *
 * Organization:
 * - Auth screens (Splash, Login, OTP, CompleteProfile)
 * - Main screen (with bottom navigation)
 * - Booking flow screens
 * - Account/Profile flow screens
 * - Order screens
 */
sealed class Screen(val route: String) {

    // ============ AUTH SCREENS ============
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Otp : Screen("otp/{phoneNumber}") {
        fun createRoute(phoneNumber: String) = "otp/$phoneNumber"
    }
    object CompleteProfile : Screen("complete_profile")

    // ============ MAIN SCREEN ============
    object Main : Screen("main")
    object Home : Screen("home")

    // ============ BOOKING FLOW SCREENS ============
    object LocationSearch : Screen("location_search/{locationType}") {
        fun createRoute(locationType: String) = "location_search/$locationType"
    }

    object MapPicker : Screen("map_picker/{lat}/{lng}/{locationType}") {
        fun createRoute(lat: Double, lng: Double, locationType: String) =
            "map_picker/$lat/$lng/$locationType"
    }

    object AddressConfirm : Screen("address_confirm/{locationType}") {
        fun createRoute(locationType: String) = "address_confirm/$locationType"
    }

    object BookingConfirm : Screen("booking_confirm")
    object ReviewBooking : Screen("review_booking")
    object Coupons : Screen("coupons")
    object AddGSTIN : Screen("add_gstin")
    object SearchingRider : Screen("searching_rider/{bookingId}") {
        fun createRoute(bookingId: String) = "searching_rider/$bookingId"
    }

    // ============ ACCOUNT FLOW SCREENS ============
    object Language : Screen("language")
    object AccountFlow : Screen("account_flow")
    object SavedAddresses : Screen("saved_addresses")
    object AddressSearch : Screen("address_search")
    object AddressMapPicker : Screen("address_map_picker/{lat}/{lng}") {
        fun createRoute(lat: Double, lng: Double) = "address_map_picker/$lat/$lng"
    }
    object AddressConfirmSave : Screen("address_confirm")
    object ProfileDetails : Screen("profile_details")

    // ============ ORDER SCREENS ============
    object OrderDetails : Screen("order_details")
    object OrderTracking : Screen("order_tracking/{bookingId}") {
        fun createRoute(bookingId: Int) = "order_tracking/$bookingId"
    }
}

/**
 * Navigation Helper for Location Flow
 * Holds pickup and drop address data during booking
 */
data class LocationFlowData(
    val pickupAddress: SavedAddress? = null,
    val dropAddress: SavedAddress? = null
)