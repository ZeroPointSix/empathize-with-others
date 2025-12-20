package com.empathy.ai.presentation.ui.screen.contact.summary

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * 缺失总结提示卡片
 *
 * 在时光轴中显示，提示用户某时间段有对话但无总结
 *
 * 显示条件：
 * - 时间段内有对话记录（≥1条）
 * - 该时间段无已有总结
 * - 时间段跨度 ≥ 3天
 *
 * 样式：
 * - 背景色：Surface variant
 * - 边框：1dp虚线，Primary色
 * - 圆角：12dp
 * - 内边距：16dp
 *
 * @param startDate 开始日期
 * @param endDate 结束日期
 * @param conversationCount 对话数量
 * @param onGenerateClick 生成总结点击回调
 * @param modifier 修饰符
 */
@Composable
fun MissingSummaryCard(
    startDate: String,
    endDate: String,
    conversationCount: Int,
    onGenerateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 标题行
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = formatDateRange(startDate, endDate),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 描述
            Text(
                text = "这段时间有 $conversationCount 条对话，但还没有总结",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // 生成按钮
            FilledTonalButton(
                onClick = onGenerateClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "生成这段时间的总结",
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

/**
 * 格式化日期范围显示
 */
private fun formatDateRange(startDate: String, endDate: String): String {
    // 简化日期格式：2024-12-10 -> 12月10日
    val startFormatted = formatSingleDate(startDate)
    val endFormatted = formatSingleDate(endDate)
    return "$startFormatted - $endFormatted"
}

/**
 * 格式化单个日期
 */
private fun formatSingleDate(date: String): String {
    return try {
        val parts = date.split("-")
        if (parts.size == 3) {
            "${parts[1].toInt()}月${parts[2].toInt()}日"
        } else {
            date
        }
    } catch (e: Exception) {
        date
    }
}
