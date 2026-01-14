package com.empathy.ai.domain.util

/**
 * 悬浮窗管理器接口
 *
 * 提供悬浮窗权限检查、请求和服务管理功能的抽象接口。
 * 实现类在app层提供（如AndroidFloatingWindowManager）。
 *
 * 注意：由于悬浮窗管理需要Android Context和Activity，
 * 实际实现保留在app模块中，presentation层通过此接口访问。
 *
 * @see TDD-00017 Clean Architecture模块化改造技术设计
 */
interface FloatingWindowManager {

    /**
     * 检查是否有悬浮窗权限
     *
     * @return PermissionResult 权限检查结果
     */
    fun hasPermission(): PermissionResult

    /**
     * 检查是否有前台服务权限
     *
     * @return PermissionResult 权限检查结果
     */
    fun hasForegroundServicePermission(): PermissionResult

    /**
     * 检查所有必需的权限
     *
     * @return PermissionResult 综合权限检查结果
     */
    fun checkAllPermissions(): PermissionResult

    /**
     * 启动悬浮窗服务
     *
     * @param displayId 可选的显示屏ID（多显示屏场景下用于指定目标显示屏）
     *
     * @return ServiceStartResult 服务启动结果
     */
    fun startService(displayId: Int? = null): ServiceStartResult

    /**
     * 停止悬浮窗服务
     *
     * @return ServiceStopResult 服务停止结果
     */
    fun stopService(): ServiceStopResult

    /**
     * 检查悬浮窗服务是否正在运行
     *
     * @return Boolean 服务是否正在运行
     */
    fun isServiceRunning(): Boolean

    /**
     * 权限检查结果
     */
    sealed class PermissionResult {
        abstract val message: String

        object Granted : PermissionResult() {
            override val message: String = "权限已授予"
        }
        data class Denied(override val message: String) : PermissionResult()
        data class Error(override val message: String) : PermissionResult()
    }

    /**
     * 服务启动结果
     */
    sealed class ServiceStartResult {
        object Success : ServiceStartResult()
        data class PermissionDenied(val message: String) : ServiceStartResult()
        data class Error(val message: String) : ServiceStartResult()
    }

    /**
     * 服务停止结果
     */
    sealed class ServiceStopResult {
        object Success : ServiceStopResult()
        object NotRunning : ServiceStopResult()
        data class Error(val message: String) : ServiceStopResult()
    }

    companion object {
        const val REQUEST_CODE_OVERLAY_PERMISSION = 1001
        
        /**
         * 请求悬浮窗权限（需要Activity）
         * 
         * 注意：此方法需要在app层实现，因为需要Activity引用。
         * presentation层应通过回调或事件机制请求权限。
         */
        @Deprecated("使用依赖注入的FloatingWindowManager实例代替静态方法")
        fun requestPermission(activity: Any): Boolean = false
    }
}
