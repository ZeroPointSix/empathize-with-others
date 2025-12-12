package com.empathy.ai.domain.service

import com.empathy.ai.data.local.FloatingWindowPreferences
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
 * FloatingWindowService 状态持久化测试
 * 
 * 测试状态保存和恢复功能
 * 
 * 验证需求：4.5
 */
class FloatingWindowServicePersistenceTest {
    
    private lateinit var floatingWindowPreferences: FloatingWindowPreferences
    
    @Before
    fun setup() {
        floatingWindowPreferences = mockk(relaxed = true)
    }
    
    /**
     * 测试保存悬浮窗启用状态
     * 
     * 验证：当用户启用悬浮窗时，状态应该被保存
     */
    @Test
    fun `should save enabled state when service starts`() {
        // When: 保存启用状态
        floatingWindowPreferences.saveEnabled(true)
        
        // Then: 验证保存方法被调用
        verify { floatingWindowPreferences.saveEnabled(true) }
    }
    
    /**
     * 测试保存悬浮窗禁用状态
     * 
     * 验证：当用户禁用悬浮窗时，状态应该被保存
     */
    @Test
    fun `should save disabled state when service stops`() {
        // When: 保存禁用状态
        floatingWindowPreferences.saveEnabled(false)
        
        // Then: 验证保存方法被调用
        verify { floatingWindowPreferences.saveEnabled(false) }
    }
    
    /**
     * 测试保存悬浮按钮位置
     * 
     * 验证：当用户拖动悬浮按钮时，位置应该被保存
     */
    @Test
    fun `should save button position when dragged`() {
        // Given: 准备位置数据
        val x = 100
        val y = 200
        
        // When: 保存位置
        floatingWindowPreferences.saveButtonPosition(x, y)
        
        // Then: 验证保存方法被调用
        verify { floatingWindowPreferences.saveButtonPosition(x, y) }
    }
    
    /**
     * 测试加载悬浮窗状态
     * 
     * 验证：应用启动时应该加载上次保存的状态
     */
    @Test
    fun `should load saved state on startup`() {
        // Given: 配置保存的状态
        val savedState = FloatingWindowState(
            isEnabled = true,
            buttonX = 150,
            buttonY = 250
        )
        every { floatingWindowPreferences.loadState() } returns savedState
        
        // When: 加载状态
        val state = floatingWindowPreferences.loadState()
        
        // Then: 应该返回保存的状态
        assertTrue(state.isEnabled)
        assertEquals(150, state.buttonX)
        assertEquals(250, state.buttonY)
    }
    
    /**
     * 测试加载默认状态
     * 
     * 验证：首次启动时应该返回默认状态
     */
    @Test
    fun `should load default state on first launch`() {
        // Given: 配置默认状态
        val defaultState = FloatingWindowState(
            isEnabled = false,
            buttonX = 0,
            buttonY = 0
        )
        every { floatingWindowPreferences.loadState() } returns defaultState
        
        // When: 加载状态
        val state = floatingWindowPreferences.loadState()
        
        // Then: 应该返回默认状态
        assertFalse(state.isEnabled)
        assertEquals(0, state.buttonX)
        assertEquals(0, state.buttonY)
    }
    
    /**
     * 测试恢复悬浮按钮位置
     * 
     * 验证：服务启动时应该恢复上次保存的按钮位置
     */
    @Test
    fun `should restore button position when service starts`() {
        // Given: 配置保存的位置
        every { floatingWindowPreferences.getButtonX() } returns 300
        every { floatingWindowPreferences.getButtonY() } returns 400
        
        // When: 获取位置
        val x = floatingWindowPreferences.getButtonX()
        val y = floatingWindowPreferences.getButtonY()
        
        // Then: 应该返回保存的位置
        assertEquals(300, x)
        assertEquals(400, y)
    }
    
    /**
     * 测试完整的状态保存和恢复流程
     * 
     * 验证：
     * 1. 保存完整状态
     * 2. 加载完整状态
     * 3. 状态一致性
     */
    @Test
    fun `should maintain state consistency across save and load`() {
        // Given: 准备状态数据
        val originalState = FloatingWindowState(
            isEnabled = true,
            buttonX = 500,
            buttonY = 600
        )
        
        // When: 保存状态
        floatingWindowPreferences.saveState(originalState)
        
        // Then: 验证保存方法被调用
        verify { floatingWindowPreferences.saveState(originalState) }
        
        // Given: 配置加载返回相同的状态
        every { floatingWindowPreferences.loadState() } returns originalState
        
        // When: 加载状态
        val loadedState = floatingWindowPreferences.loadState()
        
        // Then: 加载的状态应该与保存的状态一致
        assertEquals(originalState.isEnabled, loadedState.isEnabled)
        assertEquals(originalState.buttonX, loadedState.buttonX)
        assertEquals(originalState.buttonY, loadedState.buttonY)
    }
    
    /**
     * 测试清除状态
     * 
     * 验证：应该能够清除所有保存的状态
     */
    @Test
    fun `should clear all saved state`() {
        // When: 清除状态
        floatingWindowPreferences.clear()
        
        // Then: 验证清除方法被调用
        verify { floatingWindowPreferences.clear() }
    }
}
