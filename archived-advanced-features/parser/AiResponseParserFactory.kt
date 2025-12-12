package com.empathy.ai.data.parser

import android.content.Context
import com.squareup.moshi.Moshi

/**
 * AI响应解析器工厂类
 *
 * 负责创建和配置不同类型的解析器，支持依赖注入和策略选择
 */
object AiResponseParserFactory {
    
    /**
     * 创建默认的基于策略的解析器
     */
    fun createDefaultStrategyBasedParser(): AiResponseParser {
        val jsonCleaner = EnhancedJsonCleaner()
        val fieldMapper = SmartFieldMapper()
        val fallbackHandler = MultiLevelFallbackHandler()
        val moshi = createDefaultMoshi()
        
        return StrategyBasedAiResponseParser(
            jsonCleaner = jsonCleaner,
            fieldMapper = fieldMapper,
            fallbackHandler = fallbackHandler,
            moshi = moshi
        )
    }
    
    /**
     * 创建增强版基于策略的解析器（集成监控和学习系统）
     */
    fun createEnhancedStrategyBasedParser(context: Context): AiResponseParser {
        val jsonCleaner = EnhancedJsonCleaner()
        val fieldMapper = SmartFieldMapper()
        val fallbackHandler = MultiLevelFallbackHandler()
        val moshi = createDefaultMoshi()
        
        return EnhancedStrategyBasedAiResponseParser(
            context = context,
            jsonCleaner = jsonCleaner,
            fieldMapper = fieldMapper,
            fallbackHandler = fallbackHandler,
            moshi = moshi
        )
    }
    
    /**
     * 创建自定义配置的基于策略的解析器
     */
    fun createCustomStrategyBasedParser(
        jsonCleaner: JsonCleaner? = null,
        fieldMapper: FieldMapper? = null,
        fallbackHandler: FallbackHandler? = null,
        moshi: Moshi? = null
    ): AiResponseParser {
        return StrategyBasedAiResponseParser(
            jsonCleaner = jsonCleaner ?: EnhancedJsonCleaner(),
            fieldMapper = fieldMapper ?: SmartFieldMapper(),
            fallbackHandler = fallbackHandler ?: MultiLevelFallbackHandler(),
            moshi = moshi ?: createDefaultMoshi()
        )
    }
    
    /**
     * 创建自定义配置的增强版基于策略的解析器
     */
    fun createCustomEnhancedStrategyBasedParser(
        context: Context,
        jsonCleaner: JsonCleaner? = null,
        fieldMapper: FieldMapper? = null,
        fallbackHandler: FallbackHandler? = null,
        moshi: Moshi? = null
    ): AiResponseParser {
        return EnhancedStrategyBasedAiResponseParser(
            context = context,
            jsonCleaner = jsonCleaner ?: EnhancedJsonCleaner(),
            fieldMapper = fieldMapper ?: SmartFieldMapper(),
            fallbackHandler = fallbackHandler ?: MultiLevelFallbackHandler(),
            moshi = moshi ?: createDefaultMoshi()
        )
    }
    
    /**
     * 创建带有特定字段映射的解析器
     */
    fun createParserWithCustomFieldMapping(
        customMappings: Map<String, List<String>>,
        fuzzyThreshold: Double = 0.7
    ): AiResponseParser {
        val jsonCleaner = EnhancedJsonCleaner()
        val fieldMapper = SmartFieldMapper(
            fuzzyThreshold = fuzzyThreshold,
            enableLearning = true
        )
        
        // 添加自定义映射
        customMappings.forEach { (english, chinese) ->
            fieldMapper.addMapping(english, chinese)
        }
        
        val fallbackHandler = MultiLevelFallbackHandler()
        val moshi = createDefaultMoshi()
        
        return StrategyBasedAiResponseParser(
            jsonCleaner = jsonCleaner,
            fieldMapper = fieldMapper,
            fallbackHandler = fallbackHandler,
            moshi = moshi
        )
    }
    
    /**
     * 创建带有特定字段映射的增强版解析器
     */
    fun createEnhancedParserWithCustomFieldMapping(
        context: Context,
        customMappings: Map<String, List<String>>,
        fuzzyThreshold: Double = 0.7
    ): AiResponseParser {
        val jsonCleaner = EnhancedJsonCleaner()
        val fieldMapper = SmartFieldMapper(
            fuzzyThreshold = fuzzyThreshold,
            enableLearning = true
        )
        
        // 添加自定义映射
        customMappings.forEach { (english, chinese) ->
            fieldMapper.addMapping(english, chinese)
        }
        
        val fallbackHandler = MultiLevelFallbackHandler()
        val moshi = createDefaultMoshi()
        
        return EnhancedStrategyBasedAiResponseParser(
            context = context,
            jsonCleaner = jsonCleaner,
            fieldMapper = fieldMapper,
            fallbackHandler = fallbackHandler,
            moshi = moshi
        )
    }
    
    /**
     * 创建高性能解析器（禁用详细日志和学习功能）
     */
    fun createHighPerformanceParser(): AiResponseParser {
        val jsonCleaner = EnhancedJsonCleaner()
        val fieldMapper = SmartFieldMapper(
            fuzzyThreshold = 0.8,
            enableLearning = false
        )
        val fallbackHandler = MultiLevelFallbackHandler()
        val moshi = createDefaultMoshi()
        
        return StrategyBasedAiResponseParser(
            jsonCleaner = jsonCleaner,
            fieldMapper = fieldMapper,
            fallbackHandler = fallbackHandler,
            moshi = moshi
        )
    }
    
    /**
     * 创建增强版高性能解析器
     */
    fun createEnhancedHighPerformanceParser(context: Context): AiResponseParser {
        val jsonCleaner = EnhancedJsonCleaner()
        val fieldMapper = SmartFieldMapper(
            fuzzyThreshold = 0.8,
            enableLearning = false
        )
        val fallbackHandler = MultiLevelFallbackHandler()
        val moshi = createDefaultMoshi()
        
        return EnhancedStrategyBasedAiResponseParser(
            context = context,
            jsonCleaner = jsonCleaner,
            fieldMapper = fieldMapper,
            fallbackHandler = fallbackHandler,
            moshi = moshi
        )
    }
    
    /**
     * 创建调试模式解析器（启用详细日志）
     */
    fun createDebugModeParser(): AiResponseParser {
        val jsonCleaner = EnhancedJsonCleaner()
        val fieldMapper = SmartFieldMapper(
            fuzzyThreshold = 0.6,
            enableLearning = true
        )
        val fallbackHandler = MultiLevelFallbackHandler()
        val moshi = createDefaultMoshi()
        
        return StrategyBasedAiResponseParser(
            jsonCleaner = jsonCleaner,
            fieldMapper = fieldMapper,
            fallbackHandler = fallbackHandler,
            moshi = moshi
        )
    }
    
    /**
     * 创建增强版调试模式解析器
     */
    fun createEnhancedDebugModeParser(context: Context): AiResponseParser {
        val jsonCleaner = EnhancedJsonCleaner()
        val fieldMapper = SmartFieldMapper(
            fuzzyThreshold = 0.6,
            enableLearning = true
        )
        val fallbackHandler = MultiLevelFallbackHandler()
        val moshi = createDefaultMoshi()
        
        return EnhancedStrategyBasedAiResponseParser(
            context = context,
            jsonCleaner = jsonCleaner,
            fieldMapper = fieldMapper,
            fallbackHandler = fallbackHandler,
            moshi = moshi
        )
    }
    
    /**
     * 创建默认的Moshi实例
     */
    private fun createDefaultMoshi(): Moshi {
        return Moshi.Builder()
            .build()
    }
    
    /**
     * 解析器类型枚举
     */
    enum class ParserType {
        DEFAULT,              // 默认解析器
        HIGH_PERFORMANCE,      // 高性能解析器
        DEBUG_MODE,           // 调试模式解析器
        CUSTOM,               // 自定义解析器
        ENHANCED_DEFAULT,      // 增强版默认解析器
        ENHANCED_HIGH_PERFORMANCE, // 增强版高性能解析器
        ENHANCED_DEBUG_MODE,   // 增强版调试模式解析器
        ENHANCED_CUSTOM       // 增强版自定义解析器
    }
    
    /**
     * 根据类型创建解析器
     */
    fun createParserByType(type: ParserType, context: Context? = null): AiResponseParser {
        return when (type) {
            ParserType.DEFAULT -> createDefaultStrategyBasedParser()
            ParserType.HIGH_PERFORMANCE -> createHighPerformanceParser()
            ParserType.DEBUG_MODE -> createDebugModeParser()
            ParserType.CUSTOM -> createDefaultStrategyBasedParser() // 默认实现，可以通过其他方法自定义
            ParserType.ENHANCED_DEFAULT -> {
                requireNotNull(context) { "增强版解析器需要Context参数" }
                createEnhancedStrategyBasedParser(context)
            }
            ParserType.ENHANCED_HIGH_PERFORMANCE -> {
                requireNotNull(context) { "增强版解析器需要Context参数" }
                createEnhancedHighPerformanceParser(context)
            }
            ParserType.ENHANCED_DEBUG_MODE -> {
                requireNotNull(context) { "增强版解析器需要Context参数" }
                createEnhancedDebugModeParser(context)
            }
            ParserType.ENHANCED_CUSTOM -> {
                requireNotNull(context) { "增强版解析器需要Context参数" }
                createEnhancedStrategyBasedParser(context)
            }
        }
    }
}