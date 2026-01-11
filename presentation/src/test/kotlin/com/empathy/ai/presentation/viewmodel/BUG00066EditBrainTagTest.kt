package com.empathy.ai.presentation.viewmodel

import com.empathy.ai.domain.model.BrainTag
import com.empathy.ai.domain.model.TagType
import com.empathy.ai.domain.usecase.DeleteBrainTagUseCase
import com.empathy.ai.domain.usecase.EditBrainTagUseCase
import com.empathy.ai.domain.usecase.GetBrainTagsUseCase
import com.empathy.ai.domain.usecase.SaveBrainTagUseCase
import com.empathy.ai.presentation.ui.screen.tag.BrainTagUiEvent
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
 * BUG-00066 画像标签编辑功能缺失 - 回归测试
 *
 * ## 测试场景
 * 1. 点击标签显示编辑对话框
 * 2. 修改标签内容
 * 3. 切换标签类型
 * 4. 保存编辑
 * 5. 取消编辑
 * 6. 空内容验证
 *
 * @see BrainTagViewModel
 * @see EditBrainTagUseCase
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BUG00066EditBrainTagTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var getBrainTagsUseCase: GetBrainTagsUseCase
    private lateinit var saveBrainTagUseCase: SaveBrainTagUseCase
    private lateinit var deleteBrainTagUseCase: DeleteBrainTagUseCase
    private lateinit var editBrainTagUseCase: EditBrainTagUseCase
    private lateinit var viewModel: BrainTagViewModel

    private val testTag = BrainTag(
        id = 1L,
        contactId = "contact_1",
        content = "不要提工作压力",
        type = TagType.RISK_RED,
        source = "MANUAL"
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        getBrainTagsUseCase = mockk()
        saveBrainTagUseCase = mockk()
        deleteBrainTagUseCase = mockk()
        editBrainTagUseCase = mockk()

        // 默认返回空列表
        every { getBrainTagsUseCase(any()) } returns flowOf(listOf(testTag))

        viewModel = BrainTagViewModel(
            getBrainTagsUseCase = getBrainTagsUseCase,
            saveBrainTagUseCase = saveBrainTagUseCase,
            deleteBrainTagUseCase = deleteBrainTagUseCase,
            editBrainTagUseCase = editBrainTagUseCase
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ==================== TC-001: 点击标签显示编辑对话框 ====================

    @Test
    fun `TC-001 点击标签后显示编辑对话框`() = runTest {
        // Given
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.showEditDialog)
        assertNull(viewModel.uiState.value.editingTag)

        // When
        viewModel.onEvent(BrainTagUiEvent.StartEditTag(testTag))
        advanceUntilIdle()

        // Then
        assertTrue(viewModel.uiState.value.showEditDialog)
        assertNotNull(viewModel.uiState.value.editingTag)
        assertEquals(testTag.id, viewModel.uiState.value.editingTag?.id)
        assertEquals(testTag.content, viewModel.uiState.value.editingTag?.content)
        assertEquals(testTag.type, viewModel.uiState.value.editingTag?.type)
    }

    // ==================== TC-002: 修改标签内容 ====================

    @Test
    fun `TC-002 修改标签内容后保存成功`() = runTest {
        // Given
        val newContent = "新的标签内容"
        coEvery { editBrainTagUseCase(testTag.id, newContent, testTag.type) } returns Result.success(Unit)

        advanceUntilIdle()
        viewModel.onEvent(BrainTagUiEvent.StartEditTag(testTag))
        advanceUntilIdle()

        // When
        viewModel.onEvent(BrainTagUiEvent.ConfirmEditTag(testTag.id, newContent, testTag.type))
        advanceUntilIdle()

        // Then
        coVerify { editBrainTagUseCase(testTag.id, newContent, testTag.type) }
        assertFalse(viewModel.uiState.value.showEditDialog)
        assertNull(viewModel.uiState.value.editingTag)
    }

    // ==================== TC-003: 切换标签类型 ====================

    @Test
    fun `TC-003 切换标签类型从雷区到策略`() = runTest {
        // Given
        val newType = TagType.STRATEGY_GREEN
        coEvery { editBrainTagUseCase(testTag.id, testTag.content, newType) } returns Result.success(Unit)

        advanceUntilIdle()
        viewModel.onEvent(BrainTagUiEvent.StartEditTag(testTag))
        advanceUntilIdle()

        // When
        viewModel.onEvent(BrainTagUiEvent.ConfirmEditTag(testTag.id, testTag.content, newType))
        advanceUntilIdle()

        // Then
        coVerify { editBrainTagUseCase(testTag.id, testTag.content, newType) }
        assertFalse(viewModel.uiState.value.showEditDialog)
    }

    @Test
    fun `TC-003b 切换标签类型从策略到雷区`() = runTest {
        // Given
        val strategyTag = testTag.copy(type = TagType.STRATEGY_GREEN)
        val newType = TagType.RISK_RED
        coEvery { editBrainTagUseCase(strategyTag.id, strategyTag.content, newType) } returns Result.success(Unit)

        advanceUntilIdle()
        viewModel.onEvent(BrainTagUiEvent.StartEditTag(strategyTag))
        advanceUntilIdle()

        // When
        viewModel.onEvent(BrainTagUiEvent.ConfirmEditTag(strategyTag.id, strategyTag.content, newType))
        advanceUntilIdle()

        // Then
        coVerify { editBrainTagUseCase(strategyTag.id, strategyTag.content, newType) }
    }

    // ==================== TC-004: 取消编辑 ====================

    @Test
    fun `TC-004 取消编辑后对话框关闭且数据不变`() = runTest {
        // Given
        advanceUntilIdle()
        viewModel.onEvent(BrainTagUiEvent.StartEditTag(testTag))
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.showEditDialog)

        // When
        viewModel.onEvent(BrainTagUiEvent.CancelEditTag)
        advanceUntilIdle()

        // Then
        assertFalse(viewModel.uiState.value.showEditDialog)
        assertNull(viewModel.uiState.value.editingTag)
        coVerify(exactly = 0) { editBrainTagUseCase(any(), any(), any()) }
    }

    // ==================== TC-005: 空内容验证 ====================

    @Test
    fun `TC-005 保存空内容时显示错误`() = runTest {
        // Given
        advanceUntilIdle()
        viewModel.onEvent(BrainTagUiEvent.StartEditTag(testTag))
        advanceUntilIdle()

        // When
        viewModel.onEvent(BrainTagUiEvent.ConfirmEditTag(testTag.id, "", testTag.type))
        advanceUntilIdle()

        // Then
        assertNotNull(viewModel.uiState.value.error)
        assertEquals("标签内容不能为空", viewModel.uiState.value.error)
        coVerify(exactly = 0) { editBrainTagUseCase(any(), any(), any()) }
    }

    @Test
    fun `TC-005b 保存只有空格的内容时显示错误`() = runTest {
        // Given
        advanceUntilIdle()
        viewModel.onEvent(BrainTagUiEvent.StartEditTag(testTag))
        advanceUntilIdle()

        // When
        viewModel.onEvent(BrainTagUiEvent.ConfirmEditTag(testTag.id, "   ", testTag.type))
        advanceUntilIdle()

        // Then
        assertNotNull(viewModel.uiState.value.error)
        assertEquals("标签内容不能为空", viewModel.uiState.value.error)
    }

    // ==================== TC-006: 编辑失败处理 ====================

    @Test
    fun `TC-006 编辑失败时显示错误信息`() = runTest {
        // Given
        val errorMessage = "数据库更新失败"
        coEvery { editBrainTagUseCase(any(), any(), any()) } returns Result.failure(Exception(errorMessage))

        advanceUntilIdle()
        viewModel.onEvent(BrainTagUiEvent.StartEditTag(testTag))
        advanceUntilIdle()

        // When
        viewModel.onEvent(BrainTagUiEvent.ConfirmEditTag(testTag.id, "新内容", testTag.type))
        advanceUntilIdle()

        // Then
        assertNotNull(viewModel.uiState.value.error)
        assertEquals(errorMessage, viewModel.uiState.value.error)
    }

    // ==================== TC-007: 同时修改内容和类型 ====================

    @Test
    fun `TC-007 同时修改内容和类型`() = runTest {
        // Given
        val newContent = "新的策略标签"
        val newType = TagType.STRATEGY_GREEN
        coEvery { editBrainTagUseCase(testTag.id, newContent, newType) } returns Result.success(Unit)

        advanceUntilIdle()
        viewModel.onEvent(BrainTagUiEvent.StartEditTag(testTag))
        advanceUntilIdle()

        // When
        viewModel.onEvent(BrainTagUiEvent.ConfirmEditTag(testTag.id, newContent, newType))
        advanceUntilIdle()

        // Then
        coVerify { editBrainTagUseCase(testTag.id, newContent, newType) }
        assertFalse(viewModel.uiState.value.showEditDialog)
    }

    // ==================== TC-008: 编辑AI推断的标签 ====================

    @Test
    fun `TC-008 编辑AI推断的标签`() = runTest {
        // Given
        val aiTag = BrainTag(
            id = 2L,
            contactId = "contact_1",
            content = "AI推断的标签",
            type = TagType.RISK_RED,
            source = "AI_INFERRED"
        )
        val newContent = "修改后的AI标签"
        coEvery { editBrainTagUseCase(aiTag.id, newContent, aiTag.type) } returns Result.success(Unit)

        advanceUntilIdle()
        viewModel.onEvent(BrainTagUiEvent.StartEditTag(aiTag))
        advanceUntilIdle()

        // When
        viewModel.onEvent(BrainTagUiEvent.ConfirmEditTag(aiTag.id, newContent, aiTag.type))
        advanceUntilIdle()

        // Then
        coVerify { editBrainTagUseCase(aiTag.id, newContent, aiTag.type) }
        assertFalse(viewModel.uiState.value.showEditDialog)
    }

    // ==================== TC-009: 清除错误后可以继续编辑 ====================

    @Test
    fun `TC-009 清除错误后可以继续编辑`() = runTest {
        // Given - 先触发一个错误
        advanceUntilIdle()
        viewModel.onEvent(BrainTagUiEvent.StartEditTag(testTag))
        advanceUntilIdle()
        viewModel.onEvent(BrainTagUiEvent.ConfirmEditTag(testTag.id, "", testTag.type))
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.error)

        // When - 清除错误
        viewModel.onEvent(BrainTagUiEvent.ClearError)
        advanceUntilIdle()

        // Then
        assertNull(viewModel.uiState.value.error)

        // And - 可以继续编辑
        val newContent = "有效内容"
        coEvery { editBrainTagUseCase(testTag.id, newContent, testTag.type) } returns Result.success(Unit)
        viewModel.onEvent(BrainTagUiEvent.ConfirmEditTag(testTag.id, newContent, testTag.type))
        advanceUntilIdle()

        coVerify { editBrainTagUseCase(testTag.id, newContent, testTag.type) }
    }
}
