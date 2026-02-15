package com.mobitechs.parcelwala.ui.screens.payments

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mobitechs.parcelwala.MainActivity
import com.mobitechs.parcelwala.data.model.request.CreateOrderResponse
import com.mobitechs.parcelwala.data.model.request.CreatePaymentOrderRequest
import com.mobitechs.parcelwala.data.model.request.TransactionResponse
import com.mobitechs.parcelwala.data.model.request.VerifyPaymentRequest
import com.mobitechs.parcelwala.data.model.request.VerifyPaymentResponse
import com.mobitechs.parcelwala.data.model.request.WalletTopupOrderRequest
import com.mobitechs.parcelwala.data.model.request.WalletTopupVerifyRequest
import com.mobitechs.parcelwala.ui.theme.AppColors
import com.mobitechs.parcelwala.ui.viewmodel.PaymentEvent
import com.mobitechs.parcelwala.ui.viewmodel.PaymentViewModel
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentsScreen(
    paymentViewModel: PaymentViewModel = hiltViewModel()
) {
    val uiState by paymentViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity

    var showTopupSheet by remember { mutableStateOf(false) }
    var topupAmount by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }

    // Handle payment events
    LaunchedEffect(Unit) {
        paymentViewModel.paymentEvent.collect { event ->
            when (event) {
                is PaymentEvent.OpenRazorpayCheckout -> {
                    activity?.let { act ->
                        paymentViewModel.openRazorpayCheckout(act, event.orderResponse)
                    }
                }
                is PaymentEvent.PaymentSuccess -> {
                    snackbarHostState.showSnackbar(
                        "Payment successful! Transaction: ${event.response.transactionNumber}"
                    )
                    paymentViewModel.loadTransactions()
                }
                is PaymentEvent.PaymentFailure -> {
                    snackbarHostState.showSnackbar("Payment failed: ${event.message}")
                }
                is PaymentEvent.WalletTopupSuccess -> {
                    showTopupSheet = false
                    topupAmount = ""
                    snackbarHostState.showSnackbar(
                        "₹${event.amount.toInt()} added! New balance: ₹${event.newBalance.toInt()}"
                    )
                }
            }
        }
    }

    // Load transactions on first launch
    LaunchedEffect(Unit) {
        paymentViewModel.loadTransactions()
        paymentViewModel.loadWalletBalance()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Payments",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = AppColors.Background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Wallet Card
            item {
                WalletCard(
                    balance = uiState.walletBalance,
                    onAddMoney = { showTopupSheet = true }
                )
            }

            // Payment Methods
            item {
                PaymentMethodsSection()
            }

            // Transaction History Header
            item {
                Text(
                    "Recent Transactions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary
                )
            }

            // Transactions
            if (uiState.isTransactionsLoading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = AppColors.Primary)
                    }
                }
            } else if (uiState.transactions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Receipt,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = AppColors.TextSecondary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "No transactions yet",
                                color = AppColors.TextSecondary
                            )
                        }
                    }
                }
            } else {
                items(uiState.transactions) { transaction ->
                    TransactionItem(transaction = transaction)
                }
            }
        }
    }

    // Wallet Topup Bottom Sheet
    if (showTopupSheet) {
        ModalBottomSheet(
            onDismissRequest = { showTopupSheet = false },
            containerColor = Color.White
        ) {
            WalletTopupSheet(
                amount = topupAmount,
                onAmountChange = { topupAmount = it },
                isLoading = uiState.isLoading,
                onTopup = {
                    val amount = topupAmount.toDoubleOrNull()
                    if (amount != null && amount >= 10) {
                        // ⚠️ TEST MODE - Direct checkout without backend
                        (context as? MainActivity)?.let { act ->
                            paymentViewModel.testWalletTopup(act, amount)
                        }

                        // ✅ PRODUCTION - Uncomment when backend is ready
                        // paymentViewModel.initiateWalletTopup(amount)
                    }
                },
                onDismiss = { showTopupSheet = false }
            )
        }
    }

    // Loading overlay
    if (uiState.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = AppColors.Primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Processing payment...", color = AppColors.TextPrimary)
                }
            }
        }
    }
}

// ============================================================
// WALLET CARD
// ============================================================

@Composable
private fun WalletCard(
    balance: Double,
    onAddMoney: () -> Unit
) {
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.Primary)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Wallet Balance",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        currencyFormat.format(balance),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Icon(
                    Icons.Default.AccountBalanceWallet,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = Color.White.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onAddMoney,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = AppColors.Primary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Money", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ============================================================
// PAYMENT METHODS SECTION
// ============================================================

@Composable
private fun PaymentMethodsSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Payment Methods",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary
            )
            Spacer(modifier = Modifier.height(12.dp))

            PaymentMethodRow(
                icon = Icons.Default.PhoneAndroid,
                title = "UPI",
                subtitle = "Pay via any UPI app",
                iconColor = Color(0xFF4CAF50)
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            PaymentMethodRow(
                icon = Icons.Default.CreditCard,
                title = "Cards",
                subtitle = "Credit & Debit cards",
                iconColor = Color(0xFF2196F3)
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            PaymentMethodRow(
                icon = Icons.Default.AccountBalance,
                title = "Net Banking",
                subtitle = "All major banks",
                iconColor = Color(0xFFFF9800)
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            PaymentMethodRow(
                icon = Icons.Default.MonetizationOn,
                title = "Cash",
                subtitle = "Pay on delivery",
                iconColor = Color(0xFF9C27B0)
            )
        }
    }
}

@Composable
private fun PaymentMethodRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    iconColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
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
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(22.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            Text(subtitle, fontSize = 12.sp, color = AppColors.TextSecondary)
        }
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = AppColors.TextSecondary,
            modifier = Modifier.size(20.dp)
        )
    }
}

// ============================================================
// TRANSACTION ITEM
// ============================================================

@Composable
private fun TransactionItem(transaction: TransactionResponse) {
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

    val (icon, iconColor, amountColor) = when (transaction.transactionType.lowercase()) {
        "booking" -> Triple(Icons.Default.LocalShipping, AppColors.Primary, AppColors.TextPrimary)
        "refund" -> Triple(Icons.Default.Replay, Color(0xFF4CAF50), Color(0xFF4CAF50))
        "wallet_topup" -> Triple(Icons.Default.Add, Color(0xFF4CAF50), Color(0xFF4CAF50))
        else -> Triple(Icons.Default.Receipt, AppColors.TextSecondary, AppColors.TextPrimary)
    }

    val statusColor = when (transaction.status.lowercase()) {
        "success" -> Color(0xFF4CAF50)
        "failed" -> Color(0xFFF44336)
        "pending" -> Color(0xFFFF9800)
        "refunded" -> Color(0xFF2196F3)
        else -> AppColors.TextSecondary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when (transaction.transactionType.lowercase()) {
                        "booking" -> "Booking #${transaction.bookingNumber ?: ""}"
                        "refund" -> "Refund"
                        "wallet_topup" -> "Wallet Topup"
                        else -> transaction.transactionType
                    },
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        transaction.paymentMethod?.uppercase() ?: "N/A",
                        fontSize = 11.sp,
                        color = AppColors.TextSecondary
                    )
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(AppColors.TextSecondary)
                    )
                    Text(
                        transaction.status.replaceFirstChar { it.uppercase() },
                        fontSize = 11.sp,
                        color = statusColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Text(
                text = "${if (transaction.transactionType.lowercase() == "refund") "+" else "-"} ${currencyFormat.format(transaction.amount)}",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = amountColor
            )
        }
    }
}

// ============================================================
// WALLET TOPUP BOTTOM SHEET
// ============================================================

@Composable
private fun WalletTopupSheet(
    amount: String,
    onAmountChange: (String) -> Unit,
    isLoading: Boolean,
    onTopup: () -> Unit,
    onDismiss: () -> Unit
) {
    val quickAmounts = listOf(100, 200, 500, 1000, 2000)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp)
    ) {
        Text(
            "Add Money to Wallet",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Amount Input
        OutlinedTextField(
            value = amount,
            onValueChange = { newValue ->
                if (newValue.all { it.isDigit() || it == '.' }) {
                    onAmountChange(newValue)
                }
            },
            label = { Text("Enter Amount") },
            prefix = { Text("₹ ", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Quick Amount Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            quickAmounts.forEach { quickAmount ->
                OutlinedButton(
                    onClick = { onAmountChange(quickAmount.toString()) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (amount == quickAmount.toString())
                            AppColors.Primary.copy(alpha = 0.1f) else Color.Transparent
                    ),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                ) {
                    Text(
                        "₹$quickAmount",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Topup Button
        val amountValue = amount.toDoubleOrNull() ?: 0.0
        Button(
            onClick = onTopup,
            enabled = amountValue >= 10 && !isLoading,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    "Add ₹${amount.ifEmpty { "0" }}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }

        if (amountValue in 0.01..9.99) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Minimum amount: ₹10",
                color = Color(0xFFF44336),
                fontSize = 12.sp
            )
        }
    }
}