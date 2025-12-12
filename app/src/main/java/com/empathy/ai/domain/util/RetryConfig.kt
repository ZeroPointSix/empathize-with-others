package com.empathy.ai.domain.util

/**
 * 重试配置
 * 
 * 定义操作重试的策略参数
 */
data class RetryConfig(
    /**
     * 最大重试次数
     */
    val maxRetries: Int = 2,
    
    /**
     * 初始延迟时间（毫秒）
     */
    val initialDelayMs: Long = 1000,
    
    /**
     * 最大延迟时间（毫秒）
     */
    val maxDelayMs: Long = 5000,
    
    /**
     * 退避倍数
     */
    val backoffMultiplier: Double = 2.0
) {
    companion object {
        /**
         * 数据库操作的默认配置
         */
        val DATABASE = RetryConfig(
            maxRetries = 2,
            initialDelayMs = 1000,
            maxDelayMs = 5000,
            backoffMultiplier = 2.0
        )
        
        /**
         * 网络操作的默认配置
         */
        val NETWORK = RetryConfig(
            maxRetries = 3,
            initialDelayMs = 1000,
            maxDelayMs = 10000,
            backoffMultiplier = 2.0
        )
        
        /**
         * 不重试的配置
         */
        val NO_RETRY = RetryConfig(
            maxRetries = 0,
            initialDelayMs = 0,
            maxDelayMs = 0,
            backoffMultiplier = 1.0
        )
    }
}
