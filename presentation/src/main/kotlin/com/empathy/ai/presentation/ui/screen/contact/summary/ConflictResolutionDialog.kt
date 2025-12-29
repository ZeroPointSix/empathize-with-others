package com.empathy.ai.presentation.ui.screen.contact.summary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.empathy.ai.domain.model.ConflictResolution
import com.empathy.ai.domain.model.ConflictResult
import com.empathy.ai.domain.model.DailySummary
import com.empathy.ai.domain.model.GenerationSource
import com.empathy.ai.presentation.ui.component.dialog.IOSInputDialog

/**
 * 冲突处理对话框 - iOS风格
 *
 * 当选定日期范围内存在已有总结时显示，提供三个处理选项：
 * - 覆盖现有总结
 * - 仅补充缺失日期
 * - 取消操作
 *
 * @param conflict 冲突检测结果
 * @param selectedResolution 当前选中的处理方式
 * @param onResolutionSelected 处理方式选中回调
 * @param onConfirm 确认回调
 * @param onDismiss 关闭回调
 */
@Composable
fun ConflictResolutionDialog(
    conflict: ConflictResult.HasConflict,
    selectedResolution: ConflictResolution?,
    onResolutionSelected: (ConflictResolution) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    IOSInputDialog(
        title = "检测到已有总结",
        content = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 冲突信息
                Text(
                    text = "选定范围内已存在以下总结：",
                    style = MaterialTheme.typography.bodyMedium
                )

                // 已有总结列表
                ConflictSummaryList(summaries = conflict.existingSummaries)

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // 处理选项
                Text(
                    text = "请选择处理方式：",
                    style = MaterialTheme.typography.bodyMedium
                )

                ResolutionOptions(
                    selectedResolution = selectedResolution,
                    onResolutionSelected = onResolutionSelected
                )
            }
        },
        confirmText = "确认",
        dismissText = "取消",
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        confirmEnabled = selectedResolution != null
    )
}

/**
 * 冲突总结列表
 */
@Composable
private fun ConflictSummaryList(summaries: List<DailySummary>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        summaries.take(5).forEach { summary ->
            val sourceText = when (summary.generationSource) {
                GenerationSource.AUTO -> "自动生成"
                GenerationSource.MANUAL -> "手动生成"
            }
            Text(
                text = "• ${summary.summaryDate} ($sourceText)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (summaries.size > 5) {
            Text(
                text = "... 等${summaries.size}条总结",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 处理选项
 */
@Composable
private fun ResolutionOptions(
    selectedResolution: ConflictResolution?,
    onResolutionSelected: (ConflictResolution) -> Unit
) {
    Column(
        modifier = Modifier.selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ResolutionOption(
            resolution = ConflictResolution.OVERWRITE,
            description = "删除旧总结，重新生成",
            isSelected = selectedResolution == ConflictResolution.OVERWRITE,
            onSelected = { onResolutionSelected(ConflictResolution.OVERWRITE) }
        )

        ResolutionOption(
            resolution = ConflictResolution.FILL_GAPS,
            description = "只生成无总结的日期",
            isSelected = selectedResolution == ConflictResolution.FILL_GAPS,
            onSelected = { onResolutionSelected(ConflictResolution.FILL_GAPS) }
        )

        ResolutionOption(
            resolution = ConflictResolution.CANCEL,
            description = "返回重新选择日期",
            isSelected = selectedResolution == ConflictResolution.CANCEL,
            onSelected = { onResolutionSelected(ConflictResolution.CANCEL) }
        )
    }
}

/**
 * 单个处理选项
 */
@Composable
private fun ResolutionOption(
    resolution: ConflictResolution,
    description: String,
    isSelected: Boolean,
    onSelected: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = isSelected,
                onClick = onSelected,
                role = Role.RadioButton
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = null
        )
        Column(
            modifier = Modifier.padding(start = 8.dp)
        ) {
            Text(
                text = resolution.displayName,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
