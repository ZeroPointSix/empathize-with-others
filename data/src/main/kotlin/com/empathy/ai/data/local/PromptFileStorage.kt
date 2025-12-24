package com.empathy.ai.data.local

import android.content.Context
import android.util.Log
import com.empathy.ai.data.di.IoDispatcher
import com.empathy.ai.domain.model.GlobalPromptConfig
import com.empathy.ai.domain.model.PromptError
import com.empathy.ai.domain.model.PromptScene
import com.empathy.ai.domain.model.ScenePromptConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 提示词JSON文件存储
 *
 * 负责全局提示词配置的读写，支持内存缓存和自动备份
 */
@Singleton
class PromptFileStorage @Inject constructor(
    @ApplicationContext private val context: Context,
    private val moshi: Moshi,
    private val backup: PromptFileBackup,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    companion object {
        private const val TAG = "PromptFileStorage"
        private const val PROMPTS_DIR = "prompts"
        private const val GLOBAL_PROMPTS_FILE = "global_prompts.json"
        private const val CURRENT_CONFIG_VERSION = 3
        private val LEGACY_VARIABLE_PATTERN = Regex("""\{+\w+\}+""")
    }

    @Volatile
    private var cachedConfig: GlobalPromptConfig? = null
    private val cacheLock = Mutex()

    private val promptsDir: File
        get() = File(context.filesDir, PROMPTS_DIR).also { it.mkdirs() }

    private val globalPromptsFile: File
        get() = File(promptsDir, GLOBAL_PROMPTS_FILE)

    private val mapType = Types.newParameterizedType(
        Map::class.java, String::class.java, ScenePromptConfig::class.java
    )


    suspend fun readGlobalConfig(): Result<GlobalPromptConfig> = withContext(ioDispatcher) {
        cachedConfig?.let { return@withContext Result.success(it) }
        cacheLock.withLock {
            cachedConfig?.let { return@withContext Result.success(it) }
            try {
                if (!globalPromptsFile.exists()) {
                    val defaultConfig = GlobalPromptConfig.createDefault { scene ->
                        DefaultPrompts.getDefault(scene)
                    }
                    writeGlobalConfigInternal(defaultConfig)
                    cachedConfig = defaultConfig
                    return@withContext Result.success(defaultConfig)
                }
                val json = globalPromptsFile.readText(Charsets.UTF_8)
                val config = parseConfig(json) ?: return@withContext tryRestoreFromBackup()
                val originalVersion = extractVersionFromJson(json)
                if (originalVersion < CURRENT_CONFIG_VERSION) {
                    val migratedConfig = migrateIfNeeded(config, originalVersion)
                    writeGlobalConfigInternal(migratedConfig)
                    cachedConfig = migratedConfig
                    Log.i(TAG, "配置已迁移: v$originalVersion -> v${migratedConfig.version}")
                    return@withContext Result.success(migratedConfig)
                }
                cachedConfig = config
                Result.success(config)
            } catch (e: Exception) {
                Log.e(TAG, "读取配置失败", e)
                tryRestoreFromBackup()
            }
        }
    }

    private fun migrateIfNeeded(config: GlobalPromptConfig, fromVersion: Int): GlobalPromptConfig {
        var migratedConfig = config
        if (fromVersion < 3) {
            migratedConfig = migrateCheckToPolish(migratedConfig)
            Log.i(TAG, "执行迁移 v$fromVersion -> v3: CHECK合并到POLISH")
        }
        return migratedConfig.copy(version = CURRENT_CONFIG_VERSION)
    }


    @Suppress("DEPRECATION")
    private fun migrateCheckToPolish(config: GlobalPromptConfig): GlobalPromptConfig {
        val checkConfig = config.prompts[PromptScene.CHECK]
        val polishConfig = config.prompts[PromptScene.POLISH]
        if (checkConfig != null && checkConfig.userPrompt.isNotBlank() &&
            checkConfig.userPrompt != DefaultPrompts.getDefault(PromptScene.CHECK)) {
            val mergedPrompt = buildString {
                val existingPolish = polishConfig?.userPrompt ?: DefaultPrompts.getDefault(PromptScene.POLISH)
                if (existingPolish.isNotBlank()) {
                    append(existingPolish)
                    appendLine()
                    appendLine()
                }
                appendLine("【原安全检查指令（已合并）】")
                append(checkConfig.userPrompt)
            }
            val updatedPolishConfig = (polishConfig ?: ScenePromptConfig(
                userPrompt = DefaultPrompts.getDefault(PromptScene.POLISH), enabled = true
            )).copy(userPrompt = mergedPrompt)
            Log.i(TAG, "CHECK自定义内容已合并到POLISH")
            val updatedPrompts = config.prompts.filterKeys { !it.isDeprecated }.toMutableMap()
            updatedPrompts[PromptScene.POLISH] = updatedPolishConfig
            return config.copy(prompts = updatedPrompts)
        }
        val updatedPrompts = config.prompts.filterKeys { !it.isDeprecated }
        return config.copy(prompts = updatedPrompts)
    }

    suspend fun writeGlobalConfig(config: GlobalPromptConfig): Result<Unit> = withContext(ioDispatcher) {
        cacheLock.withLock {
            try {
                if (globalPromptsFile.exists()) backup.createBackup(globalPromptsFile)
                writeGlobalConfigInternal(config)
                cachedConfig = config
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "保存配置失败", e)
                Result.failure(PromptError.StorageError("保存配置失败", e))
            }
        }
    }

    suspend fun invalidateCache() { cacheLock.withLock { cachedConfig = null } }


    private fun writeGlobalConfigInternal(config: GlobalPromptConfig) {
        val updatedConfig = config.copy(lastModified = Instant.now().toString())
        val stringKeyMap = updatedConfig.prompts.mapKeys { it.key.name }
        val mapAdapter = moshi.adapter<Map<String, ScenePromptConfig>>(mapType)
        val jsonObject = buildString {
            append("{")
            append("\"version\":${updatedConfig.version},")
            append("\"lastModified\":\"${updatedConfig.lastModified}\",")
            append("\"prompts\":")
            append(mapAdapter.toJson(stringKeyMap))
            append("}")
        }
        globalPromptsFile.writeText(jsonObject, Charsets.UTF_8)
    }

    private fun parseConfig(json: String): GlobalPromptConfig? {
        return try {
            val mapAdapter = moshi.adapter<Map<String, Any>>(
                Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
            )
            val rootMap = mapAdapter.fromJson(json) ?: return null
            val version = (rootMap["version"] as? Number)?.toInt() ?: 1
            val lastModified = rootMap["lastModified"] as? String ?: ""
            @Suppress("UNCHECKED_CAST")
            val promptsMap = rootMap["prompts"] as? Map<String, Map<String, Any>> ?: emptyMap()
            val needsLegacyMigration = version < 2
            val prompts = promptsMap.mapNotNull { (key, value) ->
                val scene = try { PromptScene.valueOf(key) } catch (e: Exception) { return@mapNotNull null }
                var userPrompt = value["userPrompt"] as? String ?: ""
                val enabled = value["enabled"] as? Boolean ?: true
                if (needsLegacyMigration && containsLegacyVariables(userPrompt)) userPrompt = ""
                @Suppress("UNCHECKED_CAST")
                val historyList = value["history"] as? List<Map<String, String>> ?: emptyList()
                val history = historyList.mapNotNull { item ->
                    val timestamp = item["timestamp"] ?: return@mapNotNull null
                    val prompt = item["userPrompt"] ?: return@mapNotNull null
                    com.empathy.ai.domain.model.PromptHistoryItem(timestamp, prompt)
                }
                scene to ScenePromptConfig(userPrompt, enabled, history)
            }.toMap()
            GlobalPromptConfig(version = version, lastModified = lastModified, prompts = prompts)
        } catch (e: Exception) {
            Log.e(TAG, "解析配置JSON失败", e)
            null
        }
    }

    private fun containsLegacyVariables(prompt: String): Boolean = LEGACY_VARIABLE_PATTERN.containsMatchIn(prompt)

    private fun extractVersionFromJson(json: String): Int {
        return try {
            val mapAdapter = moshi.adapter<Map<String, Any>>(
                Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
            )
            (mapAdapter.fromJson(json)?.get("version") as? Number)?.toInt() ?: 1
        } catch (e: Exception) { 1 }
    }

    private suspend fun tryRestoreFromBackup(): Result<GlobalPromptConfig> {
        val restoreResult = backup.restoreFromLatestBackup(globalPromptsFile)
        if (restoreResult.isSuccess) return readGlobalConfig()
        Log.w(TAG, "备份恢复失败，创建默认配置")
        val defaultConfig = GlobalPromptConfig.createDefault { scene -> DefaultPrompts.getDefault(scene) }
        writeGlobalConfigInternal(defaultConfig)
        cachedConfig = defaultConfig
        return Result.success(defaultConfig)
    }
}
