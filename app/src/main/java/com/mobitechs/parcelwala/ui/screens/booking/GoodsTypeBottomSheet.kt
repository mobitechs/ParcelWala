package com.mobitechs.parcelwala.ui.screens.booking

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mobitechs.parcelwala.ui.components.*
import com.mobitechs.parcelwala.ui.theme.AppColors

/**
 * Goods Type Selection Bottom Sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoodsTypeBottomSheet(
    onDismiss: () -> Unit,
    onConfirm: (GoodsType) -> Unit
) {
    var selectedGoods by remember { mutableStateOf<GoodsType?>(null) }
    var weight by remember { mutableStateOf("20.0") }
    var packages by remember { mutableStateOf("01") }
    var value by remember { mutableStateOf("1500") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        dragHandle = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Drag Handle
                Box(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .width(40.dp)
                        .height(4.dp)
                        .background(
                            color = AppColors.Border,
                            shape = RoundedCornerShape(2.dp)
                        )
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Select Goods Type",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary
                )

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = AppColors.TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Search Field
            SearchField(
                value = "",
                onValueChange = { },
                placeholder = "Search goods type",
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Goods Types List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(availableGoodsTypes) { goodsType ->
                    GoodsTypeItem(
                        goodsType = goodsType,
                        isSelected = selectedGoods == goodsType,
                        onClick = { selectedGoods = goodsType }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Selected Goods Details
            selectedGoods?.let { goods ->
                InfoCard(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = 4.dp
                ) {
                    Text(
                        text = "Package Details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Weight Input
                        OutlinedTextField(
                            value = weight,
                            onValueChange = { if (it.matches(Regex("^\\d*\\.?\\d*$"))) weight = it },
                            label = { Text("Weight (Kg)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AppColors.Primary,
                                focusedLabelColor = AppColors.Primary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )

                        // Packages Input
                        OutlinedTextField(
                            value = packages,
                            onValueChange = { if (it.matches(Regex("^\\d{0,2}$"))) packages = it },
                            label = { Text("Packages") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AppColors.Primary,
                                focusedLabelColor = AppColors.Primary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Value Input
                    OutlinedTextField(
                        value = value,
                        onValueChange = { if (it.matches(Regex("^\\d*$"))) value = it },
                        label = { Text("Declared Value (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AppColors.Primary,
                            focusedLabelColor = AppColors.Primary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "(Default)",
                        style = MaterialTheme.typography.labelSmall,
                        color = AppColors.TextHint
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Disclaimer
                Text(
                    text = "Disclaimer: Porter shall bear limited liability whatsoever for loss, damage, deterioration, delay, or regulatory non-compliance arising from the transportation of the listed item. Senders discretion is advised.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextHint,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Confirm Button
                PrimaryButton(
                    text = "Confirm",
                    onClick = {
                        selectedGoods?.let {
                            onConfirm(it.copy(
                                weight = weight.toDoubleOrNull() ?: 20.0,
                                packages = packages.toIntOrNull() ?: 1,
                                value = value.toIntOrNull() ?: 1500
                            ))
                        }
                    },
                    icon = Icons.Default.Check,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

/**
 * Goods Type Item Card
 */
@Composable
private fun GoodsTypeItem(
    goodsType: GoodsType,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) AppColors.Primary.copy(alpha = 0.1f) else Color.White
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) AppColors.Primary else AppColors.Border
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = goodsType.icon,
                contentDescription = goodsType.name,
                tint = if (isSelected) AppColors.Primary else AppColors.TextSecondary,
                modifier = Modifier.size(24.dp)
            )

            Text(
                text = goodsType.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) AppColors.Primary else AppColors.TextPrimary,
                modifier = Modifier.weight(1f)
            )

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = AppColors.Primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

/**
 * Goods Type Data Class
 */
data class GoodsType(
    val id: String,
    val name: String,
    val icon: ImageVector,
    val weight: Double = 20.0,
    val packages: Int = 1,
    val value: Int = 1500
)

/**
 * Available Goods Types
 */
val availableGoodsTypes = listOf(
    GoodsType(
        id = "food",
        name = "Homemade / Prepared / Fresh Food",
        icon = Icons.Default.Restaurant
    ),
    GoodsType(
        id = "furniture",
        name = "Furnitures / Home Furnishings",
        icon = Icons.Default.Chair
    ),
    GoodsType(
        id = "general",
        name = "General Goods",
        icon = Icons.Default.Inventory
    ),
    GoodsType(
        id = "hardware",
        name = "Hardwares",
        icon = Icons.Default.Build
    ),
    GoodsType(
        id = "shifting",
        name = "House Shifting / Packers and Movers",
        icon = Icons.Default.Home
    ),
    GoodsType(
        id = "logistics",
        name = "Logistics Service Providers",
        icon = Icons.Default.LocalShipping
    ),
    GoodsType(
        id = "machines",
        name = "Machines / Equipments / Spare Parts",
        icon = Icons.Default.Settings
    )
)