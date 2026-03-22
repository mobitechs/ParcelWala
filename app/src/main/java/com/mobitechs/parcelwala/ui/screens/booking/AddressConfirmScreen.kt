package com.mobitechs.parcelwala.ui.screens.booking

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material.icons.filled.Check
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
    var useMyNumber by remember { mutableStateOf(false) }
    var nameError   by remember { mutableStateOf<String?>(null) }
    var phoneError  by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(useMyNumber) {
        if (useMyNumber && userPhoneNumber != null) {
            contactPhone = userPhoneNumber.replace("+91", "").replace(" ", "").trim()
            phoneError   = null
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
    val enterContactNameError = stringResource(R.string.enter_contact_name_error)
    val enterValidPhoneError  = stringResource(R.string.enter_valid_phone_error)

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

                    // Name field
                    StyledTextField(
                        value         = contactName,
                        onValueChange = { contactName = it; nameError = null },
                        label         = nameLabel,
                        placeholder   = stringResource(R.string.enter_contact_name),
                        icon          = Icons.Default.Person,
                        isError       = nameError != null,
                        errorMessage  = nameError,
                        imeAction     = ImeAction.Next,
                        onNext        = { focusManager.moveFocus(FocusDirection.Down) }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Phone field
                    StyledTextField(
                        value         = contactPhone,
                        onValueChange = {
                            if (it.length <= 10 && it.all { c -> c.isDigit() }) {
                                contactPhone = it; phoneError = null
                            }
                        },
                        label         = stringResource(R.string.contact_phone_required),
                        placeholder   = stringResource(R.string.enter_10_digit_mobile),
                        icon          = Icons.Default.Phone,
                        prefix        = stringResource(R.string.phone_prefix),
                        keyboardType  = KeyboardType.Phone,
                        isError       = phoneError != null,
                        errorMessage  = phoneError,
                        imeAction     = ImeAction.Next,
                        onNext        = { focusManager.moveFocus(FocusDirection.Down) }
                    )

                    // "Use my number" checkbox
                    if (isSaveAddressMode && userPhoneNumber != null) {
                        Row(
                            modifier          = Modifier.fillMaxWidth().padding(top = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked         = useMyNumber,
                                onCheckedChange = { useMyNumber = it },
                                colors          = CheckboxDefaults.colors(
                                    checkedColor   = AppColors.Primary,
                                    checkmarkColor = Color.White
                                )
                            )
                            Text(
                                text  = stringResource(R.string.use_my_mobile_format, userPhoneNumber),
                                style = MaterialTheme.typography.bodySmall,
                                color = AppColors.TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ── 4. ADDRESS DETAILS CARD ────────────────────────────────
                SectionCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    SectionHeader(
                        title    = stringResource(R.string.address_details_optional),
                        icon     = Icons.Default.Apartment,
                        modifier = Modifier.padding(bottom = 14.dp)
                    )

                    StyledTextField(
                        value         = buildingDetails,
                        onValueChange = { buildingDetails = it },
                        label         = stringResource(R.string.building_details_label),
                        placeholder   = stringResource(R.string.building_details_placeholder),
                        icon          = Icons.Default.Apartment,
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
                            if (it.length <= 6 && it.all { c -> c.isDigit() }) pincode = it
                        },
                        label         = stringResource(R.string.pincode_label),
                        placeholder   = stringResource(R.string.pincode_placeholder),
                        icon          = Icons.Default.PinDrop,
                        keyboardType  = KeyboardType.Number,
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
                                    onValueChange = { customLabel = it },
                                    label         = stringResource(R.string.label_name_optional),
                                    placeholder   = stringResource(R.string.label_placeholder),
                                    icon          = Icons.Default.Label,
                                    imeAction     = ImeAction.Done,
                                    onDone        = { focusManager.clearFocus() }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(100.dp))
            }

            // ── Sticky bottom CTA ──────────────────────────────────────────
            // navigationBarsPadding() pushes the button above the system
            // gesture bar / 3-button nav so it is never obscured.
            Surface(
                color           = Color.White,
                shadowElevation = 8.dp,
                modifier        = Modifier.navigationBarsPadding()
            ) {
                PrimaryButton(
                    text    = buttonText,
                    onClick = {
                        var isValid = true
                        if (contactName.isBlank()) { nameError = enterContactNameError; isValid = false }
                        if (contactPhone.length != 10) { phoneError = enterValidPhoneError; isValid = false }

                        if (isValid) {
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
                                    buildingDetails = buildingDetails.trim().ifEmpty { null },
                                    landmark        = landmark.trim().ifEmpty { null },
                                    pincode         = pincode.trim().ifEmpty { null },
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