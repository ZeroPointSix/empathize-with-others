package com.empathy.ai.domain.model

/**
 * 标签类型定义
 */
enum class TagType {
    /** 雷区/警告: 绝对不能踩的点 (例如: "不要提他前妻", "忌讳迟到") */
    RISK_RED,

    /** 重点/策略: 建议切入的点 (例如: "多夸他衣品好", "聊家庭") */
    STRATEGY_GREEN
}

/**
 * 策略标签 - AI 的"军师锦囊"
 *
 * 只存储对决策有直接影响的主观策略（雷区或建议），不存储客观事实
 *
 * @property id 数据库自增 ID
 * @property contactId 外键: 关联到哪个联系人
 * @property content 标签内容 (例如: "不喜欢吃香菜")
 * @property type 类型: 雷区(Red) 或 策略(Green)
 * @property source 来源标记:
 *                  "MANUAL" = 用户手动添加 (高权重，AI 不可篡改)
 *                  "AI_INFERRED" = AI 从聊天记录分析得出 (低权重，用户可确认转正)
 */
data class BrainTag(
    val id: Long = 0,
    val contactId: String,
    val content: String,
    val type: TagType,
    val source: String = "MANUAL",
    val isConfirmed: Boolean = source == "MANUAL"
) {
    /**
     * 是否为AI推断的标签
     */
    val isAiInferred: Boolean
        get() = source == "AI_INFERRED"
}
