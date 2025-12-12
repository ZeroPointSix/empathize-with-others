package com.empathy.ai.data.cache

import com.empathy.ai.data.domain.model.AiResponseParserMetrics
import com.empathy.ai.data.resource.Cache
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * 多级缓存
 * 实现L1(内存) -> L2(磁盘) -> L3(网络)的多级缓存架构
 */
class MultiLevelCache<K, V> private constructor(
    private val name: String,
    private val l1Cache: Cache<K, V>, // 内存缓存
    private val l2Cache: Cache<K, V>?, // 磁盘缓存
    private val l3Cache: Cache<K, V>?, // 网络缓存
    private val metrics: AiResponseParserMetrics
) {
    
    companion object {
        fun <K, V> create(
            name: String,
            l1Cache: Cache<K, V>,
            l2Cache: Cache<K, V>? = null,
            l3Cache: Cache<K, V>? = null,
            metrics: AiResponseParserMetrics
        ): MultiLevelCache<K, V> {
            return MultiLevelCache(name, l1Cache, l2Cache, l3Cache, metrics)
        }
    }
    
    // 缓存统计
    private val l1Hits = AtomicLong(0)
    private val l2Hits = AtomicLong(0)
    private val l3Hits = AtomicLong(0)
    private val misses = AtomicLong(0)
    private val promotions = AtomicLong(0)
    
    // 缓存状态
    private val _cacheState = MutableStateFlow(MultiLevelCacheState())
    val cacheState: StateFlow<MultiLevelCacheState> = _cacheState.asStateFlow()
    
    /**
     * 获取值
     */
    fun get(key: K): V? {
        // 首先尝试L1缓存
        var value = l1Cache.get(key)
        if (value != null) {
            l1Hits.incrementAndGet()
            metrics.recordMultiLevelCacheHit(name, 1)
            updateCacheState()
            return value
        }
        
        // L1未命中，尝试L2缓存
        if (l2Cache != null) {
            value = l2Cache.get(key)
            if (value != null) {
                l2Hits.incrementAndGet()
                metrics.recordMultiLevelCacheHit(name, 2)
                
                // 将值提升到L1缓存
                l1Cache.put(key, value)
                promotions.incrementAndGet()
                metrics.recordMultiLevelCachePromotion(name, 2, 1)
                
                updateCacheState()
                return value
            }
        }
        
        // L2未命中，尝试L3缓存
        if (l3Cache != null) {
            value = l3Cache.get(key)
            if (value != null) {
                l3Hits.incrementAndGet()
                metrics.recordMultiLevelCacheHit(name, 3)
                
                // 将值提升到L2和L1缓存
                l2Cache?.put(key, value)
                l1Cache.put(key, value)
                promotions.incrementAndGet()
                metrics.recordMultiLevelCachePromotion(name, 3, 1)
                if (l2Cache != null) {
                    metrics.recordMultiLevelCachePromotion(name, 3, 2)
                }
                
                updateCacheState()
                return value
            }
        }
        
        // 所有级别都未命中
        misses.incrementAndGet()
        metrics.recordMultiLevelCacheMiss(name)
        updateCacheState()
        return null
    }
    
    /**
     * 设置值
     */
    fun put(key: K, value: V) {
        // 同时写入所有级别的缓存
        l1Cache.put(key, value)
        l2Cache?.put(key, value)
        l3Cache?.put(key, value)
        
        metrics.recordMultiLevelCachePut(name)
        updateCacheState()
    }
    
    /**
     * 设置值并指定过期时间
     */
    fun put(key: K, value: V, expireTime: Long) {
        l1Cache.put(key, value, expireTime)
        l2Cache?.put(key, value, expireTime)
        l3Cache?.put(key, value, expireTime)
        
        metrics.recordMultiLevelCachePut(name)
        updateCacheState()
    }
    
    /**
     * 移除值
     */
    fun remove(key: K): V? {
        // 从所有级别移除
        val value = l1Cache.remove(key)
        l2Cache?.remove(key)
        l3Cache?.remove(key)
        
        metrics.recordMultiLevelCacheRemove(name)
        updateCacheState()
        return value
    }
    
    /**
     * 清空所有级别的缓存
     */
    fun clear() {
        l1Cache.clear()
        l2Cache?.clear()
        l3Cache?.clear()
        
        metrics.recordMultiLevelCacheClear(name)
        updateCacheState()
    }
    
    /**
     * 获取缓存大小（L1缓存的大小）
     */
    fun size(): Int = l1Cache.size()
    
    /**
     * 获取多级缓存统计信息
     */
    fun getStatistics(): MultiLevelCacheStatistics {
        val totalRequests = l1Hits.get() + l2Hits.get() + l3Hits.get() + misses.get()
        val overallHitRate = if (totalRequests > 0) {
            (l1Hits.get() + l2Hits.get() + l3Hits.get()).toFloat() / totalRequests
        } else 0f
        
        val l1HitRate = if (totalRequests > 0) l1Hits.get().toFloat() / totalRequests else 0f
        val l2HitRate = if (totalRequests > 0) l2Hits.get().toFloat() / totalRequests else 0f
        val l3HitRate = if (totalRequests > 0) l3Hits.get().toFloat() / totalRequests else 0f
        
        return MultiLevelCacheStatistics(
            name = name,
            l1Size = l1Cache.size(),
            l2Size = l2Cache?.size() ?: 0,
            l3Size = l3Cache?.size() ?: 0,
            l1Hits = l1Hits.get(),
            l2Hits = l2Hits.get(),
            l3Hits = l3Hits.get(),
            misses = misses.get(),
            promotions = promotions.get(),
            overallHitRate = overallHitRate,
            l1HitRate = l1HitRate,
            l2HitRate = l2HitRate,
            l3HitRate = l3HitRate,
            l1MemoryUsage = l1Cache.getStatistics().memoryUsage,
            l2MemoryUsage = l2Cache?.getStatistics()?.memoryUsage ?: 0L,
            l3MemoryUsage = l3Cache?.getStatistics()?.memoryUsage ?: 0L,
            totalMemoryUsage = l1Cache.getStatistics().memoryUsage +
                    (l2Cache?.getStatistics()?.memoryUsage ?: 0L) +
                    (l3Cache?.getStatistics()?.memoryUsage ?: 0L)
        )
    }
    
    /**
     * 更新缓存状态
     */
    private fun updateCacheState() {
        val stats = getStatistics()
        val currentState = _cacheState.value.copy(
            l1Size = stats.l1Size,
            l2Size = stats.l2Size,
            l3Size = stats.l3Size,
            overallHitRate = stats.overallHitRate,
            totalMemoryUsage = stats.totalMemoryUsage,
            lastUpdateTime = System.currentTimeMillis()
        )
        _cacheState.value = currentState
    }
    
    /**
     * 预热缓存
     */
    suspend fun warmUp(keys: List<K>, valueProvider: suspend (K) -> V?) {
        for (key in keys) {
            // 检查是否已经在缓存中
            if (get(key) == null) {
                // 从数据源获取值
                val value = valueProvider(key)
                if (value != null) {
                    put(key, value)
                    metrics.recordMultiLevelCacheWarmUp(name)
                }
            }
        }
    }
    
    /**
     * 批量获取
     */
    fun getAll(keys: Collection<K>): Map<K, V> {
        val result = mutableMapOf<K, V>()
        
        for (key in keys) {
            val value = get(key)
            if (value != null) {
                result[key] = value
            }
        }
        
        return result
    }
    
    /**
     * 批量设置
     */
    fun putAll(entries: Map<K, V>) {
        for ((key, value) in entries) {
            put(key, value)
        }
    }
    
    /**
     * 批量设置并指定过期时间
     */
    fun putAll(entries: Map<K, V>, expireTime: Long) {
        for ((key, value) in entries) {
            put(key, value, expireTime)
        }
    }
    
    /**
     * 获取缓存状态
     */
    fun getCacheState(): MultiLevelCacheState {
        return _cacheState.value
    }
}

/**
 * 多级缓存状态
 */
data class MultiLevelCacheState(
    val l1Size: Int = 0,
    val l2Size: Int = 0,
    val l3Size: Int = 0,
    val overallHitRate: Float = 0f,
    val totalMemoryUsage: Long = 0L,
    val lastUpdateTime: Long = 0L
)

/**
 * 多级缓存统计信息
 */
data class MultiLevelCacheStatistics(
    val name: String = "",
    val l1Size: Int = 0,
    val l2Size: Int = 0,
    val l3Size: Int = 0,
    val l1Hits: Long = 0L,
    val l2Hits: Long = 0L,
    val l3Hits: Long = 0L,
    val misses: Long = 0L,
    val promotions: Long = 0L,
    val overallHitRate: Float = 0f,
    val l1HitRate: Float = 0f,
    val l2HitRate: Float = 0f,
    val l3HitRate: Float = 0f,
    val l1MemoryUsage: Long = 0L,
    val l2MemoryUsage: Long = 0L,
    val l3MemoryUsage: Long = 0L,
    val totalMemoryUsage: Long = 0L
)

/**
 * 多级缓存构建器
 */
class MultiLevelCacheBuilder<K, V> {
    private var name: String = "default"
    private var l1Cache: Cache<K, V>? = null
    private var l2Cache: Cache<K, V>? = null
    private var l3Cache: Cache<K, V>? = null
    private var metrics: AiResponseParserMetrics? = null
    
    fun name(name: String) = apply { this.name = name }
    
    fun l1Cache(cache: Cache<K, V>) = apply { this.l1Cache = cache }
    
    fun l2Cache(cache: Cache<K, V>) = apply { this.l2Cache = cache }
    
    fun l3Cache(cache: Cache<K, V>) = apply { this.l3Cache = cache }
    
    fun metrics(metrics: AiResponseParserMetrics) = apply { this.metrics = metrics }
    
    fun build(): MultiLevelCache<K, V> {
        require(l1Cache != null) { "L1缓存是必需的" }
        require(metrics != null) { "指标收集器是必需的" }
        
        return MultiLevelCache.create(
            name = name,
            l1Cache = l1Cache!!,
            l2Cache = l2Cache,
            l3Cache = l3Cache,
            metrics = metrics!!
        )
    }
}

/**
 * 多级缓存工厂
 */
object MultiLevelCacheFactory {
    
    /**
     * 创建简单的两级缓存（内存 + 磁盘）
     */
    fun <K, V> createTwoLevelCache(
        name: String,
        l1Cache: Cache<K, V>,
        l2Cache: Cache<K, V>,
        metrics: AiResponseParserMetrics
    ): MultiLevelCache<K, V> {
        return MultiLevelCache.create(name, l1Cache, l2Cache, null, metrics)
    }
    
    /**
     * 创建完整的三级缓存（内存 + 磁盘 + 网络）
     */
    fun <K, V> createThreeLevelCache(
        name: String,
        l1Cache: Cache<K, V>,
        l2Cache: Cache<K, V>,
        l3Cache: Cache<K, V>,
        metrics: AiResponseParserMetrics
    ): MultiLevelCache<K, V> {
        return MultiLevelCache.create(name, l1Cache, l2Cache, l3Cache, metrics)
    }
    
    /**
     * 创建仅内存缓存
     */
    fun <K, V> createMemoryOnlyCache(
        name: String,
        l1Cache: Cache<K, V>,
        metrics: AiResponseParserMetrics
    ): MultiLevelCache<K, V> {
        return MultiLevelCache.create(name, l1Cache, null, null, metrics)
    }
}

/**
 * 缓存级别枚举
 */
enum class CacheLevel {
    L1, // 内存缓存
    L2, // 磁盘缓存
    L3  // 网络缓存
}

/**
 * 缓存策略配置
 */
data class MultiLevelCacheConfig(
    val enableL1: Boolean = true,
    val enableL2: Boolean = true,
    val enableL3: Boolean = false,
    val l1MaxSize: Int = 100,
    val l2MaxSize: Int = 1000,
    val l3MaxSize: Int = 10000,
    val l1ExpireTime: Long = 300000L, // 5分钟
    val l2ExpireTime: Long = 1800000L, // 30分钟
    val l3ExpireTime: Long = 7200000L, // 2小时
    val enablePromotion: Boolean = true,
    val promotionThreshold: Int = 3 // 访问次数阈值
)

/**
 * 缓存扩展函数
 */

/**
 * 安全获取多级缓存值，如果不存在则通过工厂函数创建
 */
suspend inline fun <K, V> MultiLevelCache<K, V>.getOrPut(
    key: K,
    crossinline defaultValue: suspend (K) -> V?
): V? {
    return get(key) ?: defaultValue(key)?.also { put(key, it) }
}

/**
 * 批量安全获取多级缓存值
 */
suspend inline fun <K, V> MultiLevelCache<K, V>.getAllOrPut(
    keys: Collection<K>,
    crossinline defaultValue: suspend (K) -> V?
): Map<K, V> {
    val result = mutableMapOf<K, V>()
    val missingKeys = mutableListOf<K>()
    
    // 首先尝试从缓存获取
    for (key in keys) {
        val value = get(key)
        if (value != null) {
            result[key] = value
        } else {
            missingKeys.add(key)
        }
    }
    
    // 为缺失的键获取值
    for (key in missingKeys) {
        val value = defaultValue(key)
        if (value != null) {
            result[key] = value
            put(key, value)
        }
    }
    
    return result
}

/**
 * 带统计的多级缓存装饰器
 */
class StatsMultiLevelCacheDecorator<K, V>(
    private val cache: MultiLevelCache<K, V>,
    private val metrics: AiResponseParserMetrics
) : MultiLevelCache<K, V> by cache {
    
    override fun get(key: K): V? {
        val startTime = System.nanoTime()
        val result = cache.get(key)
        val duration = System.nanoTime() - startTime
        
        metrics.recordMultiLevelCacheAccess(cache.name, duration, result != null)
        return result
    }
    
    override fun put(key: K, value: V) {
        val startTime = System.nanoTime()
        cache.put(key, value)
        val duration = System.nanoTime() - startTime
        
        metrics.recordMultiLevelCachePut(cache.name, duration)
    }
}