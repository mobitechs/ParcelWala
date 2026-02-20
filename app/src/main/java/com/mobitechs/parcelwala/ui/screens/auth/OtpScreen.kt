package com.mobitechs.parcelwala.ui.screens.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mobitechs.parcelwala.R
import com.mobitechs.parcelwala.ui.components.*
import com.mobitechs.parcelwala.ui.theme.AppColors
import com.mobitechs.parcelwala.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.delay

/**
 * OTP Verification Screen with Reusable Components
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtpScreen(
    phoneNumber: String,
    receivedOtp: String? = null,
    onNavigateToHome: () -> Unit,
    onNavigateToCompleteProfile: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var otp by remember { mutableStateOf(List(4) { "" }) }
    var resendTimer by remember { mutableStateOf(60) }
    var canResend by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val focusRequesters = remember { List(4) { FocusRequester() } }

    // Hoist error string for use in non-composable lambda
    val errorCompleteOtp = stringResource(R.string.error_complete_4_digit_otp)

    // Auto-verify when complete
    LaunchedEffect(otp) {
        val otpString = otp.joinToString("")
        if (otpString.length == 4) {
            viewModel.verifyOtp(otpString, phoneNumber)
        }
    }

    // Set phone number when screen loads
    LaunchedEffect(phoneNumber) {
        viewModel.setPhoneNumber(phoneNumber)
    }

    // Auto-focus first box
    LaunchedEffect(Unit) {
        delay(300)
        focusRequesters[0].requestFocus()
    }

    // Handle success
    LaunchedEffect(uiState.loginSuccess) {
        if (uiState.loginSuccess) {
            if (uiState.isNewUser) {
                onNavigateToCompleteProfile()
            } else {
                onNavigateToHome()
            }
        }
    }

    // Resend timer
    LaunchedEffect(Unit) {
        while (resendTimer > 0) {
            delay(1000)
            resendTimer--
        }
        canResend = true
    }

    // Show errors
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
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = AppColors.Drop
                )
            },
            title = { Text(stringResource(R.string.label_error_title), color = AppColors.TextPrimary) },
            text = { Text(errorMessage, color = AppColors.TextSecondary) },
            confirmButton = {
                TextButton(onClick = { showError = false }) {
                    Text(stringResource(R.string.label_ok), color = AppColors.Primary)
                }
            },
            containerColor = AppColors.Surface
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.title_verify_otp),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.content_desc_back),
                            tint = AppColors.TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppColors.Surface
                )
            )
        },
        containerColor = AppColors.Background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // Icon
            IconButtonWithBackground(
                icon = androidx.compose.material.icons.Icons.Default.Message,
                contentDescription = stringResource(R.string.content_desc_otp),
                onClick = { },
                size = 80.dp,
                backgroundColor = AppColors.Primary.copy(alpha = 0.1f),
                iconTint = AppColors.Primary
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Title
            Text(
                text = stringResource(R.string.title_verify_otp),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle
            Text(
                text = stringResource(R.string.label_enter_4_digit_code),
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextSecondary
            )

            Text(
                text = stringResource(R.string.label_phone_with_code, phoneNumber),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.Primary
            )

            // Display OTP for testing
            if (!receivedOtp.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = AppColors.Primary.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, AppColors.Primary.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.label_test_otp),
                            style = MaterialTheme.typography.labelMedium,
                            color = AppColors.Primary.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = receivedOtp,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.Primary,
                            letterSpacing = 8.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // OTP Input
            OtpInputField(
                otp = otp,
                focusRequesters = focusRequesters,
                onOtpChange = { index, value ->
                    val newOtp = otp.toMutableList()
                    newOtp[index] = value
                    otp = newOtp

                    if (value.isNotEmpty() && index < 3) {
                        focusRequesters[index + 1].requestFocus()
                    }
                },
                onBackspace = { index ->
                    if (index > 0 && otp[index].isEmpty()) {
                        focusRequesters[index - 1].requestFocus()
                    }
                }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Verify Button
            PrimaryButton(
                text = stringResource(R.string.title_verify_otp),
                onClick = {
                    val otpString = otp.joinToString("")
                    if (otpString.length == 4) {
                        viewModel.verifyOtp(otpString, phoneNumber)
                    } else {
                        errorMessage = errorCompleteOtp
                        showError = true
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading && otp.joinToString("").length == 4,
                isLoading = uiState.isLoading,
                icon = Icons.Default.Check
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Resend OTP
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.label_didnt_receive_code),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextSecondary
                )
                TextButton(
                    onClick = {
                        viewModel.resetOtpState()
                        viewModel.sendOtp(phoneNumber)
                        resendTimer = 60
                        canResend = false
                        otp = List(4) { "" }
                        focusRequesters[0].requestFocus()
                    },
                    enabled = canResend
                ) {
                    Text(
                        text = if (canResend) stringResource(R.string.label_resend_otp) else stringResource(R.string.label_resend_in, resendTimer),
                        color = if (canResend) AppColors.Primary else AppColors.TextHint,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

/**
 * OTP Input Fields - 4 digit
 */
@Composable
private fun OtpInputField(
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
                    .width(60.dp)
                    .height(64.dp)
                    .focusRequester(focusRequesters[index])
                    .onKeyEvent { keyEvent ->
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
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                ),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppColors.Primary,
                    unfocusedBorderColor = AppColors.Border,
                    cursorColor = AppColors.Primary
                ),
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}