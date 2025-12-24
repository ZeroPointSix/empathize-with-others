package com.empathy.ai.data.parser

import com.empathy.ai.domain.model.AnalysisResult
import com.empathy.ai.domain.model.ExtractedData
import com.empathy.ai.domain.model.RiskLevel
import com.empathy.ai.domain.model.SafetyCheckResult

/**
 * 降级处理器接口
 * 
 * 专门处理降级策略，当标准解析失败时提供备用方案
 * 
 * 职责：
 * - 多层次降级策略
 * - 智能默认值生成
 * - 错误恢复机制
 */
interface FallbackHandler {
    
    /**
     * 处理解析失败
     * 
     * @param error 解析错误
     * @param targetType 目标类型
     * @param context 降级上下文
     * @return 降级结果
     */
    fun <T> handleParsingFailure(
        error: Exception,
        targetType: Class<T>,
        context: FallbackContext = FallbackContext()
    ): FallbackResult<T>
    
    /**
     * 处理部分解析结果
     * 
     * @param partialData 部分解析的数据
     * @param targetType 目标类型
     * @param context 降级上下文
     * @return 降级结果
     */
    fun <T> handlePartialResult(
        partialData: Any,
        targetType: Class<T>,
        context: FallbackContext = FallbackContext()
    ): FallbackResult<T>
    
    /**
     * 生成默认值
     * 
     * @param targetType 目标类型
     * @param context 降级上下文
     * @return 默认值
     */
    fun <T> generateDefaultValue(
        targetType: Class<T>,
        context: FallbackContext = FallbackContext()
    ): T
}

/**
 * 降级上下文
 */
data class FallbackContext(
    /**
     * 原始JSON字符串
     */
    val originalJson: String = "",
    
    /**
     * 操作类型
     */
    val operationType: String = "unknown",
    
    /**
     * AI模型名称
     */
    val modelName: String = "unknown",
    
    /**
     * 是否启用智能推断
     */
    val enableIntelligentInference: Boolean = true,
    
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
    fun withProperty(key: String, value: Any): FallbackContext {
        return copy(properties = properties + (key to value))
    }
    
    /**
     * 设置原始JSON
     */
    fun withOriginalJson(json: String): FallbackContext {
        return copy(originalJson = json)
    }
    
    /**
     * 设置操作类型
     */
    fun withOperationType(operationType: String): FallbackContext {
        return copy(operationType = operationType)
    }
    
    /**
     * 设置模型名称
     */
    fun withModelName(modelName: String): FallbackContext {
        return copy(modelName = modelName)
    }
    
    /**
     * 启用智能推断
     */
    fun withIntelligentInference(enabled: Boolean = true): FallbackContext {
        return copy(enableIntelligentInference = enabled)
    }
    
    /**
     * 启用详细日志
     */
    fun withDetailedLogging(enabled: Boolean = true): FallbackContext {
        return copy(enableDetailedLogging = enabled)
    }
}

/**
 * 降级结果
 */
sealed class FallbackResult<T> {
    /**
     * 成功降级
     */
    data class Success<T>(val result: T, val strategy: FallbackStrategy, val confidence: Double) : FallbackResult<T>()
    
    /**
     * 降级失败
     */
    data class Failure<T>(val error: Exception, val attemptedStrategies: List<FallbackStrategy>) : FallbackResult<T>()
    
    /**
     * 获取数据（仅对Success有效）
     */
    fun getData(): T? = when (this) {
        is Success -> result
        is Failure -> null
    }
    
    /**
     * 是否成功
     */
    fun isSuccess(): Boolean = this is Success
    
    /**
     * 是否失败
     */
    fun isFailure(): Boolean = this is Failure
}

/**
 * 降级策略枚举
 */
enum class FallbackStrategy {
    /**
     * 使用默认值
     */
    USE_DEFAULT_VALUES,
    
    /**
     * 使用部分数据
     */
    USE_PARTIAL_DATA,
    
    /**
     * 使用缓存数据
     */
    USE_CACHED_DATA,
    
    /**
     * 重试使用不同模型
     */
    RETRY_WITH_DIFFERENT_MODEL,
    
    /**
     * 简化请求重试
     */
    SIMPLIFY_REQUEST,
    
    /**
     * 提供手动输入选项
     */
    PROVIDE_MANUAL_INPUT,
    
    /**
     * 智能推断
     */
    INTELLIGENT_INFERENCE,
    
    /**
     * 字段提取
     */
    FIELD_EXTRACTION,
    
    /**
     * 组合策略
     */
    COMBINED_STRATEGY
}

/**
 * 默认值提供者
 */
object DefaultValues {
    /**
     * AnalysisResult 默认值
     */
    val ANALYSIS_RESULT = AnalysisResult(
        replySuggestion = "AI 暂时无法生成建议，请重试或切换模型",
        strategyAnalysis = "AI 分析暂时不可用。可能的原因：\n" +
                "• 网络连接问题\n" +
                "• AI 服务暂时不可用\n" +
                "• 响应格式异常\n\n" +
                "建议：\n" +
                "1. 检查网络连接\n" +
                "2. 稍后重试\n" +
                "3. 尝试切换其他 AI 模型",
        riskLevel = RiskLevel.SAFE
    )
    
    /**
     * SafetyCheckResult 默认值
     */
    val SAFETY_CHECK_RESULT = SafetyCheckResult(
        isSafe = true,
        triggeredRisks = emptyList(),
        suggestion = "安全检查暂时不可用，请谨慎发送。\n" +
                "建议手动检查是否触及敏感话题。"
    )
    
    /**
     * ExtractedData 默认值
     */
    val EXTRACTED_DATA = ExtractedData(
        facts = emptyMap(),
        redTags = emptyList(),
        greenTags = emptyList()
    )
    
    /**
     * 获取默认值
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> getDefaultValue(targetType: Class<T>): T {
        return when (targetType) {
            AnalysisResult::class.java -> ANALYSIS_RESULT as T
            SafetyCheckResult::class.java -> SAFETY_CHECK_RESULT as T
            ExtractedData::class.java -> EXTRACTED_DATA as T
            else -> throw IllegalArgumentException("未支持的类型: ${targetType.simpleName}")
        }
    }
}
