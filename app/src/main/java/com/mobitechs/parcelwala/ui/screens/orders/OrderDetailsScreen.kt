// ui/screens/orders/OrderDetailsScreen.kt
package com.mobitechs.parcelwala.ui.screens.orders

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Support
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobitechs.parcelwala.R
import com.mobitechs.parcelwala.data.model.response.OrderResponse
import com.mobitechs.parcelwala.ui.components.StatusBarScaffold
import com.mobitechs.parcelwala.ui.theme.AppColors
import com.mobitechs.parcelwala.ui.theme.AppRadius
import com.mobitechs.parcelwala.ui.theme.AppSpacing
import com.mobitechs.parcelwala.utils.Constants
import com.mobitechs.parcelwala.utils.DateTimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailsScreen(
    order: OrderResponse,
    onBack: () -> Unit,
    onBookAgain: (OrderResponse) -> Unit = {},
    onCallDriver: (String) -> Unit = {},
    onCallSupport: () -> Unit = {}
) {
    val statusConfig  = getStatusConfig(order.status)
    val headerColor   = statusConfig.color
    val isActive      = isActiveStatus(order.status)
    val isCompleted   = isCompletedStatus(order.status)
    val isCancelled   = isCancelledStatus(order.status)
    val showBookAgain = isCompleted || isCancelled

    // StatusBarScaffold handles status bar color + restore on dispose
    StatusBarScaffold(
        statusBarColor = headerColor,
        darkStatusBarIcons = false,
        useGradientTopBar = false,          // dynamic color — header manages its own bg
        containerColor = AppColors.LightGray50
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                // ── Coloured header ───────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(headerColor)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()
                        .padding(top = 30.dp)) {

                        // TopAppBar inside the coloured header
                        TopAppBar(
                            title = {
                                Column {
                                    Text(
                                        text = stringResource(R.string.order_details),
                                        style = MaterialTheme.typography.headlineSmall.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = AppColors.White
                                    )
                                    Text(
                                        text = statusConfig.resolveLabel(),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 11.sp
                                        ),
                                        color = AppColors.White.copy(alpha = 0.8f)
                                    )
                                }
                            },
                            navigationIcon = {
                                IconButton(onClick = onBack) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowBack,
                                        contentDescription = stringResource(R.string.back),
                                        tint = AppColors.White,
                                    )
                                }
                            },
                            actions = {
//                                IconButton(onClick = onCallSupport) {
//                                    Box(
//                                        modifier = Modifier
//                                            .size(34.dp)
//                                            .clip(CircleShape)
//                                            .background(AppColors.White.copy(alpha = 0.15f)),
//                                        contentAlignment = Alignment.Center
//                                    ) {
//                                        Icon(
//                                            imageVector = Icons.Default.Support,
//                                            contentDescription = stringResource(R.string.support),
//                                            tint = AppColors.White,
//                                            modifier = Modifier.size(18.dp)
//                                        )
//                                    }
//                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Color.Transparent
                            ),
                            windowInsets = WindowInsets(0)   // ← prevents double status bar padding
                        )

                        // Status + fare row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    start = AppSpacing.LG,
                                    end = AppSpacing.LG,
                                    bottom = AppSpacing.X3L
                                ),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column {
                                Text(
                                    text = order.bookingNumber,
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontWeight = FontWeight.Bold,fontSize = 14.sp
                                    ),
                                    color = AppColors.White
                                )
                                Spacer(modifier = Modifier.height(1.dp))
                                Text(
                                    text = buildString {
                                        append(DateTimeUtils.formatDateTime(order.createdAt))
                                        append(" · ")
                                        append(order.vehicleType)
                                    },
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                    color = AppColors.White.copy(alpha = 0.75f)
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = stringResource(R.string.fare_format, order.fare.toInt()),
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = AppColors.White
                                )
                                order.paymentMethod?.takeIf { it.isNotBlank() }?.let { method ->
                                    Text(
                                        text = method.replaceFirstChar { it.uppercase() },
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                        color = AppColors.White.copy(alpha = 0.75f)
                                    )
                                }
                            }
                        }
                    }
                }

                // ── Content card peels up over header ─────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = (-AppSpacing.XL))
                        .clip(RoundedCornerShape(topStart = AppRadius.XL, topEnd = AppRadius.XL))
                        .background(AppColors.LightGray50)
                        .padding(top = AppSpacing.LG)
                ) {
                    StatsStrip(order = order, isCompleted = isCompleted, isActive = isActive)

                    if (isCancelled && !order.cancellationReason.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(AppSpacing.SM))
                        CancellationCard(order = order)
                    }

                    if (!order.otp.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(AppSpacing.SM))
                        OtpCard(otp = order.otp!!, headerColor = headerColor)
                    }

                    if (!order.driverName.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(AppSpacing.SM))
                        DriverCard(
                            order = order,
                            headerColor = headerColor,
                            onCallDriver = { order.driverPhone?.let { onCallDriver(it) } }
                        )
                    }

                    Spacer(modifier = Modifier.height(AppSpacing.SM))
                    RouteCard(order = order)

                    val hasGoods = order.goodsType != null
                            || order.goodsWeight != null
                            || order.goodsPackages != null
                            || order.goodsValue != null
                    if (hasGoods) {
                        Spacer(modifier = Modifier.height(AppSpacing.SM))
                        GoodsCard(order = order)
                    }

                    Spacer(modifier = Modifier.height(AppSpacing.SM))
                    PaymentCard(order = order, statusColor = headerColor)

                    if (isCompleted && (order.rating ?: 0) > 0) {
                        Spacer(modifier = Modifier.height(AppSpacing.SM))
                        RatingCard(order = order)
                    }

                    Spacer(modifier = Modifier.height(100.dp))
                }
            }

            // ── Bottom bar ────────────────────────────────────────────────
            Surface(color = AppColors.White, shadowElevation = 8.dp) {
                if (showBookAgain) {
                    Button(
                        onClick = { onBookAgain(order) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = AppSpacing.LG,
                                end = AppSpacing.LG,
                                top = AppSpacing.LG,
                                bottom = AppSpacing.LG
                            )
                            .navigationBarsPadding(),   // ← clears gesture/3-button nav bar
                        colors = ButtonDefaults.buttonColors(containerColor = headerColor),
                        shape = RoundedCornerShape(AppRadius.MD),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(AppSpacing.SM))
                        Text(stringResource(R.string.book_again), fontWeight = FontWeight.Bold)
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(AppSpacing.LG)
                            .navigationBarsPadding(),   // ← clears gesture/3-button nav bar
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.book_again_available_after_delivery),
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.TextSecondary
                        )
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Section card wrapper
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun SectionCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.LG),
        shape = RoundedCornerShape(AppRadius.LG),
        color = AppColors.White,
        shadowElevation = 1.dp,
        tonalElevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(AppSpacing.LG)) {
            content()
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.SemiBold,
            fontSize = 10.sp,
            letterSpacing = 0.8.sp
        ),
        color = AppColors.TextSecondary,
        modifier = Modifier.padding(bottom = AppSpacing.SM)
    )
}

// ══════════════════════════════════════════════════════════════════════════════
// Stats strip
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun StatsStrip(
    order: OrderResponse,
    isCompleted: Boolean,
    isActive: Boolean
) {
    SectionCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                icon = Icons.Default.Route,
                label = stringResource(R.string.distance_label),
                value = order.distance
                    ?.let { stringResource(R.string.distance_format, it) }
                    ?: stringResource(R.string.na)
            )

            StatDivider()

            if (isActive && !order.driverName.isNullOrBlank()) {
                StatItem(
                    icon = Icons.Default.Person,
                    label = stringResource(R.string.driver_fallback),
                    value = order.driverName!!
                )
            } else {
                StatItem(
                    icon = Icons.Default.Timer,
                    label = stringResource(R.string.duration_label),
                    value = order.estimatedTime?.takeIf { it.isNotBlank() }
                        ?: stringResource(R.string.na)
                )
            }

            if (isCompleted && (order.rating ?: 0) > 0) {
                StatDivider()
                StatItem(
                    icon = Icons.Default.Star,
                    label = stringResource(R.string.your_rating),
                    value = "★".repeat(order.rating ?: 0)
                )
            }
        }
    }
}

@Composable
private fun StatItem(icon: ImageVector, label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier.padding(vertical = AppSpacing.XS)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = AppColors.Primary,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = AppColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = AppColors.TextSecondary
        )
    }
}

@Composable
private fun StatDivider() {
    Box(
        modifier = Modifier
            .height(40.dp)
            .width(1.dp)
            .background(AppColors.DividerLight)
    )
}

// ══════════════════════════════════════════════════════════════════════════════
// Cancellation card
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun CancellationCard(order: OrderResponse) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.LG),
        shape = RoundedCornerShape(AppRadius.LG),
        color = AppColors.ErrorLight,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(AppSpacing.LG),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.MD)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(AppColors.Drop.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Cancel,
                    contentDescription = null,
                    tint = AppColors.Drop,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.order_cancelled),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = AppColors.Drop
                )
                Text(
                    text = order.cancellationReason!!,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = AppColors.Drop.copy(alpha = 0.8f)
                )
                if (!order.cancelledBy.isNullOrBlank()) {
                    Text(
                        text = stringResource(R.string.cancelled_by_format, order.cancelledBy!!),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = AppColors.Drop.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// OTP card
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun OtpCard(otp: String, headerColor: Color) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.LG),
        shape = RoundedCornerShape(AppRadius.LG),
        color = headerColor.copy(alpha = 0.08f),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.LG),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = stringResource(R.string.delivery_otp),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = AppColors.TextPrimary
                )
                Text(
                    text = stringResource(R.string.share_otp_with_driver),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = AppColors.TextSecondary
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.XS)) {
                otp.forEach { digit ->
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(AppRadius.SM))
                            .background(headerColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = digit.toString(),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = AppColors.White
                        )
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Driver card
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun DriverCard(
    order: OrderResponse,
    headerColor: Color,
    onCallDriver: () -> Unit
) {
    SectionCard {
        SectionLabel(stringResource(R.string.driver_details).uppercase())
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.MD)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(AppColors.LightGray50),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "👤", fontSize = 22.sp)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = order.driverName ?: stringResource(R.string.driver_fallback),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = AppColors.TextPrimary
                )
                if (!order.vehicleNumber.isNullOrBlank()) {
                    Text(
                        text = order.vehicleNumber!!,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = AppColors.TextSecondary
                    )
                }
                if ((order.driverRating ?: 0.0) > 0.0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = AppColors.Warning,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "${order.driverRating}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = AppColors.TextPrimary
                        )
                    }
                }
            }
            if (!order.driverPhone.isNullOrBlank()) {
                FilledIconButton(
                    onClick = onCallDriver,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = headerColor.copy(alpha = 0.12f)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = stringResource(R.string.call_driver),
                        tint = headerColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Route card
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun RouteCard(order: OrderResponse) {
    SectionCard {
        SectionLabel(stringResource(R.string.label_route).uppercase())

        RouteRow(
            color = AppColors.Pickup,
            contactName = order.pickupContactName,
            contactPhone = order.pickupContactPhone,
            address = order.pickupAddress
        )

        Column(
            modifier = Modifier.padding(start = 6.dp, top = 3.dp, bottom = 3.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            repeat(3) {
                Box(
                    modifier = Modifier
                        .size(3.dp)
                        .clip(CircleShape)
                        .background(AppColors.DividerLight)
                )
            }
        }

        RouteRow(
            color = AppColors.Drop,
            contactName = order.dropContactName,
            contactPhone = order.dropContactPhone,
            address = order.dropAddress
        )
    }
}

@Composable
private fun RouteRow(
    color: Color,
    contactName: String?,
    contactPhone: String?,
    address: String
) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.SM)
    ) {
        Box(
            modifier = Modifier
                .padding(top = 3.dp)
                .size(14.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            if (!contactName.isNullOrBlank()) {
                Text(
                    text = contactName,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    ),
                    color = AppColors.TextPrimary
                )
            }
            Text(
                text = address,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                ),
                color = AppColors.TextSecondary
            )
            if (!contactPhone.isNullOrBlank()) {
                Text(
                    text = contactPhone,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = AppColors.Primary
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Goods card
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun GoodsCard(order: OrderResponse) {
    SectionCard {
        SectionLabel(stringResource(R.string.goods_information).uppercase())
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            order.goodsType?.let {
                GoodsStatItem(label = stringResource(R.string.type_label), value = it)
            }
            order.goodsWeight?.let {
                if (order.goodsType != null) GoodsStatDivider()
                GoodsStatItem(
                    label = stringResource(R.string.weight_label),
                    value = stringResource(R.string.weight_format, it)
                )
            }
            order.goodsPackages?.let {
                if (order.goodsType != null || order.goodsWeight != null) GoodsStatDivider()
                GoodsStatItem(label = stringResource(R.string.packages_label), value = "$it")
            }
            order.goodsValue?.let {
                if (order.goodsType != null || order.goodsWeight != null || order.goodsPackages != null)
                    GoodsStatDivider()
                GoodsStatItem(
                    label = stringResource(R.string.value_label),
                    value = stringResource(R.string.value_format, it)
                )
            }
        }
        if (!order.instructions.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(AppSpacing.MD))
            HorizontalDivider(color = AppColors.DividerLight)
            Spacer(modifier = Modifier.height(AppSpacing.MD))
            Text(
                text = stringResource(R.string.instructions_label),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.sp
                ),
                color = AppColors.TextSecondary
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = order.instructions!!,
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextPrimary
            )
        }
    }
}

@Composable
private fun GoodsStatItem(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = AppColors.TextPrimary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = AppColors.TextSecondary
        )
    }
}

@Composable
private fun GoodsStatDivider() {
    Box(
        modifier = Modifier
            .height(32.dp)
            .width(1.dp)
            .background(AppColors.DividerLight)
    )
}

// ══════════════════════════════════════════════════════════════════════════════
// Payment card
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun PaymentCard(order: OrderResponse, statusColor: Color) {
    val hasDiscount = (order.discountAmount ?: 0.0) > 0.0
    val hasCoupon   = !order.couponCode.isNullOrBlank()
    val hasGstin    = !order.gstin.isNullOrBlank()

    SectionCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionLabel(stringResource(R.string.payment_details).uppercase())
            if (!order.paymentStatus.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(AppRadius.Full),
                    color = statusColor.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = order.paymentStatus!!,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        ),
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = AppSpacing.SM, vertical = 3.dp)
                    )
                }
            }
        }

        PaymentRow(
            label = stringResource(R.string.payment_method),
            trailing = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.XS)
                ) {
                    Icon(
                        imageVector = getPaymentIcon(order.paymentMethod),
                        contentDescription = null,
                        tint = AppColors.Primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = order.paymentMethod?.replaceFirstChar { it.uppercase() }
                            ?: stringResource(R.string.na),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp
                        ),
                        color = AppColors.TextPrimary
                    )
                }
            }
        )

        HorizontalDivider(color = AppColors.DividerLight, modifier = Modifier.padding(vertical = AppSpacing.SM))

        order.baseFare?.let { base ->
            PaymentRow(
                label = stringResource(R.string.base_fare),
                value = stringResource(R.string.fare_format, base.toInt())
            )
        } ?: PaymentRow(
            label = stringResource(R.string.trip_fare_label),
            value = stringResource(R.string.fare_format, order.fare.toInt())
        )

        if (order.baseFare != null && order.baseFare < order.fare && !hasDiscount) {
            val platformFee = order.fare - order.baseFare - (order.discountAmount ?: 0.0)
            if (platformFee > 0) {
                Spacer(modifier = Modifier.height(AppSpacing.XS))
                PaymentRow(
                    label = stringResource(R.string.platform_fee),
                    value = stringResource(R.string.fare_format, platformFee.toInt())
                )
            }
        }

        if (hasCoupon || hasDiscount) {
            Spacer(modifier = Modifier.height(AppSpacing.XS))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.XS)
                ) {
                    Text(text = "🏷️", fontSize = 13.sp)
                    Text(
                        text = if (hasCoupon)
                            stringResource(R.string.coupon_format, order.couponCode!!)
                        else stringResource(R.string.coupon_discount),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = AppColors.Pickup
                    )
                }
                Text(
                    text = stringResource(
                        R.string.discount_format,
                        (order.discountAmount ?: 0.0).toInt()
                    ),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    ),
                    color = AppColors.Pickup
                )
            }
        }

        HorizontalDivider(color = AppColors.DividerLight, modifier = Modifier.padding(vertical = AppSpacing.SM))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.total_amount),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = AppColors.TextPrimary
            )
            Text(
                text = stringResource(R.string.fare_format, order.fare.toInt()),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = statusColor
            )
        }

        if (hasDiscount) {
            Spacer(modifier = Modifier.height(AppSpacing.SM))
            Surface(
                shape = RoundedCornerShape(AppRadius.SM),
                color = AppColors.GreenLight
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppSpacing.MD, vertical = AppSpacing.XS),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.XS)
                ) {
                    Text(text = "🎉", fontSize = 13.sp)
                    Text(
                        text = stringResource(
                            R.string.you_save_format,
                            (order.discountAmount ?: 0.0).toInt().toString()
                        ),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp
                        ),
                        color = AppColors.Pickup
                    )
                }
            }
        }

        if (hasGstin) {
            HorizontalDivider(
                color = AppColors.DividerLight,
                modifier = Modifier.padding(top = AppSpacing.MD)
            )
            Spacer(modifier = Modifier.height(AppSpacing.SM))
            PaymentRow(
                label = stringResource(R.string.gstin_info_label),
                value = order.gstin!!
            )
        }
    }
}

@Composable
private fun PaymentRow(
    label: String,
    value: String? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
            color = AppColors.TextSecondary
        )
        if (trailing != null) {
            trailing()
        } else if (value != null) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = AppColors.TextPrimary
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Rating card
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun RatingCard(order: OrderResponse) {
    SectionCard {
        SectionLabel(stringResource(R.string.your_rating).uppercase())
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.SM)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                repeat(5) { index ->
                    Icon(
                        imageVector = if (index < (order.rating ?: 0))
                            Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = null,
                        tint = AppColors.Warning,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            if (!order.review.isNullOrBlank()) {
                Text(
                    text = stringResource(R.string.feedback_quote_format, order.review!!),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    ),
                    color = AppColors.TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}