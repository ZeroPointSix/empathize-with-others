package com.empathy.ai.domain.util

/**
 * Prompt模板常量
 *
 * 集中管理AI相关的Prompt模板，便于维护和修改
 *
 * @deprecated 请使用 [PromptBuilder] 替代，该类提供更灵活的提示词管理功能
 * @see PromptBuilder
 * @see com.empathy.ai.domain.repository.PromptRepository
 */
@Deprecated(
    message = "请使用 PromptBuilder 替代",
    replaceWith = ReplaceWith(
        "PromptBuilder",
        "com.empathy.ai.domain.util.PromptBuilder"
    )
)
object PromptTemplates {

    /**
     * 每日总结JSON Schema
     */
    const val SUMMARY_JSON_SCHEMA = """
{
  "newFacts": [{"key": "事实名称", "value": "事实内容"}],
  "updatedFacts": [{"key": "已有事实名称", "value": "更新后的内容"}],
  "deletedFactKeys": ["需要删除的事实名称"],
  "newTags": [{"content": "标签内容", "type": "RISK_RED或STRATEGY_GREEN"}],
  "relationshipScoreChange": 数字(-10到+10),
  "keyEvents": [{"description": "事件描述", "importance": "HIGH/MEDIUM/LOW"}],
  "summary": "今日互动总结（一句话）"
}"""

    /**
     * 每日总结系统指令
     */
    const val SUMMARY_SYSTEM_INSTRUCTION = """你是一个专业的社交关系分析助手。

请分析提供的对话记录，提取关键信息并生成总结。

要求：
1. 识别对话中透露的新事实（如爱好、工作、家庭等）
2. 识别需要注意的雷区（敏感话题、禁忌等）
3. 识别有效的沟通策略
4. 评估关系变化趋势
5. 提取关键事件

必须以JSON格式返回结果，严格遵循指定的Schema。
关系分数变化范围：-10到+10
重要性等级：1-5（5最重要）"""

    /**
     * 构建总结Prompt的模板
     *
     * @param contactName 联系人名称
     * @param targetGoal 攻略目标
     * @param relationshipScore 关系分数
     * @param factsSection 已知信息部分
     * @param conversationsSection 对话记录部分
     * @return 完整的Prompt
     */
    fun buildSummaryPrompt(
        contactName: String,
        targetGoal: String,
        relationshipScore: Int,
        factsSection: String,
        conversationsSection: String
    ): String {
        return buildString {
            appendLine("请分析以下与「$contactName」的对话记录，并提取关键信息。")
            appendLine()
            appendLine("【当前画像】")
            appendLine("- 攻略目标: $targetGoal")
            appendLine("- 关系分数: $relationshipScore/100")
            appendLine()
            if (factsSection.isNotBlank()) {
                appendLine("【已知信息】")
                appendLine(factsSection)
                appendLine()
            }
            appendLine("【今日对话】")
            appendLine(conversationsSection)
            appendLine()
            appendLine("请以JSON格式返回分析结果：")
            append(SUMMARY_JSON_SCHEMA)
        }
    }
}
