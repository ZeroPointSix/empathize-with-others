package com.empathy.ai.di

import com.empathy.ai.domain.repository.ApiUsageRepository
import com.empathy.ai.domain.usecase.CleanupApiUsageUseCase
import com.empathy.ai.domain.usecase.ExportApiUsageUseCase
import com.empathy.ai.domain.usecase.GetApiUsageStatsUseCase
import com.empathy.ai.domain.usecase.RecordApiUsageUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * API用量统计模块
 *
 * 提供用量统计相关UseCase的依赖注入配置
 *
 * @see TD-00025 Phase 6: 用量统计实现
 */
@Module
@InstallIn(SingletonComponent::class)
object ApiUsageModule {

    /**
     * 提供记录API用量UseCase
     */
    @Provides
    @Singleton
    fun provideRecordApiUsageUseCase(
        repository: ApiUsageRepository
    ): RecordApiUsageUseCase {
        return RecordApiUsageUseCase(repository)
    }

    /**
     * 提供获取API用量统计UseCase
     */
    @Provides
    @Singleton
    fun provideGetApiUsageStatsUseCase(
        repository: ApiUsageRepository
    ): GetApiUsageStatsUseCase {
        return GetApiUsageStatsUseCase(repository)
    }

    /**
     * 提供清理API用量UseCase
     */
    @Provides
    @Singleton
    fun provideCleanupApiUsageUseCase(
        repository: ApiUsageRepository
    ): CleanupApiUsageUseCase {
        return CleanupApiUsageUseCase(repository)
    }

    /**
     * 提供导出API用量UseCase
     */
    @Provides
    @Singleton
    fun provideExportApiUsageUseCase(
        repository: ApiUsageRepository
    ): ExportApiUsageUseCase {
        return ExportApiUsageUseCase(repository)
    }
}
