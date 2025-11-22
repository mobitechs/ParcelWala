package com.mobitechs.parcelwala.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mobitechs.parcelwala.ui.theme.AppColors

/**
 * Reusable Input Components
 * Consistent form inputs with validation and orange theme
 */

/**
 * Standard Text Input Field
 */
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    onTrailingIconClick: (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    isError: Boolean = false,
    errorMessage: String = "",
    enabled: Boolean = true,
    singleLine: Boolean = true,
    maxLines: Int = 1,
    maxLength: Int? = null
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = { newValue ->
                if (maxLength == null || newValue.length <= maxLength) {
                    onValueChange(newValue)
                }
            },
            label = {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            placeholder = {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextHint
                )
            },
            leadingIcon = leadingIcon?.let {
                {
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        tint = if (isError) AppColors.Drop else AppColors.TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            },
            trailingIcon = trailingIcon?.let {
                {
                    IconButton(
                        onClick = { onTrailingIconClick?.invoke() },
                        enabled = onTrailingIconClick != null
                    ) {
                        Icon(
                            imageVector = it,
                            contentDescription = null,
                            tint = AppColors.TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            singleLine = singleLine,
            maxLines = maxLines,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            isError = isError,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AppColors.Primary,
                unfocusedBorderColor = AppColors.Border,
                errorBorderColor = AppColors.Drop,
                focusedLabelColor = AppColors.Primary,
                unfocusedLabelColor = AppColors.TextSecondary,
                cursorColor = AppColors.Primary,
                disabledBorderColor = AppColors.Border.copy(alpha = 0.5f),
                disabledTextColor = AppColors.TextSecondary
            ),
            shape = RoundedCornerShape(12.dp)
        )

        // Error Message
        AnimatedVisibility(visible = isError && errorMessage.isNotEmpty()) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.Drop,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }

        // Character Counter
        if (maxLength != null) {
            Text(
                text = "${value.length}/$maxLength",
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.TextHint,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 16.dp, top = 4.dp)
                    .wrapContentWidth(androidx.compose.ui.Alignment.End)
            )
        }
    }
}

/**
 * Phone Number Input
 */
@Composable
fun PhoneNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String = "Mobile Number",
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    errorMessage: String = "",
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    AppTextField(
        value = value,
        onValueChange = onValueChange,
        label = "$label *",
        placeholder = "10 digit mobile number",
        leadingIcon = androidx.compose.material.icons.Icons.Default.Phone,
        modifier = modifier,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Phone,
            imeAction = ImeAction.Next
        ),
        keyboardActions = keyboardActions,
        maxLength = 10,
        isError = isError,
        errorMessage = errorMessage
    )
}

/**
 * Name Input Field
 */
@Composable
fun NameInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String = "Full Name",
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    errorMessage: String = "",
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    AppTextField(
        value = value,
        onValueChange = onValueChange,
        label = "$label *",
        placeholder = "Enter full name",
        leadingIcon = androidx.compose.material.icons.Icons.Default.Person,
        modifier = modifier,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Next
        ),
        keyboardActions = keyboardActions,
        isError = isError,
        errorMessage = errorMessage
    )
}

/**
 * Address Input Field
 */
@Composable
fun AddressInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String = "House/Flat/Shop No., Floor",
    modifier: Modifier = Modifier,
    isRequired: Boolean = false,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    AppTextField(
        value = value,
        onValueChange = onValueChange,
        label = if (isRequired) "$label *" else "$label (Optional)",
        placeholder = "e.g., Flat 101, 2nd Floor",
        leadingIcon = androidx.compose.material.icons.Icons.Default.Home,
        modifier = modifier,
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Next
        ),
        keyboardActions = keyboardActions
    )
}

/**
 * Search Input Field
 */
@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    onClear: (() -> Unit)? = null
) {
    AppTextField(
        value = value,
        onValueChange = onValueChange,
        label = "",
        placeholder = placeholder,
        leadingIcon = androidx.compose.material.icons.Icons.Default.Search,
        trailingIcon = if (value.isNotEmpty())
            androidx.compose.material.icons.Icons.Default.Clear
        else
            null,
        onTrailingIconClick = onClear,
        modifier = modifier,
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Search
        )
    )
}