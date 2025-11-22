// NavGraph.kt
package com.mobitechs.parcelwala.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.google.android.gms.maps.model.LatLng
import com.mobitechs.parcelwala.data.local.PreferencesManager
import com.mobitechs.parcelwala.data.model.request.SavedAddress
import com.mobitechs.parcelwala.ui.screens.auth.CompleteProfileScreen
import com.mobitechs.parcelwala.ui.screens.auth.LoginScreen
import com.mobitechs.parcelwala.ui.screens.auth.OtpScreen
import com.mobitechs.parcelwala.ui.screens.booking.*
import com.mobitechs.parcelwala.ui.screens.main.MainScreen
import com.mobitechs.parcelwala.ui.screens.splash.SplashScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    preferencesManager: PreferencesManager
) {
    val isLoggedIn = preferencesManager.isLoggedIn()

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToHome = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                isLoggedIn = isLoggedIn
            )
        }

        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToOtp = { phoneNumber ->
                    navController.navigate(Screen.Otp.createRoute(phoneNumber))
                }
            )
        }

        composable(
            route = Screen.Otp.route,
            arguments = listOf(
                navArgument("phoneNumber") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val phoneNumber = backStackEntry.arguments?.getString("phoneNumber") ?: ""
            OtpScreen(
                phoneNumber = phoneNumber,
                onNavigateToHome = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToCompleteProfile = {
                    navController.navigate(Screen.CompleteProfile.route) {
                        popUpTo(Screen.Otp.route) { inclusive = true }
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.CompleteProfile.route) {
            CompleteProfileScreen(
                onNavigateToHome = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.CompleteProfile.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Main.route) {
            MainScreen(
                preferencesManager = preferencesManager,
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Main.route) { inclusive = true }
                    }
                },
                onNavigateToLocationSearch = {
                    navController.navigate("location_search/pickup")
                },
                currentRoute = "home"
            )
        }

        composable("home") {
            MainScreen(
                preferencesManager = preferencesManager,
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Main.route) { inclusive = true }
                    }
                },
                onNavigateToLocationSearch = {
                    navController.navigate("location_search/pickup")
                },
                currentRoute = "home"
            )
        }

        composable("bookings") {
            MainScreen(
                preferencesManager = preferencesManager,
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Main.route) { inclusive = true }
                    }
                },
                onNavigateToLocationSearch = {},
                currentRoute = "bookings"
            )
        }

        composable("profile") {
            MainScreen(
                preferencesManager = preferencesManager,
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Main.route) { inclusive = true }
                    }
                },
                onNavigateToLocationSearch = {},
                currentRoute = "profile"
            )
        }

//        composable(
//            route = "location_search/{type}",
//            arguments = listOf(
//                navArgument("type") { type = NavType.StringType }
//            )
//        ) { backStackEntry ->
//            val locationType = backStackEntry.arguments?.getString("type") ?: "pickup"
//
//            LocationSearchScreen(
//                locationType = locationType,
//                onAddressSelected = { selectedAddress ->
//                    navController.currentBackStackEntry
//                        ?.savedStateHandle
//                        ?.set("selected_address", selectedAddress)
//                    navController.navigate("address_confirm/$locationType")
//                },
//                 onBack = { navController.popBackStack() }
//            )
//        }

//        composable(
//            route = "address_confirm/{type}",
//            arguments = listOf(
//                navArgument("type") { type = NavType.StringType }
//            )
//        ) { backStackEntry ->
//            val locationType = backStackEntry.arguments?.getString("type") ?: "pickup"
//            val selectedAddress = navController.previousBackStackEntry
//                ?.savedStateHandle
//                ?.get<com.mobitechs.parcelwala.data.model.request.SavedAddress>("selected_address")
//
//            if (selectedAddress != null) {
//                AddressConfirmScreen(
//                    address = selectedAddress,
//                    locationType = locationType,
//                    onConfirm = { confirmedAddress ->
//                        if (locationType == "pickup") {
//                            navController.currentBackStackEntry
//                                ?.savedStateHandle
//                                ?.set("pickup_address", confirmedAddress)
//                            navController.navigate("location_search/drop")
//                        } else {
//                            navController.currentBackStackEntry
//                                ?.savedStateHandle
//                                ?.set("drop_address", confirmedAddress)
//                            navController.popBackStack(Screen.Main.route, false)
//                        }
//                    },
//                    onBack = { navController.popBackStack() }
//                )
//            } else {
//                navController.popBackStack()
//            }
//        }


        // ✅ Location Search Screen
        composable(
            route = Screen.LocationSearch.route,
            arguments = listOf(
                navArgument("locationType") {
                    type = NavType.StringType
                    defaultValue = "pickup"
                }
            )
        ) { backStackEntry ->
            val locationType = backStackEntry.arguments?.getString("locationType") ?: "pickup"

            LocationSearchScreen(
                locationType = locationType,
                onAddressSelected = { address ->
                    // Save address to saved state
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("selected_address_$locationType", address)

                    // Navigate to address confirmation
                    navController.navigate(Screen.AddressConfirm.createRoute(locationType)) {
                        popUpTo(Screen.LocationSearch.route) { inclusive = true }
                    }
                },
                onMapPicker = { latLng ->
                    // Navigate to map picker
                    navController.navigate(
                        Screen.MapPicker.createRoute(
                            lat = latLng.latitude,
                            lng = latLng.longitude,
                            locationType = locationType
                        )
                    )
                },
                onBack = { navController.popBackStack() }
            )
        }

        // ✅ Map Picker Screen
        composable(
            route = Screen.MapPicker.route,
            arguments = listOf(
                navArgument("lat") { type = NavType.StringType },
                navArgument("lng") { type = NavType.StringType },
                navArgument("locationType") {
                    type = NavType.StringType
                    defaultValue = "pickup"
                }
            )
        ) { backStackEntry ->
            val lat = backStackEntry.arguments?.getString("lat")?.toDoubleOrNull() ?: 19.0760
            val lng = backStackEntry.arguments?.getString("lng")?.toDoubleOrNull() ?: 72.8777
            val locationType = backStackEntry.arguments?.getString("locationType") ?: "pickup"
            val initialLocation = LatLng(lat, lng)

            MapPickerScreen(
                initialLocation = initialLocation,
                onLocationSelected = { address ->
                    // Save selected address
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("selected_address_$locationType", address)

                    // Navigate to address confirmation
                    navController.navigate(Screen.AddressConfirm.createRoute(locationType)) {
                        popUpTo(Screen.LocationSearch.route) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        // ✅ Address Confirmation Screen
        composable(
            route = Screen.AddressConfirm.route,
            arguments = listOf(
                navArgument("locationType") {
                    type = NavType.StringType
                    defaultValue = "pickup"
                }
            )
        ) { backStackEntry ->
            val locationType = backStackEntry.arguments?.getString("locationType") ?: "pickup"
            val selectedAddress = navController.previousBackStackEntry
                ?.savedStateHandle
                ?.get<SavedAddress>("selected_address_$locationType")

            AddressConfirmationScreen(
                address = selectedAddress,
                locationType = locationType,
                onConfirm = { confirmedAddress ->
                    // Save confirmed address
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("confirmed_address_$locationType", confirmedAddress)

                    when (locationType) {
                        "pickup" -> {
                            // After pickup, ask for drop
                            navController.navigate(Screen.LocationSearch.createRoute("drop")) {
                                popUpTo(Screen.Home.route) { inclusive = false }
                            }
                        }
                        "drop" -> {
                            // After drop, go to booking confirmation
                            navController.navigate(Screen.BookingConfirm.route) {
                                popUpTo(Screen.Home.route) { inclusive = false }
                            }
                        }
                    }
                },
                onEdit = {
                    // Go back to search
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() }
            )
        }

        // ✅ Booking Confirmation Screen
        composable(route = Screen.BookingConfirm.route) {
            val pickupAddress = navController.previousBackStackEntry
                ?.savedStateHandle
                ?.get<SavedAddress>("confirmed_address_pickup")

            val dropAddress = navController.previousBackStackEntry
                ?.savedStateHandle
                ?.get<SavedAddress>("confirmed_address_drop")

            BookingConfirmationScreen(
                pickupAddress = pickupAddress,
                dropAddress = dropAddress,
                onConfirmBooking = {
                    // TODO: Create booking
                    navController.popBackStack(Screen.Home.route, inclusive = false)
                },
                onBack = { navController.popBackStack() }
            )
        }
    }
}