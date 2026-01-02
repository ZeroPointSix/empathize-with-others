package com.empathy.ai.presentation.ui.screen.aiconfig

/**
 * 用量统计页面用户事件
 *
 * @see TD-00025 Phase 6: 用量统计实现
 */
sealed interface UsageStatsUiEvent {
    /**
     * 加载统计数据
     */
    data object LoadStats : UsageStatsUiEvent

    /**
     * 刷新统计数据
     */
    data object RefreshStats : UsageStatsUiEvent

    /**
     * 切换Tab
     */
    data class SwitchTab(val tab: UsageStatsTab) : UsageStatsUiEvent

    /**
     * 切换时间范围
     */
    data class SwitchTimeRange(val range: UsageTimeRange) : UsageStatsUiEvent

    /**
     * 导出用量数据
     */
    data object ExportData : UsageStatsUiEvent

    /**
     * 显示清除确认对话框
     */
    data object ShowClearConfirmDialog : UsageStatsUiEvent

    /**
     * 关闭清除确认对话框
     */
    data object DismissClearConfirmDialog : UsageStatsUiEvent

    /**
     * 确认清除历史数据
     */
    data object ConfirmClearHistory : UsageStatsUiEvent

    /**
     * 清除错误
     */
    data object ClearError : UsageStatsUiEvent

    /**
     * 清除导出成功状态
     */
    data object ClearExportSuccess : UsageStatsUiEvent

    /**
     * 清除清除成功状态
     */
    data object ClearClearSuccess : UsageStatsUiEvent
}
