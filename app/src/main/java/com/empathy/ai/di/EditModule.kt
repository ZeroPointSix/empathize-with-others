package com.empathy.ai.di

import com.empathy.ai.domain.repository.ContactRepository
import com.empathy.ai.domain.repository.ConversationRepository
import com.empathy.ai.domain.repository.DailySummaryRepository
import com.empathy.ai.domain.usecase.EditContactInfoUseCase
import com.empathy.ai.domain.usecase.EditConversationUseCase
import com.empathy.ai.domain.usecase.EditFactUseCase
import com.empathy.ai.domain.usecase.EditSummaryUseCase
import com.empathy.ai.domain.util.ContentValidator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Singleton

/**
 * 编辑功能依赖注入模块
 *
 * 提供事实流内容编辑相关的依赖注入配置
 */
@Module
@InstallIn(SingletonComponent::class)
object EditModule {

    /**
     * 提供内容验证器
     */
    @Provides
    @Singleton
    fun provideContentValidator(): ContentValidator {
        return ContentValidator()
    }

    /**
     * 提供事实编辑用例
     */
    @Provides
    @Singleton
    fun provideEditFactUseCase(
        contactRepository: ContactRepository,
        contentValidator: ContentValidator,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): EditFactUseCase {
        return EditFactUseCase(contactRepository, contentValidator, ioDispatcher)
    }

    /**
     * 提供对话编辑用例
     */
    @Provides
    @Singleton
    fun provideEditConversationUseCase(
        conversationRepository: ConversationRepository,
        contentValidator: ContentValidator,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): EditConversationUseCase {
        return EditConversationUseCase(conversationRepository, contentValidator, ioDispatcher)
    }

    /**
     * 提供总结编辑用例
     */
    @Provides
    @Singleton
    fun provideEditSummaryUseCase(
        dailySummaryRepository: DailySummaryRepository,
        contentValidator: ContentValidator,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): EditSummaryUseCase {
        return EditSummaryUseCase(dailySummaryRepository, contentValidator, ioDispatcher)
    }

    /**
     * 提供联系人信息编辑用例
     */
    @Provides
    @Singleton
    fun provideEditContactInfoUseCase(
        contactRepository: ContactRepository,
        contentValidator: ContentValidator,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): EditContactInfoUseCase {
        return EditContactInfoUseCase(contactRepository, contentValidator, ioDispatcher)
    }
}
