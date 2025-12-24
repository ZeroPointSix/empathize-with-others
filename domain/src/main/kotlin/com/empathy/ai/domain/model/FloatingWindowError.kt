package com.empathy.ai.domain.model

/**
 * 悬浮窗错误类型
 *
 * 定义悬浮窗服务可能遇到的各种错误情况
 *
 * @property userMessage 用户友好的错误信息
 *
 * @see PRD-00009 悬浮窗功能重构需求
 * @see TDD-00009 悬浮窗功能重构技术设计
 */
sealed class FloatingWindowError(
    val userMessage: String
) : Exception(userMessage) {

    /**
     * 权限被拒绝
     */
    object PermissionDenied : FloatingWindowError(
        userMessage = "需要悬浮窗权限才能使用此功能"
    )

    /**
     * 服务启动失败
     *
     * @property reason 失败原因
     */
    data class ServiceError(
        val reason: String
    ) : FloatingWindowError(
        userMessage = "服务启动失败：$reason"
    )

    /**
     * 输入验证错误
     *
     * @property field 验证失败的字段
     */
    data class ValidationError(
        val field: String
    ) : FloatingWindowError(
        userMessage = when (field) {
            "contact" -> "请选择联系人"
            "text" -> "请输入内容"
            "textLength" -> "输入内容不能超过 5000 字符"
            else -> "输入验证失败"
        }
    )

    /**
     * UseCase 执行错误
     *
     * @property error 原始异常
     */
    data class UseCaseError(
        val error: Throwable
    ) : FloatingWindowError(
        userMessage = "操作失败：${error.message ?: "未知错误"}"
    )

    // ==================== 新增错误类型（TD-00009） ====================

    /**
     * 未配置AI服务商
     */
    object NoProviderConfigured : FloatingWindowError(
        userMessage = "请先配置AI服务商"
    )

    /**
     * 联系人未找到
     *
     * @property contactId 联系人ID
     */
    data class ContactNotFound(
        val contactId: String
    ) : FloatingWindowError(
        userMessage = "联系人不存在或已删除"
    )

    /**
     * AI调用失败
     *
     * @property reason 失败原因
     */
    data class AiCallFailed(
        val reason: String
    ) : FloatingWindowError(
        userMessage = "AI调用失败：$reason"
    )

    /**
     * 解析失败
     *
     * @property reason 失败原因
     */
    data class ParseFailed(
        val reason: String
    ) : FloatingWindowError(
        userMessage = "结果解析失败，请重试"
    )

    /**
     * 网络超时
     */
    object NetworkTimeout : FloatingWindowError(
        userMessage = "网络超时，请检查网络连接后重试"
    )

    /**
     * 输入为空
     */
    object EmptyInput : FloatingWindowError(
        userMessage = "请输入内容"
    )

    /**
     * 未选择联系人
     */
    object NoContactSelected : FloatingWindowError(
        userMessage = "请选择联系人"
    )

    /**
     * 是否可重试
     *
     * @return true 如果该错误可以通过重试解决
     */
    fun isRetryable(): Boolean = when (this) {
        is NetworkTimeout -> true
        is AiCallFailed -> true
        is ParseFailed -> true
        else -> false
    }

    companion object {
        /**
         * 从异常创建错误
         *
         * @param throwable 原始异常
         * @return 对应的 FloatingWindowError
         */
        fun fromThrowable(throwable: Throwable): FloatingWindowError {
            return when {
                throwable is FloatingWindowError -> throwable
                throwable.message?.contains("timeout", ignoreCase = true) == true -> NetworkTimeout
                throwable.message?.contains("未配置", ignoreCase = true) == true -> NoProviderConfigured
                throwable.message?.contains("未找到联系人", ignoreCase = true) == true ->
                    ContactNotFound("")
                else -> UseCaseError(throwable)
            }
        }
    }
}
