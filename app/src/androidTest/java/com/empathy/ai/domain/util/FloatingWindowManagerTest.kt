package com.empathy.ai.domain.util

import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 悬浮窗管理器集成测试
 *
 * 测试范围：
 * - 权限检查功能
 * - 服务启动/停止功能
 * - 权限结果类型
 *
 * 注意：部分测试需要在真实设备上运行，模拟器可能无法正确模拟悬浮窗权限
 */
@RunWith(AndroidJUnit4::class)
class FloatingWindowManagerTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    // ==================== 权限检查测试 ====================

    @Test
    fun hasPermission_returnsPermissionResult() {
        // When
        val result = FloatingWindowManager.hasPermission(context)

        // Then
        // 结果应该是 PermissionResult 的某个子类型
        assertTrue(
            "权限检查应返回有效结果",
            result is FloatingWindowManager.PermissionResult.Granted ||
            result is FloatingWindowManager.PermissionResult.Denied ||
            result is FloatingWindowManager.PermissionResult.Error
        )
    }

    @Test
    fun hasPermission_onAndroid6Plus_checksOverlayPermission() {
        // Given
        // Android 6.0+ 需要检查悬浮窗权限

        // When
        val result = FloatingWindowManager.hasPermission(context)

        // Then
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val systemHasPermission = Settings.canDrawOverlays(context)
            if (systemHasPermission) {
                assertEquals(
                    "系统有权限时应返回 Granted",
                    FloatingWindowManager.PermissionResult.Granted,
                    result
                )
            } else {
                assertTrue(
                    "系统无权限时应返回 Denied",
                    result is FloatingWindowManager.PermissionResult.Denied
                )
            }
        }
    }

    @Test
    fun hasForegroundServicePermission_returnsPermissionResult() {
        // When
        val result = FloatingWindowManager.hasForegroundServicePermission(context)

        // Then
        assertTrue(
            "前台服务权限检查应返回有效结果",
            result is FloatingWindowManager.PermissionResult.Granted ||
            result is FloatingWindowManager.PermissionResult.Denied ||
            result is FloatingWindowManager.PermissionResult.Error
        )
    }

    @Test
    fun checkAllPermissions_returnsPermissionResult() {
        // When
        val result = FloatingWindowManager.checkAllPermissions(context)

        // Then
        assertTrue(
            "综合权限检查应返回有效结果",
            result is FloatingWindowManager.PermissionResult.Granted ||
            result is FloatingWindowManager.PermissionResult.Denied ||
            result is FloatingWindowManager.PermissionResult.Error
        )
    }

    // ==================== 服务管理测试 ====================

    @Test
    fun startService_withoutPermission_returnsPermissionDenied() {
        // Given
        val hasPermission = FloatingWindowManager.hasPermission(context)

        // When
        val result = FloatingWindowManager.startService(context)

        // Then
        if (hasPermission !is FloatingWindowManager.PermissionResult.Granted) {
            assertTrue(
                "无权限时启动服务应返回 PermissionDenied",
                result is FloatingWindowManager.ServiceStartResult.PermissionDenied
            )
        }
    }

    @Test
    fun stopService_whenNotRunning_returnsNotRunning() {
        // When
        val result = FloatingWindowManager.stopService(context)

        // Then
        // 服务未运行时应返回 NotRunning 或 Success
        assertTrue(
            "停止未运行的服务应返回 NotRunning 或 Success",
            result is FloatingWindowManager.ServiceStopResult.NotRunning ||
            result is FloatingWindowManager.ServiceStopResult.Success
        )
    }

    // ==================== 结果类型测试 ====================

    @Test
    fun permissionResult_granted_hasCorrectMessage() {
        // Given
        val result = FloatingWindowManager.PermissionResult.Granted

        // Then
        assertEquals("权限已授予", result.message)
    }

    @Test
    fun permissionResult_denied_hasCorrectMessage() {
        // Given
        val message = "测试拒绝消息"
        val result = FloatingWindowManager.PermissionResult.Denied(message)

        // Then
        assertEquals(message, result.message)
    }

    @Test
    fun permissionResult_error_hasCorrectMessage() {
        // Given
        val message = "测试错误消息"
        val result = FloatingWindowManager.PermissionResult.Error(message)

        // Then
        assertEquals(message, result.message)
    }

    @Test
    fun serviceStartResult_permissionDenied_hasCorrectMessage() {
        // Given
        val message = "权限被拒绝"
        val result = FloatingWindowManager.ServiceStartResult.PermissionDenied(message)

        // Then
        assertEquals(message, result.message)
    }

    @Test
    fun serviceStartResult_error_hasCorrectMessage() {
        // Given
        val message = "启动错误"
        val result = FloatingWindowManager.ServiceStartResult.Error(message)

        // Then
        assertEquals(message, result.message)
    }

    @Test
    fun serviceStopResult_error_hasCorrectMessage() {
        // Given
        val message = "停止错误"
        val result = FloatingWindowManager.ServiceStopResult.Error(message)

        // Then
        assertEquals(message, result.message)
    }

    // ==================== 常量测试 ====================

    @Test
    fun requestCodeOverlayPermission_hasCorrectValue() {
        // Then
        assertEquals(1001, FloatingWindowManager.REQUEST_CODE_OVERLAY_PERMISSION)
    }

    // ==================== 边界条件测试 ====================

    @Test
    fun hasPermission_multipleCallsReturnConsistentResult() {
        // When
        val result1 = FloatingWindowManager.hasPermission(context)
        val result2 = FloatingWindowManager.hasPermission(context)
        val result3 = FloatingWindowManager.hasPermission(context)

        // Then
        assertEquals("多次调用应返回一致结果", result1, result2)
        assertEquals("多次调用应返回一致结果", result2, result3)
    }

    @Test
    fun checkAllPermissions_includesOverlayPermission() {
        // Given
        val overlayResult = FloatingWindowManager.hasPermission(context)

        // When
        val allResult = FloatingWindowManager.checkAllPermissions(context)

        // Then
        // 如果悬浮窗权限被拒绝，综合检查也应该失败
        if (overlayResult is FloatingWindowManager.PermissionResult.Denied) {
            assertTrue(
                "悬浮窗权限被拒绝时，综合检查也应失败",
                allResult !is FloatingWindowManager.PermissionResult.Granted
            )
        }
    }
}
