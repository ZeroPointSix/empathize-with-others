package com.empathy.ai.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.empathy.ai.domain.model.PromptScene
import com.empathy.ai.domain.repository.PromptRepository
import com.empathy.ai.domain.util.PromptValidator
import com.empathy.ai.domain.util.PromptVariableResolver
import com.empathy.ai.presentation.ui.screen.prompt.PromptEditMode
import com.empathy.ai.presentation.ui.screen.prompt.PromptEditorResult
import com.empathy.ai.presentation.ui.screen.prompt.PromptEditorUiEvent
import com.empathy.ai.presentation.ui.screen.prompt.PromptEditorUiState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * PromptEditorViewModel单元测试
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PromptEditorViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var promptRepository: PromptRepository
    private lateinit var promptValidator: PromptValidator
    private lateinit var variableResolver: PromptVariableResolver

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        promptRepository = mockk(relaxed = true)
        variableResolver = mockk(relaxed = true)
        promptValidator = PromptValidator(variableResolver)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(
        mode: String = "global",
        scene: String? = PromptScene.ANALYZE.name,
        contactId: String? = null,
        contactName: String? = null
    ): PromptEditorViewModel {
        val savedStateHandle = SavedStateHandle().apply {
            set("mode", mode)
            scene?.let { set("scene", it) }
            contactId?.let { set("contactId", it) }
            contactName?.let { set("contactName", it) }
        }
        return PromptEditorViewModel(promptRepository, promptValidator, savedStateHandle)
    }

    // ========== 初始化测试 ==========

    @Test
    fun `init loads global prompt successfully`() = runTest(testDispatcher) {
        // Given
        coEvery { promptRepository.getGlobalPrompt(PromptScene.ANALYZE) } returns Result.success("test prompt")

        // When
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals("test prompt", state.currentPrompt)
        assertEquals("test prompt", state.originalPrompt)
        assertFalse(state.isLoading)
    }

    @Test
    fun `init loads contact prompt successfully`() = runTest(testDispatcher) {
        // Given
        coEvery { promptRepository.getContactPrompt("1") } returns Result.success("contact prompt")

        // When
        val viewModel = createViewModel(
            mode = "contact",
            contactId = "1",
            contactName = "小明"
        )
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals("contact prompt", state.currentPrompt)
        assertTrue(state.editMode is PromptEditMode.ContactCustom)
    }

    @Test
    fun `init handles load failure`() = runTest(testDispatcher) {
        // Given
        coEvery { promptRepository.getGlobalPrompt(any()) } returns Result.failure(Exception("Network error"))

        // When
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.errorMessage?.contains("加载失败") == true)
    }

    // ========== 输入处理测试 ==========

    @Test
    fun `UpdatePrompt updates current prompt`() = runTest(testDispatcher) {
        // Given
        coEvery { promptRepository.getGlobalPrompt(any()) } returns Result.success("")
        val viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.onEvent(PromptEditorUiEvent.UpdatePrompt("new text"))
        advanceUntilIdle()

        // Then
        assertEquals("new text", viewModel.uiState.value.currentPrompt)
    }

    @Test
    fun `large text input is truncated to max length`() = runTest(testDispatcher) {
        // Given
        coEvery { promptRepository.getGlobalPrompt(any()) } returns Result.success("")
        val viewModel = createViewModel()
        advanceUntilIdle()

        // When
        val largeText = "a".repeat(5000)
        viewModel.onEvent(PromptEditorUiEvent.UpdatePrompt(largeText))
        advanceUntilIdle()

        // Then
        assertEquals(PromptEditorUiState.MAX_PROMPT_LENGTH, viewModel.uiState.value.currentPrompt.length)
    }

    // ========== 保存测试 ==========

    @Test
    fun `SavePrompt saves global prompt successfully`() = runTest(testDispatcher) {
        // Given
        coEvery { promptRepository.getGlobalPrompt(any()) } returns Result.success("")
        coEvery { promptRepository.saveGlobalPrompt(any(), any()) } returns Result.success(Unit)
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(PromptEditorUiEvent.UpdatePrompt("new prompt"))
        advanceUntilIdle()

        // When - 收集结果
        var emittedResult: PromptEditorResult? = null
        val job = launch {
            emittedResult = viewModel.result.first()
        }
        
        viewModel.onEvent(PromptEditorUiEvent.SavePrompt)
        advanceUntilIdle()
        job.join()

        // Then
        assertEquals(PromptEditorResult.Saved, emittedResult)
        coVerify { promptRepository.saveGlobalPrompt(PromptScene.ANALYZE, "new prompt") }
    }

    @Test
    fun `SavePrompt handles save failure`() = runTest(testDispatcher) {
        // Given
        coEvery { promptRepository.getGlobalPrompt(any()) } returns Result.success("")
        coEvery { promptRepository.saveGlobalPrompt(any(), any()) } returns Result.failure(Exception("Save failed"))
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(PromptEditorUiEvent.UpdatePrompt("new prompt"))
        advanceUntilIdle()

        // When
        viewModel.onEvent(PromptEditorUiEvent.SavePrompt)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertTrue(state.errorMessage?.contains("保存失败") == true)
        assertFalse(state.isSaving)
    }

    @Test
    fun `SavePrompt does nothing when canSave is false`() = runTest(testDispatcher) {
        // Given
        coEvery { promptRepository.getGlobalPrompt(any()) } returns Result.success("")
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Set over limit text
        viewModel.onEvent(PromptEditorUiEvent.UpdatePrompt("a".repeat(1001)))
        advanceUntilIdle()

        // When
        viewModel.onEvent(PromptEditorUiEvent.SavePrompt)
        advanceUntilIdle()

        // Then - should not call save
        coVerify(exactly = 0) { promptRepository.saveGlobalPrompt(any(), any()) }
    }

    // ========== 取消测试 ==========

    @Test
    fun `CancelEdit shows discard dialog when has unsaved changes`() = runTest(testDispatcher) {
        // Given
        coEvery { promptRepository.getGlobalPrompt(any()) } returns Result.success("original")
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(PromptEditorUiEvent.UpdatePrompt("modified"))
        advanceUntilIdle()

        // When
        viewModel.onEvent(PromptEditorUiEvent.CancelEdit)
        advanceUntilIdle()

        // Then
        assertTrue(viewModel.uiState.value.showDiscardDialog)
    }

    @Test
    fun `CancelEdit emits Cancelled when no unsaved changes`() = runTest(testDispatcher) {
        // Given
        coEvery { promptRepository.getGlobalPrompt(any()) } returns Result.success("original")
        val viewModel = createViewModel()
        advanceUntilIdle()

        // When - 收集结果
        var emittedResult: PromptEditorResult? = null
        val job = launch {
            emittedResult = viewModel.result.first()
        }
        
        viewModel.onEvent(PromptEditorUiEvent.CancelEdit)
        advanceUntilIdle()
        job.join()

        // Then
        assertEquals(PromptEditorResult.Cancelled, emittedResult)
    }

    @Test
    fun `ConfirmDiscard emits Cancelled and hides dialog`() = runTest(testDispatcher) {
        // Given
        coEvery { promptRepository.getGlobalPrompt(any()) } returns Result.success("original")
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(PromptEditorUiEvent.UpdatePrompt("modified"))
        viewModel.onEvent(PromptEditorUiEvent.CancelEdit)
        advanceUntilIdle()

        // When - 收集结果
        var emittedResult: PromptEditorResult? = null
        val job = launch {
            emittedResult = viewModel.result.first()
        }
        
        viewModel.onEvent(PromptEditorUiEvent.ConfirmDiscard)
        advanceUntilIdle()
        job.join()

        // Then
        assertEquals(PromptEditorResult.Cancelled, emittedResult)
        assertFalse(viewModel.uiState.value.showDiscardDialog)
    }

    @Test
    fun `DismissDiscardDialog hides dialog`() = runTest(testDispatcher) {
        // Given
        coEvery { promptRepository.getGlobalPrompt(any()) } returns Result.success("original")
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(PromptEditorUiEvent.UpdatePrompt("modified"))
        viewModel.onEvent(PromptEditorUiEvent.CancelEdit)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.showDiscardDialog)

        // When
        viewModel.onEvent(PromptEditorUiEvent.DismissDiscardDialog)
        advanceUntilIdle()

        // Then
        assertFalse(viewModel.uiState.value.showDiscardDialog)
    }

    // ========== 错误处理测试 ==========

    @Test
    fun `ClearError clears error message`() = runTest(testDispatcher) {
        // Given
        coEvery { promptRepository.getGlobalPrompt(any()) } returns Result.failure(Exception("Error"))
        val viewModel = createViewModel()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.errorMessage != null)

        // When
        viewModel.onEvent(PromptEditorUiEvent.ClearError)
        advanceUntilIdle()

        // Then
        assertNull(viewModel.uiState.value.errorMessage)
    }

    // ========== 占位符测试 ==========

    @Test
    fun `getPlaceholderText returns correct text for ANALYZE scene`() {
        val placeholder = PromptEditorViewModel.getPlaceholderText(
            PromptEditMode.GlobalScene(PromptScene.ANALYZE)
        )
        assertTrue(placeholder.contains("分析"))
    }

    @Test
    fun `getPlaceholderText returns correct text for CHECK scene`() {
        val placeholder = PromptEditorViewModel.getPlaceholderText(
            PromptEditMode.GlobalScene(PromptScene.CHECK)
        )
        assertTrue(placeholder.contains("检查"))
    }

    @Test
    fun `getPlaceholderText returns correct text for EXTRACT scene`() {
        val placeholder = PromptEditorViewModel.getPlaceholderText(
            PromptEditMode.GlobalScene(PromptScene.EXTRACT)
        )
        assertTrue(placeholder.contains("关注"))
    }

    @Test
    fun `getPlaceholderText returns correct text for SUMMARY scene`() {
        val placeholder = PromptEditorViewModel.getPlaceholderText(
            PromptEditMode.GlobalScene(PromptScene.SUMMARY)
        )
        assertTrue(placeholder.contains("总结"))
    }

    @Test
    fun `getPlaceholderText returns correct text for ContactCustom mode`() {
        val placeholder = PromptEditorViewModel.getPlaceholderText(
            PromptEditMode.ContactCustom("1", "小明")
        )
        assertTrue(placeholder.contains("聊天"))
    }
}
