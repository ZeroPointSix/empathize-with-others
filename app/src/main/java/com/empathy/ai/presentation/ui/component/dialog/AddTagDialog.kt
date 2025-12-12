package com.empathy.ai.presentation.ui.component.dialog

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.empathy.ai.domain.model.TagType
import com.empathy.ai.presentation.theme.EmpathyTheme

/**
 * 添加标签对话框
 *
 * 功能：
 * - 输入标签内容（必填）
 * - 选择标签类型（雷区/策略）
 * - 实时验证和错误显示
 *
 * @param tagContent 标签内容
 * @param selectedType 选中的标签类型
 * @param contentError 内容验证错误信息
 * @param onContentChange 内容变化回调
 * @param onTypeChange 类型变化回调
 * @param onDismiss 关闭对话框回调
 * @param onConfirm 确认添加回调
 */
@Composable
fun AddTagDialog(
    tagContent: String,
    selectedType: TagType,
    contentError: String? = null,
    onContentChange: (String) -> Unit,
    onTypeChange: (TagType) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "添加标签")
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 标签内容输入框（必填）
                OutlinedTextField(
                    value = tagContent,
                    onValueChange = onContentChange,
                    label = { Text("标签内容 *") },
                    placeholder = { Text("例如：不要提他前妻") },
                    isError = contentError != null,
                    supportingText = contentError?.let { { Text(it) } },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            onConfirm()
                        }
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // 标签类型选择
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectableGroup()
                ) {
                    Text(
                        text = "标签类型",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))

                    // 雷区选项
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selectedType == TagType.RISK_RED,
                                onClick = { onTypeChange(TagType.RISK_RED) },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedType == TagType.RISK_RED,
                            onClick = null // 由 Row 的 selectable 处理
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "雷区",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "绝对不能踩的点",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // 策略选项
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selectedType == TagType.STRATEGY_GREEN,
                                onClick = { onTypeChange(TagType.STRATEGY_GREEN) },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedType == TagType.STRATEGY_GREEN,
                            onClick = null // 由 Row 的 selectable 处理
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "策略",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "建议切入的点",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm
            ) {
                Text("添加")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
        modifier = modifier
    )
}

// ==================== Previews ====================

@Preview(name = "添加标签对话框 - 默认状态", showBackground = true)
@Composable
private fun AddTagDialogPreview() {
    EmpathyTheme {
        AddTagDialog(
            tagContent = "",
            selectedType = TagType.STRATEGY_GREEN,
            onContentChange = {},
            onTypeChange = {},
            onDismiss = {},
            onConfirm = {}
        )
    }
}

@Preview(name = "添加标签对话框 - 有内容", showBackground = true)
@Composable
private fun AddTagDialogWithContentPreview() {
    EmpathyTheme {
        AddTagDialog(
            tagContent = "不要提他前妻",
            selectedType = TagType.RISK_RED,
            onContentChange = {},
            onTypeChange = {},
            onDismiss = {},
            onConfirm = {}
        )
    }
}

@Preview(name = "添加标签对话框 - 有错误", showBackground = true)
@Composable
private fun AddTagDialogWithErrorPreview() {
    EmpathyTheme {
        AddTagDialog(
            tagContent = "",
            selectedType = TagType.STRATEGY_GREEN,
            contentError = "标签内容不能为空",
            onContentChange = {},
            onTypeChange = {},
            onDismiss = {},
            onConfirm = {}
        )
    }
}
