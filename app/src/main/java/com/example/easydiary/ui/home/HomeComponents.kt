// 文件位置: app/src/main/java/com/example/easydiary/ui/home/HomeComponents.kt
package com.example.easydiary.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
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
            .aspectRatio(1f) // 保持方形
            .clip(CircleShape)
            .background(
                when {
                    isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    isToday -> MaterialTheme.colorScheme.surfaceVariant // U4: 当日高亮
                    else -> Color.Transparent
                }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        // L5: 显示心情圆点或 Emoji
        if (entry != null) {
            Text(
                emojis[entry.moodScore],
                fontSize = 20.sp,
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            Text(
                text = date.dayOfMonth.toString(),
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else Color.Unspecified
            )
        }

        // U3: (TODO: 农历)
    }
}