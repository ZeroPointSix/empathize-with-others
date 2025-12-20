package com.empathy.ai.domain.model

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * 日期范围模型
 *
 * 表示一个日期范围，用于手动总结的日期选择
 *
 * @property startDate 开始日期，格式: "yyyy-MM-dd"
 * @property endDate 结束日期，格式: "yyyy-MM-dd"
 */
data class DateRange(
    val startDate: String,
    val endDate: String
) {
    init {
        require(startDate.matches(DATE_PATTERN)) {
            "startDate格式必须为yyyy-MM-dd"
        }
        require(endDate.matches(DATE_PATTERN)) {
            "endDate格式必须为yyyy-MM-dd"
        }
        require(getStartLocalDate() <= getEndLocalDate()) {
            "startDate不能晚于endDate"
        }
    }

    /**
     * 获取开始日期的LocalDate对象
     */
    fun getStartLocalDate(): LocalDate = LocalDate.parse(startDate, FORMATTER)

    /**
     * 获取结束日期的LocalDate对象
     */
    fun getEndLocalDate(): LocalDate = LocalDate.parse(endDate, FORMATTER)

    /**
     * 计算日期范围的天数
     */
    fun getDayCount(): Int {
        return ChronoUnit.DAYS.between(getStartLocalDate(), getEndLocalDate()).toInt() + 1
    }

    /**
     * 获取范围内所有日期的字符串列表
     */
    fun getAllDates(): List<String> {
        val start = getStartLocalDate()
        val end = getEndLocalDate()
        return generateSequence(start) { it.plusDays(1) }
            .takeWhile { !it.isAfter(end) }
            .map { it.format(FORMATTER) }
            .toList()
    }

    /**
     * 判断指定日期是否在范围内
     *
     * @param date 日期字符串，格式: "yyyy-MM-dd"
     */
    fun contains(date: String): Boolean {
        val target = LocalDate.parse(date, FORMATTER)
        return !target.isBefore(getStartLocalDate()) && !target.isAfter(getEndLocalDate())
    }

    /**
     * 获取显示用的日期范围文本
     */
    fun getDisplayText(): String {
        return if (startDate == endDate) {
            startDate
        } else {
            "$startDate 至 $endDate"
        }
    }

    companion object {
        private val FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        private val DATE_PATTERN = Regex("\\d{4}-\\d{2}-\\d{2}")

        /**
         * 最近7天
         */
        fun lastSevenDays(): DateRange {
            val today = LocalDate.now()
            return DateRange(
                startDate = today.minusDays(6).format(FORMATTER),
                endDate = today.format(FORMATTER)
            )
        }

        /**
         * 本月
         */
        fun thisMonth(): DateRange {
            val today = LocalDate.now()
            return DateRange(
                startDate = today.withDayOfMonth(1).format(FORMATTER),
                endDate = today.format(FORMATTER)
            )
        }

        /**
         * 上月
         */
        fun lastMonth(): DateRange {
            val today = LocalDate.now()
            val lastMonth = today.minusMonths(1)
            return DateRange(
                startDate = lastMonth.withDayOfMonth(1).format(FORMATTER),
                endDate = lastMonth.withDayOfMonth(lastMonth.lengthOfMonth()).format(FORMATTER)
            )
        }

        /**
         * 最近30天
         */
        fun lastThirtyDays(): DateRange {
            val today = LocalDate.now()
            return DateRange(
                startDate = today.minusDays(29).format(FORMATTER),
                endDate = today.format(FORMATTER)
            )
        }

        /**
         * 从指定日期到今天
         *
         * @param startDate 开始日期
         */
        fun fromDateToToday(startDate: String): DateRange {
            val today = LocalDate.now()
            return DateRange(
                startDate = startDate,
                endDate = today.format(FORMATTER)
            )
        }

        /**
         * 创建单日范围
         *
         * @param date 日期
         */
        fun singleDay(date: String): DateRange {
            return DateRange(startDate = date, endDate = date)
        }
    }
}
