package com.empathy.ai.data.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Dispatcher 依赖注入模块
 *
 * 提供协程调度器的依赖注入，便于测试时替换为TestDispatcher。
 * 
 * 注意：此模块使用 data.di 包中的 Qualifier 注解（@IoDispatcher、@DefaultDispatcher），
 * 确保与 data 模块中的其他类使用相同的 Qualifier。
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
