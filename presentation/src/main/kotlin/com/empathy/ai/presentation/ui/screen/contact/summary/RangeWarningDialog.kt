package com.empathy.ai.presentation.ui.screen.contact.summary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.empathy.ai.presentation.ui.component.dialog.IOSInputDialog

/**
 * 范围警告对话框 - iOS风格
 *
 * 当用户选择超过30天的日期范围时显示警告
 *
 * @param message 警告信息
 * @param onConfirm 确认继续回调
 * @param onDismiss 取消回调
 */
@Composable
fun RangeWarningDialog(
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    IOSInputDialog(
        title = "时间范围较长",
        content = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 警告图标
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(48.dp)
                )

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(
                    text = "建议分多次生成，每次不超过30天。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmText = "仍然继续",
        dismissText = "取消",
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}
