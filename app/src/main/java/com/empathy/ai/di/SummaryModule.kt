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
 * 对话总结功能依赖注入模块
 *
 * 提供手动触发AI总结功能所需的依赖注入配置。
 *
 * 业务背景 (PRD-00011):
 *   - 用户可以手动选择日期范围，对话生成AI总结
 *   - 需要验证日期范围有效性并检测冲突
 *
 * 依赖说明:
 *   1. DateRangeValidator - 日期范围验证器
 *      - 验证用户选择的日期范围是否有效
 *      - 检查是否有足够的对话数据
 *      - 验证日期范围是否在有效历史范围内
 *
 *   2. SummaryConflictChecker - 总结冲突检测器
 *      - 检测指定日期范围是否已有总结
 *      - 避免重复生成相同日期的总结
 *      - 提供冲突解决建议
 *
 *   注意: ManualSummaryUseCase已有@Inject constructor，Hilt自动处理
 *
 * 设计权衡:
 *   - 为什么分离验证器和冲突检测器？
 *       → 单一职责，便于独立测试和复用
 *   - 为什么验证器需要ContactRepository？
 *       → 检查联系人是否存在及对话数据完整性
 *
 * @see TDD-00011 手动触发AI总结功能技术设计
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
