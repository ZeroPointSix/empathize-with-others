package com.empathy.ai.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Qualifier

/**
 * IO Dispatcher 注解
 *
 * 用于标记需要在IO线程执行的操作（文件读写、网络请求等）
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

/**
 * Default Dispatcher 注解
 *
 * 用于标记需要在默认线程池执行的CPU密集型操作
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher

/**
 * Dispatcher 依赖注入模块
 *
 * 提供协程调度器的依赖注入，便于测试时替换为TestDispatcher
 */
@Module
@InstallIn(SingletonComponent::class)
object DispatcherModule {

    /**
     * 提供IO调度器
     *
     * 用于文件读写、数据库操作、网络请求等IO密集型操作
     */
    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    /**
     * 提供默认调度器
     *
     * 用于CPU密集型计算操作
     */
    @Provides
    @DefaultDispatcher
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default
}
