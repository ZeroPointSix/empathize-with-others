package com.empathy.ai.domain.model

import com.empathy.ai.domain.util.DateUtils

/**
 * 每日总结领域模型（扩展版）
 *
 * 表示某个联系人某一天或某段时间的互动总结
 * 支持单日总结（DAILY）和自定义范围总结（CUSTOM_RANGE）
 * 支持编辑追踪功能
 *
 * @property id 总结ID
 * @property contactId 联系人ID
 * @property summaryDate 总结日期，格式: "yyyy-MM-dd"（单日总结使用此字段）
 * @property content 总结内容
 * @property keyEvents 关键事件列表
 * @property newFacts 新发现的事实列表
 * @property updatedTags 标签更新列表
 * @property relationshipScoreChange 关系分数变化 (-10到+10)
 * @property relationshipTrend 关系趋势
 * @property startDate 范围总结开始日期（仅CUSTOM_RANGE类型使用）
 * @property endDate 范围总结结束日期（仅CUSTOM_RANGE类型使用）
 * @property summaryType 总结类型
 * @property generationSource 生成来源
 * @property conversationCount 分析的对话数量
 * @property generatedAt 生成时间戳
 * @property isUserModified 是否被用户修改过
 * @property lastModifiedTime 最后修改时间
 * @property originalContent 原始内容（修改前）
 */
data class DailySummary(
    val id: Long = 0,
    val contactId: String,
    val summaryDate: String,
    val content: String,
    val keyEvents: List<KeyEvent>,
    val newFacts: List<Fact>,
    val updatedTags: List<TagUpdate>,
    val relationshipScoreChange: Int,
    val relationshipTrend: RelationshipTrend,
    // ==================== v9 新增字段 ====================
    val startDate: String? = null,
    val endDate: String? = null,
    val summaryType: SummaryType = SummaryType.DAILY,
    val generationSource: GenerationSource = GenerationSource.AUTO,
    val conversationCount: Int = 0,
    val generatedAt: Long = System.currentTimeMillis(),
    // ==================== v10 编辑追踪字段 ====================
    val isUserModified: Boolean = false,
    val lastModifiedTime: Long = generatedAt,
    val originalContent: String? = null
) {
    init {
        require(contactId.isNotBlank()) { "contactId不能为空" }
        require(summaryDate.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) {
            "summaryDate格式必须为yyyy-MM-dd"
        }
        require(relationshipScoreChange in -10..10) {
            "relationshipScoreChange必须在-10到10之间"
        }
    }

    /**
     * 创建编辑后的副本
     *
     * @param newContent 新的总结内容
     * @return 编辑后的DailySummary副本
     */
    fun copyWithEdit(newContent: String): DailySummary {
        return copy(
            content = newContent,
            isUserModified = true,
            lastModifiedTime = System.currentTimeMillis(),
            // 仅首次编辑时保存原始值
            originalContent = if (originalContent == null) content else originalContent
        )
    }

    /**
     * 判断内容是否有变化
     *
     * @param newContent 新的内容
     * @return 是否有变化
     */
    fun hasChanges(newContent: String): Boolean {
        return content != newContent
    }

    /**
     * 格式化最后修改时间
     */
    fun formatLastModifiedTime(): String = DateUtils.formatRelativeTime(lastModifiedTime)

    /**
     * 判断是否有实质性内容
     */
    fun hasSubstantialContent(): Boolean {
        return keyEvents.isNotEmpty() || newFacts.isNotEmpty() || updatedTags.isNotEmpty()
    }

    /**
     * 是否为范围总结
     */
    fun isRangeSummary(): Boolean = summaryType == SummaryType.CUSTOM_RANGE

    /**
     * 是否为手动生成
     */
    fun isManualGenerated(): Boolean = generationSource == GenerationSource.MANUAL

    /**
     * 获取显示日期范围
     */
    fun getDisplayDateRange(): String {
        return if (isRangeSummary() && startDate != null && endDate != null) {
            "$startDate 至 $endDate"
        } else {
            summaryDate
        }
    }

    /**
     * 获取日期范围对象（仅范围总结有效）
     */
    fun getDateRange(): DateRange? {
        return if (isRangeSummary() && startDate != null && endDate != null) {
            DateRange(startDate, endDate)
        } else {
            null
        }
    }
}
