// 文件位置: app/src/main/java/com/example/easydiary/ui/entry/EntryComponents.kt
package com.example.easydiary.ui.entry

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.easydiary.data.model.LogType
import kotlin.math.roundToInt

// U9: 心情选择 (无状态)
@Composable
fun MoodSelector(
    selectedScore: Int,
    onScoreSelect: (Int) -> Unit // (状态提升)
) {
    val emojis = listOf("😢", "😟", "😐", "😊", "🤩") // 难过、低落、普通、开心、狂喜

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
                    Text(
                        text = emoji,
                        fontSize = 32.sp,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { onScoreSelect(index) } // L12: (状态提升)
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

// U7 & L9: 明日计划 (无状态)
@Composable
fun TomorrowPlanInput(
    texts: List<String>,
    onTextsChange: (List<String>) -> Unit // (修复 KSP Bug)
) {
    Card(
        elevation = CardDefaults.cardElevation(0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("明日计划", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            // (新) U7 & L9: 重用 TextEntryList
            TextEntryList(
                texts = texts,
                onTextsChange = onTextsChange,
                placeholder = "计划..."
            )
        }
    }
}

// L15, U8: 动态日志卡片 (无状态)
@Composable
fun DynamicLogCard(
    logType: LogType,
    logData: com.example.easydiary.ui.EntryScreenState.LogData,
    onTextsChange: (List<String>) -> Unit, // (状态提升)
    onDurationChange: (Float) -> Unit // (状态提升)
) {
    var isExpanded by remember { mutableStateOf(true) }

    Card(
        elevation = CardDefaults.cardElevation(0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = logType.name,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.fillMaxWidth().clickable { isExpanded = !isExpanded }
            )

            if (isExpanded) {
                Spacer(Modifier.height(16.dp))
                if (logType.hasText) {
                    TextEntryList( // L9
                        texts = logData.texts,
                        onTextsChange = onTextsChange,
                        placeholder = "记录点什么..."
                    )
                }
                if (logType.hasDuration) {
                    DurationSlider( // L10
                        duration = logData.duration,
                        onDurationChange = onDurationChange
                    )
                }
                if (logType.hasMedia) {
                    MediaButton() // L11
                }
            }
        }
    }
}

// L9: 文本条目列表 (无状态)
@Composable
fun TextEntryList(
    texts: List<String>,
    onTextsChange: (List<String>) -> Unit, // (状态提升)
    placeholder: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // 已有的条目
        texts.forEachIndexed { index, text ->
            // 只有最后一条是输入框，前面的都是可编辑的文本
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

        // 最后一个条目，作为“当前”输入框
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
                // (新) L9: 回车 (Done) 保存
                val currentText = texts.last()
                if (currentText.isNotBlank()) {
                    onTextsChange(texts + "") // 添加一个 "" 作为新的输入框
                }
            }),
            trailingIcon = {
                IconButton(onClick = {
                    val currentText = texts.last()
                    if (currentText.isNotBlank()) {
                        onTextsChange(texts + "") // L9: "加号" 按钮
                    }
                }) {
                    Icon(Icons.Default.Add, "添加条目")
                }
            }
        )
    }
}

// L10: 时长滑动 (无状态)
@Composable
fun DurationSlider(
    duration: Float,
    onDurationChange: (Float) -> Unit // (状态提升)
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

// L11: 添加图片/视频 (暂时不变)
@Composable
fun MediaButton() {
    Button(
        onClick = { /* TODO: L11 - 访问手机相册 */ },
        modifier = Modifier.padding(top = 8.dp)
    ) {
        Icon(Icons.Default.AddAPhoto, "添加媒体", modifier = Modifier.padding(end = 8.dp))
        Text("添加图片/视频")
    }
}