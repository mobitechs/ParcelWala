// ui/navigation/NavGraph.kt
package com.mobitechs.parcelwala.ui.navigation

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
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
import com.mobitechs.parcelwala.data.model.response.OrderResponse
import com.mobitechs.parcelwala.ui.screens.account.*
import com.mobitechs.parcelwala.ui.screens.auth.CompleteProfileScreen
import com.mobitechs.parcelwala.ui.screens.auth.LoginScreen
import com.mobitechs.parcelwala.ui.screens.auth.OtpScreen
import com.mobitechs.parcelwala.ui.screens.booking.*
import com.mobitechs.parcelwala.ui.screens.main.MainScreen
import com.mobitechs.parcelwala.ui.screens.orders.OrderDetailsScreen
import com.mobitechs.parcelwala.ui.screens.splash.SplashScreen
import com.mobitechs.parcelwala.ui.viewmodel.AccountViewModel
import com.mobitechs.parcelwala.ui.viewmodel.BookingNavigationEvent
import com.mobitechs.parcelwala.ui.viewmodel.BookingViewModel

/**
 * Main Navigation Graph
 * Handles all app navigation including:
 * - Authentication flow
 * - Main app with bottom navigation
 * - Booking flow (nested navigation)
 * - Account/Profile management flow (nested navigation)
 * - Order details
 */
@SuppressLint("UnrememberedGetBackStackEntry")
@Composable
fun NavGraph(
    navController: NavHostController,
    preferencesManager: PreferencesManager
) {
    val isLoggedIn = preferencesManager.isLoggedIn()
    val context = LocalContext.current

    // ✅ State to hold selected order for details screen
    var selectedOrder by remember { mutableStateOf<OrderResponse?>(null) }

    // ✅ State to hold order for book again flow
    var orderForBookAgain by remember { mutableStateOf<OrderResponse?>(null) }

    // ✅ State to hold address for editing
    var addressToEdit by remember { mutableStateOf<SavedAddress?>(null) }

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        // ============ SPLASH ============
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

        // ============ AUTH FLOW ============
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

        // ============ MAIN SCREEN WITH BOTTOM NAV ============
        composable(Screen.Main.route) {
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
                onNavigateToOrderDetails = { order ->
                    selectedOrder = order
                    navController.navigate(Screen.OrderDetails.route)
                },
                onBookAgain = { order ->
                    orderForBookAgain = order
                    navController.navigate("booking_flow")
                },
                // ✅ Account navigation callbacks
                onNavigateToSavedAddresses = {
                    navController.navigate("account_flow")
                },
                onNavigateToProfileDetails = {
                    navController.navigate("profile_details")
                },
                onNavigateToGSTDetails = {
                    // Handled in AccountScreen with bottom sheet
                },
                currentRoute = "home"
            )
        }

        // ============ PROFILE DETAILS SCREEN ============
        composable("profile_details") {
            ProfileDetailsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // ============ ORDER DETAILS SCREEN ============
        composable(Screen.OrderDetails.route) {
            selectedOrder?.let { order ->
                OrderDetailsScreen(
                    order = order,
                    onBack = {
                        navController.popBackStack()
                    },
                    onBookAgain = { orderToBook ->
                        orderForBookAgain = orderToBook
                        navController.navigate("booking_flow") {
                            popUpTo(Screen.Main.route) { inclusive = false }
                        }
                    },
                    onCallDriver = { phoneNumber ->
                        val intent = Intent(Intent.ACTION_DIAL).apply {
                            data = Uri.parse("tel:$phoneNumber")
                        }
                        context.startActivity(intent)
                    },
                    onCallSupport = {
                        val intent = Intent(Intent.ACTION_DIAL).apply {
                            data = Uri.parse("tel:+919876543210")
                        }
                        context.startActivity(intent)
                    }
                )
            } ?: run {
                LaunchedEffect(Unit) {
                    navController.popBackStack()
                }
            }
        }

        // ============ ACCOUNT FLOW - NESTED NAVIGATION ============
        navigation(
            startDestination = "saved_addresses",
            route = "account_flow"
        ) {
            // ✅ Saved Addresses List Screen
            composable("saved_addresses") {
                val parentEntry = remember(navController) {
                    navController.getBackStackEntry("account_flow")
                }
                val viewModel: AccountViewModel = hiltViewModel(parentEntry)

                // Handle address save/delete success
                val uiState by viewModel.uiState.collectAsState()

                LaunchedEffect(uiState.addressSaveSuccess) {
                    if (uiState.addressSaveSuccess) {
                        viewModel.clearAddressSaveSuccess()
                        viewModel.loadSavedAddresses()
                    }
                }

                SavedAddressesScreen(
                    onBack = {
                        navController.popBackStack()
                    },
                    onAddAddress = {
                        addressToEdit = null
                        navController.navigate("address_search")
                    },
                    onEditAddress = { address ->
                        addressToEdit = address
                        navController.navigate("address_search")
                    },
                    viewModel = viewModel
                )
            }

            // ✅ Address Search Screen (for adding new address)
            composable("address_search") {
                AddressSearchScreen(
                    onAddressSelected = { address ->
                        navController.currentBackStackEntry
                            ?.savedStateHandle
                            ?.set("selected_address", address)
                        navController.navigate("account_address_confirm")
                    },
                    onMapPicker = { latLng ->
                        navController.navigate(
                            "address_map_picker/${latLng.latitude}/${latLng.longitude}"
                        )
                    },
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }

            // ✅ Map Picker Screen for Address
            composable(
                route = "address_map_picker/{lat}/{lng}",
                arguments = listOf(
                    navArgument("lat") { type = NavType.StringType },
                    navArgument("lng") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val lat = backStackEntry.arguments?.getString("lat")?.toDoubleOrNull() ?: 19.0760
                val lng = backStackEntry.arguments?.getString("lng")?.toDoubleOrNull() ?: 72.8777
                val initialLocation = LatLng(lat, lng)

                MapPickerScreen(
                    initialLocation = initialLocation,
                    onLocationSelected = { address ->
                        // ✅ Set the address on the current entry's savedStateHandle
                        // Then navigate to confirm screen
                        navController.currentBackStackEntry
                            ?.savedStateHandle
                            ?.set("selected_address", address)
                        // Navigate to confirm screen instead of just popping back
                        navController.navigate("account_address_confirm") {
                            // Pop the map picker from backstack
                            popUpTo("address_map_picker/{lat}/{lng}") { inclusive = true }
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            // ✅ Address Confirmation Screen for Account (with save as options)
            composable("account_address_confirm") {
                val parentEntry = remember(navController) {
                    navController.getBackStackEntry("account_flow")
                }
                val viewModel: AccountViewModel = hiltViewModel(parentEntry)
                val user = preferencesManager.getUser()

                // ✅ Try to get address from multiple sources:
                // 1. Current back stack entry's savedStateHandle (from map picker)
                // 2. Previous back stack entry's savedStateHandle (from address search)
                // 3. Address being edited
                val selectedAddress = navController.currentBackStackEntry
                    ?.savedStateHandle
                    ?.get<SavedAddress>("selected_address")
                    ?: navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.get<SavedAddress>("selected_address")
                    ?: addressToEdit

                // ✅ Use the same AddressConfirmationScreen with additional parameters
                AddressConfirmationScreen(
                    address = selectedAddress,
                    locationType = "save", // Use "save" to trigger save address mode
                    onConfirm = { confirmedAddress ->
                        if (addressToEdit != null) {
                            viewModel.updateAddress(confirmedAddress)
                        } else {
                            viewModel.saveAddress(confirmedAddress)
                        }
                        addressToEdit = null
                        navController.navigate("saved_addresses") {
                            popUpTo("saved_addresses") { inclusive = true }
                        }
                    },
                    onEdit = {
                        navController.popBackStack()
                    },
                    onBack = {
                        navController.popBackStack()
                    },
                    // Additional parameters for save address mode
                    isEditMode = addressToEdit != null,
                    userPhoneNumber = user?.phoneNumber,
                    showSaveLocationBadge = true
                )
            }
        }

        // ============ BOOKING FLOW - NESTED NAVIGATION ============
        navigation(
            startDestination = "location_search/pickup",
            route = "booking_flow"
        ) {
            // Location Search Screen
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
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry("booking_flow")
                }
                val viewModel: BookingViewModel = hiltViewModel(parentEntry)

                // Prefill from book again order
                LaunchedEffect(orderForBookAgain) {
                    orderForBookAgain?.let { order ->
                        viewModel.prefillFromOrder(order)
                        orderForBookAgain = null
                    }
                }

                LocationSearchScreen(
                    locationType = locationType,
                    onAddressSelected = { address ->
                        navController.currentBackStackEntry
                            ?.savedStateHandle
                            ?.set("selected_address", address)
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

            // Map Picker Screen
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

            // Address Confirmation Screen for Booking
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

                // ✅ Standard booking flow - no additional parameters needed
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

            // Booking Confirmation Screen
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

            // Review Booking Screen
            composable("review_booking") {
                val parentEntry = remember(navController) {
                    navController.getBackStackEntry("booking_flow")
                }
                val viewModel: BookingViewModel = hiltViewModel(parentEntry)
                val uiState by viewModel.uiState.collectAsState()
                val vehicleTypes by viewModel.vehicleTypes.collectAsState()

                var showGoodsTypeSheet by remember { mutableStateOf(false) }
                var showRestrictedItemsSheet by remember { mutableStateOf(false) }

                val selectedVehicle = uiState.selectedVehicleId?.let { id ->
                    vehicleTypes.find { it.vehicleTypeId == id }
                }

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

                if (uiState.pickupAddress == null || uiState.dropAddress == null || selectedVehicle == null) {
                    LaunchedEffect(Unit) {
                        navController.popBackStack()
                    }
                } else {
                    ReviewBookingScreen(
                        selectedVehicle = selectedVehicle,
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

            // Coupons Screen
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

            // Add GSTIN Screen
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

            // Searching Rider Screen
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
                val vehicleTypes by viewModel.vehicleTypes.collectAsState()

                val selectedVehicle = uiState.selectedVehicleId?.let { id ->
                    vehicleTypes.find { it.vehicleTypeId == id }
                }

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

                if (uiState.pickupAddress != null && uiState.dropAddress != null && selectedVehicle != null) {
                    SearchingRiderScreen(
                        bookingId = bookingId,
                        pickupAddress = uiState.pickupAddress!!,
                        dropAddress = uiState.dropAddress!!,
                        selectedVehicle = selectedVehicle,
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