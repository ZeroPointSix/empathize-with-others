package com.empathy.ai.data.repository

import com.empathy.ai.domain.model.AiProvider

/**
 * AI 服务商兼容性配置
 *
 * 不同的 AI 服务商对 response_format 和 Function Calling 的支持不同，
 * 需要根据 Provider 动态调整约束策略。
 */
object ProviderCompatibility {
    
    /**
     * 是否支持 response_format 字段
     */
    fun supportsResponseFormat(provider: AiProvider): Boolean {
        return when {
            provider.name.contains("OpenAI", ignoreCase = true) -> true
            provider.name.contains("DeepSeek", ignoreCase = true) -> true
            provider.name.contains("Gemini", ignoreCase = true) -> true
            provider.name.contains("魔搭", ignoreCase = true) -> true
            provider.name.contains("Claude", ignoreCase = true) -> false
            else -> true
        }
    }
    
    /**
     * 是否需要在 messages 中包含 "json" 关键词
     */
    fun requiresJsonKeywordInMessages(provider: AiProvider): Boolean {
        return when {
            provider.name.contains("魔搭", ignoreCase = true) -> true
            else -> false
        }
    }
    
    /**
     * 获取 Provider 的系统指令模板
     */
    fun adaptSystemInstruction(provider: AiProvider, baseInstruction: String): String {
        return when {
            provider.name.contains("魔搭", ignoreCase = true) -> {
                if (!baseInstruction.contains("json", ignoreCase = true)) {
                    baseInstruction + "\n\n【重要】你必须返回有效的 JSON 格式。"
                } else {
                    baseInstruction
                }
            }
            else -> baseInstruction
        }
    }
    
    /**
     * 获取 Provider 的推荐超时时间（毫秒）
     */
    fun getRecommendedTimeout(provider: AiProvider): Long {
        return when {
            provider.name.contains("Gemini", ignoreCase = true) -> 40000L
            provider.name.contains("DeepSeek", ignoreCase = true) -> 25000L
            provider.name.contains("OpenAI", ignoreCase = true) -> 20000L
            provider.name.contains("魔搭", ignoreCase = true) -> 20000L
            else -> 20000L
        }
    }
    
    /**
     * 是否支持 Function Calling (tools 参数)
     */
    fun supportsFunctionCalling(provider: AiProvider): Boolean {
        return when {
            provider.name.contains("OpenAI", ignoreCase = true) -> true
            provider.name.contains("DeepSeek", ignoreCase = true) -> true
            provider.name.contains("Gemini", ignoreCase = true) -> true
            provider.name.contains("Claude", ignoreCase = true) -> true
            provider.name.contains("魔搭", ignoreCase = true) -> false
            provider.defaultModelId.contains("gpt", ignoreCase = true) -> true
            else -> false
        }
    }
    
    /**
     * 是否支持 tool_choice 参数
     */
    fun supportsToolChoice(provider: AiProvider): Boolean {
        if (provider.name.contains("魔搭", ignoreCase = true)) {
            return false
        }
        
        if (provider.baseUrl.contains("dashscope", ignoreCase = true) &&
            provider.defaultModelId.contains("deepseek", ignoreCase = true)) {
            return false
        }
        
        return supportsFunctionCalling(provider)
    }
    
    /**
     * 获取推荐的结构化输出策略
     */
    fun getStructuredOutputStrategy(provider: AiProvider): StructuredOutputStrategy {
        return when {
            supportsFunctionCalling(provider) && supportsToolChoice(provider) -> 
                StructuredOutputStrategy.FUNCTION_CALLING
            supportsResponseFormat(provider) -> StructuredOutputStrategy.RESPONSE_FORMAT
            else -> StructuredOutputStrategy.PROMPT_ONLY
        }
    }
    
    /**
     * 结构化输出策略枚举
     */
    enum class StructuredOutputStrategy {
        /** 使用 Function Calling（最可靠） */
        FUNCTION_CALLING,
        /** 使用 response_format 字段 */
        RESPONSE_FORMAT,
        /** 仅使用提示词约束（最不可靠） */
        PROMPT_ONLY
    }
}
