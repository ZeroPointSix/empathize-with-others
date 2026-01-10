package com.empathy.ai.presentation.viewmodel

import com.empathy.ai.domain.model.ConflictResolution

/**
 * 手动总结UI事件
 *
 * 定义用户在手动总结功能中可能触发的所有事件
 *
 * ## 关联文档
 * - TDD-00011: 手动触发AI总结功能技术设计
 * - BUG-00064: AI总结功能未生效修复
 *
 * ## 事件分类
 * - **日期选择事件**: ShowDatePicker, ConfirmDateRange, DismissDatePicker
 * - **冲突处理事件**: SelectConflictResolution, ConfirmConflictResolution
 * - **进度控制事件**: CancelSummary, RetryFailed
 * - **结果处理事件**: ViewResult, DismissResult, DismissSummaryDetail
 * - **通用事件**: Reset, ClearNavigation
 * - **前置检查事件**: DismissNoProviderWarning (BUG-00064新增)
 *
 * ## BUG-00064 事件变更
 * 新增 `DismissNoProviderWarning` 事件，用于关闭AI服务商未配置的警告对话框
 * 这是UI状态 `showNoProviderWarning` 的配套事件
 *
 * ## 设计决策
 * - **密封类**: 使用 sealed class 保证事件类型安全
 * - **分区组织**: 按功能模块分区，便于理解和扩展
 * - **数据类 vs 对象**: 带参数的事件使用 data class，纯操作使用 data object
 *
 * ## 事件流
 * ```
 * ShowDatePicker → ConfirmDateRange → [冲突检测] → startSummary → [ViewResult|DismissResult]
 * ```
 *
 * @see ManualSummaryUiState
 * @see TDD-00011-手动触发AI总结功能技术设计.md
 * @see BUG-00064-AI总结功能未生效-修复方案.md
 */
sealed class ManualSummaryUiEvent {

    // ==================== 日期选择事件 ====================

    /**
     * 显示日期选择器
     *
     * @param contactId 联系人ID
     */
    data class ShowDatePicker(val contactId: String) : ManualSummaryUiEvent()

    /**
     * 选择快捷选项
     *
     * @param option 快捷选项
     */
    data class SelectQuickOption(val option: QuickDateOption) : ManualSummaryUiEvent()

    /**
     * 选择自定义日期范围
     *
     * @param start 开始日期
     * @param end 结束日期
     */
    data class SelectCustomRange(val start: String, val end: String) : ManualSummaryUiEvent()

    /**
     * 确认日期范围
     */
    data object ConfirmDateRange : ManualSummaryUiEvent()

    /**
     * 关闭日期选择器
     */
    data object DismissDatePicker : ManualSummaryUiEvent()

    /**
     * 确认范围警告
     */
    data object ConfirmRangeWarning : ManualSummaryUiEvent()

    /**
     * 取消范围警告
     */
    data object DismissRangeWarning : ManualSummaryUiEvent()

    // ==================== 冲突处理事件 ====================

    /**
     * 选择冲突处理方式
     *
     * @param resolution 处理方式
     */
    data class SelectConflictResolution(val resolution: ConflictResolution) : ManualSummaryUiEvent()

    /**
     * 确认冲突处理
     */
    data object ConfirmConflictResolution : ManualSummaryUiEvent()

    /**
     * 关闭冲突对话框
     */
    data object DismissConflictDialog : ManualSummaryUiEvent()

    // ==================== 进度控制事件 ====================

    /**
     * 取消总结任务
     */
    data object CancelSummary : ManualSummaryUiEvent()

    // ==================== 结果处理事件 ====================

    /**
     * 重试失败的任务
     */
    data object RetryFailed : ManualSummaryUiEvent()

    /**
     * 查看结果
     */
    data object ViewResult : ManualSummaryUiEvent()

    /**
     * 关闭结果对话框
     */
    data object DismissResult : ManualSummaryUiEvent()

    /**
     * 关闭总结详情对话框
     */
    data object DismissSummaryDetail : ManualSummaryUiEvent()

    /**
     * 关闭错误对话框
     */
    data object DismissError : ManualSummaryUiEvent()

    // ==================== 通用事件 ====================

    /**
     * 重置状态
     */
    data object Reset : ManualSummaryUiEvent()

    /**
     * 清除导航状态
     */
    data object ClearNavigation : ManualSummaryUiEvent()

    /**
     * 关闭无AI服务商警告
     */
    data object DismissNoProviderWarning : ManualSummaryUiEvent()
}
