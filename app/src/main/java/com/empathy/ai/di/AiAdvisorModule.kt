package com.empathy.ai.di

import com.empathy.ai.domain.repository.AiAdvisorRepository
import com.empathy.ai.domain.repository.AiProviderRepository
import com.empathy.ai.domain.repository.AiRepository
import com.empathy.ai.domain.repository.ApiUsageRepository
import com.empathy.ai.domain.repository.BrainTagRepository
import com.empathy.ai.domain.repository.ContactRepository
import com.empathy.ai.domain.usecase.CreateAdvisorSessionUseCase
import com.empathy.ai.domain.usecase.DeleteAdvisorConversationUseCase
import com.empathy.ai.domain.usecase.GetAdvisorConversationsUseCase
import com.empathy.ai.domain.usecase.GetAdvisorSessionsUseCase
import com.empathy.ai.domain.usecase.SendAdvisorMessageUseCase
import com.empathy.ai.domain.usecase.SendAdvisorMessageStreamingUseCase
import com.empathy.ai.domain.util.Logger
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * AI军师功能依赖注入模块
 *
 * 提供AI军师对话功能（TD-00026）所需的UseCase依赖注入配置。
 * 🆕 FD-00028: 新增流式对话支持
 * 🆕 FD-00030: 新增会话上下文隔离和Markdown渲染支持
 *
 * 业务背景 (PRD-00026):
 *   AI军师是一个独立的智能对话模块，允许用户与AI进行自由对话，
 *   不依赖特定联系人，获取通用的沟通建议和情感支持。
 *
 * 模块职责:
 *   - 会话管理: 创建、获取、删除会话及对话记录
 *   - 消息处理: 发送消息、接收AI响应
 *   - 🆕 流式响应: SSE流式对话，支持思考过程展示
 *   - 🆕 会话隔离: 新会话只包含联系人画像，不包含历史对话
 *
 * 架构决策 (TDD-00026):
 *   - 使用SingletonComponent确保所有UseCase为单例，复用会话状态
 *   - SendAdvisorMessageUseCase依赖4个仓库，体现其核心编排角色
 *   - 🆕 SendAdvisorMessageStreamingUseCase支持流式响应
 *   - 🆕 FD-00030: SendAdvisorMessageStreamingUseCase新增BrainTagRepository依赖
 *
 * @see PRD-00026 AI军师对话功能需求
 * @see TDD-00026 AI军师对话功能技术设计
 * @see FD-00028 AI军师流式对话升级功能设计
 * @see FD-00030 AI军师Markdown渲染与会话隔离功能设计
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

    /**
     * 提供发送AI军师消息用例（流式版本）
     *
     * 🆕 FD-00028: 流式对话升级
     * 🆕 FD-00030: 会话上下文隔离和联系人画像增强
     * 🆕 BUG-00062: 添加用量统计支持
     *
     * [核心编排用例] 流式消息发送的完整流程:
     *   1. 创建用户消息记录
     *   2. 创建AI消息占位（PENDING状态）
     *   3. 创建初始Block（MAIN_TEXT）
     *   4. 调用AI Repository获取流式响应
     *   5. 实时更新Block内容
     *   6. 完成后更新消息状态
     *   7. 🆕 记录用量统计（BUG-00062）
     *
     * 与非流式版本的区别:
     *   - 返回Flow<StreamingState>而非Result<Unit>
     *   - 支持思考过程展示（DeepSeek R1等模型）
     *   - 支持停止生成功能
     *   - 使用Block架构存储消息内容
     *
     * FD-00030新增功能:
     *   - 会话上下文隔离：新会话只包含联系人画像，不包含历史对话
     *   - 联系人画像增强：添加标签（雷区/策略）和事实流信息
     *   - 依赖BrainTagRepository获取联系人标签
     *
     * BUG-00062新增功能:
     *   - 流式响应完成时记录用量统计
     *   - 支持成功/失败两种状态的用量记录
     *   - 依赖ApiUsageRepository记录用量
     *
     * @see FD-00028 AI军师流式对话升级功能设计
     * @see FD-00030 AI军师Markdown渲染与会话隔离功能设计
     * @see BUG-00062 AI用量统计统一问题修复
     * @see StreamingState 流式状态定义
     */
    @Provides
    @Singleton
    fun provideSendAdvisorMessageStreamingUseCase(
        aiAdvisorRepository: AiAdvisorRepository,
        aiRepository: AiRepository,
        contactRepository: ContactRepository,
        aiProviderRepository: AiProviderRepository,
        brainTagRepository: BrainTagRepository,  // FD-00030: 新增标签仓库依赖
        apiUsageRepository: ApiUsageRepository,  // BUG-00062: 新增用量统计仓库依赖
        logger: Logger  // CR-001: 新增日志记录器依赖
    ): SendAdvisorMessageStreamingUseCase {
        return SendAdvisorMessageStreamingUseCase(
            aiAdvisorRepository,
            aiRepository,
            contactRepository,
            aiProviderRepository,
            brainTagRepository,
            apiUsageRepository,
            logger
        )
    }
}
