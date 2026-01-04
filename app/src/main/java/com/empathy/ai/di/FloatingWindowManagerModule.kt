package com.empathy.ai.di

import com.empathy.ai.domain.util.FloatingWindowManager
import com.empathy.ai.util.AndroidFloatingWindowManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 悬浮窗管理器依赖注入模块
 *
 * 将Domain层的FloatingWindowManager抽象接口绑定到Android平台的实现。
 *
 * 架构意义 (TDD-00001/TDD-00017):
 *   - Domain层定义接口，保持对Android SDK无依赖
 *   - Data层提供具体实现，封装平台相关代码
 *   - 通过@Binds实现接口与实现的解耦
 *
 * 实现差异:
 *   - FloatingWindowManager: 纯Kotlin接口，定义悬浮窗操作契约
 *   - AndroidFloatingWindowManager: Android平台的具体实现，处理权限、Service等
 *
 * 业务规则 (PRD-00001):
 *   - 悬浮窗权限必须在应用启动时检查
 *   - 权限丢失时需要自动重置本地状态
 *
 * @see TDD-00001 悬浮窗架构设计
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
