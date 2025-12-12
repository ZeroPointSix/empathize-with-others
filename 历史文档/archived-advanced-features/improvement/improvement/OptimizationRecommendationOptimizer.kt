package com.empathy.ai.data.improvement

import android.util.Log
import com.empathy.ai.data.monitoring.AiResponseParserMetrics
import com.empathy.ai.data.optimization.AdaptivePerformanceOptimizer
import com.empathy.ai.data.optimization.PerformanceTuner
import com.empathy.ai.data.optimization.ResourceMonitor
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

/**
 * 优化建议优化器
 * 
 * 负责分析系统状态和性能数据，生成最优的优化建议
 * 通过机器学习和历史数据分析，提供精准的优化方案
 * 
 * 功能：
 * 1. 智能优化建议生成
 * 2. 优化效果预测
 * 3. 优化方案排序和筛选
 * 4. 优化历史分析
 */
class OptimizationRecommendationOptimizer private constructor(
    private val metrics: AiResponseParserMetrics,
    private val performanceOptimizer: AdaptivePerformanceOptimizer,
    private val performanceTuner: PerformanceTuner,
    private val resourceMonitor: ResourceMonitor
) {
    
    companion object {
        private const val TAG = "OptimizationRecommendationOptimizer"
        
        // 优化配置
        private const val RECOMMENDATION_INTERVAL_MS = 60_000L // 1分钟
        private const val HISTORY_SIZE = 1000
        private const val MIN_SAMPLES_FOR_RECOMMENDATION = 30
        
        // 优化阈值
        private const val MIN_IMPROVEMENT_THRESHOLD = 0.02 // 2%最小改进阈值
        private const val HIGH_IMPACT_THRESHOLD = 0.1 // 10%高影响阈值
        
        @Volatile
        private var INSTANCE: OptimizationRecommendationOptimizer? = null
        
        fun getInstance(
            metrics: AiResponseParserMetrics,
            performanceOptimizer: AdaptivePerformanceOptimizer,
            performanceTuner: PerformanceTuner,
            resourceMonitor: ResourceMonitor
        ): OptimizationRecommendationOptimizer {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: OptimizationRecommendationOptimizer(
                    metrics, performanceOptimizer, performanceTuner, resourceMonitor
                ).also { INSTANCE = it }
            }
        }
    }
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val optimizerConfig = AtomicReference(OptimizerConfig())
    private val recommendationHistory = ConcurrentHashMap<String, RecommendationHistory>()
    private val currentRecommendations = AtomicReference<List<OptimizationRecommendation>>(emptyList())
    private val recommendationStrategies = ConcurrentHashMap<String, RecommendationStrategy>()
    private val effectivenessAnalyzer = RecommendationEffectivenessAnalyzer()
    
    private var isOptimizerEnabled = false
    private var optimizationJob: Job? = null
    private val recommendationCounter = AtomicLong(0)
    
    init {
        initializeRecommendationStrategies()
        Log.i(TAG, "优化建议优化器初始化完成")
    }
    
    /**
     * 启用优化建议
     */
    fun enableOptimization(config: OptimizerConfig = OptimizerConfig()) {
        if (isOptimizerEnabled) {
            Log.w(TAG, "优化建议已启用")
            return
        }
        
        optimizerConfig.set(config)
        isOptimizerEnabled = true
        
        optimizationJob = scope.launch {
            while (isActive) {
                try {
                    performOptimizationCycle()
                    delay(RECOMMENDATION_INTERVAL_MS)
                } catch (e: Exception) {
                    Log.e(TAG, "优化周期执行失败", e)
                }
            }
        }
        
        Log.i(TAG, "优化建议已启用")
    }
    
    /**
     * 禁用优化建议
     */
    fun disableOptimization() {
        if (!isOptimizerEnabled) {
            Log.w(TAG, "优化建议未启用")
            return
        }
        
        isOptimizerEnabled = false
        optimizationJob?.cancel()
        optimizationJob = null
        
        Log.i(TAG, "优化建议已禁用")
    }
    
    /**
     * 手动触发优化
     */
    suspend fun triggerOptimization(): OptimizationResult {
        return performOptimizationCycle()
    }
    
    /**
     * 获取当前建议
     */
    fun getCurrentRecommendations(): List<OptimizationRecommendation> {
        return currentRecommendations.get()
    }
    
    /**
     * 获取优化状态
     */
    fun getOptimizationStatus(): OptimizationStatus {
        val currentRecommendations = currentRecommendations.get()
        val recommendationHistories = recommendationHistory.values.map { it.getRecentRecommendations(10) }.flatten()
        val effectivenessAnalysis = effectivenessAnalyzer.getEffectivenessAnalysis()
        
        return OptimizationStatus(
            isEnabled = isOptimizerEnabled,
            currentRecommendations = currentRecommendations,
            recommendationHistory = recommendationHistories,
            effectivenessAnalysis = effectivenessAnalysis,
            config = optimizerConfig.get()
        )
    }
    
    /**
     * 更新优化配置
     */
    fun updateConfig(config: OptimizerConfig) {
        optimizerConfig.set(config)
        Log.i(TAG, "优化配置已更新: $config")
    }
    
    /**
     * 记录建议反馈
     */
    fun recordRecommendationFeedback(
        recommendationId: String,
        effectiveness: Double,
        applied: Boolean,
        comment: String = ""
    ) {
        effectivenessAnalyzer.recordFeedback(recommendationId, effectiveness, applied, comment)
        
        Log.i(TAG, "建议反馈已记录: $recommendationId, 效果=$effectiveness, 应用=$applied")
    }
    
    /**
     * 生成优化报告
     */
    fun getOptimizationReport(): OptimizationReport {
        val overallMetrics = metrics.getOverallMetrics()
        val performanceSummary = metrics.getPerformanceSummary()
        val resourceReport = resourceMonitor.getResourceReport()
        val optimizationStatus = performanceOptimizer.getOptimizationStatus()
        val tuningStatus = performanceTuner.getTuningStatus()
        
        val currentRecommendations = currentRecommendations.get()
        val recommendationHistory = recommendationHistory.values.map { it.getRecentRecommendations(50) }.flatten()
        val effectivenessAnalysis = effectivenessAnalyzer.getEffectivenessAnalysis()
        
        return OptimizationReport(
            timestamp = System.currentTimeMillis(),
            overallMetrics = overallMetrics,
            performanceSummary = performanceSummary,
            resourceReport = resourceReport,
            optimizationStatus = optimizationStatus,
            tuningStatus = tuningStatus,
            currentRecommendations = currentRecommendations,
            recommendationHistory = recommendationHistory,
            effectivenessAnalysis = effectivenessAnalysis,
            recommendations = generateHighLevelRecommendations(
                overallMetrics, performanceSummary, resourceReport
            )
        )
    }
    
    /**
     * 执行优化周期
     */
    private suspend fun performOptimizationCycle(): OptimizationResult {
        val startTime = System.currentTimeMillis()
        val config = optimizerConfig.get()
        
        if (!config.enableAutomaticOptimization) {
            return OptimizationResult(
                success = false,
                reason = "自动优化已禁用",
                generatedRecommendations = emptyList(),
                durationMs = System.currentTimeMillis() - startTime
            )
        }
        
        val generatedRecommendations = mutableListOf<OptimizationRecommendation>()
        var optimizationSuccess = true
        var failureReason: String? = null
        
        try {
            // 1. 收集系统状态数据
            val systemState = collectSystemState()
            
            // 2. 分析优化机会
            val optimizationOpportunities = analyzeOptimizationOpportunities(systemState)
            
            // 3. 生成优化建议
            val recommendations = generateRecommendations(optimizationOpportunities, config)
            
            // 4. 筛选和排序建议
            val filteredRecommendations = filterAndSortRecommendations(recommendations, config)
            
            // 5. 预测优化效果
            val recommendationsWithPrediction = predictOptimizationEffectiveness(filteredRecommendations)
            
            // 6. 更新当前建议
            currentRecommendations.set(recommendationsWithPrediction)
            generatedRecommendations.addAll(recommendationsWithPrediction)
            
            // 7. 更新建议历史
            updateRecommendationHistory(recommendationsWithPrediction)
            
        } catch (e: Exception) {
            optimizationSuccess = false
            failureReason = e.message
            Log.e(TAG, "优化周期执行异常", e)
        }
        
        val duration = System.currentTimeMillis() - startTime
        
        return OptimizationResult(
            success = optimizationSuccess,
            reason = failureReason,
            generatedRecommendations = generatedRecommendations,
            durationMs = duration
        )
    }
    
    /**
     * 收集系统状态
     */
    private fun collectSystemState(): SystemState {
        val overallMetrics = metrics.getOverallMetrics()
        val performanceSummary = metrics.getPerformanceSummary()
        val resourceMetrics = resourceMonitor.getCurrentMetrics()
        val optimizationStatus = performanceOptimizer.getOptimizationStatus()
        val tuningStatus = performanceTuner.getTuningStatus()
        
        return SystemState(
            timestamp = System.currentTimeMillis(),
            overallMetrics = overallMetrics,
            performanceSummary = performanceSummary,
            resourceMetrics = resourceMetrics,
            optimizationStatus = optimizationStatus,
            tuningStatus = tuningStatus
        )
    }
    
    /**
     * 分析优化机会
     */
    private fun analyzeOptimizationOpportunities(systemState: SystemState): List<OptimizationOpportunity> {
        val opportunities = mutableListOf<OptimizationOpportunity>()
        
        // 基于性能指标分析机会
        val overallMetrics = systemState.overallMetrics
        
        if (overallMetrics.successRate < 0.95) {
            opportunities.add(OptimizationOpportunity(
                type = OptimizationOpportunityType.SUCCESS_RATE_IMPROVEMENT,
                priority = OpportunityPriority.HIGH,
                currentValue = overallMetrics.successRate,
                targetValue = 0.95,
                potentialImprovement = 0.95 - overallMetrics.successRate,
                description = "成功率低于95%，当前${String.format("%.2f%%", overallMetrics.successRate * 100)}",
                systemState = systemState
            ))
        }
        
        if (overallMetrics.averageParseTimeMs > 500) {
            opportunities.add(OptimizationOpportunity(
                type = OptimizationOpportunityType.PERFORMANCE_OPTIMIZATION,
                priority = OpportunityPriority.MEDIUM,
                currentValue = overallMetrics.averageParseTimeMs.toDouble(),
                targetValue = 500.0,
                potentialImprovement = (overallMetrics.averageParseTimeMs - 500) / overallMetrics.averageParseTimeMs,
                description = "平均响应时间过长，当前${overallMetrics.averageParseTimeMs}ms",
                systemState = systemState
            ))
        }
        
        // 基于资源使用分析机会
        val resourceMetrics = systemState.resourceMetrics
        
        if (resourceMetrics.memoryUsagePercent > 0.8) {
            opportunities.add(OptimizationOpportunity(
                type = OptimizationOpportunityType.MEMORY_OPTIMIZATION,
                priority = OpportunityPriority.MEDIUM,
                currentValue = resourceMetrics.memoryUsagePercent,
                targetValue = 0.7,
                potentialImprovement = 0.1,
                description = "内存使用率过高，当前${String.format("%.1f%%", resourceMetrics.memoryUsagePercent * 100)}",
                systemState = systemState
            ))
        }
        
        if (resourceMetrics.cpuUsagePercent > 0.8) {
            opportunities.add(OptimizationOpportunity(
                type = OptimizationOpportunityType.CPU_OPTIMIZATION,
                priority = OpportunityPriority.MEDIUM,
                currentValue = resourceMetrics.cpuUsagePercent,
                targetValue = 0.7,
                potentialImprovement = 0.1,
                description = "CPU使用率过高，当前${String.format("%.1f%%", resourceMetrics.cpuUsagePercent * 100)}",
                systemState = systemState
            ))
        }
        
        // 基于历史数据分析机会
        val historicalOpportunities = analyzeHistoricalOpportunities(systemState)
        opportunities.addAll(historicalOpportunities)
        
        return opportunities.sortedByDescending { it.priority.value }
    }
    
    /**
     * 分析历史优化机会
     */
    private fun analyzeHistoricalOpportunities(systemState: SystemState): List<OptimizationOpportunity> {
        val opportunities = mutableListOf<OptimizationOpportunity>()
        
        // 分析历史建议的效果
        val effectivenessAnalysis = effectivenessAnalyzer.getEffectivenessAnalysis()
        
        // 基于历史效果生成新机会
        effectivenessAnalysis.effectivenessByType.forEach { (type, effectiveness) ->
            if (effectiveness.averageEffectiveness < 0.05) {
                // 某种类型的建议效果不佳，可能需要改进策略
                opportunities.add(OptimizationOpportunity(
                    type = OptimizationOpportunityType.STRATEGY_IMPROVEMENT,
                    priority = OpportunityPriority.LOW,
                    currentValue = effectiveness.averageEffectiveness,
                    targetValue = 0.1,
                    potentialImprovement = 0.05,
                    description = "${type}类型建议效果不佳，需要改进策略",
                    systemState = systemState
                ))
            }
        }
        
        return opportunities
    }
    
    /**
     * 生成优化建议
     */
    private fun generateRecommendations(
        opportunities: List<OptimizationOpportunity>,
        config: OptimizerConfig
    ): List<OptimizationRecommendation> {
        val recommendations = mutableListOf<OptimizationRecommendation>()
        
        opportunities.forEach { opportunity ->
            val strategy = recommendationStrategies[opportunity.type.name]
            if (strategy != null && strategy.shouldRecommend(opportunity, config)) {
                val recommendation = strategy.generateRecommendation(opportunity, config)
                if (recommendation != null) {
                    recommendations.add(recommendation)
                }
            }
        }
        
        return recommendations
    }
    
    /**
     * 筛选和排序建议
     */
    private fun filterAndSortRecommendations(
        recommendations: List<OptimizationRecommendation>,
        config: OptimizerConfig
    ): List<OptimizationRecommendation> {
        return recommendations
            .filter { it.expectedImprovement >= config.minImprovementThreshold }
            .filter { it.riskLevel != RiskLevel.CRITICAL || config.enableHighRiskRecommendations }
            .sortedByDescending { it.priority.value }
            .take(config.maxRecommendationsPerCycle)
    }
    
    /**
     * 预测优化效果
     */
    private fun predictOptimizationEffectiveness(
        recommendations: List<OptimizationRecommendation>
    ): List<OptimizationRecommendation> {
        return recommendations.map { recommendation ->
            val predictedEffectiveness = predictEffectiveness(recommendation)
            recommendation.copy(predictedEffectiveness = predictedEffectiveness)
        }
    }
    
    /**
     * 预测单个建议的效果
     */
    private fun predictEffectiveness(recommendation: OptimizationRecommendation): Double {
        // 基于历史数据预测效果
        val historicalEffectiveness = effectivenessAnalyzer.getAverageEffectiveness(recommendation.type)
        
        // 基于建议特征预测效果
        val featureBasedEffectiveness = calculateFeatureBasedEffectiveness(recommendation)
        
        // 综合预测
        return (historicalEffectiveness * 0.6 + featureBasedEffectiveness * 0.4).coerceIn(0.0, 1.0)
    }
    
    /**
     * 基于特征计算效果
     */
    private fun calculateFeatureBasedEffectiveness(recommendation: OptimizationRecommendation): Double {
        var effectiveness = 0.0
        
        // 基于优先级
        effectiveness += when (recommendation.priority) {
            RecommendationPriority.CRITICAL -> 0.4
            RecommendationPriority.HIGH -> 0.3
            RecommendationPriority.MEDIUM -> 0.2
            RecommendationPriority.LOW -> 0.1
        }
        
        // 基于风险级别
        effectiveness += when (recommendation.riskLevel) {
            RiskLevel.LOW -> 0.3
            RiskLevel.MEDIUM -> 0.2
            RiskLevel.HIGH -> 0.1
            RiskLevel.CRITICAL -> -0.2
        }
        
        // 基于预期改进
        effectiveness += minOf(recommendation.expectedImprovement * 2, 0.3)
        
        return effectiveness.coerceIn(0.0, 1.0)
    }
    
    /**
     * 更新建议历史
     */
    private fun updateRecommendationHistory(recommendations: List<OptimizationRecommendation>) {
        recommendations.forEach { recommendation ->
            val history = recommendationHistory.computeIfAbsent(recommendation.type.name) {
                RecommendationHistory(recommendation.type.name)
            }
            history.addRecommendation(recommendation)
            
            // 保持历史大小
            if (history.recommendations.size > HISTORY_SIZE) {
                history.recommendations.removeAt(0)
            }
        }
    }
    
    /**
     * 生成高级建议
     */
    private fun generateHighLevelRecommendations(
        overallMetrics: AiResponseParserMetrics.OverallMetrics,
        performanceSummary: AiResponseParserMetrics.PerformanceSummary,
        resourceReport: ResourceMonitor.ResourceReport
    ): List<HighLevelRecommendation> {
        val recommendations = mutableListOf<HighLevelRecommendation>()
        
        // 整体系统健康建议
        when {
            overallMetrics.successRate < 0.9 -> {
                recommendations.add(HighLevelRecommendation(
                    type = HighLevelRecommendationType.SYSTEM_HEALTH,
                    priority = RecommendationPriority.CRITICAL,
                    title = "系统健康状态不佳",
                    description = "解析成功率低于90%，建议立即进行系统优化",
                    actionItems = listOf(
                        "检查错误日志",
                        "优化错误处理机制",
                        "增加重试策略"
                    )
                ))
            }
            
            overallMetrics.successRate < 0.95 -> {
                recommendations.add(HighLevelRecommendation(
                    type = HighLevelRecommendationType.SYSTEM_HEALTH,
                    priority = RecommendationPriority.HIGH,
                    title = "系统健康状态需要改进",
                    description = "解析成功率低于95%，建议进行系统优化",
                    actionItems = listOf(
                        "分析失败原因",
                        "优化解析算法",
                        "增强容错能力"
                    )
                ))
            }
        }
        
        // 性能优化建议
        if (overallMetrics.averageParseTimeMs > 1000) {
            recommendations.add(HighLevelRecommendation(
                type = HighLevelRecommendationType.PERFORMANCE,
                priority = RecommendationPriority.HIGH,
                title = "系统性能需要优化",
                description = "平均响应时间超过1秒，建议进行性能优化",
                actionItems = listOf(
                    "启用缓存机制",
                    "优化热点代码",
                    "考虑并行处理"
                )
            ))
        }
        
        // 资源优化建议
        val currentMetrics = resourceReport.currentMetrics
        if (currentMetrics.memoryUsagePercent > 0.85 || currentMetrics.cpuUsagePercent > 0.85) {
            recommendations.add(HighLevelRecommendation(
                type = HighLevelRecommendationType.RESOURCE,
                priority = RecommendationPriority.MEDIUM,
                title = "资源使用需要优化",
                description = "内存或CPU使用率过高，建议进行资源优化",
                actionItems = listOf(
                    "优化内存分配",
                    "减少内存泄漏",
                    "优化算法复杂度"
                )
            ))
        }
        
        return recommendations.sortedByDescending { it.priority.value }
    }
    
    /**
     * 初始化建议策略
     */
    private fun initializeRecommendationStrategies() {
        recommendationStrategies[OptimizationOpportunityType.SUCCESS_RATE_IMPROVEMENT.name] = SuccessRateRecommendationStrategy()
        recommendationStrategies[OptimizationOpportunityType.PERFORMANCE_OPTIMIZATION.name] = PerformanceRecommendationStrategy()
        recommendationStrategies[OptimizationOpportunityType.MEMORY_OPTIMIZATION.name] = MemoryRecommendationStrategy()
        recommendationStrategies[OptimizationOpportunityType.CPU_OPTIMIZATION.name] = CpuRecommendationStrategy()
        recommendationStrategies[OptimizationOpportunityType.STRATEGY_IMPROVEMENT.name] = StrategyImprovementRecommendationStrategy()
        
        Log.d(TAG, "建议策略初始化完成，共${recommendationStrategies.size}个策略")
    }
    
    /**
     * 清理资源
     */
    fun cleanup() {
        disableOptimization()
        scope.cancel()
        recommendationHistory.clear()
        currentRecommendations.set(emptyList())
        
        Log.i(TAG, "优化建议优化器资源清理完成")
    }
    
    /**
     * 优化器配置
     */
    data class OptimizerConfig(
        val enableAutomaticOptimization: Boolean = true,
        val minImprovementThreshold: Double = MIN_IMPROVEMENT_THRESHOLD,
        val maxRecommendationsPerCycle: Int = 5,
        val enableHighRiskRecommendations: Boolean = false,
        val enablePredictiveAnalysis: Boolean = true,
        val historicalAnalysisWeight: Double = 0.6,
        val featureAnalysisWeight: Double = 0.4
    )
    
    /**
     * 系统状态
     */
    data class SystemState(
        val timestamp: Long,
        val overallMetrics: AiResponseParserMetrics.OverallMetrics,
        val performanceSummary: AiResponseParserMetrics.PerformanceSummary,
        val resourceMetrics: ResourceMonitor.ResourceMetrics,
        val optimizationStatus: AdaptivePerformanceOptimizer.OptimizationStatus,
        val tuningStatus: PerformanceTuner.TuningStatus
    )
    
    /**
     * 优化机会
     */
    data class OptimizationOpportunity(
        val type: OptimizationOpportunityType,
        val priority: OpportunityPriority,
        val currentValue: Double,
        val targetValue: Double,
        val potentialImprovement: Double,
        val description: String,
        val systemState: SystemState
    )
    
    /**
     * 优化机会类型
     */
    enum class OptimizationOpportunityType {
        SUCCESS_RATE_IMPROVEMENT,
        PERFORMANCE_OPTIMIZATION,
        MEMORY_OPTIMIZATION,
        CPU_OPTIMIZATION,
        STRATEGY_IMPROVEMENT,
        CACHING_OPTIMIZATION,
        ALGORITHM_OPTIMIZATION
    }
    
    /**
     * 机会优先级
     */
    enum class OpportunityPriority(val value: Int) {
        LOW(1),
        MEDIUM(2),
        HIGH(3),
        CRITICAL(4)
    }
    
    /**
     * 优化建议
     */
    data class OptimizationRecommendation(
        val id: String = "",
        val type: OptimizationOpportunityType,
        val priority: RecommendationPriority,
        val title: String,
        val description: String,
        val expectedImprovement: Double,
        val predictedEffectiveness: Double = 0.0,
        val implementationPlan: ImplementationPlan,
        val riskLevel: RiskLevel,
        val estimatedDuration: Long,
        val resourceRequirements: ResourceRequirements,
        val dependencies: List<String> = emptyList()
    )
    
    /**
     * 实施计划
     */
    data class ImplementationPlan(
        val steps: List<ImplementationStep>,
        val rollbackPlan: List<RollbackStep>,
        val verificationSteps: List<VerificationStep> = emptyList()
    )
    
    /**
     * 实施步骤
     */
    data class ImplementationStep(
        val id: String,
        val description: String,
        val action: String,
        val expectedDuration: Long,
        val dependencies: List<String> = emptyList()
    )
    
    /**
     * 回滚步骤
     */
    data class RollbackStep(
        val id: String,
        val description: String,
        val action: String,
        val expectedDuration: Long
    )
    
    /**
     * 验证步骤
     */
    data class VerificationStep(
        val id: String,
        val description: String,
        val action: String,
        val expectedDuration: Long,
        val successCriteria: String
    )
    
    /**
     * 风险级别
     */
    enum class RiskLevel {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }
    
    /**
     * 资源需求
     */
    data class ResourceRequirements(
        val memoryRequirement: Long = 0,
        val cpuRequirement: Double = 0.0,
        val storageRequirement: Long = 0,
        val networkRequirement: Boolean = false
    )
    
    /**
     * 建议优先级
     */
    enum class RecommendationPriority(val value: Int) {
        LOW(1),
        MEDIUM(2),
        HIGH(3),
        CRITICAL(4)
    }
    
    /**
     * 优化结果
     */
    data class OptimizationResult(
        val success: Boolean,
        val reason: String? = null,
        val generatedRecommendations: List<OptimizationRecommendation>,
        val durationMs: Long
    )
    
    /**
     * 优化状态
     */
    data class OptimizationStatus(
        val isEnabled: Boolean,
        val currentRecommendations: List<OptimizationRecommendation>,
        val recommendationHistory: List<OptimizationRecommendation>,
        val effectivenessAnalysis: RecommendationEffectivenessAnalyzer.EffectivenessAnalysis,
        val config: OptimizerConfig
    )
    
    /**
     * 建议历史
     */
    class RecommendationHistory(val recommendationType: String) {
        val recommendations = mutableListOf<OptimizationRecommendation>()
        
        fun addRecommendation(recommendation: OptimizationRecommendation) {
            recommendations.add(recommendation)
        }
        
        fun getRecentRecommendations(count: Int): List<OptimizationRecommendation> {
            return recommendations.takeLast(count)
        }
    }
    
    /**
     * 高级建议
     */
    data class HighLevelRecommendation(
        val type: HighLevelRecommendationType,
        val priority: RecommendationPriority,
        val title: String,
        val description: String,
        val actionItems: List<String>
    )
    
    /**
     * 高级建议类型
     */
    enum class HighLevelRecommendationType {
        SYSTEM_HEALTH,
        PERFORMANCE,
        RESOURCE,
        CAPACITY,
        RELIABILITY
    }
    
    /**
     * 优化报告
     */
    data class OptimizationReport(
        val timestamp: Long,
        val overallMetrics: AiResponseParserMetrics.OverallMetrics,
        val performanceSummary: AiResponseParserMetrics.PerformanceSummary,
        val resourceReport: ResourceMonitor.ResourceReport,
        val optimizationStatus: AdaptivePerformanceOptimizer.OptimizationStatus,
        val tuningStatus: PerformanceTuner.TuningStatus,
        val currentRecommendations: List<OptimizationRecommendation>,
        val recommendationHistory: List<OptimizationRecommendation>,
        val effectivenessAnalysis: RecommendationEffectivenessAnalyzer.EffectivenessAnalysis,
        val recommendations: List<HighLevelRecommendation>
    )
    
    /**
     * 建议策略接口
     */
    interface RecommendationStrategy {
        fun shouldRecommend(opportunity: OptimizationOpportunity, config: OptimizerConfig): Boolean
        fun generateRecommendation(
            opportunity: OptimizationOpportunity,
            config: OptimizerConfig
        ): OptimizationRecommendation?
    }
    
    /**
     * 成功率建议策略
     */
    private class SuccessRateRecommendationStrategy : RecommendationStrategy {
        override fun shouldRecommend(opportunity: OptimizationOpportunity, config: OptimizerConfig): Boolean {
            return opportunity.type == OptimizationOpportunityType.SUCCESS_RATE_IMPROVEMENT &&
                   opportunity.potentialImprovement >= config.minImprovementThreshold
        }
        
        override fun generateRecommendation(
            opportunity: OptimizationOpportunity,
            config: OptimizerConfig
        ): OptimizationRecommendation? {
            return OptimizationRecommendation(
                id = "success_rate_${System.currentTimeMillis()}",
                type = opportunity.type,
                priority = RecommendationPriority.HIGH,
                title = "提高解析成功率",
                description = "当前成功率${String.format("%.2f%%", opportunity.currentValue * 100)}，建议提高到${String.format("%.2f%%", opportunity.targetValue * 100)}",
                expectedImprovement = opportunity.potentialImprovement,
                implementationPlan = ImplementationPlan(
                    steps = listOf(
                        ImplementationStep(
                            id = "enhance_error_handling",
                            description = "增强错误处理",
                            action = "启用增强错误处理机制",
                            expectedDuration = 5000
                        ),
                        ImplementationStep(
                            id = "improve_field_mapping",
                            description = "改进字段映射",
                            action = "优化字段映射算法",
                            expectedDuration = 3000
                        )
                    ),
                    rollbackPlan = listOf(
                        RollbackStep(
                            id = "disable_enhancements",
                            description = "禁用增强功能",
                            action = "恢复到默认配置",
                            expectedDuration = 2000
                        )
                    )
                ),
                riskLevel = RiskLevel.LOW,
                estimatedDuration = 8000,
                resourceRequirements = ResourceRequirements(
                    memoryRequirement = 10 * 1024 * 1024, // 10MB
                    cpuRequirement = 0.1
                )
            )
        }
    }
    
    /**
     * 性能建议策略
     */
    private class PerformanceRecommendationStrategy : RecommendationStrategy {
        override fun shouldRecommend(opportunity: OptimizationOpportunity, config: OptimizerConfig): Boolean {
            return opportunity.type == OptimizationOpportunityType.PERFORMANCE_OPTIMIZATION
        }
        
        override fun generateRecommendation(
            opportunity: OptimizationOpportunity,
            config: OptimizerConfig
        ): OptimizationRecommendation? {
            return OptimizationRecommendation(
                id = "performance_${System.currentTimeMillis()}",
                type = opportunity.type,
                priority = RecommendationPriority.MEDIUM,
                title = "优化解析性能",
                description = "当前平均响应时间${opportunity.currentValue}ms，建议优化到${opportunity.targetValue}ms",
                expectedImprovement = opportunity.potentialImprovement,
                implementationPlan = ImplementationPlan(
                    steps = listOf(
                        ImplementationStep(
                            id = "enable_caching",
                            description = "启用缓存",
                            action = "启用智能缓存机制",
                            expectedDuration = 3000
                        ),
                        ImplementationStep(
                            id = "optimize_algorithm",
                            description = "优化算法",
                            action = "优化解析算法",
                            expectedDuration = 5000
                        )
                    ),
                    rollbackPlan = listOf(
                        RollbackStep(
                            id = "disable_caching",
                            description = "禁用缓存",
                            action = "禁用缓存机制",
                            expectedDuration = 2000
                        )
                    )
                ),
                riskLevel = RiskLevel.MEDIUM,
                estimatedDuration = 8000,
                resourceRequirements = ResourceRequirements(
                    memoryRequirement = 20 * 1024 * 1024, // 20MB
                    cpuRequirement = 0.2
                )
            )
        }
    }
    
    /**
     * 内存建议策略
     */
    private class MemoryRecommendationStrategy : RecommendationStrategy {
        override fun shouldRecommend(opportunity: OptimizationOpportunity, config: OptimizerConfig): Boolean {
            return opportunity.type == OptimizationOpportunityType.MEMORY_OPTIMIZATION
        }
        
        override fun generateRecommendation(
            opportunity: OptimizationOpportunity,
            config: OptimizerConfig
        ): OptimizationRecommendation? {
            return OptimizationRecommendation(
                id = "memory_${System.currentTimeMillis()}",
                type = opportunity.type,
                priority = RecommendationPriority.MEDIUM,
                title = "优化内存使用",
                description = "当前内存使用率${String.format("%.1f%%", opportunity.currentValue * 100)}，建议优化到${String.format("%.1f%%", opportunity.targetValue * 100)}",
                expectedImprovement = opportunity.potentialImprovement,
                implementationPlan = ImplementationPlan(
                    steps = listOf(
                        ImplementationStep(
                            id = "enable_object_pooling",
                            description = "启用对象池",
                            action = "启用对象池机制",
                            expectedDuration = 3000
                        ),
                        ImplementationStep(
                            id = "optimize_gc",
                            description = "优化垃圾回收",
                            action = "优化垃圾回收策略",
                            expectedDuration = 2000
                        )
                    ),
                    rollbackPlan = listOf(
                        RollbackStep(
                            id = "disable_object_pooling",
                            description = "禁用对象池",
                            action = "禁用对象池机制",
                            expectedDuration = 2000
                        )
                    )
                ),
                riskLevel = RiskLevel.LOW,
                estimatedDuration = 5000,
                resourceRequirements = ResourceRequirements(
                    memoryRequirement = 5 * 1024 * 1024, // 5MB
                    cpuRequirement = 0.1
                )
            )
        }
    }
    
    /**
     * CPU建议策略
     */
    private class CpuRecommendationStrategy : RecommendationStrategy {
        override fun shouldRecommend(opportunity: OptimizationOpportunity, config: OptimizerConfig): Boolean {
            return opportunity.type == OptimizationOpportunityType.CPU_OPTIMIZATION
        }
        
        override fun generateRecommendation(
            opportunity: OptimizationOpportunity,
            config: OptimizerConfig
        ): OptimizationRecommendation? {
            return OptimizationRecommendation(
                id = "cpu_${System.currentTimeMillis()}",
                type = opportunity.type,
                priority = RecommendationPriority.MEDIUM,
                title = "优化CPU使用",
                description = "当前CPU使用率${String.format("%.1f%%", opportunity.currentValue * 100)}，建议优化到${String.format("%.1f%%", opportunity.targetValue * 100)}",
                expectedImprovement = opportunity.potentialImprovement,
                implementationPlan = ImplementationPlan(
                    steps = listOf(
                        ImplementationStep(
                            id = "enable_parallel_processing",
                            description = "启用并行处理",
                            action = "启用并行解析处理",
                            expectedDuration = 4000
                        ),
                        ImplementationStep(
                            id = "optimize_algorithm",
                            description = "优化算法",
                            action = "优化CPU密集型算法",
                            expectedDuration = 5000
                        )
                    ),
                    rollbackPlan = listOf(
                        RollbackStep(
                            id = "disable_parallel_processing",
                            description = "禁用并行处理",
                            action = "禁用并行处理",
                            expectedDuration = 2000
                        )
                    )
                ),
                riskLevel = RiskLevel.MEDIUM,
                estimatedDuration = 9000,
                resourceRequirements = ResourceRequirements(
                    cpuRequirement = 0.2
                )
            )
        }
    }
    
    /**
     * 策略改进建议策略
     */
    private class StrategyImprovementRecommendationStrategy : RecommendationStrategy {
        override fun shouldRecommend(opportunity: OptimizationOpportunity, config: OptimizerConfig): Boolean {
            return opportunity.type == OptimizationOpportunityType.STRATEGY_IMPROVEMENT
        }
        
        override fun generateRecommendation(
            opportunity: OptimizationOpportunity,
            config: OptimizerConfig
        ): OptimizationRecommendation? {
            return OptimizationRecommendation(
                id = "strategy_${System.currentTimeMillis()}",
                type = opportunity.type,
                priority = RecommendationPriority.LOW,
                title = "改进优化策略",
                description = "当前策略效果不佳，建议改进优化策略",
                expectedImprovement = opportunity.potentialImprovement,
                implementationPlan = ImplementationPlan(
                    steps = listOf(
                        ImplementationStep(
                            id = "analyze_strategy_effectiveness",
                            description = "分析策略效果",
                            action = "深入分析现有策略的效果",
                            expectedDuration = 5000
                        ),
                        ImplementationStep(
                            id = "improve_recommendation_algorithm",
                            description = "改进建议算法",
                            action = "优化建议生成算法",
                            expectedDuration = 8000
                        )
                    ),
                    rollbackPlan = listOf(
                        RollbackStep(
                            id = "restore_previous_strategy",
                            description = "恢复之前的策略",
                            action = "恢复到之前的优化策略",
                            expectedDuration = 3000
                        )
                    )
                ),
                riskLevel = RiskLevel.MEDIUM,
                estimatedDuration = 13000,
                resourceRequirements = ResourceRequirements(
                    cpuRequirement = 0.3
                )
            )
        }
    }
    
    /**
     * 建议效果分析器
     */
    class RecommendationEffectivenessAnalyzer {
        private val feedbackHistory = ConcurrentHashMap<String, MutableList<RecommendationFeedback>>()
        
        fun recordFeedback(
            recommendationId: String,
            effectiveness: Double,
            applied: Boolean,
            comment: String = ""
        ) {
            val feedback = RecommendationFeedback(
                timestamp = System.currentTimeMillis(),
                effectiveness = effectiveness,
                applied = applied,
                comment = comment
            )
            
            feedbackHistory.computeIfAbsent(recommendationId) { mutableListOf() }.add(feedback)
            
            // 保持历史大小
            val history = feedbackHistory[recommendationId] ?: return
            if (history.size > 100) {
                history.removeAt(0)
            }
        }
        
        fun getEffectivenessAnalysis(): EffectivenessAnalysis {
            val allFeedback = feedbackHistory.values.flatten()
            val appliedFeedback = allFeedback.filter { it.applied }
            
            val averageEffectiveness = if (appliedFeedback.isNotEmpty()) {
                appliedFeedback.map { it.effectiveness }.average()
            } else 0.0
            
            val effectivenessByType = appliedFeedback
                .groupBy { extractRecommendationType(it.key) }
                .mapValues { it.value.map { feedback -> feedback.effectiveness }.average() }
            
            val mostEffectiveType = effectivenessByType
                .maxByOrNull { it.value }?.key
            
            return EffectivenessAnalysis(
                totalRecommendations = allFeedback.size,
                appliedRecommendations = appliedFeedback.size,
                averageEffectiveness = averageEffectiveness,
                effectivenessByType = effectivenessByType,
                mostEffectiveType = mostEffectiveType
            )
        }
        
        fun getAverageEffectiveness(type: OptimizationOpportunityType): Double {
            val typeFeedback = feedbackHistory.values.flatten()
                .filter { extractRecommendationType(it.key) == type }
                .filter { it.applied }
            
            return if (typeFeedback.isNotEmpty()) {
                typeFeedback.map { it.effectiveness }.average()
            } else 0.0
        }
        
        private fun extractRecommendationType(recommendationId: String): OptimizationOpportunityType? {
            return try {
                val parts = recommendationId.split("_")
                when (parts.firstOrNull()) {
                    "success" -> OptimizationOpportunityType.SUCCESS_RATE_IMPROVEMENT
                    "performance" -> OptimizationOpportunityType.PERFORMANCE_OPTIMIZATION
                    "memory" -> OptimizationOpportunityType.MEMORY_OPTIMIZATION
                    "cpu" -> OptimizationOpportunityType.CPU_OPTIMIZATION
                    "strategy" -> OptimizationOpportunityType.STRATEGY_IMPROVEMENT
                    else -> null
                }
            } catch (e: Exception) {
                null
            }
        }
        
        data class EffectivenessAnalysis(
            val totalRecommendations: Int,
            val appliedRecommendations: Int,
            val averageEffectiveness: Double,
            val effectivenessByType: Map<OptimizationOpportunityType, Double>,
            val mostEffectiveType: OptimizationOpportunityType?
        )
        
        data class RecommendationFeedback(
            val timestamp: Long,
            val effectiveness: Double,
            val applied: Boolean,
            val comment: String
        )
    }
}