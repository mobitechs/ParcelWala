// ui/screens/payments/PaymentsScreen.kt
package com.mobitechs.parcelwala.ui.screens.payments

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mobitechs.parcelwala.ui.components.EmptyState
import com.mobitechs.parcelwala.ui.theme.AppColors

/**
 * Payments Screen
 * Placeholder for payments/wallet feature
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentsScreen() {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Payments",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        containerColor = AppColors.Background
    ) { paddingValues ->
        EmptyState(
            icon = Icons.Default.Payment,
            title = "Payment Methods",
            subtitle = "Manage your payment methods and wallet",
            actionText = "Add Payment Method",
            onAction = { /* Add payment method */ },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(32.dp)
        )
    }
}