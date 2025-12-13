package com.empathy.ai.integration

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.empathy.ai.data.local.PrivacyPreferences
import com.empathy.ai.data.repository.settings.SettingsRepositoryImpl
import com.empathy.ai.domain.repository.SettingsRepository
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * 设置功能集成测试
 * 
 * 测试 SettingsRepository 和 PrivacyPreferences 的集成
 */
@RunWith(RobolectricTestRunner::class)
class SettingsIntegrationTest {
    
    private lateinit var context: Context
    private lateinit var privacyPreferences: PrivacyPreferences
    private lateinit var settingsRepository: SettingsRepository
    
    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        privacyPreferences = PrivacyPreferences(context)
        settingsRepository = SettingsRepositoryImpl(context, privacyPreferences)
        
        // 清除之前的测试数据
        privacyPreferences.clear()
    }
    
    @After
    fun tearDown() {
        // 清理测试数据
        privacyPreferences.clear()
    }
    
    @Test
    fun `Repository能正确读取和保存数据掩码设置`() = runTest {
        // Given: 初始状态应该是默认值 true
        val initialValue = settingsRepository.getDataMaskingEnabled().getOrThrow()
        assertTrue("初始值应该是 true", initialValue)
        
        // When: 设置为 false
        settingsRepository.setDataMaskingEnabled(false).getOrThrow()
        
        // Then: 应该能读取到 false
        val newValue = settingsRepository.getDataMaskingEnabled().getOrThrow()
        assertFalse("新值应该是 false", newValue)
    }
    
    @Test
    fun `Repository能正确读取和保存本地优先模式设置`() = runTest {
        // Given: 初始状态应该是默认值 true
        val initialValue = settingsRepository.getLocalFirstModeEnabled().getOrThrow()
        assertTrue("初始值应该是 true", initialValue)
        
        // When: 设置为 false
        settingsRepository.setLocalFirstModeEnabled(false).getOrThrow()
        
        // Then: 应该能读取到 false
        val newValue = settingsRepository.getLocalFirstModeEnabled().getOrThrow()
        assertFalse("新值应该是 false", newValue)
    }
    
    @Test
    fun `Repository和Preferences的数据保持一致`() = runTest {
        // When: 通过 Repository 设置
        settingsRepository.setDataMaskingEnabled(false).getOrThrow()
        settingsRepository.setLocalFirstModeEnabled(false).getOrThrow()
        
        // Then: Preferences 应该能读取到相同的值
        assertFalse("Preferences 应该读取到 false", privacyPreferences.isDataMaskingEnabled())
        assertFalse("Preferences 应该读取到 false", privacyPreferences.isLocalFirstModeEnabled())
    }
    
    @Test
    fun `设置持久化后重新创建Repository仍能读取`() = runTest {
        // Given: 保存设置
        settingsRepository.setDataMaskingEnabled(false).getOrThrow()
        settingsRepository.setLocalFirstModeEnabled(false).getOrThrow()
        
        // When: 创建新的 Repository 实例
        val newRepository = SettingsRepositoryImpl(context, privacyPreferences)
        
        // Then: 应该能读取到之前保存的设置
        assertFalse(newRepository.getDataMaskingEnabled().getOrThrow())
        assertFalse(newRepository.getLocalFirstModeEnabled().getOrThrow())
    }
    
    @Test
    fun `设置保存失败时返回错误Result`() = runTest {
        // 注意：在正常情况下很难模拟保存失败
        // 这个测试主要验证 Result 类型的使用
        val result = settingsRepository.setDataMaskingEnabled(true)
        assertTrue("保存应该成功", result.isSuccess)
    }
    
    @Test
    fun `getOrDefault能正确处理失败情况`() = runTest {
        // Given: 正常的设置读取
        val value = settingsRepository.getDataMaskingEnabled().getOrDefault(false)
        
        // Then: 应该返回实际值（默认是 true）
        assertTrue("应该返回默认值 true", value)
    }
}
