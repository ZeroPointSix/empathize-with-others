package com.empathy.ai.data.local

import android.content.Context
import android.content.SharedPreferences
import com.squareup.moshi.Moshi
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * BUG-00017 测试：悬浮窗首次启动应该以悬浮球模式显示
 *
 * 测试场景：
 * 1. 首次启动时（无保存的显示模式）应该返回BUBBLE
 * 2. shouldStartAsBubble()首次应该返回true
 * 3. 用户展开对话框后保存DIALOG模式
 */
class FloatingWindowPreferencesDefaultModeTest {

    private lateinit var context: Context
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var moshi: Moshi
    private lateinit var preferences: FloatingWindowPreferences

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        sharedPreferences = mockk(relaxed = true)
        editor = mockk(relaxed = true)
        moshi = Moshi.Builder().build()

        every { context.getSharedPreferences(any(), any()) } returns sharedPreferences
        every { sharedPreferences.edit() } returns editor
        every { editor.putString(any(), any()) } returns editor
        every { editor.apply() } returns Unit

        preferences = FloatingWindowPreferences(context, moshi)
    }

    @Test
    fun `首次启动时getDisplayMode应该返回BUBBLE`() {
        // Given: 没有保存的显示模式（模拟首次安装）
        every { sharedPreferences.getString("display_mode", any()) } returns null

        // When: 获取显示模式
        val mode = preferences.getDisplayMode()

        // Then: 应该返回BUBBLE
        assertEquals(FloatingWindowPreferences.DISPLAY_MODE_BUBBLE, mode)
    }

    @Test
    fun `首次启动时shouldStartAsBubble应该返回true`() {
        // Given: 没有保存的显示模式
        every { sharedPreferences.getString("display_mode", any()) } returns null

        // When: 检查是否应该以悬浮球模式启动
        val shouldStartAsBubble = preferences.shouldStartAsBubble()

        // Then: 应该返回true
        assertTrue(shouldStartAsBubble)
    }

    @Test
    fun `保存DIALOG模式后getDisplayMode应该返回DIALOG`() {
        // Given: 已保存DIALOG模式
        every { 
            sharedPreferences.getString("display_mode", any()) 
        } returns FloatingWindowPreferences.DISPLAY_MODE_DIALOG

        // When: 获取显示模式
        val mode = preferences.getDisplayMode()

        // Then: 应该返回DIALOG
        assertEquals(FloatingWindowPreferences.DISPLAY_MODE_DIALOG, mode)
    }

    @Test
    fun `saveDisplayMode应该正确保存模式`() {
        // When: 保存DIALOG模式
        preferences.saveDisplayMode(FloatingWindowPreferences.DISPLAY_MODE_DIALOG)

        // Then: 应该调用putString保存
        verify { editor.putString("display_mode", FloatingWindowPreferences.DISPLAY_MODE_DIALOG) }
        verify { editor.apply() }
    }

    @Test
    fun `DISPLAY_MODE_BUBBLE常量值应该正确`() {
        assertEquals("BUBBLE", FloatingWindowPreferences.DISPLAY_MODE_BUBBLE)
    }

    @Test
    fun `DISPLAY_MODE_DIALOG常量值应该正确`() {
        assertEquals("DIALOG", FloatingWindowPreferences.DISPLAY_MODE_DIALOG)
    }
}
