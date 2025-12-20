package com.empathy.ai.domain.model

import org.junit.Assert.*
import org.junit.Test

/**
 * ConflictResult 和 ConflictResolution 单元测试
 */
class ConflictResultTest {

    @Test
    fun `NoConflict应该是单例`() {
        assertSame(ConflictResult.NoConflict, ConflictResult.NoConflict)
    }

    @Test
    fun `HasConflict应该正确计算冲突数量`() {
        val conflict = ConflictResult.HasConflict(
            existingSummaries = emptyList(),
            conflictDates = listOf("2025-12-10", "2025-12-11", "2025-12-12")
        )
        assertEquals(3, conflict.conflictCount)
    }

    @Test
    fun `HasConflict空冲突日期应该返回0`() {
        val conflict = ConflictResult.HasConflict(
            existingSummaries = emptyList(),
            conflictDates = emptyList()
        )
        assertEquals(0, conflict.conflictCount)
    }

    @Test
    fun `HasConflict应该保存已存在的总结列表`() {
        val summaries = listOf(
            createTestSummary("2025-12-10"),
            createTestSummary("2025-12-11")
        )
        val conflict = ConflictResult.HasConflict(
            existingSummaries = summaries,
            conflictDates = listOf("2025-12-10", "2025-12-11")
        )
        assertEquals(2, conflict.existingSummaries.size)
    }

    @Test
    fun `ConflictResolution OVERWRITE的displayName应该正确`() {
        assertEquals("覆盖现有总结", ConflictResolution.OVERWRITE.displayName)
    }

    @Test
    fun `ConflictResolution FILL_GAPS的displayName应该正确`() {
        assertEquals("仅补充缺失日期", ConflictResolution.FILL_GAPS.displayName)
    }

    @Test
    fun `ConflictResolution CANCEL的displayName应该正确`() {
        assertEquals("取消", ConflictResolution.CANCEL.displayName)
    }

    @Test
    fun `应该有三种冲突处理方式`() {
        assertEquals(3, ConflictResolution.entries.size)
    }

    private fun createTestSummary(date: String): DailySummary {
        return DailySummary(
            id = 1,
            contactId = "test-contact",
            summaryDate = date,
            content = "Test content",
            keyEvents = emptyList(),
            newFacts = emptyList(),
            updatedTags = emptyList(),
            relationshipScoreChange = 0,
            relationshipTrend = RelationshipTrend.STABLE
        )
    }
}
