package com.empathy.ai.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.empathy.ai.domain.model.ConflictResolution
import com.empathy.ai.domain.model.ConflictResult
import com.empathy.ai.domain.model.DateRange
import com.empathy.ai.domain.model.SummaryError
import com.empathy.ai.domain.model.SummaryTask
import com.empathy.ai.domain.repository.AiProviderRepository
import com.empathy.ai.domain.usecase.ManualSummaryUseCase
import com.empathy.ai.domain.usecase.SummaryException
import com.empathy.ai.domain.util.DateRangeValidator
import com.empathy.ai.domain.util.Logger
import com.empathy.ai.domain.util.SummaryConflictChecker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 手动总结功能 ViewModel
 *
 * ## 业务职责
 * 支持用户手动创建对话总结：
 * - 对话内容输入和编辑
 * - AI辅助总结生成
 * - 关键事件标记
 * - 情感标签标注
 * - 日期范围选择和验证
 * - 冲突检测和处理
 *
 * ## 关联文档
 * - PRD-00003: 联系人画像记忆系统需求
 * - BUG-00064: AI总结功能未生效修复
 *
 * ## 核心数据流
 * ```
 * SelectConversation → LoadHistory → UserEdit → [AIAssist] → Save → UpdateProfile
 * ```
 *
 * ## BUG-00064 修复内容
 * - 添加调试日志：追踪关键操作便于问题定位
 * - 前置检查：在showDatePicker时检查AI服务商配置，避免静默失败
 * - 友好提示：未配置AI服务商时显示明确的用户引导
 *
 * ## 关键业务概念
 * - **DateRange**: 日期范围选择，支持快捷选项和自定义范围
 * - **ConflictResult**: 重复总结检测结果
 * - **ConflictResolution**: 冲突处理方式（覆盖/跳过/取消）
 * - **SummaryTask**: 总结任务状态跟踪
 *
 * ## 设计决策
 * - **双模式**: 支持纯手动编辑和AI辅助两种模式
 * - **草稿保护**: 未保存退出时提示用户保存
 * - **冲突检测**: 自动检测已存在的总结，避免重复
 * - **进度展示**: 长时间AI生成过程显示进度条
 * - **前置检查**: 在功能入口处验证前置条件，提升用户体验
 *
 * ## 业务规则
 * - 总结必须关联具体联系人
 * - 敏感信息自动脱敏处理
 * - 保存后自动更新关系分数
 * - 日期范围有最大天数限制（防止Token超限）
 * - 未配置AI服务商时禁止发起总结请求
 *
 * @see com.empathy.ai.presentation.ui.screen.ManualSummaryScreen
 * @see BUG-00064-AI总结功能未生效-修复方案.md
 */
@HiltViewModel
class ManualSummaryViewModel @Inject constructor(
    private val manualSummaryUseCase: ManualSummaryUseCase,
    private val conflictChecker: SummaryConflictChecker,
    private val dateRangeValidator: DateRangeValidator,
    private val aiProviderRepository: AiProviderRepository,
    private val logger: Logger
) : ViewModel() {

    companion object {
        private const val TAG = "ManualSummaryViewModel"
    }

    private val _uiState = MutableStateFlow(ManualSummaryUiState())
    val uiState: StateFlow<ManualSummaryUiState> = _uiState.asStateFlow()

    private var currentJob: Job? = null

    /**
     * 处理UI事件
     */
    fun onEvent(event: ManualSummaryUiEvent) {
        when (event) {
            is ManualSummaryUiEvent.ShowDatePicker -> showDatePicker(event.contactId)
            is ManualSummaryUiEvent.SelectQuickOption -> selectQuickOption(event.option)
            is ManualSummaryUiEvent.SelectCustomRange -> selectCustomRange(event.start, event.end)
            is ManualSummaryUiEvent.ConfirmDateRange -> confirmDateRange()
            is ManualSummaryUiEvent.DismissDatePicker -> dismissDatePicker()
            is ManualSummaryUiEvent.ConfirmRangeWarning -> confirmRangeWarning()
            is ManualSummaryUiEvent.DismissRangeWarning -> dismissRangeWarning()
            is ManualSummaryUiEvent.SelectConflictResolution -> selectConflictResolution(event.resolution)
            is ManualSummaryUiEvent.ConfirmConflictResolution -> confirmConflictResolution()
            is ManualSummaryUiEvent.DismissConflictDialog -> dismissConflictDialog()
            is ManualSummaryUiEvent.CancelSummary -> cancelSummary()
            is ManualSummaryUiEvent.RetryFailed -> retryFailed()
            is ManualSummaryUiEvent.ViewResult -> viewResult()
            is ManualSummaryUiEvent.DismissResult -> dismissResult()
            is ManualSummaryUiEvent.DismissSummaryDetail -> dismissSummaryDetail()
            is ManualSummaryUiEvent.DismissError -> dismissError()
            is ManualSummaryUiEvent.Reset -> reset()
            is ManualSummaryUiEvent.ClearNavigation -> clearNavigation()
            is ManualSummaryUiEvent.DismissNoProviderWarning -> dismissNoProviderWarning()
        }
    }

    /**
     * 显示日期选择器
     *
     * BUG-00064修复：添加调试日志和AI服务商检查
     */
    private fun showDatePicker(contactId: String) {
        logger.d(TAG, "showDatePicker called with contactId: $contactId")

        viewModelScope.launch {
            // 检查AI服务商配置
            val provider = aiProviderRepository.getDefaultProvider().getOrNull()
            if (provider == null) {
                logger.w(TAG, "No default AI provider configured")
                _uiState.update {
                    it.copy(
                        showNoProviderWarning = true,
                        noProviderWarningMessage = "请先在设置中配置AI服务商"
                    )
                }
                return@launch
            }

            logger.d(TAG, "AI provider found: ${provider.name}")

            _uiState.update {
                it.copy(
                    contactId = contactId,
                    showDatePicker = true,
                    selectedDateRange = DateRange.lastSevenDays(),
                    selectedQuickOption = QuickDateOption.LAST_7_DAYS,
                    validationError = null
                )
            }

            logger.d(TAG, "showDatePicker state updated, showDatePicker=${_uiState.value.showDatePicker}")
        }
    }

    /**
     * 关闭无AI服务商警告
     */
    private fun dismissNoProviderWarning() {
        logger.d(TAG, "dismissNoProviderWarning called")
        _uiState.update {
            it.copy(
                showNoProviderWarning = false,
                noProviderWarningMessage = null
            )
        }
    }

    /**
     * 选择快捷选项
     */
    private fun selectQuickOption(option: QuickDateOption) {
        val dateRange = when (option) {
            QuickDateOption.LAST_7_DAYS -> DateRange.lastSevenDays()
            QuickDateOption.THIS_MONTH -> DateRange.thisMonth()
            QuickDateOption.LAST_MONTH -> DateRange.lastMonth()
            QuickDateOption.LAST_30_DAYS -> DateRange.lastThirtyDays()
            QuickDateOption.MISSING_DATES -> DateRange.lastThirtyDays() // 默认30天
        }
        _uiState.update {
            it.copy(
                selectedQuickOption = option,
                selectedDateRange = dateRange,
                validationError = null
            )
        }
    }

    /**
     * 选择自定义范围
     */
    private fun selectCustomRange(start: String, end: String) {
        _uiState.update {
            it.copy(
                selectedQuickOption = null,
                selectedDateRange = DateRange(start, end),
                validationError = null
            )
        }
    }

    /**
     * 确认日期范围
     *
     * BUG-00064修复：添加调试日志
     */
    private fun confirmDateRange() {
        val state = _uiState.value
        val contactId = state.contactId ?: run {
            logger.e(TAG, "confirmDateRange: contactId is null")
            return
        }
        val dateRange = state.selectedDateRange ?: run {
            logger.e(TAG, "confirmDateRange: dateRange is null")
            return
        }

        logger.d(TAG, "confirmDateRange: contactId=$contactId, dateRange=${dateRange.getDisplayText()}")

        viewModelScope.launch {
            // 验证日期范围
            logger.d(TAG, "Validating date range...")
            val validationResult = dateRangeValidator.validate(dateRange, contactId)
            if (validationResult.isFailure) {
                logger.e(TAG, "Date range validation failed: ${validationResult.exceptionOrNull()?.message}")
                _uiState.update {
                    it.copy(validationError = "验证失败，请重试")
                }
                return@launch
            }

            when (val result = validationResult.getOrNull()) {
                is DateRangeValidator.ValidationResult.Invalid -> {
                    logger.w(TAG, "Date range invalid: ${result.message}")
                    _uiState.update {
                        it.copy(validationError = result.message)
                    }
                    return@launch
                }
                is DateRangeValidator.ValidationResult.Warning -> {
                    logger.d(TAG, "Date range warning: ${result.message}")
                    _uiState.update {
                        it.copy(
                            showRangeWarning = true,
                            rangeWarningMessage = result.message
                        )
                    }
                    return@launch
                }
                else -> {
                    logger.d(TAG, "Date range validation passed")
                }
            }

            // 检测冲突
            logger.d(TAG, "Checking conflicts...")
            checkConflictAndProceed(contactId, dateRange)
        }
    }


    /**
     * 确认范围警告，继续执行
     */
    private fun confirmRangeWarning() {
        val state = _uiState.value
        val contactId = state.contactId ?: return
        val dateRange = state.selectedDateRange ?: return

        _uiState.update {
            it.copy(
                showRangeWarning = false,
                rangeWarningMessage = null
            )
        }

        viewModelScope.launch {
            checkConflictAndProceed(contactId, dateRange)
        }
    }

    /**
     * 取消范围警告
     */
    private fun dismissRangeWarning() {
        _uiState.update {
            it.copy(
                showRangeWarning = false,
                rangeWarningMessage = null
            )
        }
    }

    /**
     * 检测冲突并继续
     */
    private suspend fun checkConflictAndProceed(contactId: String, dateRange: DateRange) {
        val conflictResult = conflictChecker.checkConflict(contactId, dateRange)
        when (val conflict = conflictResult.getOrNull()) {
            is ConflictResult.HasConflict -> {
                _uiState.update {
                    it.copy(
                        showDatePicker = false,
                        showConflictDialog = true,
                        conflictResult = conflict
                    )
                }
            }
            else -> {
                // 无冲突，直接开始总结
                _uiState.update { it.copy(showDatePicker = false) }
                startSummary(contactId, dateRange, null)
            }
        }
    }

    /**
     * 选择冲突处理方式
     */
    private fun selectConflictResolution(resolution: ConflictResolution) {
        _uiState.update {
            it.copy(selectedConflictResolution = resolution)
        }
    }

    /**
     * 确认冲突处理
     */
    private fun confirmConflictResolution() {
        val state = _uiState.value
        val contactId = state.contactId ?: return
        val dateRange = state.selectedDateRange ?: return
        val resolution = state.selectedConflictResolution ?: return

        if (resolution == ConflictResolution.CANCEL) {
            dismissConflictDialog()
            return
        }

        _uiState.update { it.copy(showConflictDialog = false) }
        startSummary(contactId, dateRange, resolution)
    }

    /**
     * 开始总结
     *
     * BUG-00064修复：添加调试日志
     */
    private fun startSummary(
        contactId: String,
        dateRange: DateRange,
        conflictResolution: ConflictResolution?
    ) {
        logger.d(TAG, "startSummary: contactId=$contactId, dateRange=${dateRange.getDisplayText()}, resolution=$conflictResolution")

        currentJob?.cancel()
        currentJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    showProgressDialog = true,
                    task = SummaryTask(
                        contactId = contactId,
                        startDate = dateRange.startDate,
                        endDate = dateRange.endDate,
                        conflictResolution = conflictResolution
                    ).markStarted()
                )
            }

            logger.d(TAG, "Calling manualSummaryUseCase...")
            val result = manualSummaryUseCase(
                contactId = contactId,
                dateRange = dateRange,
                conflictResolution = conflictResolution
            ) { progress, step ->
                logger.d(TAG, "Progress: $progress, Step: $step")
                _uiState.update {
                    it.copy(
                        task = it.task?.withProgress(progress, step)
                    )
                }
            }

            result.fold(
                onSuccess = { summaryResult ->
                    logger.d(TAG, "Summary completed successfully: ${summaryResult.conversationCount} conversations")
                    _uiState.update {
                        it.copy(
                            showProgressDialog = false,
                            showResultDialog = true,
                            task = it.task?.markSuccess(),
                            summaryResult = summaryResult
                        )
                    }
                },
                onFailure = { error ->
                    logger.e(TAG, "Summary failed: ${error.message}", error)
                    val summaryError = when (error) {
                        is SummaryException -> error.error
                        else -> SummaryError.Unknown(error.message ?: "未知错误")
                    }
                    _uiState.update {
                        it.copy(
                            showProgressDialog = false,
                            showErrorDialog = true,
                            task = it.task?.markFailed(summaryError)
                        )
                    }
                }
            )
        }
    }

    /**
     * 取消总结
     */
    private fun cancelSummary() {
        currentJob?.cancel()
        _uiState.update {
            it.copy(
                showProgressDialog = false,
                task = it.task?.markCancelled()
            )
        }
    }

    /**
     * 重试失败的任务
     */
    private fun retryFailed() {
        val state = _uiState.value
        val task = state.task ?: return

        _uiState.update { it.copy(showErrorDialog = false) }
        startSummary(
            task.contactId,
            DateRange(task.startDate, task.endDate),
            task.conflictResolution
        )
    }

    /**
     * 查看结果
     *
     * 关闭结果统计对话框，显示总结详情对话框
     */
    private fun viewResult() {
        _uiState.update {
            it.copy(
                showResultDialog = false,
                showSummaryDetailDialog = true
            )
        }
    }

    /**
     * 关闭日期选择器
     */
    private fun dismissDatePicker() {
        _uiState.update { it.copy(showDatePicker = false) }
    }

    /**
     * 关闭冲突对话框
     */
    private fun dismissConflictDialog() {
        _uiState.update { it.copy(showConflictDialog = false) }
    }

    /**
     * 关闭结果对话框
     */
    private fun dismissResult() {
        _uiState.update { it.copy(showResultDialog = false) }
    }

    /**
     * 关闭总结详情对话框
     */
    private fun dismissSummaryDetail() {
        _uiState.update {
            it.copy(
                showSummaryDetailDialog = false,
                navigateToTimeline = true
            )
        }
    }

    /**
     * 关闭错误对话框
     */
    private fun dismissError() {
        _uiState.update { it.copy(showErrorDialog = false) }
    }

    /**
     * 重置状态
     */
    private fun reset() {
        currentJob?.cancel()
        _uiState.value = ManualSummaryUiState()
    }

    /**
     * 清除导航状态
     */
    private fun clearNavigation() {
        _uiState.update { it.copy(navigateToTimeline = false) }
    }

    override fun onCleared() {
        super.onCleared()
        currentJob?.cancel()
    }
}
