package com.empathy.ai.domain.model

import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * AI军师错误类型
 *
 * 用于统一错误处理和UI展示，支持错误分类和重试判断。
 * 密封类设计确保错误类型的完整性和类型安全。
 *
 * 业务背景 (PRD-00026/验收标准):
 * - 错误处理友好，有明确提示
 * - 用户可选择重试或配置AI服务商
 * - 网络错误/配置错误/超时等都有明确的用户提示
 *
 * 设计决策 (TDD-00026/13.1):
 * - 使用sealed class实现代数数据类型，穷尽性检查
 * - isRetryable字段指导UI显示"重试"或"去配置"按钮
 * - 默认消息提供友好的用户提示
 * - fromException工厂方法支持异常到错误类型的自动转换
 *
 * 任务追踪 (FD-00026/T003): 数据层实现 - 错误类型定义
 *
 * @property message 错误消息，用于UI展示
 * @property isRetryable 是否可重试，决定UI按钮类型
 */
sealed class AiAdvisorError(
    val message: String,
    val isRetryable: Boolean
) {
    /**
     * 网络错误
     * - 原因: 无网络连接、DNS解析失败、网络切换等
     * - 处理: 提示用户检查网络，重试可恢复
     */
    class NetworkError(
        message: String = "网络连接失败，请检查网络"
    ) : AiAdvisorError(message, isRetryable = true)

    /**
     * API错误
     * - 原因: AI服务返回错误、API限流、认证失败等
     * - 处理: 提示用户稍后重试，或检查API配置
     */
    class ApiError(
        message: String = "AI暂时无法回复，请稍后重试"
    ) : AiAdvisorError(message, isRetryable = true)

    /**
     * 配置错误
     * - 原因: 未配置AI服务商、API Key无效等
     * - 处理: 引导用户去设置页面配置，不可重试
     */
    class ConfigError(
        message: String = "请先配置AI服务商"
    ) : AiAdvisorError(message, isRetryable = false)

    /**
     * 超时错误
     * - 原因: AI响应时间过长、网络延迟等
     * - 处理: 提示用户重试，可能需要降低maxTokens
     */
    class TimeoutError(
        message: String = "AI响应超时，请重试"
    ) : AiAdvisorError(message, isRetryable = true)

    /**
     * 数据库错误
     * - 原因: 数据保存失败、数据库写入超时等
     * - 处理: 提示用户重试，检查存储空间
     */
    class DatabaseError(
        message: String = "数据保存失败，请重试"
    ) : AiAdvisorError(message, isRetryable = true)

    /**
     * 联系人不存在
     * - 原因: contactId无效或已被删除
     * - 处理: 返回上一页，不可重试
     */
    class ContactNotFoundError(
        message: String = "联系人不存在"
    ) : AiAdvisorError(message, isRetryable = false)

    /**
     * 未知错误
     * - 兜底类型，捕获未预见的异常
     * - 处理: 提示通用错误信息，保留日志供分析
     */
    class UnknownError(
        message: String = "发生未知错误"
    ) : AiAdvisorError(message, isRetryable = true)

    companion object {
        /**
         * 从异常创建错误类型
         *
         * 根据异常类型自动映射到对应的错误类型，
         * 支持Throwable类型的任意异常。
         *
         * 映射规则:
         * - UnknownHostException -> NetworkError (DNS解析失败)
         * - SocketTimeoutException -> TimeoutError (连接超时)
         * - 消息含"API" -> ApiError
         * - 消息含"配置" -> ConfigError
         * - 消息含"网络" -> NetworkError
         * - 其他 -> UnknownError
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
