package com.empathy.ai.domain.model

import org.junit.Assert.*
import org.junit.Test

/**
 * MinimizeError 错误类测试
 * 
 * 验证错误类的基本功能和消息格式
 */
class MinimizeErrorTest {
    
    @Test
    fun `创建 MinimizeFailed 错误`() {
        // Given
        val reason = "窗口管理器不可用"
        
        // When
        val error = MinimizeError.MinimizeFailed(reason)
        
        // Then
        assertEquals("最小化失败: $reason", error.message)
        assertTrue(error is MinimizeError)
        assertTrue(error is Exception)
    }
    
    @Test
    fun `创建 RestoreFailed 错误`() {
        // Given
        val reason = "指示器视图已被移除"
        
        // When
        val error = MinimizeError.RestoreFailed(reason)
        
        // Then
        assertEquals("恢复失败: $reason", error.message)
        assertTrue(error is MinimizeError)
        assertTrue(error is Exception)
    }
    
    @Test
    fun `创建 NotificationFailed 错误`() {
        // Given
        val reason = "通知权限未授予"
        
        // When
        val error = MinimizeError.NotificationFailed(reason)
        
        // Then
        assertEquals("通知失败: $reason", error.message)
        assertTrue(error is MinimizeError)
        assertTrue(error is Exception)
    }
    
    @Test
    fun `错误消息应包含原因`() {
        // Given
        val reasons = listOf(
            "内存不足",
            "系统资源不可用",
            "权限被拒绝"
        )
        
        // When & Then
        reasons.forEach { reason ->
            val minimizeError = MinimizeError.MinimizeFailed(reason)
            assertTrue(minimizeError.message?.contains(reason) == true)
            
            val restoreError = MinimizeError.RestoreFailed(reason)
            assertTrue(restoreError.message?.contains(reason) == true)
            
            val notificationError = MinimizeError.NotificationFailed(reason)
            assertTrue(notificationError.message?.contains(reason) == true)
        }
    }
    
    @Test
    fun `可以作为异常抛出和捕获`() {
        // Given
        val reason = "测试错误"
        
        // When & Then - MinimizeFailed
        try {
            throw MinimizeError.MinimizeFailed(reason)
        } catch (e: MinimizeError.MinimizeFailed) {
            assertEquals("最小化失败: $reason", e.message)
        } catch (e: Exception) {
            fail("应该捕获 MinimizeFailed 类型")
        }
        
        // When & Then - RestoreFailed
        try {
            throw MinimizeError.RestoreFailed(reason)
        } catch (e: MinimizeError.RestoreFailed) {
            assertEquals("恢复失败: $reason", e.message)
        } catch (e: Exception) {
            fail("应该捕获 RestoreFailed 类型")
        }
        
        // When & Then - NotificationFailed
        try {
            throw MinimizeError.NotificationFailed(reason)
        } catch (e: MinimizeError.NotificationFailed) {
            assertEquals("通知失败: $reason", e.message)
        } catch (e: Exception) {
            fail("应该捕获 NotificationFailed 类型")
        }
    }
    
    @Test
    fun `可以作为通用 MinimizeError 捕获`() {
        // Given
        val errors = listOf(
            MinimizeError.MinimizeFailed("原因1"),
            MinimizeError.RestoreFailed("原因2"),
            MinimizeError.NotificationFailed("原因3")
        )
        
        // When & Then
        errors.forEach { error ->
            try {
                throw error
            } catch (e: MinimizeError) {
                assertNotNull(e.message)
                assertTrue(e.message?.isNotEmpty() == true)
            } catch (e: Exception) {
                fail("应该捕获 MinimizeError 类型")
            }
        }
    }
    
    @Test
    fun `可以作为通用 Exception 捕获`() {
        // Given
        val error = MinimizeError.MinimizeFailed("测试")
        
        // When & Then
        try {
            throw error
        } catch (e: Exception) {
            assertTrue(e is MinimizeError)
            assertNotNull(e.message)
        }
    }
    
    @Test
    fun `sealed class 类型检查`() {
        // Given
        val minimizeFailed: MinimizeError = MinimizeError.MinimizeFailed("测试1")
        val restoreFailed: MinimizeError = MinimizeError.RestoreFailed("测试2")
        val notificationFailed: MinimizeError = MinimizeError.NotificationFailed("测试3")
        
        // When & Then
        when (minimizeFailed) {
            is MinimizeError.MinimizeFailed -> assertTrue(true)
            is MinimizeError.RestoreFailed -> fail("类型不匹配")
            is MinimizeError.NotificationFailed -> fail("类型不匹配")
        }
        
        when (restoreFailed) {
            is MinimizeError.MinimizeFailed -> fail("类型不匹配")
            is MinimizeError.RestoreFailed -> assertTrue(true)
            is MinimizeError.NotificationFailed -> fail("类型不匹配")
        }
        
        when (notificationFailed) {
            is MinimizeError.MinimizeFailed -> fail("类型不匹配")
            is MinimizeError.RestoreFailed -> fail("类型不匹配")
            is MinimizeError.NotificationFailed -> assertTrue(true)
        }
    }
}
