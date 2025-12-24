package com.empathy.ai.presentation.ui.component.state

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.empathy.ai.presentation.theme.EmpathyTheme

/**
 * 空状态视图组件
 *
 * 用于显示空状态提示和操作按钮
 *
 * @param message 空状态提示文字
 * @param actionText 操作按钮文字，为null时不显示按钮
 * @param onAction 操作按钮点击回调
 * @param modifier 修饰符
 * @param emptyType 空状态类型，用于显示不同的图标
 */
@Composable
fun EmptyView(
    message: String,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    emptyType: EmptyType = EmptyType.NoData
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 空状态图标
        Icon(
            imageVector = emptyType.icon,
            contentDescription = "空状态图标",
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 空状态标题
        Text(
            text = emptyType.title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 空状态消息
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        // 操作按钮
        if (actionText != null && onAction != null) {
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onAction) {
                Text(actionText)
            }
        }
    }
}

/**
 * 空状态类型枚举
 */
sealed class EmptyType(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val title: String
) {
    data object NoData : EmptyType(Icons.Default.Search, "暂无数据")
    data object NoContacts : EmptyType(Icons.Default.Add, "还没有联系人")
    data object NoTags : EmptyType(Icons.Default.Add, "还没有标签")
    data object NoResults : EmptyType(Icons.Default.Search, "没有找到结果")
}

// ============================================================
// 预览函数
// ============================================================

@Preview(name = "无数据", showBackground = true)
@Composable
private fun EmptyViewNoDataPreview() {
    EmpathyTheme {
        EmptyView(
            message = "这里还没有任何内容",
            actionText = "添加数据",
            onAction = {},
            emptyType = EmptyType.NoData
        )
    }
}

@Preview(name = "无联系人", showBackground = true)
@Composable
private fun EmptyViewNoContactsPreview() {
    EmpathyTheme {
        EmptyView(
            message = "点击下方按钮添加第一个联系人",
            actionText = "添加联系人",
            onAction = {},
            emptyType = EmptyType.NoContacts
        )
    }
}

@Preview(name = "无搜索结果", showBackground = true)
@Composable
private fun EmptyViewNoResultsPreview() {
    EmpathyTheme {
        EmptyView(
            message = "没有找到匹配的联系人，试试其他关键词",
            actionText = null,
            onAction = null,
            emptyType = EmptyType.NoResults
        )
    }
}

@Preview(name = "无操作按钮", showBackground = true)
@Composable
private fun EmptyViewNoActionPreview() {
    EmpathyTheme {
        EmptyView(
            message = "暂时没有可显示的内容",
            actionText = null,
            onAction = null,
            emptyType = EmptyType.NoData
        )
    }
}

@Preview(name = "深色模式", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EmptyViewDarkPreview() {
    EmpathyTheme {
        EmptyView(
            message = "点击下方按钮添加第一个联系人",
            actionText = "添加联系人",
            onAction = {},
            emptyType = EmptyType.NoContacts
        )
    }
}
