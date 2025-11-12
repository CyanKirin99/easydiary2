// 文件位置: app/src/main/java/com/example/easydiary/ui/entry/EntryComponents.kt
package com.example.easydiary.ui.entry

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.easydiary.data.model.LogType
import kotlin.math.roundToInt
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.easydiary.data.model.LogItemWithTexts

/**
 * 心情选择器 (编辑模式)。
 */
@Composable
fun MoodSelector(
    selectedScore: Int,
    onScoreSelect: (Int) -> Unit
) {
    val emojis = listOf("😢", "😟", "😐", "😊", "🤩")

    Card(
        elevation = CardDefaults.cardElevation(0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("现在心情", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                emojis.forEachIndexed { index, emoji ->
                    val scale by animateFloatAsState(
                        targetValue = if (selectedScore == index) 1.25f else 1.0f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "MoodEmojiScale"
                    )

                    Text(
                        text = emoji,
                        fontSize = 32.sp,
                        modifier = Modifier
                            .scale(scale)
                            .clip(CircleShape)
                            .clickable { onScoreSelect(index) }
                            .background(
                                if (selectedScore == index) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                else Color.Transparent
                            )
                            .padding(8.dp)
                    )
                }
            }
        }
    }
}

/**
 * 明日计划输入框 (编辑模式)。
 */
@Composable
fun TomorrowPlanInput(
    texts: List<String>,
    onTextsChange: (List<String>) -> Unit
) {
    Card(
        elevation = CardDefaults.cardElevation(0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("明日计划", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            TextEntryList(
                texts = texts,
                onTextsChange = onTextsChange,
                placeholder = "计划..."
            )
        }
    }
}

/**
 * 动态日志卡片 (编辑模式)。
 * 根据 [LogType] 的定义 (hasText, hasDuration, hasMedia) 显示不同的输入控件。
 */
@Composable
fun DynamicLogCard(
    logType: LogType,
    logData: com.example.easydiary.ui.EntryScreenState.LogData,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onTextsChange: (List<String>) -> Unit,
    onDurationChange: (Float) -> Unit,
    onMediaChangeRequest: (Uri?) -> Unit // 当用户选择或删除媒体时回调
) {
    Card(
        elevation = CardDefaults.cardElevation(0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(16.dp)) {
            // 标题（始终可见，点击可展开/折叠）
            Text(
                text = logType.name,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.fillMaxWidth().clickable { onToggleExpand() }
            )

            // 可折叠内容
            if (isExpanded) {
                Spacer(Modifier.height(16.dp))
                if (logType.hasText) {
                    TextEntryList(
                        texts = logData.texts,
                        onTextsChange = onTextsChange,
                        placeholder = "记录点什么..."
                    )
                }
                if (logType.hasDuration) {
                    DurationSlider(
                        duration = logData.duration,
                        onDurationChange = onDurationChange
                    )
                }
                if (logType.hasMedia) {
                    MediaPicker(
                        mediaPath = logData.mediaPath,
                        onMediaChangeRequest = onMediaChangeRequest
                    )
                }
            }
        }
    }
}

/**
 * 可动态添加的文本输入框列表 (编辑模式)。
 * 用于 [DynamicLogCard] 和 [TomorrowPlanInput]。
 */
@Composable
fun TextEntryList(
    texts: List<String>,
    onTextsChange: (List<String>) -> Unit,
    placeholder: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // 显示已有的文本行 (除了最后一行)
        texts.forEachIndexed { index, text ->
            if (index < texts.lastIndex) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { newText ->
                        val newList = texts.toMutableList()
                        newList[index] = newText
                        onTextsChange(newList)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )
            }
        }

        // 最后一个输入框 (用于输入和添加新行)
        OutlinedTextField(
            value = texts.last(),
            onValueChange = { newText ->
                val newList = texts.toMutableList()
                newList[texts.lastIndex] = newText
                onTextsChange(newList)
            },
            placeholder = { Text(placeholder) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                // 按下 "完成" 且当前行不为空时，自动添加新行
                val currentText = texts.last()
                if (currentText.isNotBlank()) {
                    onTextsChange(texts + "")
                }
            }),
            trailingIcon = {
                // 点击 "+" 按钮添加新行
                IconButton(onClick = {
                    val currentText = texts.last()
                    if (currentText.isNotBlank()) {
                        onTextsChange(texts + "")
                    }
                }) {
                    Icon(Icons.Default.Add, "添加条目")
                }
            }
        )
    }
}

/**
 * 时长选择滑块 (编辑模式)。
 */
@Composable
fun DurationSlider(
    duration: Float,
    onDurationChange: (Float) -> Unit
) {
    Column(Modifier.padding(top = 8.dp)) {
        Text("时长: ${"%.1f".format(duration)} 小时", style = MaterialTheme.typography.bodyMedium)
        Slider(
            value = duration,
            onValueChange = { onDurationChange((it * 2).roundToInt() / 2.0f) }, // 步长 0.5
            valueRange = 0f..12f,
            steps = 23
        )
    }
}

/**
 * 媒体（图片）选择器 (编辑模式)。
 */
@Composable
fun MediaPicker(
    mediaPath: String?,
    onMediaChangeRequest: (Uri?) -> Unit
) {
    val context = LocalContext.current

    // 1. 准备图片选择器 (仅限图片)
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        onMediaChangeRequest(uri) // 将选择的 Uri 回调给 ViewModel
    }

    var showFullScreen by remember { mutableStateOf(false) }

    Column(Modifier.padding(top = 8.dp)) {
        if (mediaPath == null) {
            // 2. 如果没有图片，显示添加按钮
            Button(
                onClick = {
                    launcher.launch("image/*")
                }
            ) {
                Icon(Icons.Default.AddAPhoto, "添加媒体", modifier = Modifier.padding(end = 8.dp))
                Text("添加图片")
            }
        } else {
            // 3. 如果有图片，显示预览图
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable { showFullScreen = true } // 点击打开全屏
            ) {
                AsyncImage(
                    model = mediaPath,
                    contentDescription = "选择的图片",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // 移除按钮 (右上角)
                Row(modifier = Modifier.align(Alignment.TopEnd)) {
                    IconButton(
                        onClick = { onMediaChangeRequest(null) }, // 传递 null Uri 表示删除
                        modifier = Modifier
                            .padding(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Default.Close, "移除图片", tint = Color.White)
                    }
                }
            }

            // 4. 全屏查看器
            if (showFullScreen && mediaPath != null) {
                FullScreenImageViewer(
                    path = mediaPath,
                    onDismiss = { showFullScreen = false }
                )
            }
        }
    }
}


// --- 以下是只读组件 (查看模式) ---

/**
 * 显示心情 (查看模式)。
 */
@Composable
fun ViewMood(score: Int) {
    val emojis = listOf("😢", "😟", "😐", "😊", "🤩")
    val emoji = emojis.getOrNull(score) ?: "😐"

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(emoji, fontSize = 48.sp)
        Spacer(Modifier.width(16.dp))
        Text("今天的心情", style = MaterialTheme.typography.titleLarge)
    }
}

/**
 * 显示日志卡片 (查看模式)。
 * 包括标题、文本、图片和时长。
 */
@Composable
fun ViewLogCard(
    logItem: LogItemWithTexts,
    logType: LogType?
) {
    val title = logType?.name ?: "记录 (ID: ${logItem.logItem.logTypeId})"
    val texts = logItem.texts.map { it.content }
    val duration = logItem.logItem.duration
    val mediaPath = logItem.logItem.mediaPath

    // 1. 显示标题和文本
    ViewTextCard(title = title, texts = texts)

    // 2. 显示媒体（如果存在）
    if (mediaPath != null) {
        var showFullScreen by remember { mutableStateOf(false) }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, start = 16.dp, end = 16.dp)
                .height(200.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surface)
                .clickable { showFullScreen = true } // 点击全屏
        ) {
            AsyncImage(
                model = mediaPath,
                contentDescription = "保存的图片",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // 全屏查看器
        if (showFullScreen) {
            FullScreenImageViewer(
                path = mediaPath,
                onDismiss = { showFullScreen = false }
            )
        }
    }

    // 3. 显示时长（如果存在且大于0）
    if (duration != null && duration > 0f) {
        Text(
            "时长: ${duration}h",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, top = 8.dp)
        )
    }
}

/**
 * 显示文本卡片 (查看模式)。
 * 用于 [ViewLogCard] 和明日计划。
 */
@Composable
fun ViewTextCard(title: String, texts: List<String>) {
    if (texts.isEmpty() && title != "明日计划") return

    Card(
        elevation = CardDefaults.cardElevation(0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            if (texts.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                texts.forEach {
                    // 使用 • (圆点) 分隔多行
                    Text("• $it", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

/**
 * 全屏图片查看器 (Dialog)。
 * 点击图片或背景可关闭。
 */
@Composable
fun FullScreenImageViewer(
    path: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false, // 占用全屏宽度
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        // 半透明遮罩
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .clickable { onDismiss() }, // 点击任意位置关闭
            color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.8f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp), // 留出边距
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = path,
                    contentDescription = "全屏图片",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit // 保持原始宽高比
                )
            }
        }
    }
}