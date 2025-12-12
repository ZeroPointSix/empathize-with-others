package com.empathy.ai.domain.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.Manifest

/**
 * 悬浮窗管理工具类
 *
 * 提供悬浮窗权限检查、请求和服务管理功能
 *
 * 职责：
 * - 检查悬浮窗权限
 * - 检查前台服务权限
 * - 请求悬浮窗权限
 * - 启动和停止悬浮窗服务
 */
object FloatingWindowManager {
    
    /**
     * 检查是否有悬浮窗权限
     *
     * @param context 上下文
     * @return PermissionResult 权限检查结果
     */
    fun hasPermission(context: Context): PermissionResult {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val hasOverlay = Settings.canDrawOverlays(context)
                if (hasOverlay) {
                    PermissionResult.Granted
                } else {
                    PermissionResult.Denied("悬浮窗权限未授予")
                }
            } else {
                // Android 6.0 以下默认有权限
                PermissionResult.Granted
            }
        } catch (e: Exception) {
            android.util.Log.e("FloatingWindowManager", "检查悬浮窗权限失败", e)
            PermissionResult.Error("检查权限失败: ${e.message}")
        }
    }
    
    /**
     * 检查是否有前台服务权限
     *
     * @param context 上下文
     * @return PermissionResult 权限检查结果
     */
    fun hasForegroundServicePermission(context: Context): PermissionResult {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val hasPermission = context.checkSelfPermission(Manifest.permission.FOREGROUND_SERVICE) == PackageManager.PERMISSION_GRANTED
                if (hasPermission) {
                    PermissionResult.Granted
                } else {
                    PermissionResult.Denied("前台服务权限未授予")
                }
            } else {
                // Android 9.0 以下不需要此权限
                PermissionResult.Granted
            }
        } catch (e: Exception) {
            android.util.Log.e("FloatingWindowManager", "检查前台服务权限失败", e)
            PermissionResult.Error("检查权限失败: ${e.message}")
        }
    }
    
    /**
     * 检查所有必需的权限
     *
     * @param context 上下文
     * @return PermissionResult 综合权限检查结果
     */
    fun checkAllPermissions(context: Context): PermissionResult {
        try {
            val overlayResult = hasPermission(context)
            if (overlayResult !is PermissionResult.Granted) {
                return overlayResult
            }
            
            val foregroundResult = hasForegroundServicePermission(context)
            if (foregroundResult !is PermissionResult.Granted) {
                return foregroundResult
            }
            
            return PermissionResult.Granted
        } catch (e: Exception) {
            android.util.Log.e("FloatingWindowManager", "检查所有权限失败", e)
            return PermissionResult.Error("检查权限失败: ${e.message}")
        }
    }
    
    /**
     * 请求悬浮窗权限
     *
     * 跳转到系统设置页面让用户授权
     *
     * @param activity Activity 实例
     * @return Boolean 是否成功跳转到权限设置页面
     */
    fun requestPermission(activity: Activity): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${activity.packageName}")
                )
                activity.startActivityForResult(intent, REQUEST_CODE_OVERLAY_PERMISSION)
                android.util.Log.d("FloatingWindowManager", "跳转到悬浮窗权限设置页面")
                true
            } else {
                android.util.Log.d("FloatingWindowManager", "Android 6.0 以下无需请求悬浮窗权限")
                true
            }
        } catch (e: Exception) {
            android.util.Log.e("FloatingWindowManager", "请求悬浮窗权限失败", e)
            false
        }
    }
    
    /**
     * 启动悬浮窗服务
     *
     * @param context 上下文
     * @return ServiceStartResult 服务启动结果
     */
    fun startService(context: Context): ServiceStartResult {
        return try {
            // 首先检查权限
            val permissionResult = checkAllPermissions(context)
            if (permissionResult !is PermissionResult.Granted) {
                android.util.Log.w("FloatingWindowManager", "启动服务失败: ${permissionResult.message}")
                return ServiceStartResult.PermissionDenied(permissionResult.message)
            }
            
            val intent = Intent(context, com.empathy.ai.domain.service.FloatingWindowService::class.java)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
                android.util.Log.d("FloatingWindowManager", "使用 startForegroundService 启动服务")
            } else {
                context.startService(intent)
                android.util.Log.d("FloatingWindowManager", "使用 startService 启动服务")
            }
            
            ServiceStartResult.Success
        } catch (e: SecurityException) {
            android.util.Log.e("FloatingWindowManager", "启动服务失败：安全异常", e)
            ServiceStartResult.PermissionDenied("启动服务权限不足: ${e.message}")
        } catch (e: Exception) {
            android.util.Log.e("FloatingWindowManager", "启动服务失败", e)
            ServiceStartResult.Error("启动服务失败: ${e.message}")
        }
    }
    
    /**
     * 停止悬浮窗服务
     *
     * @param context 上下文
     * @return ServiceStopResult 服务停止结果
     */
    fun stopService(context: Context): ServiceStopResult {
        return try {
            val intent = Intent(context, com.empathy.ai.domain.service.FloatingWindowService::class.java)
            val result = context.stopService(intent)
            
            if (result) {
                android.util.Log.d("FloatingWindowManager", "服务停止成功")
                ServiceStopResult.Success
            } else {
                android.util.Log.w("FloatingWindowManager", "服务停止失败：服务可能未运行")
                ServiceStopResult.NotRunning
            }
        } catch (e: Exception) {
            android.util.Log.e("FloatingWindowManager", "停止服务失败", e)
            ServiceStopResult.Error("停止服务失败: ${e.message}")
        }
    }
    
    /**
     * 权限请求码
     */
    const val REQUEST_CODE_OVERLAY_PERMISSION = 1001
    
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
}
