// NavGraph.kt
package com.mobitechs.parcelwala.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.mobitechs.parcelwala.data.local.PreferencesManager
import com.mobitechs.parcelwala.ui.screens.auth.CompleteProfileScreen
import com.mobitechs.parcelwala.ui.screens.auth.LoginScreen
import com.mobitechs.parcelwala.ui.screens.auth.OtpScreen
import com.mobitechs.parcelwala.ui.screens.booking.AddressConfirmScreen
import com.mobitechs.parcelwala.ui.screens.booking.LocationSearchScreen
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

        composable(
            route = "location_search/{type}",
            arguments = listOf(
                navArgument("type") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val locationType = backStackEntry.arguments?.getString("type") ?: "pickup"

            LocationSearchScreen(
                locationType = locationType,
                onAddressSelected = { selectedAddress ->
                    navController.currentBackStackEntry
                        ?.savedStateHandle
                        ?.set("selected_address", selectedAddress)
                    navController.navigate("address_confirm/$locationType")
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "address_confirm/{type}",
            arguments = listOf(
                navArgument("type") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val locationType = backStackEntry.arguments?.getString("type") ?: "pickup"
            val selectedAddress = navController.previousBackStackEntry
                ?.savedStateHandle
                ?.get<com.mobitechs.parcelwala.data.model.request.SavedAddress>("selected_address")

            if (selectedAddress != null) {
                AddressConfirmScreen(
                    address = selectedAddress,
                    locationType = locationType,
                    onConfirm = { confirmedAddress ->
                        if (locationType == "pickup") {
                            navController.currentBackStackEntry
                                ?.savedStateHandle
                                ?.set("pickup_address", confirmedAddress)
                            navController.navigate("location_search/drop")
                        } else {
                            navController.currentBackStackEntry
                                ?.savedStateHandle
                                ?.set("drop_address", confirmedAddress)
                            navController.popBackStack(Screen.Main.route, false)
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            } else {
                navController.popBackStack()
            }
        }
    }
}