package com.empathy.ai.presentation.ui.screen.contact.summary

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.empathy.ai.presentation.theme.iOSBlue
import com.empathy.ai.presentation.viewmodel.QuickDateOption

/**
 * 现代化快捷日期选项组件
 *
 * 设计规范:
 * - 未选中: 浅灰填充(#F5F5F7)，无边框，中灰文字(#666666)
 * - 选中: 品牌蓝色填充，白色文字
 *
 * @param selectedOption 当前选中的选项
 * @param onOptionSelected 选项选中回调
 * @param modifier 修饰符
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickDateOptions(
    selectedOption: QuickDateOption?,
    onOptionSelected: (QuickDateOption) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "快捷选项",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Medium
            ),
            color = Color(0xFF666666)
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickDateOption.entries.forEach { option ->
                ModernFilterChip(
                    text = option.displayName,
                    selected = selectedOption == option,
                    onClick = { onOptionSelected(option) }
                )
            }
        }
    }
}

/**
 * 现代化FilterChip组件
 *
 * 设计规范:
 * - 无边框设计
 * - 未选中: #F5F5F7背景，#666666文字
 * - 选中: 品牌蓝色背景，白色文字
 */
@Composable
private fun ModernFilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (selected) iOSBlue else Color(0xFFF5F5F7)
    val textColor = if (selected) Color.White else Color(0xFF666666)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
            ),
            color = textColor
        )
    }
}
