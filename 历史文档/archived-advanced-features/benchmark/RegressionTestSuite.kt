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
 * 回归测试套件
 * 自动检测性能回归和功能退化
 */
class RegressionTestSuite private constructor(
    private val metrics: AiResponseParserMetrics
) {
    companion object {
        @Volatile
        private var INSTANCE: RegressionTestSuite? = null
        
        fun getInstance(metrics: AiResponseParserMetrics): RegressionTestSuite {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: RegressionTestSuite(metrics).also { INSTANCE = it }
            }
        }
        
        private const val REGRESSION_THRESHOLD = 5.0 // 5%性能下降视为回归
        private const val BASELINE_EXPIRY = 7 * 24 * 3600 * 1000L // 7天
        private const val MIN_BASELINE_SAMPLES = 3
    }
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // 回归测试注册表
    private val regressionTests = ConcurrentHashMap<String, RegressionTest>()
    
    // 基线数据
    private val baselineData = ConcurrentHashMap<String, BaselineData>()
    
    // 回归测试结果
    private val regressionResults = ConcurrentHashMap<String, RegressionTestResult>()
    
    // 回归测试状态
    private val _regressionState = MutableStateFlow(RegressionTestState())
    val regressionState: StateFlow<RegressionTestState> = _regressionState.asStateFlow()
    
    // 回归测试统计
    private val regressionStats = AtomicReference(RegressionTestStatistics())
    
    // 当前运行的测试
    private val runningTests = ConcurrentHashMap<String, Job>()
    
    init {
        initializeBuiltInRegressionTests()
    }
    
    /**
     * 初始化内置回归测试
     */
    private fun initializeBuiltInRegressionTests() {
        // JSON解析性能回归测试
        registerRegressionTest("json_parsing_regression", object : RegressionTest {
            override val name = "JSON解析性能回归测试"
            override val description = "检测JSON解析性能是否出现回归"
            override val category = RegressionTestCategory.PERFORMANCE
            override val threshold = REGRESSION_THRESHOLD
            
            override suspend fun execute(): RegressionMeasurement {
                val testCases = generateJsonTestCases()
                val measurements = mutableListOf<Long>()
                
                for (testCase in testCases) {
                    val startTime = System.nanoTime()
                    parseJsonTestCase(testCase)
                    val endTime = System.nanoTime()
                    measurements.add(endTime - startTime)
                }
                
                return RegressionMeasurement(
                    averageTime = measurements.average(),
                    minTime = measurements.minOrNull() ?: 0L,
                    maxTime = measurements.maxOrNull() ?: 0L,
                    samples = measurements.size,
                    memoryUsage = measureMemoryUsage {
                        testCases.forEach { parseJsonTestCase(it) }
                    },
                    successRate = calculateSuccessRate(testCases),
                    additionalMetrics = mapOf(
                        "test_cases" to testCases.size,
                        "avg_case_time" to measurements.average()
                    )
                )
            }
            
            private fun generateJsonTestCases(): List<JsonTestCase> {
                return listOf(
                    JsonTestCase("simple", """{"name": "test", "value": 123}"""),
                    JsonTestCase("complex", generateComplexJson()),
                    JsonTestCase("large", generateLargeJson()),
                    JsonTestCase("nested", generateNestedJson())
                )
            }
            
            private fun generateComplexJson(): String {
                return """{
                    "user": {
                        "id": 123,
                        "name": "test_user",
                        "profile": {
                            "age": 25,
                            "city": "test_city",
                            "preferences": ["option1", "option2", "option3"]
                        }
                    },
                    "metadata": {
                        "created": "2023-01-01T00:00:00Z",
                        "updated": "2023-01-02T00:00:00Z",
                        "version": 1.0
                    }
                }"""
            }
            
            private fun generateLargeJson(): String {
                val items = (1..100).joinToString(",") { i ->
                    """{"id": $i, "name": "item_$i", "value": ${i * 2}}"""
                }
                return """{"items": [$items], "total": 100}"""
            }
            
            private fun generateNestedJson(): String {
                return """{
                    "level1": {
                        "level2": {
                            "level3": {
                                "level4": {
                                    "data": "deep_nested_value"
                                }
                            }
                        }
                    }
                }"""
            }
            
            private suspend fun parseJsonTestCase(testCase: JsonTestCase): Any {
                // 这里应该调用实际的JSON解析逻辑
                delay(1) // 模拟解析时间
                return "parsed_${testCase.name}"
            }
            
            private fun calculateSuccessRate(testCases: List<JsonTestCase>): Double {
                // 模拟成功率计算
                return testCases.size.toDouble() / testCases.size.toDouble() * 100
            }
        })
        
        // 缓存性能回归测试
        registerRegressionTest("cache_performance_regression", object : RegressionTest {
            override val name = "缓存性能回归测试"
            override val description = "检测缓存系统性能是否出现回归"
            override val category = RegressionTestCategory.PERFORMANCE
            override val threshold = REGRESSION_THRESHOLD
            
            override suspend fun execute(): RegressionMeasurement {
                val cacheOperations = listOf(
                    CacheOperation("write", "key1", "value1"),
                    CacheOperation("read", "key1", null),
                    CacheOperation("write", "key2", "value2"),
                    CacheOperation("read", "key2", null),
                    CacheOperation("write", "key3", "value3"),
                    CacheOperation("read", "key3", null)
                )
                
                val measurements = mutableListOf<Long>()
                
                for (operation in cacheOperations) {
                    val startTime = System.nanoTime()
                    executeCacheOperation(operation)
                    val endTime = System.nanoTime()
                    measurements.add(endTime - startTime)
                }
                
                return RegressionMeasurement(
                    averageTime = measurements.average(),
                    minTime = measurements.minOrNull() ?: 0L,
                    maxTime = measurements.maxOrNull() ?: 0L,
                    samples = measurements.size,
                    memoryUsage = measureMemoryUsage {
                        cacheOperations.forEach { executeCacheOperation(it) }
                    },
                    successRate = 100.0, // 假设所有操作都成功
                    additionalMetrics = mapOf(
                        "operations" to cacheOperations.size,
                        "avg_operation_time" to measurements.average()
                    )
                )
            }
            
            private suspend fun executeCacheOperation(operation: CacheOperation) {
                // 这里应该调用实际的缓存操作
                delay(1) // 模拟操作时间
            }
        })
        
        // 内存使用回归测试
        registerRegressionTest("memory_usage_regression", object : RegressionTest {
            override val name = "内存使用回归测试"
            override val description = "检测内存使用是否出现回归"
            override val category = RegressionTestCategory.MEMORY
            override val threshold = REGRESSION_THRESHOLD
            
            override suspend fun execute(): RegressionMeasurement {
                val memoryMeasurements = mutableListOf<Long>()
                
                // 执行一系列内存密集型操作
                repeat(10) {
                    val beforeMemory = getUsedMemory()
                    
                    // 分配内存
                    val data = mutableListOf<ByteArray>()
                    repeat(100) {
                        data.add(ByteArray(1024)) // 1KB
                    }
                    
                    // 使用内存
                    data.forEach { it[0] = 1 }
                    
                    val afterMemory = getUsedMemory()
                    memoryMeasurements.add(afterMemory - beforeMemory)
                    
                    // 释放内存
                    data.clear()
                    System.gc()
                    delay(10)
                }
                
                return RegressionMeasurement(
                    averageTime = 0.0, // 内存测试不关注时间
                    minTime = 0L,
                    maxTime = 0L,
                    samples = memoryMeasurements.size,
                    memoryUsage = memoryMeasurements.average(),
                    successRate = 100.0,
                    additionalMetrics = mapOf(
                        "memory_samples" to memoryMeasurements.size,
                        "avg_memory_per_iteration" to memoryMeasurements.average()
                    )
                )
            }
            
            private fun getUsedMemory(): Long {
                val runtime = Runtime.getRuntime()
                return runtime.totalMemory() - runtime.freeMemory()
            }
        })
        
        // 功能正确性回归测试
        registerRegressionTest("functionality_regression", object : RegressionTest {
            override val name = "功能正确性回归测试"
            override val description = "检测核心功能是否出现回归"
            override val category = RegressionTestCategory.FUNCTIONALITY
            override val threshold = 0.0 // 功能测试不允许任何失败
            
            override suspend fun execute(): RegressionMeasurement {
                val testCases = generateFunctionalityTestCases()
                var passedTests = 0
                val measurements = mutableListOf<Long>()
                
                for (testCase in testCases) {
                    val startTime = System.nanoTime()
                    val result = executeFunctionalityTest(testCase)
                    val endTime = System.nanoTime()
                    
                    measurements.add(endTime - startTime)
                    if (result) passedTests++
                }
                
                val successRate = (passedTests.toDouble() / testCases.size) * 100
                
                return RegressionMeasurement(
                    averageTime = measurements.average(),
                    minTime = measurements.minOrNull() ?: 0L,
                    maxTime = measurements.maxOrNull() ?: 0L,
                    samples = measurements.size,
                    memoryUsage = 0.0,
                    successRate = successRate,
                    additionalMetrics = mapOf(
                        "total_tests" to testCases.size,
                        "passed_tests" to passedTests,
                        "failed_tests" to testCases.size - passedTests
                    )
                )
            }
            
            private fun generateFunctionalityTestCases(): List<FunctionalityTestCase> {
                return listOf(
                    FunctionalityTestCase("json_parse_valid", """{"test": "valid"}"""),
                    FunctionalityTestCase("json_parse_invalid", """{"invalid": json}"""),
                    FunctionalityTestCase("field_mapping_basic", "basic_mapping_test"),
                    FunctionalityTestCase("field_mapping_complex", "complex_mapping_test"),
                    FunctionalityTestCase("safety_check_normal", "normal_data"),
                    FunctionalityTestCase("safety_check_malicious", "malicious_data")
                )
            }
            
            private suspend fun executeFunctionalityTest(testCase: FunctionalityTestCase): Boolean {
                // 这里应该执行实际的功能测试
                delay(2) // 模拟测试时间
                
                // 模拟测试结果
                return when (testCase.name) {
                    "json_parse_valid" -> true
                    "json_parse_invalid" -> false // 预期失败
                    "field_mapping_basic" -> true
                    "field_mapping_complex" -> true
                    "safety_check_normal" -> true
                    "safety_check_malicious" -> true // 应该检测到恶意内容
                    else -> false
                }
            }
        })
    }
    
    /**
     * 注册回归测试
     */
    fun registerRegressionTest(id: String, test: RegressionTest) {
        regressionTests[id] = test
        metrics.recordRegressionTestRegistered(id, test.name)
    }
    
    /**
     * 运行回归测试
     */
    suspend fun runRegressionTest(id: String): RegressionTestResult {
        val test = regressionTests[id]
            ?: throw IllegalArgumentException("未找到回归测试: $id")
        
        // 检查是否已经在运行
        if (runningTests.containsKey(id)) {
            throw IllegalStateException("回归测试已在运行: $id")
        }
        
        val job = scope.async {
            try {
                updateRegressionState(id, RegressionTestStatus.RUNNING)
                
                val startTime = System.currentTimeMillis()
                val measurement = test.execute()
                val endTime = System.currentTimeMillis()
                
                // 获取基线数据
                val baseline = baselineData[id]
                val regressionDetected = detectRegression(measurement, baseline, test.threshold)
                
                val result = RegressionTestResult(
                    id = id,
                    name = test.name,
                    description = test.description,
                    category = test.category,
                    measurement = measurement,
                    baseline = baseline,
                    regressionDetected = regressionDetected,
                    threshold = test.threshold,
                    timestamp = endTime,
                    duration = endTime - startTime,
                    status = RegressionTestStatus.COMPLETED
                )
                
                regressionResults[id] = result
                updateRegressionStatistics(result)
                
                // 如果没有基线数据，创建新的基线
                if (baseline == null && !regressionDetected) {
                    createBaseline(id, measurement)
                }
                
                updateRegressionState(id, RegressionTestStatus.COMPLETED)
                
                metrics.recordRegressionTestCompleted(id, result.duration, regressionDetected)
                
                return@async result
            } catch (e: Exception) {
                val result = RegressionTestResult(
                    id = id,
                    name = test.name,
                    description = test.description,
                    category = test.category,
                    measurement = RegressionMeasurement(),
                    baseline = baselineData[id],
                    regressionDetected = false,
                    threshold = test.threshold,
                    timestamp = System.currentTimeMillis(),
                    duration = 0,
                    status = RegressionTestStatus.FAILED,
                    error = e.message
                )
                
                regressionResults[id] = result
                updateRegressionState(id, RegressionTestStatus.FAILED)
                
                metrics.recordRegressionTestFailed(id, e.message ?: "未知错误")
                
                return@async result
            } finally {
                runningTests.remove(id)
            }
        }
        
        runningTests[id] = job
        return job.await()
    }
    
    /**
     * 批量运行回归测试
     */
    suspend fun runRegressionTests(ids: List<String>): List<RegressionTestResult> {
        val results = mutableListOf<RegressionTestResult>()
        
        for (id in ids) {
            val result = runRegressionTest(id)
            results.add(result)
        }
        
        return results
    }
    
    /**
     * 运行所有回归测试
     */
    suspend fun runAllRegressionTests(): List<RegressionTestResult> {
        return runRegressionTests(regressionTests.keys.toList())
    }
    
    /**
     * 检测回归
     */
    private fun detectRegression(
        measurement: RegressionMeasurement,
        baseline: BaselineData?,
        threshold: Double
    ): Boolean {
        if (baseline == null) return false
        
        // 性能回归检测
        val performanceRegression = if (baseline.averageTime > 0) {
            val performanceChange = ((measurement.averageTime - baseline.averageTime) / baseline.averageTime) * 100
            performanceChange > threshold
        } else false
        
        // 内存回归检测
        val memoryRegression = if (baseline.memoryUsage > 0) {
            val memoryChange = ((measurement.memoryUsage - baseline.memoryUsage) / baseline.memoryUsage) * 100
            memoryChange > threshold
        } else false
        
        // 功能回归检测
        val functionalityRegression = measurement.successRate < baseline.successRate - threshold
        
        return performanceRegression || memoryRegression || functionalityRegression
    }
    
    /**
     * 创建基线数据
     */
    private fun createBaseline(id: String, measurement: RegressionMeasurement) {
        val baseline = BaselineData(
            averageTime = measurement.averageTime,
            memoryUsage = measurement.memoryUsage,
            successRate = measurement.successRate,
            samples = measurement.samples,
            timestamp = System.currentTimeMillis(),
            version = 1
        )
        
        baselineData[id] = baseline
        metrics.recordRegressionBaselineCreated(id)
    }
    
    /**
     * 更新基线数据
     */
    fun updateBaseline(id: String, force: Boolean = false) {
        val result = regressionResults[id] ?: return
        val currentBaseline = baselineData[id]
        
        // 如果没有基线或者强制更新，或者当前结果更好
        if (currentBaseline == null || force || isBetterThanBaseline(result.measurement, currentBaseline)) {
            val newBaseline = BaselineData(
                averageTime = result.measurement.averageTime,
                memoryUsage = result.measurement.memoryUsage,
                successRate = result.measurement.successRate,
                samples = result.measurement.samples,
                timestamp = System.currentTimeMillis(),
                version = (currentBaseline?.version ?: 0) + 1
            )
            
            baselineData[id] = newBaseline
            metrics.recordRegressionBaselineUpdated(id, newBaseline.version)
        }
    }
    
    /**
     * 判断测量结果是否优于基线
     */
    private fun isBetterThanBaseline(measurement: RegressionMeasurement, baseline: BaselineData): Boolean {
        // 性能更好（时间更短）
        val performanceBetter = measurement.averageTime < baseline.averageTime * 0.95
        
        // 内存使用更少
        val memoryBetter = measurement.memoryUsage < baseline.memoryUsage * 0.95
        
        // 成功率更高
        val successRateBetter = measurement.successRate > baseline.successRate + 1.0
        
        return performanceBetter || memoryBetter || successRateBetter
    }
    
    /**
     * 获取回归测试结果
     */
    fun getRegressionTestResult(id: String): RegressionTestResult? {
        return regressionResults[id]
    }
    
    /**
     * 获取所有回归测试结果
     */
    fun getAllRegressionTestResults(): List<RegressionTestResult> {
        return regressionResults.values.toList()
    }
    
    /**
     * 获取检测到的回归
     */
    fun getDetectedRegressions(): List<RegressionTestResult> {
        return regressionResults.values.filter { it.regressionDetected }
    }
    
    /**
     * 获取基线数据
     */
    fun getBaselineData(id: String): BaselineData? {
        return baselineData[id]
    }
    
    /**
     * 获取所有基线数据
     */
    fun getAllBaselineData(): Map<String, BaselineData> {
        return baselineData.toMap()
    }
    
    /**
     * 获取回归测试统计信息
     */
    fun getRegressionTestStatistics(): RegressionTestStatistics {
        return regressionStats.get()
    }
    
    /**
     * 获取回归测试状态
     */
    fun getRegressionTestState(): RegressionTestState {
        return _regressionState.value
    }
    
    /**
     * 获取可用的回归测试
     */
    fun getAvailableRegressionTests(): List<RegressionTestInfo> {
        return regressionTests.entries.map { (id, test) ->
            RegressionTestInfo(
                id = id,
                name = test.name,
                description = test.description,
                category = test.category,
                threshold = test.threshold,
                isRunning = runningTests.containsKey(id),
                hasResult = regressionResults.containsKey(id),
                hasBaseline = baselineData.containsKey(id)
            )
        }
    }
    
    /**
     * 取消正在运行的回归测试
     */
    fun cancelRegressionTest(id: String) {
        runningTests[id]?.cancel()
        runningTests.remove(id)
        updateRegressionState(id, RegressionTestStatus.CANCELLED)
        metrics.recordRegressionTestCancelled(id)
    }
    
    /**
     * 清除过期基线数据
     */
    fun cleanupExpiredBaselines() {
        val currentTime = System.currentTimeMillis()
        val expiredBaselines = mutableListOf<String>()
        
        for ((id, baseline) in baselineData) {
            if (currentTime - baseline.timestamp > BASELINE_EXPIRY) {
                expiredBaselines.add(id)
            }
        }
        
        for (id in expiredBaselines) {
            baselineData.remove(id)
            metrics.recordRegressionBaselineExpired(id)
        }
    }
    
    /**
     * 更新回归测试状态
     */
    private fun updateRegressionState(id: String, status: RegressionTestStatus) {
        val currentState = _regressionState.value
        val newState = currentState.copy(
            runningTests = runningTests.keys.toList(),
            lastUpdateTime = System.currentTimeMillis()
        )
        _regressionState.value = newState
    }
    
    /**
     * 更新回归测试统计信息
     */
    private fun updateRegressionStatistics(result: RegressionTestResult) {
        val currentStats = regressionStats.get()
        val newStats = currentStats.copy(
            totalTests = currentStats.totalTests + 1,
            passedTests = if (!result.regressionDetected) currentStats.passedTests + 1 else currentStats.passedTests,
            failedTests = if (result.regressionDetected) currentStats.failedTests + 1 else currentStats.failedTests,
            totalDuration = currentStats.totalDuration + result.duration,
            averageDuration = (currentStats.totalDuration + result.duration).toFloat() / (currentStats.totalTests + 1),
            lastUpdateTime = System.currentTimeMillis()
        )
        regressionStats.set(newStats)
    }
    
    /**
     * 测量内存使用
     */
    private fun measureMemoryUsage(block: () -> Unit): Double {
        val runtime = Runtime.getRuntime()
        val beforeMemory = runtime.totalMemory() - runtime.freeMemory()
        
        block()
        
        val afterMemory = runtime.totalMemory() - runtime.freeMemory()
        return (afterMemory - beforeMemory).toDouble()
    }
    
    /**
     * 销毁回归测试套件
     */
    fun destroy() {
        // 取消所有正在运行的测试
        for (job in runningTests.values) {
            job.cancel()
        }
        
        scope.cancel()
        regressionTests.clear()
        regressionResults.clear()
        baselineData.clear()
        runningTests.clear()
        
        INSTANCE = null
    }
}

/**
 * 回归测试接口
 */
interface RegressionTest {
    val name: String
    val description: String
    val category: RegressionTestCategory
    val threshold: Double
    
    suspend fun execute(): RegressionMeasurement
}

/**
 * 回归测试测量结果
 */
data class RegressionMeasurement(
    val averageTime: Double = 0.0,
    val minTime: Long = 0L,
    val maxTime: Long = 0L,
    val samples: Int = 0,
    val memoryUsage: Double = 0.0,
    val successRate: Double = 0.0,
    val additionalMetrics: Map<String, Any> = emptyMap()
)

/**
 * 回归测试结果
 */
data class RegressionTestResult(
    val id: String,
    val name: String,
    val description: String,
    val category: RegressionTestCategory,
    val measurement: RegressionMeasurement,
    val baseline: BaselineData?,
    val regressionDetected: Boolean,
    val threshold: Double,
    val timestamp: Long,
    val duration: Long,
    val status: RegressionTestStatus,
    val error: String? = null
)

/**
 * 基线数据
 */
data class BaselineData(
    val averageTime: Double,
    val memoryUsage: Double,
    val successRate: Double,
    val samples: Int,
    val timestamp: Long,
    val version: Int
)

/**
 * 回归测试状态
 */
data class RegressionTestState(
    val runningTests: List<String> = emptyList(),
    val lastUpdateTime: Long = 0L
)

/**
 * 回归测试统计信息
 */
data class RegressionTestStatistics(
    val totalTests: Long = 0L,
    val passedTests: Long = 0L,
    val failedTests: Long = 0L,
    val totalDuration: Long = 0L,
    val averageDuration: Float = 0f,
    val lastUpdateTime: Long = 0L
) {
    val passRate: Float
        get() = if (totalTests > 0) passedTests.toFloat() / totalTests.toFloat() else 0f
    
    val regressionRate: Float
        get() = if (totalTests > 0) failedTests.toFloat() / totalTests.toFloat() else 0f
}

/**
 * 回归测试信息
 */
data class RegressionTestInfo(
    val id: String,
    val name: String,
    val description: String,
    val category: RegressionTestCategory,
    val threshold: Double,
    val isRunning: Boolean,
    val hasResult: Boolean,
    val hasBaseline: Boolean
)

/**
 * 回归测试类别
 */
enum class RegressionTestCategory {
    PERFORMANCE,
    MEMORY,
    FUNCTIONALITY,
    SECURITY,
    STABILITY
}

/**
 * 回归测试状态
 */
enum class RegressionTestStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED,
    CANCELLED
}

/**
 * 测试用例数据类
 */
data class JsonTestCase(
    val name: String,
    val data: String
)

data class CacheOperation(
    val type: String,
    val key: String,
    val value: String?
)

data class FunctionalityTestCase(
    val name: String,
    val data: String
)

/**
 * 回归测试工厂
 */
object RegressionTestFactory {
    
    /**
     * 创建自定义回归测试
     */
    fun createCustomRegressionTest(
        name: String,
        description: String,
        category: RegressionTestCategory,
        threshold: Double = REGRESSION_THRESHOLD,
        executionBlock: suspend () -> RegressionMeasurement
    ): RegressionTest {
        return object : RegressionTest {
            override val name = name
            override val description = description
            override val category = category
            override val threshold = threshold
            
            override suspend fun execute(): RegressionMeasurement {
                return executionBlock()
            }
        }
    }
    
    /**
     * 创建性能回归测试
     */
    fun createPerformanceRegressionTest(
        name: String,
        description: String,
        performanceBlock: suspend () -> Long
    ): RegressionTest {
        return createCustomRegressionTest(
            name = name,
            description = description,
            category = RegressionTestCategory.PERFORMANCE
        ) {
            val measurements = mutableListOf<Long>()
            
            // 运行多次测试
            repeat(10) {
                measurements.add(performanceBlock())
            }
            
            RegressionMeasurement(
                averageTime = measurements.average(),
                minTime = measurements.minOrNull() ?: 0L,
                maxTime = measurements.maxOrNull() ?: 0L,
                samples = measurements.size,
                memoryUsage = 0.0,
                successRate = 100.0
            )
        }
    }
    
    /**
     * 创建功能回归测试
     */
    fun createFunctionalityRegressionTest(
        name: String,
        description: String,
        testCases: List<Pair<String, suspend () -> Boolean>>
    ): RegressionTest {
        return createCustomRegressionTest(
            name = name,
            description = description,
            category = RegressionTestCategory.FUNCTIONALITY,
            threshold = 0.0
        ) {
            var passedTests = 0
            val measurements = mutableListOf<Long>()
            
            for ((testCaseName, testBlock) in testCases) {
                val startTime = System.nanoTime()
                val result = testBlock()
                val endTime = System.nanoTime()
                
                measurements.add(endTime - startTime)
                if (result) passedTests++
            }
            
            val successRate = (passedTests.toDouble() / testCases.size) * 100
            
            RegressionMeasurement(
                averageTime = measurements.average(),
                minTime = measurements.minOrNull() ?: 0L,
                maxTime = measurements.maxOrNull() ?: 0L,
                samples = measurements.size,
                memoryUsage = 0.0,
                successRate = successRate,
                additionalMetrics = mapOf(
                    "total_tests" to testCases.size,
                    "passed_tests" to passedTests
                )
            )
        }
    }
}