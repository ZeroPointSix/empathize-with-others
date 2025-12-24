package com.empathy.ai.domain.model

/**
 * 总结错误密封类
 *
 * 定义手动总结过程中可能出现的各种错误类型
 *
 * @property code 错误码
 * @property userMessage 用户友好的错误提示
 */
sealed class SummaryError(
    val code: String,
    val userMessage: String
) {
    /**
     * 无对话数据
     */
    object NoConversations : SummaryError(
        code = "E001",
        userMessage = "选定时间段内没有对话记录"
    )

    /**
     * 网络错误
     */
    object NetworkError : SummaryError(
        code = "E002",
        userMessage = "网络连接失败，请检查网络"
    )

    /**
     * AI服务不可用
     */
    object ApiError : SummaryError(
        code = "E003",
        userMessage = "AI服务暂时不可用"
    )

    /**
     * API配额不足
     */
    object QuotaExceeded : SummaryError(
        code = "E004",
        userMessage = "API调用次数已用完"
    )

    /**
     * 请求超时
     */
    object Timeout : SummaryError(
        code = "E005",
        userMessage = "处理超时，请稍后重试"
    )

    /**
     * 数据库错误
     */
    object DatabaseError : SummaryError(
        code = "E006",
        userMessage = "保存失败，请重试"
    )

    /**
     * 用户取消
     */
    object Cancelled : SummaryError(
        code = "E007",
        userMessage = "已取消"
    )

    /**
     * 未知错误
     *
     * 使用场景：
     * 1. 捕获到未预期的异常类型时，将异常信息包装为Unknown错误
     * 2. 第三方库抛出的非标准异常
     * 3. 系统级错误（如OOM、SecurityException等）
     *
     * 处理方式：
     * - 记录详细日志用于问题排查
     * - 向用户显示通用错误提示
     * - 默认不可重试，需要用户手动操作
     *
     * @param message 原始异常信息，用于日志记录和调试
     */
    data class Unknown(val message: String) : SummaryError(
        code = "E999",
        userMessage = "操作失败，请稍后重试"
    ) {
        /**
         * 获取调试信息（仅用于日志）
         */
        fun getDebugMessage(): String = "Unknown error: $message"
    }

    /**
     * 是否可重试
     *
     * 网络错误、超时和API错误通常是临时性的，可以重试
     */
    fun isRetryable(): Boolean = this in listOf(NetworkError, Timeout, ApiError)

    /**
     * 获取建议操作
     *
     * 根据错误类型提供用户可执行的建议
     */
    fun getSuggestedAction(): String = when (this) {
        is NoConversations -> "请选择包含对话记录的日期范围"
        is NetworkError -> "请检查网络连接后重试"
        is ApiError -> "请稍后重试，或检查AI服务配置"
        is QuotaExceeded -> "请检查API配额或更换服务商"
        is Timeout -> "请稍后重试"
        is DatabaseError -> "请重试，如问题持续请重启应用"
        is Cancelled -> "操作已取消"
        is Unknown -> "请重试，如问题持续请联系支持"
    }
}
