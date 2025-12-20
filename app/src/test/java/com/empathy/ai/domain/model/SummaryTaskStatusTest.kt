package com.empathy.ai.domain.model

import org.junit.Assert.*
import org.junit.Test

/**
 * SummaryTaskStatus 单元测试
 */
class SummaryTaskStatusTest {

    @Test
    fun `IDLE状态应该不是终态`() {
        assertFalse(SummaryTaskStatus.IDLE.isTerminal())
    }

    @Test
    fun `SUCCESS状态应该是终态`() {
        assertTrue(SummaryTaskStatus.SUCCESS.isTerminal())
    }

    @Test
    fun `FAILED状态应该是终态`() {
        assertTrue(SummaryTaskStatus.FAILED.isTerminal())
    }

    @Test
    fun `CANCELLED状态应该是终态`() {
        assertTrue(SummaryTaskStatus.CANCELLED.isTerminal())
    }

    @Test
    fun `FETCHING_DATA状态应该可取消`() {
        assertTrue(SummaryTaskStatus.FETCHING_DATA.isCancellable())
    }

    @Test
    fun `ANALYZING状态应该可取消`() {
        assertTrue(SummaryTaskStatus.ANALYZING.isCancellable())
    }

    @Test
    fun `GENERATING状态应该可取消`() {
        assertTrue(SummaryTaskStatus.GENERATING.isCancellable())
    }

    @Test
    fun `SAVING状态应该不可取消`() {
        assertFalse(SummaryTaskStatus.SAVING.isCancellable())
    }

    @Test
    fun `SUCCESS状态应该不可取消`() {
        assertFalse(SummaryTaskStatus.SUCCESS.isCancellable())
    }

    @Test
    fun `IDLE状态应该不可取消`() {
        assertFalse(SummaryTaskStatus.IDLE.isCancellable())
    }

    @Test
    fun `FETCHING_DATA状态应该正在执行中`() {
        assertTrue(SummaryTaskStatus.FETCHING_DATA.isInProgress())
    }

    @Test
    fun `ANALYZING状态应该正在执行中`() {
        assertTrue(SummaryTaskStatus.ANALYZING.isInProgress())
    }

    @Test
    fun `GENERATING状态应该正在执行中`() {
        assertTrue(SummaryTaskStatus.GENERATING.isInProgress())
    }

    @Test
    fun `SAVING状态应该正在执行中`() {
        assertTrue(SummaryTaskStatus.SAVING.isInProgress())
    }

    @Test
    fun `IDLE状态应该不在执行中`() {
        assertFalse(SummaryTaskStatus.IDLE.isInProgress())
    }

    @Test
    fun `SUCCESS状态应该不在执行中`() {
        assertFalse(SummaryTaskStatus.SUCCESS.isInProgress())
    }

    @Test
    fun `FAILED状态应该不在执行中`() {
        assertFalse(SummaryTaskStatus.FAILED.isInProgress())
    }

    @Test
    fun `各状态的displayName应该正确`() {
        assertEquals("空闲", SummaryTaskStatus.IDLE.displayName)
        assertEquals("获取数据", SummaryTaskStatus.FETCHING_DATA.displayName)
        assertEquals("AI分析中", SummaryTaskStatus.ANALYZING.displayName)
        assertEquals("生成总结", SummaryTaskStatus.GENERATING.displayName)
        assertEquals("保存结果", SummaryTaskStatus.SAVING.displayName)
        assertEquals("完成", SummaryTaskStatus.SUCCESS.displayName)
        assertEquals("失败", SummaryTaskStatus.FAILED.displayName)
        assertEquals("已取消", SummaryTaskStatus.CANCELLED.displayName)
    }

    @Test
    fun `各状态的progressRange应该正确`() {
        assertEquals(0f..0f, SummaryTaskStatus.IDLE.progressRange)
        assertEquals(0f..0.2f, SummaryTaskStatus.FETCHING_DATA.progressRange)
        assertEquals(0.2f..0.7f, SummaryTaskStatus.ANALYZING.progressRange)
        assertEquals(0.7f..0.9f, SummaryTaskStatus.GENERATING.progressRange)
        assertEquals(0.9f..1f, SummaryTaskStatus.SAVING.progressRange)
        assertEquals(1f..1f, SummaryTaskStatus.SUCCESS.progressRange)
    }
}
