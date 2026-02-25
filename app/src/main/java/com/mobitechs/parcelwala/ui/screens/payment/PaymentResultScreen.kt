package com.mobitechs.parcelwala.ui.screens.payments

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mobitechs.parcelwala.ui.theme.AppColors
import java.text.NumberFormat
import java.util.Locale

/**
 * Payment result data to display on the result screen
 */
data class PaymentResultData(
    val isSuccess: Boolean,
    val amount: Double,
    val transactionNumber: String? = null,
    val message: String? = null,
    val walletNewBalance: Double? = null,  // For wallet topup
    val isWalletTopup: Boolean = false
)

/**
 * Full-screen payment result dialog with animations
 */
@Composable
fun PaymentResultDialog(
    resultData: PaymentResultData,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        PaymentResultContent(
            resultData = resultData,
            onDone = onDismiss
        )
    }
}

@Composable
private fun PaymentResultContent(
    resultData: PaymentResultData,
    onDone: () -> Unit
) {
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

    // Animation states
    var showContent by remember { mutableStateOf(false) }
    val iconScale by animateFloatAsState(
        targetValue = if (showContent) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "iconScale"
    )

    LaunchedEffect(Unit) {
        showContent = true
    }

    val backgroundColor = if (resultData.isSuccess) {
        Brush.verticalGradient(
            colors = listOf(
                AppColors.Pickup.copy(alpha = 0.08f),
                Color.White
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                AppColors.Error.copy(alpha = 0.08f),
                Color.White
            )
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Animated icon with circle background
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .scale(iconScale)
                    .clip(CircleShape)
                    .background(
                        if (resultData.isSuccess)
                            AppColors.Pickup.copy(alpha = 0.12f)
                        else
                            AppColors.Error.copy(alpha = 0.12f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (resultData.isSuccess)
                        Icons.Default.CheckCircle
                    else
                        Icons.Default.Cancel,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = if (resultData.isSuccess) AppColors.Pickup else AppColors.Error
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Title
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(animationSpec = tween(400, delayMillis = 200)) +
                        slideInVertically(
                            animationSpec = tween(400, delayMillis = 200),
                            initialOffsetY = { 30 }
                        )
            ) {
                Text(
                    text = if (resultData.isSuccess) "Payment Successful!" else "Payment Failed",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (resultData.isSuccess) AppColors.Pickup else AppColors.Error,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle message
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(animationSpec = tween(400, delayMillis = 300)) +
                        slideInVertically(
                            animationSpec = tween(400, delayMillis = 300),
                            initialOffsetY = { 20 }
                        )
            ) {
                Text(
                    text = resultData.message ?: if (resultData.isSuccess) {
                        if (resultData.isWalletTopup)
                            "Money added to your wallet successfully"
                        else
                            "Your payment has been processed successfully"
                    } else {
                        "Something went wrong. Please try again."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextSecondary,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Amount card
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(animationSpec = tween(400, delayMillis = 400)) +
                        scaleIn(
                            animationSpec = tween(400, delayMillis = 400),
                            initialScale = 0.8f
                        )
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = AppColors.Background
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Amount",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = currencyFormat.format(resultData.amount),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.TextPrimary
                        )

                        // Transaction details
                        if (resultData.isSuccess) {
                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = AppColors.TextSecondary.copy(alpha = 0.2f))
                            Spacer(modifier = Modifier.height(16.dp))

                            // Transaction Number
                            resultData.transactionNumber?.let { txnNo ->
                                DetailRow(
                                    label = "Transaction ID",
                                    value = txnNo
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            // Payment type
                            DetailRow(
                                label = "Type",
                                value = if (resultData.isWalletTopup) "Wallet Top-up" else "Booking Payment"
                            )

                            // New wallet balance
                            if (resultData.isWalletTopup && resultData.walletNewBalance != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                DetailRow(
                                    label = "Wallet Balance",
                                    value = currencyFormat.format(resultData.walletNewBalance),
                                    valueColor = AppColors.Pickup
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            DetailRow(
                                label = "Status",
                                value = "Completed",
                                valueColor = AppColors.Pickup
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Done button
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(animationSpec = tween(400, delayMillis = 500)) +
                        slideInVertically(
                            animationSpec = tween(400, delayMillis = 500),
                            initialOffsetY = { 30 }
                        )
            ) {
                Button(
                    onClick = onDone,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (resultData.isSuccess) AppColors.Primary else AppColors.Error
                    )
                ) {
                    Text(
                        text = if (resultData.isSuccess) "Done" else "Try Again",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    valueColor: Color = AppColors.TextPrimary
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.TextSecondary,
            fontSize = 13.sp
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = valueColor,
            fontSize = 13.sp
        )
    }
}