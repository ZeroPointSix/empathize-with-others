package com.empathy.ai.domain.util

import android.util.Log
import com.empathy.ai.domain.model.AppError
import kotlinx.coroutines.delay

/**
 * 操作执行器
 * 
 * 提供统一的操作执行和错误处理模式
 */
object OperationExecutor {
    private const val TAG = "OperationExecutor"
    
    /**
     * 执行操作并处理错误
     * 
     * @param operation 要执行的操作
     * @param maxRetries 最大重试次数（默认 0，不重试）
     * @return 操作结果，失败时返回 AppError
     */
    suspend fun <T> executeOperation(
        operation: suspend () -> Result<T>,
        maxRetries: Int = 0
    ): Result<T> {
        var retryCount = 0
        var lastError: AppError? = null
        
        while (retryCount <= maxRetries) {
            try {
                val result = operation()
                
                if (result.isSuccess) {
                    return result
                }
                
                // 操作失败，映射错误
                lastError = ErrorMapper.mapResultError(result)
                
                // 检查是否可以重试
                if (lastError?.recoverable == false || retryCount >= maxRetries) {
                    break
                }
                
                // 指数退避
                retryCount++
                val delayMs = calculateBackoffDelay(retryCount)
                Log.d(TAG, "Retry $retryCount after ${delayMs}ms")
                delay(delayMs)
                
            } catch (e: Exception) {
                lastError = ErrorMapper.mapToAppError(e)
                
                // 检查是否可以重试
                if (lastError.recoverable == false || retryCount >= maxRetries) {
                    break
                }
                
                // 指数退避
                retryCount++
                val delayMs = calculateBackoffDelay(retryCount)
                Log.d(TAG, "Retry $retryCount after ${delayMs}ms due to exception: ${e.message}")
                delay(delayMs)
            }
        }
        
        // 所有重试都失败
        val finalError = lastError ?: AppError.UnknownError(null)
        Log.e(TAG, "Operation failed after $retryCount retries: ${finalError.message}")
        return Result.failure(Exception(finalError.userMessage))
    }
    
    /**
     * 使用自定义重试配置执行操作
     * 
     * @param config 重试配置
     * @param operation 要执行的操作
     * @return 操作结果
     */
    suspend fun <T> retryWithBackoff(
        config: RetryConfig = RetryConfig.DATABASE,
        operation: suspend () -> Result<T>
    ): Result<T> {
        var currentDelay = config.initialDelayMs
        var lastError: Throwable? = null
        
        repeat(config.maxRetries + 1) { attempt ->
            try {
                val result = operation()
                if (result.isSuccess) {
                    return result
                }
                lastError = result.exceptionOrNull()
            } catch (e: Exception) {
                lastError = e
            }
            
            // 如果还有重试机会，等待后重试
            if (attempt < config.maxRetries) {
                Log.d(TAG, "Retry ${attempt + 1}/${config.maxRetries} after ${currentDelay}ms")
                delay(currentDelay)
                currentDelay = (currentDelay * config.backoffMultiplier)
                    .toLong()
                    .coerceAtMost(config.maxDelayMs)
            }
        }
        
        // 所有重试都失败
        Log.e(TAG, "Operation failed after ${config.maxRetries} retries")
        return Result.failure(lastError ?: Exception("Operation failed"))
    }
    
    /**
     * 计算退避延迟时间
     * 
     * 使用指数退避策略：1s, 2s, 4s, ...
     */
    private fun calculateBackoffDelay(retryCount: Int): Long {
        val baseDelay = 1000L
        val maxDelay = 5000L
        val delay = baseDelay * (1 shl (retryCount - 1)) // 2^(retryCount-1)
        return delay.coerceAtMost(maxDelay)
    }
}
