package com.empathy.ai.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * 悬浮窗功能依赖注入模块
 *
 * 注意：PolishDraftUseCase、GenerateReplyUseCase、RefinementUseCase
 * 都已有@Inject constructor，Hilt会自动处理依赖注入，无需手动@Provides。
 *
 * @see PRD-00009 悬浮窗功能重构需求
 * @see TDD-00009 悬浮窗功能重构技术设计
 * @see TDD-00017 Clean Architecture模块化改造技术设计
 */
@Module
@InstallIn(SingletonComponent::class)
object FloatingWindowModule {
    // UseCase已有@Inject constructor，Hilt自动处理依赖注入
    // 无需手动@Provides
}
