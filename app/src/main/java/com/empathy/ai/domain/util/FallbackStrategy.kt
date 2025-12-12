package com.empathy.ai.domain.util

import android.util.Log

/**
 * 降级策略
 * 
 * 当主要操作失败时提供备用方案
 */
object FallbackStrategy {
    private const val TAG = "FallbackStrategy"
    
    /**
     * 执行操作，失败时使用降级策略
     * 
     * @param primary 主要操作
     * @param fallback 降级操作
     * @return 操作结果
     */
    suspend fun <T> executeWithFallback(
        primary: suspend () -> Result<T>,
        fallback: suspend () -> T
    ): T {
        return try {
            val result = primary()
            if (result.isSuccess) {
                result.getOrThrow()
            } else {
                Log.w(TAG, "Primary operation failed, using fallback")
                fallback()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Primary operation threw exception, using fallback: ${e.message}")
            fallback()
        }
    }
    
    /**
     * 执行操作，失败时返回默认值
     * 
     * @param operation 要执行的操作
     * @param defaultValue 默认值
     * @return 操作结果或默认值
     */
    suspend fun <T> executeOrDefault(
        operation: suspend () -> Result<T>,
        defaultValue: T
    ): T {
        return try {
            val result = operation()
            result.getOrElse {
                Log.w(TAG, "Operation failed, using default value")
                defaultValue
            }
        } catch (e: Exception) {
            Log.w(TAG, "Operation threw exception, using default value: ${e.message}")
            defaultValue
        }
    }
    
    /**
     * 执行操作，失败时返回空列表
     * 
     * 专门用于列表查询操作的降级
     */
    suspend fun <T> executeOrEmptyList(
        operation: suspend () -> Result<List<T>>
    ): List<T> {
        return executeOrDefault(operation, emptyList())
    }
    
    /**
     * 执行操作，失败时返回 null
     * 
     * 专门用于可选值查询的降级
     */
    suspend fun <T> executeOrNull(
        operation: suspend () -> Result<T>
    ): T? {
        return try {
            val result = operation()
            result.getOrNull()
        } catch (e: Exception) {
            Log.w(TAG, "Operation threw exception, returning null: ${e.message}")
            null
        }
    }
}
