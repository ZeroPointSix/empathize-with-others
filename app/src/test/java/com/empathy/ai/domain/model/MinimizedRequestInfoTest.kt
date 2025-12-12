package com.empathy.ai.domain.model

import com.squareup.moshi.Moshi
import org.junit.Assert.*
import org.junit.Test

/**
 * MinimizedRequestInfo 数据类测试
 * 
 * 验证数据类的基本功能和序列化能力
 */
class MinimizedRequestInfoTest {
    
    private val moshi = Moshi.Builder().build()
    
    @Test
    fun `创建 MinimizedRequestInfo 实例`() {
        // Given
        val id = "test-123"
        val type = ActionType.ANALYZE
        val timestamp = System.currentTimeMillis()
        
        // When
        val requestInfo = MinimizedRequestInfo(
            id = id,
            type = type,
            timestamp = timestamp
        )
        
        // Then
        assertEquals(id, requestInfo.id)
        assertEquals(type, requestInfo.type)
        assertEquals(timestamp, requestInfo.timestamp)
    }
    
    @Test
    fun `默认时间戳应为当前时间`() {
        // Given
        val beforeTime = System.currentTimeMillis()
        
        // When
        val requestInfo = MinimizedRequestInfo(
            id = "test-123",
            type = ActionType.CHECK
        )
        
        val afterTime = System.currentTimeMillis()
        
        // Then
        assertTrue(requestInfo.timestamp >= beforeTime)
        assertTrue(requestInfo.timestamp <= afterTime)
    }
    
    @Test
    fun `序列化为 JSON`() {
        // Given
        val requestInfo = MinimizedRequestInfo(
            id = "test-456",
            type = ActionType.ANALYZE,
            timestamp = 1234567890L
        )
        
        // When
        val adapter = moshi.adapter(MinimizedRequestInfo::class.java)
        val json = adapter.toJson(requestInfo)
        
        // Then
        assertNotNull(json)
        assertTrue(json.contains("test-456"))
        assertTrue(json.contains("ANALYZE"))
        assertTrue(json.contains("1234567890"))
    }
    
    @Test
    fun `从 JSON 反序列化`() {
        // Given
        val json = """
            {
                "id": "test-789",
                "type": "CHECK",
                "timestamp": 9876543210
            }
        """.trimIndent()
        
        // When
        val adapter = moshi.adapter(MinimizedRequestInfo::class.java)
        val requestInfo = adapter.fromJson(json)
        
        // Then
        assertNotNull(requestInfo)
        assertEquals("test-789", requestInfo?.id)
        assertEquals(ActionType.CHECK, requestInfo?.type)
        assertEquals(9876543210L, requestInfo?.timestamp)
    }
    
    @Test
    fun `序列化后反序列化应保持一致`() {
        // Given
        val original = MinimizedRequestInfo(
            id = "round-trip-test",
            type = ActionType.ANALYZE,
            timestamp = 1111111111L
        )
        
        // When
        val adapter = moshi.adapter(MinimizedRequestInfo::class.java)
        val json = adapter.toJson(original)
        val deserialized = adapter.fromJson(json)
        
        // Then
        assertNotNull(deserialized)
        assertEquals(original.id, deserialized?.id)
        assertEquals(original.type, deserialized?.type)
        assertEquals(original.timestamp, deserialized?.timestamp)
    }
    
    @Test
    fun `data class 相等性测试`() {
        // Given
        val requestInfo1 = MinimizedRequestInfo(
            id = "test-123",
            type = ActionType.ANALYZE,
            timestamp = 1000L
        )
        
        val requestInfo2 = MinimizedRequestInfo(
            id = "test-123",
            type = ActionType.ANALYZE,
            timestamp = 1000L
        )
        
        val requestInfo3 = MinimizedRequestInfo(
            id = "test-456",
            type = ActionType.CHECK,
            timestamp = 2000L
        )
        
        // Then
        assertEquals(requestInfo1, requestInfo2)
        assertNotEquals(requestInfo1, requestInfo3)
    }
    
    @Test
    fun `data class copy 功能`() {
        // Given
        val original = MinimizedRequestInfo(
            id = "original",
            type = ActionType.ANALYZE,
            timestamp = 1000L
        )
        
        // When
        val copied = original.copy(id = "copied")
        
        // Then
        assertEquals("copied", copied.id)
        assertEquals(original.type, copied.type)
        assertEquals(original.timestamp, copied.timestamp)
    }
}
