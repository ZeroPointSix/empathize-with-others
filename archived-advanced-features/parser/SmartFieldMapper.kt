package com.empathy.ai.data.parser

import android.content.Context
import android.util.Log
import com.empathy.ai.data.repository.FieldMappingConfig
import java.util.concurrent.ConcurrentHashMap

/**
 * 智能字段映射器实现
 * 
 * 提供高级的字段名映射功能，支持模糊匹配和动态学习
 * 基于原有AiRepositoryImpl中的mapChineseFieldNames方法重构
 */
class SmartFieldMapper : FieldMapper {
    
    companion object {
        private const val TAG = "SmartFieldMapper"
    }
    
    // 动态映射存储
    private val dynamicMappings = ConcurrentHashMap<String, List<String>>()
    
    // 缓存的映射配置
    private var cachedMappings: Map<String, List<String>>? = null
    
    // 缓存的模糊匹配配置
    private var cachedFuzzyMatchingEnabled: Boolean? = null
    private var cachedFuzzyMatchingThreshold: Double? = null
    
    override fun mapFields(json: String, context: MappingContext): String {
        if (context.enableDetailedLogging) {
            Log.d(TAG, "开始字段名映射，原始长度: ${json.length}")
        }
        
        try {
            // 加载字段映射配置
            val mappings = loadMappings(context)
            
            // 获取模糊匹配配置
            val enableFuzzyMatching = context.enableFuzzyMatching
            val fuzzyThreshold = context.fuzzyThreshold
            
            var result = json
            var mappingCount = 0
            var fuzzyMappingCount = 0
            val mappedFields = mutableListOf<Pair<String, String>>()
            val fuzzyMappedFields = mutableListOf<Triple<String, String, Double>>()
            
            // 提取JSON中的所有字段名
            val fieldNamePattern = Regex("\"([^\"]+)\"")
            val fieldNames = fieldNamePattern.findAll(result).map { it.groupValues[1] }.toSet()
            
            // 应用字段名映射（精确匹配）
            mappings.forEach { (englishName, chineseNames) ->
                chineseNames.forEach { chineseName ->
                    val pattern = "\"$chineseName\""
                    if (result.contains(pattern)) {
                        result = result.replace(pattern, "\"$englishName\"")
                        mappingCount++
                        mappedFields.add(chineseName to englishName)
                    }
                }
            }
            
            // 如果启用了模糊匹配，处理未映射的字段
            if (enableFuzzyMatching) {
                // 重新提取字段名，因为可能已经被精确匹配替换
                val remainingFieldNames = fieldNamePattern.findAll(result).map { it.groupValues[1] }.toSet()
                
                for (fieldName in remainingFieldNames) {
                    // 跳过已经是英文字段名的字段
                    if (fieldName.matches(Regex("^[a-zA-Z_][a-zA-Z0-9_]*$"))) {
                        continue
                    }
                    
                    // 尝试模糊匹配
                    var bestMatch: String? = null
                    var bestEnglishName: String? = null
                    var bestSimilarity = 0.0
                    
                    mappings.forEach { (englishName, chineseNames) ->
                        val match = fuzzyMatchField(fieldName, chineseNames, fuzzyThreshold)
                        if (match != null) {
                            val similarity = calculateSimilarity(fieldName, match)
                            if (similarity > bestSimilarity) {
                                bestSimilarity = similarity
                                bestMatch = match
                                bestEnglishName = englishName
                            }
                        }
                    }
                    
                    // 如果找到模糊匹配，进行替换
                    if (bestMatch != null && bestEnglishName != null) {
                        val pattern = "\"$fieldName\""
                        result = result.replace(pattern, "\"$bestEnglishName\"")
                        fuzzyMappingCount++
                        fuzzyMappedFields.add(Triple(fieldName, bestEnglishName, bestSimilarity))
                        
                        if (context.enableDetailedLogging) {
                            Log.d(TAG, "模糊匹配: \"$fieldName\" -> \"$bestEnglishName\" (相似度: ${"%.2f".format(bestSimilarity)})")
                        }
                    }
                }
            }
            
            // 映射统计日志
            if (mappingCount > 0 || fuzzyMappingCount > 0) {
                if (context.enableDetailedLogging) {
                    Log.d(TAG, "字段名映射完成，精确映射 $mappingCount 个字段，模糊映射 $fuzzyMappingCount 个字段")
                    mappedFields.forEach { (chinese, english) ->
                        Log.d(TAG, "  精确: $chinese -> $english")
                    }
                    fuzzyMappedFields.forEach { (original, english, similarity) ->
                        Log.d(TAG, "  模糊: $original -> $english (相似度: ${"%.2f".format(similarity)})")
                    }
                } else {
                    Log.i(TAG, "字段名映射完成，精确映射 $mappingCount 个字段，模糊映射 $fuzzyMappingCount 个字段")
                }
            }
            
            // 检测是否还有未映射的中文字段（仅在详细日志模式下执行）
            if (context.enableDetailedLogging) {
                val chineseFieldPattern = Regex("\"[\\u4e00-\\u9fff]+\"")
                if (chineseFieldPattern.containsMatchIn(result)) {
                    val unmappedFields = chineseFieldPattern.findAll(result)
                        .map { it.value }
                        .distinct()
                        .take(5) // 最多显示 5 个
                        .joinToString(", ")
                    Log.w(TAG, "检测到未映射的中文字段名: $unmappedFields")
                    Log.w(TAG, "建议更新配置文件 field_mappings.json")
                }
            }
            
            return result
            
        } catch (e: Exception) {
            Log.e(TAG, "字段名映射失败", e)
            return json // 返回原始字符串作为降级方案
        }
    }
    
    override fun addMapping(english: String, chinese: List<String>) {
        dynamicMappings[english] = chinese
        // 清除缓存，强制重新加载
        cachedMappings = null
        Log.d(TAG, "添加动态映射: $english -> $chinese")
    }
    
    override fun getAllMappings(): Map<String, List<String>> {
        val baseMappings = loadMappings(MappingContext())
        val allMappings = mutableMapOf<String, List<String>>()
        
        // 添加基础映射
        allMappings.putAll(baseMappings)
        
        // 添加动态映射
        allMappings.putAll(dynamicMappings)
        
        return allMappings
    }
    
    override fun clearMappings() {
        dynamicMappings.clear()
        cachedMappings = null
        Log.d(TAG, "清除所有字段映射")
    }
    
    /**
     * 加载字段映射配置
     */
    private fun loadMappings(context: MappingContext): Map<String, List<String>> {
        // 如果有缓存且不需要重新加载，直接返回
        if (cachedMappings != null && context.androidContext == null) {
            if (context.enableDetailedLogging) {
                Log.d(TAG, "使用缓存的字段映射配置")
            }
            return cachedMappings!!
        }
        
        return try {
            val mappings = if (context.androidContext != null) {
                FieldMappingConfig.load(context.androidContext)
            } else {
                FieldMappingConfig.getDefaultMappings()
            }
            
            // 合并动态映射
            val allMappings = mutableMapOf<String, List<String>>()
            allMappings.putAll(mappings)
            allMappings.putAll(dynamicMappings)
            
            // 缓存结果（仅在没有Android上下文时缓存）
            if (context.androidContext == null) {
                cachedMappings = allMappings
                cachedFuzzyMatchingEnabled = context.enableFuzzyMatching
                cachedFuzzyMatchingThreshold = context.fuzzyThreshold
            }
            
            if (context.enableDetailedLogging) {
                Log.d(TAG, "字段映射配置加载成功，共 ${allMappings.size} 个字段映射")
            }
            
            allMappings
            
        } catch (e: Exception) {
            Log.e(TAG, "加载字段映射配置失败，使用默认配置", e)
            val defaultMappings = FieldMappingConfig.getDefaultMappings()
            val allMappings = mutableMapOf<String, List<String>>()
            allMappings.putAll(defaultMappings)
            allMappings.putAll(dynamicMappings)
            allMappings
        }
    }
    
    /**
     * 模糊匹配字段名
     */
    private fun fuzzyMatchField(fieldName: String, candidateFields: List<String>, threshold: Double): String? {
        var bestMatch: String? = null
        var bestSimilarity = 0.0
        
        for (candidate in candidateFields) {
            val similarity = calculateSimilarity(fieldName, candidate)
            if (similarity > bestSimilarity && similarity >= threshold) {
                bestSimilarity = similarity
                bestMatch = candidate
            }
        }
        
        return bestMatch
    }
    
    /**
     * 计算两个字符串的相似度（基于编辑距离）
     */
    private fun calculateSimilarity(str1: String, str2: String): Double {
        val maxLength = maxOf(str1.length, str2.length)
        if (maxLength == 0) return 1.0
        
        val editDistance = calculateEditDistance(str1.lowercase(), str2.lowercase())
        return 1.0 - (editDistance.toDouble() / maxLength)
    }
    
    /**
     * 计算两个字符串的编辑距离（Levenshtein距离）
     */
    private fun calculateEditDistance(str1: String, str2: String): Int {
        val len1 = str1.length
        val len2 = str2.length
        
        // 创建动态规划表
        val dp = Array(len1 + 1) { IntArray(len2 + 1) }
        
        // 初始化边界条件
        for (i in 0..len1) dp[i][0] = i
        for (j in 0..len2) dp[0][j] = j
        
        // 填充动态规划表
        for (i in 1..len1) {
            for (j in 1..len2) {
                val cost = if (str1[i - 1] == str2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,      // 删除
                    dp[i][j - 1] + 1,      // 插入
                    dp[i - 1][j - 1] + cost // 替换
                )
            }
        }
        
        return dp[len1][len2]
    }
    
    /**
     * 学习新的字段映射
     * 
     * 基于使用情况动态学习新的映射关系
     */
    fun learnMapping(englishField: String, chineseCandidates: List<String>, confidence: Double = 0.8) {
        if (confidence < 0.5) {
            Log.w(TAG, "学习映射置信度过低，跳过: $englishField -> $chineseCandidates")
            return
        }
        
        // 基于现有映射推断新映射
        val existingMappings = getAllMappings()
        val similarities = chineseCandidates.map { chinese ->
            chinese to calculateSimilarity(chinese, existingMappings[englishField] ?: emptyList())
        }
        
        val bestMatch = similarities.maxByOrNull { it.second }
        
        if (bestMatch != null && bestMatch.second > confidence) {
            addMapping(englishField, listOf(bestMatch.first))
            Log.i(TAG, "学习新映射: $englishField -> ${bestMatch.first}")
        }
    }
    
    /**
     * 获取映射统计信息
     */
    fun getMappingStatistics(): MappingStatistics {
        val allMappings = getAllMappings()
        val totalMappings = allMappings.size
        val totalChineseVariants = allMappings.values.sumOf { it.size }
        
        // 计算平均变体数量
        val averageVariants = if (totalMappings > 0) {
            totalChineseVariants.toDouble() / totalMappings
        } else {
            0.0
        }
        
        // 计算动态映射比例
        val dynamicMappingCount = dynamicMappings.size
        val dynamicMappingRatio = if (totalMappings > 0) {
            dynamicMappingCount.toDouble() / totalMappings
        } else {
            0.0
        }
        
        return MappingStatistics(
            totalMappings = totalMappings,
            totalChineseVariants = totalChineseVariants,
            averageVariants = averageVariants,
            dynamicMappingCount = dynamicMappingCount,
            dynamicMappingRatio = dynamicMappingRatio
        )
    }
    
    /**
     * 映射统计信息
     */
    data class MappingStatistics(
        val totalMappings: Int,
        val totalChineseVariants: Int,
        val averageVariants: Double,
        val dynamicMappingCount: Int,
        val dynamicMappingRatio: Double
    )
}