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
import com.mobitechs.parcelwala.ui.theme.AppColors
import com.mobitechs.parcelwala.ui.viewmodel.PaymentEvent
import com.mobitechs.parcelwala.ui.viewmodel.PaymentViewModel
import java.text.NumberFormat
import java.util.Locale

/**
 * Payment method types for post-delivery payment
 */
enum class PaymentMethodType(val key: String, val label: String) {
    CASH("cash", "Cash"),
    WALLET("wallet", "Wallet"),
    UPI("upi", "UPI"),
    CARD("card", "Card"),
    NET_BANKING("netbanking", "Net Banking")
}

/**
 * Post-delivery payment screen shown after delivery is completed.
 *
 * Flow:
 * - If CASH → skip payment, go directly to rating
 * - If WALLET → check balance, deduct from wallet via API
 * - If UPI/CARD/NETBANKING → open Razorpay checkout
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDeliveryPaymentScreen(
    bookingId: String,
    baseFare: Int,
    waitingCharge: Int,
    driverName: String,
    paymentMethod: String, // The payment method chosen at booking time (cash/wallet/upi/card/netbanking)
    onPaymentComplete: () -> Unit, // Called when payment is done → proceed to rating
    onPaymentSkipped: () -> Unit,  // Called for cash payments → proceed to rating
    paymentViewModel: PaymentViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val uiState by paymentViewModel.uiState.collectAsState()
    val totalFare = baseFare + waitingCharge
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

    var showPaymentError by remember { mutableStateOf<String?>(null) }
    var paymentProcessing by remember { mutableStateOf(false) }
    var selectedMethod by remember { mutableStateOf(paymentMethod.lowercase()) }

    val snackbarHostState = remember { SnackbarHostState() }

    // If payment method is cash, auto-skip after a brief display
    val isCash = selectedMethod == PaymentMethodType.CASH.key
    val isWallet = selectedMethod == PaymentMethodType.WALLET.key
    val isOnline = selectedMethod in listOf(
        PaymentMethodType.UPI.key,
        PaymentMethodType.CARD.key,
        PaymentMethodType.NET_BANKING.key
    )

    // Handle payment events from ViewModel
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
                is PaymentEvent.WalletTopupSuccess -> {
                    // Not expected here, but handle gracefully
                }
            }
        }
    }

    // Load wallet balance if wallet is selected
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
            // Scrollable content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // ══════════════════════════════════════════
                // DELIVERY SUCCESS HEADER
                // ══════════════════════════════════════════
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    AppColors.Pickup.copy(alpha = 0.2f),
                                    AppColors.Pickup.copy(alpha = 0.05f)
                                )
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = AppColors.Pickup,
                        modifier = Modifier.size(48.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Delivery Completed!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary
                )

                Text(
                    text = "Delivered by $driverName",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextSecondary
                )

                Spacer(modifier = Modifier.height(24.dp))

                // ══════════════════════════════════════════
                // FARE BREAKDOWN CARD
                // ══════════════════════════════════════════
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

                        // Trip Fare
                        FareRow(
                            label = "Trip Fare",
                            amount = "₹$baseFare",
                            icon = Icons.Default.LocalShipping,
                            iconTint = AppColors.Primary
                        )

                        // Waiting Charge (if any)
                        if (waitingCharge > 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            FareRow(
                                label = "Waiting Charge",
                                amount = "+ ₹$waitingCharge",
                                icon = Icons.Outlined.Timer,
                                iconTint = AppColors.Error,
                                amountColor = AppColors.Error
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = AppColors.Border)
                        Spacer(modifier = Modifier.height(12.dp))

                        // Total
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
                                text = "₹$totalFare",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = AppColors.Primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ══════════════════════════════════════════
                // PAYMENT METHOD SELECTION
                // ══════════════════════════════════════════
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

                        // Cash Option
                        PaymentOptionItem(
                            icon = Icons.Default.MonetizationOn,
                            title = "Cash",
                            subtitle = "Pay driver directly",
                            iconColor = AppColors.Pickup,
                            isSelected = selectedMethod == PaymentMethodType.CASH.key,
                            onClick = { selectedMethod = PaymentMethodType.CASH.key }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Wallet Option
                        PaymentOptionItem(
                            icon = Icons.Default.AccountBalanceWallet,
                            title = "Wallet",
                            subtitle = if (uiState.walletBalance > 0)
                                "Balance: ${currencyFormat.format(uiState.walletBalance)}"
                            else "Check balance",
                            iconColor = AppColors.Primary,
                            isSelected = selectedMethod == PaymentMethodType.WALLET.key,
                            onClick = { selectedMethod = PaymentMethodType.WALLET.key },
                            trailingContent = {
                                if (isWallet && uiState.walletBalance < totalFare) {
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

                        // UPI Option
                        PaymentOptionItem(
                            icon = Icons.Default.PhoneAndroid,
                            title = "UPI",
                            subtitle = "GPay, PhonePe, Paytm, etc.",
                            iconColor = Color(0xFF4CAF50),
                            isSelected = selectedMethod == PaymentMethodType.UPI.key,
                            onClick = { selectedMethod = PaymentMethodType.UPI.key }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Card Option
                        PaymentOptionItem(
                            icon = Icons.Default.CreditCard,
                            title = "Debit/Credit Card",
                            subtitle = "Visa, MasterCard, RuPay",
                            iconColor = AppColors.Blue,
                            isSelected = selectedMethod == PaymentMethodType.CARD.key,
                            onClick = { selectedMethod = PaymentMethodType.CARD.key }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Net Banking Option
                        PaymentOptionItem(
                            icon = Icons.Default.AccountBalance,
                            title = "Net Banking",
                            subtitle = "All major banks",
                            iconColor = AppColors.Warning,
                            isSelected = selectedMethod == PaymentMethodType.NET_BANKING.key,
                            onClick = { selectedMethod = PaymentMethodType.NET_BANKING.key }
                        )
                    }
                }

                // Wallet insufficient balance warning
                AnimatedVisibility(
                    visible = isWallet && uiState.walletBalance < totalFare
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = AppColors.Error.copy(alpha = 0.08f)
                        )
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
                                    text = "Insufficient wallet balance",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AppColors.Error
                                )
                                Text(
                                    text = "You need ₹${totalFare - uiState.walletBalance.toInt()} more. Please choose another payment method or top up your wallet.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AppColors.TextSecondary
                                )
                            }
                        }
                    }
                }

                // Payment error display
                AnimatedVisibility(visible = showPaymentError != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = AppColors.Error.copy(alpha = 0.08f)
                        )
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
                                    contentDescription = "Dismiss",
                                    tint = AppColors.Error,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // ══════════════════════════════════════════
            // BOTTOM PAY BUTTON
            // ══════════════════════════════════════════
            Surface(
                shadowElevation = 8.dp,
                color = Color.White
            ) {
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
                                    // Cash payment - skip online payment, go to rating
                                    paymentProcessing = false
                                    onPaymentSkipped()
                                }
                                isWallet -> {
                                    // Wallet payment - create order via backend
                                    val bookingIdInt = bookingId.toIntOrNull()
                                    if (bookingIdInt != null) {
                                        paymentViewModel.initiateBookingPayment(
                                            bookingId = bookingIdInt,
                                            amount = totalFare.toDouble(),
                                            paymentMethod = "wallet",
                                            notes = "Wallet payment for booking #$bookingId"
                                        )
                                    } else {
                                        paymentProcessing = false
                                        showPaymentError = "Invalid booking ID"
                                    }
                                }
                                isOnline -> {
                                    // Online payment (UPI/Card/NetBanking) - Razorpay
                                    val bookingIdInt = bookingId.toIntOrNull()
                                    if (bookingIdInt != null) {
                                        paymentViewModel.initiateBookingPayment(
                                            bookingId = bookingIdInt,
                                            amount = totalFare.toDouble(),
                                            paymentMethod = selectedMethod,
                                            notes = "Payment for booking #$bookingId"
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
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Processing...",
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
                                    isCash -> "Confirm Cash Payment  •  ₹$totalFare"
                                    isWallet -> "Pay from Wallet  •  ₹$totalFare"
                                    else -> "Pay ₹$totalFare"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════
// HELPER COMPOSABLES
// ══════════════════════════════════════════════════════════════

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
            Icon(
                icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
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
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    trailingContent: @Composable (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) AppColors.Primary.copy(alpha = 0.06f)
            else AppColors.Surface
        ),
        border = if (isSelected)
            androidx.compose.foundation.BorderStroke(2.dp, AppColors.Primary)
        else
            androidx.compose.foundation.BorderStroke(1.dp, AppColors.Border)
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
                    .background(iconColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
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