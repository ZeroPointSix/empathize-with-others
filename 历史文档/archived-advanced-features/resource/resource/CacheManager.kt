package com.empathy.ai.data.resource

import android.content.Context
import com.empathy.ai.data.domain.model.AiResponseParserMetrics
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

/**
 * 缓存管理器
 * 统一管理各种缓存策略
 */
class CacheManager private constructor(
    private val context: Context,
    private val metrics: AiResponseParserMetrics
) {
    companion object {
        @Volatile
        private var INSTANCE: CacheManager? = null
        
        fun getInstance(context: Context, metrics: AiResponseParserMetrics): CacheManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: CacheManager(context.applicationContext, metrics).also { INSTANCE = it }
            }
        }
        
        private const val CLEANUP_INTERVAL = 60000L // 1分钟
        private const val DEFAULT_CACHE_SIZE = 100
        private const val DEFAULT_EXPIRE_TIME = 300000L // 5分钟
    }
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // 缓存注册表
    private val cacheRegistry = ConcurrentHashMap<String, CacheEntry<*>>()
    
    // 缓存状态
    private val _cacheState = MutableStateFlow(CacheState())
    val cacheState: StateFlow<CacheState> = _cacheState.asStateFlow()
    
    // 缓存统计
    private val cacheStatistics = ConcurrentHashMap<String, CacheStatistics>()
    
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
                    cleanupExpiredEntries()
                    updateCacheState()
                    delay(CLEANUP_INTERVAL)
                } catch (e: Exception) {
                    metrics.recordCacheError("缓存清理错误", e)
                }
            }
        }
    }
    
    /**
     * 创建缓存
     */
    fun <K, V> createCache(
        name: String,
        config: CacheConfig = CacheConfig()
    ): Cache<K, V> {
        val cache = CacheImpl(name, config, metrics)
        cacheRegistry[name] = cache as CacheEntry<*>
        cacheStatistics[name] = CacheStatistics(name)
        
        updateCacheState()
        return cache
    }
    
    /**
     * 获取缓存
     */
    @Suppress("UNCHECKED_CAST")
    fun <K, V> getCache(name: String): Cache<K, V>? {
        return cacheRegistry[name] as? Cache<K, V>
    }
    
    /**
     * 销毁缓存
     */
    fun destroyCache(name: String) {
        cacheRegistry.remove(name)?.clear()
        cacheStatistics.remove(name)
        updateCacheState()
    }
    
    /**
     * 清理过期条目
     */
    private fun cleanupExpiredEntries() {
        val currentTime = System.currentTimeMillis()
        
        for (cache in cacheRegistry.values) {
            cache.cleanupExpiredEntries(currentTime)
        }
    }
    
    /**
     * 更新缓存状态
     */
    private fun updateCacheState() {
        val totalCaches = cacheRegistry.size
        val totalEntries = cacheRegistry.values.sumOf { it.size() }
        val totalMemoryUsage = cacheRegistry.values.sumOf { it.estimateMemoryUsage() }
        
        val currentState = _cacheState.value.copy(
            totalCaches = totalCaches,
            totalEntries = totalEntries,
            totalMemoryUsage = totalMemoryUsage,
            lastUpdateTime = System.currentTimeMillis()
        )
        _cacheState.value = currentState
    }
    
    /**
     * 获取所有缓存统计信息
     */
    fun getAllCacheStatistics(): List<CacheStatistics> {
        return cacheStatistics.values.toList()
    }
    
    /**
     * 获取缓存状态
     */
    fun getCacheState(): CacheState {
        return _cacheState.value
    }
    
    /**
     * 清空所有缓存
     */
    fun clearAllCaches() {
        for (cache in cacheRegistry.values) {
            cache.clear()
        }
        updateCacheState()
    }
    
    /**
     * 销毁所有缓存
     */
    fun destroyAll() {
        cleanupJob?.cancel()
        scope.cancel()
        
        for (cache in cacheRegistry.values) {
            cache.clear()
        }
        
        cacheRegistry.clear()
        cacheStatistics.clear()
        
        INSTANCE = null
    }
}

/**
 * 缓存接口
 */
interface Cache<K, V> {
    val name: String
    val config: CacheConfig
    
    /**
     * 获取值
     */
    fun get(key: K): V?
    
    /**
     * 设置值
     */
    fun put(key: K, value: V)
    
    /**
     * 设置值并指定过期时间
     */
    fun put(key: K, value: V, expireTime: Long)
    
    /**
     * 移除值
     */
    fun remove(key: K): V?
    
    /**
     * 清空缓存
     */
    fun clear()
    
    /**
     * 获取缓存大小
     */
    fun size(): Int
    
    /**
     * 获取缓存统计信息
     */
    fun getStatistics(): CacheStatistics
    
    /**
     * 清理过期条目
     */
    fun cleanupExpiredEntries(currentTime: Long)
}

/**
 * 缓存实现
 */
internal class CacheImpl<K, V>(
    override val name: String,
    override val config: CacheConfig,
    private val metrics: AiResponseParserMetrics
) : Cache<K, V>, CacheEntry<V> {
    
    private val cache = ConcurrentHashMap<K, CacheEntry<V>>()
    private val accessOrder = mutableListOf<K>()
    private val hitCount = AtomicLong(0)
    private val missCount = AtomicLong(0)
    private val putCount = AtomicLong(0)
    private val evictionCount = AtomicLong(0)
    
    override fun get(key: K): V? {
        val entry = cache[key]
        return if (entry != null && !entry.isExpired()) {
            hitCount.incrementAndGet()
            updateAccessOrder(key)
            metrics.recordCacheHit(name)
            entry.value
        } else {
            if (entry != null && entry.isExpired()) {
                cache.remove(key)
                accessOrder.remove(key)
            }
            missCount.incrementAndGet()
            metrics.recordCacheMiss(name)
            null
        }
    }
    
    override fun put(key: K, value: V) {
        put(key, value, System.currentTimeMillis() + config.defaultExpireTime)
    }
    
    override fun put(key: K, value: V, expireTime: Long) {
        // 检查容量限制
        if (cache.size >= config.maxSize && !cache.containsKey(key)) {
            evictEntries()
        }
        
        val entry = CacheEntry(value, expireTime)
        cache[key] = entry
        updateAccessOrder(key)
        putCount.incrementAndGet()
        
        metrics.recordCachePut(name)
    }
    
    override fun remove(key: K): V? {
        val entry = cache.remove(key)
        accessOrder.remove(key)
        
        return if (entry != null && !entry.isExpired()) {
            entry.value
        } else {
            null
        }
    }
    
    override fun clear() {
        cache.clear()
        accessOrder.clear()
        metrics.recordCacheClear(name)
    }
    
    override fun size(): Int = cache.size
    
    override fun getStatistics(): CacheStatistics {
        val totalRequests = hitCount.get() + missCount.get()
        val hitRate = if (totalRequests > 0) hitCount.get().toFloat() / totalRequests else 0f
        
        return CacheStatistics(
            name = name,
            size = cache.size,
            maxSize = config.maxSize,
            hitCount = hitCount.get(),
            missCount = missCount.get(),
            putCount = putCount.get(),
            evictionCount = evictionCount.get(),
            hitRate = hitRate,
            memoryUsage = estimateMemoryUsage(),
            lastUpdateTime = System.currentTimeMillis()
        )
    }
    
    override fun cleanupExpiredEntries(currentTime: Long) {
        val toRemove = mutableListOf<K>()
        
        for ((key, entry) in cache) {
            if (entry.isExpired(currentTime)) {
                toRemove.add(key)
            }
        }
        
        for (key in toRemove) {
            cache.remove(key)
            accessOrder.remove(key)
        }
    }
    
    override fun estimateMemoryUsage(): Long {
        // 简单的内存使用估算
        return cache.size * 100L // 假设每个条目平均占用100字节
    }
    
    private fun updateAccessOrder(key: K) {
        accessOrder.remove(key)
        accessOrder.add(key)
    }
    
    private fun evictEntries() {
        when (config.evictionPolicy) {
            EvictionPolicy.LRU -> evictLRU()
            EvictionPolicy.LFU -> evictLFU()
            EvictionPolicy.FIFO -> evictFIFO()
        }
    }
    
    private fun evictLRU() {
        if (accessOrder.isNotEmpty()) {
            val keyToRemove = accessOrder.removeAt(0)
            cache.remove(keyToRemove)
            evictionCount.incrementAndGet()
        }
    }
    
    private fun evictLFU() {
        // 简化实现，移除最旧的条目
        evictFIFO()
    }
    
    private fun evictFIFO() {
        if (accessOrder.isNotEmpty()) {
            val keyToRemove = accessOrder.removeAt(0)
            cache.remove(keyToRemove)
            evictionCount.incrementAndGet()
        }
    }
}

/**
 * 缓存条目
 */
internal data class CacheEntry<V>(
    val value: V,
    val expireTime: Long
) {
    fun isExpired(currentTime: Long = System.currentTimeMillis()): Boolean {
        return expireTime > 0 && currentTime >= expireTime
    }
}

/**
 * 缓存配置
 */
data class CacheConfig(
    val maxSize: Int = DEFAULT_CACHE_SIZE,
    val defaultExpireTime: Long = DEFAULT_EXPIRE_TIME,
    val evictionPolicy: EvictionPolicy = EvictionPolicy.LRU
) {
    init {
        require(maxSize > 0) { "缓存大小必须为正数" }
        require(defaultExpireTime >= 0) { "过期时间不能为负数" }
    }
}

/**
 * 淘汰策略
 */
enum class EvictionPolicy {
    LRU,  // 最近最少使用
    LFU,  // 最少使用频率
    FIFO  // 先进先出
}

/**
 * 缓存状态
 */
data class CacheState(
    val totalCaches: Int = 0,
    val totalEntries: Int = 0,
    val totalMemoryUsage: Long = 0L,
    val lastUpdateTime: Long = 0L
)

/**
 * 缓存统计信息
 */
data class CacheStatistics(
    val name: String = "",
    val size: Int = 0,
    val maxSize: Int = 0,
    val hitCount: Long = 0L,
    val missCount: Long = 0L,
    val putCount: Long = 0L,
    val evictionCount: Long = 0L,
    val hitRate: Float = 0f,
    val memoryUsage: Long = 0L,
    val lastUpdateTime: Long = 0L
) {
    val utilizationRate: Float
        get() = if (maxSize > 0) size.toFloat() / maxSize.toFloat() else 0f
}

/**
 * 缓存条目接口
 */
internal interface CacheEntry<V> {
    fun estimateMemoryUsage(): Long
    fun clear()
}

/**
 * 缓存扩展函数
 */

/**
 * 安全获取缓存值，如果不存在则通过工厂函数创建
 */
inline fun <K, V> Cache<K, V>.getOrPut(key: K, defaultValue: () -> V): V {
    return get(key) ?: defaultValue().also { put(key, it) }
}

/**
 * 批量获取缓存值
 */
fun <K, V> Cache<K, V>.getAll(keys: Collection<K>): Map<K, V> {
    return keys.associateWith { key -> 
        get(key) 
    }.filterValues { it != null } as Map<K, V>
}

/**
 * 批量设置缓存值
 */
fun <K, V> Cache<K, V>.putAll(entries: Map<K, V>) {
    for ((key, value) in entries) {
        put(key, value)
    }
}

/**
 * 带过期时间的批量设置
 */
fun <K, V> Cache<K, V>.putAll(entries: Map<K, V>, expireTime: Long) {
    for ((key, value) in entries) {
        put(key, value, expireTime)
    }
}

/**
 * 缓存装饰器
 */

/**
 * 带统计功能的缓存装饰器
 */
class StatsCacheDecorator<K, V>(
    private val cache: Cache<K, V>,
    private val metrics: AiResponseParserMetrics
) : Cache<K, V> {
    override val name: String = cache.name
    override val config: CacheConfig = cache.config
    
    override fun get(key: K): V? {
        val startTime = System.nanoTime()
        val result = cache.get(key)
        val duration = System.nanoTime() - startTime
        
        metrics.recordCacheAccess(name, duration, result != null)
        return result
    }
    
    override fun put(key: K, value: V) {
        val startTime = System.nanoTime()
        cache.put(key, value)
        val duration = System.nanoTime() - startTime
        
        metrics.recordCachePut(name, duration)
    }
    
    override fun put(key: K, value: V, expireTime: Long) {
        val startTime = System.nanoTime()
        cache.put(key, value, expireTime)
        val duration = System.nanoTime() - startTime
        
        metrics.recordCachePut(name, duration)
    }
    
    override fun remove(key: K): V? {
        return cache.remove(key)
    }
    
    override fun clear() {
        cache.clear()
    }
    
    override fun size(): Int {
        return cache.size()
    }
    
    override fun getStatistics(): CacheStatistics {
        return cache.getStatistics()
    }
    
    override fun cleanupExpiredEntries(currentTime: Long) {
        cache.cleanupExpiredEntries(currentTime)
    }
}

/**
 * 带日志功能的缓存装饰器
 */
class LoggingCacheDecorator<K, V>(
    private val cache: Cache<K, V>,
    private val tag: String = "Cache"
) : Cache<K, V> {
    override val name: String = cache.name
    override val config: CacheConfig = cache.config
    
    override fun get(key: K): V? {
        val result = cache.get(key)
        // 这里可以添加日志记录
        return result
    }
    
    override fun put(key: K, value: V) {
        cache.put(key, value)
        // 这里可以添加日志记录
    }
    
    override fun put(key: K, value: V, expireTime: Long) {
        cache.put(key, value, expireTime)
        // 这里可以添加日志记录
    }
    
    override fun remove(key: K): V? {
        return cache.remove(key)
    }
    
    override fun clear() {
        cache.clear()
        // 这里可以添加日志记录
    }
    
    override fun size(): Int {
        return cache.size()
    }
    
    override fun getStatistics(): CacheStatistics {
        return cache.getStatistics()
    }
    
    override fun cleanupExpiredEntries(currentTime: Long) {
        cache.cleanupExpiredEntries(currentTime)
    }
}

/**
 * 缓存工厂
 */
object CacheFactory {
    
    /**
     * 创建LRU缓存
     */
    fun <K, V> createLRUCache(
        name: String,
        maxSize: Int = DEFAULT_CACHE_SIZE,
        expireTime: Long = DEFAULT_EXPIRE_TIME,
        metrics: AiResponseParserMetrics
    ): Cache<K, V> {
        val config = CacheConfig(
            maxSize = maxSize,
            defaultExpireTime = expireTime,
            evictionPolicy = EvictionPolicy.LRU
        )
        return CacheImpl(name, config, metrics)
    }
    
    /**
     * 创建FIFO缓存
     */
    fun <K, V> createFIFOCache(
        name: String,
        maxSize: Int = DEFAULT_CACHE_SIZE,
        expireTime: Long = DEFAULT_EXPIRE_TIME,
        metrics: AiResponseParserMetrics
    ): Cache<K, V> {
        val config = CacheConfig(
            maxSize = maxSize,
            defaultExpireTime = expireTime,
            evictionPolicy = EvictionPolicy.FIFO
        )
        return CacheImpl(name, config, metrics)
    }
    
    /**
     * 创建带统计的缓存
     */
    fun <K, V> createStatsCache(
        cache: Cache<K, V>,
        metrics: AiResponseParserMetrics
    ): Cache<K, V> {
        return StatsCacheDecorator(cache, metrics)
    }
    
    /**
     * 创建带日志的缓存
     */
    fun <K, V> createLoggingCache(
        cache: Cache<K, V>,
        tag: String = "Cache"
    ): Cache<K, V> {
        return LoggingCacheDecorator(cache, tag)
    }
}