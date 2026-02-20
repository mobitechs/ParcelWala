package com.mobitechs.parcelwala.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mobitechs.parcelwala.R
import com.mobitechs.parcelwala.ui.components.*
import com.mobitechs.parcelwala.ui.theme.AppColors
import com.mobitechs.parcelwala.ui.viewmodel.AuthViewModel

/**
 * Complete Profile Screen with Reusable Components
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompleteProfileScreen(
    onNavigateToHome: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var referralCode by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val focusManager = LocalFocusManager.current

    // Hoist error strings for use in non-composable lambdas
    val errorEnterName = stringResource(R.string.error_enter_name)
    val errorNameMinChars = stringResource(R.string.error_name_min_chars)
    val errorValidEmail = stringResource(R.string.error_valid_email)

    // Handle success
    LaunchedEffect(uiState.profileCompleted) {
        if (uiState.profileCompleted) {
            onNavigateToHome()
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
                        text = stringResource(R.string.title_complete_profile),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
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
                icon = Icons.Default.Person,
                contentDescription = stringResource(R.string.content_desc_profile),
                onClick = { },
                size = 80.dp,
                backgroundColor = AppColors.Primary.copy(alpha = 0.1f),
                iconTint = AppColors.Primary
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Title
            Text(
                text = stringResource(R.string.label_lets_get_started),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.label_complete_profile_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextSecondary
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Full Name
            NameInputField(
                value = fullName,
                onValueChange = { fullName = it },
                label = stringResource(R.string.label_full_name),
                modifier = Modifier.fillMaxWidth(),
                isError = fullName.isNotEmpty() && fullName.length < 3,
                errorMessage = stringResource(R.string.error_name_min_length),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Email (Optional)
            AppTextField(
                value = email,
                onValueChange = { email = it },
                label = stringResource(R.string.label_email_optional),
                placeholder = stringResource(R.string.hint_enter_email),
                leadingIcon = Icons.Default.Email,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                isError = email.isNotEmpty() && !isValidEmail(email),
                errorMessage = stringResource(R.string.error_invalid_email)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Referral Code (Optional)
            AppTextField(
                value = referralCode,
                onValueChange = {
                    if (it.length <= 8) {
                        referralCode = it.uppercase()
                    }
                },
                label = stringResource(R.string.label_referral_code_optional),
                placeholder = stringResource(R.string.hint_enter_referral_code),
                leadingIcon = Icons.Default.CardGiftcard,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { focusManager.clearFocus() }
                ),
                maxLength = 8
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Continue Button
            PrimaryButton(
                text = stringResource(R.string.label_continue),
                onClick = {
                    when {
                        fullName.isBlank() -> {
                            errorMessage = errorEnterName
                            showError = true
                        }
                        fullName.length < 3 -> {
                            errorMessage = errorNameMinChars
                            showError = true
                        }
                        email.isNotBlank() && !isValidEmail(email) -> {
                            errorMessage = errorValidEmail
                            showError = true
                        }
                        else -> {
                            viewModel.completeProfile(
                                fullName = fullName,
                                email = email.takeIf { it.isNotBlank() },
                                referralCode = referralCode.takeIf { it.isNotBlank() }
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading,
                isLoading = uiState.isLoading,
                icon = Icons.Default.Check
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Skip Button
            TextButton(
                onClick = { onNavigateToHome() }
            ) {
                Text(
                    text = stringResource(R.string.label_skip_for_now),
                    color = AppColors.TextSecondary,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Info Text
            Text(
                text = stringResource(R.string.label_update_profile_hint),
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.TextHint
            )
        }
    }
}

/**
 * Email Validation Helper
 */
private fun isValidEmail(email: String): Boolean {
    val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
    return email.matches(emailRegex)
}