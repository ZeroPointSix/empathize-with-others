package com.empathy.ai.data.parser

import com.empathy.ai.domain.model.AnalysisResult
import com.empathy.ai.domain.model.ExtractedData
import com.empathy.ai.domain.model.SafetyCheckResult

/**
 * AI响应解析器接口
 * 
 * 定义统一的解析契约，支持多种数据类型的解析
 * 
 * 设计原则：
 * - 单一职责：专注于解析逻辑
 * - 开闭原则：对扩展开放，对修改封闭
 * - 依赖倒置：依赖抽象而非具体实现
 */
interface AiResponseParser {
    
    /**
     * 解析分析结果
     * 
     * @param json AI返回的JSON字符串
     * @param context 解析上下文
     * @return 解析结果
     */
    fun parseAnalysisResult(json: String, context: ParsingContext = ParsingContext()): Result<AnalysisResult>
    
    /**
     * 解析安全检查结果
     * 
     * @param json AI返回的JSON字符串
     * @param context 解析上下文
     * @return 解析结果
     */
    fun parseSafetyCheckResult(json: String, context: ParsingContext = ParsingContext()): Result<SafetyCheckResult>
    
    /**
     * 解析提取数据
     * 
     * @param json AI返回的JSON字符串
     * @param context 解析上下文
     * @return 解析结果
     */
    fun parseExtractedData(json: String, context: ParsingContext = ParsingContext()): Result<ExtractedData>
    
    /**
     * 通用解析方法
     * 
     * @param json AI返回的JSON字符串
     * @param targetType 目标类型
     * @param context 解析上下文
     * @return 解析结果
     */
    fun <T> parse(json: String, targetType: Class<T>, context: ParsingContext = ParsingContext()): Result<T>
}

/**
 * 解析上下文
 * 
 * 包含解析过程中需要的上下文信息
 */
data class ParsingContext(
    /**
     * 操作ID，用于日志追踪
     */
    val operationId: String = "parse_${System.currentTimeMillis()}",
    
    /**
     * AI模型名称
     */
    val modelName: String = "unknown",
    
    /**
     * 操作类型
     */
    val operationType: String = "unknown",
    
    /**
     * 是否启用详细日志
     */
    val enableDetailedLogging: Boolean = false,
    
    /**
     * 自定义属性
     */
    val properties: Map<String, Any> = emptyMap()
) {
    /**
     * 添加属性
     */
    fun withProperty(key: String, value: Any): ParsingContext {
        return copy(properties = properties + (key to value))
    }
    
    /**
     * 设置模型名称
     */
    fun withModelName(modelName: String): ParsingContext {
        return copy(modelName = modelName)
    }
    
    /**
     * 设置操作类型
     */
    fun withOperationType(operationType: String): ParsingContext {
        return copy(operationType = operationType)
    }
    
    /**
     * 启用详细日志
     */
    fun withDetailedLogging(enabled: Boolean = true): ParsingContext {
        return copy(enableDetailedLogging = enabled)
    }
}
