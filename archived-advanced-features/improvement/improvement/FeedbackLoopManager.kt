package com.empathy.ai.data.improvement

import android.util.Log
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

/**
 * 反馈循环管理器
 * 
 * 负责收集、分析和处理用户反馈
 * 通过反馈驱动的改进机制，持续优化系统性能
 * 
 * 功能：
 * 1. 反馈收集和分类
 * 2. 反馈分析和模式识别
 * 3. 自动化反馈处理
 * 4. 反馈驱动的改进建议
 */
class FeedbackLoopManager private constructor() {
    
    companion object {
        private const val TAG = "FeedbackLoopManager"
        
        // 反馈配置
        private const val FEEDBACK_PROCESSING_INTERVAL_MS = 30_000L // 30秒
        private const val FEEDBACK_HISTORY_SIZE = 1000
        private const val MIN_SAMPLES_FOR_ANALYSIS = 10
        
        // 反馈阈值
        private const val NEGATIVE_FEEDBACK_THRESHOLD = 0.3 // 30%负面反馈阈值
        private const val POSITIVE_FEEDBACK_THRESHOLD = 0.7 // 70%正面反馈阈值
        
        @Volatile
        private var INSTANCE: FeedbackLoopManager? = null
        
        fun getInstance(): FeedbackLoopManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FeedbackLoopManager().also { INSTANCE = it }
            }
        }
    }
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val feedbackConfig = AtomicReference(FeedbackConfig())
    private val feedbackHistory = ConcurrentHashMap<String, FeedbackHistory>()
    private val feedbackPatterns = ConcurrentHashMap<String, FeedbackPattern>()
    private val feedbackProcessors = ConcurrentHashMap<String, FeedbackProcessor>()
    private val feedbackAnalyzer = FeedbackAnalyzer()
    private val feedbackActions = ConcurrentHashMap<String, FeedbackAction>()
    
    private var isFeedbackLoopEnabled = false
    private var feedbackProcessingJob: Job? = null
    private val feedbackCounter = AtomicLong(0)
    
    init {
        initializeFeedbackProcessors()
        initializeFeedbackActions()
        Log.i(TAG, "反馈循环管理器初始化完成")
    }
    
    /**
     * 启用反馈循环
     */
    fun enableFeedbackLoop(config: FeedbackConfig = FeedbackConfig()) {
        if (isFeedbackLoopEnabled) {
            Log.w(TAG, "反馈循环已启用")
            return
        }
        
        feedbackConfig.set(config)
        isFeedbackLoopEnabled = true
        
        feedbackProcessingJob = scope.launch {
            while (isActive) {
                try {
                    performFeedbackProcessingCycle()
                    delay(FEEDBACK_PROCESSING_INTERVAL_MS)
                } catch (e: Exception) {
                    Log.e(TAG, "反馈处理周期执行失败", e)
                }
            }
        }
        
        Log.i(TAG, "反馈循环已启用")
    }
    
    /**
     * 禁用反馈循环
     */
    fun disableFeedbackLoop() {
        if (!isFeedbackLoopEnabled) {
            Log.w(TAG, "反馈循环未启用")
            return
        }
        
        isFeedbackLoopEnabled = false
        feedbackProcessingJob?.cancel()
        feedbackProcessingJob = null
        
        Log.i(TAG, "反馈循环已禁用")
    }
    
    /**
     * 记录反馈
     */
    fun recordFeedback(
        source: String,
        type: FeedbackType,
        rating: Double,
        category: FeedbackCategory,
        comment: String = "",
        metadata: Map<String, Any> = emptyMap()
    ): String {
        val feedbackId = generateFeedbackId()
        val feedback = Feedback(
            id = feedbackId,
            source = source,
            type = type,
            rating = rating,
            category = category,
            comment = comment,
            metadata = metadata,
            timestamp = System.currentTimeMillis(),
            processed = false
        )
        
        // 记录反馈
        val history = feedbackHistory.computeIfAbsent(source) { FeedbackHistory(source) }
        history.addFeedback(feedback)
        
        // 保持历史大小
        if (history.feedbacks.size > FEEDBACK_HISTORY_SIZE) {
            history.feedbacks.removeAt(0)
        }
        
        Log.i(TAG, "反馈已记录: $feedbackId, 来源=$source, 类型=$type, 评分=$rating")
        
        return feedbackId
    }
    
    /**
     * 记录自动反馈
     */
    fun recordAutomaticFeedback(
        source: String,
        type: AutomaticFeedbackType,
        data: Map<String, Any>,
        confidence: Double = 1.0
    ): String {
        val feedbackId = generateFeedbackId()
        val feedback = AutomaticFeedback(
            id = feedbackId,
            source = source,
            type = type,
            data = data,
            confidence = confidence,
            timestamp = System.currentTimeMillis(),
            processed = false
        )
        
        // 记录自动反馈
        val history = feedbackHistory.computeIfAbsent(source) { FeedbackHistory(source) }
        history.addAutomaticFeedback(feedback)
        
        // 保持历史大小
        if (history.automaticFeedbacks.size > FEEDBACK_HISTORY_SIZE) {
            history.automaticFeedbacks.removeAt(0)
        }
        
        Log.i(TAG, "自动反馈已记录: $feedbackId, 来源=$source, 类型=$type")
        
        return feedbackId
    }
    
    /**
     * 获取反馈状态
     */
    fun getFeedbackStatus(): FeedbackStatus {
        val allFeedbacks = feedbackHistory.values.flatMap { it.feedbacks }
        val allAutomaticFeedbacks = feedbackHistory.values.flatMap { it.automaticFeedbacks }
        val feedbackPatterns = feedbackPatterns.values.toList()
        val feedbackProcessorsStatus = feedbackProcessors.values.map { it.getStatus() }
        val feedbackActionsStatus = feedbackActions.values.map { it.getStatus() }
        val feedbackAnalysis = feedbackAnalyzer.getAnalysisResult()
        
        return FeedbackStatus(
            isEnabled = isFeedbackLoopEnabled,
            totalFeedbacks = allFeedbacks.size,
            totalAutomaticFeedbacks = allAutomaticFeedbacks.size,
            feedbackBySource = feedbackHistory.mapValues { it.getRecentFeedbacks(10) },
            feedbackPatterns = feedbackPatterns,
            feedbackProcessorsStatus = feedbackProcessorsStatus,
            feedbackActionsStatus = feedbackActionsStatus,
            feedbackAnalysis = feedbackAnalysis,
            config = feedbackConfig.get()
        )
    }
    
    /**
     * 获取反馈报告
     */
    fun getFeedbackReport(): FeedbackReport {
        val allFeedbacks = feedbackHistory.values.flatMap { it.feedbacks }
        val allAutomaticFeedbacks = feedbackHistory.values.flatMap { it.automaticFeedbacks }
        
        val feedbackSummary = analyzeFeedbackSummary(allFeedbacks)
        val automaticFeedbackSummary = analyzeAutomaticFeedbackSummary(allAutomaticFeedbacks)
        val feedbackTrends = analyzeFeedbackTrends(allFeedbacks)
        val feedbackPatterns = feedbackPatterns.values.toList()
        val feedbackAnalysis = feedbackAnalyzer.getAnalysisResult()
        val recommendations = generateFeedbackRecommendations(feedbackSummary, feedbackTrends)
        
        return FeedbackReport(
            timestamp = System.currentTimeMillis(),
            feedbackSummary = feedbackSummary,
            automaticFeedbackSummary = automaticFeedbackSummary,
            feedbackTrends = feedbackTrends,
            feedbackPatterns = feedbackPatterns,
            feedbackAnalysis = feedbackAnalysis,
            recommendations = recommendations
        )
    }
    
    /**
     * 更新反馈配置
     */
    fun updateConfig(config: FeedbackConfig) {
        feedbackConfig.set(config)
        Log.i(TAG, "反馈配置已更新: $config")
    }
    
    /**
     * 手动触发反馈处理
     */
    suspend fun triggerFeedbackProcessing(): FeedbackProcessingResult {
        return performFeedbackProcessingCycle()
    }
    
    /**
     * 执行反馈处理周期
     */
    private suspend fun performFeedbackProcessingCycle(): FeedbackProcessingResult {
        val startTime = System.currentTimeMillis()
        val config = feedbackConfig.get()
        
        if (!config.enableAutomaticProcessing) {
            return FeedbackProcessingResult(
                success = false,
                reason = "自动反馈处理已禁用",
                processedFeedbacks = emptyList(),
                appliedActions = emptyList(),
                durationMs = System.currentTimeMillis() - startTime
            )
        }
        
        val processedFeedbacks = mutableListOf<Feedback>()
        val appliedActions = mutableListOf<FeedbackAction>()
        var processingSuccess = true
        var failureReason: String? = null
        
        try {
            // 1. 收集未处理的反馈
            val unprocessedFeedbacks = collectUnprocessedFeedbacks()
            
            // 2. 分类和处理反馈
            for (feedback in unprocessedFeedbacks) {
                val processor = feedbackProcessors[feedback.type.name]
                if (processor != null && processor.canProcess(feedback, config)) {
                    val result = processor.processFeedback(feedback, config)
                    
                    if (result.success) {
                        processedFeedbacks.add(feedback)
                        feedback.processed = true
                        
                        // 应用处理动作
                        result.actions.forEach { action ->
                            appliedActions.add(action)
                            action.execute()
                        }
                    } else {
                        Log.w(TAG, "反馈处理失败: ${feedback.id}, 原因: ${result.reason}")
                    }
                } else {
                    Log.w(TAG, "找不到适合的处理器: ${feedback.type}")
                }
            }
            
            // 3. 更新反馈模式
            updateFeedbackPatterns(processedFeedbacks)
            
            // 4. 触发反馈分析
            feedbackAnalyzer.analyzeFeedback(processedFeedbacks)
            
        } catch (e: Exception) {
            processingSuccess = false
            failureReason = e.message
            Log.e(TAG, "反馈处理周期执行异常", e)
        }
        
        val duration = System.currentTimeMillis() - startTime
        
        return FeedbackProcessingResult(
            success = processingSuccess,
            reason = failureReason,
            processedFeedbacks = processedFeedbacks,
            appliedActions = appliedActions,
            durationMs = duration
        )
    }
    
    /**
     * 收集未处理的反馈
     */
    private fun collectUnprocessedFeedbacks(): List<Feedback> {
        return feedbackHistory.values.flatMap { history ->
            history.feedbacks.filter { !it.processed }
        }
    }
    
    /**
     * 更新反馈模式
     */
    private fun updateFeedbackPatterns(feedbacks: List<Feedback>) {
        val config = feedbackConfig.get()
        
        if (!config.enablePatternDetection) {
            return
        }
        
        // 按类型分组反馈
        val feedbacksByType = feedbacks.groupBy { it.type }
        
        feedbacksByType.forEach { (type, typeFeedbacks) ->
            if (typeFeedbacks.size >= MIN_SAMPLES_FOR_ANALYSIS) {
                // 分析模式
                val pattern = analyzeFeedbackPattern(type, typeFeedbacks)
                
                // 更新模式
                feedbackPatterns[type.name] = pattern
                
                Log.d(TAG, "反馈模式已更新: $type -> $pattern")
            }
        }
    }
    
    /**
     * 分析反馈模式
     */
    private fun analyzeFeedbackPattern(
        type: FeedbackType,
        feedbacks: List<Feedback>
    ): FeedbackPattern {
        val ratings = feedbacks.map { it.rating }
        val averageRating = ratings.average()
        val ratingTrend = calculateRatingTrend(ratings)
        
        // 分析评论关键词
        val commentKeywords = analyzeCommentKeywords(feedbacks.mapNotNull { it.comment })
        
        // 分析时间模式
        val timePattern = analyzeTimePattern(feedbacks)
        
        // 分析来源分布
        val sourceDistribution = feedbacks.groupingBy { it.source }.mapValues { it.value.size }
        
        return FeedbackPattern(
            type = type,
            sampleCount = feedbacks.size,
            averageRating = averageRating,
            ratingTrend = ratingTrend,
            commentKeywords = commentKeywords,
            timePattern = timePattern,
            sourceDistribution = sourceDistribution,
            lastUpdated = System.currentTimeMillis()
        )
    }
    
    /**
     * 计算评分趋势
     */
    private fun calculateRatingTrend(ratings: List<Double>): RatingTrend {
        if (ratings.size < 3) {
            return RatingTrend.STABLE
        }
        
        val recent = ratings.takeLast(3).average()
        val older = ratings.dropLast(3).takeLast(3).average()
        
        return when {
            recent > older * 1.1 -> RatingTrend.IMPROVING
            recent < older * 0.9 -> RatingTrend.DECLINING
            else -> RatingTrend.STABLE
        }
    }
    
    /**
     * 分析评论关键词
     */
    private fun analyzeCommentKeywords(comments: List<String>): Map<String, Int> {
        val keywordFrequency = mutableMapOf<String, Int>()
        
        comments.forEach { comment ->
            val words = comment.lowercase().split(Regex("""\s+|[^\w]+"""))
            words.forEach { word ->
                if (word.length >= 3) {
                    keywordFrequency[word] = keywordFrequency.getOrDefault(word, 0) + 1
                }
            }
        }
        
        return keywordFrequency
            .toList()
            .sortedByDescending { it.second }
            .take(20)
            .toMap()
    }
    
    /**
     * 分析时间模式
     */
    private fun analyzeTimePattern(feedbacks: List<Feedback>): TimePattern {
        val timestamps = feedbacks.map { it.timestamp }
        
        if (timestamps.size < 2) {
            return TimePattern()
        }
        
        // 计算时间间隔
        val intervals = timestamps.zipWithNext().map { (current, next) ->
            next - current
        }
        
        val averageInterval = intervals.average()
        val peakHours = timestamps
            .map { it / (60 * 60 * 1000) % 24 } // 转换为小时
            .groupingBy { it }
            .mapValues { it.value.size }
            .maxByOrNull { it.value }?.key
        
        return TimePattern(
            averageIntervalMs = averageInterval,
            peakHour = peakHours ?: 12,
            feedbackFrequency = if (averageInterval > 0) 3600000.0 / averageInterval else 0.0
        )
    }
    
    /**
     * 分析反馈摘要
     */
    private fun analyzeFeedbackSummary(feedbacks: List<Feedback>): FeedbackSummary {
        if (feedbacks.isEmpty()) {
            return FeedbackSummary()
        }
        
        val totalFeedbacks = feedbacks.size
        val averageRating = feedbacks.map { it.rating }.average()
        val ratingDistribution = feedbacks.groupingBy { it.ratingCategory }.mapValues { it.value.size }
        val typeDistribution = feedbacks.groupingBy { it.type }.mapValues { it.value.size }
        val categoryDistribution = feedbacks.groupingBy { it.category }.mapValues { it.value.size }
        val sourceDistribution = feedbacks.groupingBy { it.source }.mapValues { it.value.size }
        
        val positiveFeedbacks = feedbacks.count { it.rating >= POSITIVE_FEEDBACK_THRESHOLD }
        val negativeFeedbacks = feedbacks.count { it.rating <= NEGATIVE_FEEDBACK_THRESHOLD }
        val positiveFeedbackRate = if (totalFeedbacks > 0) positiveFeedbacks.toDouble() / totalFeedbacks else 0.0
        
        return FeedbackSummary(
            totalFeedbacks = totalFeedbacks,
            averageRating = averageRating,
            ratingDistribution = ratingDistribution,
            typeDistribution = typeDistribution,
            categoryDistribution = categoryDistribution,
            sourceDistribution = sourceDistribution,
            positiveFeedbacks = positiveFeedbacks,
            negativeFeedbacks = negativeFeedbacks,
            positiveFeedbackRate = positiveFeedbackRate
        )
    }
    
    /**
     * 分析自动反馈摘要
     */
    private fun analyzeAutomaticFeedbackSummary(automaticFeedbacks: List<AutomaticFeedback>): AutomaticFeedbackSummary {
        if (automaticFeedbacks.isEmpty()) {
            return AutomaticFeedbackSummary()
        }
        
        val totalFeedbacks = automaticFeedbacks.size
        val typeDistribution = automaticFeedbacks.groupingBy { it.type }.mapValues { it.value.size }
        val sourceDistribution = automaticFeedbacks.groupingBy { it.source }.mapValues { it.value.size }
        val averageConfidence = automaticFeedbacks.map { it.confidence }.average()
        
        return AutomaticFeedbackSummary(
            totalFeedbacks = totalFeedbacks,
            typeDistribution = typeDistribution,
            sourceDistribution = sourceDistribution,
            averageConfidence = averageConfidence
        )
    }
    
    /**
     * 分析反馈趋势
     */
    private fun analyzeFeedbackTrends(feedbacks: List<Feedback>): FeedbackTrends {
        if (feedbacks.size < 10) {
            return FeedbackTrends()
        }
        
        // 按时间排序
        val sortedFeedbacks = feedbacks.sortedBy { it.timestamp }
        
        // 计算评分趋势
        val ratingTrend = calculateRatingTrend(sortedFeedbacks.map { it.rating })
        
        // 计算反馈频率趋势
        val timeWindows = sortedFeedbacks.windowed(7, 7) // 每周一个窗口
        val weeklyCounts = timeWindows.map { it.size }
        val frequencyTrend = calculateFrequencyTrend(weeklyCounts)
        
        // 计算类型分布趋势
        val typeTrends = mutableMapOf<FeedbackType, Trend>()
        FeedbackType.values().forEach { type ->
            val typeFeedbacks = sortedFeedbacks.filter { it.type == type }
            if (typeFeedbacks.size >= 5) {
                typeTrends[type] = calculateFrequencyTrend(
                    typeFeedbacks.windowed(5, 5).map { it.size }
                )
            }
        }
        
        return FeedbackTrends(
            ratingTrend = ratingTrend,
            frequencyTrend = frequencyTrend,
            typeTrends = typeTrends,
            timeSpan = sortedFeedbacks.last().timestamp - sortedFeedbacks.first().timestamp
        )
    }
    
    /**
     * 计算频率趋势
     */
    private fun calculateFrequencyTrend(counts: List<Int>): Trend {
        if (counts.size < 3) {
            return Trend.STABLE
        }
        
        val recent = counts.takeLast(3).average()
        val older = counts.dropLast(3).takeLast(3).average()
        
        return when {
            recent > older * 1.2 -> Trend.INCREASING
            recent < older * 0.8 -> Trend.DECREASING
            else -> Trend.STABLE
        }
    }
    
    /**
     * 生成反馈建议
     */
    private fun generateFeedbackRecommendations(
        summary: FeedbackSummary,
        trends: FeedbackTrends
    ): List<FeedbackRecommendation> {
        val recommendations = mutableListOf<FeedbackRecommendation>()
        
        // 基于正面反馈率生成建议
        when {
            summary.positiveFeedbackRate < 0.5 -> {
                recommendations.add(FeedbackRecommendation(
                    type = FeedbackRecommendationType.IMPROVE_USER_EXPERIENCE,
                    priority = RecommendationPriority.HIGH,
                    title = "用户体验需要改进",
                    description = "正面反馈率低于50%，建议立即改进用户体验",
                    actionItems = listOf(
                        "分析负面反馈原因",
                        "优化用户界面",
                        "改进功能易用性",
                        "加强用户支持"
                    )
                ))
            }
            
            summary.positiveFeedbackRate < 0.7 -> {
                recommendations.add(FeedbackRecommendation(
                    type = FeedbackRecommendationType.OPTIMIZE_FEATURES,
                    priority = RecommendationPriority.MEDIUM,
                    title = "功能需要优化",
                    description = "正面反馈率低于70%，建议优化相关功能",
                    actionItems = listOf(
                        "分析中等反馈原因",
                        "优化功能性能",
                        "改进功能设计",
                        "增加用户引导"
                    )
                ))
            }
        }
        
        // 基于趋势生成建议
        when (trends.ratingTrend) {
            Trend.DECLINING -> {
                recommendations.add(FeedbackRecommendation(
                    type = FeedbackRecommendationType.ADDRESS_DECLINING_RATING,
                    priority = RecommendationPriority.HIGH,
                    title = "评分呈下降趋势",
                    description = "用户评分持续下降，需要立即关注",
                    actionItems = listOf(
                        "深入分析下降原因",
                        "紧急修复问题",
                        "加强质量保证",
                        "主动收集用户反馈"
                    )
                ))
            }
            
            Trend.INCREASING -> {
                recommendations.add(FeedbackRecommendation(
                    type = FeedbackRecommendationType.MAINTAIN_IMPROVEMENT,
                    priority = RecommendationPriority.LOW,
                    title = "保持改进趋势",
                    description = "用户评分呈上升趋势，建议保持当前改进方向",
                    actionItems = listOf(
                        "继续当前优化策略",
                        "加强正面因素",
                        "预防潜在问题",
                        "定期评估效果"
                    )
                ))
            }
        }
        
        // 基于反馈类型分布生成建议
        summary.typeDistribution.forEach { (type, count) ->
            val totalFeedbacks = summary.totalFeedbacks
            val percentage = count.toDouble() / totalFeedbacks
            
            when {
                type == FeedbackType.BUG && percentage > 0.3 -> {
                    recommendations.add(FeedbackRecommendation(
                        type = FeedbackRecommendationType.REDUCE_BUG_REPORTS,
                        priority = RecommendationPriority.HIGH,
                        title = "Bug反馈过多",
                        description = "Bug反馈占比${String.format("%.1f%%", percentage * 100)}，建议加强质量保证",
                        actionItems = listOf(
                            "加强测试流程",
                            "改进代码审查",
                            "增加自动化测试",
                            "优化发布流程"
                        )
                    ))
                }
                
                type == FeedbackType.FEATURE_REQUEST && percentage > 0.2 -> {
                    recommendations.add(FeedbackRecommendation(
                        type = FeedbackRecommendationType.PRIORITIZE_FEATURE_REQUESTS,
                        priority = RecommendationPriority.MEDIUM,
                        title = "功能请求较多",
                        description = "功能请求占比${String.format("%.1f%%", percentage * 100)}，建议优先处理",
                        actionItems = listOf(
                            "分析功能请求模式",
                            "制定功能路线图",
                            "优化资源分配",
                            "加强与用户沟通"
                        )
                    ))
                }
            }
        }
        
        return recommendations.sortedByDescending { it.priority.value }
    }
    
    /**
     * 初始化反馈处理器
     */
    private fun initializeFeedbackProcessors() {
        feedbackProcessors[FeedbackType.BUG.name] = BugFeedbackProcessor()
        feedbackProcessors[FeedbackType.FEATURE_REQUEST.name] = FeatureRequestFeedbackProcessor()
        feedbackProcessors[FeedbackType.PERFORMANCE.name] = PerformanceFeedbackProcessor()
        feedbackProcessors[FeedbackType.USABILITY.name] = UsabilityFeedbackProcessor()
        feedbackProcessors[FeedbackType.GENERAL.name] = GeneralFeedbackProcessor()
        
        Log.d(TAG, "反馈处理器初始化完成，共${feedbackProcessors.size}个处理器")
    }
    
    /**
     * 初始化反馈动作
     */
    private fun initializeFeedbackActions() {
        feedbackActions["auto_escalation"] = AutoEscalationAction()
        feedbackActions["auto_response"] = AutoResponseAction()
        feedbackActions["auto_categorization"] = AutoCategorizationAction()
        feedbackActions["auto_prioritization"] = AutoPrioritizationAction()
        
        Log.d(TAG, "反馈动作初始化完成，共${feedbackActions.size}个动作")
    }
    
    /**
     * 生成反馈ID
     */
    private fun generateFeedbackId(): String {
        return "fb_${feedbackCounter.incrementAndGet()}_${System.currentTimeMillis()}"
    }
    
    /**
     * 清理资源
     */
    fun cleanup() {
        disableFeedbackLoop()
        scope.cancel()
        feedbackHistory.clear()
        feedbackPatterns.clear()
        feedbackProcessors.clear()
        feedbackActions.clear()
        
        Log.i(TAG, "反馈循环管理器资源清理完成")
    }
    
    /**
     * 反馈配置
     */
    data class FeedbackConfig(
        val enableAutomaticProcessing: Boolean = true,
        val enablePatternDetection: Boolean = true,
        val enableAutoActions: Boolean = true,
        val minConfidenceThreshold: Double = 0.7,
        val maxProcessingTimeMs: Long = 60000L, // 1分钟
        val enableEscalation: Boolean = true,
        val escalationThreshold: Double = 0.2 // 20%负面反馈触发升级
    )
    
    /**
     * 反馈类型
     */
    enum class FeedbackType {
        BUG,
        FEATURE_REQUEST,
        PERFORMANCE,
        USABILITY,
        GENERAL
    }
    
    /**
     * 自动反馈类型
     */
    enum class AutomaticFeedbackType {
        PERFORMANCE_METRIC,
        ERROR_RATE,
        RESOURCE_USAGE,
        SYSTEM_EVENT,
        USER_BEHAVIOR
    }
    
    /**
     * 反馈类别
     */
    enum class FeedbackCategory {
        CRITICAL,
        HIGH,
        MEDIUM,
        LOW,
        INFO
    }
    
    /**
     * 评分类别
     */
    enum class RatingCategory {
        VERY_POOR,
        POOR,
        AVERAGE,
        GOOD,
        EXCELLENT
    }
    
    /**
     * 反馈
     */
    data class Feedback(
        val id: String,
        val source: String,
        val type: FeedbackType,
        val rating: Double,
        val category: FeedbackCategory,
        val comment: String,
        val metadata: Map<String, Any>,
        val timestamp: Long,
        var processed: Boolean
    ) {
        val ratingCategory: RatingCategory
            get() = when {
                rating >= 4.5 -> RatingCategory.EXCELLENT
                rating >= 3.5 -> RatingCategory.GOOD
                rating >= 2.5 -> RatingCategory.AVERAGE
                rating >= 1.5 -> RatingCategory.POOR
                else -> RatingCategory.VERY_POOR
            }
    }
    
    /**
     * 自动反馈
     */
    data class AutomaticFeedback(
        val id: String,
        val source: String,
        val type: AutomaticFeedbackType,
        val data: Map<String, Any>,
        val confidence: Double,
        val timestamp: Long,
        var processed: Boolean
    )
    
    /**
     * 反馈历史
     */
    class FeedbackHistory(val source: String) {
        val feedbacks = mutableListOf<Feedback>()
        val automaticFeedbacks = mutableListOf<AutomaticFeedback>()
        
        fun addFeedback(feedback: Feedback) {
            feedbacks.add(feedback)
        }
        
        fun addAutomaticFeedback(automaticFeedback: AutomaticFeedback) {
            automaticFeedbacks.add(automaticFeedback)
        }
        
        fun getRecentFeedbacks(count: Int): List<Feedback> {
            return feedbacks.takeLast(count)
        }
        
        fun getRecentAutomaticFeedbacks(count: Int): List<AutomaticFeedback> {
            return automaticFeedbacks.takeLast(count)
        }
    }
    
    /**
     * 反馈模式
     */
    data class FeedbackPattern(
        val type: FeedbackType,
        val sampleCount: Int,
        val averageRating: Double,
        val ratingTrend: RatingTrend,
        val commentKeywords: Map<String, Int>,
        val timePattern: TimePattern,
        val sourceDistribution: Map<String, Int>,
        val lastUpdated: Long
    )
    
    /**
     * 时间模式
     */
    data class TimePattern(
        val averageIntervalMs: Double = 0.0,
        val peakHour: Int = 12,
        val feedbackFrequency: Double = 0.0
    )
    
    /**
     * 评分趋势
     */
    enum class RatingTrend {
        STABLE,
        IMPROVING,
        DECLINING
    }
    
    /**
     * 趋势
     */
    enum class Trend {
        STABLE,
        INCREASING,
        DECREASING
    }
    
    /**
     * 反馈状态
     */
    data class FeedbackStatus(
        val isEnabled: Boolean,
        val totalFeedbacks: Int,
        val totalAutomaticFeedbacks: Int,
        val feedbackBySource: Map<String, FeedbackHistory>,
        val feedbackPatterns: List<FeedbackPattern>,
        val feedbackProcessorsStatus: List<ProcessorStatus>,
        val feedbackActionsStatus: List<ActionStatus>,
        val feedbackAnalysis: FeedbackAnalyzer.AnalysisResult,
        val config: FeedbackConfig
    )
    
    /**
     * 处理器状态
     */
    data class ProcessorStatus(
        val name: String,
        val isEnabled: Boolean,
        val processedCount: Int,
        val lastProcessingTime: Long
    )
    
    /**
     * 动作状态
     */
    data class ActionStatus(
        val name: String,
        val isEnabled: Boolean,
        val executionCount: Int,
        val lastExecutionTime: Long
    )
    
    /**
     * 反馈摘要
     */
    data class FeedbackSummary(
        val totalFeedbacks: Int = 0,
        val averageRating: Double = 0.0,
        val ratingDistribution: Map<RatingCategory, Int> = emptyMap(),
        val typeDistribution: Map<FeedbackType, Int> = emptyMap(),
        val categoryDistribution: Map<FeedbackCategory, Int> = emptyMap(),
        val sourceDistribution: Map<String, Int> = emptyMap(),
        val positiveFeedbacks: Int = 0,
        val negativeFeedbacks: Int = 0,
        val positiveFeedbackRate: Double = 0.0
    )
    
    /**
     * 自动反馈摘要
     */
    data class AutomaticFeedbackSummary(
        val totalFeedbacks: Int = 0,
        val typeDistribution: Map<AutomaticFeedbackType, Int> = emptyMap(),
        val sourceDistribution: Map<String, Int> = emptyMap(),
        val averageConfidence: Double = 0.0
    )
    
    /**
     * 反馈趋势
     */
    data class FeedbackTrends(
        val ratingTrend: RatingTrend = RatingTrend.STABLE,
        val frequencyTrend: Trend = Trend.STABLE,
        val typeTrends: Map<FeedbackType, Trend> = emptyMap(),
        val timeSpan: Long = 0L
    )
    
    /**
     * 反馈处理结果
     */
    data class FeedbackProcessingResult(
        val success: Boolean,
        val reason: String? = null,
        val processedFeedbacks: List<Feedback>,
        val appliedActions: List<FeedbackAction>,
        val durationMs: Long
    )
    
    /**
     * 反馈报告
     */
    data class FeedbackReport(
        val timestamp: Long,
        val feedbackSummary: FeedbackSummary,
        val automaticFeedbackSummary: AutomaticFeedbackSummary,
        val feedbackTrends: FeedbackTrends,
        val feedbackPatterns: List<FeedbackPattern>,
        val feedbackAnalysis: FeedbackAnalyzer.AnalysisResult,
        val recommendations: List<FeedbackRecommendation>
    )
    
    /**
     * 反馈建议类型
     */
    enum class FeedbackRecommendationType {
        IMPROVE_USER_EXPERIENCE,
        OPTIMIZE_FEATURES,
        ADDRESS_DECLINING_RATING,
        MAINTAIN_IMPROVEMENT,
        REDUCE_BUG_REPORTS,
        PRIORITIZE_FEATURE_REQUESTS
    }
    
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
     * 反馈建议
     */
    data class FeedbackRecommendation(
        val type: FeedbackRecommendationType,
        val priority: RecommendationPriority,
        val title: String,
        val description: String,
        val actionItems: List<String>
    )
    
    /**
     * 反馈处理器接口
     */
    interface FeedbackProcessor {
        fun canProcess(feedback: Feedback, config: FeedbackConfig): Boolean
        fun processFeedback(feedback: Feedback, config: FeedbackConfig): ProcessingResult
        fun getStatus(): ProcessorStatus
    }
    
    /**
     * 处理结果
     */
    data class ProcessingResult(
        val success: Boolean,
        val actions: List<FeedbackAction>,
        val reason: String? = null
    )
    
    /**
     * 反馈动作接口
     */
    interface FeedbackAction {
        fun execute(): Boolean
        fun getStatus(): ActionStatus
    }
    
    /**
     * Bug反馈处理器
     */
    private class BugFeedbackProcessor : FeedbackProcessor {
        private var processedCount = 0
        private var lastProcessingTime = 0L
        
        override fun canProcess(feedback: Feedback, config: FeedbackConfig): Boolean {
            return feedback.type == FeedbackType.BUG
        }
        
        override fun processFeedback(feedback: Feedback, config: FeedbackConfig): ProcessingResult {
            processedCount++
            lastProcessingTime = System.currentTimeMillis()
            
            val actions = mutableListOf<FeedbackAction>()
            
            // 如果是严重Bug，触发自动升级
            if (feedback.category == FeedbackCategory.CRITICAL && config.enableEscalation) {
                actions.add(feedbackActions["auto_escalation"]!!)
            }
            
            // 如果是已知Bug模式，发送自动回复
            if (feedback.comment.contains("已知问题")) {
                actions.add(feedbackActions["auto_response"]!!)
            }
            
            return ProcessingResult(
                success = true,
                actions = actions
            )
        }
        
        override fun getStatus(): ProcessorStatus {
            return ProcessorStatus(
                name = "BugFeedbackProcessor",
                isEnabled = true,
                processedCount = processedCount,
                lastProcessingTime = lastProcessingTime
            )
        }
    }
    
    /**
     * 功能请求反馈处理器
     */
    private class FeatureRequestFeedbackProcessor : FeedbackProcessor {
        private var processedCount = 0
        private var lastProcessingTime = 0L
        
        override fun canProcess(feedback: Feedback, config: FeedbackConfig): Boolean {
            return feedback.type == FeedbackType.FEATURE_REQUEST
        }
        
        override fun processFeedback(feedback: Feedback, config: FeedbackConfig): ProcessingResult {
            processedCount++
            lastProcessingTime = System.currentTimeMillis()
            
            val actions = mutableListOf<FeedbackAction>()
            
            // 自动分类功能请求
            if (config.enableAutoActions) {
                actions.add(feedbackActions["auto_categorization"]!!)
            }
            
            // 自动优先级排序
            if (feedback.category == FeedbackCategory.HIGH) {
                actions.add(feedbackActions["auto_prioritization"]!!)
            }
            
            return ProcessingResult(
                success = true,
                actions = actions
            )
        }
        
        override fun getStatus(): ProcessorStatus {
            return ProcessorStatus(
                name = "FeatureRequestFeedbackProcessor",
                isEnabled = true,
                processedCount = processedCount,
                lastProcessingTime = lastProcessingTime
            )
        }
    }
    
    /**
     * 性能反馈处理器
     */
    private class PerformanceFeedbackProcessor : FeedbackProcessor {
        private var processedCount = 0
        private var lastProcessingTime = 0L
        
        override fun canProcess(feedback: Feedback, config: FeedbackConfig): Boolean {
            return feedback.type == FeedbackType.PERFORMANCE
        }
        
        override fun processFeedback(feedback: Feedback, config: FeedbackConfig): ProcessingResult {
            processedCount++
            lastProcessingTime = System.currentTimeMillis()
            
            val actions = mutableListOf<FeedbackAction>()
            
            // 性能问题自动升级
            if (feedback.rating <= 2.0 && config.enableEscalation) {
                actions.add(feedbackActions["auto_escalation"]!!)
            }
            
            return ProcessingResult(
                success = true,
                actions = actions
            )
        }
        
        override fun getStatus(): ProcessorStatus {
            return ProcessorStatus(
                name = "PerformanceFeedbackProcessor",
                isEnabled = true,
                processedCount = processedCount,
                lastProcessingTime = lastProcessingTime
            )
        }
    }
    
    /**
     * 易用性反馈处理器
     */
    private class UsabilityFeedbackProcessor : FeedbackProcessor {
        private var processedCount = 0
        private var lastProcessingTime = 0L
        
        override fun canProcess(feedback: Feedback, config: FeedbackConfig): Boolean {
            return feedback.type == FeedbackType.USABILITY
        }
        
        override fun processFeedback(feedback: Feedback, config: FeedbackConfig): ProcessingResult {
            processedCount++
            lastProcessingTime = System.currentTimeMillis()
            
            val actions = mutableListOf<FeedbackAction>()
            
            // 易用性问题自动分类
            if (config.enableAutoActions) {
                actions.add(feedbackActions["auto_categorization"]!!)
            }
            
            return ProcessingResult(
                success = true,
                actions = actions
            )
        }
        
        override fun getStatus(): ProcessorStatus {
            return ProcessorStatus(
                name = "UsabilityFeedbackProcessor",
                isEnabled = true,
                processedCount = processedCount,
                lastProcessingTime = lastProcessingTime
            )
        }
    }
    
    /**
     * 通用反馈处理器
     */
    private class GeneralFeedbackProcessor : FeedbackProcessor {
        private var processedCount = 0
        private var lastProcessingTime = 0L
        
        override fun canProcess(feedback: Feedback, config: FeedbackConfig): Boolean {
            return feedback.type == FeedbackType.GENERAL
        }
        
        override fun processFeedback(feedback: Feedback, config: FeedbackConfig): ProcessingResult {
            processedCount++
            lastProcessingTime = System.currentTimeMillis()
            
            val actions = mutableListOf<FeedbackAction>()
            
            // 通用反馈自动分类
            if (config.enableAutoActions) {
                actions.add(feedbackActions["auto_categorization"]!!)
            }
            
            return ProcessingResult(
                success = true,
                actions = actions
            )
        }
        
        override fun getStatus(): ProcessorStatus {
            return ProcessorStatus(
                name = "GeneralFeedbackProcessor",
                isEnabled = true,
                processedCount = processedCount,
                lastProcessingTime = lastProcessingTime
            )
        }
    }
    
    /**
     * 自动升级动作
     */
    private class AutoEscalationAction : FeedbackAction {
        private var executionCount = 0
        private var lastExecutionTime = 0L
        
        override fun execute(): Boolean {
            executionCount++
            lastExecutionTime = System.currentTimeMillis()
            
            // 实际升级逻辑
            Log.i(TAG, "执行自动升级动作")
            
            return true
        }
        
        override fun getStatus(): ActionStatus {
            return ActionStatus(
                name = "AutoEscalationAction",
                isEnabled = true,
                executionCount = executionCount,
                lastExecutionTime = lastExecutionTime
            )
        }
    }
    
    /**
     * 自动回复动作
     */
    private class AutoResponseAction : FeedbackAction {
        private var executionCount = 0
        private var lastExecutionTime = 0L
        
        override fun execute(): Boolean {
            executionCount++
            lastExecutionTime = System.currentTimeMillis()
            
            // 实际回复逻辑
            Log.i(TAG, "执行自动回复动作")
            
            return true
        }
        
        override fun getStatus(): ActionStatus {
            return ActionStatus(
                name = "AutoResponseAction",
                isEnabled = true,
                executionCount = executionCount,
                lastExecutionTime = lastExecutionTime
            )
        }
    }
    
    /**
     * 自动分类动作
     */
    private class AutoCategorizationAction : FeedbackAction {
        private var executionCount = 0
        private var lastExecutionTime = 0L
        
        override fun execute(): Boolean {
            executionCount++
            lastExecutionTime = System.currentTimeMillis()
            
            // 实际分类逻辑
            Log.i(TAG, "执行自动分类动作")
            
            return true
        }
        
        override fun getStatus(): ActionStatus {
            return ActionStatus(
                name = "AutoCategorizationAction",
                isEnabled = true,
                executionCount = executionCount,
                lastExecutionTime = lastExecutionTime
            )
        }
    }
    
    /**
     * 自动优先级排序动作
     */
    private class AutoPrioritizationAction : FeedbackAction {
        private var executionCount = 0
        private var lastExecutionTime = 0L
        
        override fun execute(): Boolean {
            executionCount++
            lastExecutionTime = System.currentTimeMillis()
            
            // 实际优先级排序逻辑
            Log.i(TAG, "执行自动优先级排序动作")
            
            return true
        }
        
        override fun getStatus(): ActionStatus {
            return ActionStatus(
                name = "AutoPrioritizationAction",
                isEnabled = true,
                executionCount = executionCount,
                lastExecutionTime = lastExecutionTime
            )
        }
    }
    
    /**
     * 反馈分析器
     */
    class FeedbackAnalyzer {
        private val analysisResult = AtomicReference<AnalysisResult>(AnalysisResult())
        
        fun analyzeFeedback(feedbacks: List<Feedback>) {
            val result = AnalysisResult(
                totalFeedbacks = feedbacks.size,
                averageRating = if (feedbacks.isNotEmpty()) feedbacks.map { it.rating }.average() else 0.0,
                ratingDistribution = if (feedbacks.isNotEmpty()) {
                    feedbacks.groupingBy { it.ratingCategory }.mapValues { it.value.size }
                } else emptyMap(),
                typeDistribution = if (feedbacks.isNotEmpty()) {
                    feedbacks.groupingBy { it.type }.mapValues { it.value.size }
                } else emptyMap(),
                categoryDistribution = if (feedbacks.isNotEmpty()) {
                    feedbacks.groupingBy { it.category }.mapValues { it.value.size }
                } else emptyMap(),
                sourceDistribution = if (feedbacks.isNotEmpty()) {
                    feedbacks.groupingBy { it.source }.mapValues { it.value.size }
                } else emptyMap(),
                lastAnalysisTime = System.currentTimeMillis()
            )
            
            analysisResult.set(result)
        }
        
        fun getAnalysisResult(): AnalysisResult {
            return analysisResult.get()
        }
        
        data class AnalysisResult(
            val totalFeedbacks: Int = 0,
            val averageRating: Double = 0.0,
            val ratingDistribution: Map<RatingCategory, Int> = emptyMap(),
            val typeDistribution: Map<FeedbackType, Int> = emptyMap(),
            val categoryDistribution: Map<FeedbackCategory, Int> = emptyMap(),
            val sourceDistribution: Map<String, Int> = emptyMap(),
            val lastAnalysisTime: Long = 0L
        )
    }
}