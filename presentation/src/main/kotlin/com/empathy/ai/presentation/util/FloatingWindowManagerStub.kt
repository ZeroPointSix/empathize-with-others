package com.empathy.ai.presentation.util

import android.app.Activity
import android.content.Context
import com.empathy.ai.domain.util.FloatingWindowManager

/**
 * FloatingWindowManager的占位实现
 *
 * 这是一个临时实现，用于让presentation模块能够编译通过。
 * 实际的FloatingWindowManager实现保留在app模块中。
 *
 * 在Phase 4中，将通过DI注入真正的实现。
 *
 * @see TDD-00017 Clean Architecture模块化改造技术设计
 */
object FloatingWindowManagerStub : FloatingWindowManager {

    private var context: Context? = null

    fun init(context: Context) {
        this.context = context.applicationContext
    }

    override fun hasPermission(): FloatingWindowManager.PermissionResult {
        return FloatingWindowManager.PermissionResult.Denied("FloatingWindowManager未初始化")
    }

    override fun hasForegroundServicePermission(): FloatingWindowManager.PermissionResult {
        return FloatingWindowManager.PermissionResult.Denied("FloatingWindowManager未初始化")
    }

    override fun checkAllPermissions(): FloatingWindowManager.PermissionResult {
        return FloatingWindowManager.PermissionResult.Denied("FloatingWindowManager未初始化")
    }

    override fun startService(): FloatingWindowManager.ServiceStartResult {
        return FloatingWindowManager.ServiceStartResult.Error("FloatingWindowManager未初始化")
    }

    override fun stopService(): FloatingWindowManager.ServiceStopResult {
        return FloatingWindowManager.ServiceStopResult.Error("FloatingWindowManager未初始化")
    }

    override fun isServiceRunning(): Boolean = false

    /**
     * 请求悬浮窗权限
     *
     * @param activity Activity实例
     * @return 是否成功跳转到权限设置页面
     */
    fun requestPermission(activity: Activity): Boolean {
        // 占位实现，实际逻辑在app模块
        return false
    }
}
