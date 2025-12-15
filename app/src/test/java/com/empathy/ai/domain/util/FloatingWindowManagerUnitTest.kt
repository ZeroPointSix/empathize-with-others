package com.empathy.ai.domain.util

import org.junit.Assert.*
import org.junit.Test

/**
 * 悬浮窗管理器单元测试
 *
 * 测试范围：
 * - PermissionResult 密封类
 * - ServiceStartResult 密封类
 * - ServiceStopResult 密封类
 * - 常量值
 */
class FloatingWindowManagerUnitTest {

    // ==================== PermissionResult 测试 ====================

    @Test
    fun `PermissionResult Granted has correct message`() {
        // Given
        val result = FloatingWindowManager.PermissionResult.Granted

        // Then
        assertEquals("权限已授予", result.message)
    }

    @Test
    fun `PermissionResult Denied stores custom message`() {
        // Given
        val customMessage = "自定义拒绝消息"

        // When
        val result = FloatingWindowManager.PermissionResult.Denied(customMessage)

        // Then
        assertEquals(customMessage, result.message)
    }

    @Test
    fun `PermissionResult Error stores custom message`() {
        // Given
        val customMessage = "自定义错误消息"

        // When
        val result = FloatingWindowManager.PermissionResult.Error(customMessage)

        // Then
        assertEquals(customMessage, result.message)
    }

    @Test
    fun `PermissionResult Denied with empty message`() {
        // When
        val result = FloatingWindowManager.PermissionResult.Denied("")

        // Then
        assertEquals("", result.message)
    }

    @Test
    fun `PermissionResult Error with empty message`() {
        // When
        val result = FloatingWindowManager.PermissionResult.Error("")

        // Then
        assertEquals("", result.message)
    }

    @Test
    fun `PermissionResult types are distinguishable`() {
        // Given
        val granted = FloatingWindowManager.PermissionResult.Granted
        val denied = FloatingWindowManager.PermissionResult.Denied("denied")
        val error = FloatingWindowManager.PermissionResult.Error("error")

        // Then
        assertTrue(granted is FloatingWindowManager.PermissionResult.Granted)
        assertTrue(denied is FloatingWindowManager.PermissionResult.Denied)
        assertTrue(error is FloatingWindowManager.PermissionResult.Error)
        
        assertFalse(granted is FloatingWindowManager.PermissionResult.Denied)
        assertFalse(denied is FloatingWindowManager.PermissionResult.Granted)
        assertFalse(error is FloatingWindowManager.PermissionResult.Granted)
    }

    // ==================== ServiceStartResult 测试 ====================

    @Test
    fun `ServiceStartResult Success is singleton`() {
        // Given
        val result1 = FloatingWindowManager.ServiceStartResult.Success
        val result2 = FloatingWindowManager.ServiceStartResult.Success

        // Then
        assertSame(result1, result2)
    }

    @Test
    fun `ServiceStartResult PermissionDenied stores message`() {
        // Given
        val message = "权限被拒绝"

        // When
        val result = FloatingWindowManager.ServiceStartResult.PermissionDenied(message)

        // Then
        assertEquals(message, result.message)
    }

    @Test
    fun `ServiceStartResult Error stores message`() {
        // Given
        val message = "启动错误"

        // When
        val result = FloatingWindowManager.ServiceStartResult.Error(message)

        // Then
        assertEquals(message, result.message)
    }

    @Test
    fun `ServiceStartResult types are distinguishable`() {
        // Given
        val success = FloatingWindowManager.ServiceStartResult.Success
        val permissionDenied = FloatingWindowManager.ServiceStartResult.PermissionDenied("denied")
        val error = FloatingWindowManager.ServiceStartResult.Error("error")

        // Then
        assertTrue(success is FloatingWindowManager.ServiceStartResult.Success)
        assertTrue(permissionDenied is FloatingWindowManager.ServiceStartResult.PermissionDenied)
        assertTrue(error is FloatingWindowManager.ServiceStartResult.Error)
    }

    @Test
    fun `ServiceStartResult PermissionDenied with special characters`() {
        // Given
        val message = "权限被拒绝：用户点击了\"取消\"按钮"

        // When
        val result = FloatingWindowManager.ServiceStartResult.PermissionDenied(message)

        // Then
        assertEquals(message, result.message)
    }

    // ==================== ServiceStopResult 测试 ====================

    @Test
    fun `ServiceStopResult Success is singleton`() {
        // Given
        val result1 = FloatingWindowManager.ServiceStopResult.Success
        val result2 = FloatingWindowManager.ServiceStopResult.Success

        // Then
        assertSame(result1, result2)
    }

    @Test
    fun `ServiceStopResult NotRunning is singleton`() {
        // Given
        val result1 = FloatingWindowManager.ServiceStopResult.NotRunning
        val result2 = FloatingWindowManager.ServiceStopResult.NotRunning

        // Then
        assertSame(result1, result2)
    }

    @Test
    fun `ServiceStopResult Error stores message`() {
        // Given
        val message = "停止错误"

        // When
        val result = FloatingWindowManager.ServiceStopResult.Error(message)

        // Then
        assertEquals(message, result.message)
    }

    @Test
    fun `ServiceStopResult types are distinguishable`() {
        // Given
        val success = FloatingWindowManager.ServiceStopResult.Success
        val notRunning = FloatingWindowManager.ServiceStopResult.NotRunning
        val error = FloatingWindowManager.ServiceStopResult.Error("error")

        // Then
        assertTrue(success is FloatingWindowManager.ServiceStopResult.Success)
        assertTrue(notRunning is FloatingWindowManager.ServiceStopResult.NotRunning)
        assertTrue(error is FloatingWindowManager.ServiceStopResult.Error)
    }

    // ==================== 常量测试 ====================

    @Test
    fun `REQUEST_CODE_OVERLAY_PERMISSION has correct value`() {
        // Then
        assertEquals(1001, FloatingWindowManager.REQUEST_CODE_OVERLAY_PERMISSION)
    }

    @Test
    fun `REQUEST_CODE_OVERLAY_PERMISSION is positive`() {
        // Then
        assertTrue(FloatingWindowManager.REQUEST_CODE_OVERLAY_PERMISSION > 0)
    }

    // ==================== 数据类相等性测试 ====================

    @Test
    fun `PermissionResult Denied equality`() {
        // Given
        val result1 = FloatingWindowManager.PermissionResult.Denied("message")
        val result2 = FloatingWindowManager.PermissionResult.Denied("message")
        val result3 = FloatingWindowManager.PermissionResult.Denied("different")

        // Then
        assertEquals(result1, result2)
        assertNotEquals(result1, result3)
    }

    @Test
    fun `PermissionResult Error equality`() {
        // Given
        val result1 = FloatingWindowManager.PermissionResult.Error("message")
        val result2 = FloatingWindowManager.PermissionResult.Error("message")
        val result3 = FloatingWindowManager.PermissionResult.Error("different")

        // Then
        assertEquals(result1, result2)
        assertNotEquals(result1, result3)
    }

    @Test
    fun `ServiceStartResult PermissionDenied equality`() {
        // Given
        val result1 = FloatingWindowManager.ServiceStartResult.PermissionDenied("message")
        val result2 = FloatingWindowManager.ServiceStartResult.PermissionDenied("message")
        val result3 = FloatingWindowManager.ServiceStartResult.PermissionDenied("different")

        // Then
        assertEquals(result1, result2)
        assertNotEquals(result1, result3)
    }

    @Test
    fun `ServiceStartResult Error equality`() {
        // Given
        val result1 = FloatingWindowManager.ServiceStartResult.Error("message")
        val result2 = FloatingWindowManager.ServiceStartResult.Error("message")
        val result3 = FloatingWindowManager.ServiceStartResult.Error("different")

        // Then
        assertEquals(result1, result2)
        assertNotEquals(result1, result3)
    }

    @Test
    fun `ServiceStopResult Error equality`() {
        // Given
        val result1 = FloatingWindowManager.ServiceStopResult.Error("message")
        val result2 = FloatingWindowManager.ServiceStopResult.Error("message")
        val result3 = FloatingWindowManager.ServiceStopResult.Error("different")

        // Then
        assertEquals(result1, result2)
        assertNotEquals(result1, result3)
    }

    // ==================== when 表达式覆盖测试 ====================

    @Test
    fun `PermissionResult can be used in when expression`() {
        // Given
        val results = listOf(
            FloatingWindowManager.PermissionResult.Granted,
            FloatingWindowManager.PermissionResult.Denied("denied"),
            FloatingWindowManager.PermissionResult.Error("error")
        )

        // When & Then
        results.forEach { result ->
            val message = when (result) {
                is FloatingWindowManager.PermissionResult.Granted -> "granted"
                is FloatingWindowManager.PermissionResult.Denied -> "denied"
                is FloatingWindowManager.PermissionResult.Error -> "error"
            }
            assertTrue(message.isNotEmpty())
        }
    }

    @Test
    fun `ServiceStartResult can be used in when expression`() {
        // Given
        val results = listOf(
            FloatingWindowManager.ServiceStartResult.Success,
            FloatingWindowManager.ServiceStartResult.PermissionDenied("denied"),
            FloatingWindowManager.ServiceStartResult.Error("error")
        )

        // When & Then
        results.forEach { result ->
            val message = when (result) {
                is FloatingWindowManager.ServiceStartResult.Success -> "success"
                is FloatingWindowManager.ServiceStartResult.PermissionDenied -> "denied"
                is FloatingWindowManager.ServiceStartResult.Error -> "error"
            }
            assertTrue(message.isNotEmpty())
        }
    }

    @Test
    fun `ServiceStopResult can be used in when expression`() {
        // Given
        val results = listOf(
            FloatingWindowManager.ServiceStopResult.Success,
            FloatingWindowManager.ServiceStopResult.NotRunning,
            FloatingWindowManager.ServiceStopResult.Error("error")
        )

        // When & Then
        results.forEach { result ->
            val message = when (result) {
                is FloatingWindowManager.ServiceStopResult.Success -> "success"
                is FloatingWindowManager.ServiceStopResult.NotRunning -> "not running"
                is FloatingWindowManager.ServiceStopResult.Error -> "error"
            }
            assertTrue(message.isNotEmpty())
        }
    }
}
