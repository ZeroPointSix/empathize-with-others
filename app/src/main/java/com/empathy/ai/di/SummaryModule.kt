package com.empathy.ai.di

import com.empathy.ai.domain.repository.ContactRepository
import com.empathy.ai.domain.repository.DailySummaryRepository
import com.empathy.ai.domain.util.DateRangeValidator
import com.empathy.ai.domain.util.SummaryConflictChecker
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 手动总结功能依赖注入模块
 *
 * 提供手动触发AI总结功能所需的依赖：
 * - DateRangeValidator: 日期范围验证器
 * - SummaryConflictChecker: 冲突检测器
 *
 * 注意：ManualSummaryUseCase已有@Inject constructor，Hilt会自动处理依赖注入。
 *
 * @see TDD-00017 Clean Architecture模块化改造技术设计
 */
@Module
@InstallIn(SingletonComponent::class)
object SummaryModule {

    /**
     * 提供日期范围验证器
     *
     * DateRangeValidator没有@Inject注解，需要手动提供
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
     *
     * SummaryConflictChecker没有@Inject注解，需要手动提供
     */
    @Provides
    @Singleton
    fun provideSummaryConflictChecker(
        dailySummaryRepository: DailySummaryRepository
    ): SummaryConflictChecker {
        return SummaryConflictChecker(dailySummaryRepository)
    }

    // ManualSummaryUseCase已有@Inject constructor，Hilt自动处理依赖注入
}
