package com.mobitechs.parcelwala.ui.screens.payments

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mobitechs.parcelwala.R
import com.mobitechs.parcelwala.data.model.response.formatRupee
import com.mobitechs.parcelwala.ui.theme.AppColors
import com.mobitechs.parcelwala.ui.viewmodel.PaymentEvent
import com.mobitechs.parcelwala.ui.viewmodel.PaymentViewModel
import java.text.NumberFormat
import java.util.Locale

enum class PaymentMethodType(val key: String, val label: String) {
    CASH("cash", "Cash"),
    WALLET("wallet", "Wallet"),
    UPI("upi", "UPI"),
    CARD("card", "Card"),
    NET_BANKING("netbanking", "Net Banking")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDeliveryPaymentScreen(
    bookingId: String,
    roundedFare: Double,
    waitingCharge: Double,
    discount: Double,
    driverName: String,
    paymentMethod: String,
    onPaymentComplete: () -> Unit,
    onPaymentSkipped: () -> Unit,
    paymentViewModel: PaymentViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val uiState by paymentViewModel.uiState.collectAsState()
    val totalFare = roundedFare
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

    var showPaymentError by remember { mutableStateOf<String?>(null) }
    var paymentProcessing by remember { mutableStateOf(false) }
    var selectedMethod by remember { mutableStateOf(paymentMethod.lowercase()) }

    val snackbarHostState = remember { SnackbarHostState() }

    val isCash = selectedMethod == PaymentMethodType.CASH.key
    val isWallet = selectedMethod == PaymentMethodType.WALLET.key
    val isOnline = selectedMethod in listOf(
        PaymentMethodType.UPI.key,
        PaymentMethodType.CARD.key,
        PaymentMethodType.NET_BANKING.key
    )

    LaunchedEffect(Unit) {
        paymentViewModel.paymentEvent.collect { event ->
            when (event) {
                is PaymentEvent.OpenRazorpayCheckout -> {
                    activity?.let { act ->
                        paymentViewModel.openRazorpayCheckout(act, event.orderResponse)
                    }
                }

                is PaymentEvent.PaymentSuccess -> {
                    paymentProcessing = false
                    onPaymentComplete()
                }

                is PaymentEvent.PaymentFailure -> {
                    paymentProcessing = false
                    showPaymentError = event.message
                    snackbarHostState.showSnackbar(
                        context.getString(R.string.label_payment_failed_snackbar, event.message)
                    )
                }

                is PaymentEvent.WalletTopupSuccess -> {}
            }
        }
    }

    LaunchedEffect(selectedMethod) {
        if (isWallet) {
            paymentViewModel.loadWalletBalance()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = AppColors.Background
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
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    AppColors.Primary.copy(alpha = 0.2f),
                                    AppColors.Primary.copy(alpha = 0.05f)
                                )
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Payment,
                        contentDescription = null,
                        tint = AppColors.Primary,
                        modifier = Modifier.size(48.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.label_complete_payment),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary
                )

                Text(
                    text = stringResource(R.string.label_driver_arrived_delivery, driverName),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextSecondary
                )

                Spacer(modifier = Modifier.height(24.dp))

                FareBreakdownCard(
                    roundedFare = roundedFare,
                    waitingCharge = waitingCharge,
                    discount = discount
                )

                Spacer(modifier = Modifier.height(20.dp))

                PaymentMethodCard(
                    selectedMethod = selectedMethod,
                    onMethodSelected = { selectedMethod = it },
                    walletBalance = uiState.walletBalance,
                    totalFare = totalFare,
                    isWallet = isWallet,
                    currencyFormat = currencyFormat
                )

                AnimatedVisibility(visible = isWallet && uiState.walletBalance < totalFare) {
                    WarningCard(
                        title = stringResource(R.string.label_insufficient_balance_title),
                        message = stringResource(
                            R.string.label_insufficient_balance_message,
                            formatRupee(totalFare - uiState.walletBalance)
                        ),
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }

                AnimatedVisibility(visible = showPaymentError != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = AppColors.Error.copy(alpha = 0.08f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = AppColors.Error,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = showPaymentError ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = AppColors.Error,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { showPaymentError = null },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = stringResource(R.string.label_dismiss),
                                    tint = AppColors.Error,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            Surface(shadowElevation = 8.dp, color = AppColors.White) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .navigationBarsPadding()
                ) {
                    val canPay = when {
                        isCash -> true
                        isWallet -> uiState.walletBalance >= totalFare
                        isOnline -> true
                        else -> false
                    }

                    Button(
                        onClick = {
                            showPaymentError = null
                            paymentProcessing = true

                            when {
                                isCash -> {
                                    paymentProcessing = false
                                    onPaymentSkipped()
                                }

                                isWallet || isOnline -> {
                                    val bookingIdInt = bookingId.toIntOrNull()
                                    if (bookingIdInt != null) {
                                        paymentViewModel.initiateBookingPayment(
                                            bookingId = bookingIdInt,
                                            amount = totalFare,
                                            paymentMethod = if (isWallet) "wallet" else selectedMethod,
                                            notes = if (isWallet)
                                                context.getString(
                                                    R.string.label_wallet_for_booking,
                                                    bookingId
                                                )
                                            else
                                                context.getString(
                                                    R.string.label_payment_for_booking,
                                                    bookingId
                                                )
                                        )
                                    } else {
                                        paymentProcessing = false
                                        showPaymentError =
                                            context.getString(R.string.label_invalid_booking_id)
                                    }
                                }
                            }
                        },
                        enabled = canPay && !paymentProcessing && !uiState.isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isCash) AppColors.Pickup else AppColors.Primary,
                            disabledContainerColor = AppColors.Border
                        )
                    ) {
                        if (paymentProcessing || uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = AppColors.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = stringResource(R.string.label_processing_ellipsis),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        } else {
                            Icon(
                                imageVector = when {
                                    isCash -> Icons.Default.MonetizationOn
                                    isWallet -> Icons.Default.AccountBalanceWallet
                                    else -> Icons.Default.Payment
                                },
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = when {
                                    isCash -> stringResource(
                                        R.string.label_confirm_cash_format,
                                        formatRupee(totalFare)
                                    )

                                    isWallet -> stringResource(
                                        R.string.label_pay_from_wallet_format,
                                        formatRupee(totalFare)
                                    )

                                    else -> stringResource(
                                        R.string.label_pay_amount_format,
                                        formatRupee(totalFare)
                                    )
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }

                    if (isCash) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(
                                R.string.label_cash_pay_info,
                                formatRupee(totalFare)
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = AppColors.TextHint,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FareBreakdownCard(
    roundedFare: Double,
    waitingCharge: Double,
    discount: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = stringResource(R.string.label_fare_summary),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            FareRow(
                label = stringResource(R.string.label_trip_fare),
                amount = formatRupee(roundedFare),
                icon = Icons.Default.LocalShipping,
                iconTint = AppColors.Primary
            )

            if (waitingCharge > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                FareRow(
                    label = stringResource(R.string.label_waiting_charge_post),
                    amount = stringResource(R.string.label_included),
                    icon = Icons.Outlined.Timer,
                    iconTint = AppColors.Warning,
                    amountColor = AppColors.TextSecondary
                )
            }

            if (discount > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                FareRow(
                    label = stringResource(R.string.label_coupon_discount),
                    amount = stringResource(
                        R.string.label_discount_amount_format,
                        formatRupee(discount)
                    ),
                    icon = Icons.Default.LocalOffer,
                    iconTint = AppColors.Pickup,
                    amountColor = AppColors.Pickup
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = AppColors.Border)
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.label_total_amount_post),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary
                )
                Text(
                    text = formatRupee(roundedFare),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.Primary
                )
            }
        }
    }
}

@Composable
private fun PaymentMethodCard(
    selectedMethod: String,
    onMethodSelected: (String) -> Unit,
    walletBalance: Double,
    totalFare: Double,
    isWallet: Boolean,
    currencyFormat: NumberFormat
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = stringResource(R.string.label_payment_method_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            PaymentOptionItem(
                icon = Icons.Default.MonetizationOn,
                title = stringResource(R.string.label_cash_option),
                subtitle = stringResource(R.string.label_pay_driver_directly),
                iconColor = AppColors.Pickup,
                isSelected = selectedMethod == PaymentMethodType.CASH.key,
                onClick = { onMethodSelected(PaymentMethodType.CASH.key) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            PaymentOptionItem(
                icon = Icons.Default.AccountBalanceWallet,
                title = stringResource(R.string.label_wallet_option),
                subtitle = if (walletBalance > 0)
                    stringResource(
                        R.string.label_wallet_balance_format,
                        currencyFormat.format(walletBalance)
                    )
                else stringResource(R.string.label_check_balance),
                iconColor = AppColors.Primary,
                isSelected = selectedMethod == PaymentMethodType.WALLET.key,
                onClick = { onMethodSelected(PaymentMethodType.WALLET.key) },
                trailingContent = {
                    if (isWallet && walletBalance < totalFare) {
                        Text(
                            text = stringResource(R.string.label_low_balance),
                            style = MaterialTheme.typography.labelSmall,
                            color = AppColors.Error,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            PaymentOptionItem(
                icon = Icons.Default.PhoneAndroid,
                title = stringResource(R.string.label_upi_option),
                subtitle = stringResource(R.string.label_upi_apps),
                iconColor = AppColors.UpiGreen,
                isSelected = selectedMethod == PaymentMethodType.UPI.key,
                onClick = { onMethodSelected(PaymentMethodType.UPI.key) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            PaymentOptionItem(
                icon = Icons.Default.CreditCard,
                title = stringResource(R.string.label_debit_credit_card),
                subtitle = stringResource(R.string.label_card_networks),
                iconColor = AppColors.Blue,
                isSelected = selectedMethod == PaymentMethodType.CARD.key,
                onClick = { onMethodSelected(PaymentMethodType.CARD.key) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            PaymentOptionItem(
                icon = Icons.Default.AccountBalance,
                title = stringResource(R.string.label_net_banking_option),
                subtitle = stringResource(R.string.label_all_major_banks),
                iconColor = AppColors.Warning,
                isSelected = selectedMethod == PaymentMethodType.NET_BANKING.key,
                onClick = { onMethodSelected(PaymentMethodType.NET_BANKING.key) }
            )
        }
    }
}

@Composable
private fun WarningCard(
    title: String,
    message: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.Error.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = AppColors.Error,
                modifier = Modifier.size(20.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.Error
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextSecondary
                )
            }
        }
    }
}

@Composable
private fun FareRow(
    label: String,
    amount: String,
    icon: ImageVector,
    iconTint: Color,
    amountColor: Color = AppColors.TextPrimary
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextSecondary
            )
        }
        Text(
            text = amount,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = amountColor
        )
    }
}

@Composable
private fun PaymentOptionItem(
    icon: ImageVector, title: String, subtitle: String, iconColor: Color,
    isSelected: Boolean, onClick: () -> Unit, trailingContent: @Composable (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) AppColors.Primary.copy(
                alpha = 0.06f
            ) else AppColors.Surface
        ),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, AppColors.Primary)
        else androidx.compose.foundation.BorderStroke(1.dp, AppColors.Border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.1f)), contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) AppColors.Primary else AppColors.TextPrimary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextSecondary
                )
            }
            trailingContent?.invoke()
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(selectedColor = AppColors.Primary)
            )
        }
    }
}