package com.empathy.ai.domain.util

import android.content.Context
import android.os.Build
import android.view.WindowManager
import android.widget.Toast
import com.empathy.ai.domain.model.FloatingWindowError

/**
 * 错误处理工具类
 *
 * 提供统一的错误处理和用户提示功能
 *
 * 职责：
 * - 处理 FloatingWindowError 错误
 * - 处理 WindowManager 相关错误
 * - 显示用户友好的错误提示
 * - 记录详细错误日志
 * - 提供错误分类和恢复建议
 */
object ErrorHandler {
    
    /**
     * 处理悬浮窗错误
     *
     * 根据错误类型显示相应的提示信息
     *
     * @param context 上下文
     * @param error 错误对象
     */
    fun handleError(context: Context, error: FloatingWindowError) {
        try {
            when (error) {
                is FloatingWindowError.PermissionDenied -> {
                    // 权限被拒绝，显示提示
                    showToast(context, error.userMessage, Toast.LENGTH_LONG)
                    logError("权限被拒绝", error, providePermissionRecoveryAdvice())
                }
                is FloatingWindowError.ServiceError -> {
                    // 服务错误，显示提示
                    val userMessage = enhanceServiceErrorMessage(error.userMessage, error)
                    showToast(context, userMessage, Toast.LENGTH_SHORT)
                    logError("服务错误", error, provideServiceRecoveryAdvice(error))
                }
                is FloatingWindowError.ValidationError -> {
                    // 验证错误，显示提示
                    showToast(context, error.userMessage, Toast.LENGTH_SHORT)
                    logError("验证错误", error)
                }
                is FloatingWindowError.UseCaseError -> {
                    // UseCase 错误，显示提示
                    val userMessage = enhanceUseCaseErrorMessage(error.userMessage, error)
                    showToast(context, userMessage, Toast.LENGTH_SHORT)
                    logError("UseCase错误", error, provideUseCaseRecoveryAdvice(error))
                }
            }
        } catch (e: Exception) {
            // 错误处理本身出错，使用最基本的方式记录
            android.util.Log.e("ErrorHandler", "处理错误时发生异常", e)
            try {
                Toast.makeText(context, "发生未知错误", Toast.LENGTH_SHORT).show()
            } catch (toastException: Exception) {
                // 连 Toast 都无法显示，只能记录到日志
                android.util.Log.e("ErrorHandler", "无法显示错误提示", toastException)
            }
        }
    }
    
    /**
     * 处理 WindowManager 相关错误
     *
     * @param context 上下文
     * @param exception 异常对象
     * @param operation 操作描述
     */
    fun handleWindowManagerError(context: Context, exception: Exception, operation: String) {
        try {
            val errorMessage = when {
                exception is SecurityException -> {
                    "悬浮窗权限不足，请在设置中授予悬浮窗权限"
                }
                exception is IllegalArgumentException && exception.message?.contains("window type") == true -> {
                    "当前系统版本不支持此类型悬浮窗，请检查系统兼容性"
                }
                exception.message?.contains("not attached to window manager") == true -> {
                    "悬浮窗已失效，请重新启动服务"
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                exception.message?.contains("overlay") == true -> {
                    "请在设置中允许应用显示在其他应用上层"
                }
                else -> {
                    "悬浮窗操作失败：${exception.message ?: "未知错误"}"
                }
            }
            
            showToast(context, errorMessage, Toast.LENGTH_LONG)
            logWindowManagerError(operation, exception, provideWindowManagerRecoveryAdvice(exception))
        } catch (e: Exception) {
            android.util.Log.e("ErrorHandler", "处理WindowManager错误时发生异常", e)
        }
    }
    
    /**
     * 处理通用异常
     *
     * @param context 上下文
     * @param exception 异常对象
     * @param operation 操作描述
     */
    fun handleGenericError(context: Context, exception: Exception, operation: String) {
        try {
            val errorMessage = when {
                exception is SecurityException -> {
                    "权限不足，请检查应用权限设置"
                }
                exception is NullPointerException -> {
                    "程序内部错误，请重新启动应用"
                }
                exception.message?.contains("network") == true -> {
                    "网络连接失败，请检查网络设置"
                }
                else -> {
                    "操作失败：${exception.message ?: "未知错误"}"
                }
            }
            
            showToast(context, errorMessage, Toast.LENGTH_SHORT)
            logGenericError(operation, exception)
        } catch (e: Exception) {
            android.util.Log.e("ErrorHandler", "处理通用错误时发生异常", e)
        }
    }
    
    /**
     * 处理AI服务相关错误
     *
     * @param context 上下文
     * @param exception 异常对象
     * @param operation 操作描述
     */
    fun handleAiError(context: Context, exception: Exception, operation: String) {
        try {
            val errorMessage = when {
                exception.message?.contains("API Key") == true -> {
                    "API密钥配置错误，请检查设置中的API密钥"
                }
                exception.message?.contains("timeout") == true -> {
                    "AI服务响应超时，请稍后重试"
                }
                exception.message?.contains("Empty response") == true -> {
                    "AI服务返回空响应，请重试或更换模型"
                }
                exception.message?.contains("parse") == true ||
                exception.message?.contains("JSON") == true -> {
                    "AI响应格式错误，请重试或联系技术支持"
                }
                exception.message?.contains("401") == true -> {
                    "API密钥无效，请检查设置中的API密钥"
                }
                exception.message?.contains("429") == true -> {
                    "API调用频率过高，请稍后重试"
                }
                exception.message?.contains("500") == true -> {
                    "AI服务内部错误，请稍后重试"
                }
                exception.message?.contains("network") == true -> {
                    "网络连接失败，请检查网络设置"
                }
                else -> {
                    "AI服务调用失败：${exception.message ?: "未知错误"}"
                }
            }
            
            showToast(context, errorMessage, Toast.LENGTH_LONG)
            logAiError(operation, exception, provideAiRecoveryAdvice(exception))
        } catch (e: Exception) {
            android.util.Log.e("ErrorHandler", "处理AI错误时发生异常", e)
        }
    }
    
    /**
     * 增强服务错误消息
     *
     * @param originalMessage 原始消息
     * @param error 错误对象
     * @return 增强后的消息
     */
    private fun enhanceServiceErrorMessage(originalMessage: String, error: FloatingWindowError.ServiceError): String {
        return when {
            originalMessage.contains("notification", ignoreCase = true) -> {
                "通知创建失败，请检查系统通知权限"
            }
            originalMessage.contains("foreground", ignoreCase = true) -> {
                "前台服务启动失败，请检查电池优化设置"
            }
            originalMessage.contains("permission", ignoreCase = true) -> {
                "服务权限不足，请检查应用权限设置"
            }
            else -> originalMessage
        }
    }
    
    /**
     * 增强 UseCase 错误消息
     *
     * @param originalMessage 原始消息
     * @param error 错误对象
     * @return 增强后的消息
     */
    private fun enhanceUseCaseErrorMessage(originalMessage: String, error: FloatingWindowError.UseCaseError): String {
        return when {
            originalMessage.contains("network", ignoreCase = true) -> {
                "网络请求失败，请检查网络连接"
            }
            originalMessage.contains("timeout", ignoreCase = true) -> {
                "请求超时，请稍后重试"
            }
            originalMessage.contains("parse", ignoreCase = true) -> {
                "数据解析失败，请重试或联系技术支持"
            }
            originalMessage.contains("AI响应格式错误", ignoreCase = true) -> {
                "AI响应格式错误，请重试或检查API配置"
            }
            else -> originalMessage
        }
    }
    
    /**
     * 提供权限恢复建议
     *
     * @return 恢复建议
     */
    private fun providePermissionRecoveryAdvice(): String {
        return "恢复建议：\n1. 进入系统设置\n2. 找到应用管理\n3. 选择本应用\n4. 授予悬浮窗权限\n5. 重启应用"
    }
    
    /**
     * 提供服务恢复建议
     *
     * @param error 错误对象
     * @return 恢复建议
     */
    private fun provideServiceRecoveryAdvice(error: FloatingWindowError.ServiceError): String {
        val errorMessage = error.userMessage
        return when {
            errorMessage.contains("notification", ignoreCase = true) -> {
                "恢复建议：\n1. 检查系统通知权限\n2. 关闭电池优化\n3. 重启应用"
            }
            errorMessage.contains("foreground", ignoreCase = true) -> {
                "恢复建议：\n1. 检查后台运行权限\n2. 关闭电池优化\n3. 重启应用"
            }
            else -> {
                "恢复建议：\n1. 重启应用\n2. 检查系统设置\n3. 联系技术支持"
            }
        }
    }
    
    /**
     * 提供 UseCase 恢复建议
     *
     * @param error 错误对象
     * @return 恢复建议
     */
    private fun provideUseCaseRecoveryAdvice(error: FloatingWindowError.UseCaseError): String {
        val errorMessage = error.userMessage
        return when {
            errorMessage.contains("network", ignoreCase = true) -> {
                "恢复建议：\n1. 检查网络连接\n2. 切换网络环境\n3. 稍后重试"
            }
            errorMessage.contains("timeout", ignoreCase = true) -> {
                "恢复建议：\n1. 检查网络状况\n2. 稍后重试\n3. 减少数据量"
            }
            else -> {
                "恢复建议：\n1. 稍后重试\n2. 重启应用\n3. 联系技术支持"
            }
        }
    }
    
    /**
     * 提供 WindowManager 恢复建议
     *
     * @param exception 异常对象
     * @return 恢复建议
     */
    private fun provideWindowManagerRecoveryAdvice(exception: Exception): String {
        return when {
            exception is SecurityException -> {
                "恢复建议：\n1. 进入系统设置\n2. 应用管理\n3. 本应用\n4. 权限\n5. 允许显示在其他应用上层"
            }
            exception.message?.contains("not attached", ignoreCase = true) == true -> {
                "恢复建议：\n1. 关闭应用\n2. 重新打开\n3. 重新启动悬浮窗服务"
            }
            else -> {
                "恢复建议：\n1. 重启应用\n2. 检查系统兼容性\n3. 联系技术支持"
            }
        }
    }
    
    /**
     * 提供AI服务恢复建议
     *
     * @param exception 异常对象
     * @return 恢复建议
     */
    private fun provideAiRecoveryAdvice(exception: Exception): String {
        return when {
            exception.message?.contains("API Key") == true ||
            exception.message?.contains("401") == true -> {
                "恢复建议：\n1. 进入设置页面\n2. 检查API密钥配置\n3. 确认密钥有效且未过期\n4. 重新尝试"
            }
            exception.message?.contains("timeout") == true -> {
                "恢复建议：\n1. 检查网络连接\n2. 稍后重试\n3. 减少输入文本长度\n4. 更换AI模型"
            }
            exception.message?.contains("parse") == true ||
            exception.message?.contains("JSON") == true -> {
                "恢复建议：\n1. 重新尝试\n2. 更换AI模型\n3. 检查输入内容格式\n4. 联系技术支持"
            }
            exception.message?.contains("429") == true -> {
                "恢复建议：\n1. 等待一段时间后重试\n2. 减少调用频率\n3. 升级API套餐\n4. 联系技术支持"
            }
            exception.message?.contains("500") == true -> {
                "恢复建议：\n1. 稍后重试\n2. 更换AI模型\n3. 检查服务商状态\n4. 联系技术支持"
            }
            exception.message?.contains("network") == true -> {
                "恢复建议：\n1. 检查网络连接\n2. 切换网络环境\n3. 重启应用\n4. 联系技术支持"
            }
            else -> {
                "恢复建议：\n1. 重新尝试\n2. 检查网络连接\n3. 更换AI模型\n4. 联系技术支持"
            }
        }
    }
    
    /**
     * 记录错误日志
     *
     * @param type 错误类型
     * @param error 错误对象
     * @param recoveryAdvice 恢复建议
     */
    private fun logError(type: String, error: FloatingWindowError, recoveryAdvice: String = "") {
        val logMessage = buildString {
            appendLine("=== $type ===")
            appendLine("错误消息: ${error.message ?: "无"}")
            appendLine("用户消息: ${error.userMessage}")
            if (recoveryAdvice.isNotEmpty()) {
                appendLine("恢复建议: $recoveryAdvice")
            }
            appendLine("时间戳: ${System.currentTimeMillis()}")
        }
        
        android.util.Log.e("FloatingWindow", logMessage, error)
    }
    
    /**
     * 记录 WindowManager 错误日志
     *
     * @param operation 操作描述
     * @param exception 异常对象
     * @param recoveryAdvice 恢复建议
     */
    private fun logWindowManagerError(operation: String, exception: Exception, recoveryAdvice: String = "") {
        val logMessage = buildString {
            appendLine("=== WindowManager 错误 ===")
            appendLine("操作: $operation")
            appendLine("异常类型: ${exception.javaClass.simpleName}")
            appendLine("异常消息: ${exception.message}")
            if (recoveryAdvice.isNotEmpty()) {
                appendLine("恢复建议: $recoveryAdvice")
            }
            appendLine("Android版本: ${Build.VERSION.RELEASE}")
            appendLine("时间戳: ${System.currentTimeMillis()}")
        }
        
        android.util.Log.e("FloatingWindow", logMessage, exception)
    }
    
    /**
     * 记录通用错误日志
     *
     * @param operation 操作描述
     * @param exception 异常对象
     */
    private fun logGenericError(operation: String, exception: Exception) {
        val logMessage = buildString {
            appendLine("=== 通用错误 ===")
            appendLine("操作: $operation")
            appendLine("异常类型: ${exception.javaClass.simpleName}")
            appendLine("异常消息: ${exception.message}")
            appendLine("时间戳: ${System.currentTimeMillis()}")
        }
        
        android.util.Log.e("FloatingWindow", logMessage, exception)
    }
    
    /**
     * 记录AI服务错误日志
     *
     * @param operation 操作描述
     * @param exception 异常对象
     * @param recoveryAdvice 恢复建议
     */
    private fun logAiError(operation: String, exception: Exception, recoveryAdvice: String = "") {
        val logMessage = buildString {
            appendLine("=== AI服务错误 ===")
            appendLine("操作: $operation")
            appendLine("异常类型: ${exception.javaClass.simpleName}")
            appendLine("异常消息: ${exception.message}")
            if (recoveryAdvice.isNotEmpty()) {
                appendLine("恢复建议: $recoveryAdvice")
            }
            appendLine("时间戳: ${System.currentTimeMillis()}")
        }
        
        android.util.Log.e("AiService", logMessage, exception)
    }
    
    /**
     * 显示 Toast 提示
     *
     * @param context 上下文
     * @param message 提示消息
     * @param duration 显示时长
     */
    fun showToast(context: Context, message: String, duration: Int = Toast.LENGTH_SHORT) {
        try {
            Toast.makeText(context, message, duration).show()
        } catch (e: Exception) {
            android.util.Log.e("ErrorHandler", "显示Toast失败: $message", e)
        }
    }
    
    /**
     * 显示成功提示
     *
     * @param context 上下文
     * @param message 成功消息
     */
    fun showSuccess(context: Context, message: String) {
        showToast(context, "✅ $message", Toast.LENGTH_SHORT)
    }
    
    /**
     * 显示错误提示
     *
     * @param context 上下文
     * @param message 错误消息
     */
    fun showError(context: Context, message: String) {
        showToast(context, "❌ $message", Toast.LENGTH_SHORT)
    }
    
    /**
     * 显示警告提示
     *
     * @param context 上下文
     * @param message 警告消息
     */
    fun showWarning(context: Context, message: String) {
        showToast(context, "⚠️ $message", Toast.LENGTH_LONG)
    }
}
