package com.empathy.ai.di

import com.empathy.ai.domain.util.ContentValidator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 编辑功能依赖注入模块
 *
 * 提供事实流内容编辑相关的依赖注入配置。
 *
 * 注意：EditFactUseCase、EditConversationUseCase、EditSummaryUseCase、EditContactInfoUseCase
 * 都已有@Inject constructor，Hilt会自动处理依赖注入，无需手动@Provides。
 *
 * @see TDD-00017 Clean Architecture模块化改造技术设计
 */
@Module
@InstallIn(SingletonComponent::class)
object EditModule {

    /**
     * 提供内容验证器
     *
     * ContentValidator没有@Inject注解，需要手动提供
     */
    @Provides
    @Singleton
    fun provideContentValidator(): ContentValidator {
        return ContentValidator()
    }
}
