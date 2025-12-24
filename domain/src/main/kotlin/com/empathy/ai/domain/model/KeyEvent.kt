package com.empathy.ai.domain.model

/**
 * 关键事件数据类
 *
 * 表示每日总结中的一个关键事件
 *
 * @property event 事件描述
 * @property importance 重要性评分 (1-10)
 */
data class KeyEvent(
    val event: String,
    val importance: Int
) {
    init {
        require(event.isNotBlank()) { "event不能为空" }
        require(importance in 1..10) { "importance必须在1到10之间" }
    }
}
