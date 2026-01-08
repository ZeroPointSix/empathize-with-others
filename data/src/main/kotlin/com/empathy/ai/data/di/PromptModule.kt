package com.empathy.ai.data.di

import android.content.Context
import com.empathy.ai.data.local.PromptFileBackup
import com.empathy.ai.data.local.PromptFileStorage
import com.empathy.ai.data.local.SystemPromptStorage
import com.empathy.ai.data.local.dao.ContactDao
import com.empathy.ai.data.repository.PromptRepositoryImpl
import com.empathy.ai.data.repository.SystemPromptRepositoryImpl
import com.empathy.ai.domain.repository.PromptRepository
import com.empathy.ai.domain.repository.SystemPromptRepository
import com.empathy.ai.domain.util.Logger
import com.empathy.ai.domain.util.PromptBuilder
import com.empathy.ai.domain.util.PromptValidator
import com.empathy.ai.domain.util.PromptVariableResolver
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Singleton

/**
 * 提示词管理系统依赖注入模块
 *
 * 提供提示词系统所有组件的依赖注入配置
 */
@Module
@InstallIn(SingletonComponent::class)
object PromptModule {

    @Provides
    @Singleton
    fun providePromptFileBackup(
        @ApplicationContext context: Context,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): PromptFileBackup {
        return PromptFileBackup(context, ioDispatcher)
    }

    @Provides
    @Singleton
    fun providePromptFileStorage(
        @ApplicationContext context: Context,
        moshi: Moshi,
        backup: PromptFileBackup,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): PromptFileStorage {
        return PromptFileStorage(context, moshi, backup, ioDispatcher)
    }

    @Provides
    @Singleton
    fun providePromptVariableResolver(): PromptVariableResolver {
        return PromptVariableResolver()
    }

    @Provides
    @Singleton
    fun providePromptValidator(
        variableResolver: PromptVariableResolver
    ): PromptValidator {
        return PromptValidator(variableResolver)
    }

    @Provides
    @Singleton
    fun providePromptRepository(
        fileStorage: PromptFileStorage,
        contactDao: ContactDao,
        validator: PromptValidator
    ): PromptRepository {
        return PromptRepositoryImpl(fileStorage, contactDao, validator)
    }

    @Provides
    @Singleton
    fun providePromptBuilder(
        promptRepository: PromptRepository,
        variableResolver: PromptVariableResolver,
        logger: Logger
    ): PromptBuilder {
        return PromptBuilder(promptRepository, variableResolver, logger)
    }

    // ========== 系统提示词编辑功能 (PRD-00033) ==========

    /**
     * 提供系统提示词存储
     */
    @Provides
    @Singleton
    fun provideSystemPromptStorage(
        @ApplicationContext context: Context,
        moshi: Moshi,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): SystemPromptStorage {
        return SystemPromptStorage(context, moshi, ioDispatcher)
    }

    /**
     * 提供系统提示词仓库
     */
    @Provides
    @Singleton
    fun provideSystemPromptRepository(
        storage: SystemPromptStorage,
        moshi: Moshi
    ): SystemPromptRepository {
        return SystemPromptRepositoryImpl(storage, moshi)
    }
}
