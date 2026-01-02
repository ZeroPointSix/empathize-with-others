package com.empathy.ai.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.empathy.ai.domain.model.AppError
import com.empathy.ai.domain.util.ErrorMapper
import kotlinx.coroutines.launch

/**
 * ViewModel 基类
 * 
 * 提供统一的错误处理和操作执行模式
 */
abstract class BaseViewModel : ViewModel() {
    
    protected val tag: String = this::class.java.simpleName
    
    /**
     * 执行操作并处理错误
     * 
     * @param operation 要执行的操作
     * @param onSuccess 成功回调
     * @param onError 错误回调（可选，默认只记录日志）
     * @param maxRetries 最大重试次数（默认 0）
     */
    protected fun performOperation(
        operation: suspend () -> Result<Unit>,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {},
        maxRetries: Int = 0
    ) {
        viewModelScope.launch {
            var retryCount = 0
            var lastError: AppError? = null
            
            while (retryCount <= maxRetries) {
                try {
                    val result = operation()
                    
                    if (result.isSuccess) {
                        onSuccess()
                        return@launch
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
                    Log.d(tag, "Retry $retryCount after ${delayMs}ms")
                    kotlinx.coroutines.delay(delayMs)
                    
                } catch (e: Exception) {
                    lastError = ErrorMapper.mapToAppError(e)
                    
                    // 检查是否可以重试
                    if (lastError.recoverable == false || retryCount >= maxRetries) {
                        break
                    }
                    
                    // 指数退避
                    retryCount++
                    val delayMs = calculateBackoffDelay(retryCount)
                    Log.d(tag, "Retry $retryCount after ${delayMs}ms due to exception: ${e.message}")
                    kotlinx.coroutines.delay(delayMs)
                }
            }
            
            // 所有重试都失败
            val finalError = lastError ?: AppError.UnknownError(message = "Unknown error")
            Log.e(tag, "Operation failed after $retryCount retries: ${finalError.message}")
            onError(finalError.userMessage)
        }
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
