package com.personaltrainer.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.personaltrainer.ui.today.TodayScreen
import com.personaltrainer.ui.log.LogScreen
import com.personaltrainer.ui.plan.PlanScreen
import com.personaltrainer.ui.progress.ProgressScreen
import com.personaltrainer.ui.settings.SettingsScreen

/**
 * Bottom-navigation host — shown after onboarding is complete.
 */
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                bottomNavItems.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Route.TODAY,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Route.TODAY) { TodayScreen(navController) }
            composable(Route.LOG) { LogScreen(navController) }
            composable(Route.PROGRESS) { ProgressScreen(navController) }
            composable(Route.PLAN) { PlanScreen(navController) }
            composable(Route.SETTINGS) { SettingsScreen(navController) }
        }
    }
}

private data class BottomNavItem(val route: String, val label: String, val icon: ImageVector)

private val bottomNavItems = listOf(
    BottomNavItem(Route.TODAY, "Today", Icons.Filled.Today),
    BottomNavItem(Route.LOG, "Log", Icons.Filled.EditNote),
    BottomNavItem(Route.PROGRESS, "Progress", Icons.Filled.ShowChart),
    BottomNavItem(Route.PLAN, "Plan", Icons.Filled.CalendarMonth),
    BottomNavItem(Route.SETTINGS, "Settings", Icons.Filled.Settings)
)
