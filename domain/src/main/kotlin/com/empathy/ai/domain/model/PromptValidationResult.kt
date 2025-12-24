package com.empathy.ai.domain.model

/**
 * 提示词验证结果
 *
 * 使用密封类表示验证的三种可能结果：成功、警告、错误
 */
sealed class PromptValidationResult {

    /**
     * 验证通过
     */
    data object Success : PromptValidationResult()

    /**
     * 警告（允许保存，但提示用户）
     *
     * @property message 警告消息
     * @property warningType 警告类型
     */
    data class Warning(
        val message: String,
        val warningType: WarningType
    ) : PromptValidationResult()

    /**
     * 错误（阻止保存）
     *
     * @property message 错误消息
     * @property errorType 错误类型
     */
    data class Error(
        val message: String,
        val errorType: ErrorType
    ) : PromptValidationResult()

    /**
     * 警告类型枚举
     */
    enum class WarningType {
        /** 接近长度限制 */
        NEAR_LENGTH_LIMIT,
        /** 定义了未使用的变量 */
        UNUSED_VARIABLES,
        /** 潜在的注入风险（来自Sanitizer） */
        POTENTIAL_INJECTION
    }

    /**
     * 错误类型枚举
     */
    enum class ErrorType {
        /** 提示词为空 */
        EMPTY_PROMPT,
        /** 超过长度限制 */
        EXCEEDS_LENGTH_LIMIT,
        /** 使用了无效变量 */
        INVALID_VARIABLES,
        /** 格式错误 */
        INVALID_FORMAT
    }
}
