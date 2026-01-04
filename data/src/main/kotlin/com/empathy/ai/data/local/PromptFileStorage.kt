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
 * 【存储策略选择】为什么用文件而非数据库？
 * 1. 用户可能需要导出/分享配置（文件更易处理）
 * 2. 配置结构简单，关系型数据库过于重量级
 * 3. 支持直接编辑JSON文件（极客用户需求）
 *
 * 【内存缓存的设计】
 * 每次读取都从文件加载太慢，加入内存缓存：
 * - 首次读取：从文件加载到内存
 * - 后续读取：直接从内存返回
 * - 写入时：同步更新内存缓存
 *
 * 【版本迁移策略】
 * 如果用户配置文件版本低于当前版本：
 * 1. 加载旧版本配置
 * 2. 执行迁移逻辑（如v2→v3的CHECK合并到POLISH）
 * 3. 写入新版本配置
 * 这样用户无需手动重新配置
 *
 * @see DefaultPrompts
 * @see PromptFileBackup
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

    /**
     * 内存缓存 + 互斥锁
     *
     * 【缓存一致性问题】
     * 使用Mutex（互斥锁）防止竞态条件：
     * - Thread A读取时尚未命中缓存，开始加载文件
     * - Thread B同时读取，期望共享加载结果
     * - Mutex确保只有一个线程执行加载，其他等待
     *
     * 【volatile的作用】
     * 确保cachedConfig的可见性，多线程环境下立即感知变化
     */
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


    /**
     * 读取全局配置
     *
     * 【三级缓存策略】
     * 1. 内存缓存（cachedConfig）：最快的访问路径
     * 2. 文件读取（globalPromptsFile）：次快，需要I/O
     * 3. 默认配置（DefaultPrompts兜底）：最慢但最可靠
     *
     * 【迁移触发条件】
     * 如果配置文件版本 < CURRENT_CONFIG_VERSION：
     * - 执行migrateIfNeeded()
     * - 迁移后写入新版本配置
     * - 避免下次再次迁移
     *
     * 【容灾恢复】
     * 如果配置文件损坏：
     * - 尝试从备份恢复
     * - 备份也损坏则创建默认配置
     */
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

    /**
     * 版本迁移
     *
     * 【为什么需要版本迁移】
     * - 提示词结构可能随功能演进而变化
     * - 旧版本用户的配置文件需要兼容新版本
     * - 用户无需手动重新配置
     *
     * 【当前迁移逻辑】
     * v2→v3：将CHECK场景的提示词合并到POLISH
     * 原因：安全检查和润色本质上都是消息发送前的校验
     */
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

    /**
     * 保存全局配置
     *
     * 【写入前备份】
     * 保存前先创建备份，防止配置损坏：
     * 1. 如果配置文件存在，先备份
     * 2. 然后写入新配置
     * 3. 成功后再更新内存缓存
     *
     * 【事务性写入】
     * 使用Mutex确保写入原子性，避免写入过程中被读取导致数据不一致
     */
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
