package com.empathy.ai.domain.util

/**
 * 性能指标配置
 *
 * 定义应用的性能要求和阈值，用于性能监控和优化。
 *
 * 设计依据:
 * - Android官方性能最佳实践
 * - Material Design 响应时间指南
 * - 用户体验研究（100ms为感知阈值）
 */
object PerformanceMetrics {

    // ========== 内存限制 ==========

    /**
     * 最大内存使用阈值（MB）
     *
     * 超过此值视为内存警告状态
     */
    const val MAX_MEMORY_MB = 200

    /**
     * 内存警告阈值（MB）
     *
     * 达到此值时触发内存优化建议
     */
    const val MEMORY_WARNING_MB = 150

    /**
     * 低内存阈值（MB）
     *
     * 低于此值可能影响应用正常运行
     */
    const val LOW_MEMORY_MB = 50

    // ========== 帧率要求 ==========

    /**
     * 目标帧率（FPS）
     *
     * 追求流畅的60fps动画效果
     */
    const val TARGET_FPS = 60

    /**
     * 可接受最低帧率（FPS）
     *
     * 低于此值会感知到卡顿
     */
    const val MIN_ACCEPTABLE_FPS = 30

    /**
     * 最大帧时间（ms）
     *
     * 16ms = 60fps，确保流畅渲染
     */
    const val MAX_FRAME_TIME_MS = 16L

    /**
     * 帧时间警告阈值（ms）
     *
     * 超过32ms会开始感知卡顿
     */
    const val FRAME_TIME_WARNING_MS = 32L

    // ========== 加载时间 ==========

    /**
     * 页面加载最大时间（ms）
     *
     * 用户可接受的页面切换时间上限
     * 依据: 尼尔森可用性原则（1秒内响应）
     */
    const val MAX_PAGE_LOAD_TIME_MS = 1000L

    /**
     * 数据加载最大时间（ms）
     *
     * 数据密集型操作的时间上限
     */
    const val MAX_DATA_LOAD_TIME_MS = 2000L

    /**
     * 操作响应最大时间（ms）
     *
     * 用户操作后的响应时间要求
     */
    const val MAX_OPERATION_TIME_MS = 300L

    /**
     * 网络请求超时时间（ms）
     *
     * AI API调用的超时设置
     */
    const val NETWORK_TIMEOUT_MS = 30000L

    // ========== 列表性能 ==========

    /**
     * 列表初始加载数量
     *
     * 首屏展示的联系人/对话数量
     */
    const val LIST_INITIAL_LOAD_SIZE = 50

    /**
     * 列表分页大小
     *
     * 每次滚动到底部加载的数量
     */
    const val LIST_PAGE_SIZE = 20

    /**
     * 列表预取距离
     *
     * 距离底部多少项时触发预加载
     */
    const val LIST_PREFETCH_DISTANCE = 10

    /**
     * 最大并发动画数
     *
     * 限制同时运行的动画数量，防止性能问题
     */
    const val MAX_CONCURRENT_ANIMATIONS = 10

    /**
     * 初始加载数量（别名）
     */
    const val INITIAL_LOAD_COUNT = 50

    /**
     * 分页大小（别名）
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
     * 数据库查询最大时间（ms）
     *
     * 超过此值视为慢查询
     */
    const val MAX_DB_QUERY_TIME_MS = 500L

    /**
     * 批量操作最大数量
     *
     * 单次批量操作的最大记录数
     */
    const val MAX_BATCH_SIZE = 100

    // ========== 监控间隔 ==========

    /**
     * 性能采样间隔（ms）
     *
     * 性能指标采集的频率
     */
    const val SAMPLING_INTERVAL_MS = 1000L

    /**
     * 内存检查间隔（ms）
     *
     * 内存监控的检查频率
     */
    const val MEMORY_CHECK_INTERVAL_MS = 5000L
}
