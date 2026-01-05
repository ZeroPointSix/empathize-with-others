package com.empathy.ai.data.parser

import android.util.Log

/**
 * 增强型JSON清理器
 *
 * 提供多层次的JSON修复和验证功能，处理AI返回的各种异常JSON格式。
 *
 * 业务背景:
 *   - AI返回的JSON可能包含Markdown代码块、Unicode转义、格式错误等
 *   - 需要在不破坏有效数据的前提下修复这些问题
 *   - 增强的日志记录便于调试和追踪问题
 *
 * 设计决策:
 *   - 清理流程按序执行，每步独立可测试
 *   - 保留原始JSON作为兜底，清理失败时返回原值
 *   - 使用CleaningContext控制各功能的开关
 *
 * 清理流程:
 *   1. removeMarkdownBlocks - 移除Markdown代码块包装
 *   2. fixUnicodeAndEscapeCharacters - 修复Unicode转义
 *   3. extractJsonObject - 提取JSON对象
 *   4. fixCommonJsonErrors - 修复常见格式错误
 *   5. validateAndFixJson - 最终验证和修复
 *
 * @see JsonCleaner 基础清理接口
 * @see CleaningContext 清理配置上下文
 */
class EnhancedJsonCleaner : JsonCleaner {
    
    companion object {
        private const val TAG = "EnhancedJsonCleaner"
    }
    
    override fun clean(json: String, context: CleaningContext): String {
        if (context.enableDetailedLogging) {
            Log.d(TAG, "Starting JSON cleaning, original length: ${json.length}")
        }
        
        try {
            var result = removeMarkdownBlocks(json)
            
            if (context.enableUnicodeFix) {
                result = fixUnicodeAndEscapeCharacters(result)
            }
            
            result = extractJsonObjectEnhanced(result)
            
            if (context.enableFormatFix) {
                result = fixCommonJsonErrors(result)
            }
            
            if (context.enableFuzzyFix) {
                result = validateAndFixJson(result)
            }
            
            if (context.enableDetailedLogging) {
                Log.d(TAG, "JSON cleaning completed, processed length: ${result.length}")
            }
            
            return result
        } catch (e: Exception) {
            Log.e(TAG, "JSON cleaning failed", e)
            return json
        }
    }
    
    /**
     * JSON有效性验证
     *
     * [Algorithm] 使用括号配对验证JSON结构
     * 步骤：
     *   1. 查找首尾花括号位置
     *   2. 统计括号数量是否相等
     *   3. 返回验证结果
     *
     * 注意：此方法不解析JSON内容，仅验证结构。
     * 更严格的验证需要使用JSON解析器。
     *
     * @param json 待验证的JSON字符串
     * @return true表示结构有效，false表示无效
     */
    override fun isValid(json: String): Boolean {
        return try {
            val startIndex = json.indexOf("{")
            val endIndex = json.lastIndexOf("}")
            
            if (startIndex == -1 || endIndex == -1 || endIndex <= startIndex) {
                return false
            }
            
            val bracketCount = json.substring(startIndex, endIndex + 1).count { it == '{' }
            val closeBracketCount = json.substring(startIndex, endIndex + 1).count { it == '}' }
            
            bracketCount == closeBracketCount
        } catch (e: Exception) {
            false
        }
    }
    
    override fun extractJsonObject(text: String): String {
        return extractJsonObjectEnhanced(text)
    }
    
    private fun removeMarkdownBlocks(json: String): String {
        val trimmed = json.trim()
        
        return when {
            trimmed.startsWith("```json") -> {
                val content = trimmed.removePrefix("```json")
                val endIndex = content.lastIndexOf("```")
                if (endIndex != -1) content.substring(0, endIndex).trim() else content.trim()
            }
            trimmed.startsWith("```") -> {
                val content = trimmed.removePrefix("```")
                val endIndex = content.lastIndexOf("```")
                if (endIndex != -1) content.substring(0, endIndex).trim() else content.trim()
            }
            else -> trimmed
        }
    }
    
    private fun fixUnicodeAndEscapeCharacters(json: String): String {
        var result = json
        
        // 只处理 Unicode 转义序列，不要破坏 JSON 的合法转义字符
        // JSON 中的 \" \n \t \\ 都是合法的，不应该被替换
        result = result.replace(Regex("\\\\u([0-9a-fA-F]{4})")) { 
            val codePoint = it.groupValues[1].toInt(16)
            codePoint.toChar().toString()
        }
        
        // 注意：不要替换 \" \n \t \\，这些是 JSON 字符串中的合法转义字符
        // 之前的代码会破坏 JSON 格式：
        // - result.replace("\\\"", "\"")  // 错误！会破坏字符串中的引号转义
        // - result.replace("\\n", "\n")   // 错误！会破坏字符串中的换行转义
        // - result.replace("\\t", "\t")   // 错误！会破坏字符串中的制表符转义
        // - result.replace("\\\\", "\\")  // 错误！会破坏字符串中的反斜杠转义
        
        return result
    }
    
    /**
     * 提取JSON对象
     *
     * [Algorithm] 括号计数算法
     * 从第一个{开始，统计嵌套深度，遇到}递减，
     * 深度为0时说明找到了完整的JSON对象。
     *
     * 设计权衡：
     *   - 不使用正则表达式，避免复杂嵌套场景处理失败
     *   - 不解析具体内容，仅提取结构
     *
     * @param text 包含JSON的文本
     * @return 提取的JSON对象，提取失败返回"{}"
     */
    private fun extractJsonObjectEnhanced(json: String): String {
        val startIndex = json.indexOf("{")
        if (startIndex == -1) return "{}"
        
        var braceCount = 0
        var endIndex = -1
        
        for (i in startIndex until json.length) {
            when (json[i]) {
                '{' -> braceCount++
                '}' -> {
                    braceCount--
                    if (braceCount == 0) {
                        endIndex = i
                        break
                    }
                }
            }
        }
        
        return if (endIndex != -1) {
            json.substring(startIndex, endIndex + 1)
        } else {
            json.substring(startIndex) + "}"
        }
    }
    
    private fun fixCommonJsonErrors(json: String): String {
        val builder = StringBuilder(json)
        fixMissingCommas(builder)
        return builder.toString()
    }
    
    private fun fixMissingCommas(builder: StringBuilder) {
        var i = 0
        while (i < builder.length - 1) {
            if (builder[i] == '}' && builder[i + 1] == '"' && i > 0 && builder[i - 1] != ',') {
                builder.insert(i + 1, ',')
                i++
            }
            i++
        }
    }
    
    private fun validateAndFixJson(json: String): String {
        if (isValid(json)) return json
        
        var result = tryFixBracketMismatch(json)
        if (isValid(result)) return result
        
        result = tryMinimalJsonFix(result)
        return result
    }
    
    /**
     * 尝试修复括号不匹配
     *
     * [Algorithm] 括号数量平衡算法
     * 场景：
     *   - 缺少右括号：补充相应数量的}
     *   - 缺少左括号：移除多余的}
     *
     * 设计权衡：
     *   - 简单的数量修复，不处理嵌套不匹配的情况
     *   - 复杂的嵌套问题交给validateAndFixJson的兜底逻辑
     *
     * @param json 可能有括号问题的JSON
     * @return 修复后的JSON
     */
    private fun tryFixBracketMismatch(json: String): String {
        val openCount = json.count { it == '{' }
        val closeCount = json.count { it == '}' }
        
        return when {
            openCount > closeCount -> json + "}".repeat(openCount - closeCount)
            closeCount > openCount -> {
                var result = json
                repeat(closeCount - openCount) {
                    val lastIndex = result.lastIndexOf('}')
                    if (lastIndex != -1) {
                        result = result.substring(0, lastIndex) + result.substring(lastIndex + 1)
                    }
                }
                result
            }
            else -> json
        }
    }
    
    private fun tryMinimalJsonFix(json: String): String {
        val result = json.trim()
        val startIndex = result.indexOf("{")
        if (startIndex != -1) {
            val endIndex = result.lastIndexOf("}")
            if (endIndex != -1 && endIndex > startIndex) {
                return result.substring(startIndex, endIndex + 1)
            }
        }
        return "{}"
    }
}
