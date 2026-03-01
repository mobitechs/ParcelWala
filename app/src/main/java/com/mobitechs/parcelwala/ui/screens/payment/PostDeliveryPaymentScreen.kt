package com.mobitechs.parcelwala.ui.screens.payments

import android.app.Activity
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.mobitechs.parcelwala.data.model.response.formatPrice
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

/**
 * Post-delivery payment screen shown when driver arrives at delivery location.
 *
 * Flow:
 * - If CASH → dismiss screen, wait for driver to confirm → server sends payment_success
 * - If WALLET → check balance, deduct from wallet via API → server sends payment_success
 * - If UPI/CARD/NETBANKING → open Razorpay checkout → server sends payment_success
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDeliveryPaymentScreen(
    bookingId: String,
    roundedFare: Double, // ✅ Int → Double
    waitingCharge: Double, // ✅ Int → Double
    discount: Double, // ✅ Int → Double
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
                    snackbarHostState.showSnackbar("Payment failed: ${event.message}")
                }
                is PaymentEvent.WalletTopupSuccess -> { }
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

                // ── HEADER ──
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
                    text = "Complete Payment",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary
                )

                Text(
                    text = "Driver arrived at delivery • $driverName",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextSecondary
                )

                Spacer(modifier = Modifier.height(24.dp))

                // ── FARE BREAKDOWN CARD ──
                FareBreakdownCard(
                    roundedFare = roundedFare,
                    waitingCharge = waitingCharge,
                    discount  = discount
                )

                Spacer(modifier = Modifier.height(20.dp))

                // ── PAYMENT METHOD SELECTION ──
                PaymentMethodCard(
                    selectedMethod = selectedMethod,
                    onMethodSelected = { selectedMethod = it },
                    walletBalance = uiState.walletBalance,
                    totalFare = totalFare,
                    isWallet = isWallet,
                    currencyFormat = currencyFormat
                )

                // Wallet insufficient balance warning
                AnimatedVisibility(visible = isWallet && uiState.walletBalance < totalFare) {
                    WarningCard(
                        title = "Insufficient wallet balance",
                        message = "You need ${formatRupee(totalFare - uiState.walletBalance)} more. Please choose another payment method or top up your wallet.",
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }

                // Payment error display
                AnimatedVisibility(visible = showPaymentError != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = AppColors.Error.copy(alpha = 0.08f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = AppColors.Error, modifier = Modifier.size(20.dp))
                            Text(text = showPaymentError ?: "", style = MaterialTheme.typography.bodySmall, color = AppColors.Error, modifier = Modifier.weight(1f))
                            IconButton(onClick = { showPaymentError = null }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = AppColors.Error, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // ── BOTTOM PAY BUTTON ──
            Surface(shadowElevation = 8.dp, color = Color.White) {
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
                                            notes = "${if (isWallet) "Wallet" else "Payment"} for booking #$bookingId"
                                        )
                                    } else {
                                        paymentProcessing = false
                                        showPaymentError = "Invalid booking ID"
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
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = "Processing...", fontWeight = FontWeight.Bold, fontSize = 16.sp)
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
                                    isCash -> "Confirm Cash Payment  •  ${formatRupee(totalFare)}" // ✅
                                    isWallet -> "Pay from Wallet  •  ${formatRupee(totalFare)}" // ✅
                                    else -> "Pay ${formatRupee(totalFare)}" // ✅
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }

                    // Info text for cash payments
                    if (isCash) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Pay ${formatRupee(totalFare)} cash to driver. Driver will confirm payment on their end.", // ✅
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

// ══════════════════════════════════════════════════════════════
// EXTRACTED COMPOSABLES — Cleaner, less duplication
// ══════════════════════════════════════════════════════════════

@Composable
private fun FareBreakdownCard(
    roundedFare: Double,
    waitingCharge: Double,
    discount: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "Fare Summary",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            FareRow(
                label = "Trip Fare",
                amount = formatRupee(roundedFare),
                icon = Icons.Default.LocalShipping,
                iconTint = AppColors.Primary
            )

            if (waitingCharge > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                FareRow(
                    label = "Waiting Charge",
                    amount = "Included",
                    icon = Icons.Outlined.Timer,
                    iconTint = AppColors.Warning,
                    amountColor = AppColors.TextSecondary
                )
            }

            if (discount > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                FareRow(
                    label = "Coupon Discount",
                    amount = "- ${formatRupee(discount)}",
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
                    text = "Total Amount",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary
                )
                Text(
                    text = formatRupee(roundedFare), // ✅
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
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "Payment Method",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            PaymentOptionItem(
                icon = Icons.Default.MonetizationOn,
                title = "Cash",
                subtitle = "Pay driver directly",
                iconColor = AppColors.Pickup,
                isSelected = selectedMethod == PaymentMethodType.CASH.key,
                onClick = { onMethodSelected(PaymentMethodType.CASH.key) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            PaymentOptionItem(
                icon = Icons.Default.AccountBalanceWallet,
                title = "Wallet",
                subtitle = if (walletBalance > 0)
                    "Balance: ${currencyFormat.format(walletBalance)}"
                else "Check balance",
                iconColor = AppColors.Primary,
                isSelected = selectedMethod == PaymentMethodType.WALLET.key,
                onClick = { onMethodSelected(PaymentMethodType.WALLET.key) },
                trailingContent = {
                    if (isWallet && walletBalance < totalFare) {
                        Text(
                            text = "Low balance",
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
                title = "UPI",
                subtitle = "GPay, PhonePe, Paytm, etc.",
                iconColor = Color(0xFF4CAF50),
                isSelected = selectedMethod == PaymentMethodType.UPI.key,
                onClick = { onMethodSelected(PaymentMethodType.UPI.key) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            PaymentOptionItem(
                icon = Icons.Default.CreditCard,
                title = "Debit/Credit Card",
                subtitle = "Visa, MasterCard, RuPay",
                iconColor = AppColors.Blue,
                isSelected = selectedMethod == PaymentMethodType.CARD.key,
                onClick = { onMethodSelected(PaymentMethodType.CARD.key) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            PaymentOptionItem(
                icon = Icons.Default.AccountBalance,
                title = "Net Banking",
                subtitle = "All major banks",
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
            Icon(Icons.Default.Warning, contentDescription = null, tint = AppColors.Error, modifier = Modifier.size(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = AppColors.Error)
                Text(text = message, style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════
// HELPER COMPOSABLES
// ══════════════════════════════════════════════════════════════

@Composable
private fun FareRow(
    label: String, amount: String, icon: ImageVector, iconTint: Color, amountColor: Color = AppColors.TextPrimary
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
            Text(text = label, style = MaterialTheme.typography.bodyMedium, color = AppColors.TextSecondary)
        }
        Text(text = amount, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = amountColor)
    }
}

@Composable
private fun PaymentOptionItem(
    icon: ImageVector, title: String, subtitle: String, iconColor: Color,
    isSelected: Boolean, onClick: () -> Unit, trailingContent: @Composable (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = if (isSelected) AppColors.Primary.copy(alpha = 0.06f) else AppColors.Surface),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, AppColors.Primary)
        else androidx.compose.foundation.BorderStroke(1.dp, AppColors.Border)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(iconColor.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(22.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, color = if (isSelected) AppColors.Primary else AppColors.TextPrimary)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
            }
            trailingContent?.invoke()
            RadioButton(selected = isSelected, onClick = onClick, colors = RadioButtonDefaults.colors(selectedColor = AppColors.Primary))
        }
    }
}