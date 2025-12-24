package com.empathy.ai.di

import com.empathy.ai.data.util.AndroidLogger
import com.empathy.ai.domain.util.Logger
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Logger依赖注入模块
 *
 * 将AndroidLogger绑定到Logger接口，使Domain层可以使用日志功能
 * 而不依赖Android SDK。
 *
 * @see TDD-00017 Clean Architecture模块化改造技术设计
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class LoggerModule {

    /**
     * 绑定Logger接口到AndroidLogger实现
     */
    @Binds
    @Singleton
    abstract fun bindLogger(impl: AndroidLogger): Logger
}
