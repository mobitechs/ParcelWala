// ui/screens/booking/ReviewBookingScreen.kt
package com.mobitechs.parcelwala.ui.screens.booking

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mobitechs.parcelwala.R
import com.mobitechs.parcelwala.data.model.request.SavedAddress
import com.mobitechs.parcelwala.data.model.response.FareDetails
import com.mobitechs.parcelwala.data.model.response.GoodsTypeResponse
import com.mobitechs.parcelwala.ui.components.AppTopBar
import com.mobitechs.parcelwala.ui.components.LoadingIndicator
import com.mobitechs.parcelwala.ui.components.PrimaryButton
import com.mobitechs.parcelwala.ui.components.StatusBarScaffold
import com.mobitechs.parcelwala.ui.theme.AppColors
import com.mobitechs.parcelwala.ui.viewmodel.BookingViewModel

// ─── Screen ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewBookingScreen(
    selectedFareDetails: FareDetails,
    pickupAddress: SavedAddress,
    dropAddress: SavedAddress,
    onConfirmBooking: () -> Unit,
    onApplyCoupon: () -> Unit,
    onViewAddressDetails: () -> Unit,
    onSelectGoodsType: () -> Unit,       // kept for nav compat; inline sheet used instead
    onViewRestrictions: () -> Unit,
    onAddGSTIN: () -> Unit,
    onBack: () -> Unit,
    viewModel: BookingViewModel = hiltViewModel()
) {
    val uiState    by viewModel.uiState.collectAsState()
    val goodsTypes by viewModel.goodsTypes.collectAsState()

    var showFareBreakdown  by remember { mutableStateOf(false) }
    var showPaymentSheet   by remember { mutableStateOf(false) }
    var showGoodsTypeSheet by remember { mutableStateOf(false) }
    var showGoodsError     by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadAvailableCoupons()
        if (viewModel.goodsTypes.value.isEmpty()) viewModel.loadGoodsTypes()
    }

    val baseFare              = selectedFareDetails.roundedFare
    val couponDiscount        = uiState.discount
    val finalFare             = if (couponDiscount > 0) baseFare - couponDiscount else baseFare
    val appliedCoupon         = uiState.appliedCoupon
    val selectedPaymentMethod = uiState.paymentMethod
    val selectedGoodsType     = goodsTypes.find { it.goodsTypeId == uiState.selectedGoodsTypeId }
    val goodsSelected         = selectedGoodsType != null

    val displayDistanceText = viewModel.getDistanceText()
        ?: String.format("%.1f km", selectedFareDetails.distanceKm)
    val displayDurationText = viewModel.getDurationText()
        ?: "${selectedFareDetails.estimatedDurationMinutes} mins"

    LaunchedEffect(goodsSelected) { if (goodsSelected) showGoodsError = false }

    StatusBarScaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.review_booking),
                onBack = onBack,
                extraContent = {
                    VehicleHeroStrip(
                        fareDetails         = selectedFareDetails,
                        finalFare           = finalFare,
                        couponDiscount      = couponDiscount,
                        displayDistanceText = displayDistanceText,
                        displayDurationText = displayDurationText
                    )
                }
            )
        },
        containerColor = AppColors.LightGray50
    ) { paddingValues ->

        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (uiState.isLoading) {
                LoadingIndicator(
                    message  = stringResource(R.string.creating_booking),
                    modifier = Modifier.fillMaxSize().padding(32.dp)
                )
            } else {
                Column(modifier = Modifier.fillMaxSize()) {

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(top = 12.dp)
                    ) {

                        // 1. Goods Type — MANDATORY
                        SectionCard(
                            title      = stringResource(R.string.goods_description),
                            badgeText  = if (!goodsSelected) stringResource(R.string.required_label) else null,
                            badgeError = true,
                            modifier   = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
                        ) {
                            GoodsTypeContent(
                                selectedGoodsType  = selectedGoodsType,
                                showError          = showGoodsError,
                                onSelectGoodsType  = { showGoodsTypeSheet = true },
                                onViewRestrictions = onViewRestrictions
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // 2. Apply Coupon — above GST
                        CouponCard(
                            appliedCoupon  = appliedCoupon,
                            couponDiscount = couponDiscount,
                            onApplyCoupon  = onApplyCoupon,
                            onRemoveCoupon = { viewModel.removeCoupon() },
                            modifier       = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // 3. GST Details
                        SectionCard(
                            title    = stringResource(R.string.gst_details),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
                        ) {
                            GSTContent(gstin = uiState.gstin, onAddGSTIN = onAddGSTIN)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // 4. Fare Summary
                        SectionCard(
                            title    = stringResource(R.string.fare_summary),
                            action   = stringResource(R.string.view_detailed_breakdown),
                            onAction = { showFareBreakdown = true },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
                        ) {
                            FareSummaryContent(
                                fareDetails       = selectedFareDetails,
                                couponDiscount    = couponDiscount,
                                appliedCouponCode = appliedCoupon,
                                finalFare         = finalFare
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // 5. Read before booking
                        BookingTermsCard(
                            freeLoadingMins = selectedFareDetails.freeLoadingTimeMins ?: 25,
                            modifier        = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier              = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                stringResource(R.string.by_booking_agree),
                                style = MaterialTheme.typography.bodySmall,
                                color = AppColors.TextSecondary
                            )
                            Text(
                                stringResource(R.string.terms_of_service),
                                style          = MaterialTheme.typography.bodySmall,
                                color          = AppColors.Primary,
                                textDecoration = TextDecoration.Underline,
                                modifier       = Modifier.clickable { }
                            )
                        }

                        Spacer(modifier = Modifier.height(100.dp))
                    }

                    // Sticky bottom bar
                    BottomBar(
                        finalFare             = finalFare,
                        couponDiscount        = couponDiscount,
                        selectedPaymentMethod = selectedPaymentMethod,
                        vehicleName           = selectedFareDetails.vehicleTypeName,
                        goodsSelected         = goodsSelected,
                        isLoading             = uiState.isLoading,
                        onPaymentClick        = { showPaymentSheet = true },
                        onConfirm = {
                            if (!goodsSelected) showGoodsError = true
                            else onConfirmBooking()
                        }
                    )
                }
            }

            uiState.error?.let { error ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .padding(bottom = 80.dp),
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text(stringResource(R.string.dismiss))
                        }
                    }
                ) { Text(error) }
            }
        }
    }

    // ── Bottom sheets ──────────────────────────────────────────────────────────

    if (showGoodsTypeSheet) {
        GoodsTypeBottomSheet(
            goodsTypes         = goodsTypes,
            selectedGoodsType  = selectedGoodsType,
            onGoodsSelected    = { viewModel.setGoodsType(it); showGoodsTypeSheet = false },
            onViewRestrictions = onViewRestrictions,
            onDismiss          = { showGoodsTypeSheet = false }
        )
    }

    if (showFareBreakdown) {
        ModalBottomSheet(
            onDismissRequest = { showFareBreakdown = false },
            containerColor   = AppColors.White
        ) {
            FareBreakdownBottomSheet(
                fareDetails         = selectedFareDetails,
                couponDiscount      = couponDiscount,
                appliedCouponCode   = appliedCoupon,
                displayDistanceText = displayDistanceText,
                displayDurationText = displayDurationText,
                onDismiss           = { showFareBreakdown = false }
            )
        }
    }

    if (showPaymentSheet) {
        PaymentMethodBottomSheet(
            selectedMethod   = selectedPaymentMethod,
            onMethodSelected = { viewModel.setPaymentMethod(it); showPaymentSheet = false },
            onDismiss        = { showPaymentSheet = false }
        )
    }
}

// ─── Vehicle Hero Strip (inside gradient AppTopBar extraContent) ──────────────

@Composable
private fun VehicleHeroStrip(
    fareDetails: FareDetails,
    finalFare: Double,
    couponDiscount: Double,
    displayDistanceText: String,
    displayDurationText: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 20.dp)
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(fareDetails.vehicleTypeIcon, style = MaterialTheme.typography.headlineSmall)
                }
                Column {
                    Text(
                        text       = fareDetails.vehicleTypeName,
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color      = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(Icons.Default.Route, null, tint = Color.White.copy(alpha = 0.75f), modifier = Modifier.size(13.dp))
                            Text(displayDistanceText, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.85f))
                        }
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(Icons.Default.Timer, null, tint = Color.White.copy(alpha = 0.75f), modifier = Modifier.size(13.dp))
                            Text(displayDurationText, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.85f))
                        }
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text          = stringResource(R.string.fare_format, finalFare),
                    style         = MaterialTheme.typography.headlineMedium,
                    fontWeight    = FontWeight.ExtraBold,
                    color         = Color.White,
                    letterSpacing = (-0.5).sp
                )
                // Fix 2: free loading time shown directly below fare amount
                if (fareDetails.freeLoadingTimeMins != null && fareDetails.freeLoadingTimeMins > 0) {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(Icons.Default.Timer, null, tint = Color.White.copy(alpha = 0.65f), modifier = Modifier.size(11.dp))
                        Text(
                            text  = "${fareDetails.freeLoadingTimeMins}m free load",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
                if (couponDiscount > 0) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .background(AppColors.Pickup, RoundedCornerShape(20.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text       = stringResource(R.string.you_save_format, couponDiscount),
                            style      = MaterialTheme.typography.labelSmall,
                            color      = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
        // Fix 3: surge banner removed entirely
    }
}

@Composable
private fun HeroChip(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Row(
        modifier = Modifier
            .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(12.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.9f))
    }
}

// ─── Section Card wrapper ─────────────────────────────────────────────────────

@Composable
private fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    badgeText: String? = null,
    badgeError: Boolean = false,
    action: String? = null,
    onAction: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Card(
        modifier  = modifier,
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(containerColor = AppColors.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text          = title.uppercase(),
                        style         = MaterialTheme.typography.labelSmall,
                        fontWeight    = FontWeight.Bold,
                        color         = AppColors.TextSecondary,
                        letterSpacing = 0.6.sp
                    )
                    if (badgeText != null) {
                        Box(
                            modifier = Modifier
                                .background(
                                    if (badgeError) AppColors.ErrorLight else AppColors.PrimaryLight,
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text       = badgeText.uppercase(),
                                style      = MaterialTheme.typography.labelSmall,
                                color      = if (badgeError) AppColors.Drop else AppColors.PrimaryDark,
                                fontWeight = FontWeight.Bold,
                                fontSize   = 9.sp
                            )
                        }
                    }
                }
                if (action != null && onAction != null) {
                    TextButton(
                        onClick        = onAction,
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                    ) {
                        Text(
                            text       = action,
                            style      = MaterialTheme.typography.labelMedium,
                            color      = AppColors.Primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(Icons.Default.ChevronRight, null, tint = AppColors.Primary, modifier = Modifier.size(14.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}

// ─── Goods Type Content ───────────────────────────────────────────────────────

@Composable
private fun GoodsTypeContent(
    selectedGoodsType: GoodsTypeResponse?,
    showError: Boolean,
    onSelectGoodsType: () -> Unit,
    onViewRestrictions: () -> Unit
) {
    AnimatedVisibility(
        visible = showError && selectedGoodsType == null,
        enter   = fadeIn() + expandVertically(),
        exit    = fadeOut() + shrinkVertically()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppColors.ErrorLight, RoundedCornerShape(10.dp))
                    .padding(10.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Info, null, tint = AppColors.Drop, modifier = Modifier.size(16.dp))
                Text(
                    text       = stringResource(R.string.select_goods_type_error),
                    style      = MaterialTheme.typography.bodySmall,
                    color      = AppColors.Drop,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
    }

    Row(
        modifier              = Modifier.fillMaxWidth().clickable(onClick = onSelectGoodsType),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier              = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        if (selectedGoodsType != null) AppColors.Primary.copy(alpha = 0.1f)
                        else AppColors.ErrorLight,
                        RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                val iconText = selectedGoodsType?.icon?.trim() ?: ""
                if (iconText.isNotBlank() && iconText != "??" && iconText != "?") {
                    Text(iconText, style = MaterialTheme.typography.titleMedium)
                } else {
                    Icon(
                        imageVector        = Icons.Default.Wallet,
                        contentDescription = null,
                        tint               = if (selectedGoodsType != null) AppColors.Primary else AppColors.Drop,
                        modifier           = Modifier.size(22.dp)
                    )
                }
            }
            Column {
                Text(
                    text       = selectedGoodsType?.name ?: stringResource(R.string.select_goods_type),
                    style      = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color      = if (selectedGoodsType != null) AppColors.TextPrimary else AppColors.TextSecondary
                )
                selectedGoodsType?.let { goods ->
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text  = "${goods.defaultWeight} kg  ·  ${goods.defaultPackages.toString().padStart(2, '0')} pkg  ·  ₹${goods.defaultValue}",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextSecondary
                    )
                } ?: Text(
                    text  = stringResource(R.string.required_to_book),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.Drop
                )
            }
        }
        Text(
            text       = if (selectedGoodsType != null) stringResource(R.string.change) else stringResource(R.string.select_label),
            style      = MaterialTheme.typography.labelMedium,
            color      = AppColors.Primary,
            fontWeight = FontWeight.SemiBold
        )
    }

    Spacer(modifier = Modifier.height(12.dp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.AmberLight, RoundedCornerShape(10.dp))
            .clickable(onClick = onViewRestrictions)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier              = Modifier.weight(1f)
        ) {
            Icon(Icons.Default.Warning, null, tint = AppColors.Warning, modifier = Modifier.size(16.dp))
            Text(
                text     = stringResource(R.string.do_not_send_restricted),
                style    = MaterialTheme.typography.bodySmall,
                color    = AppColors.OrangeDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text       = stringResource(R.string.view_label),
            style      = MaterialTheme.typography.labelMedium,
            color      = AppColors.Primary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ─── GST Content — button stacked BELOW description ──────────────────────────

@Composable
private fun GSTContent(gstin: String?, onAddGSTIN: () -> Unit) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        verticalAlignment     = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(AppColors.Primary.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Receipt, null, tint = AppColors.Primary, modifier = Modifier.size(20.dp))
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            if (gstin != null) {
                // GSTIN added — title + value in row, change inline
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text  = stringResource(R.string.gstin_added),
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.TextSecondary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text       = gstin,
                            style      = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color      = AppColors.TextPrimary
                        )
                    }
                    TextButton(onClick = onAddGSTIN) {
                        Text(
                            text       = stringResource(R.string.change),
                            style      = MaterialTheme.typography.labelMedium,
                            color      = AppColors.Primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            } else {
                // Empty state — title + info, ADD button below
                Text(
                    text       = stringResource(R.string.have_gst_number),
                    style      = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color      = AppColors.TextPrimary
                )
                Spacer(modifier = Modifier.height(3.dp))
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.Info, null, tint = AppColors.Primary, modifier = Modifier.size(13.dp))
                    Text(
                        text  = stringResource(R.string.gstin_itc_info),
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextSecondary
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                // Button placed BELOW description
                Surface(
                    onClick  = onAddGSTIN,
                    shape    = RoundedCornerShape(10.dp),
                    color    = AppColors.Primary.copy(alpha = 0.08f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier              = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Receipt, null, tint = AppColors.Primary, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text       = stringResource(R.string.add_gstin),
                            style      = MaterialTheme.typography.labelMedium,
                            color      = AppColors.Primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// ─── Coupon Card ──────────────────────────────────────────────────────────────

@Composable
private fun CouponCard(
    appliedCoupon: String?,
    couponDiscount: Double,
    onApplyCoupon: () -> Unit,
    onRemoveCoupon: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier  = modifier.clickable(enabled = appliedCoupon == null, onClick = onApplyCoupon),
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(containerColor = AppColors.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier              = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            if (appliedCoupon != null) AppColors.Pickup.copy(alpha = 0.1f)
                            else AppColors.Primary.copy(alpha = 0.1f),
                            RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.LocalOffer, null,
                        tint     = if (appliedCoupon != null) AppColors.Pickup else AppColors.Primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                if (appliedCoupon != null) {
                    Column {
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text       = appliedCoupon,
                                style      = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color      = AppColors.Pickup
                            )
                            Box(
                                modifier = Modifier
                                    .background(AppColors.Pickup.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text       = stringResource(R.string.coupon_applied),
                                    style      = MaterialTheme.typography.labelSmall,
                                    color      = AppColors.Pickup,
                                    fontWeight = FontWeight.Bold,
                                    fontSize   = 10.sp
                                )
                            }
                        }
                        if (couponDiscount > 0) {
                            Text(
                                text  = stringResource(R.string.you_save_format, couponDiscount),
                                style = MaterialTheme.typography.bodySmall,
                                color = AppColors.Pickup
                            )
                        }
                    }
                } else {
                    Column {
                        Text(
                            text       = stringResource(R.string.apply_coupon),
                            style      = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color      = AppColors.TextPrimary
                        )
                        Text(
                            text  = stringResource(R.string.save_more_with_coupon),
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.TextSecondary
                        )
                    }
                }
            }
            if (appliedCoupon != null) {
                IconButton(onClick = onRemoveCoupon) {
                    Icon(Icons.Default.Close, stringResource(R.string.remove), tint = AppColors.TextSecondary, modifier = Modifier.size(18.dp))
                }
            } else {
                Icon(Icons.Default.ChevronRight, null, tint = AppColors.TextSecondary, modifier = Modifier.size(20.dp))
            }
        }
    }
}

// ─── Fare Summary Content ─────────────────────────────────────────────────────

@Composable
private fun FareSummaryContent(
    fareDetails: FareDetails,
    couponDiscount: Double,
    appliedCouponCode: String?,
    finalFare: Double
) {
    if (fareDetails.fareBreakdown.isNotEmpty()) {
        fareDetails.fareBreakdown.forEach { item ->
            if (item.type != "tax") { FareLineRow(item.label, item.value.toInt()); Spacer(Modifier.height(6.dp)) }
        }
        HorizontalDivider(color = AppColors.DividerLight); Spacer(Modifier.height(6.dp))
        FareLineRow(stringResource(R.string.subtotal), fareDetails.subTotal.toInt()); Spacer(Modifier.height(6.dp))
        fareDetails.fareBreakdown.find { it.type == "tax" }?.let { tax ->
            FareLineRow(tax.label, tax.value.toInt()); Spacer(Modifier.height(6.dp))
        }
    } else {
        FareLineRow(stringResource(R.string.base_fare_incl_format, fareDetails.freeDistanceKm.toInt()), fareDetails.baseFare.toInt())
        if (fareDetails.distanceFare > 0) {
            Spacer(Modifier.height(6.dp))
            FareLineRow(stringResource(R.string.distance_charge_format, String.format("%.1f", fareDetails.chargeableDistanceKm)), fareDetails.distanceFare.toInt())
        }
        if (fareDetails.platformFee > 0) { Spacer(Modifier.height(6.dp)); FareLineRow(stringResource(R.string.platform_fee), fareDetails.platformFee.toInt()) }
        if (fareDetails.surgeAmount > 0) { Spacer(Modifier.height(6.dp)); FareLineRow(stringResource(R.string.surge_format, fareDetails.getSurgePercentage()), fareDetails.surgeAmount.toInt(), isSurge = true) }
        Spacer(Modifier.height(6.dp)); HorizontalDivider(color = AppColors.DividerLight); Spacer(Modifier.height(6.dp))
        FareLineRow(stringResource(R.string.subtotal), fareDetails.subTotal.toInt()); Spacer(Modifier.height(6.dp))
        FareLineRow(stringResource(R.string.gst_format, fareDetails.gstPercentage.toInt()), fareDetails.gstAmount.toInt())
    }
    if (fareDetails.discount > 0) { Spacer(Modifier.height(6.dp)); FareLineRow(stringResource(R.string.discount_label), -fareDetails.discount.toInt(), isDiscount = true) }
    if (couponDiscount > 0) {
        Spacer(Modifier.height(6.dp))
        FareLineRow(
            label = if (appliedCouponCode != null) stringResource(R.string.coupon_format, appliedCouponCode) else stringResource(R.string.coupon_discount),
            amount = -couponDiscount.toInt(), isDiscount = true
        )
    }
    Spacer(Modifier.height(8.dp)); HorizontalDivider(color = AppColors.Primary.copy(alpha = 0.25f), thickness = 1.dp); Spacer(Modifier.height(10.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.amount_payable), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = AppColors.TextPrimary)
        Text(stringResource(R.string.fare_format, finalFare), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = AppColors.Primary)
    }
}

@Composable
private fun FareLineRow(label: String, amount: Int, isDiscount: Boolean = false, isSurge: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary, modifier = Modifier.weight(1f))
        Text(
            text       = if (amount < 0) "-₹${-amount}" else "₹$amount",
            style      = MaterialTheme.typography.bodySmall,
            color      = when { isDiscount -> AppColors.Pickup; isSurge -> AppColors.WarningAmberDark; else -> AppColors.TextPrimary },
            fontWeight = if (isDiscount) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

// ─── Booking Terms Card ───────────────────────────────────────────────────────

@Composable
private fun BookingTermsCard(freeLoadingMins: Int, modifier: Modifier = Modifier) {
    val terms = listOf(
        stringResource(R.string.fare_loading_term, freeLoadingMins),
        stringResource(R.string.extra_loading_charge),
        stringResource(R.string.fare_route_change),
        stringResource(R.string.parking_charges),
        stringResource(R.string.toll_included),
        stringResource(R.string.no_overloading)
    )
    Card(modifier = modifier, shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = AppColors.White), elevation = CardDefaults.cardElevation(0.dp)) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.Info, null, tint = AppColors.TextSecondary, modifier = Modifier.size(13.dp))
                Text(stringResource(R.string.read_before_booking).uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = AppColors.TextSecondary, letterSpacing = 0.6.sp)
            }
            Spacer(modifier = Modifier.height(10.dp))
            terms.forEach { term ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("•", style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
                    Text(term, style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
                }
            }
        }
    }
}

// ─── Sticky Bottom Bar ────────────────────────────────────────────────────────

@Composable
private fun BottomBar(
    finalFare: Double,
    couponDiscount: Double,
    selectedPaymentMethod: String,
    vehicleName: String,
    goodsSelected: Boolean,
    isLoading: Boolean,
    onPaymentClick: () -> Unit,
    onConfirm: () -> Unit
) {
    Surface(color = AppColors.White, shadowElevation = 12.dp) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp).navigationBarsPadding()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(AppColors.LightGray50)
                        .clickable(onClick = onPaymentClick)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = when (selectedPaymentMethod) {
                            "Card" -> Icons.Default.CreditCard; "UPI" -> Icons.Default.Payment; "Wallet" -> Icons.Default.Wallet; else -> Icons.Default.Money
                        }, contentDescription = null, tint = AppColors.Primary, modifier = Modifier.size(18.dp)
                    )
                    Column {
                        Text("Pay via", style = MaterialTheme.typography.labelSmall, color = AppColors.TextSecondary)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(selectedPaymentMethod, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = AppColors.TextPrimary)
                            Icon(Icons.Default.ArrowDropDown, null, tint = AppColors.TextSecondary, modifier = Modifier.size(16.dp))
                        }
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(stringResource(R.string.fare_format, finalFare), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = AppColors.TextPrimary)
                    if (couponDiscount > 0) Text(stringResource(R.string.you_save_format, couponDiscount), style = MaterialTheme.typography.labelSmall, color = AppColors.Pickup, fontWeight = FontWeight.Medium)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            PrimaryButton(
                text     = if (goodsSelected) stringResource(R.string.book_vehicle_format, vehicleName) else stringResource(R.string.select_goods_to_continue),
                onClick  = onConfirm,
                icon     = if (goodsSelected) Icons.Default.Check else null,
                modifier = Modifier.fillMaxWidth(),
                enabled  = !isLoading
            )
        }
    }
}

// ─── Goods Type Bottom Sheet ──────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoodsTypeBottomSheet(
    goodsTypes: List<GoodsTypeResponse>,
    selectedGoodsType: GoodsTypeResponse?,
    onGoodsSelected: (GoodsTypeResponse) -> Unit,
    onViewRestrictions: () -> Unit,
    onDismiss: () -> Unit
) {
    // Track pending selection locally — only confirmed on button press
    var pendingSelection by remember { mutableStateOf(selectedGoodsType) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor   = AppColors.White,
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)          // sheet takes 85% of screen height
                .padding(horizontal = 16.dp)
        ) {

            // Header
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("What are you sending?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
                    Text("Select the type of goods for this trip", style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, stringResource(R.string.close), tint = AppColors.TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Scrollable goods list — takes remaining space above the footer
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding      = PaddingValues(bottom = 8.dp),
                modifier            = Modifier.weight(1f)
            ) {
                items(goodsTypes.filter { it.isActive }) { goods ->
                    GoodsTypeCard(
                        goods      = goods,
                        isSelected = goods.goodsTypeId == pendingSelection?.goodsTypeId,
                        onSelect   = { pendingSelection = goods }   // only mark, don't confirm yet
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Restricted items banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppColors.AmberLight, RoundedCornerShape(12.dp))
                    .clickable(onClick = onViewRestrictions)
                    .padding(12.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(AppColors.Warning.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Warning, null, tint = AppColors.Warning, modifier = Modifier.size(18.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Restricted items", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = AppColors.OrangeDark)
                    Text("Items not allowed for transport. Tap to view.", style = MaterialTheme.typography.bodySmall, color = AppColors.OrangeDark.copy(alpha = 0.7f))
                }
                Icon(Icons.Default.ChevronRight, null, tint = AppColors.OrangeDark, modifier = Modifier.size(18.dp))
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Confirm button — only active when something is selected
            PrimaryButton(
                text     = if (pendingSelection != null) "Confirm — ${pendingSelection!!.name}" else "Select a goods type",
                onClick  = { pendingSelection?.let { onGoodsSelected(it) } },
                icon     = if (pendingSelection != null) Icons.Default.Check else null,
                modifier = Modifier.fillMaxWidth(),
                enabled  = pendingSelection != null
            )

            Spacer(modifier = Modifier.height(20.dp).navigationBarsPadding())
        }
    }
}

@Composable
private fun GoodsTypeCard(
    goods: GoodsTypeResponse,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue   = if (isSelected) AppColors.Primary else AppColors.DividerLight,
        animationSpec = tween(200), label = "border"
    )
    val bgColor by animateColorAsState(
        targetValue   = if (isSelected) AppColors.Primary.copy(alpha = 0.05f) else AppColors.White,
        animationSpec = tween(200), label = "bg"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onSelect)
            .padding(14.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    if (isSelected) AppColors.Primary.copy(alpha = 0.1f) else AppColors.LightGray50,
                    RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            val iconText = goods.icon.trim()
            if (iconText.isNotBlank() && iconText != "??" && iconText != "?") {
                Text(text = iconText, style = MaterialTheme.typography.titleLarge)
            } else {
                // fallback: show a package icon
                Icon(
                    imageVector        = Icons.Default.Wallet,
                    contentDescription = null,
                    tint               = if (isSelected) AppColors.Primary else AppColors.TextSecondary,
                    modifier           = Modifier.size(24.dp)
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = goods.name,
                style      = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color      = if (isSelected) AppColors.Primary else AppColors.TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                GoodsSpecChip("${goods.defaultWeight} kg", isSelected)
                GoodsSpecChip("${goods.defaultPackages.toString().padStart(2, '0')} pkg", isSelected)
                GoodsSpecChip("₹${goods.defaultValue}", isSelected)
            }
        }

        if (isSelected) {
            Icon(Icons.Rounded.CheckCircle, null, tint = AppColors.Primary, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun GoodsSpecChip(label: String, isSelected: Boolean) {
    Box(
        modifier = Modifier
            .background(
                if (isSelected) AppColors.Primary.copy(alpha = 0.1f) else AppColors.LightGray50,
                RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 6.dp, vertical = 3.dp)
    ) {
        Text(
            text     = label,
            style    = MaterialTheme.typography.labelSmall,
            color    = if (isSelected) AppColors.PrimaryDark else AppColors.TextSecondary,
            fontSize = 10.sp
        )
    }
}

// ─── Fare Breakdown Bottom Sheet ──────────────────────────────────────────────

@Composable
private fun FareBreakdownBottomSheet(
    fareDetails: FareDetails,
    couponDiscount: Double,
    appliedCouponCode: String?,
    displayDistanceText: String,
    displayDurationText: String,
    onDismiss: () -> Unit
) {
    val finalAmount = fareDetails.roundedFare - couponDiscount
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.fare_breakdown), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
            IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, stringResource(R.string.close), tint = AppColors.TextSecondary) }
        }
        Spacer(Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth().background(AppColors.LightGray50, RoundedCornerShape(10.dp)).padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(fareDetails.vehicleTypeName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = AppColors.TextPrimary)
                Text("$displayDistanceText · $displayDurationText", style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
            }
            Text(fareDetails.vehicleTypeIcon, style = MaterialTheme.typography.headlineMedium)
        }
        Spacer(Modifier.height(16.dp))
        if (fareDetails.fareBreakdown.isNotEmpty()) {
            fareDetails.fareBreakdown.forEach { item -> BreakdownRow(item.label, item.value, item.type); Spacer(Modifier.height(8.dp)) }
        } else {
            BreakdownRow(stringResource(R.string.base_fare_incl_format, fareDetails.freeDistanceKm.toInt()), fareDetails.baseFare); Spacer(Modifier.height(8.dp))
            if (fareDetails.distanceFare > 0) { BreakdownRow(stringResource(R.string.distance_charge_format, String.format("%.1f", fareDetails.chargeableDistanceKm)), fareDetails.distanceFare); Spacer(Modifier.height(8.dp)) }
            if (fareDetails.platformFee > 0) { BreakdownRow(stringResource(R.string.platform_fee), fareDetails.platformFee); Spacer(Modifier.height(8.dp)) }
            if (fareDetails.surgeAmount > 0) { BreakdownRow(stringResource(R.string.surge_format, fareDetails.getSurgePercentage()), fareDetails.surgeAmount, "surge"); Spacer(Modifier.height(8.dp)) }
            BreakdownRow(stringResource(R.string.gst_format, fareDetails.gstPercentage.toInt()), fareDetails.gstAmount, "tax"); Spacer(Modifier.height(8.dp))
        }
        if (fareDetails.discount > 0) { HorizontalDivider(color = AppColors.DividerLight); Spacer(Modifier.height(8.dp)); BreakdownRow(stringResource(R.string.discount_label), -fareDetails.discount, "discount"); Spacer(Modifier.height(8.dp)) }
        if (couponDiscount > 0) {
            if (fareDetails.discount <= 0) { HorizontalDivider(color = AppColors.DividerLight); Spacer(Modifier.height(8.dp)) }
            BreakdownRow(if (appliedCouponCode != null) stringResource(R.string.coupon_format, appliedCouponCode) else stringResource(R.string.coupon_discount), -couponDiscount.toDouble(), "discount"); Spacer(Modifier.height(8.dp))
        }
        HorizontalDivider(color = AppColors.Primary, thickness = 2.dp); Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.total_amount), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
            Text(stringResource(R.string.fare_format, finalAmount), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = AppColors.Primary)
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun BreakdownRow(label: String, amount: Double, type: String = "charge") {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = AppColors.TextSecondary)
        Text(
            text  = when { type == "discount" && amount < 0 -> "-₹${(-amount).toInt()}"; type == "discount" && amount > 0 -> "-₹${amount.toInt()}"; else -> "₹${amount.toInt()}" },
            style = MaterialTheme.typography.bodyMedium,
            color = when (type) { "discount" -> AppColors.Pickup; "surge" -> AppColors.WarningAmberDark; else -> AppColors.TextPrimary }
        )
    }
}

// ─── Payment Method Bottom Sheet ──────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaymentMethodBottomSheet(
    selectedMethod: String,
    onMethodSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val methods = listOf("Cash", "Card", "UPI", "Wallet")
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = AppColors.White) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(stringResource(R.string.choose_payment_method), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
            Spacer(Modifier.height(24.dp))
            methods.forEach { method ->
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onMethodSelected(method) }.padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(
                                    if (selectedMethod == method) AppColors.Primary.copy(alpha = 0.1f) else AppColors.LightGray50,
                                    RoundedCornerShape(10.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (method) { "Cash" -> Icons.Default.Money; "Card" -> Icons.Default.CreditCard; "UPI" -> Icons.Default.Payment; else -> Icons.Default.Wallet },
                                contentDescription = method,
                                tint = if (selectedMethod == method) AppColors.Primary else AppColors.TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text       = method,
                            style      = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (selectedMethod == method) FontWeight.SemiBold else FontWeight.Normal,
                            color      = if (selectedMethod == method) AppColors.TextPrimary else AppColors.TextSecondary
                        )
                    }
                    if (selectedMethod == method) Icon(Icons.Rounded.CheckCircle, stringResource(R.string.selected_label), tint = AppColors.Primary)
                }
                if (method != methods.last()) HorizontalDivider(color = AppColors.DividerLight)
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}