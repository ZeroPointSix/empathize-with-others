package com.empathy.ai.data.local.converter

import androidx.room.TypeConverter
import com.empathy.ai.domain.model.TagType
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

/**
 * Room类型转换器
 *
 * 【核心职责】SQLite与Kotlin类型系统的"翻译官"
 * SQLite只支持基本类型（Int、String等），无法直接存储：
 * - Map<String, String>
 * - TagType枚举
 *
 * 此类的任务：
 * - 入库前：将复杂对象转换为String（翻译成SQLite能理解的语言）
 * - 出库后：将String恢复为对象（翻译回Kotlin能理解的语言）
 *
 * 【Moshi vs Gson】选择Moshi的原因：
 * 1. 与Retrofit天然集成（项目已使用）
 * 2. 编译时代码生成，性能更好
 * 3. Kotlin反射支持（KotlinJsonAdapterFactory）
 *
 * 【容错设计】toTagType()的try-catch：
 * 如果数据库存了旧版本的枚举值（代码中已删除），
 * 使用默认值STRATEGY_GREEN而不是抛出异常。
 * 这种"优雅降级"确保旧数据不会导致App崩溃。
 *
 * @see com.empathy.ai.data.local.entity.ContactProfileEntity
 * @see com.empathy.ai.data.local.entity.BrainTagEntity
 */
class RoomTypeConverters {

    private val moshi = Moshi.Builder().build()
    private val mapType = Types.newParameterizedType(Map::class.java, String::class.java, String::class.java)

    /**
     * Map → JSON String（入库）
     *
     * 【示例】{"hobby": "fishing"} → "{\"hobby\": \"fishing\"}"
     *
     * 【边界处理】如果value为null，返回"{}"空JSON对象
     * 这样查询时不会返回null，而是空Map，避免NPE
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
     * JSON String → Map（出库）
     *
     * 【防御性编程】如果数据库里存了损坏的JSON：
     * - 不抛出异常
     * - 返回空Map
     * - 记录日志（可选）
     *
     * 这样即使数据损坏，App也不会崩溃。
     *
     * @param value JSON格式的字符串
     * @return Map<String, String>对象，如果为空或解析失败返回空Map
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
     * String → TagType（出库）
     *
     * 【容错设计】如果数据库里有旧版本的枚举值：
     * 1. 捕获IllegalArgumentException（枚举名不存在）
     * 2. 返回默认值STRATEGY_GREEN
     * 3. 记录日志但不阻断流程
     *
     * 这样即使数据库存储了已删除的枚举值，
     * App也不会崩溃，只是回退到默认行为。
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
