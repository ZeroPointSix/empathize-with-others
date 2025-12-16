package com.empathy.ai.domain.model

import java.time.LocalDate

/**
 * 提示词变量上下文
 *
 * 存储用于变量替换的上下文数据，支持从联系人画像自动构建
 *
 * @property contactName 联系人名称
 * @property relationshipStatus 关系状态描述
 * @property riskTags 雷区标签列表
 * @property strategyTags 策略标签列表
 * @property factsCount 事实数量
 * @property todayDate 今日日期
 */
data class PromptContext(
    val contactName: String? = null,
    val relationshipStatus: String? = null,
    val riskTags: List<String> = emptyList(),
    val strategyTags: List<String> = emptyList(),
    val factsCount: Int = 0,
    val todayDate: String = ""
) {
    /**
     * 根据变量名获取对应的值
     *
     * @param name 变量名（不区分大小写）
     * @return 变量值，如果变量不存在则返回null
     */
    fun getVariable(name: String): String? = when (name.lowercase()) {
        "contact_name" -> contactName
        "relationship_status" -> relationshipStatus
        "risk_tags" -> riskTags.joinToString("、").ifEmpty { "无" }
        "strategy_tags" -> strategyTags.joinToString("、").ifEmpty { "无" }
        "facts_count" -> factsCount.toString()
        "today_date" -> todayDate
        else -> null
    }

    companion object {
        /**
         * 从联系人画像构建上下文
         *
         * @param contact 联系人画像
         * @return 填充好的上下文对象
         */
        fun fromContact(contact: ContactProfile): PromptContext {
            val riskTags = contact.facts
                .filter { it.key == "雷区" || it.key.contains("雷区") }
                .map { it.value }

            val strategyTags = contact.facts
                .filter { it.key == "策略" || it.key.contains("策略") }
                .map { it.value }

            return PromptContext(
                contactName = contact.name,
                relationshipStatus = contact.getRelationshipLevel().displayName,
                riskTags = riskTags,
                strategyTags = strategyTags,
                factsCount = contact.facts.size,
                todayDate = LocalDate.now().toString()
            )
        }
    }
}
