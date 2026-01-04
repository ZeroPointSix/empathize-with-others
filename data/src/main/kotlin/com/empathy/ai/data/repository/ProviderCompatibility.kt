/**
 * Package com.empathy.ai.data.repository 实现了多服务商兼容层
 *
 * 业务背景 (PRD-00025):
 *   - 应用支持多种AI服务商（OpenAI、DeepSeek、魔搭、Claude等）
 *   - 各服务商的API特性差异大，需要动态适配
 *
 * 设计决策 (TDD-00025):
 *   - 使用策略模式封装差异性判断逻辑
 *   - 每个判断方法独立，便于后续新增服务商支持
 *   - 策略优先级：Function Calling > Response Format > Prompt Only
 *
 * 任务追踪:
 *   - TD-00025 - AI配置功能完善
 */
package com.empathy.ai.data.repository

import com.empathy.ai.domain.model.AiProvider

/**
 * AI 服务商兼容性配置
 *
 * 不同的 AI 服务商对 response_format 和 Function Calling 的支持不同，
 * 需要根据 Provider 动态调整约束策略。
 *
 * 设计权衡 (TDD-00025):
 *   - 选择策略模式：避免大量条件判断，便于扩展新服务商
 *   - 每个方法独立测试：降低回归风险
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
     *
     * [Strategy] 多策略结构化输出适配 (TDD-00025/ProviderCompatibility)
     * 不同的AI服务商对JSON结构化输出的支持能力不同：
     *   1. FUNCTION_CALLING - OpenAI/DeepSeek/Gemini (最可靠，强制函数调用)
     *   2. RESPONSE_FORMAT - 大部分支持 response_format 字段
     *   3. PROMPT_ONLY - 仅通过提示词约束（最不可靠，作为兜底）
     * 优先级：Function Calling > Response Format > Prompt Only
     *
     * 业务规则 (PRD-00025):
     *   - 优先使用最可靠的输出方式，确保AI返回可解析的JSON
     *   - Claude/魔搭等不支持 Function Calling 的使用 Response Format
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
