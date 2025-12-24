package com.empathy.ai.data.util

import android.util.Log

/**
 * AI响应清洗器
 *
 * 用于从AI返回的文本中提取有效的建议内容。
 */
object AiResponseCleaner {

    private const val TAG = "AiResponseCleaner"
    private const val MIN_SUGGESTION_LENGTH = 3

    fun cleanSuggestion(rawResponse: String): List<String> {
        if (rawResponse.isBlank()) {
            return emptyList()
        }

        val pattern = Regex("""[""\u201C\u201D]([^""\u201C\u201D]+)[""\u201C\u201D]""")
        val matches = pattern.findAll(rawResponse)
            .map { it.groupValues[1].trim() }
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

    fun cleanSingleSuggestion(rawResponse: String): String {
        val suggestions = cleanSuggestion(rawResponse)
        return suggestions.firstOrNull() ?: rawResponse
    }

    fun cleanAndFormat(rawResponse: String, separator: String = "\n"): String {
        val suggestions = cleanSuggestion(rawResponse)
        return if (suggestions.size > 1) {
            suggestions.mapIndexed { index, s -> "${index + 1}. $s" }.joinToString(separator)
        } else {
            suggestions.firstOrNull() ?: rawResponse
        }
    }

    fun hasQuotedSuggestion(text: String): Boolean {
        val pattern = Regex("""[""\u201C\u201D]([^""\u201C\u201D]+)[""\u201C\u201D]""")
        return pattern.findAll(text)
            .any { it.groupValues[1].trim().length >= MIN_SUGGESTION_LENGTH }
    }


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

    fun smartClean(rawResponse: String): String {
        if (rawResponse.isBlank()) {
            return ""
        }

        val quotedSuggestions = cleanSuggestion(rawResponse)
        
        if (quotedSuggestions.size > 1 || 
            (quotedSuggestions.size == 1 && quotedSuggestions[0] != rawResponse)) {
            return quotedSuggestions.first()
        }

        val cleaned = removeExplanationPrefix(rawResponse)
        
        return if (cleaned.length < rawResponse.length * 0.5) {
            rawResponse
        } else {
            cleaned
        }
    }
}
