package com.empathy.ai.presentation.util

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * 自适应动画配置
 * 根据设备性能自动调整动画参数
 * 
 * @see TDD-00020 9.6 动画降级处理
 */
object AdaptiveAnimationConfig {
    private var cachedIsLowEndDevice: Boolean? = null
    
    /**
     * 检测是否为低端设备
     * 低端设备定义：
     * - Android 8.0以下
     * - 或系统标记为低内存设备
     * 
     * @param context 上下文
     * @return true表示低端设备
     */
    fun isLowEndDevice(context: Context): Boolean {
        if (cachedIsLowEndDevice == null) {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            cachedIsLowEndDevice = activityManager.isLowRamDevice ||
                    Build.VERSION.SDK_INT < Build.VERSION_CODES.O
        }
        return cachedIsLowEndDevice!!
    }
    
    /**
     * 获取自适应动画时长
     * 低端设备减半动画时长
     * 
     * @param context 上下文
     * @param defaultDuration 默认时长（毫秒）
     * @return 调整后的时长
     */
    fun getAnimationDuration(context: Context, defaultDuration: Int = 250): Int {
        return if (isLowEndDevice(context)) {
            (defaultDuration * 0.5).toInt()
        } else {
            defaultDuration
        }
    }
    
    /**
     * 获取自适应动画规格（Composable版本）
     * 低端设备使用简化动画
     * 
     * @param defaultDurationMillis 默认时长
     * @return 动画规格
     */
    @Composable
    fun <T> adaptiveAnimationSpec(
        defaultDurationMillis: Int = 250
    ): AnimationSpec<T> {
        val context = LocalContext.current
        return remember(context) {
            if (isLowEndDevice(context)) {
                tween(
                    durationMillis = (defaultDurationMillis * 0.5).toInt(),
                    easing = LinearEasing
                )
            } else {
                tween(
                    durationMillis = defaultDurationMillis,
                    easing = FastOutSlowInEasing
                )
            }
        }
    }
    
    /**
     * 重置缓存（用于测试）
     */
    fun resetCache() {
        cachedIsLowEndDevice = null
    }
}

/**
 * 内存监控工具
 * 用于检测内存状态并提供降级建议
 * 
 * @see TDD-00020 9.8 内存不足处理
 */
object MemoryMonitor {
    private const val LOW_MEMORY_THRESHOLD = 50 * 1024 * 1024L // 50MB
    
    /**
     * 检查是否内存不足
     * @return true表示内存不足
     */
    fun isLowMemory(): Boolean {
        val runtime = Runtime.getRuntime()
        val freeMemory = runtime.freeMemory()
        val maxMemory = runtime.maxMemory()
        val usedMemory = runtime.totalMemory() - freeMemory
        val availableMemory = maxMemory - usedMemory
        
        return availableMemory < LOW_MEMORY_THRESHOLD
    }
    
    /**
     * 获取内存使用建议
     * @return 内存建议枚举
     */
    fun getMemoryAdvice(): MemoryAdvice {
        return when {
            isLowMemory() -> MemoryAdvice.REDUCE_QUALITY
            else -> MemoryAdvice.NORMAL
        }
    }
    
    /**
     * 获取当前可用内存（MB）
     * @return 可用内存大小
     */
    fun getAvailableMemoryMB(): Long {
        val runtime = Runtime.getRuntime()
        val freeMemory = runtime.freeMemory()
        val maxMemory = runtime.maxMemory()
        val usedMemory = runtime.totalMemory() - freeMemory
        val availableMemory = maxMemory - usedMemory
        return availableMemory / (1024 * 1024)
    }
}

/**
 * 内存建议枚举
 */
enum class MemoryAdvice {
    /** 正常模式 - 使用完整渲染 */
    NORMAL,
    /** 降低质量模式 - 简化渲染 */
    REDUCE_QUALITY
}
