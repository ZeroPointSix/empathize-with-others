package com.empathy.ai.domain.model

import com.squareup.moshi.JsonClass
import java.time.Instant

/**
 * 场景提示词配置
 *
 * 存储单个场景的提示词配置，包括用户自定义提示词和历史记录
 *
 * @property userPrompt 用户自定义提示词
 * @property enabled 是否启用该场景
 * @property history 历史记录列表（最多保留3条）
 */
@JsonClass(generateAdapter = true)
data class ScenePromptConfig(
    val userPrompt: String,
    val enabled: Boolean = true,
    val history: List<PromptHistoryItem> = emptyList()
) {
    companion object {
        /**
         * 最大历史记录数量
         */
        const val MAX_HISTORY_SIZE = 3
    }

    /**
     * 添加历史记录
     *
     * 将当前提示词保存到历史记录中，自动裁剪超出限制的旧记录
     *
     * @param oldPrompt 要保存的旧提示词
     * @return 更新后的配置对象
     */
    fun addHistory(oldPrompt: String): ScenePromptConfig {
        val newHistory = listOf(
            PromptHistoryItem(
                timestamp = Instant.now().toString(),
                userPrompt = oldPrompt
            )
        ) + history
        return copy(history = newHistory.take(MAX_HISTORY_SIZE))
    }
}
