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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mobitechs.parcelwala.R
import com.mobitechs.parcelwala.data.model.request.SavedAddress
import com.mobitechs.parcelwala.ui.components.AppTopBar
import com.mobitechs.parcelwala.ui.components.EmptyState
import com.mobitechs.parcelwala.ui.components.LoadingIndicator
import com.mobitechs.parcelwala.ui.components.StatusBarScaffold
import com.mobitechs.parcelwala.ui.theme.AppColors
import com.mobitechs.parcelwala.ui.viewmodel.AccountViewModel

// ── Address type grouping keys ────────────────────────────────────────────────

private const val GROUP_HOME_WORK = "home_work"
private const val GROUP_OTHERS    = "others"

private fun SavedAddress.groupKey(): String {
    val t = addressType.lowercase()
    return if (t == "home" || t == "work" || t == "shop") GROUP_HOME_WORK else GROUP_OTHERS
}

// ══════════════════════════════════════════════════════════════════════════════
// SavedAddressesScreen
// ══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedAddressesScreen(
    onBack: () -> Unit,
    onAddAddress: () -> Unit,
    onEditAddress: (SavedAddress) -> Unit,
    viewModel: AccountViewModel = hiltViewModel()
) {
    val uiState         by viewModel.uiState.collectAsState()
    val savedAddresses  by viewModel.savedAddresses.collectAsState()
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
                TextButton(onClick = {
                    viewModel.deleteAddress(address.addressId)
                    addressToDelete = null
                }) {
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
        if (uiState.addressDeleteSuccess) viewModel.clearAddressDeleteSuccess()
    }

    StatusBarScaffold(
        topBar = {
            AppTopBar(
                title   = stringResource(R.string.saved_addresses),
                onBack  = onBack,
                actions = {
                    OutlinedButton(
                        onClick         = onAddAddress,
                        modifier        = Modifier.padding(end = 8.dp),
                        colors          = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border          = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = SolidColor(Color.White.copy(alpha = 0.60f))
                        ),
                        shape           = RoundedCornerShape(20.dp),
                        contentPadding  = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            stringResource(R.string.add),
                            fontWeight = FontWeight.SemiBold,
                            fontSize   = 13.sp
                        )
                    }
                },
                extraContent = {
                    // Frosted count card — only when there are addresses
                    if (savedAddresses.isNotEmpty()) {
                        AddressCountCard(count = savedAddresses.size)
                    }
                }
            )
        },
        containerColor = AppColors.Background
    ) { padding ->
        when {
            uiState.isLoadingAddresses -> {
                Box(
                    modifier          = Modifier.fillMaxSize().padding(padding),
                    contentAlignment  = Alignment.Center
                ) {
                    LoadingIndicator(message = stringResource(R.string.loading_addresses))
                }
            }

            savedAddresses.isEmpty() -> {
                Box(
                    modifier          = Modifier.fillMaxSize().padding(padding),
                    contentAlignment  = Alignment.Center
                ) {
                    EmptyState(
                        icon        = Icons.Outlined.LocationOff,
                        title       = stringResource(R.string.no_saved_addresses),
                        subtitle    = stringResource(R.string.add_addresses_subtitle),
                        actionText  = stringResource(R.string.add_address),
                        onAction    = onAddAddress
                    )
                }
            }

            else -> {
                // ── Group addresses ────────────────────────────────────────
                val grouped = savedAddresses.groupBy { it.groupKey() }
                val homeWork = grouped[GROUP_HOME_WORK].orEmpty()
                val others   = grouped[GROUP_OTHERS].orEmpty()

                LazyColumn(
                    modifier       = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(
                        start   = 16.dp, end = 16.dp,
                        top     = 16.dp, bottom = 100.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // ── Home & Work group ──────────────────────────────────
                    if (homeWork.isNotEmpty()) {
                        item {
                            GroupCard(
                                label     = "Home & Work",
                                addresses = homeWork,
                                onEdit    = onEditAddress,
                                onDelete  = { addressToDelete = it }
                            )
                        }
                    }

                    // ── Others group ───────────────────────────────────────
                    if (others.isNotEmpty()) {
                        item {
                            GroupCard(
                                label     = "Others",
                                addresses = others,
                                onEdit    = onEditAddress,
                                onDelete  = { addressToDelete = it }
                            )
                        }
                    }

                    // ── Add new button ─────────────────────────────────────
                    item { AddNewAddressButton(onClick = onAddAddress) }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// AddressCountCard — sits inside gradient header
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun AddressCountCard(count: Int) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
        shape  = RoundedCornerShape(14.dp),
        color  = Color.White.copy(alpha = 0.12f),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = Color.White.copy(alpha = 0.25f)
        )
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier         = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint        = Color.White,
                    modifier    = Modifier.size(20.dp)
                )
            }
            Column {
                Text(
                    text       = "$count saved ${if (count == 1) "address" else "addresses"}",
                    style      = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color      = Color.White
                )
                Text(
                    text  = stringResource(R.string.add_addresses_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.70f)
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// GroupCard — white rounded card wrapping a labelled section of addresses
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun GroupCard(
    label: String,
    addresses: List<SavedAddress>,
    onEdit: (SavedAddress) -> Unit,
    onDelete: (SavedAddress) -> Unit
) {
    Surface(
        modifier        = Modifier.fillMaxWidth(),
        shape           = RoundedCornerShape(16.dp),
        color           = Color.White,
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {

            // ── Section label ──────────────────────────────────────────────
            Text(
                text     = label.uppercase(),
                style    = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 0.7.sp,
                    fontSize      = 9.sp
                ),
                fontWeight = FontWeight.Bold,
                color      = AppColors.TextSecondary,
                modifier   = Modifier.padding(
                    start  = 14.dp, end = 14.dp,
                    top    = 10.dp, bottom = 4.dp
                )
            )

            // ── Address rows ───────────────────────────────────────────────
            addresses.forEachIndexed { index, address ->
                AddressRow(
                    address  = address,
                    onEdit   = { onEdit(address) },
                    onDelete = { onDelete(address) }
                )
                // Divider between rows — not after the last one
                if (index < addresses.lastIndex) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(0.5.dp)
                            .padding(start = 60.dp, end = 14.dp)
                            .background(AppColors.Divider)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// AddressRow — compact single row: circle icon + texts + edit/delete buttons
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun AddressRow(
    address: SavedAddress,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ── Circle type icon ───────────────────────────────────────────────
        AddressTypeIcon(
            addressType = address.addressType,
            modifier    = Modifier.size(36.dp)
        )

        // ── Name + contact badge + address line ────────────────────────────
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // Name row — label + optional contact badge
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text       = address.getDisplayLabel().replaceFirstChar { it.uppercase() },
                    style      = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                    fontWeight = FontWeight.SemiBold,
                    color      = AppColors.TextPrimary
                )
                // Contact name badge — shown when available
                address.contactName?.takeIf { it.isNotEmpty() }?.let { name ->
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = AppColors.Background
                    ) {
                        Text(
                            text     = name.take(10) + if (name.length > 10) "…" else "",
                            style    = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                            color    = AppColors.TextSecondary,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Address line — truncated to 1 line
            Text(
                text     = address.address,
                style    = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color    = AppColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Building / landmark context line — only when available
            val contextLine = buildList {
                address.buildingDetails?.takeIf { it.isNotEmpty() }?.let { add(it) }
                address.landmark?.takeIf { it.isNotEmpty() }?.let { add(it) }
            }.joinToString(" · ")

            if (contextLine.isNotEmpty()) {
                Text(
                    text     = contextLine,
                    style    = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color    = AppColors.Primary.copy(alpha = 0.80f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // ── Edit + Delete icon buttons ─────────────────────────────────────
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            // Edit
            Surface(
                onClick = onEdit,
                shape   = RoundedCornerShape(9.dp),
                color   = AppColors.Primary.copy(alpha = 0.08f)
            ) {
                Icon(
                    imageVector        = Icons.Default.Edit,
                    contentDescription = stringResource(R.string.edit),
                    tint               = AppColors.Primary,
                    modifier           = Modifier
                        .padding(7.dp)
                        .size(15.dp)
                )
            }
            // Delete
            Surface(
                onClick = onDelete,
                shape   = RoundedCornerShape(9.dp),
                color   = AppColors.Drop.copy(alpha = 0.08f)
            ) {
                Icon(
                    imageVector        = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.delete),
                    tint               = AppColors.Drop,
                    modifier           = Modifier
                        .padding(7.dp)
                        .size(15.dp)
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// AddNewAddressButton — dashed CTA below the list
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun AddNewAddressButton(onClick: () -> Unit) {
    Surface(
        onClick  = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        color    = AppColors.Primary.copy(alpha = 0.03f),
        border   = androidx.compose.foundation.BorderStroke(
            width = 1.5.dp,
            color = AppColors.Primary.copy(alpha = 0.35f)
        )
    ) {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            // Plus icon in a small rounded box
            Box(
                modifier         = Modifier
                    .size(26.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(AppColors.Primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = Icons.Default.Add,
                    contentDescription = null,
                    tint               = AppColors.Primary,
                    modifier           = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text       = stringResource(R.string.add_address),
                    style      = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                    fontWeight = FontWeight.SemiBold,
                    color      = AppColors.Primary
                )
                Text(
                    text  = "Home, work, or custom",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = AppColors.TextSecondary
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// AddressTypeIcon — circular icon per address type
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun AddressTypeIcon(
    addressType: String,
    modifier: Modifier = Modifier
) {
    val type = addressType.lowercase()

    data class IconConfig(
        val icon: androidx.compose.ui.graphics.vector.ImageVector,
        val bg: Color,
        val tint: Color
    )

    val config = when (type) {
        "home"        -> IconConfig(Icons.Default.Home,      Color(0xFFECFDF5), Color(0xFF059669))
        "work", "shop"-> IconConfig(Icons.Default.Store,     Color(0xFFFEF9C3), Color(0xFFD97706))
        "apartment"   -> IconConfig(Icons.Default.Apartment, Color(0xFFEEF2FF), Color(0xFF4F46E5))
        else          -> IconConfig(Icons.Default.Place,     AppColors.Background, AppColors.TextSecondary)
    }

    Box(
        modifier         = modifier
            .clip(CircleShape)
            .background(config.bg),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector        = config.icon,
            contentDescription = addressType,
            tint               = config.tint,
            modifier           = Modifier.size(18.dp)
        )
    }
}