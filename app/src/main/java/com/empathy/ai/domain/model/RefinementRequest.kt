package com.empathy.ai.domain.model

/**
 * 微调请求模型
 *
 * 承载微调重新生成的请求参数
 *
 * @property originalInput 原始用户输入
 * @property originalTask 原始任务类型
 * @property lastAiResponse AI上次的回复
 * @property refinementInstruction 用户的微调指令（可选，为空表示直接重新生成）
 * @property contactId 联系人ID
 *
 * @see PRD-00009 悬浮窗功能重构需求
 * @see TDD-00009 悬浮窗功能重构技术设计
 */
data class RefinementRequest(
    /**
     * 原始用户输入
     */
    val originalInput: String,

    /**
     * 原始任务类型
     */
    val originalTask: ActionType,

    /**
     * AI上次的回复
     */
    val lastAiResponse: String,

    /**
     * 用户的微调指令（可选）
     * 为空表示直接重新生成
     */
    val refinementInstruction: String? = null,

    /**
     * 联系人ID
     */
    val contactId: String
) {
    /**
     * 是否有微调指令
     *
     * @return true 如果用户提供了微调指令
     */
    fun hasInstruction(): Boolean = !refinementInstruction.isNullOrBlank()

    /**
     * 获取微调指令（如果没有则返回空字符串）
     *
     * @return 微调指令或空字符串
     */
    fun getInstructionOrEmpty(): String = refinementInstruction ?: ""
}
