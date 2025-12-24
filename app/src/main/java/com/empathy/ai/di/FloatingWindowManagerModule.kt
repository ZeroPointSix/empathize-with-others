package com.empathy.ai.di

import com.empathy.ai.domain.util.FloatingWindowManager
import com.empathy.ai.util.AndroidFloatingWindowManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * FloatingWindowManager依赖注入模块
 *
 * 绑定FloatingWindowManager接口到AndroidFloatingWindowManager实现。
 *
 * @see TDD-00017 Clean Architecture模块化改造技术设计
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class FloatingWindowManagerModule {

    @Binds
    @Singleton
    abstract fun bindFloatingWindowManager(
        impl: AndroidFloatingWindowManager
    ): FloatingWindowManager
}
