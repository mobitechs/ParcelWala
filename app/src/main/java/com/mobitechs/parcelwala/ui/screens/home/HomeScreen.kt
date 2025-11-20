package com.mobitechs.parcelwala.ui.screens.home


import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.mobitechs.parcelwala.R
import com.mobitechs.parcelwala.data.model.response.VehicleTypeResponse
import com.mobitechs.parcelwala.ui.components.ErrorView
import com.mobitechs.parcelwala.ui.components.LoadingDialog
import com.mobitechs.parcelwala.ui.viewmodel.HomeViewModel

/**
 * Home Screen with vehicle selection
 * Features:
 * - GST Benefits banner
 * - Pickup location selector
 * - Vehicle type grid
 * - Announcements
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToLocationSearch: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Show loading dialog
    if (uiState.isLoading) {
        LoadingDialog()
    }

    // Show error if any
    uiState.error?.let { error ->
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text("Error") },
            text = { Text(error) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearError() }) {
                    Text("OK")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // GST Benefits Banner
        GSTBenefitsBanner()

        Spacer(modifier = Modifier.height(16.dp))

        // Pickup Location Selector
        PickupLocationCard(
            location = uiState.pickupLocation,
            onClick = onNavigateToLocationSearch
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Vehicle Types Grid
        if (uiState.vehicleTypes.isNotEmpty()) {
            VehicleTypesGrid(
                vehicleTypes = uiState.vehicleTypes,
                onVehicleSelected = { vehicle ->
                    // Navigate to location search with selected vehicle
                    // For now, just navigate to location search
                    onNavigateToLocationSearch()
                }
            )
        } else if (!uiState.isLoading) {
            // Show empty state
                ErrorView(
                message = "No vehicle types available",
                onRetry = { viewModel.refresh() }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Announcements Section
        AnnouncementsSection()

        Spacer(modifier = Modifier.height(16.dp))

        // Marketing Text
        MarketingText()
    }
}

/**
 * GST Benefits Banner at top
 */
@Composable
private fun GSTBenefitsBanner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF2196F3)) // Blue background
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "DON'T MISS",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White
                )
                Text(
                    text = "GST Benefits!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Button(
                onClick = { /* TODO: Navigate to GST input */ },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFF2196F3)
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text("Add GSTIN →")
            }

            // GST folder icon
            Icon(
                painter = painterResource(id = R.drawable.ic_folder), // Add this icon ic_folder
                contentDescription = "GST",
                tint = Color(0xFFFFEB3B), // Yellow
                modifier = Modifier.size(64.dp)
            )
        }
    }
}

/**
 * Pickup location card
 */
@Composable
private fun PickupLocationCard(
    location: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_launcher_foreground), // Add this icon ic_location
                contentDescription = "Pickup",
                tint = Color(0xFF4CAF50), // Green
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Pick up from",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
                Text(
                    text = location,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1
                )
            }

            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Change",
                tint = Color.Gray
            )
        }
    }
}

/**
 * Vehicle types grid
 */
@Composable
private fun VehicleTypesGrid(
    vehicleTypes: List<VehicleTypeResponse>,
    onVehicleSelected: (VehicleTypeResponse) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(vehicleTypes) { vehicle ->
            VehicleCard(
                vehicle = vehicle,
                onClick = { onVehicleSelected(vehicle) }
            )
        }
    }
}

/**
 * Individual vehicle card
 */
@Composable
private fun VehicleCard(
    vehicle: VehicleTypeResponse,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF5F5F5)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Vehicle image placeholder
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(Color.White, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                // TODO: Load actual vehicle image
                Icon(
                    painter = painterResource(id = R.drawable.ic_vehicle), // Add this icon ic_vehicle
                    contentDescription = vehicle.displayName,
                    modifier = Modifier.size(48.dp),
                    tint = Color(0xFF2196F3)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = vehicle.displayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = Color.Black,
                maxLines = 2
            )
        }
    }
}

/**
 * Announcements section
 */
@Composable
private fun AnnouncementsSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Announcements",
            style = MaterialTheme.typography.titleMedium,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFF5F5F5)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_announcement), // Add this icon ic_announcement
                        contentDescription = "Announcement",
                        tint = Color(0xFFFF9800), // Orange
                        modifier = Modifier.size(32.dp)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = "Introducing Loading-Unloading Service!",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                TextButton(onClick = { /* TODO */ }) {
                    Text("View all")
                }
            }
        }
    }
}

/**
 * Marketing text at bottom
 */
@Composable
private fun MarketingText() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Delivery hai?",
            style = MaterialTheme.typography.displaySmall,
            color = Color(0xFFE3F2FD), // Light blue
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Ho Jayega!",
            style = MaterialTheme.typography.displaySmall,
            color = Color(0xFFE3F2FD), // Light blue
            fontWeight = FontWeight.Bold
        )
    }
}