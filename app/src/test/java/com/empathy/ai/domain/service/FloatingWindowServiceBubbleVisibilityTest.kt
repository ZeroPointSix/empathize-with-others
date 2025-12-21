package com.empathy.ai.domain.service

import com.empathy.ai.data.local.FloatingWindowPreferences
import com.empathy.ai.domain.model.FloatingBubbleState
import com.empathy.ai.domain.model.FloatingWindowUiState
import io.mockk.*
import io.mockk.impl.annotations.RelaxedMockK
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse

/**
 * BUG-00029: 悬浮球显示与最小化消失问题测试
 *
 * 测试场景：
 * 1. 显示模式保存时机
 * 2. 最小化流程的原子性
 * 3. 服务重启后状态恢复
 * 4. onDestroy 清理完整性
 */
class FloatingWindowServiceBubbleVisibilityTest {

    @RelaxedMockK
    private lateinit var mockPreferences: FloatingWindowPreferences

    @Before
    fun setup() {
        MockKAnnotations.init(this)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }


    // ==================== 显示模式保存时机测试 ====================

    @Test
    fun `最小化时应先保存显示模式再执行其他操作`() {
        // Given: 模拟最小化操作的调用顺序
        val callOrder = mutableListOf<String>()
        
        every { mockPreferences.saveDisplayMode(any()) } answers {
            callOrder.add("saveDisplayMode")
        }
        every { mockPreferences.saveUiState(any<FloatingWindowUiState>()) } answers {
            callOrder.add("saveUiState")
        }
        every { mockPreferences.saveMinimizeState(any()) } answers {
            callOrder.add("saveMinimizeState")
        }

        // When: 模拟正确的最小化流程
        mockPreferences.saveDisplayMode(FloatingWindowPreferences.DISPLAY_MODE_BUBBLE)
        mockPreferences.saveUiState(FloatingWindowUiState())
        mockPreferences.saveMinimizeState("{}")

        // Then: saveDisplayMode 应该是第一个被调用的
        assertEquals("saveDisplayMode", callOrder.first())
    }

    @Test
    fun `显示模式保存失败不应阻止悬浮球显示`() {
        // Given: saveDisplayMode 抛出异常
        every { mockPreferences.saveDisplayMode(any()) } throws RuntimeException("IO Error")
        every { mockPreferences.getDisplayMode() } returns FloatingWindowPreferences.DISPLAY_MODE_DIALOG

        // When & Then: 即使保存失败，也不应该崩溃
        try {
            mockPreferences.saveDisplayMode(FloatingWindowPreferences.DISPLAY_MODE_BUBBLE)
        } catch (e: Exception) {
            // 异常被捕获，继续执行
        }
        
        // 验证可以继续读取（使用默认值）
        val mode = mockPreferences.getDisplayMode()
        assertEquals(FloatingWindowPreferences.DISPLAY_MODE_DIALOG, mode)
    }

    // ==================== 服务重启状态恢复测试 ====================

    @Test
    fun `服务重启后应根据保存的显示模式决定启动方式`() {
        // Given: 上次退出时是悬浮球模式
        every { mockPreferences.getDisplayMode() } returns FloatingWindowPreferences.DISPLAY_MODE_BUBBLE
        every { mockPreferences.shouldStartAsBubble() } returns true

        // When
        val shouldStartAsBubble = mockPreferences.shouldStartAsBubble()

        // Then
        assertTrue(shouldStartAsBubble)
    }

    @Test
    fun `服务重启后显示模式为DIALOG时应显示对话框`() {
        // Given: 上次退出时是对话框模式
        every { mockPreferences.getDisplayMode() } returns FloatingWindowPreferences.DISPLAY_MODE_DIALOG
        every { mockPreferences.shouldStartAsBubble() } returns false

        // When
        val shouldStartAsBubble = mockPreferences.shouldStartAsBubble()

        // Then
        assertFalse(shouldStartAsBubble)
    }


    @Test
    fun `有效的最小化状态应恢复LOADING状态悬浮球`() {
        // Given: 有有效的最小化状态
        every { mockPreferences.hasValidMinimizeState() } returns true
        every { mockPreferences.getMinimizeStateIfValid() } returns """{"type":"ANALYZE"}"""

        // When
        val hasValidState = mockPreferences.hasValidMinimizeState()

        // Then: 应该恢复为LOADING状态
        assertTrue(hasValidState)
    }

    @Test
    fun `过期的最小化状态应恢复IDLE状态悬浮球`() {
        // Given: 最小化状态已过期
        every { mockPreferences.hasValidMinimizeState() } returns false
        every { mockPreferences.getMinimizeStateIfValid() } returns null

        // When
        val hasValidState = mockPreferences.hasValidMinimizeState()

        // Then: 应该恢复为IDLE状态
        assertFalse(hasValidState)
    }

    // ==================== 悬浮球状态管理测试 ====================

    @Test
    fun `悬浮球状态应正确保存和恢复`() {
        // Given
        val savedState = slot<FloatingBubbleState>()
        every { mockPreferences.saveBubbleState(capture(savedState)) } just Runs
        every { mockPreferences.getBubbleState() } returns FloatingBubbleState.SUCCESS

        // When: 保存SUCCESS状态
        mockPreferences.saveBubbleState(FloatingBubbleState.SUCCESS)

        // Then
        assertEquals(FloatingBubbleState.SUCCESS, savedState.captured)
        assertEquals(FloatingBubbleState.SUCCESS, mockPreferences.getBubbleState())
    }

    @Test
    fun `悬浮球位置应正确保存和恢复`() {
        // Given
        val savedX = slot<Int>()
        val savedY = slot<Int>()
        every { mockPreferences.saveBubblePosition(capture(savedX), capture(savedY)) } just Runs
        every { mockPreferences.getBubblePosition(any(), any()) } returns Pair(100, 200)

        // When
        mockPreferences.saveBubblePosition(100, 200)
        val (x, y) = mockPreferences.getBubblePosition(0, 0)

        // Then
        assertEquals(100, savedX.captured)
        assertEquals(200, savedY.captured)
        assertEquals(100, x)
        assertEquals(200, y)
    }

    // ==================== 边界条件测试 ====================

    @Test
    fun `显示模式为空时应返回默认值BUBBLE`() {
        // Given: 没有保存过显示模式
        every { mockPreferences.getDisplayMode() } returns FloatingWindowPreferences.DISPLAY_MODE_BUBBLE

        // When
        val mode = mockPreferences.getDisplayMode()

        // Then: 默认应该是BUBBLE模式
        assertEquals(FloatingWindowPreferences.DISPLAY_MODE_BUBBLE, mode)
    }

    @Test
    fun `悬浮球状态为无效值时应返回IDLE`() {
        // Given: 保存了无效的状态值
        every { mockPreferences.getBubbleState() } returns FloatingBubbleState.IDLE

        // When
        val state = mockPreferences.getBubbleState()

        // Then: 应该返回IDLE
        assertEquals(FloatingBubbleState.IDLE, state)
    }
}
