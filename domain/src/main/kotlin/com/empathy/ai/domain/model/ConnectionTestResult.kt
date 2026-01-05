package com.empathy.ai.domain.model

/**
 * 连接测试结果
 *
 * 用于测试AI服务商连接状态，包含成功/失败结果和详细信息。
 *
 * 业务背景 (PRD-00002):
 * - 用户配置AI服务商后需要测试连接是否正常
 * - 区分不同错误类型，便于用户定位问题
 * - latencyMs用于评估服务商响应速度
 *
 * 设计决策:
 * - 使用嵌套枚举定义错误类型，覆盖常见失败场景
 * - 提供工厂方法和用户友好消息，便于UI使用
 *
 * @property isSuccess 测试是否成功
 * @property latencyMs 响应延迟（毫秒）
 * @property errorType 错误类型（如果失败）
 * @property errorMessage 错误消息（如果失败）
 */
data class ConnectionTestResult(
    /**
     * 测试是否成功
     */
    val isSuccess: Boolean,
    
    /**
     * 响应延迟（毫秒）
     */
    val latencyMs: Long = 0,
    
    /**
     * 错误类型（如果失败）
     */
    val errorType: ErrorType? = null,
    
    /**
     * 错误消息（如果失败）
     */
    val errorMessage: String? = null
) {
    /**
     * 连接测试错误类型
     */
    enum class ErrorType {
        /**
         * API Key 无效
         */
        INVALID_API_KEY,
        
        /**
         * 端点不可达
         */
        ENDPOINT_UNREACHABLE,
        
        /**
         * 模型不存在
         */
        MODEL_NOT_FOUND,
        
        /**
         * 请求超时
         */
        TIMEOUT,
        
        /**
         * 配额用尽
         */
        QUOTA_EXCEEDED,
        
        /**
         * 网络错误
         */
        NETWORK_ERROR,
        
        /**
         * 未知错误
         */
        UNKNOWN
    }
    
    companion object {
        /**
         * 创建成功结果
         */
        fun success(latencyMs: Long): ConnectionTestResult {
            return ConnectionTestResult(
                isSuccess = true,
                latencyMs = latencyMs
            )
        }
        
        /**
         * 创建失败结果
         */
        fun failure(errorType: ErrorType, errorMessage: String): ConnectionTestResult {
            return ConnectionTestResult(
                isSuccess = false,
                errorType = errorType,
                errorMessage = errorMessage
            )
        }
    }
    
    /**
     * 获取用户友好的错误消息
     */
    fun getUserFriendlyMessage(): String {
        if (isSuccess) {
            return "连接成功，延迟 ${latencyMs}ms"
        }
        
        return when (errorType) {
            ErrorType.INVALID_API_KEY -> "API Key 无效，请检查配置"
            ErrorType.ENDPOINT_UNREACHABLE -> "无法连接到服务器，请检查 API 端点"
            ErrorType.MODEL_NOT_FOUND -> "模型不存在，请检查模型名称"
            ErrorType.TIMEOUT -> "请求超时，请稍后重试"
            ErrorType.QUOTA_EXCEEDED -> "API 配额已用尽"
            ErrorType.NETWORK_ERROR -> "网络错误，请检查网络连接"
            ErrorType.UNKNOWN -> errorMessage ?: "未知错误"
            null -> errorMessage ?: "测试失败"
        }
    }
}
