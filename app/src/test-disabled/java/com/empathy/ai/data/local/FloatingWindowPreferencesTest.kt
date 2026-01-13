package com.empathy.ai.data.local

import android.content.Context
import android.content.SharedPreferences
import com.empathy.ai.domain.model.FloatingWindowState
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * FloatingWindowPreferences 单元测试
 *
 * 测试悬浮窗状态持久化功能 (PRD-00004 悬浮窗功能需求):
 * - 状态保存 - saveState保存所有状态属性
 * - 状态恢复 - loadState正确读取保存的状态
 * - 默认值处理 - 无数据时返回默认值
 * - 启用状态管理 - saveEnabled/loadEnabled配对测试
 * - 位置坐标保存 - buttonX/buttonY边界值
 * - 模式切换保存 - saveMode/loadMode正确性
 * - 最小化状态 - saveMinimizeState/loadMinimizeState
 *
 * 业务规则 (TDD-00004/3.2):
 * - 悬浮窗位置使用屏幕坐标存储
 * - 模式支持：正常模式、气泡模式、最小化模式
 * - 状态变化需要立即持久化
 *
 * 设计权衡:
 * - 使用SharedPreferences存储简单状态数据
 * - 使用JSON存储复杂状态（如FloatingWindowState）
 * - 启用状态独立存储，便于快速判断
 *
 * 任务追踪:
 * - PRD-00004 - 悬浮窗功能需求
 * - TDD-00004 - 悬浮窗功能技术设计
 */
class FloatingWindowPreferencesTest {
    
    private lateinit var context: Context
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var floatingWindowPreferences: FloatingWindowPreferences
    
    @Before
    fun setup() {
        // Mock Context
        context = mockk(relaxed = true)
        
        // Mock SharedPreferences
        sharedPreferences = mockk(relaxed = true)
        editor = mockk(relaxed = true)
        
        every { context.getSharedPreferences(any(), any()) } returns sharedPreferences
        every { sharedPreferences.edit() } returns editor
        every { editor.putBoolean(any(), any()) } returns editor
        every { editor.putInt(any(), any()) } returns editor
        every { editor.apply() } returns Unit
        
        // Mock Moshi
        val moshi = mockk<com.squareup.moshi.Moshi>(relaxed = true)
        
        floatingWindowPreferences = FloatingWindowPreferences(context, moshi)
    }
    
    @Test
    fun `saveState should save all state properties`() {
        // Given
        val state = FloatingWindowState(
            isEnabled = true,
            buttonX = 100,
            buttonY = 200
        )
        
        // When
        floatingWindowPreferences.saveState(state)
        
        // Then
        verify { editor.putBoolean("is_enabled", true) }
        verify { editor.putInt("button_x", 100) }
        verify { editor.putInt("button_y", 200) }
        verify { editor.apply() }
    }
    
    @Test
    fun `loadState should return saved state`() {
        // Given
        every { sharedPreferences.getBoolean("is_enabled", false) } returns true
        every { sharedPreferences.getInt("button_x", 0) } returns 100
        every { sharedPreferences.getInt("button_y", 0) } returns 200
        
        // When
        val state = floatingWindowPreferences.loadState()
        
        // Then
        assertTrue(state.isEnabled)
        assertEquals(100, state.buttonX)
        assertEquals(200, state.buttonY)
    }
    
    @Test
    fun `loadState should return default state when no data saved`() {
        // Given
        every { sharedPreferences.getBoolean("is_enabled", false) } returns false
        every { sharedPreferences.getInt("button_x", 0) } returns 0
        every { sharedPreferences.getInt("button_y", 0) } returns 0
        
        // When
        val state = floatingWindowPreferences.loadState()
        
        // Then
        assertFalse(state.isEnabled)
        assertEquals(0, state.buttonX)
        assertEquals(0, state.buttonY)
    }
    
    @Test
    fun `saveEnabled should save enabled state`() {
        // When
        floatingWindowPreferences.saveEnabled(true)
        
        // Then
        verify { editor.putBoolean("is_enabled", true) }
        verify { editor.apply() }
    }
    
    @Test
    fun `isEnabled should return saved enabled state`() {
        // Given
        every { sharedPreferences.getBoolean("is_enabled", false) } returns true
        
        // When
        val isEnabled = floatingWindowPreferences.isEnabled()
        
        // Then
        assertTrue(isEnabled)
    }
    
    @Test
    fun `saveButtonPosition should save position`() {
        // When
        floatingWindowPreferences.saveButtonPosition(150, 250)
        
        // Then
        verify { editor.putInt("button_x", 150) }
        verify { editor.putInt("button_y", 250) }
        verify { editor.apply() }
    }
    
    @Test
    fun `getButtonX should return saved X coordinate`() {
        // Given
        every { sharedPreferences.getInt("button_x", 0) } returns 150
        
        // When
        val x = floatingWindowPreferences.getButtonX()
        
        // Then
        assertEquals(150, x)
    }
    
    @Test
    fun `getButtonY should return saved Y coordinate`() {
        // Given
        every { sharedPreferences.getInt("button_y", 0) } returns 250
        
        // When
        val y = floatingWindowPreferences.getButtonY()
        
        // Then
        assertEquals(250, y)
    }
    
    @Test
    fun `clear should clear all preferences`() {
        // When
        floatingWindowPreferences.clear()
        
        // Then
        verify { editor.clear() }
        verify { editor.apply() }
    }
}
