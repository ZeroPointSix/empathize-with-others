package com.empathy.ai.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * PrivacyPreferences 单元测试
 * 
 * 测试隐私设置的持久化功能
 */
@RunWith(RobolectricTestRunner::class)
class PrivacyPreferencesTest {
    
    private lateinit var context: Context
    private lateinit var privacyPreferences: PrivacyPreferences
    
    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        privacyPreferences = PrivacyPreferences(context)
        // 清除之前的测试数据
        privacyPreferences.clear()
    }
    
    @After
    fun tearDown() {
        // 清理测试数据
        privacyPreferences.clear()
    }
    
    @Test
    fun `数据掩码默认开启`() {
        // When: 首次读取
        val enabled = privacyPreferences.isDataMaskingEnabled()
        
        // Then: 应该返回默认值 true
        assertTrue(enabled)
    }
    
    @Test
    fun `能正确保存和读取数据掩码设置`() {
        // Given: 设置为关闭
        privacyPreferences.setDataMaskingEnabled(false)
        
        // When: 读取设置
        val enabled = privacyPreferences.isDataMaskingEnabled()
        
        // Then: 应该返回 false
        assertFalse(enabled)
    }
    
    @Test
    fun `本地优先模式默认开启`() {
        // When: 首次读取
        val enabled = privacyPreferences.isLocalFirstModeEnabled()
        
        // Then: 应该返回默认值 true
        assertTrue(enabled)
    }
    
    @Test
    fun `能正确保存和读取本地优先模式设置`() {
        // Given: 设置为关闭
        privacyPreferences.setLocalFirstModeEnabled(false)
        
        // When: 读取设置
        val enabled = privacyPreferences.isLocalFirstModeEnabled()
        
        // Then: 应该返回 false
        assertFalse(enabled)
    }
    
    @Test
    fun `重置为默认值能正确工作`() {
        // Given: 修改所有设置
        privacyPreferences.setDataMaskingEnabled(false)
        privacyPreferences.setLocalFirstModeEnabled(false)
        
        // When: 重置为默认值
        privacyPreferences.resetToDefaults()
        
        // Then: 所有设置应该恢复为默认值
        assertTrue(privacyPreferences.isDataMaskingEnabled())
        assertTrue(privacyPreferences.isLocalFirstModeEnabled())
    }
    
    @Test
    fun `清除所有设置后应该返回默认值`() {
        // Given: 修改所有设置
        privacyPreferences.setDataMaskingEnabled(false)
        privacyPreferences.setLocalFirstModeEnabled(false)
        
        // When: 清除所有设置
        privacyPreferences.clear()
        
        // Then: 应该返回默认值
        assertTrue(privacyPreferences.isDataMaskingEnabled())
        assertTrue(privacyPreferences.isLocalFirstModeEnabled())
    }
    
    @Test
    fun `设置持久化后重新创建实例仍能读取`() {
        // Given: 保存设置
        privacyPreferences.setDataMaskingEnabled(false)
        privacyPreferences.setLocalFirstModeEnabled(false)
        
        // When: 创建新实例
        val newPreferences = PrivacyPreferences(context)
        
        // Then: 应该能读取到之前保存的设置
        assertFalse(newPreferences.isDataMaskingEnabled())
        assertFalse(newPreferences.isLocalFirstModeEnabled())
    }
}
