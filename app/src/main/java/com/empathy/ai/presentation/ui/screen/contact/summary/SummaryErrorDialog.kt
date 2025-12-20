package com.empathy.ai.presentation.ui.screen.contact.summary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.empathy.ai.domain.model.SummaryError

/**
 * 总结错误对话框
 *
 * 显示总结失败时的错误信息和建议操作
 *
 * @param error 错误信息
 * @param onRetry 重试回调（仅在可重试错误时显示）
 * @param onDismiss 关闭回调
 */
@Composable
fun SummaryErrorDialog(
    error: SummaryError,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp)
            )
        },
        title = { Text("总结生成失败") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 错误信息
                Text(
                    text = "原因：${error.userMessage}",
                    style = MaterialTheme.typography.bodyMedium
                )

                // 建议操作
                Text(
                    text = "建议：",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = error.getSuggestedAction(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            if (error.isRetryable()) {
                TextButton(onClick = onRetry) {
                    Text("重试")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (error.isRetryable()) "取消" else "确定")
            }
        }
    )
}
