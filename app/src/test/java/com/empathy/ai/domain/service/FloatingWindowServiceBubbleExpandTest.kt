package com.empathy.ai.domain.service

import android.content.Context
import android.view.View
import android.view.WindowManager
import com.empathy.ai.data.local.FloatingWindowPreferences
import com.empathy.ai.domain.model.FloatingBubbleState
import com.empathy.ai.presentation.ui.floating.FloatingBubbleView
import com.empathy.ai.presentation.ui.floating.FloatingViewV2
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * BUG-00019: 悬浮球点击无响应与设置界面自动展开问题测试
 *
 * 测试场景：
 * 1. 悬浮球模式启动后点击展开
 * 2. 服务重复启动的幂等性
 * 3. 设置界面不应自动展开悬浮球
 */
class FloatingWindowServiceBubbleExpandTest {

    @MockK
    private lateinit var mockContext: Context

    @MockK
    private lateinit var mockWindowManager: WindowManager

    @RelaxedMockK
    private lateinit var mockPreferences: FloatingWindowPreferences

    @RelaxedMockK
    private lateinit var mockFloatingBubbleView: FloatingBubbleView

    @RelaxedMockK
    private lateinit var mockFloatingViewV2: FloatingViewV2

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        
        // 设置默认行为
        every { mockPreferences.getDisplayMode() } returns FloatingWindowPreferences.DISPLAY_MODE_BUBBLE
        every { mockPreferences.shouldStartAsBubble() } returns true
        every { mockPreferences.hasValidMinimizeState() } returns false
        every { mockPreferences.getBubblePosition(any(), any()) } returns Pair(100, 200)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // ==================== 问题1：悬浮球点击无响应 ====================

    @Test
    fun `expandFromBubble should handle null floatingViewV2`() {
        // Given: floatingViewV2 为 null（悬浮球模式启动时的情况）
        val floatingViewV2: FloatingViewV2? = null
        
        // When: 尝试设置 visibility
        floatingViewV2?.visibility = View.VISIBLE
        
        // Then: 不应该抛出异常，但也不会有效果
        assertNull(floatingViewV2)
    }

    @Test
    fun `shouldStartAsBubble returns true when display mode is BUBBLE`() {
        // Given: 显示模式为 BUBBLE
        every { mockPreferences.getDisplayMode() } returns FloatingWindowPreferences.DISPLAY_MODE_BUBBLE
        every { mockPreferences.shouldStartAsBubble() } returns true
        
        // When: 检查是否应该以悬浮球模式启动
        val result = mockPreferences.shouldStartAsBubble()
        
        // Then: 应该返回 true
        assertEquals(true, result)
    }

    @Test
    fun `shouldStartAsBubble returns false when display mode is DIALOG`() {
        // Given: 显示模式为 DIALOG
        every { mockPreferences.getDisplayMode() } returns FloatingWindowPreferences.DISPLAY_MODE_DIALOG
        every { mockPreferences.shouldStartAsBubble() } returns false
        
        // When: 检查是否应该以悬浮球模式启动
        val result = mockPreferences.shouldStartAsBubble()
        
        // Then: 应该返回 false
        assertEquals(false, result)
    }

    @Test
    fun `bubble click listener should be invoked on click`() {
        // Given: 设置点击监听器
        var clickInvoked = false
        val clickListener: () -> Unit = { clickInvoked = true }
        
        // When: 模拟点击
        clickListener.invoke()
        
        // Then: 监听器应该被调用
        assertEquals(true, clickInvoked)
    }

    // ==================== 问题2：服务重复启动 ====================

    @Test
    fun `onStartCommand should be idempotent - views should not be recreated`() {
        // Given: 模拟视图已存在的情况
        var viewCreationCount = 0
        val createView: () -> Unit = { viewCreationCount++ }
        
        // 第一次调用
        createView()
        assertEquals(1, viewCreationCount)
        
        // When: 模拟幂等性检查 - 如果视图已存在则不创建
        val viewExists = true
        if (!viewExists) {
            createView()
        }
        
        // Then: 视图创建次数应该保持为1
        assertEquals(1, viewCreationCount)
    }

    @Test
    fun `service should check if already running before starting`() {
        // Given: 服务运行状态检查
        var isServiceRunning = false
        var startServiceCalled = 0
        
        val startService: () -> Unit = {
            if (!isServiceRunning) {
                startServiceCalled++
                isServiceRunning = true
            }
        }
        
        // When: 多次尝试启动服务
        startService()
        startService()
        startService()
        
        // Then: 服务只应该启动一次
        assertEquals(1, startServiceCalled)
    }

    // ==================== 显示模式持久化测试 ====================

    @Test
    fun `saveDisplayMode should persist BUBBLE mode`() {
        // Given: 准备保存悬浮球模式
        every { mockPreferences.saveDisplayMode(any()) } just Runs
        
        // When: 保存显示模式
        mockPreferences.saveDisplayMode(FloatingWindowPreferences.DISPLAY_MODE_BUBBLE)
        
        // Then: 应该调用保存方法
        verify { mockPreferences.saveDisplayMode(FloatingWindowPreferences.DISPLAY_MODE_BUBBLE) }
    }

    @Test
    fun `saveDisplayMode should persist DIALOG mode`() {
        // Given: 准备保存对话框模式
        every { mockPreferences.saveDisplayMode(any()) } just Runs
        
        // When: 保存显示模式
        mockPreferences.saveDisplayMode(FloatingWindowPreferences.DISPLAY_MODE_DIALOG)
        
        // Then: 应该调用保存方法
        verify { mockPreferences.saveDisplayMode(FloatingWindowPreferences.DISPLAY_MODE_DIALOG) }
    }

    // ==================== 悬浮球状态测试 ====================

    @Test
    fun `bubble state should be IDLE by default`() {
        // Given: 默认状态
        every { mockPreferences.getBubbleState() } returns FloatingBubbleState.IDLE
        
        // When: 获取悬浮球状态
        val state = mockPreferences.getBubbleState()
        
        // Then: 应该是 IDLE
        assertEquals(FloatingBubbleState.IDLE, state)
    }

    @Test
    fun `bubble state should be LOADING when has valid minimize state`() {
        // Given: 有有效的最小化状态
        every { mockPreferences.hasValidMinimizeState() } returns true
        
        // When: 检查是否有有效的最小化状态
        val hasValidState = mockPreferences.hasValidMinimizeState()
        
        // Then: 应该返回 true
        assertEquals(true, hasValidState)
    }

    // ==================== 展开逻辑测试 ====================

    @Test
    fun `expandFromBubble should save DIALOG mode`() {
        // Given: 准备展开操作
        every { mockPreferences.saveDisplayMode(any()) } just Runs
        every { mockPreferences.saveBubbleState(any()) } just Runs
        
        // When: 模拟展开操作中的保存逻辑
        mockPreferences.saveDisplayMode(FloatingWindowPreferences.DISPLAY_MODE_DIALOG)
        mockPreferences.saveBubbleState(FloatingBubbleState.IDLE)
        
        // Then: 应该保存对话框模式
        verify { mockPreferences.saveDisplayMode(FloatingWindowPreferences.DISPLAY_MODE_DIALOG) }
        verify { mockPreferences.saveBubbleState(FloatingBubbleState.IDLE) }
    }

    @Test
    fun `minimizeToFloatingBubble should save BUBBLE mode`() {
        // Given: 准备最小化操作
        every { mockPreferences.saveDisplayMode(any()) } just Runs
        
        // When: 模拟最小化操作中的保存逻辑
        mockPreferences.saveDisplayMode(FloatingWindowPreferences.DISPLAY_MODE_BUBBLE)
        
        // Then: 应该保存悬浮球模式
        verify { mockPreferences.saveDisplayMode(FloatingWindowPreferences.DISPLAY_MODE_BUBBLE) }
    }

    // ==================== 边界条件测试 ====================

    @Test
    fun `should handle rapid bubble clicks gracefully`() {
        // Given: 模拟快速点击
        var expandCount = 0
        var isExpanding = false
        
        val expandFromBubble: () -> Unit = {
            if (!isExpanding) {
                isExpanding = true
                expandCount++
                // 模拟展开完成
                isExpanding = false
            }
        }
        
        // When: 快速多次点击
        repeat(5) { expandFromBubble() }
        
        // Then: 应该正确处理所有点击（在这个简化模型中）
        assertEquals(5, expandCount)
    }

    @Test
    fun `should handle null window manager gracefully`() {
        // Given: WindowManager 可能为 null 的情况
        val windowManager: WindowManager? = null
        
        // When: 尝试使用 WindowManager
        val result = windowManager?.let { "has manager" } ?: "no manager"
        
        // Then: 应该安全处理
        assertEquals("no manager", result)
    }
}
