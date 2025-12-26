package com.empathy.ai.presentation.ui.screen.contact.overview

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.empathy.ai.presentation.ui.component.timeline.TimelineItem

/**
 * 沟通目标数据
 */
data class CommunicationGoal(
    val id: String,
    val title: String,
    val description: String,
    val progress: Float,
    val dueDate: String? = null
)

/**
 * 最近事实项（简化版，用于概览页显示）
 */
data class RecentFactItem(
    val id: String,
    val content: String,
    val timestamp: Long,
    val emotionType: String? = null
)

/**
 * 快速操作枚举
 */
enum class QuickAction(
    @DrawableRes val iconRes: Int = 0,
    @StringRes val labelRes: Int = 0
) {
    CHAT,
    CALL,
    GIFT,
    NOTE
}

/**
 * 概览页面UI状态
 * 
 * @param contactName 联系人姓名
 * @param avatarUrl 头像URL
 * @param isOnline 是否在线
 * @param daysKnown 认识天数
 * @param healthScore 关系健康度分数（0-100）
 * @param trendData 趋势数据（最近7天）
 * @param recentFacts 最近事实列表
 * @param goals 沟通目标列表
 * @param isLoading 是否加载中
 * @param error 错误信息
 * 
 * @see TDD-00020 8.1 OverviewTab状态管理
 */
data class OverviewUiState(
    val contactName: String = "",
    val avatarUrl: String? = null,
    val isOnline: Boolean = false,
    val daysKnown: Int = 0,
    val healthScore: Int = 0,
    val trendData: List<Float> = emptyList(),
    val recentFacts: List<RecentFactItem> = emptyList(),
    val goals: List<CommunicationGoal> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
) {
    /**
     * 健康度进度（0f-1f），自动从healthScore计算
     */
    val healthProgress: Float
        get() = (healthScore.coerceIn(0, 100) / 100f)
    
    /**
     * 是否有趋势数据
     */
    val hasTrendData: Boolean
        get() = trendData.isNotEmpty()
    
    /**
     * 是否有最近事实
     */
    val hasRecentFacts: Boolean
        get() = recentFacts.isNotEmpty()
    
    /**
     * 是否有错误
     */
    val hasError: Boolean
        get() = error != null
    
    /**
     * 获取关系状态描述
     */
    fun getRelationshipStatus(): String {
        return when {
            healthScore >= 80 -> "关系良好"
            healthScore >= 60 -> "关系稳定"
            healthScore >= 40 -> "需要关注"
            else -> "需要改善"
        }
    }
    
    /**
     * 获取趋势描述
     */
    fun getTrendDescription(): String {
        if (trendData.size < 2) return "数据不足"
        val lastValue = trendData.last()
        val firstValue = trendData.first()
        return when {
            lastValue > firstValue -> "上升趋势"
            lastValue < firstValue -> "下降趋势"
            else -> "保持稳定"
        }
    }
}
