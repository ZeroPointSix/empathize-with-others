package com.empathy.ai.domain.model

import com.squareup.moshi.JsonClass

/**
 * 提示词历史记录项
 *
 * 记录提示词的历史版本，用于版本回退功能
 *
 * @property timestamp 保存时间戳（ISO 8601格式）
 * @property userPrompt 用户提示词内容
 */
@JsonClass(generateAdapter = true)
data class PromptHistoryItem(
    val timestamp: String,
    val userPrompt: String
)
