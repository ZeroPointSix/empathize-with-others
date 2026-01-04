package com.empathy.ai.data.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * DispatcherModule 实现了协程调度器的依赖注入配置 (TDD-00017/7.2)
 *
 * 设计目的：便于单元测试时替换调度器
 *
 * 为什么需要注入而非直接使用Dispatchers？
 * - Dispatchers.IO/Default 是全局单例，无法在运行时替换
 * - 单元测试需要使用 TestDispatcher 控制协程执行时机
 * - 通过 Hilt 注入解耦，实现测试可替换性
 *
 * Qualifiers 使用：
 * - @IoDispatcher: 文件IO、数据库操作、网络请求
 * - @DefaultDispatcher: CPU密集型计算
 * - @MainDispatcher: UI主线程（由 presentation 层提供）
 *
 * @see Qualifiers.kt 限定符定义
 */
@Module
@InstallIn(SingletonComponent::class)
object DispatcherModule {

    /**
     * 提供IO调度器 (TDD-00017/7.2)
     *
     * 使用场景：IO密集型操作
     * - 文件读写 (File I/O)
     * - 数据库操作 (Room, SharedPreferences)
     * - 网络请求 (Retrofit, OkHttp)
     *
     * 技术说明：Dispatchers.IO 使用共享线程池，线程数根据CPU核心数动态调整
     * 适合场景：耗时但不需要复杂计算的IO操作
     *
     * @return CoroutineDispatcher.IO 调度器实例
     */
    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    /**
     * 提供默认调度器 (TDD-00017/7.2)
     *
     * 使用场景：CPU密集型计算操作
     * - JSON 解析和序列化
     * - 复杂的数据转换逻辑
     * - 内存中的数据处理
     *
     * 技术说明：Dispatchers.Default 默认也使用共享线程池
     * 与 IO 的区别在于：适合计算密集型而非IO密集型任务
     *
     * @return CoroutineDispatcher.Default 调度器实例
     */
    @Provides
    @DefaultDispatcher
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default
}
