package com.mobitechs.parcelwala.ui.screens.booking

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.EditLocation
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.TripOrigin
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.mobitechs.parcelwala.R
import com.mobitechs.parcelwala.data.model.request.SavedAddress
import com.mobitechs.parcelwala.ui.components.EmptyState
import com.mobitechs.parcelwala.ui.components.PrimaryButton
import com.mobitechs.parcelwala.ui.components.StatusBarScaffold
import com.mobitechs.parcelwala.ui.theme.AppColors
import com.mobitechs.parcelwala.utils.Validators
import com.mobitechs.parcelwala.utils.rememberContactPicker
import com.mobitechs.parcelwala.ui.viewmodel.BookingViewModel

// ══════════════════════════════════════════════════════════════════════════════
// AddressConfirmationScreen  —  Variation 1: Map hero + stacked section cards
// ══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddressConfirmationScreen(
    address: SavedAddress?,
    locationType: String,
    onConfirm: (SavedAddress) -> Unit,
    onChangeLocation: () -> Unit,
    onBack: () -> Unit,
    isEditMode: Boolean = false,
    userPhoneNumber: String? = null,
    /** Signed-in user's name, used by the "these are my details" shortcut. */
    userName: String? = null,
    /**
     * The customer's saved addresses. When non-empty a "Saved address" button
     * appears, so a regular sender doesn't retype the same flat number and
     * contact every single booking. Empty list simply hides the option.
     */
    savedAddresses: List<SavedAddress> = emptyList(),
    /**
     * Opens the full saved-addresses page. The sheet lists what's already here;
     * "See all" is for browsing, editing, or adding a new one. Whatever comes back
     * gets applied through [onSavedAddressPicked].
     */
    onSeeAllAddresses: () -> Unit = {},
    /** Set true by the caller to open the saved-address list. */
    openSavedAddressPicker: Boolean = false,
    /** Called once the picker has been opened, so the caller can reset its flag. */
    onSavedAddressPickerHandled: () -> Unit = {},
    showSaveLocationBadge: Boolean = false,
    viewModel: BookingViewModel? = null
) {
    val focusManager       = LocalFocusManager.current
    val isSaveAddressMode  = locationType == "save" || showSaveLocationBadge

    // ── Resolve the address to display ────────────────────────────────────────
    val uiState      = viewModel?.uiState?.collectAsState()?.value
    val actualAddress = when {
        viewModel != null && uiState?.pendingAddress != null -> uiState.pendingAddress
        viewModel != null && isEditMode ->
            if (locationType == "pickup") uiState?.pickupAddress else uiState?.dropAddress
        else -> address
    }

    // ── Form state ────────────────────────────────────────────────────────────
    val addressKey = "${actualAddress?.addressId}_${actualAddress?.latitude}_${actualAddress?.longitude}"

    var contactName     by remember(addressKey) { mutableStateOf(actualAddress?.contactName ?: "") }
    var contactPhone    by remember(addressKey) { mutableStateOf(actualAddress?.contactPhone ?: "") }
    var buildingDetails by remember(addressKey) { mutableStateOf(actualAddress?.buildingDetails ?: "") }
    var landmark        by remember(addressKey) { mutableStateOf(actualAddress?.landmark ?: "") }
    var pincode         by remember(addressKey) { mutableStateOf(actualAddress?.pincode ?: "") }
    var selectedType    by remember(addressKey) {
        mutableStateOf(
            when (actualAddress?.addressType?.lowercase()) {
                "home" -> "Home"; "shop" -> "Shop"; else -> "Other"
            }
        )
    }
    var customLabel by remember(addressKey) {
        mutableStateOf(
            if (actualAddress?.addressType.equals("other", ignoreCase = true)) {
                val lbl = actualAddress?.label ?: ""
                if (!lbl.equals("other", ignoreCase = true) &&
                    !lbl.equals("selected location", ignoreCase = true)) lbl else ""
            } else ""
        )
    }
    // ── ITEM 3: "these are my details" ────────────────────────────────────────
    // Most people booking a pickup are standing at the pickup themselves, and
    // most people receiving are sending to someone they've saved. One tap fills
    // the name and number from the signed-in account instead of retyping them.
    var useMyDetails by remember(addressKey) { mutableStateOf(false) }

    // ── ITEMS 1 & 4: validation state ────────────────────────────────────────
    // serverNameError / serverPhoneError hold errors pushed in from a failed
    // submit. Everything else is derived live from Validators, so the button
    // enables and disables as the user types.
    var serverNameError  by remember(addressKey) { mutableStateOf<String?>(null) }
    var serverPhoneError by remember(addressKey) { mutableStateOf<String?>(null) }

    var nameTouched     by remember(addressKey) { mutableStateOf(false) }
    var phoneTouched    by remember(addressKey) { mutableStateOf(false) }
    var buildingTouched by remember(addressKey) { mutableStateOf(false) }
    var pincodeTouched  by remember(addressKey) { mutableStateOf(false) }
    var labelTouched    by remember(addressKey) { mutableStateOf(false) }

    var showSavedAddressPicker by remember { mutableStateOf(false) }

    // Caller asked for the list (via "See all"). Open it, then hand the flag back
    // so it doesn't reopen on every recomposition.
    LaunchedEffect(openSavedAddressPicker) {
        if (openSavedAddressPicker) {
            showSavedAddressPicker = true
            onSavedAddressPickerHandled()
        }
    }

    /**
     * Copies a saved address into the form. Coordinates and the formatted address
     * are deliberately NOT copied — the pin the user already dropped on this screen
     * is the location they mean. Only the details they'd otherwise retype come over.
     */
    fun applySavedAddress(saved: SavedAddress) {
        saved.contactName?.takeIf { it.isNotBlank() }?.let {
            contactName = it; nameTouched = true; serverNameError = null
        }
        saved.contactPhone?.takeIf { it.isNotBlank() }?.let {
            contactPhone = it.filter { c -> c.isDigit() }.takeLast(10)
            phoneTouched = true; serverPhoneError = null
        }
        saved.buildingDetails?.takeIf { it.isNotBlank() }?.let {
            buildingDetails = it; buildingTouched = true
        }
        saved.landmark?.takeIf { it.isNotBlank() }?.let { landmark = it }
        saved.pincode?.takeIf { it.isNotBlank() }?.let {
            pincode = it.filter { c -> c.isDigit() }.take(6); pincodeTouched = true
        }
        useMyDetails = false
        showSavedAddressPicker = false
    }

    // ── ITEM 5: pick from the phonebook ──────────────────────────────────────
    val pickContact = rememberContactPicker { contact ->
        contactName  = contact.name
        contactPhone = contact.phone
        useMyDetails = false
        nameTouched  = true
        phoneTouched = true
        serverNameError  = null
        serverPhoneError = null
    }

    LaunchedEffect(useMyDetails) {
        if (useMyDetails) {
            userName?.takeIf { it.isNotBlank() }?.let {
                contactName = it
                nameTouched = true
                serverNameError = null
            }
            userPhoneNumber?.let {
                contactPhone = it.filter { c -> c.isDigit() }.takeLast(10)
                phoneTouched = true
                serverPhoneError = null
            }
        }
    }

    // ── Map state ─────────────────────────────────────────────────────────────
    val hasCoordinates = actualAddress?.latitude != null && actualAddress.longitude != null &&
            actualAddress.latitude != 0.0 && actualAddress.longitude != 0.0
    val defaultLat = actualAddress?.latitude ?: 19.0760
    val defaultLng = actualAddress?.longitude ?: 72.8777
    val mapKey = "${actualAddress?.latitude?.toString()?.take(8)}_${actualAddress?.longitude?.toString()?.take(8)}"

    val cameraPositionState = rememberCameraPositionState(key = mapKey) {
        position = CameraPosition.fromLatLngZoom(LatLng(defaultLat, defaultLng), 16f)
    }
    LaunchedEffect(actualAddress?.latitude, actualAddress?.longitude) {
        if (hasCoordinates) {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(LatLng(actualAddress!!.latitude, actualAddress.longitude), 16f),
                durationMs = 300
            )
        }
    }

    // ── Empty state guard ─────────────────────────────────────────────────────
    if (actualAddress == null) {
        EmptyState(
            icon       = Icons.Default.ErrorOutline,
            title      = stringResource(R.string.no_address_selected),
            subtitle   = stringResource(R.string.select_location_first),
            actionText = stringResource(R.string.go_back),
            onAction   = onBack,
            modifier   = Modifier.fillMaxSize().padding(32.dp)
        )
        return
    }

    // ── Screen copy ───────────────────────────────────────────────────────────
    val screenTitle = when {
        isSaveAddressMode && isEditMode -> stringResource(R.string.edit_address_title)
        isSaveAddressMode               -> stringResource(R.string.add_new_address_title)
        isEditMode -> if (locationType == "pickup")
            stringResource(R.string.edit_pickup_details)
        else stringResource(R.string.edit_drop_details)
        locationType == "pickup"        -> stringResource(R.string.confirm_pickup_location)
        else                            -> stringResource(R.string.confirm_drop_location)
    }
    val buttonText = when {
        isSaveAddressMode            -> stringResource(R.string.save_address)
        isEditMode                   -> stringResource(R.string.save_changes)
        locationType == "pickup"     -> stringResource(R.string.confirm_pickup)
        else                         -> stringResource(R.string.confirm_drop)
    }
    val sectionHeader = when {
        isSaveAddressMode            -> stringResource(R.string.contact_details_header)
        locationType == "pickup"     -> stringResource(R.string.sender_details_header)
        else                         -> stringResource(R.string.receiver_details_header)
    }
    val nameLabel = when {
        isSaveAddressMode            -> stringResource(R.string.name_required)
        locationType == "pickup"     -> stringResource(R.string.sender_name_required)
        else                         -> stringResource(R.string.receiver_name_required)
    }
    // enterContactNameError / enterValidPhoneError removed — Validators owns
    // these messages now, so there's one source of truth per rule.

    // ══════════════════════════════════════════════════════════════════════════
    // ITEMS 1 & 4 — validation
    //
    // Building details used to be optional and the header literally said so.
    // A driver standing outside a twelve-floor building with only "MG Road" to
    // go on has to phone the customer, which is exactly what the field exists to
    // prevent. It is required now, along with pincode.
    //
    // Errors are computed live rather than only on submit, so the Confirm button
    // reflects the true state of the form at all times.
    // ══════════════════════════════════════════════════════════════════════════

    val nameRule     = Validators.contactName(contactName)
    val phoneRule    = Validators.mobile(contactPhone)
    val buildingRule = Validators.buildingDetails(buildingDetails)
    val pincodeRule  = Validators.pincode(pincode)
    val labelRule    = if (isSaveAddressMode && selectedType == "Other") {
        Validators.label(customLabel)
    } else null

    val nameErrorShown     = serverNameError  ?: if (nameTouched) nameRule else null
    val phoneErrorShown    = serverPhoneError ?: if (phoneTouched) phoneRule else null
    val buildingErrorShown = if (buildingTouched) buildingRule else null
    val pincodeErrorShown  = if (pincodeTouched) pincodeRule else null
    val labelErrorShown    = if (labelTouched) labelRule else null

    val isFormValid = nameRule == null &&
            phoneRule == null &&
            buildingRule == null &&
            pincodeRule == null &&
            labelRule == null

    // How many required fields are still blocking the button. Shown above the
    // CTA so a disabled button is never a mystery.
    val missingCount = listOf(nameRule, phoneRule, buildingRule, pincodeRule, labelRule)
        .count { it != null }

    // ── Location accent colours ────────────────────────────────────────────────
    val accentColor = when {
        isSaveAddressMode        -> AppColors.Primary
        locationType == "pickup" -> AppColors.Pickup
        else                     -> AppColors.Drop
    }
    val locationIcon = when {
        isSaveAddressMode        -> Icons.Default.Place
        locationType == "pickup" -> Icons.Default.TripOrigin
        else                     -> Icons.Default.LocationOn
    }
    val locationLabel = when {
        isSaveAddressMode        -> stringResource(R.string.selected_location)
        locationType == "pickup" -> stringResource(R.string.pickup_location)
        else                     -> stringResource(R.string.drop_location)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Layout — uses StatusBarScaffold to keep the gradient top bar consistent
    // with every other screen in the app (no rogue white TopAppBar).
    // ══════════════════════════════════════════════════════════════════════════
    StatusBarScaffold(
        topBar = {
            com.mobitechs.parcelwala.ui.components.AppTopBar(
                title  = screenTitle,
                onBack = onBack
            )
        },
        containerColor = AppColors.Background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            // ── Scrollable body ────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {

                // ── 1. MAP PREVIEW ─────────────────────────────────────────
                MapPreviewSection(
                    hasCoordinates      = hasCoordinates,
                    actualAddress       = actualAddress,
                    cameraPositionState = cameraPositionState,
                    locationType        = locationType,
                    isSaveAddressMode   = isSaveAddressMode,
                    onChangeLocation    = onChangeLocation
                )

                Spacer(modifier = Modifier.height(16.dp))

                // ── 2. ADDRESS DISPLAY CARD ────────────────────────────────
                SectionCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Row(
                        modifier          = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Coloured dot icon
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(accentColor.copy(alpha = 0.10f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector        = locationIcon,
                                contentDescription = null,
                                tint               = accentColor,
                                modifier           = Modifier.size(18.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text       = locationLabel,
                                style      = MaterialTheme.typography.labelSmall.copy(
                                    fontSize      = 9.sp,
                                    letterSpacing = 0.6.sp
                                ),
                                fontWeight = FontWeight.Bold,
                                color      = accentColor
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text  = actualAddress.address.ifEmpty {
                                    stringResource(R.string.address_not_available)
                                },
                                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                                color = AppColors.TextPrimary
                            )
                            if (hasCoordinates) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text  = "${
                                        String.format("%.6f", actualAddress.latitude)
                                    }, ${String.format("%.6f", actualAddress.longitude)}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = AppColors.TextSecondary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ── 3. CONTACT DETAILS CARD ────────────────────────────────
                SectionCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    SectionHeader(
                        title    = sectionHeader,
                        icon     = Icons.Default.Person,
                        modifier = Modifier.padding(bottom = 14.dp)
                    )

                    // ══════════════════════════════════════════════════════
                    // HOW DO YOU WANT TO FILL THIS IN?
                    //
                    // Three ways to answer, offered before the empty fields rather
                    // than after them. Previously "I'm the sender" sat underneath
                    // the inputs, so most people had already typed their own name
                    // and number by the time they saw it.
                    //
                    // Order is deliberate: the most common case first.
                    // ══════════════════════════════════════════════════════

                    // 1 — It's me
                    if (userPhoneNumber != null || !userName.isNullOrBlank()) {
                        Surface(
                            onClick = { useMyDetails = !useMyDetails },
                            shape   = RoundedCornerShape(10.dp),
                            color   = if (useMyDetails) AppColors.Primary.copy(alpha = 0.08f)
                                      else AppColors.Surface,
                            border  = BorderStroke(
                                width = if (useMyDetails) 1.5.dp else 1.dp,
                                color = if (useMyDetails) AppColors.Primary else AppColors.Border
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier          = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked         = useMyDetails,
                                    onCheckedChange = { useMyDetails = it },
                                    colors          = CheckboxDefaults.colors(
                                        checkedColor   = AppColors.Primary,
                                        checkmarkColor = Color.White
                                    ),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (locationType == "pickup")
                                            stringResource(R.string.im_the_sender)
                                        else
                                            stringResource(R.string.im_the_receiver),
                                        style      = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color      = AppColors.TextPrimary
                                    )
                                    val mine = listOfNotNull(
                                        userName?.takeIf { it.isNotBlank() },
                                        userPhoneNumber?.filter { it.isDigit() }?.takeLast(10)
                                            ?.takeIf { it.isNotEmpty() }
                                    ).joinToString("  ·  ")
                                    if (mine.isNotBlank()) {
                                        Text(
                                            text  = mine,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = AppColors.TextSecondary
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // 2 & 3 — pick from the phonebook, or from an address already saved
                    Row(
                        modifier              = Modifier.fillMaxWidth().padding(bottom = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick  = { pickContact() },
                            modifier = Modifier.weight(1f),
                            shape    = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                            colors   = ButtonDefaults.outlinedButtonColors(
                                contentColor = AppColors.Primary
                            )
                        ) {
                            Icon(
                                Icons.Default.Contacts, null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text  = stringResource(R.string.choose_from_contacts),
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1
                            )
                        }

                        // Only offered when there's actually something saved.
                        // An empty picker is worse than no picker.
                        if (savedAddresses.isNotEmpty()) {
                            OutlinedButton(
                                onClick  = { showSavedAddressPicker = true },
                                modifier = Modifier.weight(1f),
                                shape    = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                                colors   = ButtonDefaults.outlinedButtonColors(
                                    contentColor = AppColors.Primary
                                )
                            ) {
                                Icon(
                                    Icons.Default.Bookmark, null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text  = stringResource(R.string.use_saved_address),
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    // Name field
                    StyledTextField(
                        value         = contactName,
                        onValueChange = {
                            contactName = Validators.nameInput(it)
                            nameTouched = true
                            serverNameError = null
                        },
                        label         = nameLabel,
                        placeholder   = stringResource(R.string.enter_contact_name),
                        icon          = Icons.Default.Person,
                        isError       = nameErrorShown != null,
                        errorMessage  = nameErrorShown,
                        imeAction     = ImeAction.Next,
                        onNext        = { focusManager.moveFocus(FocusDirection.Down) }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Phone field
                    StyledTextField(
                        value         = contactPhone,
                        onValueChange = {
                            contactPhone = Validators.digitsOnly(it, 10)
                            phoneTouched = true
                            serverPhoneError = null
                        },
                        label         = stringResource(R.string.contact_phone_required),
                        placeholder   = stringResource(R.string.enter_10_digit_mobile),
                        icon          = Icons.Default.Phone,
                        prefix        = stringResource(R.string.phone_prefix),
                        keyboardType  = KeyboardType.Phone,
                        isError       = phoneErrorShown != null,
                        errorMessage  = phoneErrorShown,
                        imeAction     = ImeAction.Next,
                        onNext        = { focusManager.moveFocus(FocusDirection.Down) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ── 4. ADDRESS DETAILS CARD ────────────────────────────────
                SectionCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    // ── ITEM 1: these are required now, not optional ──────
                    Row(
                        modifier              = Modifier.fillMaxWidth().padding(bottom = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        SectionHeader(
                            title    = stringResource(R.string.address_details_required),
                            icon     = Icons.Default.Apartment
                        )
                        // Jumps to the full address book — browse, edit, or add a
                        // new one. The chosen address flows back and fills these
                        // fields, so nothing has to be typed twice.
                        Text(
                            text       = stringResource(R.string.see_all),
                            style      = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color      = AppColors.Primary,
                            modifier   = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onSeeAllAddresses() }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    StyledTextField(
                        value         = buildingDetails,
                        onValueChange = {
                            buildingDetails = it.take(100)
                            buildingTouched = true
                        },
                        label         = stringResource(R.string.building_details_required),
                        placeholder   = stringResource(R.string.building_details_placeholder),
                        icon          = Icons.Default.Apartment,
                        isError       = buildingErrorShown != null,
                        errorMessage  = buildingErrorShown,
                        imeAction     = ImeAction.Next,
                        onNext        = { focusManager.moveFocus(FocusDirection.Down) }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    StyledTextField(
                        value         = landmark,
                        onValueChange = { landmark = it },
                        label         = stringResource(R.string.landmark_label),
                        placeholder   = stringResource(R.string.landmark_placeholder),
                        icon          = Icons.Default.Place,
                        imeAction     = ImeAction.Next,
                        onNext        = { focusManager.moveFocus(FocusDirection.Down) }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    StyledTextField(
                        value         = pincode,
                        onValueChange = {
                            pincode = Validators.digitsOnly(it, 6)
                            pincodeTouched = true
                        },
                        label         = stringResource(R.string.pincode_required),
                        placeholder   = stringResource(R.string.pincode_placeholder),
                        icon          = Icons.Default.PinDrop,
                        keyboardType  = KeyboardType.Number,
                        isError       = pincodeErrorShown != null,
                        errorMessage  = pincodeErrorShown,
                        imeAction     = ImeAction.Done,
                        onDone        = { focusManager.clearFocus() }
                    )
                }

                // ── 5. SAVE AS CARD (only in save mode) ────────────────────
                if (isSaveAddressMode) {
                    Spacer(modifier = Modifier.height(12.dp))

                    SectionCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                        SectionHeader(
                            title    = stringResource(R.string.save_address_as),
                            icon     = Icons.Default.Label,
                            modifier = Modifier.padding(bottom = 14.dp)
                        )

                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AddressTypeChip(
                                text       = stringResource(R.string.label_home),
                                icon       = Icons.Default.Home,
                                isSelected = selectedType == "Home",
                                onClick    = { selectedType = "Home"; customLabel = "" },
                                modifier   = Modifier.weight(1f)
                            )
                            AddressTypeChip(
                                text       = stringResource(R.string.label_shop),
                                icon       = Icons.Default.Store,
                                isSelected = selectedType == "Shop",
                                onClick    = { selectedType = "Shop"; customLabel = "" },
                                modifier   = Modifier.weight(1f)
                            )
                            AddressTypeChip(
                                text       = stringResource(R.string.label_other),
                                icon       = Icons.Default.MoreHoriz,
                                isSelected = selectedType == "Other",
                                onClick    = { selectedType = "Other" },
                                modifier   = Modifier.weight(1f)
                            )
                        }

                        // Custom label field — slides in when "Other" is picked
                        AnimatedVisibility(
                            visible = selectedType == "Other",
                            enter   = fadeIn() + expandVertically(),
                            exit    = fadeOut() + shrinkVertically()
                        ) {
                            Column {
                                Spacer(modifier = Modifier.height(12.dp))
                                StyledTextField(
                                    value         = customLabel,
                                    onValueChange = {
                                        customLabel = it.take(25)
                                        labelTouched = true
                                    },
                                    label         = stringResource(R.string.label_name_required),
                                    placeholder   = stringResource(R.string.label_placeholder),
                                    icon          = Icons.Default.Label,
                                    isError       = labelErrorShown != null,
                                    errorMessage  = labelErrorShown,
                                    imeAction     = ImeAction.Done,
                                    onDone        = { focusManager.clearFocus() }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(100.dp))
            }

            // ── Saved address picker ───────────────────────────────────────
            if (showSavedAddressPicker) {
                SavedAddressPickerSheet(
                    addresses = savedAddresses,
                    onPick    = { applySavedAddress(it) },
                    onDismiss = { showSavedAddressPicker = false }
                )
            }

            // ── Sticky bottom CTA ──────────────────────────────────────────
            // navigationBarsPadding() pushes the button above the system
            // gesture bar / 3-button nav so it is never obscured.
            Surface(
                color           = Color.White,
                shadowElevation = 8.dp,
                modifier        = Modifier.navigationBarsPadding()
            ) {
                Column {
                // A disabled button with no explanation is the worst kind of dead
                // end. Because the button is genuinely disabled until the form is
                // valid, its onClick can never fire to reveal the errors — so this
                // line has to show unconditionally whenever the form is incomplete.
                if (!isFormValid) {
                    Text(
                        text = pluralStringResource(
                            R.plurals.fields_remaining, missingCount, missingCount
                        ),
                        style      = MaterialTheme.typography.labelMedium,
                        color      = AppColors.Drop,
                        fontWeight = FontWeight.Medium,
                        modifier   = Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp)
                    )
                }

                PrimaryButton(
                    text    = buttonText,
                    enabled = isFormValid,
                    onClick = {
                        // Reveal every error at once for anyone who jumped straight
                        // to the button.
                        nameTouched = true; phoneTouched = true
                        buildingTouched = true; pincodeTouched = true
                        labelTouched = true
                        focusManager.clearFocus()

                        if (isFormValid) {
                            val finalLabel = when (selectedType) {
                                "Home"  -> "Home"
                                "Shop"  -> "Shop"
                                "Other" -> customLabel.trim().ifEmpty { "Other" }
                                else    -> actualAddress.label.ifEmpty { "Other" }
                            }
                            onConfirm(
                                actualAddress.copy(
                                    addressType     = if (isSaveAddressMode) selectedType else actualAddress.addressType,
                                    label           = if (isSaveAddressMode) finalLabel else actualAddress.label.ifEmpty { "Address" },
                                    contactName     = contactName.trim(),
                                    contactPhone    = contactPhone.trim(),
                                    buildingDetails = buildingDetails.trim(),
                                    landmark        = landmark.trim().ifEmpty { null },
                                    pincode         = pincode.trim(),
                                    latitude        = actualAddress.latitude,
                                    longitude       = actualAddress.longitude,
                                    address         = actualAddress.address
                                )
                            )
                        }
                    },
                    icon     = when {
                        isSaveAddressMode || isEditMode -> Icons.Default.Check
                        else                            -> Icons.Default.ArrowForward
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                )
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// SavedAddressPickerSheet
//
// Reuses the addresses the customer has already saved rather than making them
// retype a flat number and contact they've entered before. Picking one fills the
// contact and building details; the map pin stays where they put it.
// ══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SavedAddressPickerSheet(
    addresses: List<SavedAddress>,
    onPick: (SavedAddress) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor   = AppColors.Surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text       = stringResource(R.string.pick_saved_address),
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color      = AppColors.TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text  = stringResource(R.string.pick_saved_address_hint),
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextSecondary
            )
            Spacer(modifier = Modifier.height(14.dp))

            addresses.forEach { saved ->
                Surface(
                    onClick  = { onPick(saved) },
                    shape    = RoundedCornerShape(12.dp),
                    color    = AppColors.Surface,
                    border   = BorderStroke(1.dp, AppColors.Border),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    Row(
                        modifier          = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when (saved.addressType.lowercase()) {
                                "home" -> Icons.Default.Home
                                "shop" -> Icons.Default.Store
                                else   -> Icons.Default.Place
                            },
                            contentDescription = null,
                            tint     = AppColors.Primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text       = saved.label.ifBlank { saved.addressType },
                                style      = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color      = AppColors.TextPrimary
                            )
                            val detail = listOfNotNull(
                                saved.buildingDetails?.takeIf { it.isNotBlank() },
                                saved.contactName?.takeIf { it.isNotBlank() }
                            ).joinToString("  ·  ")
                            if (detail.isNotBlank()) {
                                Text(
                                    text     = detail,
                                    style    = MaterialTheme.typography.bodySmall,
                                    color    = AppColors.TextSecondary,
                                    maxLines = 1
                                )
                            }
                        }
                        Icon(
                            Icons.Default.ArrowForward, null,
                            tint = AppColors.TextSecondary, modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// MapPreviewSection
// Full-width map (or placeholder) with a "Change location" pill.
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun MapPreviewSection(
    hasCoordinates: Boolean,
    actualAddress: SavedAddress,
    cameraPositionState: com.google.maps.android.compose.CameraPositionState,
    locationType: String,
    isSaveAddressMode: Boolean,
    onChangeLocation: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
    ) {
        if (hasCoordinates) {
            GoogleMap(
                modifier            = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                uiSettings          = MapUiSettings(
                    zoomControlsEnabled   = false,
                    scrollGesturesEnabled = false,
                    zoomGesturesEnabled   = false,
                    tiltGesturesEnabled   = false,
                    rotationGesturesEnabled = false
                )
            ) {
                Marker(
                    state = MarkerState(
                        position = LatLng(actualAddress.latitude, actualAddress.longitude)
                    ),
                    title = if (locationType == "pickup") "Pickup" else "Drop"
                )
            }
        } else {
            // Placeholder when coordinates are unavailable
            Box(
                modifier         = Modifier
                    .fillMaxSize()
                    .background(AppColors.Background),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector        = Icons.Default.Map,
                        contentDescription = null,
                        tint               = AppColors.TextSecondary.copy(alpha = 0.40f),
                        modifier           = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text  = stringResource(R.string.location_not_available),
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextSecondary
                    )
                }
            }
        }

        // "Location saved" badge — save mode only
        if (isSaveAddressMode) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp),
                shape = RoundedCornerShape(20.dp),
                color = AppColors.TextPrimary.copy(alpha = 0.85f)
            ) {
                Text(
                    text     = stringResource(R.string.location_saved_badge),
                    style    = MaterialTheme.typography.labelMedium,
                    color    = Color.White,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                )
            }
        }

        // "Change location" pill — bottom-right
        FilledTonalButton(
            onClick  = onChangeLocation,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(10.dp),
            colors   = ButtonDefaults.filledTonalButtonColors(containerColor = Color.White),
            shape    = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector        = Icons.Default.EditLocation,
                contentDescription = null,
                tint               = AppColors.Primary,
                modifier           = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text  = stringResource(R.string.change_location),
                color = AppColors.Primary,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// SectionCard — white rounded card wrapping a logical group of fields
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun SectionCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier        = modifier.fillMaxWidth(),
        shape           = RoundedCornerShape(16.dp),
        color           = Color.White,
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            content()
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// SectionHeader — icon + title row at the top of each card
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun SectionHeader(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Row(
        modifier          = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier         = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(AppColors.Primary.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = icon,
                contentDescription = null,
                tint               = AppColors.Primary,
                modifier           = Modifier.size(15.dp)
            )
        }
        Text(
            text       = title,
            style      = MaterialTheme.typography.titleSmall.copy(fontSize = 13.sp),
            fontWeight = FontWeight.Bold,
            color      = AppColors.TextPrimary
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// StyledTextField — OutlinedTextField wrapped in the app's colour system
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun StyledTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    icon: ImageVector,
    prefix: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    isError: Boolean = false,
    errorMessage: String? = null,
    onNext: (() -> Unit)? = null,
    onDone: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value         = value,
        onValueChange = onValueChange,
        label         = { Text(label, fontSize = 12.sp) },
        placeholder   = { Text(placeholder, fontSize = 12.sp) },
        leadingIcon   = {
            Icon(
                imageVector        = icon,
                contentDescription = null,
                tint               = AppColors.Primary,
                modifier           = Modifier.size(18.dp)
            )
        },
        prefix        = prefix?.let { { Text(it, fontSize = 12.sp) } },
        isError       = isError,
        supportingText = errorMessage?.let {
            { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 11.sp) }
        },
        modifier      = modifier.fillMaxWidth(),
        colors        = OutlinedTextFieldDefaults.colors(
            focusedBorderColor   = AppColors.Primary,
            unfocusedBorderColor = AppColors.Border,
            focusedLabelColor    = AppColors.Primary
        ),
        shape         = RoundedCornerShape(12.dp),
        singleLine    = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction    = imeAction
        ),
        keyboardActions = KeyboardActions(
            onNext = onNext?.let { { it() } },
            onDone = onDone?.let { { it() } }
        )
    )
}

// ══════════════════════════════════════════════════════════════════════════════
// AddressTypeChip — square selector chip for Home / Shop / Other
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun AddressTypeChip(
    text: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick  = onClick,
        modifier = modifier,
        shape    = RoundedCornerShape(12.dp),
        color    = if (isSelected) AppColors.Primary.copy(alpha = 0.08f) else AppColors.Background,
        border   = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 1.5.dp else 0.5.dp,
            color = if (isSelected) AppColors.Primary else AppColors.Divider
        )
    ) {
        Column(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment   = Alignment.CenterHorizontally,
            verticalArrangement   = Arrangement.spacedBy(5.dp)
        ) {
            // Icon in a small tinted circle
            Box(
                modifier         = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) AppColors.Primary.copy(alpha = 0.12f)
                        else AppColors.Divider.copy(alpha = 0.50f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = icon,
                    contentDescription = text,
                    tint               = if (isSelected) AppColors.Primary else AppColors.TextSecondary,
                    modifier           = Modifier.size(17.dp)
                )
            }
            Text(
                text       = text,
                style      = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp),
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color      = if (isSelected) AppColors.Primary else AppColors.TextSecondary
            )
        }
    }
}