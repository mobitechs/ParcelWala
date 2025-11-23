// ui/screens/booking/CouponScreen.kt
package com.mobitechs.parcelwala.ui.screens.booking

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import com.mobitechs.parcelwala.data.model.response.CouponResponse
import com.mobitechs.parcelwala.ui.components.EmptyState
import com.mobitechs.parcelwala.ui.components.InfoCard
import com.mobitechs.parcelwala.ui.components.LoadingIndicator
import com.mobitechs.parcelwala.ui.theme.AppColors
import com.mobitechs.parcelwala.ui.viewmodel.BookingViewModel

/**
 * Coupon Screen
 * Shows available coupons and allows manual code entry
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CouponScreen(
    onBack: () -> Unit,
    onCouponApplied: (String) -> Unit,
    viewModel: BookingViewModel = hiltViewModel()
) {
    var manualCouponCode by remember { mutableStateOf("") }
    var isApplying by remember { mutableStateOf(false) }

    // Observe coupons from ViewModel
    val availableCoupons by viewModel.availableCoupons.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    // Load coupons if not already loaded
    LaunchedEffect(Unit) {
        if (availableCoupons.isEmpty()) {
            viewModel.loadAvailableCoupons()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Apply Coupon",
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading && availableCoupons.isEmpty() && !isApplying) {
                LoadingIndicator(
                    message = "Loading coupons...",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp)
                )
            } else {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Manual Coupon Entry
                    InfoCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Have a coupon code?",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.TextPrimary
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = manualCouponCode,
                                onValueChange = { manualCouponCode = it.uppercase() },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("Enter code") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Characters,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        if (manualCouponCode.isNotBlank()) {
                                            isApplying = true
                                            viewModel.applyCoupon(manualCouponCode)
                                        }
                                    }
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AppColors.Primary,
                                    unfocusedBorderColor = AppColors.Border
                                )
                            )

                            Button(
                                onClick = {
                                    if (manualCouponCode.isNotBlank()) {
                                        isApplying = true
                                        viewModel.applyCoupon(manualCouponCode)
                                    }
                                },
                                enabled = manualCouponCode.isNotBlank() && !isApplying,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AppColors.Primary
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                if (isApplying) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text(
                                        text = "Apply",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Available Coupons Section
                    if (availableCoupons.isEmpty()) {
                        EmptyState(
                            icon = Icons.Default.LocalOffer,
                            title = "No Coupons Available",
                            subtitle = "Check back later for exciting offers",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp)
                        )
                    } else {
                        Text(
                            text = "Available Coupons",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.TextPrimary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(availableCoupons) { coupon ->
                                CouponCard(
                                    coupon = coupon,
                                    onApply = {
                                        isApplying = true
                                        viewModel.applyCoupon(coupon.code)
                                    }
                                )
                            }

                            item {
                                Spacer(modifier = Modifier.height(80.dp))
                            }
                        }
                    }
                }
            }

            // Show success/error messages
            LaunchedEffect(uiState.error, uiState.appliedCoupon) {
                if (isApplying) {
                    if (uiState.appliedCoupon != null) {
                        onCouponApplied(uiState.appliedCoupon!!)
                        isApplying = false
                    } else if (uiState.error != null) {
                        isApplying = false
                    }
                }
            }

            // Show error snackbar
            uiState.error?.let { error ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("Dismiss")
                        }
                    }
                ) {
                    Text(error)
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
    coupon: CouponResponse,
    onApply: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Coupon Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalOffer,
                        contentDescription = null,
                        tint = AppColors.Primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = coupon.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    )
                }

                if (coupon.expiryDate != null) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFFFF9E6)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = Color(0xFFFF9800),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = coupon.expiryDate,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFFF9800)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Coupon Code
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = AppColors.Primary,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .background(
                        color = AppColors.Primary.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = coupon.code,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.Primary
                )

                TextButton(onClick = onApply) {
                    Text(
                        text = "APPLY",
                        color = AppColors.Primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Description
            Text(
                text = coupon.description,
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Discount Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (coupon.discountType == "percentage") {
                    CouponInfoChip(
                        icon = Icons.Default.Percent,
                        text = "${coupon.discountValue}% OFF"
                    )
                } else {
                    CouponInfoChip(
                        icon = Icons.Default.CurrencyRupee,
                        text = "₹${coupon.discountValue} OFF"
                    )
                }

                CouponInfoChip(
                    icon = Icons.Default.ShoppingCart,
                    text = "Min ₹${coupon.minOrderValue}"
                )

                if (coupon.maxDiscount != null && coupon.discountType == "percentage") {
                    CouponInfoChip(
                        icon = Icons.Default.TrendingDown,
                        text = "Max ₹${coupon.maxDiscount}"
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            HorizontalDivider(color = AppColors.Divider)

            Spacer(modifier = Modifier.height(12.dp))

            // Terms & Conditions
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = AppColors.TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = coupon.terms,
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextSecondary
                )
            }
        }
    }
}

/**
 * Coupon Info Chip
 */
@Composable
private fun CouponInfoChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = AppColors.Background
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AppColors.Primary,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.TextPrimary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}