// ui/screens/booking/AddressConfirmationScreen.kt
package com.mobitechs.parcelwala.ui.screens.booking

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.mobitechs.parcelwala.data.model.request.SavedAddress
import com.mobitechs.parcelwala.ui.components.*
import com.mobitechs.parcelwala.ui.theme.AppColors
import com.mobitechs.parcelwala.ui.viewmodel.BookingViewModel

/**
 * Address Confirmation Screen
 * Used for both booking flow and account saved addresses
 *
 * @param address The address to confirm/edit (for account flow)
 * @param locationType Type of location - "pickup", "drop", or "save" (for account)
 * @param onConfirm Callback when address is confirmed
 * @param onChangeLocation Callback to change location (goes to LocationSearchScreen)
 * @param onBack Callback for back navigation
 * @param isEditMode Whether editing an existing saved address
 * @param userPhoneNumber User's phone number for "Use my number" feature (optional)
 * @param showSaveLocationBadge Whether to show "This location will be saved" badge (optional)
 * @param viewModel BookingViewModel for booking flow (optional, for reading pendingAddress)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddressConfirmationScreen(
    address: SavedAddress?,
    locationType: String,
    onConfirm: (SavedAddress) -> Unit,
    onChangeLocation: () -> Unit,
    onBack: () -> Unit,
    // Optional parameters for account flow
    isEditMode: Boolean = false,
    userPhoneNumber: String? = null,
    showSaveLocationBadge: Boolean = false,
    // Optional ViewModel for booking flow
    viewModel: BookingViewModel? = null
) {
    val focusManager = LocalFocusManager.current

    // Determine if this is for saving address (account flow) or booking flow
    val isSaveAddressMode = locationType == "save" || showSaveLocationBadge

    // ============ GET ACTUAL ADDRESS ============
    // For booking flow with ViewModel, observe the state directly to handle async updates
    val uiState = viewModel?.uiState?.collectAsState()?.value

    // Determine which address to use:
    // 1. If pendingAddress exists, use that (Change location / MapPicker / LocationSearch)
    // 2. If isEditMode=true AND NO pendingAddress, use existing pickup/drop address (Edit button)
    // 3. Otherwise use the passed address parameter (for account flow)
    // NOTE: No remember{} here - we want this to always reflect the latest state
    val actualAddress = when {
        // For booking flow - if pendingAddress exists, always use it (from MapPicker/LocationSearch)
        viewModel != null && uiState?.pendingAddress != null -> {
            uiState.pendingAddress
        }
        // For booking flow editing existing address (Edit button, no pending address)
        viewModel != null && isEditMode -> {
            if (locationType == "pickup") uiState?.pickupAddress else uiState?.dropAddress
        }
        // For account flow or fallback
        else -> address
    }

    // ============ FORM STATE ============
    // Use key based on address ID and coordinates so fields update when address changes
    val addressKey = "${actualAddress?.addressId}_${actualAddress?.latitude}_${actualAddress?.longitude}"

    // Contact Details
    var contactName by remember(addressKey) {
        mutableStateOf(actualAddress?.contactName ?: "")
    }
    var contactPhone by remember(addressKey) {
        mutableStateOf(actualAddress?.contactPhone ?: "")
    }

    // Address Details - Using correct fields from SavedAddress
    var flatNumber by remember(addressKey) {
        mutableStateOf(actualAddress?.flatNumber ?: "")
    }
    var buildingName by remember(addressKey) {
        mutableStateOf(actualAddress?.buildingName ?: "")
    }
    var landmark by remember(addressKey) {
        mutableStateOf(actualAddress?.landmark ?: "")
    }
    var pincode by remember(addressKey) {
        mutableStateOf(actualAddress?.pincode ?: "")
    }

    // Address Type Selection
    var selectedType by remember(addressKey) {
        mutableStateOf(actualAddress?.addressType?.replaceFirstChar { it.uppercase() } ?: "Home")
    }

    // UI State
    var useMyNumber by remember { mutableStateOf(false) }
    var nameError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }

    // Handle "Use my mobile number" checkbox
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

    // Use key to force recreation when coordinates change significantly
    val mapKey = "${actualAddress?.latitude?.toString()?.take(8)}_${actualAddress?.longitude?.toString()?.take(8)}"

    val cameraPositionState = rememberCameraPositionState(key = mapKey) {
        position = CameraPosition.fromLatLngZoom(LatLng(defaultLat, defaultLng), 16f)
    }

    // Move camera when address coordinates change
    LaunchedEffect(actualAddress?.latitude, actualAddress?.longitude) {
        if (actualAddress?.latitude != null && actualAddress.longitude != null &&
            actualAddress.latitude != 0.0 && actualAddress.longitude != 0.0) {
            val newPosition = LatLng(actualAddress.latitude, actualAddress.longitude)
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(newPosition, 16f),
                durationMs = 300
            )
        }
    }

    // Empty state if no address
    if (actualAddress == null) {
        EmptyState(
            icon = Icons.Default.ErrorOutline,
            title = "No Address Selected",
            subtitle = "Please select a location first",
            actionText = "Go Back",
            onAction = onBack,
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
        )
        return
    }

    // ============ SCREEN CONFIGURATION ============
    val screenTitle = when {
        isSaveAddressMode && isEditMode -> "Edit Address"
        isSaveAddressMode -> "Add New Address"
        isEditMode -> "Edit ${if (locationType == "pickup") "Pickup" else "Drop"} Details"
        locationType == "pickup" -> "Confirm Pickup Location"
        else -> "Confirm Drop Location"
    }

    val buttonText = when {
        isSaveAddressMode -> "Save Address"
        isEditMode -> "Save Changes"
        locationType == "pickup" -> "Confirm Pickup"
        else -> "Confirm Drop"
    }

    val sectionHeaderText = when {
        isSaveAddressMode -> "Contact Details"
        locationType == "pickup" -> "Sender Details"
        else -> "Receiver Details"
    }

    val nameFieldLabel = when {
        isSaveAddressMode -> "Name *"
        locationType == "pickup" -> "Sender Name *"
        else -> "Receiver Name *"
    }

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
                                text = "Update contact and address details",
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
                            contentDescription = "Back",
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
                                    position = LatLng(
                                        actualAddress.latitude,
                                        actualAddress.longitude
                                    )
                                ),
                                title = if (locationType == "pickup") "Pickup" else "Drop"
                            )
                        }
                    } else {
                        // No coordinates - show placeholder
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
                                    text = "Location coordinates not available",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AppColors.TextHint
                                )
                            }
                        }
                    }

                    // "This location will be saved" badge for save mode
                    if (isSaveAddressMode) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 12.dp),
                            shape = RoundedCornerShape(20.dp),
                            color = AppColors.TextPrimary.copy(alpha = 0.9f)
                        ) {
                            Text(
                                text = "This location will be saved",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }

                    // Change Location Button overlay
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
                                text = "Change Location",
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
                                    isSaveAddressMode -> "Selected Location"
                                    locationType == "pickup" -> "Pickup Location"
                                    else -> "Drop Location"
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
                                text = actualAddress.address,
                                style = MaterialTheme.typography.bodyMedium,
                                color = AppColors.TextPrimary
                            )
                            // Show coordinates for verification
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
                    onValueChange = {
                        contactName = it
                        nameError = null
                    },
                    label = { Text(nameFieldLabel) },
                    placeholder = { Text("Enter contact person name") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = AppColors.Primary
                        )
                    },
                    isError = nameError != null,
                    supportingText = nameError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppColors.Primary,
                        unfocusedBorderColor = AppColors.Border
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Contact Phone
                OutlinedTextField(
                    value = contactPhone,
                    onValueChange = {
                        if (it.length <= 10 && it.all { char -> char.isDigit() }) {
                            contactPhone = it
                            phoneError = null
                        }
                    },
                    label = { Text("Contact Phone *") },
                    placeholder = { Text("Enter 10 digit mobile number") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            tint = AppColors.Primary
                        )
                    },
                    prefix = { Text("+91 ") },
                    isError = phoneError != null,
                    supportingText = phoneError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppColors.Primary,
                        unfocusedBorderColor = AppColors.Border
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    )
                )

                // "Use my mobile number" checkbox - only for save address mode
                if (isSaveAddressMode && userPhoneNumber != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = useMyNumber,
                            onCheckedChange = { useMyNumber = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = AppColors.Primary,
                                checkmarkColor = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Use my mobile number: $userPhoneNumber",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppColors.TextPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ============ ADDRESS DETAILS SECTION ============
                Text(
                    text = "Address Details (Optional)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Flat/House Number
                OutlinedTextField(
                    value = flatNumber,
                    onValueChange = { flatNumber = it },
                    label = { Text("Flat / House No.") },
                    placeholder = { Text("e.g., Flat 101, House 25") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = null,
                            tint = AppColors.Primary
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppColors.Primary,
                        unfocusedBorderColor = AppColors.Border
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Building Name
                OutlinedTextField(
                    value = buildingName,
                    onValueChange = { buildingName = it },
                    label = { Text("Building / Society Name") },
                    placeholder = { Text("e.g., Sun Tower, Green Valley") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Apartment,
                            contentDescription = null,
                            tint = AppColors.Primary
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppColors.Primary,
                        unfocusedBorderColor = AppColors.Border
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Landmark
                OutlinedTextField(
                    value = landmark,
                    onValueChange = { landmark = it },
                    label = { Text("Landmark") },
                    placeholder = { Text("e.g., Near petrol pump, Opposite mall") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = null,
                            tint = AppColors.Primary
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppColors.Primary,
                        unfocusedBorderColor = AppColors.Border
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Pincode
                OutlinedTextField(
                    value = pincode,
                    onValueChange = {
                        if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                            pincode = it
                        }
                    },
                    label = { Text("Pincode") },
                    placeholder = { Text("Enter 6 digit pincode") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.PinDrop,
                            contentDescription = null,
                            tint = AppColors.Primary
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppColors.Primary,
                        unfocusedBorderColor = AppColors.Border
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { focusManager.clearFocus() }
                    )
                )

                // ============ SAVE AS SECTION (for Save Address Mode) ============
                if (isSaveAddressMode) {
                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Save address as",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Address Type Selector
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AddressTypeChip(
                            text = "Home",
                            icon = Icons.Default.Home,
                            isSelected = selectedType == "Home",
                            onClick = { selectedType = "Home" },
                            modifier = Modifier.weight(1f)
                        )
                        AddressTypeChip(
                            text = "Shop",
                            icon = Icons.Default.Store,
                            isSelected = selectedType == "Shop",
                            onClick = { selectedType = "Shop" },
                            modifier = Modifier.weight(1f)
                        )
                        AddressTypeChip(
                            text = "Other",
                            icon = Icons.Default.Place,
                            isSelected = selectedType == "Other",
                            onClick = { selectedType = "Other" },
                            modifier = Modifier.weight(1f)
                        )
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
                        // Validate required fields
                        var isValid = true

                        if (contactName.isBlank()) {
                            nameError = "Please enter contact name"
                            isValid = false
                        }

                        if (contactPhone.length != 10) {
                            phoneError = "Please enter valid 10 digit number"
                            isValid = false
                        }

                        if (isValid) {
                            // Create updated address with ALL fields including lat/lng
                            val confirmedAddress = actualAddress.copy(
                                addressType = if (isSaveAddressMode) selectedType.lowercase() else actualAddress.addressType,
                                label = if (isSaveAddressMode) selectedType else actualAddress.label,
                                contactName = contactName.trim(),
                                contactPhone = contactPhone.trim(),
                                flatNumber = flatNumber.trim().ifEmpty { null },
                                buildingName = buildingName.trim().ifEmpty { null },
                                landmark = landmark.trim().ifEmpty { null },
                                pincode = pincode.trim().ifEmpty { null },
                                // Explicitly preserve coordinates
                                latitude = actualAddress.latitude,
                                longitude = actualAddress.longitude
                            )
                            onConfirm(confirmedAddress)
                        }
                    },
                    icon = when {
                        isSaveAddressMode -> Icons.Default.Check
                        isEditMode -> Icons.Default.Check
                        else -> Icons.Default.ArrowForward
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
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