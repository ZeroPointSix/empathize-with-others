package com.empathy.ai.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.empathy.ai.domain.repository.PrivacyRepository
import com.empathy.ai.domain.service.PrivacyEngine
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PrivacyRepositoryImpl 实现了隐私规则的数据访问层
 *
 * 【架构位置】Clean Architecture Data层
 * 【业务背景】(PRD-00003)隐私保护绝对优先原则
 *   - 隐私映射：用户自定义的敏感词替换规则
 *   - 文本脱敏：发送AI前移除或替换敏感信息
 *   - 混合策略：自定义映射 + 内置敏感词检测
 *
 * 【设计决策】(TDD-00003)
 *   - 使用SharedPreferences存储（简单轻量）
 *   - 手动JSON序列化，减少第三方依赖
 *   - 脱敏在IO线程执行，避免阻塞主线程
 *
 * 【关键逻辑】
 *   - maskText：调用PrivacyEngine.maskHybrid实现混合脱敏
 *   - unmaskText：还原脱敏文本（用于本地显示）
 *   - 脱敏不持久化：每次使用时动态脱敏，保证数据安全
 *
 * 【任务追踪】
 *   - FD-00003/Task-004: 隐私保护和数据脱敏
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

    private fun parseMappingFromJson(json: String): Map<String, String> {
        return try {
            if (json == "{}") return emptyMap()

            val mapping = mutableMapOf<String, String>()
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

    /**
     * maskText 文本脱敏处理
     *
     * 【业务规则】(PRD-00003/AC-004)隐私保护绝对优先
     *   - 所有敏感数据必须经过脱敏后才能发送给AI
     *   - 支持多种模式：自定义映射 + 内置敏感词检测
     *
     * 【脱敏策略】
     *   1. 优先使用用户自定义的隐私映射规则
     *   2. 再应用内置敏感词检测（手机号、身份证号、邮箱）
     *   3. 使用PrivacyEngine.maskHybrid实现混合脱敏
     *
     * 【安全考量】
     *   - 脱敏在IO线程执行，避免阻塞主线程
     *   - 脱敏结果不持久化，每次使用时动态脱敏
     */
    override suspend fun maskText(text: String): String {
        return withContext(Dispatchers.IO) {
            val mapping = getPrivacyMapping().getOrNull().orEmpty()
            PrivacyEngine.maskHybrid(
                rawText = text,
                privacyMapping = mapping,
                enabledPatterns = listOf("手机号", "身份证号", "邮箱")
            )
        }
    }

    override suspend fun unmaskText(maskedText: String): String {
        return withContext(Dispatchers.IO) {
            val mapping = getPrivacyMapping().getOrNull().orEmpty()
            var result = maskedText
            mapping.forEach { (original, mask) ->
                result = result.replace(mask, original)
            }
            result
        }
    }
}
