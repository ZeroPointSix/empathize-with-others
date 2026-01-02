package com.empathy.ai.data.util

import com.empathy.ai.domain.model.AppError
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/**
 * API错误处理器
 *
 * 统一处理网络请求中的各种错误，转换为用户友好的错误信息
 *
 * 支持的错误类型：
 * - 网络错误（无网络、DNS解析失败）
 * - 超时错误
 * - SSL/TLS错误
 * - HTTP错误（401、403、404、429、500等）
 * - 代理错误
 * - 认证错误
 * - 频率限制错误
 *
 * @see TD-00025 T5-07: 统一错误处理逻辑
 */
object ApiErrorHandler {

    /**
     * 处理异常并返回AppError
     *
     * @param exception 原始异常
     * @param context 错误上下文（如"连接测试"、"获取模型列表"）
     * @return AppError 统一的错误对象
     */
    fun handleException(exception: Throwable, context: String = ""): AppError {
        val contextPrefix = if (context.isNotBlank()) "$context: " else ""
        
        return when (exception) {
            // 网络相关错误
            is UnknownHostException -> AppError.NetworkError(
                message = "${contextPrefix}无法连接到服务器，请检查网络连接或服务器地址",
                cause = exception
            )
            
            is SocketTimeoutException -> AppError.NetworkError(
                message = "${contextPrefix}连接超时，请检查网络状况或稍后重试",
                cause = exception
            )
            
            is SSLException -> AppError.NetworkError(
                message = "${contextPrefix}SSL/TLS连接失败，请检查证书配置或代理设置",
                cause = exception
            )
            
            is IOException -> handleIOException(exception, contextPrefix)
            
            // HTTP错误
            is HttpException -> handleHttpException(exception, contextPrefix)
            
            // 其他错误
            else -> AppError.UnknownError(
                message = "${contextPrefix}${exception.message ?: "未知错误"}",
                cause = exception
            )
        }
    }

    /**
     * 处理IO异常
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
     * 处理HTTP异常
     */
    private fun handleHttpException(exception: HttpException, contextPrefix: String): AppError {
        val code = exception.code()
        val errorBody = try {
            exception.response()?.errorBody()?.string()
        } catch (e: Exception) {
            null
        }
        
        return when (code) {
            // 认证错误
            401 -> AppError.AuthenticationError(
                message = "${contextPrefix}API密钥无效或已过期，请检查密钥配置",
                cause = exception
            )
            
            // 权限错误
            403 -> AppError.AuthorizationError(
                message = "${contextPrefix}没有访问权限，请检查API密钥权限",
                cause = exception
            )
            
            // 资源不存在
            404 -> AppError.NotFoundError(
                message = "${contextPrefix}请求的资源不存在，请检查API端点",
                cause = exception
            )
            
            // 频率限制
            429 -> {
                val retryAfter = extractRetryAfter(errorBody)
                AppError.RateLimitError(
                    message = "${contextPrefix}请求过于频繁，请${retryAfter}后重试",
                    retryAfterSeconds = parseRetryAfterSeconds(retryAfter),
                    cause = exception
                )
            }
            
            // 服务器错误
            in 500..599 -> AppError.ServerError(
                message = "${contextPrefix}服务器错误 ($code)，请稍后重试",
                cause = exception
            )
            
            // 代理认证错误
            407 -> AppError.ProxyAuthError(
                message = "${contextPrefix}代理认证失败，请检查代理用户名和密码",
                cause = exception
            )
            
            // 其他HTTP错误
            else -> AppError.HttpError(
                code = code,
                message = "${contextPrefix}HTTP错误 $code: ${extractErrorMessage(errorBody)}",
                cause = exception
            )
        }
    }

    /**
     * 从错误响应中提取重试时间
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
     */
    private fun formatRetryTime(seconds: Int): String {
        return when {
            seconds >= 60 -> "${seconds / 60}分钟"
            else -> "${seconds}秒"
        }
    }

    /**
     * 从错误响应中提取错误消息
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
     * @param error AppError对象
     * @return 用户友好的错误消息
     */
    fun getUserFriendlyMessage(error: AppError): String {
        return error.message
    }

    /**
     * 判断错误是否可重试
     *
     * @param error AppError对象
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
     * 获取建议的重试延迟（毫秒）
     *
     * @param error AppError对象
     * @param attempt 当前重试次数
     * @return 建议的重试延迟（毫秒）
     */
    fun getRetryDelay(error: AppError, attempt: Int): Long {
        val baseDelay = when (error) {
            is AppError.RateLimitError -> error.retryAfterSeconds * 1000L
            is AppError.ServerError -> 5000L
            else -> 1000L
        }
        
        // 指数退避
        return baseDelay * (1 shl attempt.coerceAtMost(4))
    }
}
