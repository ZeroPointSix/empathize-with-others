package com.empathy.ai.presentation.ui.screen.prompt.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.empathy.ai.presentation.R
import com.empathy.ai.presentation.theme.EmpathyTheme

/**
 * 放弃修改确认对话框
 *
 * @param onConfirm 确认放弃回调
 * @param onDismiss 取消回调
 */
@Composable
fun DiscardConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.prompt_editor_discard_title),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Text(
                text = stringResource(R.string.prompt_editor_discard_message),
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(R.string.prompt_editor_discard_confirm),
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.prompt_editor_discard_cancel))
            }
        }
    )
}

@Preview
@Composable
private fun DiscardConfirmDialogPreview() {
    EmpathyTheme {
        DiscardConfirmDialog(
            onConfirm = {},
            onDismiss = {}
        )
    }
}
