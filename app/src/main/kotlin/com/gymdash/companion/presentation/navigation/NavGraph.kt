package com.gymdash.companion.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.gymdash.companion.presentation.history.SyncHistoryScreen
import com.gymdash.companion.presentation.home.HomeScreen
import com.gymdash.companion.presentation.login.LoginScreen
import com.gymdash.companion.presentation.settings.SettingsScreen
import com.gymdash.companion.ui.fooddiary.BarcodeScannerScreen
import com.gymdash.companion.ui.fooddiary.FoodDiaryScreen
import com.gymdash.companion.ui.fooddiary.FoodDiaryViewModel
import com.gymdash.companion.ui.fooddiary.FoodSearchScreen

object Routes {
    const val LOGIN = "login"
    const val HOME = "home"
    const val SETTINGS = "settings"
    const val HISTORY = "history"
    const val FOOD_DIARY = "food_diary"
    const val FOOD_SCANNER = "food_scanner"
    const val FOOD_SEARCH = "food_search"
}

data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(Routes.HOME, "Home", Icons.Default.Home),
    BottomNavItem(Routes.FOOD_DIARY, "Food Diary", Icons.Default.Create),
    BottomNavItem(Routes.HISTORY, "History", Icons.Default.History),
    BottomNavItem(Routes.SETTINGS, "Settings", Icons.Default.Settings),
)

private val bottomNavRoutes = bottomNavItems.map { it.route }.toSet()

@Composable
fun NavGraph(
    startDestination: String = Routes.LOGIN,
    forceLoginNavigation: Boolean = false,
    onForceLoginHandled: () -> Unit = {}
) {
    val navController = rememberNavController()

    LaunchedEffect(forceLoginNavigation) {
        if (forceLoginNavigation) {
            navController.navigate(Routes.LOGIN) {
                popUpTo(0) { inclusive = true }
            }
            onForceLoginHandled()
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute in bottomNavRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val selected = navBackStackEntry?.destination?.hierarchy?.any {
                            it.route == item.route
                        } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.LOGIN) {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    }
                )
            }
            composable(Routes.HOME) {
                HomeScreen()
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onLogout = {
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
            composable(Routes.HISTORY) {
                SyncHistoryScreen()
            }
            composable(Routes.FOOD_DIARY) {
                val viewModel: FoodDiaryViewModel = hiltViewModel()
                FoodDiaryScreen(
                    repository = viewModel.repository,
                    onNavigateToScanner = { navController.navigate(Routes.FOOD_SCANNER) },
                    onNavigateToSearch = { navController.navigate(Routes.FOOD_SEARCH) }
                )
            }
            composable(Routes.FOOD_SCANNER) {
                val viewModel: FoodDiaryViewModel = hiltViewModel()
                BarcodeScannerScreen(
                    repository = viewModel.repository,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToSearch = { navController.navigate(Routes.FOOD_SEARCH) }
                )
            }
            composable(Routes.FOOD_SEARCH) {
                val viewModel: FoodDiaryViewModel = hiltViewModel()
                FoodSearchScreen(
                    repository = viewModel.repository,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
