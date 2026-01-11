// ui/components/OtpVerificationDialog.kt
package com.mobitechs.parcelwala.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.mobitechs.parcelwala.ui.theme.AppColors

/**
 * ════════════════════════════════════════════════════════════════════════════
 * OTP VERIFICATION DIALOG
 * ════════════════════════════════════════════════════════════════════════════
 * Shows OTP when driver arrives at pickup/delivery location
 * Used for verifying the correct person received/delivered the parcel
 * ════════════════════════════════════════════════════════════════════════════
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtpVerificationDialog(
    otp: String,
    title: String = "Pickup OTP",
    message: String = "Share this OTP with the driver to confirm pickup",
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Icon
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(AppColors.Primary.copy(alpha = 0.1f), RoundedCornerShape(32.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = AppColors.Primary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Title
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Message
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // OTP Display
                OtpDisplay(otp = otp)

                Spacer(modifier = Modifier.height(24.dp))

                // Close Button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Got It",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}

/**
 * OTP Display Component
 * Shows the 4-digit OTP in individual boxes
 */
@Composable
private fun OtpDisplay(otp: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
    ) {
        otp.forEach { digit ->
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(AppColors.Primary.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                    .border(2.dp, AppColors.Primary, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = digit.toString(),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.Primary
                )
            }
        }
    }
}

/**
 * ════════════════════════════════════════════════════════════════════════════
 * OTP INPUT DIALOG (For Driver App)
 * ════════════════════════════════════════════════════════════════════════════
 * Driver enters OTP to verify pickup/delivery
 * ════════════════════════════════════════════════════════════════════════════
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtpInputDialog(
    title: String = "Enter Pickup OTP",
    message: String = "Ask the customer for the OTP",
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    isLoading: Boolean = false
) {
    var otp by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Icon
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(AppColors.Primary.copy(alpha = 0.1f), RoundedCornerShape(32.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = AppColors.Primary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Title
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Message
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // OTP Input
                OtpInput(
                    otp = otp,
                    onOtpChange = {
                        otp = it
                        isError = false
                    },
                    isError = isError
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isLoading
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            if (otp.length == 4) {
                                onConfirm(otp)
                            } else {
                                isError = true
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isLoading && otp.length == 4
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "Verify",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * OTP Input Component
 * 4-digit OTP input with individual boxes
 */
@Composable
private fun OtpInput(
    otp: String,
    onOtpChange: (String) -> Unit,
    isError: Boolean = false
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Visual OTP boxes
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
        ) {
            repeat(4) { index ->
                val digit = otp.getOrNull(index)?.toString() ?: ""
                val isFilled = digit.isNotEmpty()

                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(
                            if (isFilled) AppColors.Primary.copy(alpha = 0.1f)
                            else Color.White,
                            RoundedCornerShape(12.dp)
                        )
                        .border(
                            width = 2.dp,
                            color = when {
                                isError -> AppColors.Drop
                                isFilled -> AppColors.Primary
                                else -> AppColors.Border
                            },
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = digit,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isFilled) AppColors.Primary else AppColors.TextHint
                    )
                }
            }
        }

        // Hidden text field for keyboard input
        OutlinedTextField(
            value = otp,
            onValueChange = {
                if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                    onOtpChange(it)
                }
            },
            modifier = Modifier
                .size(0.dp)
                .focusRequester(focusRequester),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.NumberPassword,
                imeAction = ImeAction.Done
            ),
            singleLine = true
        )

        if (isError) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Please enter a 4-digit OTP",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.Drop
            )
        }
    }
}