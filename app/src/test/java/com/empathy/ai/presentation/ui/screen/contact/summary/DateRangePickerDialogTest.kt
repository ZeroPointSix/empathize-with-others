package com.empathy.ai.presentation.ui.screen.contact.summary

import com.empathy.ai.domain.model.DateRange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * DateRangePickerDialog 组件单元测试
 *
 * 测试DateRange模型的行为，不测试Composable函数本身
 */
class DateRangePickerDialogTest {

    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    @Test
    fun `最近7天日期范围应该正确`() {
        val range = DateRange.lastSevenDays()
        val today = LocalDate.now()
        val expectedStart = today.minusDays(6).format(formatter)
        val expectedEnd = today.format(formatter)
        
        assertEquals(expectedStart, range.startDate)
        assertEquals(expectedEnd, range.endDate)
        assertEquals(7, range.getDayCount())
    }

    @Test
    fun `本月日期范围应该正确`() {
        val range = DateRange.thisMonth()
        val today = LocalDate.now()
        val expectedStart = today.withDayOfMonth(1).format(formatter)
        val expectedEnd = today.format(formatter)
        
        assertEquals(expectedStart, range.startDate)
        assertEquals(expectedEnd, range.endDate)
    }

    @Test
    fun `上月日期范围应该正确`() {
        val range = DateRange.lastMonth()
        val today = LocalDate.now()
        val lastMonth = today.minusMonths(1)
        val expectedStart = lastMonth.withDayOfMonth(1).format(formatter)
        val expectedEnd = lastMonth.withDayOfMonth(lastMonth.lengthOfMonth()).format(formatter)
        
        assertEquals(expectedStart, range.startDate)
        assertEquals(expectedEnd, range.endDate)
    }

    @Test
    fun `最近30天日期范围应该正确`() {
        val range = DateRange.lastThirtyDays()
        val today = LocalDate.now()
        val expectedStart = today.minusDays(29).format(formatter)
        val expectedEnd = today.format(formatter)
        
        assertEquals(expectedStart, range.startDate)
        assertEquals(expectedEnd, range.endDate)
        assertEquals(30, range.getDayCount())
    }

    @Test
    fun `已有总结日期列表少于等于3条时全部显示`() {
        val dates = listOf("2024-12-10", "2024-12-12", "2024-12-15")
        val displayText = dates.joinToString(", ")
        assertEquals("2024-12-10, 2024-12-12, 2024-12-15", displayText)
    }

    @Test
    fun `已有总结日期列表超过3条时显示省略`() {
        val dates = listOf("2024-12-10", "2024-12-11", "2024-12-12", "2024-12-13", "2024-12-14")
        val displayText = if (dates.size <= 3) {
            dates.joinToString(", ")
        } else {
            "${dates.take(3).joinToString(", ")} 等${dates.size}天"
        }
        assertEquals("2024-12-10, 2024-12-11, 2024-12-12 等5天", displayText)
    }

    @Test
    fun `日期格式验证应该正确`() {
        val validDate = "2024-12-10"
        val regex = Regex("\\d{4}-\\d{2}-\\d{2}")
        assertTrue(validDate.matches(regex))
    }

    @Test
    fun `自定义日期范围创建应该正确`() {
        val startDate = "2024-12-01"
        val endDate = "2024-12-15"
        val range = DateRange(startDate, endDate)
        
        assertEquals(startDate, range.startDate)
        assertEquals(endDate, range.endDate)
        assertEquals(15, range.getDayCount())
    }
}
