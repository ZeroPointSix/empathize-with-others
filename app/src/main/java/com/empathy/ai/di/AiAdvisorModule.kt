package com.empathy.ai.di

import com.empathy.ai.domain.repository.AiAdvisorRepository
import com.empathy.ai.domain.repository.AiProviderRepository
import com.empathy.ai.domain.repository.AiRepository
import com.empathy.ai.domain.repository.ContactRepository
import com.empathy.ai.domain.usecase.CreateAdvisorSessionUseCase
import com.empathy.ai.domain.usecase.DeleteAdvisorConversationUseCase
import com.empathy.ai.domain.usecase.GetAdvisorConversationsUseCase
import com.empathy.ai.domain.usecase.GetAdvisorSessionsUseCase
import com.empathy.ai.domain.usecase.SendAdvisorMessageUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * AI军师功能DI模块
 *
 * TD-00026: 提供AI军师对话功能相关的UseCase依赖注入
 */
@Module
@InstallIn(SingletonComponent::class)
object AiAdvisorModule {

    /**
     * 提供创建AI军师会话用例
     */
    @Provides
    @Singleton
    fun provideCreateAdvisorSessionUseCase(
        aiAdvisorRepository: AiAdvisorRepository
    ): CreateAdvisorSessionUseCase {
        return CreateAdvisorSessionUseCase(aiAdvisorRepository)
    }

    /**
     * 提供获取AI军师会话列表用例
     */
    @Provides
    @Singleton
    fun provideGetAdvisorSessionsUseCase(
        aiAdvisorRepository: AiAdvisorRepository
    ): GetAdvisorSessionsUseCase {
        return GetAdvisorSessionsUseCase(aiAdvisorRepository)
    }

    /**
     * 提供获取AI军师对话记录用例
     */
    @Provides
    @Singleton
    fun provideGetAdvisorConversationsUseCase(
        aiAdvisorRepository: AiAdvisorRepository
    ): GetAdvisorConversationsUseCase {
        return GetAdvisorConversationsUseCase(aiAdvisorRepository)
    }

    /**
     * 提供删除AI军师对话记录用例
     */
    @Provides
    @Singleton
    fun provideDeleteAdvisorConversationUseCase(
        aiAdvisorRepository: AiAdvisorRepository
    ): DeleteAdvisorConversationUseCase {
        return DeleteAdvisorConversationUseCase(aiAdvisorRepository)
    }

    /**
     * 提供发送AI军师消息用例
     */
    @Provides
    @Singleton
    fun provideSendAdvisorMessageUseCase(
        aiAdvisorRepository: AiAdvisorRepository,
        aiRepository: AiRepository,
        contactRepository: ContactRepository,
        aiProviderRepository: AiProviderRepository
    ): SendAdvisorMessageUseCase {
        return SendAdvisorMessageUseCase(
            aiAdvisorRepository,
            aiRepository,
            contactRepository,
            aiProviderRepository
        )
    }
}
