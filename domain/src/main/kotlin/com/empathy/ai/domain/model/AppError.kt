package com.empathy.ai.domain.model

/**
 * 统一的应用错误类型
 *
 * 提供类型安全的错误分类和用户友好的错误消息
 *
 * TD-00025: 扩展网络和代理相关错误类型
 */
sealed class AppError(
    open val message: String,
    open val userMessage: String,
    val recoverable: Boolean = true
) {
    /**
     * 数据库操作错误
     */
    data class DatabaseError(
        val operation: String,
        val cause: Throwable?
    ) : AppError(
        message = "Database operation failed: $operation",
        userMessage = "数据保存失败，请稍后重试",
        recoverable = true
    )

    /**
     * 验证错误
     */
    data class ValidationError(
        val field: String,
        val reason: String
    ) : AppError(
        message = "Validation failed for $field: $reason",
        userMessage = reason,
        recoverable = true
    )

    /**
     * 并发冲突错误
     */
    data class ConcurrencyError(
        val resource: String
    ) : AppError(
        message = "Concurrent modification detected on $resource",
        userMessage = "数据已被其他操作修改，请刷新后重试",
        recoverable = true
    )

    /**
     * 资源不足错误
     */
    data class ResourceError(
        val resourceType: String
    ) : AppError(
        message = "$resourceType insufficient",
        userMessage = when (resourceType) {
            "STORAGE" -> "存储空间不足，请清理后重试"
            "MEMORY" -> "内存不足，请关闭其他应用后重试"
            else -> "系统资源不足"
        },
        recoverable = false
    )

    /**
     * 权限错误
     */
    data class PermissionError(
        val permission: String
    ) : AppError(
        message = "Permission denied: $permission",
        userMessage = "缺少必要权限，请在设置中授予权限",
        recoverable = false
    )

    /**
     * 未知错误
     */
    data class UnknownError(
        override val message: String = "Unknown error",
        val cause: Throwable? = null
    ) : AppError(
        message = message,
        userMessage = "操作失败，请稍后重试",
        recoverable = true
    )

    // ==================== TD-00025: 网络相关错误 ====================

    /**
     * 网络错误
     */
    data class NetworkError(
        override val message: String,
        val cause: Throwable? = null
    ) : AppError(
        message = message,
        userMessage = message,
        recoverable = true
    )

    /**
     * 认证错误（401）
     */
    data class AuthenticationError(
        override val message: String,
        val cause: Throwable? = null
    ) : AppError(
        message = message,
        userMessage = message,
        recoverable = false
    )

    /**
     * 授权错误（403）
     */
    data class AuthorizationError(
        override val message: String,
        val cause: Throwable? = null
    ) : AppError(
        message = message,
        userMessage = message,
        recoverable = false
    )

    /**
     * 资源不存在错误（404）
     */
    data class NotFoundError(
        override val message: String,
        val cause: Throwable? = null
    ) : AppError(
        message = message,
        userMessage = message,
        recoverable = false
    )

    /**
     * 频率限制错误（429）
     */
    data class RateLimitError(
        override val message: String,
        val retryAfterSeconds: Int = 60,
        val cause: Throwable? = null
    ) : AppError(
        message = message,
        userMessage = message,
        recoverable = true
    )

    /**
     * 服务器错误（5xx）
     */
    data class ServerError(
        override val message: String,
        val cause: Throwable? = null
    ) : AppError(
        message = message,
        userMessage = message,
        recoverable = true
    )

    /**
     * HTTP错误（其他状态码）
     */
    data class HttpError(
        val code: Int,
        override val message: String,
        val cause: Throwable? = null
    ) : AppError(
        message = message,
        userMessage = message,
        recoverable = code in 500..599
    )

    /**
     * 代理错误
     */
    data class ProxyError(
        override val message: String,
        val cause: Throwable? = null
    ) : AppError(
        message = message,
        userMessage = message,
        recoverable = true
    )

    /**
     * 代理认证错误（407）
     */
    data class ProxyAuthError(
        override val message: String,
        val cause: Throwable? = null
    ) : AppError(
        message = message,
        userMessage = message,
        recoverable = false
    )

    /**
     * 请求取消错误
     */
    data class CancelledError(
        override val message: String,
        val cause: Throwable? = null
    ) : AppError(
        message = message,
        userMessage = "请求已取消",
        recoverable = false
    )
}
