package com.empathy.ai.presentation.ui.component.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.empathy.ai.R

/**
 * 删除确认对话框
 *
 * 显示警告图标、标题和提示消息
 * 删除按钮为红色（error颜色）
 *
 * @param title 对话框标题
 * @param message 提示消息
 * @param onConfirm 确认删除回调
 * @param onDismiss 取消回调
 */
@Composable
fun DeleteConfirmDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(stringResource(R.string.delete))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

/**
 * 删除事实确认对话框
 */
@Composable
fun DeleteFactConfirmDialog(
    factKey: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    DeleteConfirmDialog(
        title = stringResource(R.string.delete_fact_title),
        message = stringResource(R.string.delete_fact_message, factKey),
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}

/**
 * 删除对话确认对话框
 */
@Composable
fun DeleteConversationConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    DeleteConfirmDialog(
        title = stringResource(R.string.delete_conversation_title),
        message = stringResource(R.string.delete_conversation_message),
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}
