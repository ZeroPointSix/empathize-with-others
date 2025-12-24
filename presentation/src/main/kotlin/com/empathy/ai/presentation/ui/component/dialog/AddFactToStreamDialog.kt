package com.empathy.ai.presentation.ui.component.dialog

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.empathy.ai.presentation.theme.EmpathyTheme

@OptIn(ExperimentalMaterial3Api::class)

/**
 * 添加事实到事实流对话框
 *
 * 功能：
 * - 输入事实类型（key）
 * - 输入事实内容（value）
 * - 表单验证
 *
 * @param onDismiss 关闭对话框回调
 * @param onConfirm 确认添加回调，参数为 (key, value)
 */
@Composable
fun AddFactToStreamDialog(
    onDismiss: () -> Unit,
    onConfirm: (key: String, value: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var factKey by remember { mutableStateOf("") }
    var factValue by remember { mutableStateOf("") }
    var keyError by remember { mutableStateOf<String?>(null) }
    var valueError by remember { mutableStateOf<String?>(null) }
    
    // 预设的事实类型
    val presetKeys = listOf(
        "性格特点",
        "兴趣爱好",
        "工作信息",
        "家庭情况",
        "重要日期",
        "禁忌话题",
        "沟通策略",
        "其他"
    )
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加事实") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "记录关于这位联系人的重要信息",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // 事实类型选择
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = factKey,
                        onValueChange = { 
                            factKey = it
                            keyError = null
                        },
                        label = { Text("事实类型 *") },
                        placeholder = { Text("选择或输入类型") },
                        isError = keyError != null,
                        supportingText = keyError?.let { { Text(it) } },
                        singleLine = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        presetKeys.forEach { key ->
                            DropdownMenuItem(
                                text = { Text(key) },
                                onClick = {
                                    factKey = key
                                    expanded = false
                                    keyError = null
                                }
                            )
                        }
                    }
                }
                
                // 事实内容输入
                OutlinedTextField(
                    value = factValue,
                    onValueChange = { 
                        factValue = it
                        valueError = null
                    },
                    label = { Text("事实内容 *") },
                    placeholder = { Text("例如：喜欢吃辣、周末常去健身房") },
                    isError = valueError != null,
                    supportingText = valueError?.let { { Text(it) } },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    // 验证
                    var hasError = false
                    if (factKey.isBlank()) {
                        keyError = "请选择或输入事实类型"
                        hasError = true
                    }
                    if (factValue.isBlank()) {
                        valueError = "请输入事实内容"
                        hasError = true
                    }
                    
                    if (!hasError) {
                        onConfirm(factKey.trim(), factValue.trim())
                    }
                }
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

@Preview(name = "添加事实对话框", showBackground = true)
@Composable
private fun AddFactToStreamDialogPreview() {
    EmpathyTheme {
        AddFactToStreamDialog(
            onDismiss = {},
            onConfirm = { _, _ -> }
        )
    }
}
