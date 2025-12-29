package com.empathy.ai.presentation.ui.component.dialog

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.empathy.ai.presentation.theme.EmpathyTheme

/**
 * iOS 风格权限请求对话框
 * 
 * 用于向用户解释悬浮窗权限的用途并引导用户授权
 * 
 * BUG-00036 修复：迁移到 iOS 风格对话框
 * 
 * @param onDismiss 取消回调
 * @param onConfirm 确认回调（跳转到设置页面）
 */
@Composable
fun PermissionRequestDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    // 使用 IOSAlertDialog.kt 中定义的 IOSPermissionRequestDialog
    IOSPermissionRequestDialog(
        onDismiss = onDismiss,
        onConfirm = onConfirm
    )
}

/**
 * 预览：权限请求对话框
 */
@Preview(showBackground = true)
@Composable
private fun PermissionRequestDialogPreview() {
    EmpathyTheme {
        PermissionRequestDialog(
            onDismiss = {},
            onConfirm = {}
        )
    }
}
