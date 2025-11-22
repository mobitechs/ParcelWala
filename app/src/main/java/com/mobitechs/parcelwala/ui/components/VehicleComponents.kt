package com.mobitechs.parcelwala.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobitechs.parcelwala.ui.theme.AppColors

/**
 * Reusable Vehicle Selection Components
 */

/**
 * Vehicle Type Data Class
 */
data class VehicleType(
    val id: String,
    val name: String,
    val icon: String,
    val description: String,
    val capacity: String,
    val price: Int
)

/**
 * Vehicle Selection Card
 */
@Composable
fun VehicleSelectionCard(
    vehicle: VehicleType,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) AppColors.Primary else AppColors.Border,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onSelect),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                AppColors.Primary.copy(alpha = 0.05f)
            else
                Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Vehicle Icon
            VehicleIcon(icon = vehicle.icon)

            // Vehicle Details
            VehicleDetails(
                name = vehicle.name,
                description = vehicle.description,
                capacity = vehicle.capacity,
                modifier = Modifier.weight(1f)
            )

            // Price and Selection
            VehiclePriceSection(
                price = vehicle.price,
                isSelected = isSelected
            )
        }
    }
}

/**
 * Vehicle Icon Display
 */
@Composable
private fun VehicleIcon(
    icon: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(60.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(AppColors.Background),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = icon,
            fontSize = 32.sp
        )
    }
}

/**
 * Vehicle Information Section
 */
@Composable
private fun VehicleDetails(
    name: String,
    description: String,
    capacity: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = AppColors.TextPrimary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.TextSecondary,
            lineHeight = 16.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = capacity,
            style = MaterialTheme.typography.labelSmall,
            color = AppColors.Primary,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Vehicle Price and Selection Indicator
 */
@Composable
private fun VehiclePriceSection(
    price: Int,
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "₹$price",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = AppColors.Primary
            )
            Text(
                text = "Estimated",
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.TextSecondary,
                fontSize = 10.sp
            )
        }

        RadioButton(
            selected = isSelected,
            onClick = null,
            colors = RadioButtonDefaults.colors(
                selectedColor = AppColors.Primary,
                unselectedColor = AppColors.Border
            )
        )
    }
}

/**
 * Available Vehicles List
 */
val availableVehicles = listOf(
    VehicleType(
        id = "bike",
        name = "Bike",
        icon = "🏍️",
        description = "Small packages, documents",
        capacity = "Up to 10 kg",
        price = 50
    ),
    VehicleType(
        id = "3wheeler",
        name = "3 Wheeler",
        icon = "🛺",
        description = "Medium sized parcels",
        capacity = "Up to 50 kg",
        price = 120
    ),
    VehicleType(
        id = "tata_ace",
        name = "Tata Ace",
        icon = "🚛",
        description = "Furniture, appliances",
        capacity = "Up to 500 kg",
        price = 300
    ),
    VehicleType(
        id = "pickup",
        name = "Pickup Truck",
        icon = "🚙",
        description = "Large items, bulk goods",
        capacity = "Up to 750 kg",
        price = 450
    ),
    VehicleType(
        id = "tempo",
        name = "Tempo",
        icon = "🚚",
        description = "House shifting, bulk",
        capacity = "Up to 1500 kg",
        price = 800
    )
)