package com.empathy.ai.data.di

import com.empathy.ai.data.local.dao.ConversationLogDao
import com.empathy.ai.data.local.dao.DailySummaryDao
import com.empathy.ai.data.local.dao.FailedSummaryTaskDao
import com.empathy.ai.data.repository.ConversationRepositoryImpl
import com.empathy.ai.data.repository.DailySummaryRepositoryImpl
import com.empathy.ai.data.repository.FailedTaskRepositoryImpl
import com.empathy.ai.domain.repository.ConversationRepository
import com.empathy.ai.domain.repository.DailySummaryRepository
import com.empathy.ai.domain.repository.FailedTaskRepository
import com.empathy.ai.domain.util.ContextBuilder
import com.empathy.ai.domain.util.ConversationContextBuilder
import com.empathy.ai.domain.util.Logger
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 记忆系统模块
 *
 * 提供记忆系统相关的依赖注入配置
 */
@Module
@InstallIn(SingletonComponent::class)
object MemoryModule {

    @Provides
    @Singleton
    fun provideConversationRepository(
        dao: ConversationLogDao
    ): ConversationRepository = ConversationRepositoryImpl(dao)

    @Provides
    @Singleton
    fun provideDailySummaryRepository(
        dao: DailySummaryDao,
        moshi: Moshi
    ): DailySummaryRepository = DailySummaryRepositoryImpl(dao, moshi)

    @Provides
    @Singleton
    fun provideFailedTaskRepository(
        dao: FailedSummaryTaskDao
    ): FailedTaskRepository = FailedTaskRepositoryImpl(dao)

    @Provides
    @Singleton
    fun provideContextBuilder(logger: Logger): ContextBuilder = ContextBuilder(logger)

    @Provides
    @Singleton
    fun provideConversationContextBuilder(): ConversationContextBuilder = ConversationContextBuilder()
}
