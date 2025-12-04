// ui/screens/main/MainScreen.kt
package com.mobitechs.parcelwala.ui.screens.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mobitechs.parcelwala.data.local.PreferencesManager
import com.mobitechs.parcelwala.data.manager.ActiveBooking
import com.mobitechs.parcelwala.data.model.response.OrderResponse
import com.mobitechs.parcelwala.ui.screens.account.AccountScreen
import com.mobitechs.parcelwala.ui.screens.home.HomeScreen
import com.mobitechs.parcelwala.ui.screens.orders.OrdersScreen
import com.mobitechs.parcelwala.ui.screens.payments.PaymentsScreen
import com.mobitechs.parcelwala.ui.theme.AppColors

/**
 * Bottom Navigation Items
 * Defines the tabs in the bottom navigation bar
 */
sealed class BottomNavItem(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Home : BottomNavItem(
        route = "home",
        title = "Home",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    )

    data object Orders : BottomNavItem(
        route = "orders",
        title = "Orders",
        selectedIcon = Icons.Filled.Receipt,
        unselectedIcon = Icons.Outlined.Receipt
    )

    data object Payments : BottomNavItem(
        route = "payments",
        title = "Payments",
        selectedIcon = Icons.Filled.Payment,
        unselectedIcon = Icons.Outlined.Payment
    )

    data object Account : BottomNavItem(
        route = "account",
        title = "Account",
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person
    )
}

/**
 * Main Screen with Bottom Navigation
 *
 * Contains:
 * - Home tab with vehicle selection
 * - Orders tab with order history
 * - Payments tab
 * - Account tab with profile management
 */
@Composable
fun MainScreen(
    preferencesManager: PreferencesManager,
    onNavigateToLogin: () -> Unit,
    onNavigateToLocationSearch: () -> Unit,
    onNavigateToOrderDetails: (OrderResponse) -> Unit = {},
    onBookAgain: (OrderResponse) -> Unit = {},
    onNavigateToActiveBooking: (ActiveBooking) -> Unit,
    onNavigateToSavedAddresses: () -> Unit = {},
    onNavigateToProfileDetails: () -> Unit = {},
    onNavigateToGSTDetails: () -> Unit = {},
    currentRoute: String = "home"
) {
    val navController = rememberNavController()
    val bottomNavItems = listOf(
        BottomNavItem.Home,
        BottomNavItem.Orders,
        BottomNavItem.Payments,
        BottomNavItem.Account
    )

    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                navController = navController,
                items = bottomNavItems
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            NavHost(
                navController = navController,
                startDestination = currentRoute.ifEmpty { BottomNavItem.Home.route }
            ) {
                // ============ HOME TAB ============
                composable(BottomNavItem.Home.route) {
                    HomeScreen(
                        onNavigateToLocationSearch = onNavigateToLocationSearch,
                        onNavigateToActiveBooking = { activeBooking ->
                            // Navigate to active booking screen
                            onNavigateToActiveBooking(activeBooking)
                        }
                    )
                }

                // ============ ORDERS TAB ============
                composable(BottomNavItem.Orders.route) {
                    OrdersScreen(
                        onOrderClick = { order ->
                            onNavigateToOrderDetails(order)
                        },
                        onBookAgain = { order ->
                            onBookAgain(order)
                        }
                    )
                }

                // ============ PAYMENTS TAB ============
                composable(BottomNavItem.Payments.route) {
                    PaymentsScreen()
                }

                // ============ ACCOUNT TAB ============
                composable(BottomNavItem.Account.route) {
                    AccountScreen(
                        onNavigateToSavedAddresses = onNavigateToSavedAddresses,
                        onNavigateToHelpSupport = { /* TODO: Navigate to Help */ },
                        onNavigateToGSTDetails = onNavigateToGSTDetails,
                        onNavigateToReferral = { /* TODO: Navigate to Referral */ },
                        onNavigateToTerms = { /* TODO: Navigate to Terms */ },
                        onLogout = onNavigateToLogin
                    )
                }
            }
        }
    }
}

/**
 * Bottom Navigation Bar
 * Custom styled navigation bar with orange theme
 */
@Composable
private fun BottomNavigationBar(
    navController: NavHostController,
    items: List<BottomNavItem>
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(
        containerColor = Color.White,
        contentColor = AppColors.Primary,
        tonalElevation = 8.dp
    ) {
        items.forEach { item ->
            val isSelected = currentRoute == item.route

            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.title
                    )
                },
                label = {
                    Text(
                        text = item.title,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                selected = isSelected,
                onClick = {
                    if (currentRoute != item.route) {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = AppColors.Primary,
                    selectedTextColor = AppColors.Primary,
                    unselectedIconColor = AppColors.TextSecondary,
                    unselectedTextColor = AppColors.TextSecondary,
                    // Transparent indicator - no light orange background
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}