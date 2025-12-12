package com.empathy.ai.domain.util

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager

/**
 * 微信运行检测工具类（可选功能）
 * 
 * 提供微信应用的检测功能，用于优化悬浮窗服务的行为
 * 
 * 职责：
 * - 检测微信是否已安装
 * - 检测微信是否正在运行
 * - 获取微信版本信息
 * 
 * 注意：此功能为可选功能，不影响核心功能的使用
 */
object WeChatDetector {
    
    /**
     * 微信包名
     */
    private const val WECHAT_PACKAGE_NAME = "com.tencent.mm"
    
    /**
     * 检测微信是否已安装
     * 
     * @param context 上下文
     * @return true 如果微信已安装，false 如果未安装
     */
    fun isWeChatInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(WECHAT_PACKAGE_NAME, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
    
    /**
     * 检测微信是否正在运行
     * 
     * @param context 上下文
     * @return true 如果微信正在运行，false 如果未运行
     */
    fun isWeChatRunning(context: Context): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val runningApps = activityManager.runningAppProcesses ?: return false
        
        return runningApps.any { it.processName == WECHAT_PACKAGE_NAME }
    }
    
    /**
     * 获取微信版本名称
     * 
     * @param context 上下文
     * @return 微信版本名称，如果未安装则返回 null
     */
    fun getWeChatVersionName(context: Context): String? {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(WECHAT_PACKAGE_NAME, 0)
            packageInfo.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }
    
    /**
     * 获取微信版本号
     * 
     * @param context 上下文
     * @return 微信版本号，如果未安装则返回 -1
     */
    @Suppress("DEPRECATION")
    fun getWeChatVersionCode(context: Context): Long {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(WECHAT_PACKAGE_NAME, 0)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                packageInfo.versionCode.toLong()
            }
        } catch (e: PackageManager.NameNotFoundException) {
            -1L
        }
    }
    
    /**
     * 获取微信状态信息
     * 
     * @param context 上下文
     * @return 微信状态信息
     */
    fun getWeChatStatus(context: Context): WeChatStatus {
        val isInstalled = isWeChatInstalled(context)
        val isRunning = if (isInstalled) isWeChatRunning(context) else false
        val versionName = if (isInstalled) getWeChatVersionName(context) else null
        val versionCode = if (isInstalled) getWeChatVersionCode(context) else -1L
        
        return WeChatStatus(
            isInstalled = isInstalled,
            isRunning = isRunning,
            versionName = versionName,
            versionCode = versionCode
        )
    }
}

/**
 * 微信状态数据类
 * 
 * @property isInstalled 是否已安装
 * @property isRunning 是否正在运行
 * @property versionName 版本名称
 * @property versionCode 版本号
 */
data class WeChatStatus(
    val isInstalled: Boolean,
    val isRunning: Boolean,
    val versionName: String?,
    val versionCode: Long
) {
    /**
     * 获取状态描述
     */
    fun getStatusDescription(): String {
        return when {
            !isInstalled -> "微信未安装"
            isRunning -> "微信正在运行 (版本: $versionName)"
            else -> "微信已安装但未运行 (版本: $versionName)"
        }
    }
    
    /**
     * 检查是否为支持的微信版本
     * 
     * 支持的版本：8.0.46, 8.0.47, 8.0.48
     */
    fun isSupportedVersion(): Boolean {
        if (!isInstalled || versionName == null) return false
        
        val supportedVersions = listOf("8.0.46", "8.0.47", "8.0.48")
        return supportedVersions.any { versionName.startsWith(it) }
    }
}
