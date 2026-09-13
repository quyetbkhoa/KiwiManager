package com.kiwi.manager.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.kiwi.manager.domain.model.ThemeMode
import com.kiwi.manager.ui.screen.adbconnect.AdbConnectScreen
import com.kiwi.manager.ui.screen.adbconnect.AdbConnectViewModel
import com.kiwi.manager.ui.screen.appdetail.AppDetailScreen
import com.kiwi.manager.ui.screen.appdetail.AppDetailViewModel
import com.kiwi.manager.ui.screen.home.HomeScreen
import com.kiwi.manager.ui.screen.home.HomeViewModel
import com.kiwi.manager.ui.screen.settings.SettingsScreen
import com.kiwi.manager.ui.screen.settings.SettingsViewModel

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object AppDetail : Screen("app_detail/{appId}") {
        fun createRoute(appId: String) = "app_detail/$appId"
    }
    data object AdbConnect : Screen("adb_connect")
    data object Settings : Screen("settings")
}

@Composable
fun KiwiNavGraph(
    navController: NavHostController,
    themeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(route = Screen.Home.route) {
            val viewModel: HomeViewModel = viewModel()
            HomeScreen(
                viewModel = viewModel,
                onNavigateToAppDetail = { appId ->
                    navController.navigate(Screen.AppDetail.createRoute(appId))
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToAdbConnect = {
                    navController.navigate(Screen.AdbConnect.route)
                }
            )
        }

        composable(
            route = Screen.AppDetail.route,
            arguments = listOf(navArgument("appId") { type = NavType.StringType })
        ) {
            val viewModel: AppDetailViewModel = viewModel()
            AppDetailScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAdbConnect = { navController.navigate(Screen.AdbConnect.route) }
            )
        }

        composable(route = Screen.AdbConnect.route) {
            val viewModel: AdbConnectViewModel = viewModel()
            AdbConnectScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(route = Screen.Settings.route) {
            val viewModel: SettingsViewModel = viewModel()
            SettingsScreen(
                viewModel = viewModel,
                themeMode = themeMode,
                onThemeChange = onThemeChange,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
