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
        // Splash Screen
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

        // Login Screen
        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToOtp = { phoneNumber ->
                    navController.navigate(Screen.Otp.createRoute(phoneNumber))
                }
            )
        }

        // OTP Screen
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

        // Complete Profile Screen
        composable(Screen.CompleteProfile.route) {
            CompleteProfileScreen(
                onNavigateToHome = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.CompleteProfile.route) { inclusive = true }
                    }
                }
            )
        }

        // ✅ FIXED: Changed from Screen.Home.route to Screen.Main.route
        // Main Screen (with bottom navigation)
        composable(Screen.Main.route) {
            MainScreen(
                preferencesManager = preferencesManager,
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Main.route) { inclusive = true }
                    }
                }
            )
        }

        // Location Search
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
                    // Save address to savedStateHandle for next screen
                    navController.currentBackStackEntry
                        ?.savedStateHandle
                        ?.set("selected_address", selectedAddress)

                    // Navigate to address confirmation
                    navController.navigate("address_confirm/$locationType")
                },
                onBack = { navController.popBackStack() }
            )
        }

        // Address Confirmation
        composable(
            route = "address_confirm/{type}",
            arguments = listOf(
                navArgument("type") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val locationType = backStackEntry.arguments?.getString("type") ?: "pickup"

            // Get address from previous screen's savedStateHandle
            val selectedAddress = navController.previousBackStackEntry
                ?.savedStateHandle
                ?.get<com.mobitechs.parcelwala.data.model.request.SavedAddress>("selected_address")

            if (selectedAddress != null) {
                AddressConfirmScreen(
                    address = selectedAddress,
                    locationType = locationType,
                    onConfirm = { confirmedAddress ->
                        if (locationType == "pickup") {
                            // Save pickup address
                            navController.currentBackStackEntry
                                ?.savedStateHandle
                                ?.set("pickup_address", confirmedAddress)

                            // Navigate to drop location search
                            navController.navigate("location_search/drop")
                        } else {
                            // Save drop address
                            navController.currentBackStackEntry
                                ?.savedStateHandle
                                ?.set("drop_address", confirmedAddress)

                            // Both addresses confirmed, navigate to booking summary
                            // TODO: Implement booking summary screen
                            navController.popBackStack(Screen.Main.route, false)
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            } else {
                // If no address selected, go back
                navController.popBackStack()
            }
        }
    }
}