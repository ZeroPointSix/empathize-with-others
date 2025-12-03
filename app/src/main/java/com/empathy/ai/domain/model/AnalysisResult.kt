package com.empathy.ai.domain.model

/**
 * 风险等级枚举
 */
enum class RiskLevel {
    /** 安全 */
    SAFE,

    /** 警告 (有轻微不妥) */
    WARNING,

    /** 危险 (踩雷了) */
    DANGER
}

/**
 * 分析结果 - AI 计算后的输出
 *
 * 直接用于 UI 展示
 *
 * @property replySuggestion 话术建议: AI 生成的可以直接发送的回复文本
 * @property strategyAnalysis 军师分析: AI 对当前局势的心理分析 (给用户看的)
 * @property riskLevel 风险等级: 用于 UI 显示红/黄/绿颜色
 */
data class AnalysisResult(
    val replySuggestion: String,
    val strategyAnalysis: String,
    val riskLevel: RiskLevel
)
