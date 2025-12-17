package com.empathy.ai.domain.usecase

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.empathy.ai.data.local.PrivacyPreferences
import com.empathy.ai.data.repository.settings.SettingsRepositoryImpl
import com.empathy.ai.domain.model.SafetyCheckResult
import com.empathy.ai.domain.repository.AiRepository
import com.empathy.ai.domain.repository.BrainTagRepository
import com.empathy.ai.domain.repository.PrivacyRepository
import com.empathy.ai.domain.repository.SettingsRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * CheckDraftUseCase 集成测试
 * 
 * 测试本地优先模式设置对业务逻辑的影响
 */
@RunWith(RobolectricTestRunner::class)
class CheckDraftUseCaseIntegrationTest {
    
    private lateinit var context: Context
    private lateinit var privacyPreferences: PrivacyPreferences
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var brainTagRepository: BrainTagRepository
    private lateinit var privacyRepository: PrivacyRepository
    private lateinit var aiRepository: AiRepository
    private lateinit var checkDraftUseCase: CheckDraftUseCase
    
    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        privacyPreferences = PrivacyPreferences(context)
        settingsRepository = SettingsRepositoryImpl(context, privacyPreferences)
        
        // Mock 其他依赖
        brainTagRepository = mockk()
        privacyRepository = mockk()
        aiRepository = mockk()
        
        // 创建 UseCase
        checkDraftUseCase = CheckDraftUseCase(
            brainTagRepository,
            privacyRepository,
            aiRepository,
            settingsRepository
        )
        
        // 清除之前的测试数据
        privacyPreferences.clear()
        
        // 设置默认的 Mock 行为
        coEvery { brainTagRepository.getTagsForContact(any()) } returns flowOf(emptyList())
        coEvery { privacyRepository.getPrivacyMapping() } returns Result.success(emptyMap())
    }
    
    @After
    fun tearDown() {
        privacyPreferences.clear()
    }
    
    @Test
    fun `本地优先模式开启时优先使用本地规则`() = runTest {
        // Given: 本地优先模式开启（默认）
        assertTrue("本地优先模式应该默认开启", 
            settingsRepository.getLocalFirstModeEnabled().getOrThrow())
        
        // 没有雷区标签，本地检查应该通过
        coEvery { brainTagRepository.getTagsForContact(any()) } returns flowOf(emptyList())
        
        // When: 执行检查
        val result = checkDraftUseCase.invoke(
            contactId = "test_contact",
            draftSnapshot = "这是一条安全的消息",
            enableDeepCheck = false
        )
        
        // Then: 应该返回安全结果，且不调用 AI
        assertTrue("应该返回成功", result.isSuccess)
        assertTrue("应该是安全的", result.getOrThrow().isSafe)
    }
    
    @Test
    fun `本地优先模式关闭时跳过本地规则直接使用AI`() = runTest {
        // Given: 关闭本地优先模式
        settingsRepository.setLocalFirstModeEnabled(false).getOrThrow()
        assertFalse("本地优先模式应该已关闭", 
            settingsRepository.getLocalFirstModeEnabled().getOrThrow())
        
        // Mock AI 返回安全结果
        coEvery { 
            aiRepository.checkDraftSafety(any(), any()) 
        } returns Result.success(SafetyCheckResult(isSafe = true))
        
        // When: 执行检查（即使有雷区标签也会跳过本地检查）
        val result = checkDraftUseCase.invoke(
            contactId = "test_contact",
            draftSnapshot = "测试消息",
            enableDeepCheck = true
        )
        
        // Then: 应该调用 AI 检查
        assertTrue("应该返回成功", result.isSuccess)
    }
    
    @Test
    fun `设置变更后立即生效`() = runTest {
        // Given: 初始状态是开启
        assertTrue(settingsRepository.getLocalFirstModeEnabled().getOrThrow())
        
        // When: 关闭本地优先模式
        settingsRepository.setLocalFirstModeEnabled(false).getOrThrow()
        
        // Then: 立即读取应该是关闭状态
        assertFalse(settingsRepository.getLocalFirstModeEnabled().getOrThrow())
        
        // When: 再次开启
        settingsRepository.setLocalFirstModeEnabled(true).getOrThrow()
        
        // Then: 应该是开启状态
        assertTrue(settingsRepository.getLocalFirstModeEnabled().getOrThrow())
    }
}
