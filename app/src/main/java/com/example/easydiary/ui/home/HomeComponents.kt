// 文件位置: app/src/main/java/com/example/easydiary/ui/home/HomeComponents.kt
package com.example.easydiary.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape // (*** 1. 新增导入 ***)
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.easydiary.data.model.DiaryEntry
import java.time.LocalDate

/**
 * 日历单元格 (改编自 V1)
 */
@Composable
fun DayCell(
    date: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    entry: DiaryEntry?,
    onClick: () -> Unit
) {
    val emojis = listOf("😢", "😟", "😐", "😊", "🤩")

    Box(
        modifier = Modifier
            .padding(4.dp)
            .aspectRatio(0.75f) // [修改点 1] 调高格子 (W/H ratio, < 1.0 即为高大于宽)
            .clip(RoundedCornerShape(12.dp)) // [修改点 2] 使用圆角矩形
            .background(
                when {
                    isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    isToday -> MaterialTheme.colorScheme.surfaceVariant // U4: 当日高亮
                    else -> Color.Transparent
                }
            )
            .clickable(onClick = onClick),
        // [修改点 3] 移除 contentAlignment, 下方使用 Column
    ) {
        // [修改点 4] 使用 Column 垂直排列
        Column(
            modifier = Modifier.fillMaxSize().padding(vertical = 4.dp), // 增加一点垂直内边距
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center // 垂直居中
        ) {
            // 1. 始终显示日期
            Text(
                text = date.dayOfMonth.toString(),
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else Color.Unspecified,
                fontSize = 14.sp // 确保日期清晰
            )

            // 2. 如果有心情，显示在日期下方
            if (entry != null) {
                Spacer(Modifier.height(4.dp)) // 日期和表情的间距
                Text(
                    emojis[entry.moodScore],
                    fontSize = 18.sp, // 调整表情大小
                )
            }
        }
    }
}