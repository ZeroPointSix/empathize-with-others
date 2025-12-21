package com.empathy.ai.di

import com.empathy.ai.domain.repository.AiProviderRepository
import com.empathy.ai.domain.repository.AiRepository
import com.empathy.ai.domain.repository.BrainTagRepository
import com.empathy.ai.domain.repository.ContactRepository
import com.empathy.ai.domain.repository.PrivacyRepository
import com.empathy.ai.domain.service.SessionContextService
import com.empathy.ai.domain.usecase.GenerateReplyUseCase
import com.empathy.ai.domain.usecase.PolishDraftUseCase
import com.empathy.ai.domain.usecase.RefinementUseCase
import com.empathy.ai.domain.util.PromptBuilder
import com.empathy.ai.domain.util.UserProfileContextBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 悬浮窗功能依赖注入模块
 *
 * 提供悬浮窗重构相关的UseCase依赖
 *
 * @see PRD-00009 悬浮窗功能重构需求
 * @see TDD-00009 悬浮窗功能重构技术设计
 * @see BUG-00015 三种模式上下文不共通问题分析
 * @see TD-00013 自己画像界面任务清单 - 润色/回复模式用户画像集成
 */
@Module
@InstallIn(SingletonComponent::class)
object FloatingWindowModule {

    /**
     * 提供润色草稿用例
     *
     * 注意：已移除对PrivacyEngine的直接依赖，改为通过PrivacyRepository接口
     * 符合Clean Architecture的依赖倒置原则
     *
     * BUG-00015修复：添加SessionContextService以支持历史上下文共享
     * BUG-00023修复：移除ConversationRepository，改为用户点击复制按钮时保存
     * TD-00013修复：添加UserProfileContextBuilder以支持用户画像上下文
     */
    @Provides
    @Singleton
    fun providePolishDraftUseCase(
        contactRepository: ContactRepository,
        brainTagRepository: BrainTagRepository,
        privacyRepository: PrivacyRepository,
        aiRepository: AiRepository,
        aiProviderRepository: AiProviderRepository,
        promptBuilder: PromptBuilder,
        sessionContextService: SessionContextService,
        userProfileContextBuilder: UserProfileContextBuilder
    ): PolishDraftUseCase {
        return PolishDraftUseCase(
            contactRepository = contactRepository,
            brainTagRepository = brainTagRepository,
            privacyRepository = privacyRepository,
            aiRepository = aiRepository,
            aiProviderRepository = aiProviderRepository,
            promptBuilder = promptBuilder,
            sessionContextService = sessionContextService,
            userProfileContextBuilder = userProfileContextBuilder
        )
    }

    /**
     * 提供生成回复用例
     *
     * 注意：已移除对PrivacyEngine的直接依赖，改为通过PrivacyRepository接口
     * 符合Clean Architecture的依赖倒置原则
     *
     * BUG-00015修复：添加SessionContextService以支持历史上下文共享
     * BUG-00023修复：移除ConversationRepository，改为用户点击复制按钮时保存
     * TD-00013修复：添加UserProfileContextBuilder以支持用户画像上下文
     */
    @Provides
    @Singleton
    fun provideGenerateReplyUseCase(
        contactRepository: ContactRepository,
        brainTagRepository: BrainTagRepository,
        privacyRepository: PrivacyRepository,
        aiRepository: AiRepository,
        aiProviderRepository: AiProviderRepository,
        promptBuilder: PromptBuilder,
        sessionContextService: SessionContextService,
        userProfileContextBuilder: UserProfileContextBuilder
    ): GenerateReplyUseCase {
        return GenerateReplyUseCase(
            contactRepository = contactRepository,
            brainTagRepository = brainTagRepository,
            privacyRepository = privacyRepository,
            aiRepository = aiRepository,
            aiProviderRepository = aiProviderRepository,
            promptBuilder = promptBuilder,
            sessionContextService = sessionContextService,
            userProfileContextBuilder = userProfileContextBuilder
        )
    }

    /**
     * 提供微调重新生成用例
     *
     * 【重要设计决策】
     * RefinementUseCase 直接调用 AiRepository，而不是原始的 UseCase。
     * 原因：原始 UseCase 会将用户输入保存到对话历史记录中。如果重新生成时
     * 再次调用这些 UseCase，会导致同一条输入被重复保存到历史记录中。
     */
    @Provides
    @Singleton
    fun provideRefinementUseCase(
        aiRepository: AiRepository,
        aiProviderRepository: AiProviderRepository
    ): RefinementUseCase {
        return RefinementUseCase(
            aiRepository = aiRepository,
            aiProviderRepository = aiProviderRepository
        )
    }
}
