package com.empathy.ai.presentation.ui.component.dialog

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

/**
 * 权限请求对话框
 * 
 * 用于向用户解释悬浮窗权限的用途并引导用户授权
 * 
 * @param onDismiss 取消回调
 * @param onConfirm 确认回调（跳转到设置页面）
 */
@Composable
fun PermissionRequestDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "需要悬浮窗权限",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Text(
                text = """
                    悬浮窗权限用于在聊天应用上显示快捷按钮，方便您快速访问 AI 助手功能。
                    
                    我们承诺：
                    • 不会读取您的聊天内容
                    • 不会收集您的个人信息
                    • 所有数据仅在本地处理
                """.trimIndent(),
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("去设置")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

/**
 * 预览：权限请求对话框
 */
@Preview(showBackground = true)
@Composable
private fun PermissionRequestDialogPreview() {
    MaterialTheme {
        PermissionRequestDialog(
            onDismiss = {},
            onConfirm = {}
        )
    }
}
