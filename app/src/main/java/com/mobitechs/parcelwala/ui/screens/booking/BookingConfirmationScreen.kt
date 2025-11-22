package com.mobitechs.parcelwala.ui.screens.booking

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mobitechs.parcelwala.data.model.request.SavedAddress
import com.mobitechs.parcelwala.ui.components.*
import com.mobitechs.parcelwala.ui.theme.AppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingConfirmationScreen(
    pickupAddress: SavedAddress?,
    dropAddress: SavedAddress?,
    onConfirmBooking: () -> Unit,
    onBack: () -> Unit
) {
    var selectedVehicle by remember { mutableStateOf<VehicleType?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Confirm Booking",
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
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                // Locations Summary
                item {
                    InfoCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                        pickupAddress?.let {
                            LocationDetailItem(
                                address = it,
                                type = "Pickup",
                                color = AppColors.Pickup
                            )
                        }

                        JourneyConnector()

                        dropAddress?.let {
                            LocationDetailItem(
                                address = it,
                                type = "Drop",
                                color = AppColors.Drop
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Vehicle Selection
                item {
                    SectionHeader(
                        text = "Select Vehicle",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                items(availableVehicles) { vehicle ->
                    VehicleSelectionCard(
                        vehicle = vehicle,
                        isSelected = selectedVehicle == vehicle,
                        onSelect = { selectedVehicle = vehicle },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                item {
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }

            // Confirm Button
            Surface(
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    if (selectedVehicle != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Total Amount",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = AppColors.TextSecondary
                                )
                                Text(
                                    text = "₹${selectedVehicle!!.price}",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = AppColors.Primary
                                )
                            }
                            Text(
                                text = "Cash on Delivery",
                                style = MaterialTheme.typography.bodyMedium,
                                color = AppColors.TextSecondary
                            )
                        }
                    }

                    PrimaryButton(
                        text = "Confirm Booking",
                        onClick = onConfirmBooking,
                        enabled = selectedVehicle != null,
                        icon = Icons.Default.CheckCircle,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}