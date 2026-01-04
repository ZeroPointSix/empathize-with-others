package com.empathy.ai.data.repository

/**
 * DailySummaryRepositoryImpl 单元测试
 *
 * 测试每日总结仓库实现 (PRD-00011 手动触发AI总结功能):
 * - 基础CRUD操作 - saveSummary、getSummariesByContact、getSummaryByDate
 * - 存在性检查 - hasSummaryForDate
 * - 删除操作 - deleteByContactId、cleanupOldSummaries
 * - 范围查询 - getSummariesInRange、getSummarizedDatesInRange
 * - 类型过滤 - getManualSummaries、getSummariesByType
 * - 缺失日期统计 - countMissingDatesInRange
 * - 实体映射 - toEntity/toDomain方法验证
 *
 * 版本新增功能 (v9):
 * - 支持日期范围查询和统计
 * - 支持手动/自动生成总结区分
 * - 支持多种总结类型（DAILY、CUSTOM_RANGE等）
 *
 * 任务追踪:
 * - PRD-00011 - 手动触发AI总结功能需求
 * - TDD-00011 - 手动触发AI总结功能技术设计
 */
class DailySummaryRepositoryImplTest {

    private lateinit var dao: DailySummaryDao
    private lateinit var moshi: Moshi
    private lateinit var repository: DailySummaryRepositoryImpl

    private val testContactId = "contact-123"
    private val testDate = "2025-12-19"
    private val testStartDate = "2025-12-01"
    private val testEndDate = "2025-12-19"

    @Before
    fun setup() {
        dao = mockk()
        moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
        repository = DailySummaryRepositoryImpl(dao, moshi)
    }

    // ==================== 基础方法测试 ====================

    @Test
    fun `saveSummary should return success with id`() = runTest {
        // Given
        val summary = createTestDailySummary()
        coEvery { dao.insert(any()) } returns 1L

        // When
        val result = repository.saveSummary(summary)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(1L, result.getOrNull())
        coVerify { dao.insert(any()) }
    }

    @Test
    fun `saveSummary should return failure on exception`() = runTest {
        // Given
        val summary = createTestDailySummary()
        coEvery { dao.insert(any()) } throws RuntimeException("Database error")

        // When
        val result = repository.saveSummary(summary)

        // Then
        assertTrue(result.isFailure)
    }

    @Test
    fun `getSummariesByContact should return summaries list`() = runTest {
        // Given
        val entities = listOf(createTestEntity())
        coEvery { dao.getSummariesByContact(testContactId) } returns entities

        // When
        val result = repository.getSummariesByContact(testContactId)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.size)
    }

    @Test
    fun `getSummaryByDate should return summary when exists`() = runTest {
        // Given
        val entity = createTestEntity()
        coEvery { dao.getSummaryByDate(testContactId, testDate) } returns entity

        // When
        val result = repository.getSummaryByDate(testContactId, testDate)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(testContactId, result.getOrNull()?.contactId)
    }

    @Test
    fun `getSummaryByDate should return null when not exists`() = runTest {
        // Given
        coEvery { dao.getSummaryByDate(testContactId, testDate) } returns null

        // When
        val result = repository.getSummaryByDate(testContactId, testDate)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(null, result.getOrNull())
    }

    @Test
    fun `hasSummaryForDate should return true when exists`() = runTest {
        // Given
        coEvery { dao.hasSummaryForDate(testContactId, testDate) } returns true

        // When
        val result = repository.hasSummaryForDate(testContactId, testDate)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(true, result.getOrNull())
    }

    @Test
    fun `deleteByContactId should return deleted count`() = runTest {
        // Given
        coEvery { dao.deleteByContactId(testContactId) } returns 5

        // When
        val result = repository.deleteByContactId(testContactId)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(5, result.getOrNull())
    }

    @Test
    fun `cleanupOldSummaries should return deleted count`() = runTest {
        // Given
        val timestamp = System.currentTimeMillis()
        coEvery { dao.cleanupOldSummaries(timestamp) } returns 10

        // When
        val result = repository.cleanupOldSummaries(timestamp)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(10, result.getOrNull())
    }

    @Test
    fun `getRecentSummaries should return summaries list`() = runTest {
        // Given
        val entities = listOf(createTestEntity(), createTestEntity(id = 2))
        coEvery { dao.getRecentSummaries(testContactId, 7) } returns entities

        // When
        val result = repository.getRecentSummaries(testContactId, 7)

        // Then
        assertEquals(2, result.size)
    }

    @Test
    fun `getRecentSummaries should return empty list on exception`() = runTest {
        // Given
        coEvery { dao.getRecentSummaries(testContactId, 7) } throws RuntimeException("Error")

        // When
        val result = repository.getRecentSummaries(testContactId, 7)

        // Then
        assertTrue(result.isEmpty())
    }


    // ==================== v9 新增方法测试 ====================

    @Test
    fun `getSummariesInRange should return summaries in date range`() = runTest {
        // Given
        val entities = listOf(
            createTestEntity(summaryDate = "2025-12-01"),
            createTestEntity(id = 2, summaryDate = "2025-12-10"),
            createTestEntity(id = 3, summaryDate = "2025-12-19")
        )
        coEvery { dao.getSummariesInRange(testContactId, testStartDate, testEndDate) } returns entities

        // When
        val result = repository.getSummariesInRange(testContactId, testStartDate, testEndDate)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(3, result.getOrNull()?.size)
    }

    @Test
    fun `getSummariesInRange should return failure on exception`() = runTest {
        // Given
        coEvery { dao.getSummariesInRange(testContactId, testStartDate, testEndDate) } throws RuntimeException("Error")

        // When
        val result = repository.getSummariesInRange(testContactId, testStartDate, testEndDate)

        // Then
        assertTrue(result.isFailure)
    }

    @Test
    fun `getSummarizedDatesInRange should return date list`() = runTest {
        // Given
        val dates = listOf("2025-12-01", "2025-12-10", "2025-12-19")
        coEvery { dao.getSummarizedDatesInRange(testContactId, testStartDate, testEndDate) } returns dates

        // When
        val result = repository.getSummarizedDatesInRange(testContactId, testStartDate, testEndDate)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(3, result.getOrNull()?.size)
        assertEquals("2025-12-01", result.getOrNull()?.first())
    }

    @Test
    fun `getSummarizedDatesInRange should return failure on exception`() = runTest {
        // Given
        coEvery { dao.getSummarizedDatesInRange(testContactId, testStartDate, testEndDate) } throws RuntimeException("Error")

        // When
        val result = repository.getSummarizedDatesInRange(testContactId, testStartDate, testEndDate)

        // Then
        assertTrue(result.isFailure)
    }

    @Test
    fun `deleteSummariesInRange should return deleted count`() = runTest {
        // Given
        coEvery { dao.deleteSummariesInRange(testContactId, testStartDate, testEndDate) } returns 5

        // When
        val result = repository.deleteSummariesInRange(testContactId, testStartDate, testEndDate)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(5, result.getOrNull())
    }

    @Test
    fun `deleteSummariesInRange should return failure on exception`() = runTest {
        // Given
        coEvery { dao.deleteSummariesInRange(testContactId, testStartDate, testEndDate) } throws RuntimeException("Error")

        // When
        val result = repository.deleteSummariesInRange(testContactId, testStartDate, testEndDate)

        // Then
        assertTrue(result.isFailure)
    }

    @Test
    fun `getManualSummaries should return manual summaries`() = runTest {
        // Given
        val entities = listOf(
            createTestEntity(generationSource = "MANUAL"),
            createTestEntity(id = 2, generationSource = "MANUAL")
        )
        coEvery { dao.getManualSummaries(testContactId) } returns entities

        // When
        val result = repository.getManualSummaries(testContactId)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull()?.size)
        assertTrue(result.getOrNull()?.all { it.isManualGenerated() } == true)
    }

    @Test
    fun `getManualSummaries should return failure on exception`() = runTest {
        // Given
        coEvery { dao.getManualSummaries(testContactId) } throws RuntimeException("Error")

        // When
        val result = repository.getManualSummaries(testContactId)

        // Then
        assertTrue(result.isFailure)
    }

    @Test
    fun `countMissingDatesInRange should return missing count`() = runTest {
        // Given
        coEvery { dao.countMissingDatesInRange(testContactId, testStartDate, testEndDate) } returns 10

        // When
        val result = repository.countMissingDatesInRange(testContactId, testStartDate, testEndDate)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(10, result.getOrNull())
    }

    @Test
    fun `countMissingDatesInRange should return failure on exception`() = runTest {
        // Given
        coEvery { dao.countMissingDatesInRange(testContactId, testStartDate, testEndDate) } throws RuntimeException("Error")

        // When
        val result = repository.countMissingDatesInRange(testContactId, testStartDate, testEndDate)

        // Then
        assertTrue(result.isFailure)
    }

    @Test
    fun `getSummariesByType should return summaries of specified type`() = runTest {
        // Given
        val entities = listOf(
            createTestEntity(summaryType = "CUSTOM_RANGE"),
            createTestEntity(id = 2, summaryType = "CUSTOM_RANGE")
        )
        coEvery { dao.getSummariesByType(testContactId, "CUSTOM_RANGE") } returns entities

        // When
        val result = repository.getSummariesByType(testContactId, SummaryType.CUSTOM_RANGE)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull()?.size)
        assertTrue(result.getOrNull()?.all { it.isRangeSummary() } == true)
    }

    @Test
    fun `getSummariesByType should return failure on exception`() = runTest {
        // Given
        coEvery { dao.getSummariesByType(testContactId, "DAILY") } throws RuntimeException("Error")

        // When
        val result = repository.getSummariesByType(testContactId, SummaryType.DAILY)

        // Then
        assertTrue(result.isFailure)
    }

    // ==================== 映射测试 ====================

    @Test
    fun `toEntity should map all v9 fields correctly`() = runTest {
        // Given
        val summary = DailySummary(
            id = 1,
            contactId = testContactId,
            summaryDate = testDate,
            content = "Test content",
            keyEvents = emptyList(),
            newFacts = emptyList(),
            updatedTags = emptyList(),
            relationshipScoreChange = 5,
            relationshipTrend = RelationshipTrend.IMPROVING,
            startDate = testStartDate,
            endDate = testEndDate,
            summaryType = SummaryType.CUSTOM_RANGE,
            generationSource = GenerationSource.MANUAL,
            conversationCount = 10,
            generatedAt = 1234567890L
        )
        coEvery { dao.insert(any()) } returns 1L

        // When
        val result = repository.saveSummary(summary)

        // Then
        assertTrue(result.isSuccess)
        coVerify {
            dao.insert(match { entity ->
                entity.startDate == testStartDate &&
                entity.endDate == testEndDate &&
                entity.summaryType == "CUSTOM_RANGE" &&
                entity.generationSource == "MANUAL" &&
                entity.conversationCount == 10 &&
                entity.generatedAt == 1234567890L
            })
        }
    }

    @Test
    fun `toDomain should map all v9 fields correctly`() = runTest {
        // Given
        val entity = DailySummaryEntity(
            id = 1,
            contactId = testContactId,
            summaryDate = testDate,
            content = "Test content",
            keyEventsJson = "[]",
            relationshipScore = 5,
            createdAt = System.currentTimeMillis(),
            startDate = testStartDate,
            endDate = testEndDate,
            summaryType = "CUSTOM_RANGE",
            generationSource = "MANUAL",
            conversationCount = 10,
            generatedAt = 1234567890L
        )
        coEvery { dao.getSummaryByDate(testContactId, testDate) } returns entity

        // When
        val result = repository.getSummaryByDate(testContactId, testDate)

        // Then
        assertTrue(result.isSuccess)
        val summary = result.getOrNull()!!
        assertEquals(testStartDate, summary.startDate)
        assertEquals(testEndDate, summary.endDate)
        assertEquals(SummaryType.CUSTOM_RANGE, summary.summaryType)
        assertEquals(GenerationSource.MANUAL, summary.generationSource)
        assertEquals(10, summary.conversationCount)
        assertEquals(1234567890L, summary.generatedAt)
        assertTrue(summary.isRangeSummary())
        assertTrue(summary.isManualGenerated())
    }

    @Test
    fun `toDomain should handle invalid enum values gracefully`() = runTest {
        // Given
        val entity = DailySummaryEntity(
            id = 1,
            contactId = testContactId,
            summaryDate = testDate,
            content = "Test content",
            keyEventsJson = "[]",
            relationshipScore = 0,
            createdAt = System.currentTimeMillis(),
            summaryType = "INVALID_TYPE",
            generationSource = "INVALID_SOURCE"
        )
        coEvery { dao.getSummaryByDate(testContactId, testDate) } returns entity

        // When
        val result = repository.getSummaryByDate(testContactId, testDate)

        // Then
        assertTrue(result.isSuccess)
        val summary = result.getOrNull()!!
        assertEquals(SummaryType.DAILY, summary.summaryType)
        assertEquals(GenerationSource.AUTO, summary.generationSource)
    }

    // ==================== 辅助方法 ====================

    private fun createTestDailySummary(
        id: Long = 1,
        contactId: String = testContactId,
        summaryDate: String = testDate
    ) = DailySummary(
        id = id,
        contactId = contactId,
        summaryDate = summaryDate,
        content = "Test summary content",
        keyEvents = emptyList(),
        newFacts = emptyList(),
        updatedTags = emptyList(),
        relationshipScoreChange = 0,
        relationshipTrend = RelationshipTrend.STABLE
    )

    private fun createTestEntity(
        id: Long = 1,
        contactId: String = testContactId,
        summaryDate: String = testDate,
        summaryType: String = "DAILY",
        generationSource: String = "AUTO"
    ) = DailySummaryEntity(
        id = id,
        contactId = contactId,
        summaryDate = summaryDate,
        content = "Test content",
        keyEventsJson = "[]",
        relationshipScore = 0,
        createdAt = System.currentTimeMillis(),
        summaryType = summaryType,
        generationSource = generationSource
    )
}
