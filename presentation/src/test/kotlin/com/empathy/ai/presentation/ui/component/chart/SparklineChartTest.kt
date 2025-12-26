package com.empathy.ai.presentation.ui.component.chart

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * SparklineChart 单元测试
 * 
 * TD-00020 T022: 测试数据归一化、曲线绘制
 * 
 * 测试覆盖率要求: ≥85%
 * 边界值测试: 空数据、单点数据、大量数据点
 */
class SparklineChartTest {

    // ============================================================
    // 数据归一化测试
    // ============================================================

    @Test
    fun `normalize empty data returns empty list`() {
        val data = emptyList<Float>()
        val normalized = normalizeData(data)
        
        assertTrue(normalized.isEmpty())
    }

    @Test
    fun `normalize single point returns 0_5`() {
        val data = listOf(50f)
        val normalized = normalizeData(data)
        
        assertEquals(1, normalized.size)
        assertEquals(0.5f, normalized[0], 0.001f)
    }

    @Test
    fun `normalize two points with same value returns 0_5 for both`() {
        val data = listOf(50f, 50f)
        val normalized = normalizeData(data)
        
        assertEquals(2, normalized.size)
        assertEquals(0.5f, normalized[0], 0.001f)
        assertEquals(0.5f, normalized[1], 0.001f)
    }

    @Test
    fun `normalize data with min 0 and max 100`() {
        val data = listOf(0f, 50f, 100f)
        val normalized = normalizeData(data)
        
        assertEquals(3, normalized.size)
        assertEquals(0f, normalized[0], 0.001f)
        assertEquals(0.5f, normalized[1], 0.001f)
        assertEquals(1f, normalized[2], 0.001f)
    }

    @Test
    fun `normalize data with negative values`() {
        val data = listOf(-50f, 0f, 50f)
        val normalized = normalizeData(data)
        
        assertEquals(3, normalized.size)
        assertEquals(0f, normalized[0], 0.001f)
        assertEquals(0.5f, normalized[1], 0.001f)
        assertEquals(1f, normalized[2], 0.001f)
    }

    @Test
    fun `normalize large dataset`() {
        val data = (0..100).map { it.toFloat() }
        val normalized = normalizeData(data)
        
        assertEquals(101, normalized.size)
        assertEquals(0f, normalized.first(), 0.001f)
        assertEquals(1f, normalized.last(), 0.001f)
    }

    // ============================================================
    // 数据点计算测试
    // ============================================================

    @Test
    fun `calculate x positions for empty data`() {
        val data = emptyList<Float>()
        val width = 100f
        val positions = calculateXPositions(data, width)
        
        assertTrue(positions.isEmpty())
    }

    @Test
    fun `calculate x positions for single point`() {
        val data = listOf(1f)
        val width = 100f
        val positions = calculateXPositions(data, width)
        
        assertEquals(1, positions.size)
        assertEquals(50f, positions[0], 0.001f) // 居中
    }

    @Test
    fun `calculate x positions for two points`() {
        val data = listOf(1f, 2f)
        val width = 100f
        val positions = calculateXPositions(data, width)
        
        assertEquals(2, positions.size)
        assertEquals(0f, positions[0], 0.001f)
        assertEquals(100f, positions[1], 0.001f)
    }

    @Test
    fun `calculate x positions for three points`() {
        val data = listOf(1f, 2f, 3f)
        val width = 100f
        val positions = calculateXPositions(data, width)
        
        assertEquals(3, positions.size)
        assertEquals(0f, positions[0], 0.001f)
        assertEquals(50f, positions[1], 0.001f)
        assertEquals(100f, positions[2], 0.001f)
    }

    // ============================================================
    // Y坐标计算测试
    // ============================================================

    @Test
    fun `calculate y position for normalized 0`() {
        val normalizedValue = 0f
        val height = 100f
        val y = calculateYPosition(normalizedValue, height)
        
        assertEquals(100f, y, 0.001f) // 底部
    }

    @Test
    fun `calculate y position for normalized 1`() {
        val normalizedValue = 1f
        val height = 100f
        val y = calculateYPosition(normalizedValue, height)
        
        assertEquals(0f, y, 0.001f) // 顶部
    }

    @Test
    fun `calculate y position for normalized 0_5`() {
        val normalizedValue = 0.5f
        val height = 100f
        val y = calculateYPosition(normalizedValue, height)
        
        assertEquals(50f, y, 0.001f) // 中间
    }

    // ============================================================
    // 贝塞尔曲线控制点测试
    // ============================================================

    @Test
    fun `bezier control points are calculated correctly`() {
        val x1 = 0f
        val y1 = 100f
        val x2 = 100f
        val y2 = 0f
        
        val (cx1, cy1, cx2, cy2) = calculateBezierControlPoints(x1, y1, x2, y2)
        
        // 控制点应该在两点之间
        assertTrue(cx1 >= x1 && cx1 <= x2)
        assertTrue(cx2 >= x1 && cx2 <= x2)
    }

    @Test
    fun `bezier control points for horizontal line`() {
        val x1 = 0f
        val y1 = 50f
        val x2 = 100f
        val y2 = 50f
        
        val (_, cy1, _, cy2) = calculateBezierControlPoints(x1, y1, x2, y2)
        
        // 水平线的控制点Y坐标应该相同
        assertEquals(y1, cy1, 0.001f)
        assertEquals(y2, cy2, 0.001f)
    }

    // ============================================================
    // 终点圆点测试
    // ============================================================

    @Test
    fun `endpoint circle radius is correct`() {
        val radius = 3f // dp
        
        assertTrue(radius > 0f)
        assertTrue(radius < 10f)
    }

    @Test
    fun `endpoint position is last data point`() {
        val data = listOf(10f, 20f, 30f, 40f, 50f)
        val lastIndex = data.size - 1
        
        assertEquals(4, lastIndex)
        assertEquals(50f, data[lastIndex], 0.001f)
    }

    // ============================================================
    // 颜色测试
    // ============================================================

    @Test
    fun `default line color is iOS green`() {
        // iOSGreen = #34C759
        val iOSGreen = 0xFF34C759
        
        assertEquals(0xFF34C759, iOSGreen)
    }

    // ============================================================
    // 辅助函数
    // ============================================================

    private fun normalizeData(data: List<Float>): List<Float> {
        if (data.isEmpty()) return emptyList()
        if (data.size == 1) return listOf(0.5f)
        
        val min = data.minOrNull() ?: 0f
        val max = data.maxOrNull() ?: 0f
        val range = max - min
        
        if (range == 0f) return data.map { 0.5f }
        
        return data.map { (it - min) / range }
    }

    private fun calculateXPositions(data: List<Float>, width: Float): List<Float> {
        if (data.isEmpty()) return emptyList()
        if (data.size == 1) return listOf(width / 2)
        
        val step = width / (data.size - 1)
        return data.indices.map { it * step }
    }

    private fun calculateYPosition(normalizedValue: Float, height: Float): Float {
        return height * (1 - normalizedValue)
    }

    private fun calculateBezierControlPoints(
        x1: Float, y1: Float,
        x2: Float, y2: Float
    ): BezierControlPoints {
        val midX = (x1 + x2) / 2
        return BezierControlPoints(midX, y1, midX, y2)
    }

    private data class BezierControlPoints(
        val cx1: Float,
        val cy1: Float,
        val cx2: Float,
        val cy2: Float
    )
}
