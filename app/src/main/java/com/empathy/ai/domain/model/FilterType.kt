package com.empathy.ai.domain.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 筛选类型枚举
 *
 * 用于事实流的快速筛选功能
 *
 * @property displayName 显示名称
 * @property icon 图标
 */
enum class FilterType(
    val displayName: String,
    val icon: ImageVector
) {
    /**
     * 显示全部
     */
    ALL("全部", Icons.Default.FilterList),
    
    /**
     * 只显示AI总结
     */
    AI_SUMMARY("只看AI", Icons.Default.Psychology),
    
    /**
     * 只显示冲突事件
     */
    CONFLICT("只看冲突", Icons.Default.Warning),
    
    /**
     * 只显示约会事件
     */
    DATE("只看约会", Icons.Default.Restaurant),
    
    /**
     * 只显示甜蜜时刻
     */
    SWEET("只看甜蜜", Icons.Default.Favorite);
    
    /**
     * 应用筛选条件
     *
     * @param items 要筛选的时间线项目列表
     * @return 筛选后的列表
     */
    fun apply(items: List<TimelineItem>): List<TimelineItem> {
        return when (this) {
            ALL -> items
            AI_SUMMARY -> items.filterIsInstance<TimelineItem.AiSummary>()
            CONFLICT -> items.filter { it.emotionType == EmotionType.CONFLICT }
            DATE -> items.filter { it.emotionType == EmotionType.DATE }
            SWEET -> items.filter { it.emotionType == EmotionType.SWEET }
        }
    }
}
