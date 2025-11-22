package com.mobitechs.parcelwala.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mobitechs.parcelwala.R
import com.mobitechs.parcelwala.data.model.response.VehicleTypeResponse
import com.mobitechs.parcelwala.ui.components.*
import com.mobitechs.parcelwala.ui.theme.AppColors
import com.mobitechs.parcelwala.ui.viewmodel.HomeViewModel

@Composable
fun HomeScreen(
    onNavigateToLocationSearch: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.isLoading) {
        LoadingIndicator(message = "Loading...")
    }

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
            .background(AppColors.Background)
    ) {
        // GST Banner
        GSTBenefitsBanner()

        Spacer(modifier = Modifier.height(16.dp))

        // Pickup Location
        PickupLocationCard(
            location = uiState.pickupLocation,
            onClick = onNavigateToLocationSearch
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Vehicles
        if (uiState.vehicleTypes.isNotEmpty()) {
            VehicleTypesGrid(
                vehicleTypes = uiState.vehicleTypes,
                onVehicleSelected = { onNavigateToLocationSearch() }
            )
        } else if (!uiState.isLoading) {
            EmptyState(
                icon = Icons.Default.DirectionsCar,
                title = "No vehicles available",
                subtitle = "Please try again later",
                actionText = "Retry",
                onAction = { viewModel.refresh() },
                modifier = Modifier.padding(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        AnnouncementsSection()

        Spacer(modifier = Modifier.weight(1f))

        MarketingText()

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun GSTBenefitsBanner() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(
            containerColor = AppColors.Primary
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "DON'T MISS",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Text(
                    text = "GST Benefits!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            PrimaryButton(
                text = "Add GSTIN →",
                onClick = { /* TODO */ },
                modifier = Modifier.height(40.dp)
            )
        }
    }
}

@Composable
private fun PickupLocationCard(
    location: String,
    onClick: () -> Unit
) {
    InfoCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            LocationTypeIcon(
                locationType = "pickup",
                size = 40.dp
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Pick up from",
                    style = MaterialTheme.typography.labelMedium,
                    color = AppColors.TextSecondary
                )
                Text(
                    text = location,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = AppColors.TextPrimary,
                    maxLines = 1
                )
            }

            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Change",
                tint = AppColors.TextSecondary
            )
        }
    }
}

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
            VehicleCard(vehicle, onVehicleSelected)
        }
    }
}

@Composable
private fun VehicleCard(
    vehicle: VehicleTypeResponse,
    onClick: (VehicleTypeResponse) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable { onClick(vehicle) },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
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
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AppColors.Primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = vehicle.displayName,
                    modifier = Modifier.size(40.dp),
                    tint = AppColors.Primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = vehicle.displayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = AppColors.TextPrimary,
                maxLines = 2
            )
        }
    }
}

@Composable
private fun AnnouncementsSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        SectionHeader(
            text = "Announcements",
            modifier = Modifier.padding(vertical = 8.dp)
        )

        InfoCard {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = AppColors.Primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Introducing Loading-Unloading Service!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.TextPrimary
                    )
                }
                TextButton(onClick = { }) {
                    Text("View all", color = AppColors.Primary)
                }
            }
        }
    }
}

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
            color = AppColors.Primary.copy(alpha = 0.3f),
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Ho Jayega!",
            style = MaterialTheme.typography.displaySmall,
            color = AppColors.Primary.copy(alpha = 0.3f),
            fontWeight = FontWeight.Bold
        )
    }
}