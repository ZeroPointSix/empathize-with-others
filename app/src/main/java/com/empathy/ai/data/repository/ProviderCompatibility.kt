package com.empathy.ai.data.repository

import com.empathy.ai.domain.model.AiProvider

/**
 * AI 服务商兼容性配置
 *
 * 不同的 AI 服务商对 response_format 和 Function Calling 的支持不同，
 * 需要根据 Provider 动态调整约束策略。
 *
 * 兼容性矩阵（更新版）：
 * ┌─────────────┬──────────────────┬──────────────────┬──────────────────┬─────────────────────────────────┐
 * │ Provider    │ response_format   │ tools            │ tool_choice      │ 备注                            │
 * ├─────────────┼──────────────────┼──────────────────┼──────────────────┼─────────────────────────────────┤
 * │ OpenAI      │ ✅ 完全支持      │ ✅ 完全支持      │ ✅ 完全支持      │ 标准 OpenAI API                 │
 * │ DeepSeek    │ ✅ 完全支持      │ ✅ 完全支持      │ ✅ 完全支持      │ 官方 API 兼容 OpenAI            │
 * │ Gemini      │ ⚠️ 部分支持      │ ✅ 完全支持      │ ✅ 完全支持      │ 需要特殊配置                    │
 * │ 魔搭        │ ⚠️ 条件支持      │ ⚠️ 部分支持      │ ❌ 不支持        │ DeepSeek 模型不支持 tool_choice │
 * │ Claude      │ ❌ 不支持        │ ✅ 完全支持      │ ✅ 完全支持      │ 使用提示词约束                  │
 * └─────────────┴──────────────────┴──────────────────┴──────────────────┴─────────────────────────────────┘
 * 
 * 重要发现：
 * - 魔搭平台托管的 DeepSeek v3.1 模型不支持 tool_choice 参数
 * - 需要区分 "支持 tools" 和 "支持 tool_choice"
 * - 对于不支持 tool_choice 的模型，应回退到 response_format 策略
 */
object ProviderCompatibility {
    
    /**
     * 是否支持 response_format 字段
     *
     * @param provider AI 服务商配置
     * @return true 表示支持，false 表示不支持
     */
    fun supportsResponseFormat(provider: AiProvider): Boolean {
        return when {
            provider.name.contains("OpenAI", ignoreCase = true) -> true
            provider.name.contains("DeepSeek", ignoreCase = true) -> true
            provider.name.contains("Gemini", ignoreCase = true) -> true
            provider.name.contains("魔搭", ignoreCase = true) -> true  // 条件支持
            provider.name.contains("Claude", ignoreCase = true) -> false
            else -> true  // 默认支持
        }
    }
    
    /**
     * 是否需要在 messages 中包含 "json" 关键词
     *
     * 某些服务商（如魔搭）要求在 messages 中明确包含 "json" 关键词才能使用 response_format
     *
     * @param provider AI 服务商配置
     * @return true 表示需要，false 表示不需要
     */
    fun requiresJsonKeywordInMessages(provider: AiProvider): Boolean {
        return when {
            provider.name.contains("魔搭", ignoreCase = true) -> true
            else -> false
        }
    }
    
    /**
     * 获取 Provider 的系统指令模板
     *
     * 不同的 Provider 可能需要不同的系统指令格式
     *
     * @param provider AI 服务商配置
     * @param baseInstruction 基础指令
     * @return 适配后的系统指令
     */
    fun adaptSystemInstruction(provider: AiProvider, baseInstruction: String): String {
        return when {
            provider.name.contains("魔搭", ignoreCase = true) -> {
                // 魔搭需要在指令中明确包含 "json" 关键词
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
     *
     * @param provider AI 服务商配置
     * @return 超时时间（毫秒）
     */
    fun getRecommendedTimeout(provider: AiProvider): Long {
        return when {
            provider.name.contains("Gemini", ignoreCase = true) -> 40000L  // Gemini 较慢
            provider.name.contains("DeepSeek", ignoreCase = true) -> 25000L
            provider.name.contains("OpenAI", ignoreCase = true) -> 20000L
            provider.name.contains("魔搭", ignoreCase = true) -> 20000L
            else -> 20000L  // 默认 20 秒
        }
    }
    
    /**
     * 是否支持 Function Calling (tools 参数)
     *
     * 注意：支持 tools 不代表支持 tool_choice，需要分开检测
     *
     * @param provider AI 服务商配置
     * @return true 表示支持，false 表示不支持
     */
    fun supportsFunctionCalling(provider: AiProvider): Boolean {
        return when {
            provider.name.contains("OpenAI", ignoreCase = true) -> true
            provider.name.contains("DeepSeek", ignoreCase = true) -> true
            provider.name.contains("Gemini", ignoreCase = true) -> true
            provider.name.contains("Claude", ignoreCase = true) -> true
            // 魔搭平台：部分模型支持，但 tool_choice 受限
            provider.name.contains("魔搭", ignoreCase = true) -> false  // 改为 false，因为 tool_choice 不支持
            // 检查模型名称
            provider.defaultModelId.contains("gpt", ignoreCase = true) -> true
            else -> false  // 默认不支持，使用传统方式
        }
    }
    
    /**
     * 是否支持 tool_choice 参数
     *
     * tool_choice 用于强制模型调用指定的函数。
     * 某些平台（如魔搭）的某些模型（如 DeepSeek v3.1）不支持此参数。
     *
     * @param provider AI 服务商配置
     * @return true 表示支持，false 表示不支持
     */
    fun supportsToolChoice(provider: AiProvider): Boolean {
        // 魔搭平台的 DeepSeek 模型不支持 tool_choice
        if (provider.name.contains("魔搭", ignoreCase = true)) {
            return false
        }
        
        // 通过魔搭调用的 DeepSeek 模型
        if (provider.baseUrl.contains("dashscope", ignoreCase = true) &&
            provider.defaultModelId.contains("deepseek", ignoreCase = true)) {
            return false
        }
        
        // 其他情况跟随 supportsFunctionCalling
        return supportsFunctionCalling(provider)
    }
    
    /**
     * 获取推荐的结构化输出策略
     *
     * 策略选择逻辑（按优先级）：
     * 1. 如果支持完整的 Function Calling（含 tool_choice）→ FUNCTION_CALLING
     * 2. 如果支持 response_format → RESPONSE_FORMAT
     * 3. 否则 → PROMPT_ONLY
     *
     * @param provider AI 服务商配置
     * @return 推荐策略
     */
    fun getStructuredOutputStrategy(provider: AiProvider): StructuredOutputStrategy {
        return when {
            // 只有同时支持 tools 和 tool_choice 才使用 Function Calling
            supportsFunctionCalling(provider) && supportsToolChoice(provider) -> 
                StructuredOutputStrategy.FUNCTION_CALLING
            // 其次使用 response_format
            supportsResponseFormat(provider) -> StructuredOutputStrategy.RESPONSE_FORMAT
            // 最后使用提示词约束
            else -> StructuredOutputStrategy.PROMPT_ONLY
        }
    }
    
    /**
     * 结构化输出策略枚举
     */
    enum class StructuredOutputStrategy {
        /** 使用 Function Calling（最可靠，需要 tools + tool_choice 都支持） */
        FUNCTION_CALLING,
        /** 使用 response_format 字段 */
        RESPONSE_FORMAT,
        /** 仅使用提示词约束（最不可靠） */
        PROMPT_ONLY
    }
}
