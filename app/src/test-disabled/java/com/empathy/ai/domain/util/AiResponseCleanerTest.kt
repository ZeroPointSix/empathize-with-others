package com.empathy.ai.domain.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * AiResponseCleaner 单元测试
 *
 * 测试AI响应清洗器的各种场景
 */
class AiResponseCleanerTest {

    // ==================== cleanSuggestion 测试 ====================

    @Test
    fun `cleanSuggestion - 提取双引号内容`() {
        // 使用英文双引号
        val input = "我觉得这句不错：\"今晚可能不太行，改天请你喝奶茶呀\""
        val result = AiResponseCleaner.cleanSuggestion(input)
        
        assertEquals(1, result.size)
        assertEquals("今晚可能不太行，改天请你喝奶茶呀", result[0])
    }

    @Test
    fun `cleanSuggestion - 提取多个引号内容`() {
        val input = "建议改成这样比较委婉：\"今晚可能不太行，改天请你喝奶茶呀\"，或者试探一下：\"你是想约我吗？\""
        val result = AiResponseCleaner.cleanSuggestion(input)
        
        assertEquals(2, result.size)
        assertEquals("今晚可能不太行，改天请你喝奶茶呀", result[0])
        assertEquals("你是想约我吗？", result[1])
    }

    @Test
    fun `cleanSuggestion - 提取中文引号内容`() {
        // 使用中文引号 \u201C 和 \u201D
        val input = "可以这样回复：\u201C好的，那我们改天再约\u201D"
        val result = AiResponseCleaner.cleanSuggestion(input)
        
        assertEquals(1, result.size)
        assertEquals("好的，那我们改天再约", result[0])
    }

    @Test
    fun `cleanSuggestion - 混合引号类型`() {
        val input = "试试\"这样说\"或者\u201C那样说\u201D"
        val result = AiResponseCleaner.cleanSuggestion(input)
        
        assertEquals(2, result.size)
    }

    @Test
    fun `cleanSuggestion - 无引号内容返回原文`() {
        val input = "对方这句话其实是在测试你的底线，建议你不要直接回复，先冷处理一下。"
        val result = AiResponseCleaner.cleanSuggestion(input)
        
        assertEquals(1, result.size)
        assertEquals(input, result[0])
    }

    @Test
    fun `cleanSuggestion - 过滤太短的引号内容`() {
        val input = "他说\"好\"，你可以回复：\"那我们明天见\""
        val result = AiResponseCleaner.cleanSuggestion(input)
        
        // "好"太短被过滤，只保留"那我们明天见"
        assertEquals(1, result.size)
        assertEquals("那我们明天见", result[0])
    }

    @Test
    fun `cleanSuggestion - 空字符串返回空列表`() {
        val result = AiResponseCleaner.cleanSuggestion("")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `cleanSuggestion - 空白字符串返回空列表`() {
        val result = AiResponseCleaner.cleanSuggestion("   ")
        assertTrue(result.isEmpty())
    }

    // ==================== cleanSingleSuggestion 测试 ====================

    @Test
    fun `cleanSingleSuggestion - 返回第一条建议`() {
        val input = "建议：\"第一条\"或者\"第二条\""
        val result = AiResponseCleaner.cleanSingleSuggestion(input)
        
        assertEquals("第一条", result)
    }

    @Test
    fun `cleanSingleSuggestion - 无引号返回原文`() {
        val input = "直接说就好"
        val result = AiResponseCleaner.cleanSingleSuggestion(input)
        
        assertEquals(input, result)
    }

    // ==================== cleanAndFormat 测试 ====================

    @Test
    fun `cleanAndFormat - 多条建议编号展示`() {
        val input = "试试\"第一条\"或者\"第二条\"或者\"第三条\""
        val result = AiResponseCleaner.cleanAndFormat(input)
        
        assertTrue(result.contains("1. 第一条"))
        assertTrue(result.contains("2. 第二条"))
        assertTrue(result.contains("3. 第三条"))
    }

    @Test
    fun `cleanAndFormat - 单条建议不编号`() {
        val input = "建议：\"就这样说\""
        val result = AiResponseCleaner.cleanAndFormat(input)
        
        assertEquals("就这样说", result)
    }

    @Test
    fun `cleanAndFormat - 自定义分隔符`() {
        val input = "试试\"第一条\"或者\"第二条\""
        val result = AiResponseCleaner.cleanAndFormat(input, " | ")
        
        assertTrue(result.contains(" | "))
    }

    // ==================== hasQuotedSuggestion 测试 ====================

    @Test
    fun `hasQuotedSuggestion - 有引号内容返回true`() {
        val input = "建议：\"这样说\""
        assertTrue(AiResponseCleaner.hasQuotedSuggestion(input))
    }

    @Test
    fun `hasQuotedSuggestion - 无引号内容返回false`() {
        val input = "直接说就好"
        assertFalse(AiResponseCleaner.hasQuotedSuggestion(input))
    }

    @Test
    fun `hasQuotedSuggestion - 引号内容太短返回false`() {
        val input = "他说\"好\""
        assertFalse(AiResponseCleaner.hasQuotedSuggestion(input))
    }

    // ==================== removeExplanationPrefix 测试 ====================

    @Test
    fun `removeExplanationPrefix - 移除我觉得前缀`() {
        val input = "我觉得这句不错：好的，明天见"
        val result = AiResponseCleaner.removeExplanationPrefix(input)
        
        assertEquals("好的，明天见", result)
    }

    @Test
    fun `removeExplanationPrefix - 移除建议你前缀`() {
        val input = "建议你这样回复：谢谢关心"
        val result = AiResponseCleaner.removeExplanationPrefix(input)
        
        assertEquals("谢谢关心", result)
    }

    @Test
    fun `removeExplanationPrefix - 移除可以试试前缀`() {
        val input = "可以试试这个：我也很高兴"
        val result = AiResponseCleaner.removeExplanationPrefix(input)
        
        assertEquals("我也很高兴", result)
    }

    @Test
    fun `removeExplanationPrefix - 无前缀保持原样`() {
        val input = "好的，明天见"
        val result = AiResponseCleaner.removeExplanationPrefix(input)
        
        assertEquals(input, result)
    }

    // ==================== smartClean 测试 ====================

    @Test
    fun `smartClean - 优先提取引号内容`() {
        val input = "我觉得这句不错：\"今晚可能不太行，改天请你喝奶茶呀\""
        val result = AiResponseCleaner.smartClean(input)
        
        assertEquals("今晚可能不太行，改天请你喝奶茶呀", result)
    }

    @Test
    fun `smartClean - 无引号时移除废话前缀`() {
        val input = "建议你这样回复：谢谢你的关心，我也很高兴认识你"
        val result = AiResponseCleaner.smartClean(input)
        
        assertEquals("谢谢你的关心，我也很高兴认识你", result)
    }

    @Test
    fun `smartClean - 纯净内容保持原样`() {
        val input = "谢谢你的关心，我也很高兴认识你"
        val result = AiResponseCleaner.smartClean(input)
        
        assertEquals(input, result)
    }

    @Test
    fun `smartClean - 空字符串返回空`() {
        val result = AiResponseCleaner.smartClean("")
        assertEquals("", result)
    }

    @Test
    fun `smartClean - 复杂场景测试`() {
        // 模拟AI返回的复杂响应
        val input = "对方这句话其实是在试探你的态度。\n" +
            "我觉得你可以这样回复：\"那我们改天再约，今天确实有点累了\"\n" +
            "这样既表达了你的想法，又不会显得太生硬。"
        
        val result = AiResponseCleaner.smartClean(input)
        
        assertEquals("那我们改天再约，今天确实有点累了", result)
    }

    // ==================== 边界情况测试 ====================

    @Test
    fun `边界情况 - 只有引号没有内容`() {
        val input = "试试\"\""
        val result = AiResponseCleaner.cleanSuggestion(input)
        
        // 空引号被过滤，返回原文
        assertEquals(1, result.size)
        assertEquals(input, result[0])
    }

    @Test
    fun `边界情况 - 嵌套引号`() {
        val input = "他说\"她说'好的'\"，你可以回复：\"明白了\""
        val result = AiResponseCleaner.cleanSuggestion(input)
        
        // 应该能提取到外层引号的内容
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `边界情况 - 特殊字符`() {
        val input = "建议：\"😊好的，没问题！\""
        val result = AiResponseCleaner.cleanSuggestion(input)
        
        assertEquals(1, result.size)
        assertEquals("😊好的，没问题！", result[0])
    }
}
