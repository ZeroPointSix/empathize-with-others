package com.empathy.ai.domain.model

/**
 * AI 从文本中提取的信息
 *
 * 用于FeedTextUseCase返回的提取结果，需要用户确认后才会入库
 *
 * @property facts 提取的事实信息（键值对形式）
 * @property redTags 雷区标签列表
 * @property greenTags 策略标签列表
 */
data class ExtractedData(
    val facts: Map<String, String> = emptyMap(),
    val redTags: List<String> = emptyList(),
    val greenTags: List<String> = emptyList()
)
