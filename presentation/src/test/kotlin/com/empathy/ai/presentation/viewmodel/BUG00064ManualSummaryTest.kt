package com.empathy.ai.presentation.viewmodel

import com.empathy.ai.domain.model.AiProvider
import com.empathy.ai.domain.model.ConflictResult
import com.empathy.ai.domain.model.DailySummary
import com.empathy.ai.domain.model.RelationshipTrend
import com.empathy.ai.domain.repository.AiProviderRepository
import com.empathy.ai.domain.usecase.ManualSummaryUseCase
import com.empathy.ai.domain.util.DateRangeValidator
import com.empathy.ai.domain.util.Logger
import com.empathy.ai.domain.util.SummaryConflictChecker
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * BUG-00064 回归测试
 *
 * 验证 AI 总结功能的核心流程是否正常工作
 *
 * 问题描述：AI总结功能未生效，点击FAB按钮后无反应
 * 根本原因：
 * 1. 缺少调试日志，无法追踪问题
 * 2. 未检查AI服务商配置
 * 3. 错误处理静默，用户看不到错误信息
 *
 * 修复方案：
 * 1. 添加调试日志
 * 2. 在showDatePicker前检查AI服务商配置
 * 3. 显示友好的错误提示
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BUG00064ManualSummaryTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: ManualSummaryViewModel
    private lateinit var manualSummaryUseCase: ManualSummaryUseCase
    private lateinit var conflictChecker: SummaryConflictChecker
    private lateinit var dateRangeValidator: DateRangeValidator
    private lateinit var aiProviderRepository: AiProviderRepository
    private lateinit var logger: Logger

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        manualSummaryUseCase = mockk()
        conflictChecker = mockk()
        dateRangeValidator = mockk()
        aiProviderRepository = mockk()
        logger = mockk(relaxed = true)

        viewModel = ManualSummaryViewModel(
            manualSummaryUseCase = manualSummaryUseCase,
            conflictChecker = conflictChecker,
            dateRangeValidator = dateRangeValidator,
            aiProviderRepository = aiProviderRepository,
            logger = logger
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * BUG-00064 核心场景：点击FAB按钮后应该显示日期选择器
     *
     * 前置条件：用户已配置AI服务商
     * 操作：点击FAB按钮
     * 预期结果：日期选择器显示
     */
    @Test
    fun `BUG00064_点击FAB按钮_配置了AI服务商_应该显示日期选择器`() = runTest {
        // Given: 用户已配置AI服务商
        val mockProvider = mockk<AiProvider>()
        every { mockProvider.name } returns "DeepSeek"
        coEvery { aiProviderRepository.getDefaultProvider() } returns Result.success(mockProvider)

        // When: 用户点击FAB按钮
        viewModel.onEvent(ManualSummaryUiEvent.ShowDatePicker("contact_123"))
        advanceUntilIdle()

        // Then: 日期选择器应该显示
        val state = viewModel.uiState.value
        assertTrue("日期选择器应该显示", state.showDatePicker)
        assertEquals("联系人ID应该正确", "contact_123", state.contactId)
        assertNotNull("应该有默认日期范围", state.selectedDateRange)
        assertEquals("默认快捷选项应该是最近7天", QuickDateOption.LAST_7_DAYS, state.selectedQuickOption)
    }

    /**
     * BUG-00064 核心场景：未配置AI服务商时应该显示警告
     *
     * 前置条件：用户未配置AI服务商
     * 操作：点击FAB按钮
     * 预期结果：显示警告对话框，而不是日期选择器
     */
    @Test
    fun `BUG00064_点击FAB按钮_未配置AI服务商_应该显示警告`() = runTest {
        // Given: 用户未配置AI服务商
        coEvery { aiProviderRepository.getDefaultProvider() } returns Result.success(null)

        // When: 用户点击FAB按钮
        viewModel.onEvent(ManualSummaryUiEvent.ShowDatePicker("contact_123"))
        advanceUntilIdle()

        // Then: 应该显示警告，而不是日期选择器
        val state = viewModel.uiState.value
        assertTrue("应该显示无服务商警告", state.showNoProviderWarning)
        assertFalse("日期选择器不应该显示", state.showDatePicker)
        assertNotNull("警告信息应该有值", state.noProviderWarningMessage)
    }

    /**
     * BUG-00064 核心场景：关闭无服务商警告
     *
     * 前置条件：无服务商警告正在显示
     * 操作：点击"知道了"按钮
     * 预期结果：警告对话框关闭
     */
    @Test
    fun `BUG00064_关闭无服务商警告_应该正确清除状态`() = runTest {
        // Given: 无服务商警告正在显示
        coEvery { aiProviderRepository.getDefaultProvider() } returns Result.success(null)
        viewModel.onEvent(ManualSummaryUiEvent.ShowDatePicker("contact_123"))
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.showNoProviderWarning)

        // When: 点击"知道了"按钮
        viewModel.onEvent(ManualSummaryUiEvent.DismissNoProviderWarning)
        advanceUntilIdle()

        // Then: 警告对话框应该关闭
        val state = viewModel.uiState.value
        assertFalse("警告应该关闭", state.showNoProviderWarning)
        assertEquals("警告信息应该清空", null, state.noProviderWarningMessage)
    }

    /**
     * BUG-00064 核心场景：完整的总结流程
     *
     * 前置条件：配置了AI服务商，有对话记录
     * 操作：点击FAB → 确认日期范围 → 等待总结完成
     * 预期结果：显示总结结果
     */
    @Test
    fun `BUG00064_完整总结流程_应该成功生成总结`() = runTest {
        // Given: 配置所有必要的mock
        val mockProvider = mockk<AiProvider>()
        every { mockProvider.name } returns "DeepSeek"
        coEvery { aiProviderRepository.getDefaultProvider() } returns Result.success(mockProvider)

        coEvery {
            dateRangeValidator.validate(any(), any())
        } returns Result.success(DateRangeValidator.ValidationResult.Valid)

        coEvery {
            conflictChecker.checkConflict(any(), any())
        } returns Result.success(ConflictResult.NoConflict)

        val mockSummary = DailySummary(
            id = 1,
            contactId = "contact_123",
            summaryDate = "2026-01-10",
            content = "测试总结内容",
            keyEvents = emptyList(),
            newFacts = emptyList(),
            updatedTags = emptyList(),
            relationshipScoreChange = 5,
            relationshipTrend = RelationshipTrend.IMPROVING
        )
        val mockResult = ManualSummaryUseCase.SummaryResult(
            summary = mockSummary,
            conversationCount = 10,
            keyEventCount = 2,
            factCount = 3,
            relationshipChange = 5
        )
        coEvery {
            manualSummaryUseCase(any(), any(), any(), any())
        } returns Result.success(mockResult)

        // When: 执行完整流程
        // Step 1: 点击FAB
        viewModel.onEvent(ManualSummaryUiEvent.ShowDatePicker("contact_123"))
        advanceUntilIdle()

        // Step 2: 确认日期范围
        viewModel.onEvent(ManualSummaryUiEvent.ConfirmDateRange)
        advanceUntilIdle()

        // Then: 应该成功显示结果
        val state = viewModel.uiState.value
        assertTrue("应该显示结果对话框", state.showResultDialog)
        assertNotNull("应该有总结结果", state.summaryResult)
        assertEquals("对话数量应该正确", 10, state.summaryResult?.conversationCount)

        // 验证UseCase被调用
        coVerify { manualSummaryUseCase(any(), any(), any(), any()) }
    }

    /**
     * BUG-00064 核心场景：日志输出验证
     *
     * 验证关键操作是否输出日志，便于调试
     */
    @Test
    fun `BUG00064_关键操作_应该输出日志`() = runTest {
        // Given
        val mockProvider = mockk<AiProvider>()
        every { mockProvider.name } returns "DeepSeek"
        coEvery { aiProviderRepository.getDefaultProvider() } returns Result.success(mockProvider)

        // When
        viewModel.onEvent(ManualSummaryUiEvent.ShowDatePicker("contact_123"))
        advanceUntilIdle()

        // Then: 验证日志被调用
        coVerify { logger.d(any(), match { it.contains("showDatePicker") }) }
    }

    /**
     * BUG-00064 场景：AI服务商获取失败
     *
     * 前置条件：获取AI服务商时发生错误
     * 操作：点击FAB按钮
     * 预期结果：显示警告对话框
     */
    @Test
    fun `BUG00064_AI服务商获取失败_应该显示警告`() = runTest {
        // Given: 获取AI服务商失败
        coEvery { aiProviderRepository.getDefaultProvider() } returns Result.failure(Exception("Network error"))

        // When: 用户点击FAB按钮
        viewModel.onEvent(ManualSummaryUiEvent.ShowDatePicker("contact_123"))
        advanceUntilIdle()

        // Then: 应该显示警告（因为getOrNull()返回null）
        val state = viewModel.uiState.value
        assertTrue("应该显示无服务商警告", state.showNoProviderWarning)
        assertFalse("日期选择器不应该显示", state.showDatePicker)
    }
}
