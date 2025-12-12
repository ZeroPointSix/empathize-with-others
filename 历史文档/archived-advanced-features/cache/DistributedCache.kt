package com.empathy.ai.data.cache

import com.empathy.ai.data.domain.model.AiResponseParserMetrics
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

/**
 * 分布式缓存管理器
 * 支持多节点缓存同步和一致性保证
 */
class DistributedCache private constructor(
    private val nodeId: String,
    private val metrics: AiResponseParserMetrics
) {
    companion object {
        @Volatile
        private var INSTANCE: DistributedCache? = null
        
        fun getInstance(nodeId: String, metrics: AiResponseParserMetrics): DistributedCache {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DistributedCache(nodeId, metrics).also { INSTANCE = it }
            }
        }
        
        private const val SYNC_INTERVAL = 30000L // 30秒
        private const val HEARTBEAT_INTERVAL = 10000L // 10秒
        private const val NODE_TIMEOUT = 60000L // 1分钟
        private const val MAX_RETRY_ATTEMPTS = 3
    }
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // 节点注册表
    private val nodeRegistry = ConcurrentHashMap<String, NodeInfo>()
    
    // 缓存数据
    private val cacheData = ConcurrentHashMap<String, CacheEntry>()
    
    // 分布式状态
    private val _distributedState = MutableStateFlow(DistributedCacheState())
    val distributedState: StateFlow<DistributedCacheState> = _distributedState.asStateFlow()
    
    // 分布式统计
    private val distributedStats = AtomicReference(DistributedCacheStatistics())
    
    // 同步队列
    private val syncQueue = mutableListOf<SyncOperation>()
    private val syncMutex = Mutex()
    
    // 网络模拟器（实际应用中应该是真实的网络通信）
    private val networkSimulator = NetworkSimulator(nodeId)
    
    // 同步任务
    private var syncJob: Job? = null
    private var heartbeatJob: Job? = null
    
    init {
        registerCurrentNode()
        startSyncTask()
        startHeartbeatTask()
    }
    
    /**
     * 注册当前节点
     */
    private fun registerCurrentNode() {
        val currentNode = NodeInfo(
            id = nodeId,
            address = "node://$nodeId",
            status = NodeStatus.ACTIVE,
            lastHeartbeat = System.currentTimeMillis(),
            cacheSize = 0,
            version = 1
        )
        
        nodeRegistry[nodeId] = currentNode
        metrics.recordDistributedCacheNodeRegistered(nodeId)
    }
    
    /**
     * 启动同步任务
     */
    private fun startSyncTask() {
        syncJob = scope.launch {
            while (isActive) {
                try {
                    performSync()
                    delay(SYNC_INTERVAL)
                } catch (e: Exception) {
                    metrics.recordDistributedCacheError("同步任务错误", e)
                }
            }
        }
    }
    
    /**
     * 启动心跳任务
     */
    private fun startHeartbeatTask() {
        heartbeatJob = scope.launch {
            while (isActive) {
                try {
                    sendHeartbeat()
                    checkNodeHealth()
                    delay(HEARTBEAT_INTERVAL)
                } catch (e: Exception) {
                    metrics.recordDistributedCacheError("心跳任务错误", e)
                }
            }
        }
    }
    
    /**
     * 获取值
     */
    suspend fun get(key: String): Any? {
        // 首先尝试本地缓存
        val localEntry = cacheData[key]
        if (localEntry != null && !localEntry.isExpired()) {
            metrics.recordDistributedCacheHit(nodeId, key, "local")
            return localEntry.value
        }
        
        // 尝试从其他节点获取
        val remoteValue = getFromRemoteNodes(key)
        if (remoteValue != null) {
            // 更新本地缓存
            cacheData[key] = CacheEntry(remoteValue, System.currentTimeMillis() + 300000L) // 5分钟过期
            metrics.recordDistributedCacheHit(nodeId, key, "remote")
            return remoteValue
        }
        
        metrics.recordDistributedCacheMiss(nodeId, key)
        return null
    }
    
    /**
     * 设置值
     */
    suspend fun put(key: String, value: Any, expireTime: Long = 0L) {
        val actualExpireTime = if (expireTime > 0) expireTime else System.currentTimeMillis() + 300000L
        val entry = CacheEntry(value, actualExpireTime)
        
        // 更新本地缓存
        cacheData[key] = entry
        
        // 添加到同步队列
        syncMutex.withLock {
            syncQueue.add(SyncOperation(
                type = SyncOperationType.PUT,
                key = key,
                value = value,
                expireTime = actualExpireTime,
                timestamp = System.currentTimeMillis(),
                nodeId = nodeId
            ))
        }
        
        metrics.recordDistributedCachePut(nodeId, key)
    }
    
    /**
     * 移除值
     */
    suspend fun remove(key: String): Any? {
        val value = cacheData.remove(key)
        
        // 添加到同步队列
        syncMutex.withLock {
            syncQueue.add(SyncOperation(
                type = SyncOperationType.REMOVE,
                key = key,
                value = null,
                expireTime = 0,
                timestamp = System.currentTimeMillis(),
                nodeId = nodeId
            ))
        }
        
        metrics.recordDistributedCacheRemove(nodeId, key)
        return value?.value
    }
    
    /**
     * 清空缓存
     */
    suspend fun clear() {
        cacheData.clear()
        
        // 添加到同步队列
        syncMutex.withLock {
            syncQueue.add(SyncOperation(
                type = SyncOperationType.CLEAR,
                key = "",
                value = null,
                expireTime = 0,
                timestamp = System.currentTimeMillis(),
                nodeId = nodeId
            ))
        }
        
        metrics.recordDistributedCacheClear(nodeId)
    }
    
    /**
     * 获取缓存大小
     */
    fun size(): Int = cacheData.size
    
    /**
     * 从远程节点获取值
     */
    private suspend fun getFromRemoteNodes(key: String): Any? {
        val activeNodes = nodeRegistry.values.filter { it.status == NodeStatus.ACTIVE && it.id != nodeId }
        
        for (node in activeNodes) {
            try {
                val value = networkSimulator.requestValue(node.id, key)
                if (value != null) {
                    return value
                }
            } catch (e: Exception) {
                metrics.recordDistributedCacheError("远程节点请求错误: ${node.id}", e)
            }
        }
        
        return null
    }
    
    /**
     * 执行同步
     */
    private suspend fun performSync() {
        syncMutex.withLock {
            if (syncQueue.isEmpty()) return@withLock
            
            val operationsToSync = syncQueue.toList()
            syncQueue.clear()
            
            for (operation in operationsToSync) {
                try {
                    syncToNodes(operation)
                } catch (e: Exception) {
                    metrics.recordDistributedCacheError("同步操作错误", e)
                    // 重新加入队列重试
                    syncQueue.add(operation)
                }
            }
        }
        
        updateDistributedState()
    }
    
    /**
     * 同步到其他节点
     */
    private suspend fun syncToNodes(operation: SyncOperation) {
        val activeNodes = nodeRegistry.values.filter { it.status == NodeStatus.ACTIVE && it.id != nodeId }
        
        for (node in activeNodes) {
            var retryCount = 0
            var success = false
            
            while (retryCount < MAX_RETRY_ATTEMPTS && !success) {
                try {
                    networkSimulator.syncOperation(node.id, operation)
                    success = true
                    metrics.recordDistributedCacheSyncSuccess(nodeId, node.id, operation.type.name)
                } catch (e: Exception) {
                    retryCount++
                    metrics.recordDistributedCacheError("同步重试错误: ${node.id}", e)
                    if (retryCount < MAX_RETRY_ATTEMPTS) {
                        delay(1000L * retryCount) // 指数退避
                    }
                }
            }
            
            if (!success) {
                metrics.recordDistributedCacheSyncFailure(nodeId, node.id, operation.type.name)
            }
        }
    }
    
    /**
     * 发送心跳
     */
    private suspend fun sendHeartbeat() {
        val currentNode = nodeRegistry[nodeId] ?: return
        
        currentNode.lastHeartbeat = System.currentTimeMillis()
        currentNode.cacheSize = cacheData.size
        
        // 广播心跳到其他节点
        val activeNodes = nodeRegistry.values.filter { it.status == NodeStatus.ACTIVE && it.id != nodeId }
        
        for (node in activeNodes) {
            try {
                networkSimulator.sendHeartbeat(node.id, currentNode)
            } catch (e: Exception) {
                metrics.recordDistributedCacheError("心跳发送错误: ${node.id}", e)
            }
        }
    }
    
    /**
     * 检查节点健康状态
     */
    private fun checkNodeHealth() {
        val currentTime = System.currentTimeMillis()
        val unhealthyNodes = mutableListOf<String>()
        
        for ((nodeId, node) in nodeRegistry) {
            if (nodeId != this.nodeId && currentTime - node.lastHeartbeat > NODE_TIMEOUT) {
                node.status = NodeStatus.INACTIVE
                unhealthyNodes.add(nodeId)
                metrics.recordDistributedCacheNodeUnhealthy(nodeId)
            }
        }
        
        // 移除长时间不活跃的节点
        for (nodeId in unhealthyNodes) {
            if (currentTime - nodeRegistry[nodeId]?.lastHeartbeat ?: 0 > NODE_TIMEOUT * 3) {
                nodeRegistry.remove(nodeId)
                metrics.recordDistributedCacheNodeRemoved(nodeId)
            }
        }
    }
    
    /**
     * 更新分布式状态
     */
    private fun updateDistributedState() {
        val totalNodes = nodeRegistry.size
        val activeNodes = nodeRegistry.values.count { it.status == NodeStatus.ACTIVE }
        val totalCacheSize = nodeRegistry.values.sumOf { it.cacheSize }
        
        val currentState = _distributedState.value.copy(
            totalNodes = totalNodes,
            activeNodes = activeNodes,
            totalCacheSize = totalCacheSize,
            localCacheSize = cacheData.size,
            lastSyncTime = System.currentTimeMillis()
        )
        _distributedState.value = currentState
        
        updateDistributedStatistics()
    }
    
    /**
     * 更新分布式统计信息
     */
    private fun updateDistributedStatistics() {
        val currentStats = distributedStats.get()
        val newStats = currentStats.copy(
            totalSyncs = currentStats.totalSyncs + 1,
            lastUpdateTime = System.currentTimeMillis()
        )
        distributedStats.set(newStats)
    }
    
    /**
     * 处理远程同步操作
     */
    suspend fun handleRemoteSync(operation: SyncOperation) {
        when (operation.type) {
            SyncOperationType.PUT -> {
                val entry = CacheEntry(operation.value!!, operation.expireTime)
                cacheData[operation.key] = entry
                metrics.recordDistributedCacheRemotePut(nodeId, operation.key, operation.nodeId)
            }
            
            SyncOperationType.REMOVE -> {
                cacheData.remove(operation.key)
                metrics.recordDistributedCacheRemoteRemove(nodeId, operation.key, operation.nodeId)
            }
            
            SyncOperationType.CLEAR -> {
                cacheData.clear()
                metrics.recordDistributedCacheRemoteClear(nodeId, operation.nodeId)
            }
        }
    }
    
    /**
     * 处理远程心跳
     */
    fun handleRemoteHeartbeat(nodeInfo: NodeInfo) {
        nodeRegistry[nodeInfo.id] = nodeInfo
        metrics.recordDistributedCacheHeartbeatReceived(nodeId, nodeInfo.id)
    }
    
    /**
     * 处理远程值请求
     */
    fun handleRemoteValueRequest(key: String): Any? {
        val entry = cacheData[key]
        return if (entry != null && !entry.isExpired()) {
            entry.value
        } else null
    }
    
    /**
     * 添加节点
     */
    suspend fun addNode(nodeInfo: NodeInfo) {
        nodeRegistry[nodeInfo.id] = nodeInfo
        metrics.recordDistributedCacheNodeAdded(nodeInfo.id)
        
        // 同步当前缓存数据到新节点
        syncCacheToNewNode(nodeInfo.id)
    }
    
    /**
     * 同步缓存数据到新节点
     */
    private suspend fun syncCacheToNewNode(targetNodeId: String) {
        for ((key, entry) in cacheData) {
            if (!entry.isExpired()) {
                val operation = SyncOperation(
                    type = SyncOperationType.PUT,
                    key = key,
                    value = entry.value,
                    expireTime = entry.expireTime,
                    timestamp = System.currentTimeMillis(),
                    nodeId = nodeId
                )
                
                try {
                    networkSimulator.syncOperation(targetNodeId, operation)
                } catch (e: Exception) {
                    metrics.recordDistributedCacheError("新节点同步错误: $targetNodeId", e)
                }
            }
        }
    }
    
    /**
     * 移除节点
     */
    fun removeNode(nodeId: String) {
        nodeRegistry.remove(nodeId)
        metrics.recordDistributedCacheNodeRemoved(nodeId)
    }
    
    /**
     * 获取节点信息
     */
    fun getNodeInfo(nodeId: String): NodeInfo? {
        return nodeRegistry[nodeId]
    }
    
    /**
     * 获取所有节点信息
     */
    fun getAllNodes(): List<NodeInfo> {
        return nodeRegistry.values.toList()
    }
    
    /**
     * 获取分布式统计信息
     */
    fun getDistributedStatistics(): DistributedCacheStatistics {
        return distributedStats.get()
    }
    
    /**
     * 获取分布式状态
     */
    fun getDistributedState(): DistributedCacheState {
        return _distributedState.value
    }
    
    /**
     * 强制同步
     */
    suspend fun forceSync() {
        performSync()
    }
    
    /**
     * 销毁分布式缓存
     */
    fun destroy() {
        syncJob?.cancel()
        heartbeatJob?.cancel()
        scope.cancel()
        
        nodeRegistry.clear()
        cacheData.clear()
        syncQueue.clear()
        
        INSTANCE = null
    }
}

/**
 * 缓存条目
 */
data class CacheEntry(
    val value: Any,
    val expireTime: Long
) {
    fun isExpired(currentTime: Long = System.currentTimeMillis()): Boolean {
        return expireTime > 0 && currentTime >= expireTime
    }
}

/**
 * 节点信息
 */
data class NodeInfo(
    val id: String,
    val address: String,
    var status: NodeStatus,
    var lastHeartbeat: Long,
    var cacheSize: Int,
    var version: Int
)

/**
 * 节点状态
 */
enum class NodeStatus {
    ACTIVE,
    INACTIVE,
    FAILED
}

/**
 * 同步操作
 */
data class SyncOperation(
    val type: SyncOperationType,
    val key: String,
    val value: Any?,
    val expireTime: Long,
    val timestamp: Long,
    val nodeId: String
)

/**
 * 同步操作类型
 */
enum class SyncOperationType {
    PUT,
    REMOVE,
    CLEAR
}

/**
 * 分布式缓存状态
 */
data class DistributedCacheState(
    val totalNodes: Int = 0,
    val activeNodes: Int = 0,
    val totalCacheSize: Int = 0,
    val localCacheSize: Int = 0,
    val lastSyncTime: Long = 0L,
    val lastUpdateTime: Long = 0L
)

/**
 * 分布式缓存统计信息
 */
data class DistributedCacheStatistics(
    val totalSyncs: Long = 0L,
    val successfulSyncs: Long = 0L,
    val failedSyncs: Long = 0L,
    val lastUpdateTime: Long = 0L
) {
    val syncSuccessRate: Float
        get() = if (totalSyncs > 0) successfulSyncs.toFloat() / totalSyncs.toFloat() else 0f
}

/**
 * 网络模拟器（实际应用中应该是真实的网络通信实现）
 */
class NetworkSimulator(private val nodeId: String) {
    private val requestHandlers = ConcurrentHashMap<String, DistributedCache>()
    
    fun registerHandler(nodeId: String, handler: DistributedCache) {
        requestHandlers[nodeId] = handler
    }
    
    suspend fun requestValue(targetNodeId: String, key: String): Any? {
        val handler = requestHandlers[targetNodeId]
        return handler?.handleRemoteValueRequest(key)
    }
    
    suspend fun syncOperation(targetNodeId: String, operation: SyncOperation) {
        val handler = requestHandlers[targetNodeId]
        handler?.handleRemoteSync(operation)
    }
    
    suspend fun sendHeartbeat(targetNodeId: String, nodeInfo: NodeInfo) {
        val handler = requestHandlers[targetNodeId]
        handler?.handleRemoteHeartbeat(nodeInfo)
    }
}

/**
 * 分布式缓存构建器
 */
class DistributedCacheBuilder {
    private var nodeId: String = "default"
    private var metrics: AiResponseParserMetrics? = null
    
    fun nodeId(nodeId: String) = apply { this.nodeId = nodeId }
    
    fun metrics(metrics: AiResponseParserMetrics) = apply { this.metrics = metrics }
    
    fun build(): DistributedCache {
        require(metrics != null) { "指标收集器是必需的" }
        
        return DistributedCache.getInstance(nodeId, metrics!!)
    }
}

/**
 * 分布式缓存工厂
 */
object DistributedCacheFactory {
    
    /**
     * 创建分布式缓存集群
     */
    fun createCluster(
        nodeIds: List<String>,
        metrics: AiResponseParserMetrics
    ): List<DistributedCache> {
        val caches = mutableListOf<DistributedCache>()
        val networkSimulator = NetworkSimulator("cluster")
        
        for (nodeId in nodeIds) {
            val cache = DistributedCache.getInstance(nodeId, metrics)
            networkSimulator.registerHandler(nodeId, cache)
            caches.add(cache)
        }
        
        return caches
    }
    
    /**
     * 创建单节点分布式缓存
     */
    fun createSingleNode(
        nodeId: String,
        metrics: AiResponseParserMetrics
    ): DistributedCache {
        return DistributedCache.getInstance(nodeId, metrics)
    }
}

/**
 * 分布式缓存扩展函数
 */

/**
 * 安全获取分布式缓存值
 */
suspend inline fun <reified T> DistributedCache.getTyped(key: String): T? {
    val value = get(key)
    return if (value is T) value else null
}

/**
 * 批量获取分布式缓存值
 */
suspend fun DistributedCache.getAll(keys: List<String>): Map<String, Any?> {
    val result = mutableMapOf<String, Any?>()
    
    for (key in keys) {
        val value = get(key)
        if (value != null) {
            result[key] = value
        }
    }
    
    return result
}

/**
 * 批量设置分布式缓存值
 */
suspend fun DistributedCache.putAll(entries: Map<String, Any>) {
    for ((key, value) in entries) {
        put(key, value)
    }
}