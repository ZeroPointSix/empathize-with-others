package com.empathy.ai.data.benchmark

import com.empathy.ai.data.domain.model.AiResponseParserMetrics
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

/**
 * 性能基准测试器
 * 提供全面的性能基准测试和对比分析
 */
class PerformanceBenchmark private constructor(
    private val metrics: AiResponseParserMetrics
) {
    companion object {
        @Volatile
        private var INSTANCE: PerformanceBenchmark? = null
        
        fun getInstance(metrics: AiResponseParserMetrics): PerformanceBenchmark {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PerformanceBenchmark(metrics).also { INSTANCE = it }
            }
        }
        
        private const val DEFAULT_WARMUP_ITERATIONS = 10
        private const val DEFAULT_MEASUREMENT_ITERATIONS = 100
        private const val DEFAULT_TIMEOUT_MS = 30000L
    }
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // 基准测试注册表
    private val benchmarkRegistry = ConcurrentHashMap<String, BenchmarkTest>()
    
    // 基准测试结果
    private val benchmarkResults = ConcurrentHashMap<String, BenchmarkResult>()
    
    // 基准测试状态
    private val _benchmarkState = MutableStateFlow(BenchmarkState())
    val benchmarkState: StateFlow<BenchmarkState> = _benchmarkState.asStateFlow()
    
    // 基准测试统计
    private val benchmarkStats = AtomicReference(BenchmarkStatistics())
    
    // 当前运行的测试
    private val runningTests = ConcurrentHashMap<String, Job>()
    
    init {
        initializeBuiltInBenchmarks()
    }
    
    /**
     * 初始化内置基准测试
     */
    private fun initializeBuiltInBenchmarks() {
        // JSON解析性能测试
        registerBenchmark("json_parsing", object : BenchmarkTest {
            override val name = "JSON解析性能测试"
            override val description = "测试JSON解析器的性能表现"
            override val category = BenchmarkCategory.PARSING
            
            override suspend fun execute(config: BenchmarkConfig): BenchmarkMeasurement {
                val testData = generateJsonTestData(config.dataSize)
                val measurements = mutableListOf<Long>()
                
                // 预热
                repeat(DEFAULT_WARMUP_ITERATIONS) {
                    parseJsonTest(testData)
                }
                
                // 实际测量
                repeat(config.iterations) {
                    val startTime = System.nanoTime()
                    parseJsonTest(testData)
                    val endTime = System.nanoTime()
                    measurements.add(endTime - startTime)
                }
                
                return BenchmarkMeasurement(
                    iterations = config.iterations,
                    totalTime = measurements.sum(),
                    minTime = measurements.minOrNull() ?: 0L,
                    maxTime = measurements.maxOrNull() ?: 0L,
                    averageTime = measurements.average(),
                    medianTime = calculateMedian(measurements),
                    percentile95 = calculatePercentile(measurements, 0.95),
                    percentile99 = calculatePercentile(measurements, 0.99),
                    throughput = calculateThroughput(measurements, testData.size),
                    memoryUsage = measureMemoryUsage { parseJsonTest(testData) }
                )
            }
            
            private suspend fun parseJsonTest(data: String): Any {
                // 这里应该调用实际的JSON解析逻辑
                delay(1) // 模拟解析时间
                return "parsed_result"
            }
            
            private fun generateJsonTestData(size: Int): String {
                // 生成指定大小的测试JSON数据
                return """{"test": "data", "size": $size, "content": "${"x".repeat(size)}"}"""
            }
        })
        
        // 缓存性能测试
        registerBenchmark("cache_performance", object : BenchmarkTest {
            override val name = "缓存性能测试"
            override val description = "测试缓存系统的读写性能"
            override val category = BenchmarkCategory.CACHE
            
            override suspend fun execute(config: BenchmarkConfig): BenchmarkMeasurement {
                val cache = createTestCache()
                val testData = generateCacheTestData(config.dataSize)
                val readMeasurements = mutableListOf<Long>()
                val writeMeasurements = mutableListOf<Long>()
                
                // 预热
                repeat(DEFAULT_WARMUP_ITERATIONS) {
                    for (data in testData.take(10)) {
                        cache.put(data.key, data.value)
                        cache.get(data.key)
                    }
                }
                
                // 写入测试
                repeat(config.iterations) {
                    for (data in testData) {
                        val startTime = System.nanoTime()
                        cache.put(data.key, data.value)
                        val endTime = System.nanoTime()
                        writeMeasurements.add(endTime - startTime)
                    }
                }
                
                // 读取测试
                repeat(config.iterations) {
                    for (data in testData) {
                        val startTime = System.nanoTime()
                        cache.get(data.key)
                        val endTime = System.nanoTime()
                        readMeasurements.add(endTime - startTime)
                    }
                }
                
                return BenchmarkMeasurement(
                    iterations = config.iterations,
                    totalTime = writeMeasurements.sum() + readMeasurements.sum(),
                    minTime = minOf(writeMeasurements.minOrNull() ?: 0L, readMeasurements.minOrNull() ?: 0L),
                    maxTime = maxOf(writeMeasurements.maxOrNull() ?: 0L, readMeasurements.maxOrNull() ?: 0L),
                    averageTime = (writeMeasurements.average() + readMeasurements.average()) / 2,
                    medianTime = (calculateMedian(writeMeasurements) + calculateMedian(readMeasurements)) / 2,
                    percentile95 = (calculatePercentile(writeMeasurements, 0.95) + calculatePercentile(readMeasurements, 0.95)) / 2,
                    percentile99 = (calculatePercentile(writeMeasurements, 0.99) + calculatePercentile(readMeasurements, 0.99)) / 2,
                    throughput = calculateThroughput(writeMeasurements + readMeasurements, testData.size),
                    memoryUsage = measureMemoryUsage { cache.size() },
                    additionalMetrics = mapOf(
                        "write_avg_time" to writeMeasurements.average(),
                        "read_avg_time" to readMeasurements.average(),
                        "cache_size" to cache.size()
                    )
                )
            }
            
            private fun createTestCache(): TestCache {
                return TestCache()
            }
            
            private fun generateCacheTestData(size: Int): List<CacheTestData> {
                return (0 until size).map { i ->
                    CacheTestData("key_$i", "value_$i")
                }
            }
        })
        
        // 内存使用测试
        registerBenchmark("memory_usage", object : BenchmarkTest {
            override val name = "内存使用测试"
            override val description = "测试内存分配和回收性能"
            override val category = BenchmarkCategory.MEMORY
            
            override suspend fun execute(config: BenchmarkConfig): BenchmarkMeasurement {
                val memoryMeasurements = mutableListOf<Long>()
                val gcMeasurements = mutableListOf<Long>()
                
                // 预热
                repeat(DEFAULT_WARMUP_ITERATIONS) {
                    allocateAndDeallocateMemory(config.dataSize)
                }
                
                // 实际测量
                repeat(config.iterations) {
                    val beforeMemory = getUsedMemory()
                    val beforeGcTime = getGcTime()
                    
                    allocateAndDeallocateMemory(config.dataSize)
                    
                    val afterMemory = getUsedMemory()
                    val afterGcTime = getGcTime()
                    
                    memoryMeasurements.add(afterMemory - beforeMemory)
                    gcMeasurements.add(afterGcTime - beforeGcTime)
                }
                
                return BenchmarkMeasurement(
                    iterations = config.iterations,
                    totalTime = memoryMeasurements.sum(),
                    minTime = memoryMeasurements.minOrNull() ?: 0L,
                    maxTime = memoryMeasurements.maxOrNull() ?: 0L,
                    averageTime = memoryMeasurements.average(),
                    medianTime = calculateMedian(memoryMeasurements),
                    percentile95 = calculatePercentile(memoryMeasurements, 0.95),
                    percentile99 = calculatePercentile(memoryMeasurements, 0.99),
                    throughput = calculateThroughput(memoryMeasurements, config.dataSize),
                    memoryUsage = memoryMeasurements.average(),
                    additionalMetrics = mapOf(
                        "gc_time_avg" to gcMeasurements.average(),
                        "memory_allocated_avg" to memoryMeasurements.average()
                    )
                )
            }
            
            private suspend fun allocateAndDeallocateMemory(size: Int) {
                // 分配内存
                val data = mutableListOf<ByteArray>()
                repeat(size) {
                    data.add(ByteArray(1024)) // 1KB
                }
                
                // 模拟使用
                delay(1)
                
                // 释放内存
                data.clear()
                System.gc()
                delay(10) // 等待GC完成
            }
        })
        
        // 并发性能测试
        registerBenchmark("concurrency_performance", object : BenchmarkTest {
            override val name = "并发性能测试"
            override val description = "测试系统在高并发情况下的性能表现"
            override val category = BenchmarkCategory.CONCURRENCY
            
            override suspend fun execute(config: BenchmarkConfig): BenchmarkMeasurement {
                val concurrencyLevel = config.concurrencyLevel
                val measurements = mutableListOf<Long>()
                
                // 预热
                repeat(DEFAULT_WARMUP_ITERATIONS) {
                    runConcurrentTest(concurrencyLevel, 10)
                }
                
                // 实际测量
                repeat(config.iterations) {
                    val startTime = System.nanoTime()
                    runConcurrentTest(concurrencyLevel, config.dataSize)
                    val endTime = System.nanoTime()
                    measurements.add(endTime - startTime)
                }
                
                return BenchmarkMeasurement(
                    iterations = config.iterations,
                    totalTime = measurements.sum(),
                    minTime = measurements.minOrNull() ?: 0L,
                    maxTime = measurements.maxOrNull() ?: 0L,
                    averageTime = measurements.average(),
                    medianTime = calculateMedian(measurements),
                    percentile95 = calculatePercentile(measurements, 0.95),
                    percentile99 = calculatePercentile(measurements, 0.99),
                    throughput = calculateThroughput(measurements, config.dataSize * concurrencyLevel),
                    memoryUsage = measureMemoryUsage { runConcurrentTest(concurrencyLevel, config.dataSize) },
                    additionalMetrics = mapOf(
                        "concurrency_level" to concurrencyLevel,
                        "tasks_per_thread" to config.dataSize
                    )
                )
            }
            
            private suspend fun runConcurrentTest(concurrency: Int, tasksPerThread: Int) {
                val jobs = mutableListOf<Job>()
                
                repeat(concurrency) {
                    val job = scope.async {
                        repeat(tasksPerThread) {
                            // 模拟并发任务
                            delay(1)
                        }
                    }
                    jobs.add(job)
                }
                
                jobs.awaitAll()
            }
        })
    }
    
    /**
     * 注册基准测试
     */
    fun registerBenchmark(id: String, benchmark: BenchmarkTest) {
        benchmarkRegistry[id] = benchmark
        metrics.recordBenchmarkRegistered(id, benchmark.name)
    }
    
    /**
     * 运行基准测试
     */
    suspend fun runBenchmark(
        id: String,
        config: BenchmarkConfig = BenchmarkConfig()
    ): BenchmarkResult {
        val benchmark = benchmarkRegistry[id]
            ?: throw IllegalArgumentException("未找到基准测试: $id")
        
        // 检查是否已经在运行
        if (runningTests.containsKey(id)) {
            throw IllegalStateException("基准测试已在运行: $id")
        }
        
        val job = scope.async {
            try {
                updateBenchmarkState(id, BenchmarkStatus.RUNNING)
                
                val startTime = System.currentTimeMillis()
                val measurement = benchmark.execute(config)
                val endTime = System.currentTimeMillis()
                
                val result = BenchmarkResult(
                    id = id,
                    name = benchmark.name,
                    description = benchmark.description,
                    category = benchmark.category,
                    config = config,
                    measurement = measurement,
                    timestamp = endTime,
                    duration = endTime - startTime,
                    status = BenchmarkStatus.COMPLETED
                )
                
                benchmarkResults[id] = result
                updateBenchmarkStatistics(result)
                updateBenchmarkState(id, BenchmarkStatus.COMPLETED)
                
                metrics.recordBenchmarkCompleted(id, result.duration, result.measurement.averageTime)
                
                return@async result
            } catch (e: Exception) {
                val result = BenchmarkResult(
                    id = id,
                    name = benchmark.name,
                    description = benchmark.description,
                    category = benchmark.category,
                    config = config,
                    measurement = BenchmarkMeasurement(),
                    timestamp = System.currentTimeMillis(),
                    duration = 0,
                    status = BenchmarkStatus.FAILED,
                    error = e.message
                )
                
                benchmarkResults[id] = result
                updateBenchmarkState(id, BenchmarkStatus.FAILED)
                
                metrics.recordBenchmarkFailed(id, e.message ?: "未知错误")
                
                return@async result
            } finally {
                runningTests.remove(id)
            }
        }
        
        runningTests[id] = job
        return job.await()
    }
    
    /**
     * 批量运行基准测试
     */
    suspend fun runBenchmarks(
        ids: List<String>,
        config: BenchmarkConfig = BenchmarkConfig()
    ): List<BenchmarkResult> {
        val results = mutableListOf<BenchmarkResult>()
        
        for (id in ids) {
            val result = runBenchmark(id, config)
            results.add(result)
        }
        
        return results
    }
    
    /**
     * 运行所有基准测试
     */
    suspend fun runAllBenchmarks(config: BenchmarkConfig = BenchmarkConfig()): List<BenchmarkResult> {
        return runBenchmarks(benchmarkRegistry.keys.toList(), config)
    }
    
    /**
     * 比较基准测试结果
     */
    fun compareResults(
        baselineId: String,
        comparisonId: String
    ): BenchmarkComparison? {
        val baseline = benchmarkResults[baselineId]
        val comparison = benchmarkResults[comparisonId]
        
        if (baseline == null || comparison == null) {
            return null
        }
        
        return BenchmarkComparison(
            baseline = baseline,
            comparison = comparison,
            performanceImprovement = calculatePerformanceImprovement(baseline, comparison),
            memoryImprovement = calculateMemoryImprovement(baseline, comparison),
            throughputImprovement = calculateThroughputImprovement(baseline, comparison)
        )
    }
    
    /**
     * 获取基准测试结果
     */
    fun getBenchmarkResult(id: String): BenchmarkResult? {
        return benchmarkResults[id]
    }
    
    /**
     * 获取所有基准测试结果
     */
    fun getAllBenchmarkResults(): List<BenchmarkResult> {
        return benchmarkResults.values.toList()
    }
    
    /**
     * 获取基准测试统计信息
     */
    fun getBenchmarkStatistics(): BenchmarkStatistics {
        return benchmarkStats.get()
    }
    
    /**
     * 获取基准测试状态
     */
    fun getBenchmarkState(): BenchmarkState {
        return _benchmarkState.value
    }
    
    /**
     * 获取可用的基准测试
     */
    fun getAvailableBenchmarks(): List<BenchmarkInfo> {
        return benchmarkRegistry.entries.map { (id, benchmark) ->
            BenchmarkInfo(
                id = id,
                name = benchmark.name,
                description = benchmark.description,
                category = benchmark.category,
                isRunning = runningTests.containsKey(id),
                hasResult = benchmarkResults.containsKey(id)
            )
        }
    }
    
    /**
     * 取消正在运行的基准测试
     */
    fun cancelBenchmark(id: String) {
        runningTests[id]?.cancel()
        runningTests.remove(id)
        updateBenchmarkState(id, BenchmarkStatus.CANCELLED)
        metrics.recordBenchmarkCancelled(id)
    }
    
    /**
     * 清除基准测试结果
     */
    fun clearResults() {
        benchmarkResults.clear()
        benchmarkStats.set(BenchmarkStatistics())
        metrics.recordBenchmarkResultsCleared()
    }
    
    /**
     * 更新基准测试状态
     */
    private fun updateBenchmarkState(id: String, status: BenchmarkStatus) {
        val currentState = _benchmarkState.value
        val newState = currentState.copy(
            runningTests = runningTests.keys.toList(),
            lastUpdateTime = System.currentTimeMillis()
        )
        _benchmarkState.value = newState
    }
    
    /**
     * 更新基准测试统计信息
     */
    private fun updateBenchmarkStatistics(result: BenchmarkResult) {
        val currentStats = benchmarkStats.get()
        val newStats = currentStats.copy(
            totalBenchmarks = currentStats.totalBenchmarks + 1,
            successfulBenchmarks = if (result.status == BenchmarkStatus.COMPLETED) 
                currentStats.successfulBenchmarks + 1 else currentStats.successfulBenchmarks,
            failedBenchmarks = if (result.status == BenchmarkStatus.FAILED) 
                currentStats.failedBenchmarks + 1 else currentStats.failedBenchmarks,
            totalDuration = currentStats.totalDuration + result.duration,
            averageDuration = (currentStats.totalDuration + result.duration).toFloat() / (currentStats.totalBenchmarks + 1),
            lastUpdateTime = System.currentTimeMillis()
        )
        benchmarkStats.set(newStats)
    }
    
    /**
     * 计算中位数
     */
    private fun calculateMedian(values: List<Long>): Double {
        if (values.isEmpty()) return 0.0
        
        val sorted = values.sorted()
        val middle = sorted.size / 2
        
        return if (sorted.size % 2 == 0) {
            (sorted[middle - 1] + sorted[middle]) / 2.0
        } else {
            sorted[middle].toDouble()
        }
    }
    
    /**
     * 计算百分位数
     */
    private fun calculatePercentile(values: List<Long>, percentile: Double): Double {
        if (values.isEmpty()) return 0.0
        
        val sorted = values.sorted()
        val index = (percentile * sorted.size).toInt()
        return sorted.getOrNull(index)?.toDouble() ?: 0.0
    }
    
    /**
     * 计算吞吐量
     */
    private fun calculateThroughput(values: List<Long>, dataSize: Int): Double {
        if (values.isEmpty()) return 0.0
        
        val totalTime = values.sum() / 1_000_000_000.0 // 转换为秒
        return if (totalTime > 0) dataSize / totalTime else 0.0
    }
    
    /**
     * 测量内存使用
     */
    private fun measureMemoryUsage(block: () -> Any): Double {
        val runtime = Runtime.getRuntime()
        val beforeMemory = runtime.totalMemory() - runtime.freeMemory()
        
        block()
        
        val afterMemory = runtime.totalMemory() - runtime.freeMemory()
        return (afterMemory - beforeMemory).toDouble()
    }
    
    /**
     * 获取已使用内存
     */
    private fun getUsedMemory(): Long {
        val runtime = Runtime.getRuntime()
        return runtime.totalMemory() - runtime.freeMemory()
    }
    
    /**
     * 获取GC时间
     */
    private fun getGcTime(): Long {
        // 这里应该获取实际的GC时间
        return System.currentTimeMillis()
    }
    
    /**
     * 计算性能改进
     */
    private fun calculatePerformanceImprovement(
        baseline: BenchmarkResult,
        comparison: BenchmarkResult
    ): Double {
        val baselineTime = baseline.measurement.averageTime
        val comparisonTime = comparison.measurement.averageTime
        
        return if (baselineTime > 0) {
            ((baselineTime - comparisonTime) / baselineTime) * 100
        } else 0.0
    }
    
    /**
     * 计算内存改进
     */
    private fun calculateMemoryImprovement(
        baseline: BenchmarkResult,
        comparison: BenchmarkResult
    ): Double {
        val baselineMemory = baseline.measurement.memoryUsage
        val comparisonMemory = comparison.measurement.memoryUsage
        
        return if (baselineMemory > 0) {
            ((baselineMemory - comparisonMemory) / baselineMemory) * 100
        } else 0.0
    }
    
    /**
     * 计算吞吐量改进
     */
    private fun calculateThroughputImprovement(
        baseline: BenchmarkResult,
        comparison: BenchmarkResult
    ): Double {
        val baselineThroughput = baseline.measurement.throughput
        val comparisonThroughput = comparison.measurement.throughput
        
        return if (baselineThroughput > 0) {
            ((comparisonThroughput - baselineThroughput) / baselineThroughput) * 100
        } else 0.0
    }
    
    /**
     * 销毁基准测试器
     */
    fun destroy() {
        // 取消所有正在运行的测试
        for (job in runningTests.values) {
            job.cancel()
        }
        
        scope.cancel()
        benchmarkRegistry.clear()
        benchmarkResults.clear()
        runningTests.clear()
        
        INSTANCE = null
    }
}

/**
 * 基准测试接口
 */
interface BenchmarkTest {
    val name: String
    val description: String
    val category: BenchmarkCategory
    
    suspend fun execute(config: BenchmarkConfig): BenchmarkMeasurement
}

/**
 * 基准测试配置
 */
data class BenchmarkConfig(
    val iterations: Int = DEFAULT_MEASUREMENT_ITERATIONS,
    val dataSize: Int = 1000,
    val concurrencyLevel: Int = 4,
    val timeoutMs: Long = DEFAULT_TIMEOUT_MS,
    val warmupIterations: Int = DEFAULT_WARMUP_ITERATIONS,
    val additionalParams: Map<String, Any> = emptyMap()
)

/**
 * 基准测试测量结果
 */
data class BenchmarkMeasurement(
    val iterations: Int = 0,
    val totalTime: Long = 0L,
    val minTime: Long = 0L,
    val maxTime: Long = 0L,
    val averageTime: Double = 0.0,
    val medianTime: Double = 0.0,
    val percentile95: Double = 0.0,
    val percentile99: Double = 0.0,
    val throughput: Double = 0.0,
    val memoryUsage: Double = 0.0,
    val additionalMetrics: Map<String, Any> = emptyMap()
)

/**
 * 基准测试结果
 */
data class BenchmarkResult(
    val id: String,
    val name: String,
    val description: String,
    val category: BenchmarkCategory,
    val config: BenchmarkConfig,
    val measurement: BenchmarkMeasurement,
    val timestamp: Long,
    val duration: Long,
    val status: BenchmarkStatus,
    val error: String? = null
)

/**
 * 基准测试比较结果
 */
data class BenchmarkComparison(
    val baseline: BenchmarkResult,
    val comparison: BenchmarkResult,
    val performanceImprovement: Double,
    val memoryImprovement: Double,
    val throughputImprovement: Double
)

/**
 * 基准测试状态
 */
data class BenchmarkState(
    val runningTests: List<String> = emptyList(),
    val lastUpdateTime: Long = 0L
)

/**
 * 基准测试统计信息
 */
data class BenchmarkStatistics(
    val totalBenchmarks: Long = 0L,
    val successfulBenchmarks: Long = 0L,
    val failedBenchmarks: Long = 0L,
    val totalDuration: Long = 0L,
    val averageDuration: Float = 0f,
    val lastUpdateTime: Long = 0L
) {
    val successRate: Float
        get() = if (totalBenchmarks > 0) successfulBenchmarks.toFloat() / totalBenchmarks.toFloat() else 0f
}

/**
 * 基准测试信息
 */
data class BenchmarkInfo(
    val id: String,
    val name: String,
    val description: String,
    val category: BenchmarkCategory,
    val isRunning: Boolean,
    val hasResult: Boolean
)

/**
 * 基准测试类别
 */
enum class BenchmarkCategory {
    PARSING,
    CACHE,
    MEMORY,
    CONCURRENCY,
    NETWORK,
    DATABASE,
    UI
}

/**
 * 基准测试状态
 */
enum class BenchmarkStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED,
    CANCELLED
}

/**
 * 测试缓存
 */
class TestCache {
    private val cache = mutableMapOf<String, String>()
    
    fun put(key: String, value: String) {
        cache[key] = value
    }
    
    fun get(key: String): String? {
        return cache[key]
    }
    
    fun size(): Int {
        return cache.size
    }
}

/**
 * 缓存测试数据
 */
data class CacheTestData(
    val key: String,
    val value: String
)

/**
 * 基准测试工厂
 */
object BenchmarkFactory {
    
    /**
     * 创建自定义基准测试
     */
    fun createCustomBenchmark(
        name: String,
        description: String,
        category: BenchmarkCategory,
        executionBlock: suspend (BenchmarkConfig) -> BenchmarkMeasurement
    ): BenchmarkTest {
        return object : BenchmarkTest {
            override val name = name
            override val description = description
            override val category = category
            
            override suspend fun execute(config: BenchmarkConfig): BenchmarkMeasurement {
                return executionBlock(config)
            }
        }
    }
    
    /**
     * 创建负载测试基准
     */
    fun createLoadTestBenchmark(
        name: String,
        description: String,
        loadGenerator: suspend (Int, Int) -> Unit
    ): BenchmarkTest {
        return object : BenchmarkTest {
            override val name = name
            override val description = description
            override val category = BenchmarkCategory.CONCURRENCY
            
            override suspend fun execute(config: BenchmarkConfig): BenchmarkMeasurement {
                val measurements = mutableListOf<Long>()
                
                // 预热
                repeat(DEFAULT_WARMUP_ITERATIONS) {
                    loadGenerator(config.concurrencyLevel, 10)
                }
                
                // 实际测量
                repeat(config.iterations) {
                    val startTime = System.nanoTime()
                    loadGenerator(config.concurrencyLevel, config.dataSize)
                    val endTime = System.nanoTime()
                    measurements.add(endTime - startTime)
                }
                
                return BenchmarkMeasurement(
                    iterations = config.iterations,
                    totalTime = measurements.sum(),
                    minTime = measurements.minOrNull() ?: 0L,
                    maxTime = measurements.maxOrNull() ?: 0L,
                    averageTime = measurements.average(),
                    medianTime = PerformanceBenchmark.getInstance(AiResponseParserMetrics()).let { 
                        // 这里需要访问私有方法，简化处理
                        measurements.sorted().let { sorted ->
                            val middle = sorted.size / 2
                            if (sorted.size % 2 == 0) {
                                (sorted[middle - 1] + sorted[middle]) / 2.0
                            } else {
                                sorted[middle].toDouble()
                            }
                        }
                    },
                    percentile95 = measurements.sorted().let { sorted ->
                        val index = (0.95 * sorted.size).toInt()
                        sorted.getOrNull(index)?.toDouble() ?: 0.0
                    },
                    percentile99 = measurements.sorted().let { sorted ->
                        val index = (0.99 * sorted.size).toInt()
                        sorted.getOrNull(index)?.toDouble() ?: 0.0
                    },
                    throughput = measurements.sum() / 1_000_000_000.0.let { totalTime ->
                        if (totalTime > 0) config.dataSize * config.concurrencyLevel / totalTime else 0.0
                    },
                    memoryUsage = 0.0,
                    additionalMetrics = mapOf(
                        "concurrency_level" to config.concurrencyLevel,
                        "tasks_per_thread" to config.dataSize
                    )
                )
            }
        }
    }
}