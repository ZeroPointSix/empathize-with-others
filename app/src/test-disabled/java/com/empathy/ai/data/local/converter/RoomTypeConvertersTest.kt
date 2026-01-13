package com.empathy.ai.data.local.converter

import com.empathy.ai.domain.model.TagType
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Room类型转换器测试
 *
 * 测试场景:
 * 1. Map → JSON String 转换
 * 2. JSON String → Map 转换
 * 3. Enum → String 转换
 * 4. String → Enum 转换
 * 5. 空值和边界情况处理
 */
class RoomTypeConvertersTest {

    private lateinit var converter: RoomTypeConverters

    @Before
    fun setup() {
        converter = RoomTypeConverters()
    }

    @Test
    fun `fromStringMap - 空Map应该返回空JSON对象`() {
        // Given
        val map = emptyMap<String, String>()

        // When
        val result = converter.fromStringMap(map)

        // Then
        assertEquals("{}", result)
    }

    @Test
    fun `fromStringMap - 简单Map应该正确序列化`() {
        // Given
        val map = mapOf("hobby" to "fishing", "food" to "pizza")

        // When
        val result = converter.fromStringMap(map)

        // Then
        assertTrue(result.contains("\"hobby\":\"fishing\""))
        assertTrue(result.contains("\"food\":\"pizza\""))
    }

    @Test
    fun `fromStringMap - null应该返回空JSON对象`() {
        // When
        val result = converter.fromStringMap(null)

        // Then
        assertEquals("{}", result)
    }

    @Test
    fun `toStringMap - 空字符串应该返回空Map`() {
        // When
        val result = converter.toStringMap("")

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun `toStringMap - null应该返回空Map`() {
        // When
        val result = converter.toStringMap(null)

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun `toStringMap - 有效的JSON应该反序列化为Map`() {
        // Given
        val json = "{\"hobby\":\"fishing\",\"food\":\"pizza\"}"

        // When
        val result = converter.toStringMap(json)

        // Then
        assertEquals("fishing", result["hobby"])
        assertEquals("pizza", result["food"])
    }

    @Test
    fun `toStringMap - 损坏的JSON应该返回空Map而不是崩溃`() {
        // Given
        val invalidJson = "{invalid json}"

        // When
        val result = converter.toStringMap(invalidJson)

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun `fromTagType - RISK_RED应该返回正确字符串`() {
        // When
        val result = converter.fromTagType(TagType.RISK_RED)

        // Then
        assertEquals("RISK_RED", result)
    }

    @Test
    fun `fromTagType - STRATEGY_GREEN应该返回正确字符串`() {
        // When
        val result = converter.fromTagType(TagType.STRATEGY_GREEN)

        // Then
        assertEquals("STRATEGY_GREEN", result)
    }

    @Test
    fun `fromTagType - null应该返回 STRATEGY_GREEN`() {
        // When
        val result = converter.fromTagType(null)

        // Then
        assertEquals("STRATEGY_GREEN", result)
    }

    @Test
    fun `toTagType - RISK_RED字符串应该返回RISK_RED枚举`() {
        // When
        val result = converter.toTagType("RISK_RED")

        // Then
        assertEquals(TagType.RISK_RED, result)
    }

    @Test
    fun `toTagType - STRATEGY_GREEN字符串应该返回STRATEGY_GREEN枚举`() {
        // When
        val result = converter.toTagType("STRATEGY_GREEN")

        // Then
        assertEquals(TagType.STRATEGY_GREEN, result)
    }

    @Test
    fun `toTagType - 空字符串应该返回STRATEGY_GREEN`() {
        // When
        val result = converter.toTagType("")

        // Then
        assertEquals(TagType.STRATEGY_GREEN, result)
    }

    @Test
    fun `toTagType - null应该返回STRATEGY_GREEN`() {
        // When
        val result = converter.toTagType(null)

        // Then
        assertEquals(TagType.STRATEGY_GREEN, result)
    }

    @Test
    fun `toTagType - 不存在的枚举值应该返回STRATEGY_GREEN而不是崩溃`() {
        // When
        val result = converter.toTagType("INVALID_ENUM_VALUE")

        // Then
        assertEquals(TagType.STRATEGY_GREEN, result)
    }

    @Test
    fun `完整转换循环 - Map应该可以序列化后反序列化`() {
        // Given
        val originalMap = mapOf(
            "phone" to "13812345678",
            "address" to "Beijing",
            "personality" to "吃软不吃硬"
        )

        // When
        val json = converter.fromStringMap(originalMap)
        val deserializedMap = converter.toStringMap(json)

        // Then
        assertEquals(originalMap, deserializedMap)
    }

    @Test
    fun `完整转换循环 - TagType应该可以来回转换`() {
        // Given
        val originalType = TagType.RISK_RED

        // When
        val stringValue = converter.fromTagType(originalType)
        val deserializedType = converter.toTagType(stringValue)

        // Then
        assertEquals(originalType, deserializedType)
    }
}
