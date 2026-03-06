package com.mobitechs.parcelwala.ui.screens.payments

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mobitechs.parcelwala.R
import com.mobitechs.parcelwala.ui.theme.AppColors
import java.text.NumberFormat
import java.util.Locale

data class PaymentResultData(
    val isSuccess: Boolean,
    val amount: Double,
    val transactionNumber: String? = null,
    val message: String? = null,
    val walletNewBalance: Double? = null,
    val isWalletTopup: Boolean = false
)

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

    var showContent by remember { mutableStateOf(false) }
    val iconScale by animateFloatAsState(
        targetValue = if (showContent) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "iconScale"
    )

    LaunchedEffect(Unit) { showContent = true }

    val backgroundColor = if (resultData.isSuccess) {
        Brush.verticalGradient(
            colors = listOf(
                AppColors.Pickup.copy(alpha = 0.08f),
                AppColors.White
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                AppColors.Error.copy(alpha = 0.08f),
                AppColors.White
            )
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.White),
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

            Box(
                modifier = Modifier
                    .size(96.dp)
                    .scale(iconScale)
                    .clip(CircleShape)
                    .background(
                        if (resultData.isSuccess) AppColors.Pickup.copy(alpha = 0.12f) else AppColors.Error.copy(
                            alpha = 0.12f
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (resultData.isSuccess) Icons.Default.CheckCircle else Icons.Default.Cancel,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = if (resultData.isSuccess) AppColors.Pickup else AppColors.Error
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(animationSpec = tween(400, delayMillis = 200)) +
                        slideInVertically(
                            animationSpec = tween(400, delayMillis = 200),
                            initialOffsetY = { 30 })
            ) {
                Text(
                    text = if (resultData.isSuccess) stringResource(R.string.label_payment_successful)
                    else stringResource(R.string.label_payment_failed),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (resultData.isSuccess) AppColors.Pickup else AppColors.Error,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(animationSpec = tween(400, delayMillis = 300)) +
                        slideInVertically(
                            animationSpec = tween(400, delayMillis = 300),
                            initialOffsetY = { 20 })
            ) {
                Text(
                    text = resultData.message ?: if (resultData.isSuccess) {
                        if (resultData.isWalletTopup) stringResource(R.string.label_wallet_topup_success_message)
                        else stringResource(R.string.label_payment_success_message)
                    } else {
                        stringResource(R.string.label_payment_failure_message)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextSecondary,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(animationSpec = tween(400, delayMillis = 400)) +
                        scaleIn(animationSpec = tween(400, delayMillis = 400), initialScale = 0.8f)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = AppColors.Background)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.label_amount),
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

                        if (resultData.isSuccess) {
                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = AppColors.TextSecondary.copy(alpha = 0.2f))
                            Spacer(modifier = Modifier.height(16.dp))

                            resultData.transactionNumber?.let { txnNo ->
                                DetailRow(
                                    label = stringResource(R.string.label_transaction_id),
                                    value = txnNo
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            DetailRow(
                                label = stringResource(R.string.label_type),
                                value = if (resultData.isWalletTopup) stringResource(R.string.label_wallet_topup_type)
                                else stringResource(R.string.label_booking_payment_type)
                            )

                            if (resultData.isWalletTopup && resultData.walletNewBalance != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                DetailRow(
                                    label = stringResource(R.string.label_wallet_balance_result),
                                    value = currencyFormat.format(resultData.walletNewBalance),
                                    valueColor = AppColors.Pickup
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            DetailRow(
                                label = stringResource(R.string.label_status),
                                value = stringResource(R.string.label_status_completed),
                                valueColor = AppColors.Pickup
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(animationSpec = tween(400, delayMillis = 500)) +
                        slideInVertically(
                            animationSpec = tween(400, delayMillis = 500),
                            initialOffsetY = { 30 })
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
                        text = if (resultData.isSuccess) stringResource(R.string.label_done)
                        else stringResource(R.string.label_try_again_btn),
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