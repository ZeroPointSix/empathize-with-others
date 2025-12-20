package com.empathy.ai.data.local.converter

import androidx.room.TypeConverter
import com.empathy.ai.domain.model.Fact
import com.empathy.ai.domain.model.FactSource
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

/**
 * Fact列表类型转换器
 *
 * 用于Room数据库存储List<Fact>
 * 同时兼容旧格式Map<String, String>的迁移
 *
 * 优化：使用静态Moshi实例和预编译的Adapter，避免重复创建
 */
class FactListConverter {

    companion object {
        /**
         * 静态Moshi实例，避免重复创建
         */
        private val moshi: Moshi by lazy {
            Moshi.Builder()
                .addLast(KotlinJsonAdapterFactory())
                .build()
        }

        /**
         * List<Fact>的类型
         */
        private val factListType = Types.newParameterizedType(List::class.java, Fact::class.java)

        /**
         * Map<String, String>的类型（用于兼容旧格式）
         */
        private val mapType = Types.newParameterizedType(
            Map::class.java,
            String::class.java,
            String::class.java
        )

        /**
         * 预编译的List<Fact> Adapter
         */
        private val factListAdapter: JsonAdapter<List<Fact>> by lazy {
            moshi.adapter(factListType)
        }

        /**
         * 预编译的Map<String, String> Adapter（用于兼容旧格式）
         */
        private val mapAdapter: JsonAdapter<Map<String, String>> by lazy {
            moshi.adapter(mapType)
        }
    }

    @TypeConverter
    fun fromFactList(facts: List<Fact>?): String {
        if (facts.isNullOrEmpty()) return "[]"
        return factListAdapter.toJson(facts)
    }

    @TypeConverter
    fun toFactList(json: String?): List<Fact> {
        if (json.isNullOrBlank() || json == "[]") return emptyList()

        return try {
            // 尝试解析为List<Fact>
            factListAdapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            // 降级：尝试解析为旧格式Map<String, String>
            tryParseOldFormat(json)
        }
    }

    /**
     * 尝试解析旧格式Map<String, String>
     * 为旧数据自动生成UUID
     */
    private fun tryParseOldFormat(json: String): List<Fact> {
        return try {
            val oldMap = mapAdapter.fromJson(json) ?: emptyMap()
            val now = System.currentTimeMillis()
            oldMap.map { (key, value) ->
                Fact(
                    id = java.util.UUID.randomUUID().toString(),
                    key = key,
                    value = value,
                    timestamp = now,
                    source = FactSource.MANUAL
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
