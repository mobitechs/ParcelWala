// ui/navigation/NavGraph.kt
package com.mobitechs.parcelwala.ui.navigation

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.mobitechs.parcelwala.data.manager.ActiveBooking
import com.mobitechs.parcelwala.data.model.request.SavedAddress
import com.mobitechs.parcelwala.data.model.response.OrderResponse
import com.mobitechs.parcelwala.ui.screens.account.AddressSearchScreen
import com.mobitechs.parcelwala.ui.screens.account.ProfileDetailsScreen
import com.mobitechs.parcelwala.ui.screens.account.SavedAddressesScreen
import com.mobitechs.parcelwala.ui.screens.auth.CompleteProfileScreen
import com.mobitechs.parcelwala.ui.screens.auth.LoginScreen
import com.mobitechs.parcelwala.ui.screens.auth.OtpScreen
import com.mobitechs.parcelwala.ui.screens.booking.AddGSTINScreen
import com.mobitechs.parcelwala.ui.screens.booking.AddressConfirmationScreen
import com.mobitechs.parcelwala.ui.screens.booking.BookingConfirmationScreen
import com.mobitechs.parcelwala.ui.screens.booking.CouponScreen
import com.mobitechs.parcelwala.ui.screens.booking.GoodsTypeBottomSheet
import com.mobitechs.parcelwala.ui.screens.booking.LocationSearchScreen
import com.mobitechs.parcelwala.ui.screens.booking.MapPickerScreen
import com.mobitechs.parcelwala.ui.screens.booking.RestrictedItemsBottomSheet
import com.mobitechs.parcelwala.ui.screens.booking.ReviewBookingScreen
import com.mobitechs.parcelwala.ui.screens.booking.SearchingRiderScreen
import com.mobitechs.parcelwala.ui.screens.main.MainScreen
import com.mobitechs.parcelwala.ui.screens.orders.OrderDetailsScreen
import com.mobitechs.parcelwala.ui.screens.splash.SplashScreen
import com.mobitechs.parcelwala.ui.theme.AppColors
import com.mobitechs.parcelwala.ui.viewmodel.AccountViewModel
import com.mobitechs.parcelwala.ui.viewmodel.BookingNavigationEvent
import com.mobitechs.parcelwala.ui.viewmodel.BookingViewModel

/**
 * Main Navigation Graph
 * Handles all app navigation including:
 * - Authentication flow
 * - Main app with bottom navigation
 * - Booking flow (nested navigation) with Book Again support
 * - Account/Profile management flow (nested navigation)
 * - Order details
 *
 * KEY FEATURES:
 * - Edit button (✏️): Edit contact details only → address_confirm?isEdit=true
 * - Change button (📍): Change location → location_search?isChange=true
 * - Book Again: Prefill from order → booking_confirm directly
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

    // ✅ State to track if this is a Book Again flow
    var isBookAgainFlow by remember { mutableStateOf(false) }

    // ✅ State to hold address for editing (account flow)
    var addressToEdit by remember { mutableStateOf<SavedAddress?>(null) }

    // ✅ State to hold address selected from search/map picker (account flow)
    var pendingAccountAddress by remember { mutableStateOf<SavedAddress?>(null) }

// Add state for active booking navigation
    var activeBookingToResume by remember { mutableStateOf<ActiveBooking?>(null) }


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
                    // ✅ Normal booking flow - reset flags
                    isBookAgainFlow = false
                    orderForBookAgain = null
                    navController.navigate("booking_flow")
                },
                onNavigateToOrderDetails = { order ->
                    selectedOrder = order
                    navController.navigate(Screen.OrderDetails.route)
                },
                onBookAgain = { order ->
                    // ✅ Book Again flow - set flags
                    orderForBookAgain = order
                    isBookAgainFlow = true
                    navController.navigate("booking_flow")
                },
                onNavigateToActiveBooking = { activeBooking ->
                    activeBookingToResume = activeBooking
                    navController.navigate("active_booking_flow")
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
                        // ✅ Book Again from order details
                        orderForBookAgain = orderToBook
                        isBookAgainFlow = true
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
                        navController.navigate("address_search")  // ✅ New address → go to search
                    },
                    onEditAddress = { address ->
                        addressToEdit = address
                        navController.navigate("account_address_confirm")  // ✅ FIXED: Edit → go directly to confirm
                    },
                    viewModel = viewModel
                )
            }

            // ✅ Address Search Screen (for adding new address OR changing location)
            composable("address_search") {
                AddressSearchScreen(
                    onAddressSelected = { address ->
                        // ✅ If editing (changing location), preserve contact info from original address
                        val mergedAddress = if (addressToEdit != null) {
                            address.copy(
                                addressId = addressToEdit!!.addressId,
                                addressType = addressToEdit!!.addressType,
                                label = addressToEdit!!.label,
                                contactName = addressToEdit!!.contactName,
                                contactPhone = addressToEdit!!.contactPhone,
                                buildingDetails = addressToEdit!!.buildingDetails,  // Updated field
                                landmark = addressToEdit!!.landmark,
                                pincode = addressToEdit!!.pincode,
                                isDefault = addressToEdit!!.isDefault
                                // latitude, longitude, address come from the new selection
                            )
                        } else {
                            address
                        }

                        // ✅ FIXED: Use state variable instead of savedStateHandle
                        pendingAccountAddress = mergedAddress
                        navController.navigate("account_address_confirm")
                    },
                    onMapPicker = { latLng ->
                        navController.navigate(
                            "address_map_picker/${latLng.latitude}/${latLng.longitude}"
                        )
                    },
                    onBack = {
                        pendingAccountAddress = null  // ✅ Clear on back
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
                        // ✅ If editing (changing location), preserve contact info from original address
                        val mergedAddress = if (addressToEdit != null) {
                            address.copy(
                                addressId = addressToEdit!!.addressId,
                                addressType = addressToEdit!!.addressType,
                                label = addressToEdit!!.label,
                                contactName = addressToEdit!!.contactName,
                                contactPhone = addressToEdit!!.contactPhone,
                                buildingDetails = addressToEdit!!.buildingDetails,  // Updated field
                                landmark = addressToEdit!!.landmark,
                                pincode = addressToEdit!!.pincode,
                                isDefault = addressToEdit!!.isDefault
                            )
                        } else {
                            address
                        }

                        // ✅ FIXED: Use state variable instead of savedStateHandle
                        pendingAccountAddress = mergedAddress
                        navController.navigate("account_address_confirm") {
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

                // ✅ FIXED: Priority for address selection using state variables
                // 1. pendingAccountAddress (from address_search or map_picker - new/changed location)
                // 2. addressToEdit (for direct edit without location change)
                val selectedAddress = pendingAccountAddress ?: addressToEdit

                AddressConfirmationScreen(
                    address = selectedAddress,
                    locationType = "save",
                    onConfirm = { confirmedAddress ->
                        if (addressToEdit != null) {
                            viewModel.updateAddress(confirmedAddress)
                        } else {
                            viewModel.saveAddress(confirmedAddress)
                        }
                        // ✅ Clear both state variables
                        addressToEdit = null
                        pendingAccountAddress = null
                        navController.navigate("saved_addresses") {
                            popUpTo("saved_addresses") { inclusive = true }
                        }
                    },
                    onChangeLocation = {
                        // ✅ Clear pendingAccountAddress before going to search
                        pendingAccountAddress = null
                        // Keep addressToEdit so contact info can be preserved
                        navController.navigate("address_search")
                    },
                    onBack = {
                        // ✅ Clear both state variables when going back
                        addressToEdit = null
                        pendingAccountAddress = null
                        navController.popBackStack()
                    },
                    isEditMode = addressToEdit != null,
                    userPhoneNumber = user?.phoneNumber,
                    showSaveLocationBadge = true
                )
            }
        }

        // ============ BOOKING FLOW - NESTED NAVIGATION ============
        navigation(
            startDestination = "booking_entry",
            route = "booking_flow"
        ) {
            // ═══════════════════════════════════════════════════════════════
            // ENTRY POINT - Decides flow direction (Book Again vs Normal)
            // ═══════════════════════════════════════════════════════════════
            composable("booking_entry") { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry("booking_flow")
                }
                val viewModel: BookingViewModel = hiltViewModel(parentEntry)

                LaunchedEffect(orderForBookAgain, isBookAgainFlow) {
                    if (isBookAgainFlow && orderForBookAgain != null) {
                        // ✅ Book Again flow - prefill and go to booking confirm
                        viewModel.prefillFromOrder(orderForBookAgain!!)
                        orderForBookAgain = null
                        isBookAgainFlow = false
                        navController.navigate("booking_confirm") {
                            popUpTo("booking_entry") { inclusive = true }
                        }
                    } else {
                        // ✅ Normal flow - go to location search
                        navController.navigate("location_search/pickup") {
                            popUpTo("booking_entry") { inclusive = true }
                        }
                    }
                }

                // Loading while deciding
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AppColors.Primary)
                }
            }

            // ═══════════════════════════════════════════════════════════════
            // LOCATION SEARCH SCREEN
            // Route: location_search/{locationType}?isChange={isChange}
            // - locationType: "pickup" or "drop"
            // - isChange: true when changing existing location (preserves contact info)
            // ═══════════════════════════════════════════════════════════════
            composable(
                route = "location_search/{locationType}?isChange={isChange}",
                arguments = listOf(
                    navArgument("locationType") {
                        type = NavType.StringType
                        defaultValue = "pickup"
                    },
                    navArgument("isChange") {
                        type = NavType.BoolType
                        defaultValue = false
                    }
                )
            ) { backStackEntry ->
                val locationType = backStackEntry.arguments?.getString("locationType") ?: "pickup"
                val isChange = backStackEntry.arguments?.getBoolean("isChange") ?: false

                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry("booking_flow")
                }
                val viewModel: BookingViewModel = hiltViewModel(parentEntry)
                val uiState by viewModel.uiState.collectAsState()

                // Get existing address for preserving contact info when changing location
                val existingAddress =
                    if (locationType == "pickup") uiState.pickupAddress else uiState.dropAddress

                LocationSearchScreen(
                    locationType = locationType,
                    onAddressSelected = { address ->
                        // ✅ Preserve contact info from existing address when changing location
                        val mergedAddress = if (isChange && existingAddress != null) {
                            address.copy(
                                contactName = existingAddress.contactName,
                                contactPhone = existingAddress.contactPhone,
                                buildingDetails = existingAddress.buildingDetails,
                                landmark = existingAddress.landmark,
                                pincode = existingAddress.pincode
                            )
                        } else {
                            address
                        }

                        // ✅ Use ViewModel to pass address (more reliable than savedStateHandle)
                        viewModel.setPendingAddress(mergedAddress)
                        navController.navigate("address_confirm/$locationType?isChange=$isChange")
                    },
                    onMapPicker = { latLng ->
                        navController.navigate(
                            "map_picker/${latLng.latitude}/${latLng.longitude}/$locationType?isChange=$isChange"
                        )
                    },
                    onBack = {
                        if (isChange) {
                            // Go back to booking_confirm when changing
                            navController.popBackStack()
                        } else if (locationType == "pickup") {
                            navController.navigate(Screen.Main.route) {
                                popUpTo("booking_flow") { inclusive = true }
                            }
                        } else {
                            navController.popBackStack()
                        }
                    }
                )
            }

            // ═══════════════════════════════════════════════════════════════
            // MAP PICKER SCREEN
            // Route: map_picker/{lat}/{lng}/{locationType}?isChange={isChange}
            // ═══════════════════════════════════════════════════════════════
            composable(
                route = "map_picker/{lat}/{lng}/{locationType}?isChange={isChange}",
                arguments = listOf(
                    navArgument("lat") { type = NavType.StringType },
                    navArgument("lng") { type = NavType.StringType },
                    navArgument("locationType") { type = NavType.StringType },
                    navArgument("isChange") {
                        type = NavType.BoolType
                        defaultValue = false
                    }
                )
            ) { backStackEntry ->
                val lat = backStackEntry.arguments?.getString("lat")?.toDoubleOrNull() ?: 19.0760
                val lng = backStackEntry.arguments?.getString("lng")?.toDoubleOrNull() ?: 72.8777
                val locationType = backStackEntry.arguments?.getString("locationType") ?: "pickup"
                val isChange = backStackEntry.arguments?.getBoolean("isChange") ?: false
                val initialLocation = LatLng(lat, lng)

                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry("booking_flow")
                }
                val viewModel: BookingViewModel = hiltViewModel(parentEntry)
                val uiState by viewModel.uiState.collectAsState()

                // Get existing address for preserving contact info
                val existingAddress =
                    if (locationType == "pickup") uiState.pickupAddress else uiState.dropAddress

                MapPickerScreen(
                    initialLocation = initialLocation,
                    onLocationSelected = { address ->
                        // ✅ Preserve contact info from existing address
                        val mergedAddress = if (isChange && existingAddress != null) {
                            address.copy(
                                contactName = existingAddress.contactName,
                                contactPhone = existingAddress.contactPhone,
                                buildingDetails = existingAddress.buildingDetails,
                                landmark = existingAddress.landmark,
                                pincode = existingAddress.pincode
                            )
                        } else {
                            address
                        }

                        // ✅ Use ViewModel to pass address (more reliable than savedStateHandle)
                        viewModel.setPendingAddress(mergedAddress)
                        navController.navigate("address_confirm/$locationType?isChange=$isChange") {
                            // Pop map picker from stack
                            popUpTo("map_picker/{lat}/{lng}/{locationType}?isChange={isChange}") {
                                inclusive = true
                            }
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            // ═══════════════════════════════════════════════════════════════
            // ADDRESS CONFIRMATION SCREEN
            // Route: address_confirm/{locationType}?isEdit={isEdit}&isChange={isChange}
            //
            // Parameters:
            // - locationType: "pickup" or "drop"
            // - isEdit: true when editing existing address details (from Edit ✏️ button)
            // - isChange: true when changing location (from Change 📍 button)
            //
            // Behavior:
            // - isEdit=true: Load existing address from ViewModel, show "Save Changes"
            // - isChange=true/false (new): Load address from ViewModel's pendingAddress
            // ═══════════════════════════════════════════════════════════════
            composable(
                route = "address_confirm/{locationType}?isEdit={isEdit}&isChange={isChange}",
                arguments = listOf(
                    navArgument("locationType") { type = NavType.StringType },
                    navArgument("isEdit") {
                        type = NavType.BoolType
                        defaultValue = false
                    },
                    navArgument("isChange") {
                        type = NavType.BoolType
                        defaultValue = false
                    }
                )
            ) { backStackEntry ->
                val locationType = backStackEntry.arguments?.getString("locationType") ?: "pickup"
                val isEdit = backStackEntry.arguments?.getBoolean("isEdit") ?: false
                val isChange = backStackEntry.arguments?.getBoolean("isChange") ?: false

                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry("booking_flow")
                }
                val viewModel: BookingViewModel = hiltViewModel(parentEntry)

                AddressConfirmationScreen(
                    address = null, // Will be read from ViewModel
                    locationType = locationType,
                    isEditMode = isEdit || isChange,
                    viewModel = viewModel, // ✅ Pass ViewModel
                    onConfirm = { confirmedAddress ->
                        // ✅ Save address to ViewModel (includes lat/lng)
                        if (locationType == "pickup") {
                            viewModel.setPickupAddress(confirmedAddress)
                        } else {
                            viewModel.setDropAddress(confirmedAddress)
                        }

                        // ✅ Clear pending address
                        viewModel.clearPendingAddress()

                        // ✅ Navigate based on flow
                        if (isEdit || isChange) {
                            // Editing or Changing: go back to booking_confirm
                            navController.navigate("booking_confirm") {
                                popUpTo("booking_confirm") { inclusive = true }
                            }
                        } else if (locationType == "pickup") {
                            // Normal flow: go to drop location search
                            navController.navigate("location_search/drop") {
                                popUpTo("location_search/pickup") { inclusive = false }
                            }
                        } else {
                            // Normal flow (drop): go to booking confirm
                            navController.navigate("booking_confirm") {
                                popUpTo("location_search/pickup") { inclusive = false }
                            }
                        }
                    },
                    onChangeLocation = {
                        // ✅ Clear pending address before going to location search
                        viewModel.clearPendingAddress()
                        // Change location button clicked - go to location search with isChange=true
                        navController.navigate("location_search/$locationType?isChange=true")
                    },
                    onBack = {
                        viewModel.clearPendingAddress()
                        navController.popBackStack()
                    }
                )
            }

            // ═══════════════════════════════════════════════════════════════
            // BOOKING CONFIRMATION SCREEN
            // Shows both addresses with Edit (✏️) and Change (📍) buttons
            // ═══════════════════════════════════════════════════════════════
            composable("booking_confirm") {
                val parentEntry = remember(navController) {
                    navController.getBackStackEntry("booking_flow")
                }
                val viewModel: BookingViewModel = hiltViewModel(parentEntry)
                val uiState by viewModel.uiState.collectAsState()

                BookingConfirmationScreen(
                    pickupAddress = uiState.pickupAddress,
                    dropAddress = uiState.dropAddress,
                    preSelectedVehicleId = uiState.preferredVehicleTypeId,
                    isPrefilledFromOrder = uiState.isBookAgain,
                    onVehicleSelected = { fareDetails ->  // ✅ Changed: Now receives FareDetails
                        viewModel.selectFareDetails(fareDetails)  // ✅ Changed: Use new method
                        navController.navigate("review_booking")
                    },
                    // ✅ Edit (✏️): Go to address_confirm to edit details (name, phone, flat, etc.)
                    onEditPickup = {
                        viewModel.clearPendingAddress()
                        navController.navigate("address_confirm/pickup?isEdit=true")
                    },
                    onEditDrop = {
                        viewModel.clearPendingAddress()
                        navController.navigate("address_confirm/drop?isEdit=true")
                    },
                    // ✅ Change (📍): Go to location_search to change location
                    onChangePickup = {
                        viewModel.clearPendingAddress()
                        navController.navigate("location_search/pickup?isChange=true")
                    },
                    onChangeDrop = {
                        viewModel.clearPendingAddress()
                        navController.navigate("location_search/drop?isChange=true")
                    },
                    onBack = {
                        if (uiState.isBookAgain) {
                            navController.navigate(Screen.Main.route) {
                                popUpTo("booking_flow") { inclusive = true }
                            }
                        } else {
                            navController.popBackStack()
                        }
                    },
                    viewModel = viewModel
                )
            }

            // ═══════════════════════════════════════════════════════════════
            // REVIEW BOOKING SCREEN
            // ═══════════════════════════════════════════════════════════════
            composable("review_booking") {
                val parentEntry = remember(navController) {
                    navController.getBackStackEntry("booking_flow")
                }
                val viewModel: BookingViewModel = hiltViewModel(parentEntry)
                val uiState by viewModel.uiState.collectAsState()
                val selectedFareDetails by viewModel.selectedFareDetails.collectAsState()  // ✅ Changed: Use FareDetails

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

                // ✅ Changed: Check for selectedFareDetails instead of selectedVehicle
                if (uiState.pickupAddress == null || uiState.dropAddress == null || selectedFareDetails == null) {
                    LaunchedEffect(Unit) {
                        navController.popBackStack()
                    }
                } else {
                    ReviewBookingScreen(
                        selectedFareDetails = selectedFareDetails!!,  // ✅ Changed: Pass FareDetails
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

            // ═══════════════════════════════════════════════════════════════
            // COUPONS SCREEN
            // ═══════════════════════════════════════════════════════════════
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

            // ═══════════════════════════════════════════════════════════════
            // ADD GSTIN SCREEN
            // ═══════════════════════════════════════════════════════════════
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

            // ═══════════════════════════════════════════════════════════════
            // SEARCHING RIDER SCREEN
            // ═══════════════════════════════════════════════════════════════
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
                val selectedFareDetails by viewModel.selectedFareDetails.collectAsState()  // ✅ Changed

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

                // ✅ Changed: Use selectedFareDetails instead of selectedVehicle
                if (uiState.pickupAddress != null && uiState.dropAddress != null && selectedFareDetails != null) {
                    SearchingRiderScreen(
                        bookingId = bookingId,
                        pickupAddress = uiState.pickupAddress!!,
                        dropAddress = uiState.dropAddress!!,
                        selectedFareDetails = selectedFareDetails!!,  // ✅ Changed
                        fare = uiState.finalFare,
                        onRiderFound = {},
                        onContactSupport = {},
                        onViewDetails = {},
                        viewModel = viewModel
                    )
                } else {
                    // ✅ Add loading/fallback UI instead of blank screen
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = AppColors.Primary)
                    }
                }
            }


        }

        // ✅ ADD THIS - ACTIVE BOOKING FLOW (separate from booking_flow)
        navigation(
            startDestination = "active_searching_rider",
            route = "active_booking_flow"
        ) {
            composable("active_searching_rider") {
                val activeBooking = activeBookingToResume

                if (activeBooking != null) {
                    val viewModel: BookingViewModel = hiltViewModel()

                    SearchingRiderScreen(
                        bookingId = activeBooking.bookingId,
                        pickupAddress = activeBooking.pickupAddress,
                        dropAddress = activeBooking.dropAddress,
                        selectedFareDetails = activeBooking.fareDetails,
                        fare = activeBooking.fare,
                        onRiderFound = { },
                        onContactSupport = { },
                        onViewDetails = { },
                        viewModel = viewModel
                    )
                } else {
                    LaunchedEffect(Unit) {
                        navController.popBackStack()
                    }
                }
            }
        }



    }//nva host

}