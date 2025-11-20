package com.mobitechs.parcelwala.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mobitechs.parcelwala.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtpScreen(
    phoneNumber: String,
    onNavigateToHome: () -> Unit,
    onNavigateToCompleteProfile: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var otp by remember { mutableStateOf(List(6) { "" }) }
    var resendTimer by remember { mutableStateOf(60) }
    var canResend by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    // Focus requesters for each OTP box
    val focusRequesters = remember { List(6) { FocusRequester() } }

    // Auto-focus first box on screen load
    LaunchedEffect(Unit) {
        delay(300)
        focusRequesters[0].requestFocus()
    }

    // Handle login success
    LaunchedEffect(uiState.loginSuccess) {
        if (uiState.loginSuccess) {
            if (uiState.isNewUser) {
                onNavigateToCompleteProfile()
            } else {
                onNavigateToHome()
            }
        }
    }

    // Auto-verify when 6 digits entered
    LaunchedEffect(otp) {
        val otpString = otp.joinToString("")
        if (otpString.length == 6) {
            viewModel.verifyOtp(otpString)
        }
    }

    // Resend timer countdown
    LaunchedEffect(Unit) {
        while (resendTimer > 0) {
            delay(1000)
            resendTimer--
        }
        canResend = true
    }

    // Show error
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            errorMessage = error
            showError = true
            viewModel.clearError()
        }
    }

    if (showError) {
        AlertDialog(
            onDismissRequest = { showError = false },
            title = { Text("Error") },
            text = { Text(errorMessage) },
            confirmButton = {
                TextButton(onClick = { showError = false }) {
                    Text("OK")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Verify OTP") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = "Verify OTP",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Enter the 6-digit code sent to",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "+91 $phoneNumber",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(40.dp))

            // OTP Input Boxes with auto-focus
            OtpInputField(
                otp = otp,
                focusRequesters = focusRequesters,
                onOtpChange = { index, value ->
                    val newOtp = otp.toMutableList()
                    newOtp[index] = value
                    otp = newOtp

                    // Auto-move to next box
                    if (value.isNotEmpty() && index < 5) {
                        focusRequesters[index + 1].requestFocus()
                    }
                },
                onBackspace = { index ->
                    // Move to previous box on backspace
                    if (index > 0 && otp[index].isEmpty()) {
                        focusRequesters[index - 1].requestFocus()
                    }
                }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Verify Button
            Button(
                onClick = {
                    val otpString = otp.joinToString("")
                    if (otpString.length == 6) {
                        viewModel.verifyOtp(otpString)
                    } else {
                        errorMessage = "Please enter complete OTP"
                        showError = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !uiState.isLoading && otp.joinToString("").length == 6
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(
                        text = "Verify OTP",
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Resend OTP
            TextButton(
                onClick = {
                    viewModel.resetOtpState()
                    viewModel.sendOtp(phoneNumber)
                    resendTimer = 60
                    canResend = false
                    otp = List(6) { "" }
                    focusRequesters[0].requestFocus()
                },
                enabled = canResend
            ) {
                Text(
                    if (canResend) "Resend OTP"
                    else "Resend OTP in ${resendTimer}s"
                )
            }
        }
    }
}

@Composable
fun OtpInputField(
    otp: List<String>,
    focusRequesters: List<FocusRequester>,
    onOtpChange: (Int, String) -> Unit,
    onBackspace: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        otp.forEachIndexed { index, digit ->
            OutlinedTextField(
                value = digit,
                onValueChange = { value ->
                    if (value.length <= 1 && value.all { it.isDigit() }) {
                        onOtpChange(index, value)
                    }
                },
                modifier = Modifier
                    .width(50.dp)
                    .height(60.dp)
                    .focusRequester(focusRequesters[index])
                    .onKeyEvent { keyEvent ->
                        // Handle backspace
                        if (keyEvent.key == Key.Backspace) {
                            if (digit.isEmpty()) {
                                onBackspace(index)
                            }
                            true
                        } else {
                            false
                        }
                    },
                textStyle = MaterialTheme.typography.headlineMedium.copy(
                    textAlign = TextAlign.Center
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                ),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )
        }
    }
}