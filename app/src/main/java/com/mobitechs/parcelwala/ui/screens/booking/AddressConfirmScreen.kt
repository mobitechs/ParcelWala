// ui/screens/booking/AddressConfirmationScreen.kt
package com.mobitechs.parcelwala.ui.screens.booking

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.mobitechs.parcelwala.R
import com.mobitechs.parcelwala.data.model.request.SavedAddress
import com.mobitechs.parcelwala.ui.components.*
import com.mobitechs.parcelwala.ui.theme.AppColors
import com.mobitechs.parcelwala.ui.viewmodel.BookingViewModel

/**
 * Address Confirmation Screen
 * Used for both booking flow and account saved addresses
 */
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
    val focusManager = LocalFocusManager.current

    val isSaveAddressMode = locationType == "save" || showSaveLocationBadge

    // ============ GET ACTUAL ADDRESS ============
    val uiState = viewModel?.uiState?.collectAsState()?.value

    val actualAddress = when {
        viewModel != null && uiState?.pendingAddress != null -> {
            uiState.pendingAddress
        }
        viewModel != null && isEditMode -> {
            if (locationType == "pickup") uiState?.pickupAddress else uiState?.dropAddress
        }
        else -> address
    }

    // ============ FORM STATE ============
    val addressKey = "${actualAddress?.addressId}_${actualAddress?.latitude}_${actualAddress?.longitude}"

    var contactName by remember(addressKey) { mutableStateOf(actualAddress?.contactName ?: "") }
    var contactPhone by remember(addressKey) { mutableStateOf(actualAddress?.contactPhone ?: "") }
    var buildingDetails by remember(addressKey) { mutableStateOf(actualAddress?.buildingDetails ?: "") }
    var landmark by remember(addressKey) { mutableStateOf(actualAddress?.landmark ?: "") }
    var pincode by remember(addressKey) { mutableStateOf(actualAddress?.pincode ?: "") }

    var selectedType by remember(addressKey) {
        mutableStateOf(
            when (actualAddress?.addressType?.lowercase()) {
                "home" -> "Home"
                "shop" -> "Shop"
                else -> "Other"
            }
        )
    }

    var customLabel by remember(addressKey) {
        mutableStateOf(
            if (actualAddress?.addressType?.equals("other", ignoreCase = true) == true) {
                val label = actualAddress.label
                if (!label.equals("other", ignoreCase = true) &&
                    !label.equals("selected location", ignoreCase = true)) {
                    label
                } else { "" }
            } else { "" }
        )
    }

    var useMyNumber by remember { mutableStateOf(false) }
    var nameError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(useMyNumber) {
        if (useMyNumber && userPhoneNumber != null) {
            contactPhone = userPhoneNumber.replace("+91", "").replace(" ", "").trim()
            phoneError = null
        }
    }

    // ============ MAP STATE ============
    val hasCoordinates = actualAddress?.latitude != null && actualAddress.longitude != null &&
            actualAddress.latitude != 0.0 && actualAddress.longitude != 0.0
    val defaultLat = actualAddress?.latitude ?: 19.0760
    val defaultLng = actualAddress?.longitude ?: 72.8777
    val mapKey = "${actualAddress?.latitude?.toString()?.take(8)}_${actualAddress?.longitude?.toString()?.take(8)}"

    val cameraPositionState = rememberCameraPositionState(key = mapKey) {
        position = CameraPosition.fromLatLngZoom(LatLng(defaultLat, defaultLng), 16f)
    }

    LaunchedEffect(actualAddress?.latitude, actualAddress?.longitude) {
        if (actualAddress?.latitude != null && actualAddress.longitude != null &&
            actualAddress.latitude != 0.0 && actualAddress.longitude != 0.0) {
            val newPosition = LatLng(actualAddress.latitude, actualAddress.longitude)
            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(newPosition, 16f), durationMs = 300)
        }
    }

    // Empty state if no address
    if (actualAddress == null) {
        EmptyState(
            icon = Icons.Default.ErrorOutline,
            title = stringResource(R.string.no_address_selected),
            subtitle = stringResource(R.string.select_location_first),
            actionText = stringResource(R.string.go_back),
            onAction = onBack,
            modifier = Modifier.fillMaxSize().padding(32.dp)
        )
        return
    }

    // ============ SCREEN CONFIGURATION ============
    val screenTitle = when {
        isSaveAddressMode && isEditMode -> stringResource(R.string.edit_address_title)
        isSaveAddressMode -> stringResource(R.string.add_new_address_title)
        isEditMode -> if (locationType == "pickup") stringResource(R.string.edit_pickup_details) else stringResource(R.string.edit_drop_details)
        locationType == "pickup" -> stringResource(R.string.confirm_pickup_location)
        else -> stringResource(R.string.confirm_drop_location)
    }

    val buttonText = when {
        isSaveAddressMode -> stringResource(R.string.save_address)
        isEditMode -> stringResource(R.string.save_changes)
        locationType == "pickup" -> stringResource(R.string.confirm_pickup)
        else -> stringResource(R.string.confirm_drop)
    }

    val sectionHeaderText = when {
        isSaveAddressMode -> stringResource(R.string.contact_details_header)
        locationType == "pickup" -> stringResource(R.string.sender_details_header)
        else -> stringResource(R.string.receiver_details_header)
    }

    val nameFieldLabel = when {
        isSaveAddressMode -> stringResource(R.string.name_required)
        locationType == "pickup" -> stringResource(R.string.sender_name_required)
        else -> stringResource(R.string.receiver_name_required)
    }

    val enterContactNameError = stringResource(R.string.enter_contact_name_error)
    val enterValidPhoneError = stringResource(R.string.enter_valid_phone_error)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = screenTitle,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (isEditMode && !isSaveAddressMode) {
                            Text(
                                text = stringResource(R.string.update_contact_subtitle),
                                style = MaterialTheme.typography.labelSmall,
                                color = AppColors.TextSecondary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = AppColors.TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        containerColor = AppColors.Background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Scrollable Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                // ============ MAP PREVIEW ============
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    if (hasCoordinates) {
                        GoogleMap(
                            modifier = Modifier.fillMaxSize(),
                            cameraPositionState = cameraPositionState,
                            uiSettings = MapUiSettings(
                                zoomControlsEnabled = false,
                                scrollGesturesEnabled = false,
                                zoomGesturesEnabled = false,
                                tiltGesturesEnabled = false,
                                rotationGesturesEnabled = false
                            )
                        ) {
                            Marker(
                                state = MarkerState(
                                    position = LatLng(actualAddress.latitude, actualAddress.longitude)
                                ),
                                title = if (locationType == "pickup") stringResource(R.string.pickup_fallback) else stringResource(R.string.drop_fallback)
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(AppColors.Surface),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Map,
                                    contentDescription = null,
                                    tint = AppColors.TextHint,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = stringResource(R.string.location_not_available),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AppColors.TextHint
                                )
                            }
                        }
                    }

                    if (isSaveAddressMode) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 12.dp),
                            shape = RoundedCornerShape(20.dp),
                            color = AppColors.TextPrimary.copy(alpha = 0.9f)
                        ) {
                            Text(
                                text = stringResource(R.string.location_saved_badge),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = onChangeLocation,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.EditLocation,
                                contentDescription = null,
                                tint = AppColors.Primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.change_location),
                                color = AppColors.Primary,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ============ ADDRESS DISPLAY CARD ============
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = when {
                                isSaveAddressMode -> Icons.Default.Place
                                locationType == "pickup" -> Icons.Default.TripOrigin
                                else -> Icons.Default.LocationOn
                            },
                            contentDescription = null,
                            tint = when {
                                isSaveAddressMode -> AppColors.Primary
                                locationType == "pickup" -> AppColors.Pickup
                                else -> AppColors.Drop
                            },
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = when {
                                    isSaveAddressMode -> stringResource(R.string.selected_location)
                                    locationType == "pickup" -> stringResource(R.string.pickup_location)
                                    else -> stringResource(R.string.drop_location)
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = when {
                                    isSaveAddressMode -> AppColors.Primary
                                    locationType == "pickup" -> AppColors.Pickup
                                    else -> AppColors.Drop
                                },
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = actualAddress.address.ifEmpty { stringResource(R.string.address_not_available) },
                                style = MaterialTheme.typography.bodyMedium,
                                color = AppColors.TextPrimary
                            )
                            if (hasCoordinates) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "📍 ${String.format("%.6f", actualAddress.latitude)}, ${String.format("%.6f", actualAddress.longitude)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AppColors.TextHint
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ============ CONTACT DETAILS SECTION ============
                Text(
                    text = sectionHeaderText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Contact Name
                OutlinedTextField(
                    value = contactName,
                    onValueChange = { contactName = it; nameError = null },
                    label = { Text(nameFieldLabel) },
                    placeholder = { Text(stringResource(R.string.enter_contact_name)) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = AppColors.Primary)
                    },
                    isError = nameError != null,
                    supportingText = nameError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AppColors.Primary, unfocusedBorderColor = AppColors.Border),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Contact Phone
                OutlinedTextField(
                    value = contactPhone,
                    onValueChange = {
                        if (it.length <= 10 && it.all { char -> char.isDigit() }) { contactPhone = it; phoneError = null }
                    },
                    label = { Text(stringResource(R.string.contact_phone_required)) },
                    placeholder = { Text(stringResource(R.string.enter_10_digit_mobile)) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Phone, contentDescription = null, tint = AppColors.Primary)
                    },
                    prefix = { Text(stringResource(R.string.phone_prefix)) },
                    isError = phoneError != null,
                    supportingText = phoneError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AppColors.Primary, unfocusedBorderColor = AppColors.Border),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                )

                // "Use my mobile number" checkbox
                if (isSaveAddressMode && userPhoneNumber != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = useMyNumber,
                            onCheckedChange = { useMyNumber = it },
                            colors = CheckboxDefaults.colors(checkedColor = AppColors.Primary, checkmarkColor = Color.White)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.use_my_mobile_format, userPhoneNumber),
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppColors.TextPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ============ ADDRESS DETAILS SECTION ============
                Text(
                    text = stringResource(R.string.address_details_optional),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Building Details
                OutlinedTextField(
                    value = buildingDetails,
                    onValueChange = { buildingDetails = it },
                    label = { Text(stringResource(R.string.building_details_label)) },
                    placeholder = { Text(stringResource(R.string.building_details_placeholder)) },
                    leadingIcon = { Icon(imageVector = Icons.Default.Apartment, contentDescription = null, tint = AppColors.Primary) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AppColors.Primary, unfocusedBorderColor = AppColors.Border),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Landmark
                OutlinedTextField(
                    value = landmark,
                    onValueChange = { landmark = it },
                    label = { Text(stringResource(R.string.landmark_label)) },
                    placeholder = { Text(stringResource(R.string.landmark_placeholder)) },
                    leadingIcon = { Icon(imageVector = Icons.Default.Place, contentDescription = null, tint = AppColors.Primary) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AppColors.Primary, unfocusedBorderColor = AppColors.Border),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Pincode
                OutlinedTextField(
                    value = pincode,
                    onValueChange = { if (it.length <= 6 && it.all { char -> char.isDigit() }) { pincode = it } },
                    label = { Text(stringResource(R.string.pincode_label)) },
                    placeholder = { Text(stringResource(R.string.pincode_placeholder)) },
                    leadingIcon = { Icon(imageVector = Icons.Default.PinDrop, contentDescription = null, tint = AppColors.Primary) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AppColors.Primary, unfocusedBorderColor = AppColors.Border),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                )

                // ============ SAVE AS SECTION ============
                if (isSaveAddressMode) {
                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = stringResource(R.string.save_address_as),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Address Type Selector
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AddressTypeChip(
                            text = stringResource(R.string.home_type), icon = Icons.Default.Home,
                            isSelected = selectedType == "Home",
                            onClick = { selectedType = "Home"; customLabel = "" },
                            modifier = Modifier.weight(1f)
                        )
                        AddressTypeChip(
                            text = stringResource(R.string.shop_type), icon = Icons.Default.Store,
                            isSelected = selectedType == "Shop",
                            onClick = { selectedType = "Shop"; customLabel = "" },
                            modifier = Modifier.weight(1f)
                        )
                        AddressTypeChip(
                            text = stringResource(R.string.other_type), icon = Icons.Default.MoreHoriz,
                            isSelected = selectedType == "Other",
                            onClick = { selectedType = "Other" },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    AnimatedVisibility(
                        visible = selectedType == "Other",
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = customLabel,
                                onValueChange = { customLabel = it },
                                label = { Text(stringResource(R.string.label_name_optional)) },
                                placeholder = { Text(stringResource(R.string.label_placeholder)) },
                                leadingIcon = { Icon(imageVector = Icons.Default.Label, contentDescription = null, tint = AppColors.Primary) },
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AppColors.Primary, unfocusedBorderColor = AppColors.Border),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(100.dp))
            }

            // ============ BOTTOM BUTTON ============
            Surface(
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                PrimaryButton(
                    text = buttonText,
                    onClick = {
                        var isValid = true
                        if (contactName.isBlank()) { nameError = enterContactNameError; isValid = false }
                        if (contactPhone.length != 10) { phoneError = enterValidPhoneError; isValid = false }

                        if (isValid) {
                            val finalLabel = when (selectedType) {
                                "Home" -> "Home"
                                "Shop" -> "Shop"
                                "Other" -> customLabel.trim().ifEmpty { "Other" }
                                else -> actualAddress.label.ifEmpty { "Other" }
                            }

                            val confirmedAddress = actualAddress.copy(
                                addressType = if (isSaveAddressMode) selectedType else actualAddress.addressType,
                                label = if (isSaveAddressMode) finalLabel else actualAddress.label.ifEmpty { "Address" },
                                contactName = contactName.trim(),
                                contactPhone = contactPhone.trim(),
                                buildingDetails = buildingDetails.trim().ifEmpty { null },
                                landmark = landmark.trim().ifEmpty { null },
                                pincode = pincode.trim().ifEmpty { null },
                                latitude = actualAddress.latitude,
                                longitude = actualAddress.longitude,
                                address = actualAddress.address
                            )
                            onConfirm(confirmedAddress)
                        }
                    },
                    icon = when {
                        isSaveAddressMode -> Icons.Default.Check
                        isEditMode -> Icons.Default.Check
                        else -> Icons.Default.ArrowForward
                    },
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                )
            }
        }
    }
}

/**
 * Address Type Selection Chip
 */
@Composable
private fun AddressTypeChip(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) AppColors.Primary.copy(alpha = 0.1f) else Color.White,
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) AppColors.Primary else AppColors.Border
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = if (isSelected) AppColors.Primary else AppColors.TextSecondary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) AppColors.Primary else AppColors.TextSecondary
            )
        }
    }
}