// ui/screens/booking/AddGSTINScreen.kt
package com.mobitechs.parcelwala.ui.screens.booking

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import com.mobitechs.parcelwala.R
import com.mobitechs.parcelwala.ui.components.InfoCard
import com.mobitechs.parcelwala.ui.components.PrimaryButton
import com.mobitechs.parcelwala.ui.theme.AppColors

/**
 * Add GSTIN Screen
 * Allows user to add GST number for invoice generation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGSTINScreen(
    onSave: (String) -> Unit,
    onBack: () -> Unit
) {
    var gstin by remember { mutableStateOf("") }
    var companyName by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current

    // GSTIN validation regex (15 characters)
    val gstinRegex = Regex("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.add_gst_details_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = AppColors.TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        containerColor = AppColors.Background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                // Info Banner
                InfoCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = AppColors.Primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = stringResource(R.string.why_add_gstin),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = AppColors.TextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.gstin_benefit_description),
                                style = MaterialTheme.typography.bodySmall,
                                color = AppColors.TextSecondary
                            )
                        }
                    }
                }

                // Form Fields
                InfoCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.enter_gst_details),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // GSTIN Field
                    OutlinedTextField(
                        value = gstin,
                        onValueChange = {
                            if (it.length <= 15) {
                                gstin = it.uppercase()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.gstin_label)) },
                        placeholder = { Text(stringResource(R.string.gstin_placeholder)) },
                        singleLine = true,
                        isError = showError && !gstinRegex.matches(gstin),
                        supportingText = {
                            if (showError && !gstinRegex.matches(gstin)) {
                                Text(
                                    text = stringResource(R.string.gstin_invalid_error),
                                    color = MaterialTheme.colorScheme.error
                                )
                            } else {
                                Text(
                                    text = stringResource(R.string.gstin_char_count, gstin.length),
                                    color = AppColors.TextSecondary
                                )
                            }
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Receipt,
                                contentDescription = null,
                                tint = AppColors.Primary
                            )
                        },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AppColors.Primary,
                            focusedLabelColor = AppColors.Primary
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Company Name Field
                    OutlinedTextField(
                        value = companyName,
                        onValueChange = { companyName = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.company_name_label)) },
                        placeholder = { Text(stringResource(R.string.company_name_placeholder)) },
                        singleLine = true,
                        isError = showError && companyName.isBlank(),
                        supportingText = {
                            if (showError && companyName.isBlank()) {
                                Text(
                                    text = stringResource(R.string.company_name_required),
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Business,
                                contentDescription = null,
                                tint = AppColors.Primary
                            )
                        },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { focusManager.clearFocus() }
                        ),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AppColors.Primary,
                            focusedLabelColor = AppColors.Primary
                        )
                    )
                }

                // Benefits Section
                InfoCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.benefits_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    BenefitItem(icon = Icons.Default.CheckCircle, text = stringResource(R.string.benefit_itc))
                    Spacer(modifier = Modifier.height(8.dp))
                    BenefitItem(icon = Icons.Default.CheckCircle, text = stringResource(R.string.benefit_invoice))
                    Spacer(modifier = Modifier.height(8.dp))
                    BenefitItem(icon = Icons.Default.CheckCircle, text = stringResource(R.string.benefit_accounting))
                    Spacer(modifier = Modifier.height(8.dp))
                    BenefitItem(icon = Icons.Default.CheckCircle, text = stringResource(R.string.benefit_tax_filing))
                }

                Spacer(modifier = Modifier.height(100.dp))
            }

            // Save Button
            Surface(
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                PrimaryButton(
                    text = stringResource(R.string.save_gst_details),
                    onClick = {
                        if (gstinRegex.matches(gstin) && companyName.isNotBlank()) {
                            onSave(gstin)
                        } else {
                            showError = true
                        }
                    },
                    icon = Icons.Default.Save,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            }
        }
    }
}

/**
 * Benefit Item Component
 */
@Composable
private fun BenefitItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = AppColors.Pickup,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.TextPrimary
        )
    }
}