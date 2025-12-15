package com.empathy.ai.presentation.util

import android.content.Context
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Size
import com.empathy.ai.domain.util.PerformanceMetrics

/**
 * 图片加载配置
 *
 * 提供优化的Coil图片加载配置（T068）
 *
 * 性能优化措施：
 * - 配置内存缓存（50MB）
 * - 配置磁盘缓存（100MB）
 * - 启用crossfade动画
 * - 指定图片尺寸限制
 * - 启用硬件位图
 *
 * 使用示例：
 * ```kotlin
 * // 在Application中初始化
 * val imageLoader = ImageLoaderConfig.createImageLoader(context)
 * Coil.setImageLoader(imageLoader)
 *
 * // 在Composable中使用
 * AsyncImage(
 *     model = ImageLoaderConfig.createRequest(context, url),
 *     contentDescription = "图片"
 * )
 * ```
 */
object ImageLoaderConfig {
    
    /**
     * 内存缓存大小（字节）
     */
    private val MEMORY_CACHE_SIZE = PerformanceMetrics.IMAGE_CACHE_SIZE_MB * 1024 * 1024
    
    /**
     * 磁盘缓存大小（字节）
     */
    private const val DISK_CACHE_SIZE = 100L * 1024 * 1024 // 100MB
    
    /**
     * 磁盘缓存目录名
     */
    private const val DISK_CACHE_DIR = "image_cache"
    
    /**
     * Crossfade动画时长（毫秒）
     */
    private const val CROSSFADE_DURATION_MS = 200
    
    /**
     * 创建优化的ImageLoader
     *
     * @param context 应用上下文
     * @return 配置好的ImageLoader实例
     */
    fun createImageLoader(context: Context): ImageLoader {
        return ImageLoader.Builder(context)
            // 内存缓存配置
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizeBytes(MEMORY_CACHE_SIZE)
                    .build()
            }
            // 磁盘缓存配置
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve(DISK_CACHE_DIR))
                    .maxSizeBytes(DISK_CACHE_SIZE)
                    .build()
            }
            // 启用crossfade动画
            .crossfade(true)
            .crossfade(CROSSFADE_DURATION_MS)
            // 启用硬件位图（Android O+）
            .allowHardware(true)
            // 尊重缓存头
            .respectCacheHeaders(true)
            .build()
    }
    
    /**
     * 创建优化的图片请求
     *
     * @param context 上下文
     * @param data 图片数据（URL、Uri、File等）
     * @param size 目标尺寸（可选）
     * @return 配置好的ImageRequest
     */
    fun createRequest(
        context: Context,
        data: Any?,
        size: Size? = null
    ): ImageRequest {
        return ImageRequest.Builder(context)
            .data(data)
            .apply {
                // 设置尺寸限制
                if (size != null) {
                    size(size)
                } else {
                    size(PerformanceMetrics.IMAGE_MAX_SIZE_PX)
                }
            }
            // 缓存策略
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            // 启用crossfade
            .crossfade(true)
            .crossfade(CROSSFADE_DURATION_MS)
            .build()
    }
    
    /**
     * 创建缩略图请求
     *
     * @param context 上下文
     * @param data 图片数据
     * @return 配置好的缩略图请求
     */
    fun createThumbnailRequest(
        context: Context,
        data: Any?
    ): ImageRequest {
        return ImageRequest.Builder(context)
            .data(data)
            .size(PerformanceMetrics.THUMBNAIL_SIZE_PX)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .crossfade(true)
            .crossfade(CROSSFADE_DURATION_MS)
            .build()
    }
    
    /**
     * 创建头像请求
     *
     * 针对头像图片的优化配置：
     * - 固定尺寸
     * - 优先使用缓存
     *
     * @param context 上下文
     * @param data 图片数据
     * @param sizePx 头像尺寸（像素）
     * @return 配置好的头像请求
     */
    fun createAvatarRequest(
        context: Context,
        data: Any?,
        sizePx: Int = 120
    ): ImageRequest {
        return ImageRequest.Builder(context)
            .data(data)
            .size(sizePx)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .crossfade(true)
            .crossfade(CROSSFADE_DURATION_MS)
            .build()
    }
}
