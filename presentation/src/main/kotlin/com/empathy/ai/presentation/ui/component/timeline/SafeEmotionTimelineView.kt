package com.empathy.ai.presentation.ui.component.timeline

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.empathy.ai.presentation.theme.EmpathyTheme
import com.empathy.ai.presentation.theme.EmotionType
import com.empathy.ai.presentation.ui.component.state.EmptyStateView
import com.empathy.ai.presentation.ui.component.state.ErrorStateView

/**
 * 安全的情绪时光轴视图
 * 
 * 包装EmotionTimelineView添加错误处理:
 * - 空状态显示EmptyStateView
 * - 错误状态显示ErrorStateView
 * - 加载状态显示LoadingIndicator
 * - 数据预加载验证
 * 
 * @param items 时光轴数据项列表
 * @param isLoading 是否加载中
 * @param error 错误信息
 * @param onItemClick 数据项点击回调
 * @param onRetry 重试回调
 * @param modifier 修饰符
 * 
 * @see TDD-00020 9.7 列表加载错误处理
 */
@Composable
fun SafeEmotionTimelineView(
    items: List<TimelineItem>,
    isLoading: Boolean = false,
    error: String? = null,
    onItemClick: (TimelineItem) -> Unit = {},
    onRetry: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when {
            // 加载状态
            isLoading -> {
                LoadingIndicator(
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // 错误状态
            error != null -> {
                ErrorStateView(
                    message = error,
                    onRetry = onRetry,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // 空状态
            items.isEmpty() -> {
                EmptyStateView(
                    message = "暂无事实记录",
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // 正常显示
            else -> {
                // 数据预验证
                val validatedItems = items.filter { item ->
                    item.id.isNotBlank() && item.content.isNotBlank()
                }
                
                if (validatedItems.isEmpty()) {
                    EmptyStateView(
                        message = "暂无有效记录",
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    EmotionTimelineView(
                        items = validatedItems,
                        onItemClick = onItemClick,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

/**
 * 加载指示器
 */
@Composable
private fun LoadingIndicator(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

// ============================================================
// 预览函数
// ============================================================

@Preview(name = "安全时光轴-正常", showBackground = true)
@Composable
private fun SafeEmotionTimelineViewNormalPreview() {
    EmpathyTheme {
        SafeEmotionTimelineView(
            items = listOf(
                TimelineItem(
                    id = "1",
                    emotionType = EmotionType.SWEET,
                    timestamp = "今天 14:30",
                    content = "今天一起吃了晚餐，聊得很开心"
                ),
                TimelineItem(
                    id = "2",
                    emotionType = EmotionType.GIFT,
                    timestamp = "昨天 18:00",
                    content = "收到了生日礼物"
                )
            ),
            isLoading = false,
            error = null
        )
    }
}

@Preview(name = "安全时光轴-加载中", showBackground = true)
@Composable
private fun SafeEmotionTimelineViewLoadingPreview() {
    EmpathyTheme {
        SafeEmotionTimelineView(
            items = emptyList(),
            isLoading = true,
            error = null
        )
    }
}

@Preview(name = "安全时光轴-错误", showBackground = true)
@Composable
private fun SafeEmotionTimelineViewErrorPreview() {
    EmpathyTheme {
        SafeEmotionTimelineView(
            items = emptyList(),
            isLoading = false,
            error = "网络连接失败"
        )
    }
}

@Preview(name = "安全时光轴-空状态", showBackground = true)
@Composable
private fun SafeEmotionTimelineViewEmptyPreview() {
    EmpathyTheme {
        SafeEmotionTimelineView(
            items = emptyList(),
            isLoading = false,
            error = null
        )
    }
}
