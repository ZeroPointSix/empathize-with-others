package com.empathy.ai.data.local.converter

import androidx.room.TypeConverter
import com.empathy.ai.domain.model.TagType
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

/**
 * Room类型转换器
 *
 * 设计目标:
 * SQLite只支持基本类型(Int, String等),无法直接存储复杂对象如Map或枚举。
 * 此类作为"翻译官",在数据存入前将复杂对象转换为String,取出后再恢复为对象。
 *
 * 实现策略:
 * 使用Moshi进行序列化/反序列化,性能更好且与Retrofit集成。
 *
 * 转换规则:
 * 1. Map<String, String> ↔ JSON String
 * 2. TagType(Enum) ↔ String
 *
 * @see com.empathy.ai.data.local.entity.ContactProfileEntity
 * @see com.empathy.ai.data.local.entity.BrainTagEntity
 */
class RoomTypeConverters {

    private val moshi = Moshi.Builder().build()
    private val mapType = Types.newParameterizedType(Map::class.java, String::class.java, String::class.java)

    /**
     * Map → JSON String (入库)
     *
     * 示例: {"hobby": "fishing"} → "{\"hobby\": \"fishing\"}"
     *
     * @param value Map<String, String>对象
     * @return JSON格式的字符串
     */
    @TypeConverter
    fun fromStringMap(value: Map<String, String>?): String {
        val adapter = moshi.adapter<Map<String, String>>(mapType)
        return adapter.toJson(value ?: emptyMap())
    }

    /**
     * JSON String → Map (出库)
     *
     * @param value JSON格式的字符串
     * @return Map<String, String>对象,如果为空或解析失败返回空Map
     */
    @TypeConverter
    fun toStringMap(value: String?): Map<String, String> {
        if (value.isNullOrEmpty()) {
            return emptyMap()
        }

        return try {
            val adapter = moshi.adapter<Map<String, String>>(mapType)
            adapter.fromJson(value) ?: emptyMap()
        } catch (e: Exception) {
            // 防御性编程:如果数据库里存了损坏的JSON,返回空Map而不是Crash
            emptyMap()
        }
    }

    /**
     * TagType(Enum) → String (入库)
     *
     * 使用枚举的name属性存储,例如: RISK_RED → "RISK_RED"
     *
     * @param value TagType枚举值
     * @return 枚举名称字符串
     */
    @TypeConverter
    fun fromTagType(value: TagType?): String {
        return value?.name ?: TagType.STRATEGY_GREEN.name // 默认值
    }

    /**
     * String → TagType(Enum) (出库)
     *
     * 容错设计:如果数据库里存了旧版本的枚举值(代码里已删除),
     * 使用try-catch返回默认值(STRATEGY_GREEN),避免Crash。
     *
     * @param value 枚举名称字符串
     * @return TagType枚举值
     */
    @TypeConverter
    fun toTagType(value: String?): TagType {
        if (value.isNullOrEmpty()) {
            return TagType.STRATEGY_GREEN
        }

        return try {
            TagType.valueOf(value)
        } catch (e: IllegalArgumentException) {
            // 容错:如果数据库里有旧版本的枚举值,返回默认值
            TagType.STRATEGY_GREEN
        }
    }
}
