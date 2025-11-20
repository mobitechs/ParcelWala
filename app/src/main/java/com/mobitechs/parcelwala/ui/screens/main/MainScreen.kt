// MainScreen.kt
package com.mobitechs.parcelwala.ui.screens.main

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mobitechs.parcelwala.data.local.PreferencesManager
import com.mobitechs.parcelwala.ui.navigation.BottomNavItem
import com.mobitechs.parcelwala.ui.screens.home.HomeScreen
import com.mobitechs.parcelwala.ui.theme.AppTheme
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
                containerColor = AppTheme.colors.surface,
                contentColor = AppTheme.colors.primary
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
                            selectedIconColor = AppTheme.colors.primary,
                            selectedTextColor = AppTheme.colors.primary,
                            indicatorColor = AppTheme.colors.primaryContainer,
                            unselectedIconColor = AppTheme.colors.onSurfaceVariant,
                            unselectedTextColor = AppTheme.colors.onSurfaceVariant
                        )
                    )
                }
            }
        }
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
    Box(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.List,
                contentDescription = null,
                modifier = Modifier.size(100.dp),
                tint = AppTheme.colors.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "My Bookings",
                style = AppTheme.typography.headlineMedium,
                color = AppTheme.colors.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Your booking history will appear here",
                style = AppTheme.typography.bodyMedium,
                color = AppTheme.colors.onSurfaceVariant
            )
        }
    }
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
            title = { Text("Logout") },
            text = { Text("Are you sure you want to logout?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        kotlinx.coroutines.GlobalScope.launch {
                            preferencesManager.clearAll()
                        }
                        onNavigateToLogin()
                    }
                ) {
                    Text("Logout")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                actions = {
                    IconButton(onClick = { showLogoutDialog = true }) {
                        Icon(Icons.Default.ExitToApp, "Logout")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppTheme.colors.primary,
                    titleContentColor = AppTheme.colors.onPrimary,
                    actionIconContentColor = AppTheme.colors.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = AppTheme.colors.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = AppTheme.colors.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = user?.fullName ?: "Guest User",
                        style = AppTheme.typography.headlineSmall,
                        color = AppTheme.colors.onPrimaryContainer
                    )
                    Text(
                        text = user?.phoneNumber ?: "",
                        style = AppTheme.typography.bodyMedium,
                        color = AppTheme.colors.onPrimaryContainer
                    )
                    user?.email?.let {
                        Text(
                            text = it,
                            style = AppTheme.typography.bodySmall,
                            color = AppTheme.colors.onPrimaryContainer
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            user?.let { userData ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = AppTheme.colors.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        ProfileItem("Customer ID", userData.customerId.toString())
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        ProfileItem("Wallet Balance", "₹${userData.walletBalance}")
                        userData.referralCode?.let {
                            Divider(modifier = Modifier.padding(vertical = 8.dp))
                            ProfileItem("Referral Code", it)
                        }
                    }
                }
            }
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
            style = AppTheme.typography.bodyMedium,
            color = AppTheme.colors.onSurfaceVariant
        )
        Text(
            text = value,
            style = AppTheme.typography.bodyMedium,
            color = AppTheme.colors.onSurface
        )
    }
}