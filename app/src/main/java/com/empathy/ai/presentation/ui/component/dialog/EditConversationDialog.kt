package com.empathy.ai.presentation.ui.component.dialog

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.empathy.ai.presentation.theme.EmpathyTheme

/**
 * 编辑对话记录对话框
 *
 * 功能：
 * - 编辑用户输入的对话内容
 * - 删除对话记录
 *
 * @param initialContent 初始内容
 * @param onDismiss 关闭对话框回调
 * @param onConfirm 确认编辑回调
 * @param onDelete 删除回调
 */
@Composable
fun EditConversationDialog(
    initialContent: String,
    onDismiss: () -> Unit,
    onConfirm: (newContent: String) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var content by remember { mutableStateOf(initialContent) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除这条对话记录吗？此操作无法撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    }
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("取消")
                }
            }
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("编辑对话") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "修改你发送给AI的内容：",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        label = { Text("对话内容") },
                        placeholder = { Text("请输入对话内容") },
                        minLines = 3,
                        maxLines = 6,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { onConfirm(content.trim()) },
                    enabled = content.isNotBlank()
                ) {
                    Text("保存")
                }
            },
            dismissButton = {
                Row {
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "删除",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                    TextButton(onClick = onDismiss) {
                        Text("取消")
                    }
                }
            },
            modifier = modifier
        )
    }
}

// ==================== Previews ====================

@Preview(name = "编辑对话对话框", showBackground = true)
@Composable
private fun EditConversationDialogPreview() {
    EmpathyTheme {
        EditConversationDialog(
            initialContent = "今天想约她出去吃饭，但不知道怎么开口比较好",
            onDismiss = {},
            onConfirm = {},
            onDelete = {}
        )
    }
}
