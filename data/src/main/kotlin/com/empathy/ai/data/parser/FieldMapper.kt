package com.empathy.ai.data.parser

import android.content.Context

/**
 * 字段映射器接口
 * 
 * 专门处理字段名映射，支持中英文字段名转换
 * 
 * 职责：
 * - 中英文字段名映射
 * - 模糊匹配支持
 * - 配置文件管理
 */
interface FieldMapper {
    
    /**
     * 映射字段名
     * 
     * @param json 原始JSON字符串
     * @param context 映射上下文
     * @return 映射后的JSON字符串
     */
    fun mapFields(json: String, context: MappingContext = MappingContext()): String
    
    /**
     * 添加字段映射
     * 
     * @param english 英文字段名
     * @param chinese 中文字段名列表
     */
    fun addMapping(english: String, chinese: List<String>)
    
    /**
     * 获取所有映射
     * 
     * @return 字段映射Map
     */
    fun getAllMappings(): Map<String, List<String>>
    
    /**
     * 清除所有映射
     */
    fun clearMappings()
}

/**
 * 映射上下文
 */
data class MappingContext(
    /**
     * 是否启用模糊匹配
     */
    val enableFuzzyMatching: Boolean = true,
    
    /**
     * 模糊匹配阈值
     */
    val fuzzyThreshold: Double = 0.8,
    
    /**
     * 是否启用详细日志
     */
    val enableDetailedLogging: Boolean = false,
    
    /**
     * Android上下文，用于加载配置文件
     */
    val androidContext: Context? = null,
    
    /**
     * 自定义属性
     */
    val properties: Map<String, Any> = emptyMap()
) {
    /**
     * 添加属性
     */
    fun withProperty(key: String, value: Any): MappingContext {
        return copy(properties = properties + (key to value))
    }
    
    /**
     * 启用模糊匹配
     */
    fun withFuzzyMatching(enabled: Boolean = true): MappingContext {
        return copy(enableFuzzyMatching = enabled)
    }
    
    /**
     * 设置模糊匹配阈值
     */
    fun withFuzzyThreshold(threshold: Double): MappingContext {
        return copy(fuzzyThreshold = threshold)
    }
    
    /**
     * 启用详细日志
     */
    fun withDetailedLogging(enabled: Boolean = true): MappingContext {
        return copy(enableDetailedLogging = enabled)
    }
    
    /**
     * 设置Android上下文
     */
    fun withAndroidContext(context: Context): MappingContext {
        return copy(androidContext = context)
    }
}
