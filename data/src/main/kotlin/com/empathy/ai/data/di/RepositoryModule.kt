package com.empathy.ai.data.di

import com.empathy.ai.data.local.CleanupPreferencesImpl
import com.empathy.ai.data.local.FloatingWindowPreferences
import com.empathy.ai.data.parser.AiSummaryResponseParserImpl
import com.empathy.ai.data.repository.AiProviderRepositoryImpl
import com.empathy.ai.data.repository.AiRepositoryImpl
import com.empathy.ai.data.repository.BrainTagRepositoryImpl
import com.empathy.ai.data.repository.ContactRepositoryImpl
import com.empathy.ai.data.repository.PrivacyRepositoryImpl
import com.empathy.ai.data.repository.UserProfileRepositoryImpl
import com.empathy.ai.data.repository.settings.SettingsRepositoryImpl
import com.empathy.ai.domain.repository.AiProviderRepository
import com.empathy.ai.domain.repository.AiRepository
import com.empathy.ai.domain.repository.BrainTagRepository
import com.empathy.ai.domain.repository.ContactRepository
import com.empathy.ai.domain.repository.FloatingWindowPreferencesRepository
import com.empathy.ai.domain.repository.PrivacyRepository
import com.empathy.ai.domain.repository.SettingsRepository
import com.empathy.ai.domain.repository.UserProfileRepository
import com.empathy.ai.domain.util.AiSummaryResponseParser
import com.empathy.ai.domain.util.CleanupPreferences
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * 仓库模块
 *
 * 提供 Repository 接口与实现的绑定配置。
 * 使用 @Binds 简化依赖注入,避免手动实例化对象。
 *
 * @see TDD-00017 Clean Architecture模块化改造技术设计
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindContactRepository(impl: ContactRepositoryImpl): ContactRepository

    @Binds
    abstract fun bindBrainTagRepository(impl: BrainTagRepositoryImpl): BrainTagRepository

    @Binds
    abstract fun bindAiRepository(impl: AiRepositoryImpl): AiRepository

    @Binds
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    abstract fun bindPrivacyRepository(impl: PrivacyRepositoryImpl): PrivacyRepository

    @Binds
    abstract fun bindAiProviderRepository(impl: AiProviderRepositoryImpl): AiProviderRepository

    @Binds
    abstract fun bindUserProfileRepository(impl: UserProfileRepositoryImpl): UserProfileRepository

    @Binds
    abstract fun bindFloatingWindowPreferencesRepository(
        impl: FloatingWindowPreferences
    ): FloatingWindowPreferencesRepository

    @Binds
    abstract fun bindAiSummaryResponseParser(
        impl: AiSummaryResponseParserImpl
    ): AiSummaryResponseParser

    @Binds
    abstract fun bindCleanupPreferences(
        impl: CleanupPreferencesImpl
    ): CleanupPreferences
}
