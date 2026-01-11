package com.empathy.ai.presentation.ui.component.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.empathy.ai.domain.model.BrainTag
import com.empathy.ai.domain.model.TagType

/**
 * 编辑标签对话框
 *
 * ## 功能说明 (BUG-00066)
 * iOS 风格的编辑对话框，支持：
 * - 修改标签内容
 * - 切换标签类型（雷区/策略）
 *
 * ## 设计决策
 * - 使用 AlertDialog 保持与系统风格一致
 * - 类型选择使用 FilterChip，直观展示当前选中状态
 * - 内容验证：不允许保存空内容
 *
 * @param tag 要编辑的标签
 * @param onConfirm 确认回调，参数为 (tagId, newContent, newType)
 * @param onDismiss 取消回调
 */
@Composable
fun EditBrainTagDialog(
    tag: BrainTag,
    onConfirm: (Long, String, TagType) -> Unit,
    onDismiss: () -> Unit
) {
    // 编辑状态
    var content by remember { mutableStateOf(tag.content) }
    var selectedType by remember { mutableStateOf(tag.type) }
    var contentError by remember { mutableStateOf<String?>(null) }

    // iOS 风格颜色
    val iOSBlue = Color(0xFF007AFF)
    val iOSRed = Color(0xFFFF3B30)
    val iOSGreen = Color(0xFF34C759)
    val iOSTextSecondary = Color(0xFF8E8E93)

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(14.dp),
        title = {
            Text(
                text = "编辑标签",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 标签内容输入
                OutlinedTextField(
                    value = content,
                    onValueChange = {
                        content = it
                        contentError = if (it.isBlank()) "内容不能为空" else null
                    },
                    label = { Text("标签内容") },
                    isError = contentError != null,
                    supportingText = contentError?.let { error ->
                        { Text(error, color = MaterialTheme.colorScheme.error) }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // 标签类型选择
                Text(
                    text = "标签类型",
                    style = MaterialTheme.typography.bodyMedium,
                    color = iOSTextSecondary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 雷区标签
                    FilterChip(
                        selected = selectedType == TagType.RISK_RED,
                        onClick = { selectedType = TagType.RISK_RED },
                        label = { Text("🚫 雷区") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = iOSRed.copy(alpha = 0.1f),
                            selectedLabelColor = iOSRed
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    // 策略标签
                    FilterChip(
                        selected = selectedType == TagType.STRATEGY_GREEN,
                        onClick = { selectedType = TagType.STRATEGY_GREEN },
                        label = { Text("✅ 策略") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = iOSGreen.copy(alpha = 0.1f),
                            selectedLabelColor = iOSGreen
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                // 标签来源提示（只读）
                if (tag.isAiInferred) {
                    Text(
                        text = "💡 此标签由 AI 推断，编辑后来源保持不变",
                        style = MaterialTheme.typography.bodySmall,
                        color = iOSTextSecondary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (content.isNotBlank()) {
                        onConfirm(tag.id, content.trim(), selectedType)
                    } else {
                        contentError = "内容不能为空"
                    }
                }
            ) {
                Text("保存", color = iOSBlue)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = iOSTextSecondary)
            }
        }
    )
}
