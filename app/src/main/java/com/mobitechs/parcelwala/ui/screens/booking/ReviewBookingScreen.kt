// ui/screens/booking/ReviewBookingScreen.kt
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mobitechs.parcelwala.data.model.request.SavedAddress
import com.mobitechs.parcelwala.data.model.response.GoodsTypeResponse
import com.mobitechs.parcelwala.data.model.response.VehicleTypeResponse
import com.mobitechs.parcelwala.ui.components.*
import com.mobitechs.parcelwala.ui.theme.AppColors
import com.mobitechs.parcelwala.ui.viewmodel.BookingViewModel

/**
 * Review Booking Screen
 * Final review before confirming the booking
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewBookingScreen(
    selectedVehicle: VehicleTypeResponse,
    pickupAddress: SavedAddress,
    dropAddress: SavedAddress,
    onConfirmBooking: () -> Unit,
    onApplyCoupon: () -> Unit,
    onViewAddressDetails: () -> Unit,
    onSelectGoodsType: () -> Unit,
    onViewRestrictions: () -> Unit,
    onAddGSTIN: () -> Unit,
    onBack: () -> Unit,
    viewModel: BookingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showTermsDialog by remember { mutableStateOf(false) }
    var showPaymentSheet by remember { mutableStateOf(false) }

    // Load coupons and goods types when screen opens
    LaunchedEffect(Unit) {
        viewModel.loadAvailableCoupons()
        if (viewModel.goodsTypes.value.isEmpty()) {
            viewModel.loadGoodsTypes()
        }
    }

    // Use actual values from ViewModel state
    val baseFare = uiState.baseFare
    val discount = uiState.discount
    val finalFare = uiState.finalFare
    val appliedCoupon = uiState.appliedCoupon
    val selectedPaymentMethod = uiState.paymentMethod
    val selectedGoodsTypeId = uiState.selectedGoodsTypeId

    // Get selected goods type from ViewModel
    val goodsTypes by viewModel.goodsTypes.collectAsState()
    val selectedGoodsType = goodsTypes.find { it.goodsTypeId == selectedGoodsTypeId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Review Booking",
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
            if (uiState.isLoading) {
                LoadingIndicator(
                    message = "Creating booking...",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp)
                )
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Scrollable Content
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Vehicle Info Card
                        VehicleInfoCard(
                            vehicle = selectedVehicle,
                            onViewAddressDetails = onViewAddressDetails,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        )

                        // Offers and Discounts
                        SectionHeader(
                            text = "Offers and Discounts",
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        ApplyCouponCard(
                            appliedCoupon = appliedCoupon,
                            onApplyCoupon = onApplyCoupon,
                            onRemoveCoupon = { viewModel.removeCoupon() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Fare Summary
                        SectionHeader(
                            text = "Fare Summary",
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        FareSummaryCard(
                            baseFare = baseFare,
                            discount = discount,
                            finalFare = finalFare,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // GST Details
                        SectionHeader(
                            text = "GST Details",
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        GSTCard(
                            gstin = uiState.gstin,
                            onAddGSTIN = onAddGSTIN,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Goods Description
                        SectionHeader(
                            text = "Goods Description",
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        GoodsDescriptionCard(
                            selectedGoodsType = selectedGoodsType,
                            onSelectGoodsType = onSelectGoodsType,
                            onViewRestrictions = onViewRestrictions,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Read Before Booking
                        SectionHeader(
                            text = "Read before Booking",
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        InfoCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            BookingTermsList()
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Terms Agreement
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "By booking you agree to our ",
                                style = MaterialTheme.typography.bodySmall,
                                color = AppColors.TextSecondary
                            )
                            Text(
                                text = "terms of service",
                                style = MaterialTheme.typography.bodySmall,
                                color = AppColors.Primary,
                                textDecoration = TextDecoration.Underline,
                                modifier = Modifier.clickable { showTermsDialog = true }
                            )
                            Text(
                                text = " and ",
                                style = MaterialTheme.typography.bodySmall,
                                color = AppColors.TextSecondary
                            )
                            Text(
                                text = "privacy policy",
                                style = MaterialTheme.typography.bodySmall,
                                color = AppColors.Primary,
                                textDecoration = TextDecoration.Underline,
                                modifier = Modifier.clickable { showTermsDialog = true }
                            )
                        }

                        Spacer(modifier = Modifier.height(100.dp))
                    }

                    // Bottom Payment Section
                    Surface(
                        color = Color.White,
                        shadowElevation = 8.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            // Payment Method Selection
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showPaymentSheet = true }
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Wallet,
                                        contentDescription = "Payment",
                                        tint = AppColors.Pickup,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "Choose Payment Method",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = AppColors.TextSecondary
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = selectedPaymentMethod,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = AppColors.TextPrimary
                                            )
                                            Icon(
                                                imageVector = Icons.Default.ArrowDropDown,
                                                contentDescription = null,
                                                tint = AppColors.TextSecondary
                                            )
                                        }
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "₹$finalFare",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = AppColors.Primary
                                    )
                                    TextButton(
                                        onClick = { /* Show fare breakup */ },
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text(
                                            text = "View Breakup",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = AppColors.Primary
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Confirm Button
                            PrimaryButton(
                                text = "Book ${selectedVehicle.name}",
                                onClick = onConfirmBooking,
                                icon = Icons.Default.Check,
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !uiState.isLoading
                            )
                        }
                    }
                }
            }

            // Show error
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

    // Payment Method Bottom Sheet
    if (showPaymentSheet) {
        PaymentMethodBottomSheet(
            selectedMethod = selectedPaymentMethod,
            onMethodSelected = { method ->
                viewModel.setPaymentMethod(method)
                showPaymentSheet = false
            },
            onDismiss = { showPaymentSheet = false }
        )
    }
}

/**
 * Vehicle Info Card
 */
@Composable
private fun VehicleInfoCard(
    vehicle: VehicleTypeResponse,
    onViewAddressDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    InfoCard(
        modifier = modifier,
        elevation = 4.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(
                            color = AppColors.Primary.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = vehicle.icon,
                        style = MaterialTheme.typography.displaySmall
                    )
                }

                Column {
                    Text(
                        text = vehicle.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    )
                    TextButton(
                        onClick = onViewAddressDetails,
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = "View Address Details",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppColors.Primary
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "1 min",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.Pickup
                )
                Text(
                    text = "away",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Free Loading Time Info
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = AppColors.Primary.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Timer,
                contentDescription = null,
                tint = AppColors.Primary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "Free 25 mins of loading-unloading time included.",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextPrimary
            )
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = AppColors.Primary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/**
 * Apply Coupon Card with Remove Option
 */
@Composable
private fun ApplyCouponCard(
    appliedCoupon: String?,
    onApplyCoupon: () -> Unit,
    onRemoveCoupon: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable(
                enabled = appliedCoupon == null,
                onClick = onApplyCoupon
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocalOffer,
                    contentDescription = "Coupon",
                    tint = if (appliedCoupon != null) AppColors.Pickup else AppColors.Primary,
                    modifier = Modifier.size(24.dp)
                )

                if (appliedCoupon != null) {
                    Column {
                        Text(
                            text = appliedCoupon,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.Pickup
                        )
                        Text(
                            text = "Coupon Applied",
                            style = MaterialTheme.typography.labelSmall,
                            color = AppColors.TextSecondary
                        )
                    }
                } else {
                    Text(
                        text = "Apply Coupon",
                        style = MaterialTheme.typography.titleMedium,
                        color = AppColors.TextPrimary
                    )
                }
            }

            if (appliedCoupon != null) {
                IconButton(onClick = onRemoveCoupon) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove",
                        tint = AppColors.Drop
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Apply",
                    tint = AppColors.TextSecondary
                )
            }
        }
    }
}

/**
 * Fare Summary Card
 */
@Composable
private fun FareSummaryCard(
    baseFare: Int,
    discount: Int,
    finalFare: Int,
    modifier: Modifier = Modifier
) {
    InfoCard(modifier = modifier) {
        FareRow(label = "Trip Fare (incl. Toll)", amount = baseFare)

        if (discount > 0) {
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = AppColors.Divider)
            Spacer(modifier = Modifier.height(12.dp))
            FareRow(
                label = "Coupon Discount",
                amount = -discount,
                isDiscount = true
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = AppColors.Divider)
        Spacer(modifier = Modifier.height(12.dp))

        FareRow(
            label = "Net Fare",
            amount = if (discount > 0) baseFare - discount else baseFare,
            isBold = false
        )

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = AppColors.Divider)
        Spacer(modifier = Modifier.height(12.dp))

        FareRow(
            label = "Amount Payable (rounded)",
            amount = finalFare,
            isBold = true,
            isTotal = true
        )
    }
}

/**
 * Fare Row Item
 */
@Composable
private fun FareRow(
    label: String,
    amount: Int,
    isBold: Boolean = false,
    isDiscount: Boolean = false,
    isTotal: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = if (isBold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = AppColors.TextPrimary
        )
        Text(
            text = "₹${if (amount < 0) "-${-amount}" else amount}",
            style = if (isBold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = when {
                isDiscount -> AppColors.Pickup
                isTotal -> AppColors.Primary
                else -> AppColors.TextPrimary
            }
        )
    }
}

/**
 * GST Card
 */
@Composable
private fun GSTCard(
    gstin: String?,
    onAddGSTIN: () -> Unit,
    modifier: Modifier = Modifier
) {
    InfoCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Receipt,
                    contentDescription = "GST",
                    tint = AppColors.Primary,
                    modifier = Modifier.size(24.dp)
                )

                if (gstin != null) {
                    Column {
                        Text(
                            text = "GSTIN Added",
                            style = MaterialTheme.typography.labelMedium,
                            color = AppColors.TextSecondary
                        )
                        Text(
                            text = gstin,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.TextPrimary
                        )
                    }
                } else {
                    Text(
                        text = "Have a GST Number?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    )
                }
            }

            TextButton(onClick = onAddGSTIN) {
                Text(
                    text = if (gstin != null) "Change" else "Add GSTIN",
                    color = AppColors.Primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (gstin == null) {
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = AppColors.Primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Get invoices with GSTIN for Input Tax Credit",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextSecondary
                )
            }
        }
    }
}

/**
 * Goods Description Card
 */
@Composable
private fun GoodsDescriptionCard(
    selectedGoodsType: GoodsTypeResponse?,
    onSelectGoodsType: () -> Unit,
    onViewRestrictions: () -> Unit,
    modifier: Modifier = Modifier
) {
    InfoCard(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onSelectGoodsType),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = selectedGoodsType?.name ?: "Select Goods Type",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary
                )

                selectedGoodsType?.let { goods ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${goods.defaultWeight} Kg • ${goods.defaultPackages.toString().padStart(2, '0')} Package • ₹${goods.defaultValue} (Default)",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextSecondary
                    )
                }
            }

            TextButton(onClick = onSelectGoodsType) {
                Text(
                    text = if (selectedGoodsType != null) "Change" else "Select",
                    color = AppColors.Primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Restricted Items Warning
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = Color(0xFFFFF9E6),
                    shape = RoundedCornerShape(8.dp)
                )
                .clickable(onClick = onViewRestrictions)
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFFF9800),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Do not send restricted items",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = AppColors.TextPrimary
                )
            }

            TextButton(
                onClick = onViewRestrictions,
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    text = "View List",
                    color = AppColors.Primary,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = AppColors.Primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Booking Terms List
 */
@Composable
private fun BookingTermsList() {
    val terms = listOf(
        "Fare includes 25 mins free loading/unloading time.",
        "₹ 2.0/min for additional loading/unloading time.",
        "Fare may change if route or location changes.",
        "Parking charges to be paid by customer.",
        "Fare includes toll and permit charges, if any.",
        "We don't allow overloading."
    )

    Column {
        terms.forEach { term ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    text = "•",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextSecondary,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = term,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextSecondary
                )
            }
        }
    }
}

/**
 * Payment Method Bottom Sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaymentMethodBottomSheet(
    selectedMethod: String,
    onMethodSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val paymentMethods = listOf("Cash", "Card", "UPI", "Wallet")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Choose Payment Method",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary
            )

            Spacer(modifier = Modifier.height(24.dp))

            paymentMethods.forEach { method ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onMethodSelected(method) }
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = when (method) {
                                "Cash" -> Icons.Default.Money
                                "Card" -> Icons.Default.CreditCard
                                "UPI" -> Icons.Default.Payment
                                "Wallet" -> Icons.Default.Wallet
                                else -> Icons.Default.Payment
                            },
                            contentDescription = method,
                            tint = AppColors.Primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = method,
                            style = MaterialTheme.typography.titleMedium,
                            color = AppColors.TextPrimary
                        )
                    }

                    if (selectedMethod == method) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Selected",
                            tint = AppColors.Primary
                        )
                    }
                }
                if (method != paymentMethods.last()) {
                    HorizontalDivider(color = AppColors.Divider)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}