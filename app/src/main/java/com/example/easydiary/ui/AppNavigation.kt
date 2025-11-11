// 文件位置: app/src/main/java/com/example/easydiary/ui/AppNavigation.kt
package com.example.easydiary.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.example.easydiary.ui.io.DataIOScreen
import com.example.easydiary.ui.settings.LogTypeSettingsScreen
import com.example.easydiary.ui.settings.SettingsScreen
import com.example.easydiary.ui.settings.ThemeSettingsScreen
import com.example.easydiary.ui.settings.ViewSettingsScreen
import com.example.easydiary.ui.statistics.StatisticsScreen
import java.time.LocalDate

// (路由定义保持不变)
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
    object DataIO : Screen("data_io")
}

val navItems = listOf(Screen.Home, Screen.Settings)

@Composable
fun AppNavigation(
    viewModel: DiaryViewModel,
    onFinish: () -> Unit
) {
    val navController = rememberNavController()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val isMainScreen = navItems.any { it.route == currentDestination?.route }

    var showExitDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = isMainScreen && !showExitDialog) {
        showExitDialog = true
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("确认退出") },
            text = { Text("您确定要退出 Easy Diary 吗？") },
            confirmButton = {
                TextButton(onClick = onFinish) {
                    Text("退出")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) { Text("取消") }
            }
        )
    }

    Scaffold(
        bottomBar = {
            if (isMainScreen) {
                AppBottomBar(
                    navController = navController,
                    currentDestination = currentDestination,
                    // [修改点 1] 传递“添加”按钮的点击事件
                    onAddClick = {
                        navController.navigate(Screen.Add.createRoute(LocalDate.now()))
                    }
                )
            }
        }
        // [修改点 2] 移除 floatingActionButton 和 floatingActionButtonPosition
        /*
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
        */
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(paddingValues)
        ) {

            // (NavHost composable 路由保持不变)

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
                    onBack = { navController.popBackStack() },
                    onDateChange = { newDate ->
                        navController.popBackStack()
                        navController.navigate(Screen.Add.createRoute(newDate))
                    }
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
                    onBack = { navController.popBackStack() },
                    onLogClick = { date ->
                        navController.navigate(Screen.Add.createRoute(date))
                    }
                )
            }

            composable(Screen.DataIO.route) {
                DataIOScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

// [修改点 3] 重写 AppBottomBar
@Composable
fun AppBottomBar(
    navController: NavHostController,
    currentDestination: NavDestination?,
    onAddClick: () -> Unit // 接收点击事件
) {
    NavigationBar(
        // (可选) 确保导航栏有足够的高度容纳按钮
        modifier = Modifier.height(80.dp)
    ) {
        // 1. 主页 (权重 1)
        CustomNavItem(
            modifier = Modifier.weight(1f), // [修改点 4] 添加权重
            screen = Screen.Home,
            currentDestination = currentDestination,
            onClick = { navController.navigate(Screen.Home.route) }
        )

        // 2. 自定义“添加”按钮 (权重 1)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clickable(onClick = onAddClick),
            contentAlignment = Alignment.Center
        ) {
            // 模拟 FAB 的外观
            Box(
                modifier = Modifier
                    .size(56.dp) // 标准 FAB 尺寸
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Add,
                    "添加",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(36.dp) // 原始图标大小
                )
            }
        }

        // 3. 我的 (权重 1)
        CustomNavItem(
            modifier = Modifier.weight(1f), // [修改点 5] 添加权重
            screen = Screen.Settings,
            currentDestination = currentDestination,
            onClick = { navController.navigate(Screen.Settings.route) }
        )
    }
}

// [修改点 6] CustomNavItem 接受 Modifier
@Composable
fun RowScope.CustomNavItem(
    screen: Screen,
    currentDestination: NavDestination?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier // 添加 Modifier 参数
) {
    NavigationBarItem(
        modifier = modifier, // 应用 Modifier
        selected = currentDestination.isRoute(screen.route),
        onClick = onClick,
        icon = { Icon(screen.icon!!, contentDescription = screen.label) },
        label = { Text(screen.label!!) }
    )
}

fun NavDestination?.isRoute(route: String): Boolean {
    return this?.hierarchy?.any { it.route == route } == true
}