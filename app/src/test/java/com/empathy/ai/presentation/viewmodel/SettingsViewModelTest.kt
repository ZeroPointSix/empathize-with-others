package com.empathy.ai.presentation.viewmodel

import android.app.Application
import com.empathy.ai.domain.repository.AiProviderRepository
import com.empathy.ai.domain.repository.SettingsRepository
import com.empathy.ai.presentation.ui.screen.settings.SettingsUiEvent
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

/**
 * SettingsViewModel 单元测试
 *
 * 测试范围：
 * - AI 服务商选择
 * - 隐私设置切换
 * - 悬浮窗设置
 * 
 * 注意：API Key 配置已移至服务商配置中统一管理（AiConfigScreen）
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    // Mock 依赖
    private lateinit var mockApplication: Application
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var floatingWindowPreferences: com.empathy.ai.data.local.FloatingWindowPreferences
    private lateinit var aiProviderRepository: AiProviderRepository

    // 测试对象
    private lateinit var viewModel: SettingsViewModel

    // 测试调度器
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        // 创建 Mock 对象
        mockApplication = mockk(relaxed = true)
        settingsRepository = mockk()
        floatingWindowPreferences = mockk(relaxed = true)
        aiProviderRepository = mockk(relaxed = true)

        // 默认行为
        coEvery { settingsRepository.getAiProvider() } returns Result.success("OpenAI")
        coEvery { floatingWindowPreferences.isEnabled() } returns false
        coEvery { floatingWindowPreferences.getButtonX() } returns 0
        coEvery { floatingWindowPreferences.getButtonY() } returns 0
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * 测试：选择 AI 服务商
     */
    @Test
    fun `selectProvider should save provider and show success message`() = runTest(testDispatcher) {
        // Given
        val newProvider = "DeepSeek"
        coEvery { settingsRepository.saveAiProvider(any()) } returns Result.success(Unit)

        viewModel = SettingsViewModel(mockApplication, settingsRepository, floatingWindowPreferences, aiProviderRepository)
        advanceUntilIdle()

        // When: 选择新服务商
        viewModel.onEvent(SettingsUiEvent.SelectProvider(newProvider))
        advanceUntilIdle()

        // Then: 验证保存被调用
        coVerify { settingsRepository.saveAiProvider(newProvider) }

        // 验证状态更新
        assertEquals(newProvider, viewModel.uiState.value.selectedProvider)
        assertEquals("AI 服务商已切换为 $newProvider", viewModel.uiState.value.successMessage)
        assertFalse(viewModel.uiState.value.showProviderDialog)
    }

    /**
     * 测试：加载设置
     */
    @Test
    fun `init should load settings from repository`() = runTest(testDispatcher) {
        // Given
        coEvery { settingsRepository.getAiProvider() } returns Result.success("DeepSeek")

        // When: 创建 ViewModel（自动加载）
        viewModel = SettingsViewModel(mockApplication, settingsRepository, floatingWindowPreferences, aiProviderRepository)
        advanceUntilIdle()

        // Then: 验证设置被加载
        assertEquals("DeepSeek", viewModel.uiState.value.selectedProvider)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    /**
     * 测试：显示和隐藏服务商对话框
     */
    @Test
    fun `show and hide provider dialog`() = runTest(testDispatcher) {
        // Given
        viewModel = SettingsViewModel(mockApplication, settingsRepository, floatingWindowPreferences, aiProviderRepository)
        advanceUntilIdle()

        // When: 显示对话框
        viewModel.onEvent(SettingsUiEvent.ShowProviderDialog)
        advanceUntilIdle()

        // Then: 对话框应该显示
        assertTrue(viewModel.uiState.value.showProviderDialog)

        // When: 隐藏对话框
        viewModel.onEvent(SettingsUiEvent.HideProviderDialog)
        advanceUntilIdle()

        // Then: 对话框应该隐藏
        assertFalse(viewModel.uiState.value.showProviderDialog)
    }

    /**
     * 测试：切换数据掩码
     */
    @Test
    fun `toggleDataMasking should toggle state`() = runTest(testDispatcher) {
        // Given
        viewModel = SettingsViewModel(mockApplication, settingsRepository, floatingWindowPreferences, aiProviderRepository)
        advanceUntilIdle()

        val initialState = viewModel.uiState.value.dataMaskingEnabled

        // When: 切换数据掩码
        viewModel.onEvent(SettingsUiEvent.ToggleDataMasking)
        advanceUntilIdle()

        // Then: 状态应该反转
        assertEquals(!initialState, viewModel.uiState.value.dataMaskingEnabled)
    }

    /**
     * 测试：切换本地优先模式
     */
    @Test
    fun `toggleLocalFirstMode should toggle state`() = runTest(testDispatcher) {
        // Given
        viewModel = SettingsViewModel(mockApplication, settingsRepository, floatingWindowPreferences, aiProviderRepository)
        advanceUntilIdle()

        val initialState = viewModel.uiState.value.localFirstMode

        // When: 切换本地优先模式
        viewModel.onEvent(SettingsUiEvent.ToggleLocalFirstMode)
        advanceUntilIdle()

        // Then: 状态应该反转
        assertEquals(!initialState, viewModel.uiState.value.localFirstMode)
    }

    /**
     * 测试：清除错误
     */
    @Test
    fun `clearError should clear error message`() = runTest(testDispatcher) {
        // Given
        viewModel = SettingsViewModel(mockApplication, settingsRepository, floatingWindowPreferences, aiProviderRepository)
        advanceUntilIdle()

        // When: 清除错误
        viewModel.onEvent(SettingsUiEvent.ClearError)
        advanceUntilIdle()

        // Then: 错误应该被清除
        assertEquals(null, viewModel.uiState.value.error)
    }
    
    /**
     * 测试：清除成功消息
     */
    @Test
    fun `clearSuccessMessage should clear success message`() = runTest(testDispatcher) {
        // Given
        coEvery { settingsRepository.saveAiProvider(any()) } returns Result.success(Unit)
        
        viewModel = SettingsViewModel(mockApplication, settingsRepository, floatingWindowPreferences, aiProviderRepository)
        advanceUntilIdle()
        
        // 先触发一个成功消息
        viewModel.onEvent(SettingsUiEvent.SelectProvider("DeepSeek"))
        advanceUntilIdle()
        
        // 验证有成功消息
        assertEquals("AI 服务商已切换为 DeepSeek", viewModel.uiState.value.successMessage)

        // When: 清除成功消息
        viewModel.onEvent(SettingsUiEvent.ClearSuccessMessage)
        advanceUntilIdle()

        // Then: 成功消息应该被清除
        assertEquals(null, viewModel.uiState.value.successMessage)
    }
}
