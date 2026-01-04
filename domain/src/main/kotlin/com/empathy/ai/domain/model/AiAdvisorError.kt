package com.empathy.ai.domain.model

import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * AI军师错误类型
 *
 * 用于统一错误处理和UI展示，支持错误分类和重试判断。
 *
 * @property message 错误消息
 * @property isRetryable 是否可重试
 */
sealed class AiAdvisorError(
    val message: String,
    val isRetryable: Boolean
) {
    /** 网络错误 */
    class NetworkError(
        message: String = "网络连接失败，请检查网络"
    ) : AiAdvisorError(message, isRetryable = true)

    /** API错误 */
    class ApiError(
        message: String = "AI暂时无法回复，请稍后重试"
    ) : AiAdvisorError(message, isRetryable = true)

    /** 配置错误 */
    class ConfigError(
        message: String = "请先配置AI服务商"
    ) : AiAdvisorError(message, isRetryable = false)

    /** 超时错误 */
    class TimeoutError(
        message: String = "AI响应超时，请重试"
    ) : AiAdvisorError(message, isRetryable = true)

    /** 数据库错误 */
    class DatabaseError(
        message: String = "数据保存失败，请重试"
    ) : AiAdvisorError(message, isRetryable = true)

    /** 联系人不存在 */
    class ContactNotFoundError(
        message: String = "联系人不存在"
    ) : AiAdvisorError(message, isRetryable = false)

    /** 未知错误 */
    class UnknownError(
        message: String = "发生未知错误"
    ) : AiAdvisorError(message, isRetryable = true)

    companion object {
        /**
         * 从异常创建错误类型
         *
         * @param e 异常
         * @return 对应的错误类型
         */
        fun fromException(e: Throwable): AiAdvisorError {
            return when (e) {
                is UnknownHostException -> NetworkError()
                is SocketTimeoutException -> TimeoutError()
                else -> when {
                    e.message?.contains("API", ignoreCase = true) == true -> 
                        ApiError(e.message ?: "API错误")
                    e.message?.contains("配置", ignoreCase = true) == true -> 
                        ConfigError()
                    e.message?.contains("网络", ignoreCase = true) == true -> 
                        NetworkError(e.message ?: "网络错误")
                    else -> UnknownError(e.message ?: "未知错误")
                }
            }
        }
    }
}
