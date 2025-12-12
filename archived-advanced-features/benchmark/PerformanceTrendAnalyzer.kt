package com.empathy.ai.data.benchmark

import com.empathy.ai.data.domain.model.AiResponseParserMetrics
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.*

/**
 * 性能趋势分析器
 * 分析性能数据趋势并预测未来性能
 */
class PerformanceTrendAnalyzer private constructor(
    private val metrics: AiResponseParserMetrics
) {
    companion object {
        @Volatile
        private var INSTANCE: PerformanceTrendAnalyzer? = null
        
        fun getInstance(metrics: AiResponseParserMetrics): PerformanceTrendAnalyzer {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PerformanceTrendAnalyzer(metrics).also { INSTANCE = it }
            }
        }
        
        private const val ANALYSIS_INTERVAL = 300000L // 5分钟
        private const val MIN_DATA_POINTS = 10
        private const val TREND_WINDOW_SIZE = 20
        private const val PREDICTION_HORIZON = 7 // 预测未来7个时间点
    }
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // 性能数据存储
    private val performanceData = ConcurrentHashMap<String, MutableList<PerformanceDataPoint>>()
    
    // 趋势分析结果
    private val trendAnalysisResults = ConcurrentHashMap<String, TrendAnalysisResult>()
    
    // 预测模型
    private val predictionModels = ConcurrentHashMap<String, PredictionModel>()
    
    // 分析状态
    private val _analysisState = MutableStateFlow(AnalysisState())
    val analysisState: StateFlow<AnalysisState> = _analysisState.asStateFlow()
    
    // 分析统计
    private val analysisStats = AtomicReference(AnalysisStatistics())
    
    // 分析任务
    private var analysisJob: Job? = null
    
    init {
        startAnalysisTask()
    }
    
    /**
     * 启动分析任务
     */
    private fun startAnalysisTask() {
        analysisJob = scope.launch {
            while (isActive) {
                try {
                    performTrendAnalysis()
                    updatePredictionModels()
                    delay(ANALYSIS_INTERVAL)
                } catch (e: Exception) {
                    metrics.recordPerformanceTrendError("趋势分析错误", e)
                }
            }
        }
    }
    
    /**
     * 添加性能数据点
     */
    fun addPerformanceDataPoint(
        metricId: String,
        value: Double,
        timestamp: Long = System.currentTimeMillis()
    ) {
        val dataPoint = PerformanceDataPoint(
            timestamp = timestamp,
            value = value,
            metricId = metricId
        )
        
        val dataPoints = performanceData.getOrPut(metricId) { mutableListOf() }
        dataPoints.add(dataPoint)
        
        // 限制数据点数量
        if (dataPoints.size > 1000) {
            dataPoints.removeAt(0)
        }
        
        metrics.recordPerformanceDataPointAdded(metricId, value)
    }
    
    /**
     * 批量添加性能数据点
     */
    fun addPerformanceDataPoints(metricId: String, dataPoints: List<PerformanceDataPoint>) {
        val existingDataPoints = performanceData.getOrPut(metricId) { mutableListOf() }
        existingDataPoints.addAll(dataPoints)
        
        // 限制数据点数量
        if (existingDataPoints.size > 1000) {
            val excess = existingDataPoints.size - 1000
            repeat(excess) {
                existingDataPoints.removeAt(0)
            }
        }
        
        metrics.recordPerformanceDataPointsAdded(metricId, dataPoints.size)
    }
    
    /**
     * 执行趋势分析
     */
    private fun performTrendAnalysis() {
        val currentTime = System.currentTimeMillis()
        
        for ((metricId, dataPoints) in performanceData) {
            if (dataPoints.size < MIN_DATA_POINTS) continue
            
            try {
                val result = analyzeTrend(metricId, dataPoints)
                trendAnalysisResults[metricId] = result
                
                metrics.recordTrendAnalysisCompleted(metricId, result.trendDirection.name)
            } catch (e: Exception) {
                metrics.recordPerformanceTrendError("指标趋势分析错误: $metricId", e)
            }
        }
        
        updateAnalysisState()
        updateAnalysisStatistics()
    }
    
    /**
     * 分析单个指标的趋势
     */
    private fun analyzeTrend(metricId: String, dataPoints: List<PerformanceDataPoint>): TrendAnalysisResult {
        val recentDataPoints = dataPoints.takeLast(TREND_WINDOW_SIZE)
        val values = recentDataPoints.map { it.value }
        val timestamps = recentDataPoints.map { it.timestamp }
        
        // 计算线性回归
        val regression = calculateLinearRegression(timestamps, values)
        
        // 计算趋势方向
        val trendDirection = determineTrendDirection(regression.slope)
        
        // 计算趋势强度
        val trendStrength = calculateTrendStrength(regression, values)
        
        // 计算季节性
        val seasonality = detectSeasonality(values)
        
        // 计算变化点
        val changePoints = detectChangePoints(values)
        
        // 计算异常值
        val outliers = detectOutliers(values)
        
        // 计算波动性
        val volatility = calculateVolatility(values)
        
        return TrendAnalysisResult(
            metricId = metricId,
            dataPoints = recentDataPoints.size,
            trendDirection = trendDirection,
            trendStrength = trendStrength,
            slope = regression.slope,
            intercept = regression.intercept,
            rSquared = regression.rSquared,
            seasonality = seasonality,
            changePoints = changePoints,
            outliers = outliers,
            volatility = volatility,
            lastUpdateTime = System.currentTimeMillis()
        )
    }
    
    /**
     * 计算线性回归
     */
    private fun calculateLinearRegression(x: List<Long>, y: List<Double>): LinearRegression {
        val n = x.size.toDouble()
        val sumX = x.sum().toDouble()
        val sumY = y.sum()
        val sumXY = x.zip(y).sumOf { (xi, yi) -> xi * yi }
        val sumX2 = x.sumOf { it * it }.toDouble()
        val sumY2 = y.sumOf { it * it }
        
        val slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX)
        val intercept = (sumY - slope * sumX) / n
        
        // 计算R²
        val meanY = sumY / n
        val totalSumSquares = y.sumOf { (it - meanY) * (it - meanY) }
        val residualSumSquares = x.zip(y).sumOf { (xi, yi) ->
            val predicted = slope * xi + intercept
            (yi - predicted) * (yi - predicted)
        }
        
        val rSquared = if (totalSumSquares > 0) 1 - (residualSumSquares / totalSumSquares) else 0.0
        
        return LinearRegression(slope, intercept, rSquared)
    }
    
    /**
     * 确定趋势方向
     */
    private fun determineTrendDirection(slope: Double): TrendDirection {
        return when {
            slope > 0.1 -> TrendDirection.INCREASING
            slope < -0.1 -> TrendDirection.DECREASING
            else -> TrendDirection.STABLE
        }
    }
    
    /**
     * 计算趋势强度
     */
    private fun calculateTrendStrength(regression: LinearRegression, values: List<Double>): TrendStrength {
        val rSquared = regression.rSquared
        
        return when {
            rSquared > 0.8 -> TrendStrength.STRONG
            rSquared > 0.5 -> TrendStrength.MODERATE
            rSquared > 0.2 -> TrendStrength.WEAK
            else -> TrendStrength.NONE
        }
    }
    
    /**
     * 检测季节性
     */
    private fun detectSeasonality(values: List<Double>): SeasonalityInfo {
        if (values.size < 20) return SeasonalityInfo(false, 0, 0.0)
        
        // 简化的季节性检测
        val autocorrelations = mutableListOf<Double>()
        
        for (lag in 1..minOf(values.size / 2, 20)) {
            val correlation = calculateAutocorrelation(values, lag)
            autocorrelations.add(correlation)
        }
        
        val maxCorrelation = autocorrelations.maxOrNull() ?: 0.0
        val bestLag = autocorrelations.indexOf(maxCorrelation) + 1
        
        val hasSeasonality = maxCorrelation > 0.5
        
        return SeasonalityInfo(hasSeasonality, bestLag, maxCorrelation)
    }
    
    /**
     * 计算自相关系数
     */
    private fun calculateAutocorrelation(values: List<Double>, lag: Int): Double {
        if (values.size <= lag) return 0.0
        
        val n = values.size - lag
        val mean = values.average()
        
        var numerator = 0.0
        var denominator = 0.0
        
        for (i in 0 until n) {
            val deviation1 = values[i] - mean
            val deviation2 = values[i + lag] - mean
            
            numerator += deviation1 * deviation2
            denominator += deviation1 * deviation1
        }
        
        return if (denominator > 0) numerator / denominator else 0.0
    }
    
    /**
     * 检测变化点
     */
    private fun detectChangePoints(values: List<Double>): List<ChangePoint> {
        if (values.size < 10) return emptyList()
        
        val changePoints = mutableListOf<ChangePoint>()
        val windowSize = maxOf(values.size / 10, 5)
        
        for (i in windowSize until values.size - windowSize) {
            val beforeWindow = values.subList(i - windowSize, i)
            val afterWindow = values.subList(i, i + windowSize)
            
            val beforeMean = beforeWindow.average()
            val afterMean = afterWindow.average()
            
            val changeMagnitude = abs(afterMean - beforeMean)
            val changeThreshold = beforeMean * 0.2 // 20%变化阈值
            
            if (changeMagnitude > changeThreshold) {
                changePoints.add(
                    ChangePoint(
                        index = i,
                        timestamp = 0L, // 需要从实际数据中获取
                        beforeValue = beforeMean,
                        afterValue = afterMean,
                        changeMagnitude = changeMagnitude
                    )
                )
            }
        }
        
        return changePoints
    }
    
    /**
     * 检测异常值
     */
    private fun detectOutliers(values: List<Double>): List<Outlier> {
        if (values.size < 5) return emptyList()
        
        val outliers = mutableListOf<Outlier>()
        val mean = values.average()
        val stdDev = calculateStandardDeviation(values)
        val threshold = 2.0 // 2个标准差
        
        for ((index, value) in values.withIndex()) {
            val zScore = abs((value - mean) / stdDev)
            
            if (zScore > threshold) {
                outliers.add(
                    Outlier(
                        index = index,
                        timestamp = 0L, // 需要从实际数据中获取
                        value = value,
                        zScore = zScore
                    )
                )
            }
        }
        
        return outliers
    }
    
    /**
     * 计算标准差
     */
    private fun calculateStandardDeviation(values: List<Double>): Double {
        val mean = values.average()
        val variance = values.map { (it - mean) * (it - mean) }.average()
        return sqrt(variance)
    }
    
    /**
     * 计算波动性
     */
    private fun calculateVolatility(values: List<Double>): Double {
        if (values.size < 2) return 0.0
        
        val returns = mutableListOf<Double>()
        
        for (i in 1 until values.size) {
            val returnValue = (values[i] - values[i - 1]) / values[i - 1]
            returns.add(returnValue)
        }
        
        return calculateStandardDeviation(returns)
    }
    
    /**
     * 更新预测模型
     */
    private fun updatePredictionModels() {
        for ((metricId, dataPoints) in performanceData) {
            if (dataPoints.size < MIN_DATA_POINTS) continue
            
            try {
                val model = createPredictionModel(metricId, dataPoints)
                predictionModels[metricId] = model
                
                metrics.recordPredictionModelUpdated(metricId)
            } catch (e: Exception) {
                metrics.recordPerformanceTrendError("预测模型更新错误: $metricId", e)
            }
        }
    }
    
    /**
     * 创建预测模型
     */
    private fun createPredictionModel(metricId: String, dataPoints: List<PerformanceDataPoint>): PredictionModel {
        val values = dataPoints.map { it.value }
        val timestamps = dataPoints.map { it.timestamp }
        
        // 使用移动平均和线性回归的组合预测
        val regression = calculateLinearRegression(timestamps, values)
        val movingAverage = calculateMovingAverage(values, 5)
        
        return SimplePredictionModel(
            metricId = metricId,
            regression = regression,
            movingAverage = movingAverage,
            lastTimestamp = timestamps.last(),
            lastValue = values.last(),
            createdAt = System.currentTimeMillis()
        )
    }
    
    /**
     * 计算移动平均
     */
    private fun calculateMovingAverage(values: List<Double>, windowSize: Int): Double {
        if (values.size < windowSize) return values.average()
        
        val recentValues = values.takeLast(windowSize)
        return recentValues.average()
    }
    
    /**
     * 预测未来性能
     */
    fun predictFuturePerformance(
        metricId: String,
        horizon: Int = PREDICTION_HORIZON
    ): List<PerformancePrediction> {
        val model = predictionModels[metricId]
            ?: return emptyList()
        
        return model.predict(horizon)
    }
    
    /**
     * 获取趋势分析结果
     */
    fun getTrendAnalysisResult(metricId: String): TrendAnalysisResult? {
        return trendAnalysisResults[metricId]
    }
    
    /**
     * 获取所有趋势分析结果
     */
    fun getAllTrendAnalysisResults(): Map<String, TrendAnalysisResult> {
        return trendAnalysisResults.toMap()
    }
    
    /**
     * 获取性能数据
     */
    fun getPerformanceData(metricId: String, limit: Int = 100): List<PerformanceDataPoint> {
        return performanceData[metricId]?.takeLast(limit) ?: emptyList()
    }
    
    /**
     * 获取所有性能数据
     */
    fun getAllPerformanceData(limit: Int = 100): Map<String, List<PerformanceDataPoint>> {
        val result = mutableMapOf<String, List<PerformanceDataPoint>>()
        
        for ((metricId, dataPoints) in performanceData) {
            result[metricId] = dataPoints.takeLast(limit)
        }
        
        return result
    }
    
    /**
     * 获取预测模型
     */
    fun getPredictionModel(metricId: String): PredictionModel? {
        return predictionModels[metricId]
    }
    
    /**
     * 获取分析统计信息
     */
    fun getAnalysisStatistics(): AnalysisStatistics {
        return analysisStats.get()
    }
    
    /**
     * 获取分析状态
     */
    fun getAnalysisState(): AnalysisState {
        return _analysisState.value
    }
    
    /**
     * 清除性能数据
     */
    fun clearPerformanceData(metricId: String? = null) {
        if (metricId != null) {
            performanceData.remove(metricId)
            trendAnalysisResults.remove(metricId)
            predictionModels.remove(metricId)
        } else {
            performanceData.clear()
            trendAnalysisResults.clear()
            predictionModels.clear()
        }
        
        metrics.recordPerformanceDataCleared(metricId ?: "all")
    }
    
    /**
     * 手动触发趋势分析
     */
    fun triggerTrendAnalysis() {
        scope.launch {
            performTrendAnalysis()
            updatePredictionModels()
        }
    }
    
    /**
     * 更新分析状态
     */
    private fun updateAnalysisState() {
        val currentState = _analysisState.value
        val newState = currentState.copy(
            analyzedMetrics = trendAnalysisResults.keys.toList(),
            totalDataPoints = performanceData.values.sumOf { it.size },
            lastUpdateTime = System.currentTimeMillis()
        )
        _analysisState.value = newState
    }
    
    /**
     * 更新分析统计信息
     */
    private fun updateAnalysisStatistics() {
        val currentStats = analysisStats.get()
        val newStats = currentStats.copy(
            totalAnalyses = currentStats.totalAnalyses + 1,
            lastUpdateTime = System.currentTimeMillis()
        )
        analysisStats.set(newStats)
    }
    
    /**
     * 销毁性能趋势分析器
     */
    fun destroy() {
        analysisJob?.cancel()
        scope.cancel()
        
        performanceData.clear()
        trendAnalysisResults.clear()
        predictionModels.clear()
        
        INSTANCE = null
    }
}

/**
 * 性能数据点
 */
data class PerformanceDataPoint(
    val timestamp: Long,
    val value: Double,
    val metricId: String
)

/**
 * 趋势分析结果
 */
data class TrendAnalysisResult(
    val metricId: String,
    val dataPoints: Int,
    val trendDirection: TrendDirection,
    val trendStrength: TrendStrength,
    val slope: Double,
    val intercept: Double,
    val rSquared: Double,
    val seasonality: SeasonalityInfo,
    val changePoints: List<ChangePoint>,
    val outliers: List<Outlier>,
    val volatility: Double,
    val lastUpdateTime: Long
)

/**
 * 线性回归结果
 */
data class LinearRegression(
    val slope: Double,
    val intercept: Double,
    val rSquared: Double
)

/**
 * 季节性信息
 */
data class SeasonalityInfo(
    val hasSeasonality: Boolean,
    val period: Int,
    val strength: Double
)

/**
 * 变化点
 */
data class ChangePoint(
    val index: Int,
    val timestamp: Long,
    val beforeValue: Double,
    val afterValue: Double,
    val changeMagnitude: Double
)

/**
 * 异常值
 */
data class Outlier(
    val index: Int,
    val timestamp: Long,
    val value: Double,
    val zScore: Double
)

/**
 * 预测模型接口
 */
interface PredictionModel {
    val metricId: String
    val createdAt: Long
    
    fun predict(horizon: Int): List<PerformancePrediction>
}

/**
 * 性能预测
 */
data class PerformancePrediction(
    val timestamp: Long,
    val predictedValue: Double,
    val confidenceInterval: ConfidenceInterval,
    val predictionType: PredictionType
)

/**
 * 置信区间
 */
data class ConfidenceInterval(
    val lowerBound: Double,
    val upperBound: Double,
    val confidenceLevel: Double
)

/**
 * 预测类型
 */
enum class PredictionType {
    POINT,
    INTERVAL,
    DISTRIBUTION
}

/**
 * 简单预测模型实现
 */
class SimplePredictionModel(
    override val metricId: String,
    private val regression: LinearRegression,
    private val movingAverage: Double,
    private val lastTimestamp: Long,
    private val lastValue: Double,
    override val createdAt: Long
) : PredictionModel {
    
    override fun predict(horizon: Int): List<PerformancePrediction> {
        val predictions = mutableListOf<PerformancePrediction>()
        val timeInterval = 60000L // 1分钟间隔
        
        for (i in 1..horizon) {
            val futureTimestamp = lastTimestamp + (i * timeInterval)
            val predictedValue = regression.slope * futureTimestamp + regression.intercept
            
            // 计算置信区间（简化实现）
            val margin = abs(predictedValue * 0.1) // 10%误差范围
            val confidenceInterval = ConfidenceInterval(
                lowerBound = predictedValue - margin,
                upperBound = predictedValue + margin,
                confidenceLevel = 0.95
            )
            
            predictions.add(
                PerformancePrediction(
                    timestamp = futureTimestamp,
                    predictedValue = predictedValue,
                    confidenceInterval = confidenceInterval,
                    predictionType = PredictionType.POINT
                )
            )
        }
        
        return predictions
    }
}

/**
 * 分析状态
 */
data class AnalysisState(
    val analyzedMetrics: List<String> = emptyList(),
    val totalDataPoints: Int = 0,
    val lastUpdateTime: Long = 0L
)

/**
 * 分析统计信息
 */
data class AnalysisStatistics(
    val totalAnalyses: Long = 0L,
    val totalPredictions: Long = 0L,
    val lastUpdateTime: Long = 0L
)

/**
 * 趋势方向
 */
enum class TrendDirection {
    INCREASING,
    DECREASING,
    STABLE
}

/**
 * 趋势强度
 */
enum class TrendStrength {
    NONE,
    WEAK,
    MODERATE,
    STRONG
}

/**
 * 性能趋势分析工厂
 */
object PerformanceTrendAnalyzerFactory {
    
    /**
     * 创建自定义趋势分析器
     */
    fun createCustomTrendAnalyzer(
        metrics: AiResponseParserMetrics,
        customAnalysis: (String, List<PerformanceDataPoint>) -> TrendAnalysisResult
    ): PerformanceTrendAnalyzer {
        // 这里可以创建带有自定义分析逻辑的分析器
        return PerformanceTrendAnalyzer.getInstance(metrics)
    }
    
    /**
     * 创建实时趋势分析器
     */
    fun createRealTimeAnalyzer(
        metrics: AiResponseParserMetrics,
        analysisInterval: Long = 60000L // 1分钟
    ): PerformanceTrendAnalyzer {
        // 这里可以创建具有自定义分析间隔的分析器
        return PerformanceTrendAnalyzer.getInstance(metrics)
    }
}