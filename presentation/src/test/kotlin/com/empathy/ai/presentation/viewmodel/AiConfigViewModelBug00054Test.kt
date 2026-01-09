package com.empathy.ai.presentation.viewmodel

import com.empathy.ai.domain.model.AiModel
import com.empathy.ai.domain.model.AiProvider
import com.empathy.ai.domain.model.ConnectionTestResult
import com.empathy.ai.domain.model.ProxyConfig
import com.empathy.ai.domain.repository.AiProviderRepository
import com.empathy.ai.domain.usecase.DeleteProviderUseCase
import com.empathy.ai.domain.usecase.GetProvidersUseCase
import com.empathy.ai.domain.usecase.SaveProviderUseCase
import com.empathy.ai.domain.usecase.TestConnectionUseCase
import com.empathy.ai.domain.usecase.ValidationException
import com.empathy.ai.presentation.ui.screen.aiconfig.AiConfigUiEvent
import com.empathy.ai.presentation.ui.screen.aiconfig.FormModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * BUG-00054 AiConfigViewModel 专用测试
 *
 * 测试场景：
 * 1. 添加模型时自动设置默认模型
 * 2. 删除默认模型时自动选择新默认
 * 3. 保存失败时正确显示错误
 * 4. 超时设置的状态更新
 *
 * @see BUG-00054-AI配置功能多项问题.md
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AiConfigViewModelBug00054Test {

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
        apiKey = "sk-test123",
        models = testModels,
        defaultModelId = "gpt-4",
        isDefault = true
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

    // ==================== P1: 添加模型时自动设置默认模型 ====================

    @Test
    fun `addFormModel_whenFirstModel_setsAsDefault`() = runTest {
        // Given
        val viewModel = createViewModel()
        advanceUntilIdle()

        // 打开添加对话框
        viewModel.onEvent(AiConfigUiEvent.ShowAddDialog)
        advanceUntilIdle()

        // 确认初始状态：没有模型，没有默认模型
        assertEquals(0, viewModel.uiState.value.formModels.size)
        assertEquals("", viewModel.uiState.value.formDefaultModelId)

        // When: 添加第一个模型
        viewModel.onEvent(AiConfigUiEvent.AddFormModel("gpt-4", "GPT-4"))
        advanceUntilIdle()

        // Then: 第一个模型应该自动成为默认
        val state = viewModel.uiState.value
        assertEquals(1, state.formModels.size)
        assertEquals("gpt-4", state.formDefaultModelId)
    }

    @Test
    fun `addFormModel_whenNotFirstModel_keepsExistingDefault`() = runTest {
        // Given
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(AiConfigUiEvent.ShowAddDialog)
        advanceUntilIdle()

        // 添加第一个模型
        viewModel.onEvent(AiConfigUiEvent.AddFormModel("gpt-4", "GPT-4"))
        advanceUntilIdle()

        // When: 添加第二个模型
        viewModel.onEvent(AiConfigUiEvent.AddFormModel("gpt-3.5-turbo", "GPT-3.5"))
        advanceUntilIdle()

        // Then: 默认模型应该保持不变
        val state = viewModel.uiState.value
        assertEquals(2, state.formModels.size)
        assertEquals("gpt-4", state.formDefaultModelId)  // 仍然是第一个
    }

    @Test
    fun `addFormModel_withDuplicateId_showsError`() = runTest {
        // Given
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(AiConfigUiEvent.ShowAddDialog)
        viewModel.onEvent(AiConfigUiEvent.AddFormModel("gpt-4", "GPT-4"))
        advanceUntilIdle()

        // When: 添加重复ID的模型
        viewModel.onEvent(AiConfigUiEvent.AddFormModel("gpt-4", "Another GPT-4"))
        advanceUntilIdle()

        // Then: 应该显示错误
        val state = viewModel.uiState.value
        assertEquals(1, state.formModels.size)  // 仍然只有一个
        assertNotNull(state.formModelsError)
        assertTrue(state.formModelsError!!.contains("已存在"))
    }

    // ==================== P1: 删除默认模型时自动选择新默认 ====================

    @Test
    fun `removeFormModel_whenRemovingDefault_selectsNewDefault`() = runTest {
        // Given
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(AiConfigUiEvent.ShowAddDialog)
        viewModel.onEvent(AiConfigUiEvent.AddFormModel("gpt-4", "GPT-4"))
        viewModel.onEvent(AiConfigUiEvent.AddFormModel("gpt-3.5-turbo", "GPT-3.5"))
        advanceUntilIdle()

        // 确认 gpt-4 是默认
        assertEquals("gpt-4", viewModel.uiState.value.formDefaultModelId)

        // When: 删除默认模型
        viewModel.onEvent(AiConfigUiEvent.RemoveFormModel("gpt-4"))
        advanceUntilIdle()

        // Then: 应该自动选择剩余的第一个作为默认
        val state = viewModel.uiState.value
        assertEquals(1, state.formModels.size)
        assertEquals("gpt-3.5-turbo", state.formDefaultModelId)
    }

    @Test
    fun `removeFormModel_whenRemovingNonDefault_keepsDefault`() = runTest {
        // Given
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(AiConfigUiEvent.ShowAddDialog)
        viewModel.onEvent(AiConfigUiEvent.AddFormModel("gpt-4", "GPT-4"))
        viewModel.onEvent(AiConfigUiEvent.AddFormModel("gpt-3.5-turbo", "GPT-3.5"))
        advanceUntilIdle()

        // When: 删除非默认模型
        viewModel.onEvent(AiConfigUiEvent.RemoveFormModel("gpt-3.5-turbo"))
        advanceUntilIdle()

        // Then: 默认模型应该保持不变
        val state = viewModel.uiState.value
        assertEquals(1, state.formModels.size)
        assertEquals("gpt-4", state.formDefaultModelId)
    }

    @Test
    fun `removeFormModel_whenRemovingLastModel_clearsDefault`() = runTest {
        // Given
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(AiConfigUiEvent.ShowAddDialog)
        viewModel.onEvent(AiConfigUiEvent.AddFormModel("gpt-4", "GPT-4"))
        advanceUntilIdle()

        // When: 删除唯一的模型
        viewModel.onEvent(AiConfigUiEvent.RemoveFormModel("gpt-4"))
        advanceUntilIdle()

        // Then: 默认模型应该被清空，并显示错误
        val state = viewModel.uiState.value
        assertEquals(0, state.formModels.size)
        assertEquals("", state.formDefaultModelId)
        assertNotNull(state.formModelsError)
    }

    // ==================== P1: 保存失败时正确显示错误 ====================

    @Test
    fun `saveProvider_whenValidationFails_showsError`() = runTest {
        // Given
        val viewModel = createViewModel()
        advanceUntilIdle()

        // 模拟验证失败
        coEvery { saveProviderUseCase(any()) } returns Result.failure(
            ValidationException("默认模型必须在模型列表中")
        )

        viewModel.onEvent(AiConfigUiEvent.ShowAddDialog)
        viewModel.onEvent(AiConfigUiEvent.UpdateFormName("Test Provider"))
        viewModel.onEvent(AiConfigUiEvent.UpdateFormBaseUrl("https://api.test.com/v1"))
        viewModel.onEvent(AiConfigUiEvent.UpdateFormApiKey("sk-test12345678"))
        viewModel.onEvent(AiConfigUiEvent.AddFormModel("gpt-4", "GPT-4"))
        advanceUntilIdle()

        // When: 尝试保存
        viewModel.onEvent(AiConfigUiEvent.SaveProvider)
        advanceUntilIdle()

        // Then: 应该显示错误
        val state = viewModel.uiState.value
        assertNotNull(state.error)
        assertTrue(state.error!!.contains("默认模型"))
        assertFalse(state.isSaving)
    }

    @Test
    fun `saveProvider_whenSuccess_closesDialogAndNavigatesBack`() = runTest {
        // Given
        val viewModel = createViewModel()
        advanceUntilIdle()

        coEvery { saveProviderUseCase(any()) } returns Result.success(Unit)

        viewModel.onEvent(AiConfigUiEvent.ShowAddDialog)
        viewModel.onEvent(AiConfigUiEvent.UpdateFormName("Test Provider"))
        viewModel.onEvent(AiConfigUiEvent.UpdateFormBaseUrl("https://api.test.com/v1"))
        viewModel.onEvent(AiConfigUiEvent.UpdateFormApiKey("sk-test12345678"))
        viewModel.onEvent(AiConfigUiEvent.AddFormModel("gpt-4", "GPT-4"))
        advanceUntilIdle()

        // When: 保存
        viewModel.onEvent(AiConfigUiEvent.SaveProvider)
        advanceUntilIdle()

        // Then: 对话框应该关闭，触发导航返回
        val state = viewModel.uiState.value
        assertFalse(state.showFormDialog)
        assertTrue(state.shouldNavigateBack)
        assertNull(state.error)
    }

    @Test
    fun `saveProvider_whenEmptyModels_showsValidationError`() = runTest {
        // Given
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(AiConfigUiEvent.ShowAddDialog)
        viewModel.onEvent(AiConfigUiEvent.UpdateFormName("Test Provider"))
        viewModel.onEvent(AiConfigUiEvent.UpdateFormBaseUrl("https://api.test.com/v1"))
        viewModel.onEvent(AiConfigUiEvent.UpdateFormApiKey("sk-test12345678"))
        // 不添加任何模型
        advanceUntilIdle()

        // When: 尝试保存
        viewModel.onEvent(AiConfigUiEvent.SaveProvider)
        advanceUntilIdle()

        // Then: 应该显示模型验证错误
        val state = viewModel.uiState.value
        assertNotNull(state.formModelsError)
        // SaveProviderUseCase 不应该被调用
        coVerify(exactly = 0) { saveProviderUseCase(any()) }
    }

    // ==================== P3: 超时设置状态更新 ====================

    @Test
    fun `updateRequestTimeout_updatesState`() = runTest {
        // Given
        val viewModel = createViewModel()
        advanceUntilIdle()

        // When: 更新超时设置
        viewModel.onEvent(AiConfigUiEvent.UpdateRequestTimeout(60))
        advanceUntilIdle()

        // Then: 状态应该更新
        assertEquals(60, viewModel.uiState.value.requestTimeout)
    }

    @Test
    fun `updateMaxTokens_updatesState`() = runTest {
        // Given
        val viewModel = createViewModel()
        advanceUntilIdle()

        // When: 更新最大Token数
        viewModel.onEvent(AiConfigUiEvent.UpdateMaxTokens(8192))
        advanceUntilIdle()

        // Then: 状态应该更新
        assertEquals(8192, viewModel.uiState.value.maxTokens)
    }

    // ==================== 编辑模式测试 ====================

    @Test
    fun `showEditDialog_loadsProviderData`() = runTest {
        // Given
        val viewModel = createViewModel()
        advanceUntilIdle()

        // When: 打开编辑对话框
        viewModel.onEvent(AiConfigUiEvent.ShowEditDialog(testProvider))
        advanceUntilIdle()

        // Then: 表单应该填充供应商数据
        val state = viewModel.uiState.value
        assertTrue(state.showFormDialog)
        assertEquals(testProvider.name, state.formName)
        assertEquals(testProvider.baseUrl, state.formBaseUrl)
        assertEquals(testProvider.apiKey, state.formApiKey)
        assertEquals(testProvider.models.size, state.formModels.size)
        assertEquals(testProvider.defaultModelId, state.formDefaultModelId)
    }

    @Test
    fun `setFormDefaultModel_updatesDefault`() = runTest {
        // Given
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(AiConfigUiEvent.ShowAddDialog)
        viewModel.onEvent(AiConfigUiEvent.AddFormModel("gpt-4", "GPT-4"))
        viewModel.onEvent(AiConfigUiEvent.AddFormModel("gpt-3.5-turbo", "GPT-3.5"))
        advanceUntilIdle()

        // 确认初始默认是 gpt-4
        assertEquals("gpt-4", viewModel.uiState.value.formDefaultModelId)

        // When: 手动设置新的默认模型
        viewModel.onEvent(AiConfigUiEvent.SetFormDefaultModel("gpt-3.5-turbo"))
        advanceUntilIdle()

        // Then: 默认模型应该更新
        assertEquals("gpt-3.5-turbo", viewModel.uiState.value.formDefaultModelId)
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
