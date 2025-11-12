// 文件位置: app/src/main/java/com/example/easydiary/ui/settings/SettingsComponents.kt
package com.example.easydiary.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 可重用的设置单选项 (用于视图选择和主题选择)。
 *
 * @param label 选项显示的标签。
 * @param isSelected 选项是否被选中。
 * @param onClick 点击选项时的回调。
 */
@Composable
fun SettingsRadioItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick
        )
        Text(
            text = label,
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}