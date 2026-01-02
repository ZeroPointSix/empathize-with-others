package com.empathy.ai.presentation.ui.screen.aiconfig

import com.empathy.ai.domain.model.ApiUsageStats
import com.empathy.ai.domain.model.ModelUsageStats
import com.empathy.ai.domain.model.ProviderUsageStats

/**
 * 用量统计页面UI状态
 *
 * @see TD-00025 Phase 6: 用量统计实现
 */
data class UsageStatsUiState(
    // 加载状态
    val isLoading: Boolean = false,
    val error: String? = null,

    // 统计数据
    val stats: ApiUsageStats? = null,

    // 当前选中的Tab
    val selectedTab: UsageStatsTab = UsageStatsTab.BY_PROVIDER,

    // 时间范围
    val timeRange: UsageTimeRange = UsageTimeRange.TODAY,

    // 操作状态
    val isExporting: Boolean = false,
    val isClearing: Boolean = false,
    val exportSuccess: Boolean = false,
    val clearSuccess: Boolean = false,

    // 确认对话框
    val showClearConfirmDialog: Boolean = false
) {
    /**
     * 计算属性：总请求数
     */
    val totalRequests: Int
        get() = stats?.totalRequests ?: 0

    /**
     * 计算属性：总Token数
     */
    val totalTokens: Long
        get() = stats?.totalTokens ?: 0L

    /**
     * 计算属性：成功率
     */
    val successRate: Float
        get() = stats?.getSuccessRate() ?: 0f

    /**
     * 计算属性：按服务商统计列表
     */
    val providerStats: List<ProviderUsageStats>
        get() = stats?.providerStats ?: emptyList()

    /**
     * 计算属性：按模型统计列表
     */
    val modelStats: List<ModelUsageStats>
        get() = stats?.modelStats ?: emptyList()

    /**
     * 计算属性：是否有数据
     */
    val hasData: Boolean
        get() = totalRequests > 0
}

/**
 * 用量统计Tab类型
 */
enum class UsageStatsTab {
    BY_PROVIDER,
    BY_MODEL;

    fun getDisplayName(): String = when (this) {
        BY_PROVIDER -> "按服务商"
        BY_MODEL -> "按模型"
    }
}

/**
 * 用量统计时间范围
 */
enum class UsageTimeRange {
    TODAY,
    THIS_WEEK,
    THIS_MONTH,
    ALL;

    fun getDisplayName(): String = when (this) {
        TODAY -> "今日"
        THIS_WEEK -> "本周"
        THIS_MONTH -> "本月"
        ALL -> "全部"
    }

    /**
     * 获取时间范围的起始时间戳
     */
    fun getStartTimestamp(): Long {
        val now = System.currentTimeMillis()
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = now

        return when (this) {
            TODAY -> {
                calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
                calendar.set(java.util.Calendar.MINUTE, 0)
                calendar.set(java.util.Calendar.SECOND, 0)
                calendar.set(java.util.Calendar.MILLISECOND, 0)
                calendar.timeInMillis
            }
            THIS_WEEK -> {
                calendar.set(java.util.Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
                calendar.set(java.util.Calendar.MINUTE, 0)
                calendar.set(java.util.Calendar.SECOND, 0)
                calendar.set(java.util.Calendar.MILLISECOND, 0)
                calendar.timeInMillis
            }
            THIS_MONTH -> {
                calendar.set(java.util.Calendar.DAY_OF_MONTH, 1)
                calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
                calendar.set(java.util.Calendar.MINUTE, 0)
                calendar.set(java.util.Calendar.SECOND, 0)
                calendar.set(java.util.Calendar.MILLISECOND, 0)
                calendar.timeInMillis
            }
            ALL -> 0L
        }
    }
}
