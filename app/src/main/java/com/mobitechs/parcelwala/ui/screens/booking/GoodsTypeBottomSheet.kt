// ui/screens/booking/GoodsTypeBottomSheet.kt
package com.mobitechs.parcelwala.ui.screens.booking

import androidx.compose.foundation.background
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
import com.mobitechs.parcelwala.data.model.response.GoodsTypeResponse
import com.mobitechs.parcelwala.ui.components.PrimaryButton
import com.mobitechs.parcelwala.ui.theme.AppColors
import com.mobitechs.parcelwala.ui.viewmodel.BookingViewModel

/**
 * Goods Type Bottom Sheet - FULL HEIGHT WITH FIXED STYLING
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoodsTypeBottomSheet(
    onDismiss: () -> Unit,
    onConfirm: (GoodsTypeResponse) -> Unit,
    viewModel: BookingViewModel = hiltViewModel()
) {
    var selectedGoodsType by remember { mutableStateOf<GoodsTypeResponse?>(null) }

    // Observe goods types from ViewModel
    val goodsTypes by viewModel.goodsTypes.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    // Load goods types if not already loaded
    LaunchedEffect(Unit) {
        if (goodsTypes.isEmpty()) {
            viewModel.loadGoodsTypes()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true // FULL HEIGHT
        ),
        dragHandle = {
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
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight(0.92f) // FULL HEIGHT
                .fillMaxWidth()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Select Goods Type",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    )
                    Text(
                        text = "Choose the type of goods you're shipping",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.TextSecondary
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = AppColors.TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (uiState.isLoading && goodsTypes.isEmpty()) {
                // Loading state
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AppColors.Primary)
                }
            } else {
                // Goods Type List - Scrollable
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                ) {
                    goodsTypes.forEach { goodsType ->
                        GoodsTypeCard(
                            goodsType = goodsType,
                            isSelected = selectedGoodsType?.goodsTypeId == goodsType.goodsTypeId,
                            onClick = { selectedGoodsType = goodsType }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Bottom Action Button - FIXED AT BOTTOM
            Surface(
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                PrimaryButton(
                    text = "Confirm Selection",
                    onClick = {
                        selectedGoodsType?.let { onConfirm(it) }
                    },
                    icon = Icons.Default.Check,
                    enabled = selectedGoodsType != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                )
            }
        }
    }
}

/**
 * Goods Type Card Item - FIXED: WHITE BACKGROUND WITH BORDER ONLY
 */
@Composable
private fun GoodsTypeCard(
    goodsType: GoodsTypeResponse,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White // ALWAYS WHITE BACKGROUND
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) AppColors.Primary else AppColors.Border
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(
                        color = if (isSelected)
                            AppColors.Primary.copy(alpha = 0.1f)
                        else
                            AppColors.Background,
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = goodsType.icon,
                    style = MaterialTheme.typography.headlineLarge
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = goodsType.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) AppColors.Primary else AppColors.TextPrimary,
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Default Values
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    InfoChip(
                        icon = Icons.Default.FitnessCenter,
                        text = "${goodsType.defaultWeight} kg"
                    )
                    InfoChip(
                        icon = Icons.Default.Inventory,
                        text = "${goodsType.defaultPackages} pkg"
                    )
                    InfoChip(
                        icon = Icons.Default.CurrencyRupee,
                        text = "${goodsType.defaultValue}"
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Selection Indicator
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = AppColors.Primary,
                    unselectedColor = AppColors.Border
                )
            )
        }
    }
}

/**
 * Info Chip for Goods Details
 */
@Composable
private fun InfoChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = AppColors.TextSecondary,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = AppColors.TextSecondary
        )
    }
}