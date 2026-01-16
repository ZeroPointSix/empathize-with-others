package com.empathy.ai.data.local

import android.media.projection.MediaProjection
import java.util.concurrent.atomic.AtomicReference

/**
 * MediaProjection 进程内缓存（仅内存，不落盘）。
 *
 * 说明：MediaProjection 授权 Intent 包含 Binder/FD，无法安全序列化。
 * 因此仅在进程生命周期内缓存，供悬浮窗截图复用。
 */
object MediaProjectionHolder {
    private val projectionRef = AtomicReference<MediaProjection?>()

    fun store(projection: MediaProjection?) {
        projectionRef.set(projection)
    }

    fun take(): MediaProjection? {
        return projectionRef.getAndSet(null)
    }

    fun peek(): MediaProjection? {
        return projectionRef.get()
    }

    fun clear() {
        projectionRef.getAndSet(null)?.stop()
    }
}
