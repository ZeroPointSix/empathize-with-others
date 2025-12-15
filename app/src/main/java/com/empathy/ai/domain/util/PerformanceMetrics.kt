package com.empathy.ai.domain.util

/**
 * 性能指标配置
 *
 * 定义应用的性能要求和阈值
 *
 * 设计原则：
 * - 用户体验优先：确保流畅的交互体验
 * - 资源效率：合理使用内存和CPU
 * - 可监控：提供清晰的性能指标
 */
object PerformanceMetrics {
    
    // ========== 内存限制 ==========
    
    /**
     * 最大内存使用量（MB）
     * 超过此值应触发内存清理
     */
    const val MAX_MEMORY_MB = 200
    
    /**
     * 内存警告阈值（MB）
     * 达到此值时记录警告日志
     */
    const val MEMORY_WARNING_MB = 150
    
    /**
     * 低内存阈值（MB）
     * 低于此值时可能影响性能
     */
    const val LOW_MEMORY_MB = 50
    
    // ========== 帧率要求 ==========
    
    /**
     * 目标帧率（fps）
     */
    const val TARGET_FPS = 60
    
    /**
     * 最低可接受帧率（fps）
     */
    const val MIN_ACCEPTABLE_FPS = 30
    
    /**
     * 单帧最大时间（毫秒）
     * 60fps = 16.67ms/帧
     */
    const val MAX_FRAME_TIME_MS = 16L
    
    /**
     * 帧时间警告阈值（毫秒）
     */
    const val FRAME_TIME_WARNING_MS = 32L
    
    // ========== 加载时间 ==========
    
    /**
     * 页面加载最大时间（毫秒）
     */
    const val MAX_PAGE_LOAD_TIME_MS = 1000L
    
    /**
     * 数据加载最大时间（毫秒）
     */
    const val MAX_DATA_LOAD_TIME_MS = 2000L
    
    /**
     * 操作响应最大时间（毫秒）
     */
    const val MAX_OPERATION_TIME_MS = 300L
    
    /**
     * 网络请求超时时间（毫秒）
     */
    const val NETWORK_TIMEOUT_MS = 30000L
    
    // ========== 列表性能 ==========
    
    /**
     * 列表初始加载最大条数
     */
    const val LIST_INITIAL_LOAD_SIZE = 50
    
    /**
     * 列表分页大小
     */
    const val LIST_PAGE_SIZE = 20
    
    /**
     * 列表预加载距离（条）
     */
    const val LIST_PREFETCH_DISTANCE = 10
    
    /**
     * 最大同时运行的动画数量
     */
    const val MAX_CONCURRENT_ANIMATIONS = 10
    
    /**
     * 时间线初始加载条数（T066优化）
     */
    const val INITIAL_LOAD_COUNT = 50
    
    /**
     * 时间线分页大小（T066优化）
     */
    const val PAGE_SIZE = 30
    
    // ========== 图片加载 ==========
    
    /**
     * 图片缓存大小（MB）
     */
    const val IMAGE_CACHE_SIZE_MB = 50
    
    /**
     * 图片最大尺寸（像素）
     */
    const val IMAGE_MAX_SIZE_PX = 1080
    
    /**
     * 缩略图尺寸（像素）
     */
    const val THUMBNAIL_SIZE_PX = 200
    
    // ========== 数据库性能 ==========
    
    /**
     * 数据库查询最大时间（毫秒）
     */
    const val MAX_DB_QUERY_TIME_MS = 500L
    
    /**
     * 批量操作最大条数
     */
    const val MAX_BATCH_SIZE = 100
    
    // ========== 监控间隔 ==========
    
    /**
     * 性能采样间隔（毫秒）
     */
    const val SAMPLING_INTERVAL_MS = 1000L
    
    /**
     * 内存检查间隔（毫秒）
     */
    const val MEMORY_CHECK_INTERVAL_MS = 5000L
    
    // ========== 辅助方法 ==========
    
    /**
     * 检查内存使用是否正常
     */
    fun isMemoryUsageNormal(usedMemoryMb: Long): Boolean {
        return usedMemoryMb < MEMORY_WARNING_MB
    }
    
    /**
     * 检查内存使用是否超限
     */
    fun isMemoryExceeded(usedMemoryMb: Long): Boolean {
        return usedMemoryMb >= MAX_MEMORY_MB
    }
    
    /**
     * 检查帧时间是否正常
     */
    fun isFrameTimeNormal(frameTimeMs: Long): Boolean {
        return frameTimeMs <= MAX_FRAME_TIME_MS
    }
    
    /**
     * 检查加载时间是否正常
     */
    fun isLoadTimeNormal(loadTimeMs: Long): Boolean {
        return loadTimeMs <= MAX_PAGE_LOAD_TIME_MS
    }
    
    /**
     * 检查操作响应时间是否正常
     */
    fun isOperationTimeNormal(operationTimeMs: Long): Boolean {
        return operationTimeMs <= MAX_OPERATION_TIME_MS
    }
}
