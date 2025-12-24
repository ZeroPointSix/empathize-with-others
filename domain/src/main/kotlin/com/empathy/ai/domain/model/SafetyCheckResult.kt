package com.empathy.ai.domain.model

/**
 * 安全检查结果
 *
 * 用于 AiRepository.checkDraftSafety 的返回值
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
