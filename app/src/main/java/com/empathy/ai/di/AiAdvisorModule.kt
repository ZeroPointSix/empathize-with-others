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
 * AI军师功能依赖注入模块
 *
 * 提供AI军师对话功能（TD-00026）所需的UseCase依赖注入配置。
 *
 * 业务背景 (PRD-00026):
 *   AI军师是一个独立的智能对话模块，允许用户与AI进行自由对话，
 *   不依赖特定联系人，获取通用的沟通建议和情感支持。
 *
 * 模块职责:
 *   - 会话管理: 创建、获取、删除会话及对话记录
 *   - 消息处理: 发送消息、接收AI响应
 *
 * 架构决策 (TDD-00026):
 *   - 使用SingletonComponent确保所有UseCase为单例，复用会话状态
 *   - SendAdvisorMessageUseCase依赖4个仓库，体现其核心编排角色
 *
 * @see PRD-00026 AI军师对话功能需求
 * @see TDD-00026 AI军师对话功能技术设计
 */
@Module
@InstallIn(SingletonComponent::class)
object AiAdvisorModule {

    /**
     * 提供创建AI军师会话用例
     *
     * 用户发起新对话时调用，创建空会话记录并返回会话ID
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
     *
     * 返回用户所有会话的摘要信息（最近消息、时间戳等），
     * 用于会话列表UI展示，支持按时间倒序排列。
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
     *
     * 根据会话ID获取该会话的所有消息记录，
     * 用于聊天详情页的对话内容展示，按时间正序排列。
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
     *
     * 根据会话ID删除该会话及其所有对话记录，
     * 支持批量删除或单条删除，用于用户管理自己的对话历史。
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
     *
     * [核心编排用例] 消息发送的完整流程:
     *   1. 创建用户消息记录
     *   2. 调用AI Repository获取响应
     *   3. 创建AI消息记录
     *   4. 返回对话结果
     *
     * 依赖4个仓库的协作:
     *   - AiAdvisorRepository: 会话消息持久化
     *   - AiRepository: AI服务调用（核心）
     *   - ContactRepository: 联系人上下文（如需关联）
     *   - AiProviderRepository: AI提供商配置管理
     *
     * 权衡 (TDD-00026): 4个依赖确保了功能的完整性，
     * 但也增加了耦合度，后续可考虑合并仓库接口简化依赖。
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
