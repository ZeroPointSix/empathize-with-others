package com.empathy.ai.presentation.ui.screen.prompt.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.empathy.ai.presentation.R
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.ui.component.dialog.IOSAlertDialog

/**
 * iOS 风格放弃修改确认对话框
 *
 * BUG-00036 修复：迁移到 iOS 风格对话框
 *
 * @param onConfirm 确认放弃回调
 * @param onDismiss 取消回调
 */
@Composable
fun DiscardConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    IOSAlertDialog(
        title = stringResource(R.string.prompt_editor_discard_title),
        message = stringResource(R.string.prompt_editor_discard_message),
        confirmText = stringResource(R.string.prompt_editor_discard_confirm),
        dismissText = stringResource(R.string.prompt_editor_discard_cancel),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        isDestructive = true
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
