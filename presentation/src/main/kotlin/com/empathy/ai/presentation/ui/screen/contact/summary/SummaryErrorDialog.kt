package com.empathy.ai.presentation.ui.screen.contact.summary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.empathy.ai.domain.model.SummaryError
import com.empathy.ai.presentation.ui.component.dialog.IOSInputDialog

/**
 * 总结错误对话框 - iOS风格
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
    IOSInputDialog(
        title = "总结生成失败",
        content = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 错误图标
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(48.dp)
                )

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
        confirmText = if (error.isRetryable()) "重试" else "确定",
        dismissText = if (error.isRetryable()) "取消" else "",
        onConfirm = if (error.isRetryable()) onRetry else onDismiss,
        onDismiss = onDismiss,
        showDismissButton = error.isRetryable()
    )
}
