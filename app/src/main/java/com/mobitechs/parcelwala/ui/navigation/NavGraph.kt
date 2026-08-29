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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.mobitechs.parcelwala.ui.booking2.sendParcelFlow
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.google.android.gms.maps.model.LatLng
import com.mobitechs.parcelwala.MainActivity
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
import com.mobitechs.parcelwala.ui.screens.booking.AddressConfirmationScreen
import com.mobitechs.parcelwala.ui.screens.booking.CouponScreen
import com.mobitechs.parcelwala.ui.screens.booking.MapPickerScreen
import com.mobitechs.parcelwala.ui.screens.booking.RiderFoundScreen
import com.mobitechs.parcelwala.ui.screens.booking.SearchingRiderScreen
import com.mobitechs.parcelwala.ui.screens.main.MainScreen
import com.mobitechs.parcelwala.ui.screens.orders.OrderDetailsScreen
import com.mobitechs.parcelwala.ui.screens.payments.PostDeliveryPaymentScreen
import com.mobitechs.parcelwala.ui.screens.splash.SplashScreen
import com.mobitechs.parcelwala.ui.theme.AppColors
import com.mobitechs.parcelwala.ui.viewmodel.AccountViewModel
import com.mobitechs.parcelwala.ui.viewmodel.BookingNavigationEvent
import com.mobitechs.parcelwala.ui.viewmodel.BookingViewModel
import com.mobitechs.parcelwala.ui.viewmodel.RiderTrackingNavigationEvent
import com.mobitechs.parcelwala.ui.viewmodel.RiderTrackingViewModel
import com.mobitechs.parcelwala.utils.Constants

// ═══════════════════════════════════════════════════════════════════════════
// POST-DELIVERY PAYMENT ROUTE — shared by booking_flow & active_booking_flow
// ═══════════════════════════════════════════════════════════════════════════

private const val PAYMENT_ROUTE_PATTERN =
    "{bookingId}/{roundedFare}/{waitingCharge}/{discount}/{driverName}/{paymentMethod}"

private val paymentNavArguments = listOf(
    navArgument("bookingId") { type = NavType.StringType },
    navArgument("roundedFare") { type = NavType.StringType },
    navArgument("waitingCharge") { type = NavType.StringType },
    navArgument("discount") { type = NavType.StringType },
    navArgument("driverName") { type = NavType.StringType },
    navArgument("paymentMethod") { type = NavType.StringType }
)

private fun buildPaymentRoute(
    prefix: String,
    bookingId: String,
    roundedFare: Double,
    waitingCharge: Double,
    discount: Double,
    driverName: String,
    paymentMethod: String
): String {
    return "$prefix/$bookingId/$roundedFare/$waitingCharge/$discount/${Uri.encode(driverName)}/$paymentMethod"
}

@SuppressLint("UnrememberedGetBackStackEntry")
@Composable
fun NavGraph(
    navController: NavHostController,
    preferencesManager: PreferencesManager
) {
    val isLoggedIn = preferencesManager.isLoggedIn()
    val context = LocalContext.current
    val activity = context as? MainActivity

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
                navArgument("otp") { type = NavType.StringType; defaultValue = "null" }
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
                onNavigateBack = { navController.popBackStack() }
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
                    // Enter through the GRAPH, not straight at the picker.
                    //
                    // Navigating directly to a destination inside a nested graph
                    // makes Navigation put that graph's start destination
                    // (booking_entry) on the back stack underneath it. Pressing
                    // back from the picker would land on booking_entry, whose
                    // LaunchedEffect immediately navigates forward again — a back
                    // button that refuses to go back.
                    //
                    // booking_entry pops itself with popUpTo(inclusive) as it
                    // routes onward, so entering here leaves a clean stack.
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
                onNavigateToSavedAddresses = { navController.navigate("account_flow") },
                onNavigateToProfileDetails = { navController.navigate("profile_details") },
                onNavigateToLanguage = { navController.navigate(Screen.Language.route) },
                onNavigateToGSTDetails = { },
                currentRoute = "home"
            )
        }

        composable(Screen.Language.route) {
            com.mobitechs.parcelwala.ui.screens.account.LanguageSelectionScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable("profile_details") {
            ProfileDetailsScreen(onBack = { navController.popBackStack() })
        }

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
                        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber")))
                    },
                    onCallSupport = {
                        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse(Constants.SUPPORT_MOBILE_NO)))
                    }
                )
            } ?: run { LaunchedEffect(Unit) { navController.popBackStack() } }
        }

        // ============ ACCOUNT FLOW ============
        navigation(startDestination = "saved_addresses", route = "account_flow") {
            composable("saved_addresses") {
                val parentEntry = remember(navController) { navController.getBackStackEntry("account_flow") }
                val viewModel: AccountViewModel = hiltViewModel(parentEntry)
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                LaunchedEffect(uiState.addressSaveSuccess) {
                    if (uiState.addressSaveSuccess) {
                        viewModel.clearAddressSaveSuccess()
                        viewModel.loadSavedAddresses()
                    }
                }

                SavedAddressesScreen(
                    onBack = { navController.popBackStack() },
                    onAddAddress = { addressToEdit = null; navController.navigate("address_search") },
                    onEditAddress = { address -> addressToEdit = address; navController.navigate("account_address_confirm") },
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
                    onMapPicker = { latLng -> navController.navigate("address_map_picker/${latLng.latitude}/${latLng.longitude}") },
                    onBack = { pendingAccountAddress = null; navController.popBackStack() }
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
                val parentEntry = remember(navController) { navController.getBackStackEntry("account_flow") }
                val viewModel: AccountViewModel = hiltViewModel(parentEntry)
                val user = preferencesManager.getUser()
                val selectedAddress = pendingAccountAddress ?: addressToEdit

                // ITEM 2 — wait for the save to finish before navigating.
                //
                // The old code navigated in the same breath as calling save.
                // popUpTo("saved_addresses") { inclusive = true } tears down the
                // account_flow entry that owns this AccountViewModel, which
                // cancelled the in-flight POST and left the rebuilt list screen
                // fetching before the write had landed.
                val accountState by viewModel.uiState.collectAsStateWithLifecycle()

                // Both saveAddress() and updateAddress() set addressSaveSuccess.
                LaunchedEffect(accountState.addressSaveSuccess) {
                    if (accountState.addressSaveSuccess) {
                        addressToEdit = null
                        pendingAccountAddress = null
                        navController.popBackStack("saved_addresses", inclusive = false)
                    }
                }

                AddressConfirmationScreen(
                    address = selectedAddress,
                    locationType = "save",
                    onConfirm = { confirmedAddress ->
                        // Navigation now happens in the LaunchedEffect above,
                        // once the server has confirmed the write.
                        if (addressToEdit != null) viewModel.updateAddress(confirmedAddress)
                        else viewModel.saveAddress(confirmedAddress)
                    },
                    onChangeLocation = { pendingAccountAddress = null; navController.navigate("address_search") },
                    onBack = { addressToEdit = null; pendingAccountAddress = null; navController.popBackStack() },
                    isEditMode = addressToEdit != null,
                    userPhoneNumber = user?.phoneNumber,
                    userName = user?.fullName,
                    showSaveLocationBadge = true
                )
            }
        }

        // ════════════════════════════════════════════════════════════════════
        // BOOKING FLOW
        // ════════════════════════════════════════════════════════════════════
        navigation(startDestination = "booking_entry", route = "booking_flow") {

            // ════════════════════════════════════════════════════════════════
            // BOOKING FLOW v2  ("Send a parcel") — four steps, price at tap two
            // ════════════════════════════════════════════════════════════════
            //
            // Registered INSIDE booking_flow on purpose: coupons and
            // searching_rider both resolve their ViewModel via
            // getBackStackEntry("booking_flow"). A sibling graph would hand them
            // a fresh BookingViewModel with no pickup, drop or fare — and
            // searching_rider renders nothing at all in that state, so the
            // customer would land on a blank screen with a live booking.
            //
            // Sharing the graph means one BookingViewModel across old screens,
            // new screens, coupons and tracking.
            sendParcelFlow(navController)


            composable("booking_entry") { backStackEntry ->
                val parentEntry = remember(backStackEntry) { navController.getBackStackEntry("booking_flow") }
                val viewModel: BookingViewModel = hiltViewModel(parentEntry)
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                LaunchedEffect(Unit) {
                    // Book Again now lands in the v2 flow like every other entry
                    // point. Both addresses are already prefilled from the order,
                    // so it goes STRAIGHT to the fare sheet — a repeat delivery
                    // becomes one tap to a price, which is the whole reason
                    // "Send again" exists.
                    if (uiState.isBookAgain && uiState.pickupAddress != null && uiState.dropAddress != null) {
                        navController.navigate("sendparcel_fare") { popUpTo("booking_entry") { inclusive = true } }
                    } else if (isBookAgainFlow && orderForBookAgain != null) {
                        viewModel.prefillFromOrder(orderForBookAgain!!)
                        orderForBookAgain = null; isBookAgainFlow = false
                        navController.navigate("sendparcel_fare") { popUpTo("booking_entry") { inclusive = true } }
                    } else {
                        navController.navigate("sendparcel_destination/DROP") { popUpTo("booking_entry") { inclusive = true } }
                    }
                }
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AppColors.Primary)
                }
            }


            composable("coupons") {
                val parentEntry = remember(navController) { navController.getBackStackEntry("booking_flow") }
                val viewModel: BookingViewModel = hiltViewModel(parentEntry)
                CouponScreen(
                    onBack = { navController.popBackStack() },
                    onCouponApplied = { couponCode -> viewModel.applyCoupon(couponCode); navController.popBackStack() },
                    viewModel = viewModel
                )
            }


            // ════════════════════════════════════════════════════════════════
            // SEARCHING RIDER
            // ════════════════════════════════════════════════════════════════
            composable(
                route = "searching_rider/{bookingId}",
                arguments = listOf(navArgument("bookingId") { type = NavType.StringType })
            ) { backStackEntry ->
                val bookingId = backStackEntry.arguments?.getString("bookingId") ?: ""
                val parentEntry = remember(navController) { navController.getBackStackEntry("booking_flow") }
                val bookingViewModel: BookingViewModel = hiltViewModel(parentEntry)
                val riderTrackingViewModel: RiderTrackingViewModel = hiltViewModel(parentEntry)
                val uiState by bookingViewModel.uiState.collectAsStateWithLifecycle()
                val selectedFareDetails by bookingViewModel.selectedFareDetails.collectAsStateWithLifecycle()

                // "Search again" creates a BRAND NEW booking, so this screen has
                // to re-point itself at the new id. Both listeners that normally
                // catch NavigateToSearchingRider live on screens that were popped
                // on the way here, so without this the retry silently created a
                // booking the customer never saw.
                LaunchedEffect(Unit) {
                    bookingViewModel.navigationEvent.collect { event ->
                        if (event is BookingNavigationEvent.NavigateToSearchingRider) {
                            navController.navigate("searching_rider/${event.bookingId}") {
                                popUpTo("searching_rider/{bookingId}") { inclusive = true }
                            }
                        }
                    }
                }

                LaunchedEffect(Unit) {
                    riderTrackingViewModel.navigationEvent.collect { event ->
                        when (event) {
                            is RiderTrackingNavigationEvent.RiderAssigned -> {
                                navController.navigate("rider_found/${event.bookingId}") {
                                    popUpTo("searching_rider/{bookingId}") { inclusive = true }
                                }
                            }
                            is RiderTrackingNavigationEvent.NoRiderAvailable -> { /* Stay */ }
                            is RiderTrackingNavigationEvent.NavigateToHome,
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
                                navController.navigate(Screen.Main.route) { popUpTo("booking_flow") { inclusive = true } }
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
                            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:+919876543210")))
                        },
                        onViewDetails = { },
                        onCancelBooking = { reason -> riderTrackingViewModel.cancelBooking(reason) },
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
            // RIDER FOUND — rating dialog is shown here after DELIVERED
            // ════════════════════════════════════════════════════════════════
            composable(
                route = "rider_found/{bookingId}",
                arguments = listOf(navArgument("bookingId") { type = NavType.StringType })
            ) { backStackEntry ->
                val bookingId = backStackEntry.arguments?.getString("bookingId") ?: ""
                val parentEntry = remember(navController) { navController.getBackStackEntry("booking_flow") }
                val bookingViewModel: BookingViewModel = hiltViewModel(parentEntry)
                val riderTrackingViewModel: RiderTrackingViewModel = hiltViewModel(parentEntry)
                val uiState by bookingViewModel.uiState.collectAsStateWithLifecycle()

                LaunchedEffect(Unit) {
                    riderTrackingViewModel.navigationEvent.collect { event ->
                        when (event) {
                            // REMOVED - payment is no longer a navigation destination.
                            // ARRIVED_DELIVERY used to push a full-screen payment route,
                            // destroying the live map at the most anxious moment of the
                            // trip, then popping back for the rating dialog:
                            //   map -> payment -> map -> dialog
                            // RiderFoundScreen now raises payment as a sheet OVER the map,
                            // driven by paymentState.showPaymentScreen.
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
                                navController.navigate("searching_rider/$bookingId") {
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
                                navController.navigate(Screen.Main.route) { popUpTo("booking_flow") { inclusive = true } }
                            }
                            else -> {}
                        }
                    }
                }

                DisposableEffect(Unit) { onDispose { } }

                if (uiState.pickupAddress != null && uiState.dropAddress != null) {
                    RiderFoundScreen(
                        bookingId = bookingId,
                        pickupAddress = uiState.pickupAddress!!,
                        dropAddress = uiState.dropAddress!!,
                        fare = uiState.finalFare,
                        onCancelBooking = { reason -> riderTrackingViewModel.cancelBooking(reason) },
                        onContactSupport = {
                            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:+919876543210")))
                        },
                        viewModel = riderTrackingViewModel
                    )
                } else {
                    LaunchedEffect(Unit) { navController.popBackStack() }
                }
            }

            // ════════════════════════════════════════════════════════════════
            // POST-DELIVERY PAYMENT (booking_flow)
            //
            // ✅ KEY FIX: Only pop back when showRatingDialog = true.
            //    DO NOT pop on isPaymentCompleted — that fires on PAYMENT_SUCCESS
            //    but DELIVERED hasn't arrived yet. Popping here kills the screen
            //    before the rating dialog can show.
            //    We pop back to RiderFoundScreen which observes ratingState
            //    and shows the rating dialog when DELIVERED fires.
            // ════════════════════════════════════════════════════════════════
            composable(
                route = "post_delivery_payment/$PAYMENT_ROUTE_PATTERN",
                arguments = paymentNavArguments
            ) { backStackEntry ->
                val bookingId = backStackEntry.arguments?.getString("bookingId") ?: ""
                val roundedFare = backStackEntry.arguments?.getString("roundedFare")?.toDoubleOrNull() ?: 0.0
                val waitingCharge = backStackEntry.arguments?.getString("waitingCharge")?.toDoubleOrNull() ?: 0.0
                val discount = backStackEntry.arguments?.getString("discount")?.toDoubleOrNull() ?: 0.0
                val driverName = Uri.decode(backStackEntry.arguments?.getString("driverName") ?: "Driver")
                val paymentMethod = backStackEntry.arguments?.getString("paymentMethod") ?: "cash"

                val parentEntry = remember(navController) { navController.getBackStackEntry("booking_flow") }
                val riderTrackingViewModel: RiderTrackingViewModel = hiltViewModel(parentEntry)

                val ratingState by riderTrackingViewModel.ratingState.collectAsStateWithLifecycle()

                // ✅ ONLY pop back when DELIVERED has fired and rating is ready.
                //    isPaymentCompleted alone must NOT trigger navigation — DELIVERED hasn't arrived yet.
                LaunchedEffect(ratingState.showRatingDialog) {
                    if (ratingState.showRatingDialog) {
                        // Pop back to RiderFoundScreen — it will show the rating dialog
                        navController.popBackStack()
                    }
                }

                // ✅ NavigateToHome fires from ViewModel after rating completes
                LaunchedEffect(Unit) {
                    riderTrackingViewModel.navigationEvent.collect { event ->
                        when (event) {
                            is RiderTrackingNavigationEvent.NavigateToHome -> {
                                navController.navigate(Screen.Main.route) {
                                    popUpTo("booking_flow") { inclusive = true }
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
                            // Customer paid online via Razorpay — send payment_success to SignalR
                            // Do NOT navigate yet — wait for DELIVERED → showRatingDialog
                            riderTrackingViewModel.onPaymentCompleted()
                        },
                        onPaymentSkipped = {
                            // Customer paid cash — send payment_success to SignalR
                            // Do NOT navigate yet — wait for DELIVERED → showRatingDialog
                            riderTrackingViewModel.onCashPaymentConfirmed()
                        },
                        paymentViewModel = it.paymentViewModel
                    )
                }
            }
        } // closes booking_flow

        // ════════════════════════════════════════════════════════════════════
        // ACTIVE BOOKING FLOW (Resume)
        // ════════════════════════════════════════════════════════════════════
        navigation(startDestination = "active_searching_rider", route = "active_booking_flow") {

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
                                is RiderTrackingNavigationEvent.NoRiderAvailable -> { /* Stay */ }
                                is RiderTrackingNavigationEvent.BookingCancelled,
                                is RiderTrackingNavigationEvent.NavigateToHome -> {
                                    navController.navigate(Screen.Main.route) {
                                        popUpTo("active_booking_flow") { inclusive = true }
                                    }
                                }
                                is RiderTrackingNavigationEvent.DriverCancelledRetrySearch -> { /* Stay */ }
                                else -> {}
                            }
                        }
                    }

                    LaunchedEffect(Unit) {
                        bookingViewModel.navigationEvent.collect { event ->
                            when (event) {
                                is BookingNavigationEvent.NavigateToHome -> {
                                    navController.navigate(Screen.Main.route) { popUpTo("active_booking_flow") { inclusive = true } }
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
                            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:+919876543210")))
                        },
                        onViewDetails = { },
                        onCancelBooking = { reason -> riderTrackingViewModel.cancelBooking(reason) },
                        bookingViewModel = bookingViewModel,
                        riderTrackingViewModel = riderTrackingViewModel
                    )
                } else {
                    LaunchedEffect(Unit) { navController.popBackStack() }
                }
            }

            // ═══════════════════════════════════════════════════════════════════
            // ACTIVE RIDER FOUND — rating dialog shown here after DELIVERED
            // ═══════════════════════════════════════════════════════════════════
            composable(
                route = "active_rider_found/{bookingId}",
                arguments = listOf(navArgument("bookingId") { type = NavType.StringType })
            ) { backStackEntry ->
                val bookingId = backStackEntry.arguments?.getString("bookingId") ?: ""
                val activeBooking = activeBookingToResume

                if (activeBooking != null) {
                    val parentEntry = remember(navController) { navController.getBackStackEntry("active_booking_flow") }
                    val bookingViewModel: BookingViewModel = hiltViewModel(parentEntry)
                    val riderTrackingViewModel: RiderTrackingViewModel = hiltViewModel(parentEntry)

                    LaunchedEffect(Unit) {
                        riderTrackingViewModel.navigationEvent.collect { event ->
                            when (event) {
                            // REMOVED - payment is no longer a navigation destination.
                            // ARRIVED_DELIVERY used to push a full-screen payment route,
                            // destroying the live map at the most anxious moment of the
                            // trip, then popping back for the rating dialog:
                            //   map -> payment -> map -> dialog
                            // RiderFoundScreen now raises payment as a sheet OVER the map,
                            // driven by paymentState.showPaymentScreen.
                                is RiderTrackingNavigationEvent.BookingCancelled -> {
                                    navController.navigate(Screen.Main.route) { popUpTo("active_booking_flow") { inclusive = true } }
                                }
                                is RiderTrackingNavigationEvent.DriverCancelledRetrySearch -> {
                                    navController.navigate("active_searching_rider") { popUpTo("active_booking_flow") { inclusive = false } }
                                }
                                is RiderTrackingNavigationEvent.NavigateToHome -> {
                                    navController.navigate(Screen.Main.route) { popUpTo("active_booking_flow") { inclusive = true } }
                                }
                                else -> {}
                            }
                        }
                    }

                    LaunchedEffect(Unit) {
                        bookingViewModel.navigationEvent.collect { event ->
                            when (event) {
                                is BookingNavigationEvent.NavigateToHome -> {
                                    navController.navigate(Screen.Main.route) { popUpTo("active_booking_flow") { inclusive = true } }
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
                        onCancelBooking = { reason -> riderTrackingViewModel.cancelBooking(reason) },
                        onContactSupport = {
                            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:+919876543210")))
                        },
                        viewModel = riderTrackingViewModel
                    )
                } else {
                    LaunchedEffect(Unit) { navController.popBackStack() }
                }
            }

            // ═══════════════════════════════════════════════════════════════════
            // POST-DELIVERY PAYMENT (active_booking_flow)
            //
            // ✅ Same fix as booking_flow payment screen:
            //    Only pop on showRatingDialog, never on isPaymentCompleted alone.
            // ═══════════════════════════════════════════════════════════════════
            composable(
                route = "active_post_delivery_payment/$PAYMENT_ROUTE_PATTERN",
                arguments = paymentNavArguments
            ) { backStackEntry ->
                val bookingId = backStackEntry.arguments?.getString("bookingId") ?: ""
                val roundedFare = backStackEntry.arguments?.getString("roundedFare")?.toDoubleOrNull() ?: 0.0
                val waitingCharge = backStackEntry.arguments?.getString("waitingCharge")?.toDoubleOrNull() ?: 0.0
                val discount = backStackEntry.arguments?.getString("discount")?.toDoubleOrNull() ?: 0.0
                val driverName = Uri.decode(backStackEntry.arguments?.getString("driverName") ?: "Driver")
                val paymentMethod = backStackEntry.arguments?.getString("paymentMethod") ?: "cash"

                val parentEntry = remember(navController) { navController.getBackStackEntry("active_booking_flow") }
                val riderTrackingViewModel: RiderTrackingViewModel = hiltViewModel(parentEntry)

                val ratingState by riderTrackingViewModel.ratingState.collectAsStateWithLifecycle()

                // ✅ ONLY pop when DELIVERED fires and rating dialog is ready
                LaunchedEffect(ratingState.showRatingDialog) {
                    if (ratingState.showRatingDialog) {
                        navController.popBackStack()
                    }
                }

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
                            // Send payment_success — do NOT pop, wait for DELIVERED
                            riderTrackingViewModel.onPaymentCompleted()
                        },
                        onPaymentSkipped = {
                            // Cash confirmed — do NOT pop, wait for DELIVERED
                            riderTrackingViewModel.onCashPaymentConfirmed()
                        },
                        paymentViewModel = it.paymentViewModel
                    )
                }
            }
        } // closes active_booking_flow
    } // closes NavHost
}