package com.empathy.ai.domain.model

/**
 * 安全检查结果
 *
 * 用于检查用户草稿是否存在潜在风险（如雷区触发），指导润色和回复功能。
 *
 * 业务背景 (PRD-00009):
 * - 检查用户输入是否触发了预定义的雷区规则
 * - isSafe字段用于UI判断是否显示红色警示框
 * - triggeredRisks列出具体触发的雷区，便于用户理解风险点
 * - suggestion提供修正建议，帮助用户改进输入
 *
 * 设计决策 (TDD-00009):
 * - 使用data class便于数据传递和测试
 * - 空列表表示没有触发任何雷区
 * - suggestion为可选字段，无建议时为null
 *
 * @property isSafe 是否通过检查 (UI 根据此字段决定是否变红框)
 * @property triggeredRisks 触发了哪些具体的雷区规则 (用于 UI Toast 提示)
 * @property suggestion 简短的修正建议 (可选)
 */
data class SafetyCheckResult(
    val isSafe: Boolean,
    val triggeredRisks: List<String> = emptyList(),
    val suggestion: String? = null
)
