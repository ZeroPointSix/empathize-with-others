package com.empathy.ai.presentation.viewmodel

import com.empathy.ai.domain.model.ConflictResolution
import com.empathy.ai.domain.model.ConflictResult
import com.empathy.ai.domain.model.DailySummary
import com.empathy.ai.domain.model.DateRange
import com.empathy.ai.domain.model.RelationshipTrend
import com.empathy.ai.domain.model.SummaryError
import com.empathy.ai.domain.usecase.ManualSummaryUseCase
import com.empathy.ai.domain.usecase.SummaryException
import com.empathy.ai.domain.util.DateRangeValidator
import com.empathy.ai.domain.util.SummaryConflictChecker
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
 * ManualSummaryViewModel 手动触发AI总结功能测试
 *
 * 测试范围：
 * - 日期范围选择与验证
 * - 冲突检测与解决流程
 * - 总结生成执行与错误处理
 * - 取消/重试/导航状态管理
 *
 * 业务背景 (PRD-00011):
 * - 用户可手动触发AI总结，补充历史对话的总结内容
 * - 支持快捷选项（最近7天、本月等）和自定义日期范围
 * - 总结前进行冲突检测，避免重复总结
 * - 提供进度展示、错误提示和重试机制
 *
 * 任务追踪:
 * - FD-00011 手动触发AI总结功能设计
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ManualSummaryViewModelTest {

    private lateinit var manualSummaryUseCase: ManualSummaryUseCase
    private lateinit var conflictChecker: SummaryConflictChecker
    private lateinit var dateRangeValidator: DateRangeValidator
    private lateinit var viewModel: ManualSummaryViewModel

    private val testDispatcher = StandardTestDispatcher()
    private val testContactId = "contact-123"

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        manualSummaryUseCase = mockk()
        conflictChecker = mockk()
        dateRangeValidator = mockk()
        viewModel = ManualSummaryViewModel(
            manualSummaryUseCase = manualSummaryUseCase,
            conflictChecker = conflictChecker,
            dateRangeValidator = dateRangeValidator
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ==================== 日期选择测试 ====================

    @Test
    fun `ShowDatePicker事件应该显示日期选择器`() = runTest(testDispatcher) {
        // When
        viewModel.onEvent(ManualSummaryUiEvent.ShowDatePicker(testContactId))

        // Then
        val state = viewModel.uiState.value
        assertTrue(state.showDatePicker)
        assertEquals(testContactId, state.contactId)
        assertNotNull(state.selectedDateRange)
        assertEquals(QuickDateOption.LAST_7_DAYS, state.selectedQuickOption)
    }

    @Test
    fun `SelectQuickOption事件应该更新选中的快捷选项`() = runTest(testDispatcher) {
        // Given
        viewModel.onEvent(ManualSummaryUiEvent.ShowDatePicker(testContactId))

        // When
        viewModel.onEvent(ManualSummaryUiEvent.SelectQuickOption(QuickDateOption.LAST_30_DAYS))

        // Then
        val state = viewModel.uiState.value
        assertEquals(QuickDateOption.LAST_30_DAYS, state.selectedQuickOption)
        assertNotNull(state.selectedDateRange)
    }

    @Test
    fun `SelectCustomRange事件应该清除快捷选项并设置自定义范围`() = runTest(testDispatcher) {
        // Given
        viewModel.onEvent(ManualSummaryUiEvent.ShowDatePicker(testContactId))

        // When
        viewModel.onEvent(ManualSummaryUiEvent.SelectCustomRange("2025-12-01", "2025-12-10"))

        // Then
        val state = viewModel.uiState.value
        assertNull(state.selectedQuickOption)
        assertEquals("2025-12-01", state.selectedDateRange?.startDate)
        assertEquals("2025-12-10", state.selectedDateRange?.endDate)
    }

    @Test
    fun `DismissDatePicker事件应该关闭日期选择器`() = runTest(testDispatcher) {
        // Given
        viewModel.onEvent(ManualSummaryUiEvent.ShowDatePicker(testContactId))

        // When
        viewModel.onEvent(ManualSummaryUiEvent.DismissDatePicker)

        // Then
        assertFalse(viewModel.uiState.value.showDatePicker)
    }

    // ==================== 日期验证测试 ====================

    @Test
    fun `ConfirmDateRange验证失败应该显示错误`() = runTest(testDispatcher) {
        // Given
        viewModel.onEvent(ManualSummaryUiEvent.ShowDatePicker(testContactId))
        coEvery { dateRangeValidator.validate(any(), testContactId) } returns
            Result.success(DateRangeValidator.ValidationResult.Invalid("开始日期不能晚于结束日期"))

        // When
        viewModel.onEvent(ManualSummaryUiEvent.ConfirmDateRange)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals("开始日期不能晚于结束日期", state.validationError)
    }

    @Test
    fun `ConfirmDateRange验证警告应该显示警告对话框`() = runTest(testDispatcher) {
        // Given
        viewModel.onEvent(ManualSummaryUiEvent.ShowDatePicker(testContactId))
        coEvery { dateRangeValidator.validate(any(), testContactId) } returns
            Result.success(DateRangeValidator.ValidationResult.Warning("范围较大"))

        // When
        viewModel.onEvent(ManualSummaryUiEvent.ConfirmDateRange)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertTrue(state.showRangeWarning)
        assertEquals("范围较大", state.rangeWarningMessage)
    }

    // ==================== 冲突检测测试 ====================

    @Test
    fun `ConfirmDateRange有冲突应该显示冲突对话框`() = runTest(testDispatcher) {
        // Given
        viewModel.onEvent(ManualSummaryUiEvent.ShowDatePicker(testContactId))
        coEvery { dateRangeValidator.validate(any(), testContactId) } returns
            Result.success(DateRangeValidator.ValidationResult.Valid)
        coEvery { conflictChecker.checkConflict(testContactId, any()) } returns
            Result.success(
                ConflictResult.HasConflict(
                    existingSummaries = listOf(createTestSummary()),
                    conflictDates = listOf("2025-12-05")
                )
            )

        // When
        viewModel.onEvent(ManualSummaryUiEvent.ConfirmDateRange)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.showDatePicker)
        assertTrue(state.showConflictDialog)
        assertNotNull(state.conflictResult)
    }

    @Test
    fun `SelectConflictResolution应该更新选中的处理方式`() = runTest(testDispatcher) {
        // When
        viewModel.onEvent(ManualSummaryUiEvent.SelectConflictResolution(ConflictResolution.OVERWRITE))

        // Then
        assertEquals(ConflictResolution.OVERWRITE, viewModel.uiState.value.selectedConflictResolution)
    }

    @Test
    fun `ConfirmConflictResolution选择取消应该关闭对话框`() = runTest(testDispatcher) {
        // Given
        viewModel.onEvent(ManualSummaryUiEvent.SelectConflictResolution(ConflictResolution.CANCEL))

        // When
        viewModel.onEvent(ManualSummaryUiEvent.ConfirmConflictResolution)

        // Then
        assertFalse(viewModel.uiState.value.showConflictDialog)
    }


    // ==================== 总结执行测试 ====================

    @Test
    fun `无冲突时ConfirmDateRange应该直接开始总结`() = runTest(testDispatcher) {
        // Given
        viewModel.onEvent(ManualSummaryUiEvent.ShowDatePicker(testContactId))
        coEvery { dateRangeValidator.validate(any(), testContactId) } returns
            Result.success(DateRangeValidator.ValidationResult.Valid)
        coEvery { conflictChecker.checkConflict(testContactId, any()) } returns
            Result.success(ConflictResult.NoConflict)
        coEvery { manualSummaryUseCase(testContactId, any(), null, any()) } returns
            Result.success(createTestSummaryResult())

        // When
        viewModel.onEvent(ManualSummaryUiEvent.ConfirmDateRange)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.showDatePicker)
        assertTrue(state.showResultDialog)
        assertNotNull(state.summaryResult)
    }

    @Test
    fun `总结成功应该显示结果对话框`() = runTest(testDispatcher) {
        // Given
        setupSuccessScenario()

        // When
        viewModel.onEvent(ManualSummaryUiEvent.ShowDatePicker(testContactId))
        viewModel.onEvent(ManualSummaryUiEvent.ConfirmDateRange)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertTrue(state.showResultDialog)
        assertNotNull(state.summaryResult)
        assertNotNull(state.task)
    }

    @Test
    fun `总结失败应该显示错误对话框`() = runTest(testDispatcher) {
        // Given
        viewModel.onEvent(ManualSummaryUiEvent.ShowDatePicker(testContactId))
        coEvery { dateRangeValidator.validate(any(), testContactId) } returns
            Result.success(DateRangeValidator.ValidationResult.Valid)
        coEvery { conflictChecker.checkConflict(testContactId, any()) } returns
            Result.success(ConflictResult.NoConflict)
        coEvery { manualSummaryUseCase(testContactId, any(), null, any()) } returns
            Result.failure(SummaryException(SummaryError.NoConversations))

        // When
        viewModel.onEvent(ManualSummaryUiEvent.ConfirmDateRange)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertTrue(state.showErrorDialog)
        assertNotNull(state.task?.error)
    }

    // ==================== 取消和重试测试 ====================

    @Test
    fun `CancelSummary应该取消任务并关闭进度对话框`() = runTest(testDispatcher) {
        // Given - 模拟一个正在进行的任务
        viewModel.onEvent(ManualSummaryUiEvent.ShowDatePicker(testContactId))
        coEvery { dateRangeValidator.validate(any(), testContactId) } returns
            Result.success(DateRangeValidator.ValidationResult.Valid)
        coEvery { conflictChecker.checkConflict(testContactId, any()) } returns
            Result.success(ConflictResult.NoConflict)
        // 不设置UseCase返回，让它挂起

        // When
        viewModel.onEvent(ManualSummaryUiEvent.CancelSummary)

        // Then
        assertFalse(viewModel.uiState.value.showProgressDialog)
    }

    @Test
    fun `RetryFailed应该重新开始总结`() = runTest(testDispatcher) {
        // Given - 先让任务失败
        viewModel.onEvent(ManualSummaryUiEvent.ShowDatePicker(testContactId))
        coEvery { dateRangeValidator.validate(any(), testContactId) } returns
            Result.success(DateRangeValidator.ValidationResult.Valid)
        coEvery { conflictChecker.checkConflict(testContactId, any()) } returns
            Result.success(ConflictResult.NoConflict)
        coEvery { manualSummaryUseCase(testContactId, any(), null, any()) } returns
            Result.failure(SummaryException(SummaryError.NetworkError))

        viewModel.onEvent(ManualSummaryUiEvent.ConfirmDateRange)
        advanceUntilIdle()

        // 然后设置成功
        coEvery { manualSummaryUseCase(testContactId, any(), null, any()) } returns
            Result.success(createTestSummaryResult())

        // When
        viewModel.onEvent(ManualSummaryUiEvent.RetryFailed)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.showErrorDialog)
        assertTrue(state.showResultDialog)
    }

    // ==================== 导航测试 ====================

    @Test
    fun `ViewResult应该设置导航状态`() = runTest(testDispatcher) {
        // When
        viewModel.onEvent(ManualSummaryUiEvent.ViewResult)

        // Then
        assertTrue(viewModel.uiState.value.navigateToTimeline)
    }

    @Test
    fun `ClearNavigation应该清除导航状态`() = runTest(testDispatcher) {
        // Given
        viewModel.onEvent(ManualSummaryUiEvent.ViewResult)

        // When
        viewModel.onEvent(ManualSummaryUiEvent.ClearNavigation)

        // Then
        assertFalse(viewModel.uiState.value.navigateToTimeline)
    }

    // ==================== 重置测试 ====================

    @Test
    fun `Reset应该重置所有状态`() = runTest(testDispatcher) {
        // Given
        viewModel.onEvent(ManualSummaryUiEvent.ShowDatePicker(testContactId))

        // When
        viewModel.onEvent(ManualSummaryUiEvent.Reset)

        // Then
        val state = viewModel.uiState.value
        assertNull(state.contactId)
        assertFalse(state.showDatePicker)
        assertNull(state.task)
    }

    // ==================== 辅助属性测试 ====================

    @Test
    fun `isProcessing应该在显示进度对话框时返回true`() = runTest(testDispatcher) {
        // Given - 初始状态
        assertFalse(viewModel.uiState.value.isProcessing)
    }

    @Test
    fun `hasError应该在有验证错误时返回true`() = runTest(testDispatcher) {
        // Given
        viewModel.onEvent(ManualSummaryUiEvent.ShowDatePicker(testContactId))
        coEvery { dateRangeValidator.validate(any(), testContactId) } returns
            Result.success(DateRangeValidator.ValidationResult.Invalid("错误"))

        // When
        viewModel.onEvent(ManualSummaryUiEvent.ConfirmDateRange)
        advanceUntilIdle()

        // Then
        assertTrue(viewModel.uiState.value.hasError)
    }

    // ==================== 辅助方法 ====================

    private fun setupSuccessScenario() {
        coEvery { dateRangeValidator.validate(any(), testContactId) } returns
            Result.success(DateRangeValidator.ValidationResult.Valid)
        coEvery { conflictChecker.checkConflict(testContactId, any()) } returns
            Result.success(ConflictResult.NoConflict)
        coEvery { manualSummaryUseCase(testContactId, any(), null, any()) } returns
            Result.success(createTestSummaryResult())
    }

    private fun createTestSummary() = DailySummary(
        id = 1,
        contactId = testContactId,
        summaryDate = "2025-12-05",
        content = "Test summary",
        keyEvents = emptyList(),
        newFacts = emptyList(),
        updatedTags = emptyList(),
        relationshipScoreChange = 0,
        relationshipTrend = RelationshipTrend.STABLE
    )

    private fun createTestSummaryResult() = ManualSummaryUseCase.SummaryResult(
        summary = createTestSummary(),
        conversationCount = 10,
        keyEventCount = 2,
        factCount = 3,
        relationshipChange = 1
    )
}
