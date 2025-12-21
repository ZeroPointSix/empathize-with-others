package com.empathy.ai.domain.util

import javax.inject.Inject
import javax.inject.Singleton

/**
 * 内容验证服务
 *
 * 提供统一的内容验证逻辑，用于事实流内容编辑功能
 * 所有验证方法返回ValidationResult，便于统一处理
 */
@Singleton
class ContentValidator @Inject constructor() {

    companion object {
        // 字符限制常量
        const val MAX_FACT_KEY_LENGTH = 50
        const val MAX_FACT_VALUE_LENGTH = 500
        const val MAX_CONVERSATION_LENGTH = 1000
        const val MAX_SUMMARY_LENGTH = 2000
        const val MAX_CONTACT_NAME_LENGTH = 50
        const val MAX_CONTACT_GOAL_LENGTH = 200
    }

    /**
     * 验证事实键（类型）
     *
     * @param key 事实键
     * @return 验证结果
     */
    fun validateFactKey(key: String): ValidationResult {
        val trimmed = key.trim()
        return when {
            trimmed.isBlank() -> ValidationResult.Error("事实类型不能为空")
            trimmed.length > MAX_FACT_KEY_LENGTH ->
                ValidationResult.Error("事实类型不能超过${MAX_FACT_KEY_LENGTH}字")
            else -> ValidationResult.Valid
        }
    }

    /**
     * 验证事实值（内容）
     *
     * @param value 事实值
     * @return 验证结果
     */
    fun validateFactValue(value: String): ValidationResult {
        val trimmed = value.trim()
        return when {
            trimmed.isBlank() -> ValidationResult.Error("事实内容不能为空")
            trimmed.length > MAX_FACT_VALUE_LENGTH ->
                ValidationResult.Error("事实内容不能超过${MAX_FACT_VALUE_LENGTH}字")
            else -> ValidationResult.Valid
        }
    }

    /**
     * 验证对话内容
     *
     * @param content 对话内容
     * @return 验证结果
     */
    fun validateConversation(content: String): ValidationResult {
        val trimmed = content.trim()
        return when {
            trimmed.isBlank() -> ValidationResult.Error("对话内容不能为空")
            trimmed.length > MAX_CONVERSATION_LENGTH ->
                ValidationResult.Error("对话内容不能超过${MAX_CONVERSATION_LENGTH}字")
            else -> ValidationResult.Valid
        }
    }

    /**
     * 验证总结内容
     *
     * @param content 总结内容
     * @return 验证结果
     */
    fun validateSummary(content: String): ValidationResult {
        val trimmed = content.trim()
        return when {
            trimmed.isBlank() -> ValidationResult.Error("总结内容不能为空")
            trimmed.length > MAX_SUMMARY_LENGTH ->
                ValidationResult.Error("总结内容不能超过${MAX_SUMMARY_LENGTH}字")
            else -> ValidationResult.Valid
        }
    }

    /**
     * 验证联系人姓名
     *
     * @param name 联系人姓名
     * @return 验证结果
     */
    fun validateContactName(name: String): ValidationResult {
        val trimmed = name.trim()
        return when {
            trimmed.isBlank() -> ValidationResult.Error("联系人姓名不能为空")
            trimmed.length > MAX_CONTACT_NAME_LENGTH ->
                ValidationResult.Error("联系人姓名不能超过${MAX_CONTACT_NAME_LENGTH}字")
            else -> ValidationResult.Valid
        }
    }

    /**
     * 验证联系人目标
     *
     * @param goal 联系人目标
     * @return 验证结果
     */
    fun validateContactGoal(goal: String): ValidationResult {
        val trimmed = goal.trim()
        return when {
            trimmed.isBlank() -> ValidationResult.Error("联系人目标不能为空")
            trimmed.length > MAX_CONTACT_GOAL_LENGTH ->
                ValidationResult.Error("联系人目标不能超过${MAX_CONTACT_GOAL_LENGTH}字")
            else -> ValidationResult.Valid
        }
    }

    /**
     * 验证结果密封类
     */
    sealed class ValidationResult {
        /**
         * 验证通过
         */
        object Valid : ValidationResult()

        /**
         * 验证失败
         *
         * @property message 错误消息
         */
        data class Error(val message: String) : ValidationResult()

        /**
         * 是否验证通过
         */
        fun isValid(): Boolean = this is Valid

        /**
         * 获取错误消息
         *
         * @return 错误消息，验证通过时返回null
         */
        fun getErrorMessage(): String? = (this as? Error)?.message
    }
}
