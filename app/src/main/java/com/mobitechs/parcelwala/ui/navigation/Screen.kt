package com.mobitechs.parcelwala.ui.navigation

import com.mobitechs.parcelwala.data.model.request.SavedAddress


sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Otp : Screen("otp/{phoneNumber}") {
        fun createRoute(phoneNumber: String) = "otp/$phoneNumber"
    }
    object CompleteProfile : Screen("complete_profile")
    object Main : Screen("main")
    object Home : Screen("home")

    // Booking flow
//    object LocationSearch : Screen("location_search/{type}") {
//        fun createRoute(type: String) = "location_search/$type"
//    }
//    object AddressConfirm : Screen("address_confirm/{type}") {
//        fun createRoute(type: String) = "address_confirm/$type"
//    }

    // ✅ Location & Booking Screens
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
}




// Navigation Helper for Location Flow
data class LocationFlowData(
    val pickupAddress: SavedAddress? = null,
    val dropAddress: SavedAddress? = null
)