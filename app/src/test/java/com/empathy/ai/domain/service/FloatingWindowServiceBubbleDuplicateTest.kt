package com.empathy.ai.domain.service

import android.content.Context
import android.content.Intent
import android.view.WindowManager
import com.empathy.ai.data.local.FloatingWindowPreferences
import com.empathy.ai.domain.model.FloatingBubbleState
import com.empathy.ai.domain.model.ActionType
import com.empathy.ai.presentation.ui.floating.FloatingBubbleView
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import io.mockk.verifySequence
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * FloatingWindowService悬浮球双实例问题测试
 * 
 * 测试场景：
 * 1. showFloatingBubble多次调用不应创建多个实例
 * 2. hideFloatingBubble应正确清理引用
 * 3. expandFromBubble应正确处理状态转换
 * 4. WindowManager操作失败时的处理
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class FloatingWindowServiceBubbleDuplicateTest {

    private lateinit var service: FloatingWindowService
    private lateinit var context: Context
    private lateinit var windowManager: WindowManager
    private lateinit var preferences: FloatingWindowPreferences

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        windowManager = mockk(relaxed = true)
        preferences = mockk(relaxed = true)
        
        // 创建服务实例（使用反射访问私有字段）
        service = FloatingWindowService()
        
        // Mock依赖
        every { context.getSystemService(Context.WINDOW_SERVICE) } returns windowManager
        every { context.resources } returns mockk(relaxed = true)
        every { context.resources.displayMetrics } returns mockk {
            every { density } returns 2.0f
            every { widthPixels } returns 1080
            every { heightPixels } returns 1920
        }
    }

    @Test
    fun `showFloatingBubble多次调用不应创建多个实例`() {
        // Given - Mock preferences
        every { preferences.getBubblePosition(any(), any()) } returns Pair(100, 200)
        
        // When - 第一次调用showFloatingBubble
        service::class.java.getDeclaredMethod("showFloatingBubble", FloatingBubbleState::class.java)
            .apply { isAccessible = true }
            .invoke(service, FloatingBubbleState.IDLE)
        
        // Then - 第二次调用应该只更新状态，不创建新实例
        service::class.java.getDeclaredMethod("showFloatingBubble", FloatingBubbleState::class.java)
            .apply { isAccessible = true }
            .invoke(service, FloatingBubbleState.LOADING)
        
        // 验证只调用了一次addView
        verify(exactly = 1) { windowManager.addView(any(), any()) }
    }

    @Test
    fun `hideFloatingBubble应正确清理引用`() {
        // Given - 先显示悬浮球
        every { preferences.getBubblePosition(any(), any()) } returns Pair(100, 200)
        
        service::class.java.getDeclaredMethod("showFloatingBubble", FloatingBubbleState::class.java)
            .apply { isAccessible = true }
            .invoke(service, FloatingBubbleState.IDLE)
        
        // When - 隐藏悬浮球
        service::class.java.getDeclaredMethod("hideFloatingBubble")
            .apply { isAccessible = true }
            .invoke(service)
        
        // Then - 验证removeView被调用
        verify { windowManager.removeView(any()) }
        
        // 验证floatingBubbleView被置为null（通过反射检查）
        val floatingBubbleViewField = service::class.java.getDeclaredField("floatingBubbleView")
        floatingBubbleViewField.isAccessible = true
        val floatingBubbleView = floatingBubbleViewField.get(service)
        
        assertNull(floatingBubbleView, "hideFloatingBubble后floatingBubbleView应为null")
    }

    @Test
    fun `expandFromBubble应正确处理状态转换`() {
        // Given - 先显示悬浮球
        every { preferences.getBubblePosition(any(), any()) } returns Pair(100, 200)
        every { preferences.shouldStartAsBubble() } returns true
        
        service::class.java.getDeclaredMethod("showFloatingBubble", FloatingBubbleState::class.java)
            .apply { isAccessible = true }
            .invoke(service, FloatingBubbleState.IDLE)
        
        // When - 从悬浮球展开
        service::class.java.getDeclaredMethod("expandFromBubble")
            .apply { isAccessible = true }
            .invoke(service)
        
        // Then - 验证状态转换
        verify { preferences.saveDisplayMode("DIALOG") }
        verify { preferences.saveBubbleState(FloatingBubbleState.IDLE) }
    }

    @Test
    fun `WindowManager addView失败时应正确处理`() {
        // Given - Mock addView抛出异常
        every { preferences.getBubblePosition(any(), any()) } returns Pair(100, 200)
        every { windowManager.addView(any(), any()) } throws RuntimeException("Permission denied")
        
        // When - 尝试显示悬浮球
        try {
            service::class.java.getDeclaredMethod("showFloatingBubble", FloatingBubbleState::class.java)
                .apply { isAccessible = true }
                .invoke(service, FloatingBubbleState.IDLE)
        } catch (e: Exception) {
            // 预期会有异常
        }
        
        // Then - 验证floatingBubbleView被置为null
        val floatingBubbleViewField = service::class.java.getDeclaredField("floatingBubbleView")
        floatingBubbleViewField.isAccessible = true
        val floatingBubbleView = floatingBubbleViewField.get(service)
        
        assertNull(floatingBubbleView, "addView失败后floatingBubbleView应为null")
    }

    @Test
    fun `WindowManager removeView失败时应正确处理`() {
        // Given - 先显示悬浮球
        every { preferences.getBubblePosition(any(), any()) } returns Pair(100, 200)
        
        service::class.java.getDeclaredMethod("showFloatingBubble", FloatingBubbleState::class.java)
            .apply { isAccessible = true }
            .invoke(service, FloatingBubbleState.IDLE)
        
        // Mock removeView抛出异常
        every { windowManager.removeView(any()) } throws RuntimeException("View not attached")
        
        // When - 尝试隐藏悬浮球
        service::class.java.getDeclaredMethod("hideFloatingBubble")
            .apply { isAccessible = true }
            .invoke(service)
        
        // Then - 验证仍然尝试清理引用
        val floatingBubbleViewField = service::class.java.getDeclaredField("floatingBubbleView")
        floatingBubbleViewField.isAccessible = true
        val floatingBubbleView = floatingBubbleViewField.get(service)
        
        assertNull(floatingBubbleView, "removeView失败后floatingBubbleView仍应为null")
    }

    @Test
    fun `最小化到悬浮球应正确保存状态`() {
        // Given - Mock preferences
        every { preferences.saveDisplayMode(any()) } returns Unit
        every { preferences.saveBubblePosition(any(), any()) } returns Unit
        every { preferences.saveBubbleState(any()) } returns Unit
        
        // When - 最小化到悬浮球
        service::class.java.getDeclaredMethod("minimizeToFloatingBubble")
            .apply { isAccessible = true }
            .invoke(service)
        
        // Then - 验证状态保存
        verify { preferences.saveDisplayMode("BUBBLE") }
        verify { preferences.saveBubbleState(FloatingBubbleState.IDLE) }
    }

    @Test
    fun `AI请求状态应正确更新悬浮球状态`() {
        // Given - 先显示悬浮球
        every { preferences.getBubblePosition(any(), any()) } returns Pair(100, 200)
        
        service::class.java.getDeclaredMethod("showFloatingBubble", FloatingBubbleState::class.java)
            .apply { isAccessible = true }
            .invoke(service, FloatingBubbleState.IDLE)
        
        // When - AI请求开始
        service::class.java.getDeclaredMethod("onAiRequestStarted", ActionType::class.java)
            .apply { isAccessible = true }
            .invoke(service, ActionType.ANALYZE)
        
        // Then - 验证悬浮球状态更新为LOADING
        // 这需要检查FloatingBubbleView的状态
        val floatingBubbleViewField = service::class.java.getDeclaredField("floatingBubbleView")
        floatingBubbleViewField.isAccessible = true
        val floatingBubbleView = floatingBubbleViewField.get(service) as? FloatingBubbleView
        
        assertNotNull(floatingBubbleView, "悬浮球应该存在")
        assertEquals(FloatingBubbleState.LOADING, floatingBubbleView?.getState())
    }
}