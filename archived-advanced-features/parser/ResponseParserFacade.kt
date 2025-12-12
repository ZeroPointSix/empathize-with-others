package com.empathy.ai.data.parser

import android.content.Context
import android.util.Log
import com.empathy.ai.domain.model.AnalysisResult
import com.empathy.ai.domain.model.SafetyCheckResult
import com.empathy.ai.domain.usecase.ExtractedData

/**
 * 响应解析器门面类
 *
 * 提供统一的解析接口，封装底层解析器的复杂性
 * 作为AiRepositoryImpl和具体解析实现之间的适配层
 *
 * 职责：
 * 1. 提供简化的解析API
 * 2. 管理解析器生命周期
 * 3. 处理解析上下文传递
 * 4. 提供向后兼容性
 * 5. 支持增强版解析器（集成监控和学习系统）
 */
class ResponseParserFacade(
    private val aiResponseParser: AiResponseParser = AiResponseParserFactory.createDefaultStrategyBasedParser()
) {
    
    companion object {
        private const val TAG = "ResponseParserFacade"
    }
    
    /**
     * 解析AnalysisResult（向后兼容方法）
     * 
     * @param json AI返回的JSON字符串
     * @param modelName AI模型名称（可选，用于日志和统计）
     * @return 解析结果
     */
    fun parseAnalysisResult(json: String, modelName: String = "unknown"): Result<AnalysisResult> {
        val context = ParsingContext(
            operationId = generateOperationId("parseAnalysisResult"),
            modelName = modelName,
            operationType = "analysis",
            enableDetailedLogging = Log.isLoggable(TAG, Log.DEBUG)
        )
        
        return aiResponseParser.parseAnalysisResult(json, context)
    }
    
    /**
     * 解析SafetyCheckResult（向后兼容方法）
     * 
     * @param json AI返回的JSON字符串
     * @param modelName AI模型名称（可选，用于日志和统计）
     * @return 解析结果
     */
    fun parseSafetyCheckResult(json: String, modelName: String = "unknown"): Result<SafetyCheckResult> {
        val context = ParsingContext(
            operationId = generateOperationId("parseSafetyCheckResult"),
            modelName = modelName,
            operationType = "safety_check",
            enableDetailedLogging = Log.isLoggable(TAG, Log.DEBUG)
        )
        
        return aiResponseParser.parseSafetyCheckResult(json, context)
    }
    
    /**
     * 解析ExtractedData（向后兼容方法）
     * 
     * @param json AI返回的JSON字符串
     * @param modelName AI模型名称（可选，用于日志和统计）
     * @return 解析结果
     */
    fun parseExtractedData(json: String, modelName: String = "unknown"): Result<ExtractedData> {
        val context = ParsingContext(
            operationId = generateOperationId("parseExtractedData"),
            modelName = modelName,
            operationType = "extraction",
            enableDetailedLogging = Log.isLoggable(TAG, Log.DEBUG)
        )
        
        return aiResponseParser.parseExtractedData(json, context)
    }
    
    /**
     * 通用解析方法（向后兼容方法）
     * 
     * @param json AI返回的JSON字符串
     * @param targetType 目标类型
     * @param modelName AI模型名称（可选，用于日志和统计）
     * @return 解析结果
     */
    inline fun <reified T> parse(json: String, modelName: String = "unknown"): Result<T> {
        val context = ParsingContext(
            operationId = generateOperationId("parse"),
            modelName = modelName,
            operationType = when (T::class) {
                AnalysisResult::class -> "analysis"
                SafetyCheckResult::class -> "safety_check"
                ExtractedData::class -> "extraction"
                else -> "generic"
            },
            enableDetailedLogging = Log.isLoggable(TAG, Log.DEBUG)
        )
        
        return aiResponseParser.parse(json, T::class.java, context)
    }
    
    /**
     * 带上下文的解析方法（新API）
     * 
     * @param json AI返回的JSON字符串
     * @param context 解析上下文
     * @return 解析结果
     */
    fun parseAnalysisResultWithContext(json: String, context: ParsingContext): Result<AnalysisResult> {
        return aiResponseParser.parseAnalysisResult(json, context)
    }
    
    /**
     * 带上下文的解析方法（新API）
     * 
     * @param json AI返回的JSON字符串
     * @param context 解析上下文
     * @return 解析结果
     */
    fun parseSafetyCheckResultWithContext(json: String, context: ParsingContext): Result<SafetyCheckResult> {
        return aiResponseParser.parseSafetyCheckResult(json, context)
    }
    
    /**
     * 带上下文的解析方法（新API）
     * 
     * @param json AI返回的JSON字符串
     * @param context 解析上下文
     * @return 解析结果
     */
    fun parseExtractedDataWithContext(json: String, context: ParsingContext): Result<ExtractedData> {
        return aiResponseParser.parseExtractedData(json, context)
    }
    
    /**
     * 生成操作ID
     */
    private fun generateOperationId(operation: String): String {
        return "${operation}_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }
    
    /**
     * 获取底层解析器（用于高级用法）
     */
    fun getUnderlyingParser(): AiResponseParser {
        return aiResponseParser
    }
    
    /**
     * 创建自定义配置的门面实例
     */
    companion object {
        /**
         * 创建默认门面
         */
        fun createDefault(): ResponseParserFacade {
            return ResponseParserFacade(
                AiResponseParserFactory.createDefaultStrategyBasedParser()
            )
        }
        
        /**
         * 创建增强版默认门面（集成监控和学习系统）
         */
        fun createEnhancedDefault(context: Context): ResponseParserFacade {
            return ResponseParserFacade(
                AiResponseParserFactory.createParserByType(AiResponseParserFactory.ParserType.ENHANCED_DEFAULT, context)
            )
        }
        
        /**
         * 创建高性能门面
         */
        fun createHighPerformance(): ResponseParserFacade {
            return ResponseParserFacade(
                AiResponseParserFactory.createParserByType(AiResponseParserFactory.ParserType.HIGH_PERFORMANCE)
            )
        }
        
        /**
         * 创建增强版高性能门面
         */
        fun createEnhancedHighPerformance(context: Context): ResponseParserFacade {
            return ResponseParserFacade(
                AiResponseParserFactory.createParserByType(AiResponseParserFactory.ParserType.ENHANCED_HIGH_PERFORMANCE, context)
            )
        }
        
        /**
         * 创建调试模式门面
         */
        fun createDebugMode(): ResponseParserFacade {
            return ResponseParserFacade(
                AiResponseParserFactory.createParserByType(AiResponseParserFactory.ParserType.DEBUG_MODE)
            )
        }
        
        /**
         * 创建增强版调试模式门面
         */
        fun createEnhancedDebugMode(context: Context): ResponseParserFacade {
            return ResponseParserFacade(
                AiResponseParserFactory.createParserByType(AiResponseParserFactory.ParserType.ENHANCED_DEBUG_MODE, context)
            )
        }
        
        /**
         * 创建自定义门面
         */
        fun createCustom(
            jsonCleaner: JsonCleaner? = null,
            fieldMapper: FieldMapper? = null,
            fallbackHandler: FallbackHandler? = null
        ): ResponseParserFacade {
            val parser = AiResponseParserFactory.createCustomStrategyBasedParser(
                jsonCleaner = jsonCleaner,
                fieldMapper = fieldMapper,
                fallbackHandler = fallbackHandler
            )
            return ResponseParserFacade(parser)
        }
        
        /**
         * 创建增强版自定义门面
         */
        fun createEnhancedCustom(
            context: Context,
            jsonCleaner: JsonCleaner? = null,
            fieldMapper: FieldMapper? = null,
            fallbackHandler: FallbackHandler? = null
        ): ResponseParserFacade {
            val parser = AiResponseParserFactory.createCustomEnhancedStrategyBasedParser(
                context = context,
                jsonCleaner = jsonCleaner,
                fieldMapper = fieldMapper,
                fallbackHandler = fallbackHandler
            )
            return ResponseParserFacade(parser)
        }
        
        /**
         * 创建带有自定义字段映射的门面
         */
        fun createWithCustomFieldMapping(
            customMappings: Map<String, List<String>>,
            fuzzyThreshold: Double = 0.7
        ): ResponseParserFacade {
            val parser = AiResponseParserFactory.createParserWithCustomFieldMapping(
                customMappings = customMappings,
                fuzzyThreshold = fuzzyThreshold
            )
            return ResponseParserFacade(parser)
        }
        
        /**
         * 创建增强版带有自定义字段映射的门面
         */
        fun createEnhancedWithCustomFieldMapping(
            context: Context,
            customMappings: Map<String, List<String>>,
            fuzzyThreshold: Double = 0.7
        ): ResponseParserFacade {
            val parser = AiResponseParserFactory.createEnhancedParserWithCustomFieldMapping(
                context = context,
                customMappings = customMappings,
                fuzzyThreshold = fuzzyThreshold
            )
            return ResponseParserFacade(parser)
        }
    }
}