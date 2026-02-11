package com.mobitechs.parcelwala.ui.screens.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mobitechs.parcelwala.R
import com.mobitechs.parcelwala.ui.components.*
import com.mobitechs.parcelwala.ui.theme.AppColors
import com.mobitechs.parcelwala.ui.viewmodel.AuthViewModel

/**
 * Login Screen with Reusable Components
 */
@Composable
fun LoginScreen(
    onNavigateToOtp: (String, String?) -> Unit,  // ✅ CHANGED: Added otp parameter
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var phoneNumber by remember { mutableStateOf("") }
    var termsAccepted by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    // Navigate to OTP when sent - pass OTP from response
    LaunchedEffect(uiState.otpSent) {
        if (uiState.otpSent) {
            onNavigateToOtp(phoneNumber, uiState.otpData?.otp)
            viewModel.resetOtpState()
        }
    }

    // Show errors
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            errorMessage = error
            showError = true
            viewModel.clearError()
        }
    }

    // Error Dialog
    if (showError) {
        AlertDialog(
            onDismissRequest = { showError = false },
            icon = {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Warning,
                    contentDescription = null,
                    tint = AppColors.Drop
                )
            },
            title = { Text("Error", color = AppColors.TextPrimary) },
            text = { Text(errorMessage, color = AppColors.TextSecondary) },
            confirmButton = {
                TextButton(onClick = { showError = false }) {
                    Text("OK", color = AppColors.Primary)
                }
            },
            containerColor = AppColors.Surface
        )
    }

    Scaffold(
        containerColor = AppColors.Background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            // Logo
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.logo_pw),
                    contentDescription = "App Logo",
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Title
            Text(
                text = "Welcome to Parcel Wala",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Enter your phone number to continue",
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Phone Number Input
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Country Code
                Surface(
                    modifier = Modifier.width(80.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = AppColors.Surface,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        AppColors.Border
                    )
                ) {
                    Box(
                        modifier = Modifier.padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+91",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = AppColors.TextPrimary
                        )
                    }
                }

                // Phone Number Field
                PhoneNumberField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = "Phone Number",
                    modifier = Modifier.weight(1f),
                    isError = phoneNumber.isNotEmpty() && phoneNumber.length != 10,
                    errorMessage = "Enter valid 10-digit number"
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Terms & Conditions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = termsAccepted,
                    onCheckedChange = { termsAccepted = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = AppColors.Primary,
                        uncheckedColor = AppColors.Border
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "I agree to Terms & Conditions and Privacy Policy",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Send OTP Button
            PrimaryButton(
                text = "Send OTP",
                onClick = {
                    when {
                        phoneNumber.length != 10 -> {
                            errorMessage = "Please enter a valid 10-digit phone number"
                            showError = true
                        }
                        !termsAccepted -> {
                            errorMessage = "Please accept Terms & Conditions to continue"
                            showError = true
                        }
                        else -> viewModel.sendOtp(phoneNumber)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading,
                isLoading = uiState.isLoading
            )

            Spacer(modifier = Modifier.weight(1f))

            // Footer Info
            Text(
                text = "By continuing, you agree to our Terms of Service and Privacy Policy",
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.TextHint,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}