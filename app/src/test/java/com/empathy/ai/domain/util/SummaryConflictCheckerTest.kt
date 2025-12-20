package com.empathy.ai.domain.util

import com.empathy.ai.domain.model.ConflictResult
import com.empathy.ai.domain.model.DailySummary
import com.empathy.ai.domain.model.DateRange
import com.empathy.ai.domain.model.RelationshipTrend
import com.empathy.ai.domain.repository.DailySummaryRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * SummaryConflictChecker 单元测试
 */
class SummaryConflictCheckerTest {

    private lateinit var dailySummaryRepository: DailySummaryRepository
    private lateinit var checker: SummaryConflictChecker

    private val testContactId = "contact-123"
    private val testStartDate = "2025-12-01"
    private val testEndDate = "2025-12-10"

    @Before
    fun setup() {
        dailySummaryRepository = mockk()
        checker = SummaryConflictChecker(dailySummaryRepository)
    }

    // ==================== checkConflict 测试 ====================

    @Test
    fun `无已有总结时返回NoConflict`() = runTest {
        // Given
        val dateRange = DateRange(testStartDate, testEndDate)
        coEvery {
            dailySummaryRepository.getSummariesInRange(testContactId, testStartDate, testEndDate)
        } returns Result.success(emptyList())

        // When
        val result = checker.checkConflict(testContactId, dateRange)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(ConflictResult.NoConflict, result.getOrNull())
    }

    @Test
    fun `有已有总结时返回HasConflict`() = runTest {
        // Given
        val dateRange = DateRange(testStartDate, testEndDate)
        val existingSummaries = listOf(
            createTestSummary("2025-12-03"),
            createTestSummary("2025-12-05"),
            createTestSummary("2025-12-08")
        )
        coEvery {
            dailySummaryRepository.getSummariesInRange(testContactId, testStartDate, testEndDate)
        } returns Result.success(existingSummaries)

        // When
        val result = checker.checkConflict(testContactId, dateRange)

        // Then
        assertTrue(result.isSuccess)
        val conflict = result.getOrNull()
        assertTrue(conflict is ConflictResult.HasConflict)
        val hasConflict = conflict as ConflictResult.HasConflict
        assertEquals(3, hasConflict.conflictCount)
        assertEquals(listOf("2025-12-03", "2025-12-05", "2025-12-08"), hasConflict.conflictDates)
        assertEquals(3, hasConflict.existingSummaries.size)
    }

    @Test
    fun `单个冲突时返回正确的冲突信息`() = runTest {
        // Given
        val dateRange = DateRange(testStartDate, testEndDate)
        val existingSummaries = listOf(createTestSummary("2025-12-05"))
        coEvery {
            dailySummaryRepository.getSummariesInRange(testContactId, testStartDate, testEndDate)
        } returns Result.success(existingSummaries)

        // When
        val result = checker.checkConflict(testContactId, dateRange)

        // Then
        assertTrue(result.isSuccess)
        val conflict = result.getOrNull() as ConflictResult.HasConflict
        assertEquals(1, conflict.conflictCount)
        assertEquals("2025-12-05", conflict.conflictDates.first())
    }

    @Test
    fun `Repository返回空列表时视为无冲突`() = runTest {
        // Given
        val dateRange = DateRange(testStartDate, testEndDate)
        coEvery {
            dailySummaryRepository.getSummariesInRange(testContactId, testStartDate, testEndDate)
        } returns Result.success(emptyList())

        // When
        val result = checker.checkConflict(testContactId, dateRange)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(ConflictResult.NoConflict, result.getOrNull())
    }

    @Test
    fun `Repository异常时返回失败`() = runTest {
        // Given
        val dateRange = DateRange(testStartDate, testEndDate)
        coEvery {
            dailySummaryRepository.getSummariesInRange(testContactId, testStartDate, testEndDate)
        } throws RuntimeException("Database error")

        // When
        val result = checker.checkConflict(testContactId, dateRange)

        // Then
        assertTrue(result.isFailure)
    }

    // ==================== findMissingDates 测试 ====================

    @Test
    fun `所有日期都缺失时返回完整日期列表`() = runTest {
        // Given
        val dateRange = DateRange("2025-12-01", "2025-12-03")
        coEvery {
            dailySummaryRepository.getSummarizedDatesInRange(testContactId, "2025-12-01", "2025-12-03")
        } returns Result.success(emptyList())

        // When
        val result = checker.findMissingDates(testContactId, dateRange)

        // Then
        assertTrue(result.isSuccess)
        val missingDates = result.getOrNull()!!
        assertEquals(3, missingDates.size)
        assertEquals(listOf("2025-12-01", "2025-12-02", "2025-12-03"), missingDates)
    }

    @Test
    fun `部分日期已有总结时返回缺失日期`() = runTest {
        // Given
        val dateRange = DateRange("2025-12-01", "2025-12-05")
        coEvery {
            dailySummaryRepository.getSummarizedDatesInRange(testContactId, "2025-12-01", "2025-12-05")
        } returns Result.success(listOf("2025-12-02", "2025-12-04"))

        // When
        val result = checker.findMissingDates(testContactId, dateRange)

        // Then
        assertTrue(result.isSuccess)
        val missingDates = result.getOrNull()!!
        assertEquals(3, missingDates.size)
        assertEquals(listOf("2025-12-01", "2025-12-03", "2025-12-05"), missingDates)
    }

    @Test
    fun `所有日期都有总结时返回空列表`() = runTest {
        // Given
        val dateRange = DateRange("2025-12-01", "2025-12-03")
        coEvery {
            dailySummaryRepository.getSummarizedDatesInRange(testContactId, "2025-12-01", "2025-12-03")
        } returns Result.success(listOf("2025-12-01", "2025-12-02", "2025-12-03"))

        // When
        val result = checker.findMissingDates(testContactId, dateRange)

        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()!!.isEmpty())
    }

    @Test
    fun `findMissingDates Repository异常时返回失败`() = runTest {
        // Given
        val dateRange = DateRange(testStartDate, testEndDate)
        coEvery {
            dailySummaryRepository.getSummarizedDatesInRange(testContactId, testStartDate, testEndDate)
        } throws RuntimeException("Database error")

        // When
        val result = checker.findMissingDates(testContactId, dateRange)

        // Then
        assertTrue(result.isFailure)
    }

    // ==================== countMissingDates 测试 ====================

    @Test
    fun `countMissingDates返回正确数量`() = runTest {
        // Given
        val dateRange = DateRange(testStartDate, testEndDate)
        coEvery {
            dailySummaryRepository.countMissingDatesInRange(testContactId, testStartDate, testEndDate)
        } returns Result.success(5)

        // When
        val result = checker.countMissingDates(testContactId, dateRange)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(5, result.getOrNull())
    }

    // ==================== hasAnyConflict 测试 ====================

    @Test
    fun `hasAnyConflict有冲突时返回true`() = runTest {
        // Given
        val dateRange = DateRange(testStartDate, testEndDate)
        coEvery {
            dailySummaryRepository.getSummariesInRange(testContactId, testStartDate, testEndDate)
        } returns Result.success(listOf(createTestSummary("2025-12-05")))

        // When
        val result = checker.hasAnyConflict(testContactId, dateRange)

        // Then
        assertTrue(result)
    }

    @Test
    fun `hasAnyConflict无冲突时返回false`() = runTest {
        // Given
        val dateRange = DateRange(testStartDate, testEndDate)
        coEvery {
            dailySummaryRepository.getSummariesInRange(testContactId, testStartDate, testEndDate)
        } returns Result.success(emptyList())

        // When
        val result = checker.hasAnyConflict(testContactId, dateRange)

        // Then
        assertFalse(result)
    }

    // ==================== 辅助方法 ====================

    private fun createTestSummary(date: String) = DailySummary(
        id = 0,
        contactId = testContactId,
        summaryDate = date,
        content = "Test summary for $date",
        keyEvents = emptyList(),
        newFacts = emptyList(),
        updatedTags = emptyList(),
        relationshipScoreChange = 0,
        relationshipTrend = RelationshipTrend.STABLE
    )
}
