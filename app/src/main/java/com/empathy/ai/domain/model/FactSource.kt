package com.empathy.ai.domain.model

/**
 * 事实来源枚举
 *
 * 标识Fact的来源是用户手动添加还是AI推断
 */
enum class FactSource {
    /**
     * 用户手动添加的事实
     */
    MANUAL,

    /**
     * AI推断的事实
     */
    AI_INFERRED
}
