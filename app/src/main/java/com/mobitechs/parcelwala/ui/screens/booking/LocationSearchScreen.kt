package com.mobitechs.parcelwala.ui.screens.booking

// ui/screens/booking/LocationSearchScreen.kt
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.mobitechs.parcelwala.ui.viewmodel.LocationSearchViewModel

/**
 * Location Search Screen
 * For selecting pickup or drop location
 *
 * @param locationType "pickup" or "drop"
 * @param onAddressSelected Callback when address is selected
 * @param onBack Back navigation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationSearchScreen(
    locationType: String,
    onAddressSelected: (SavedAddress) -> Unit,
    onBack: () -> Unit,
    viewModel: LocationSearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.White)
        ) {
            // Search bar
            SearchBar(
                query = uiState.searchQuery,
                onQueryChange = { viewModel.updateSearchQuery(it) },
                locationType = locationType
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Saved Addresses
            if (uiState.savedAddresses.isNotEmpty()) {
                SavedAddressesSection(
                    addresses = uiState.savedAddresses,
                    onAddressClick = { address ->
                        viewModel.selectAddress(address)
                        onAddressSelected(address)
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Recent Pickups
            if (uiState.recentPickups.isNotEmpty()) {
                RecentPickupsSection(
                    pickups = uiState.recentPickups,
                    onPickupClick = { address ->
                        viewModel.selectAddress(address)
                        onAddressSelected(address)
                    }
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Bottom actions
            BottomActions(
                onUseCurrentLocation = { /* TODO: Get current location */ },
                onLocateOnMap = { /* TODO: Open map picker */ }
            )
        }
    }
}

/**
 * Search bar component
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    locationType: String
) {
    val placeholder = when (locationType) {
        "pickup" -> "Where is your PickUp ?"
        "drop" -> "Where to deliver ?"
        else -> "Enter location"
    }

    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        placeholder = { Text(placeholder) },
        leadingIcon = {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(
                        color = if (locationType == "pickup") Color(0xFF4CAF50) else Color(0xFFFF5722),
                        shape = RoundedCornerShape(4.dp)
                    )
            )
        },
        trailingIcon = {
            IconButton(onClick = { /* TODO: Voice search */ }) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Voice search",
                    tint = Color(0xFF2196F3)
                )
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFF2196F3),
            unfocusedBorderColor = Color.LightGray
        ),
        shape = RoundedCornerShape(12.dp)
    )
}

/**
 * Saved addresses section
 */
@Composable
private fun SavedAddressesSection(
    addresses: List<SavedAddress>,
    onAddressClick: (SavedAddress) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "Saved",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Saved Addresses",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = "View all"
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        addresses.take(1).forEach { address ->
            AddressItem(
                address = address,
                onClick = { onAddressClick(address) }
            )
        }
    }
}

/**
 * Recent pickups section
 */
@Composable
private fun RecentPickupsSection(
    pickups: List<SavedAddress>,
    onPickupClick: (SavedAddress) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Recent pickups",
            style = MaterialTheme.typography.titleSmall,
            color = Color.Gray,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn {
            items(pickups) { pickup ->
                AddressItem(
                    address = pickup,
                    onClick = { onPickupClick(pickup) }
                )
            }
        }
    }
}

/**
 * Individual address item
 */
@Composable
private fun AddressItem(
    address: SavedAddress,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.Default.AccessTime,
            contentDescription = "Recent",
            modifier = Modifier.size(24.dp),
            tint = Color.Gray
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = address.label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = address.address,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                maxLines = 2
            )
            address.contactName?.let { name ->
                Text(
                    text = "$name • ${address.contactPhone}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }

        IconButton(onClick = { /* TODO: Save address */ }) {
            Icon(
                imageVector = Icons.Default.FavoriteBorder,
                contentDescription = "Save",
                tint = Color.Gray
            )
        }
    }

    Divider(color = Color.LightGray, thickness = 0.5.dp)
}

/**
 * Bottom actions
 */
@Composable
private fun BottomActions(
    onUseCurrentLocation: () -> Unit,
    onLocateOnMap: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = onUseCurrentLocation,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color(0xFF2196F3)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MyLocation,
                contentDescription = "Current location"
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Use current location")
        }

        Button(
            onClick = onLocateOnMap,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color(0xFF2196F3)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Place,
                contentDescription = "Map"
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Locate on the Map")
        }
    }
}