package com.mobitechs.parcelwala.ui.screens.booking

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.LatLng
import com.mobitechs.parcelwala.data.model.request.SavedAddress
import com.mobitechs.parcelwala.ui.components.*
import com.mobitechs.parcelwala.ui.theme.AppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddressConfirmationScreen(
    address: SavedAddress?,
    locationType: String,
    onConfirm: (SavedAddress) -> Unit,
    onEdit: () -> Unit,
    onBack: () -> Unit
) {
    // State
    var houseNumber by remember { mutableStateOf("") }
    var contactName by remember { mutableStateOf("") }
    var contactPhone by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("Home") }
    var showError by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current

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
        address.latitude ?: 19.0760,
        address.longitude ?: 72.8777
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (locationType == "pickup") "Pickup Details" else "Drop Details",
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
                // Map Preview
                MapPreview(
                    location = location,
                    locationName = address.label,
                    onEdit = onEdit
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Address Summary
                LocationSummaryCard(
                    address = address,
                    locationType = locationType,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Additional Info
                InfoCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    SectionHeader(
                        text = if (locationType == "pickup") "Sender Details" else "Receiver Details"
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    AddressInputField(
                        value = houseNumber,
                        onValueChange = { houseNumber = it },
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    NameInputField(
                        value = contactName,
                        onValueChange = { contactName = it },
                        label = if (locationType == "pickup") "Sender Name" else "Receiver Name",
                        isError = showError && contactName.isBlank(),
                        errorMessage = "Name is required",
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    PhoneNumberField(
                        value = contactPhone,
                        onValueChange = { contactPhone = it },
                        isError = showError && (contactPhone.isBlank() || contactPhone.length != 10),
                        errorMessage = if (contactPhone.isBlank()) "Phone is required" else "Enter 10 digit number",
                        keyboardActions = KeyboardActions(
                            onDone = { focusManager.clearFocus() }
                        )
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Save As",
                        style = MaterialTheme.typography.labelLarge,
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

            // Confirm Button
            Surface(
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                PrimaryButton(
                    text = if (locationType == "pickup")
                        "Confirm & Add Drop Location"
                    else
                        "Confirm & Continue",
                    onClick = {
                        if (contactName.isBlank() || contactPhone.length != 10) {
                            showError = true
                            return@PrimaryButton
                        }

                        val confirmedAddress = address.copy(
                            addressType = selectedType,
                            landmark = houseNumber,
                            contactName = contactName,
                            contactPhone = contactPhone,
                            label = selectedType
                        )
                        onConfirm(confirmedAddress)
                    },
                    icon = Icons.Default.ArrowForward,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            }
        }
    }
}