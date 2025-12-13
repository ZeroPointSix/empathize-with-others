package com.empathy.ai.domain.model

/**
 * AI 服务商
 *
 * 表示一个 AI 服务提供商的完整配置
 *
 * @property id 唯一标识（UUID）
 * @property name 显示名称（例如: "OpenAI", "DeepSeek"）
 * @property baseUrl API 端点（例如: "https://api.openai.com/v1"）
 * @property apiKey API Key（内存中明文，存储时加密）
 * @property models 可用模型列表
 * @property defaultModelId 默认模型 ID
 * @property isDefault 是否为默认服务商
 * @property timeoutMs 请求超时时间（毫秒），不同服务商响应速度不同
 * @property createdAt 创建时间戳
 */
data class AiProvider(
    val id: String,
    val name: String,
    val baseUrl: String,
    val apiKey: String,
    val models: List<AiModel>,
    val defaultModelId: String,
    val isDefault: Boolean = false,
    val timeoutMs: Long = 30000L,  // 默认 30 秒
    val createdAt: Long = System.currentTimeMillis()
) {
    /**
     * 获取默认模型
     */
    fun getDefaultModel(): AiModel? {
        return models.find { it.id == defaultModelId }
    }
    
    /**
     * 验证配置是否有效
     */
    fun isValid(): Boolean {
        return name.isNotBlank() &&
                baseUrl.isNotBlank() &&
                apiKey.isNotBlank() &&
                models.isNotEmpty() &&
                models.any { it.id == defaultModelId }
    }
}
