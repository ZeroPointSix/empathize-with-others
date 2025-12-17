package com.empathy.ai.domain.util

import android.util.Log

/**
 * AI响应清洗器
 *
 * 用于从AI返回的文本中提取有效的建议内容。
 * 
 * 设计原则：
 * - 优先提取引号内的核心建议（AI被要求用引号包裹建议）
 * - 过滤掉AI的废话解释
 * - 提供保底方案，确保用户始终能看到内容
 *
 * 使用场景：
 * - 分析结果中的 replySuggestion 字段
 * - 安全检查结果中的 suggestion 字段
 */
object AiResponseCleaner {

    private const val TAG = "AiResponseCleaner"

    /**
     * 最小有效建议长度
     * 过滤掉太短的内容（如单个词的引用）
     */
    private const val MIN_SUGGESTION_LENGTH = 3

    /**
     * 清洗AI回复，优先提取引号内的建议内容
     *
     * 处理逻辑：
     * 1. 正则提取所有引号内的内容
     * 2. 过滤掉过短或空的字符串
     * 3. 如果提取到有效内容 → 返回提取的列表
     * 4. 如果没提取到 → 返回原始文本作为保底
     *
     * @param rawResponse AI返回的原始文本
     * @return 清洗后的建议列表
     */
    fun cleanSuggestion(rawResponse: String): List<String> {
        if (rawResponse.isBlank()) {
            return emptyList()
        }

        // 1. 正则匹配：双引号 "" 或中文引号 "" 里面的内容
        val pattern = Regex("""[""\u201C\u201D]([^""\u201C\u201D]+)[""\u201C\u201D]""")
        val matches = pattern.findAll(rawResponse)
            .map { it.groupValues[1].trim() }
            .filter { it.isNotBlank() && it.length >= MIN_SUGGESTION_LENGTH }
            .toList()

        Log.d(TAG, "提取到 ${matches.size} 条建议: ${matches.take(3)}")

        // 2. 命中判断
        return if (matches.isNotEmpty()) {
            // 成功提取到建议，返回建议列表
            matches
        } else {
            // 没提取到，返回原文本作为保底
            Log.d(TAG, "未提取到引号内容，使用原文保底")
            listOf(rawResponse)
        }
    }

    /**
     * 清洗并返回单条建议
     *
     * 适用于只需要一条建议的场景。
     * 如果提取到多条，返回第一条；否则返回原文。
     *
     * @param rawResponse AI返回的原始文本
     * @return 清洗后的单条建议
     */
    fun cleanSingleSuggestion(rawResponse: String): String {
        val suggestions = cleanSuggestion(rawResponse)
        return suggestions.firstOrNull() ?: rawResponse
    }

    /**
     * 清洗并格式化为展示文本
     *
     * 适用于需要展示多条建议的场景。
     * 如果提取到多条，用换行分隔；否则返回原文。
     *
     * @param rawResponse AI返回的原始文本
     * @param separator 分隔符，默认换行
     * @return 格式化后的展示文本
     */
    fun cleanAndFormat(rawResponse: String, separator: String = "\n"): String {
        val suggestions = cleanSuggestion(rawResponse)
        return if (suggestions.size > 1) {
            // 多条建议，编号展示
            suggestions.mapIndexed { index, s -> "${index + 1}. $s" }.joinToString(separator)
        } else {
            suggestions.firstOrNull() ?: rawResponse
        }
    }

    /**
     * 检查文本是否包含有效的引号建议
     *
     * @param text 要检查的文本
     * @return 是否包含引号内的有效建议
     */
    fun hasQuotedSuggestion(text: String): Boolean {
        val pattern = Regex("""[""\u201C\u201D]([^""\u201C\u201D]+)[""\u201C\u201D]""")
        return pattern.findAll(text)
            .any { it.groupValues[1].trim().length >= MIN_SUGGESTION_LENGTH }
    }

    /**
     * 移除AI常见的废话前缀
     *
     * AI有时会在建议前加上解释性文字，如：
     * - "我觉得这句不错："
     * - "建议你这样回复："
     * - "可以试试这个："
     *
     * @param text 原始文本
     * @return 移除废话前缀后的文本
     */
    fun removeExplanationPrefix(text: String): String {
        val prefixPatterns = listOf(
            Regex("""^(我觉得|建议你?|可以试试|推荐|不妨)[^：:""\u201C\u201D]*[：:]\s*"""),
            Regex("""^(这样说|换成|改成)[^：:""\u201C\u201D]*[：:]\s*"""),
            Regex("""^[^：:""\u201C\u201D]{0,20}(比较好|更好|更合适)[：:]\s*""")
        )

        var result = text.trim()
        for (pattern in prefixPatterns) {
            result = result.replace(pattern, "")
        }
        return result.trim()
    }

    /**
     * 智能清洗：结合引号提取和废话移除
     *
     * 这是推荐使用的主方法，综合了所有清洗策略。
     *
     * @param rawResponse AI返回的原始文本
     * @return 清洗后的建议文本
     */
    fun smartClean(rawResponse: String): String {
        if (rawResponse.isBlank()) {
            return ""
        }

        // 1. 先尝试提取引号内容
        val quotedSuggestions = cleanSuggestion(rawResponse)
        
        // 2. 如果提取到了引号内容，直接返回第一条
        if (quotedSuggestions.size > 1 || 
            (quotedSuggestions.size == 1 && quotedSuggestions[0] != rawResponse)) {
            return quotedSuggestions.first()
        }

        // 3. 没有引号内容，尝试移除废话前缀
        val cleaned = removeExplanationPrefix(rawResponse)
        
        // 4. 如果清洗后内容变化不大，返回原文
        return if (cleaned.length < rawResponse.length * 0.5) {
            // 清洗掉了太多内容，可能误删了，返回原文
            rawResponse
        } else {
            cleaned
        }
    }
}
