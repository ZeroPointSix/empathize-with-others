package com.empathy.ai.domain.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

    // ========== 正则匹配功能测试 ==========

    @Test
    fun `maskByPattern should detect and mask phone numbers`() {
        // Given
        val rawText = "我的手机号是13800138000，另一个是13912345678"
        val phonePattern = PrivacyEngine.Patterns.PHONE_NUMBER

        // When
        val maskedText = PrivacyEngine.maskByPattern(rawText, phonePattern, "[PHONE_{index}]")

        // Then
        assertEquals("我的手机号是[PHONE_1]，另一个是[PHONE_2]", maskedText)
    }

    @Test
    fun `maskByPattern should detect and mask ID cards`() {
        // Given
        val rawText = "身份证号：11010519491231002X"
        val idCardPattern = PrivacyEngine.Patterns.ID_CARD

        // When
        val maskedText = PrivacyEngine.maskByPattern(rawText, idCardPattern, "[ID_{index}]")

        // Then
        assertEquals("身份证号：[ID_1]", maskedText)
    }

    @Test
    fun `maskByPattern should handle no matches`() {
        // Given
        val rawText = "普通文本内容"
        val phonePattern = PrivacyEngine.Patterns.PHONE_NUMBER

        // When
        val maskedText = PrivacyEngine.maskByPattern(rawText, phonePattern)

        // Then
        assertEquals("普通文本内容", maskedText)
    }

    // ========== 自动检测功能测试 ==========

    @Test
    fun `maskWithAutoDetection should detect phone numbers automatically`() {
        // Given
        val rawText = "我的手机号是13800138000，请记下"

        // When
        val maskedText = PrivacyEngine.maskWithAutoDetection(rawText, listOf("手机号"))

        // Then
        assertEquals("我的手机号是[手机号_1]，请记下", maskedText)
    }

    @Test
    fun `maskWithAutoDetection should detect multiple types of sensitive info`() {
        // Given
        val rawText = "联系我：13800138000，邮箱：test@example.com"

        // When
        val maskedText = PrivacyEngine.maskWithAutoDetection(
            rawText,
            listOf("手机号", "邮箱")
        )

        // Then
        assertTrue(maskedText.contains("[手机号_1]"))
        assertTrue(maskedText.contains("[邮箱_1]"))
        assertEquals("联系我：[手机号_1]，邮箱：[邮箱_1]", maskedText)
    }

    @Test
    fun `maskWithAutoDetection should handle overlapping patterns`() {
        // Given
        val rawText = "我的手机号13800138000，紧急电话也是13800138000"

        // When
        val maskedText = PrivacyEngine.maskWithAutoDetection(rawText, listOf("手机号"))

        // Then - 应该正确识别两个独立手机号
        assertEquals("我的手机号[手机号_1]，紧急电话也是[手机号_2]", maskedText)
    }

    // ========== 混合模式测试 ==========

    @Test
    fun `maskHybrid should combine mapping and pattern detection`() {
        // Given
        val rawText = "我叫张三，手机号是13800138000"
        val mapping = mapOf("张三" to "[NAME_01]")

        // When
        val maskedText = PrivacyEngine.maskHybrid(
            rawText,
            mapping,
            listOf("手机号")
        )

        // Then
        assertEquals("我叫[NAME_01]，手机号是[手机号_1]", maskedText)
    }

    @Test
    fun `maskHybrid should work with empty mapping but enabled patterns`() {
        // Given
        val rawText = "联系我的邮箱：test@example.com"

        // When
        val maskedText = PrivacyEngine.maskHybrid(
            rawText,
            emptyMap(),
            listOf("邮箱")
        )

        // Then
        assertEquals("联系我的邮箱：[邮箱_1]", maskedText)
    }

    // ========== 扫描检测功能测试 ==========

    @Test
    fun `detectSensitiveInfo should return empty list when no sensitive info detected`() {
        // Given
        val rawText = "普通聊天内容"

        // When
        val detected = PrivacyEngine.detectSensitiveInfo(rawText)

        // Then
        assertTrue(detected.isEmpty())
    }

    @Test
    fun `detectSensitiveInfo should detect all phone numbers`() {
        // Given
        val rawText = "我的电话13800138000，备用13912345678"

        // When
        val detected = PrivacyEngine.detectSensitiveInfo(rawText, listOf("手机号"))

        // Then
        assertEquals(2, detected.size)
        assertEquals("13800138000", detected[0].matchedText)
        assertEquals("13912345678", detected[1].matchedText)
        assertEquals("手机号", detected[0].patternName)
    }

    @Test
    fun `detectSensitiveInfo should detect ID card and phone together`() {
        // Given
        val rawText = "我的身份证11010519491231002X，手机号13800138000"

        // When
        val detected = PrivacyEngine.detectSensitiveInfo(
            rawText,
            listOf("手机号", "身份证号")
        )

        // Then
        assertEquals(2, detected.size)

        val phoneInfo = detected.find { it.patternName == "手机号" }
        val idCardInfo = detected.find { it.patternName == "身份证号" }

        assertTrue(phoneInfo != null)
        assertTrue(idCardInfo != null)
        assertEquals("13800138000", phoneInfo?.matchedText)
        assertEquals("11010519491231002X", idCardInfo?.matchedText)
    }

    @Test
    fun `detectSensitiveInfo should return sorted by position`() {
        // Given
        val rawText = "手机13800138000然后后面的身份证号11010519491231002X"

        // When
        val detected = PrivacyEngine.detectSensitiveInfo(
            rawText,
            listOf("手机号", "身份证号")
        )

        // Then
        assertEquals(2, detected.size)
        // 应该按在文本中出现顺序排序
        assertEquals("手机号", detected[0].patternName)
        assertEquals("身份证号", detected[1].patternName)
        assertTrue(detected[0].range.first < detected[1].range.first)
    }
}
