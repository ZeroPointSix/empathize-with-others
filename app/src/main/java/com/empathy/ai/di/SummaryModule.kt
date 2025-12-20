package com.empathy.ai.di

import com.empathy.ai.domain.repository.AiProviderRepository
import com.empathy.ai.domain.repository.AiRepository
import com.empathy.ai.domain.repository.BrainTagRepository
import com.empathy.ai.domain.repository.ContactRepository
import com.empathy.ai.domain.repository.ConversationRepository
import com.empathy.ai.domain.repository.DailySummaryRepository
import com.empathy.ai.domain.usecase.ManualSummaryUseCase
import com.empathy.ai.domain.util.ContextBuilder
import com.empathy.ai.domain.util.DateRangeValidator
import com.empathy.ai.domain.util.PromptBuilder
import com.empathy.ai.domain.util.SummaryConflictChecker
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Singleton

/**
 * 手动总结功能依赖注入模块
 *
 * 提供手动触发AI总结功能所需的依赖：
 * - DateRangeValidator: 日期范围验证器
 * - SummaryConflictChecker: 冲突检测器
 * - ManualSummaryUseCase: 手动总结用例
 */
@Module
@InstallIn(SingletonComponent::class)
object SummaryModule {

    /**
     * 提供日期范围验证器
     */
    @Provides
    @Singleton
    fun provideDateRangeValidator(
        contactRepository: ContactRepository
    ): DateRangeValidator {
        return DateRangeValidator(contactRepository)
    }

    /**
     * 提供冲突检测器
     */
    @Provides
    @Singleton
    fun provideSummaryConflictChecker(
        dailySummaryRepository: DailySummaryRepository
    ): SummaryConflictChecker {
        return SummaryConflictChecker(dailySummaryRepository)
    }

    /**
     * 提供手动总结用例
     */
    @Provides
    @Singleton
    fun provideManualSummaryUseCase(
        conversationRepository: ConversationRepository,
        dailySummaryRepository: DailySummaryRepository,
        contactRepository: ContactRepository,
        brainTagRepository: BrainTagRepository,
        aiRepository: AiRepository,
        aiProviderRepository: AiProviderRepository,
        dateRangeValidator: DateRangeValidator,
        conflictChecker: SummaryConflictChecker,
        contextBuilder: ContextBuilder,
        promptBuilder: PromptBuilder,
        moshi: Moshi,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): ManualSummaryUseCase {
        return ManualSummaryUseCase(
            conversationRepository = conversationRepository,
            dailySummaryRepository = dailySummaryRepository,
            contactRepository = contactRepository,
            brainTagRepository = brainTagRepository,
            aiRepository = aiRepository,
            aiProviderRepository = aiProviderRepository,
            dateRangeValidator = dateRangeValidator,
            conflictChecker = conflictChecker,
            contextBuilder = contextBuilder,
            promptBuilder = promptBuilder,
            moshi = moshi,
            ioDispatcher = ioDispatcher
        )
    }
}
