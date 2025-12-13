package com.empathy.ai.data.parser

import android.util.Log

/**
 * Enhanced JSON Cleaner Implementation
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
