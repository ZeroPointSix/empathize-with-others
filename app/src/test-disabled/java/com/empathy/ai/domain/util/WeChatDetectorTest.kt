package com.empathy.ai.domain.util

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * 微信检测工具类测试
 * 
 * 测试 WeChatDetector 的各项功能
 */
class WeChatDetectorTest {
    
    private lateinit var context: Context
    private lateinit var packageManager: PackageManager
    private lateinit var activityManager: ActivityManager
    
    @Before
    fun setup() {
        context = mockk(relaxed = true)
        packageManager = mockk(relaxed = true)
        activityManager = mockk(relaxed = true)
        
        every { context.packageManager } returns packageManager
        every { context.getSystemService(Context.ACTIVITY_SERVICE) } returns activityManager
    }
    
    @Test
    fun `isWeChatInstalled - 微信已安装时返回 true`() {
        // Given
        val packageInfo = mockk<PackageInfo>()
        every { packageManager.getPackageInfo("com.tencent.mm", 0) } returns packageInfo
        
        // When
        val result = WeChatDetector.isWeChatInstalled(context)
        
        // Then
        assertTrue(result)
    }
    
    @Test
    fun `isWeChatInstalled - 微信未安装时返回 false`() {
        // Given
        every { 
            packageManager.getPackageInfo("com.tencent.mm", 0) 
        } throws PackageManager.NameNotFoundException()
        
        // When
        val result = WeChatDetector.isWeChatInstalled(context)
        
        // Then
        assertFalse(result)
    }
    
    @Test
    fun `isWeChatRunning - 微信正在运行时返回 true`() {
        // Given
        val runningProcess = mockk<ActivityManager.RunningAppProcessInfo>()
        runningProcess.processName = "com.tencent.mm"
        every { activityManager.runningAppProcesses } returns listOf(runningProcess)
        
        // When
        val result = WeChatDetector.isWeChatRunning(context)
        
        // Then
        assertTrue(result)
    }
    
    @Test
    fun `isWeChatRunning - 微信未运行时返回 false`() {
        // Given
        val runningProcess = mockk<ActivityManager.RunningAppProcessInfo>()
        runningProcess.processName = "com.other.app"
        every { activityManager.runningAppProcesses } returns listOf(runningProcess)
        
        // When
        val result = WeChatDetector.isWeChatRunning(context)
        
        // Then
        assertFalse(result)
    }
    
    @Test
    fun `isWeChatRunning - 无运行进程时返回 false`() {
        // Given
        every { activityManager.runningAppProcesses } returns null
        
        // When
        val result = WeChatDetector.isWeChatRunning(context)
        
        // Then
        assertFalse(result)
    }
    
    @Test
    fun `getWeChatVersionName - 返回正确的版本名称`() {
        // Given
        val packageInfo = mockk<PackageInfo>()
        packageInfo.versionName = "8.0.48"
        every { packageManager.getPackageInfo("com.tencent.mm", 0) } returns packageInfo
        
        // When
        val result = WeChatDetector.getWeChatVersionName(context)
        
        // Then
        assertEquals("8.0.48", result)
    }
    
    @Test
    fun `getWeChatVersionName - 微信未安装时返回 null`() {
        // Given
        every { 
            packageManager.getPackageInfo("com.tencent.mm", 0) 
        } throws PackageManager.NameNotFoundException()
        
        // When
        val result = WeChatDetector.getWeChatVersionName(context)
        
        // Then
        assertNull(result)
    }
    
    @Test
    fun `getWeChatStatus - 返回完整的状态信息`() {
        // Given
        val packageInfo = mockk<PackageInfo>()
        packageInfo.versionName = "8.0.48"
        packageInfo.versionCode = 2080
        every { packageManager.getPackageInfo("com.tencent.mm", 0) } returns packageInfo
        
        val runningProcess = mockk<ActivityManager.RunningAppProcessInfo>()
        runningProcess.processName = "com.tencent.mm"
        every { activityManager.runningAppProcesses } returns listOf(runningProcess)
        
        // When
        val status = WeChatDetector.getWeChatStatus(context)
        
        // Then
        assertTrue(status.isInstalled)
        assertTrue(status.isRunning)
        assertEquals("8.0.48", status.versionName)
        assertEquals(2080L, status.versionCode)
    }
    
    @Test
    fun `WeChatStatus - getStatusDescription 返回正确的描述`() {
        // 未安装
        val notInstalledStatus = WeChatStatus(
            isInstalled = false,
            isRunning = false,
            versionName = null,
            versionCode = -1
        )
        assertEquals("微信未安装", notInstalledStatus.getStatusDescription())
        
        // 已安装但未运行
        val installedNotRunningStatus = WeChatStatus(
            isInstalled = true,
            isRunning = false,
            versionName = "8.0.48",
            versionCode = 2080
        )
        assertEquals("微信已安装但未运行 (版本: 8.0.48)", installedNotRunningStatus.getStatusDescription())
        
        // 正在运行
        val runningStatus = WeChatStatus(
            isInstalled = true,
            isRunning = true,
            versionName = "8.0.48",
            versionCode = 2080
        )
        assertEquals("微信正在运行 (版本: 8.0.48)", runningStatus.getStatusDescription())
    }
    
    @Test
    fun `WeChatStatus - isSupportedVersion 正确识别支持的版本`() {
        // 支持的版本
        val supportedVersions = listOf("8.0.46", "8.0.47", "8.0.48")
        supportedVersions.forEach { version ->
            val status = WeChatStatus(
                isInstalled = true,
                isRunning = true,
                versionName = version,
                versionCode = 2080
            )
            assertTrue("版本 $version 应该被支持", status.isSupportedVersion())
        }
        
        // 不支持的版本
        val unsupportedStatus = WeChatStatus(
            isInstalled = true,
            isRunning = true,
            versionName = "7.0.0",
            versionCode = 1700
        )
        assertFalse(unsupportedStatus.isSupportedVersion())
        
        // 未安装
        val notInstalledStatus = WeChatStatus(
            isInstalled = false,
            isRunning = false,
            versionName = null,
            versionCode = -1
        )
        assertFalse(notInstalledStatus.isSupportedVersion())
    }
}
