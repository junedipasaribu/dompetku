package com.junps.dompetku.ui.navigation

import androidx.compose.foundation.layout.padding
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
import com.junps.dompetku.ui.category.CategoryScreen
import com.junps.dompetku.ui.components.FeaturePlaceholder
import com.junps.dompetku.ui.theme.DompetBackground

@Composable
fun DompetKuApp(
    navController: NavHostController = rememberNavController(),
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
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
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppDestination.Dashboard.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(AppDestination.Dashboard.route) {
                DestinationPlaceholder(AppDestination.Dashboard, "Ringkasan keuangan akan tersedia di branch dashboard.")
            }
            composable(AppDestination.History.route) {
                DestinationPlaceholder(AppDestination.History, "Riwayat transaksi akan tersedia di branch history.")
            }
            composable(AppDestination.Analytics.route) {
                DestinationPlaceholder(AppDestination.Analytics, "Analisis keuangan akan tersedia di branch analytics.")
            }
            composable(AppDestination.Categories.route) {
                CategoryScreen()
            }
        }
    }
}

@Composable
private fun DestinationPlaceholder(
    destination: AppDestination,
    description: String,
) {
    FeaturePlaceholder(
        title = destination.label,
        description = description,
        icon = destination.icon,
    )
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
