package com.empathy.ai.data.remote.model

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.lang.reflect.Type

/**
 * MessageDto.content 自定义适配器工厂。
 *
 * 背景 (PRD-00036/3.5, TDD-00036):
 * - OpenAI Chat Completion 中 `content` 既可能是字符串，也可能是多模态数组（text + image_url）。
 * - 由于 `MessageDto.content` 是 `Any`，默认生成的 Moshi Adapter 对多态内容支持有限，因此在网络层显式注册本适配器。
 *
 * 设计约束:
 * - 写入（toJson）严格校验：多模态模式下必须是 `List<MessageContentPartDto>`，否则抛出异常，避免静默发错请求体。
 * - 读取（fromJson）兼容旧数据与异常数据：空/未知类型回退为空字符串，避免历史记录解析阻塞对话加载。
 */
class MessageDtoContentJsonAdapterFactory : JsonAdapter.Factory {
    override fun create(
        type: Type,
        annotations: Set<Annotation>,
        moshi: Moshi
    ): JsonAdapter<*>? {
        if (Types.getRawType(type) != MessageDto::class.java) return null
        val partsType = Types.newParameterizedType(List::class.java, MessageContentPartDto::class.java)
        val partsAdapter = moshi.adapter<List<MessageContentPartDto>>(partsType)
        return MessageDtoContentJsonAdapter(partsAdapter)
    }
}

private class MessageDtoContentJsonAdapter(
    private val partsAdapter: JsonAdapter<List<MessageContentPartDto>>
) : JsonAdapter<MessageDto>() {
    override fun toJson(writer: JsonWriter, value: MessageDto?) {
        if (value == null) {
            writer.nullValue()
            return
        }
        writer.beginObject()
        writer.name("role").value(value.role)
        writer.name("content")
        when (val content = value.content) {
            is String -> writer.value(content)
            is List<*> -> {
                val parts = content.filterIsInstance<MessageContentPartDto>()
                if (parts.size != content.size) {
                    throw JsonDataException("MessageDto.content contains invalid part type")
                }
                partsAdapter.toJson(writer, parts)
            }
            else -> throw JsonDataException("Unsupported MessageDto.content type: ${content::class.java}")
        }
        writer.endObject()
    }

    override fun fromJson(reader: JsonReader): MessageDto? {
        var role: String? = null
        var content: Any? = null
        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "role" -> role = reader.nextString()
                "content" -> {
                    content = when (reader.peek()) {
                        JsonReader.Token.STRING -> reader.nextString()
                        JsonReader.Token.BEGIN_ARRAY -> partsAdapter.fromJson(reader) ?: emptyList<MessageContentPartDto>()
                        JsonReader.Token.NULL -> {
                            reader.nextNull<Unit>()
                            ""
                        }
                        else -> {
                            reader.skipValue()
                            ""
                        }
                    }
                }
                else -> reader.skipValue()
            }
        }
        reader.endObject()
        if (role == null) {
            throw JsonDataException("Missing role in MessageDto")
        }
        if (content == null) {
            throw JsonDataException("Missing content in MessageDto")
        }
        return MessageDto(role = role, content = content)
    }
}
