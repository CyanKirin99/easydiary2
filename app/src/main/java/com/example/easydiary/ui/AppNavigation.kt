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
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
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

/**
 * 定义应用内的所有导航路由。
 */
sealed class Screen(val route: String, val label: String? = null, val icon: ImageVector? = null) {
    object Home : Screen("home", "主页", Icons.Default.Home)
    object Settings : Screen("settings", "我的", Icons.Default.Person)
    // 日记条目路由（包含日期参数）
    object Add : Screen(route = "entry") {
        const val routeTemplate = "entry/{date}"
        fun createRoute(date: LocalDate): String = "entry/${date}"
    }
    // 设置子页面
    object LogTypeSettings : Screen("log_type_settings")
    object ThemeSettings : Screen("theme_settings")
    object ViewSettings : Screen("view_settings")
    object Statistics : Screen("statistics")
    object DataIO : Screen("data_io")
}

// 底部导航栏的条目
val navItems = listOf(Screen.Home, Screen.Settings)

/**
 * 应用的主导航组件 (NavHost) 和界面骨架 (Scaffold)。
 * 负责管理页面跳转和底部导航栏。
 *
 * @param viewModel DiaryViewModel
 * @param onFinish 当用户确认退出时调用。
 */
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

    // 处理主屏幕上的返回键，弹出退出对话框
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
                    onAddClick = {
                        // 点击中央按钮，导航到当天的日记条目
                        navController.navigate(Screen.Add.createRoute(LocalDate.now()))
                    }
                )
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(paddingValues),
            enterTransition = { fadeIn(animationSpec = tween(800)) },
            exitTransition = { fadeOut(animationSpec = tween(800)) },
            popEnterTransition = { fadeIn(animationSpec = tween(800)) },
            popExitTransition = { fadeOut(animationSpec = tween(800)) }
        ) {

            // --- 主页 ---
            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = viewModel,
                    onDateClick = { date ->
                        navController.navigate(Screen.Add.createRoute(date))
                    }
                )
            }

            // --- 设置主页 ---
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigate = { route -> navController.navigate(route) }
                )
            }

            // --- 日记条目详情/编辑页 ---
            composable(Screen.Add.routeTemplate) { backStackEntry ->
                val dateStr = backStackEntry.arguments?.getString("date") ?: LocalDate.now().toString()
                val selectedDate = LocalDate.parse(dateStr)

                EntryScreen(
                    viewModel = viewModel,
                    selectedDate = selectedDate,
                    onBack = { navController.popBackStack() },
                    onDateChange = { newDate ->
                        // 切换日期时，替换当前导航栈
                        navController.popBackStack()
                        navController.navigate(Screen.Add.createRoute(newDate))
                    }
                )
            }

            // --- 设置子页面 ---
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

/**
 * 自定义的底部导航栏，中间有一个模拟的悬浮按钮 (FAB)。
 */
@Composable
fun AppBottomBar(
    navController: NavHostController,
    currentDestination: NavDestination?,
    onAddClick: () -> Unit // 接收点击事件
) {
    NavigationBar(
        modifier = Modifier.height(80.dp) // 确保有足够的高度容纳按钮
    ) {
        // 1. 主页
        CustomNavItem(
            modifier = Modifier.weight(1f),
            screen = Screen.Home,
            currentDestination = currentDestination,
            onClick = { navController.navigate(Screen.Home.route) }
        )

        // 2. 自定义“添加”按钮 (模拟 FAB)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clickable(onClick = onAddClick),
            contentAlignment = Alignment.Center
        ) {
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
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        // 3. 我的
        CustomNavItem(
            modifier = Modifier.weight(1f),
            screen = Screen.Settings,
            currentDestination = currentDestination,
            onClick = { navController.navigate(Screen.Settings.route) }
        )
    }
}

/**
 * 自定义的 [NavigationBarItem]，用于 [AppBottomBar]。
 */
@Composable
fun RowScope.CustomNavItem(
    screen: Screen,
    currentDestination: NavDestination?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBarItem(
        modifier = modifier,
        selected = currentDestination.isRoute(screen.route),
        onClick = onClick,
        icon = { Icon(screen.icon!!, contentDescription = screen.label) },
        label = { Text(screen.label!!) }
    )
}

/**
 * 扩展函数：检查当前导航目标是否匹配指定路由。
 */
fun NavDestination?.isRoute(route: String): Boolean {
    return this?.hierarchy?.any { it.route == route } == true
}