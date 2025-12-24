package com.empathy.ai.domain.model

/**
 * AI总结响应模型
 *
 * 用于表示AI生成的总结结果，包含总结内容、关键事件、新事实、标签更新等信息。
 * 这是Domain层的领域模型，不依赖任何序列化库。
 */
data class AiSummaryResponse(
    /** 总结内容 */
    val summary: String,
    /** 关键事件列表 */
    val keyEvents: List<KeyEventData>? = null,
    /** 新发现的事实 */
    val newFacts: List<FactData> = emptyList(),
    /** 更新的事实 */
    val updatedFacts: List<FactData>? = null,
    /** 删除的事实键 */
    val deletedFactKeys: List<String>? = null,
    /** 新标签 */
    val newTags: List<TagUpdateData>? = null,
    /** 更新的标签 */
    val updatedTags: List<TagUpdateData>? = null,
    /** 关系分数变化 */
    val relationshipScoreChange: Int = 0,
    /** 关系趋势 */
    val relationshipTrend: String = "STABLE"
)

/**
 * 关键事件数据
 */
data class KeyEventData(
    /** 事件描述 */
    val event: String,
    /** 重要程度（1-5） */
    val importance: Int = 3
)

/**
 * 事实数据
 */
data class FactData(
    /** 事实键 */
    val key: String,
    /** 事实值 */
    val value: String,
    /** 来源 */
    val source: String = "AI_INFERRED"
)

/**
 * 标签更新数据
 */
data class TagUpdateData(
    /** 操作类型（ADD/REMOVE） */
    val action: String = "ADD",
    /** 标签类型 */
    val type: String,
    /** 标签内容 */
    val content: String
)
