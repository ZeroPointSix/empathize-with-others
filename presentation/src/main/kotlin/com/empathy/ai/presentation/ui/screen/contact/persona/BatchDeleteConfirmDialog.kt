package com.empathy.ai.presentation.ui.screen.contact.persona

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.empathy.ai.presentation.R

/**
 * 批量删除确认对话框
 *
 * @param selectedCount 已选中的标签数量
 * @param onConfirm 确认删除回调
 * @param onDismiss 取消回调
 */
@Composable
fun BatchDeleteConfirmDialog(
    selectedCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.delete_confirm_title))
        },
        text = {
            Text(
                text = stringResource(R.string.delete_tags_confirm_message, selectedCount)
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm
            ) {
                Text(
                    text = stringResource(R.string.delete),
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
