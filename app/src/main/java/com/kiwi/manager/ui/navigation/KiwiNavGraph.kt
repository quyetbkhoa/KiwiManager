package com.kiwi.manager.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.kiwi.manager.domain.model.ThemeMode
import com.kiwi.manager.ui.component.GlassBottomNav
import com.kiwi.manager.ui.component.NavTab
import com.kiwi.manager.ui.screen.adbconnect.AdbConnectScreen
import com.kiwi.manager.ui.screen.adbconnect.AdbConnectViewModel
import com.kiwi.manager.ui.screen.appdetail.AppDetailScreen
import com.kiwi.manager.ui.screen.appdetail.AppDetailViewModel
import com.kiwi.manager.ui.screen.home.HomeScreen
import com.kiwi.manager.ui.screen.home.HomeViewModel
import com.kiwi.manager.ui.screen.settings.SettingsScreen
import com.kiwi.manager.ui.screen.settings.SettingsViewModel
import com.kiwi.manager.ui.screen.watchapps.WatchAppManagerScreen
import com.kiwi.manager.ui.screen.watchapps.WatchAppViewModel

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object AppDetail : Screen("app_detail/{appId}") {
        fun createRoute(appId: String) = "app_detail/$appId"
    }
    data object WatchApps : Screen("watch_apps")
    data object AdbConnect : Screen("adb_connect")
    data object Settings : Screen("settings")
}

@Composable
fun KiwiNavGraph(
    navController: NavHostController,
    themeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Home.route
    val isBottomNavVisible = currentRoute in listOf(
        Screen.Home.route,
        Screen.WatchApps.route,
        Screen.AdbConnect.route,
        Screen.Settings.route
    )

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.fillMaxSize()
        ) {
            composable(route = Screen.Home.route) {
                val viewModel: HomeViewModel = viewModel()
                HomeScreen(
                    viewModel = viewModel,
                    onNavigateToAppDetail = { appId ->
                        navController.navigate(Screen.AppDetail.createRoute(appId))
                    },
                    onNavigateToAdbConnect = {
                        navController.navigate(Screen.AdbConnect.route) {
                            popUpTo(Screen.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
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
                    onNavigateToAdbConnect = {
                        navController.navigate(Screen.AdbConnect.route)
                    }
                )
            }

            composable(route = Screen.WatchApps.route) {
                val viewModel: WatchAppViewModel = viewModel()
                WatchAppManagerScreen(
                    viewModel = viewModel,
                    onNavigateToAdbConnect = {
                        navController.navigate(Screen.AdbConnect.route) {
                            popUpTo(Screen.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateBack = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(route = Screen.AdbConnect.route) {
                val viewModel: AdbConnectViewModel = viewModel()
                AdbConnectScreen(
                    viewModel = viewModel,
                    onNavigateToWatchApps = {
                        navController.navigate(Screen.WatchApps.route) {
                            popUpTo(Screen.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateBack = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(route = Screen.Settings.route) {
                val viewModel: SettingsViewModel = viewModel()
                SettingsScreen(
                    viewModel = viewModel,
                    themeMode = themeMode,
                    onThemeChange = onThemeChange,
                    onNavigateBack = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    }
                )
            }
        }

        // Floating Glass Bottom Navigation Bar
        AnimatedVisibility(
            visible = isBottomNavVisible,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            GlassBottomNav(
                currentRoute = currentRoute,
                onTabSelected = { tab ->
                    if (currentRoute != tab.route) {
                        navController.navigate(tab.route) {
                            popUpTo(Screen.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }
    }
}
