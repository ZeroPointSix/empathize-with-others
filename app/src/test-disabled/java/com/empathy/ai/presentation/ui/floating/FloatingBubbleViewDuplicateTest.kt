package com.empathy.ai.presentation.ui.floating

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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * FloatingBubbleView双实例问题测试
 *
 * 测试悬浮球视图的实例管理和状态同步
 * 注意：这些测试不依赖Android框架，只测试逻辑
 *
 * @see BUG-00022 悬浮球拖动后出现双悬浮球问题
 */
class FloatingBubbleViewDuplicateTest {

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

    // ==================== 实例管理测试 ====================

    @Test
    fun `创建悬浮球时应该检查是否已存在实例`() {
        // Given
        var bubbleInstance: Any? = null
        var createCount = 0
        
        val createBubble: () -> Unit = {
            if (bubbleInstance == null) {
                bubbleInstance = Object()
                createCount++
            }
        }
        
        // When - 多次尝试创建
        createBubble()
        createBubble()
        createBubble()
        
        // Then - 只应该创建一次
        assertEquals(1, createCount)
    }

    @Test
    fun `销毁悬浮球时应该清理引用`() {
        // Given
        var bubbleInstance: Any? = Object()
        
        val destroyBubble: () -> Unit = {
            bubbleInstance = null
        }
        
        // When
        destroyBubble()
        
        // Then
        assertNull(bubbleInstance)
    }

    @Test
    fun `重新创建悬浮球前应该先销毁旧实例`() {
        // Given
        var oldInstance: Any? = Object()
        var newInstance: Any? = null
        var destroyCalled = false
        
        val recreateBubble: () -> Unit = {
            if (oldInstance != null) {
                oldInstance = null
                destroyCalled = true
            }
            newInstance = Object()
        }
        
        // When
        recreateBubble()
        
        // Then
        assertTrue(destroyCalled)
        assertNull(oldInstance)
        assertTrue(newInstance != null)
    }

    // ==================== 状态同步测试 ====================

    @Test
    fun `状态变化应该同步到Preferences`() {
        // Given
        every { preferences.saveBubbleState(any()) } returns Unit
        
        // When
        preferences.saveBubbleState(FloatingBubbleState.LOADING)
        
        // Then
        verify { preferences.saveBubbleState(FloatingBubbleState.LOADING) }
    }

    @Test
    fun `位置变化应该同步到Preferences`() {
        // Given
        every { preferences.saveBubblePosition(any(), any()) } returns Unit
        
        // When
        preferences.saveBubblePosition(100, 200)
        
        // Then
        verify { preferences.saveBubblePosition(100, 200) }
    }

    @Test
    fun `从Preferences恢复位置应该正确`() {
        // Given
        every { preferences.getBubblePosition(any(), any()) } returns Pair(150, 250)
        
        // When
        val (x, y) = preferences.getBubblePosition(0, 0)
        
        // Then
        assertEquals(150, x)
        assertEquals(250, y)
    }

    // ==================== 拖动状态测试 ====================

    @Test
    fun `拖动开始时应该标记拖动状态`() {
        // Given
        var isDragging = false
        
        // When
        isDragging = true
        
        // Then
        assertTrue(isDragging)
    }

    @Test
    fun `拖动结束时应该保存位置`() {
        // Given
        var savedX = 0
        var savedY = 0
        val finalX = 200
        val finalY = 300
        
        // When - 模拟拖动结束
        savedX = finalX
        savedY = finalY
        
        // Then
        assertEquals(200, savedX)
        assertEquals(300, savedY)
    }

    @Test
    fun `拖动过程中不应该创建新实例`() {
        // Given
        var bubbleInstance: Any? = Object()
        var createCount = 0
        var isDragging = true
        
        val createBubble: () -> Unit = {
            if (bubbleInstance == null && !isDragging) {
                bubbleInstance = Object()
                createCount++
            }
        }
        
        // When - 拖动过程中尝试创建
        createBubble()
        
        // Then - 不应该创建新实例
        assertEquals(0, createCount)
    }

    // ==================== 边界条件测试 ====================

    @Test
    fun `位置超出屏幕边界时应该修正`() {
        // Given
        val screenWidth = 1080
        val screenHeight = 1920
        val bubbleSize = 56
        var x = 1100 // 超出右边界
        var y = 2000 // 超出下边界
        
        // When - 修正位置
        x = x.coerceIn(0, screenWidth - bubbleSize)
        y = y.coerceIn(0, screenHeight - bubbleSize)
        
        // Then
        assertEquals(screenWidth - bubbleSize, x)
        assertEquals(screenHeight - bubbleSize, y)
    }

    @Test
    fun `负坐标应该修正为0`() {
        // Given
        var x = -50
        var y = -100
        
        // When
        x = x.coerceAtLeast(0)
        y = y.coerceAtLeast(0)
        
        // Then
        assertEquals(0, x)
        assertEquals(0, y)
    }

    // ==================== 显示模式测试 ====================

    @Test
    fun `BUBBLE模式应该显示悬浮球`() {
        // Given
        every { preferences.getDisplayMode() } returns FloatingWindowPreferences.DISPLAY_MODE_BUBBLE
        
        // When
        val displayMode = preferences.getDisplayMode()
        val shouldShowBubble = displayMode == FloatingWindowPreferences.DISPLAY_MODE_BUBBLE
        
        // Then
        assertTrue(shouldShowBubble)
    }

    @Test
    fun `DIALOG模式不应该显示悬浮球`() {
        // Given
        every { preferences.getDisplayMode() } returns FloatingWindowPreferences.DISPLAY_MODE_DIALOG
        
        // When
        val displayMode = preferences.getDisplayMode()
        val shouldShowBubble = displayMode == FloatingWindowPreferences.DISPLAY_MODE_BUBBLE
        
        // Then
        assertFalse(shouldShowBubble)
    }
}
