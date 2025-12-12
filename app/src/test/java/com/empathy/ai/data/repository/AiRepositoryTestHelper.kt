package com.empathy.ai.data.repository

import com.empathy.ai.data.parser.*
import com.empathy.ai.domain.model.AnalysisResult
import com.empathy.ai.domain.model.RiskLevel
import com.empathy.ai.domain.model.SafetyCheckResult
import com.empathy.ai.domain.usecase.ExtractedData
import com.squareup.moshi.Moshi

/**
 * AI仓库测试辅助类
 * 
 * 提供对AiRepositoryImpl私有方法的公共访问，减少测试中的反射使用
 * 这些方法原本是AiRepositoryImpl的私有方法，现在迁移为公共测试接口
 */
object AiRepositoryTestHelper {
    
    /**
     * 测试专用的JSON清洗器接口
     */
    interface TestJsonCleaner {
        fun clean(json: String, context: CleaningContext = CleaningContext()): String
        fun isValid(json: String): Boolean
        fun extractJsonObject(text: String): String
    }
    
    /**
     * 测试专用的字段映射器接口
     */
    interface TestFieldMapper {
        fun mapFields(json: String, context: MappingContext = MappingContext()): String
        fun addMapping(english: String, chinese: List<String>)
        fun getAllMappings(): Map<String, List<String>>
        fun clearMappings()
    }
    
    /**
     * 测试专用的降级处理器接口
     */
    interface TestFallbackHandler {
        fun <T> handleParsingFailure(error: Exception, targetType: Class<T>, context: FallbackContext = FallbackContext()): FallbackResult<T>
        fun <T> handlePartialResult(partialData: Any, targetType: Class<T>, context: FallbackContext = FallbackContext()): FallbackResult<T>
        fun <T> generateDefaultValue(targetType: Class<T>, context: FallbackContext = FallbackContext()): T
    }
    
    /**
     * 测试专用的解析器接口
     */
    interface TestAiResponseParser {
        fun parseAnalysisResult(json: String, context: ParsingContext = ParsingContext()): Result<AnalysisResult>
        fun parseSafetyCheckResult(json: String, context: ParsingContext = ParsingContext()): Result<SafetyCheckResult>
        fun parseExtractedData(json: String, context: ParsingContext = ParsingContext()): Result<ExtractedData>
        fun <T> parse(json: String, targetType: Class<T>, context: ParsingContext = ParsingContext()): Result<T>
    }
    
    /**
     * 创建测试专用的JSON清洗器实现
     */
    fun createTestJsonCleaner(): TestJsonCleaner {
        return object : TestJsonCleaner {
            override fun clean(json: String, context: CleaningContext): String {
                // 使用EnhancedJsonCleaner的实际实现
                val cleaner = EnhancedJsonCleaner()
                return cleaner.clean(json, context)
            }
            
            override fun isValid(json: String): Boolean {
                // 使用EnhancedJsonCleaner的实际实现
                val cleaner = EnhancedJsonCleaner()
                return cleaner.isValid(json)
            }
            
            override fun extractJsonObject(text: String): String {
                // 使用EnhancedJsonCleaner的实际实现
                val cleaner = EnhancedJsonCleaner()
                return cleaner.extractJsonObject(text)
            }
        }
    }
    
    /**
     * 创建测试专用的字段映射器实现
     */
    fun createTestFieldMapper(): TestFieldMapper {
        return object : TestFieldMapper {
            override fun mapFields(json: String, context: MappingContext): String {
                // 使用SmartFieldMapper的实际实现
                val mapper = SmartFieldMapper()
                return mapper.mapFields(json, context)
            }
            
            override fun addMapping(english: String, chinese: List<String>) {
                // 使用SmartFieldMapper的实际实现
                val mapper = SmartFieldMapper()
                mapper.addMapping(english, chinese)
            }
            
            override fun getAllMappings(): Map<String, List<String>> {
                // 使用SmartFieldMapper的实际实现
                val mapper = SmartFieldMapper()
                return mapper.getAllMappings()
            }
            
            override fun clearMappings() {
                // 使用SmartFieldMapper的实际实现
                val mapper = SmartFieldMapper()
                mapper.clearMappings()
            }
        }
    }
    
    /**
     * 创建测试专用的降级处理器实现
     */
    fun createTestFallbackHandler(): TestFallbackHandler {
        return object : TestFallbackHandler {
            override fun <T> handleParsingFailure(error: Exception, targetType: Class<T>, context: FallbackContext): FallbackResult<T> {
                // 使用MultiLevelFallbackHandler的实际实现
                val handler = MultiLevelFallbackHandler()
                return handler.handleParsingFailure(error, targetType, context)
            }
            
            override fun <T> handlePartialResult(partialData: Any, targetType: Class<T>, context: FallbackContext): FallbackResult<T> {
                // 使用MultiLevelFallbackHandler的实际实现
                val handler = MultiLevelFallbackHandler()
                return handler.handlePartialResult(partialData, targetType, context)
            }
            
            override fun <T> generateDefaultValue(targetType: Class<T>, context: FallbackContext): T {
                // 使用MultiLevelFallbackHandler的实际实现
                val handler = MultiLevelFallbackHandler()
                return handler.generateDefaultValue(targetType, context)
            }
        }
    }
    
    /**
     * 创建测试专用的解析器实现
     */
    fun createTestAiResponseParser(): TestAiResponseParser {
        return object : TestAiResponseParser {
            override fun parseAnalysisResult(json: String, context: ParsingContext): Result<AnalysisResult> {
                // 使用StrategyBasedAiResponseParser的实际实现
                val parser = AiResponseParserFactory.createDefaultStrategyBasedParser()
                return parser.parseAnalysisResult(json, context)
            }
            
            override fun parseSafetyCheckResult(json: String, context: ParsingContext): Result<SafetyCheckResult> {
                // 使用StrategyBasedAiResponseParser的实际实现
                val parser = AiResponseParserFactory.createDefaultStrategyBasedParser()
                return parser.parseSafetyCheckResult(json, context)
            }
            
            override fun parseExtractedData(json: String, context: ParsingContext): Result<ExtractedData> {
                // 使用StrategyBasedAiResponseParser的实际实现
                val parser = AiResponseParserFactory.createDefaultStrategyBasedParser()
                return parser.parseExtractedData(json, context)
            }
            
            override fun <T> parse(json: String, targetType: Class<T>, context: ParsingContext): Result<T> {
                // 使用StrategyBasedAiResponseParser的实际实现
                val parser = AiResponseParserFactory.createDefaultStrategyBasedParser()
                return parser.parse(json, targetType, context)
            }
        }
    }
    
    /**
     * 测试专用的字段提取方法
     * 这些方法原本是AiRepositoryImpl的私有方法，现在提供为公共测试接口
     */
    object FieldExtractionTestUtils {
        
        /**
         * 提取标准字段
         */
        fun extractStandardField(jsonMap: Map<String, Any>, fieldName: String): String {
            return (jsonMap[fieldName] as? String)?.takeIf { it.isNotBlank() } ?: ""
        }
        
        /**
         * 提取标准风险等级
         */
        fun extractStandardRiskLevel(jsonMap: Map<String, Any>): RiskLevel {
            val riskLevelStr = jsonMap["riskLevel"] as? String
            return when (riskLevelStr?.uppercase()) {
                "SAFE" -> RiskLevel.SAFE
                "WARNING" -> RiskLevel.WARNING
                "DANGER" -> RiskLevel.DANGER
                else -> RiskLevel.SAFE
            }
        }
        
        /**
         * 提取中文字段
         */
        fun extractChineseField(jsonMap: Map<String, Any>, fieldNames: List<String>): String {
            for (fieldName in fieldNames) {
                val value = jsonMap[fieldName] as? String
                if (!value.isNullOrBlank()) {
                    return value
                }
            }
            return ""
        }
        
        /**
         * 提取中文风险等级
         */
        fun extractChineseRiskLevel(jsonMap: Map<String, Any>): RiskLevel {
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
        
        /**
         * 提取变体字段
         */
        fun extractVariantField(jsonMap: Map<String, Any>, fieldNames: List<String>): String {
            for (fieldName in fieldNames) {
                val value = jsonMap[fieldName] as? String
                if (!value.isNullOrBlank()) {
                    return value
                }
            }
            return ""
        }
        
        /**
         * 从嵌套结构提取
         */
        fun extractFromNestedStructure(jsonMap: Map<String, Any>): ExtractedAnalysisData {
            var replySuggestion = ""
            var strategyAnalysis = ""
            var riskLevel = RiskLevel.SAFE
            
            // 检查常见的嵌套结构
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
            
            // 检查其他可能的嵌套结构
            val responseObj = jsonMap["response"] as? Map<String, Any>
            if (responseObj != null) {
                if (replySuggestion.isBlank()) replySuggestion = responseObj["text"] as? String ?: ""
                if (strategyAnalysis.isBlank()) strategyAnalysis = responseObj["analysis"] as? String ?: ""
            }
            
            return ExtractedAnalysisData(replySuggestion, strategyAnalysis, riskLevel)
        }
        
        /**
         * 从数组字段提取
         */
        fun extractFromArrayField(jsonMap: Map<String, Any>, fieldNames: List<String>): String {
            for (fieldName in fieldNames) {
                val array = jsonMap[fieldName] as? List<*>
                if (!array.isNullOrEmpty()) {
                    // 尝试获取第一个字符串元素
                    val firstElement = array.firstOrNull() as? String
                    if (!firstElement.isNullOrBlank()) {
                        return firstElement
                    }
                    
                    // 如果是对象数组，尝试获取text或content字段
                    val firstObject = array.firstOrNull() as? Map<String, Any>
                    if (firstObject != null) {
                        val text = firstObject["text"] as? String
                        val content = firstObject["content"] as? String
                        if (!text.isNullOrBlank()) return text
                        if (!content.isNullOrBlank()) return content
                    }
                }
            }
            return ""
        }
        
        /**
         * 从文本内容推断
         */
        fun inferFromTextContent(rawJson: String): ExtractedAnalysisData {
            var replySuggestion = ""
            var strategyAnalysis = ""
            var riskLevel = RiskLevel.SAFE
            
            // 尝试从原始文本中提取有用信息
            val lines = rawJson.split("\n")
            
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
            val lowerText = rawJson.lowercase()
            riskLevel = when {
                lowerText.contains("危险") || lowerText.contains("高风险") || lowerText.contains("严重") -> RiskLevel.DANGER
                lowerText.contains("注意") || lowerText.contains("风险") || lowerText.contains("谨慎") -> RiskLevel.WARNING
                else -> RiskLevel.SAFE
            }
            
            return ExtractedAnalysisData(replySuggestion, strategyAnalysis, riskLevel)
        }
        
        /**
         * 生成回复建议
         */
        fun generateReplySuggestion(jsonMap: Map<String, Any>, rawJson: String): String {
            // 尝试从现有内容中提取关键信息
            val keywords = extractKeywords(jsonMap, rawJson)
            
            return when {
                keywords.contains("问题") || keywords.contains("询问") -> "这是一个很好的问题，我需要更多信息来给出准确的回答。"
                keywords.contains("感谢") || keywords.contains("谢谢") -> "不客气，很高兴能帮到你。"
                keywords.contains("建议") || keywords.contains("意见") -> "感谢你的建议，我会认真考虑。"
                keywords.contains("抱歉") || keywords.contains("对不起") -> "没关系，我理解你的情况。"
                else -> "我理解你的意思，让我们继续这个话题。"
            }
        }
        
        /**
         * 生成策略分析
         */
        fun generateStrategyAnalysis(jsonMap: Map<String, Any>, rawJson: String): String {
            val keywords = extractKeywords(jsonMap, rawJson)
            
            return when {
                keywords.contains("工作") || keywords.contains("项目") -> "对方可能正在讨论工作相关话题，建议保持专业态度，提供有价值的见解。"
                keywords.contains("情感") || keywords.contains("感受") -> "对方正在表达情感，建议给予理解和支持，避免过度分析。"
                keywords.contains("问题") || keywords.contains("困难") -> "对方可能遇到困难，建议提供帮助和支持，避免直接给出解决方案。"
                keywords.contains("计划") || keywords.contains("安排") -> "对方在讨论计划，建议关注细节和时间安排，提供实用建议。"
                else -> "对方正在进行一般性交流，建议保持友好态度，适当表达自己的观点。"
            }
        }
        
        /**
         * 从JSON和原始文本中提取关键词
         */
        private fun extractKeywords(jsonMap: Map<String, Any>, rawJson: String): Set<String> {
            val keywords = mutableSetOf<String>()
            
            // 从JSON Map中提取关键词
            jsonMap.values.forEach { value ->
                if (value is String && value.length < 50) { // 只考虑短文本
                    keywords.addAll(value.split(Regex("[\\s,，。！？；：]+")).filter { it.isNotBlank() })
                }
            }
            
            // 从原始文本中提取关键词
            val commonKeywords = setOf("工作", "生活", "情感", "问题", "建议", "感谢", "计划", "困难", "项目", "感受")
            commonKeywords.forEach { keyword ->
                if (rawJson.contains(keyword)) {
                    keywords.add(keyword)
                }
            }
            
            return keywords
        }
    }
    
    /**
     * 测试数据类，用于提取分析结果
     */
    data class ExtractedAnalysisData(
        val replySuggestion: String,
        val strategyAnalysis: String,
        val riskLevel: RiskLevel
    )
    
    /**
     * 测试专用的Moshi实例
     */
    val testMoshi: Moshi = Moshi.Builder().build()
}