package com.empathy.ai.data.remote.model

import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * MessageDto.content 多态序列化/反序列化测试。
 *
 * 业务规则 (PRD-00036/3.5):
 * - 支持图片的模型：user message 的 content 允许为多模态数组（text + image_url）。
 * - 不支持图片或无附件：退化为字符串 content，保持兼容性。
 *
 * 设计约束 (TDD-00036):
 * - 通过 `MessageDtoContentJsonAdapterFactory` 统一处理 `Any` 字段的多态结构，避免生成 Adapter 的行为不确定性。
 */
class MessageDtoContentJsonAdapterTest {

    private lateinit var moshi: Moshi

    @Before
    fun setUp() {
        moshi = Moshi.Builder()
            .add(MessageDtoContentJsonAdapterFactory())
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    @Test
    fun `fromJson_whenContentIsString_parsesAsString`() {
        val adapter = moshi.adapter(MessageDto::class.java)
        val message = adapter.fromJson("""{"role":"user","content":"你好"}""")
        assertNotNull(message)
        assertEquals("user", message?.role)
        assertTrue(message?.content is String)
        assertEquals("你好", message?.content as String)
    }

    @Test
    fun `fromJson_whenContentIsArray_parsesAsParts`() {
        val adapter = moshi.adapter(MessageDto::class.java)
        val json = """
            {
              "role": "user",
              "content": [
                { "type": "text", "text": "请看这张图" },
                { "type": "image_url", "image_url": { "url": "data:image/jpeg;base64,AAA" } }
              ]
            }
        """.trimIndent()

        val message = adapter.fromJson(json)
        assertNotNull(message)
        assertEquals("user", message?.role)
        assertTrue(message?.content is List<*>)

        val parts = (message?.content as List<*>).filterIsInstance<MessageContentPartDto>()
        assertEquals(2, parts.size)
        assertEquals("text", parts[0].type)
        assertEquals("请看这张图", parts[0].text)
        assertEquals("image_url", parts[1].type)
        assertEquals("data:image/jpeg;base64,AAA", parts[1].imageUrl?.url)
    }

    @Test
    fun `toJson_whenContentIsString_roundTrips`() {
        val adapter = moshi.adapter(MessageDto::class.java)
        val original = MessageDto.text(role = "user", content = "hello")
        val json = adapter.toJson(original)
        val parsed = adapter.fromJson(json)
        assertNotNull(parsed)
        assertEquals("user", parsed?.role)
        assertTrue(parsed?.content is String)
        assertEquals("hello", parsed?.content as String)
    }

    @Test
    fun `toJson_whenContentIsParts_roundTrips`() {
        val adapter = moshi.adapter(MessageDto::class.java)
        val original = MessageDto.multimodal(
            role = "user",
            parts = listOf(
                MessageContentPartDto(type = "text", text = "hello"),
                MessageContentPartDto(type = "image_url", imageUrl = ImageUrlDto("data:image/jpeg;base64,AAA"))
            )
        )

        val json = adapter.toJson(original)
        val parsed = adapter.fromJson(json)
        assertNotNull(parsed)
        assertEquals("user", parsed?.role)
        assertTrue(parsed?.content is List<*>)

        val parts = (parsed?.content as List<*>).filterIsInstance<MessageContentPartDto>()
        assertEquals(2, parts.size)
        assertEquals("text", parts[0].type)
        assertEquals("hello", parts[0].text)
        assertEquals("image_url", parts[1].type)
        assertEquals("data:image/jpeg;base64,AAA", parts[1].imageUrl?.url)
    }

    @Test(expected = JsonDataException::class)
    fun `toJson_whenContentListContainsInvalidType_throws`() {
        val adapter = moshi.adapter(MessageDto::class.java)
        adapter.toJson(MessageDto(role = "user", content = listOf("bad")))
    }

    @Test(expected = JsonDataException::class)
    fun `fromJson_whenMissingRole_throws`() {
        val adapter = moshi.adapter(MessageDto::class.java)
        adapter.fromJson("""{"content":"hi"}""")
    }

    @Test(expected = JsonDataException::class)
    fun `fromJson_whenMissingContent_throws`() {
        val adapter = moshi.adapter(MessageDto::class.java)
        adapter.fromJson("""{"role":"user"}""")
    }
}

