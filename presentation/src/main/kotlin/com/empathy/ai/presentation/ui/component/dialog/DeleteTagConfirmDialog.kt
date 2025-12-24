package com.empathy.ai.presentation.ui.component.dialog

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.empathy.ai.presentation.theme.EmpathyTheme

/**
 * 删除标签确认对话框
 *
 * 功能：
 * - 显示要删除的标签内容
 * - 提供确认和取消按钮
 * - 警告用户删除操作不可恢复
 *
 * @param tagContent 要删除的标签内容
 * @param onConfirm 确认删除回调
 * @param onDismiss 取消删除回调
 * @param modifier 修饰符
 */
@Composable
fun DeleteTagConfirmDialog(
    tagContent: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "警告",
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text(text = "确认删除标签")
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "确定要删除以下标签吗？",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                // 显示标签内容
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = tagContent,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                
                Text(
                    text = "此操作将从所有关联的联系人中移除该标签，且无法撤销。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("删除")
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

@Preview(name = "删除确认对话框 - 雷区标签", showBackground = true)
@Composable
private fun DeleteTagConfirmDialogRiskPreview() {
    EmpathyTheme {
        DeleteTagConfirmDialog(
            tagContent = "不要提工作压力",
            onConfirm = {},
            onDismiss = {}
        )
    }
}

@Preview(name = "删除确认对话框 - 策略标签", showBackground = true)
@Composable
private fun DeleteTagConfirmDialogStrategyPreview() {
    EmpathyTheme {
        DeleteTagConfirmDialog(
            tagContent = "喜欢聊摄影技巧",
            onConfirm = {},
            onDismiss = {}
        )
    }
}

@Preview(name = "删除确认对话框 - 长文本", showBackground = true)
@Composable
private fun DeleteTagConfirmDialogLongTextPreview() {
    EmpathyTheme {
        DeleteTagConfirmDialog(
            tagContent = "这是一个很长的标签内容，用于测试对话框在处理长文本时的显示效果",
            onConfirm = {},
            onDismiss = {}
        )
    }
}

@Preview(
    name = "删除确认对话框 - 深色模式",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun DeleteTagConfirmDialogDarkPreview() {
    EmpathyTheme {
        DeleteTagConfirmDialog(
            tagContent = "避免讨论家庭问题",
            onConfirm = {},
            onDismiss = {}
        )
    }
}
