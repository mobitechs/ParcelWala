package com.mobitechs.parcelwala.ui.screens.main

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mobitechs.parcelwala.data.local.PreferencesManager
import com.mobitechs.parcelwala.ui.components.*
import com.mobitechs.parcelwala.ui.navigation.BottomNavItem
import com.mobitechs.parcelwala.ui.screens.home.HomeScreen
import com.mobitechs.parcelwala.ui.theme.AppColors
import com.mobitechs.parcelwala.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    preferencesManager: PreferencesManager,
    onNavigateToLogin: () -> Unit,
    onNavigateToLocationSearch: () -> Unit,
    currentRoute: String = "home",
    viewModel: MainViewModel = hiltViewModel()
) {
    val selectedTab by viewModel.selectedTab.collectAsState()

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                contentColor = AppColors.Primary
            ) {
                listOf(
                    BottomNavItem.Home,
                    BottomNavItem.Bookings,
                    BottomNavItem.Profile
                ).forEach { item ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.title
                            )
                        },
                        label = { Text(item.title) },
                        selected = currentRoute == item.route,
                        onClick = { viewModel.selectTab(item) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AppColors.Primary,
                            selectedTextColor = AppColors.Primary,
                            indicatorColor = AppColors.PrimaryLight,
                            unselectedIconColor = AppColors.TextSecondary,
                            unselectedTextColor = AppColors.TextSecondary
                        )
                    )
                }
            }
        },
        containerColor = AppColors.Background
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (currentRoute) {
                "home" -> HomeScreen(onNavigateToLocationSearch = onNavigateToLocationSearch)
                "bookings" -> BookingsScreen()
                "profile" -> ProfileScreen(preferencesManager, onNavigateToLogin)
            }
        }
    }
}

@Composable
fun BookingsScreen() {
    EmptyState(
        icon = Icons.Default.List,
        title = "My Bookings",
        subtitle = "Your booking history will appear here",
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    preferencesManager: PreferencesManager,
    onNavigateToLogin: () -> Unit
) {
    val user = preferencesManager.getUser()
    var showLogoutDialog by remember { mutableStateOf(false) }

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
            title = { Text("Logout", color = AppColors.TextPrimary) },
            text = {
                Text(
                    "Are you sure you want to logout?",
                    color = AppColors.TextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        kotlinx.coroutines.GlobalScope.launch {
                            preferencesManager.clearAll()
                        }
                        onNavigateToLogin()
                    }
                ) {
                    Text("Logout", color = AppColors.Drop)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel", color = AppColors.Primary)
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
                        "Profile",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = { showLogoutDialog = true }) {
                        Icon(
                            Icons.Default.Logout,
                            "Logout",
                            tint = AppColors.Drop
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        containerColor = AppColors.Background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Profile Card
            InfoCard(elevation = 4.dp) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    IconButtonWithBackground(
                        icon = Icons.Default.Person,
                        contentDescription = "Profile",
                        onClick = { },
                        size = 64.dp
                    )

                    Column {
                        Text(
                            text = user?.fullName ?: "Guest User",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.TextPrimary
                        )
                        Text(
                            text = user?.phoneNumber ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppColors.TextSecondary
                        )
                        user?.email?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = AppColors.TextHint
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            user?.let { userData ->
                InfoCard {
                    SectionHeader(text = "Account Details")

                    Spacer(modifier = Modifier.height(16.dp))

                    ProfileItem("Customer ID", userData.customerId.toString())
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = AppColors.Divider
                    )
                    ProfileItem("Wallet Balance", "₹${userData.walletBalance}")

                    userData.referralCode?.let {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = AppColors.Divider
                        )
                        ProfileItem("Referral Code", it)
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Logout Button
            PrimaryButton(
                text = "Logout",
                onClick = { showLogoutDialog = true },
                icon = Icons.Default.Logout,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun ProfileItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.TextSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = AppColors.TextPrimary
        )
    }
}