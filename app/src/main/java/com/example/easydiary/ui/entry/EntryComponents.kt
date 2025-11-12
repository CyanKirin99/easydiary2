// 文件位置: app/src/main/java/com/example/easydiary/ui/entry/EntryComponents.kt
// [已修改]: 1. MediaPicker 启动器保持 "image/*"。 2. MediaPicker 支持图片展开/收起。
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

// --- [新增导入] ---
import androidx.compose.material.icons.filled.AspectRatio
// --- [新增导入 结束] ---

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

// (DynamicLogCard 保持不变)
@Composable
fun DynamicLogCard(
    logType: LogType,
    logData: com.example.easydiary.ui.EntryScreenState.LogData,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onTextsChange: (List<String>) -> Unit,
    onDurationChange: (Float) -> Unit,
    onMediaPathChange: (String?) -> Unit
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
                        onMediaPathChange = onMediaPathChange
                    )
                }
            }
        }
    }
}

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

// --- [修改点: MediaPicker] ---
@Composable
fun MediaPicker(
    mediaPath: String?,
    onMediaPathChange: (String?) -> Unit
) {
    val context = LocalContext.current

    // 1. 准备图片选择器 (保持 "image/*")
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        onMediaPathChange(uri?.toString())
    }

    // 2. 添加状态
    var isImageExpanded by remember { mutableStateOf(false) }

    // 3. 根据状态决定高度和缩放
    val (heightModifier, imageScale) = if (isImageExpanded) {
        Pair(Modifier.heightIn(max = 600.dp), ContentScale.FillWidth)
    } else {
        Pair(Modifier.height(180.dp), ContentScale.Crop)
    }

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
                    .then(heightModifier) // 应用动态高度
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                AsyncImage(
                    model = mediaPath,
                    contentDescription = "选择的图片",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = imageScale // 应用动态缩放
                )

                // 6. 将两个按钮放在右上角
                Row(modifier = Modifier.align(Alignment.TopEnd)) {
                    // 切换按钮
                    IconButton(
                        onClick = { isImageExpanded = !isImageExpanded },
                        modifier = Modifier
                            .padding(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Default.AspectRatio, "Toggle view", tint = Color.White)
                    }

                    // 移除按钮
                    IconButton(
                        onClick = { onMediaPathChange(null) },
                        modifier = Modifier
                            .padding(top = 8.dp, bottom = 8.dp, end = 8.dp) // 调整内边距
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Default.Close, "移除图片", tint = Color.White)
                    }
                }
            }
        }
    }
}