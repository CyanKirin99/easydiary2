// 文件位置: app/src/main/java/com/example/easydiary/ui/entry/EntryComponents.kt
// [已修改]: 1. MediaPicker 使用 handleMediaSelection 修复图片丢失Bug。
// [已修改]: 2. 添加 FullScreenImageViewer 以实现真正的"点击放大"。
// [已修改]: 3. MediaPicker 和 ViewLogCard 使用 FullScreenImageViewer。
// [已修改]: 4. 从 EntryScreen.kt 移入 ViewMood, ViewLogCard, ViewTextCard。

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

// (MoodSelector 保持不变)
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

// (TomorrowPlanInput 保持不变)
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

// --- [修改点 1: DynamicLogCard] ---
@Composable
fun DynamicLogCard(
    logType: LogType,
    logData: com.example.easydiary.ui.EntryScreenState.LogData,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onTextsChange: (List<String>) -> Unit,
    onDurationChange: (Float) -> Unit,
    // onMediaPathChange: (String?) -> Unit, // (移除)
    onMediaChangeRequest: (Uri?) -> Unit // (新增)
) {
    Card(
        elevation = CardDefaults.cardElevation(0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = logType.name,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.fillMaxWidth().clickable { onToggleExpand() }
            )

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
                        // onMediaPathChange = onMediaPathChange // (移除)
                        onMediaChangeRequest = onMediaChangeRequest // (新增)
                    )
                }
            }
        }
    }
}
// --- [修改点 1 结束] ---

// (TextEntryList 保持不变)
@Composable
fun TextEntryList(
    texts: List<String>,
    onTextsChange: (List<String>) -> Unit,
    placeholder: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                val currentText = texts.last()
                if (currentText.isNotBlank()) {
                    onTextsChange(texts + "")
                }
            }),
            trailingIcon = {
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

// (DurationSlider 保持不变)
@Composable
fun DurationSlider(
    duration: Float,
    onDurationChange: (Float) -> Unit
) {
    Column(Modifier.padding(top = 8.dp)) {
        Text("时长: ${"%.1f".format(duration)} 小时", style = MaterialTheme.typography.bodyMedium)
        Slider(
            value = duration,
            onValueChange = { onDurationChange((it * 2).roundToInt() / 2.0f) },
            valueRange = 0f..12f,
            steps = 23
        )
    }
}

// --- [修改点 2: MediaPicker] ---
@Composable
fun MediaPicker(
    mediaPath: String?,
    // onMediaPathChange: (String?) -> Unit, // (移除)
    onMediaChangeRequest: (Uri?) -> Unit // (新增)
) {
    val context = LocalContext.current

    // 1. 准备图片选择器 (保持 "image/*")
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        // onMediaPathChange(uri?.toString()) // (移除)
        onMediaChangeRequest(uri) // (新增)
    }

    // 2. 移除 isImageExpanded, heightModifier, imageScale
    var showFullScreen by remember { mutableStateOf(false) } // (新增)

    Column(Modifier.padding(top = 8.dp)) {
        if (mediaPath == null) {
            // 4. 如果没有图片，显示添加按钮 (保持 "image/*")
            Button(
                onClick = {
                    launcher.launch("image/*") // <-- 保持仅图片
                }
            ) {
                Icon(Icons.Default.AddAPhoto, "添加媒体", modifier = Modifier.padding(end = 8.dp))
                Text("添加图片")
            }
        } else {
            // 5. 如果有图片，显示预览和按钮
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp) // (修改) 始终固定高度
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable { showFullScreen = true } // (修改) 点击打开弹窗
            ) {
                AsyncImage(
                    model = mediaPath,
                    contentDescription = "选择的图片",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop // (修改) 始终裁剪
                )

                // 6. 将两个按钮放在右上角
                Row(modifier = Modifier.align(Alignment.TopEnd)) {
                    // (移除 AspectRatio 按钮)

                    // 移除按钮
                    IconButton(
                        // onClick = { onMediaPathChange(null) }, // (移除)
                        onClick = { onMediaChangeRequest(null) }, // (新增)
                        modifier = Modifier
                            .padding(8.dp) // (修改) 调整内边距
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Default.Close, "移除图片", tint = Color.White)
                    }
                }
            }

            // (新增) 弹窗
            if (showFullScreen && mediaPath != null) {
                FullScreenImageViewer(
                    path = mediaPath,
                    onDismiss = { showFullScreen = false }
                )
            }
        }
    }
}
// --- [修改点 2 结束] ---


// --- [新增: 从 EntryScreen.kt 移入的组件] ---

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

// --- [修改点 3: ViewLogCard] ---
@Composable
fun ViewLogCard(
    logItem: LogItemWithTexts,
    logType: LogType?
) {
    val title = logType?.name ?: "记录 (ID: ${logItem.logItem.logTypeId})"
    val texts = logItem.texts.map { it.content }
    val duration = logItem.logItem.duration
    val mediaPath = logItem.logItem.mediaPath

    ViewTextCard(title = title, texts = texts)

    if (mediaPath != null) {
        // 1. 添加状态
        var showFullScreen by remember { mutableStateOf(false) }

        // 2. 使用 Box 添加按钮
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, start = 16.dp, end = 16.dp)
                .height(200.dp) // (修改) 始终固定高度
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surface)
                .clickable { showFullScreen = true } // (修改) 点击打开弹窗
        ) {
            AsyncImage(
                model = mediaPath,
                contentDescription = "保存的图片",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop // (修改) 始终裁剪
            )
            // (移除 AspectRatio 按钮)
        }

        // 3. (新增) 弹窗
        if (showFullScreen) {
            FullScreenImageViewer(
                path = mediaPath,
                onDismiss = { showFullScreen = false }
            )
        }
    }

    if (duration != null && duration > 0f) {
        Text(
            "时长: ${duration}h",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, top = 8.dp)
        )
    }
}
// --- [修改点 3 结束] ---

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
                    Text("• $it", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

// --- [新增: 全屏查看器] ---
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
        // 使用 Surface 覆盖整个屏幕
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .clickable { onDismiss() }, // 点击任意位置关闭
            color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.8f) // 半透明遮罩
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
                    contentScale = ContentScale.Fit // [核心] 保持原始宽高比
                )
            }
        }
    }
}
// --- [新增 结束] ---