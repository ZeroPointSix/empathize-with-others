package com.empathy.ai.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.empathy.ai.domain.model.ConflictResolution
import com.empathy.ai.domain.model.ConflictResult
import com.empathy.ai.domain.model.DateRange
import com.empathy.ai.domain.model.SummaryError
import com.empathy.ai.domain.model.SummaryTask
import com.empathy.ai.domain.usecase.ManualSummaryUseCase
import com.empathy.ai.domain.usecase.SummaryException
import com.empathy.ai.domain.util.DateRangeValidator
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
 * 手动总结ViewModel
 *
 * 管理手动触发AI总结功能的业务逻辑和UI状态
 */
@HiltViewModel
class ManualSummaryViewModel @Inject constructor(
    private val manualSummaryUseCase: ManualSummaryUseCase,
    private val conflictChecker: SummaryConflictChecker,
    private val dateRangeValidator: DateRangeValidator
) : ViewModel() {

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
        }
    }

    /**
     * 显示日期选择器
     */
    private fun showDatePicker(contactId: String) {
        _uiState.update {
            it.copy(
                contactId = contactId,
                showDatePicker = true,
                selectedDateRange = DateRange.lastSevenDays(),
                selectedQuickOption = QuickDateOption.LAST_7_DAYS,
                validationError = null
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
     */
    private fun confirmDateRange() {
        val state = _uiState.value
        val contactId = state.contactId ?: return
        val dateRange = state.selectedDateRange ?: return

        viewModelScope.launch {
            // 验证日期范围
            val validationResult = dateRangeValidator.validate(dateRange, contactId)
            if (validationResult.isFailure) {
                _uiState.update {
                    it.copy(validationError = "验证失败，请重试")
                }
                return@launch
            }

            when (val result = validationResult.getOrNull()) {
                is DateRangeValidator.ValidationResult.Invalid -> {
                    _uiState.update {
                        it.copy(validationError = result.message)
                    }
                    return@launch
                }
                is DateRangeValidator.ValidationResult.Warning -> {
                    _uiState.update {
                        it.copy(
                            showRangeWarning = true,
                            rangeWarningMessage = result.message
                        )
                    }
                    return@launch
                }
                else -> {}
            }

            // 检测冲突
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
     */
    private fun startSummary(
        contactId: String,
        dateRange: DateRange,
        conflictResolution: ConflictResolution?
    ) {
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

            val result = manualSummaryUseCase(
                contactId = contactId,
                dateRange = dateRange,
                conflictResolution = conflictResolution
            ) { progress, step ->
                _uiState.update {
                    it.copy(
                        task = it.task?.withProgress(progress, step)
                    )
                }
            }

            result.fold(
                onSuccess = { summaryResult ->
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
