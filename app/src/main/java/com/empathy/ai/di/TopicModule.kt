package com.empathy.ai.di

import com.empathy.ai.data.di.IoDispatcher
import com.empathy.ai.data.local.dao.ConversationTopicDao
import com.empathy.ai.data.repository.TopicRepositoryImpl
import com.empathy.ai.domain.repository.TopicRepository
import com.empathy.ai.domain.usecase.ClearTopicUseCase
import com.empathy.ai.domain.usecase.GetTopicUseCase
import com.empathy.ai.domain.usecase.SetTopicUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Singleton

/**
 * 对话主题依赖注入模块
 *
 * 提供主题功能所需的所有依赖：
 * - TopicRepository: 主题数据仓库
 * - SetTopicUseCase: 设置主题用例
 * - GetTopicUseCase: 获取主题用例
 * - ClearTopicUseCase: 清除主题用例
 */
@Module
@InstallIn(SingletonComponent::class)
object TopicModule {

    @Provides
    @Singleton
    fun provideTopicRepository(
        topicDao: ConversationTopicDao,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): TopicRepository {
        return TopicRepositoryImpl(topicDao, ioDispatcher)
    }

    @Provides
    fun provideSetTopicUseCase(
        topicRepository: TopicRepository
    ): SetTopicUseCase {
        return SetTopicUseCase(topicRepository)
    }

    @Provides
    fun provideGetTopicUseCase(
        topicRepository: TopicRepository
    ): GetTopicUseCase {
        return GetTopicUseCase(topicRepository)
    }

    @Provides
    fun provideClearTopicUseCase(
        topicRepository: TopicRepository
    ): ClearTopicUseCase {
        return ClearTopicUseCase(topicRepository)
    }
}
