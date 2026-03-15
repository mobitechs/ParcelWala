package com.mobitechs.parcelwala.ui.screens.account

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.outlined.LocationOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mobitechs.parcelwala.R
import com.mobitechs.parcelwala.data.model.request.SavedAddress
import com.mobitechs.parcelwala.ui.components.AppTopBar
import com.mobitechs.parcelwala.ui.components.EmptyState
import com.mobitechs.parcelwala.ui.components.LoadingIndicator
import com.mobitechs.parcelwala.ui.components.StatusBarScaffold
import com.mobitechs.parcelwala.ui.theme.AppColors
import com.mobitechs.parcelwala.ui.viewmodel.AccountViewModel

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

    // ── Delete confirmation dialog ─────────────────────────────────────────
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
            text = { Text(stringResource(R.string.delete_address_message)) },
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

    StatusBarScaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.saved_addresses),
                onBack = onBack,
                actions = {
                    OutlinedButton(
                        onClick = onAddAddress,
                        modifier = Modifier.padding(end = 8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = androidx.compose.ui.graphics.SolidColor(Color.White)
                        ),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            stringResource(R.string.add),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                // ── Info card lives inside the gradient ────────────────────
                extraContent = {
                    if (savedAddresses.isNotEmpty()) {
                        AddressInfoCard(count = savedAddresses.size)
                    }
                }
            )
        },
        containerColor = AppColors.Background
    ) { padding ->
        when {
            uiState.isLoadingAddresses -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    LoadingIndicator(message = stringResource(R.string.loading_addresses))
                }
            }

            savedAddresses.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
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
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(
                        start = 16.dp, end = 16.dp,
                        top = 16.dp, bottom = 100.dp
                    ),
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

                    // ── Dashed Add New button at bottom of list ────────────
                    item {
                        AddNewAddressButton(onClick = onAddAddress)
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// AddressInfoCard — frosted card inside gradient showing count
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun AddressInfoCard(count: Int) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.12f),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = Color.White.copy(alpha = 0.25f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column {
                Text(
                    text = "$count saved ${if (count == 1) "address" else "addresses"}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = stringResource(R.string.add_addresses_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.75f)
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// SavedAddressCard
// ══════════════════════════════════════════════════════════════════════════════

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
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // ── Header: icon + label + contact ────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AddressTypeIcon(
                    addressType = address.addressType,
                    modifier = Modifier.size(44.dp)
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = address.getDisplayLabel().uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary,
                        letterSpacing = androidx.compose.ui.unit.TextUnit.Unspecified
                    )
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

            Spacer(modifier = Modifier.height(10.dp))

            // ── Address text box ──────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(AppColors.Background)
                    .padding(10.dp)
            ) {
                Text(
                    text = address.address.ifEmpty { stringResource(R.string.no_address_details) },
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextSecondary,
                    maxLines = 2
                )
            }

            // ── Building details & landmark ───────────────────────────────
            if (!address.buildingDetails.isNullOrEmpty() || !address.landmark.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    address.buildingDetails?.takeIf { it.isNotEmpty() }?.let { details ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = AppColors.Primary.copy(alpha = 0.08f)
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
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = details,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AppColors.Primary
                                )
                            }
                        }
                    }
                    address.landmark?.takeIf { it.isNotEmpty() }?.let { lm ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = AppColors.TextSecondary.copy(alpha = 0.08f)
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
                                    modifier = Modifier.size(11.dp)
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

            Spacer(modifier = Modifier.height(10.dp))

            // ── Edit / Delete action row ───────────────────────────────────
            HorizontalDivider(color = AppColors.Divider, thickness = 0.5.dp)

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
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.edit),
                        color = AppColors.Primary,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                Box(
                    modifier = Modifier
                        .width(0.5.dp)
                        .height(36.dp)
                        .align(Alignment.CenterVertically)
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
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.delete),
                        color = AppColors.Drop,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// AddNewAddressButton — dashed outline CTA at bottom of list
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun AddNewAddressButton(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = AppColors.Primary.copy(alpha = 0.03f),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.5.dp,
            color = AppColors.Primary.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(AppColors.Primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = AppColors.Primary,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.add_address),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.Primary
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// AddressTypeIcon
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun AddressTypeIcon(
    addressType: String,
    modifier: Modifier = Modifier
) {
    val type = addressType.lowercase()

    val (icon, backgroundColor, iconTint) = when (type) {
        "home" -> Triple(
            Icons.Default.Home,
            AppColors.Primary.copy(alpha = 0.10f),
            AppColors.Primary
        )
        "shop", "work" -> Triple(
            Icons.Default.Store,
            Color(0xFFFFF3E0),
            Color(0xFFD4900A)
        )
        else -> Triple(
            Icons.Default.Place,
            AppColors.TextSecondary.copy(alpha = 0.10f),
            AppColors.TextSecondary
        )
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
            modifier = Modifier.size(22.dp)
        )
    }
}