// 文件位置: app/src/main/java/com/example/easydiary/ui/AppNavigation.kt
package com.example.easydiary.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
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
import com.example.easydiary.ui.settings.FontSettingsScreen
import com.example.easydiary.ui.settings.LogTypeSettingsScreen
import com.example.easydiary.ui.settings.SettingsScreen
import com.example.easydiary.ui.settings.ThemeSettingsScreen
import com.example.easydiary.ui.settings.ViewSettingsScreen
import com.example.easydiary.ui.statistics.StatisticsScreen
import java.time.LocalDate

sealed class Screen(val route: String, val label: String? = null, val icon: ImageVector? = null) {
    object Home : Screen("home", "主页", Icons.Default.Home)
    object Settings : Screen("settings", "我的", Icons.Default.Person)
    object Add : Screen(route = "entry") {
        const val routeTemplate = "entry/{date}"
        fun createRoute(date: LocalDate): String = "entry/${date}"
    }
    object LogTypeSettings : Screen("log_type_settings")
    object ThemeSettings : Screen("theme_settings")
    object FontSettings : Screen("font_settings")
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
                    onAddClick = {
                        navController.navigate(Screen.Add.createRoute(LocalDate.now()))
                    }
                )
            } else {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(paddingValues),
            enterTransition = { fadeIn(animationSpec = tween(300)) },
            exitTransition = { fadeOut(animationSpec = tween(300)) },
            popEnterTransition = { fadeIn(animationSpec = tween(300)) },
            popExitTransition = { fadeOut(animationSpec = tween(300)) }
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
            composable(Screen.FontSettings.route) {
                FontSettingsScreen(
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

@Composable
fun AppBottomBar(
    navController: NavHostController,
    currentDestination: NavDestination?,
    onAddClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavItem(
                modifier = Modifier.weight(1f),
                screen = Screen.Home,
                isSelected = currentDestination.isRoute(Screen.Home.route),
                onClick = { navController.navigate(Screen.Home.route) }
            )

            val fabHaptic = LocalHapticFeedback.current
            var isFabPressed by remember { mutableStateOf(false) }

            val fabScale by animateFloatAsState(
                targetValue = if (isFabPressed) 0.85f else 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessHigh
                ),
                label = "fabScale"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .pointerInput(onAddClick) {
                        detectTapGestures(
                            onPress = {
                                isFabPressed = true
                                fabHaptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                tryAwaitRelease()
                                isFabPressed = false
                            },
                            onTap = { onAddClick() }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .size(56.dp)
                        .scale(fabScale)
                        .offset(y = (-8).dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    shadowElevation = 8.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Add,
                            "添加",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            BottomNavItem(
                modifier = Modifier.weight(1f),
                screen = Screen.Settings,
                isSelected = currentDestination.isRoute(Screen.Settings.route),
                onClick = { navController.navigate(Screen.Settings.route) }
            )
        }
    }
}

@Composable
fun RowScope.BottomNavItem(
    screen: Screen,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    }

    val haptic = LocalHapticFeedback.current
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "navItemScale"
    )

    Column(
        modifier = modifier
            .scale(scale)
            .fillMaxHeight()
            .pointerInput(onClick) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = { onClick() }
                )
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = screen.icon!!,
            contentDescription = screen.label,
            tint = color,
            modifier = Modifier.size(26.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = screen.label!!,
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

fun NavDestination?.isRoute(route: String): Boolean {
    return this?.hierarchy?.any { it.route == route } == true
}