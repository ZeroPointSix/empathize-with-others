package com.empathy.ai.data.parser

import android.content.Context
import android.util.Log
import com.empathy.ai.domain.model.AnalysisResult
import com.empathy.ai.domain.model.SafetyCheckResult
import com.empathy.ai.domain.usecase.ExtractedData
import com.empathy.ai.data.observability.ObservabilityManager
import com.empathy.ai.data.observability.ParsingTracer
import com.empathy.ai.data.observability.DetailedLogger
import com.empathy.ai.data.monitoring.AiResponseParserMetrics
import com.empathy.ai.data.monitoring.ParsingPerformanceTracker
import com.empathy.ai.data.learning.FieldMappingLearningEngine
import com.empathy.ai.data.learning.ParsingPatternAnalyzer
import com.squareup.moshi.Moshi
import kotlin.system.measureTimeMillis

/**
 * 增强版基于策略的AI响应解析器
 * 
 * 在原有StrategyBasedAiResponseParser基础上集成：
 * 1. 性能监控系统
 * 2. 学习机制
 * 3. 告警系统
 * 4. 可观测性功能
 * 
 * 特性：
 * - 全面的性能指标收集
 * - 智能字段映射学习
 * - 详细的解析追踪
 * - 自动错误分析和改进建议
 */
class EnhancedStrategyBasedAiResponseParser(
    private val context: Context,
    private val jsonCleaner: JsonCleaner,
    private val fieldMapper: FieldMapper,
    private val fallbackHandler: FallbackHandler,
    private val moshi: Moshi = Moshi.Builder().build(),
    private val observabilityManager: ObservabilityManager = ObservabilityManager.getInstance(context)
) : AiResponseParser {
    
    companion object {
        private const val TAG = "EnhancedStrategyBasedAiResponseParser"
    }
    
    // 获取可观测性组件
    private val metrics = observabilityManager.getMetrics()
    private val performanceTracker = observabilityManager.getPerformanceTracker()
    private val diagnosticCollector = observabilityManager.getDiagnosticCollector()
    private val parsingTracer = observabilityManager.getParsingTracer()
    private val detailedLogger = observabilityManager.getDetailedLogger()
    private val fieldMappingLearningEngine = observabilityManager.getFieldMappingLearningEngine()
    private val parsingPatternAnalyzer = observabilityManager.getParsingPatternAnalyzer()
    
    init {
        // 确保可观测性系统已初始化
        if (!observabilityManager.getConfiguration().enableObservability) {
            Log.w(TAG, "可观测性系统未启用，部分功能将不可用")
        }
    }
    
    override fun parseAnalysisResult(json: String, context: ParsingContext): Result<AnalysisResult> {
        val operationId = context.operationId
        val modelName = context.modelName
        val operationType = context.operationType
        
        // 开始追踪会话
        val traceSession = parsingTracer.startTraceSession(
            operationType = operationType,
            requestId = operationId,
            input = json,
            metadata = mapOf(
                "modelName" to modelName,
                "inputSize" to json.length,
                "targetType" to "AnalysisResult"
            )
        )
        
        // 开始性能跟踪
        val performanceSession = performanceTracker.startTrackingSession(operationId, operationType)
        
        // 记录开始日志
        detailedLogger.info(
            category = DetailedLogger.LogCategory.PARSER,
            message = "开始解析AnalysisResult",
            data = mapOf(
                "operationId" to operationId,
                "modelName" to modelName,
                "inputSize" to json.length
            ),
            tags = setOf("analysis", "start")
        )
        
        var success = false
        var duration = 0L
        var result: Result<AnalysisResult>? = null
        var errorMessage: String? = null
        
        try {
            // 测量解析时间
            duration = measureTimeMillis {
                result = parseAnalysisResultInternal(json, context, traceSession)
            }
            
            success = result!!.isSuccess
            
            // 记录性能指标
            metrics.recordParsingResult(operationType, modelName, success, duration)
            performanceSession.recordCompletion(success, duration)
            
            // 记录解析器事件
            detailedLogger.logParserEvent(
                operationType = operationType,
                requestId = operationId,
                success = success,
                duration = duration,
                inputSize = json.length,
                errorMessage = errorMessage
            )
            
            // 记录诊断信息
            diagnosticCollector.collectParserDiagnostics(
                operationType = operationType,
                requestId = operationId,
                success = success,
                duration = duration,
                errorMessage = errorMessage,
                additionalData = mapOf(
                    "modelName" to modelName,
                    "targetType" to "AnalysisResult"
                )
            )
            
            // 如果成功，收集学习数据
            if (success) {
                collectLearningData(json, result!!.getOrNull(), context)
            }
            
            return result!!
            
        } catch (e: Exception) {
            success = false
            errorMessage = e.message
            duration = measureTimeMillis {
                result = Result.failure(e)
            }
            
            // 记录错误
            detailedLogger.error(
                category = DetailedLogger.LogCategory.PARSER,
                message = "解析AnalysisResult时发生异常",
                data = mapOf(
                    "operationId" to operationId,
                    "modelName" to modelName,
                    "duration" to duration,
                    "error" to (e.message ?: "未知错误")
                ),
                tags = setOf("analysis", "error"),
                throwable = e
            )
            
            // 记录性能指标
            metrics.recordParsingResult(operationType, modelName, false, duration)
            performanceSession.recordCompletion(false, duration)
            
            // 记录诊断信息
            diagnosticCollector.collectParserDiagnostics(
                operationType = operationType,
                requestId = operationId,
                success = false,
                duration = duration,
                errorMessage = errorMessage,
                additionalData = mapOf(
                    "modelName" to modelName,
                    "targetType" to "AnalysisResult",
                    "exceptionType" to e.javaClass.simpleName
                )
            )
            
            return Result.failure(e)
            
        } finally {
            // 结束追踪会话
            parsingTracer.endTraceSession(traceSession.id, success, result?.getOrNull(), errorMessage)
            
            // 结束性能跟踪
            performanceSession.endSession()
            
            // 记录性能日志
            detailedLogger.logPerformanceEvent(
                operation = operationType,
                metrics = mapOf(
                    "duration" to duration,
                    "inputSize" to json.length,
                    "success" to if (success) 1.0 else 0.0
                ),
                thresholds = mapOf(
                    "duration" to 5000.0, // 5秒阈值
                    "inputSize" to 100000.0 // 100KB阈值
                )
            )
        }
    }
    
    override fun parseSafetyCheckResult(json: String, context: ParsingContext): Result<SafetyCheckResult> {
        val operationId = context.operationId
        val modelName = context.modelName
        val operationType = context.operationType
        
        // 开始追踪会话
        val traceSession = parsingTracer.startTraceSession(
            operationType = operationType,
            requestId = operationId,
            input = json,
            metadata = mapOf(
                "modelName" to modelName,
                "inputSize" to json.length,
                "targetType" to "SafetyCheckResult"
            )
        )
        
        // 开始性能跟踪
        val performanceSession = performanceTracker.startTrackingSession(operationId, operationType)
        
        // 记录开始日志
        detailedLogger.info(
            category = DetailedLogger.LogCategory.PARSER,
            message = "开始解析SafetyCheckResult",
            data = mapOf(
                "operationId" to operationId,
                "modelName" to modelName,
                "inputSize" to json.length
            ),
            tags = setOf("safety_check", "start")
        )
        
        var success = false
        var duration = 0L
        var result: Result<SafetyCheckResult>? = null
        var errorMessage: String? = null
        
        try {
            // 测量解析时间
            duration = measureTimeMillis {
                result = parseSafetyCheckResultInternal(json, context, traceSession)
            }
            
            success = result!!.isSuccess
            
            // 记录性能指标
            metrics.recordParsingResult(operationType, modelName, success, duration)
            performanceSession.recordCompletion(success, duration)
            
            // 记录解析器事件
            detailedLogger.logParserEvent(
                operationType = operationType,
                requestId = operationId,
                success = success,
                duration = duration,
                inputSize = json.length,
                errorMessage = errorMessage
            )
            
            // 记录诊断信息
            diagnosticCollector.collectParserDiagnostics(
                operationType = operationType,
                requestId = operationId,
                success = success,
                duration = duration,
                errorMessage = errorMessage,
                additionalData = mapOf(
                    "modelName" to modelName,
                    "targetType" to "SafetyCheckResult"
                )
            )
            
            // 如果成功，收集学习数据
            if (success) {
                collectLearningData(json, result!!.getOrNull(), context)
            }
            
            return result!!
            
        } catch (e: Exception) {
            success = false
            errorMessage = e.message
            duration = measureTimeMillis {
                result = Result.failure(e)
            }
            
            // 记录错误
            detailedLogger.error(
                category = DetailedLogger.LogCategory.PARSER,
                message = "解析SafetyCheckResult时发生异常",
                data = mapOf(
                    "operationId" to operationId,
                    "modelName" to modelName,
                    "duration" to duration,
                    "error" to (e.message ?: "未知错误")
                ),
                tags = setOf("safety_check", "error"),
                throwable = e
            )
            
            // 记录性能指标
            metrics.recordParsingResult(operationType, modelName, false, duration)
            performanceSession.recordCompletion(false, duration)
            
            // 记录诊断信息
            diagnosticCollector.collectParserDiagnostics(
                operationType = operationType,
                requestId = operationId,
                success = false,
                duration = duration,
                errorMessage = errorMessage,
                additionalData = mapOf(
                    "modelName" to modelName,
                    "targetType" to "SafetyCheckResult",
                    "exceptionType" to e.javaClass.simpleName
                )
            )
            
            return Result.failure(e)
            
        } finally {
            // 结束追踪会话
            parsingTracer.endTraceSession(traceSession.id, success, result?.getOrNull(), errorMessage)
            
            // 结束性能跟踪
            performanceSession.endSession()
            
            // 记录性能日志
            detailedLogger.logPerformanceEvent(
                operation = operationType,
                metrics = mapOf(
                    "duration" to duration,
                    "inputSize" to json.length,
                    "success" to if (success) 1.0 else 0.0
                ),
                thresholds = mapOf(
                    "duration" to 3000.0, // 3秒阈值
                    "inputSize" to 50000.0 // 50KB阈值
                )
            )
        }
    }
    
    override fun parseExtractedData(json: String, context: ParsingContext): Result<ExtractedData> {
        val operationId = context.operationId
        val modelName = context.modelName
        val operationType = context.operationType
        
        // 开始追踪会话
        val traceSession = parsingTracer.startTraceSession(
            operationType = operationType,
            requestId = operationId,
            input = json,
            metadata = mapOf(
                "modelName" to modelName,
                "inputSize" to json.length,
                "targetType" to "ExtractedData"
            )
        )
        
        // 开始性能跟踪
        val performanceSession = performanceTracker.startTrackingSession(operationId, operationType)
        
        // 记录开始日志
        detailedLogger.info(
            category = DetailedLogger.LogCategory.PARSER,
            message = "开始解析ExtractedData",
            data = mapOf(
                "operationId" to operationId,
                "modelName" to modelName,
                "inputSize" to json.length
            ),
            tags = setOf("extraction", "start")
        )
        
        var success = false
        var duration = 0L
        var result: Result<ExtractedData>? = null
        var errorMessage: String? = null
        
        try {
            // 测量解析时间
            duration = measureTimeMillis {
                result = parseExtractedDataInternal(json, context, traceSession)
            }
            
            success = result!!.isSuccess
            
            // 记录性能指标
            metrics.recordParsingResult(operationType, modelName, success, duration)
            performanceSession.recordCompletion(success, duration)
            
            // 记录解析器事件
            detailedLogger.logParserEvent(
                operationType = operationType,
                requestId = operationId,
                success = success,
                duration = duration,
                inputSize = json.length,
                errorMessage = errorMessage
            )
            
            // 记录诊断信息
            diagnosticCollector.collectParserDiagnostics(
                operationType = operationType,
                requestId = operationId,
                success = success,
                duration = duration,
                errorMessage = errorMessage,
                additionalData = mapOf(
                    "modelName" to modelName,
                    "targetType" to "ExtractedData"
                )
            )
            
            // 如果成功，收集学习数据
            if (success) {
                collectLearningData(json, result!!.getOrNull(), context)
            }
            
            return result!!
            
        } catch (e: Exception) {
            success = false
            errorMessage = e.message
            duration = measureTimeMillis {
                result = Result.failure(e)
            }
            
            // 记录错误
            detailedLogger.error(
                category = DetailedLogger.LogCategory.PARSER,
                message = "解析ExtractedData时发生异常",
                data = mapOf(
                    "operationId" to operationId,
                    "modelName" to modelName,
                    "duration" to duration,
                    "error" to (e.message ?: "未知错误")
                ),
                tags = setOf("extraction", "error"),
                throwable = e
            )
            
            // 记录性能指标
            metrics.recordParsingResult(operationType, modelName, false, duration)
            performanceSession.recordCompletion(false, duration)
            
            // 记录诊断信息
            diagnosticCollector.collectParserDiagnostics(
                operationType = operationType,
                requestId = operationId,
                success = false,
                duration = duration,
                errorMessage = errorMessage,
                additionalData = mapOf(
                    "modelName" to modelName,
                    "targetType" to "ExtractedData",
                    "exceptionType" to e.javaClass.simpleName
                )
            )
            
            return Result.failure(e)
            
        } finally {
            // 结束追踪会话
            parsingTracer.endTraceSession(traceSession.id, success, result?.getOrNull(), errorMessage)
            
            // 结束性能跟踪
            performanceSession.endSession()
            
            // 记录性能日志
            detailedLogger.logPerformanceEvent(
                operation = operationType,
                metrics = mapOf(
                    "duration" to duration,
                    "inputSize" to json.length,
                    "success" to if (success) 1.0 else 0.0
                ),
                thresholds = mapOf(
                    "duration" to 4000.0, // 4秒阈值
                    "inputSize" to 80000.0 // 80KB阈值
                )
            )
        }
    }
    
    override fun <T> parse(json: String, targetType: Class<T>, context: ParsingContext): Result<T> {
        val operationId = context.operationId
        val modelName = context.modelName
        val operationType = context.operationType
        
        // 开始追踪会话
        val traceSession = parsingTracer.startTraceSession(
            operationType = operationType,
            requestId = operationId,
            input = json,
            metadata = mapOf(
                "modelName" to modelName,
                "inputSize" to json.length,
                "targetType" to targetType.simpleName
            )
        )
        
        // 开始性能跟踪
        val performanceSession = performanceTracker.startTrackingSession(operationId, operationType)
        
        // 记录开始日志
        detailedLogger.info(
            category = DetailedLogger.LogCategory.PARSER,
            message = "开始通用解析",
            data = mapOf(
                "operationId" to operationId,
                "modelName" to modelName,
                "targetType" to targetType.simpleName,
                "inputSize" to json.length
            ),
            tags = setOf("generic", "start")
        )
        
        var success = false
        var duration = 0L
        var result: Result<T>? = null
        var errorMessage: String? = null
        
        try {
            // 测量解析时间
            duration = measureTimeMillis {
                result = parseInternal(json, targetType, context, traceSession)
            }
            
            success = result!!.isSuccess
            
            // 记录性能指标
            metrics.recordParsingResult(operationType, modelName, success, duration)
            performanceSession.recordCompletion(success, duration)
            
            // 记录解析器事件
            detailedLogger.logParserEvent(
                operationType = operationType,
                requestId = operationId,
                success = success,
                duration = duration,
                inputSize = json.length,
                errorMessage = errorMessage
            )
            
            // 记录诊断信息
            diagnosticCollector.collectParserDiagnostics(
                operationType = operationType,
                requestId = operationId,
                success = success,
                duration = duration,
                errorMessage = errorMessage,
                additionalData = mapOf(
                    "modelName" to modelName,
                    "targetType" to targetType.simpleName
                )
            )
            
            // 如果成功，收集学习数据
            if (success) {
                collectLearningData(json, result!!.getOrNull(), context)
            }
            
            return result!!
            
        } catch (e: Exception) {
            success = false
            errorMessage = e.message
            duration = measureTimeMillis {
                result = Result.failure(e)
            }
            
            // 记录错误
            detailedLogger.error(
                category = DetailedLogger.LogCategory.PARSER,
                message = "通用解析时发生异常",
                data = mapOf(
                    "operationId" to operationId,
                    "modelName" to modelName,
                    "targetType" to targetType.simpleName,
                    "duration" to duration,
                    "error" to (e.message ?: "未知错误")
                ),
                tags = setOf("generic", "error"),
                throwable = e
            )
            
            // 记录性能指标
            metrics.recordParsingResult(operationType, modelName, false, duration)
            performanceSession.recordCompletion(false, duration)
            
            // 记录诊断信息
            diagnosticCollector.collectParserDiagnostics(
                operationType = operationType,
                requestId = operationId,
                success = false,
                duration = duration,
                errorMessage = errorMessage,
                additionalData = mapOf(
                    "modelName" to modelName,
                    "targetType" to targetType.simpleName,
                    "exceptionType" to e.javaClass.simpleName
                )
            )
            
            return Result.failure(e)
            
        } finally {
            // 结束追踪会话
            parsingTracer.endTraceSession(traceSession.id, success, result?.getOrNull(), errorMessage)
            
            // 结束性能跟踪
            performanceSession.endSession()
            
            // 记录性能日志
            detailedLogger.logPerformanceEvent(
                operation = operationType,
                metrics = mapOf(
                    "duration" to duration,
                    "inputSize" to json.length,
                    "success" to if (success) 1.0 else 0.0
                ),
                thresholds = mapOf(
                    "duration" to 5000.0, // 5秒阈值
                    "inputSize" to 100000.0 // 100KB阈值
                )
            )
        }
    }
    
    /**
     * 内部解析AnalysisResult方法
     */
    private fun parseAnalysisResultInternal(
        json: String,
        context: ParsingContext,
        traceSession: ParsingTracer.TraceSession
    ): Result<AnalysisResult> {
        val operationId = context.operationId
        
        // 开始预处理跨度
        val preprocessingSpan = parsingTracer.startSpan(
            traceSession.id,
            "JSON清洗",
            ParsingTracer.SpanType.PREPROCESSING
        )
        
        try {
            // 清洗JSON
            val cleanedJson = jsonCleaner.clean(json, context.toCleaningContext())
            
            // 结束预处理跨度
            parsingTracer.endSpan(traceSession.id, preprocessingSpan!!, true, cleanedJson)
            
            // 开始解析跨度
            val parsingSpan = parsingTracer.startSpan(
                traceSession.id,
                "标准解析策略",
                ParsingTracer.SpanType.PARSING,
                preprocessingSpan
            )
            
            // 尝试标准解析策略
            val standardResult = StandardParsingStrategy.parseAnalysisResult(cleanedJson, moshi)
            if (standardResult.isSuccess) {
                parsingTracer.endSpan(traceSession.id, parsingSpan!!, true, standardResult.getOrNull())
                detailedLogger.debug(
                    category = DetailedLogger.LogCategory.PARSER,
                    message = "标准解析策略成功",
                    data = mapOf("operationId" to operationId),
                    tags = setOf("analysis", "standard_success")
                )
                return standardResult
            }
            
            // 结束标准解析跨度
            parsingTracer.endSpan(traceSession.id, parsingSpan!!, false, null, "标准解析失败")
            
            // 开始容错解析跨度
            val fallbackSpan = parsingTracer.startSpan(
                traceSession.id,
                "容错解析策略",
                ParsingTracer.SpanType.PARSING
            )
            
            // 尝试容错解析策略
            val fallbackResult = FallbackParsingStrategy.parseAnalysisResult(cleanedJson, moshi)
            if (fallbackResult.isSuccess) {
                parsingTracer.endSpan(traceSession.id, fallbackSpan!!, true, fallbackResult.getOrNull())
                detailedLogger.debug(
                    category = DetailedLogger.LogCategory.PARSER,
                    message = "容错解析策略成功",
                    data = mapOf("operationId" to operationId),
                    tags = setOf("analysis", "fallback_success")
                )
                return fallbackResult
            }
            
            // 结束容错解析跨度
            parsingTracer.endSpan(traceSession.id, fallbackSpan!!, false, null, "容错解析失败")
            
            // 开始智能解析跨度
            val intelligentSpan = parsingTracer.startSpan(
                traceSession.id,
                "智能解析策略",
                ParsingTracer.SpanType.PARSING
            )
            
            // 尝试智能解析策略
            val intelligentResult = IntelligentParsingStrategy.parseAnalysisResult(cleanedJson, moshi)
            if (intelligentResult.isSuccess) {
                parsingTracer.endSpan(traceSession.id, intelligentSpan!!, true, intelligentResult.getOrNull())
                detailedLogger.debug(
                    category = DetailedLogger.LogCategory.PARSER,
                    message = "智能解析策略成功",
                    data = mapOf("operationId" to operationId),
                    tags = setOf("analysis", "intelligent_success")
                )
                return intelligentResult
            }
            
            // 结束智能解析跨度
            parsingTracer.endSpan(traceSession.id, intelligentSpan!!, false, null, "智能解析失败")
            
            // 开始降级处理跨度
            val degradationSpan = parsingTracer.startSpan(
                traceSession.id,
                "降级处理",
                ParsingTracer.SpanType.CUSTOM
            )
            
            // 所有策略都失败，使用降级处理器
            detailedLogger.warning(
                category = DetailedLogger.LogCategory.PARSER,
                message = "所有解析策略失败，使用降级处理器",
                data = mapOf("operationId" to operationId),
                tags = setOf("analysis", "degradation")
            )
            
            val fallbackContext = context.toFallbackContext(json)
            val fallbackResult = fallbackHandler.handleParsingFailure(
                Exception("所有解析策略失败"),
                AnalysisResult::class.java,
                fallbackContext
            )
            
            when (fallbackResult) {
                is FallbackResult.Success -> {
                    parsingTracer.endSpan(traceSession.id, degradationSpan!!, true, fallbackResult.data)
                    detailedLogger.info(
                        category = DetailedLogger.LogCategory.PARSER,
                        message = "降级处理成功",
                        data = mapOf(
                            "operationId" to operationId,
                            "strategy" to fallbackResult.strategy
                        ),
                        tags = setOf("analysis", "degradation_success")
                    )
                    return Result.success(fallbackResult.data)
                }
                is FallbackResult.Failure -> {
                    parsingTracer.endSpan(traceSession.id, degradationSpan!!, false, null, fallbackResult.error.message)
                    detailedLogger.error(
                        category = DetailedLogger.LogCategory.PARSER,
                        message = "降级处理失败",
                        data = mapOf(
                            "operationId" to operationId,
                            "error" to (fallbackResult.error.message ?: "未知错误")
                        ),
                        tags = setOf("analysis", "degradation_failure"),
                        throwable = fallbackResult.error
                    )
                    return Result.failure(fallbackResult.error)
                }
            }
            
        } catch (e: Exception) {
            detailedLogger.error(
                category = DetailedLogger.LogCategory.PARSER,
                message = "解析AnalysisResult时发生异常",
                data = mapOf(
                    "operationId" to operationId,
                    "error" to (e.message ?: "未知错误")
                ),
                tags = setOf("analysis", "exception"),
                throwable = e
            )
            return Result.failure(e)
        }
    }
    
    /**
     * 内部解析SafetyCheckResult方法
     */
    private fun parseSafetyCheckResultInternal(
        json: String,
        context: ParsingContext,
        traceSession: ParsingTracer.TraceSession
    ): Result<SafetyCheckResult> {
        val operationId = context.operationId
        
        // 开始预处理跨度
        val preprocessingSpan = parsingTracer.startSpan(
            traceSession.id,
            "JSON清洗",
            ParsingTracer.SpanType.PREPROCESSING
        )
        
        try {
            // 清洗JSON
            val cleanedJson = jsonCleaner.clean(json, context.toCleaningContext())
            
            // 结束预处理跨度
            parsingTracer.endSpan(traceSession.id, preprocessingSpan!!, true, cleanedJson)
            
            // 开始解析跨度
            val parsingSpan = parsingTracer.startSpan(
                traceSession.id,
                "标准解析策略",
                ParsingTracer.SpanType.PARSING,
                preprocessingSpan
            )
            
            // 尝试标准解析策略
            val standardResult = StandardParsingStrategy.parseSafetyCheckResult(cleanedJson, moshi)
            if (standardResult.isSuccess) {
                parsingTracer.endSpan(traceSession.id, parsingSpan!!, true, standardResult.getOrNull())
                detailedLogger.debug(
                    category = DetailedLogger.LogCategory.PARSER,
                    message = "标准解析策略成功",
                    data = mapOf("operationId" to operationId),
                    tags = setOf("safety_check", "standard_success")
                )
                return standardResult
            }
            
            // 结束标准解析跨度
            parsingTracer.endSpan(traceSession.id, parsingSpan!!, false, null, "标准解析失败")
            
            // 开始容错解析跨度
            val fallbackSpan = parsingTracer.startSpan(
                traceSession.id,
                "容错解析策略",
                ParsingTracer.SpanType.PARSING
            )
            
            // 尝试容错解析策略
            val fallbackResult = FallbackParsingStrategy.parseSafetyCheckResult(cleanedJson, moshi)
            if (fallbackResult.isSuccess) {
                parsingTracer.endSpan(traceSession.id, fallbackSpan!!, true, fallbackResult.getOrNull())
                detailedLogger.debug(
                    category = DetailedLogger.LogCategory.PARSER,
                    message = "容错解析策略成功",
                    data = mapOf("operationId" to operationId),
                    tags = setOf("safety_check", "fallback_success")
                )
                return fallbackResult
            }
            
            // 结束容错解析跨度
            parsingTracer.endSpan(traceSession.id, fallbackSpan!!, false, null, "容错解析失败")
            
            // 开始智能解析跨度
            val intelligentSpan = parsingTracer.startSpan(
                traceSession.id,
                "智能解析策略",
                ParsingTracer.SpanType.PARSING
            )
            
            // 尝试智能解析策略
            val intelligentResult = IntelligentParsingStrategy.parseSafetyCheckResult(cleanedJson, moshi)
            if (intelligentResult.isSuccess) {
                parsingTracer.endSpan(traceSession.id, intelligentSpan!!, true, intelligentResult.getOrNull())
                detailedLogger.debug(
                    category = DetailedLogger.LogCategory.PARSER,
                    message = "智能解析策略成功",
                    data = mapOf("operationId" to operationId),
                    tags = setOf("safety_check", "intelligent_success")
                )
                return intelligentResult
            }
            
            // 结束智能解析跨度
            parsingTracer.endSpan(traceSession.id, intelligentSpan!!, false, null, "智能解析失败")
            
            // 开始降级处理跨度
            val degradationSpan = parsingTracer.startSpan(
                traceSession.id,
                "降级处理",
                ParsingTracer.SpanType.CUSTOM
            )
            
            // 所有策略都失败，使用降级处理器
            detailedLogger.warning(
                category = DetailedLogger.LogCategory.PARSER,
                message = "所有解析策略失败，使用降级处理器",
                data = mapOf("operationId" to operationId),
                tags = setOf("safety_check", "degradation")
            )
            
            val fallbackContext = context.toFallbackContext(json)
            val fallbackResult = fallbackHandler.handleParsingFailure(
                Exception("所有解析策略失败"),
                SafetyCheckResult::class.java,
                fallbackContext
            )
            
            when (fallbackResult) {
                is FallbackResult.Success -> {
                    parsingTracer.endSpan(traceSession.id, degradationSpan!!, true, fallbackResult.data)
                    detailedLogger.info(
                        category = DetailedLogger.LogCategory.PARSER,
                        message = "降级处理成功",
                        data = mapOf(
                            "operationId" to operationId,
                            "strategy" to fallbackResult.strategy
                        ),
                        tags = setOf("safety_check", "degradation_success")
                    )
                    return Result.success(fallbackResult.data)
                }
                is FallbackResult.Failure -> {
                    parsingTracer.endSpan(traceSession.id, degradationSpan!!, false, null, fallbackResult.error.message)
                    detailedLogger.error(
                        category = DetailedLogger.LogCategory.PARSER,
                        message = "降级处理失败",
                        data = mapOf(
                            "operationId" to operationId,
                            "error" to (fallbackResult.error.message ?: "未知错误")
                        ),
                        tags = setOf("safety_check", "degradation_failure"),
                        throwable = fallbackResult.error
                    )
                    return Result.failure(fallbackResult.error)
                }
            }
            
        } catch (e: Exception) {
            detailedLogger.error(
                category = DetailedLogger.LogCategory.PARSER,
                message = "解析SafetyCheckResult时发生异常",
                data = mapOf(
                    "operationId" to operationId,
                    "error" to (e.message ?: "未知错误")
                ),
                tags = setOf("safety_check", "exception"),
                throwable = e
            )
            return Result.failure(e)
        }
    }
    
    /**
     * 内部解析ExtractedData方法
     */
    private fun parseExtractedDataInternal(
        json: String,
        context: ParsingContext,
        traceSession: ParsingTracer.TraceSession
    ): Result<ExtractedData> {
        val operationId = context.operationId
        
        // 开始预处理跨度
        val preprocessingSpan = parsingTracer.startSpan(
            traceSession.id,
            "JSON清洗",
            ParsingTracer.SpanType.PREPROCESSING
        )
        
        try {
            // 清洗JSON
            val cleanedJson = jsonCleaner.clean(json, context.toCleaningContext())
            
            // 结束预处理跨度
            parsingTracer.endSpan(traceSession.id, preprocessingSpan!!, true, cleanedJson)
            
            // 开始解析跨度
            val parsingSpan = parsingTracer.startSpan(
                traceSession.id,
                "标准解析策略",
                ParsingTracer.SpanType.PARSING,
                preprocessingSpan
            )
            
            // 尝试标准解析策略
            val standardResult = StandardParsingStrategy.parseExtractedData(cleanedJson, moshi)
            if (standardResult.isSuccess) {
                parsingTracer.endSpan(traceSession.id, parsingSpan!!, true, standardResult.getOrNull())
                detailedLogger.debug(
                    category = DetailedLogger.LogCategory.PARSER,
                    message = "标准解析策略成功",
                    data = mapOf("operationId" to operationId),
                    tags = setOf("extraction", "standard_success")
                )
                return standardResult
            }
            
            // 结束标准解析跨度
            parsingTracer.endSpan(traceSession.id, parsingSpan!!, false, null, "标准解析失败")
            
            // 开始容错解析跨度
            val fallbackSpan = parsingTracer.startSpan(
                traceSession.id,
                "容错解析策略",
                ParsingTracer.SpanType.PARSING
            )
            
            // 尝试容错解析策略
            val fallbackResult = FallbackParsingStrategy.parseExtractedData(cleanedJson, moshi)
            if (fallbackResult.isSuccess) {
                parsingTracer.endSpan(traceSession.id, fallbackSpan!!, true, fallbackResult.getOrNull())
                detailedLogger.debug(
                    category = DetailedLogger.LogCategory.PARSER,
                    message = "容错解析策略成功",
                    data = mapOf("operationId" to operationId),
                    tags = setOf("extraction", "fallback_success")
                )
                return fallbackResult
            }
            
            // 结束容错解析跨度
            parsingTracer.endSpan(traceSession.id, fallbackSpan!!, false, null, "容错解析失败")
            
            // 开始智能解析跨度
            val intelligentSpan = parsingTracer.startSpan(
                traceSession.id,
                "智能解析策略",
                ParsingTracer.SpanType.PARSING
            )
            
            // 尝试智能解析策略
            val intelligentResult = IntelligentParsingStrategy.parseExtractedData(cleanedJson, moshi)
            if (intelligentResult.isSuccess) {
                parsingTracer.endSpan(traceSession.id, intelligentSpan!!, true, intelligentResult.getOrNull())
                detailedLogger.debug(
                    category = DetailedLogger.LogCategory.PARSER,
                    message = "智能解析策略成功",
                    data = mapOf("operationId" to operationId),
                    tags = setOf("extraction", "intelligent_success")
                )
                return intelligentResult
            }
            
            // 结束智能解析跨度
            parsingTracer.endSpan(traceSession.id, intelligentSpan!!, false, null, "智能解析失败")
            
            // 开始降级处理跨度
            val degradationSpan = parsingTracer.startSpan(
                traceSession.id,
                "降级处理",
                ParsingTracer.SpanType.CUSTOM
            )
            
            // 所有策略都失败，使用降级处理器
            detailedLogger.warning(
                category = DetailedLogger.LogCategory.PARSER,
                message = "所有解析策略失败，使用降级处理器",
                data = mapOf("operationId" to operationId),
                tags = setOf("extraction", "degradation")
            )
            
            val fallbackContext = context.toFallbackContext(json)
            val fallbackResult = fallbackHandler.handleParsingFailure(
                Exception("所有解析策略失败"),
                ExtractedData::class.java,
                fallbackContext
            )
            
            when (fallbackResult) {
                is FallbackResult.Success -> {
                    parsingTracer.endSpan(traceSession.id, degradationSpan!!, true, fallbackResult.data)
                    detailedLogger.info(
                        category = DetailedLogger.LogCategory.PARSER,
                        message = "降级处理成功",
                        data = mapOf(
                            "operationId" to operationId,
                            "strategy" to fallbackResult.strategy
                        ),
                        tags = setOf("extraction", "degradation_success")
                    )
                    return Result.success(fallbackResult.data)
                }
                is FallbackResult.Failure -> {
                    parsingTracer.endSpan(traceSession.id, degradationSpan!!, false, null, fallbackResult.error.message)
                    detailedLogger.error(
                        category = DetailedLogger.LogCategory.PARSER,
                        message = "降级处理失败",
                        data = mapOf(
                            "operationId" to operationId,
                            "error" to (fallbackResult.error.message ?: "未知错误")
                        ),
                        tags = setOf("extraction", "degradation_failure"),
                        throwable = fallbackResult.error
                    )
                    return Result.failure(fallbackResult.error)
                }
            }
            
        } catch (e: Exception) {
            detailedLogger.error(
                category = DetailedLogger.LogCategory.PARSER,
                message = "解析ExtractedData时发生异常",
                data = mapOf(
                    "operationId" to operationId,
                    "error" to (e.message ?: "未知错误")
                ),
                tags = setOf("extraction", "exception"),
                throwable = e
            )
            return Result.failure(e)
        }
    }
    
    /**
     * 内部通用解析方法
     */
    private fun <T> parseInternal(
        json: String,
        targetType: Class<T>,
        context: ParsingContext,
        traceSession: ParsingTracer.TraceSession
    ): Result<T> {
        val operationId = context.operationId
        
        try {
            // 根据目标类型选择适当的解析策略
            when (targetType) {
                AnalysisResult::class.java -> {
                    val result = parseAnalysisResultInternal(json, context, traceSession)
                    @Suppress("UNCHECKED_CAST")
                    result as Result<T>
                }
                
                SafetyCheckResult::class.java -> {
                    val result = parseSafetyCheckResultInternal(json, context, traceSession)
                    @Suppress("UNCHECKED_CAST")
                    result as Result<T>
                }
                
                ExtractedData::class.java -> {
                    val result = parseExtractedDataInternal(json, context, traceSession)
                    @Suppress("UNCHECKED_CAST")
                    result as Result<T>
                }
                
                else -> {
                    detailedLogger.warning(
                        category = DetailedLogger.LogCategory.PARSER,
                        message = "不支持的类型",
                        data = mapOf(
                            "operationId" to operationId,
                            "targetType" to targetType.simpleName
                        ),
                        tags = setOf("generic", "unsupported_type")
                    )
                    Result.failure(IllegalArgumentException("不支持的类型: ${targetType.simpleName}"))
                }
            }
            
        } catch (e: Exception) {
            detailedLogger.error(
                category = DetailedLogger.LogCategory.PARSER,
                message = "通用解析时发生异常",
                data = mapOf(
                    "operationId" to operationId,
                    "targetType" to targetType.simpleName,
                    "error" to (e.message ?: "未知错误")
                ),
                tags = setOf("generic", "exception"),
                throwable = e
            )
            Result.failure(e)
        }
    }
    
    /**
     * 收集学习数据
     */
    private fun collectLearningData(json: String, result: Any?, context: ParsingContext) {
        try {
            // 收集字段映射学习数据
            fieldMappingLearningEngine.collectMappingData(json, result, context)
            
            // 收集解析模式分析数据
            parsingPatternAnalyzer.collectPatternData(json, result, context)
            
            detailedLogger.debug(
                category = DetailedLogger.LogCategory.LEARNING,
                message = "学习数据收集完成",
                data = mapOf(
                    "operationId" to context.operationId,
                    "operationType" to context.operationType
                ),
                tags = setOf("learning", "data_collection")
            )
            
        } catch (e: Exception) {
            detailedLogger.warning(
                category = DetailedLogger.LogCategory.LEARNING,
                message = "学习数据收集失败",
                data = mapOf(
                    "operationId" to context.operationId,
                    "error" to (e.message ?: "未知错误")
                ),
                tags = setOf("learning", "collection_error"),
                throwable = e
            )
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