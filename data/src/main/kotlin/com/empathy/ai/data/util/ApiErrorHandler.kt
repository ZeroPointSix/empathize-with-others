package com.empathy.ai.data.util

import com.empathy.ai.domain.model.AppError
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/**
 * ApiErrorHandler 实现了API错误的统一处理和转换 (TD-00025/T5-07)
 *
 * 业务价值：将技术错误转换为用户友好的错误信息
 *
 * 为什么需要统一错误处理？
 * - 网络错误类型繁多（IOException子类就有十几种）
 * - 不同HTTP状态码需要不同提示
 * - 技术错误消息对用户无意义
 * - 需要区分"可重试"和"不可重试"错误
 *
 * 设计决策：
 * - 多级 when 判断：清晰区分错误类型，便于扩展
 * - 提取错误消息：避免暴露敏感技术细节
 * - 上下文前缀：帮助用户定位问题来源
 *
 * 错误分类：
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │ 分类               │ 错误类型                          │ 用户提示              │
 * ├─────────────────────────────────────────────────────────────────────────────┤
 * │ 网络错误           │ UnknownHostException, SSL...      │ 检查网络连接          │
 * │ HTTP客户端错误     │ 401认证, 403权限, 404找不到       │ 检查配置              │
 * │ HTTP服务端错误     │ 500-599                          │ 稍后重试              │
 * │ 限流错误           │ 429                              │ 等待后重试            │
 * │ 代理错误           │ 407, SOCKS失败                   │ 检查代理配置          │
 * └─────────────────────────────────────────────────────────────────────────────┘
 *
 * @see TD-00025 T5-07: 统一错误处理逻辑
 */
object ApiErrorHandler {

    /**
     * 处理异常并返回用户友好的 AppError
     *
     * 【处理流程】
     * 1. 识别异常类型（UnknownHost, SocketTimeout, HttpException...）
     * 2. 转换为对应的 AppError 子类
     * 3. 添加上下文前缀（如"连接测试: "）
     * 4. 保留原始异常作为 cause
     *
     * 【设计权衡】
     * - 为什么用 when 而非 if-else？→ 类型安全，编译器检查所有分支
     * - 为什么保留 cause？→ 便于调试和日志记录
     * - 为什么需要 context 参数？→ 帮助用户定位问题来源
     *
     * @param exception 原始异常（可能是 IOException, HttpException 等）
     * @param context 错误上下文（如"连接测试"、"获取模型列表"）
     * @return 统一的 AppError 对象
     */
    fun handleException(exception: Throwable, context: String = ""): AppError {
        val contextPrefix = if (context.isNotBlank()) "$context: " else ""

        return when (exception) {
            // 网络相关错误（DNS解析失败、无网络等）
            is UnknownHostException -> AppError.NetworkError(
                message = "${contextPrefix}无法连接到服务器，请检查网络连接或服务器地址",
                cause = exception
            )

            // 超时错误
            is SocketTimeoutException -> AppError.NetworkError(
                message = "${contextPrefix}连接超时，请检查网络状况或稍后重试",
                cause = exception
            )

            // SSL/TLS证书错误
            is SSLException -> AppError.NetworkError(
                message = "${contextPrefix}SSL/TLS连接失败，请检查证书配置或代理设置",
                cause = exception
            )

            // 其他 IO 错误（代理、连接拒绝等）
            is IOException -> handleIOException(exception, contextPrefix)

            // HTTP 错误（4xx, 5xx 状态码）
            is HttpException -> handleHttpException(exception, contextPrefix)

            // 兜底：未知错误
            else -> AppError.UnknownError(
                message = "${contextPrefix}${exception.message ?: "未知错误"}",
                cause = exception
            )
        }
    }

    /**
     * 处理 IO 异常（TD-00025/T5-07）
     *
     * 【错误模式匹配】
     * - "proxy"/"SOCKS" → 代理配置问题
     * - "Connection refused" → 服务器拒绝连接
     * - "Connection reset" → 连接被重置
     * - "Canceled" → 用户取消请求
     *
     * @param exception IO 异常
     * @param contextPrefix 错误上下文前缀
     * @return 对应的 AppError
     */
    private fun handleIOException(exception: IOException, contextPrefix: String): AppError {
        val message = exception.message ?: ""

        return when {
            // 代理相关错误
            message.contains("proxy", ignoreCase = true) ||
            message.contains("SOCKS", ignoreCase = true) -> AppError.ProxyError(
                message = "${contextPrefix}代理连接失败，请检查代理配置",
                cause = exception
            )

            // 连接被拒绝
            message.contains("Connection refused", ignoreCase = true) -> AppError.NetworkError(
                message = "${contextPrefix}连接被拒绝，请检查服务器地址和端口",
                cause = exception
            )

            // 连接重置
            message.contains("Connection reset", ignoreCase = true) -> AppError.NetworkError(
                message = "${contextPrefix}连接被重置，请稍后重试",
                cause = exception
            )

            // 请求被取消
            message.contains("Canceled", ignoreCase = true) -> AppError.CancelledError(
                message = "${contextPrefix}请求已取消",
                cause = exception
            )

            // 默认网络错误
            else -> AppError.NetworkError(
                message = "${contextPrefix}网络错误: ${exception.message}",
                cause = exception
            )
        }
    }

    /**
     * 处理 HTTP 异常（TD-00025/T5-07）
     *
     * 【HTTP状态码处理】
     * - 401 → API密钥无效或过期
     * - 403 → 无访问权限
     * - 404 → 资源不存在
     * - 429 → 频率限制（提取重试时间）
     * - 500-599 → 服务器错误
     * - 407 → 代理认证失败
     *
     * @param exception HTTP 异常
     * @param contextPrefix 错误上下文前缀
     * @return 对应的 AppError
     */
    private fun handleHttpException(exception: HttpException, contextPrefix: String): AppError {
        val code = exception.code()
        val errorBody = try {
            exception.response()?.errorBody()?.string()
        } catch (e: Exception) {
            null
        }

        return when (code) {
            // 401: 认证错误（API密钥问题）
            401 -> AppError.AuthenticationError(
                message = "${contextPrefix}API密钥无效或已过期，请检查密钥配置",
                cause = exception
            )

            // 403: 权限错误（密钥无权限访问）
            403 -> AppError.AuthorizationError(
                message = "${contextPrefix}没有访问权限，请检查API密钥权限",
                cause = exception
            )

            // 404: 资源不存在
            404 -> AppError.NotFoundError(
                message = "${contextPrefix}请求的资源不存在，请检查API端点",
                cause = exception
            )

            // 429: 频率限制（Rate Limit）
            429 -> {
                val retryAfter = extractRetryAfter(errorBody)
                AppError.RateLimitError(
                    message = "${contextPrefix}请求过于频繁，请${retryAfter}后重试",
                    retryAfterSeconds = parseRetryAfterSeconds(retryAfter),
                    cause = exception
                )
            }

            // 500-599: 服务器错误
            in 500..599 -> AppError.ServerError(
                message = "${contextPrefix}服务器错误 ($code)，请稍后重试",
                cause = exception
            )

            // 407: 代理认证错误
            407 -> AppError.ProxyAuthError(
                message = "${contextPrefix}代理认证失败，请检查代理用户名和密码",
                cause = exception
            )

            // 其他 HTTP 错误
            else -> AppError.HttpError(
                code = code,
                message = "${contextPrefix}HTTP错误 $code: ${extractErrorMessage(errorBody)}",
                cause = exception
            )
        }
    }

    /**
     * 从错误响应中提取重试时间（TD-00025/T5-07）
     *
     * 【提取逻辑】
     * - 尝试匹配 JSON 中的 "retry_after" 字段
     * - 如果匹配成功，格式化为用户友好的时间
     *
     * @param errorBody 错误响应体
     * @return 格式化的时间字符串（如"30秒"、"2分钟"）
     */
    private fun extractRetryAfter(errorBody: String?): String {
        if (errorBody == null) return "稍后"

        // 尝试从JSON中提取
        val retryAfterRegex = """"retry_after":\s*(\d+)""".toRegex()
        val match = retryAfterRegex.find(errorBody)

        return if (match != null) {
            val seconds = match.groupValues[1].toIntOrNull() ?: 60
            formatRetryTime(seconds)
        } else {
            "稍后"
        }
    }

    /**
     * 解析重试时间（秒）
     *
     * @param retryAfter 格式化的重试时间字符串
     * @return 秒数
     */
    private fun parseRetryAfterSeconds(retryAfter: String): Int {
        return when {
            retryAfter.contains("分钟") -> {
                val minutes = retryAfter.filter { it.isDigit() }.toIntOrNull() ?: 1
                minutes * 60
            }
            retryAfter.contains("秒") -> {
                retryAfter.filter { it.isDigit() }.toIntOrNull() ?: 60
            }
            else -> 60
        }
    }

    /**
     * 格式化重试时间
     *
     * @param seconds 秒数
     * @return 格式化的字符串
     */
    private fun formatRetryTime(seconds: Int): String {
        return when {
            seconds >= 60 -> "${seconds / 60}分钟"
            else -> "${seconds}秒"
        }
    }

    /**
     * 从错误响应中提取错误消息
     *
     * 【提取逻辑】
     * - 尝试匹配 JSON 中的 "message" 字段
     * - 如果匹配失败，返回响应体前100字符
     *
     * @param errorBody 错误响应体
     * @return 提取的错误消息
     */
    private fun extractErrorMessage(errorBody: String?): String {
        if (errorBody == null) return "未知错误"

        // 尝试从JSON中提取message字段
        val messageRegex = """"message":\s*"([^"]+)"""".toRegex()
        val match = messageRegex.find(errorBody)

        return match?.groupValues?.get(1) ?: errorBody.take(100)
    }

    /**
     * 获取用户友好的错误消息
     *
     * @param error AppError 对象
     * @return 用户友好的错误消息
     */
    fun getUserFriendlyMessage(error: AppError): String {
        return error.message
    }

    /**
     * 判断错误是否可重试 (TD-00025/T5-07)
     *
     * 【重试策略】
     * - 网络错误：可重试（可能是临时网络问题）
     * - 限流错误：可重试（等待后重试）
     * - 服务器错误：可重试（服务器可能临时故障）
     * - 认证/权限错误：不可重试（重试也无用）
     * - 资源不存在：不可重试（重试也找不到）
     * - 用户取消：不可重试（用户不想继续）
     *
     * @param error AppError 对象
     * @return 是否可重试
     */
    fun isRetryable(error: AppError): Boolean {
        return when (error) {
            is AppError.NetworkError -> true
            is AppError.RateLimitError -> true
            is AppError.ServerError -> true
            is AppError.CancelledError -> false
            is AppError.AuthenticationError -> false
            is AppError.AuthorizationError -> false
            is AppError.NotFoundError -> false
            is AppError.ProxyError -> true
            is AppError.ProxyAuthError -> false
            else -> false
        }
    }

    /**
     * 获取建议的重试延迟（毫秒）- 指数退避策略
     *
     * 【指数退避】
     * - baseDelay * 2^attempt（最多2^4=16倍）
     * - 避免短时间内大量重试请求
     * - 让服务器有恢复时间
     *
     * 【延迟计算】
     * - 限流错误：使用服务器返回的 retryAfterSeconds
     * - 服务器错误：基础延迟5秒
     * - 其他错误：基础延迟1秒
     *
     * @param error AppError 对象
     * @param attempt 当前重试次数（从0开始）
     * @return 建议的重试延迟（毫秒）
     */
    fun getRetryDelay(error: AppError, attempt: Int): Long {
        val baseDelay = when (error) {
            is AppError.RateLimitError -> error.retryAfterSeconds * 1000L
            is AppError.ServerError -> 5000L
            else -> 1000L
        }

        // 指数退避：1, 2, 4, 8, 16 倍（最多4次）
        return baseDelay * (1 shl attempt.coerceAtMost(4))
    }
}
