package com.mobitechs.parcelwala.ui.screens.booking

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mobitechs.parcelwala.ui.components.*
import com.mobitechs.parcelwala.ui.theme.AppColors
import com.mobitechs.parcelwala.ui.viewmodel.BookingViewModel

/**
 * Coupons & Offers Screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CouponScreen(
    onBack: () -> Unit,
    onCouponApplied: (String) -> Unit,
    viewModel: BookingViewModel = hiltViewModel()
) {
    var couponCode by remember { mutableStateOf("") }
    val availableCoupons = remember { getCouponsList() }
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Coupons & Offers",
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
            // Coupon Input Section
            Surface(
                color = Color.White,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = couponCode,
                        onValueChange = { couponCode = it.uppercase() },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Enter coupon code") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (couponCode.isNotBlank()) {
                                    onCouponApplied(couponCode)
                                }
                            }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AppColors.Primary,
                            unfocusedBorderColor = AppColors.Border
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Button(
                        onClick = {
                            if (couponCode.isNotBlank()) {
                                onCouponApplied(couponCode)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.Primary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        enabled = couponCode.isNotBlank()
                    ) {
                        Text(
                            text = "APPLY",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Available Coupons List
            if (availableCoupons.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.LocalOffer,
                    title = "No active offers",
                    subtitle = "Please check back later",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(availableCoupons) { coupon ->
                        CouponCard(
                            coupon = coupon,
                            isApplied = uiState.appliedCoupon == coupon.code,
                            onApply = {
                                onCouponApplied(coupon.code)
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Coupon Card
 */
@Composable
private fun CouponCard(
    coupon: Coupon,
    isApplied: Boolean,
    onApply: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isApplied) 4.dp else 2.dp
        ),
        border = if (isApplied) {
            androidx.compose.foundation.BorderStroke(2.dp, AppColors.Primary)
        } else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Coupon Icon and Code
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                color = AppColors.Pickup.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalOffer,
                            contentDescription = null,
                            tint = AppColors.Pickup,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column {
                        Text(
                            text = coupon.code,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.TextPrimary
                        )
                        Text(
                            text = "Save ₹${coupon.discount}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = AppColors.Pickup
                        )
                    }
                }

                // Apply Button
                if (isApplied) {
                    StatusBadge(
                        text = "APPLIED",
                        backgroundColor = AppColors.Pickup,
                        textColor = Color.White
                    )
                } else {
                    TextButton(onClick = onApply) {
                        Text(
                            text = "APPLY",
                            color = AppColors.Primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Description
            Text(
                text = coupon.description,
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextSecondary
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Terms
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = AppColors.TextHint,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = coupon.terms,
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextHint
                )
            }

            if (coupon.expiryDate != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = AppColors.Drop,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Valid till ${coupon.expiryDate}",
                        style = MaterialTheme.typography.labelSmall,
                        color = AppColors.Drop,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * Coupon Data Class
 */
data class Coupon(
    val code: String,
    val description: String,
    val discount: Int,
    val terms: String,
    val expiryDate: String? = null
)

/**
 * Get Coupons List (Mock Data)
 */
private fun getCouponsList(): List<Coupon> {
    // Return empty for "No active offers" state
    // Uncomment below for sample coupons

    return listOf(
        Coupon(
            code = "FIRST50",
            description = "Get ₹50 off on your first booking",
            discount = 50,
            terms = "Valid on orders above ₹200. First time users only.",
            expiryDate = "31 Dec 2024"
        ),
        Coupon(
            code = "SAVE100",
            description = "Flat ₹100 off on all bookings",
            discount = 100,
            terms = "Valid on orders above ₹500. Use once per user.",
            expiryDate = "15 Dec 2024"
        ),
        Coupon(
            code = "WEEKEND20",
            description = "Get 20% off on weekend bookings",
            discount = 20,
            terms = "Valid on Saturdays and Sundays only. Max discount ₹150.",
            expiryDate = null
        )
    )
}