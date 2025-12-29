package com.empathy.ai.presentation.ui.screen.contact.summary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.empathy.ai.domain.usecase.ManualSummaryUseCase
import com.empathy.ai.presentation.ui.component.dialog.IOSInputDialog

/**
 * 总结结果对话框 - iOS风格
 *
 * 显示总结生成成功后的统计信息：
 * - 分析的对话数量
 * - 提取的关键事件数
 * - 发现的新事实数
 * - 关系评分变化
 *
 * @param result 总结结果
 * @param onViewSummary 查看总结回调
 * @param onDismiss 关闭回调
 */
@Composable
fun SummaryResultDialog(
    result: ManualSummaryUseCase.SummaryResult,
    onViewSummary: () -> Unit,
    onDismiss: () -> Unit
) {
    IOSInputDialog(
        title = "总结生成成功",
        content = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 成功图标
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )

                // 日期范围
                Text(
                    text = "已为 ${result.summary.getDisplayDateRange()} 生成总结",
                    style = MaterialTheme.typography.bodyMedium
                )

                HorizontalDivider()

                // 统计信息
                StatisticRow(
                    icon = "📊",
                    label = "分析对话",
                    value = "${result.conversationCount} 条"
                )
                StatisticRow(
                    icon = "🎯",
                    label = "关键事件",
                    value = "${result.keyEventCount} 个"
                )
                StatisticRow(
                    icon = "💡",
                    label = "新发现事实",
                    value = "${result.factCount} 条"
                )
                StatisticRow(
                    icon = "📈",
                    label = "关系变化",
                    value = formatRelationshipChange(result.relationshipChange)
                )
            }
        },
        confirmText = "查看总结",
        dismissText = "返回",
        onConfirm = onViewSummary,
        onDismiss = onDismiss
    )
}

/**
 * 统计行
 */
@Composable
private fun StatisticRow(
    icon: String,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = icon)
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * 格式化关系变化
 */
private fun formatRelationshipChange(change: Int): String {
    return when {
        change > 0 -> "+$change"
        change < 0 -> "$change"
        else -> "无变化"
    }
}
