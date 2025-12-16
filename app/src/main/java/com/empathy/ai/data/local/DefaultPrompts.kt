package com.empathy.ai.data.local

import com.empathy.ai.domain.model.PromptScene

/**
 * 默认提示词模板
 *
 * 存储各场景的默认提示词，用于首次启动或恢复默认设置
 */
object DefaultPrompts {

    /**
     * 获取指定场景的默认提示词
     *
     * @param scene 场景类型
     * @return 默认提示词内容
     */
    fun getDefault(scene: PromptScene): String = when (scene) {
        PromptScene.ANALYZE -> ANALYZE_DEFAULT
        PromptScene.CHECK -> CHECK_DEFAULT
        PromptScene.EXTRACT -> EXTRACT_DEFAULT
        PromptScene.SUMMARY -> SUMMARY_DEFAULT
    }

    /**
     * 聊天分析场景默认提示词
     */
    private const val ANALYZE_DEFAULT = """请分析与「{{contact_name}}」的聊天内容，提供沟通建议。

当前关系状态：{{relationship_status}}
已知雷区：{{risk_tags}}
有效策略：{{strategy_tags}}
已记录事实：{{facts_count}}条

请根据以上信息，分析对话内容并给出：
1. 对方当前的情绪状态和意图
2. 潜在的沟通风险点
3. 推荐的回复策略
4. 具体的回复建议"""

    /**
     * 安全检查场景默认提示词
     */
    private const val CHECK_DEFAULT = """请检查以下草稿内容是否存在沟通风险。

联系人：{{contact_name}}
已知雷区：{{risk_tags}}

请检查：
1. 是否触及已知雷区话题
2. 是否存在可能引起误解的表达
3. 语气是否合适
4. 是否有敏感词汇需要调整"""

    /**
     * 信息提取场景默认提示词
     */
    private const val EXTRACT_DEFAULT = """请从以下内容中提取关于「{{contact_name}}」的关键信息。

需要提取的信息类型：
1. 个人基本信息（生日、职业、爱好等）
2. 关系相关信息（家庭、朋友、同事等）
3. 偏好和习惯
4. 重要事件和日期
5. 沟通中需要注意的点（雷区）"""

    /**
     * 每日总结场景默认提示词
     */
    private const val SUMMARY_DEFAULT = """请总结今日（{{today_date}}）与「{{contact_name}}」的互动情况。

当前关系状态：{{relationship_status}}
已记录事实：{{facts_count}}条

请生成：
1. 今日互动要点总结
2. 新发现的事实信息
3. 关系变化评估
4. 后续跟进建议"""
}
