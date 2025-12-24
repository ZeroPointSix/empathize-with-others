package com.empathy.ai.domain.util

import android.util.Log

/**
 * 悬浮窗调试日志记录器
 * 用于记录悬浮窗操作、状态转换和异常信息
 */
object FloatingViewDebugLogger {
    
    private const val TAG = "FloatingViewDebug"
    
    /**
     * 记录状态转换
     */
    fun logStateTransition(fromState: String, toState: String, operation: String) {
        Log.d(TAG, "状态转换: $fromState -> $toState, 操作: $operation")
    }
    
    /**
     * 记录最小化流程
     */
    fun logMinimizeFlow(step: String, success: Boolean, detail: String) {
        val status = if (success) "成功" else "失败"
        Log.d(TAG, "最小化流程[$status]: $step - $detail")
    }
    
    /**
     * 记录视图状态
     */
    fun logViewState(operation: String, dialogVisible: Boolean, buttonVisible: Boolean, indicatorVisible: Boolean) {
        Log.d(TAG, "视图状态[$operation]: 对话框=$dialogVisible, 按钮=$buttonVisible, 指示器=$indicatorVisible")
    }
    
    /**
     * 记录资源清理
     */
    fun logResourceCleanup(operation: String, success: Boolean, detail: String) {
        val status = if (success) "成功" else "失败"
        Log.d(TAG, "资源清理[$status]: $operation - $detail")
    }
    
    /**
     * 记录监听器清理
     */
    fun logListenerCleanup(listenerType: String, count: Int, success: Boolean) {
        val status = if (success) "成功" else "失败"
        Log.d(TAG, "监听器清理[$status]: $listenerType, 数量: $count")
    }
    
    /**
     * 记录异常
     */
    fun logException(operation: String, exception: Throwable) {
        Log.e(TAG, "异常[$operation]: ${exception.message}", exception)
    }
    
    /**
     * 记录性能信息
     */
    fun logPerformance(operation: String, duration: Long) {
        Log.d(TAG, "性能[$operation]: 耗时 ${duration}ms")
    }
    
    /**
     * 记录用户交互
     */
    fun logUserInteraction(action: String, detail: String) {
        Log.d(TAG, "用户交互: $action - $detail")
    }
}
