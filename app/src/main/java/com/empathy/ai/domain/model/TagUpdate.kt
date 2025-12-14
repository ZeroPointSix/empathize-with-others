package com.empathy.ai.domain.model

/**
 * 标签更新数据类
 *
 * 表示每日总结中对BrainTag的更新操作
 *
 * @property action 操作类型："ADD" 或 "REMOVE"
 * @property type 标签类型："RISK_RED" 或 "STRATEGY_GREEN"
 * @property content 标签内容
 */
data class TagUpdate(
    val action: String,
    val type: String,
    val content: String
) {
    init {
        require(action in listOf("ADD", "REMOVE")) { "action必须是ADD或REMOVE" }
        require(type in listOf("RISK_RED", "STRATEGY_GREEN")) {
            "type必须是RISK_RED或STRATEGY_GREEN"
        }
        require(content.isNotBlank()) { "content不能为空" }
    }
}
