package com.empathy.ai.domain.model

import java.util.UUID

/**
 * AI 服务商预设配置
 * 
 * 为常见的 AI 服务商提供预设配置，包括：
 * - 基础 URL
 * - 可用模型列表
 * - 推荐的超时时间
 * 
 * 用户只需要填写 API Key 即可快速配置
 */
object ProviderPresets {
    
    /**
     * OpenAI GPT-4 预设
     * 
     * 特点：
     * - 响应速度：中等（通常 5-15 秒）
     * - 推荐超时：20 秒
     */
    fun createOpenAiGpt4(apiKey: String): AiProvider {
        return AiProvider(
            id = UUID.randomUUID().toString(),
            name = "OpenAI GPT-4",
            baseUrl = "https://api.openai.com/v1",
            apiKey = apiKey,
            models = listOf(
                AiModel(
                    id = "gpt-4",
                    displayName = "GPT-4",
                    supportsImage = true
                ),
                AiModel(
                    id = "gpt-4-turbo",
                    displayName = "GPT-4 Turbo",
                    supportsImage = true
                )
            ),
            defaultModelId = "gpt-4-turbo",
            isDefault = false,
            timeoutMs = 20000L  // 20 秒
        )
    }
    
    /**
     * OpenAI GPT-3.5 预设
     * 
     * 特点：
     * - 响应速度：快（通常 3-8 秒）
     * - 推荐超时：15 秒
     */
    fun createOpenAiGpt35(apiKey: String): AiProvider {
        return AiProvider(
            id = UUID.randomUUID().toString(),
            name = "OpenAI GPT-3.5",
            baseUrl = "https://api.openai.com/v1",
            apiKey = apiKey,
            models = listOf(
                AiModel(
                    id = "gpt-3.5-turbo",
                    displayName = "GPT-3.5 Turbo",
                    supportsImage = true
                )
            ),
            defaultModelId = "gpt-3.5-turbo",
            isDefault = false,
            timeoutMs = 15000L  // 15 秒
        )
    }
    
    /**
     * Google Gemini Pro 预设
     * 
     * 特点：
     * - 响应速度：较慢（通常 15-30 秒）
     * - 推荐超时：40 秒
     */
    fun createGeminiPro(apiKey: String): AiProvider {
        return AiProvider(
            id = UUID.randomUUID().toString(),
            name = "Google Gemini Pro",
            baseUrl = "https://generativelanguage.googleapis.com/v1beta",
            apiKey = apiKey,
            models = listOf(
                AiModel(
                    id = "gemini-pro",
                    displayName = "Gemini Pro",
                    supportsImage = true
                ),
                AiModel(
                    id = "gemini-1.5-pro",
                    displayName = "Gemini 1.5 Pro",
                    supportsImage = true
                )
            ),
            defaultModelId = "gemini-1.5-pro",
            isDefault = false,
            timeoutMs = 40000L  // 40 秒（Gemini 较慢）
        )
    }
    
    /**
     * DeepSeek 预设
     * 
     * 特点：
     * - 响应速度：中等（通常 8-20 秒）
     * - 推荐超时：25 秒
     */
    fun createDeepSeek(apiKey: String): AiProvider {
        return AiProvider(
            id = UUID.randomUUID().toString(),
            name = "DeepSeek",
            baseUrl = "https://api.deepseek.com/v1",
            apiKey = apiKey,
            models = listOf(
                AiModel(
                    id = "deepseek-chat",
                    displayName = "DeepSeek Chat",
                    supportsImage = true
                ),
                AiModel(
                    id = "deepseek-coder",
                    displayName = "DeepSeek Coder",
                    supportsImage = true
                )
            ),
            defaultModelId = "deepseek-chat",
            isDefault = false,
            timeoutMs = 25000L  // 25 秒
        )
    }
    
    /**
     * 自定义 OpenAI 兼容服务商
     * 
     * 用于配置自定义的 OpenAI 兼容 API（如本地部署的模型）
     * 
     * @param name 服务商名称
     * @param baseUrl 基础 URL
     * @param apiKey API Key
     * @param modelId 模型 ID
     * @param modelName 模型显示名称
     * @param timeoutMs 超时时间（毫秒），默认 30 秒
     */
    fun createCustomOpenAiCompatible(
        name: String,
        baseUrl: String,
        apiKey: String,
        modelId: String,
        modelName: String,
        timeoutMs: Long = 30000L
    ): AiProvider {
        return AiProvider(
            id = UUID.randomUUID().toString(),
            name = name,
            baseUrl = baseUrl,
            apiKey = apiKey,
            models = listOf(
                AiModel(
                    id = modelId,
                    displayName = modelName,
                    supportsImage = true
                )
            ),
            defaultModelId = modelId,
            isDefault = false,
            timeoutMs = timeoutMs
        )
    }
    
    /**
     * 获取所有预设配置的列表（不包含 API Key）
     * 
     * 用于在 UI 中展示可选的服务商
     */
    fun getAllPresets(): List<ProviderPresetInfo> {
        return listOf(
            ProviderPresetInfo(
                name = "OpenAI GPT-4",
                description = "最强大的模型，适合复杂分析",
                recommendedTimeout = 20000L,
                factory = ::createOpenAiGpt4
            ),
            ProviderPresetInfo(
                name = "OpenAI GPT-3.5",
                description = "快速且经济的模型",
                recommendedTimeout = 15000L,
                factory = ::createOpenAiGpt35
            ),
            ProviderPresetInfo(
                name = "Google Gemini Pro",
                description = "Google 的多模态模型",
                recommendedTimeout = 40000L,
                factory = ::createGeminiPro
            ),
            ProviderPresetInfo(
                name = "DeepSeek",
                description = "高性价比的中文模型",
                recommendedTimeout = 25000L,
                factory = ::createDeepSeek
            )
        )
    }
}

/**
 * Provider 预设信息
 * 
 * 用于在 UI 中展示预设配置的元数据
 * 
 * @property name 服务商名称
 * @property description 服务商描述
 * @property recommendedTimeout 推荐的超时时间（毫秒）
 * @property factory 创建 Provider 的工厂函数
 */
data class ProviderPresetInfo(
    val name: String,
    val description: String,
    val recommendedTimeout: Long,
    val factory: (String) -> AiProvider
)
