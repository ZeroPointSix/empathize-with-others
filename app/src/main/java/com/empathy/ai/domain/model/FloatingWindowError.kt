package com.empathy.ai.domain.model

/**
 * 悬浮窗错误类型
 * 
 * 定义悬浮窗服务可能遇到的各种错误情况
 */
sealed class FloatingWindowError(
    val userMessage: String
) : Exception(userMessage) {
    
    /**
     * 权限被拒绝
     */
    object PermissionDenied : FloatingWindowError(
        userMessage = "需要悬浮窗权限才能使用此功能"
    )
    
    /**
     * 服务启动失败
     * 
     * @property reason 失败原因
     */
    data class ServiceError(
        val reason: String
    ) : FloatingWindowError(
        userMessage = "服务启动失败：$reason"
    )
    
    /**
     * 输入验证错误
     * 
     * @property field 验证失败的字段
     */
    data class ValidationError(
        val field: String
    ) : FloatingWindowError(
        userMessage = when (field) {
            "contact" -> "请选择联系人"
            "text" -> "请输入内容"
            "textLength" -> "输入内容不能超过 5000 字符"
            else -> "输入验证失败"
        }
    )
    
    /**
     * UseCase 执行错误
     * 
     * @property error 原始异常
     */
    data class UseCaseError(
        val error: Throwable
    ) : FloatingWindowError(
        userMessage = "操作失败：${error.message ?: "未知错误"}"
    )
}
