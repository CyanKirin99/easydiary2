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
import com.example.easydiary.data.AppTheme
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

// V2 Application - 持有 V2 的数据库和 Repository
class EasyDiaryApplication : Application() {
    val applicationScope = CoroutineScope(SupervisorJob())

    val database by lazy { DiaryDatabase.getDatabase(this, applicationScope) }
    // (*** 1. 修正: 传入 database 实例 ***)
    val repository by lazy { DiaryRepository(database.diaryDao(), database) }

    val settingsRepository by lazy { SettingsRepository(this) }

    val viewModelFactory by lazy { DiaryViewModelFactory(repository, settingsRepository) }
}


class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val viewModel: DiaryViewModel = viewModel(
                factory = (application as EasyDiaryApplication).viewModelFactory
            )

            val appTheme by viewModel.appTheme.collectAsState(initial = AppTheme.SYSTEM)
            val useDarkTheme = when (appTheme) {
                AppTheme.SYSTEM -> isSystemInDarkTheme()
                AppTheme.LIGHT -> false
                AppTheme.DARK -> true
            }

            EasyDiaryTheme(darkTheme = useDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(viewModel = viewModel, onFinish = { finish() })
                }
            }
        }
    }
}