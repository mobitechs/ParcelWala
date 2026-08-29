package com.mobitechs.parcelwala.ui.booking2

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.google.android.gms.maps.model.LatLng
import com.mobitechs.parcelwala.data.model.request.SavedAddress
import com.mobitechs.parcelwala.ui.theme.AppColors
import com.mobitechs.parcelwala.utils.rememberContactPicker
import com.mobitechs.parcelwala.utils.rememberLocationPermissionState
import com.mobitechs.parcelwala.ui.viewmodel.AccountUiState
import com.mobitechs.parcelwala.ui.viewmodel.AccountViewModel
import com.mobitechs.parcelwala.ui.viewmodel.BookingNavigationEvent
import com.mobitechs.parcelwala.ui.viewmodel.BookingViewModel
import com.mobitechs.parcelwala.ui.viewmodel.LocationSearchViewModel
import com.mobitechs.parcelwala.ui.viewmodel.MapPickerViewModel

/**
 * ════════════════════════════════════════════════════════════════════════════
 * BOOKING FLOW v2 — ROUTES
 * ════════════════════════════════════════════════════════════════════════════
 *
 * Registered INSIDE the existing `booking_flow` graph, so `coupons` and
 * `searching_rider` — which both resolve their ViewModel via
 * `getBackStackEntry("booking_flow")` — receive the SAME `BookingViewModel`
 * that holds this flow's pickup, drop and selected fare. As a sibling graph
 * they would get a fresh one, and `searching_rider` renders nothing at all
 * without those three, so the customer would land on a blank screen with a live
 * booking on the server.
 *
 * ─────────────────────────────────────────────────────────────────────────
 * WHERE CONTACT DETAILS LIVE — and why not in `remember`
 * ─────────────────────────────────────────────────────────────────────────
 *
 * The first version held receiver details in a `remember { mutableStateOf(…) }`
 * inside the receiver route and only committed them on Continue. Tapping
 * "Change" on the sender block navigated away, the composable left composition,
 * the `remember` was destroyed, and on return the fields re-initialised from a
 * drop address that had never been written — so the customer's typing vanished.
 *
 * Contact details now live in `BookingViewModel.uiState` on the pickup and drop
 * addresses, written on every keystroke through `setContactDetails`. There is
 * no second copy to fall out of sync, and the state survives navigation,
 * rotation and process death for free.
 *
 * `setContactDetails` exists precisely because `setPickupAddress` and
 * `setDropAddress` both call `clearVehicleFares()`, which nulls the selected
 * fare. Stamping the receiver through `setDropAddress` wiped the price the
 * customer had already chosen — that is what made the confirm screen show a
 * blank vehicle, a total of ₹0, and a permanently disabled button.
 */
/**
 * Fallback map centre when we have neither a chosen pickup nor location
 * permission. Mumbai — the city this service operates in. Anything is better
 * than LatLng(0, 0), which renders as featureless ocean and reads as a broken
 * map rather than a missing permission.
 */
private val DEFAULT_MAP_CENTER = LatLng(19.0760, 72.8777)

/**
 * The picker's route PATTERN, for popUpTo anchors.
 *
 * It is the flow's landing screen now that the "Send a parcel" lobby is gone,
 * so it is what every forward navigation unwinds back to. Written once here
 * because popUpTo silently does nothing when the route string does not match a
 * back-stack entry exactly — a typo would not fail, it would just quietly leave
 * a growing stack behind.
 */
private const val PICKER_ROUTE = "sendparcel_destination/{slot}"

/**
 * Key the details screen uses to tell the picker which row to reopen for
 * address search when the customer taps "Change".
 */
private const val ACTIVATE_SLOT_KEY = "activate_slot"

fun NavGraphBuilder.sendParcelFlow(navController: NavHostController) {

    // ═══════════════════════════════════════════════════════════════════════
    // 1. LOCATION PICKER — the flow's landing screen
    // ═══════════════════════════════════════════════════════════════════════
    //
    // WHAT HAPPENED TO THE "SEND A PARCEL" HOME SCREEN
    //
    // It was a lobby. It showed the pickup as a one-line label, a "Where to
    // deliver?" row and a couple of shortcut chips — and every one of those
    // controls did the same thing: open this screen. A whole screen and
    // navigation transition whose only purpose was to get to the next screen.
    //
    // The picker now IS the landing, so opening "Send a parcel" puts the
    // customer straight on the two fields they came to fill.
    //
    // The `{slot}` argument is only the STARTING focus. Both ends are visible
    // at once and the customer moves between them without navigating, so one
    // visit can set, correct and swap both.
    composable(
        route = "sendparcel_destination/{slot}",
        arguments = listOf(navArgument("slot") { type = NavType.StringType })
    ) { entry ->
        val parent = remember(entry) { navController.getBackStackEntry("booking_flow") }
        val booking: BookingViewModel = hiltViewModel(parent)
        val account: AccountViewModel = hiltViewModel(parent)
        // Scoped to the graph, not this entry: the details screen can send the
        // customer back here, and a fresh ViewModel would drop the resolved
        // current location and re-run the GPS lookup every time.
        val locationVm: LocationSearchViewModel = hiltViewModel(parent)

        val state by locationVm.uiState.collectAsStateWithLifecycle()
        val bookingState by booking.uiState.collectAsStateWithLifecycle()
        val accountState by account.uiState.collectAsStateWithLifecycle()
        val savedAddresses by account.savedAddresses.collectAsStateWithLifecycle()

        // Which row is in SEARCH mode, or null when neither is.
        //
        // Nullable on purpose. A filled row must render as a summary the
        // customer can read and tap through to the details screen — not as an
        // empty search box that has silently discarded the address it holds.
        // Returning here from the fare step with both ends set is the case that
        // makes this necessary: neither row is being edited, so neither should
        // look like it is.
        //
        // Seeded to the route's slot only if that slot is actually empty,
        // otherwise to whichever end still needs filling.
        var activeSlot by rememberSaveable {
            val requested = entry.slotArg()
            val isEmpty = { s: LocationSlot ->
                when (s) {
                    LocationSlot.PICKUP -> booking.uiState.value.pickupAddress == null
                    LocationSlot.DROP -> booking.uiState.value.dropAddress == null
                }
            }
            mutableStateOf(
                when {
                    isEmpty(requested) -> requested
                    isEmpty(LocationSlot.PICKUP) -> LocationSlot.PICKUP
                    isEmpty(LocationSlot.DROP) -> LocationSlot.DROP
                    else -> null
                }
            )
        }
        var showSavedOnly by rememberSaveable { mutableStateOf(false) }

        // ─────────────────────────────────────────────────────────────────────
        // PICKUP DEFAULTS TO WHERE THE CUSTOMER IS STANDING
        //
        // This used to live on the deleted home screen. Almost every parcel is
        // handed over from wherever the sender currently is, so asking them to
        // search for their own address is asking them to type something the
        // phone already knows.
        //
        // `withSender` stamps the profile's name and number onto it in the same
        // step, so the pickup arrives complete — location, contact name and
        // contact number — rather than as a bare address that still needs two
        // more fields filled in later. All three stay editable: the row opens
        // the details screen, and "Change" there reopens this one.
        // ─────────────────────────────────────────────────────────────────────
        // FIX — nothing on this screen asked for the location permission.
        //
        // getCurrentLocation() throws SecurityException without it and
        // LocationSearchViewModel catches that into an error field nothing
        // renders, so on a fresh install the pickup row simply stayed empty
        // forever with no prompt and no explanation. The old home screen this
        // replaced had the same gap; it was just less visible there because the
        // pickup was one line of small text rather than half the screen.
        val hasLocationPermission = rememberLocationPermissionState { granted ->
            if (granted && booking.uiState.value.pickupAddress == null) {
                locationVm.getCurrentLocation()
            }
        }

        // Once per screen entry, NOT keyed on the permission.
        //
        // This used to sit inside the effect below, which re-runs when the
        // permission resolves — so the address book was fetched twice on every
        // first visit, on top of the two fetches the two ViewModels already do
        // in their init blocks. Four GETs for one list.
        LaunchedEffect(Unit) { account.loadSavedAddresses() }

        LaunchedEffect(hasLocationPermission.value) {
            if (hasLocationPermission.value && bookingState.pickupAddress == null) {
                locationVm.getCurrentLocation()
            }
        }
        LaunchedEffect(state.selectedAddress) {
            val resolved = state.selectedAddress
            if (resolved != null && bookingState.pickupAddress == null) {
                booking.setPickupAddress(resolved.withSender(accountState.toSender()))
            }
        }

        // Only "resolving" while we are actually going to get an answer. With
        // the permission refused there is nothing in flight, so a spinner would
        // sit there forever promising something that is never coming.
        val isResolvingPickup = bookingState.pickupAddress == null &&
                hasLocationPermission.value &&
                state.isLoading

        // The details screen asks us to reopen a specific row for editing when
        // the customer taps "Change" there. A SavedStateHandle is the only way
        // back up the stack that survives the details screen being destroyed.
        val requestedSlot by entry.savedStateHandle
            .getStateFlow<String?>(ACTIVATE_SLOT_KEY, null)
            .collectAsStateWithLifecycle()
        LaunchedEffect(requestedSlot) {
            requestedSlot?.let { name ->
                runCatching { LocationSlot.valueOf(name) }.getOrNull()?.let { activeSlot = it }
                locationVm.updateSearchQuery("")
                // Consume it, or every return to this screen re-triggers.
                entry.savedStateHandle[ACTIVATE_SLOT_KEY] = null
            }
        }

        LaunchedEffect(Unit) { account.loadSavedAddresses() }

        /**
         * Write an address into the active row.
         *
         * Only leaves the screen once BOTH ends are set — the whole point of
         * showing them together is that filling one does not fling you onward
         * before you have checked the other.
         */
        fun commit(address: SavedAddress) {
            val target = activeSlot ?: return
            val nowPickup: SavedAddress?
            val nowDrop: SavedAddress?
            // FIX — picking a SAVED address arrived with its contact already
            // wiped.
            //
            // This used to unconditionally overwrite the incoming address's
            // contact with the one from the slot's PREVIOUS occupant, to avoid
            // losing a name the customer had typed. On the common path that
            // previous occupant is null, so choosing "Home · Prarthana" from the
            // list stamped null over her name and number — and the details
            // screen then opened with two empty fields for an address the app
            // demonstrably knew the contact for.
            //
            // The incoming address wins wherever it actually carries a value;
            // the previous contact is only a fallback, which is what makes
            // re-picking an address for the same person still keep their number.
            fun merge(incoming: String?, kept: String?) =
                incoming?.trim()?.takeIf { it.isNotBlank() } ?: kept

            when (target) {
                LocationSlot.PICKUP -> {
                    val kept = bookingState.pickupAddress
                    val merged = address.copy(
                        contactName = merge(address.contactName, kept?.contactName),
                        contactPhone = merge(address.contactPhone, kept?.contactPhone),
                        buildingDetails = merge(address.buildingDetails, kept?.buildingDetails)
                    )
                    // withSender PREFERS the profile over whatever the address
                    // already carries, so it is applied only as a last resort.
                    // Running it unconditionally would relabel a saved pickup
                    // that belongs to someone else — a shop's address stamped
                    // with the account holder's name — which is the same class
                    // of bug as the wipe above, just quieter.
                    val hasContact = !merged.contactName.isNullOrBlank() ||
                            !merged.contactPhone.isNullOrBlank()
                    booking.setPickupAddress(
                        if (hasContact) merged else merged.withSender(accountState.toSender())
                    )
                    nowPickup = address
                    nowDrop = bookingState.dropAddress
                }
                LocationSlot.DROP -> {
                    val kept = bookingState.dropAddress
                    booking.setDropAddress(
                        address.copy(
                            contactName = merge(address.contactName, kept?.contactName),
                            contactPhone = merge(address.contactPhone, kept?.contactPhone),
                            buildingDetails = merge(
                                address.buildingDetails, kept?.buildingDetails
                            )
                        )
                    )
                    nowPickup = bookingState.pickupAddress
                    nowDrop = address
                }
            }
            locationVm.updateSearchQuery("")

            if (nowPickup != null && nowDrop != null) {
                activeSlot = null
                navController.navigate("sendparcel_fare") {
                    popUpTo(PICKER_ROUTE)
                }
            } else {
                // Move focus to whichever end is still empty, so the next tap
                // needs no aiming.
                activeSlot =
                    if (nowPickup == null) LocationSlot.PICKUP else LocationSlot.DROP
            }
        }

        /** Resolve a suggestion to a full address, then hand it to [block]. */
        fun resolve(suggestion: PlaceSuggestion, block: (SavedAddress) -> Unit) {
            when {
                suggestion.placeId.startsWith("history:") ->
                    suggestion.historyLatLng()?.let { (lat, lng) ->
                        block(
                            SavedAddress(
                                address = suggestion.secondaryText,
                                latitude = lat,
                                longitude = lng,
                                label = suggestion.primaryText
                            )
                        )
                    }
                suggestion.placeId.startsWith("saved:") ->
                    savedAddresses
                        .firstOrNull { it.addressId == suggestion.placeId.removePrefix("saved:") }
                        ?.let(block)
                // A live Places result carries no coordinates until we fetch
                // details, so this is the one path that costs an API call.
                else -> locationVm.selectPlace(suggestion.placeId, block)
            }
        }


        val suggestions = remember(
            state.predictions, state.searchHistory, savedAddresses,
            state.searchQuery, showSavedOnly
        ) {
            when {
                showSavedOnly -> savedAddresses.map { it.toSuggestion() }
                state.searchQuery.length >= 3 -> state.predictions.map { it.toSuggestion() }
                else -> savedAddresses.take(3).map { it.toSuggestion() } +
                        state.searchHistory.take(5).map { it.toSuggestion() }
            }
        }

        LocationPickerScreen(
            pickup = bookingState.pickupAddress,
            drop = bookingState.dropAddress,
            activeSlot = activeSlot,
            query = state.searchQuery,
            suggestions = suggestions,
            isSearching = state.isLoadingPredictions,
            isResolvingPickup = isResolvingPickup,
            isSavedFilterOn = showSavedOnly,
            // Both ends are set but the customer got here some way other than
            // committing a suggestion — editing a contact, swapping, or coming
            // back from the fare. This is their way onward.
            onContinue = {
                activeSlot = null
                navController.navigate("sendparcel_fare") { popUpTo(PICKER_ROUTE) }
            },
            onActiveSlotChange = {
                activeSlot = it
                locationVm.updateSearchQuery("")
            },
            // A row that already HAS an address opens the details screen rather
            // than reopening search. Contact name and number are as likely to
            // need changing as the address itself — the pickup arrives
            // pre-filled from the profile, and the sender is not always the
            // account holder — and there was previously no way to reach them
            // before the fare step.
            onEditDetails = { navController.navigate("sendparcel_details/$it") },
            onQueryChange = {
                // Typing means the customer is searching, not browsing saved.
                if (it.isNotEmpty()) showSavedOnly = false
                locationVm.updateSearchQuery(it)
            },
            onSuggestionClick = { suggestion -> resolve(suggestion) { commit(it) } },
            onSaveSuggestion = { suggestion ->
                // Saving must NOT also select the address — the heart is a
                // bookmark, not a choice. Resolving and saving without touching
                // the booking is what keeps those two actions separate.
                resolve(suggestion) { resolved ->
                    account.saveAddress(
                        resolved.copy(
                            addressId = "",
                            addressType = "Other",
                            label = suggestion.primaryText.take(40)
                        )
                    )
                }
            },
            // ─────────────────────────────────────────────────────────────────
            // SWAP — addresses AND their contacts change ends together.
            //
            // setPickupAddress / setDropAddress each clear the fare and kick off
            // a route+fare fetch, which is correct: the journey genuinely
            // reversed and the old quote no longer describes it. Both calls
            // fire, but BookingViewModel cancels the in-flight job at the start
            // of each fetch, so only the second one — the one that sees both
            // ends in their new positions — survives to produce a quote.
            // ─────────────────────────────────────────────────────────────────
            onSwap = {
                val oldPickup = bookingState.pickupAddress
                val oldDrop = bookingState.dropAddress
                if (oldPickup != null && oldDrop != null) {
                    booking.setPickupAddress(oldDrop)
                    booking.setDropAddress(oldPickup)
                }
            },
            onPickOnMap = {
                // With neither row in search mode, "Select on map" fills the end
                // that is still empty; if both are set it edits the drop, which
                // is the one people adjust.
                val target = activeSlot
                    ?: when {
                        bookingState.pickupAddress == null -> LocationSlot.PICKUP
                        bookingState.dropAddress == null -> LocationSlot.DROP
                        else -> LocationSlot.DROP
                    }
                // Anchor the map on the end being edited if it already has a
                // point, otherwise on the other end — starting the pin near
                // where the customer is thinking beats starting it on a city.
                val anchor = when (target) {
                    LocationSlot.PICKUP ->
                        bookingState.pickupAddress ?: bookingState.dropAddress
                    LocationSlot.DROP ->
                        bookingState.dropAddress ?: bookingState.pickupAddress
                }
                navController.navigate(
                    "sendparcel_map/${anchor?.latitude ?: 0.0}/${anchor?.longitude ?: 0.0}/$target"
                )
            },
            onToggleSavedFilter = {
                showSavedOnly = !showSavedOnly
                if (showSavedOnly) locationVm.updateSearchQuery("")
            },
            onBack = { navController.popBackStack() }
        )
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 2. ADDRESS DETAILS — reached from a filled row on the picker
    // ═══════════════════════════════════════════════════════════════════════
    //
    // The same [AddressContactScreen] the post-fare steps use, but reached
    // BEFORE pricing and returning to the picker instead of moving the flow on.
    //
    // WHY THIS EXISTS
    // The pickup is pre-filled from GPS and the profile — address, contact name
    // and contact number all arrive without being asked for. That is only
    // acceptable if all three are correctable, and until this route there was
    // nowhere to correct the name or number until after the customer had
    // committed to a price. The sender is frequently not the account holder: a
    // shop booking on a customer's behalf needs that customer on the pickup so
    // the rider calls the right person.
    //
    // Nothing here touches the address coordinates, so nothing here can
    // invalidate a quote. Changing the ADDRESS goes back through the picker,
    // which re-quotes openly.
    composable(
        route = "sendparcel_details/{slot}",
        arguments = listOf(navArgument("slot") { type = NavType.StringType })
    ) { entry ->
        val parent = remember(entry) { navController.getBackStackEntry("booking_flow") }
        val booking: BookingViewModel = hiltViewModel(parent)
        val account: AccountViewModel = hiltViewModel(parent)

        val slot = entry.slotArg()
        val state by booking.uiState.collectAsStateWithLifecycle()
        val accountState by account.uiState.collectAsStateWithLifecycle()

        val isPickup = slot == LocationSlot.PICKUP
        val address = if (isPickup) state.pickupAddress else state.dropAddress

        var saveAs by rememberSaveable { mutableStateOf<String?>(null) }

        // Both ends read and write the same three fields on their SavedAddress,
        // so one pair of accessors covers them and the two branches cannot drift.
        val name = address?.contactName.orEmpty()
            .ifBlank { if (isPickup) accountState.toSender().name else "" }
        val phone = address?.contactPhone.orEmpty()
            .ifBlank { if (isPickup) accountState.toSender().phone else "" }
        val note = address?.buildingDetails.orEmpty()

        fun write(
            newName: String = name,
            newPhone: String = phone,
            newNote: String = note
        ) {
            val current = address ?: return
            val updated = current.copy(
                contactName = newName.trim(),
                contactPhone = newPhone.filter { it.isDigit() },
                buildingDetails = newNote.trim().takeIf { it.isNotBlank() }
            )
            if (isPickup) booking.setContactDetails(pickup = updated)
            else booking.setContactDetails(drop = updated)
        }

        val pickContact = rememberContactPicker { picked ->
            write(
                newName = picked.name,
                newPhone = picked.phone.filter { c -> c.isDigit() }.takeLast(10)
            )
        }

        AddressContactScreen(
            slot = slot,
            address = address,
            name = name,
            phone = phone,
            addressNote = note,
            myMobileNumber = accountState.toSender().phone,
            saveAsLabel = saveAs,
            stepLabel = null,
            ctaLabel = "Confirm and Proceed",
            onNameChange = { write(newName = it) },
            onPhoneChange = { write(newPhone = it) },
            onAddressNoteChange = { write(newNote = it) },
            onSaveAsChange = { saveAs = it },
            onPickFromContacts = pickContact,
            // Tell the picker which row to reopen for search, then go back to
            // it. Pushing a second picker instead would leave the customer
            // pressing back through a stack of identical screens.
            onChangeAddress = {
                navController.previousBackStackEntry
                    ?.savedStateHandle?.set(ACTIVATE_SLOT_KEY, slot.name)
                navController.popBackStack()
            },
            onAdjustOnMap = {
                navController.navigate(
                    "sendparcel_map/${address?.latitude ?: 0.0}/${address?.longitude ?: 0.0}/$slot"
                )
            },
            onContinue = {
                saveAs?.let { label ->
                    address?.let {
                        account.saveAddress(
                            it.copy(addressId = "", addressType = label, label = label)
                        )
                    }
                }
                navController.popBackStack()
            },
            onBack = { navController.popBackStack() }
        )
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 3. MAP PIN
    // ═══════════════════════════════════════════════════════════════════════
    composable(
        route = "sendparcel_map/{lat}/{lng}/{slot}",
        arguments = listOf(
            navArgument("lat") { type = NavType.FloatType },
            navArgument("lng") { type = NavType.FloatType },
            navArgument("slot") { type = NavType.StringType }
        )
    ) { entry ->
        val parent = remember(entry) { navController.getBackStackEntry("booking_flow") }
        val booking: BookingViewModel = hiltViewModel(parent)
        val account: AccountViewModel = hiltViewModel(parent)
        val mapVm: MapPickerViewModel = hiltViewModel()

        val slot = entry.slotArg()
        val lat = entry.arguments?.getFloat("lat")?.toDouble() ?: 0.0
        val lng = entry.arguments?.getFloat("lng")?.toDouble() ?: 0.0

        val state by mapVm.uiState.collectAsStateWithLifecycle()
        val accountState by account.uiState.collectAsStateWithLifecycle()

        // ─────────────────────────────────────────────────────────────────────
        // FIX — the second half of "the map does not load".
        //
        // getCurrentLocation() throws SecurityException without ACCESS_FINE_LOCATION,
        // and MapPickerViewModel catches it into an error field nothing renders.
        // Arriving here with no pickup yet (lat/lng = 0) and no permission left
        // selectedLocation null forever, so the screen sat on LatLng(0, 0).
        //
        // Nothing on this screen asked for the permission — LocationSearchScreen
        // did, but that is a different screen in a different flow, so a user who
        // reached the map picker without passing through it was never prompted.
        // ─────────────────────────────────────────────────────────────────────
        val hasLocationPermission = rememberLocationPermissionState { granted ->
            if (granted && lat == 0.0 && lng == 0.0) {
                mapVm.getCurrentLocation { mapVm.updateLocation(it) }
            }
        }

        LaunchedEffect(hasLocationPermission.value) {
            when {
                lat != 0.0 || lng != 0.0 -> mapVm.updateLocation(LatLng(lat, lng))
                hasLocationPermission.value -> mapVm.getCurrentLocation { mapVm.updateLocation(it) }
                // Permission refused and no anchor to fall back on. Showing the
                // city the app serves beats showing the middle of the ocean —
                // the customer can pan from somewhere recognisable.
                else -> mapVm.updateLocation(DEFAULT_MAP_CENTER)
            }
        }

        MapPinPickerScreen(
            slot = slot,
            // Never hand the map LatLng(0, 0) — see MapPinPickerScreen's camera
            // note. The route args are the anchor when we have one, the default
            // city centre when we do not.
            center = state.selectedLocation
                ?: LatLng(lat, lng).takeIf { lat != 0.0 || lng != 0.0 }
                ?: DEFAULT_MAP_CENTER,
            address = state.address,
            isResolving = state.isLoading,
            onCenterChanged = mapVm::updateLocation,
            onConfirm = {
                val point = state.selectedLocation ?: return@MapPinPickerScreen
                val address = SavedAddress(
                    address = state.address,
                    latitude = point.latitude,
                    longitude = point.longitude
                )
                when (slot) {
                    LocationSlot.PICKUP ->
                        booking.setPickupAddress(address.withSender(accountState.toSender()))
                    LocationSlot.DROP -> booking.setDropAddress(address)
                }
                // FIX — a confirmed PICKUP used to ALWAYS pop back to
                // `sendparcel_home`. That was fine when the map was only ever
                // reached while setting up a new booking, but the pickup-details
                // screen can now send the customer here to nudge the pin. Doing
                // that meant landing back on Home with the whole flow unwound —
                // fare gone, contacts gone — for what was meant to be a small
                // correction.
                //
                // Once BOTH ends exist, moving either of them is a re-quote, so
                // both slots take the same route to the fare screen. Only a
                // pickup chosen before there is any destination goes back to
                // Home, which is where the flow legitimately continues.
                val otherEndSet = when (slot) {
                    LocationSlot.PICKUP -> booking.uiState.value.dropAddress != null
                    LocationSlot.DROP -> booking.uiState.value.pickupAddress != null
                }
                if (otherEndSet) {
                    navController.navigate("sendparcel_fare") { popUpTo(PICKER_ROUTE) }
                } else {
                    navController.popBackStack(PICKER_ROUTE, inclusive = false)
                }
            },
            onBack = { navController.popBackStack() }
        )
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 4. VEHICLE + FARE
    // ═══════════════════════════════════════════════════════════════════════
    composable("sendparcel_fare") { entry ->
        val parent = remember(entry) { navController.getBackStackEntry("booking_flow") }
        val booking: BookingViewModel = hiltViewModel(parent)

        val state by booking.uiState.collectAsStateWithLifecycle()
        val fares by booking.vehicleFares.collectAsStateWithLifecycle()
        val isFareLoading by booking.isFareLoading.collectAsStateWithLifecycle()
        val selectedFare by booking.selectedFareDetails.collectAsStateWithLifecycle()
        val routeInfo by booking.routeInfo.collectAsStateWithLifecycle()

        val pickup = state.pickupAddress
        val drop = state.dropAddress

        // setDropAddress / setPickupAddress already kick off
        // calculateRouteAndThenFares(). We only re-request if we somehow landed
        // here with neither fares nor a request in flight — requesting again
        // unconditionally would cancel the in-flight job on every recomposition.
        LaunchedEffect(pickup, drop) {
            if (pickup != null && drop != null && fares.isEmpty() && !isFareLoading) {
                booking.calculateRoute(
                    pickup.latitude, pickup.longitude, drop.latitude, drop.longitude
                )
            }
        }

        val options = remember(fares) { fares.toVehicleOptions() }

        // Auto-select so the CTA is live with a real price immediately. An
        // enabled button showing ₹248 converts far better than a grey one
        // asking the customer to make a choice first.
        LaunchedEffect(fares) {
            if (selectedFare == null && fares.isNotEmpty()) {
                booking.selectFareDetails(fares.minByOrNull { it.roundedFare } ?: fares.first())
            }
        }

        val draft = BookingDraft(
            pickup = pickup,
            drop = drop,
            vehicles = options,
            selectedVehicleId = selectedFare?.vehicleTypeId?.toString(),
            goodsType = state.selectedGoodsTypeName ?: "Documents",
            goodsWeightKg = state.goodsWeight,
            paymentMethod = state.paymentMethod,
            couponCode = state.appliedCoupon,
            couponDiscount = state.discount,
            isLoadingFares = isFareLoading,
            routeDistanceKm = booking.getRoadDistanceKm(),
            routeDurationMin = booking.getRoadEtaMinutes()
        )

        FareStepScreen(
            routePolyline = routeInfo?.polylinePoints.orEmpty(),
            draft = draft,
            onBack = { navController.popBackStack() },
            onSelectVehicle = { id ->
                fares.firstOrNull { it.vehicleTypeId.toString() == id }
                    ?.let(booking::selectFareDetails)
            },
            onEditGoodsType = { navController.navigate("sendparcel_goods") },
            onEditPayment = {
                booking.setPaymentMethod(
                    if (state.paymentMethod.equals("cash", true)) "Online" else "Cash"
                )
            },
            onEditCoupon = { navController.navigate("coupons") },
            onBook = { navController.navigate("sendparcel_receiver") }
        )
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 5. DELIVERY DETAILS — map, address and receiver on one screen
    // ═══════════════════════════════════════════════════════════════════════
    //
    // Contact state still lives in the ViewModel on the address itself, written
    // through on every keystroke. `setContactDetails` — never setDropAddress —
    // because this screen runs AFTER the fare and setDropAddress would clear it.
    composable("sendparcel_receiver") { entry ->
        val parent = remember(entry) { navController.getBackStackEntry("booking_flow") }
        val booking: BookingViewModel = hiltViewModel(parent)
        val account: AccountViewModel = hiltViewModel(parent)

        val state by booking.uiState.collectAsStateWithLifecycle()
        val accountState by account.uiState.collectAsStateWithLifecycle()

        val receiver = state.dropAddress.toReceiver()

        // Which address-book label the customer picked, if any. Local to the
        // screen: it is an instruction for what to do on Continue, not part of
        // the booking payload.
        var saveAs by rememberSaveable { mutableStateOf<String?>(null) }

        fun update(updated: ReceiverDetails) {
            state.dropAddress?.let { booking.setContactDetails(drop = it.withReceiver(updated)) }
        }

        // Reuses the app's existing utils/ContactPicker — same one-shot URI
        // approach, so no READ_CONTACTS prompt.
        val pickContact = rememberContactPicker { picked ->
            update(
                receiver.copy(
                    name = picked.name,
                    phone = picked.phone.filter { c -> c.isDigit() }.takeLast(10)
                )
            )
        }

        AddressContactScreen(
            slot = LocationSlot.DROP,
            address = state.dropAddress,
            name = receiver.name,
            phone = receiver.phone,
            addressNote = receiver.addressNote,
            myMobileNumber = accountState.toSender().phone,
            saveAsLabel = saveAs,
            stepLabel = "1 of 2",
            ctaLabel = "Confirm and Proceed",
            onNameChange = { update(receiver.copy(name = it)) },
            onPhoneChange = { update(receiver.copy(phone = it)) },
            onAddressNoteChange = { update(receiver.copy(addressNote = it)) },
            onSaveAsChange = { saveAs = it },
            onPickFromContacts = pickContact,
            // Both routes back into the picker. Changing the location from here
            // re-quotes openly rather than silently repricing behind the
            // customer — see the note on AddressContactScreen.
            // Back to the ONE picker instance rather than stacking another.
            // Committing an address there re-quotes and returns to the fare, which
            // is correct: moving an end after pricing invalidates the price.
            onChangeAddress = {
                navController.navigate("sendparcel_destination/DROP") {
                    popUpTo(PICKER_ROUTE) { inclusive = true }
                }
            },
            onAdjustOnMap = {
                val a = state.dropAddress
                navController.navigate(
                    "sendparcel_map/${a?.latitude ?: 0.0}/${a?.longitude ?: 0.0}/DROP"
                )
            },
            onContinue = {
                saveAs?.let { label ->
                    state.dropAddress?.let { addr ->
                        account.saveAddress(
                            addr.copy(addressId = "", addressType = label, label = label)
                        )
                    }
                }
                navController.navigate("sendparcel_sender")
            },
            onBack = { navController.popBackStack() }
        )
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 6. PICKUP DETAILS — per booking, NOT the account profile
    // ═══════════════════════════════════════════════════════════════════════
    composable("sendparcel_sender") { entry ->
        val parent = remember(entry) { navController.getBackStackEntry("booking_flow") }
        val booking: BookingViewModel = hiltViewModel(parent)
        val account: AccountViewModel = hiltViewModel(parent)

        val state by booking.uiState.collectAsStateWithLifecycle()
        val accountState by account.uiState.collectAsStateWithLifecycle()

        val sender = state.pickupAddress.toSender(fallback = accountState.toSender())

        var saveAs by rememberSaveable { mutableStateOf<String?>(null) }

        fun update(updated: SenderDetails) {
            state.pickupAddress?.let { booking.setContactDetails(pickup = it.withSender(updated)) }
        }

        val pickContact = rememberContactPicker { picked ->
            update(
                sender.copy(
                    name = picked.name,
                    phone = picked.phone.filter { c -> c.isDigit() }.takeLast(10)
                )
            )
        }

        AddressContactScreen(
            slot = LocationSlot.PICKUP,
            address = state.pickupAddress,
            name = sender.name,
            phone = sender.phone,
            addressNote = sender.addressNote,
            myMobileNumber = accountState.toSender().phone,
            saveAsLabel = saveAs,
            stepLabel = "2 of 2",
            ctaLabel = "Confirm and Proceed",
            onNameChange = { update(sender.copy(name = it)) },
            onPhoneChange = { update(sender.copy(phone = it)) },
            onAddressNoteChange = { update(sender.copy(addressNote = it)) },
            onSaveAsChange = { saveAs = it },
            onPickFromContacts = pickContact,
            onChangeAddress = {
                navController.navigate("sendparcel_destination/PICKUP") {
                    popUpTo(PICKER_ROUTE) { inclusive = true }
                }
            },
            onAdjustOnMap = {
                val a = state.pickupAddress
                navController.navigate(
                    "sendparcel_map/${a?.latitude ?: 0.0}/${a?.longitude ?: 0.0}/PICKUP"
                )
            },
            onContinue = {
                saveAs?.let { label ->
                    state.pickupAddress?.let { addr ->
                        account.saveAddress(
                            addr.copy(addressId = "", addressType = label, label = label)
                        )
                    }
                }
                navController.navigate("sendparcel_confirm")
            },
            onBack = { navController.popBackStack() }
        )
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 7. CONFIRM
    // ═══════════════════════════════════════════════════════════════════════
    composable("sendparcel_confirm") { entry ->
        val parent = remember(entry) { navController.getBackStackEntry("booking_flow") }
        val booking: BookingViewModel = hiltViewModel(parent)
        val account: AccountViewModel = hiltViewModel(parent)

        val state by booking.uiState.collectAsStateWithLifecycle()
        val selectedFare by booking.selectedFareDetails.collectAsStateWithLifecycle()
        val accountState by account.uiState.collectAsStateWithLifecycle()

        LaunchedEffect(Unit) {
            booking.navigationEvent.collect { event ->
                if (event is BookingNavigationEvent.NavigateToSearchingRider) {
                    navController.navigate("searching_rider/${event.bookingId}") {
                        // The booking now exists on the server, so every screen
                        // that led to it is a dead end and gets popped.
                        //
                        // Anchored on the picker, NOT on "booking_flow". Popping
                        // the graph would clear the BookingViewModel that
                        // searching_rider itself resolves from
                        // getBackStackEntry("booking_flow") — the screen renders
                        // nothing without the pickup, drop and fare it holds, so
                        // the customer would land on a blank screen with a live
                        // booking on the server.
                        //
                        // KNOWN GAP: "Send again" jumps straight to the fare step
                        // and never puts the picker on the stack, so this matches
                        // nothing on that path and back can reach the confirm
                        // screen of an already-placed booking. Pre-existing, and
                        // not fixable by moving this anchor — it needs the repeat
                        // path to seed the picker first.
                        popUpTo(PICKER_ROUTE) { inclusive = true }
                    }
                }
            }
        }

        val draft = BookingDraft(
            pickup = state.pickupAddress,
            drop = state.dropAddress,
            sender = state.pickupAddress.toSender(fallback = accountState.toSender()),
            receiver = state.dropAddress.toReceiver(),
            vehicles = listOfNotNull(selectedFare?.toVehicleOption()),
            selectedVehicleId = selectedFare?.vehicleTypeId?.toString(),
            goodsType = state.selectedGoodsTypeName ?: "Documents",
            goodsWeightKg = state.goodsWeight,
            paymentMethod = state.paymentMethod,
            couponCode = state.appliedCoupon,
            couponDiscount = state.discount
        )

        ConfirmBookingScreen(
            draft = draft,
            isSubmitting = state.isLoading,
            errorMessage = state.error,
            onEditGoodsType = { navController.navigate("sendparcel_goods") },
            onEditPayment = {
                booking.setPaymentMethod(
                    if (state.paymentMethod.equals("cash", true)) "Online" else "Cash"
                )
            },
            onConfirm = { booking.confirmBooking() },
            onBack = { navController.popBackStack() }
        )
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 8. GOODS TYPE
    // ═══════════════════════════════════════════════════════════════════════
    composable("sendparcel_goods") { entry ->
        val parent = remember(entry) { navController.getBackStackEntry("booking_flow") }
        val booking: BookingViewModel = hiltViewModel(parent)
        val goodsTypes by booking.goodsTypes.collectAsStateWithLifecycle()
        val state by booking.uiState.collectAsStateWithLifecycle()

        LaunchedEffect(Unit) { booking.loadGoodsTypes() }

        GoodsTypePickerScreen(
            goodsTypes = goodsTypes,
            selectedId = state.selectedGoodsTypeId,
            weightKg = state.goodsWeight,
            // Selecting a type no longer leaves immediately. It re-seeds the
            // weight from that type's default, and the customer needs to see
            // and possibly correct that number — popping straight back would
            // hide the very field the type change just rewrote.
            onSelect = { booking.setGoodsType(it) },
            onWeightChange = { booking.setGoodsWeight(it) },
            onDone = { navController.popBackStack() },
            onBack = { navController.popBackStack() }
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// FARE STEP — map behind, sheet in front, and a back button that exists
// ═══════════════════════════════════════════════════════════════════════════

/**
 * The previous version of this screen accepted an `onBack` and never rendered
 * anything to call it, leaving the customer with no visible way out of the most
 * important screen in the flow.
 */
@Composable
private fun FareStepScreen(
    routePolyline: List<LatLng>,
    draft: BookingDraft,
    onBack: () -> Unit,
    onSelectVehicle: (String) -> Unit,
    onEditGoodsType: () -> Unit,
    onEditPayment: () -> Unit,
    onEditCoupon: () -> Unit,
    onBook: () -> Unit
) {
    // How much of the map the sheet is covering.
    //
    // Measured rather than guessed: the sheet grows and shrinks with the number
    // of vehicles, whether a coupon line is showing, and the navigation bar
    // inset. A hard-coded figure would frame the route correctly on exactly one
    // device. Feeding it to the map as content padding means "fit the route"
    // fits it into the part the customer can actually SEE — without it the map
    // centres on the full viewport and pushes half the journey behind the sheet,
    // which is what the screenshot showed.
    val density = LocalDensity.current
    var sheetHeight by remember { mutableStateOf(0.dp) }

    Box(modifier = Modifier.fillMaxSize().background(AppColors.Background)) {
        RoutePreviewMap(
            route = routePolyline,
            pickup = draft.pickup?.let { LatLng(it.latitude, it.longitude) },
            drop = draft.drop?.let { LatLng(it.latitude, it.longitude) },
            contentPadding = PaddingValues(bottom = sheetHeight),
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(12.dp)
        ) {
            MapFloatingButton(onClick = onBack, contentDescription = "Go back") {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack, null,
                    tint = AppColors.TextPrimary
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .onGloballyPositioned { coords ->
                    val measured = with(density) { coords.size.height.toDp() }
                    if (measured != sheetHeight) sheetHeight = measured
                }
        ) {
            VehicleFareSheet(
                draft = draft,
                onSelectVehicle = onSelectVehicle,
                onEditGoodsType = onEditGoodsType,
                onEditPayment = onEditPayment,
                onEditCoupon = onEditCoupon,
                onBook = onBook
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// MAPPING HELPERS
// ═══════════════════════════════════════════════════════════════════════════

private fun androidx.navigation.NavBackStackEntry.slotArg(): LocationSlot =
    runCatching { LocationSlot.valueOf(arguments?.getString("slot") ?: "DROP") }
        .getOrDefault(LocationSlot.DROP)

/**
 * Profile → sender defaults.
 *
 * `AccountUiState` carries the profile both nested (`user`) and flattened
 * (`userName` / `phoneNumber`) depending on which load path ran, so we check
 * both. Otherwise the sender block reads "You" with no number purely because
 * the customer arrived via a different entry point.
 */
internal fun AccountUiState.toSender() = SenderDetails(
    name = user?.fullName?.takeIf { it.isNotBlank() } ?: userName.orEmpty(),
    phone = user?.phoneNumber?.takeIf { it.isNotBlank() } ?: phoneNumber.orEmpty()
)

/** Drop address → receiver details. The address IS the storage. */
private fun SavedAddress?.toReceiver() = ReceiverDetails(
    name = this?.contactName.orEmpty(),
    phone = this?.contactPhone.orEmpty(),
    addressNote = this?.buildingDetails.orEmpty()
)

/**
 * Pickup address → sender details, falling back to the profile.
 *
 * The fallback only applies while the pickup carries no contact yet. Once the
 * customer has edited the sender, their values win — which is the whole point
 * of the sender screen, since the sender is not always the account holder.
 */
private fun SavedAddress?.toSender(fallback: SenderDetails) = SenderDetails(
    name = this?.contactName?.takeIf { it.isNotBlank() } ?: fallback.name,
    phone = this?.contactPhone?.takeIf { it.isNotBlank() } ?: fallback.phone,
    addressNote = this?.buildingDetails.orEmpty()
)
