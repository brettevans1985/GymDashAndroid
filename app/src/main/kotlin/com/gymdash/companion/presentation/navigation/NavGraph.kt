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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.gymdash.companion.presentation.history.SyncHistoryScreen
import com.gymdash.companion.presentation.home.HomeScreen
import com.gymdash.companion.presentation.login.LoginScreen
import com.gymdash.companion.data.remote.dto.ThemeColorsDto
import com.gymdash.companion.presentation.settings.SettingsScreen
import com.gymdash.companion.presentation.settings.ThemeSettingsScreen
import com.gymdash.companion.ui.fooddiary.BarcodeScannerForBuilderScreen
import com.gymdash.companion.ui.fooddiary.BarcodeScannerScreen
import com.gymdash.companion.ui.fooddiary.FoodBuilderScreen
import com.gymdash.companion.ui.fooddiary.FoodBuilderViewModel
import com.gymdash.companion.ui.fooddiary.FoodDiaryScreen
import com.gymdash.companion.ui.fooddiary.FoodDiaryViewModel
import com.gymdash.companion.ui.fooddiary.FoodSearchScreen
import com.gymdash.companion.ui.fooddiary.WaterTrackerViewModel

object Routes {
    const val LOGIN = "login"
    const val HOME = "home"
    const val SETTINGS = "settings"
    const val HISTORY = "history"
    const val FOOD_DIARY = "food_diary"
    const val FOOD_SCANNER = "food_scanner/{date}"
    const val FOOD_SEARCH = "food_search/{date}"
    const val FOOD_BUILDER = "food_builder/{date}"
    const val FOOD_BUILDER_SCANNER = "food_builder_scanner"
    const val THEME_SETTINGS = "theme_settings"

    fun foodScanner(date: String) = "food_scanner/$date"
    fun foodSearch(date: String) = "food_search/$date"
    fun foodBuilder(date: String) = "food_builder/$date"
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
    onForceLoginHandled: () -> Unit = {},
    onThemeChanged: (ThemeColorsDto) -> Unit = {}
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
                    },
                    onNavigateToThemes = {
                        navController.navigate(Routes.THEME_SETTINGS)
                    }
                )
            }
            composable(Routes.THEME_SETTINGS) {
                ThemeSettingsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onThemeChanged = onThemeChanged
                )
            }
            composable(Routes.HISTORY) {
                SyncHistoryScreen()
            }
            composable(Routes.FOOD_DIARY) { backStackEntry ->
                val viewModel: FoodDiaryViewModel = hiltViewModel()
                val waterViewModel: WaterTrackerViewModel = hiltViewModel()

                // Auto-refresh when returning from add screens
                val entryAdded = backStackEntry.savedStateHandle.get<Boolean>("entry_added")
                LaunchedEffect(entryAdded) {
                    if (entryAdded == true) {
                        viewModel.loadDiary()
                        backStackEntry.savedStateHandle.remove<Boolean>("entry_added")
                    }
                }

                FoodDiaryScreen(
                    viewModel = viewModel,
                    waterTrackerViewModel = waterViewModel,
                    onNavigateToScanner = { date ->
                        navController.navigate(Routes.foodScanner(date))
                    },
                    onNavigateToSearch = { date ->
                        navController.navigate(Routes.foodSearch(date))
                    },
                    onNavigateToBuilder = { date ->
                        navController.navigate(Routes.foodBuilder(date))
                    }
                )
            }
            composable(
                Routes.FOOD_SCANNER,
                arguments = listOf(navArgument("date") { type = NavType.StringType })
            ) { backStackEntry ->
                val date = backStackEntry.arguments?.getString("date") ?: ""
                val viewModel: FoodDiaryViewModel = hiltViewModel()
                BarcodeScannerScreen(
                    repository = viewModel.repository,
                    date = date,
                    onNavigateBack = {
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set("entry_added", true)
                        navController.popBackStack()
                    },
                    onNavigateToSearch = {
                        navController.navigate(Routes.foodSearch(date))
                    }
                )
            }
            composable(
                Routes.FOOD_SEARCH,
                arguments = listOf(navArgument("date") { type = NavType.StringType })
            ) { backStackEntry ->
                val date = backStackEntry.arguments?.getString("date") ?: ""
                val viewModel: FoodDiaryViewModel = hiltViewModel()
                FoodSearchScreen(
                    repository = viewModel.repository,
                    date = date,
                    onNavigateBack = {
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set("entry_added", true)
                        navController.popBackStack()
                    }
                )
            }
            composable(
                Routes.FOOD_BUILDER,
                arguments = listOf(navArgument("date") { type = NavType.StringType })
            ) { backStackEntry ->
                val date = backStackEntry.arguments?.getString("date") ?: ""
                val viewModel: FoodBuilderViewModel = hiltViewModel()

                // Observe barcode returned from builder scanner
                val scannedBarcode = backStackEntry.savedStateHandle.get<String>("scanned_barcode")
                LaunchedEffect(scannedBarcode) {
                    if (scannedBarcode != null) {
                        viewModel.scanBarcode(scannedBarcode)
                        backStackEntry.savedStateHandle.remove<String>("scanned_barcode")
                    }
                }

                FoodBuilderScreen(
                    viewModel = viewModel,
                    date = date,
                    onNavigateBack = {
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set("entry_added", true)
                        navController.popBackStack()
                    },
                    onNavigateToScanner = { navController.navigate(Routes.FOOD_BUILDER_SCANNER) }
                )
            }
            composable(Routes.FOOD_BUILDER_SCANNER) {
                val viewModel: FoodDiaryViewModel = hiltViewModel()
                BarcodeScannerForBuilderScreen(
                    repository = viewModel.repository,
                    onNavigateBack = { navController.popBackStack() },
                    onBarcodeScanned = { barcode ->
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set("scanned_barcode", barcode)
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
