package com.mobitechs.parcelwala.ui.screens.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mobitechs.parcelwala.R
import com.mobitechs.parcelwala.data.model.request.LoginIdentifier
import com.mobitechs.parcelwala.data.model.request.LoginMethod
import com.mobitechs.parcelwala.ui.components.AppTextField
import com.mobitechs.parcelwala.ui.components.PhoneNumberField
import com.mobitechs.parcelwala.ui.components.PrimaryButton
import com.mobitechs.parcelwala.ui.theme.AppColors
import com.mobitechs.parcelwala.ui.viewmodel.AuthViewModel
import com.mobitechs.parcelwala.utils.Validators

/**
 * Login — design option A: a **Mobile number | Email** toggle.
 *
 * Mobile stays the default and works exactly as before (+91, 10 digits, SMS
 * code). Email swaps in one email field and sends the 4-digit code by email.
 * Everything after this screen — the code screen, Complete profile, Home — is
 * the same flow for both; the [LoginIdentifier] handed to [onNavigateToOtp]
 * says which one the code went to.
 *
 * Backend contract: backend-docs/EMAIL_LOGIN_API.md.
 */
@Composable
fun LoginScreen(
    onNavigateToOtp: (LoginIdentifier, String?) -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current

    // Saveable so rotating the phone or a trip to another app keeps the tab and
    // what was typed.
    var method by rememberSaveable { mutableStateOf(LoginMethod.PHONE) }
    var phoneNumber by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var emailTouched by rememberSaveable { mutableStateOf(false) }
    var termsAccepted by rememberSaveable { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    // Hoist error strings for use in non-composable lambdas
    val errorValidPhone = stringResource(R.string.error_valid_phone_10_digit)
    val errorValidEmail = stringResource(R.string.error_valid_email)
    val errorAcceptTerms = stringResource(R.string.error_accept_terms)

    val emailInvalid = Validators.emailRequired(email) != null

    // Navigate to the code screen once the code is sent — with WHICH identifier
    // it went to, and the test OTP when the backend returns one.
    LaunchedEffect(uiState.otpSent) {
        if (uiState.otpSent) {
            val identifier = when (method) {
                LoginMethod.PHONE -> LoginIdentifier.phone(phoneNumber)
                LoginMethod.EMAIL -> LoginIdentifier.email(email)
            }
            onNavigateToOtp(identifier, uiState.otpData?.otp)
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
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = AppColors.Drop
                )
            },
            title = {
                Text(
                    stringResource(R.string.label_error),
                    color = AppColors.Heading
                )
            },
            text = { Text(errorMessage, color = AppColors.TextSecondary) },
            confirmButton = {
                TextButton(onClick = { showError = false }) {
                    Text(stringResource(R.string.ok), color = AppColors.Primary)
                }
            },
            containerColor = AppColors.Surface
        )
    }

    val submit: () -> Unit = {
        focusManager.clearFocus()
        when {
            method == LoginMethod.PHONE && phoneNumber.length != 10 -> {
                errorMessage = errorValidPhone
                showError = true
            }

            method == LoginMethod.EMAIL && emailInvalid -> {
                emailTouched = true
                errorMessage = errorValidEmail
                showError = true
            }

            !termsAccepted -> {
                errorMessage = errorAcceptTerms
                showError = true
            }

            method == LoginMethod.PHONE -> viewModel.sendOtp(LoginIdentifier.phone(phoneNumber))
            else -> viewModel.sendOtp(LoginIdentifier.email(email))
        }
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
            Spacer(modifier = Modifier.height(36.dp))

            // Logo
            Box(
                modifier = Modifier
                    .size(108.dp)
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.logo_pw),
                    contentDescription = stringResource(R.string.content_desc_app_logo),
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Title
            Text(
                text = stringResource(R.string.label_welcome),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = AppColors.Heading,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.login_subtitle_both),
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            // ── Mobile number | Email ─────────────────────────────────────────
            LoginMethodToggle(
                selected = method,
                onSelect = {
                    if (it != method) {
                        method = it
                        focusManager.clearFocus()
                    }
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            when (method) {
                LoginMethod.PHONE -> {
                    // Phone Number Input — unchanged from before the toggle.
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
                                    text = stringResource(R.string.label_country_code),
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
                            label = stringResource(R.string.label_phone_number),
                            modifier = Modifier.weight(1f),
                            isError = phoneNumber.isNotEmpty() && phoneNumber.length != 10,
                            errorMessage = stringResource(R.string.error_invalid_phone)
                        )
                    }
                    HelperLine(stringResource(R.string.helper_sms_code))
                }

                LoginMethod.EMAIL -> {
                    AppTextField(
                        value = email,
                        // No spaces in an address; trimming on the fly also stops
                        // a keyboard's trailing space from failing validation.
                        onValueChange = { email = it.filterNot(Char::isWhitespace) },
                        label = stringResource(R.string.label_email_address),
                        placeholder = stringResource(R.string.hint_email_example),
                        leadingIcon = Icons.Default.Email,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = {
                            emailTouched = true
                            focusManager.clearFocus()
                        }),
                        isError = emailTouched && email.isNotEmpty() && emailInvalid,
                        errorMessage = errorValidEmail,
                        maxLength = 254
                    )
                    HelperLine(stringResource(R.string.helper_email_code))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

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
                    text = stringResource(R.string.label_terms_agreement),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Send code button — the label says where the code will go.
            PrimaryButton(
                text = stringResource(
                    if (method == LoginMethod.EMAIL) R.string.label_send_code_email
                    else R.string.label_send_otp
                ),
                onClick = submit,
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading,
                isLoading = uiState.isLoading
            )

            Spacer(modifier = Modifier.weight(1f))

            // Footer Info
            Text(
                text = stringResource(R.string.label_footer_terms),
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.TextHint,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

/**
 * Two-way segmented control. Real selectable semantics (role = Tab inside a
 * selectable group), so TalkBack reads "Email, tab, 2 of 2, selected".
 */
@Composable
private fun LoginMethodToggle(
    selected: LoginMethod,
    onSelect: (LoginMethod) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(AppColors.PrimaryLight)
            .padding(4.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ToggleOption(
            icon = Icons.Default.PhoneAndroid,
            label = stringResource(R.string.login_tab_mobile),
            selected = selected == LoginMethod.PHONE,
            onClick = { onSelect(LoginMethod.PHONE) },
            modifier = Modifier.weight(1f)
        )
        ToggleOption(
            icon = Icons.Default.Email,
            label = stringResource(R.string.login_tab_email),
            selected = selected == LoginMethod.EMAIL,
            onClick = { onSelect(LoginMethod.EMAIL) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ToggleOption(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier = modifier
            .height(44.dp)
            .then(if (selected) Modifier.shadow(1.dp, shape) else Modifier)
            .clip(shape)
            .background(if (selected) AppColors.White else Color.Transparent)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) AppColors.Primary else AppColors.Gray600,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) AppColors.Primary else AppColors.Gray600
        )
    }
}

@Composable
private fun HelperLine(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = AppColors.TextSecondary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp, start = 4.dp)
    )
}
