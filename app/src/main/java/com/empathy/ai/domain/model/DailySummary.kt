package com.empathy.ai.domain.model

/**
 * 每日总结领域模型
 *
 * 表示某个联系人某一天的互动总结
 *
 * @property id 总结ID
 * @property contactId 联系人ID
 * @property summaryDate 总结日期，格式: "yyyy-MM-dd"
 * @property content 总结内容
 * @property keyEvents 关键事件列表
 * @property newFacts 新发现的事实列表
 * @property updatedTags 标签更新列表
 * @property relationshipScoreChange 关系分数变化 (-10到+10)
 * @property relationshipTrend 关系趋势
 */
data class DailySummary(
    val id: Long = 0,
    val contactId: String,
    val summaryDate: String,
    val content: String,
    val keyEvents: List<KeyEvent>,
    val newFacts: List<Fact>,
    val updatedTags: List<TagUpdate>,
    val relationshipScoreChange: Int,
    val relationshipTrend: RelationshipTrend
) {
    init {
        require(contactId.isNotBlank()) { "contactId不能为空" }
        require(summaryDate.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) {
            "summaryDate格式必须为yyyy-MM-dd"
        }
        require(relationshipScoreChange in -10..10) {
            "relationshipScoreChange必须在-10到10之间"
        }
    }

    /**
     * 判断是否有实质性内容
     */
    fun hasSubstantialContent(): Boolean {
        return keyEvents.isNotEmpty() || newFacts.isNotEmpty() || updatedTags.isNotEmpty()
    }
}
