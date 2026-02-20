// ui/screens/account/AccountScreen.kt
package com.mobitechs.parcelwala.ui.screens.account

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mobitechs.parcelwala.R
import com.mobitechs.parcelwala.ui.components.InfoCard
import com.mobitechs.parcelwala.ui.theme.AppColors
import com.mobitechs.parcelwala.ui.viewmodel.AccountViewModel

/**
 * Account Screen
 * Main profile/account management screen with navigation to sub-features
 *
 * Features:
 * - User profile header with View/Edit option
 * - Quick action cards (Saved Addresses, Help & Support)
 * - GST Details section
 * - Logout option
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    onNavigateToSavedAddresses: () -> Unit,
    onNavigateToHelpSupport: () -> Unit,
    onNavigateToGSTDetails: () -> Unit,
    onNavigateToReferral: () -> Unit,
    onNavigateToTerms: () -> Unit,
    onLogout: () -> Unit,
    viewModel: AccountViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showGSTBottomSheet by remember { mutableStateOf(false) }
    var showEditProfileSheet by remember { mutableStateOf(false) }

    // Logout confirmation dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Logout,
                    contentDescription = null,
                    tint = AppColors.Drop
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.logout_title),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(stringResource(R.string.logout_message))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        viewModel.logout { onLogout() }
                    }
                ) {
                    Text(stringResource(R.string.logout), color = AppColors.Drop)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text(stringResource(R.string.cancel), color = AppColors.TextSecondary)
                }
            },
            containerColor = Color.White
        )
    }

    // GST Bottom Sheet
    if (showGSTBottomSheet) {
        AddGSTBottomSheet(
            onDismiss = { showGSTBottomSheet = false },
            onSave = { gstin ->
                viewModel.saveGSTIN(gstin)
                showGSTBottomSheet = false
            }
        )
    }

    // Edit Profile Bottom Sheet
    if (showEditProfileSheet) {
        EditProfileBottomSheet(
            currentUser = uiState.user,
            onDismiss = { showEditProfileSheet = false },
            onSave = { firstName, lastName, email ->
                viewModel.updateProfile(firstName, lastName, email)
                showEditProfileSheet = false
            }
        )
    }

    Scaffold(
        containerColor = AppColors.Background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // ============ PROFILE HEADER ============
            ProfileHeaderCard(
                userName = uiState.userName ?: stringResource(R.string.default_user_name),
                email = uiState.email,
                isEmailVerified = uiState.isEmailVerified,
                onViewClick = { showEditProfileSheet = true },
                onAddGSTClick = { showGSTBottomSheet = true }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ============ QUICK ACTION CARDS ============
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Saved Addresses Card
                QuickActionCard(
                    icon = Icons.Default.Favorite,
                    title = stringResource(R.string.saved_addresses),
                    onClick = onNavigateToSavedAddresses,
                    modifier = Modifier.weight(1f)
                )

                // Help & Support Card
                QuickActionCard(
                    icon = Icons.Default.HelpOutline,
                    title = stringResource(R.string.help_support),
                    onClick = onNavigateToHelpSupport,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ============ GST DETAILS SECTION ============
            GSTDetailsCard(
                gstin = uiState.gstin,
                onAddGSTIN = { showGSTBottomSheet = true },
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ============ MENU ITEMS ============
            InfoCard(
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                // Referral
                MenuItemRow(
                    icon = Icons.Outlined.CardGiftcard,
                    title = stringResource(R.string.refer_earn),
                    subtitle = stringResource(R.string.refer_earn_subtitle),
                    onClick = onNavigateToReferral
                )

                HorizontalDivider(color = AppColors.Divider, thickness = 0.5.dp)

                // Terms & Conditions
                MenuItemRow(
                    icon = Icons.Outlined.Description,
                    title = stringResource(R.string.terms_conditions),
                    subtitle = stringResource(R.string.terms_subtitle),
                    onClick = onNavigateToTerms
                )

                HorizontalDivider(color = AppColors.Divider, thickness = 0.5.dp)

                // Logout
                MenuItemRow(
                    icon = Icons.Outlined.Logout,
                    title = stringResource(R.string.logout),
                    subtitle = stringResource(R.string.logout_subtitle),
                    onClick = { showLogoutDialog = true },
                    iconTint = AppColors.Drop,
                    textColor = AppColors.Drop
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // App Version
            Text(
                text = stringResource(R.string.version_format, stringResource(R.string.app_version)),
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextHint,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 100.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

/**
 * Profile Header Card
 * Displays user info with View and Add GST options
 */
@Composable
private fun ProfileHeaderCard(
    userName: String,
    email: String?,
    isEmailVerified: Boolean,
    onViewClick: () -> Unit,
    onAddGSTClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.Primary.copy(alpha = 0.05f))
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // View Button - Top Right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onViewClick) {
                    Text(
                        text = stringResource(R.string.view_label),
                        color = AppColors.Primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = AppColors.Primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Profile Avatar
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(AppColors.Primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = stringResource(R.string.profile_content_description),
                    tint = AppColors.Primary,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // User Name
            Text(
                text = userName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Email with verify option
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = email ?: stringResource(R.string.add_email),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextSecondary
                )
                if (email != null && !isEmailVerified) {
                    Text(
                        text = stringResource(R.string.verify),
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.Primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Add GST Details Button
            OutlinedButton(
                onClick = onAddGSTClick,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = AppColors.Primary
                ),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(AppColors.Primary)
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.add_gst_details),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/**
 * Quick Action Card
 * Reusable card for quick actions like Saved Addresses, Help
 */
@Composable
private fun QuickActionCard(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(100.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = AppColors.Primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = AppColors.TextPrimary
            )
        }
    }
}

/**
 * GST Details Card
 * Shows GST section with add option
 */
@Composable
private fun GSTDetailsCard(
    gstin: String?,
    onAddGSTIN: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Receipt,
                    contentDescription = null,
                    tint = AppColors.TextSecondary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = stringResource(R.string.gst_details),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.TextPrimary
                )
            }

            TextButton(onClick = onAddGSTIN) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = AppColors.Primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.add_gstin),
                    color = AppColors.Primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/**
 * Menu Item Row
 * Reusable menu item with icon, title, and subtitle
 */
@Composable
private fun MenuItemRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    iconTint: Color = AppColors.TextSecondary,
    textColor: Color = AppColors.TextPrimary
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = textColor
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextSecondary
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = AppColors.TextHint,
            modifier = Modifier.size(20.dp)
        )
    }
}