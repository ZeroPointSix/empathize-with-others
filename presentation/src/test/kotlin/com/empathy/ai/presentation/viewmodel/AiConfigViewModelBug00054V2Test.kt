package com.empathy.ai.presentation.viewmodel

import com.empathy.ai.domain.model.AiModel
import com.empathy.ai.domain.model.AiProvider
import com.empathy.ai.domain.model.ProxyConfig
import com.empathy.ai.domain.repository.AiProviderRepository
import com.empathy.ai.domain.usecase.DeleteProviderUseCase
import com.empathy.ai.domain.usecase.GetProvidersUseCase
import com.empathy.ai.domain.usecase.SaveProviderUseCase
import com.empathy.ai.domain.usecase.TestConnectionUseCase
import com.empathy.ai.presentation.ui.screen.aiconfig.AiConfigUiEvent
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * BUG-00054 第二轮修复测试
 *
 * 测试场景：
 * 1. saveProvider 应该使用 formTimeoutMs
 * 2. showEditDialog 应该加载 timeoutMs
 * 3. updateFormTimeoutMs 应该更新状态
 * 4. 超时值应该被限制在有效范围内
 *
 * @see BUG-00054-AI配置功能多项问题.md
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AiConfigViewModelBug00054V2Test {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var getProvidersUseCase: GetProvidersUseCase
    private lateinit var saveProviderUseCase: SaveProviderUseCase
    private lateinit var deleteProviderUseCase: DeleteProviderUseCase
    private lateinit var testConnectionUseCase: TestConnectionUseCase
    private lateinit var aiProviderRepository: AiProviderRepository

    private val testModels = listOf(
        AiModel(id = "gpt-4", displayName = "GPT-4"),
        AiModel(id = "gpt-3.5-turbo", displayName = "GPT-3.5 Turbo")
    )

    private val testProvider = AiProvider(
        id = "test-provider-1",
        name = "Test Provider",
        baseUrl = "https://api.test.com/v1",
        apiKey = "sk-test123456789",
        models = testModels,
        defaultModelId = "gpt-4",
        isDefault = true,
        timeoutMs = 60000L  // 60秒超时
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        getProvidersUseCase = mockk(relaxed = true)
        saveProviderUseCase = mockk(relaxed = true)
        deleteProviderUseCase = mockk(relaxed = true)
        testConnectionUseCase = mockk(relaxed = true)
        aiProviderRepository = mockk(relaxed = true)

        // 默认 mock 行为
        every { getProvidersUseCase() } returns flowOf(emptyList())
        coEvery { aiProviderRepository.getProxyConfig() } returns ProxyConfig()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ==================== P3 第二轮修复：超时设置测试 ====================

    @Test
    fun `saveProvider_shouldUseFormTimeoutMs`() = runTest {
        // Given
        val providerSlot = slot<AiProvider>()
        coEvery { saveProviderUseCase(capture(providerSlot)) } returns Result.success(Unit)

        val viewModel = createViewModel()
        advanceUntilIdle()

        // 打开添加对话框并填写表单
        viewModel.onEvent(AiConfigUiEvent.ShowAddDialog)
        viewModel.onEvent(AiConfigUiEvent.UpdateFormName("Test Provider"))
        viewModel.onEvent(AiConfigUiEvent.UpdateFormBaseUrl("https://api.test.com/v1"))
        viewModel.onEvent(AiConfigUiEvent.UpdateFormApiKey("sk-test123456789"))
        viewModel.onEvent(AiConfigUiEvent.AddFormModel("gpt-4", "GPT-4"))
        
        // 设置超时为 60 秒
        viewModel.onEvent(AiConfigUiEvent.UpdateFormTimeoutMs(60000L))
        advanceUntilIdle()

        // When: 保存
        viewModel.onEvent(AiConfigUiEvent.SaveProvider)
        advanceUntilIdle()

        // Then: 保存的 Provider 应该包含正确的 timeoutMs
        coVerify { saveProviderUseCase(any()) }
        assertEquals(60000L, providerSlot.captured.timeoutMs)
    }

    @Test
    fun `showEditDialog_shouldLoadTimeoutMs`() = runTest {
        // Given
        val viewModel = createViewModel()
        advanceUntilIdle()

        // When: 打开编辑对话框
        viewModel.onEvent(AiConfigUiEvent.ShowEditDialog(testProvider))
        advanceUntilIdle()

        // Then: 表单应该加载 Provider 的 timeoutMs
        val state = viewModel.uiState.value
        assertEquals(60000L, state.formTimeoutMs)
    }

    @Test
    fun `updateFormTimeoutMs_shouldUpdateState`() = runTest {
        // Given
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(AiConfigUiEvent.ShowAddDialog)
        advanceUntilIdle()

        // 确认初始值
        assertEquals(30000L, viewModel.uiState.value.formTimeoutMs)

        // When: 更新超时设置
        viewModel.onEvent(AiConfigUiEvent.UpdateFormTimeoutMs(90000L))
        advanceUntilIdle()

        // Then: 状态应该更新
        assertEquals(90000L, viewModel.uiState.value.formTimeoutMs)
    }

    @Test
    fun `updateFormTimeoutMs_shouldClampToValidRange_whenTooSmall`() = runTest {
        // Given
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(AiConfigUiEvent.ShowAddDialog)
        advanceUntilIdle()

        // When: 设置过小的超时值（小于5秒）
        viewModel.onEvent(AiConfigUiEvent.UpdateFormTimeoutMs(1000L))
        advanceUntilIdle()

        // Then: 应该被限制到最小值 5000ms
        assertEquals(5000L, viewModel.uiState.value.formTimeoutMs)
    }

    @Test
    fun `updateFormTimeoutMs_shouldClampToValidRange_whenTooLarge`() = runTest {
        // Given
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(AiConfigUiEvent.ShowAddDialog)
        advanceUntilIdle()

        // When: 设置过大的超时值（大于120秒）
        viewModel.onEvent(AiConfigUiEvent.UpdateFormTimeoutMs(200000L))
        advanceUntilIdle()

        // Then: 应该被限制到最大值 120000ms
        assertEquals(120000L, viewModel.uiState.value.formTimeoutMs)
    }

    @Test
    fun `showAddDialog_shouldUseDefaultTimeoutMs`() = runTest {
        // Given
        val viewModel = createViewModel()
        advanceUntilIdle()

        // When: 打开添加对话框
        viewModel.onEvent(AiConfigUiEvent.ShowAddDialog)
        advanceUntilIdle()

        // Then: 应该使用默认超时值 30000ms
        assertEquals(30000L, viewModel.uiState.value.formTimeoutMs)
    }

    @Test
    fun `saveProvider_withCustomTimeout_shouldPersistCorrectly`() = runTest {
        // Given
        val providerSlot = slot<AiProvider>()
        coEvery { saveProviderUseCase(capture(providerSlot)) } returns Result.success(Unit)

        val viewModel = createViewModel()
        advanceUntilIdle()

        // 打开添加对话框并填写完整表单
        viewModel.onEvent(AiConfigUiEvent.ShowAddDialog)
        viewModel.onEvent(AiConfigUiEvent.UpdateFormName("Custom Timeout Provider"))
        viewModel.onEvent(AiConfigUiEvent.UpdateFormBaseUrl("https://api.custom.com/v1"))
        viewModel.onEvent(AiConfigUiEvent.UpdateFormApiKey("sk-custom123456789"))
        viewModel.onEvent(AiConfigUiEvent.AddFormModel("custom-model", "Custom Model"))
        viewModel.onEvent(AiConfigUiEvent.UpdateFormTimeoutMs(45000L))  // 45秒
        advanceUntilIdle()

        // When: 保存
        viewModel.onEvent(AiConfigUiEvent.SaveProvider)
        advanceUntilIdle()

        // Then: 验证保存的 Provider
        coVerify { saveProviderUseCase(any()) }
        val savedProvider = providerSlot.captured
        assertEquals("Custom Timeout Provider", savedProvider.name)
        assertEquals(45000L, savedProvider.timeoutMs)
    }

    // ==================== 辅助方法 ====================

    private fun createViewModel(): AiConfigViewModel {
        return AiConfigViewModel(
            getProvidersUseCase = getProvidersUseCase,
            saveProviderUseCase = saveProviderUseCase,
            deleteProviderUseCase = deleteProviderUseCase,
            testConnectionUseCase = testConnectionUseCase,
            aiProviderRepository = aiProviderRepository
        )
    }
}
