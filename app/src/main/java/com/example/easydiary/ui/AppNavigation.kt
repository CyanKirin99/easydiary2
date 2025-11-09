// 文件位置: app/src/main/java/com/example/easydiary/ui/AppNavigation.kt
package com.example.easydiary.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.easydiary.ui.entry.EntryScreen
import com.example.easydiary.ui.home.HomeScreen
import com.example.easydiary.ui.settings.LogTypeSettingsScreen
import com.example.easydiary.ui.settings.SettingsScreen
import com.example.easydiary.ui.settings.ThemeSettingsScreen
import com.example.easydiary.ui.settings.ViewSettingsScreen
import com.example.easydiary.ui.statistics.StatisticsScreen
import java.time.LocalDate

// V2 导航路由
sealed class Screen(val route: String, val label: String? = null, val icon: ImageVector? = null) {
    object Home : Screen("home", "主页", Icons.Default.Home)
    object Settings : Screen("settings", "我的", Icons.Default.Person)
    object Add : Screen(route = "entry") {
        const val routeTemplate = "entry/{date}"
        fun createRoute(date: LocalDate): String = "entry/${date}"
    }
    object LogTypeSettings : Screen("log_type_settings")
    object ThemeSettings : Screen("theme_settings")
    object ViewSettings : Screen("view_settings")
    object Statistics : Screen("statistics")
}

val navItems = listOf(Screen.Home, Screen.Settings)

@Composable
fun AppNavigation(viewModel: DiaryViewModel) {
    val navController = rememberNavController()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val isMainScreen = navItems.any { it.route == currentDestination?.route }

    Scaffold(
        bottomBar = {
            if (isMainScreen) {
                AppBottomBar(
                    navController = navController,
                    currentDestination = currentDestination
                )
            }
        },
        floatingActionButton = {
            if (isMainScreen) {
                FloatingActionButton(
                    onClick = {
                        navController.navigate(Screen.Add.createRoute(LocalDate.now()))
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, "添加", modifier = Modifier.size(36.dp))
                }
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(paddingValues)
        ) {

            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = viewModel,
                    onDateClick = { date ->
                        navController.navigate(Screen.Add.createRoute(date))
                    }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigate = { route -> navController.navigate(route) }
                )
            }

            composable(Screen.Add.routeTemplate) { backStackEntry ->
                val dateStr = backStackEntry.arguments?.getString("date") ?: LocalDate.now().toString()
                val selectedDate = LocalDate.parse(dateStr)

                EntryScreen(
                    viewModel = viewModel,
                    selectedDate = selectedDate,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.LogTypeSettings.route) {
                LogTypeSettingsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.ThemeSettings.route) {
                ThemeSettingsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.ViewSettings.route) {
                ViewSettingsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Statistics.route) {
                StatisticsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

// (*** 修复: 重新添加 AppBottomBar ***)
@Composable
fun AppBottomBar(
    navController: NavHostController,
    currentDestination: NavDestination?
) {
    NavigationBar {
        CustomNavItem(
            screen = Screen.Home,
            currentDestination = currentDestination,
            onClick = { navController.navigate(Screen.Home.route) }
        )
        NavigationBarItem(
            selected = false,
            onClick = { /* Do Nothing */ },
            icon = { /* Empty */ },
            enabled = false
        )
        CustomNavItem(
            screen = Screen.Settings,
            currentDestination = currentDestination,
            onClick = { navController.navigate(Screen.Settings.route) }
        )
    }
}

// (*** 修复: 重新添加 CustomNavItem ***)
@Composable
fun RowScope.CustomNavItem(
    screen: Screen,
    currentDestination: NavDestination?,
    onClick: () -> Unit
) {
    NavigationBarItem(
        selected = currentDestination.isRoute(screen.route),
        onClick = onClick,
        icon = { Icon(screen.icon!!, contentDescription = screen.label) },
        label = { Text(screen.label!!) }
    )
}

// (*** 修复: 重新添加 isRoute ***)
fun NavDestination?.isRoute(route: String): Boolean {
    return this?.hierarchy?.any { it.route == route } == true
}