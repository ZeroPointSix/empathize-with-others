package com.empathy.ai.domain.service

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * PrivacyEngine 单元测试
 *
 * 测试隐私脱敏功能
 */
class PrivacyEngineTest {

    @Test
    fun `mask should replace sensitive information correctly`() {
        // Given
        val rawText = "我叫张三，手机号是13800138000"
        val privacyMapping = mapOf(
            "张三" to "[NAME_01]",
            "13800138000" to "[PHONE_01]"
        )

        // When
        val maskedText = PrivacyEngine.mask(rawText, privacyMapping)

        // Then
        assertEquals("我叫[NAME_01]，手机号是[PHONE_01]", maskedText)
    }

    @Test
    fun `mask should be case insensitive`() {
        // Given
        val rawText = "I NEED MONEY"
        val privacyMapping = mapOf(
            "money" to "[SENSITIVE]"
        )

        // When
        val maskedText = PrivacyEngine.mask(rawText, privacyMapping)

        // Then
        assertEquals("I NEED [SENSITIVE]", maskedText)
    }

    @Test
    fun `mask should handle empty mapping`() {
        // Given
        val rawText = "普通文本"
        val privacyMapping = emptyMap<String, String>()

        // When
        val maskedText = PrivacyEngine.mask(rawText, privacyMapping)

        // Then
        assertEquals("普通文本", maskedText)
    }

    @Test
    fun `maskBatch should handle multiple texts`() {
        // Given
        val rawTexts = listOf(
            "我叫张三",
            "我的手机号是13800138000",
            "张三的号码是13800138000"
        )
        val privacyMapping = mapOf(
            "张三" to "[NAME_01]",
            "13800138000" to "[PHONE_01]"
        )

        // When
        val maskedTexts = PrivacyEngine.maskBatch(rawTexts, privacyMapping)

        // Then
        assertEquals(3, maskedTexts.size)
        assertEquals("我叫[NAME_01]", maskedTexts[0])
        assertEquals("我的手机号是[PHONE_01]", maskedTexts[1])
        assertEquals("[NAME_01]的号码是[PHONE_01]", maskedTexts[2])
    }
}
