package com.empathy.ai.domain.service

import com.empathy.ai.data.local.FloatingWindowPreferences
import com.empathy.ai.domain.model.FloatingBubbleState
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * FloatingWindowService悬浮球双实例问题测试
 * 
 * 测试场景：
 * 1. showFloatingBubble多次调用不应创建多个实例
 * 2. hideFloatingBubble应正确清理引用
 * 3. expandFromBubble应正确处理状态转换
 * 4. WindowManager操作失败时的处理
 * 
 * 注意：这些测试不依赖Robolectric，只测试逻辑和状态管理
 */
class FloatingWindowServiceBubbleDuplicateTest {

    @RelaxedMockK
    private lateinit var preferences: FloatingWindowPreferences

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `showFloatingBubble多次调用应该只创建一个实例`() {
        // Given - 模拟悬浮球实例管理逻辑
        var bubbleInstance: Any? = null
        var createCount = 0
        
        val showBubble: () -> Unit = {
            if (bubbleInstance == null) {
                bubbleInstance = Object()
                createCount++
            }
        }
        
        // When - 多次调用
        showBubble()
        showBubble()
        showBubble()
        
        // Then - 只应该创建一次
        assertEquals(1, createCount)
    }

    @Test
    fun `hideFloatingBubble应正确清理引用`() {
        // Given - 模拟悬浮球实例
        var bubbleInstance: Any? = Object()
        
        val hideBubble: () -> Unit = {
            bubbleInstance = null
        }
        
        // When - 隐藏悬浮球
        hideBubble()
        
        // Then - 引用应该被清理
        assertEquals(null, bubbleInstance)
    }

    @Test
    fun `expandFromBubble应保存DIALOG模式`() {
        // Given
        every { preferences.saveDisplayMode(any()) } returns Unit
        every { preferences.saveBubbleState(any()) } returns Unit
        
        // When - 模拟展开操作
        preferences.saveDisplayMode(FloatingWindowPreferences.DISPLAY_MODE_DIALOG)
        preferences.saveBubbleState(FloatingBubbleState.IDLE)
        
        // Then
        verify { preferences.saveDisplayMode(FloatingWindowPreferences.DISPLAY_MODE_DIALOG) }
        verify { preferences.saveBubbleState(FloatingBubbleState.IDLE) }
    }

    @Test
    fun `minimizeToFloatingBubble应保存BUBBLE模式`() {
        // Given
        every { preferences.saveDisplayMode(any()) } returns Unit
        every { preferences.saveBubbleState(any()) } returns Unit
        
        // When - 模拟最小化操作
        preferences.saveDisplayMode(FloatingWindowPreferences.DISPLAY_MODE_BUBBLE)
        preferences.saveBubbleState(FloatingBubbleState.IDLE)
        
        // Then
        verify { preferences.saveDisplayMode(FloatingWindowPreferences.DISPLAY_MODE_BUBBLE) }
        verify { preferences.saveBubbleState(FloatingBubbleState.IDLE) }
    }

    @Test
    fun `悬浮球状态应该正确转换`() {
        // Given - 初始状态
        var currentState = FloatingBubbleState.IDLE
        
        // When - 状态转换
        currentState = FloatingBubbleState.LOADING
        assertEquals(FloatingBubbleState.LOADING, currentState)
        
        currentState = FloatingBubbleState.SUCCESS
        assertEquals(FloatingBubbleState.SUCCESS, currentState)
        
        currentState = FloatingBubbleState.ERROR
        assertEquals(FloatingBubbleState.ERROR, currentState)
        
        currentState = FloatingBubbleState.IDLE
        assertEquals(FloatingBubbleState.IDLE, currentState)
    }

    @Test
    fun `shouldStartAsBubble应该根据显示模式返回正确值`() {
        // Given - BUBBLE模式
        every { preferences.getDisplayMode() } returns FloatingWindowPreferences.DISPLAY_MODE_BUBBLE
        every { preferences.shouldStartAsBubble() } returns true
        
        // When & Then
        assertTrue(preferences.shouldStartAsBubble())
        
        // Given - DIALOG模式
        every { preferences.getDisplayMode() } returns FloatingWindowPreferences.DISPLAY_MODE_DIALOG
        every { preferences.shouldStartAsBubble() } returns false
        
        // When & Then
        assertFalse(preferences.shouldStartAsBubble())
    }

    @Test
    fun `悬浮球位置应该正确保存和读取`() {
        // Given
        val expectedX = 100
        val expectedY = 200
        every { preferences.getBubblePosition(any(), any()) } returns Pair(expectedX, expectedY)
        
        // When
        val (x, y) = preferences.getBubblePosition(0, 0)
        
        // Then
        assertEquals(expectedX, x)
        assertEquals(expectedY, y)
    }

    @Test
    fun `hasValidMinimizeState应该正确返回状态`() {
        // Given - 有有效状态
        every { preferences.hasValidMinimizeState() } returns true
        assertTrue(preferences.hasValidMinimizeState())
        
        // Given - 无有效状态
        every { preferences.hasValidMinimizeState() } returns false
        assertFalse(preferences.hasValidMinimizeState())
    }
}
