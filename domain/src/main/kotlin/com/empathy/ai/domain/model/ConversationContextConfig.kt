package com.empathy.ai.domain.model

/**
 * 对话上下文配置
 *
 * 用于配置历史对话回顾的行为参数
 *
 * @property historyCount 历史对话条数 (0/5/10)
 * @property hotSessionThreshold 热对话阈值（毫秒），默认10分钟
 * @property warmSessionThreshold 温对话阈值（毫秒），默认3小时
 * @property maxSingleMessageLength 单条消息最大字符数，超过截断并追加省略号
 * @property maxTotalContextLength 历史上下文总字符数上限，超出时移除最早消息
 */
data class ConversationContextConfig(
    val historyCount: Int = DEFAULT_HISTORY_COUNT,
    val hotSessionThreshold: Long = DEFAULT_HOT_SESSION_THRESHOLD,
    val warmSessionThreshold: Long = DEFAULT_WARM_SESSION_THRESHOLD,
    val maxSingleMessageLength: Int = DEFAULT_MAX_SINGLE_MESSAGE_LENGTH,
    val maxTotalContextLength: Int = DEFAULT_MAX_TOTAL_CONTEXT_LENGTH
) {
    companion object {
        /** 默认历史条数 */
        const val DEFAULT_HISTORY_COUNT = 5

        /** 热对话阈值：10分钟（毫秒） */
        const val DEFAULT_HOT_SESSION_THRESHOLD = 10 * 60 * 1000L

        /** 温对话阈值：3小时（毫秒） */
        const val DEFAULT_WARM_SESSION_THRESHOLD = 3 * 60 * 60 * 1000L

        /** 单条消息最大字符数 */
        const val DEFAULT_MAX_SINGLE_MESSAGE_LENGTH = 500

        /** 历史上下文总字符数上限 */
        const val DEFAULT_MAX_TOTAL_CONTEXT_LENGTH = 2000

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
        require(maxSingleMessageLength > 0) { "单条消息最大字符数必须大于0" }
        require(maxTotalContextLength > 0) { "历史上下文总字符数上限必须大于0" }
        require(maxTotalContextLength >= maxSingleMessageLength) {
            "历史上下文总字符数上限必须大于等于单条消息最大字符数"
        }
    }
}
