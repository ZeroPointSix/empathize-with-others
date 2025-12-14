package com.empathy.ai.data.local.converter

import com.empathy.ai.domain.model.Fact
import com.empathy.ai.domain.model.FactSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * FactListConverter类型转换器单元测试
 */
class FactListConverterTest {

    private lateinit var converter: FactListConverter

    @Before
    fun setup() {
        converter = FactListConverter()
    }

    // ==================== fromFactList 测试 ====================

    @Test
    fun `fromFactList返回空数组字符串当输入为null`() {
        val result = converter.fromFactList(null)
        assertEquals("[]", result)
    }

    @Test
    fun `fromFactList返回空数组字符串当输入为空列表`() {
        val result = converter.fromFactList(emptyList())
        assertEquals("[]", result)
    }

    @Test
    fun `fromFactList正确序列化单个Fact`() {
        val facts = listOf(
            Fact("职业", "产品经理", 1702540800000L, FactSource.MANUAL)
        )
        val result = converter.fromFactList(facts)

        assertTrue(result.contains("\"key\":\"职业\""))
        assertTrue(result.contains("\"value\":\"产品经理\""))
        assertTrue(result.contains("\"timestamp\":1702540800000"))
        assertTrue(result.contains("\"source\":\"MANUAL\""))
    }

    @Test
    fun `fromFactList正确序列化多个Facts`() {
        val facts = listOf(
            Fact("职业", "产品经理", 1702540800000L, FactSource.MANUAL),
            Fact("爱好", "摄影", 1702540800000L, FactSource.AI_INFERRED)
        )
        val result = converter.fromFactList(facts)

        assertTrue(result.contains("职业"))
        assertTrue(result.contains("产品经理"))
        assertTrue(result.contains("爱好"))
        assertTrue(result.contains("摄影"))
        assertTrue(result.contains("MANUAL"))
        assertTrue(result.contains("AI_INFERRED"))
    }

    @Test
    fun `fromFactList处理特殊字符`() {
        val facts = listOf(
            Fact("备注", "包含\"引号\"和\\斜杠", 1702540800000L, FactSource.MANUAL)
        )
        val result = converter.fromFactList(facts)

        // 验证可以正常序列化
        assertTrue(result.isNotBlank())
        assertTrue(result.startsWith("["))
        assertTrue(result.endsWith("]"))
    }

    @Test
    fun `fromFactList处理中文字符`() {
        val facts = listOf(
            Fact("性格特点", "外向、热情、善于沟通", 1702540800000L, FactSource.MANUAL)
        )
        val result = converter.fromFactList(facts)

        assertTrue(result.contains("性格特点"))
        assertTrue(result.contains("外向、热情、善于沟通"))
    }

    // ==================== toFactList 测试 ====================

    @Test
    fun `toFactList返回空列表当输入为null`() {
        val result = converter.toFactList(null)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `toFactList返回空列表当输入为空字符串`() {
        val result = converter.toFactList("")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `toFactList返回空列表当输入为空白字符串`() {
        val result = converter.toFactList("   ")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `toFactList返回空列表当输入为空数组`() {
        val result = converter.toFactList("[]")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `toFactList正确反序列化单个Fact`() {
        val json = """[{"key":"职业","value":"产品经理","timestamp":1702540800000,"source":"MANUAL"}]"""
        val result = converter.toFactList(json)

        assertEquals(1, result.size)
        assertEquals("职业", result[0].key)
        assertEquals("产品经理", result[0].value)
        assertEquals(1702540800000L, result[0].timestamp)
        assertEquals(FactSource.MANUAL, result[0].source)
    }

    @Test
    fun `toFactList正确反序列化多个Facts`() {
        val json = """[
            {"key":"职业","value":"产品经理","timestamp":1702540800000,"source":"MANUAL"},
            {"key":"爱好","value":"摄影","timestamp":1702540800000,"source":"AI_INFERRED"}
        ]"""
        val result = converter.toFactList(json)

        assertEquals(2, result.size)
        assertEquals("职业", result[0].key)
        assertEquals("爱好", result[1].key)
        assertEquals(FactSource.MANUAL, result[0].source)
        assertEquals(FactSource.AI_INFERRED, result[1].source)
    }

    @Test
    fun `toFactList兼容旧格式Map`() {
        val oldFormatJson = """{"职业":"产品经理","爱好":"摄影"}"""
        val result = converter.toFactList(oldFormatJson)

        assertEquals(2, result.size)
        // 旧格式转换后source应该是MANUAL
        assertTrue(result.all { it.source == FactSource.MANUAL })
        // 验证key和value正确
        val keys = result.map { it.key }.toSet()
        assertTrue(keys.contains("职业"))
        assertTrue(keys.contains("爱好"))
    }

    @Test
    fun `toFactList返回空列表当JSON格式无效`() {
        val invalidJson = "这不是有效的JSON"
        val result = converter.toFactList(invalidJson)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `toFactList返回空列表当JSON结构不匹配`() {
        val invalidJson = """{"invalid": 123}"""
        val result = converter.toFactList(invalidJson)
        // 应该尝试解析为旧格式，但由于值不是字符串，应该返回空列表
        assertTrue(result.isEmpty())
    }

    // ==================== 往返测试 ====================

    @Test
    fun `序列化和反序列化往返一致`() {
        val originalFacts = listOf(
            Fact("职业", "产品经理", 1702540800000L, FactSource.MANUAL),
            Fact("爱好", "摄影", 1702540800000L, FactSource.AI_INFERRED),
            Fact("性格", "外向", 1702540800000L, FactSource.MANUAL)
        )

        val json = converter.fromFactList(originalFacts)
        val restoredFacts = converter.toFactList(json)

        assertEquals(originalFacts.size, restoredFacts.size)
        for (i in originalFacts.indices) {
            assertEquals(originalFacts[i].key, restoredFacts[i].key)
            assertEquals(originalFacts[i].value, restoredFacts[i].value)
            assertEquals(originalFacts[i].timestamp, restoredFacts[i].timestamp)
            assertEquals(originalFacts[i].source, restoredFacts[i].source)
        }
    }

    @Test
    fun `空列表往返一致`() {
        val originalFacts = emptyList<Fact>()
        val json = converter.fromFactList(originalFacts)
        val restoredFacts = converter.toFactList(json)

        assertTrue(restoredFacts.isEmpty())
    }

    @Test
    fun `处理emoji字符往返一致`() {
        val originalFacts = listOf(
            Fact("心情", "开心😀", 1702540800000L, FactSource.MANUAL)
        )

        val json = converter.fromFactList(originalFacts)
        val restoredFacts = converter.toFactList(json)

        assertEquals(1, restoredFacts.size)
        assertEquals("心情", restoredFacts[0].key)
        assertEquals("开心😀", restoredFacts[0].value)
    }

    @Test
    fun `处理换行符往返一致`() {
        val originalFacts = listOf(
            Fact("备注", "第一行\n第二行", 1702540800000L, FactSource.MANUAL)
        )

        val json = converter.fromFactList(originalFacts)
        val restoredFacts = converter.toFactList(json)

        assertEquals(1, restoredFacts.size)
        assertEquals("第一行\n第二行", restoredFacts[0].value)
    }
}
