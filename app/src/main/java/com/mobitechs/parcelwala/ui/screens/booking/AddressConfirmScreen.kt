package com.mobitechs.parcelwala.ui.screens.booking


import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mobitechs.parcelwala.data.model.request.SavedAddress
import com.mobitechs.parcelwala.ui.viewmodel.AddressConfirmViewModel

/**
 * Address Confirmation Screen
 * For confirming and adding details to selected address
 *
 * @param address Pre-selected address
 * @param locationType "pickup" or "drop"
 * @param onConfirm Callback when address is confirmed
 * @param onBack Back navigation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddressConfirmScreen(
    address: SavedAddress,
    locationType: String,
    onConfirm: (SavedAddress) -> Unit,
    onBack: () -> Unit,
    viewModel: AddressConfirmViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Initialize with address
    LaunchedEffect(address) {
        viewModel.initializeWithAddress(address)
    }

    // Show error if any
    uiState.error?.let { error ->
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text("Validation Error") },
            text = { Text(error) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearError() }) {
                    Text("OK")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO: Show address on map */ }) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Location",
                            tint = Color(0xFF2196F3)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp
            ) {
                Button(
                    onClick = {
                        viewModel.confirmAddress { confirmedAddress ->
                            onConfirm(confirmedAddress)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2196F3)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Confirm And Proceed",
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Map view placeholder (in real app, show Google Maps)
            MapPlaceholder(address = uiState.address)

            Spacer(modifier = Modifier.height(16.dp))

            // Address details form
            AddressDetailsForm(
                uiState = uiState,
                onFlatBuildingChange = { viewModel.updateFlatBuilding(it) },
                onSenderNameChange = { viewModel.updateSenderName(it) },
                onSenderMobileChange = { viewModel.updateSenderMobile(it) },
                onUseMyMobile = { viewModel.useMyMobile("8655883062") }, // TODO: Get from user session
                onSaveAsChange = { viewModel.setSaveAs(it) },
                onSaveLabelChange = { viewModel.setSaveLabel(it) }
            )
        }
    }
}

/**
 * Map placeholder
 */
@Composable
private fun MapPlaceholder(address: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(Color(0xFFE0E0E0)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Place,
                contentDescription = "Map",
                modifier = Modifier.size(48.dp),
                tint = Color(0xFF4CAF50)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Your goods will be picked from here",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

/**
 * Address details form
 */
@Composable
private fun AddressDetailsForm(
    uiState: com.mobitechs.parcelwala.ui.viewmodel.AddressConfirmUiState,
    onFlatBuildingChange: (String) -> Unit,
    onSenderNameChange: (String) -> Unit,
    onSenderMobileChange: (String) -> Unit,
    onUseMyMobile: () -> Unit,
    onSaveAsChange: (String) -> Unit,
    onSaveLabelChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Address display with change button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = "Location",
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = uiState.address.split(",").take(2).joinToString(", "),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = uiState.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        maxLines = 2
                    )
                }
            }

            TextButton(onClick = { /* TODO: Change address */ }) {
                Text("Change", color = Color(0xFF2196F3))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Flat/Building number
        OutlinedTextField(
            value = uiState.flatBuilding,
            onValueChange = onFlatBuildingChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("House / Apartment / Shop (optional)") },
            placeholder = { Text("be block") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF2196F3),
                unfocusedBorderColor = Color.LightGray
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Sender name
        OutlinedTextField(
            value = uiState.senderName,
            onValueChange = onSenderNameChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Sender's Name") },
            trailingIcon = {
                IconButton(onClick = { /* TODO: Select from contacts */ }) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Contacts",
                        tint = Color(0xFF2196F3)
                    )
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF2196F3),
                unfocusedBorderColor = Color.LightGray
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Sender mobile
        OutlinedTextField(
            value = uiState.senderMobile,
            onValueChange = onSenderMobileChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Sender's Mobile number") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF2196F3),
                unfocusedBorderColor = Color.LightGray
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Use my mobile checkbox
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = false, // TODO: Implement state
                onCheckedChange = { if (it) onUseMyMobile() }
            )
            Text(
                text = "Use my mobile number : 8655883062",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Save as section
        Text(
            text = "Save as (optional):",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SaveAsChip(
                label = "Home",
                icon = Icons.Default.Home,
                isSelected = uiState.saveAs == "home",
                onClick = { onSaveAsChange("home") }
            )
            SaveAsChip(
                label = "Shop",
                icon = Icons.Default.Business,
                isSelected = uiState.saveAs == "shop",
                onClick = { onSaveAsChange("shop") }
            )
            SaveAsChip(
                label = "Other",
                icon = Icons.Default.LocationOn,
                isSelected = uiState.saveAs == "other",
                onClick = { onSaveAsChange("other") }
            )
        }

        // If "Other" is selected, show label input
        if (uiState.saveAs == "other") {
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = uiState.saveLabel,
                onValueChange = onSaveLabelChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Enter label (e.g., Office, Friend's place)") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF2196F3),
                    unfocusedBorderColor = Color.LightGray
                )
            )
        }
    }
}

/**
 * Save as chip button
 */
@Composable
private fun SaveAsChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) Color(0xFF2196F3) else Color.White,
            contentColor = if (isSelected) Color.White else Color.Gray
        ),
        border = if (!isSelected) BorderStroke(1.dp, Color.LightGray) else null,
        shape = RoundedCornerShape(24.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(label)
    }
}