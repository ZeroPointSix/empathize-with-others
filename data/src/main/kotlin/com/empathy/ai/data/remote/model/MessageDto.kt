package com.empathy.ai.data.remote.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * 消息单元 DTO
 *
 * 符合 OpenAI Chat Completion API 标准的消息结构。
 *
 * 【设计决策】role 字段的设计
 * - system: 系统指令，设置 AI 行为边界和角色定义
 *           【最佳实践】放在消息列表首位，确保指令被优先考虑
 *           【示例】"你是一个专业的社交关系分析顾问..."
 * - user: 用户输入，实际问题或对话内容
 *         【格式】纯文本或带格式的文本
 * - assistant: AI 回复，用于维护对话历史
 *              【用途】上下文连贯性，让 AI 知道自己的历史回复
 *
 * 【上下文构建策略】TDD-00026
 * ┌─────────────────────────────────────────────────────────────────────┐
 * │ 消息历史限制                                                        │
 * ├─────────────────────────────────────────────────────────────────────┤
 * │ System Prompts: 固定不变（AI军师 Header + Footer）                 │
 * │ 对话历史摘要: 最多20条（AI军师分析用）                              │
 * │ 会话上下文: 最多10条（当前对话流）                                  │
 * │ 当前消息: 1条（用户最新输入）                                       │
 * └─────────────────────────────────────────────────────────────────────┘
 *
 * 【示例】典型消息列表结构：
 * ```
 * messages = [
 *   MessageDto(role="system", content="你是一个专业的社交关系分析顾问..."),
 *   MessageDto(role="user", content="帮我分析一下最近的对话"),
 *   MessageDto(role="assistant", content="好的，请提供对话记录"),
 *   MessageDto(role="user", content="[最近20条对话历史]"),
 *   MessageDto(role="user", content="你觉得她最近对我是什么态度？")
 * ]
 * ```
 *
 * @property role 角色: "system" | "user" | "assistant"
 * @property content 消息内容
 *
 * 示例:
 * ```json
 * {
 *   "role": "user",
 *   "content": "你好"
 * }
 * ```
 */
@JsonClass(generateAdapter = true)
data class MessageDto(
    @Json(name = "role")
    val role: String,

    @Json(name = "content")
    val content: String
)
