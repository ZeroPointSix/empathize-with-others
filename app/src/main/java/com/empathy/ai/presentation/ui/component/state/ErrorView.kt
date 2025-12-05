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
import androidx.compose.material.icons.filled.Warning
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
 * 错误视图组件
 *
 * 用于显示错误信息并提供重试操作
 *
 * @param message 错误消息
 * @param onRetry 重试回调
 * @param modifier 修饰符
 * @param errorType 错误类型，用于显示不同的图标和样式
 */
@Composable
fun ErrorView(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    errorType: ErrorType = ErrorType.General
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 错误图标
        Icon(
            imageVector = errorType.icon,
            contentDescription = "错误图标",
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 错误标题
        Text(
            text = errorType.title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 错误消息
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // 重试按钮
        Button(onClick = onRetry) {
            Text("重试")
        }
    }
}

/**
 * 错误类型枚举
 */
sealed class ErrorType(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val title: String
) {
    data object General : ErrorType(Icons.Default.Warning, "出错了")
    data object Network : ErrorType(Icons.Default.Warning, "网络错误")
    data object NotFound : ErrorType(Icons.Default.Warning, "未找到数据")
    data object Permission : ErrorType(Icons.Default.Warning, "权限不足")
}

// ============================================================
// 预览函数
// ============================================================

@Preview(name = "一般错误", showBackground = true)
@Composable
private fun ErrorViewGeneralPreview() {
    EmpathyTheme {
        ErrorView(
            message = "加载数据时发生错误，请稍后重试。",
            onRetry = {},
            errorType = ErrorType.General
        )
    }
}

@Preview(name = "网络错误", showBackground = true)
@Composable
private fun ErrorViewNetworkPreview() {
    EmpathyTheme {
        ErrorView(
            message = "无法连接到服务器，请检查网络连接后重试。",
            onRetry = {},
            errorType = ErrorType.Network
        )
    }
}

@Preview(name = "未找到数据", showBackground = true)
@Composable
private fun ErrorViewNotFoundPreview() {
    EmpathyTheme {
        ErrorView(
            message = "未找到相关数据，请尝试其他搜索条件。",
            onRetry = {},
            errorType = ErrorType.NotFound
        )
    }
}

@Preview(name = "长文本消息", showBackground = true)
@Composable
private fun ErrorViewLongMessagePreview() {
    EmpathyTheme {
        ErrorView(
            message = "保存联系人信息时发生错误。可能是因为网络连接不稳定，或者服务器正在维护中。请稍后再试，如果问题持续存在，请联系技术支持。",
            onRetry = {},
            errorType = ErrorType.General
        )
    }
}

@Preview(name = "深色模式", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ErrorViewDarkPreview() {
    EmpathyTheme {
        ErrorView(
            message = "无法连接到服务器，请检查网络连接后重试。",
            onRetry = {},
            errorType = ErrorType.Network
        )
    }
}
