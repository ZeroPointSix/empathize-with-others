package com.empathy.ai.domain.util

import com.empathy.ai.domain.model.ActionType

/**
 * 身份前缀工具类
 *
 * 负责处理对话内容的身份标识，包括添加、解析和清理前缀。
 * 用于区分"对方说的"和"我要回复的"内容，让AI能够正确理解对话角色。
 *
 * 使用场景：
 * - 【帮我分析】：自动添加 PREFIX_CONTACT 前缀
 * - 【帮我检查】：自动添加 PREFIX_USER 前缀
 *
 * 🚨 重要：整个项目中除了本文件，禁止直接使用前缀字符串字面量！
 * 所有引用必须通过常量：IdentityPrefixHelper.PREFIX_CONTACT 或 PREFIX_USER
 *
 * @see PRD-00008 输入内容身份识别与双向对话历史需求
 * @see TDD-00008 输入内容身份识别与双向对话历史技术设计
 */
object IdentityPrefixHelper {

    /**
     * 对方说的内容前缀
     *
     * 用于标识对方发来的消息，在【帮我分析】场景使用
     */
    const val PREFIX_CONTACT = "【对方说】："

    /**
     * 我正在回复的内容前缀
     *
     * 用于标识用户打算发送的内容，在【帮我检查】场景使用
     */
    const val PREFIX_USER = "【我正在回复】："

    /**
     * 身份类型枚举
     *
     * 用于标识对话内容的发送者身份
     */
    enum class IdentityRole(val displayName: String) {
        /** 对方发来的消息 */
        CONTACT("对方"),
        
        /** 我要发送的消息 */
        USER("我"),
        
        /** 旧数据兼容（无前缀的历史数据） */
        LEGACY("历史")
    }

    /**
     * 解析结果数据类
     *
     * @property role 身份类型
     * @property content 纯文本内容（不含前缀）
     */
    data class ParseResult(
        val role: IdentityRole,
        val content: String
    )

    /**
     * 安全地添加身份前缀
     *
     * 防御性编程：如果用户输入已经包含前缀，先清理再添加，防止双重前缀。
     *
     * @param content 用户输入内容
     * @param actionType 操作类型（ANALYZE 或 CHECK）
     * @return 带前缀的内容
     *
     * 示例：
     * - addPrefix("你好", ANALYZE) → "【对方说】：你好"
     * - addPrefix("【对方说】：你好", ANALYZE) → "【对方说】：你好"（不重复添加）
     * - addPrefix("【我正在回复】：你好", ANALYZE) → "【对方说】：你好"（替换为正确前缀）
     */
    fun addPrefix(content: String, actionType: ActionType): String {
        if (content.isBlank()) return content

        // 先清理可能存在的前缀（防止双重前缀）
        val cleanContent = stripAllPrefixes(content)

        // 根据操作类型添加对应前缀
        val prefix = when (actionType) {
            ActionType.ANALYZE -> PREFIX_CONTACT
            @Suppress("DEPRECATION")
            ActionType.CHECK -> PREFIX_USER
            ActionType.POLISH -> PREFIX_USER  // 润色场景：用户的草稿
            ActionType.REPLY -> PREFIX_CONTACT  // 回复场景：对方的消息
        }

        return "$prefix$cleanContent"
    }

    /**
     * 根据身份角色添加前缀
     *
     * @param content 纯文本内容
     * @param role 身份角色
     * @return 带前缀的内容（LEGACY 角色不添加前缀）
     */
    fun addPrefixByRole(content: String, role: IdentityRole): String {
        if (content.isBlank()) return content

        // 先清理可能存在的前缀
        val cleanContent = stripAllPrefixes(content)

        return when (role) {
            IdentityRole.CONTACT -> "$PREFIX_CONTACT$cleanContent"
            IdentityRole.USER -> "$PREFIX_USER$cleanContent"
            IdentityRole.LEGACY -> cleanContent
        }
    }

    /**
     * 解析身份前缀
     *
     * @param content 原始内容（可能带前缀）
     * @return 解析结果（身份 + 纯内容）
     *
     * 示例：
     * - parse("【对方说】：你好") → ParseResult(CONTACT, "你好")
     * - parse("【我正在回复】：你好") → ParseResult(USER, "你好")
     * - parse("你好") → ParseResult(LEGACY, "你好")
     */
    fun parse(content: String): ParseResult {
        val cleanContent = stripAllPrefixes(content)

        val role = when {
            content.startsWith(PREFIX_CONTACT) -> IdentityRole.CONTACT
            content.startsWith(PREFIX_USER) -> IdentityRole.USER
            else -> IdentityRole.LEGACY
        }

        return ParseResult(role, cleanContent)
    }

    /**
     * 去除所有身份前缀（递归处理，防止多重前缀）
     *
     * 场景：用户从截图 OCR 复制了带前缀的内容
     *
     * @param content 原始内容
     * @return 纯文本内容
     *
     * 示例：
     * - stripAllPrefixes("【对方说】：你好") → "你好"
     * - stripAllPrefixes("【对方说】：【对方说】：你好") → "你好"
     * - stripAllPrefixes("【对方说】：【我正在回复】：你好") → "你好"
     */
    fun stripAllPrefixes(content: String): String {
        var result = content

        // 循环去除，直到没有前缀为止
        while (true) {
            val stripped = when {
                result.startsWith(PREFIX_CONTACT) ->
                    result.removePrefix(PREFIX_CONTACT)
                result.startsWith(PREFIX_USER) ->
                    result.removePrefix(PREFIX_USER)
                else -> result
            }

            if (stripped == result) break
            result = stripped
        }

        return result
    }

    /**
     * 根据身份类型获取前缀
     *
     * @param role 身份类型
     * @return 对应的前缀，LEGACY 返回空字符串
     */
    fun getPrefixByRole(role: IdentityRole): String {
        return when (role) {
            IdentityRole.CONTACT -> PREFIX_CONTACT
            IdentityRole.USER -> PREFIX_USER
            IdentityRole.LEGACY -> ""
        }
    }

    /**
     * 重新拼接前缀
     *
     * 用于编辑对话后保存，保留原始身份信息。
     *
     * @param role 原始身份
     * @param newContent 新内容（纯文本）
     * @return 带前缀的内容
     *
     * 示例：
     * - rebuildWithPrefix(CONTACT, "再见") → "【对方说】：再见"
     * - rebuildWithPrefix(LEGACY, "你好") → "你好"
     */
    fun rebuildWithPrefix(role: IdentityRole, newContent: String): String {
        val prefix = getPrefixByRole(role)
        return "$prefix$newContent"
    }

    /**
     * 检查内容是否包含身份前缀
     *
     * @param content 要检查的内容
     * @return 是否包含前缀
     */
    fun hasPrefix(content: String): Boolean {
        return content.startsWith(PREFIX_CONTACT) || content.startsWith(PREFIX_USER)
    }
}
