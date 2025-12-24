package com.empathy.ai.domain.util

/**
 * 性能指标配置
 */
object PerformanceMetrics {
    const val MAX_MEMORY_MB = 200
    const val MEMORY_WARNING_MB = 150
    const val LOW_MEMORY_MB = 50
    const val TARGET_FPS = 60
    const val MIN_ACCEPTABLE_FPS = 30
    const val MAX_FRAME_TIME_MS = 16L
    const val FRAME_TIME_WARNING_MS = 32L
    const val MAX_PAGE_LOAD_TIME_MS = 1000L
    const val MAX_DATA_LOAD_TIME_MS = 2000L
    const val MAX_OPERATION_TIME_MS = 300L
    const val NETWORK_TIMEOUT_MS = 30000L
    const val LIST_INITIAL_LOAD_SIZE = 50
    const val LIST_PAGE_SIZE = 20
    const val LIST_PREFETCH_DISTANCE = 10
    const val MAX_CONCURRENT_ANIMATIONS = 10
    const val INITIAL_LOAD_COUNT = 50
    const val PAGE_SIZE = 30
    const val IMAGE_CACHE_SIZE_MB = 50
    const val IMAGE_MAX_SIZE_PX = 1080
    const val THUMBNAIL_SIZE_PX = 200
    const val MAX_DB_QUERY_TIME_MS = 500L
    const val MAX_BATCH_SIZE = 100
    const val SAMPLING_INTERVAL_MS = 1000L
    const val MEMORY_CHECK_INTERVAL_MS = 5000L
}
