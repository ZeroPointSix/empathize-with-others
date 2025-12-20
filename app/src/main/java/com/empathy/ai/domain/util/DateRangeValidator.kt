package com.empathy.ai.domain.util

import com.empathy.ai.domain.model.DateRange
import com.empathy.ai.domain.repository.ContactRepository
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 日期范围验证器
 *
 * 负责验证手动总结的日期范围是否有效
 *
 * 验证规则：
 * 1. 开始日期不能晚于结束日期
 * 2. 结束日期不能晚于今天
 * 3. 开始日期不能早于联系人创建日期
 * 4. 范围不能超过90天（超过30天会警告）
 */
@Singleton
class DateRangeValidator @Inject constructor(
    private val contactRepository: ContactRepository
) {
    companion object {
        /** 默认最大天数（超过会警告） */
        const val DEFAULT_MAX_DAYS = 30

        /** 扩展最大天数（超过会拒绝） */
        const val EXTENDED_MAX_DAYS = 90
    }

    /**
     * 验证结果密封类
     */
    sealed class ValidationResult {
        /**
         * 验证通过
         */
        object Valid : ValidationResult()

        /**
         * 验证通过但有警告
         *
         * @param message 警告信息
         */
        data class Warning(val message: String) : ValidationResult()

        /**
         * 验证失败
         *
         * @param message 错误信息
         */
        data class Invalid(val message: String) : ValidationResult()
    }

    /**
     * 验证日期范围
     *
     * @param dateRange 日期范围
     * @param contactId 联系人ID
     * @return 验证结果
     */
    suspend fun validate(
        dateRange: DateRange,
        contactId: String
    ): Result<ValidationResult> {
        return try {
            val start = dateRange.getStartLocalDate()
            val end = dateRange.getEndLocalDate()
            val today = LocalDate.now()

            // 规则1: 开始日期不能晚于结束日期
            if (start.isAfter(end)) {
                return Result.success(
                    ValidationResult.Invalid("开始日期不能晚于结束日期")
                )
            }

            // 规则2: 结束日期不能晚于今天
            if (end.isAfter(today)) {
                return Result.success(
                    ValidationResult.Invalid("结束日期不能晚于今天")
                )
            }

            // 规则3: 检查联系人是否存在
            // 注意：ContactProfile模型当前没有createdAt字段，跳过创建日期检查
            val profile = contactRepository.getProfile(contactId).getOrNull()
            if (profile == null) {
                return Result.success(
                    ValidationResult.Invalid("联系人不存在")
                )
            }

            // 规则4: 检查范围天数
            val dayCount = dateRange.getDayCount()
            when {
                dayCount > EXTENDED_MAX_DAYS -> {
                    return Result.success(
                        ValidationResult.Invalid("日期范围不能超过${EXTENDED_MAX_DAYS}天")
                    )
                }
                dayCount > DEFAULT_MAX_DAYS -> {
                    return Result.success(
                        ValidationResult.Warning(
                            "您选择了${dayCount}天的时间范围，这可能需要较长时间处理"
                        )
                    )
                }
            }

            Result.success(ValidationResult.Valid)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 快速验证日期范围（不检查联系人创建日期）
     *
     * @param dateRange 日期范围
     * @return 验证结果
     */
    fun validateQuick(dateRange: DateRange): ValidationResult {
        val start = dateRange.getStartLocalDate()
        val end = dateRange.getEndLocalDate()
        val today = LocalDate.now()

        // 规则1: 开始日期不能晚于结束日期
        if (start.isAfter(end)) {
            return ValidationResult.Invalid("开始日期不能晚于结束日期")
        }

        // 规则2: 结束日期不能晚于今天
        if (end.isAfter(today)) {
            return ValidationResult.Invalid("结束日期不能晚于今天")
        }

        // 规则3: 检查范围天数
        val dayCount = dateRange.getDayCount()
        return when {
            dayCount > EXTENDED_MAX_DAYS -> {
                ValidationResult.Invalid("日期范围不能超过${EXTENDED_MAX_DAYS}天")
            }
            dayCount > DEFAULT_MAX_DAYS -> {
                ValidationResult.Warning(
                    "您选择了${dayCount}天的时间范围，这可能需要较长时间处理"
                )
            }
            else -> ValidationResult.Valid
        }
    }
}
