// 文件位置: app/src/main/java/com/example/easydiary/MainActivity.kt
package com.example.easydiary

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.easydiary.data.AppFontFamily
import com.example.easydiary.data.AppTheme
import com.example.easydiary.data.ThemePreset
import com.example.easydiary.data.DiaryDatabase
import com.example.easydiary.data.DiaryRepository
import com.example.easydiary.data.SettingsRepository
import com.example.easydiary.ui.AppNavigation
import com.example.easydiary.ui.DiaryViewModel
import com.example.easydiary.ui.DiaryViewModelFactory
import com.example.easydiary.ui.theme.EasyDiaryTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 自定义 Application 类，用于持有和惰性初始化应用级的单例，
 * 如数据库 (Database)、仓库 (Repository) 和 ViewModelFactory。
 */
class EasyDiaryApplication : Application() {
    val applicationScope = CoroutineScope(SupervisorJob())

    // 惰性初始化数据库
    val database by lazy { DiaryDatabase.getDatabase(this, applicationScope) }

    // 惰性初始化仓库，并传入 database 实例
    val repository by lazy { DiaryRepository(database.diaryDao(), database) }

    // 惰性初始化设置仓库
    val settingsRepository by lazy { SettingsRepository(this) }

    // 惰性初始化 ViewModelFactory，注入依赖
    val viewModelFactory by lazy {
        DiaryViewModelFactory(repository, settingsRepository, this)
    }
}


class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            // 从 Application 获取 ViewModelFactory 来创建 ViewModel
            val viewModel: DiaryViewModel = viewModel(
                factory = (application as EasyDiaryApplication).viewModelFactory
            )

            // 订阅主题设置
            val appTheme by viewModel.appTheme.collectAsState(initial = AppTheme.SYSTEM)
            val themePreset by viewModel.themePreset.collectAsState(initial = ThemePreset.WARM)
            val appFontFamily by viewModel.appFontFamily.collectAsState(initial = AppFontFamily.DEFAULT)
            val useDarkTheme = when (appTheme) {
                AppTheme.SYSTEM -> isSystemInDarkTheme()
                AppTheme.LIGHT -> false
                AppTheme.DARK -> true
            }

            EasyDiaryTheme(
                darkTheme = useDarkTheme,
                themePreset = themePreset,
                fontFamily = appFontFamily
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(
                        viewModel = viewModel,
                        onFinish = { finish() } // 传入退出应用的回调
                    )
                }
            }
        }
    }
}