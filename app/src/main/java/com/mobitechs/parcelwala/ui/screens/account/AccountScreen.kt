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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import com.mobitechs.parcelwala.ui.components.StatusBarScaffold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    onNavigateToSavedAddresses: () -> Unit,
    onNavigateToHelpSupport: () -> Unit,
    onNavigateToGSTDetails: () -> Unit,
    onNavigateToReferral: () -> Unit,
    onNavigateToTerms: () -> Unit,
    onNavigateToLanguage: () -> Unit,
    onLogout: () -> Unit,
    viewModel: AccountViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showGSTBottomSheet by remember { mutableStateOf(false) }
    var showEditProfileSheet by remember { mutableStateOf(false) }

    //cal it in future
//    private fun loadUserStats() {
//     viewModelScope.launch {
//         val stats = userRepository.getUserStats()
//         _uiState.update {
//             it.copy(
//                 totalOrders = stats.totalOrders,
//                 referralEarned = stats.referralEarned
//             )
//         }
//     }
// }

    // ── Logout dialog ──────────────────────────────────────────────────────────
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
            text = { Text(stringResource(R.string.logout_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    viewModel.logout { onLogout() }
                }) {
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

    // ── Bottom sheets ──────────────────────────────────────────────────────────
    if (showGSTBottomSheet) {
        AddGSTBottomSheet(
            onDismiss = { showGSTBottomSheet = false },
            onSave = { gstin ->
                viewModel.saveGSTIN(gstin)
                showGSTBottomSheet = false
            }
        )
    }

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

    StatusBarScaffold(
        statusBarColor = AppColors.Primary,
        darkStatusBarIcons = false,
        containerColor = AppColors.Background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {

            // ── Profile header ─────────────────────────────────────────────────
            ProfileHeaderCard(
                userName = uiState.userName ?: stringResource(R.string.default_user_name),
                phoneNumber = uiState.phoneNumber,
                totalOrders = uiState.totalOrders ?: 0,
                referralEarned = uiState.referralEarned ?: 0,
                onEditClick = { showEditProfileSheet = true }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── "My Account" group ─────────────────────────────────────────────
            SectionLabel(text = stringResource(R.string.my_account_section))

            MenuCard(modifier = Modifier.padding(horizontal = 16.dp)) {

                MenuItemRow(
                    icon = Icons.Outlined.Home,
                    iconBackgroundColor = Color(0xFFFFF3F0),
                    iconTint = AppColors.Primary,
                    title = stringResource(R.string.saved_addresses),
                    subtitle = stringResource(R.string.saved_addresses_subtitle),
                    onClick = onNavigateToSavedAddresses
                )

                MenuDivider()

                MenuItemRow(
                    icon = Icons.Default.Receipt,
                    iconBackgroundColor = Color(0xFFFFFBF0),
                    iconTint = Color(0xFFD4900A),
                    title = stringResource(R.string.gst_details),
                    subtitle = stringResource(R.string.gst_details_subtitle),
                    onClick = { showGSTBottomSheet = true }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── "More" group ───────────────────────────────────────────────────
            SectionLabel(text = stringResource(R.string.more_section))

            MenuCard(modifier = Modifier.padding(horizontal = 16.dp)) {

                MenuItemRow(
                    icon = Icons.Outlined.CardGiftcard,
                    iconBackgroundColor = Color(0xFFF0FAF3),
                    iconTint = Color(0xFF2D9B44),
                    title = stringResource(R.string.refer_earn),
                    subtitle = stringResource(R.string.refer_earn_subtitle),
                    onClick = onNavigateToReferral,
                    trailingBadge = {
                        ReferralBadge(text = stringResource(R.string.referral_badge_value))
                    }
                )

                MenuDivider()

                MenuItemRow(
                    icon = Icons.Outlined.Language,
                    iconBackgroundColor = Color(0xFFF5F0FF),
                    iconTint = Color(0xFF7C5DE8),
                    title = stringResource(R.string.language),
                    subtitle = stringResource(R.string.language_options_display),
                    onClick = onNavigateToLanguage
                )

                MenuDivider()

                MenuItemRow(
                    icon = Icons.Outlined.HelpOutline,
                    iconBackgroundColor = Color(0xFFF0F5FF),
                    iconTint = Color(0xFF4A6FE8),
                    title = stringResource(R.string.help_support),
                    subtitle = stringResource(R.string.help_support_subtitle),
                    onClick = onNavigateToHelpSupport
                )

                MenuDivider()

                MenuItemRow(
                    icon = Icons.Outlined.Description,
                    iconBackgroundColor = Color(0xFFF4F4F4),
                    iconTint = AppColors.TextSecondary,
                    title = stringResource(R.string.terms_conditions),
                    subtitle = stringResource(R.string.terms_subtitle),
                    onClick = onNavigateToTerms
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Logout (V3 style — plain card, red accent) ─────────────────────
            MenuCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                MenuItemRow(
                    icon = Icons.Outlined.Logout,
                    iconBackgroundColor = Color(0xFFFFEEEE),
                    iconTint = AppColors.Drop,
                    title = stringResource(R.string.logout),
                    subtitle = stringResource(R.string.logout_subtitle),
                    titleColor = AppColors.Drop,
                    chevronTint = AppColors.Drop,
                    onClick = { showLogoutDialog = true }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── App version ────────────────────────────────────────────────────
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

// ══════════════════════════════════════════════════════════════════════════════
// Profile Header  (Variation 2 style)
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun ProfileHeaderCard(
    userName: String,
    phoneNumber: String?,
    totalOrders: Int,
    referralEarned: Int,
    onEditClick: () -> Unit
) {

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.Primary)
            .padding(horizontal = 20.dp)
            .padding(top =  20.dp, bottom = 24.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {

            // Avatar row + Edit button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Rounded-square avatar
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.20f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = stringResource(R.string.profile_content_description),
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Edit profile chip
                Surface(
                    onClick = onEditClick,
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = 0.18f),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.40f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = stringResource(R.string.edit_profile_label),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Name + phone
            Text(
                text = userName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = phoneNumber ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.80f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Stat cards row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    value = totalOrders.toString(),
                    label = stringResource(R.string.stat_total_orders),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    value = "₹$referralEarned",
                    label = stringResource(R.string.stat_referral_earned),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun StatCard(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.16f)
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.75f)
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Reusable components
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(
            letterSpacing = 0.6.sp,
            fontSize = 10.sp
        ),
        fontWeight = FontWeight.SemiBold,
        color = AppColors.TextSecondary,
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .padding(bottom = 6.dp)
    )
}

@Composable
private fun MenuCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        content = {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                content()
            }
        }
    )
}

@Composable
private fun MenuDivider() {
    HorizontalDivider(
        color = AppColors.Divider,
        thickness = 0.5.dp
    )
}

@Composable
private fun MenuItemRow(
    icon: ImageVector,
    iconBackgroundColor: Color,
    iconTint: Color,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    titleColor: Color = AppColors.TextPrimary,
    chevronTint: Color = AppColors.TextHint,
    trailingBadge: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Icon container
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconBackgroundColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
        }

        // Text
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = titleColor
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextSecondary
                )
            }
        }

        // Optional badge
        trailingBadge?.invoke()

        // Chevron
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = chevronTint,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun ReferralBadge(text: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFE9F7EC)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF188038),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}