package com.mobitechs.parcelwala.ui.navigation



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
    object LocationSearch : Screen("location_search/{type}") {
        fun createRoute(type: String) = "location_search/$type"
    }
    object AddressConfirm : Screen("address_confirm/{type}") {
        fun createRoute(type: String) = "address_confirm/$type"
    }
}