package com.empathy.ai.presentation.ui.screen.contact.persona

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.empathy.ai.presentation.R
import com.empathy.ai.presentation.ui.component.dialog.IOSDeleteConfirmDialog

/**
 * iOS 风格批量删除确认对话框
 *
 * BUG-00036 修复：迁移到 iOS 风格对话框
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
    IOSDeleteConfirmDialog(
        title = stringResource(R.string.delete_confirm_title),
        message = stringResource(R.string.delete_tags_confirm_message, selectedCount),
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}
