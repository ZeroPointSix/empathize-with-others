package com.empathy.ai.domain.model

/**
 * 风险等级枚举
 *
 * 用于评估对话内容的风险程度，指导用户沟通策略。
 *
 * @property displayName 显示名称
 */
enum class RiskLevel(val displayName: String) {
    /** 安全 - 可以正常发送 */
    SAFE("安全"),

    /** 警告 - 有轻微不妥，建议修改 */
    WARNING("警告"),

    /** 危险 - 可能踩雷，建议调整策略 */
    DANGER("危险")
}

/**
 * 分析结果 - AI 计算后的输出
 *
 * 承载AI分析后的核心结果，直接用于UI展示。
 * 这是"帮我分析"功能的最终输出，包含话术建议和风险评估。
 *
 * 业务背景 (PRD-00001):
 * - 悬浮窗核心功能之一，用户点击后AI分析并返回建议
 * - replySuggestion用于快速复制到聊天框
 * - strategyAnalysis帮助用户理解当前关系状态
 * - riskLevel指导用户调整沟通策略
 *
 * 设计决策:
 * - 三个字段均为必填，确保UI展示完整性
 * - riskLevel使用枚举而非字符串，便于UI主题适配
 *
 * @property replySuggestion 话术建议: AI 生成的可以直接发送的回复文本
 * @property strategyAnalysis 军师分析: AI 对当前局势的心理分析 (给用户看的)
 * @property riskLevel 风险等级: 用于 UI 显示红/黄/绿颜色
 * @see RiskLevel 风险等级枚举
 */
data class AnalysisResult(
    val replySuggestion: String,
    val strategyAnalysis: String,
    val riskLevel: RiskLevel
)
