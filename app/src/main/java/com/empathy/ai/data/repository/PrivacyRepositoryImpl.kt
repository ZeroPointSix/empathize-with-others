package com.empathy.ai.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.empathy.ai.domain.repository.PrivacyRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 隐私规则仓库实现类
 *
 * 使用 SharedPreferences 存储隐私映射规则
 * 在实际项目中，这里应该使用加密存储
 */
@Singleton
class PrivacyRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : PrivacyRepository {

    companion object {
        private const val PRIVACY_PREFS_NAME = "privacy_rules"
        private const val PRIVACY_MAPPING_KEY = "privacy_mapping"
    }

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(PRIVACY_PREFS_NAME, Context.MODE_PRIVATE)

    override suspend fun getPrivacyMapping(): Result<Map<String, String>> {
        return try {
            withContext(Dispatchers.IO) {
                val mappingJson = sharedPreferences.getString(PRIVACY_MAPPING_KEY, "{}")
                val mapping = parseMappingFromJson(mappingJson ?: "{}")
                Result.success(mapping)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addRule(original: String, mask: String): Result<Unit> {
        return try {
            withContext(Dispatchers.IO) {
                val currentMapping = getPrivacyMapping().getOrNull().orEmpty().toMutableMap()
                currentMapping[original] = mask

                val mappingJson = serializeMappingToJson(currentMapping)
                sharedPreferences.edit()
                    .putString(PRIVACY_MAPPING_KEY, mappingJson)
                    .apply()

                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeRule(original: String): Result<Unit> {
        return try {
            withContext(Dispatchers.IO) {
                val currentMapping = getPrivacyMapping().getOrNull().orEmpty().toMutableMap()
                currentMapping.remove(original)

                val mappingJson = serializeMappingToJson(currentMapping)
                sharedPreferences.edit()
                    .putString(PRIVACY_MAPPING_KEY, mappingJson)
                    .apply()

                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 简单的JSON解析（实际项目中应该使用Gson或Moshi）
     */
    private fun parseMappingFromJson(json: String): Map<String, String> {
        return try {
            // 这里使用简单的JSON解析，实际项目中应该使用专业的JSON库
            if (json == "{}") return emptyMap()

            val mapping = mutableMapOf<String, String>()
            // 简化的解析逻辑，实际项目中需要更完整的JSON解析
            json.trim()
                .removePrefix("{")
                .removeSuffix("}")
                .split(",")
                .forEach { pair ->
                    val keyValue = pair.split(":")
                    if (keyValue.size == 2) {
                        val key = keyValue[0].trim().removeSurrounding("\"")
                        val value = keyValue[1].trim().removeSurrounding("\"")
                        mapping[key] = value
                    }
                }
            mapping
        } catch (e: Exception) {
            emptyMap()
        }
    }

    /**
     * 简单的JSON序列化（实际项目中应该使用Gson或Moshi）
     */
    private fun serializeMappingToJson(mapping: Map<String, String>): String {
        return if (mapping.isEmpty()) {
            "{}"
        } else {
            val entries = mapping.map { (key, value) ->
                "\"$key\":\"$value\""
            }.joinToString(",")
            "{$entries}"
        }
    }
}