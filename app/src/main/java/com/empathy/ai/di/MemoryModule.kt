package com.empathy.ai.di

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

    /**
     * 提供ConversationRepository
     */
    @Provides
    @Singleton
    fun provideConversationRepository(
        dao: ConversationLogDao
    ): ConversationRepository = ConversationRepositoryImpl(dao)

    /**
     * 提供DailySummaryRepository
     */
    @Provides
    @Singleton
    fun provideDailySummaryRepository(
        dao: DailySummaryDao,
        moshi: Moshi
    ): DailySummaryRepository = DailySummaryRepositoryImpl(dao, moshi)

    /**
     * 提供FailedTaskRepository
     */
    @Provides
    @Singleton
    fun provideFailedTaskRepository(
        dao: FailedSummaryTaskDao
    ): FailedTaskRepository = FailedTaskRepositoryImpl(dao)

    /**
     * 提供ContextBuilder
     */
    @Provides
    @Singleton
    fun provideContextBuilder(): ContextBuilder = ContextBuilder()
}
