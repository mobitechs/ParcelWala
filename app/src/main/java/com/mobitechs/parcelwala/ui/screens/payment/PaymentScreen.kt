package com.mobitechs.parcelwala.ui.screens.payments

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mobitechs.parcelwala.R
import com.mobitechs.parcelwala.data.model.request.TransactionResponse
import com.mobitechs.parcelwala.ui.components.AppTopBar
import com.mobitechs.parcelwala.ui.components.StatusBarScaffold
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
    var paymentResultData by remember { mutableStateOf<PaymentResultData?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    val paymentSuccessToast = stringResource(R.string.label_payment_success_toast)
    val walletTopupToastFormat = stringResource(R.string.label_wallet_topup_toast_format)

    LaunchedEffect(Unit) {
        paymentViewModel.paymentEvent.collect { event ->
            when (event) {
                is PaymentEvent.OpenRazorpayCheckout -> {
                    activity?.let { act ->
                        paymentViewModel.openRazorpayCheckout(act, event.orderResponse)
                    }
                }
                is PaymentEvent.PaymentSuccess -> {
                    paymentResultData = PaymentResultData(
                        isSuccess = true,
                        amount = event.response.amount,
                        transactionNumber = event.response.transactionNumber,
                        isWalletTopup = false
                    )
                    Toast.makeText(context, paymentSuccessToast, Toast.LENGTH_SHORT).show()
                    paymentViewModel.loadTransactions()
                }
                is PaymentEvent.PaymentFailure -> {
                    paymentResultData = PaymentResultData(
                        isSuccess = false,
                        amount = uiState.currentAmount,
                        message = event.message,
                        isWalletTopup = uiState.topupOrderResponse != null
                    )
                    Toast.makeText(
                        context,
                        context.getString(R.string.label_payment_failed_toast, event.message),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                is PaymentEvent.WalletTopupSuccess -> {
                    showTopupSheet = false
                    topupAmount = ""
                    paymentResultData = PaymentResultData(
                        isSuccess = true,
                        amount = event.amount,
                        walletNewBalance = event.newBalance,
                        isWalletTopup = true
                    )
                    Toast.makeText(
                        context,
                        String.format(walletTopupToastFormat, event.amount.toInt()),
                        Toast.LENGTH_SHORT
                    ).show()
                    paymentViewModel.loadTransactions()
                    paymentViewModel.loadWalletBalance()
                }
                is PaymentEvent.WalletPayBookingSuccess -> { /* not used here */ }
            }
        }
    }

    LaunchedEffect(Unit) {
        paymentViewModel.loadTransactions()
        paymentViewModel.loadWalletBalance()
    }

    StatusBarScaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.title_payments),
                extraContent = {
                    WalletExtraContent(
                        balance = uiState.walletBalance,
                        onAddMoney = { showTopupSheet = true },
                        onQuickAmount = { amount ->
                            topupAmount = amount.toString()
                            showTopupSheet = true
                        }
                    )
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = AppColors.Background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.label_recent_transactions),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary,
                    modifier = Modifier.padding(
                        start = 16.dp, end = 16.dp,
                        top = 20.dp, bottom = 10.dp
                    )
                )
            }

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
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(AppColors.Primary.copy(alpha = 0.08f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Receipt,
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp),
                                    tint = AppColors.Primary.copy(alpha = 0.5f)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                stringResource(R.string.label_no_transactions),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = AppColors.TextSecondary
                            )
                        }
                    }
                }
            } else {
                items(uiState.transactions) { transaction ->
                    TransactionItem(
                        transaction = transaction,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }

    if (showTopupSheet) {
        ModalBottomSheet(
            onDismissRequest = { showTopupSheet = false },
            containerColor = AppColors.Surface
        ) {
            WalletTopupSheet(
                amount = topupAmount,
                onAmountChange = { topupAmount = it },
                isLoading = uiState.isLoading,
                onTopup = {
                    val amount = topupAmount.toDoubleOrNull()
                    if (amount != null && amount >= 10) {
                        paymentViewModel.initiateWalletTopup(amount)
                    }
                },
                onDismiss = { showTopupSheet = false }
            )
        }
    }

    paymentResultData?.let { resultData ->
        PaymentResultDialog(
            resultData = resultData,
            onDismiss = { paymentResultData = null }
        )
    }

    if (uiState.isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.Black.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = AppColors.Surface)
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = AppColors.Primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(stringResource(R.string.label_processing_payment), color = AppColors.TextPrimary)
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// WalletExtraContent — pure content, no gradient/curve/statusbar padding
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun WalletExtraContent(
    balance: Double,
    onAddMoney: () -> Unit,
    onQuickAmount: (Int) -> Unit
) {
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    val quickAmounts = listOf(100, 200, 500, 1000)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(R.string.label_wallet_balance),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.80f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = currencyFormat.format(balance),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AccountBalanceWallet,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            quickAmounts.forEach { amount ->
                Surface(
                    onClick = { onQuickAmount(amount) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f))
                ) {
                    Text(
                        text = "+₹$amount",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        modifier = Modifier.padding(vertical = 7.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onAddMoney,
            modifier = Modifier.fillMaxWidth().height(46.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = AppColors.Primary
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(stringResource(R.string.label_add_money), fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Transaction Item
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun TransactionItem(
    transaction: TransactionResponse,
    modifier: Modifier = Modifier
) {
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    val bookingLabel = stringResource(R.string.label_booking_number_prefix, transaction.bookingNumber ?: "")
    val refundLabel = stringResource(R.string.label_refund)
    val walletTopupLabel = stringResource(R.string.label_wallet_topup)
    val isCredit = transaction.transactionType.lowercase() in listOf("refund", "wallet_topup")

    val (icon, iconBg, iconTint) = when (transaction.transactionType.lowercase()) {
        "booking"      -> Triple(Icons.Default.LocalShipping, AppColors.Primary.copy(alpha = 0.10f), AppColors.Primary)
        "refund"       -> Triple(Icons.Default.Replay,         Color(0xFFF0FAF3), Color(0xFF2D9B44))
        "wallet_topup" -> Triple(Icons.Default.Add,            Color(0xFFF0FAF3), Color(0xFF2D9B44))
        else           -> Triple(Icons.Default.Receipt,         AppColors.Background, AppColors.TextSecondary)
    }

    val amountColor = if (isCredit) Color(0xFF2D9B44) else AppColors.TextPrimary
    val statusColor = when (transaction.status.lowercase()) {
        "success"  -> Color(0xFF2D9B44)
        "failed"   -> AppColors.Drop
        "pending"  -> AppColors.Warning
        "refunded" -> AppColors.Blue
        else       -> AppColors.TextSecondary
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier.size(42.dp).clip(CircleShape).background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when (transaction.transactionType.lowercase()) {
                        "booking"      -> bookingLabel
                        "refund"       -> refundLabel
                        "wallet_topup" -> walletTopupLabel
                        else           -> transaction.transactionType
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    transaction.paymentMethod?.uppercase()?.takeIf { it.isNotBlank() }?.let {
                        Text(it, fontSize = 10.sp, color = AppColors.TextSecondary)
                        Box(modifier = Modifier.size(3.dp).clip(CircleShape).background(AppColors.TextHint))
                    }
                    Text(
                        text = transaction.status.replaceFirstChar { it.uppercase() },
                        fontSize = 10.sp,
                        color = statusColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Text(
                text = "${if (isCredit) "+" else "-"}${currencyFormat.format(transaction.amount)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = amountColor
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Wallet Top-up Bottom Sheet
// ══════════════════════════════════════════════════════════════════════════════

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
            text = stringResource(R.string.label_add_money_to_wallet),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = AppColors.TextPrimary
        )

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = amount,
            onValueChange = { newValue ->
                if (newValue.all { it.isDigit() || it == '.' }) onAmountChange(newValue)
            },
            label = { Text(stringResource(R.string.label_enter_amount)) },
            prefix = {
                Text(
                    stringResource(R.string.label_currency_prefix),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
        )

        Spacer(modifier = Modifier.height(14.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            quickAmounts.forEach { quickAmount ->
                val isSelected = amount == quickAmount.toString()
                Surface(
                    onClick = { onAmountChange(quickAmount.toString()) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) AppColors.Primary.copy(alpha = 0.10f) else Color.Transparent,
                    border = BorderStroke(1.dp, if (isSelected) AppColors.Primary else AppColors.Border)
                ) {
                    Text(
                        text = "₹$quickAmount",
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) AppColors.Primary else AppColors.TextSecondary,
                        modifier = Modifier.padding(vertical = 8.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        val amountValue = amount.toDoubleOrNull() ?: 0.0
        Button(
            onClick = onTopup,
            enabled = amountValue >= 10 && !isLoading,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
                Text(
                    text = stringResource(R.string.label_add_amount, amount.ifEmpty { "0" }),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }

        if (amountValue in 0.01..9.99) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(stringResource(R.string.label_minimum_amount), color = AppColors.Drop, fontSize = 12.sp)
        }
    }
}