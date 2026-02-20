// ui/screens/account/AccountBottomSheets.kt
package com.mobitechs.parcelwala.ui.screens.account

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mobitechs.parcelwala.R
import com.mobitechs.parcelwala.data.model.response.User
import com.mobitechs.parcelwala.ui.components.PrimaryButton
import com.mobitechs.parcelwala.ui.theme.AppColors

/**
 * Add GST Bottom Sheet
 * Simple bottom sheet for adding GSTIN number
 *
 * Features:
 * - GST document illustration
 * - Title and description
 * - GSTIN input field with validation
 * - Confirm button
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGSTBottomSheet(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var gstin by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    // GSTIN validation regex (15 characters)
    val gstinRegex = Regex("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$")
    val isValidGstin = gstinRegex.matches(gstin)

    // ✅ Use rememberModalBottomSheetState with skipPartiallyExpanded = true
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            // Close button on right side
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.close),
                        tint = AppColors.TextSecondary
                    )
                }
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // GST Document Illustration
            Surface(
                modifier = Modifier.size(80.dp),
                shape = RoundedCornerShape(16.dp),
                color = AppColors.Primary.copy(alpha = 0.1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    tint = AppColors.Primary,
                    modifier = Modifier
                        .padding(20.dp)
                        .size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Title
            Text(
                text = stringResource(R.string.add_gstin_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Description
            Text(
                text = stringResource(R.string.add_gstin_description),
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // GSTIN Input Field
            OutlinedTextField(
                value = gstin,
                onValueChange = {
                    if (it.length <= 15) {
                        gstin = it.uppercase()
                        showError = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.gstin_label)) },
                placeholder = { Text(stringResource(R.string.gstin_placeholder)) },
                singleLine = true,
                isError = showError && gstin.isNotEmpty() && !isValidGstin,
                supportingText = {
                    if (showError && gstin.isNotEmpty() && !isValidGstin) {
                        Text(
                            text = stringResource(R.string.gstin_error),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    imeAction = ImeAction.Done
                ),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppColors.Primary,
                    focusedLabelColor = AppColors.Primary,
                    cursorColor = AppColors.Primary
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Confirm Button
            PrimaryButton(
                text = stringResource(R.string.confirm),
                onClick = {
                    if (isValidGstin) {
                        onSave(gstin)
                    } else {
                        showError = true
                    }
                },
                enabled = gstin.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Edit Profile Bottom Sheet
 * Bottom sheet for editing user profile details
 *
 * Features:
 * - First Name and Last Name fields
 * - Mobile Number (disabled - cannot be changed)
 * - Email Address field
 * - Confirm button
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileBottomSheet(
    currentUser: User?,
    onDismiss: () -> Unit,
    onSave: (firstName: String, lastName: String, email: String) -> Unit
) {
    // Parse full name into first and last name
    val nameParts = currentUser?.fullName?.split(" ", limit = 2) ?: listOf("", "")
    var firstName by remember { mutableStateOf(nameParts.getOrElse(0) { "" }) }
    var lastName by remember { mutableStateOf(nameParts.getOrElse(1) { "" }) }
    var email by remember { mutableStateOf(currentUser?.email ?: "") }
    var showError by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current

    // ✅ Use rememberModalBottomSheetState with skipPartiallyExpanded = true
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            // Close button on right side
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.close),
                        tint = AppColors.TextSecondary
                    )
                }
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            // Title
            Text(
                text = stringResource(R.string.edit_profile_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary
            )

            Spacer(modifier = Modifier.height(24.dp))

            // First Name and Last Name Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // First Name
                OutlinedTextField(
                    value = firstName,
                    onValueChange = { firstName = it },
                    modifier = Modifier.weight(1f),
                    label = { Text(stringResource(R.string.first_name_label)) },
                    singleLine = true,
                    isError = showError && firstName.isBlank(),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Right) }
                    ),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppColors.Primary,
                        focusedLabelColor = AppColors.Primary,
                        cursorColor = AppColors.Primary
                    )
                )

                // Last Name
                OutlinedTextField(
                    value = lastName,
                    onValueChange = { lastName = it },
                    modifier = Modifier.weight(1f),
                    label = { Text(stringResource(R.string.last_name_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppColors.Primary,
                        focusedLabelColor = AppColors.Primary,
                        cursorColor = AppColors.Primary
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Mobile Number (Disabled)
            OutlinedTextField(
                value = currentUser?.phoneNumber ?: "",
                onValueChange = { },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.mobile_number_disabled_label)) },
                enabled = false,
                singleLine = true,
                supportingText = {
                    Text(
                        text = stringResource(R.string.cannot_be_changed),
                        color = AppColors.TextHint
                    )
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    disabledBorderColor = AppColors.Border,
                    disabledLabelColor = AppColors.TextSecondary,
                    disabledTextColor = AppColors.TextSecondary
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Email Address
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.email_address_label)) },
                placeholder = { Text(stringResource(R.string.email_placeholder)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { focusManager.clearFocus() }
                ),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppColors.Primary,
                    focusedLabelColor = AppColors.Primary,
                    cursorColor = AppColors.Primary
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Confirm Button
            PrimaryButton(
                text = stringResource(R.string.confirm),
                onClick = {
                    if (firstName.isNotBlank()) {
                        onSave(firstName, lastName, email)
                    } else {
                        showError = true
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}