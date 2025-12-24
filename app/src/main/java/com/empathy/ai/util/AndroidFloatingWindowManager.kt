package com.empathy.ai.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.empathy.ai.domain.service.FloatingWindowService
import com.empathy.ai.domain.util.FloatingWindowManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * FloatingWindowManager的Android实现
 *
 * 提供悬浮窗权限检查、请求和服务管理功能。
 *
 * @see TDD-00017 Clean Architecture模块化改造技术设计
 */
@Singleton
class AndroidFloatingWindowManager @Inject constructor(
    @ApplicationContext private val context: Context
) : FloatingWindowManager {

    companion object {
        private const val TAG = "AndroidFloatingWindowManager"
    }

    override fun hasPermission(): FloatingWindowManager.PermissionResult {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(context)) {
                    FloatingWindowManager.PermissionResult.Granted
                } else {
                    FloatingWindowManager.PermissionResult.Denied("悬浮窗权限未授予")
                }
            } else {
                // Android 6.0以下不需要悬浮窗权限
                FloatingWindowManager.PermissionResult.Granted
            }
        } catch (e: Exception) {
            Log.e(TAG, "检查悬浮窗权限失败", e)
            FloatingWindowManager.PermissionResult.Error("检查权限失败: ${e.message}")
        }
    }

    override fun hasForegroundServicePermission(): FloatingWindowManager.PermissionResult {
        // Android 9.0+ 需要前台服务权限，但通常在Manifest中声明即可
        return FloatingWindowManager.PermissionResult.Granted
    }

    override fun checkAllPermissions(): FloatingWindowManager.PermissionResult {
        val overlayResult = hasPermission()
        if (overlayResult !is FloatingWindowManager.PermissionResult.Granted) {
            return overlayResult
        }
        return hasForegroundServicePermission()
    }

    override fun startService(): FloatingWindowManager.ServiceStartResult {
        return try {
            val permissionResult = hasPermission()
            if (permissionResult !is FloatingWindowManager.PermissionResult.Granted) {
                return FloatingWindowManager.ServiceStartResult.PermissionDenied(
                    permissionResult.message
                )
            }

            val intent = Intent(context, FloatingWindowService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            Log.d(TAG, "悬浮窗服务启动成功")
            FloatingWindowManager.ServiceStartResult.Success
        } catch (e: Exception) {
            Log.e(TAG, "启动悬浮窗服务失败", e)
            FloatingWindowManager.ServiceStartResult.Error("启动服务失败: ${e.message}")
        }
    }

    override fun stopService(): FloatingWindowManager.ServiceStopResult {
        return try {
            val intent = Intent(context, FloatingWindowService::class.java)
            val stopped = context.stopService(intent)
            if (stopped) {
                Log.d(TAG, "悬浮窗服务停止成功")
                FloatingWindowManager.ServiceStopResult.Success
            } else {
                Log.d(TAG, "悬浮窗服务未在运行")
                FloatingWindowManager.ServiceStopResult.NotRunning
            }
        } catch (e: Exception) {
            Log.e(TAG, "停止悬浮窗服务失败", e)
            FloatingWindowManager.ServiceStopResult.Error("停止服务失败: ${e.message}")
        }
    }

    override fun isServiceRunning(): Boolean {
        // 简单实现：检查服务是否在运行
        // 更精确的实现可以使用ActivityManager
        return try {
            val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            @Suppress("DEPRECATION")
            for (service in manager.getRunningServices(Int.MAX_VALUE)) {
                if (FloatingWindowService::class.java.name == service.service.className) {
                    return true
                }
            }
            false
        } catch (e: Exception) {
            Log.e(TAG, "检查服务运行状态失败", e)
            false
        }
    }

    /**
     * 请求悬浮窗权限
     *
     * @param activity Activity实例
     * @return 是否成功跳转到权限设置页面
     */
    fun requestPermission(activity: Activity): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                )
                activity.startActivityForResult(
                    intent,
                    FloatingWindowManager.REQUEST_CODE_OVERLAY_PERMISSION
                )
                true
            } else {
                // Android 6.0以下不需要请求权限
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "请求悬浮窗权限失败", e)
            false
        }
    }
}
