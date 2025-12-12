package com.empathy.ai.data.parser

import android.util.Log
import com.empathy.ai.domain.model.AnalysisResult
import com.empathy.ai.domain.model.RiskLevel
import com.empathy.ai.domain.model.SafetyCheckResult
import com.empathy.ai.domain.usecase.ExtractedData
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.util.concurrent.ConcurrentHashMap

/**
 * 多层次降级处理器实现
 * 
 * 提供全面的降级策略，支持多层次字段提取和智能推断
 * 基于原有AiRepositoryImpl中的多层次降级策略重构
 */
class MultiLevelFallbackHandler(
    private val jsonCleaner: JsonCleaner,
    private val fieldMapper: FieldMapper,
    private val moshi: Moshi = Moshi.Builder().build()
) : FallbackHandler {
    
    companion object {
        private const val TAG = "MultiLevelFallbackHandler"
        
        // 高风险关键词
        private val HIGH_RISK_KEYWORDS = listOf(
            "高风险", "危险", "严重", "紧急", "立即", "禁止", "绝对不能"
        )
        
        // 中等风险关键词
        private val MEDIUM_RISK_KEYWORDS = listOf(
            "风险", "注意", "谨慎", "小心", "避免", "不宜"
        )
        
        // 常见事实信息关键词
        private val FACT_KEYWORDS = listOf(
            "生日", "爱好", "职业", "年龄", "性别", "地区", "信息", "资料"
        )
        
        // 常见风险标签关键词
        private val RISK_TAG_KEYWORDS = listOf(
            "不要", "避免", "禁止", "风险", "警告", "注意", "敏感", "隐私", "前任", "收入"
        )
        
        // 常见策略标签关键词
        private val STRATEGY_TAG_KEYWORDS = listOf(
            "推荐", "建议", "可以", "分享", "讨论", "兴趣", "旅行", "美食", "工作"
        )
    }
    
    // 学习到的映射缓存
    private val learnedMappings = ConcurrentHashMap<String, List<String>>()
    
    override fun <T> handleParsingFailure(
        error: Exception,
        targetType: Class<T>,
        context: FallbackContext
    ): FallbackResult<T> {
        Log.w(TAG, "处理解析失败: ${error.javaClass.simpleName}, 目标类型: ${targetType.simpleName}")
        
        if (context.enableDetailedLogging) {
            Log.d(TAG, "原始JSON: ${context.originalJson.take(200)}...")
        }
        
        return try {
            // 尝试多层次字段提取策略
            val extractedData = extractFieldsWithMultiLevelStrategy(context.originalJson, targetType)
            
            when (targetType) {
                AnalysisResult::class.java -> {
                    val analysisData = extractedData as? ExtractedAnalysisData
                    if (analysisData != null) {
                        val result = AnalysisResult(
                            replySuggestion = analysisData.replySuggestion,
                            strategyAnalysis = analysisData.strategyAnalysis,
                            riskLevel = analysisData.riskLevel
                        )
                        
                        // 评估解析质量
                        val quality = analyzeParseQuality(result, listOf("replySuggestion", "strategyAnalysis", "riskLevel"))
                        
                        if (quality.overallScore >= 0.3) {
                            Log.i(TAG, "使用多层次字段提取策略成功")
                            return FallbackResult.Success(result, FallbackStrategy.FIELD_EXTRACTION, quality.overallScore)
                        }
                    }
                }
                
                SafetyCheckResult::class.java -> {
                    val safetyData = extractedData as? ExtractedSafetyCheckData
                    if (safetyData != null) {
                        val result = SafetyCheckResult(
                            isSafe = safetyData.isSafe,
                            triggeredRisks = safetyData.triggeredRisks,
                            suggestion = safetyData.suggestion
                        )
                        
                        // 评估解析质量
                        val quality = analyzeParseQuality(result, listOf("isSafe", "triggeredRisks", "suggestion"))
                        
                        if (quality.overallScore >= 0.3) {
                            Log.i(TAG, "使用多层次字段提取策略成功")
                            return FallbackResult.Success(result, FallbackStrategy.FIELD_EXTRACTION, quality.overallScore)
                        }
                    }
                }
                
                ExtractedData::class.java -> {
                    val extractedDataInfo = extractedData as? ExtractedExtractedData
                    if (extractedDataInfo != null) {
                        val result = ExtractedData(
                            facts = extractedDataInfo.facts,
                            redTags = extractedDataInfo.redTags,
                            greenTags = extractedDataInfo.greenTags
                        )
                        
                        // 评估解析质量
                        val quality = analyzeParseQuality(result, listOf("facts", "redTags", "greenTags"))
                        
                        if (quality.overallScore >= 0.3) {
                            Log.i(TAG, "使用多层次字段提取策略成功")
                            return FallbackResult.Success(result, FallbackStrategy.FIELD_EXTRACTION, quality.overallScore)
                        }
                    }
                }
            }
            
            // 如果多层次提取失败，尝试智能推断
            if (context.enableIntelligentInference) {
                val inferredResult = tryIntelligentInference(targetType, context)
                if (inferredResult != null) {
                    return inferredResult
                }
            }
            
            // 最后使用默认值
            Log.w(TAG, "所有降级策略失败，使用默认值")
            val defaultValue = DefaultValues.getDefaultValue(targetType)
            return FallbackResult.Success(defaultValue, FallbackStrategy.USE_DEFAULT_VALUES, 0.1)
            
        } catch (e: Exception) {
            Log.e(TAG, "降级处理失败", e)
            val defaultValue = DefaultValues.getDefaultValue(targetType)
            return FallbackResult.Success(defaultValue, FallbackStrategy.USE_DEFAULT_VALUES, 0.1)
        }
    }
    
    override fun <T> handlePartialResult(
        partialData: Any,
        targetType: Class<T>,
        context: FallbackContext
    ): FallbackResult<T> {
        Log.i(TAG, "处理部分解析结果: ${targetType.simpleName}")
        
        return try {
            when (targetType) {
                AnalysisResult::class.java -> {
                    val partial = partialData as? AnalysisResult
                    if (partial != null) {
                        // 补充缺失的字段
                        val enhanced = enhancePartialAnalysisResult(partial, context)
                        val quality = analyzeParseQuality(enhanced, listOf("replySuggestion", "strategyAnalysis", "riskLevel"))
                        return FallbackResult.Success(enhanced as T, FallbackStrategy.USE_PARTIAL_DATA, quality.overallScore)
                    }
                }
                
                SafetyCheckResult::class.java -> {
                    val partial = partialData as? SafetyCheckResult
                    if (partial != null) {
                        // 补充缺失的字段
                        val enhanced = enhancePartialSafetyCheckResult(partial, context)
                        val quality = analyzeParseQuality(enhanced, listOf("isSafe", "triggeredRisks", "suggestion"))
                        return FallbackResult.Success(enhanced as T, FallbackStrategy.USE_PARTIAL_DATA, quality.overallScore)
                    }
                }
                
                ExtractedData::class.java -> {
                    val partial = partialData as? ExtractedData
                    if (partial != null) {
                        // 补充缺失的字段
                        val enhanced = enhancePartialExtractedData(partial, context)
                        val quality = analyzeParseQuality(enhanced, listOf("facts", "redTags", "greenTags"))
                        return FallbackResult.Success(enhanced as T, FallbackStrategy.USE_PARTIAL_DATA, quality.overallScore)
                    }
                }
            }
            
            // 无法增强，使用默认值
            val defaultValue = DefaultValues.getDefaultValue(targetType)
            return FallbackResult.Success(defaultValue, FallbackStrategy.USE_DEFAULT_VALUES, 0.1)
            
        } catch (e: Exception) {
            Log.e(TAG, "处理部分结果失败", e)
            val defaultValue = DefaultValues.getDefaultValue(targetType)
            return FallbackResult.Success(defaultValue, FallbackStrategy.USE_DEFAULT_VALUES, 0.1)
        }
    }
    
    override fun <T> generateDefaultValue(
        targetType: Class<T>,
        context: FallbackContext
    ): T {
        Log.i(TAG, "生成默认值: ${targetType.simpleName}")
        return DefaultValues.getDefaultValue(targetType)
    }
    
    /**
     * 多层次字段提取策略
     */
    private fun <T> extractFieldsWithMultiLevelStrategy(json: String, targetType: Class<T>): Any? {
        return try {
            // 预处理JSON
            val cleanedJson = jsonCleaner.clean(json)
            
            // 尝试解析为通用Map
            val mapAdapter = moshi.adapter<Map<String, Any>>(
                Types.newParameterizedType(
                    Map::class.java,
                    String::class.java,
                    Any::class.java
                )
            ).lenient()
            val jsonMap = mapAdapter.fromJson(cleanedJson)
            
            if (jsonMap == null) {
                Log.w(TAG, "无法解析JSON为Map")
                return null
            }
            
            // 应用字段名映射
            val mappedJson = fieldMapper.mapFields(cleanedJson)
            val mappedMapAdapter = moshi.adapter<Map<String, Any>>(
                Types.newParameterizedType(
                    Map::class.java,
                    String::class.java,
                    Any::class.java
                )
            ).lenient()
            val mappedJsonMap = mappedMapAdapter.fromJson(mappedJson)
            
            val finalMap = mappedJsonMap ?: jsonMap
            
            when (targetType) {
                AnalysisResult::class.java -> extractAnalysisData(finalMap, cleanedJson)
                SafetyCheckResult::class.java -> extractSafetyCheckData(finalMap, cleanedJson)
                ExtractedData::class.java -> extractExtractedDataInfo(finalMap, cleanedJson)
                else -> null
            }
        } catch (e: Exception) {
            Log.w(TAG, "多层次字段提取失败", e)
            null
        }
    }
    
    /**
     * 提取分析结果数据
     */
    private fun extractAnalysisData(jsonMap: Map<String, Any>, rawJson: String): ExtractedAnalysisData {
        var replySuggestion = ""
        var strategyAnalysis = ""
        var riskLevel = RiskLevel.SAFE
        
        // 层次1：标准字段提取
        replySuggestion = extractStandardField(jsonMap, "replySuggestion")
        strategyAnalysis = extractStandardField(jsonMap, "strategyAnalysis")
        riskLevel = extractStandardRiskLevel(jsonMap)
        
        // 层次2：中文字段映射提取
        if (replySuggestion.isBlank()) {
            replySuggestion = extractChineseField(jsonMap, listOf("回复建议", "建议回复", "话术建议", "具体的回复建议", "建议的回复内容", "回复内容"))
        }
        if (strategyAnalysis.isBlank()) {
            strategyAnalysis = extractChineseField(jsonMap, listOf("策略分析", "心理分析", "军师分析", "对方当前的情绪和潜在意图", "关键洞察", "策略建议"))
        }
        if (riskLevel == RiskLevel.SAFE) {
            riskLevel = extractChineseRiskLevel(jsonMap)
        }
        
        // 层次3：变体字段名提取
        if (replySuggestion.isBlank()) {
            replySuggestion = extractVariantField(jsonMap, listOf("reply", "response", "answer", "recommended_response", "suggestion"))
        }
        if (strategyAnalysis.isBlank()) {
            strategyAnalysis = extractVariantField(jsonMap, listOf("strategy", "analysis", "insights", "summary", "assessment"))
        }
        
        // 层次4：嵌套结构提取
        if (replySuggestion.isBlank() || strategyAnalysis.isBlank()) {
            val nestedData = extractFromNestedStructure(jsonMap)
            if (replySuggestion.isBlank()) replySuggestion = nestedData.replySuggestion
            if (strategyAnalysis.isBlank()) strategyAnalysis = nestedData.strategyAnalysis
            if (riskLevel == RiskLevel.SAFE && nestedData.riskLevel != RiskLevel.SAFE) {
                riskLevel = nestedData.riskLevel
            }
        }
        
        // 层次5：数组格式提取
        if (replySuggestion.isBlank()) {
            replySuggestion = extractFromArrayField(jsonMap, listOf("suggestions", "replySuggestions", "response_suggestions", "recommendations"))
        }
        if (strategyAnalysis.isBlank()) {
            strategyAnalysis = extractFromArrayField(jsonMap, listOf("points", "insights", "analysis_points", "key_points"))
        }
        
        // 层次6：文本内容推断
        if (replySuggestion.isBlank() || strategyAnalysis.isBlank()) {
            val inferredData = inferFromTextContent(rawJson)
            if (replySuggestion.isBlank()) replySuggestion = inferredData.replySuggestion
            if (strategyAnalysis.isBlank()) strategyAnalysis = inferredData.strategyAnalysis
            if (riskLevel == RiskLevel.SAFE && inferredData.riskLevel != RiskLevel.SAFE) {
                riskLevel = inferredData.riskLevel
            }
        }
        
        // 层次7：智能默认值生成
        if (replySuggestion.isBlank()) {
            replySuggestion = generateReplySuggestion(jsonMap, rawJson)
        }
        if (strategyAnalysis.isBlank()) {
            strategyAnalysis = generateStrategyAnalysis(jsonMap, rawJson)
        }
        
        return ExtractedAnalysisData(replySuggestion, strategyAnalysis, riskLevel)
    }
    
    /**
     * 提取安全检查结果数据
     */
    private fun extractSafetyCheckData(jsonMap: Map<String, Any>, rawJson: String): ExtractedSafetyCheckData {
        var isSafe: Boolean? = null
        var triggeredRisks: List<String>? = null
        var suggestion: String? = null
        
        // 层次1：标准字段提取
        isSafe = extractStandardIsSafe(jsonMap)
        triggeredRisks = extractStandardTriggeredRisks(jsonMap)
        suggestion = extractStandardSuggestion(jsonMap)
        
        // 层次2：中文字段映射提取
        if (isSafe == null) {
            isSafe = extractChineseIsSafe(jsonMap)
        }
        if (triggeredRisks == null) {
            triggeredRisks = extractChineseTriggeredRisks(jsonMap)
        }
        if (suggestion == null) {
            suggestion = extractChineseSuggestion(jsonMap)
        }
        
        // 层次3：变体字段名提取
        if (isSafe == null) {
            isSafe = extractVariantIsSafe(jsonMap)
        }
        if (triggeredRisks == null) {
            triggeredRisks = extractVariantTriggeredRisks(jsonMap)
        }
        if (suggestion == null) {
            suggestion = extractVariantSuggestion(jsonMap)
        }
        
        // 层次4：嵌套结构提取
        if (isSafe == null || triggeredRisks == null || suggestion == null) {
            val nestedData = extractFromNestedStructureForSafetyCheck(jsonMap)
            if (isSafe == null) isSafe = nestedData.isSafe
            if (triggeredRisks == null) triggeredRisks = nestedData.triggeredRisks
            if (suggestion == null) suggestion = nestedData.suggestion
        }
        
        // 层次5：数组格式提取
        if (triggeredRisks == null) {
            triggeredRisks = extractFromArrayForSafetyCheck(jsonMap)
        }
        
        // 层次6：文本内容推断
        if (isSafe == null || triggeredRisks == null || suggestion == null) {
            val inferredData = inferFromTextContentForSafetyCheck(rawJson)
            if (isSafe == null) isSafe = inferredData.isSafe
            if (triggeredRisks == null) triggeredRisks = inferredData.triggeredRisks
            if (suggestion == null) suggestion = inferredData.suggestion
        }
        
        // 层次7：智能默认值生成
        if (isSafe == null) {
            isSafe = generateDefaultIsSafe(jsonMap, rawJson)
        }
        if (triggeredRisks == null) {
            triggeredRisks = generateDefaultTriggeredRisks(jsonMap, rawJson)
        }
        if (suggestion == null) {
            suggestion = generateDefaultSuggestion(jsonMap, rawJson)
        }
        
        return ExtractedSafetyCheckData(
            isSafe = isSafe ?: true,
            triggeredRisks = triggeredRisks ?: emptyList(),
            suggestion = suggestion ?: "安全检查完成，未发现明显风险"
        )
    }
    
    /**
     * 提取提取数据信息
     */
    private fun extractExtractedDataInfo(jsonMap: Map<String, Any>, rawJson: String): ExtractedExtractedData {
        var facts: Map<String, String>? = null
        var redTags: List<String>? = null
        var greenTags: List<String>? = null
        
        // 层次1：标准字段提取
        facts = extractStandardFacts(jsonMap)
        redTags = extractStandardRedTags(jsonMap)
        greenTags = extractStandardGreenTags(jsonMap)
        
        // 层次2：中文字段映射提取
        if (facts == null) {
            facts = extractChineseFacts(jsonMap)
        }
        if (redTags == null) {
            redTags = extractChineseRedTags(jsonMap)
        }
        if (greenTags == null) {
            greenTags = extractChineseGreenTags(jsonMap)
        }
        
        // 层次3：变体字段名提取
        if (facts == null) {
            facts = extractVariantFacts(jsonMap)
        }
        if (redTags == null) {
            redTags = extractVariantRedTags(jsonMap)
        }
        if (greenTags == null) {
            greenTags = extractVariantGreenTags(jsonMap)
        }
        
        // 层次4：嵌套结构提取
        if (facts == null || redTags == null || greenTags == null) {
            val nestedData = extractFromNestedStructureForExtractedData(jsonMap)
            if (facts == null) facts = nestedData.facts
            if (redTags == null) redTags = nestedData.redTags
            if (greenTags == null) greenTags = nestedData.greenTags
        }
        
        // 层次5：数组格式提取
        if (redTags == null || greenTags == null) {
            val arrayData = extractFromArrayForExtractedData(jsonMap)
            if (redTags == null) redTags = arrayData.redTags
            if (greenTags == null) greenTags = arrayData.greenTags
        }
        
        // 层次6：文本内容推断
        if (facts == null || redTags == null || greenTags == null) {
            val inferredData = inferFromTextContentForExtractedData(rawJson)
            if (facts == null) facts = inferredData.facts
            if (redTags == null) redTags = inferredData.redTags
            if (greenTags == null) greenTags = inferredData.greenTags
        }
        
        // 层次7：智能默认值生成
        if (facts == null) {
            facts = generateDefaultFacts(jsonMap, rawJson)
        }
        if (redTags == null) {
            redTags = generateDefaultRedTags(jsonMap, rawJson)
        }
        if (greenTags == null) {
            greenTags = generateDefaultGreenTags(jsonMap, rawJson)
        }
        
        return ExtractedExtractedData(
            facts = facts ?: emptyMap(),
            redTags = redTags ?: emptyList(),
            greenTags = greenTags ?: emptyList()
        )
    }
    
    /**
     * 尝试智能推断
     */
    private fun <T> tryIntelligentInference(targetType: Class<T>, context: FallbackContext): FallbackResult<T>? {
        Log.d(TAG, "尝试智能推断: ${targetType.simpleName}")
        
        return try {
            when (targetType) {
                AnalysisResult::class.java -> {
                    val result = inferAnalysisResultFromText(context.originalJson)
                    val quality = analyzeParseQuality(result, listOf("replySuggestion", "strategyAnalysis", "riskLevel"))
                    FallbackResult.Success(result as T, FallbackStrategy.INTELLIGENT_INFERENCE, quality.overallScore)
                }
                
                SafetyCheckResult::class.java -> {
                    val result = inferSafetyCheckResultFromText(context.originalJson)
                    val quality = analyzeParseQuality(result, listOf("isSafe", "triggeredRisks", "suggestion"))
                    FallbackResult.Success(result as T, FallbackStrategy.INTELLIGENT_INFERENCE, quality.overallScore)
                }
                
                ExtractedData::class.java -> {
                    val result = inferExtractedDataFromText(context.originalJson)
                    val quality = analyzeParseQuality(result, listOf("facts", "redTags", "greenTags"))
                    FallbackResult.Success(result as T, FallbackStrategy.INTELLIGENT_INFERENCE, quality.overallScore)
                }
                
                else -> null
            }
        } catch (e: Exception) {
            Log.w(TAG, "智能推断失败", e)
            null
        }
    }
    
    /**
     * 从文本推断分析结果
     */
    private fun inferAnalysisResultFromText(text: String): AnalysisResult {
        val lines = text.split("\n")
        var replySuggestion = ""
        var strategyAnalysis = ""
        var riskLevel = RiskLevel.SAFE
        
        // 查找可能的回复建议
        for (line in lines) {
            if (replySuggestion.isBlank() && line.contains("建议") && line.contains("回复")) {
                replySuggestion = line.substringAfter(":").substringAfter("：").trim()
            }
            if (strategyAnalysis.isBlank() && (line.contains("分析") || line.contains("策略"))) {
                strategyAnalysis = line.substringAfter(":").substringAfter("：").trim()
            }
        }
        
        // 智能推断风险等级
        val lowerText = text.lowercase()
        riskLevel = when {
            lowerText.contains("危险") || lowerText.contains("高风险") || lowerText.contains("严重") -> RiskLevel.DANGER
            lowerText.contains("注意") || lowerText.contains("风险") || lowerText.contains("谨慎") -> RiskLevel.WARNING
            else -> RiskLevel.SAFE
        }
        
        // 生成默认值
        if (replySuggestion.isBlank()) {
            replySuggestion = generateReplySuggestion(emptyMap(), text)
        }
        if (strategyAnalysis.isBlank()) {
            strategyAnalysis = generateStrategyAnalysis(emptyMap(), text)
        }
        
        return AnalysisResult(replySuggestion, strategyAnalysis, riskLevel)
    }
    
    /**
     * 从文本推断安全检查结果
     */
    private fun inferSafetyCheckResultFromText(text: String): SafetyCheckResult {
        val lines = text.split("\n")
        var isSafe: Boolean? = null
        var triggeredRisks: List<String>? = null
        var suggestion: String? = null
        
        // 查找可能的安全状态
        for (line in lines) {
            if (isSafe == null && (line.contains("安全") || line.contains("safe"))) {
                isSafe = when {
                    line.contains("不安全") || line.contains("unsafe") || line.contains("风险") -> false
                    line.contains("安全") || line.contains("safe") -> true
                    else -> null
                }
            }
            if (suggestion == null && (line.contains("建议") || line.contains("suggestion"))) {
                suggestion = line.substringAfter(":").substringAfter("：").trim()
            }
        }
        
        // 智能推断风险列表
        val riskPattern = Regex("""(?:风险|雷区|问题|issue|risk)[:：]\s*([^\n]+)""")
        val riskMatches = riskPattern.findAll(text).map { it.groupValues[1].trim() }.toList()
        if (riskMatches.isNotEmpty()) {
            triggeredRisks = riskMatches
        }
        
        // 智能推断安全状态
        if (isSafe == null) {
            val lowerText = text.lowercase()
            isSafe = when {
                lowerText.contains("危险") || lowerText.contains("高风险") || lowerText.contains("严重") -> false
                lowerText.contains("安全") || lowerText.contains("无风险") || lowerText.contains("正常") -> true
                else -> null
            }
        }
        
        // 生成默认值
        if (triggeredRisks == null) {
            triggeredRisks = generateDefaultTriggeredRisks(emptyMap(), text)
        }
        if (suggestion == null) {
            suggestion = generateDefaultSuggestion(emptyMap(), text)
        }
        
        return SafetyCheckResult(
            isSafe = isSafe ?: true,
            triggeredRisks = triggeredRisks ?: emptyList(),
            suggestion = suggestion ?: "安全检查完成，未发现明显风险"
        )
    }
    
    /**
     * 从文本推断提取数据
     */
    private fun inferExtractedDataFromText(text: String): ExtractedData {
        val lines = text.split("\n")
        var facts: Map<String, String>? = null
        var redTags: List<String>? = null
        var greenTags: List<String>? = null
        
        // 提取事实信息
        val factsMap = mutableMapOf<String, String>()
        for (line in lines) {
            // 查找键值对格式的事实信息
            if (line.contains(":") || line.contains("：")) {
                val parts = line.split(Regex("[:：]"), limit = 2)
                if (parts.size == 2) {
                    val key = parts[0].trim().removeSurrounding("\"")
                    val value = parts[1].trim().removeSurrounding("\"")
                    if (key.isNotBlank() && value.isNotBlank() &&
                        (key.contains("生日") || key.contains("爱好") || key.contains("职业") ||
                         key.contains("年龄") || key.contains("性别") || key.contains("地区") ||
                         key.lowercase().contains("birthday") || key.lowercase().contains("hobby") ||
                         key.lowercase().contains("profession") || key.lowercase().contains("age"))) {
                        factsMap[key] = value
                    }
                }
            }
        }
        if (factsMap.isNotEmpty()) {
            facts = factsMap
        }
        
        // 智能推断标签
        val redTagsList = mutableListOf<String>()
        val greenTagsList = mutableListOf<String>()
        
        for (line in lines) {
            // 查找可能的红色标签（不要做的事）
            if (line.contains("不要") || line.contains("避免") || line.contains("禁止") ||
                line.lowercase().contains("don't") || line.lowercase().contains("avoid") ||
                line.lowercase().contains("never") || line.lowercase().contains("prohibited")) {
                val tag = line.substringAfter(":").substringAfter("：").trim().removeSurrounding("\"")
                if (tag.isNotBlank()) {
                    redTagsList.add(tag)
                }
            }
            
            // 查找可能的绿色标签（推荐做的事）
            if (line.contains("推荐") || line.contains("建议") || line.contains("可以") ||
                line.lowercase().contains("recommend") || line.lowercase().contains("suggest") ||
                line.lowercase().contains("should") || line.lowercase().contains("good")) {
                val tag = line.substringAfter(":").substringAfter("：").trim().removeSurrounding("\"")
                if (tag.isNotBlank()) {
                    greenTagsList.add(tag)
                }
            }
        }
        
        if (redTagsList.isNotEmpty()) {
            redTags = redTagsList.distinct()
        }
        if (greenTagsList.isNotEmpty()) {
            greenTags = greenTagsList.distinct()
        }
        
        // 生成默认值
        if (facts == null) {
            facts = generateDefaultFacts(emptyMap(), text)
        }
        if (redTags == null) {
            redTags = generateDefaultRedTags(emptyMap(), text)
        }
        if (greenTags == null) {
            greenTags = generateDefaultGreenTags(emptyMap(), text)
        }
        
        return ExtractedData(
            facts = facts ?: emptyMap(),
            redTags = redTags ?: emptyList(),
            greenTags = greenTags ?: emptyList()
        )
    }
    
    /**
     * 分析解析质量
     */
    private fun analyzeParseQuality(result: Any, expectedFields: List<String>): ParseQuality {
        val completeness = calculateCompleteness(result, expectedFields)
        val accuracy = calculateAccuracy(result)
        val confidence = calculateConfidence(result)
        val overallScore = (completeness + accuracy + confidence) / 3.0
        
        return ParseQuality(completeness, accuracy, confidence, overallScore)
    }
    
    /**
     * 计算解析完整性
     */
    private fun calculateCompleteness(result: Any, expectedFields: List<String>): Double {
        val availableFields = when (result) {
            is AnalysisResult -> listOfNotNull(
                if (result.replySuggestion.isNotBlank()) "replySuggestion" else null,
                if (result.strategyAnalysis.isNotBlank()) "strategyAnalysis" else null,
                "riskLevel" // riskLevel 总是有值的
            )
            is SafetyCheckResult -> listOfNotNull(
                "isSafe", // isSafe 总是有值的
                "triggeredRisks", // triggeredRisks 总是有值的
                if (result.suggestion.isNotBlank()) "suggestion" else null
            )
            is ExtractedData -> listOfNotNull(
                if (result.facts.isNotEmpty()) "facts" else null,
                if (result.redTags.isNotEmpty()) "redTags" else null,
                if (result.greenTags.isNotEmpty()) "greenTags" else null
            )
            else -> emptyList()
        }
        
        return expectedFields.count { it in availableFields }.toDouble() / expectedFields.size.toDouble()
    }
    
    /**
     * 计算解析准确性
     */
    private fun calculateAccuracy(result: Any): Double {
        return when (result) {
            is AnalysisResult -> {
                var score = 1.0
                if (result.replySuggestion.isBlank()) score -= 0.4
                if (result.strategyAnalysis.isBlank()) score -= 0.4
                // riskLevel 总是有值的，不减分
                score.coerceAtLeast(0.0)
            }
            is SafetyCheckResult -> {
                var score = 1.0
                // isSafe 和 triggeredRisks 总是有值的
                if (result.suggestion.isBlank()) score -= 0.3
                score.coerceAtLeast(0.0)
            }
            is ExtractedData -> {
                var score = 1.0
                if (result.facts.isEmpty()) score -= 0.35
                if (result.redTags.isEmpty()) score -= 0.35
                if (result.greenTags.isEmpty()) score -= 0.3
                score.coerceAtLeast(0.0)
            }
            else -> 0.0
        }
    }
    
    /**
     * 计算解析置信度
     */
    private fun calculateConfidence(result: Any): Double {
        return when (result) {
            is AnalysisResult -> {
                val replyLength = result.replySuggestion.length
                val analysisLength = result.strategyAnalysis.length
                
                when {
                    replyLength > 50 && analysisLength > 100 -> 0.9
                    replyLength > 20 && analysisLength > 50 -> 0.7
                    replyLength > 0 && analysisLength > 0 -> 0.5
                    else -> 0.2
                }
            }
            is SafetyCheckResult -> {
                val suggestionLength = result.suggestion.length
                when {
                    suggestionLength > 50 -> 0.9
                    suggestionLength > 20 -> 0.7
                    suggestionLength > 0 -> 0.5
                    else -> 0.3
                }
            }
            is ExtractedData -> {
                val factsCount = result.facts.size
                val redTagsCount = result.redTags.size
                val greenTagsCount = result.greenTags.size
                
                when {
                    factsCount >= 3 && (redTagsCount >= 2 || greenTagsCount >= 2) -> 0.9
                    factsCount >= 1 && (redTagsCount >= 1 || greenTagsCount >= 1) -> 0.7
                    factsCount >= 1 || redTagsCount >= 1 || greenTagsCount >= 1 -> 0.5
                    else -> 0.2
                }
            }
            else -> 0.0
        }
    }
    
    // 以下是各种辅助方法的简化实现，基于原有AiRepositoryImpl的方法重构
    
    private fun extractStandardField(jsonMap: Map<String, Any>, fieldName: String): String {
        return (jsonMap[fieldName] as? String)?.takeIf { it.isNotBlank() } ?: ""
    }
    
    private fun extractStandardRiskLevel(jsonMap: Map<String, Any>): RiskLevel {
        val riskLevelStr = jsonMap["riskLevel"] as? String
        return when (riskLevelStr?.uppercase()) {
            "SAFE" -> RiskLevel.SAFE
            "WARNING" -> RiskLevel.WARNING
            "DANGER" -> RiskLevel.DANGER
            else -> RiskLevel.SAFE
        }
    }
    
    private fun extractChineseField(jsonMap: Map<String, Any>, fieldNames: List<String>): String {
        for (fieldName in fieldNames) {
            val value = jsonMap[fieldName] as? String
            if (!value.isNullOrBlank()) {
                return value
            }
        }
        return ""
    }
    
    private fun extractChineseRiskLevel(jsonMap: Map<String, Any>): RiskLevel {
        val chineseFieldNames = listOf("风险等级", "风险级别", "风险")
        for (fieldName in chineseFieldNames) {
            val value = jsonMap[fieldName] as? String
            if (!value.isNullOrBlank()) {
                return when (value.lowercase()) {
                    "安全", "低", "safe" -> RiskLevel.SAFE
                    "警告", "注意", "中", "warning" -> RiskLevel.WARNING
                    "危险", "高", "danger" -> RiskLevel.DANGER
                    else -> RiskLevel.SAFE
                }
            }
        }
        return RiskLevel.SAFE
    }
    
    private fun extractVariantField(jsonMap: Map<String, Any>, fieldNames: List<String>): String {
        for (fieldName in fieldNames) {
            val value = jsonMap[fieldName] as? String
            if (!value.isNullOrBlank()) {
                return value
            }
        }
        return ""
    }
    
    private fun extractFromNestedStructure(jsonMap: Map<String, Any>): ExtractedAnalysisData {
        var replySuggestion = ""
        var strategyAnalysis = ""
        var riskLevel = RiskLevel.SAFE
        
        val analysisObj = jsonMap["analysis"] as? Map<String, Any>
        if (analysisObj != null) {
            replySuggestion = analysisObj["reply"] as? String ?: ""
            strategyAnalysis = analysisObj["content"] as? String ?: ""
            val riskLevelStr = analysisObj["risk_level"] as? String
            riskLevel = when (riskLevelStr?.lowercase()) {
                "低", "low", "safe" -> RiskLevel.SAFE
                "中", "medium", "warning" -> RiskLevel.WARNING
                "高", "high", "danger" -> RiskLevel.DANGER
                else -> RiskLevel.SAFE
            }
        }
        
        return ExtractedAnalysisData(replySuggestion, strategyAnalysis, riskLevel)
    }
    
    private fun extractFromArrayField(jsonMap: Map<String, Any>, fieldNames: List<String>): String {
        for (fieldName in fieldNames) {
            val array = jsonMap[fieldName] as? List<*>
            if (!array.isNullOrEmpty()) {
                val firstElement = array.firstOrNull() as? String
                if (!firstElement.isNullOrBlank()) {
                    return firstElement
                }
            }
        }
        return ""
    }
    
    private fun inferFromTextContent(rawJson: String): ExtractedAnalysisData {
        var replySuggestion = ""
        var strategyAnalysis = ""
        var riskLevel = RiskLevel.SAFE
        
        val lines = rawJson.split("\n")
        
        for (line in lines) {
            if (replySuggestion.isBlank() && line.contains("建议") && line.contains("回复")) {
                replySuggestion = line.substringAfter(":").substringAfter("：").trim()
            }
            if (strategyAnalysis.isBlank() && (line.contains("分析") || line.contains("策略"))) {
                strategyAnalysis = line.substringAfter(":").substringAfter("：").trim()
            }
        }
        
        val lowerText = rawJson.lowercase()
        riskLevel = when {
            lowerText.contains("危险") || lowerText.contains("高风险") || lowerText.contains("严重") -> RiskLevel.DANGER
            lowerText.contains("注意") || lowerText.contains("风险") || lowerText.contains("谨慎") -> RiskLevel.WARNING
            else -> RiskLevel.SAFE
        }
        
        return ExtractedAnalysisData(replySuggestion, strategyAnalysis, riskLevel)
    }
    
    private fun generateReplySuggestion(jsonMap: Map<String, Any>, rawJson: String): String {
        val keywords = extractKeywords(jsonMap, rawJson)
        
        return when {
            keywords.contains("问题") || keywords.contains("询问") -> "这是一个很好的问题，我需要更多信息来给出准确的回答。"
            keywords.contains("感谢") || keywords.contains("谢谢") -> "不客气，很高兴能帮到你。"
            keywords.contains("建议") || keywords.contains("意见") -> "感谢你的建议，我会认真考虑。"
            keywords.contains("抱歉") || keywords.contains("对不起") -> "没关系，我理解你的情况。"
            else -> "我理解你的意思，让我们继续这个话题。"
        }
    }
    
    private fun generateStrategyAnalysis(jsonMap: Map<String, Any>, rawJson: String): String {
        val keywords = extractKeywords(jsonMap, rawJson)
        
        return when {
            keywords.contains("工作") || keywords.contains("项目") -> "对方可能正在讨论工作相关话题，建议保持专业态度，提供有价值的见解。"
            keywords.contains("情感") || keywords.contains("感受") -> "对方正在表达情感，建议给予理解和支持，避免过度分析。"
            keywords.contains("问题") || keywords.contains("困难") -> "对方可能遇到困难，建议提供帮助和支持，避免直接给出解决方案。"
            keywords.contains("计划") || keywords.contains("安排") -> "对方在讨论计划，建议关注细节和时间安排，提供实用建议。"
            else -> "对方正在进行一般性交流，建议保持友好态度，适当表达自己的观点。"
        }
    }
    
    private fun extractKeywords(jsonMap: Map<String, Any>, rawJson: String): Set<String> {
        val keywords = mutableSetOf<String>()
        
        jsonMap.values.forEach { value ->
            if (value is String && value.length < 50) {
                keywords.addAll(value.split(Regex("[\\s,，。！？；：]+")).filter { it.isNotBlank() })
            }
        }
        
        val commonKeywords = setOf("工作", "生活", "情感", "问题", "建议", "感谢", "计划", "困难", "项目", "感受")
        commonKeywords.forEach { keyword ->
            if (rawJson.contains(keyword)) {
                keywords.add(keyword)
            }
        }
        
        return keywords
    }
    
    // 以下是SafetyCheckResult和ExtractedData的辅助方法
    // 为了简化代码，这里只实现基本版本，完整实现可以参考原有代码
    
    private fun extractStandardIsSafe(jsonMap: Map<String, Any>): Boolean? {
        return (jsonMap["isSafe"] as? Boolean)
    }
    
    private fun extractStandardTriggeredRisks(jsonMap: Map<String, Any>): List<String>? {
        return (jsonMap["triggeredRisks"] as? List<String>)?.filter { it.isNotBlank() }
    }
    
    private fun extractStandardSuggestion(jsonMap: Map<String, Any>): String? {
        return (jsonMap["suggestion"] as? String)?.takeIf { it.isNotBlank() }
    }
    
    private fun extractChineseIsSafe(jsonMap: Map<String, Any>): Boolean? {
        val chineseFieldNames = listOf("是否安全", "安全", "安全性")
        for (fieldName in chineseFieldNames) {
            (jsonMap[fieldName] as? Boolean)?.let { return it }
            
            (jsonMap[fieldName] as? String)?.let {
                return when (it.lowercase()) {
                    "true", "yes", "是", "安全" -> true
                    "false", "no", "否", "不安全" -> false
                    else -> null
                }
            }
        }
        return null
    }
    
    private fun extractChineseTriggeredRisks(jsonMap: Map<String, Any>): List<String>? {
        val chineseFieldNames = listOf("触发的风险", "风险列表", "雷区列表", "触发雷区")
        for (fieldName in chineseFieldNames) {
            (jsonMap[fieldName] as? List<String>)?.let {
                return it.filter { risk -> risk.isNotBlank() }
            }
        }
        return null
    }
    
    private fun extractChineseSuggestion(jsonMap: Map<String, Any>): String? {
        val chineseFieldNames = listOf("建议", "修改建议", "修正建议", "优化建议")
        for (fieldName in chineseFieldNames) {
            (jsonMap[fieldName] as? String)?.let {
                if (it.isNotBlank()) return it
            }
        }
        return null
    }
    
    private fun extractVariantIsSafe(jsonMap: Map<String, Any>): Boolean? {
        val variantFieldNames = listOf("safe", "security", "check_result", "result")
        for (fieldName in variantFieldNames) {
            (jsonMap[fieldName] as? Boolean)?.let { return it }
        }
        return null
    }
    
    private fun extractVariantTriggeredRisks(jsonMap: Map<String, Any>): List<String>? {
        val variantFieldNames = listOf("risks", "warnings", "alerts", "issues", "problems")
        for (fieldName in variantFieldNames) {
            (jsonMap[fieldName] as? List<String>)?.let {
                return it.filter { risk -> risk.isNotBlank() }
            }
        }
        return null
    }
    
    private fun extractVariantSuggestion(jsonMap: Map<String, Any>): String? {
        val variantFieldNames = listOf("recommendation", "advice", "tip", "guidance", "instruction")
        for (fieldName in variantFieldNames) {
            (jsonMap[fieldName] as? String)?.let {
                if (it.isNotBlank()) return it
            }
        }
        return null
    }
    
    private fun extractFromNestedStructureForSafetyCheck(jsonMap: Map<String, Any>): ExtractedSafetyCheckData {
        var isSafe: Boolean? = null
        var triggeredRisks: List<String>? = null
        var suggestion: String? = null
        
        val checkObj = jsonMap["check"] as? Map<String, Any>
        if (checkObj != null) {
            isSafe = checkObj["isSafe"] as? Boolean
            triggeredRisks = checkObj["triggeredRisks"] as? List<String>
            suggestion = checkObj["suggestion"] as? String
        }
        
        return ExtractedSafetyCheckData(
            isSafe = isSafe ?: true,
            triggeredRisks = triggeredRisks ?: emptyList(),
            suggestion = suggestion ?: "安全检查完成，未发现明显风险"
        )
    }
    
    private fun extractFromArrayForSafetyCheck(jsonMap: Map<String, Any>): List<String>? {
        val arrayFieldNames = listOf("risks", "warnings", "alerts", "issues", "problems", "risk_list")
        for (fieldName in arrayFieldNames) {
            val array = jsonMap[fieldName] as? List<*>
            if (!array.isNullOrEmpty()) {
                val stringElements = array.mapNotNull { it as? String }.filter { it.isNotBlank() }
                if (stringElements.isNotEmpty()) {
                    return stringElements
                }
            }
        }
        return null
    }
    
    private fun inferFromTextContentForSafetyCheck(rawJson: String): ExtractedSafetyCheckData {
        var isSafe: Boolean? = null
        var triggeredRisks: List<String>? = null
        var suggestion: String? = null
        
        val lines = rawJson.split("\n")
        
        for (line in lines) {
            if (isSafe == null && (line.contains("安全") || line.contains("safe"))) {
                isSafe = when {
                    line.contains("不安全") || line.contains("unsafe") || line.contains("风险") -> false
                    line.contains("安全") || line.contains("safe") -> true
                    else -> null
                }
            }
            if (suggestion == null && (line.contains("建议") || line.contains("suggestion"))) {
                suggestion = line.substringAfter(":").substringAfter("：").trim()
            }
        }
        
        val riskPattern = Regex("""(?:风险|雷区|问题|issue|risk)[:：]\s*([^\n]+)""")
        val riskMatches = riskPattern.findAll(rawJson).map { it.groupValues[1].trim() }.toList()
        if (riskMatches.isNotEmpty()) {
            triggeredRisks = riskMatches
        }
        
        if (isSafe == null) {
            val lowerText = rawJson.lowercase()
            isSafe = when {
                lowerText.contains("危险") || lowerText.contains("高风险") || lowerText.contains("严重") -> false
                lowerText.contains("安全") || lowerText.contains("无风险") || lowerText.contains("正常") -> true
                else -> null
            }
        }
        
        return ExtractedSafetyCheckData(
            isSafe = isSafe ?: true,
            triggeredRisks = triggeredRisks ?: emptyList(),
            suggestion = suggestion ?: "安全检查完成，未发现明显风险"
        )
    }
    
    private fun generateDefaultIsSafe(jsonMap: Map<String, Any>, rawJson: String): Boolean {
        val hasRiskIndicators = jsonMap.keys.any { key ->
            key.lowercase().contains("risk") ||
            key.lowercase().contains("danger") ||
            key.lowercase().contains("warning")
        }
        
        val hasRiskKeywords = rawJson.lowercase().contains("风险") ||
                              rawJson.lowercase().contains("危险") ||
                              rawJson.lowercase().contains("警告")
        
        return !(hasRiskIndicators || hasRiskKeywords)
    }
    
    private fun generateDefaultTriggeredRisks(jsonMap: Map<String, Any>, rawJson: String): List<String> {
        val risks = mutableListOf<String>()
        
        jsonMap.forEach { (key, value) ->
            if (key.lowercase().contains("risk") || key.lowercase().contains("warning")) {
                when (value) {
                    is String -> if (value.isNotBlank()) risks.add(value)
                    is List<*> -> risks.addAll(value.mapNotNull { it?.toString() }.filter { it.isNotBlank() })
                }
            }
        }
        
        if (risks.isEmpty()) {
            val lowerText = rawJson.lowercase()
            when {
                lowerText.contains("敏感") || lowerText.contains("隐私") -> risks.add("避免谈论隐私话题")
                lowerText.contains("政治") || lowerText.contains("宗教") -> risks.add("避免政治宗教话题")
                lowerText.contains("前任") || lowerText.contains("ex") -> risks.add("避免谈论前任")
                lowerText.contains("收入") || lowerText.contains("money") -> risks.add("避免直接询问收入")
            }
        }
        
        return risks.distinct()
    }
    
    private fun generateDefaultSuggestion(jsonMap: Map<String, Any>, rawJson: String): String {
        val isSafe = generateDefaultIsSafe(jsonMap, rawJson)
        
        return if (isSafe) {
            "内容检查通过，未发现明显风险，可以发送。"
        } else {
            "检测到潜在风险，建议修改后再发送。"
        }
    }
    
    // ExtractedData的辅助方法
    private fun extractStandardFacts(jsonMap: Map<String, Any>): Map<String, String>? {
        return (jsonMap["facts"] as? Map<String, Any>)?.let { factsMap ->
            flattenFacts(factsMap)
        }
    }
    
    private fun extractStandardRedTags(jsonMap: Map<String, Any>): List<String>? {
        return (jsonMap["redTags"] as? List<String>)?.let { tags ->
            deduplicateTags(tags)
        }
    }
    
    private fun extractStandardGreenTags(jsonMap: Map<String, Any>): List<String>? {
        return (jsonMap["greenTags"] as? List<String>)?.let { tags ->
            deduplicateTags(tags)
        }
    }
    
    private fun extractChineseFacts(jsonMap: Map<String, Any>): Map<String, String>? {
        val chineseFieldNames = listOf("事实", "事实信息", "基本信息", "个人资料", "用户信息")
        for (fieldName in chineseFieldNames) {
            (jsonMap[fieldName] as? Map<String, Any>)?.let { factsMap ->
                return flattenFacts(factsMap)
            }
        }
        return null
    }
    
    private fun extractChineseRedTags(jsonMap: Map<String, Any>): List<String>? {
        val chineseFieldNames = listOf("红色标签", "雷区", "风险标签", "红标签", "不要做的事", "敏感话题")
        for (fieldName in chineseFieldNames) {
            (jsonMap[fieldName] as? List<String>)?.let {
                return deduplicateTags(it)
            }
        }
        return null
    }
    
    private fun extractChineseGreenTags(jsonMap: Map<String, Any>): List<String>? {
        val chineseFieldNames = listOf("绿色标签", "策略", "策略标签", "绿标签", "推荐做法", "沟通技巧")
        for (fieldName in chineseFieldNames) {
            (jsonMap[fieldName] as? List<String>)?.let {
                return deduplicateTags(it)
            }
        }
        return null
    }
    
    private fun extractVariantFacts(jsonMap: Map<String, Any>): Map<String, String>? {
        val variantFieldNames = listOf("information", "data", "profile", "user_profile", "personal_info")
        for (fieldName in variantFieldNames) {
            (jsonMap[fieldName] as? Map<String, Any>)?.let { factsMap ->
                return flattenFacts(factsMap)
            }
        }
        return null
    }
    
    private fun extractVariantRedTags(jsonMap: Map<String, Any>): List<String>? {
        val variantFieldNames = listOf("risks", "warnings", "alerts", "donts", "avoid", "red_flags")
        for (fieldName in variantFieldNames) {
            (jsonMap[fieldName] as? List<String>)?.let {
                return deduplicateTags(it)
            }
        }
        return null
    }
    
    private fun extractVariantGreenTags(jsonMap: Map<String, Any>): List<String>? {
        val variantFieldNames = listOf("recommendations", "suggestions", "tips", "dos", "best_practices", "green_flags")
        for (fieldName in variantFieldNames) {
            (jsonMap[fieldName] as? List<String>)?.let {
                return deduplicateTags(it)
            }
        }
        return null
    }
    
    private fun extractFromNestedStructureForExtractedData(jsonMap: Map<String, Any>): ExtractedExtractedData {
        var facts: Map<String, String>? = null
        var redTags: List<String>? = null
        var greenTags: List<String>? = null
        
        val dataObj = jsonMap["data"] as? Map<String, Any>
        if (dataObj != null) {
            if (facts == null) facts = (dataObj["facts"] as? Map<String, Any>)?.let { flattenFacts(it) }
            if (redTags == null) redTags = (dataObj["redTags"] as? List<String>)?.let { deduplicateTags(it) }
            if (greenTags == null) greenTags = (dataObj["greenTags"] as? List<String>)?.let { deduplicateTags(it) }
        }
        
        return ExtractedExtractedData(
            facts = facts ?: emptyMap(),
            redTags = redTags ?: emptyList(),
            greenTags = greenTags ?: emptyList()
        )
    }
    
    private fun extractFromArrayForExtractedData(jsonMap: Map<String, Any>): ExtractedExtractedData {
        var redTags: List<String>? = null
        var greenTags: List<String>? = null
        
        val arrayFieldNames = listOf("tags", "labels", "categories", "items")
        for (fieldName in arrayFieldNames) {
            val array = jsonMap[fieldName] as? List<*>
            if (!array.isNullOrEmpty()) {
                val stringElements = array.mapNotNull { it as? String }.filter { it.isNotBlank() }
                if (stringElements.isNotEmpty()) {
                    val (red, green) = stringElements.partition { element ->
                        element.contains("不要") || element.contains("避免") || element.contains("风险") ||
                        element.lowercase().contains("don't") || element.lowercase().contains("avoid") ||
                        element.lowercase().contains("risk") || element.lowercase().contains("warning")
                    }
                    
                    if (redTags == null && red.isNotEmpty()) redTags = red.distinct()
                    if (greenTags == null && green.isNotEmpty()) greenTags = green.distinct()
                }
            }
        }
        
        return ExtractedExtractedData(
            facts = emptyMap(),
            redTags = redTags ?: emptyList(),
            greenTags = greenTags ?: emptyList()
        )
    }
    
    private fun inferFromTextContentForExtractedData(rawJson: String): ExtractedExtractedData {
        var facts: Map<String, String>? = null
        var redTags: List<String>? = null
        var greenTags: List<String>? = null
        
        val lines = rawJson.split("\n")
        
        val factsMap = mutableMapOf<String, String>()
        for (line in lines) {
            if (line.contains(":") || line.contains("：")) {
                val parts = line.split(Regex("[:：]"), limit = 2)
                if (parts.size == 2) {
                    val key = parts[0].trim().removeSurrounding("\"")
                    val value = parts[1].trim().removeSurrounding("\"")
                    if (key.isNotBlank() && value.isNotBlank() &&
                        (key.contains("生日") || key.contains("爱好") || key.contains("职业") ||
                         key.contains("年龄") || key.contains("性别") || key.contains("地区") ||
                         key.lowercase().contains("birthday") || key.lowercase().contains("hobby") ||
                         key.lowercase().contains("profession") || key.lowercase().contains("age"))) {
                        factsMap[key] = value
                    }
                }
            }
        }
        if (factsMap.isNotEmpty()) {
            facts = factsMap
        }
        
        val redTagsList = mutableListOf<String>()
        val greenTagsList = mutableListOf<String>()
        
        for (line in lines) {
            if (line.contains("不要") || line.contains("避免") || line.contains("禁止") ||
                line.lowercase().contains("don't") || line.lowercase().contains("avoid") ||
                line.lowercase().contains("never") || line.lowercase().contains("prohibited")) {
                val tag = line.substringAfter(":").substringAfter("：").trim().removeSurrounding("\"")
                if (tag.isNotBlank()) {
                    redTagsList.add(tag)
                }
            }
            
            if (line.contains("推荐") || line.contains("建议") || line.contains("可以") ||
                line.lowercase().contains("recommend") || line.lowercase().contains("suggest") ||
                line.lowercase().contains("should") || line.lowercase().contains("good")) {
                val tag = line.substringAfter(":").substringAfter("：").trim().removeSurrounding("\"")
                if (tag.isNotBlank()) {
                    greenTagsList.add(tag)
                }
            }
        }
        
        if (redTagsList.isNotEmpty()) {
            redTags = redTagsList.distinct()
        }
        if (greenTagsList.isNotEmpty()) {
            greenTags = greenTagsList.distinct()
        }
        
        return ExtractedExtractedData(
            facts = facts ?: emptyMap(),
            redTags = redTags ?: emptyList(),
            greenTags = greenTags ?: emptyList()
        )
    }
    
    private fun generateDefaultFacts(jsonMap: Map<String, Any>, rawJson: String): Map<String, String> {
        val facts = mutableMapOf<String, String>()
        
        jsonMap.forEach { (key, value) ->
            if (key.lowercase().contains("info") || key.lowercase().contains("data") ||
                key.contains("信息") || key.contains("资料")) {
                when (value) {
                    is Map<*, *> -> {
                        value.forEach { (k, v) ->
                            if (k is String && v != null) {
                                facts[k] = v.toString()
                            }
                        }
                    }
                    is String -> {
                        if (value.isNotBlank()) {
                            facts[key] = value
                        }
                    }
                }
            }
        }
        
        if (facts.isEmpty()) {
            val infoPattern = Regex("""(?:信息|资料|info|data)[:：]\s*([^\n]+)""")
            val matches = infoPattern.findAll(rawJson).map { it.groupValues[1].trim() }.toList()
            if (matches.isNotEmpty()) {
                facts["提取信息"] = matches.joinToString(", ")
            }
        }
        
        return facts
    }
    
    private fun generateDefaultRedTags(jsonMap: Map<String, Any>, rawJson: String): List<String> {
        val redTags = mutableListOf<String>()
        
        jsonMap.forEach { (key, value) ->
            if (key.lowercase().contains("risk") || key.lowercase().contains("warning") ||
                key.lowercase().contains("danger") || key.contains("风险") || key.contains("警告")) {
                when (value) {
                    is String -> if (value.isNotBlank()) redTags.add(value)
                    is List<*> -> redTags.addAll(value.mapNotNull { it?.toString() }.filter { it.isNotBlank() })
                }
            }
        }
        
        if (redTags.isEmpty()) {
            val lowerText = rawJson.lowercase()
            when {
                lowerText.contains("敏感") || lowerText.contains("隐私") -> redTags.add("避免谈论隐私话题")
                lowerText.contains("政治") || lowerText.contains("宗教") -> redTags.add("避免政治宗教话题")
                lowerText.contains("前任") || lowerText.contains("ex") -> redTags.add("避免谈论前任")
                lowerText.contains("收入") || lowerText.contains("money") -> redTags.add("避免直接询问收入")
            }
        }
        
        return redTags.distinct()
    }
    
    private fun generateDefaultGreenTags(jsonMap: Map<String, Any>, rawJson: String): List<String> {
        val greenTags = mutableListOf<String>()
        
        jsonMap.forEach { (key, value) ->
            if (key.lowercase().contains("suggest") || key.lowercase().contains("recommend") ||
                key.lowercase().contains("tip") || key.contains("建议") || key.contains("推荐")) {
                when (value) {
                    is String -> if (value.isNotBlank()) greenTags.add(value)
                    is List<*> -> greenTags.addAll(value.mapNotNull { it?.toString() }.filter { it.isNotBlank() })
                }
            }
        }
        
        if (greenTags.isEmpty()) {
            val lowerText = rawJson.lowercase()
            when {
                lowerText.contains("爱好") || lowerText.contains("兴趣") -> greenTags.add("分享兴趣爱好")
                lowerText.contains("工作") || lowerText.contains("career") -> greenTags.add("讨论工作话题")
                lowerText.contains("旅行") || lowerText.contains("travel") -> greenTags.add("分享旅行经历")
                lowerText.contains("美食") || lowerText.contains("food") -> greenTags.add("讨论美食话题")
            }
        }
        
        return greenTags.distinct()
    }
    
    private fun flattenFacts(factsMap: Map<String, Any>): Map<String, String> {
        val result = mutableMapOf<String, String>()
        
        factsMap.forEach { (key, value) ->
            when (value) {
                is String -> {
                    if (value.isNotBlank()) {
                        result[key] = value
                    }
                }
                is Number -> {
                    result[key] = value.toString()
                }
                is Boolean -> {
                    result[key] = value.toString()
                }
                is Map<*, *> -> {
                    try {
                        val jsonAdapter = moshi.adapter<Map<String, Any>>(
                            Types.newParameterizedType(
                                Map::class.java,
                                String::class.java,
                                Any::class.java
                            )
                        )
                        val jsonString = jsonAdapter.toJson(value as Map<String, Any>)
                        result[key] = jsonString
                    } catch (e: Exception) {
                        Log.w(TAG, "无法序列化嵌套对象: key=$key", e)
                        result[key] = value.toString()
                    }
                }
                is List<*> -> {
                    val listString = value.joinToString(", ") { it.toString() }
                    if (listString.isNotBlank()) {
                        result[key] = listString
                    }
                }
                else -> {
                    val stringValue = value.toString()
                    if (stringValue.isNotBlank()) {
                        result[key] = stringValue
                    }
                }
            }
        }
        
        return result
    }
    
    private fun deduplicateTags(tags: List<String>): List<String> {
        val originalSize = tags.size
        
        val deduplicated = tags
            .filter { it.isNotBlank() }
            .map { it.trim() }
            .toCollection(LinkedHashSet())
            .toList()
        
        if (deduplicated.size < originalSize) {
            Log.d(TAG, "标签去重: 原始${originalSize}个 -> 去重后${deduplicated.size}个")
        }
        
        return deduplicated
    }
    
    // 部分结果增强方法
    private fun enhancePartialAnalysisResult(partial: AnalysisResult, context: FallbackContext): AnalysisResult {
        var replySuggestion = partial.replySuggestion
        var strategyAnalysis = partial.strategyAnalysis
        var riskLevel = partial.riskLevel
        
        if (replySuggestion.isBlank()) {
            replySuggestion = generateReplySuggestion(emptyMap(), context.originalJson)
        }
        if (strategyAnalysis.isBlank()) {
            strategyAnalysis = generateStrategyAnalysis(emptyMap(), context.originalJson)
        }
        
        return AnalysisResult(replySuggestion, strategyAnalysis, riskLevel)
    }
    
    private fun enhancePartialSafetyCheckResult(partial: SafetyCheckResult, context: FallbackContext): SafetyCheckResult {
        var isSafe = partial.isSafe
        var triggeredRisks = partial.triggeredRisks
        var suggestion = partial.suggestion
        
        if (triggeredRisks.isEmpty()) {
            triggeredRisks = generateDefaultTriggeredRisks(emptyMap(), context.originalJson)
        }
        if (suggestion.isBlank()) {
            suggestion = generateDefaultSuggestion(emptyMap(), context.originalJson)
        }
        
        return SafetyCheckResult(isSafe, triggeredRisks, suggestion)
    }
    
    private fun enhancePartialExtractedData(partial: ExtractedData, context: FallbackContext): ExtractedData {
        var facts = partial.facts
        var redTags = partial.redTags
        var greenTags = partial.greenTags
        
        if (facts.isEmpty()) {
            facts = generateDefaultFacts(emptyMap(), context.originalJson)
        }
        if (redTags.isEmpty()) {
            redTags = generateDefaultRedTags(emptyMap(), context.originalJson)
        }
        if (greenTags.isEmpty()) {
            greenTags = generateDefaultGreenTags(emptyMap(), context.originalJson)
        }
        
        return ExtractedData(facts, redTags, greenTags)
    }
    
    // 数据类定义
    private data class ExtractedAnalysisData(
        val replySuggestion: String,
        val strategyAnalysis: String,
        val riskLevel: RiskLevel
    )
    
    private data class ExtractedSafetyCheckData(
        val isSafe: Boolean,
        val triggeredRisks: List<String>,
        val suggestion: String
    )
    
    private data class ExtractedExtractedData(
        val facts: Map<String, String>,
        val redTags: List<String>,
        val greenTags: List<String>
    )
    
    private data class ParseQuality(
        val completeness: Double,
        val accuracy: Double,
        val confidence: Double,
        val overallScore: Double
    )
}