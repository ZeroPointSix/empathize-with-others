package com.empathy.ai.data.resource

import com.empathy.ai.data.domain.model.AiResponseParserMetrics
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.PriorityBlockingQueue
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

/**
 * 资源池管理器
 * 实现对象池化以减少内存分配开销
 */
class ResourcePool private constructor(
    private val metrics: AiResponseParserMetrics
) {
    companion object {
        @Volatile
        private var INSTANCE: ResourcePool? = null
        
        fun getInstance(metrics: AiResponseParserMetrics): ResourcePool {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ResourcePool(metrics).also { INSTANCE = it }
            }
        }
        
        private const val DEFAULT_POOL_SIZE = 10
        private const val MAX_POOL_SIZE = 50
        private const val CLEANUP_INTERVAL = 30000L // 30秒
        private const val IDLE_TIMEOUT = 60000L // 1分钟
    }
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // 资源池注册表
    private val resourcePools = ConcurrentHashMap<String, ResourcePoolEntry<*>>()
    
    // 池状态
    private val _poolState = MutableStateFlow(PoolState())
    val poolState: StateFlow<PoolState> = _poolState.asStateFlow()
    
    // 资源统计
    private val resourceStats = ConcurrentHashMap<String, ResourceStatistics>()
    
    // 清理任务
    private var cleanupJob: Job? = null
    
    init {
        startCleanupTask()
    }
    
    /**
     * 启动清理任务
     */
    private fun startCleanupTask() {
        cleanupJob = scope.launch {
            while (isActive) {
                try {
                    cleanupIdleResources()
                    delay(CLEANUP_INTERVAL)
                } catch (e: Exception) {
                    metrics.recordResourcePoolError("资源池清理错误", e)
                }
            }
        }
    }
    
    /**
     * 创建资源池
     */
    fun <T : Any> createPool(
        name: String,
        factory: ResourceFactory<T>,
        config: PoolConfig = PoolConfig()
    ): ResourcePool<T> {
        val pool = ResourcePoolImpl(name, factory, config, metrics)
        resourcePools[name] = pool as ResourcePoolEntry<*>
        resourceStats[name] = ResourceStatistics(name)
        
        updatePoolState()
        return pool
    }
    
    /**
     * 获取资源池
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getPool(name: String): ResourcePool<T>? {
        return resourcePools[name] as? ResourcePool<T>
    }
    
    /**
     * 销毁资源池
     */
    fun destroyPool(name: String) {
        resourcePools.remove(name)?.destroy()
        resourceStats.remove(name)
        updatePoolState()
    }
    
    /**
     * 清理空闲资源
     */
    private fun cleanupIdleResources() {
        val currentTime = System.currentTimeMillis()
        
        for (pool in resourcePools.values) {
            pool.cleanupIdleResources(currentTime, IDLE_TIMEOUT)
        }
        
        updatePoolState()
    }
    
    /**
     * 更新池状态
     */
    private fun updatePoolState() {
        val totalPools = resourcePools.size
        val totalResources = resourcePools.values.sumOf { it.totalResources() }
        val totalIdleResources = resourcePools.values.sumOf { it.idleResources() }
        val totalActiveResources = totalResources - totalIdleResources
        
        val currentState = _poolState.value.copy(
            totalPools = totalPools,
            totalResources = totalResources,
            idleResources = totalIdleResources,
            activeResources = totalActiveResources,
            lastUpdateTime = System.currentTimeMillis()
        )
        _poolState.value = currentState
    }
    
    /**
     * 获取所有资源池统计信息
     */
    fun getAllPoolStatistics(): List<ResourceStatistics> {
        return resourceStats.values.toList()
    }
    
    /**
     * 获取资源池状态
     */
    fun getPoolState(): PoolState {
        return _poolState.value
    }
    
    /**
     * 销毁所有资源池
     */
    fun destroyAll() {
        cleanupJob?.cancel()
        scope.cancel()
        
        for (pool in resourcePools.values) {
            pool.destroy()
        }
        
        resourcePools.clear()
        resourceStats.clear()
        
        INSTANCE = null
    }
}

/**
 * 资源池接口
 */
interface ResourcePool<T : Any> {
    val name: String
    val config: PoolConfig
    
    /**
     * 借用资源
     */
    suspend fun borrow(): T
    
    /**
     * 归还资源
     */
    suspend fun release(resource: T)
    
    /**
     * 获取池统计信息
     */
    fun getStatistics(): ResourceStatistics
    
    /**
     * 清理空闲资源
     */
    fun cleanupIdleResources(currentTime: Long, idleTimeout: Long)
    
    /**
     * 销毁池
     */
    fun destroy()
}

/**
 * 资源池实现
 */
internal class ResourcePoolImpl<T : Any>(
    override val name: String,
    private val factory: ResourceFactory<T>,
    override val config: PoolConfig,
    private val metrics: AiResponseParserMetrics
) : ResourcePool<T>, ResourcePoolEntry<T> {
    
    private val mutex = Mutex()
    private val idleResources = PriorityBlockingQueue<PooledResource<T>>(config.maxSize)
    private val activeResources = ConcurrentHashMap<T, PooledResource<T>>()
    private val totalCreated = AtomicInteger(0)
    private val totalBorrowed = AtomicLong(0)
    private val totalReturned = AtomicLong(0)
    private val currentSize = AtomicInteger(0)
    
    override suspend fun borrow(): T {
        return mutex.withLock {
            val resource = tryGetFromPool() ?: createNewResource()
            
            activeResources[resource] = PooledResource(resource, System.currentTimeMillis())
            totalBorrowed.incrementAndGet()
            
            metrics.recordResourcePoolBorrow(name, currentSize.get())
            
            resource
        }
    }
    
    override suspend fun release(resource: T) {
        mutex.withLock {
            val pooledResource = activeResources.remove(resource)
            if (pooledResource != null) {
                if (factory.validate(resource)) {
                    if (factory.reset(resource)) {
                        idleResources.offer(pooledResource)
                        totalReturned.incrementAndGet()
                    } else {
                        // 重置失败，销毁资源
                        factory.destroy(resource)
                        currentSize.decrementAndGet()
                    }
                } else {
                    // 验证失败，销毁资源
                    factory.destroy(resource)
                    currentSize.decrementAndGet()
                }
                
                metrics.recordResourcePoolRelease(name, currentSize.get())
            }
        }
    }
    
    override fun getStatistics(): ResourceStatistics {
        return ResourceStatistics(
            name = name,
            totalCreated = totalCreated.get(),
            totalBorrowed = totalBorrowed.get(),
            totalReturned = totalReturned.get(),
            currentSize = currentSize.get(),
            idleSize = idleResources.size,
            activeSize = activeResources.size,
            maxPoolSize = config.maxSize,
            minPoolSize = config.minSize,
            lastUpdateTime = System.currentTimeMillis()
        )
    }
    
    override fun cleanupIdleResources(currentTime: Long, idleTimeout: Long) {
        val toRemove = mutableListOf<PooledResource<T>>()
        
        // 收集需要清理的资源
        for (resource in idleResources) {
            if (currentTime - resource.lastUsedTime > idleTimeout && currentSize.get() > config.minSize) {
                toRemove.add(resource)
            }
        }
        
        // 清理资源
        for (resource in toRemove) {
            if (idleResources.remove(resource)) {
                factory.destroy(resource.resource)
                currentSize.decrementAndGet()
            }
        }
    }
    
    override fun destroy() {
        // 清理所有资源
        for (resource in idleResources) {
            factory.destroy(resource.resource)
        }
        idleResources.clear()
        
        for (resource in activeResources.values) {
            factory.destroy(resource.resource)
        }
        activeResources.clear()
        
        currentSize.set(0)
    }
    
    override fun totalResources(): Int = currentSize.get()
    
    override fun idleResources(): Int = idleResources.size
    
    private fun tryGetFromPool(): T? {
        while (true) {
            val pooledResource = idleResources.poll()
            if (pooledResource == null) return null
            
            val resource = pooledResource.resource
            if (factory.validate(resource)) {
                return resource
            } else {
                // 资源无效，销毁并继续尝试
                factory.destroy(resource)
                currentSize.decrementAndGet()
            }
        }
    }
    
    private fun createNewResource(): T {
        if (currentSize.get() >= config.maxSize) {
            throw IllegalStateException("资源池已达到最大容量: ${config.maxSize}")
        }
        
        val resource = factory.create()
        currentSize.incrementAndGet()
        totalCreated.incrementAndGet()
        
        return resource
    }
}

/**
 * 资源工厂接口
 */
interface ResourceFactory<T : Any> {
    /**
     * 创建新资源
     */
    fun create(): T
    
    /**
     * 验证资源是否有效
     */
    fun validate(resource: T): Boolean
    
    /**
     * 重置资源状态
     */
    fun reset(resource: T): Boolean
    
    /**
     * 销毁资源
     */
    fun destroy(resource: T)
}

/**
 * 资源池配置
 */
data class PoolConfig(
    val minSize: Int = 2,
    val maxSize: Int = DEFAULT_POOL_SIZE,
    val createTimeout: Long = 5000L,
    val borrowTimeout: Long = 3000L
) {
    init {
        require(minSize >= 0) { "最小池大小不能为负数" }
        require(maxSize > minSize) { "最大池大小必须大于最小池大小" }
        require(createTimeout > 0) { "创建超时时间必须为正数" }
        require(borrowTimeout > 0) { "借用超时时间必须为正数" }
    }
}

/**
 * 池化资源
 */
internal data class PooledResource<T : Any>(
    val resource: T,
    val lastUsedTime: Long
) : Comparable<PooledResource<T>> {
    override fun compareTo(other: PooledResource<T>): Int {
        return this.lastUsedTime.compareTo(other.lastUsedTime)
    }
}

/**
 * 资源池条目
 */
internal interface ResourcePoolEntry<T : Any> {
    fun totalResources(): Int
    fun idleResources(): Int
    fun destroy()
}

/**
 * 池状态
 */
data class PoolState(
    val totalPools: Int = 0,
    val totalResources: Int = 0,
    val idleResources: Int = 0,
    val activeResources: Int = 0,
    val lastUpdateTime: Long = 0L
)

/**
 * 资源统计信息
 */
data class ResourceStatistics(
    val name: String = "",
    val totalCreated: Int = 0,
    val totalBorrowed: Long = 0L,
    val totalReturned: Long = 0L,
    val currentSize: Int = 0,
    val idleSize: Int = 0,
    val activeSize: Int = 0,
    val maxPoolSize: Int = 0,
    val minPoolSize: Int = 0,
    val lastUpdateTime: Long = 0L
) {
    val utilizationRate: Float
        get() = if (maxPoolSize > 0) currentSize.toFloat() / maxPoolSize.toFloat() else 0f
    
    val hitRate: Float
        get() = if (totalBorrowed > 0) (totalBorrowed - totalCreated).toFloat() / totalBorrowed.toFloat() else 0f
}

/**
 * 资源池扩展函数
 */
inline fun <T : Any, R> ResourcePool<T>.use(block: (T) -> R): R {
    return runBlocking {
        val resource = borrow()
        try {
            block(resource)
        } finally {
            release(resource)
        }
    }
}

/**
 * 异步资源池使用
 */
suspend fun <T : Any, R> ResourcePool<T>.useSuspend(block: suspend (T) -> R): R {
    val resource = borrow()
    try {
        return block(resource)
    } finally {
        release(resource)
    }
}

/**
 * 常见资源工厂实现
 */

/**
 * StringBuilder资源工厂
 */
class StringBuilderResourceFactory : ResourceFactory<StringBuilder> {
    override fun create(): StringBuilder = StringBuilder(1024)
    
    override fun validate(resource: StringBuilder): Boolean = true
    
    override fun reset(resource: StringBuilder): Boolean {
        resource.clear()
        return true
    }
    
    override fun destroy(resource: StringBuilder) {
        // StringBuilder不需要特殊销毁
    }
}

/**
 * JSON解析器资源工厂
 */
class JsonParserResourceFactory : ResourceFactory<JsonParserResource> {
    override fun create(): JsonParserResource = JsonParserResource()
    
    override fun validate(resource: JsonParserResource): Boolean = resource.isValid()
    
    override fun reset(resource: JsonParserResource): Boolean {
        return resource.reset()
    }
    
    override fun destroy(resource: JsonParserResource) {
        resource.destroy()
    }
}

/**
 * JSON解析器资源
 */
class JsonParserResource {
    private var parser: Any? = null // 实际的JSON解析器实例
    private var isValid = true
    
    fun isValid(): Boolean = isValid
    
    fun reset(): Boolean {
        try {
            // 重置解析器状态
            isValid = true
            return true
        } catch (e: Exception) {
            isValid = false
            return false
        }
    }
    
    fun destroy() {
        parser = null
        isValid = false
    }
    
    fun getParser(): Any? {
        return if (isValid) parser else null
    }
}

/**
 * 字符串缓冲区资源工厂
 */
class StringBufferResourceFactory : ResourceFactory<StringBuffer> {
    override fun create(): StringBuffer = StringBuffer(1024)
    
    override fun validate(resource: StringBuffer): Boolean = true
    
    override fun reset(resource: StringBuffer): Boolean {
        resource.setLength(0)
        return true
    }
    
    override fun destroy(resource: StringBuffer) {
        // StringBuffer不需要特殊销毁
    }
}