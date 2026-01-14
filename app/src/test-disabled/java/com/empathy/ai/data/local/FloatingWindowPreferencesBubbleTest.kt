package com.empathy.ai.data.local

import android.content.Context
import android.content.SharedPreferences
import com.empathy.ai.domain.model.FloatingBubbleState
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * FloatingWindowPreferences 悬浮球相关功能单元测试
 *
 * @see TDD-00010 悬浮球状态指示与拖动技术设计
 */
class FloatingWindowPreferencesBubbleTest {

    private lateinit var context: Context
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var preferences: FloatingWindowPreferences

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        sharedPreferences = mockk(relaxed = true)
        editor = mockk(relaxed = true)

        every { context.getSharedPreferences(any(), any()) } returns sharedPreferences
        every { sharedPreferences.edit() } returns editor
        every { editor.putInt(any(), any()) } returns editor
        every { editor.putString(any(), any()) } returns editor
        every { editor.putLong(any(), any()) } returns editor
        every { editor.remove(any()) } returns editor
        every { editor.apply() } returns Unit

        val moshi = com.squareup.moshi.Moshi.Builder().build()
        preferences = FloatingWindowPreferences(context, moshi)
    }

    // ==================== 悬浮球位置测试 ====================

    @Test
    fun `saveBubblePosition should save x and y coordinates`() {
        preferences.saveBubblePosition(100, 200)

        verify { editor.putInt("bubble_position_x", 100) }
        verify { editor.putInt("bubble_position_y", 200) }
        verify { editor.apply() }
    }

    @Test
    fun `getBubblePosition should return saved position`() {
        every { sharedPreferences.getInt("bubble_position_x", -1) } returns 100
        every { sharedPreferences.getInt("bubble_position_y", -1) } returns 200

        val (x, y) = preferences.getBubblePosition(0, 0)

        assertEquals(100, x)
        assertEquals(200, y)
    }

    @Test
    fun `getBubblePosition should return default when not saved`() {
        every { sharedPreferences.getInt("bubble_position_x", -1) } returns -1
        every { sharedPreferences.getInt("bubble_position_y", -1) } returns -1

        val (x, y) = preferences.getBubblePosition(500, 600)

        assertEquals(500, x)
        assertEquals(600, y)
    }

    @Test
    fun `getBubblePosition should return default when only x is invalid`() {
        every { sharedPreferences.getInt("bubble_position_x", -1) } returns -1
        every { sharedPreferences.getInt("bubble_position_y", -1) } returns 200

        val (x, y) = preferences.getBubblePosition(500, 600)

        assertEquals(500, x)
        assertEquals(600, y)
    }

    // ==================== 悬浮球状态测试 ====================

    @Test
    fun `saveBubbleState should save state name`() {
        preferences.saveBubbleState(FloatingBubbleState.LOADING)

        verify { editor.putString("bubble_state", "LOADING") }
        verify { editor.apply() }
    }

    @Test
    fun `getBubbleState should return saved state`() {
        every { sharedPreferences.getString("bubble_state", null) } returns "SUCCESS"

        val state = preferences.getBubbleState()

        assertEquals(FloatingBubbleState.SUCCESS, state)
    }

    @Test
    fun `getBubbleState should return IDLE when not saved`() {
        every { sharedPreferences.getString("bubble_state", null) } returns null

        val state = preferences.getBubbleState()

        assertEquals(FloatingBubbleState.IDLE, state)
    }

    @Test
    fun `getBubbleState should return IDLE for invalid state name`() {
        every { sharedPreferences.getString("bubble_state", null) } returns "INVALID_STATE"

        val state = preferences.getBubbleState()

        assertEquals(FloatingBubbleState.IDLE, state)
    }

    @Test
    fun `getBubbleState should return IDLE for empty string`() {
        every { sharedPreferences.getString("bubble_state", null) } returns ""

        val state = preferences.getBubbleState()

        assertEquals(FloatingBubbleState.IDLE, state)
    }

    // ==================== 最小化状态测试 ====================

    @Test
    fun `saveMinimizeState should save timestamp and request info`() {
        val requestInfo = """{"type":"ANALYZE","contactId":"123"}"""
        
        preferences.saveMinimizeState(requestInfo)

        verify { editor.putLong(eq("minimize_timestamp"), any()) }
        verify { editor.putString("minimize_request_info", requestInfo) }
        verify { editor.apply() }
    }

    @Test
    fun `getMinimizeStateIfValid should return request info when not expired`() {
        val currentTime = System.currentTimeMillis()
        val requestInfo = """{"type":"ANALYZE"}"""
        
        every { sharedPreferences.getLong("minimize_timestamp", 0) } returns currentTime - 5 * 60 * 1000 // 5分钟前
        every { sharedPreferences.getString("minimize_request_info", null) } returns requestInfo

        val result = preferences.getMinimizeStateIfValid()

        assertEquals(requestInfo, result)
    }

    @Test
    fun `getMinimizeStateIfValid should return null when expired`() {
        val currentTime = System.currentTimeMillis()
        
        every { sharedPreferences.getLong("minimize_timestamp", 0) } returns currentTime - 15 * 60 * 1000 // 15分钟前
        every { sharedPreferences.getString("minimize_request_info", null) } returns """{"type":"ANALYZE"}"""

        val result = preferences.getMinimizeStateIfValid()

        assertNull(result)
        // 验证清除了过期状态
        verify { editor.remove("minimize_timestamp") }
        verify { editor.remove("minimize_request_info") }
    }

    @Test
    fun `clearMinimizeState should remove timestamp and request info`() {
        preferences.clearMinimizeState()

        verify { editor.remove("minimize_timestamp") }
        verify { editor.remove("minimize_request_info") }
        verify { editor.apply() }
    }

    @Test
    fun `hasValidMinimizeState should return true when state is valid`() {
        val currentTime = System.currentTimeMillis()
        
        every { sharedPreferences.getLong("minimize_timestamp", 0) } returns currentTime - 5 * 60 * 1000
        every { sharedPreferences.getString("minimize_request_info", null) } returns """{"type":"ANALYZE"}"""

        val result = preferences.hasValidMinimizeState()

        assertTrue(result)
    }

    @Test
    fun `hasValidMinimizeState should return false when expired`() {
        val currentTime = System.currentTimeMillis()
        
        every { sharedPreferences.getLong("minimize_timestamp", 0) } returns currentTime - 15 * 60 * 1000
        every { sharedPreferences.getString("minimize_request_info", null) } returns """{"type":"ANALYZE"}"""

        val result = preferences.hasValidMinimizeState()

        assertFalse(result)
    }

    @Test
    fun `hasValidMinimizeState should return false when no request info`() {
        val currentTime = System.currentTimeMillis()
        
        every { sharedPreferences.getLong("minimize_timestamp", 0) } returns currentTime - 5 * 60 * 1000
        every { sharedPreferences.getString("minimize_request_info", null) } returns null

        val result = preferences.hasValidMinimizeState()

        assertFalse(result)
    }

    // ==================== 边界情况测试 ====================

    @Test
    fun `getBubblePosition should handle zero coordinates as valid`() {
        every { sharedPreferences.getInt("bubble_position_x", -1) } returns 0
        every { sharedPreferences.getInt("bubble_position_y", -1) } returns 0

        val (x, y) = preferences.getBubblePosition(500, 600)

        assertEquals(0, x)
        assertEquals(0, y)
    }

    @Test
    fun `getMinimizeStateIfValid should return null when timestamp is zero`() {
        every { sharedPreferences.getLong("minimize_timestamp", 0) } returns 0
        every { sharedPreferences.getString("minimize_request_info", null) } returns """{"type":"ANALYZE"}"""

        val result = preferences.getMinimizeStateIfValid()

        assertNull(result)
    }
}
