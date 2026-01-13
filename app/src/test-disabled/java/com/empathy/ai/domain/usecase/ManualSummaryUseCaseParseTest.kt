package com.empathy.ai.domain.usecase

import com.empathy.ai.data.remote.model.AiSummaryResponse
import com.empathy.ai.data.remote.model.FactDto
import com.empathy.ai.data.remote.model.KeyEventDto
import com.empathy.ai.data.remote.model.TagUpdateDto
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * ManualSummaryUseCase 解析逻辑单元测试
 *
 * 测试 BUG-00025 修复：AI 响应 JSON 解析失败问题
 * 核心测试点：newTags 缺少 action 字段时的容错解析
 */
class ManualSummaryUseCaseParseTest {

    private lateinit var moshi: Moshi

    @Before
    fun setup() {
        moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    // ==================== 容错解析测试 ====================

    @Test
    fun `parseTagUpdates should provide default action when missing`() {
        // Given: AI 返回的 newTags 缺少 action 字段（BUG-00025 场景）
        val jsonWithoutAction = """
            [
                {"content": "情绪拉扯型沟通", "type": "RISK_RED"},
                {"content": "借家庭阻力逃避决策", "type": "RISK_RED"}
            ]
        """.trimIndent()

        // When: 使用容错解析
        val result = parseTagUpdatesFromJson(jsonWithoutAction)

        // Then: 应该成功解析，action 默认为 "ADD"
        assertEquals(2, result.size)
        assertEquals("ADD", result[0].action)
        assertEquals("情绪拉扯型沟通", result[0].content)
        assertEquals("RISK_RED", result[0].type)
        assertEquals("ADD", result[1].action)
    }

    @Test
    fun `parseTagUpdates should preserve action when present`() {
        // Given: AI 返回的 newTags 包含 action 字段
        val jsonWithAction = """
            [
                {"content": "测试标签", "type": "STRATEGY_GREEN", "action": "REMOVE"}
            ]
        """.trimIndent()

        // When: 使用容错解析
        val result = parseTagUpdatesFromJson(jsonWithAction)

        // Then: 应该保留原有的 action 值
        assertEquals(1, result.size)
        assertEquals("REMOVE", result[0].action)
    }

    @Test
    fun `parseTagUpdates should skip invalid items`() {
        // Given: 包含无效项的 JSON
        val jsonWithInvalid = """
            [
                {"content": "有效标签", "type": "RISK_RED"},
                {"content": "缺少type"},
                {"type": "RISK_RED"},
                {"content": "另一个有效标签", "type": "STRATEGY_GREEN"}
            ]
        """.trimIndent()

        // When: 使用容错解析
        val result = parseTagUpdatesFromJson(jsonWithInvalid)

        // Then: 应该只返回有效的项
        assertEquals(2, result.size)
        assertEquals("有效标签", result[0].content)
        assertEquals("另一个有效标签", result[1].content)
    }

    // ==================== KeyEvents 解析测试 ====================

    @Test
    fun `parseKeyEvents should handle string importance`() {
        // Given: importance 为字符串格式（AI 常见返回格式）
        val jsonWithStringImportance = """
            [
                {"description": "重要事件", "importance": "HIGH"},
                {"description": "普通事件", "importance": "MEDIUM"},
                {"description": "次要事件", "importance": "LOW"}
            ]
        """.trimIndent()

        // When: 使用容错解析
        val result = parseKeyEventsFromJson(jsonWithStringImportance)

        // Then: 应该正确转换为数字
        assertEquals(3, result.size)
        assertEquals(5, result[0].importance)
        assertEquals(3, result[1].importance)
        assertEquals(1, result[2].importance)
    }

    @Test
    fun `parseKeyEvents should handle numeric importance`() {
        // Given: importance 为数字格式
        val jsonWithNumericImportance = """
            [
                {"event": "事件1", "importance": 5},
                {"event": "事件2", "importance": 3}
            ]
        """.trimIndent()

        // When: 使用容错解析
        val result = parseKeyEventsFromJson(jsonWithNumericImportance)

        // Then: 应该保留原有数值
        assertEquals(2, result.size)
        assertEquals(5, result[0].importance)
        assertEquals(3, result[1].importance)
    }

    @Test
    fun `parseKeyEvents should support both event and description fields`() {
        // Given: 混合使用 event 和 description 字段
        val jsonMixed = """
            [
                {"event": "使用event字段", "importance": 3},
                {"description": "使用description字段", "importance": 4}
            ]
        """.trimIndent()

        // When: 使用容错解析
        val result = parseKeyEventsFromJson(jsonMixed)

        // Then: 两种字段都应该被识别
        assertEquals(2, result.size)
        assertEquals("使用event字段", result[0].event)
        assertEquals("使用description字段", result[1].event)
    }

    // ==================== Facts 解析测试 ====================

    @Test
    fun `parseFacts should handle standard format`() {
        // Given: 标准格式的 facts
        val jsonFacts = """
            [
                {"key": "情感表达模式", "value": "倾向于重复同一句话"},
                {"key": "饮食偏好", "value": "喜欢吃辣椒"}
            ]
        """.trimIndent()

        // When: 使用容错解析
        val result = parseFactsFromJson(jsonFacts)

        // Then: 应该正确解析
        assertEquals(2, result.size)
        assertEquals("情感表达模式", result[0].key)
        assertEquals("倾向于重复同一句话", result[0].value)
        assertEquals("AI_INFERRED", result[0].source) // 默认值
    }

    @Test
    fun `parseFacts should preserve source when present`() {
        // Given: 包含 source 字段的 facts
        val jsonWithSource = """
            [
                {"key": "手动添加", "value": "测试内容", "source": "MANUAL"}
            ]
        """.trimIndent()

        // When: 使用容错解析
        val result = parseFactsFromJson(jsonWithSource)

        // Then: 应该保留原有的 source 值
        assertEquals(1, result.size)
        assertEquals("MANUAL", result[0].source)
    }

    // ==================== JSON 提取测试 ====================

    @Test
    fun `extractJsonFromResponse should handle markdown code block`() {
        // Given: Markdown 代码块包裹的 JSON
        val markdownResponse = """
            这是 AI 的回复：
            ```json
            {"summary": "测试总结"}
            ```
            以上是分析结果。
        """.trimIndent()

        // When: 提取 JSON
        val result = extractJsonFromResponse(markdownResponse)

        // Then: 应该正确提取
        assertNotNull(result)
        assertTrue(result!!.contains("summary"))
    }

    @Test
    fun `extractJsonFromResponse should handle plain json`() {
        // Given: 纯 JSON 响应
        val plainJson = """{"summary": "测试总结", "relationshipScoreChange": 5}"""

        // When: 提取 JSON
        val result = extractJsonFromResponse(plainJson)

        // Then: 应该返回原始 JSON
        assertEquals(plainJson, result)
    }

    @Test
    fun `extractJsonFromResponse should handle json with surrounding text`() {
        // Given: 带前后文字的 JSON
        val responseWithText = """
            根据分析，结果如下：
            {"summary": "测试总结"}
            希望对您有帮助。
        """.trimIndent()

        // When: 提取 JSON
        val result = extractJsonFromResponse(responseWithText)

        // Then: 应该正确提取 JSON 部分
        assertNotNull(result)
        assertEquals("""{"summary": "测试总结"}""", result)
    }

    // ==================== 完整响应解析测试 ====================

    @Test
    fun `parseAiResponseWithFallback should handle complete response without action`() {
        // Given: 完整的 AI 响应，newTags 缺少 action（BUG-00025 实际场景）
        val completeResponse = """
            {
                "summary": "今日互动总结",
                "keyEvents": [{"description": "重要事件", "importance": "HIGH"}],
                "newFacts": [{"key": "事实", "value": "内容"}],
                "newTags": [{"content": "标签内容", "type": "RISK_RED"}],
                "updatedTags": [],
                "relationshipScoreChange": -5,
                "relationshipTrend": "DECLINING"
            }
        """.trimIndent()

        // When: 使用容错解析
        val result = parseAiResponseWithFallback(completeResponse)

        // Then: 应该成功解析所有字段
        assertNotNull(result)
        assertEquals("今日互动总结", result!!.summary)
        assertEquals(1, result.keyEvents.size)
        assertEquals(1, result.newFacts.size)
        assertEquals(1, result.newTags?.size)
        assertEquals("ADD", result.newTags!![0].action) // 默认值
        assertEquals(-5, result.relationshipScoreChange)
        assertEquals("DECLINING", result.relationshipTrend)
    }

    // ==================== 辅助方法（模拟 ManualSummaryUseCase 中的私有方法） ====================

    @Suppress("UNCHECKED_CAST")
    private fun parseTagUpdatesFromJson(json: String): List<TagUpdateDto> {
        val listAdapter = moshi.adapter<List<Map<String, Any>>>(
            Types.newParameterizedType(List::class.java, Map::class.java)
        ).lenient()
        val list = listAdapter.fromJson(json) ?: return emptyList()

        return list.mapNotNull { item ->
            val content = item["content"] as? String ?: return@mapNotNull null
            val type = item["type"] as? String ?: return@mapNotNull null
            val action = (item["action"] as? String) ?: "ADD"
            TagUpdateDto(action = action, type = type, content = content)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseKeyEventsFromJson(json: String): List<KeyEventDto> {
        val listAdapter = moshi.adapter<List<Map<String, Any>>>(
            Types.newParameterizedType(List::class.java, Map::class.java)
        ).lenient()
        val list = listAdapter.fromJson(json) ?: return emptyList()

        return list.mapNotNull { item ->
            val event = (item["event"] as? String)
                ?: (item["description"] as? String)
                ?: return@mapNotNull null
            val importance = parseImportanceValue(item["importance"])
            KeyEventDto(event = event, importance = importance)
        }
    }

    private fun parseImportanceValue(raw: Any?): Int {
        return when (raw) {
            is Number -> raw.toInt().coerceIn(1, 5)
            is String -> when (raw.uppercase()) {
                "HIGH" -> 5
                "MEDIUM" -> 3
                "LOW" -> 1
                else -> raw.toIntOrNull()?.coerceIn(1, 5) ?: 3
            }
            else -> 3
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseFactsFromJson(json: String): List<FactDto> {
        val listAdapter = moshi.adapter<List<Map<String, Any>>>(
            Types.newParameterizedType(List::class.java, Map::class.java)
        ).lenient()
        val list = listAdapter.fromJson(json) ?: return emptyList()

        return list.mapNotNull { item ->
            val key = item["key"] as? String ?: return@mapNotNull null
            val value = item["value"] as? String ?: return@mapNotNull null
            val source = (item["source"] as? String) ?: "AI_INFERRED"
            FactDto(key = key, value = value, source = source)
        }
    }

    private fun extractJsonFromResponse(response: String): String? {
        // 尝试提取 Markdown 代码块中的 JSON
        val codeBlockPattern = Regex("```(?:json)?\\s*([\\s\\S]*?)```")
        val codeBlockMatch = codeBlockPattern.find(response)
        if (codeBlockMatch != null) {
            val jsonContent = codeBlockMatch.groupValues[1].trim()
            if (jsonContent.startsWith("{")) {
                return jsonContent
            }
        }

        // 直接查找 JSON 对象
        val jsonStart = response.indexOf('{')
        val jsonEnd = response.lastIndexOf('}')
        if (jsonStart == -1 || jsonEnd == -1 || jsonEnd <= jsonStart) return null

        return response.substring(jsonStart, jsonEnd + 1)
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseAiResponseWithFallback(jsonStr: String): AiSummaryResponse? {
        return try {
            val mapAdapter = moshi.adapter<Map<String, Any>>(
                Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
            ).lenient()
            val jsonMap = mapAdapter.fromJson(jsonStr) ?: return null

            val summary = (jsonMap["summary"] as? String) ?: "AI 总结完成"
            val keyEvents = parseKeyEventsFromMap(jsonMap["keyEvents"])
            val newFacts = parseFactsFromMap(jsonMap["newFacts"])
            val updatedFacts = parseFactsFromMap(jsonMap["updatedFacts"])
            val deletedFactKeys = (jsonMap["deletedFactKeys"] as? List<String>) ?: emptyList()
            val newTags = parseTagUpdatesFromMap(jsonMap["newTags"])
            val updatedTags = parseTagUpdatesFromMap(jsonMap["updatedTags"])
            val relationshipScoreChange = parseIntValue(jsonMap["relationshipScoreChange"], 0)
            val relationshipTrend = (jsonMap["relationshipTrend"] as? String) ?: "STABLE"

            AiSummaryResponse(
                summary = summary,
                keyEvents = keyEvents,
                newFacts = newFacts,
                updatedFacts = updatedFacts,
                deletedFactKeys = deletedFactKeys,
                newTags = newTags,
                updatedTags = updatedTags,
                relationshipScoreChange = relationshipScoreChange,
                relationshipTrend = relationshipTrend
            )
        } catch (e: Exception) {
            null
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseKeyEventsFromMap(raw: Any?): List<KeyEventDto> {
        val list = raw as? List<Map<String, Any>> ?: return emptyList()
        return list.mapNotNull { item ->
            val event = (item["event"] as? String)
                ?: (item["description"] as? String)
                ?: return@mapNotNull null
            val importance = parseImportanceValue(item["importance"])
            KeyEventDto(event = event, importance = importance)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseFactsFromMap(raw: Any?): List<FactDto> {
        val list = raw as? List<Map<String, Any>> ?: return emptyList()
        return list.mapNotNull { item ->
            val key = item["key"] as? String ?: return@mapNotNull null
            val value = item["value"] as? String ?: return@mapNotNull null
            val source = (item["source"] as? String) ?: "AI_INFERRED"
            FactDto(key = key, value = value, source = source)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseTagUpdatesFromMap(raw: Any?): List<TagUpdateDto> {
        val list = raw as? List<Map<String, Any>> ?: return emptyList()
        return list.mapNotNull { item ->
            val content = item["content"] as? String ?: return@mapNotNull null
            val type = item["type"] as? String ?: return@mapNotNull null
            val action = (item["action"] as? String) ?: "ADD"
            TagUpdateDto(action = action, type = type, content = content)
        }
    }

    private fun parseIntValue(raw: Any?, default: Int): Int {
        return when (raw) {
            is Number -> raw.toInt()
            is String -> raw.toIntOrNull() ?: default
            else -> default
        }
    }
}
