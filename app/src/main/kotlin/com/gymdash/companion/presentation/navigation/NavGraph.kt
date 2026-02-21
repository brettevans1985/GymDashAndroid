package com.gymdash.companion.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
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

    NavHost(navController = navController, startDestination = startDestination) {
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
            HomeScreen(
                onNavigateToHistory = { navController.navigate(Routes.HISTORY) },
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) },
                onNavigateToFoodDiary = { navController.navigate(Routes.FOOD_DIARY) }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onLogout = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.HISTORY) {
            SyncHistoryScreen(
                onNavigateBack = { navController.popBackStack() }
            )
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
