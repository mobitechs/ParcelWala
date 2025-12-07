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
import com.mobitechs.parcelwala.data.model.response.FareDetails
import com.mobitechs.parcelwala.data.model.response.GoodsTypeResponse
import com.mobitechs.parcelwala.ui.components.*
import com.mobitechs.parcelwala.ui.theme.AppColors
import com.mobitechs.parcelwala.ui.viewmodel.BookingViewModel

/**
 * Review Booking Screen
 * Shows fare summary from FareDetails (API response)
 *
 * FIXES:
 * 1. Coupon discount now properly shows in fare summary
 * 2. Fare breakdown uses API data only - no duplicate GST
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewBookingScreen(
    selectedFareDetails: FareDetails,
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
    var showFareBreakdown by remember { mutableStateOf(false) }
    var showPaymentSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadAvailableCoupons()
        if (viewModel.goodsTypes.value.isEmpty()) viewModel.loadGoodsTypes()
    }

    // ✅ FIX: Use API's rounded fare as base, then apply coupon discount
    val baseFare = selectedFareDetails.roundedFare
    val couponDiscount = uiState.discount  // This comes from coupon calculation
    val finalFare = if (couponDiscount > 0) baseFare - couponDiscount else baseFare
    val appliedCoupon = uiState.appliedCoupon
    val selectedPaymentMethod = uiState.paymentMethod

    val goodsTypes by viewModel.goodsTypes.collectAsState()
    val selectedGoodsType = goodsTypes.find { it.goodsTypeId == uiState.selectedGoodsTypeId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Review Booking", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back", tint = AppColors.TextPrimary) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = AppColors.Background
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (uiState.isLoading) {
                LoadingIndicator(message = "Creating booking...", modifier = Modifier.fillMaxSize().padding(32.dp))
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                        // Vehicle & Trip Info
                        VehicleTripInfoCard(fareDetails = selectedFareDetails, onViewAddressDetails = onViewAddressDetails, modifier = Modifier.fillMaxWidth().padding(16.dp))

                        // Offers
                        SectionHeader(text = "Offers and Discounts", modifier = Modifier.padding(horizontal = 16.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        ApplyCouponCard(appliedCoupon = appliedCoupon, onApplyCoupon = onApplyCoupon, onRemoveCoupon = { viewModel.removeCoupon() }, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))

                        Spacer(modifier = Modifier.height(24.dp))

                        // Fare Summary - ✅ FIX: Pass coupon discount properly
                        SectionHeader(text = "Fare Summary", modifier = Modifier.padding(horizontal = 16.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        FareSummaryCard(
                            fareDetails = selectedFareDetails,
                            couponDiscount = couponDiscount,  // ✅ Renamed for clarity
                            appliedCouponCode = appliedCoupon,  // ✅ Pass coupon code for display
                            finalFare = finalFare,
                            onViewBreakdown = { showFareBreakdown = true },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // GST
                        SectionHeader(text = "GST Details", modifier = Modifier.padding(horizontal = 16.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        GSTCard(gstin = uiState.gstin, onAddGSTIN = onAddGSTIN, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))

                        Spacer(modifier = Modifier.height(24.dp))

                        // Goods
                        SectionHeader(text = "Goods Description", modifier = Modifier.padding(horizontal = 16.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        GoodsDescriptionCard(selectedGoodsType = selectedGoodsType, onSelectGoodsType = onSelectGoodsType, onViewRestrictions = onViewRestrictions, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))

                        Spacer(modifier = Modifier.height(24.dp))

                        // Terms
                        SectionHeader(text = "Read before Booking", modifier = Modifier.padding(horizontal = 16.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        InfoCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) { BookingTermsList(freeLoadingMins = selectedFareDetails.freeLoadingTimeMins ?: 25) }

                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.Center) {
                            Text("By booking you agree to our ", style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
                            Text("terms of service", style = MaterialTheme.typography.bodySmall, color = AppColors.Primary, textDecoration = TextDecoration.Underline, modifier = Modifier.clickable { })
                        }
                        Spacer(modifier = Modifier.height(100.dp))
                    }

                    // Bottom Payment - ✅ FIX: Show correct final fare with discount
                    Surface(color = Color.White, shadowElevation = 8.dp) {
                        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable { showPaymentSheet = true }.padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Icon(Icons.Default.Wallet, "Payment", tint = AppColors.Pickup, modifier = Modifier.size(32.dp))
                                    Column {
                                        Text("Payment Method", style = MaterialTheme.typography.labelMedium, color = AppColors.TextSecondary)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(selectedPaymentMethod, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
                                            Icon(Icons.Default.ArrowDropDown, null, tint = AppColors.TextSecondary)
                                        }
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("₹$finalFare", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = AppColors.Primary)
                                    if (couponDiscount > 0) {
                                        Text("You save ₹$couponDiscount", style = MaterialTheme.typography.labelSmall, color = AppColors.Pickup)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            PrimaryButton(text = "Book ${selectedFareDetails.vehicleTypeName}", onClick = onConfirmBooking, icon = Icons.Default.Check, modifier = Modifier.fillMaxWidth(), enabled = !uiState.isLoading)
                        }
                    }
                }
            }

            uiState.error?.let { error ->
                Snackbar(modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp).padding(bottom = 80.dp), action = { TextButton(onClick = { viewModel.clearError() }) { Text("Dismiss") } }) { Text(error) }
            }
        }
    }

    if (showFareBreakdown) {
        ModalBottomSheet(onDismissRequest = { showFareBreakdown = false }, containerColor = Color.White) {
            FareBreakdownBottomSheet(
                fareDetails = selectedFareDetails,
                couponDiscount = couponDiscount,
                appliedCouponCode = appliedCoupon,
                onDismiss = { showFareBreakdown = false }
            )
        }
    }

    if (showPaymentSheet) {
        PaymentMethodBottomSheet(selectedMethod = selectedPaymentMethod, onMethodSelected = { viewModel.setPaymentMethod(it); showPaymentSheet = false }, onDismiss = { showPaymentSheet = false })
    }
}

@Composable
private fun VehicleTripInfoCard(fareDetails: FareDetails, onViewAddressDetails: () -> Unit, modifier: Modifier = Modifier) {
    InfoCard(modifier = modifier, elevation = 4.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.size(60.dp).background(AppColors.Primary.copy(alpha = 0.1f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                    Text(fareDetails.vehicleTypeIcon, style = MaterialTheme.typography.displaySmall)
                }
                Column {
                    Text(fareDetails.vehicleTypeName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
                    TextButton(onClick = onViewAddressDetails, contentPadding = PaddingValues(0.dp)) {
                        Text("View Trip Details", style = MaterialTheme.typography.bodyMedium, color = AppColors.Primary)
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.Route, null, tint = AppColors.TextSecondary, modifier = Modifier.size(16.dp))
                    Text(String.format("%.1f km", fareDetails.distanceKm), style = MaterialTheme.typography.bodyMedium, color = AppColors.TextSecondary)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.Timer, null, tint = AppColors.TextSecondary, modifier = Modifier.size(16.dp))
                    Text(fareDetails.getEtaText(), style = MaterialTheme.typography.bodyMedium, color = AppColors.TextSecondary)
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth().background(AppColors.Primary.copy(alpha = 0.05f), RoundedCornerShape(8.dp)).padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.Timer, null, tint = AppColors.Primary, modifier = Modifier.size(20.dp))
            Text("Free ${fareDetails.freeLoadingTimeMins ?: 25} mins of loading-unloading time included", style = MaterialTheme.typography.bodySmall, color = AppColors.TextPrimary)
        }
        if (fareDetails.hasSurgePricing()) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth().background(Color(0xFFFFF3E0), RoundedCornerShape(8.dp)).padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.TrendingUp, null, tint = Color(0xFFE65100), modifier = Modifier.size(20.dp))
                Text("High demand: ${fareDetails.getSurgePercentage()}% surge pricing applied", style = MaterialTheme.typography.bodySmall, color = Color(0xFFE65100))
            }
        }
    }
}

/**
 * ✅ FIXED: Fare Summary Card
 * - Uses API data from fareBreakdown when available
 * - Properly shows coupon discount row when applied
 * - Calculates correct final fare
 */
@Composable
private fun FareSummaryCard(
    fareDetails: FareDetails,
    couponDiscount: Int,  // Renamed for clarity
    appliedCouponCode: String?,  // Added to show coupon code
    finalFare: Int,
    onViewBreakdown: () -> Unit,
    modifier: Modifier = Modifier
) {
    InfoCard(modifier = modifier) {
        // ✅ Use API fareBreakdown if available, otherwise use individual fields
        if (fareDetails.fareBreakdown.isNotEmpty()) {
            // Display from API fareBreakdown (excludes GST as we'll show it separately for consistency)
            fareDetails.fareBreakdown.forEachIndexed { index, item ->
                if (item.type != "tax") {  // Show non-tax items first
                    FareRow(item.label, item.value.toInt())
                    Spacer(Modifier.height(8.dp))
                }
            }

            // Divider before subtotal
            HorizontalDivider(color = AppColors.Divider)
            Spacer(Modifier.height(8.dp))

            // Subtotal
            FareRow("Subtotal", fareDetails.subTotal.toInt())
            Spacer(Modifier.height(8.dp))

            // GST from fareBreakdown
            fareDetails.fareBreakdown.find { it.type == "tax" }?.let { taxItem ->
                FareRow(taxItem.label, taxItem.value.toInt())
                Spacer(Modifier.height(8.dp))
            }
        } else {
            // Fallback: Use individual fare fields from API
            FareRow("Base Fare (incl. ${fareDetails.freeDistanceKm.toInt()} km)", fareDetails.baseFare.toInt())
            if (fareDetails.distanceFare > 0) {
                Spacer(Modifier.height(8.dp))
                FareRow("Distance (${String.format("%.1f", fareDetails.chargeableDistanceKm)} km)", fareDetails.distanceFare.toInt())
            }
            if (fareDetails.platformFee > 0) {
                Spacer(Modifier.height(8.dp))
                FareRow("Platform Fee", fareDetails.platformFee.toInt())
            }
            if (fareDetails.surgeAmount > 0) {
                Spacer(Modifier.height(8.dp))
                FareRow("Surge (${fareDetails.getSurgePercentage()}%)", fareDetails.surgeAmount.toInt(), isSurge = true)
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = AppColors.Divider)
            Spacer(Modifier.height(8.dp))

            FareRow("Subtotal", fareDetails.subTotal.toInt())
            Spacer(Modifier.height(8.dp))
            FareRow("GST (${fareDetails.gstPercentage.toInt()}%)", fareDetails.gstAmount.toInt())
        }

        // ✅ API discount (if any from fare calculation)
        if (fareDetails.discount > 0) {
            Spacer(Modifier.height(8.dp))
            FareRow("Discount", -fareDetails.discount.toInt(), isDiscount = true)
        }

        // ✅ FIX: Show coupon discount when applied
        if (couponDiscount > 0) {
            Spacer(Modifier.height(8.dp))
            FareRow(
                label = if (appliedCouponCode != null) "Coupon ($appliedCouponCode)" else "Coupon Discount",
                amount = -couponDiscount,
                isDiscount = true
            )
        }

        // Final Amount
        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = AppColors.Divider, thickness = 2.dp)
        Spacer(Modifier.height(8.dp))
        FareRow("Amount Payable", finalFare, isBold = true, isTotal = true)

        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onViewBreakdown, modifier = Modifier.fillMaxWidth()) {
            Text("View Detailed Breakdown", color = AppColors.Primary, fontWeight = FontWeight.Medium)
            Icon(Icons.Default.ChevronRight, null, tint = AppColors.Primary, modifier = Modifier.size(20.dp))
        }
    }
}

/**
 * ✅ FIXED: Fare Breakdown Bottom Sheet
 * - Only uses API fareBreakdown data - no static duplicates
 * - Properly shows coupon discount
 * - Correct total calculation
 */
@Composable
private fun FareBreakdownBottomSheet(
    fareDetails: FareDetails,
    couponDiscount: Int,
    appliedCouponCode: String?,
    onDismiss: () -> Unit
) {
    val finalAmount = fareDetails.roundedFare - couponDiscount

    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        // Header
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Fare Breakdown", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
            IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Close", tint = AppColors.TextSecondary) }
        }
        Spacer(Modifier.height(16.dp))

        // Vehicle Info
        Row(modifier = Modifier.fillMaxWidth().background(AppColors.Background, RoundedCornerShape(8.dp)).padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(fareDetails.vehicleTypeName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("${String.format("%.1f", fareDetails.distanceKm)} km • ${fareDetails.estimatedDurationMinutes} mins", style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
            }
            Text(fareDetails.vehicleTypeIcon, style = MaterialTheme.typography.headlineMedium)
        }
        Spacer(Modifier.height(16.dp))

        // ✅ FIX: Use ONLY API fareBreakdown - prevents duplicate GST
        if (fareDetails.fareBreakdown.isNotEmpty()) {
            fareDetails.fareBreakdown.forEach { item ->
                BreakdownRow(
                    label = item.label,
                    amount = item.value,
                    type = item.type
                )
                Spacer(Modifier.height(8.dp))
            }
        } else {
            // Fallback: Build breakdown from individual fields
            BreakdownRow("Base Fare (incl. ${fareDetails.freeDistanceKm.toInt()}km)", fareDetails.baseFare)
            Spacer(Modifier.height(8.dp))

            if (fareDetails.distanceFare > 0) {
                BreakdownRow("Distance Charges (${String.format("%.1f", fareDetails.chargeableDistanceKm)}km)", fareDetails.distanceFare)
                Spacer(Modifier.height(8.dp))
            }
            if (fareDetails.platformFee > 0) {
                BreakdownRow("Platform Fee", fareDetails.platformFee)
                Spacer(Modifier.height(8.dp))
            }
            if (fareDetails.surgeAmount > 0) {
                BreakdownRow("Surge (${fareDetails.getSurgePercentage()}%)", fareDetails.surgeAmount, "surge")
                Spacer(Modifier.height(8.dp))
            }

            // GST - only add once in fallback
            BreakdownRow("GST (${fareDetails.gstPercentage.toInt()}%)", fareDetails.gstAmount, "tax")
            Spacer(Modifier.height(8.dp))
        }

        // API discount (if any from fare calculation)
        if (fareDetails.discount > 0) {
            HorizontalDivider(color = AppColors.Divider)
            Spacer(Modifier.height(8.dp))
            BreakdownRow("Discount", -fareDetails.discount, "discount")
            Spacer(Modifier.height(8.dp))
        }

        // ✅ FIX: Show coupon discount in breakdown
        if (couponDiscount > 0) {
            if (fareDetails.discount <= 0) {
                HorizontalDivider(color = AppColors.Divider)
                Spacer(Modifier.height(8.dp))
            }
            BreakdownRow(
                label = if (appliedCouponCode != null) "Coupon ($appliedCouponCode)" else "Coupon Discount",
                amount = -couponDiscount.toDouble(),
                type = "discount"
            )
            Spacer(Modifier.height(8.dp))
        }

        // Total Amount
        HorizontalDivider(color = AppColors.Primary, thickness = 2.dp)
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Total Amount", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
            Text("₹$finalAmount", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = AppColors.Primary)
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun BreakdownRow(label: String, amount: Double, type: String = "charge") {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = AppColors.TextSecondary)
        Text(
            when {
                type == "discount" && amount < 0 -> "-₹${(-amount).toInt()}"
                type == "discount" && amount > 0 -> "-₹${amount.toInt()}"
                else -> "₹${amount.toInt()}"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = when (type) {
                "discount" -> AppColors.Pickup
                "surge" -> Color(0xFFE65100)
                else -> AppColors.TextPrimary
            }
        )
    }
}

@Composable
private fun FareRow(label: String, amount: Int, isBold: Boolean = false, isDiscount: Boolean = false, isSurge: Boolean = false, isTotal: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            label,
            style = if (isBold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = AppColors.TextPrimary
        )
        Text(
            if (amount < 0) "-₹${-amount}" else "₹$amount",
            style = if (isBold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = when {
                isDiscount -> AppColors.Pickup
                isSurge -> Color(0xFFE65100)
                isTotal -> AppColors.Primary
                else -> AppColors.TextPrimary
            }
        )
    }
}

@Composable
private fun ApplyCouponCard(appliedCoupon: String?, onApplyCoupon: () -> Unit, onRemoveCoupon: () -> Unit, modifier: Modifier = Modifier) {
    Card(modifier = modifier.clickable(enabled = appliedCoupon == null, onClick = onApplyCoupon), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.LocalOffer, "Coupon", tint = if (appliedCoupon != null) AppColors.Pickup else AppColors.Primary, modifier = Modifier.size(24.dp))
                if (appliedCoupon != null) {
                    Column {
                        Text(appliedCoupon, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = AppColors.Pickup)
                        Text("Coupon Applied", style = MaterialTheme.typography.labelSmall, color = AppColors.TextSecondary)
                    }
                } else Text("Apply Coupon", style = MaterialTheme.typography.titleMedium, color = AppColors.TextPrimary)
            }
            if (appliedCoupon != null) IconButton(onClick = onRemoveCoupon) { Icon(Icons.Default.Close, "Remove", tint = AppColors.Drop) }
            else Icon(Icons.Default.ChevronRight, "Apply", tint = AppColors.TextSecondary)
        }
    }
}

@Composable
private fun GSTCard(gstin: String?, onAddGSTIN: () -> Unit, modifier: Modifier = Modifier) {
    InfoCard(modifier = modifier) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.Receipt, "GST", tint = AppColors.Primary, modifier = Modifier.size(24.dp))
                if (gstin != null) Column { Text("GSTIN Added", style = MaterialTheme.typography.labelMedium, color = AppColors.TextSecondary); Text(gstin, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary) }
                else Text("Have a GST Number?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
            }
            TextButton(onClick = onAddGSTIN) { Text(if (gstin != null) "Change" else "Add GSTIN", color = AppColors.Primary, fontWeight = FontWeight.Bold) }
        }
        if (gstin == null) { Spacer(Modifier.height(8.dp)); Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { Icon(Icons.Default.Info, null, tint = AppColors.Primary, modifier = Modifier.size(16.dp)); Text("Get invoices with GSTIN for Input Tax Credit", style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary) } }
    }
}

@Composable
private fun GoodsDescriptionCard(selectedGoodsType: GoodsTypeResponse?, onSelectGoodsType: () -> Unit, onViewRestrictions: () -> Unit, modifier: Modifier = Modifier) {
    InfoCard(modifier = modifier) {
        Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onSelectGoodsType), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(selectedGoodsType?.name ?: "Select Goods Type", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
                selectedGoodsType?.let { Spacer(Modifier.height(4.dp)); Text("${it.defaultWeight} Kg • ${it.defaultPackages.toString().padStart(2, '0')} Package • ₹${it.defaultValue} (Default)", style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary) }
            }
            TextButton(onClick = onSelectGoodsType) { Text(if (selectedGoodsType != null) "Change" else "Select", color = AppColors.Primary, fontWeight = FontWeight.Bold) }
        }
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth().background(Color(0xFFFFF9E6), RoundedCornerShape(8.dp)).clickable(onClick = onViewRestrictions).padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { Icon(Icons.Default.Warning, null, tint = Color(0xFFFF9800), modifier = Modifier.size(20.dp)); Text("Do not send restricted items", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = AppColors.TextPrimary) }
            TextButton(onClick = onViewRestrictions, contentPadding = PaddingValues(0.dp)) { Text("View", color = AppColors.Primary, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun BookingTermsList(freeLoadingMins: Int = 25) {
    val terms = listOf("Fare includes $freeLoadingMins mins free loading/unloading time.", "₹ 2.0/min for additional loading/unloading time.", "Fare may change if route or location changes.", "Parking charges to be paid by customer.", "Fare includes toll and permit charges, if any.", "We don't allow overloading.")
    Column { terms.forEach { Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) { Text("•", style = MaterialTheme.typography.bodyMedium, color = AppColors.TextSecondary, modifier = Modifier.padding(end = 8.dp)); Text(it, style = MaterialTheme.typography.bodyMedium, color = AppColors.TextSecondary) } } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaymentMethodBottomSheet(selectedMethod: String, onMethodSelected: (String) -> Unit, onDismiss: () -> Unit) {
    val methods = listOf("Cash", "Card", "UPI", "Wallet")
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text("Choose Payment Method", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
            Spacer(Modifier.height(24.dp))
            methods.forEach { method ->
                Row(modifier = Modifier.fillMaxWidth().clickable { onMethodSelected(method) }.padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(when (method) { "Cash" -> Icons.Default.Money; "Card" -> Icons.Default.CreditCard; "UPI" -> Icons.Default.Payment; else -> Icons.Default.Wallet }, method, tint = AppColors.Primary, modifier = Modifier.size(24.dp))
                        Text(method, style = MaterialTheme.typography.titleMedium, color = AppColors.TextPrimary)
                    }
                    if (selectedMethod == method) Icon(Icons.Default.CheckCircle, "Selected", tint = AppColors.Primary)
                }
                if (method != methods.last()) HorizontalDivider(color = AppColors.Divider)
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}