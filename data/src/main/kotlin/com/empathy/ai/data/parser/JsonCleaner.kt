package com.empathy.ai.data.parser

/**
 * JSON清洗器接口
 * 
 * 专门处理JSON清洗，移除格式化问题，修复常见错误
 * 
 * 职责：
 * - 移除Markdown代码块标记
 * - 修复JSON格式错误
 * - 处理Unicode编码和转义字符
 * - 提取有效的JSON对象
 */
interface JsonCleaner {
    
    /**
     * 清洗JSON字符串
     * 
     * @param json 原始JSON字符串
     * @param context 清洗上下文
     * @return 清洗后的JSON字符串
     */
    fun clean(json: String, context: CleaningContext = CleaningContext()): String
    
    /**
     * 验证JSON格式
     * 
     * @param json JSON字符串
     * @return 是否为有效JSON
     */
    fun isValid(json: String): Boolean
    
    /**
     * 提取JSON对象
     * 
     * @param text 可能包含JSON的文本
     * @return 提取的JSON对象字符串
     */
    fun extractJsonObject(text: String): String
}

/**
 * 清洗上下文
 */
data class CleaningContext(
    /**
     * 是否启用详细日志
     */
    val enableDetailedLogging: Boolean = false,
    
    /**
     * 是否启用Unicode修复
     */
    val enableUnicodeFix: Boolean = true,
    
    /**
     * 是否启用格式修复
     */
    val enableFormatFix: Boolean = true,
    
    /**
     * 是否启用模糊修复
     */
    val enableFuzzyFix: Boolean = false,
    
    /**
     * 自定义配置
     */
    val properties: Map<String, Any> = emptyMap()
) {
    /**
     * 添加属性
     */
    fun withProperty(key: String, value: Any): CleaningContext {
        return copy(properties = properties + (key to value))
    }
    
    /**
     * 启用详细日志
     */
    fun withDetailedLogging(enabled: Boolean = true): CleaningContext {
        return copy(enableDetailedLogging = enabled)
    }
    
    /**
     * 启用Unicode修复
     */
    fun withUnicodeFix(enabled: Boolean = true): CleaningContext {
        return copy(enableUnicodeFix = enabled)
    }
    
    /**
     * 启用格式修复
     */
    fun withFormatFix(enabled: Boolean = true): CleaningContext {
        return copy(enableFormatFix = enabled)
    }
    
    /**
     * 启用模糊修复
     */
    fun withFuzzyFix(enabled: Boolean = true): CleaningContext {
        return copy(enableFuzzyFix = enabled)
    }
}
