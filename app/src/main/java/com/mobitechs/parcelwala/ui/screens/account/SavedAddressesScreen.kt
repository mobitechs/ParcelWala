// ui/screens/account/SavedAddressesScreen.kt
package com.mobitechs.parcelwala.ui.screens.account

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mobitechs.parcelwala.R
import com.mobitechs.parcelwala.data.model.request.SavedAddress
import com.mobitechs.parcelwala.ui.components.EmptyState
import com.mobitechs.parcelwala.ui.components.LoadingIndicator
import com.mobitechs.parcelwala.ui.theme.AppColors
import com.mobitechs.parcelwala.ui.viewmodel.AccountViewModel

/**
 * Saved Addresses Screen
 * Displays list of saved addresses with edit and delete options
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedAddressesScreen(
    onBack: () -> Unit,
    onAddAddress: () -> Unit,
    onEditAddress: (SavedAddress) -> Unit,
    viewModel: AccountViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val savedAddresses by viewModel.savedAddresses.collectAsState()

    var addressToDelete by remember { mutableStateOf<SavedAddress?>(null) }

    // Delete confirmation dialog
    addressToDelete?.let { address ->
        AlertDialog(
            onDismissRequest = { addressToDelete = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = AppColors.Drop
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.delete_address_title),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(stringResource(R.string.delete_address_message))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteAddress(address.addressId)
                        addressToDelete = null
                    }
                ) {
                    Text(stringResource(R.string.delete), color = AppColors.Drop)
                }
            },
            dismissButton = {
                TextButton(onClick = { addressToDelete = null }) {
                    Text(stringResource(R.string.cancel), color = AppColors.TextSecondary)
                }
            },
            containerColor = Color.White
        )
    }

    LaunchedEffect(uiState.addressDeleteSuccess) {
        if (uiState.addressDeleteSuccess) {
            viewModel.clearAddressDeleteSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.saved_addresses),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
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
                actions = {
                    OutlinedButton(
                        onClick = onAddAddress,
                        modifier = Modifier.padding(end = 8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = AppColors.Primary
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = androidx.compose.ui.graphics.SolidColor(AppColors.Primary)
                        ),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.add),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        containerColor = AppColors.Background
    ) { padding ->
        when {
            uiState.isLoadingAddresses -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    LoadingIndicator(message = stringResource(R.string.loading_addresses))
                }
            }
            savedAddresses.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyState(
                        icon = Icons.Outlined.LocationOff,
                        title = stringResource(R.string.no_saved_addresses),
                        subtitle = stringResource(R.string.add_addresses_subtitle),
                        actionText = stringResource(R.string.add_address),
                        onAction = onAddAddress
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = savedAddresses,
                        key = { it.addressId }
                    ) { address ->
                        SavedAddressCard(
                            address = address,
                            onEdit = { onEditAddress(address) },
                            onDelete = { addressToDelete = address }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }
}

/**
 * Saved Address Card - Updated to use getDisplayLabel()
 */
@Composable
private fun SavedAddressCard(
    address: SavedAddress,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AddressTypeIcon(
                    addressType = address.addressType,
                    modifier = Modifier.size(48.dp)
                )

                Column(modifier = Modifier.weight(1f)) {
                    // Use getDisplayLabel() for proper "Other (label)" format
                    Text(
                        text = address.getDisplayLabel(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    )

                    // Contact info
                    val contactInfo = buildString {
                        address.contactName?.takeIf { it.isNotEmpty() }?.let { append(it) }
                        address.contactPhone?.takeIf { it.isNotEmpty() }?.let {
                            if (isNotEmpty()) append(" • ")
                            append(it)
                        }
                    }
                    if (contactInfo.isNotEmpty()) {
                        Text(
                            text = contactInfo,
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Address Text
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(AppColors.Background)
                    .padding(12.dp)
            ) {
                Text(
                    text = address.address.ifEmpty { stringResource(R.string.no_address_details) },
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextSecondary,
                    maxLines = 2
                )
            }

            // Building Details & Landmark if available
            if (!address.buildingDetails.isNullOrEmpty() || !address.landmark.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    address.buildingDetails?.let { details ->
                        if (details.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = AppColors.Primary.copy(alpha = 0.1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Apartment,
                                        contentDescription = null,
                                        tint = AppColors.Primary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = details,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AppColors.Primary
                                    )
                                }
                            }
                        }
                    }

                    address.landmark?.let { lm ->
                        if (lm.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = AppColors.TextSecondary.copy(alpha = 0.1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Place,
                                        contentDescription = null,
                                        tint = AppColors.TextSecondary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = lm,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AppColors.TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TextButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = AppColors.Primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.edit),
                        color = AppColors.Primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(24.dp)
                        .background(AppColors.Divider)
                )

                TextButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = AppColors.Drop,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.delete),
                        color = AppColors.Drop,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

/**
 * Address Type Icon
 */
@Composable
fun AddressTypeIcon(
    addressType: String,
    modifier: Modifier = Modifier
) {
    // Handle both capitalized and lowercase
    val type = addressType.lowercase()

    val (icon, backgroundColor) = when (type) {
        "home" -> Icons.Default.Home to AppColors.Primary.copy(alpha = 0.1f)
        "shop" -> Icons.Default.Store to AppColors.Pickup.copy(alpha = 0.1f)
        else -> Icons.Default.Place to AppColors.TextSecondary.copy(alpha = 0.1f)
    }

    val iconTint = when (type) {
        "home" -> AppColors.Primary
        "shop" -> AppColors.Pickup
        else -> AppColors.TextSecondary
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = addressType,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )
    }
}