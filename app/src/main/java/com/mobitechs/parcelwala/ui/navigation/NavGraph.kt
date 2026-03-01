// ui/navigation/NavGraph.kt
package com.mobitechs.parcelwala.ui.navigation

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import com.mobitechs.parcelwala.ui.screens.booking.RiderFoundScreen
import com.mobitechs.parcelwala.ui.screens.booking.SearchingRiderScreen
import com.mobitechs.parcelwala.ui.screens.main.MainScreen
import com.mobitechs.parcelwala.ui.screens.orders.OrderDetailsScreen
import com.mobitechs.parcelwala.ui.screens.payments.PostDeliveryPaymentScreen
import com.mobitechs.parcelwala.ui.screens.splash.SplashScreen
import com.mobitechs.parcelwala.MainActivity
import com.mobitechs.parcelwala.ui.theme.AppColors
import com.mobitechs.parcelwala.ui.viewmodel.AccountViewModel
import com.mobitechs.parcelwala.ui.viewmodel.BookingNavigationEvent
import com.mobitechs.parcelwala.ui.viewmodel.BookingViewModel
import com.mobitechs.parcelwala.ui.viewmodel.RiderTrackingNavigationEvent
import com.mobitechs.parcelwala.ui.viewmodel.RiderTrackingViewModel
import com.mobitechs.parcelwala.utils.Constants

@SuppressLint("UnrememberedGetBackStackEntry")
@Composable
fun NavGraph(
    navController: NavHostController,
    preferencesManager: PreferencesManager
) {
    val isLoggedIn = preferencesManager.isLoggedIn()
    val context = LocalContext.current

    var selectedOrder by remember { mutableStateOf<OrderResponse?>(null) }
    var orderForBookAgain by remember { mutableStateOf<OrderResponse?>(null) }
    var isBookAgainFlow by remember { mutableStateOf(false) }
    var addressToEdit by remember { mutableStateOf<SavedAddress?>(null) }
    var pendingAccountAddress by remember { mutableStateOf<SavedAddress?>(null) }
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
                onNavigateToOtp = { phoneNumber, otp ->
                    val encodedOtp = otp?.let { Uri.encode(it) } ?: "null"
                    navController.navigate("otp/$phoneNumber?otp=$encodedOtp")
                }
            )
        }

        composable(
            route = "otp/{phoneNumber}?otp={otp}",
            arguments = listOf(
                navArgument("phoneNumber") { type = NavType.StringType },
                navArgument("otp") {
                    type = NavType.StringType
                    defaultValue = "null"
                }
            )
        ) { backStackEntry ->
            val phoneNumber = backStackEntry.arguments?.getString("phoneNumber") ?: ""
            val receivedOtp = backStackEntry.arguments?.getString("otp")?.let {
                if (it == "null") null else it
            }

            OtpScreen(
                phoneNumber = phoneNumber,
                receivedOtp = receivedOtp,
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

        // ============ MAIN SCREEN ============
        composable(Screen.Main.route) {
            MainScreen(
                preferencesManager = preferencesManager,
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Main.route) { inclusive = true }
                    }
                },
                onNavigateToLocationSearch = {
                    isBookAgainFlow = false
                    orderForBookAgain = null
                    navController.navigate("booking_flow")
                },
                onNavigateToOrderDetails = { order ->
                    selectedOrder = order
                    navController.navigate(Screen.OrderDetails.route)
                },
                onBookAgain = { order ->
                    orderForBookAgain = order
                    isBookAgainFlow = true
                    navController.navigate("booking_flow")
                },
                onNavigateToActiveBooking = { activeBooking ->
                    activeBookingToResume = activeBooking
                    navController.navigate("active_booking_flow")
                },
                onNavigateToSavedAddresses = {
                    navController.navigate("account_flow")
                },
                onNavigateToProfileDetails = {
                    navController.navigate("profile_details")
                },
                onNavigateToLanguage = {
                    navController.navigate(Screen.Language.route)
                },
                onNavigateToGSTDetails = { },
                currentRoute = "home"
            )
        }

        composable(Screen.Language.route) {
            com.mobitechs.parcelwala.ui.screens.account.LanguageSelectionScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // ============ PROFILE DETAILS ============
        composable("profile_details") {
            ProfileDetailsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // ============ ORDER DETAILS ============
        composable(Screen.OrderDetails.route) {
            selectedOrder?.let { order ->
                OrderDetailsScreen(
                    order = order,
                    onBack = { navController.popBackStack() },
                    onBookAgain = { orderToBook ->
                        orderForBookAgain = orderToBook
                        isBookAgainFlow = true
                        navController.navigate("booking_flow")
                    },
                    onCallDriver = { phoneNumber ->
                        val intent = Intent(Intent.ACTION_DIAL).apply {
                            data = Uri.parse("tel:$phoneNumber")
                        }
                        context.startActivity(intent)
                    },
                    onCallSupport = {
                        val intent = Intent(Intent.ACTION_DIAL).apply {
                            data = Uri.parse(Constants.SUPPORT_MOBILE_NO)
                        }
                        context.startActivity(intent)
                    }
                )
            } ?: run {
                LaunchedEffect(Unit) { navController.popBackStack() }
            }
        }

        // ============ ACCOUNT FLOW ============
        navigation(
            startDestination = "saved_addresses",
            route = "account_flow"
        ) {
            composable("saved_addresses") {
                val parentEntry = remember(navController) {
                    navController.getBackStackEntry("account_flow")
                }
                val viewModel: AccountViewModel = hiltViewModel(parentEntry)
                val uiState by viewModel.uiState.collectAsState()

                LaunchedEffect(uiState.addressSaveSuccess) {
                    if (uiState.addressSaveSuccess) {
                        viewModel.clearAddressSaveSuccess()
                        viewModel.loadSavedAddresses()
                    }
                }

                SavedAddressesScreen(
                    onBack = { navController.popBackStack() },
                    onAddAddress = {
                        addressToEdit = null
                        navController.navigate("address_search")
                    },
                    onEditAddress = { address ->
                        addressToEdit = address
                        navController.navigate("account_address_confirm")
                    },
                    viewModel = viewModel
                )
            }

            composable("address_search") {
                AddressSearchScreen(
                    onAddressSelected = { address ->
                        val mergedAddress = if (addressToEdit != null) {
                            address.copy(
                                addressId = addressToEdit!!.addressId,
                                addressType = addressToEdit!!.addressType,
                                label = addressToEdit!!.label,
                                contactName = addressToEdit!!.contactName,
                                contactPhone = addressToEdit!!.contactPhone,
                                buildingDetails = addressToEdit!!.buildingDetails,
                                landmark = addressToEdit!!.landmark,
                                pincode = addressToEdit!!.pincode,
                                isDefault = addressToEdit!!.isDefault
                            )
                        } else address

                        pendingAccountAddress = mergedAddress
                        navController.navigate("account_address_confirm")
                    },
                    onMapPicker = { latLng ->
                        navController.navigate("address_map_picker/${latLng.latitude}/${latLng.longitude}")
                    },
                    onBack = {
                        pendingAccountAddress = null
                        navController.popBackStack()
                    }
                )
            }

            composable(
                route = "address_map_picker/{lat}/{lng}",
                arguments = listOf(
                    navArgument("lat") { type = NavType.StringType },
                    navArgument("lng") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val lat = backStackEntry.arguments?.getString("lat")?.toDoubleOrNull() ?: 19.0760
                val lng = backStackEntry.arguments?.getString("lng")?.toDoubleOrNull() ?: 72.8777

                MapPickerScreen(
                    initialLocation = LatLng(lat, lng),
                    onLocationSelected = { address ->
                        val mergedAddress = if (addressToEdit != null) {
                            address.copy(
                                addressId = addressToEdit!!.addressId,
                                addressType = addressToEdit!!.addressType,
                                label = addressToEdit!!.label,
                                contactName = addressToEdit!!.contactName,
                                contactPhone = addressToEdit!!.contactPhone,
                                buildingDetails = addressToEdit!!.buildingDetails,
                                landmark = addressToEdit!!.landmark,
                                pincode = addressToEdit!!.pincode,
                                isDefault = addressToEdit!!.isDefault
                            )
                        } else address

                        pendingAccountAddress = mergedAddress
                        navController.navigate("account_address_confirm") {
                            popUpTo("address_map_picker/{lat}/{lng}") { inclusive = true }
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable("account_address_confirm") {
                val parentEntry = remember(navController) {
                    navController.getBackStackEntry("account_flow")
                }
                val viewModel: AccountViewModel = hiltViewModel(parentEntry)
                val user = preferencesManager.getUser()
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
                        addressToEdit = null
                        pendingAccountAddress = null
                        navController.navigate("saved_addresses") {
                            popUpTo("saved_addresses") { inclusive = true }
                        }
                    },
                    onChangeLocation = {
                        pendingAccountAddress = null
                        navController.navigate("address_search")
                    },
                    onBack = {
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

        // ════════════════════════════════════════════════════════════════════
        // BOOKING FLOW - NESTED NAVIGATION
        // ════════════════════════════════════════════════════════════════════
        navigation(
            startDestination = "booking_entry",
            route = "booking_flow"
        ) {
            // Entry Point
            composable("booking_entry") { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry("booking_flow")
                }
                val viewModel: BookingViewModel = hiltViewModel(parentEntry)
                val uiState by viewModel.uiState.collectAsState()

                LaunchedEffect(Unit) {
                    if (uiState.isBookAgain && uiState.pickupAddress != null && uiState.dropAddress != null) {
                        navController.navigate("booking_confirm") {
                            popUpTo("booking_entry") { inclusive = true }
                        }
                    } else if (isBookAgainFlow && orderForBookAgain != null) {
                        viewModel.prefillFromOrder(orderForBookAgain!!)
                        orderForBookAgain = null
                        isBookAgainFlow = false
                        navController.navigate("booking_confirm") {
                            popUpTo("booking_entry") { inclusive = true }
                        }
                    } else {
                        navController.navigate("location_search/pickup") {
                            popUpTo("booking_entry") { inclusive = true }
                        }
                    }
                }

                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AppColors.Primary)
                }
            }

            // Location Search
            composable(
                route = "location_search/{locationType}?isChange={isChange}",
                arguments = listOf(
                    navArgument("locationType") { type = NavType.StringType; defaultValue = "pickup" },
                    navArgument("isChange") { type = NavType.BoolType; defaultValue = false }
                )
            ) { backStackEntry ->
                val locationType = backStackEntry.arguments?.getString("locationType") ?: "pickup"
                val isChange = backStackEntry.arguments?.getBoolean("isChange") ?: false
                val parentEntry = remember(backStackEntry) { navController.getBackStackEntry("booking_flow") }
                val viewModel: BookingViewModel = hiltViewModel(parentEntry)
                val uiState by viewModel.uiState.collectAsState()
                val existingAddress = if (locationType == "pickup") uiState.pickupAddress else uiState.dropAddress

                LocationSearchScreen(
                    locationType = locationType,
                    onAddressSelected = { address ->
                        val mergedAddress = if (isChange && existingAddress != null) {
                            address.copy(
                                contactName = existingAddress.contactName,
                                contactPhone = existingAddress.contactPhone,
                                buildingDetails = existingAddress.buildingDetails,
                                landmark = existingAddress.landmark,
                                pincode = existingAddress.pincode
                            )
                        } else address
                        viewModel.setPendingAddress(mergedAddress)
                        navController.navigate("address_confirm/$locationType?isChange=$isChange")
                    },
                    onMapPicker = { latLng ->
                        navController.navigate("map_picker/${latLng.latitude}/${latLng.longitude}/$locationType?isChange=$isChange")
                    },
                    onBack = {
                        if (isChange) navController.popBackStack()
                        else if (locationType == "pickup") {
                            navController.navigate(Screen.Main.route) { popUpTo("booking_flow") { inclusive = true } }
                        } else navController.popBackStack()
                    }
                )
            }

            // Map Picker
            composable(
                route = "map_picker/{lat}/{lng}/{locationType}?isChange={isChange}",
                arguments = listOf(
                    navArgument("lat") { type = NavType.StringType },
                    navArgument("lng") { type = NavType.StringType },
                    navArgument("locationType") { type = NavType.StringType },
                    navArgument("isChange") { type = NavType.BoolType; defaultValue = false }
                )
            ) { backStackEntry ->
                val lat = backStackEntry.arguments?.getString("lat")?.toDoubleOrNull() ?: 19.0760
                val lng = backStackEntry.arguments?.getString("lng")?.toDoubleOrNull() ?: 72.8777
                val locationType = backStackEntry.arguments?.getString("locationType") ?: "pickup"
                val isChange = backStackEntry.arguments?.getBoolean("isChange") ?: false
                val parentEntry = remember(backStackEntry) { navController.getBackStackEntry("booking_flow") }
                val viewModel: BookingViewModel = hiltViewModel(parentEntry)
                val uiState by viewModel.uiState.collectAsState()
                val existingAddress = if (locationType == "pickup") uiState.pickupAddress else uiState.dropAddress

                MapPickerScreen(
                    initialLocation = LatLng(lat, lng),
                    onLocationSelected = { address ->
                        val mergedAddress = if (isChange && existingAddress != null) {
                            address.copy(
                                contactName = existingAddress.contactName,
                                contactPhone = existingAddress.contactPhone,
                                buildingDetails = existingAddress.buildingDetails,
                                landmark = existingAddress.landmark,
                                pincode = existingAddress.pincode
                            )
                        } else address
                        viewModel.setPendingAddress(mergedAddress)
                        navController.navigate("address_confirm/$locationType?isChange=$isChange") {
                            popUpTo("map_picker/{lat}/{lng}/{locationType}?isChange={isChange}") { inclusive = true }
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            // Address Confirm
            composable(
                route = "address_confirm/{locationType}?isEdit={isEdit}&isChange={isChange}",
                arguments = listOf(
                    navArgument("locationType") { type = NavType.StringType },
                    navArgument("isEdit") { type = NavType.BoolType; defaultValue = false },
                    navArgument("isChange") { type = NavType.BoolType; defaultValue = false }
                )
            ) { backStackEntry ->
                val locationType = backStackEntry.arguments?.getString("locationType") ?: "pickup"
                val isEdit = backStackEntry.arguments?.getBoolean("isEdit") ?: false
                val isChange = backStackEntry.arguments?.getBoolean("isChange") ?: false
                val parentEntry = remember(backStackEntry) { navController.getBackStackEntry("booking_flow") }
                val viewModel: BookingViewModel = hiltViewModel(parentEntry)

                AddressConfirmationScreen(
                    address = null,
                    locationType = locationType,
                    isEditMode = isEdit || isChange,
                    viewModel = viewModel,
                    onConfirm = { confirmedAddress ->
                        if (locationType == "pickup") viewModel.setPickupAddress(confirmedAddress)
                        else viewModel.setDropAddress(confirmedAddress)
                        viewModel.clearPendingAddress()

                        if (isEdit || isChange) {
                            navController.navigate("booking_confirm") { popUpTo("booking_confirm") { inclusive = true } }
                        } else if (locationType == "pickup") {
                            navController.navigate("location_search/drop") { popUpTo("location_search/pickup") { inclusive = false } }
                        } else {
                            navController.navigate("booking_confirm") { popUpTo("location_search/pickup") { inclusive = false } }
                        }
                    },
                    onChangeLocation = {
                        viewModel.clearPendingAddress()
                        navController.navigate("location_search/$locationType?isChange=true")
                    },
                    onBack = {
                        viewModel.clearPendingAddress()
                        navController.popBackStack()
                    }
                )
            }

            // Booking Confirm
            composable("booking_confirm") {
                val parentEntry = remember(navController) { navController.getBackStackEntry("booking_flow") }
                val viewModel: BookingViewModel = hiltViewModel(parentEntry)
                val uiState by viewModel.uiState.collectAsState()

                BookingConfirmationScreen(
                    pickupAddress = uiState.pickupAddress,
                    dropAddress = uiState.dropAddress,
                    preSelectedVehicleId = uiState.preferredVehicleTypeId,
                    isPrefilledFromOrder = uiState.isBookAgain,
                    onVehicleSelected = { fareDetails ->
                        viewModel.selectFareDetails(fareDetails)
                        navController.navigate("review_booking")
                    },
                    onEditPickup = {
                        viewModel.clearPendingAddress()
                        navController.navigate("address_confirm/pickup?isEdit=true")
                    },
                    onEditDrop = {
                        viewModel.clearPendingAddress()
                        navController.navigate("address_confirm/drop?isEdit=true")
                    },
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
                            navController.navigate(Screen.Main.route) { popUpTo("booking_flow") { inclusive = true } }
                        } else navController.popBackStack()
                    },
                    viewModel = viewModel
                )
            }

            // Review Booking
            composable("review_booking") {
                val parentEntry = remember(navController) { navController.getBackStackEntry("booking_flow") }
                val viewModel: BookingViewModel = hiltViewModel(parentEntry)
                val uiState by viewModel.uiState.collectAsState()
                val selectedFareDetails by viewModel.selectedFareDetails.collectAsState()
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
                                navController.navigate(Screen.Main.route) { popUpTo("booking_flow") { inclusive = true } }
                            }
                        }
                    }
                }

                if (uiState.pickupAddress == null || uiState.dropAddress == null || selectedFareDetails == null) {
                    LaunchedEffect(Unit) { navController.popBackStack() }
                } else {
                    ReviewBookingScreen(
                        selectedFareDetails = selectedFareDetails!!,
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
                        RestrictedItemsBottomSheet(onDismiss = { showRestrictedItemsSheet = false })
                    }
                }
            }

            // Coupons
            composable("coupons") {
                val parentEntry = remember(navController) { navController.getBackStackEntry("booking_flow") }
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

            // Add GSTIN
            composable("add_gstin") {
                val parentEntry = remember(navController) { navController.getBackStackEntry("booking_flow") }
                val viewModel: BookingViewModel = hiltViewModel(parentEntry)

                AddGSTINScreen(
                    onSave = { gstin ->
                        viewModel.addGSTIN(gstin)
                        navController.popBackStack()
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            // ════════════════════════════════════════════════════════════════
            // SEARCHING RIDER SCREEN
            // ════════════════════════════════════════════════════════════════
            composable(
                route = "searching_rider/{bookingId}",
                arguments = listOf(navArgument("bookingId") { type = NavType.StringType })
            ) { backStackEntry ->
                val bookingId = backStackEntry.arguments?.getString("bookingId") ?: ""
                val parentEntry = remember(navController) { navController.getBackStackEntry("booking_flow") }
                val bookingViewModel: BookingViewModel = hiltViewModel(parentEntry)
                val riderTrackingViewModel: RiderTrackingViewModel = hiltViewModel(parentEntry)

                val uiState by bookingViewModel.uiState.collectAsState()
                val selectedFareDetails by bookingViewModel.selectedFareDetails.collectAsState()

                LaunchedEffect(Unit) {
                    riderTrackingViewModel.navigationEvent.collect { event ->
                        when (event) {
                            is RiderTrackingNavigationEvent.RiderAssigned -> {
                                navController.navigate("rider_found/${event.bookingId}") {
                                    popUpTo("searching_rider/{bookingId}") { inclusive = true }
                                }
                            }
                            is RiderTrackingNavigationEvent.NoRiderAvailable -> {
                                // Stay on this screen - UI will show retry option
                            }
                            is RiderTrackingNavigationEvent.NavigateToHome -> {
                                navController.navigate(Screen.Main.route) {
                                    popUpTo("booking_flow") { inclusive = true }
                                }
                            }
                            is RiderTrackingNavigationEvent.BookingCancelled -> {
                                navController.navigate(Screen.Main.route) {
                                    popUpTo("booking_flow") { inclusive = true }
                                }
                            }
                            else -> {}
                        }
                    }
                }

                LaunchedEffect(Unit) {
                    bookingViewModel.navigationEvent.collect { event ->
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

                if (uiState.pickupAddress != null && uiState.dropAddress != null && selectedFareDetails != null) {
                    SearchingRiderScreen(
                        bookingId = bookingId,
                        pickupAddress = uiState.pickupAddress!!,
                        dropAddress = uiState.dropAddress!!,
                        selectedFareDetails = selectedFareDetails!!,
                        fare = uiState.finalFare,
                        onRiderFound = { },
                        onContactSupport = {
                            val intent = Intent(Intent.ACTION_DIAL).apply {
                                data = Uri.parse("tel:+919876543210")
                            }
                            context.startActivity(intent)
                        },
                        onViewDetails = { },
                        onCancelBooking = { reason ->
                            riderTrackingViewModel.cancelBooking(reason)
                        },
                        bookingViewModel = bookingViewModel,
                        riderTrackingViewModel = riderTrackingViewModel
                    )
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AppColors.Primary)
                    }
                }
            }

            // ════════════════════════════════════════════════════════════════
            // RIDER FOUND SCREEN
            // ════════════════════════════════════════════════════════════════
            composable(
                route = "rider_found/{bookingId}",
                arguments = listOf(navArgument("bookingId") { type = NavType.StringType })
            ) { backStackEntry ->
                val bookingId = backStackEntry.arguments?.getString("bookingId") ?: ""
                val parentEntry = remember(navController) { navController.getBackStackEntry("booking_flow") }
                val bookingViewModel: BookingViewModel = hiltViewModel(parentEntry)
                val riderTrackingViewModel: RiderTrackingViewModel = hiltViewModel(parentEntry)

                val uiState by bookingViewModel.uiState.collectAsState()

                LaunchedEffect(Unit) {
                    riderTrackingViewModel.navigationEvent.collect { event ->
                        when (event) {
                            is RiderTrackingNavigationEvent.RiderArrived -> {
                                // Stay on RiderFoundScreen - UI updates via state
                            }
                            is RiderTrackingNavigationEvent.ParcelPickedUp -> {
                                // Stay on RiderFoundScreen - map switches to delivery route
                            }
                            is RiderTrackingNavigationEvent.Delivered -> {
                                // Rating dialog shown in RiderFoundScreen (for cash)
                                // or payment screen navigated to (for online)
                            }
                            is RiderTrackingNavigationEvent.ShowPaymentScreen -> {
                                val roundedFare = event.roundedFare.toString()
                                val waitingChargeStr = event.waitingCharge.toString()
                                val discount = event.discount.toString()
                                navController.navigate(
                                    "post_delivery_payment/${event.bookingId}/$roundedFare/$waitingChargeStr/$discount/${event.driverName}/${event.paymentMethod}"
                                )
                            }
                            is RiderTrackingNavigationEvent.NavigateToHome -> {
                                navController.navigate(Screen.Main.route) {
                                    popUpTo("booking_flow") { inclusive = true }
                                }
                            }
                            is RiderTrackingNavigationEvent.BookingCancelled -> {
                                navController.navigate(Screen.Main.route) {
                                    popUpTo("booking_flow") { inclusive = true }
                                }
                            }
                            is RiderTrackingNavigationEvent.DriverCancelledRetrySearch -> {
                                navController.navigate("searching_rider/${bookingId}") {
                                    popUpTo("rider_found/{bookingId}") { inclusive = true }
                                }
                            }
                            else -> {}
                        }
                    }
                }

                LaunchedEffect(Unit) {
                    bookingViewModel.navigationEvent.collect { event ->
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

                DisposableEffect(Unit) {
                    onDispose { }
                }

                if (uiState.pickupAddress != null && uiState.dropAddress != null) {
                    RiderFoundScreen(
                        bookingId = bookingId,
                        pickupAddress = uiState.pickupAddress!!,
                        dropAddress = uiState.dropAddress!!,
                        fare = uiState.finalFare,
                        onCancelBooking = { reason ->
                            riderTrackingViewModel.cancelBooking(reason)
                        },
                        onContactSupport = {
                            val intent = Intent(Intent.ACTION_DIAL).apply {
                                data = Uri.parse("tel:+919876543210")
                            }
                            context.startActivity(intent)
                        },
                        viewModel = riderTrackingViewModel
                    )
                } else {
                    LaunchedEffect(Unit) { navController.popBackStack() }
                }
            }

            // ════════════════════════════════════════════════════════════════
            // POST-DELIVERY PAYMENT SCREEN (booking_flow)
            // ════════════════════════════════════════════════════════════════
            composable(
                route = "post_delivery_payment/{bookingId}/{roundedFare}/{waitingCharge}/{discount}/{driverName}/{paymentMethod}",
                arguments = listOf(
                    navArgument("bookingId") { type = NavType.StringType },
                    navArgument("roundedFare") { type = NavType.StringType },          // ✅ StringType for Double
                    navArgument("waitingCharge") { type = NavType.StringType },     // ✅ StringType for Double
                    navArgument("discount") { type = NavType.StringType },     // ✅ StringType for Double
                    navArgument("driverName") { type = NavType.StringType },
                    navArgument("paymentMethod") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val bookingId = backStackEntry.arguments?.getString("bookingId") ?: ""
                val roundedFare = backStackEntry.arguments?.getString("roundedFare")?.toDoubleOrNull() ?: 0.0
                val waitingCharge = backStackEntry.arguments?.getString("waitingCharge")?.toDoubleOrNull() ?: 0.0
                val discount = backStackEntry.arguments?.getString("discount")?.toDoubleOrNull() ?: 0.0
                val driverName = backStackEntry.arguments?.getString("driverName") ?: ""
                val paymentMethod = backStackEntry.arguments?.getString("paymentMethod") ?: "cash"

                PostDeliveryPaymentScreen(
                    bookingId = bookingId,
                    roundedFare = roundedFare,
                    waitingCharge = waitingCharge,
                    discount = discount,
                    driverName = driverName,
                    paymentMethod = paymentMethod,
                    onPaymentComplete = { /* navigate to completion */ },
                    onPaymentSkipped = { /* handle cash flow */ }
                )
            }
        } // ✅ CLOSES booking_flow navigation block

        // ════════════════════════════════════════════════════════════════════
        // ACTIVE BOOKING FLOW (Resume) — sibling of booking_flow
        // ════════════════════════════════════════════════════════════════════
        navigation(
            startDestination = "active_searching_rider",
            route = "active_booking_flow"
        ) {
            // ═══════════════════════════════════════════════════════════════════
            // SEARCHING / RESUMING SCREEN
            // ═══════════════════════════════════════════════════════════════════
            composable("active_searching_rider") {
                val activeBooking = activeBookingToResume

                if (activeBooking != null) {
                    val bookingViewModel: BookingViewModel = hiltViewModel()
                    val riderTrackingViewModel: RiderTrackingViewModel = hiltViewModel()

                    LaunchedEffect(Unit) {
                        riderTrackingViewModel.navigationEvent.collect { event ->
                            when (event) {
                                is RiderTrackingNavigationEvent.RiderAssigned -> {
                                    navController.navigate("active_rider_found/${event.bookingId}") {
                                        popUpTo("active_searching_rider") { inclusive = true }
                                    }
                                }
                                is RiderTrackingNavigationEvent.NoRiderAvailable -> {
                                    // Stay on searching screen — UI shows retry
                                }
                                is RiderTrackingNavigationEvent.BookingCancelled -> {
                                    navController.navigate(Screen.Main.route) {
                                        popUpTo("active_booking_flow") { inclusive = true }
                                    }
                                }
                                is RiderTrackingNavigationEvent.DriverCancelledRetrySearch -> {
                                    // Stay on searching screen — auto retry
                                }
                                is RiderTrackingNavigationEvent.NavigateToHome -> {
                                    navController.navigate(Screen.Main.route) {
                                        popUpTo("active_booking_flow") { inclusive = true }
                                    }
                                }
                                else -> {}
                            }
                        }
                    }

                    LaunchedEffect(Unit) {
                        bookingViewModel.navigationEvent.collect { event ->
                            when (event) {
                                is BookingNavigationEvent.NavigateToHome -> {
                                    navController.navigate(Screen.Main.route) {
                                        popUpTo("active_booking_flow") { inclusive = true }
                                    }
                                }
                                else -> {}
                            }
                        }
                    }

                    SearchingRiderScreen(
                        bookingId = activeBooking.bookingId,
                        pickupAddress = activeBooking.pickupAddress,
                        dropAddress = activeBooking.dropAddress,
                        selectedFareDetails = activeBooking.fareDetails,
                        fare = activeBooking.fare,
                        onRiderFound = { },
                        onContactSupport = {
                            val intent = Intent(Intent.ACTION_DIAL).apply {
                                data = Uri.parse("tel:+919876543210")
                            }
                            context.startActivity(intent)
                        },
                        onViewDetails = { },
                        onCancelBooking = { reason ->
                            riderTrackingViewModel.cancelBooking(reason)
                        },
                        bookingViewModel = bookingViewModel,
                        riderTrackingViewModel = riderTrackingViewModel
                    )
                } else {
                    LaunchedEffect(Unit) { navController.popBackStack() }
                }
            }

            // ═══════════════════════════════════════════════════════════════════
            // RIDER FOUND SCREEN (active booking resume)
            // ═══════════════════════════════════════════════════════════════════
            composable(
                route = "active_rider_found/{bookingId}",
                arguments = listOf(navArgument("bookingId") { type = NavType.StringType })
            ) { backStackEntry ->
                val bookingId = backStackEntry.arguments?.getString("bookingId") ?: ""
                val activeBooking = activeBookingToResume

                if (activeBooking != null) {
                    val parentEntry = remember(navController) {
                        navController.getBackStackEntry("active_booking_flow")
                    }
                    val bookingViewModel: BookingViewModel = hiltViewModel(parentEntry)
                    val riderTrackingViewModel: RiderTrackingViewModel = hiltViewModel(parentEntry)

                    LaunchedEffect(Unit) {
                        riderTrackingViewModel.navigationEvent.collect { event ->
                            when (event) {
                                is RiderTrackingNavigationEvent.BookingCancelled -> {
                                    navController.navigate(Screen.Main.route) {
                                        popUpTo("active_booking_flow") { inclusive = true }
                                    }
                                }
                                is RiderTrackingNavigationEvent.DriverCancelledRetrySearch -> {
                                    navController.navigate("active_searching_rider") {
                                        popUpTo("active_booking_flow") { inclusive = false }
                                    }
                                }
                                is RiderTrackingNavigationEvent.Delivered -> {
                                    // Stay on screen — rating dialog will show (for cash)
                                    // or payment screen navigated (for online)
                                }
                                is RiderTrackingNavigationEvent.ShowPaymentScreen -> {

                                    val roundedFare = event.roundedFare.toString()           // ✅ Double → String
                                    val waitingChargeStr = event.waitingCharge.toString() // ✅ Double → String
                                    val discount = event.discount.toString()
                                    navController.navigate(
                                        "active_post_delivery_payment/${event.bookingId}/$roundedFare/$waitingChargeStr/$discount/${Uri.encode(event.driverName)}/${event.paymentMethod}"
                                    )
                                }
                                is RiderTrackingNavigationEvent.NavigateToHome -> {
                                    navController.navigate(Screen.Main.route) {
                                        popUpTo("active_booking_flow") { inclusive = true }
                                    }
                                }
                                else -> {
                                    // RiderArrived, ParcelPickedUp, RiderEnroute
                                    // all handled within RiderFoundScreen UI itself
                                }
                            }
                        }
                    }

                    LaunchedEffect(Unit) {
                        bookingViewModel.navigationEvent.collect { event ->
                            when (event) {
                                is BookingNavigationEvent.NavigateToHome -> {
                                    navController.navigate(Screen.Main.route) {
                                        popUpTo("active_booking_flow") { inclusive = true }
                                    }
                                }
                                else -> {}
                            }
                        }
                    }

                    RiderFoundScreen(
                        bookingId = bookingId,
                        pickupAddress = activeBooking.pickupAddress,
                        dropAddress = activeBooking.dropAddress,
                        fare = activeBooking.fare,
                        onCancelBooking = { reason ->
                            riderTrackingViewModel.cancelBooking(reason)
                        },
                        onContactSupport = {
                            val intent = Intent(Intent.ACTION_DIAL).apply {
                                data = Uri.parse("tel:+919876543210")
                            }
                            context.startActivity(intent)
                        },
                        viewModel = riderTrackingViewModel
                    )
                } else {
                    LaunchedEffect(Unit) { navController.popBackStack() }
                }
            }

            // ═══════════════════════════════════════════════════════════════════
            // POST-DELIVERY PAYMENT SCREEN (active_booking_flow)
            // ═══════════════════════════════════════════════════════════════════
            composable(
                route = "active_post_delivery_payment/{bookingId}/{baseFare}/{waitingCharge}/{discount}/{driverName}/{paymentMethod}",
                arguments = listOf(
                    navArgument("bookingId") { type = NavType.StringType },
                    navArgument("roundedFare") { type = NavType.StringType },          // ✅ IntType → StringType
                    navArgument("waitingCharge") { type = NavType.StringType },     // ✅ IntType → StringType
                    navArgument("discount") { type = NavType.StringType },     // ✅ IntType → StringType
                    navArgument("driverName") { type = NavType.StringType },
                    navArgument("paymentMethod") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val bookingId = backStackEntry.arguments?.getString("bookingId") ?: ""
                val roundedFare = backStackEntry.arguments?.getString("roundedFare")?.toDoubleOrNull() ?: 0.0         // ✅ Fixed
                val waitingCharge = backStackEntry.arguments?.getString("waitingCharge")?.toDoubleOrNull() ?: 0.0  // ✅ Fixed
                val discount = backStackEntry.arguments?.getString("discount")?.toDoubleOrNull() ?: 0.0  // ✅ Fixed
                val driverName = Uri.decode(backStackEntry.arguments?.getString("driverName") ?: "Driver")
                val paymentMethod = backStackEntry.arguments?.getString("paymentMethod") ?: "cash"

                val parentEntry = remember(navController) {
                    navController.getBackStackEntry("active_booking_flow")
                }
                val riderTrackingViewModel: RiderTrackingViewModel = hiltViewModel(parentEntry)
                val activity = context as? MainActivity

                // Listen for NavigateToHome (after rating completes)
                LaunchedEffect(Unit) {
                    riderTrackingViewModel.navigationEvent.collect { event ->
                        when (event) {
                            is RiderTrackingNavigationEvent.NavigateToHome -> {
                                navController.navigate(Screen.Main.route) {
                                    popUpTo("active_booking_flow") { inclusive = true }
                                }
                            }
                            else -> {}
                        }
                    }
                }

                activity?.let {
                    PostDeliveryPaymentScreen(
                        bookingId = bookingId,
                        roundedFare = roundedFare,
                        waitingCharge = waitingCharge,
                        discount = discount,
                        driverName = driverName,
                        paymentMethod = paymentMethod,
                        onPaymentComplete = {
                            riderTrackingViewModel.onPaymentCompleted()
                            navController.popBackStack() // Back to RiderFoundScreen where rating shows
                        },
                        onPaymentSkipped = {
                            riderTrackingViewModel.onCashPaymentConfirmed()
                            navController.popBackStack()
                        },
                        paymentViewModel = it.paymentViewModel
                    )
                }
            }
        } // closes active_booking_flow
    } // closes NavHost
}