package com.empathy.ai.data.optimization

import android.util.Log
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

/**
 * 动态负载均衡器
 * 
 * 负责根据实时负载情况动态分配解析任务
 * 通过智能负载分配策略，优化系统整体性能
 * 
 * 功能：
 * 1. 动态负载分配
 * 2. 负载监控和预测
 * 3. 自适应负载均衡策略
 * 4. 负载热点识别和优化
 */
class DynamicLoadBalancer private constructor() {
    
    companion object {
        private const val TAG = "DynamicLoadBalancer"
        
        // 负载均衡配置
        private const val LOAD_BALANCING_INTERVAL_MS = 5_000L // 5秒
        private const val LOAD_HISTORY_SIZE = 60 // 1分钟历史（每秒一个点）
        private const val LOAD_PREDICTION_WINDOW = 10 // 预测窗口大小
        
        // 负载阈值
        private const val HIGH_LOAD_THRESHOLD = 0.8 // 80%负载阈值
        private const val OVERLOAD_THRESHOLD = 0.9 // 90%过载阈值
        
        @Volatile
        private var INSTANCE: DynamicLoadBalancer? = null
        
        fun getInstance(): DynamicLoadBalancer {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DynamicLoadBalancer().also { INSTANCE = it }
            }
        }
    }
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val loadBalancingConfig = AtomicReference(LoadBalancingConfig())
    private val nodeRegistry = ConcurrentHashMap<String, LoadBalancingNode>()
    private val loadHistory = ConcurrentHashMap<String, LoadHistory>()
    private val currentStrategy = AtomicReference<LoadBalancingStrategy>(RoundRobinStrategy())
    private val loadPredictor = LoadPredictor()
    
    private var isLoadBalancingEnabled = false
    private var loadBalancingJob: Job? = null
    private val requestCounter = AtomicLong(0)
    
    init {
        initializeDefaultNodes()
        Log.i(TAG, "动态负载均衡器初始化完成")
    }
    
    /**
     * 启用负载均衡
     */
    fun enableLoadBalancing(config: LoadBalancingConfig = LoadBalancingConfig()) {
        if (isLoadBalancingEnabled) {
            Log.w(TAG, "负载均衡已启用")
            return
        }
        
        loadBalancingConfig.set(config)
        isLoadBalancingEnabled = true
        
        loadBalancingJob = scope.launch {
            while (isActive) {
                try {
                    performLoadBalancingCycle()
                    delay(LOAD_BALANCING_INTERVAL_MS)
                } catch (e: Exception) {
                    Log.e(TAG, "负载均衡周期执行失败", e)
                }
            }
        }
        
        Log.i(TAG, "负载均衡已启用")
    }
    
    /**
     * 禁用负载均衡
     */
    fun disableLoadBalancing() {
        if (!isLoadBalancingEnabled) {
            Log.w(TAG, "负载均衡未启用")
            return
        }
        
        isLoadBalancingEnabled = false
        loadBalancingJob?.cancel()
        loadBalancingJob = null
        
        Log.i(TAG, "负载均衡已禁用")
    }
    
    /**
     * 分配解析任务
     */
    suspend fun allocateParsingTask(
        operationType: String,
        modelName: String,
        dataSize: Int,
        priority: TaskPriority = TaskPriority.NORMAL
    ): AllocationResult {
        if (!isLoadBalancingEnabled) {
            // 负载均衡未启用，使用默认节点
            val defaultNode = nodeRegistry["default"] ?: return AllocationResult.failure("默认节点不可用")
            return AllocationResult.success(defaultNode.id, "默认分配")
        }
        
        val requestId = generateRequestId()
        val config = loadBalancingConfig.get()
        
        // 1. 预测负载
        val loadPrediction = loadPredictor.predictLoad(operationType, dataSize)
        
        // 2. 选择负载均衡策略
        val strategy = selectLoadBalancingStrategy(loadPrediction, config)
        
        // 3. 选择最佳节点
        val selectedNode = strategy.selectNode(
            nodes = nodeRegistry.values.toList(),
            operationType = operationType,
            dataSize = dataSize,
            priority = priority,
            loadPrediction = loadPrediction
        )
        
        return if (selectedNode != null) {
            // 4. 更新节点负载
            updateNodeLoad(selectedNode.id, operationType, dataSize, true)
            
            // 5. 记录分配
            recordAllocation(selectedNode.id, operationType, modelName, dataSize, priority)
            
            AllocationResult.success(selectedNode.id, selectedNode.reason)
        } else {
            AllocationResult.failure("没有可用的节点")
        }
    }
    
    /**
     * 释放解析任务
     */
    fun releaseParsingTask(nodeId: String, success: Boolean, durationMs: Long) {
        if (!isLoadBalancingEnabled) {
            return
        }
        
        updateNodeLoad(nodeId, "", 0, false)
        recordCompletion(nodeId, success, durationMs)
        
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "任务完成: 节点=$nodeId, 成功=$success, 耗时=${durationMs}ms")
        }
    }
    
    /**
     * 注册节点
     */
    fun registerNode(node: LoadBalancingNode): Boolean {
        if (nodeRegistry.containsKey(node.id)) {
            Log.w(TAG, "节点已存在: ${node.id}")
            return false
        }
        
        nodeRegistry[node.id] = node
        loadHistory[node.id] = LoadHistory()
        
        Log.i(TAG, "节点已注册: ${node.id}")
        return true
    }
    
    /**
     * 注销节点
     */
    fun unregisterNode(nodeId: String): Boolean {
        if (!nodeRegistry.containsKey(nodeId)) {
            Log.w(TAG, "节点不存在: $nodeId")
            return false
        }
        
        nodeRegistry.remove(nodeId)
        loadHistory.remove(nodeId)
        
        Log.i(TAG, "节点已注销: $nodeId")
        return true
    }
    
    /**
     * 更新节点状态
     */
    fun updateNodeStatus(nodeId: String, status: NodeStatus): Boolean {
        val node = nodeRegistry[nodeId] ?: return false
        
        val oldStatus = node.status
        node.status = status
        
        if (oldStatus != status) {
            Log.i(TAG, "节点状态变更: $nodeId $oldStatus -> $status")
        }
        
        return true
    }
    
    /**
     * 获取负载均衡状态
     */
    fun getLoadBalancingStatus(): LoadBalancingStatus {
        val currentStrategy = currentStrategy.get()
        val nodeStatuses = nodeRegistry.values.map { it.toNodeStatus() }
        val loadMetrics = calculateLoadMetrics()
        
        return LoadBalancingStatus(
            isEnabled = isLoadBalancingEnabled,
            currentStrategy = currentStrategy::class.java.simpleName,
            nodeStatuses = nodeStatuses,
            loadMetrics = loadMetrics,
            config = loadBalancingConfig.get()
        )
    }
    
    /**
     * 更新负载均衡配置
     */
    fun updateConfig(config: LoadBalancingConfig) {
        loadBalancingConfig.set(config)
        Log.i(TAG, "负载均衡配置已更新: $config")
    }
    
    /**
     * 获取负载均衡报告
     */
    fun getLoadBalancingReport(): LoadBalancingReport {
        val nodeReports = nodeRegistry.values.map { it.toNodeReport() }
        val loadMetrics = calculateLoadMetrics()
        val allocationHistory = getAllocationHistory()
        
        return LoadBalancingReport(
            timestamp = System.currentTimeMillis(),
            isEnabled = isLoadBalancingEnabled,
            currentStrategy = currentStrategy.get()::class.java.simpleName,
            nodeReports = nodeReports,
            loadMetrics = loadMetrics,
            allocationHistory = allocationHistory,
            recommendations = generateLoadBalancingRecommendations(loadMetrics, nodeReports)
        )
    }
    
    /**
     * 执行负载均衡周期
     */
    private suspend fun performLoadBalancingCycle() {
        val config = loadBalancingConfig.get()
        
        if (!config.enableAutoBalancing) {
            return
        }
        
        try {
            // 1. 更新节点负载信息
            updateNodeLoadMetrics()
            
            // 2. 检测负载热点
            val loadHotspots = detectLoadHotspots()
            
            // 3. 调整负载均衡策略
            adjustLoadBalancingStrategy(loadHotspots, config)
            
            // 4. 优化节点配置
            optimizeNodeConfiguration(loadHotspots, config)
            
        } catch (e: Exception) {
            Log.e(TAG, "负载均衡周期执行异常", e)
        }
    }
    
    /**
     * 选择负载均衡策略
     */
    private fun selectLoadBalancingStrategy(
        loadPrediction: LoadPrediction,
        config: LoadBalancingConfig
    ): LoadBalancingStrategy {
        val currentStrategy = currentStrategy.get()
        
        // 根据负载预测和配置选择最佳策略
        return when {
            loadPrediction.expectedLoad > OVERLOAD_THRESHOLD -> {
                // 高负载情况，使用最少连接策略
                LeastConnectionsStrategy()
            }
            
            loadPrediction.expectedLoad > HIGH_LOAD_THRESHOLD -> {
                // 中等负载情况，使用加权轮询策略
                WeightedRoundRobinStrategy()
            }
            
            config.enablePredictiveBalancing && loadPrediction.confidence > 0.8 -> {
                // 启用预测性负载均衡且置信度高
                PredictiveBalancingStrategy(loadPredictor)
            }
            
            else -> {
                // 默认使用当前策略
                currentStrategy
            }
        }
    }
    
    /**
     * 更新节点负载
     */
    private fun updateNodeLoad(
        nodeId: String,
        operationType: String,
        dataSize: Int,
        isAllocation: Boolean
    ) {
        val node = nodeRegistry[nodeId] ?: return
        val history = loadHistory[nodeId] ?: return
        
        val currentTime = System.currentTimeMillis()
        
        if (isAllocation) {
            node.currentConnections++
            node.totalRequests++
            
            // 更新操作类型统计
            node.operationTypeStats.computeIfAbsent(operationType) { 0 }
            node.operationTypeStats[operationType] = node.operationTypeStats[operationType]!! + 1
            
            // 更新数据大小统计
            node.totalDataSize += dataSize
        } else {
            node.currentConnections = maxOf(node.currentConnections - 1, 0)
        }
        
        // 更新负载历史
        history.addLoadPoint(LoadPoint(
            timestamp = currentTime,
            connections = node.currentConnections,
            load = calculateNodeLoad(node)
        ))
        
        // 保持历史大小
        if (history.loadPoints.size > LOAD_HISTORY_SIZE) {
            history.loadPoints.removeAt(0)
        }
    }
    
    /**
     * 计算节点负载
     */
    private fun calculateNodeLoad(node: LoadBalancingNode): Double {
        val connectionLoad = node.currentConnections.toDouble() / node.maxConnections
        val cpuLoad = node.cpuUsagePercent
        val memoryLoad = node.memoryUsagePercent
        
        // 综合负载计算（可配置权重）
        return (connectionLoad * 0.4 + cpuLoad * 0.3 + memoryLoad * 0.3)
    }
    
    /**
     * 记录分配
     */
    private fun recordAllocation(
        nodeId: String,
        operationType: String,
        modelName: String,
        dataSize: Int,
        priority: TaskPriority
    ) {
        val node = nodeRegistry[nodeId] ?: return
        
        node.allocationHistory.add(
            AllocationRecord(
                timestamp = System.currentTimeMillis(),
                operationType = operationType,
                modelName = modelName,
                dataSize = dataSize,
                priority = priority
            )
        )
        
        // 保持历史大小
        if (node.allocationHistory.size > 1000) {
            node.allocationHistory.removeAt(0)
        }
    }
    
    /**
     * 记录完成
     */
    private fun recordCompletion(nodeId: String, success: Boolean, durationMs: Long) {
        val node = nodeRegistry[nodeId] ?: return
        
        if (success) {
            node.successfulRequests++
        } else {
            node.failedRequests++
        }
        
        node.totalResponseTime += durationMs
        node.averageResponseTime = node.totalResponseTime.toDouble() / node.totalRequests
    }
    
    /**
     * 更新节点负载指标
     */
    private suspend fun updateNodeLoadMetrics() {
        // 在实际应用中，这里可以从各个节点获取实时负载信息
        // 这里使用模拟数据
        nodeRegistry.values.forEach { node ->
            // 模拟CPU和内存使用率
            node.cpuUsagePercent = 0.3 + (node.currentConnections.toDouble() / node.maxConnections) * 0.4
            node.memoryUsagePercent = 0.4 + (node.currentConnections.toDouble() / node.maxConnections) * 0.3
            
            // 更新节点状态
            node.status = when {
                calculateNodeLoad(node) > OVERLOAD_THRESHOLD -> NodeStatus.OVERLOADED
                calculateNodeLoad(node) > HIGH_LOAD_THRESHOLD -> NodeStatus.HIGH_LOAD
                node.currentConnections == 0 -> NodeStatus.IDLE
                else -> NodeStatus.NORMAL
            }
        }
    }
    
    /**
     * 检测负载热点
     */
    private fun detectLoadHotspots(): List<LoadHotspot> {
        val hotspots = mutableListOf<LoadHotspot>()
        
        nodeRegistry.values.forEach { node ->
            val load = calculateNodeLoad(node)
            
            when {
                load > OVERLOAD_THRESHOLD -> {
                    hotspots.add(LoadHotspot(
                        nodeId = node.id,
                        type = HotspotType.OVERLOAD,
                        severity = HotspotSeverity.CRITICAL,
                        currentValue = load,
                        description = "节点${node.id}过载，负载率${String.format("%.1f%%", load * 100)}"
                    ))
                }
                
                load > HIGH_LOAD_THRESHOLD -> {
                    hotspots.add(LoadHotspot(
                        nodeId = node.id,
                        type = HotspotType.HIGH_LOAD,
                        severity = HotspotSeverity.WARNING,
                        currentValue = load,
                        description = "节点${node.id}高负载，负载率${String.format("%.1f%%", load * 100)}"
                    ))
                }
            }
        }
        
        return hotspots
    }
    
    /**
     * 调整负载均衡策略
     */
    private fun adjustLoadBalancingStrategy(
        hotspots: List<LoadHotspot>,
        config: LoadBalancingConfig
    ) {
        val criticalHotspots = hotspots.filter { it.severity == HotspotSeverity.CRITICAL }
        
        if (criticalHotspots.isNotEmpty()) {
            // 存在严重过载节点，切换到最少连接策略
            val newStrategy = LeastConnectionsStrategy()
            if (newStrategy::class.java != currentStrategy.get()::class.java) {
                currentStrategy.set(newStrategy)
                Log.i(TAG, "负载均衡策略已切换到最少连接策略")
            }
        }
    }
    
    /**
     * 优化节点配置
     */
    private fun optimizeNodeConfiguration(
        hotspots: List<LoadHotspot>,
        config: LoadBalancingConfig
    ) {
        // 根据热点情况优化节点配置
        hotspots.forEach { hotspot ->
            val node = nodeRegistry[hotspot.nodeId] ?: return@forEach
            
            when (hotspot.type) {
                HotspotType.OVERLOAD -> {
                    // 过载节点，减少最大连接数
                    if (config.enableAutoScaling) {
                        node.maxConnections = maxOf(node.maxConnections - 5, 1)
                        Log.i(TAG, "减少过载节点${node.id}的最大连接数到${node.maxConnections}")
                    }
                }
                
                HotspotType.HIGH_LOAD -> {
                    // 高负载节点，记录警告
                    Log.w(TAG, "节点${node.id}高负载: ${hotspot.description}")
                }
            }
        }
    }
    
    /**
     * 计算负载指标
     */
    private fun calculateLoadMetrics(): LoadMetrics {
        val nodes = nodeRegistry.values.toList()
        if (nodes.isEmpty()) {
            return LoadMetrics()
        }
        
        val totalConnections = nodes.sumOf { it.currentConnections }
        val maxConnections = nodes.sumOf { it.maxConnections }
        val averageLoad = nodes.map { calculateNodeLoad(it) }.average()
        val maxLoad = nodes.map { calculateNodeLoad(it) }.maxOrNull() ?: 0.0
        
        val totalRequests = nodes.sumOf { it.totalRequests }
        val successfulRequests = nodes.sumOf { it.successfulRequests }
        val failedRequests = nodes.sumOf { it.failedRequests }
        val successRate = if (totalRequests > 0) successfulRequests.toDouble() / totalRequests else 0.0
        
        val averageResponseTime = nodes.mapNotNull { 
            if (it.totalRequests > 0) it.averageResponseTime else null 
        }.average()
        
        return LoadMetrics(
            totalConnections = totalConnections,
            maxConnections = maxConnections,
            connectionUtilization = if (maxConnections > 0) totalConnections.toDouble() / maxConnections else 0.0,
            averageLoad = averageLoad,
            maxLoad = maxLoad,
            totalRequests = totalRequests,
            successfulRequests = successfulRequests,
            failedRequests = failedRequests,
            successRate = successRate,
            averageResponseTime = averageResponseTime
        )
    }
    
    /**
     * 获取分配历史
     */
    private fun getAllocationHistory(): List<AllocationRecord> {
        return nodeRegistry.values.flatMap { it.allocationHistory }
            .sortedByDescending { it.timestamp }
            .take(100)
    }
    
    /**
     * 生成负载均衡建议
     */
    private fun generateLoadBalancingRecommendations(
        loadMetrics: LoadMetrics,
        nodeReports: List<NodeReport>
    ): List<LoadBalancingRecommendation> {
        val recommendations = mutableListOf<LoadBalancingRecommendation>()
        
        // 连接利用率建议
        when {
            loadMetrics.connectionUtilization > 0.9 -> {
                recommendations.add(LoadBalancingRecommendation(
                    type = RecommendationType.ADD_NODES,
                    priority = RecommendationPriority.CRITICAL,
                    message = "连接利用率过高，建议增加节点",
                    action = "添加新的负载均衡节点以分散负载"
                ))
            }
            
            loadMetrics.connectionUtilization > 0.8 -> {
                recommendations.add(LoadBalancingRecommendation(
                    type = RecommendationType.OPTIMIZE_STRATEGY,
                    priority = RecommendationPriority.HIGH,
                    message = "连接利用率较高，建议优化负载均衡策略",
                    action = "考虑使用加权轮询或最少连接策略"
                ))
            }
        }
        
        // 成功率建议
        if (loadMetrics.successRate < 0.95) {
            recommendations.add(LoadBalancingRecommendation(
                type = RecommendationType.IMPROVE_ERROR_HANDLING,
                priority = RecommendationPriority.MEDIUM,
                message = "请求成功率偏低，建议改进错误处理",
                action = "增强错误恢复机制和重试策略"
            ))
        }
        
        // 响应时间建议
        if (loadMetrics.averageResponseTime > 1000) {
            recommendations.add(LoadBalancingRecommendation(
                type = RecommendationType.OPTIMIZE_PERFORMANCE,
                priority = RecommendationPriority.MEDIUM,
                message = "平均响应时间过长，建议优化性能",
                action = "优化解析算法或增加缓存机制"
            ))
        }
        
        return recommendations.sortedByDescending { it.priority.value }
    }
    
    /**
     * 初始化默认节点
     */
    private fun initializeDefaultNodes() {
        val defaultNode = LoadBalancingNode(
            id = "default",
            name = "默认节点",
            host = "localhost",
            port = 8080,
            maxConnections = 100,
            weight = 1.0,
            status = NodeStatus.ACTIVE
        )
        
        registerNode(defaultNode)
        Log.d(TAG, "默认节点初始化完成")
    }
    
    /**
     * 生成请求ID
     */
    private fun generateRequestId(): String {
        return "req_${requestCounter.incrementAndGet()}_${System.currentTimeMillis()}"
    }
    
    /**
     * 清理资源
     */
    fun cleanup() {
        disableLoadBalancing()
        scope.cancel()
        nodeRegistry.clear()
        loadHistory.clear()
        
        Log.i(TAG, "动态负载均衡器资源清理完成")
    }
    
    /**
     * 负载均衡配置
     */
    data class LoadBalancingConfig(
        val enableAutoBalancing: Boolean = true,
        val enablePredictiveBalancing: Boolean = true,
        val enableAutoScaling: Boolean = false,
        val balancingIntervalMs: Long = LOAD_BALANCING_INTERVAL_MS,
        val loadThresholdHigh: Double = HIGH_LOAD_THRESHOLD,
        val loadThresholdOverload: Double = OVERLOAD_THRESHOLD,
        val maxHistorySize: Int = LOAD_HISTORY_SIZE
    )
    
    /**
     * 负载均衡节点
     */
    data class LoadBalancingNode(
        val id: String,
        val name: String,
        val host: String,
        val port: Int,
        var maxConnections: Int,
        var weight: Double = 1.0,
        var status: NodeStatus = NodeStatus.INACTIVE,
        var currentConnections: Int = 0,
        var totalRequests: Long = 0,
        var successfulRequests: Long = 0,
        var failedRequests: Long = 0,
        var totalResponseTime: Long = 0,
        var averageResponseTime: Double = 0.0,
        var cpuUsagePercent: Double = 0.0,
        var memoryUsagePercent: Double = 0.0,
        var totalDataSize: Long = 0,
        val operationTypeStats: MutableMap<String, Int> = mutableMapOf(),
        val allocationHistory: MutableList<AllocationRecord> = mutableListOf()
    ) {
        fun toNodeStatus(): NodeStatusInfo {
            return NodeStatusInfo(
                id = id,
                name = name,
                host = host,
                port = port,
                status = status,
                currentConnections = currentConnections,
                maxConnections = maxConnections,
                totalRequests = totalRequests,
                successRate = if (totalRequests > 0) successfulRequests.toDouble() / totalRequests else 0.0,
                averageResponseTime = averageResponseTime,
                cpuUsagePercent = cpuUsagePercent,
                memoryUsagePercent = memoryUsagePercent,
                load = currentConnections.toDouble() / maxConnections
            )
        }
        
        fun toNodeReport(): NodeReport {
            return NodeReport(
                id = id,
                name = name,
                status = status,
                currentConnections = currentConnections,
                maxConnections = maxConnections,
                totalRequests = totalRequests,
                successfulRequests = successfulRequests,
                failedRequests = failedRequests,
                successRate = if (totalRequests > 0) successfulRequests.toDouble() / totalRequests else 0.0,
                averageResponseTime = averageResponseTime,
                cpuUsagePercent = cpuUsagePercent,
                memoryUsagePercent = memoryUsagePercent,
                operationTypeStats = operationTypeStats.toMap(),
                recentAllocations = allocationHistory.takeLast(10)
            )
        }
    }
    
    /**
     * 节点状态
     */
    enum class NodeStatus {
        INACTIVE,   // 未激活
        ACTIVE,      // 激活
        IDLE,        // 空闲
        NORMAL,      // 正常
        HIGH_LOAD,   // 高负载
        OVERLOADED   // 过载
    }
    
    /**
     * 节点状态信息
     */
    data class NodeStatusInfo(
        val id: String,
        val name: String,
        val host: String,
        val port: Int,
        val status: NodeStatus,
        val currentConnections: Int,
        val maxConnections: Int,
        val totalRequests: Long,
        val successRate: Double,
        val averageResponseTime: Double,
        val cpuUsagePercent: Double,
        val memoryUsagePercent: Double,
        val load: Double
    )
    
    /**
     * 任务优先级
     */
    enum class TaskPriority {
        LOW,
        NORMAL,
        HIGH,
        CRITICAL
    }
    
    /**
     * 分配结果
     */
    data class AllocationResult(
        val success: Boolean,
        val nodeId: String? = null,
        val message: String
    ) {
        companion object {
            fun success(nodeId: String, message: String): AllocationResult {
                return AllocationResult(true, nodeId, message)
            }
            
            fun failure(message: String): AllocationResult {
                return AllocationResult(false, null, message)
            }
        }
    }
    
    /**
     * 分配记录
     */
    data class AllocationRecord(
        val timestamp: Long,
        val operationType: String,
        val modelName: String,
        val dataSize: Int,
        val priority: TaskPriority
    )
    
    /**
     * 负载历史记录
     */
    class LoadHistory {
        val loadPoints = mutableListOf<LoadPoint>()
        
        fun addLoadPoint(loadPoint: LoadPoint) {
            loadPoints.add(loadPoint)
        }
    }
    
    /**
     * 负载点
     */
    data class LoadPoint(
        val timestamp: Long,
        val connections: Int,
        val load: Double
    )
    
    /**
     * 负载预测
     */
    data class LoadPrediction(
        val expectedLoad: Double,
        val confidence: Double,
        val predictionWindowMs: Long
    )
    
    /**
     * 负载均衡策略接口
     */
    interface LoadBalancingStrategy {
        fun selectNode(
            nodes: List<LoadBalancingNode>,
            operationType: String,
            dataSize: Int,
            priority: TaskPriority,
            loadPrediction: LoadPrediction
        ): LoadBalancingNode?
    }
    
    /**
     * 轮询策略
     */
    class RoundRobinStrategy : LoadBalancingStrategy {
        private var currentIndex = 0
        
        override fun selectNode(
            nodes: List<LoadBalancingNode>,
            operationType: String,
            dataSize: Int,
            priority: TaskPriority,
            loadPrediction: LoadPrediction
        ): LoadBalancingNode? {
            val activeNodes = nodes.filter { it.status == NodeStatus.ACTIVE || it.status == NodeStatus.NORMAL }
            if (activeNodes.isEmpty()) {
                return null
            }
            
            val selectedNode = activeNodes[currentIndex % activeNodes.size]
            currentIndex++
            return selectedNode
        }
    }
    
    /**
     * 最少连接策略
     */
    class LeastConnectionsStrategy : LoadBalancingStrategy {
        override fun selectNode(
            nodes: List<LoadBalancingNode>,
            operationType: String,
            dataSize: Int,
            priority: TaskPriority,
            loadPrediction: LoadPrediction
        ): LoadBalancingNode? {
            return nodes
                .filter { it.status == NodeStatus.ACTIVE || it.status == NodeStatus.NORMAL }
                .minByOrNull { it.currentConnections }
        }
    }
    
    /**
     * 加权轮询策略
     */
    class WeightedRoundRobinStrategy : LoadBalancingStrategy {
        override fun selectNode(
            nodes: List<LoadBalancingNode>,
            operationType: String,
            dataSize: Int,
            priority: TaskPriority,
            loadPrediction: LoadPrediction
        ): LoadBalancingNode? {
            val activeNodes = nodes.filter { it.status == NodeStatus.ACTIVE || it.status == NodeStatus.NORMAL }
            if (activeNodes.isEmpty()) {
                return null
            }
            
            // 根据权重和当前负载计算选择概率
            val totalWeight = activeNodes.sumOf { it.weight }
            var randomWeight = Math.random() * totalWeight
            
            for (node in activeNodes) {
                randomWeight -= node.weight
                if (randomWeight <= 0) {
                    return node
                }
            }
            
            return activeNodes.first()
        }
    }
    
    /**
     * 预测性负载均衡策略
     */
    class PredictiveBalancingStrategy(
        private val loadPredictor: LoadPredictor
    ) : LoadBalancingStrategy {
        override fun selectNode(
            nodes: List<LoadBalancingNode>,
            operationType: String,
            dataSize: Int,
            priority: TaskPriority,
            loadPrediction: LoadPrediction
        ): LoadBalancingNode? {
            val activeNodes = nodes.filter { it.status == NodeStatus.ACTIVE || it.status == NodeStatus.NORMAL }
            if (activeNodes.isEmpty()) {
                return null
            }
            
            // 选择预期负载最低的节点
            return activeNodes.minByOrNull { node ->
                val nodeLoad = node.currentConnections.toDouble() / node.maxConnections
                val predictedLoad = loadPredictor.predictNodeLoad(node, operationType, dataSize)
                nodeLoad * 0.7 + predictedLoad * 0.3
            }
        }
    }
    
    /**
     * 负载预测器
     */
    class LoadPredictor {
        private val loadHistory = mutableMapOf<String, MutableList<Double>>()
        
        fun predictLoad(operationType: String, dataSize: Int): LoadPrediction {
            // 简化的负载预测算法
            val history = loadHistory.getOrPut(operationType) { mutableListOf() }
            
            if (history.size < 3) {
                return LoadPrediction(0.5, 0.3, 60000) // 1分钟窗口
            }
            
            // 使用移动平均预测
            val recentLoads = history.takeLast(5)
            val predictedLoad = recentLoads.average()
            val confidence = minOf(recentLoads.size.toDouble() / 5, 1.0)
            
            return LoadPrediction(predictedLoad, confidence, 60000)
        }
        
        fun predictNodeLoad(node: LoadBalancingNode, operationType: String, dataSize: Int): Double {
            // 简化的节点负载预测
            val currentLoad = node.currentConnections.toDouble() / node.maxConnections
            val dataSizeImpact = dataSize.toDouble() / 10000 // 假设10KB为基准
            
            return currentLoad + dataSizeImpact * 0.1
        }
    }
    
    /**
     * 负载热点
     */
    data class LoadHotspot(
        val nodeId: String,
        val type: HotspotType,
        val severity: HotspotSeverity,
        val currentValue: Double,
        val description: String
    )
    
    /**
     * 热点类型
     */
    enum class HotspotType {
        OVERLOAD,
        HIGH_LOAD,
        CONNECTION_SPIKE,
        RESPONSE_TIME_SPIKE
    }
    
    /**
     * 热点严重程度
     */
    enum class HotspotSeverity {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }
    
    /**
     * 负载指标
     */
    data class LoadMetrics(
        val totalConnections: Int = 0,
        val maxConnections: Int = 0,
        val connectionUtilization: Double = 0.0,
        val averageLoad: Double = 0.0,
        val maxLoad: Double = 0.0,
        val totalRequests: Long = 0,
        val successfulRequests: Long = 0,
        val failedRequests: Long = 0,
        val successRate: Double = 0.0,
        val averageResponseTime: Double = 0.0
    )
    
    /**
     * 负载均衡状态
     */
    data class LoadBalancingStatus(
        val isEnabled: Boolean,
        val currentStrategy: String,
        val nodeStatuses: List<NodeStatusInfo>,
        val loadMetrics: LoadMetrics,
        val config: LoadBalancingConfig
    )
    
    /**
     * 节点报告
     */
    data class NodeReport(
        val id: String,
        val name: String,
        val status: NodeStatus,
        val currentConnections: Int,
        val maxConnections: Int,
        val totalRequests: Long,
        val successfulRequests: Long,
        val failedRequests: Long,
        val successRate: Double,
        val averageResponseTime: Double,
        val cpuUsagePercent: Double,
        val memoryUsagePercent: Double,
        val operationTypeStats: Map<String, Int>,
        val recentAllocations: List<AllocationRecord>
    )
    
    /**
     * 负载均衡报告
     */
    data class LoadBalancingReport(
        val timestamp: Long,
        val isEnabled: Boolean,
        val currentStrategy: String,
        val nodeReports: List<NodeReport>,
        val loadMetrics: LoadMetrics,
        val allocationHistory: List<AllocationRecord>,
        val recommendations: List<LoadBalancingRecommendation>
    )
    
    /**
     * 建议类型
     */
    enum class RecommendationType {
        ADD_NODES,
        REMOVE_NODES,
        OPTIMIZE_STRATEGY,
        IMPROVE_ERROR_HANDLING,
        OPTIMIZE_PERFORMANCE
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
     * 负载均衡建议
     */
    data class LoadBalancingRecommendation(
        val type: RecommendationType,
        val priority: RecommendationPriority,
        val message: String,
        val action: String
    )
}