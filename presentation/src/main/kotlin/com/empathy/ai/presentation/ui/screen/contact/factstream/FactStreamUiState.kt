package com.empathy.ai.presentation.ui.screen.contact.factstream

import com.empathy.ai.presentation.ui.component.timeline.TimelineItem

/**
 * 视图模式枚举
 */
enum class ViewMode(val displayName: String) {
    TIMELINE("时光轴"),
    LIST("清单")
}

/**
 * 筛选类型枚举
 */
enum class FilterType(val displayName: String, val emoji: String? = null) {
    ALL("全部", null),
    SWEET("甜蜜", "❤️"),
    CONFLICT("冲突", "⛈️"),
    NEUTRAL("中性", "😐"),
    GIFT("礼物", "🎁"),
    DATE("约会", "🍽️"),
    DEEP_TALK("深谈", "💬"),
    AI_SUMMARY("AI总结", "🧠")
}

/**
 * 事实流数据项（简化版，用于UI显示）
 */
data class FactStreamItem(
    val id: String,
    val content: String,
    val emotionType: String,
    val timestamp: Long,
    val aiSuggestion: String? = null,
    val isAiSummary: Boolean = false,
    val scoreChange: Int = 0,
    val tags: List<String> = emptyList()
) {
    /**
     * 是否有正向分数变化
     */
    val hasPositiveScoreChange: Boolean
        get() = scoreChange > 0
    
    /**
     * 是否有负向分数变化
     */
    val hasNegativeScoreChange: Boolean
        get() = scoreChange < 0
}

/**
 * 事实流页面UI状态
 * 
 * @param viewMode 当前视图模式
 * @param selectedFilter 当前选中的筛选类型
 * @param items 时光轴/清单数据项
 * @param isLoading 是否加载中
 * @param error 错误信息
 * @param hasMore 是否有更多数据
 * 
 * @see TDD-00020 8.2 FactStreamTab状态管理
 */
data class FactStreamUiState(
    val viewMode: ViewMode = ViewMode.TIMELINE,
    val selectedFilter: FilterType = FilterType.ALL,
    val items: List<FactStreamItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val hasMore: Boolean = false
) {
    /**
     * 是否有数据项
     */
    val hasItems: Boolean
        get() = items.isNotEmpty()
    
    /**
     * 是否有错误
     */
    val hasError: Boolean
        get() = error != null
    
    /**
     * 获取过滤后的数据项
     */
    val filteredItems: List<FactStreamItem>
        get() {
            if (selectedFilter == FilterType.ALL) return items
            
            return items.filter { item ->
                when (selectedFilter) {
                    FilterType.AI_SUMMARY -> item.isAiSummary
                    FilterType.SWEET -> item.emotionType == "SWEET"
                    FilterType.CONFLICT -> item.emotionType == "CONFLICT"
                    FilterType.NEUTRAL -> item.emotionType == "NEUTRAL"
                    FilterType.GIFT -> item.emotionType == "GIFT"
                    FilterType.DATE -> item.emotionType == "DATE"
                    FilterType.DEEP_TALK -> item.emotionType == "DEEP_TALK"
                    else -> true
                }
            }
        }
    
    /**
     * 获取筛选类型列表
     */
    fun getFilterTypes(): List<FilterType> = FilterType.entries
    
    /**
     * 是否显示空状态
     */
    fun shouldShowEmptyState(): Boolean {
        return !isLoading && error == null && filteredItems.isEmpty()
    }
}
