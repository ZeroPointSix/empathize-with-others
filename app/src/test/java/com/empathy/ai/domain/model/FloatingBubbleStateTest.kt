package com.empathy.ai.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * FloatingBubbleState 单元测试
 *
 * @see TDD-00010 悬浮球状态指示与拖动技术设计
 */
class FloatingBubbleStateTest {

    @Test
    fun `test all states exist`() {
        // 验证四种状态都存在
        val states = FloatingBubbleState.entries
        assertEquals(4, states.size)
        
        assertNotNull(FloatingBubbleState.IDLE)
        assertNotNull(FloatingBubbleState.LOADING)
        assertNotNull(FloatingBubbleState.SUCCESS)
        assertNotNull(FloatingBubbleState.ERROR)
    }

    @Test
    fun `test default state is IDLE`() {
        assertEquals(FloatingBubbleState.IDLE, FloatingBubbleState.default())
    }

    @Test
    fun `test state names are correct`() {
        assertEquals("IDLE", FloatingBubbleState.IDLE.name)
        assertEquals("LOADING", FloatingBubbleState.LOADING.name)
        assertEquals("SUCCESS", FloatingBubbleState.SUCCESS.name)
        assertEquals("ERROR", FloatingBubbleState.ERROR.name)
    }

    @Test
    fun `test valueOf returns correct state`() {
        assertEquals(FloatingBubbleState.IDLE, FloatingBubbleState.valueOf("IDLE"))
        assertEquals(FloatingBubbleState.LOADING, FloatingBubbleState.valueOf("LOADING"))
        assertEquals(FloatingBubbleState.SUCCESS, FloatingBubbleState.valueOf("SUCCESS"))
        assertEquals(FloatingBubbleState.ERROR, FloatingBubbleState.valueOf("ERROR"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `test valueOf throws exception for invalid name`() {
        FloatingBubbleState.valueOf("INVALID")
    }

    @Test
    fun `test ordinal values`() {
        assertEquals(0, FloatingBubbleState.IDLE.ordinal)
        assertEquals(1, FloatingBubbleState.LOADING.ordinal)
        assertEquals(2, FloatingBubbleState.SUCCESS.ordinal)
        assertEquals(3, FloatingBubbleState.ERROR.ordinal)
    }
}
