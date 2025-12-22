package com.empathy.ai.domain.model

import com.squareup.moshi.JsonClass
import java.time.Instant

/**
 * 全局提示词配置
 *
 * 存储所有场景的提示词配置，支持JSON序列化
 *
 * @property version 配置版本号，用于数据迁移
 * @property lastModified 最后修改时间（ISO 8601格式）
 * @property prompts 各场景的提示词配置映射
 *
 * @see TDD-00015 提示词设置优化技术设计
 */
@JsonClass(generateAdapter = true)
data class GlobalPromptConfig(
    val version: Int = CURRENT_VERSION,
    val lastModified: String = "",
    val prompts: Map<PromptScene, ScenePromptConfig> = emptyMap()
) {
    companion object {
        /**
         * 当前配置版本
         *
         * 版本历史：
         * - v1: 初始版本，6个场景
         * - v2: 三层分离架构，用户提示词不再包含变量占位符
         * - v3: 简化为4个场景，CHECK合并到POLISH，EXTRACT隐藏
         */
        const val CURRENT_VERSION = 3

        /**
         * 创建默认配置
         *
         * 使用DefaultPrompts中的默认提示词初始化活跃场景
         * 只为非废弃场景创建配置
         *
         * @param defaultPromptsProvider 默认提示词提供者函数
         * @return 初始化好的全局配置
         */
        fun createDefault(
            defaultPromptsProvider: (PromptScene) -> String = { "" }
        ): GlobalPromptConfig {
            // 只为活跃（非废弃）场景创建配置
            return GlobalPromptConfig(
                version = CURRENT_VERSION,
                lastModified = Instant.now().toString(),
                prompts = PromptScene.getActiveScenes().associateWith { scene ->
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
