package com.mobitechs.parcelwala.ui.screens.booking

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mobitechs.parcelwala.R
import com.mobitechs.parcelwala.data.model.response.CouponResponse
import com.mobitechs.parcelwala.ui.components.EmptyState
import com.mobitechs.parcelwala.ui.components.LoadingIndicator
import com.mobitechs.parcelwala.ui.components.StatusBarScaffold
import com.mobitechs.parcelwala.ui.theme.AppColors
import com.mobitechs.parcelwala.ui.viewmodel.BookingViewModel

// ── Header palette ────────────────────────────────────────────────────────────
private val FieldBg     = Color.White.copy(alpha = 0.13f)
private val FieldBorder = Color.White.copy(alpha = 0.22f)
private val HeaderText  = Color.White
private val HeaderMuted = Color.White.copy(alpha = 0.55f)

// ══════════════════════════════════════════════════════════════════════════════
// CouponScreen  —  Variation 1: dark header entry + accent-bar coupon cards
// ══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CouponScreen(
    onBack: () -> Unit,
    onCouponApplied: (String) -> Unit,
    viewModel: BookingViewModel = hiltViewModel()
) {
    var manualCode  by remember { mutableStateOf("") }
    var isApplying  by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    val availableCoupons by viewModel.availableCoupons.collectAsState()
    val uiState          by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        if (availableCoupons.isEmpty()) viewModel.loadAvailableCoupons()
    }

    // Navigate away once a coupon is applied or reset error state
    LaunchedEffect(uiState.error, uiState.appliedCoupon) {
        if (isApplying) {
            when {
                uiState.appliedCoupon != null -> {
                    onCouponApplied(uiState.appliedCoupon!!)
                    isApplying = false
                }
                uiState.error != null -> isApplying = false
            }
        }
    }

    StatusBarScaffold(
        topBar = {
            CouponTopBar(
                code           = manualCode,
                onCodeChange   = { manualCode = it.uppercase() },
                onClearCode    = { manualCode = "" },
                isApplying     = isApplying,
                focusRequester = focusRequester,
                onApply        = {
                    if (manualCode.isNotBlank() && !isApplying) {
                        isApplying = true
                        viewModel.applyCoupon(manualCode)
                    }
                },
                onBack         = onBack,
                couponCount    = availableCoupons.size
            )
        },
        containerColor = AppColors.Background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading && availableCoupons.isEmpty() && !isApplying -> {
                    LoadingIndicator(
                        message  = stringResource(R.string.label_loading_coupons),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp)
                    )
                }

                availableCoupons.isEmpty() -> {
                    EmptyState(
                        icon       = Icons.Default.LocalOffer,
                        title      = stringResource(R.string.label_no_coupons),
                        subtitle   = stringResource(R.string.label_no_coupons_subtitle),
                        modifier   = Modifier
                            .fillMaxWidth()
                            .padding(32.dp)
                    )
                }

                else -> {
                    LazyColumn(
                        modifier       = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start   = 16.dp, end = 16.dp,
                            top     = 16.dp, bottom = 100.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            // Section label
                            Text(
                                text       = stringResource(R.string.label_available_coupons).uppercase(),
                                style      = MaterialTheme.typography.labelSmall.copy(
                                    fontSize      = 9.sp,
                                    letterSpacing = 0.7.sp
                                ),
                                fontWeight = FontWeight.Bold,
                                color      = AppColors.TextSecondary,
                                modifier   = Modifier.padding(bottom = 4.dp)
                            )
                        }

                        items(availableCoupons) { coupon ->
                            CouponCard(
                                coupon    = coupon,
                                onApply   = {
                                    isApplying = true
                                    viewModel.applyCoupon(coupon.code)
                                }
                            )
                        }
                    }
                }
            }

            // Error snackbar — pinned above system nav
            uiState.error?.let { error ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(16.dp),
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text(stringResource(R.string.label_dismiss))
                        }
                    },
                    containerColor = AppColors.Gray900,
                    contentColor   = AppColors.White,
                    shape          = RoundedCornerShape(12.dp)
                ) {
                    Text(error)
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// CouponTopBar
// Sits inside GradientTopBarWrapper. Back + title row, then a live
// BasicTextField entry bar with Apply button — all inside the gradient.
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun CouponTopBar(
    code: String,
    onCodeChange: (String) -> Unit,
    onClearCode: () -> Unit,
    isApplying: Boolean,
    focusRequester: FocusRequester,
    onApply: () -> Unit,
    onBack: () -> Unit,
    couponCount: Int
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ── Back + title ───────────────────────────────────────────────────
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.12f))
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = Icons.Default.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    tint               = HeaderText,
                    modifier           = Modifier.size(16.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = stringResource(R.string.title_apply_coupon),
                    style      = MaterialTheme.typography.titleMedium.copy(
                        fontWeight    = FontWeight.Bold,
                        letterSpacing = (-0.2).sp
                    ),
                    color = HeaderText
                )
                if (couponCount > 0) {
                    Text(
                        text  = "$couponCount offers available",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = HeaderMuted
                    )
                }
            }
        }

        // ── Code entry bar ─────────────────────────────────────────────────
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape    = RoundedCornerShape(12.dp),
            color    = FieldBg,
            border   = androidx.compose.foundation.BorderStroke(1.dp, FieldBorder)
        ) {
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector        = Icons.Default.LocalOffer,
                    contentDescription = null,
                    tint               = HeaderMuted,
                    modifier           = Modifier.size(16.dp)
                )

                BasicTextField(
                    value         = code,
                    onValueChange = onCodeChange,
                    modifier      = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                    textStyle     = TextStyle(
                        color       = HeaderText,
                        fontSize    = 13.sp,
                        fontWeight  = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    cursorBrush   = SolidColor(HeaderText),
                    singleLine    = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        imeAction      = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { onApply() }),
                    decorationBox   = { inner ->
                        Box {
                            if (code.isEmpty()) {
                                Text(
                                    text  = stringResource(R.string.hint_enter_code),
                                    style = TextStyle(
                                        color    = HeaderMuted,
                                        fontSize = 13.sp
                                    )
                                )
                            }
                            inner()
                        }
                    }
                )

                // Clear icon when there's text
                if (code.isNotEmpty() && !isApplying) {
                    Icon(
                        imageVector        = Icons.Default.Close,
                        contentDescription = "Clear",
                        tint               = HeaderMuted,
                        modifier           = Modifier
                            .size(16.dp)
                            .clickable { onClearCode() }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }

                // Apply button — inside the field bar on the right
                Surface(
                    onClick  = onApply,
                    shape    = RoundedCornerShape(9.dp),
                    color    = if (code.isNotBlank() && !isApplying)
                        Color.White
                    else
                        Color.White.copy(alpha = 0.25f),
                    modifier = Modifier.height(34.dp)
                ) {
                    Row(
                        modifier              = Modifier.padding(horizontal = 12.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (isApplying) {
                            CircularProgressIndicator(
                                modifier    = Modifier.size(14.dp),
                                color       = AppColors.PrimaryDeep,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text       = stringResource(R.string.label_apply),
                                style      = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                                fontWeight = FontWeight.Bold,
                                color      = if (code.isNotBlank()) AppColors.PrimaryDeep
                                else Color.White.copy(alpha = 0.50f)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// CouponCard
// White card with a 6dp coloured left accent bar.
// Layout: title + expiry chip | dashed code band with APPLY pill | description
// | info chips | divider | T&C row
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun CouponCard(
    coupon: CouponResponse,
    onApply: () -> Unit
) {
    // Derive accent colour from discount type
    val accentColor = if (coupon.discountType == "percentage")
        Color(0xFF4F46E5) // indigo
    else
        Color(0xFF059669) // emerald

    val codeBandBg    = accentColor.copy(alpha = 0.06f)
    val codeBandBorder= accentColor.copy(alpha = 0.30f)
    val applyBg       = accentColor.copy(alpha = 0.12f)

    Surface(
        modifier        = Modifier.fillMaxWidth(),
        shape           = RoundedCornerShape(14.dp),
        color           = Color.White,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {

            // ── Coloured left accent bar ───────────────────────────────────
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(accentColor)
            )

            // ── Card body ──────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {

                // Title + expiry
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(
                        text       = coupon.title,
                        style      = MaterialTheme.typography.titleSmall.copy(fontSize = 13.sp),
                        fontWeight = FontWeight.Bold,
                        color      = AppColors.TextPrimary
                    )
                    coupon.expiryDate?.let { expiry ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = AppColors.AmberLight
                        ) {
                            Row(
                                modifier              = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector        = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint               = AppColors.Warning,
                                    modifier           = Modifier.size(11.dp)
                                )
                                Text(
                                    text  = expiry,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    color = AppColors.Warning,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Code band — dashed border, code on left, APPLY pill on right
                Surface(
                    shape  = RoundedCornerShape(9.dp),
                    color  = codeBandBg,
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = codeBandBorder
                    )
                ) {
                    Row(
                        modifier              = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Text(
                            text       = coupon.code,
                            style      = MaterialTheme.typography.titleMedium.copy(
                                fontSize      = 15.sp,
                                letterSpacing = 1.5.sp
                            ),
                            fontWeight = FontWeight.ExtraBold,
                            color      = accentColor
                        )
                        Surface(
                            onClick = onApply,
                            shape   = RoundedCornerShape(7.dp),
                            color   = applyBg
                        ) {
                            Row(
                                modifier              = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text       = stringResource(R.string.label_apply_uppercase),
                                    style      = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                    fontWeight = FontWeight.Bold,
                                    color      = accentColor
                                )
                                Icon(
                                    imageVector        = Icons.Default.ArrowForward,
                                    contentDescription = null,
                                    tint               = accentColor,
                                    modifier           = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Description
                Text(
                    text  = coupon.description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize   = 11.sp,
                        lineHeight = 16.sp
                    ),
                    color = AppColors.TextSecondary
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Info chips
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (coupon.discountType == "percentage") {
                        InfoChip(
                            icon  = Icons.Default.Percent,
                            text  = "${coupon.discountValue}% OFF",
                            color = accentColor
                        )
                    } else {
                        InfoChip(
                            icon  = Icons.Default.CurrencyRupee,
                            text  = "₹${coupon.discountValue} OFF",
                            color = accentColor
                        )
                    }
                    InfoChip(
                        icon  = Icons.Default.ShoppingCart,
                        text  = "Min ₹${coupon.minOrderValue}",
                        color = AppColors.TextSecondary
                    )
                    if (coupon.maxDiscount != null && coupon.discountType == "percentage") {
                        InfoChip(
                            icon  = Icons.Default.TrendingDown,
                            text  = "Max ₹${coupon.maxDiscount}",
                            color = AppColors.TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = AppColors.Divider, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(8.dp))

                // T&C row
                Row(
                    verticalAlignment     = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector        = Icons.Default.Info,
                        contentDescription = null,
                        tint               = AppColors.TextSecondary.copy(alpha = 0.50f),
                        modifier           = Modifier.size(13.dp)
                    )
                    Text(
                        text  = coupon.terms,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize   = 10.sp,
                            lineHeight = 14.sp
                        ),
                        color = AppColors.TextSecondary
                    )
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// InfoChip — small rounded chip with icon + text
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun InfoChip(
    icon: ImageVector,
    text: String,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.08f)
    ) {
        Row(
            modifier              = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Icon(
                imageVector        = icon,
                contentDescription = null,
                tint               = color,
                modifier           = Modifier.size(11.dp)
            )
            Text(
                text       = text,
                style      = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                fontWeight = FontWeight.SemiBold,
                color      = color
            )
        }
    }
}