package com.empathy.ai.domain.model

/**
 * 最小化功能错误类型
 * 
 * 定义最小化功能相关的错误类型，用于统一错误处理
 */
sealed class MinimizeError(message: String) : Exception(message) {
    
    /**
     * 最小化失败
     * 
     * @property reason 失败原因
     */
    class MinimizeFailed(reason: String) : MinimizeError("最小化失败: $reason")
    
    /**
     * 恢复失败
     * 
     * @property reason 失败原因
     */
    class RestoreFailed(reason: String) : MinimizeError("恢复失败: $reason")
    
    /**
     * 通知失败
     * 
     * @property reason 失败原因
     */
    class NotificationFailed(reason: String) : MinimizeError("通知失败: $reason")
}
