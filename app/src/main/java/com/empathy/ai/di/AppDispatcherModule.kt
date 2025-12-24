package com.empathy.ai.di

import com.empathy.ai.data.di.DefaultDispatcher
import com.empathy.ai.data.di.IoDispatcher
import com.empathy.ai.domain.util.CoroutineDispatchers
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

/**
 * 应用级协程调度器模块
 *
 * 提供CoroutineDispatchers接口的实现，使Domain层可以使用协程调度器
 * 而不依赖Android或Hilt的Qualifier注解。
 *
 * @see TDD-00017 Clean Architecture模块化改造技术设计
 */
@Module
@InstallIn(SingletonComponent::class)
object AppDispatcherModule {

    /**
     * 提供CoroutineDispatchers接口实现
     *
     * 将Hilt提供的各个调度器组合成Domain层使用的接口
     */
    @Provides
    @Singleton
    fun provideCoroutineDispatchers(
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
        @DefaultDispatcher defaultDispatcher: CoroutineDispatcher
    ): CoroutineDispatchers {
        return object : CoroutineDispatchers {
            override val io: CoroutineDispatcher = ioDispatcher
            override val default: CoroutineDispatcher = defaultDispatcher
            override val main: CoroutineDispatcher = Dispatchers.Main
        }
    }
}
