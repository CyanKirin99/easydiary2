// 文件位置: app/src/main/java/com/example/easydiary/MainActivity.kt
package com.example.easydiary

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme // (*** 新增 L19 ***)
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.easydiary.data.AppTheme // (*** 新增 L19 ***)
import com.example.easydiary.data.DiaryDatabase
import com.example.easydiary.data.DiaryRepository
import com.example.easydiary.data.SettingsRepository // (*** 新增 L14/L19 ***)
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
    val repository by lazy { DiaryRepository(database.diaryDao()) }

    // (*** 新增 L14/L19 ***)
    val settingsRepository by lazy { SettingsRepository(this) }

    // (*** 修改: 传入两个 Repository ***)
    val viewModelFactory by lazy { DiaryViewModelFactory(repository, settingsRepository) }
}


class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            // 获取 V2 ViewModel 实例
            val viewModel: DiaryViewModel = viewModel(
                factory = (application as EasyDiaryApplication).viewModelFactory
            )

            // (*** 新增 L19: 订阅主题设置 ***)
            val appTheme by viewModel.appTheme.collectAsState(initial = AppTheme.SYSTEM)
            val useDarkTheme = when (appTheme) {
                AppTheme.SYSTEM -> isSystemInDarkTheme()
                AppTheme.LIGHT -> false
                AppTheme.DARK -> true
            }

            // (*** 修改: 传入 L19 的 darkTheme ***)
            EasyDiaryTheme(darkTheme = useDarkTheme) {

                // ... (BackHandler, AlertDialog) ...
                var showExitDialog by remember { mutableStateOf(false) }
                BackHandler(enabled = true) {
                    // TODO: 检查是否在 Home 屏幕
                    showExitDialog = true
                }
                if (showExitDialog) {
                    AlertDialog(
                        onDismissRequest = { showExitDialog = false },
                        title = { Text("确认退出") },
                        text = { Text("您确定要退出 Easy Diary 吗？") },
                        confirmButton = {
                            TextButton(onClick = { finish() }) { Text("退出") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showExitDialog = false }) { Text("取消") }
                        }
                    )
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(viewModel = viewModel)
                }
            }
        }
    }
}