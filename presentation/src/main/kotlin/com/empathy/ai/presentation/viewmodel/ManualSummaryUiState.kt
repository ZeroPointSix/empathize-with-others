package com.empathy.ai.presentation.viewmodel

import com.empathy.ai.domain.model.ConflictResult
import com.empathy.ai.domain.model.ConflictResolution
import com.empathy.ai.domain.model.DateRange
import com.empathy.ai.domain.model.SummaryTask
import com.empathy.ai.domain.usecase.ManualSummaryUseCase

/**
 * 手动总结UI状态
 *
 * 管理手动触发AI总结功能的所有UI状态
 *
 * ## 关联文档
 * - TDD-00011: 手动触发AI总结功能技术设计
 * - BUG-00064: AI总结功能未生效修复
 *
 * ## 状态分组
 * - **日期选择状态**: showDatePicker, selectedDateRange, validationError
 * - **冲突处理状态**: showConflictDialog, conflictResult
 * - **进度状态**: showProgressDialog, task
 * - **结果状态**: showResultDialog, showSummaryDetailDialog
 * - **错误状态**: showErrorDialog
 * - **前置检查状态**: showNoProviderWarning (BUG-00064新增)
 *
 * ## BUG-00064 状态变更
 * 新增 `showNoProviderWarning` 和 `noProviderWarningMessage` 字段
 * 用于在用户未配置AI服务商时显示友好提示，避免静默失败
 *
 * ## 设计决策
 * - **状态分区**: 按功能模块分区组织，便于理解和维护
 * - **派生属性**: 提供 isProcessing, hasError, progressPercent 等派生属性
 * - **不可变性**: 使用 data class 保证状态不可变性
 *
 * @see ManualSummaryUiEvent
 * @see TDD-00011-手动触发AI总结功能技术设计.md
 * @see BUG-00064-AI总结功能未生效-修复方案.md
 */
data class ManualSummaryUiState(
    /** 当前联系人ID */
    val contactId: String? = null,

    // ==================== 日期选择状态 ====================

    /** 是否显示日期选择器 */
    val showDatePicker: Boolean = false,

    /** 选中的快捷选项 */
    val selectedQuickOption: QuickDateOption? = QuickDateOption.LAST_7_DAYS,

    /** 选中的日期范围 */
    val selectedDateRange: DateRange? = null,

    /** 验证错误信息 */
    val validationError: String? = null,

    /** 是否显示范围警告 */
    val showRangeWarning: Boolean = false,

    /** 范围警告信息 */
    val rangeWarningMessage: String? = null,

    // ==================== 冲突处理状态 ====================

    /** 是否显示冲突对话框 */
    val showConflictDialog: Boolean = false,

    /** 冲突检测结果 */
    val conflictResult: ConflictResult.HasConflict? = null,

    /** 选中的冲突处理方式 */
    val selectedConflictResolution: ConflictResolution? = null,

    // ==================== 进度状态 ====================

    /** 是否显示进度对话框 */
    val showProgressDialog: Boolean = false,

    /** 当前任务 */
    val task: SummaryTask? = null,

    // ==================== 结果状态 ====================

    /** 是否显示结果对话框（统计信息） */
    val showResultDialog: Boolean = false,

    /** 是否显示总结详情对话框 */
    val showSummaryDetailDialog: Boolean = false,

    /** 总结结果 */
    val summaryResult: ManualSummaryUseCase.SummaryResult? = null,

    // ==================== 错误状态 ====================

    /** 是否显示错误对话框 */
    val showErrorDialog: Boolean = false,

    // ==================== 导航状态 ====================

    /** 是否导航到时光轴 */
    val navigateToTimeline: Boolean = false,

    // ==================== 前置检查状态 ====================

    /** 是否显示无AI服务商警告 */
    val showNoProviderWarning: Boolean = false,

    /** 无AI服务商警告信息 */
    val noProviderWarningMessage: String? = null
) {
    /**
     * 是否正在处理中
     */
    val isProcessing: Boolean
        get() = showProgressDialog

    /**
     * 是否有错误
     */
    val hasError: Boolean
        get() = validationError != null || showErrorDialog

    /**
     * 当前进度百分比
     */
    val progressPercent: Int
        get() = ((task?.progress ?: 0f) * 100).toInt()

    /**
     * 当前步骤描述
     */
    val currentStepDescription: String
        get() = task?.currentStep ?: ""
}

/**
 * 快捷日期选项枚举
 */
enum class QuickDateOption(val displayName: String) {
    /** 最近7天 */
    LAST_7_DAYS("最近7天"),

    /** 本月 */
    THIS_MONTH("本月"),

    /** 上月 */
    LAST_MONTH("上月"),

    /** 最近30天 */
    LAST_30_DAYS("最近30天"),

    /** 未总结时段 */
    MISSING_DATES("未总结时段")
}
