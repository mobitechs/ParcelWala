// ui/screens/booking/RestrictedItemsBottomSheet.kt
package com.mobitechs.parcelwala.ui.screens.booking

import androidx.compose.foundation.background
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
import com.mobitechs.parcelwala.data.model.response.RestrictedItemResponse
import com.mobitechs.parcelwala.ui.components.InfoCard
import com.mobitechs.parcelwala.ui.theme.AppColors
import com.mobitechs.parcelwala.ui.viewmodel.BookingViewModel

/**
 * Restricted Items Bottom Sheet - FULL HEIGHT
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestrictedItemsBottomSheet(
    onDismiss: () -> Unit,
    viewModel: BookingViewModel = hiltViewModel()
) {
    // Observe restricted items from ViewModel
    val restrictedItems by viewModel.restrictedItems.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    // Load restricted items if not already loaded
    LaunchedEffect(Unit) {
        if (restrictedItems.isEmpty()) {
            viewModel.loadRestrictedItems()
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
                        text = "Restricted Items",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    )
                    Text(
                        text = "These items cannot be shipped",
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

            // Warning Banner
            InfoCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFFF9800),
                        modifier = Modifier.size(28.dp)
                    )
                    Column {
                        Text(
                            text = "Important Notice",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.TextPrimary
                        )
                        Text(
                            text = "Shipping restricted items may result in booking cancellation and legal action",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.isLoading && restrictedItems.isEmpty()) {
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
                // Restricted Items List
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(restrictedItems) { item ->
                        RestrictedItemCard(item)
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }

            // Bottom Button - FIXED AT BOTTOM
            Surface(
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.Primary
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "I Understand",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }
    }
}

/**
 * Restricted Item Card
 */
@Composable
private fun RestrictedItemCard(
    item: RestrictedItemResponse
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFF9F0)
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = Color(0xFFFFE0B2)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Warning Icon
            Icon(
                imageVector = Icons.Default.Block,
                contentDescription = null,
                tint = Color(0xFFFF6F00),
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Item Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary
                )

                if (item.description != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextSecondary
                    )
                }

                if (item.category != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = getCategoryColor(item.category).copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = item.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = getCategoryColor(item.category),
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Get color based on category
 */
private fun getCategoryColor(category: String): Color {
    return when (category.lowercase()) {
        "illegal" -> Color(0xFFD32F2F)
        "dangerous" -> Color(0xFFFF6F00)
        "restricted" -> Color(0xFFF57C00)
        "prohibited" -> Color(0xFFE64A19)
        else -> Color(0xFFFF9800)
    }
}