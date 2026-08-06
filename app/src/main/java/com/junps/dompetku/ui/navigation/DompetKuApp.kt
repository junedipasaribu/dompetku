package com.junps.dompetku.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.junps.dompetku.ui.category.CategoryScreen
import com.junps.dompetku.ui.dashboard.DashboardScreen
import com.junps.dompetku.ui.analytics.AnalyticsScreen
import com.junps.dompetku.ui.history.HistoryScreen
import com.junps.dompetku.ui.transaction.TransactionFormScreen
import com.junps.dompetku.ui.theme.DompetBackground
import com.junps.dompetku.ui.theme.DompetGreen

@Composable
fun DompetKuApp(
    navController: NavHostController = rememberNavController(),
    dashboardContent: @Composable (onTransactionClick: (String) -> Unit) -> Unit = { onTransactionClick ->
        DashboardScreen(onTransactionClick = onTransactionClick)
    },
    historyContent: @Composable (onTransactionClick: (String) -> Unit) -> Unit = { onTransactionClick ->
        HistoryScreen(onTransactionClick = onTransactionClick)
    },
    transactionFormContent: @Composable (onNavigateBack: () -> Unit) -> Unit = { onNavigateBack ->
        TransactionFormScreen(onNavigateBack = onNavigateBack)
    },
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val isDashboard = currentDestination?.hierarchy?.any {
        it.route == AppDestination.Dashboard.route
    } == true
    val showBottomBar = AppDestination.bottomBarItems.any { destination ->
        currentDestination?.hierarchy?.any { it.route == destination.route } == true
    }

    Scaffold(
        containerColor = DompetBackground,
        bottomBar = {
            if (showBottomBar) {
                DompetKuBottomBar(
                    currentRoute = currentDestination?.route,
                    onDestinationSelected = { destination ->
                        navController.navigate(destination.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        },
        floatingActionButton = {
            if (isDashboard) {
                FloatingActionButton(
                    onClick = { navController.navigate(TransactionFormDestination.createRoute()) },
                    containerColor = DompetGreen,
                    contentColor = Color.White,
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Tambah transaksi")
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppDestination.Dashboard.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(AppDestination.Dashboard.route) {
                dashboardContent { transactionId ->
                    navController.navigate(TransactionFormDestination.createRoute(transactionId))
                }
            }
            composable(AppDestination.History.route) {
                historyContent { transactionId ->
                    navController.navigate(TransactionFormDestination.createRoute(transactionId))
                }
            }
            composable(AppDestination.Analytics.route) {
                AnalyticsScreen()
            }
            composable(AppDestination.Categories.route) {
                CategoryScreen()
            }
            composable(
                route = TransactionFormDestination.route,
                arguments = listOf(
                    navArgument(TransactionFormDestination.transactionIdArgument) {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
            ) {
                transactionFormContent { navController.popBackStack() }
            }
        }
    }
}

@Composable
private fun DompetKuBottomBar(
    currentRoute: String?,
    onDestinationSelected: (AppDestination) -> Unit,
) {
    NavigationBar(containerColor = Color.White) {
        AppDestination.bottomBarItems.forEach { destination ->
            NavigationBarItem(
                selected = currentRoute == destination.route,
                onClick = { onDestinationSelected(destination) },
                icon = {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = destination.label,
                    )
                },
                label = { Text(destination.label) },
            )
        }
    }
}
