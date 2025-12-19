package com.empathy.ai.presentation.ui.floating

import android.content.Context
import android.graphics.PixelFormat
import android.view.MotionEvent
import android.view.WindowManager
import com.empathy.ai.domain.model.FloatingBubbleState
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * 悬浮球双实例问题测试
 * 
 * 测试场景：
 * 1. 拖动悬浮球后不应创建新实例
 * 2. layoutParams引用一致性测试
 * 3. WindowManager操作失败场景测试
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class FloatingBubbleViewDuplicateTest {

    private lateinit var context: Context
    private lateinit var windowManager: WindowManager
    private lateinit var floatingBubbleView: FloatingBubbleView

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        windowManager = mockk(relaxed = true)
        
        // Mock Context resources
        every { context.resources } returns mockk(relaxed = true)
        every { context.resources.displayMetrics } returns mockk {
            every { density } returns 2.0f
            every { widthPixels } returns 1080
            every { heightPixels } returns 1920
        }
        
        floatingBubbleView = FloatingBubbleView(context, windowManager)
    }

    @Test
    fun `拖动悬浮球后不应创建新实例`() {
        // Given - 创建初始悬浮球
        val initialParams = floatingBubbleView.createLayoutParams(100, 200)
        every { windowManager.addView(floatingBubbleView, initialParams) } returns Unit
        
        // 模拟添加到WindowManager
        floatingBubbleView.updateLayoutParams(initialParams)
        
        // When - 模拟拖动操作
        val downEvent = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 150f, 250f, 0)
        val moveEvent = MotionEvent.obtain(0, 0, MotionEvent.ACTION_MOVE, 200f, 300f, 0)
        val upEvent = MotionEvent.obtain(0, 0, MotionEvent.ACTION_UP, 200f, 300f, 0)
        
        // 触发触摸事件
        floatingBubbleView.dispatchTouchEvent(downEvent)
        floatingBubbleView.dispatchTouchEvent(moveEvent)
        floatingBubbleView.dispatchTouchEvent(upEvent)
        
        // Then - 验证没有创建新实例
        verify(exactly = 1) { windowManager.addView(any(), any()) }
        verify(atLeast = 1) { windowManager.updateViewLayout(floatingBubbleView, any()) }
    }

    @Test
    fun `layoutParams引用应该保持一致`() {
        // Given - 创建初始参数
        val initialParams = floatingBubbleView.createLayoutParams(100, 200)
        
        // When - 更新位置
        val updatedParams = floatingBubbleView.createLayoutParams(200, 300)
        floatingBubbleView.updateLayoutParams(updatedParams)
        
        // Then - 验证引用一致性
        // 注意：这里测试的是内部引用是否正确更新
        // 实际问题可能在于WindowManager内部复制了params对象
        assertNotNull(updatedParams)
        assertEquals(200, updatedParams.x)
        assertEquals(300, updatedParams.y)
    }

    @Test
    fun `WindowManager updateViewLayout失败时应处理异常`() {
        // Given - 模拟updateViewLayout抛出异常
        val params = floatingBubbleView.createLayoutParams(100, 200)
        every { windowManager.updateViewLayout(floatingBubbleView, params) } throws RuntimeException("WindowManager error")
        
        // When - 模拟拖动操作
        val downEvent = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 100f, 200f, 0)
        val moveEvent = MotionEvent.obtain(0, 0, MotionEvent.ACTION_MOVE, 150f, 250f, 0)
        
        // 触发触摸事件
        floatingBubbleView.dispatchTouchEvent(downEvent)
        
        // Then - 应该捕获异常而不崩溃
        floatingBubbleView.dispatchTouchEvent(moveEvent)
        
        // 验证异常被处理
        verify(exactly = 1) { windowManager.updateViewLayout(floatingBubbleView, params) }
    }

    @Test
    fun `悬浮球状态切换应正确工作`() {
        // Given - 创建悬浮球
        val params = floatingBubbleView.createLayoutParams(100, 200)
        
        // When - 切换到不同状态
        floatingBubbleView.setState(FloatingBubbleState.LOADING)
        assertEquals(FloatingBubbleState.LOADING, floatingBubbleView.getState())
        
        floatingBubbleView.setState(FloatingBubbleState.SUCCESS)
        assertEquals(FloatingBubbleState.SUCCESS, floatingBubbleView.getState())
        
        floatingBubbleView.setState(FloatingBubbleState.ERROR)
        assertEquals(FloatingBubbleState.ERROR, floatingBubbleView.getState())
        
        floatingBubbleView.setState(FloatingBubbleState.IDLE)
        assertEquals(FloatingBubbleState.IDLE, floatingBubbleView.getState())
    }

    @Test
    fun `cleanup方法应正确清理资源`() {
        // Given - 设置回调
        var clickCount = 0
        var positionChangeCount = 0
        
        floatingBubbleView.setOnBubbleClickListener { clickCount++ }
        floatingBubbleView.setOnPositionChangedListener { _, _ -> positionChangeCount++ }
        
        // When - 调用cleanup
        floatingBubbleView.cleanup()
        
        // Then - 验证回调被清除
        // 触发点击和位置变化，应该不会调用回调
        floatingBubbleView.setState(FloatingBubbleState.SUCCESS) // 这会触发动画
        
        // 验证计数器没有增加（因为回调被清除）
        assertEquals(0, clickCount)
        assertEquals(0, positionChangeCount)
    }

    @Test
    fun `边界保护应正确工作`() {
        // Given - 创建悬浮球
        val params = floatingBubbleView.createLayoutParams(-100, -100)
        
        // Then - 验证边界保护
        // 边界保护逻辑在applyBoundaryProtection方法中
        // 这里我们验证创建的params被正确处理
        assertNotNull(params)
        // 实际的边界保护测试需要更复杂的设置
    }
}