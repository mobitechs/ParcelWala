// ui/screens/booking/AddressConfirmationScreen.kt
package com.mobitechs.parcelwala.ui.screens.booking

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.LatLng
import com.mobitechs.parcelwala.data.model.request.SavedAddress
import com.mobitechs.parcelwala.ui.components.*
import com.mobitechs.parcelwala.ui.theme.AppColors

/**
 * Address Confirmation Screen
 * Used for both booking flow and account saved addresses
 *
 * @param address The address to confirm/edit
 * @param locationType Type of location - "pickup", "drop", or "save" (for account)
 * @param onConfirm Callback when address is confirmed
 * @param onEdit Callback to edit/change location
 * @param onBack Callback for back navigation
 * @param isEditMode Whether editing an existing saved address (optional, for account flow)
 * @param userPhoneNumber User's phone number for "Use my number" feature (optional)
 * @param showSaveLocationBadge Whether to show "This location will be saved" badge (optional)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddressConfirmationScreen(
    address: SavedAddress?,
    locationType: String,
    onConfirm: (SavedAddress) -> Unit,
    onEdit: () -> Unit,
    onBack: () -> Unit,
    // Optional parameters for account flow
    isEditMode: Boolean = false,
    userPhoneNumber: String? = null,
    showSaveLocationBadge: Boolean = false
) {
    // Determine if this is for saving address (account flow) or booking flow
    val isSaveAddressMode = locationType == "save" || showSaveLocationBadge

    // ✅ Form State - initialize with existing address data
    // Use key based on address ID and coordinates so fields update when address changes
    val addressKey = "${address?.addressId}_${address?.latitude}_${address?.longitude}"

    var houseNumber by remember(addressKey) {
        mutableStateOf(address?.landmark ?: "")
    }
    var pincode by remember(addressKey) {
        mutableStateOf("")
    }
    var contactName by remember(addressKey) {
        mutableStateOf(address?.contactName ?: "")
    }
    var contactPhone by remember(addressKey) {
        mutableStateOf(address?.contactPhone ?: "")
    }
    var useMyNumber by remember { mutableStateOf(false) }
    var selectedType by remember(addressKey) {
        mutableStateOf(address?.addressType?.replaceFirstChar { it.uppercase() } ?: "Home")
    }
    var showError by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current

    // Handle "Use my mobile number" checkbox
    LaunchedEffect(useMyNumber) {
        if (useMyNumber && userPhoneNumber != null) {
            contactPhone = userPhoneNumber.replace("+91", "").replace(" ", "").trim()
        }
    }

    // Empty state if no address
    if (address == null) {
        EmptyState(
            icon = Icons.Default.Warning,
            title = "No location selected",
            actionText = "Go Back",
            onAction = onBack
        )
        return
    }

    val location = LatLng(
        address.latitude,
        address.longitude
    )

    // Determine screen title
    val screenTitle = when {
        isSaveAddressMode && isEditMode -> "Edit Address"
        isSaveAddressMode -> "Add New Address"
        locationType == "pickup" -> "Pickup Details"
        else -> "Drop Details"
    }

    // Determine button text
    val buttonText = when {
        isSaveAddressMode -> "Confirm and Save"
        locationType == "pickup" -> "Confirm & Add Drop Location"
        else -> "Confirm & Continue"
    }

    // Determine section header text
    val sectionHeaderText = when {
        isSaveAddressMode -> "Contact Details"
        locationType == "pickup" -> "Sender Details"
        else -> "Receiver Details"
    }

    // Determine name field label
    val nameFieldLabel = when {
        isSaveAddressMode -> "Name"
        locationType == "pickup" -> "Sender Name"
        else -> "Receiver Name"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = screenTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
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
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                // ============ MAP PREVIEW ============
                Box(modifier = Modifier.fillMaxWidth()) {
                    MapPreview(
                        location = location,
                        locationName = address.label,
                        onEdit = onEdit
                    )

                    // "This location will be saved" badge - only for save address mode
                    if (isSaveAddressMode) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 16.dp),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
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
                }

                // ============ ADDRESS DISPLAY CARD (for save mode) ============
                if (isSaveAddressMode) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .offset(y = (-16).dp),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Place,
                                contentDescription = null,
                                tint = AppColors.Primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = address.label,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AppColors.TextPrimary
                                )
                                Text(
                                    text = address.address,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AppColors.TextSecondary,
                                    maxLines = 2
                                )
                            }
                            TextButton(onClick = onEdit) {
                                Text(
                                    text = "Change",
                                    color = AppColors.Primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                } else {
                    Spacer(modifier = Modifier.height(16.dp))

                    // Address Summary Card (for booking flow)
                    LocationSummaryCard(
                        address = address,
                        locationType = locationType,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // ============ FORM FIELDS ============
                InfoCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    SectionHeader(text = sectionHeaderText)

                    Spacer(modifier = Modifier.height(16.dp))

                    // House/Apartment/Shop (Optional)
                    AddressInputField(
                        value = houseNumber,
                        onValueChange = { houseNumber = it },
                        label = if (isSaveAddressMode) "House / Apartment / Shop (optional)" else "House/Flat/Shop No., Floor",
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        )
                    )

                    // Pincode (Optional) - only for save address mode
                    if (isSaveAddressMode) {
                        Spacer(modifier = Modifier.height(16.dp))

                        AppTextField(
                            value = pincode,
                            onValueChange = {
                                if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                                    pincode = it
                                }
                            },
                            label = "Pincode (optional)",
                            placeholder = "Enter pincode",
                            leadingIcon = Icons.Default.PinDrop,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                                imeAction = androidx.compose.ui.text.input.ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            maxLength = 6
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Name (Required)
                    NameInputField(
                        value = contactName,
                        onValueChange = { contactName = it },
                        label = nameFieldLabel,
                        isError = showError && contactName.isBlank(),
                        errorMessage = "Name is required",
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Contact Number (Required)
                    PhoneNumberField(
                        value = contactPhone,
                        onValueChange = { contactPhone = it },
                        isError = showError && (contactPhone.isBlank() || contactPhone.length != 10),
                        errorMessage = if (contactPhone.isBlank()) "Phone is required" else "Enter 10 digit number",
                        keyboardActions = KeyboardActions(
                            onDone = { focusManager.clearFocus() }
                        )
                    )

                    // "Use my mobile number" checkbox - only for save address mode
                    if (isSaveAddressMode && userPhoneNumber != null) {
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
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
                                text = "Use my mobile number : $userPhoneNumber",
                                style = MaterialTheme.typography.bodyMedium,
                                color = AppColors.TextPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Save As Section
                    Text(
                        text = if (isSaveAddressMode) "Save address as :" else "Save As",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                        color = AppColors.TextSecondary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    SaveAsSelector(
                        selected = selectedType,
                        onSelected = { selectedType = it }
                    )
                }

                Spacer(modifier = Modifier.height(100.dp))
            }

            // ============ CONFIRM BUTTON ============
            Surface(
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                PrimaryButton(
                    text = buttonText,
                    onClick = {
                        if (contactName.isBlank() || contactPhone.length != 10) {
                            showError = true
                            return@PrimaryButton
                        }

                        val confirmedAddress = address.copy(
                            addressType = selectedType.lowercase(),
                            landmark = houseNumber.ifBlank { null },
                            contactName = contactName,
                            contactPhone = contactPhone,
                            label = selectedType
                        )
                        onConfirm(confirmedAddress)
                    },
                    icon = if (isSaveAddressMode) null else Icons.Default.ArrowForward,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            }
        }
    }
}