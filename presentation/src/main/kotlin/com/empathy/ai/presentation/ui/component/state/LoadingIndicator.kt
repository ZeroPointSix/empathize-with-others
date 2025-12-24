package com.empathy.ai.presentation.ui.component.state

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.empathy.ai.presentation.theme.EmpathyTheme

/**
 * 加载指示器组件
 *
 * 用于显示数据加载状态，支持不同大小和可选的提示文本
 *
 * @param message 加载提示文字，为空时不显示文字
 * @param size 指示器大小，默认为Medium
 * @param modifier 修饰符
 */
@Composable
fun LoadingIndicator(
    message: String? = null,
    size: LoadingSize = LoadingSize.Medium,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(16.dp)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(size.dp),
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = size.strokeWidth
        )
        
        if (message != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

/**
 * 全屏加载指示器
 *
 * 用于全屏遮罩的加载状态显示
 *
 * @param message 加载提示文字
 * @param modifier 修饰符
 */
@Composable
fun LoadingIndicatorFullScreen(
    message: String? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        LoadingIndicator(
            message = message,
            size = LoadingSize.Large
        )
    }
}

/**
 * 加载指示器大小枚举
 */
enum class LoadingSize(val dp: Dp, val strokeWidth: Dp) {
    Small(32.dp, 3.dp),
    Medium(48.dp, 4.dp),
    Large(64.dp, 5.dp)
}

// ============================================================
// 预览函数
// ============================================================

@Preview(name = "小尺寸", showBackground = true)
@Composable
private fun LoadingIndicatorSmallPreview() {
    EmpathyTheme {
        LoadingIndicator(
            message = "加载中...",
            size = LoadingSize.Small
        )
    }
}

@Preview(name = "中尺寸", showBackground = true)
@Composable
private fun LoadingIndicatorMediumPreview() {
    EmpathyTheme {
        LoadingIndicator(
            message = "正在加载数据...",
            size = LoadingSize.Medium
        )
    }
}

@Preview(name = "大尺寸", showBackground = true)
@Composable
private fun LoadingIndicatorLargePreview() {
    EmpathyTheme {
        LoadingIndicator(
            message = "正在分析聊天记录...",
            size = LoadingSize.Large
        )
    }
}

@Preview(name = "无文字", showBackground = true)
@Composable
private fun LoadingIndicatorNoMessagePreview() {
    EmpathyTheme {
        LoadingIndicator(
            message = null,
            size = LoadingSize.Medium
        )
    }
}

@Preview(name = "全屏模式", showBackground = true)
@Composable
private fun LoadingIndicatorFullScreenPreview() {
    EmpathyTheme {
        LoadingIndicatorFullScreen(
            message = "正在加载联系人..."
        )
    }
}

@Preview(name = "深色模式", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun LoadingIndicatorDarkPreview() {
    EmpathyTheme {
        LoadingIndicator(
            message = "加载中...",
            size = LoadingSize.Medium
        )
    }
}
