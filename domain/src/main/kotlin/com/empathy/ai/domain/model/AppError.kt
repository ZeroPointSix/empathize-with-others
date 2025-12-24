package com.empathy.ai.domain.model

/**
 * 统一的应用错误类型
 *
 * 提供类型安全的错误分类和用户友好的错误消息
 */
sealed class AppError(
    val message: String,
    val userMessage: String,
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
        val cause: Throwable?
    ) : AppError(
        message = "Unknown error: ${cause?.message}",
        userMessage = "操作失败，请稍后重试",
        recoverable = true
    )
}
