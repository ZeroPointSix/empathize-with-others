package com.empathy.ai.domain.util

import android.database.sqlite.SQLiteException
import com.empathy.ai.domain.model.AppError

/**
 * 错误映射工具
 * 
 * 将系统异常映射为应用错误类型
 */
object ErrorMapper {
    /**
     * 将 Throwable 映射为 AppError
     */
    fun mapToAppError(error: Throwable): AppError {
        return when (error) {
            is SQLiteException -> AppError.DatabaseError(
                operation = "database operation",
                cause = error
            )
            is IllegalArgumentException -> AppError.ValidationError(
                field = "input",
                reason = error.message ?: "无效输入"
            )
            is OutOfMemoryError -> AppError.ResourceError("MEMORY")
            is SecurityException -> AppError.PermissionError(
                permission = error.message ?: "unknown"
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
