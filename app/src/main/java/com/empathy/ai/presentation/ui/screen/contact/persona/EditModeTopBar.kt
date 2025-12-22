package com.empathy.ai.presentation.ui.screen.contact.persona

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Deselect
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.empathy.ai.R

/**
 * 编辑模式顶栏
 *
 * 显示选中数量和操作按钮
 *
 * @param selectedCount 已选中的数量
 * @param onExitEditMode 退出编辑模式回调
 * @param onSelectAll 全选回调
 * @param onDeselectAll 取消全选回调
 * @param modifier Modifier
 */
@Composable
fun EditModeTopBar(
    selectedCount: Int,
    onExitEditMode: () -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 关闭按钮
            IconButton(onClick = onExitEditMode) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.exit_edit_mode),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            // 选中数量文本
            Text(
                text = stringResource(R.string.selected_count, selectedCount),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.weight(1f)
            )

            // 全选按钮
            IconButton(onClick = onSelectAll) {
                Icon(
                    imageVector = Icons.Default.SelectAll,
                    contentDescription = stringResource(R.string.select_all),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            // 取消全选按钮
            IconButton(onClick = onDeselectAll) {
                Icon(
                    imageVector = Icons.Default.Deselect,
                    contentDescription = stringResource(R.string.deselect_all),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}
