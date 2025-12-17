package com.empathy.ai.presentation.ui.component.dialog

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.empathy.ai.domain.util.IdentityPrefixHelper
import com.empathy.ai.presentation.theme.EmpathyTheme

/**
 * 编辑对话记录对话框
 *
 * 功能：
 * - 编辑用户输入的对话内容
 * - 删除对话记录
 *
 * 【PRD-00008】身份前缀处理：
 * - 加载时：解析前缀，只显示纯文本，同时记住原始身份
 * - 保存时：根据记住的身份，重新拼接前缀
 * - 用户无感知前缀存在
 *
 * @param initialContent 初始内容（可能带身份前缀）
 * @param onDismiss 关闭对话框回调
 * @param onConfirm 确认编辑回调（返回带前缀的完整内容）
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
    // 【PRD-00008】解析身份前缀，记住原始身份
    val parseResult = remember(initialContent) {
        IdentityPrefixHelper.parse(initialContent)
    }
    
    // 编辑框只显示纯文本内容（不含前缀）
    var content by remember { mutableStateOf(parseResult.content) }
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
            // 【PRD-00008】标题显示身份标签，让用户知道这是谁说的
            title = { 
                Text("编辑对话 (${parseResult.role.displayName})") 
            },
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
                    onClick = { 
                        // 【PRD-00008】保存时重新拼接前缀，保留原始身份
                        val finalContent = IdentityPrefixHelper.rebuildWithPrefix(
                            role = parseResult.role,
                            newContent = content.trim()
                        )
                        onConfirm(finalContent) 
                    },
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

@Preview(name = "编辑对话对话框 - 对方说", showBackground = true)
@Composable
private fun EditConversationDialogContactPreview() {
    EmpathyTheme {
        EditConversationDialog(
            initialContent = "${IdentityPrefixHelper.PREFIX_CONTACT}你怎么才回消息？",
            onDismiss = {},
            onConfirm = {},
            onDelete = {}
        )
    }
}

@Preview(name = "编辑对话对话框 - 我正在回复", showBackground = true)
@Composable
private fun EditConversationDialogUserPreview() {
    EmpathyTheme {
        EditConversationDialog(
            initialContent = "${IdentityPrefixHelper.PREFIX_USER}刚才在开会",
            onDismiss = {},
            onConfirm = {},
            onDelete = {}
        )
    }
}

@Preview(name = "编辑对话对话框 - 旧数据", showBackground = true)
@Composable
private fun EditConversationDialogLegacyPreview() {
    EmpathyTheme {
        EditConversationDialog(
            initialContent = "今天想约她出去吃饭，但不知道怎么开口比较好",
            onDismiss = {},
            onConfirm = {},
            onDelete = {}
        )
    }
}
