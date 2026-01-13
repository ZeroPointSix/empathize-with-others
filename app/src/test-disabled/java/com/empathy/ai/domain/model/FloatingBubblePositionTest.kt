package com.empathy.ai.domain.model

import com.empathy.ai.domain.model.FloatingBubblePosition.Companion.isValid
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * FloatingBubblePosition 单元测试
 *
 * @see TDD-00010 悬浮球状态指示与拖动技术设计
 */
class FloatingBubblePositionTest {

    @Test
    fun `test create position with valid coordinates`() {
        val position = FloatingBubblePosition(100, 200)
        assertEquals(100, position.x)
        assertEquals(200, position.y)
    }

    @Test
    fun `test create position with zero coordinates`() {
        val position = FloatingBubblePosition(0, 0)
        assertEquals(0, position.x)
        assertEquals(0, position.y)
        assertTrue(position.isValid())
    }

    @Test
    fun `test default position calculation`() {
        val screenWidth = 1080
        val screenHeight = 1920
        val bubbleSize = 168 // 56dp * 3 density
        val margin = 48 // 16dp * 3 density
        
        val position = FloatingBubblePosition.default(
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            bubbleSizePx = bubbleSize,
            marginPx = margin
        )
        
        // 默认位置应该在屏幕右侧
        assertEquals(screenWidth - bubbleSize - margin, position.x)
        // 默认位置应该在屏幕中间
        assertEquals((screenHeight - bubbleSize) / 2, position.y)
    }

    @Test
    fun `test default position without margin`() {
        val screenWidth = 1080
        val screenHeight = 1920
        val bubbleSize = 168
        
        val position = FloatingBubblePosition.default(
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            bubbleSizePx = bubbleSize
        )
        
        assertEquals(screenWidth - bubbleSize, position.x)
        assertEquals((screenHeight - bubbleSize) / 2, position.y)
    }

    @Test
    fun `test invalid position`() {
        val position = FloatingBubblePosition.invalid()
        assertEquals(-1, position.x)
        assertEquals(-1, position.y)
        assertFalse(position.isValid())
    }

    @Test
    fun `test isValid with positive coordinates`() {
        val position = FloatingBubblePosition(100, 200)
        assertTrue(position.isValid())
    }

    @Test
    fun `test isValid with negative x coordinate`() {
        val position = FloatingBubblePosition(-1, 200)
        assertFalse(position.isValid())
    }

    @Test
    fun `test isValid with negative y coordinate`() {
        val position = FloatingBubblePosition(100, -1)
        assertFalse(position.isValid())
    }

    @Test
    fun `test isValid with both negative coordinates`() {
        val position = FloatingBubblePosition(-1, -1)
        assertFalse(position.isValid())
    }

    @Test
    fun `test data class equality`() {
        val position1 = FloatingBubblePosition(100, 200)
        val position2 = FloatingBubblePosition(100, 200)
        val position3 = FloatingBubblePosition(100, 300)
        
        assertEquals(position1, position2)
        assertFalse(position1 == position3)
    }

    @Test
    fun `test data class copy`() {
        val original = FloatingBubblePosition(100, 200)
        val copied = original.copy(y = 300)
        
        assertEquals(100, copied.x)
        assertEquals(300, copied.y)
    }

    @Test
    fun `test constants`() {
        assertEquals(56, FloatingBubblePosition.DEFAULT_SIZE_DP)
        assertEquals(16, FloatingBubblePosition.DEFAULT_MARGIN_DP)
    }
}
