package com.empathy.ai.domain.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 权限管理器
 *
 * 统一管理应用所需的各种权限
 *
 * 职责：
 * - 检查权限状态
 * - 提供权限请求Intent
 * - 监听权限状态变化
 *
 * 支持的权限：
 * - 悬浮窗权限（SYSTEM_ALERT_WINDOW）
 * - 无障碍服务权限
 * - 通知权限（Android 13+）
 */
@Singleton
class PermissionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    // ========== 权限状态 ==========
    
    private val _overlayPermissionState = MutableStateFlow(checkOverlayPermission())
    val overlayPermissionState: StateFlow<Boolean> = _overlayPermissionState.asStateFlow()
    
    private val _accessibilityPermissionState = MutableStateFlow(checkAccessibilityPermission())
    val accessibilityPermissionState: StateFlow<Boolean> = _accessibilityPermissionState.asStateFlow()
    
    // ========== 悬浮窗权限 ==========
    
    /**
     * 检查悬浮窗权限
     *
     * @return true 如果已授权
     */
    fun checkOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true // Android 6.0以下默认有权限
        }
    }
    
    /**
     * 获取悬浮窗权限设置Intent
     *
     * @return 跳转到悬浮窗权限设置页面的Intent
     */
    fun getOverlayPermissionIntent(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else {
            // Android 6.0以下跳转到应用详情页
            getAppSettingsIntent()
        }
    }
    
    /**
     * 刷新悬浮窗权限状态
     */
    fun refreshOverlayPermission() {
        _overlayPermissionState.value = checkOverlayPermission()
    }
    
    // ========== 无障碍服务权限 ==========
    
    /**
     * 检查无障碍服务是否启用
     *
     * @param serviceName 无障碍服务的完整类名
     * @return true 如果服务已启用
     */
    fun checkAccessibilityPermission(
        serviceName: String = "${context.packageName}/.service.EmpathyAccessibilityService"
    ): Boolean {
        return try {
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: ""
            
            enabledServices.contains(serviceName) ||
                enabledServices.contains(context.packageName)
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 获取无障碍服务设置Intent
     */
    fun getAccessibilitySettingsIntent(): Intent {
        return Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
    
    /**
     * 刷新无障碍服务权限状态
     */
    fun refreshAccessibilityPermission() {
        _accessibilityPermissionState.value = checkAccessibilityPermission()
    }
    
    // ========== 通知权限 ==========
    
    /**
     * 检查通知权限（Android 13+）
     */
    fun checkNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val notificationManager = context.getSystemService(
                android.app.NotificationManager::class.java
            )
            notificationManager?.areNotificationsEnabled() ?: false
        } else {
            true // Android 13以下默认有权限
        }
    }
    
    /**
     * 获取通知设置Intent
     */
    fun getNotificationSettingsIntent(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else {
            getAppSettingsIntent()
        }
    }
    
    // ========== 应用设置 ==========
    
    /**
     * 获取应用详情设置Intent
     */
    fun getAppSettingsIntent(): Intent {
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
    
    // ========== 批量检查 ==========
    
    /**
     * 权限状态数据类
     */
    data class PermissionStatus(
        val overlay: Boolean,
        val accessibility: Boolean,
        val notification: Boolean
    ) {
        val allGranted: Boolean
            get() = overlay && accessibility && notification
        
        val anyMissing: Boolean
            get() = !overlay || !accessibility || !notification
    }
    
    /**
     * 获取所有权限状态
     */
    fun getAllPermissionStatus(): PermissionStatus {
        return PermissionStatus(
            overlay = checkOverlayPermission(),
            accessibility = checkAccessibilityPermission(),
            notification = checkNotificationPermission()
        )
    }
    
    /**
     * 刷新所有权限状态
     */
    fun refreshAllPermissions() {
        refreshOverlayPermission()
        refreshAccessibilityPermission()
    }
}
