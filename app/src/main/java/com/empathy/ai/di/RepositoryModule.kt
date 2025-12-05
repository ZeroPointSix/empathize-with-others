package com.empathy.ai.di

import com.empathy.ai.data.repository.AiRepositoryImpl
import com.empathy.ai.data.repository.BrainTagRepositoryImpl
import com.empathy.ai.data.repository.ContactRepositoryImpl
import com.empathy.ai.data.repository.PrivacyRepositoryImpl
import com.empathy.ai.data.repository.settings.SettingsRepositoryImpl
import com.empathy.ai.domain.repository.AiRepository
import com.empathy.ai.domain.repository.BrainTagRepository
import com.empathy.ai.domain.repository.ContactRepository
import com.empathy.ai.domain.repository.PrivacyRepository
import com.empathy.ai.domain.repository.SettingsRepository
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
 * @constructor 创建仓库模块
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    /**
     * 绑定 ContactRepository
     *
     * 将 ContactRepository 接口绑定到其实现类 ContactRepositoryImpl。
     *
     * @param impl ContactRepositoryImpl 实例
     * @return ContactRepository 接口
     */
    @Binds
    abstract fun bindContactRepository(impl: ContactRepositoryImpl): ContactRepository

    /**
     * 绑定 BrainTagRepository
     *
     * 将 BrainTagRepository 接口绑定到其实现类 BrainTagRepositoryImpl。
     *
     * @param impl BrainTagRepositoryImpl 实例
     * @return BrainTagRepository 接口
     */
    @Binds
    abstract fun bindBrainTagRepository(impl: BrainTagRepositoryImpl): BrainTagRepository

    /**
     * 绑定 AiRepository
     *
     * 将 AiRepository 接口绑定到其实现类 AiRepositoryImpl。
     *
     * @param impl AiRepositoryImpl 实例
     * @return AiRepository 接口
     */
    @Binds
    abstract fun bindAiRepository(impl: AiRepositoryImpl): AiRepository

    /**
     * 绑定 SettingsRepository
     *
     * 将 SettingsRepository 接口绑定到其实现类 SettingsRepositoryImpl。
     *
     * @param impl SettingsRepositoryImpl 实例
     * @return SettingsRepository 接口
     */
    @Binds
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    /**
     * 绑定 PrivacyRepository
     *
     * 将 PrivacyRepository 接口绑定到其实现类 PrivacyRepositoryImpl。
     *
     * @param impl PrivacyRepositoryImpl 实例
     * @return PrivacyRepository 接口
     */
    @Binds
    abstract fun bindPrivacyRepository(impl: PrivacyRepositoryImpl): PrivacyRepository
}
