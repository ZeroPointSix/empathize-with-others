package com.empathy.ai.domain.model

/**
 * AI 服务商
 *
 * 表示一个 AI 服务提供商的完整配置。
 * 支持多服务商管理和模型配置。
 *
 * 业务背景 (PRD-00025):
 * - 支持用户配置多个AI服务商（OpenAI、DeepSeek等）
 * - 每个服务商支持多个模型选择
 * - 提供商配置是使用AI功能的前提条件
 * - AI军师功能需要默认提供商才能正常工作（PRD-00026）
 *
 * 设计决策 (TDD-00025):
 * - 分离temperature和maxTokens控制生成质量
 * - timeoutMs针对不同服务商差异化配置
 * - isDefault标记支持多服务商切换
 * - 安全方法（getSafeTemperature/getSafeMaxTokens）提供边界保护
 *
 * 任务追踪: FD-00025/AI配置功能设计
 *
 * @property id 唯一标识（UUID）
 * @property name 显示名称（例如: "OpenAI", "DeepSeek"）
 * @property baseUrl API 端点（例如: "https://api.openai.com/v1"）
 * @property apiKey API Key（内存中明文，存储时加密）
 * @property models 可用模型列表
 * @property defaultModelId 默认模型 ID
 * @property isDefault 是否为默认服务商
 * @property timeoutMs 请求超时时间（毫秒），不同服务商响应速度不同
 * @property temperature 生成温度（0.0-2.0），控制输出的随机性
 * @property maxTokens 最大Token数（1-128000），限制输出长度
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
    val temperature: Float = DEFAULT_TEMPERATURE,
    val maxTokens: Int = DEFAULT_MAX_TOKENS,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        /** Temperature 最小值 */
        const val TEMPERATURE_MIN = 0.0f
        /** Temperature 最大值 */
        const val TEMPERATURE_MAX = 2.0f
        /** Temperature 默认值 */
        const val DEFAULT_TEMPERATURE = 0.7f
        
        /** MaxTokens 最小值 */
        const val MAX_TOKENS_MIN = 1
        /** MaxTokens 最大值 */
        const val MAX_TOKENS_MAX = 128000
        /** MaxTokens 默认值 */
        const val DEFAULT_MAX_TOKENS = 4096
    }

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

    /**
     * 验证 Temperature 是否在有效范围内
     */
    fun isTemperatureValid(): Boolean {
        return temperature in TEMPERATURE_MIN..TEMPERATURE_MAX
    }

    /**
     * 验证 MaxTokens 是否在有效范围内
     */
    fun isMaxTokensValid(): Boolean {
        return maxTokens in MAX_TOKENS_MIN..MAX_TOKENS_MAX
    }

    /**
     * 获取安全的 Temperature 值（边界保护）
     */
    fun getSafeTemperature(): Float {
        return temperature.coerceIn(TEMPERATURE_MIN, TEMPERATURE_MAX)
    }

    /**
     * 获取安全的 MaxTokens 值（边界保护）
     */
    fun getSafeMaxTokens(): Int {
        return maxTokens.coerceIn(MAX_TOKENS_MIN, MAX_TOKENS_MAX)
    }
}
