package com.empathy.ai.domain.model

import com.squareup.moshi.JsonClass
import java.time.Instant

/**
 * 全局提示词配置
 *
 * 存储所有场景的提示词配置，支持JSON序列化
 *
 * @property version 配置版本号
 * @property lastModified 最后修改时间（ISO 8601格式）
 * @property prompts 各场景的提示词配置映射
 */
@JsonClass(generateAdapter = true)
data class GlobalPromptConfig(
    val version: Int = 1,
    val lastModified: String = "",
    val prompts: Map<PromptScene, ScenePromptConfig> = emptyMap()
) {
    companion object {
        /**
         * 创建默认配置
         *
         * 使用DefaultPrompts中的默认提示词初始化所有场景
         *
         * @param defaultPromptsProvider 默认提示词提供者函数
         * @return 初始化好的全局配置
         */
        fun createDefault(
            defaultPromptsProvider: (PromptScene) -> String = { "" }
        ): GlobalPromptConfig {
            return GlobalPromptConfig(
                version = 1,
                lastModified = Instant.now().toString(),
                prompts = PromptScene.entries.associateWith { scene ->
                    ScenePromptConfig(
                        userPrompt = defaultPromptsProvider(scene),
                        enabled = true,
                        history = emptyList()
                    )
                }
            )
        }
    }
}
