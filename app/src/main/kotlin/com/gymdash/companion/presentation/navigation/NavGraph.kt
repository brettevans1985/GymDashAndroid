package com.gymdash.companion.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.gymdash.companion.presentation.history.SyncHistoryScreen
import com.gymdash.companion.presentation.home.HomeScreen
import com.gymdash.companion.presentation.login.LoginScreen
import com.gymdash.companion.presentation.settings.SettingsScreen

object Routes {
    const val LOGIN = "login"
    const val HOME = "home"
    const val SETTINGS = "settings"
    const val HISTORY = "history"
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
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) }
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
    }
}
