package com.empathy.ai.domain.util

import com.empathy.ai.domain.model.AppError

/**
 * 错误映射工具
 * 
 * 将系统异常映射为应用错误类型
 * 
 * 注意：此类是纯Kotlin实现，不依赖Android SDK
 * 平台特定的异常映射（如SQLiteException）应在data层处理
 */
object ErrorMapper {
    /**
     * 将 Throwable 映射为 AppError
     */
    fun mapToAppError(error: Throwable): AppError {
        return when (error) {
            is IllegalArgumentException -> AppError.ValidationError(
                field = "input",
                reason = error.message ?: "无效输入"
            )
            is OutOfMemoryError -> AppError.ResourceError("MEMORY")
            is SecurityException -> AppError.PermissionError(
                permission = error.message ?: "unknown"
            )
            is IllegalStateException -> AppError.ValidationError(
                field = "state",
                reason = error.message ?: "非法状态"
            )
            else -> AppError.UnknownError(error)
        }
    }
    
    /**
     * 从 Result 中提取错误并映射
     */
    fun <T> mapResultError(result: Result<T>): AppError? {
        return result.exceptionOrNull()?.let { mapToAppError(it) }
    }
}
