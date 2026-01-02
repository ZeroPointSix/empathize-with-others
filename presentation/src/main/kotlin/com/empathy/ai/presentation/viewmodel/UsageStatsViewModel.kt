package com.empathy.ai.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.empathy.ai.domain.model.UsageStatsPeriod
import com.empathy.ai.domain.usecase.CleanupApiUsageUseCase
import com.empathy.ai.domain.usecase.ExportApiUsageUseCase
import com.empathy.ai.domain.usecase.GetApiUsageStatsUseCase
import com.empathy.ai.presentation.ui.screen.aiconfig.UsageStatsTab
import com.empathy.ai.presentation.ui.screen.aiconfig.UsageStatsUiEvent
import com.empathy.ai.presentation.ui.screen.aiconfig.UsageStatsUiState
import com.empathy.ai.presentation.ui.screen.aiconfig.UsageTimeRange
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 用量统计页面ViewModel
 *
 * 职责：
 * 1. 管理用量统计页面的UI状态
 * 2. 处理用户交互事件
 * 3. 调用UseCase执行业务逻辑
 *
 * @see TD-00025 Phase 6: 用量统计实现
 */
@HiltViewModel
class UsageStatsViewModel @Inject constructor(
    private val getApiUsageStatsUseCase: GetApiUsageStatsUseCase,
    private val cleanupApiUsageUseCase: CleanupApiUsageUseCase,
    private val exportApiUsageUseCase: ExportApiUsageUseCase
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(UsageStatsUiState())
    val uiState: StateFlow<UsageStatsUiState> = _uiState.asStateFlow()

    init {
        loadStats()
    }

    /**
     * 统一的事件处理入口
     */
    fun onEvent(event: UsageStatsUiEvent) {
        when (event) {
            is UsageStatsUiEvent.LoadStats -> loadStats()
            is UsageStatsUiEvent.RefreshStats -> loadStats()
            is UsageStatsUiEvent.SwitchTab -> switchTab(event.tab)
            is UsageStatsUiEvent.SwitchTimeRange -> switchTimeRange(event.range)
            is UsageStatsUiEvent.ExportData -> exportData()
            is UsageStatsUiEvent.ShowClearConfirmDialog -> showClearConfirmDialog()
            is UsageStatsUiEvent.DismissClearConfirmDialog -> dismissClearConfirmDialog()
            is UsageStatsUiEvent.ConfirmClearHistory -> confirmClearHistory()
            is UsageStatsUiEvent.ClearError -> clearError()
            is UsageStatsUiEvent.ClearExportSuccess -> clearExportSuccess()
            is UsageStatsUiEvent.ClearClearSuccess -> clearClearSuccess()
        }
    }

    /**
     * 加载统计数据
     */
    private fun loadStats() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val timeRange = _uiState.value.timeRange
            val period = when (timeRange) {
                UsageTimeRange.TODAY -> UsageStatsPeriod.TODAY
                UsageTimeRange.THIS_WEEK -> UsageStatsPeriod.THIS_WEEK
                UsageTimeRange.THIS_MONTH -> UsageStatsPeriod.THIS_MONTH
                UsageTimeRange.ALL -> UsageStatsPeriod.ALL
            }

            val result = getApiUsageStatsUseCase(period)

            result.onSuccess { stats ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        stats = stats,
                        error = null
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = error.message ?: "加载统计数据失败"
                    )
                }
            }
        }
    }

    /**
     * 切换Tab
     */
    private fun switchTab(tab: UsageStatsTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    /**
     * 切换时间范围
     */
    private fun switchTimeRange(range: UsageTimeRange) {
        _uiState.update { it.copy(timeRange = range) }
        loadStats()
    }

    /**
     * 导出用量数据
     */
    private fun exportData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, error = null) }

            val result = exportApiUsageUseCase()

            result.onSuccess { exportPath ->
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        exportSuccess = true,
                        error = null
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        error = error.message ?: "导出数据失败"
                    )
                }
            }
        }
    }

    /**
     * 显示清除确认对话框
     */
    private fun showClearConfirmDialog() {
        _uiState.update { it.copy(showClearConfirmDialog = true) }
    }

    /**
     * 关闭清除确认对话框
     */
    private fun dismissClearConfirmDialog() {
        _uiState.update { it.copy(showClearConfirmDialog = false) }
    }

    /**
     * 确认清除历史数据
     */
    private fun confirmClearHistory() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    showClearConfirmDialog = false,
                    isClearing = true,
                    error = null
                )
            }

            // 清除90天前的数据
            val result = cleanupApiUsageUseCase(CLEANUP_DAYS_THRESHOLD)

            result.onSuccess { deletedCount ->
                _uiState.update {
                    it.copy(
                        isClearing = false,
                        clearSuccess = true,
                        error = null
                    )
                }
                // 重新加载统计数据
                loadStats()
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isClearing = false,
                        error = error.message ?: "清除数据失败"
                    )
                }
            }
        }
    }

    /**
     * 清除错误
     */
    private fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * 清除导出成功状态
     */
    private fun clearExportSuccess() {
        _uiState.update { it.copy(exportSuccess = false) }
    }

    /**
     * 清除清除成功状态
     */
    private fun clearClearSuccess() {
        _uiState.update { it.copy(clearSuccess = false) }
    }

    companion object {
        /** 清理数据的天数阈值 */
        private const val CLEANUP_DAYS_THRESHOLD = 90
    }
}
