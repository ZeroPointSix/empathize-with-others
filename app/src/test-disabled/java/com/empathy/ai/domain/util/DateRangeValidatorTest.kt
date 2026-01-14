package com.empathy.ai.domain.util

import com.empathy.ai.domain.model.ContactProfile
import com.empathy.ai.domain.model.DateRange
import com.empathy.ai.domain.repository.ContactRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * DateRangeValidator 单元测试
 */
class DateRangeValidatorTest {

    private lateinit var contactRepository: ContactRepository
    private lateinit var validator: DateRangeValidator

    private val testContactId = "contact-123"
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val today = LocalDate.now()

    @Before
    fun setup() {
        contactRepository = mockk()
        validator = DateRangeValidator(contactRepository)

        // 默认返回一个有效的联系人
        coEvery { contactRepository.getProfile(testContactId) } returns Result.success(
            createTestProfile()
        )
    }

    // ==================== 有效日期范围测试 ====================

    @Test
    fun `验证有效日期范围返回Valid`() = runTest {
        val dateRange = DateRange.lastSevenDays()

        val result = validator.validate(dateRange, testContactId)

        assertTrue(result.isSuccess)
        assertEquals(DateRangeValidator.ValidationResult.Valid, result.getOrNull())
    }

    @Test
    fun `验证单日范围返回Valid`() = runTest {
        val todayStr = today.format(formatter)
        val dateRange = DateRange(todayStr, todayStr)

        val result = validator.validate(dateRange, testContactId)

        assertTrue(result.isSuccess)
        assertEquals(DateRangeValidator.ValidationResult.Valid, result.getOrNull())
    }

    @Test
    fun `验证30天范围返回Valid`() = runTest {
        val dateRange = DateRange(
            startDate = today.minusDays(29).format(formatter),
            endDate = today.format(formatter)
        )

        val result = validator.validate(dateRange, testContactId)

        assertTrue(result.isSuccess)
        assertEquals(DateRangeValidator.ValidationResult.Valid, result.getOrNull())
    }

    // ==================== 无效日期范围测试 ====================

    @Test
    fun `开始日期晚于结束日期返回Invalid`() = runTest {
        val dateRange = DateRange(
            startDate = today.format(formatter),
            endDate = today.minusDays(5).format(formatter)
        )

        val result = validator.validate(dateRange, testContactId)

        assertTrue(result.isSuccess)
        val validation = result.getOrNull()
        assertTrue(validation is DateRangeValidator.ValidationResult.Invalid)
        assertEquals("开始日期不能晚于结束日期", (validation as DateRangeValidator.ValidationResult.Invalid).message)
    }

    @Test
    fun `结束日期晚于今天返回Invalid`() = runTest {
        val dateRange = DateRange(
            startDate = today.format(formatter),
            endDate = today.plusDays(1).format(formatter)
        )

        val result = validator.validate(dateRange, testContactId)

        assertTrue(result.isSuccess)
        val validation = result.getOrNull()
        assertTrue(validation is DateRangeValidator.ValidationResult.Invalid)
        assertEquals("结束日期不能晚于今天", (validation as DateRangeValidator.ValidationResult.Invalid).message)
    }

    @Test
    fun `联系人不存在返回Invalid`() = runTest {
        coEvery { contactRepository.getProfile(testContactId) } returns Result.success(null)

        val dateRange = DateRange.lastSevenDays()

        val result = validator.validate(dateRange, testContactId)

        assertTrue(result.isSuccess)
        val validation = result.getOrNull()
        assertTrue(validation is DateRangeValidator.ValidationResult.Invalid)
        assertEquals("联系人不存在", (validation as DateRangeValidator.ValidationResult.Invalid).message)
    }

    @Test
    fun `超过90天返回Invalid`() = runTest {
        val dateRange = DateRange(
            startDate = today.minusDays(91).format(formatter),
            endDate = today.format(formatter)
        )

        val result = validator.validate(dateRange, testContactId)

        assertTrue(result.isSuccess)
        val validation = result.getOrNull()
        assertTrue(validation is DateRangeValidator.ValidationResult.Invalid)
        assertTrue((validation as DateRangeValidator.ValidationResult.Invalid).message.contains("90"))
    }

    // ==================== 警告测试 ====================

    @Test
    fun `超过30天返回Warning`() = runTest {
        val dateRange = DateRange(
            startDate = today.minusDays(45).format(formatter),
            endDate = today.format(formatter)
        )

        val result = validator.validate(dateRange, testContactId)

        assertTrue(result.isSuccess)
        val validation = result.getOrNull()
        assertTrue(validation is DateRangeValidator.ValidationResult.Warning)
        assertTrue((validation as DateRangeValidator.ValidationResult.Warning).message.contains("46"))
    }

    @Test
    fun `正好31天返回Warning`() = runTest {
        val dateRange = DateRange(
            startDate = today.minusDays(30).format(formatter),
            endDate = today.format(formatter)
        )

        val result = validator.validate(dateRange, testContactId)

        assertTrue(result.isSuccess)
        val validation = result.getOrNull()
        assertTrue(validation is DateRangeValidator.ValidationResult.Warning)
    }

    // ==================== 快速验证测试 ====================

    @Test
    fun `快速验证有效范围返回Valid`() {
        val dateRange = DateRange.lastSevenDays()

        val result = validator.validateQuick(dateRange)

        assertEquals(DateRangeValidator.ValidationResult.Valid, result)
    }

    @Test
    fun `快速验证开始晚于结束返回Invalid`() {
        val dateRange = DateRange(
            startDate = today.format(formatter),
            endDate = today.minusDays(5).format(formatter)
        )

        val result = validator.validateQuick(dateRange)

        assertTrue(result is DateRangeValidator.ValidationResult.Invalid)
    }

    @Test
    fun `快速验证超过30天返回Warning`() {
        val dateRange = DateRange(
            startDate = today.minusDays(45).format(formatter),
            endDate = today.format(formatter)
        )

        val result = validator.validateQuick(dateRange)

        assertTrue(result is DateRangeValidator.ValidationResult.Warning)
    }

    // ==================== 边界情况测试 ====================

    @Test
    fun `Repository异常时返回失败`() = runTest {
        coEvery { contactRepository.getProfile(testContactId) } throws RuntimeException("Database error")

        val dateRange = DateRange.lastSevenDays()

        val result = validator.validate(dateRange, testContactId)

        assertTrue(result.isFailure)
    }

    @Test
    fun `正好90天返回Warning而非Invalid`() = runTest {
        val dateRange = DateRange(
            startDate = today.minusDays(89).format(formatter),
            endDate = today.format(formatter)
        )

        val result = validator.validate(dateRange, testContactId)

        assertTrue(result.isSuccess)
        val validation = result.getOrNull()
        assertTrue(validation is DateRangeValidator.ValidationResult.Warning)
    }

    // ==================== 辅助方法 ====================

    private fun createTestProfile(
        id: String = testContactId
    ) = ContactProfile(
        id = id,
        name = "Test Contact",
        targetGoal = "测试目标"
    )
}
