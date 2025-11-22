// NavGraph.kt
package com.mobitechs.parcelwala.ui.navigation

import android.annotation.SuppressLint
import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.google.android.gms.maps.model.LatLng
import com.mobitechs.parcelwala.data.local.PreferencesManager
import com.mobitechs.parcelwala.data.model.request.SavedAddress
import com.mobitechs.parcelwala.ui.screens.auth.CompleteProfileScreen
import com.mobitechs.parcelwala.ui.screens.auth.LoginScreen
import com.mobitechs.parcelwala.ui.screens.auth.OtpScreen
import com.mobitechs.parcelwala.ui.screens.booking.*
import com.mobitechs.parcelwala.ui.screens.main.MainScreen
import com.mobitechs.parcelwala.ui.screens.splash.SplashScreen
import com.mobitechs.parcelwala.ui.viewmodel.BookingNavigationEvent
import com.mobitechs.parcelwala.ui.viewmodel.BookingViewModel

@SuppressLint("UnrememberedGetBackStackEntry")
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
                    // Navigate to booking flow
                    navController.navigate("booking_flow")
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
                    navController.navigate("booking_flow")
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

        // ✅ BOOKING FLOW - Nested Navigation to Share ViewModel
        navigation(
            startDestination = "location_search/pickup",
            route = "booking_flow"
        ) {
            // ✅ Location Search Screen
            composable(
                route = "location_search/{locationType}",
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
                        // Save to savedStateHandle
                        navController.currentBackStackEntry
                            ?.savedStateHandle
                            ?.set("selected_address", address)

                        // Navigate to address confirmation
                        navController.navigate("address_confirm/$locationType")
                    },
                    onMapPicker = { latLng ->
                        navController.navigate(
                            "map_picker/${latLng.latitude}/${latLng.longitude}/$locationType"
                        )
                    },
                    onBack = {
                        if (locationType == "pickup") {
                            navController.navigate(Screen.Main.route) {
                                popUpTo("booking_flow") { inclusive = true }
                            }
                        } else {
                            navController.popBackStack()
                        }
                    }
                )
            }

            // ✅ Map Picker Screen
            composable(
                route = "map_picker/{lat}/{lng}/{locationType}",
                arguments = listOf(
                    navArgument("lat") { type = NavType.StringType },
                    navArgument("lng") { type = NavType.StringType },
                    navArgument("locationType") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val lat = backStackEntry.arguments?.getString("lat")?.toDoubleOrNull() ?: 19.0760
                val lng = backStackEntry.arguments?.getString("lng")?.toDoubleOrNull() ?: 72.8777
                val locationType = backStackEntry.arguments?.getString("locationType") ?: "pickup"
                val initialLocation = LatLng(lat, lng)

                MapPickerScreen(
                    initialLocation = initialLocation,
                    onLocationSelected = { address ->
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set("selected_address", address)
                        navController.popBackStack()
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            // ✅ Address Confirmation Screen
            composable(
                route = "address_confirm/{locationType}",
                arguments = listOf(
                    navArgument("locationType") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val locationType = backStackEntry.arguments?.getString("locationType") ?: "pickup"
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry("booking_flow")
                }
                val viewModel: BookingViewModel = hiltViewModel(parentEntry)

                val selectedAddress = navController.previousBackStackEntry
                    ?.savedStateHandle
                    ?.get<SavedAddress>("selected_address")

                AddressConfirmationScreen(
                    address = selectedAddress,
                    locationType = locationType,
                    onConfirm = { confirmedAddress ->
                        if (locationType == "pickup") {
                            viewModel.setPickupAddress(confirmedAddress)
                            navController.navigate("location_search/drop") {
                                popUpTo("location_search/pickup") { inclusive = false }
                            }
                        } else {
                            viewModel.setDropAddress(confirmedAddress)
                            navController.navigate("booking_confirm") {
                                popUpTo("location_search/pickup") { inclusive = false }
                            }
                        }
                    },
                    onEdit = { navController.popBackStack() },
                    onBack = { navController.popBackStack() }
                )
            }

            // ✅ Booking Confirmation Screen
            composable("booking_confirm") {
                val parentEntry = remember(navController) {
                    navController.getBackStackEntry("booking_flow")
                }
                val viewModel: BookingViewModel = hiltViewModel(parentEntry)
                val uiState by viewModel.uiState.collectAsState()

                BookingConfirmationScreen(
                    pickupAddress = uiState.pickupAddress,
                    dropAddress = uiState.dropAddress,
                    onVehicleSelected = { vehicle ->
                        viewModel.setSelectedVehicle(vehicle)
                        navController.navigate("review_booking")
                    },
                    onChangePickup = {
                        navController.navigate("location_search/pickup")
                    },
                    onChangeDrop = {
                        navController.navigate("location_search/drop")
                    },
                    onBack = { navController.popBackStack() },
                    viewModel = viewModel
                )
            }

            // ✅ Review Booking Screen
            composable("review_booking") {
                val parentEntry = remember(navController) {
                    navController.getBackStackEntry("booking_flow")
                }
                val viewModel: BookingViewModel = hiltViewModel(parentEntry)
                val uiState by viewModel.uiState.collectAsState()

                var showGoodsTypeSheet by remember { mutableStateOf(false) }
                var showRestrictedItemsSheet by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    viewModel.navigationEvent.collect { event ->
                        when (event) {
                            is BookingNavigationEvent.NavigateToSearchingRider -> {
                                navController.navigate("searching_rider/${event.bookingId}") {
                                    popUpTo("booking_flow") { inclusive = false }
                                }
                            }
                            BookingNavigationEvent.NavigateToHome -> {
                                navController.navigate(Screen.Main.route) {
                                    popUpTo("booking_flow") { inclusive = true }
                                }
                            }
                        }
                    }
                }

                if (uiState.pickupAddress == null || uiState.dropAddress == null || uiState.selectedVehicle == null) {
                    LaunchedEffect(Unit) {
                        navController.popBackStack()
                    }
                } else {
                    ReviewBookingScreen(
                        selectedVehicle = uiState.selectedVehicle!!,
                        pickupAddress = uiState.pickupAddress!!,
                        dropAddress = uiState.dropAddress!!,
                        onConfirmBooking = { viewModel.confirmBooking() },
                        onApplyCoupon = { navController.navigate("coupons") },
                        onViewAddressDetails = {},
                        onSelectGoodsType = { showGoodsTypeSheet = true },
                        onViewRestrictions = { showRestrictedItemsSheet = true },
                        onAddGSTIN = { navController.navigate("add_gstin") },
                        onBack = { navController.popBackStack() },
                        viewModel = viewModel
                    )

                    if (showGoodsTypeSheet) {
                        GoodsTypeBottomSheet(
                            onDismiss = { showGoodsTypeSheet = false },
                            onConfirm = { goodsType ->
                                viewModel.setGoodsType(goodsType)
                                showGoodsTypeSheet = false
                            }
                        )
                    }

                    if (showRestrictedItemsSheet) {
                        RestrictedItemsBottomSheet(
                            onDismiss = { showRestrictedItemsSheet = false }
                        )
                    }
                }
            }

            // ✅ Coupons Screen
            composable("coupons") {
                val parentEntry = remember(navController) {
                    navController.getBackStackEntry("booking_flow")
                }
                val viewModel: BookingViewModel = hiltViewModel(parentEntry)

                CouponScreen(
                    onBack = { navController.popBackStack() },
                    onCouponApplied = { couponCode ->
                        viewModel.applyCoupon(couponCode)
                        navController.popBackStack()
                    },
                    viewModel = viewModel
                )
            }

            // ✅ Add GSTIN Screen
            composable("add_gstin") {
                val parentEntry = remember(navController) {
                    navController.getBackStackEntry("booking_flow")
                }
                val viewModel: BookingViewModel = hiltViewModel(parentEntry)

                AddGSTINScreen(
                    onSave = { gstin ->
                        viewModel.addGSTIN(gstin)
                        navController.popBackStack()
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            // ✅ Searching Rider Screen
            composable(
                route = "searching_rider/{bookingId}",
                arguments = listOf(
                    navArgument("bookingId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val bookingId = backStackEntry.arguments?.getString("bookingId") ?: ""
                val parentEntry = remember(navController) {
                    navController.getBackStackEntry("booking_flow")
                }
                val viewModel: BookingViewModel = hiltViewModel(parentEntry)
                val uiState by viewModel.uiState.collectAsState()

                LaunchedEffect(Unit) {
                    viewModel.navigationEvent.collect { event ->
                        when (event) {
                            is BookingNavigationEvent.NavigateToHome -> {
                                navController.navigate(Screen.Main.route) {
                                    popUpTo("booking_flow") { inclusive = true }
                                }
                            }
                            else -> {}
                        }
                    }
                }

                if (uiState.pickupAddress != null && uiState.dropAddress != null && uiState.selectedVehicle != null) {
                    SearchingRiderScreen(
                        bookingId = bookingId,
                        pickupAddress = uiState.pickupAddress!!,
                        dropAddress = uiState.dropAddress!!,
                        selectedVehicle = uiState.selectedVehicle!!,
                        fare = uiState.finalFare,
                        onRiderFound = {},
                        onContactSupport = {},
                        onViewDetails = {},
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

