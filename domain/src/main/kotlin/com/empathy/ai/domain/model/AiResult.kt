package com.empathy.ai.domain.model

/**
 * AI结果密封类
 *
 * 统一三种功能（分析、润色、回复）的返回类型，便于UI层统一处理。
 * 使用代数数据类型（ADT）确保类型安全和穷尽性检查。
 *
 * 业务背景 (PRD-00009):
 * - 悬浮窗支持三种核心功能：分析、润色、回复
 * - 每种功能返回不同类型的结果，但有共同的展示需求
 * - 统一类型便于UI层用when表达式处理所有情况
 * - getCopyableText/getDisplayContent提供统一的UI接口
 *
 * 设计决策 (TDD-00009):
 * - sealed class实现ADT，编译器强制穷尽性检查
 * - 每个子类包含具体的结果类型（AnalysisResult/PolishResult/ReplyResult）
 * - 扩展方法getCopyableText/getDisplayContent/getActionType封装通用逻辑
 *
 * @see PRD-00009 悬浮窗功能重构需求
 * @see TDD-00009 悬浮窗功能重构技术设计
 */
sealed class AiResult {

    /**
     * 分析结果
     *
     * AI军师对对话内容的深度分析，包含策略分析和话术建议
     *
     * @property result 分析结果详情
     */
    data class Analysis(val result: AnalysisResult) : AiResult()

    /**
     * 润色结果
     *
     * 用户草稿的润色版本，保持原意但表达更得体
     *
     * @property result 润色结果详情
     */
    data class Polish(val result: PolishResult) : AiResult()

    /**
     * 回复结果
     *
     * AI根据对话上下文生成的建议回复
     *
     * @property result 回复结果详情
     */
    data class Reply(val result: ReplyResult) : AiResult()

    /**
     * 获取可复制的文本
     *
     * 根据不同结果类型返回对应的可复制内容
     *
     * @return 可复制的纯文本
     */
    fun getCopyableText(): String = when (this) {
        is Analysis -> result.replySuggestion
        is Polish -> result.getCopyableText()
        is Reply -> result.getCopyableText()
    }

    /**
     * 获取显示内容
     *
     * 根据不同结果类型返回格式化的显示内容
     *
     * @return 格式化的显示内容
     */
    fun getDisplayContent(): String = when (this) {
        is Analysis -> buildString {
            appendLine("【军师分析】")
            appendLine(result.strategyAnalysis)
            appendLine()
            appendLine("【话术建议】")
            append(result.replySuggestion)
        }
        is Polish -> result.getDisplayContent()
        is Reply -> result.getDisplayContent()
    }

    /**
     * 获取结果类型对应的操作类型
     *
     * @return 对应的 ActionType
     */
    fun getActionType(): ActionType = when (this) {
        is Analysis -> ActionType.ANALYZE
        is Polish -> ActionType.POLISH
        is Reply -> ActionType.REPLY
    }
}
