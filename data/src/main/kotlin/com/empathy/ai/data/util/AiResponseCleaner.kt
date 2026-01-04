package com.empathy.ai.data.util

import android.util.Log

/**
 * AiResponseCleaner 实现了AI响应内容清洗功能
 *
 * 业务背景：AI返回的建议可能包含多种格式
 * - 完整的句子解释（"我觉得你可以这样说..."）
 * - 用引号包裹的建议内容（"建议内容"）
 * - 前缀说明文字（"比较好："、"建议你："）
 *
 * 为什么需要清洗？
 * - 用户期望看到的是可直接使用的沟通建议
 * - AI的解释性文字对用户价值较低
 * - 提取核心建议可以提升用户体验
 *
 * 设计策略（优先级从高到低）：
 * 1. 提取引号内容 → AI倾向于用引号标注核心建议
 * 2. 移除前缀文字 → 去除"我觉得"、"建议你"等冗余前缀
 * 3. 返回原文兜底 → 无法提取时返回原始内容
 *
 * @see PRD-00005 提示词管理系统需求
 */
object AiResponseCleaner {

    private const val TAG = "AiResponseCleaner"

    /**
     * 最小建议长度阈值
     *
     * 过滤AI的无意义短句（如单字符或无意义内容）
     * 设置为3是因为中文最短有效建议通常为2-3字
     */
    private const val MIN_SUGGESTION_LENGTH = 3

    /**
     * 从AI响应中提取建议列表
     *
     * 【算法策略】引号提取法
     * - 使用正则匹配双引号 " " 和中文引号 "「」" "『』"
     * - 提取引号内的内容作为建议
     * - 过滤太短的内容（噪音）
     *
     * 【兜底策略】
     * - 如果没有提取到引号内容，返回原文
     * - 确保用户至少能看到一些内容
     *
     * @param rawResponse AI原始响应
     * @return 提取的建议列表（可能包含原文作为兜底）
     */
    fun cleanSuggestion(rawResponse: String): List<String> {
        if (rawResponse.isBlank()) {
            return emptyList()
        }

        // 引号正则：支持 "、"""、「」、『』 等多种引号
        val pattern = Regex("""[""\u201C\u201D]([^""\u201C\u201D]+)[""\u201C\u201D]""")
        val matches = pattern.findAll(rawResponse)
            .map { it.groupValues[1].trim() }  // 提取引号内内容
            .filter { it.isNotBlank() && it.length >= MIN_SUGGESTION_LENGTH }
            .toList()

        Log.d(TAG, "提取到 ${matches.size} 条建议: ${matches.take(3)}")

        return if (matches.isNotEmpty()) {
            matches
        } else {
            Log.d(TAG, "未提取到引号内容，使用原文保底")
            listOf(rawResponse)
        }
    }

    /**
     * 提取单条建议
     *
     * @param rawResponse AI原始响应
     * @return 第一条建议，如果提取失败则返回原文
     */
    fun cleanSingleSuggestion(rawResponse: String): String {
        val suggestions = cleanSuggestion(rawResponse)
        return suggestions.firstOrNull() ?: rawResponse
    }

    /**
     * 清洗并格式化响应
     *
     * 【使用场景】在UI中展示多条建议时
     * - 自动编号（1. 2. 3.）
     * - 使用指定分隔符连接
     *
     * @param rawResponse AI原始响应
     * @param separator 分隔符，默认换行
     * @return 格式化后的字符串
     */
    fun cleanAndFormat(rawResponse: String, separator: String = "\n"): String {
        val suggestions = cleanSuggestion(rawResponse)
        return if (suggestions.size > 1) {
            suggestions.mapIndexed { index, s -> "${index + 1}. $s" }.joinToString(separator)
        } else {
            suggestions.firstOrNull() ?: rawResponse
        }
    }

    /**
     * 检查响应是否包含引号建议
     *
     * 【使用场景】快速判断是否需要进一步处理
     * - 用于预处理判断
     * - 避免不必要的解析开销
     *
     * @param text 待检查的文本
     * @return 是否包含有效引号建议
     */
    fun hasQuotedSuggestion(text: String): Boolean {
        val pattern = Regex("""[""\u201C\u201D]([^""\u201C\u201D]+)[""\u201C\u201D]""")
        return pattern.findAll(text)
            .any { it.groupValues[1].trim().length >= MIN_SUGGESTION_LENGTH }
    }

    /**
     * 移除AI解释性前缀
     *
     * 【前缀模式】常见的AI解释性开头
     * - "我觉得你可以这样说："
     * - "建议你："
     * - "比较好："
     *
     * 【设计决策】使用正则列表而非单一模式
     * - 覆盖更多变体（我觉得、建议你、我建议...）
     * - 支持中英文标点（:：）
     *
     * @param text 输入文本
     * @return 移除前缀后的文本
     */
    fun removeExplanationPrefix(text: String): String {
        val prefixPatterns = listOf(
            // 模式1: "我觉得/建议你/可以试试/推荐/不妨" + 任意内容 + 冒号
            Regex("""^(我觉得|建议你?|可以试试|推荐|不妨)[^：:""\u201C\u201D]*[：:]\s*"""),
            // 模式2: "这样说/换成/改成" + 任意内容 + 冒号
            Regex("""^(这样说|换成|改成)[^：:""\u201C\u201D]*[：:]\s*"""),
            // 模式3: 任意开头 + "比较好/更好/更合适" + 冒号（限制前导字符数避免过度匹配）
            Regex("""^[^：:""\u201C\u201D]{0,20}(比较好|更好|更合适)[：:]\s*""")
        )

        var result = text.trim()
        for (pattern in prefixPatterns) {
            result = result.replace(pattern, "")
        }
        return result.trim()
    }

    /**
     * 智能清洗（推荐使用）
     *
     * 【策略选择逻辑】
     * 1. 优先提取引号内容（通常质量最高）
     * 2. 引号内容只有1条且等于原文 → 尝试移除前缀
     * 3. 前缀移除后长度小于原文50% → 保留原文（可能误删）
     *
     * @param rawResponse AI原始响应
     * @return 清洗后的文本
     */
    fun smartClean(rawResponse: String): String {
        if (rawResponse.isBlank()) {
            return ""
        }

        val quotedSuggestions = cleanSuggestion(rawResponse)

        // 策略1: 有多条引号内容或引号内容与原文不同 → 使用引号内容
        if (quotedSuggestions.size > 1 ||
            (quotedSuggestions.size == 1 && quotedSuggestions[0] != rawResponse)) {
            return quotedSuggestions.first()
        }

        // 策略2: 尝试移除前缀
        val cleaned = removeExplanationPrefix(rawResponse)

        // 策略3: 如果移除后内容太少（<50%），保留原文（可能是有效内容被误删）
        return if (cleaned.length < rawResponse.length * 0.5) {
            rawResponse
        } else {
            cleaned
        }
    }
}
