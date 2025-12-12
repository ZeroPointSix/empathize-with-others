package com.empathy.ai.data.parser

import android.util.Log
import com.empathy.ai.domain.model.AnalysisResult
import com.empathy.ai.domain.model.SafetyCheckResult
import com.empathy.ai.domain.usecase.ExtractedData
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

/**
 * 基于策略的AI响应解析器实现
 * 
 * 使用策略模式实现不同解析策略，支持智能降级
 * 
 * 策略：
 * - StandardParsingStrategy: 标准解析策略
 * - FallbackParsingStrategy: 容错解析策略
 * - IntelligentParsingStrategy: 智能解析策略
 */
class StrategyBasedAiResponseParser(
    private val jsonCleaner: JsonCleaner,
    private val fieldMapper: FieldMapper,
    private val fallbackHandler: FallbackHandler,
    private val moshi: Moshi = Moshi.Builder().build()
) : AiResponseParser {
    
    companion object {
        private const val TAG = "StrategyBasedAiResponseParser"
    }
    
    override fun parseAnalysisResult(json: String, context: ParsingContext): Result<AnalysisResult> {
        val operationId = context.operationId
        Log.d(TAG, "开始解析AnalysisResult [ID: $operationId]")
        
        return try {
            // 清洗JSON
            val cleanedJson = jsonCleaner.clean(json, context.toCleaningContext())
            
            // 尝试标准解析策略
            val standardResult = StandardParsingStrategy.parseAnalysisResult(cleanedJson, moshi)
            if (standardResult.isSuccess) {
                Log.i(TAG, "标准解析策略成功 [ID: $operationId]")
                return standardResult
            }
            
            // 尝试容错解析策略
            val fallbackResult = FallbackParsingStrategy.parseAnalysisResult(cleanedJson, moshi)
            if (fallbackResult.isSuccess) {
                Log.i(TAG, "容错解析策略成功 [ID: $operationId]")
                return fallbackResult
            }
            
            // 尝试智能解析策略
            val intelligentResult = IntelligentParsingStrategy.parseAnalysisResult(cleanedJson, moshi)
            if (intelligentResult.isSuccess) {
                Log.i(TAG, "智能解析策略成功 [ID: $operationId]")
                return intelligentResult
            }
            
            // 所有策略都失败，使用降级处理器
            Log.w(TAG, "所有解析策略失败，使用降级处理器 [ID: $operationId]")
            val fallbackContext = context.toFallbackContext(json)
            val fallbackResult = fallbackHandler.handleParsingFailure(
                Exception("所有解析策略失败"),
                AnalysisResult::class.java,
                fallbackContext
            )
            
            when (fallbackResult) {
                is FallbackResult.Success -> {
                    Log.i(TAG, "降级处理成功 [ID: $operationId], 策略: ${fallbackResult.strategy}")
                    return Result.success(fallbackResult.data)
                }
                is FallbackResult.Failure -> {
                    Log.e(TAG, "降级处理失败 [ID: $operationId]", fallbackResult.error)
                    return Result.failure(fallbackResult.error)
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "解析AnalysisResult时发生异常 [ID: $operationId]", e)
            return Result.failure(e)
        }
    }
    
    override fun parseSafetyCheckResult(json: String, context: ParsingContext): Result<SafetyCheckResult> {
        val operationId = context.operationId
        Log.d(TAG, "开始解析SafetyCheckResult [ID: $operationId]")
        
        return try {
            // 清洗JSON
            val cleanedJson = jsonCleaner.clean(json, context.toCleaningContext())
            
            // 尝试标准解析策略
            val standardResult = StandardParsingStrategy.parseSafetyCheckResult(cleanedJson, moshi)
            if (standardResult.isSuccess) {
                Log.i(TAG, "标准解析策略成功 [ID: $operationId]")
                return standardResult
            }
            
            // 尝试容错解析策略
            val fallbackResult = FallbackParsingStrategy.parseSafetyCheckResult(cleanedJson, moshi)
            if (fallbackResult.isSuccess) {
                Log.i(TAG, "容错解析策略成功 [ID: $operationId]")
                return fallbackResult
            }
            
            // 尝试智能解析策略
            val intelligentResult = IntelligentParsingStrategy.parseSafetyCheckResult(cleanedJson, moshi)
            if (intelligentResult.isSuccess) {
                Log.i(TAG, "智能解析策略成功 [ID: $operationId]")
                return intelligentResult
            }
            
            // 所有策略都失败，使用降级处理器
            Log.w(TAG, "所有解析策略失败，使用降级处理器 [ID: $operationId]")
            val fallbackContext = context.toFallbackContext(json)
            val fallbackResult = fallbackHandler.handleParsingFailure(
                Exception("所有解析策略失败"),
                SafetyCheckResult::class.java,
                fallbackContext
            )
            
            when (fallbackResult) {
                is FallbackResult.Success -> {
                    Log.i(TAG, "降级处理成功 [ID: $operationId], 策略: ${fallbackResult.strategy}")
                    return Result.success(fallbackResult.data)
                }
                is FallbackResult.Failure -> {
                    Log.e(TAG, "降级处理失败 [ID: $operationId]", fallbackResult.error)
                    return Result.failure(fallbackResult.error)
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "解析SafetyCheckResult时发生异常 [ID: $operationId]", e)
            return Result.failure(e)
        }
    }
    
    override fun parseExtractedData(json: String, context: ParsingContext): Result<ExtractedData> {
        val operationId = context.operationId
        Log.d(TAG, "开始解析ExtractedData [ID: $operationId]")
        
        return try {
            // 清洗JSON
            val cleanedJson = jsonCleaner.clean(json, context.toCleaningContext())
            
            // 尝试标准解析策略
            val standardResult = StandardParsingStrategy.parseExtractedData(cleanedJson, moshi)
            if (standardResult.isSuccess) {
                Log.i(TAG, "标准解析策略成功 [ID: $operationId]")
                return standardResult
            }
            
            // 尝试容错解析策略
            val fallbackResult = FallbackParsingStrategy.parseExtractedData(cleanedJson, moshi)
            if (fallbackResult.isSuccess) {
                Log.i(TAG, "容错解析策略成功 [ID: $operationId]")
                return fallbackResult
            }
            
            // 尝试智能解析策略
            val intelligentResult = IntelligentParsingStrategy.parseExtractedData(cleanedJson, moshi)
            if (intelligentResult.isSuccess) {
                Log.i(TAG, "智能解析策略成功 [ID: $operationId]")
                return intelligentResult
            }
            
            // 所有策略都失败，使用降级处理器
            Log.w(TAG, "所有解析策略失败，使用降级处理器 [ID: $operationId]")
            val fallbackContext = context.toFallbackContext(json)
            val fallbackResult = fallbackHandler.handleParsingFailure(
                Exception("所有解析策略失败"),
                ExtractedData::class.java,
                fallbackContext
            )
            
            when (fallbackResult) {
                is FallbackResult.Success -> {
                    Log.i(TAG, "降级处理成功 [ID: $operationId], 策略: ${fallbackResult.strategy}")
                    return Result.success(fallbackResult.data)
                }
                is FallbackResult.Failure -> {
                    Log.e(TAG, "降级处理失败 [ID: $operationId]", fallbackResult.error)
                    return Result.failure(fallbackResult.error)
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "解析ExtractedData时发生异常 [ID: $operationId]", e)
            return Result.failure(e)
        }
    }
    
    override fun <T> parse(json: String, targetType: Class<T>, context: ParsingContext): Result<T> {
        val operationId = context.operationId
        Log.d(TAG, "开始通用解析 [ID: $operationId], 类型: ${targetType.simpleName}")
        
        return try {
            // 清洗JSON
            val cleanedJson = jsonCleaner.clean(json, context.toCleaningContext())
            
            // 根据目标类型选择适当的解析策略
            when (targetType) {
                AnalysisResult::class.java -> {
                    val result = parseAnalysisResult(cleanedJson, context)
                    @Suppress("UNCHECKED_CAST")
                    result as Result<T>
                }
                
                SafetyCheckResult::class.java -> {
                    val result = parseSafetyCheckResult(cleanedJson, context)
                    @Suppress("UNCHECKED_CAST")
                    result as Result<T>
                }
                
                ExtractedData::class.java -> {
                    val result = parseExtractedData(cleanedJson, context)
                    @Suppress("UNCHECKED_CAST")
                    result as Result<T>
                }
                
                else -> {
                    Log.w(TAG, "不支持的类型: ${targetType.simpleName}")
                    Result.failure(IllegalArgumentException("不支持的类型: ${targetType.simpleName}"))
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "通用解析时发生异常 [ID: $operationId]", e)
            Result.failure(e)
        }
    }
    
    /**
     * 扩展函数：将ParsingContext转换为CleaningContext
     */
    private fun ParsingContext.toCleaningContext(): CleaningContext {
        return CleaningContext(
            enableDetailedLogging = this.enableDetailedLogging,
            enableUnicodeFix = true,
            enableFormatFix = true,
            enableFuzzyFix = true,
            properties = this.properties
        )
    }
    
    /**
     * 扩展函数：将ParsingContext转换为FallbackContext
     */
    private fun ParsingContext.toFallbackContext(json: String): FallbackContext {
        return FallbackContext(
            originalJson = json,
            operationType = this.operationType,
            modelName = this.modelName,
            enableIntelligentInference = true,
            enableDetailedLogging = this.enableDetailedLogging,
            properties = this.properties
        )
    }
}

/**
 * 标准解析策略
 */
object StandardParsingStrategy {
    private const val TAG = "StandardParsingStrategy"
    
    fun parseAnalysisResult(json: String, moshi: Moshi): Result<AnalysisResult> {
        return try {
            val adapter = moshi.adapter(AnalysisResult::class.java)
            val result = adapter.fromJson(json)
            
            if (result != null) {
                android.util.Log.d(TAG, "标准解析AnalysisResult成功")
                Result.success(result)
            } else {
                android.util.Log.w(TAG, "标准解析AnalysisResult返回null")
                Result.failure(Exception("标准解析返回null"))
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "标准解析AnalysisResult失败", e)
            Result.failure(e)
        }
    }
    
    fun parseSafetyCheckResult(json: String, moshi: Moshi): Result<SafetyCheckResult> {
        return try {
            val adapter = moshi.adapter(SafetyCheckResult::class.java)
            val result = adapter.fromJson(json)
            
            if (result != null) {
                android.util.Log.d(TAG, "标准解析SafetyCheckResult成功")
                Result.success(result)
            } else {
                android.util.Log.w(TAG, "标准解析SafetyCheckResult返回null")
                Result.failure(Exception("标准解析返回null"))
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "标准解析SafetyCheckResult失败", e)
            Result.failure(e)
        }
    }
    
    fun parseExtractedData(json: String, moshi: Moshi): Result<ExtractedData> {
        return try {
            val adapter = moshi.adapter(ExtractedData::class.java)
            val result = adapter.fromJson(json)
            
            if (result != null) {
                android.util.Log.d(TAG, "标准解析ExtractedData成功")
                Result.success(result)
            } else {
                android.util.Log.w(TAG, "标准解析ExtractedData返回null")
                Result.failure(Exception("标准解析返回null"))
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "标准解析ExtractedData失败", e)
            Result.failure(e)
        }
    }
}

/**
 * 容错解析策略
 */
object FallbackParsingStrategy {
    private const val TAG = "FallbackParsingStrategy"
    
    fun parseAnalysisResult(json: String, moshi: Moshi): Result<AnalysisResult> {
        return try {
            // 使用lenient模式
            val adapter = moshi.adapter(AnalysisResult::class.java).lenient()
            val result = adapter.fromJson(json)
            
            if (result != null) {
                android.util.Log.d(TAG, "容错解析AnalysisResult成功")
                Result.success(result)
            } else {
                android.util.Log.w(TAG, "容错解析AnalysisResult返回null")
                Result.failure(Exception("容错解析返回null"))
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "容错解析AnalysisResult失败", e)
            Result.failure(e)
        }
    }
    
    fun parseSafetyCheckResult(json: String, moshi: Moshi): Result<SafetyCheckResult> {
        return try {
            // 使用lenient模式
            val adapter = moshi.adapter(SafetyCheckResult::class.java).lenient()
            val result = adapter.fromJson(json)
            
            if (result != null) {
                android.util.Log.d(TAG, "容错解析SafetyCheckResult成功")
                Result.success(result)
            } else {
                android.util.Log.w(TAG, "容错解析SafetyCheckResult返回null")
                Result.failure(Exception("容错解析返回null"))
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "容错解析SafetyCheckResult失败", e)
            Result.failure(e)
        }
    }
    
    fun parseExtractedData(json: String, moshi: Moshi): Result<ExtractedData> {
        return try {
            // 使用lenient模式
            val adapter = moshi.adapter(ExtractedData::class.java).lenient()
            val result = adapter.fromJson(json)
            
            if (result != null) {
                android.util.Log.d(TAG, "容错解析ExtractedData成功")
                Result.success(result)
            } else {
                android.util.Log.w(TAG, "容错解析ExtractedData返回null")
                Result.failure(Exception("容错解析返回null"))
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "容错解析ExtractedData失败", e)
            Result.failure(e)
        }
    }
}

/**
 * 智能解析策略
 */
object IntelligentParsingStrategy {
    private const val TAG = "IntelligentParsingStrategy"
    
    fun parseAnalysisResult(json: String, moshi: Moshi): Result<AnalysisResult> {
        return try {
            // 先尝试解析为通用Map
            val mapAdapter = moshi.adapter<Map<String, Any>>(
                Types.newParameterizedType(
                    Map::class.java,
                    String::class.java,
                    Any::class.java
                )
            ).lenient()
            val jsonMap = mapAdapter.fromJson(json)
            
            if (jsonMap == null) {
                android.util.Log.w(TAG, "智能解析AnalysisResult：无法解析为Map")
                return Result.failure(Exception("无法解析为Map"))
            }
            
            // 应用字段名映射
            // 这里应该使用FieldMapper，但为了简化，我们直接进行基本映射
            val mappedMap = applyBasicFieldMapping(jsonMap)
            
            // 尝试从映射后的Map构建AnalysisResult
            val result = buildAnalysisResultFromMap(mappedMap)
            
            android.util.Log.d(TAG, "智能解析AnalysisResult成功")
            Result.success(result)
            
        } catch (e: Exception) {
            android.util.Log.w(TAG, "智能解析AnalysisResult失败", e)
            Result.failure(e)
        }
    }
    
    fun parseSafetyCheckResult(json: String, moshi: Moshi): Result<SafetyCheckResult> {
        return try {
            // 先尝试解析为通用Map
            val mapAdapter = moshi.adapter<Map<String, Any>>(
                Types.newParameterizedType(
                    Map::class.java,
                    String::class.java,
                    Any::class.java
                )
            ).lenient()
            val jsonMap = mapAdapter.fromJson(json)
            
            if (jsonMap == null) {
                android.util.Log.w(TAG, "智能解析SafetyCheckResult：无法解析为Map")
                return Result.failure(Exception("无法解析为Map"))
            }
            
            // 应用字段名映射
            val mappedMap = applyBasicFieldMapping(jsonMap)
            
            // 尝试从映射后的Map构建SafetyCheckResult
            val result = buildSafetyCheckResultFromMap(mappedMap)
            
            android.util.Log.d(TAG, "智能解析SafetyCheckResult成功")
            Result.success(result)
            
        } catch (e: Exception) {
            android.util.Log.w(TAG, "智能解析SafetyCheckResult失败", e)
            Result.failure(e)
        }
    }
    
    fun parseExtractedData(json: String, moshi: Moshi): Result<ExtractedData> {
        return try {
            // 先尝试解析为通用Map
            val mapAdapter = moshi.adapter<Map<String, Any>>(
                Types.newParameterizedType(
                    Map::class.java,
                    String::class.java,
                    Any::class.java
                )
            ).lenient()
            val jsonMap = mapAdapter.fromJson(json)
            
            if (jsonMap == null) {
                android.util.Log.w(TAG, "智能解析ExtractedData：无法解析为Map")
                return Result.failure(Exception("无法解析为Map"))
            }
            
            // 应用字段名映射
            val mappedMap = applyBasicFieldMapping(jsonMap)
            
            // 尝试从映射后的Map构建ExtractedData
            val result = buildExtractedDataFromMap(mappedMap)
            
            android.util.Log.d(TAG, "智能解析ExtractedData成功")
            Result.success(result)
            
        } catch (e: Exception) {
            android.util.Log.w(TAG, "智能解析ExtractedData失败", e)
            Result.failure(e)
        }
    }
    
    /**
     * 应用基本字段映射
     */
    private fun applyBasicFieldMapping(jsonMap: Map<String, Any>): Map<String, Any> {
        val result = mutableMapOf<String, Any>()
        
        // AnalysisResult字段映射
        val analysisMappings = mapOf(
            "回复建议" to "replySuggestion",
            "建议回复" to "replySuggestion",
            "话术建议" to "replySuggestion",
            "具体的回复建议" to "replySuggestion",
            "建议的回复内容" to "replySuggestion",
            "回复内容" to "replySuggestion",
            
            "策略分析" to "strategyAnalysis",
            "心理分析" to "strategyAnalysis",
            "军师分析" to "strategyAnalysis",
            "对方当前的情绪和潜在意图" to "strategyAnalysis",
            "对方当前情绪和潜在意图" to "strategyAnalysis",
            "情绪和潜在意图" to "strategyAnalysis",
            "关键洞察" to "strategyAnalysis",
            "策略建议" to "strategyAnalysis",
            "分析" to "strategyAnalysis",
            "分析结果" to "strategyAnalysis",
            
            "风险等级" to "riskLevel",
            "风险级别" to "riskLevel",
            "可能存在的风险点" to "riskLevel",
            "风险" to "riskLevel"
        )
        
        // SafetyCheckResult字段映射
        val safetyMappings = mapOf(
            "是否安全" to "isSafe",
            "安全" to "isSafe",
            "安全性" to "isSafe",
            
            "触发的风险" to "triggeredRisks",
            "风险列表" to "triggeredRisks",
            "触发雷区" to "triggeredRisks",
            "触发风险" to "triggeredRisks",
            
            "建议" to "suggestion",
            "修改建议" to "suggestion",
            "修正建议" to "suggestion",
            "优化建议" to "suggestion"
        )
        
        // ExtractedData字段映射
        val extractedMappings = mapOf(
            "事实" to "facts",
            "事实信息" to "facts",
            "基本信息" to "facts",
            "个人资料" to "facts",
            "用户信息" to "facts",
            
            "红色标签" to "redTags",
            "雷区" to "redTags",
            "风险标签" to "redTags",
            "红标签" to "redTags",
            "不要做的事" to "redTags",
            "敏感话题" to "redTags",
            
            "绿色标签" to "greenTags",
            "策略" to "greenTags",
            "策略标签" to "greenTags",
            "绿标签" to "greenTags",
            "推荐做法" to "greenTags",
            "沟通技巧" to "greenTags"
        )
        
        // 合并所有映射
        val allMappings = mutableMapOf<String, String>()
        allMappings.putAll(analysisMappings)
        allMappings.putAll(safetyMappings)
        allMappings.putAll(extractedMappings)
        
        // 应用映射
        jsonMap.forEach { (key, value) ->
            val mappedKey = allMappings[key]
            if (mappedKey != null) {
                result[mappedKey] = value
            } else {
                result[key] = value
            }
        }
        
        return result
    }
    
    /**
     * 从Map构建AnalysisResult
     */
    private fun buildAnalysisResultFromMap(jsonMap: Map<String, Any>): AnalysisResult {
        val replySuggestion = (jsonMap["replySuggestion"] as? String) ?: ""
        val strategyAnalysis = (jsonMap["strategyAnalysis"] as? String) ?: ""
        
        val riskLevelStr = jsonMap["riskLevel"] as? String
        val riskLevel = when (riskLevelStr?.uppercase()) {
            "SAFE" -> com.empathy.ai.domain.model.RiskLevel.SAFE
            "WARNING" -> com.empathy.ai.domain.model.RiskLevel.WARNING
            "DANGER" -> com.empathy.ai.domain.model.RiskLevel.DANGER
            else -> com.empathy.ai.domain.model.RiskLevel.SAFE
        }
        
        return AnalysisResult(replySuggestion, strategyAnalysis, riskLevel)
    }
    
    /**
     * 从Map构建SafetyCheckResult
     */
    private fun buildSafetyCheckResultFromMap(jsonMap: Map<String, Any>): SafetyCheckResult {
        val isSafe = (jsonMap["isSafe"] as? Boolean) ?: true
        val triggeredRisks = (jsonMap["triggeredRisks"] as? List<String>) ?: emptyList()
        val suggestion = (jsonMap["suggestion"] as? String) ?: "安全检查完成，未发现明显风险"
        
        return SafetyCheckResult(isSafe, triggeredRisks, suggestion)
    }
    
    /**
     * 从Map构建ExtractedData
     */
    private fun buildExtractedDataFromMap(jsonMap: Map<String, Any>): ExtractedData {
        val facts = (jsonMap["facts"] as? Map<String, String>) ?: emptyMap()
        val redTags = (jsonMap["redTags"] as? List<String>) ?: emptyList()
        val greenTags = (jsonMap["greenTags"] as? List<String>) ?: emptyList()
        
        return ExtractedData(facts, redTags, greenTags)
    }
}