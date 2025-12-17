package com.empathy.ai.data.local

import com.empathy.ai.domain.model.PromptScene

/**
 * 默认提示词模板
 *
 * 存储各场景的默认提示词，用于首次启动或恢复默认设置
 *
 * 设计原则（三层分离架构）：
 * - 用户提示词只定义"AI应该怎么做"，不需要关心数据
 * - 联系人信息、聊天记录等数据由系统自动注入，用户不可见
 * - 默认提示词应该是友好的自然语言，不包含技术性变量占位符
 */
object DefaultPrompts {

    /**
     * 获取指定场景的默认提示词
     *
     * @param scene 场景类型
     * @return 默认提示词内容（用户友好的自然语言）
     */
    fun getDefault(scene: PromptScene): String = when (scene) {
        PromptScene.ANALYZE -> ANALYZE_DEFAULT
        PromptScene.CHECK -> CHECK_DEFAULT
        PromptScene.EXTRACT -> EXTRACT_DEFAULT
        PromptScene.SUMMARY -> SUMMARY_DEFAULT
        PromptScene.POLISH -> POLISH_DEFAULT
        PromptScene.REPLY -> REPLY_DEFAULT
    }

    /**
     * 聊天分析场景默认提示词
     *
     * 用户可以在此基础上自定义AI的分析风格和关注重点
     * 联系人信息、雷区、策略等数据会由系统自动注入
     */
    private const val ANALYZE_DEFAULT = ""

    /**
     * 安全检查场景默认提示词
     *
     * 用户可以自定义检查的重点和风格
     * 雷区规则等数据会由系统自动注入
     */
    private const val CHECK_DEFAULT = ""

    /**
     * 信息提取场景默认提示词
     *
     * 用户可以自定义需要重点关注的信息类型
     */
    private const val EXTRACT_DEFAULT = ""

    /**
     * 每日总结场景默认提示词
     *
     * 用户可以自定义总结的风格和重点
     * 日期、联系人信息等数据会由系统自动注入
     */
    private const val SUMMARY_DEFAULT = ""

    /**
     * 润色场景默认提示词
     *
     * 用户可以自定义润色的风格和重点
     * 联系人信息、雷区等数据会由系统自动注入
     */
    private const val POLISH_DEFAULT = ""

    /**
     * 回复场景默认提示词
     *
     * 用户可以自定义回复建议的风格和重点
     * 联系人信息、策略等数据会由系统自动注入
     */
    private const val REPLY_DEFAULT = ""
}
