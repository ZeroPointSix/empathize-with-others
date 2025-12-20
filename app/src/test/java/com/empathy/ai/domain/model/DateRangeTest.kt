package com.empathy.ai.domain.model

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * DateRange 单元测试
 */
class DateRangeTest {

    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    @Test
    fun `创建有效的日期范围应该成功`() {
        val range = DateRange("2025-12-01", "2025-12-15")
        assertEquals("2025-12-01", range.startDate)
        assertEquals("2025-12-15", range.endDate)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `startDate格式错误应该抛出异常`() {
        DateRange("2025/12/01", "2025-12-15")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `endDate格式错误应该抛出异常`() {
        DateRange("2025-12-01", "12-15-2025")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `startDate晚于endDate应该抛出异常`() {
        DateRange("2025-12-15", "2025-12-01")
    }

    @Test
    fun `相同日期的范围应该有效`() {
        val range = DateRange("2025-12-10", "2025-12-10")
        assertEquals(1, range.getDayCount())
    }

    @Test
    fun `getDayCount应该正确计算天数`() {
        val range = DateRange("2025-12-01", "2025-12-10")
        assertEquals(10, range.getDayCount())
    }

    @Test
    fun `getAllDates应该返回所有日期`() {
        val range = DateRange("2025-12-01", "2025-12-03")
        val dates = range.getAllDates()
        assertEquals(3, dates.size)
        assertEquals("2025-12-01", dates[0])
        assertEquals("2025-12-02", dates[1])
        assertEquals("2025-12-03", dates[2])
    }

    @Test
    fun `contains应该正确判断日期是否在范围内`() {
        val range = DateRange("2025-12-01", "2025-12-10")
        assertTrue(range.contains("2025-12-01"))
        assertTrue(range.contains("2025-12-05"))
        assertTrue(range.contains("2025-12-10"))
        assertFalse(range.contains("2025-11-30"))
        assertFalse(range.contains("2025-12-11"))
    }

    @Test
    fun `getDisplayText单日应该只显示一个日期`() {
        val range = DateRange("2025-12-10", "2025-12-10")
        assertEquals("2025-12-10", range.getDisplayText())
    }

    @Test
    fun `getDisplayText范围应该显示开始至结束`() {
        val range = DateRange("2025-12-01", "2025-12-10")
        assertEquals("2025-12-01 至 2025-12-10", range.getDisplayText())
    }

    @Test
    fun `lastSevenDays应该返回正确的范围`() {
        val range = DateRange.lastSevenDays()
        assertEquals(7, range.getDayCount())
        assertEquals(LocalDate.now().format(formatter), range.endDate)
    }

    @Test
    fun `thisMonth应该从本月1日开始`() {
        val range = DateRange.thisMonth()
        val today = LocalDate.now()
        assertEquals(today.withDayOfMonth(1).format(formatter), range.startDate)
        assertEquals(today.format(formatter), range.endDate)
    }

    @Test
    fun `lastMonth应该返回上月完整范围`() {
        val range = DateRange.lastMonth()
        val lastMonth = LocalDate.now().minusMonths(1)
        assertEquals(lastMonth.withDayOfMonth(1).format(formatter), range.startDate)
        assertEquals(lastMonth.withDayOfMonth(lastMonth.lengthOfMonth()).format(formatter), range.endDate)
    }

    @Test
    fun `lastThirtyDays应该返回30天范围`() {
        val range = DateRange.lastThirtyDays()
        assertEquals(30, range.getDayCount())
        assertEquals(LocalDate.now().format(formatter), range.endDate)
    }

    @Test
    fun `fromDateToToday应该从指定日期到今天`() {
        val startDate = "2025-12-01"
        val range = DateRange.fromDateToToday(startDate)
        assertEquals(startDate, range.startDate)
        assertEquals(LocalDate.now().format(formatter), range.endDate)
    }

    @Test
    fun `singleDay应该创建单日范围`() {
        val date = "2025-12-10"
        val range = DateRange.singleDay(date)
        assertEquals(date, range.startDate)
        assertEquals(date, range.endDate)
        assertEquals(1, range.getDayCount())
    }

    @Test
    fun `getStartLocalDate应该返回正确的LocalDate`() {
        val range = DateRange("2025-12-10", "2025-12-15")
        assertEquals(LocalDate.of(2025, 12, 10), range.getStartLocalDate())
    }

    @Test
    fun `getEndLocalDate应该返回正确的LocalDate`() {
        val range = DateRange("2025-12-10", "2025-12-15")
        assertEquals(LocalDate.of(2025, 12, 15), range.getEndLocalDate())
    }
}
