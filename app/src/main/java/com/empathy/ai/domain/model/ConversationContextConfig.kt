package com.empathy.ai.domain.model

/**
 * 对话上下文配置
 *
 * 用于配置历史对话回顾的行为参数
 *
 * @property historyCount 历史对话条数 (0/5/10)
 * @property hotSessionThreshold 热对话阈值（毫秒），默认10分钟
 * @property warmSessionThreshold 温对话阈值（毫秒），默认3小时
 */
data class ConversationContextConfig(
    val historyCount: Int = DEFAULT_HISTORY_COUNT,
    val hotSessionThreshold: Long = DEFAULT_HOT_SESSION_THRESHOLD,
    val warmSessionThreshold: Long = DEFAULT_WARM_SESSION_THRESHOLD
) {
    companion object {
        /** 默认历史条数 */
        const val DEFAULT_HISTORY_COUNT = 5

        /** 热对话阈值：10分钟（毫秒） */
        const val DEFAULT_HOT_SESSION_THRESHOLD = 10 * 60 * 1000L

        /** 温对话阈值：3小时（毫秒） */
        const val DEFAULT_WARM_SESSION_THRESHOLD = 3 * 60 * 60 * 1000L

        /** 可选的历史条数选项 */
        val HISTORY_COUNT_OPTIONS = listOf(0, 5, 10)
    }

    init {
        require(historyCount in HISTORY_COUNT_OPTIONS) {
            "历史条数必须是 ${HISTORY_COUNT_OPTIONS.joinToString()} 之一"
        }
        require(hotSessionThreshold > 0) { "热对话阈值必须大于0" }
        require(warmSessionThreshold > hotSessionThreshold) {
            "温对话阈值必须大于热对话阈值"
        }
    }
}
