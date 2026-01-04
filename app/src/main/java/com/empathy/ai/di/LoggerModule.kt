package com.empathy.ai.di

import com.empathy.ai.data.util.AndroidLogger
import com.empathy.ai.domain.util.Logger
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 日志功能依赖注入模块
 *
 * 将Domain层的Logger抽象接口绑定到Android平台的实现。
 *
 * 架构意义 (TDD-00017 Clean Architecture模块化改造):
 *   - Domain层定义Logger接口，保持业务逻辑层对Android SDK无依赖
 *   - Data层提供AndroidLogger实现，封装Android Log API
 *   - 通过@Binds实现跨模块依赖注入
 *
 * 设计权衡:
 *   - 为什么不在Domain层直接使用Android Log？
 *     → 保持Domain层纯Kotlin，可独立测试，无需Android环境
 *   - 为什么使用接口而非直接传递Logger实例？
 *     → 符合依赖倒置原则，便于Mock测试
 *
 * 使用场景:
 *   - Domain层UseCase通过Logger记录业务日志
 *   - 避免在业务逻辑中直接耦合Android API
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
