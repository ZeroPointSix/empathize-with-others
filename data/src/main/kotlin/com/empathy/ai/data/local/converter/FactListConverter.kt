package com.empathy.ai.data.local.converter

import android.util.Log
import androidx.room.TypeConverter
import com.empathy.ai.domain.model.Fact
import com.empathy.ai.domain.model.FactSource
import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.util.UUID

/**
 * Fact的JSON中间表示
 * 
 * 使用@JsonClass确保所有字段都被序列化
 * id字段设为可选（nullable），以兼容旧格式JSON
 */
@JsonClass(generateAdapter = true)
data class FactJson(
    val id: String? = null,  // 可选，兼容旧格式
    val key: String,
    val value: String,
    val timestamp: Long,
    val source: String,
    val isUserModified: Boolean = false,
    val lastModifiedTime: Long = 0L,
    val originalKey: String? = null,
    val originalValue: String? = null
)

/**
 * Fact的自定义Moshi适配器
 * 
 * 确保id字段在序列化时被包含，在反序列化时被正确读取
 * 如果JSON中没有id字段，自动生成UUID
 */
class FactJsonAdapter {
    @ToJson
    fun toJson(fact: Fact): FactJson {
        return FactJson(
            id = fact.id,  // 显式包含 id
            key = fact.key,
            value = fact.value,
            timestamp = fact.timestamp,
            source = fact.source.name,
            isUserModified = fact.isUserModified,
            lastModifiedTime = fact.lastModifiedTime,
            originalKey = fact.originalKey,
            originalValue = fact.originalValue
        )
    }

    @FromJson
    fun fromJson(json: FactJson): Fact {
        // 如果id为null或空，生成新的UUID
        val factId = if (json.id.isNullOrBlank()) {
            UUID.randomUUID().toString()
        } else {
            json.id
        }
        
        return Fact(
            id = factId,
            key = json.key,
            value = json.value,
            timestamp = json.timestamp,
            source = try { FactSource.valueOf(json.source) } catch (e: Exception) { FactSource.MANUAL },
            isUserModified = json.isUserModified,
            lastModifiedTime = if (json.lastModifiedTime > 0) json.lastModifiedTime else json.timestamp,
            originalKey = json.originalKey,
            originalValue = json.originalValue
        )
    }
}

/**
 * Fact列表类型转换器
 *
 * 用于Room数据库存储List<Fact>
 * 同时兼容旧格式Map<String, String>的迁移
 *
 * 优化：使用静态Moshi实例和预编译的Adapter，避免重复创建
 * 修复：使用自定义FactJsonAdapter确保id字段被正确序列化
 */
class FactListConverter {

    companion object {
        private const val TAG = "FactListConverter"
        
        /**
         * 静态Moshi实例，使用自定义适配器确保id字段被序列化
         */
        private val moshi: Moshi by lazy {
            Moshi.Builder()
                .add(FactJsonAdapter())  // 添加自定义适配器
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
        
        // 调试日志：序列化前
        Log.d(TAG, "========== 序列化 Facts ==========")
        Log.d(TAG, "序列化前 facts 数量: ${facts.size}")
        facts.forEachIndexed { index, fact ->
            Log.d(TAG, "  [$index] id=${fact.id}, key=${fact.key}")
        }
        
        val json = factListAdapter.toJson(facts)
        
        // 调试日志：序列化后
        Log.d(TAG, "序列化后 JSON 长度: ${json.length}")
        Log.d(TAG, "序列化后 JSON 内容(前500字符): ${json.take(500)}")
        
        return json
    }

    @TypeConverter
    fun toFactList(json: String?): List<Fact> {
        if (json.isNullOrBlank() || json == "[]") return emptyList()

        // 调试日志：反序列化前
        Log.d(TAG, "========== 反序列化 Facts ==========")
        Log.d(TAG, "反序列化前 JSON 长度: ${json.length}")
        Log.d(TAG, "反序列化前 JSON 内容(前500字符): ${json.take(500)}")

        return try {
            // 尝试解析为List<Fact>
            val result = factListAdapter.fromJson(json) ?: emptyList()
            
            // 调试日志：反序列化后
            Log.d(TAG, "反序列化后 facts 数量: ${result.size}")
            result.forEachIndexed { index, fact ->
                Log.d(TAG, "  [$index] id=${fact.id}, key=${fact.key}")
            }
            
            result
        } catch (e: Exception) {
            Log.e(TAG, "反序列化失败，尝试旧格式", e)
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
