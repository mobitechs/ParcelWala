package com.mobitechs.parcelwala.ui.screens.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mobitechs.parcelwala.R
import com.mobitechs.parcelwala.ui.components.AppTextField
import com.mobitechs.parcelwala.ui.components.IconButtonWithBackground
import com.mobitechs.parcelwala.ui.components.NameInputField
import com.mobitechs.parcelwala.ui.components.PrimaryButton
import com.mobitechs.parcelwala.ui.theme.AppColors
import com.mobitechs.parcelwala.ui.viewmodel.AuthViewModel
import com.mobitechs.parcelwala.utils.Validators
import com.mobitechs.parcelwala.utils.rememberField
import com.mobitechs.parcelwala.utils.rememberForm

/**
 * Complete Profile.
 *
 * WHAT CHANGED
 *
 * Before: the Continue button was always enabled, validation ran on click, and
 * failures came back as a modal AlertDialog. A 400 from the server showed the
 * string "Network error" because the repository threw the response body away.
 *
 * Now:
 *  - the button is disabled until every field is actually valid
 *  - errors sit under the field they belong to, not in a popup
 *  - nothing turns red until the user has touched that field or tried to submit
 *  - server-side validation errors are mapped back onto the matching input, so
 *    "Full name must be at least 3 characters" appears under Full name
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompleteProfileScreen(
    onNavigateToHome: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    // ── the form ─────────────────────────────────────────────────────────────
    // Each field owns its value, whether it's been touched, and any server error.
    val name = rememberField(validate = Validators::fullName)
    val email = rememberField(required = false, validate = Validators::emailOptional)
    val referral = rememberField(required = false, validate = Validators::referralCode)
    val form = rememberForm(name, email, referral)

    // ── success ──────────────────────────────────────────────────────────────
    LaunchedEffect(uiState.profileCompleted) {
        if (uiState.profileCompleted) onNavigateToHome()
    }

    // ── failure ──────────────────────────────────────────────────────────────
    // Field-level errors go onto the inputs. Anything the server complained about
    // that doesn't map to a field falls back to a snackbar, so nothing is lost.
    LaunchedEffect(uiState.error, uiState.fieldErrors) {
        val serverFieldErrors = uiState.fieldErrors
        when {
            serverFieldErrors.isNotEmpty() -> {
                val unmatched = form.applyServerErrors(
                    errors = serverFieldErrors,
                    bindings = mapOf(
                        "full_name" to name,
                        "email" to email,
                        "referral_code" to referral
                    )
                )
                unmatched.firstOrNull()?.let { snackbarHostState.showSnackbar(it) }
                viewModel.clearError()
            }

            uiState.error != null -> {
                snackbarHostState.showSnackbar(uiState.error!!)
                viewModel.clearError()
            }
        }
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
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

            IconButtonWithBackground(
                icon = Icons.Default.Person,
                contentDescription = stringResource(R.string.content_desc_profile),
                onClick = { },
                size = 80.dp,
                backgroundColor = AppColors.Primary.copy(alpha = 0.1f),
                iconTint = AppColors.Primary
            )

            Spacer(modifier = Modifier.height(24.dp))

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

            // ── Full name (required) ─────────────────────────────────────────
            NameInputField(
                value = name.value,
                // Filter at the source: digits and symbols can't be typed at all,
                // rather than being typed and then rejected.
                onValueChange = { name.onChange(Validators.nameInput(it)) },
                label = stringResource(R.string.label_full_name),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { if (!it.isFocused) name.markTouched() },
                isError = name.errorToShow != null,
                errorMessage = name.errorToShow.orEmpty(),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Email (optional, but must be valid if filled) ────────────────
            AppTextField(
                value = email.value,
                onValueChange = email::onChange,
                label = stringResource(R.string.label_email_optional),
                placeholder = stringResource(R.string.hint_enter_email),
                leadingIcon = Icons.Default.Email,
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { if (!it.isFocused) email.markTouched() },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                isError = email.errorToShow != null,
                errorMessage = email.errorToShow.orEmpty()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Referral code (optional) ─────────────────────────────────────
            AppTextField(
                value = referral.value,
                onValueChange = { referral.onChange(Validators.upperAlphaNumeric(it, 8)) },
                label = stringResource(R.string.label_referral_code_optional),
                placeholder = stringResource(R.string.hint_enter_referral_code),
                leadingIcon = Icons.Default.CardGiftcard,
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { if (!it.isFocused) referral.markTouched() },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                isError = referral.errorToShow != null,
                errorMessage = referral.errorToShow.orEmpty(),
                maxLength = 8
            )

            Spacer(modifier = Modifier.height(32.dp))

            PrimaryButton(
                text = stringResource(R.string.label_continue),
                onClick = {
                    focusManager.clearFocus()
                    // touchAll() reveals every error at once for anyone who reaches
                    // the button without visiting each field.
                    form.touchAll()
                    if (!form.isValid) return@PrimaryButton

                    viewModel.completeProfile(
                        fullName = name.trimmed,
                        email = email.orNull(),
                        referralCode = referral.orNull()
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                // The whole point: off until the form can actually be submitted.
                enabled = form.isValid && !uiState.isLoading,
                isLoading = uiState.isLoading,
                icon = Icons.Default.Check
            )

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = onNavigateToHome) {
                Text(
                    text = stringResource(R.string.label_skip_for_now),
                    color = AppColors.TextSecondary,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = stringResource(R.string.label_update_profile_hint),
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.TextHint
            )
        }
    }
}
