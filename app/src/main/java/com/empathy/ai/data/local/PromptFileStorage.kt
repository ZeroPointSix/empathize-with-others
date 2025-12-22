package com.empathy.ai.data.local

import android.content.Context
import android.util.Log
import com.empathy.ai.di.IoDispatcher
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
 *
 * @see TDD-00015 提示词设置优化技术设计
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

        /**
         * 当前配置版本号
         *
         * 版本历史：
         * - v1: 初始版本，用户提示词包含变量占位符
         * - v2: 三层分离架构，用户提示词不再包含变量占位符
         * - v3: 简化为4个场景，CHECK合并到POLISH，EXTRACT隐藏
         */
        private const val CURRENT_CONFIG_VERSION = 3

        /**
         * 旧版本提示词中的变量占位符模式
         * 用于检测需要迁移的旧数据
         */
        private val LEGACY_VARIABLE_PATTERN = Regex("""\{+\w+\}+""")
    }

    @Volatile
    private var cachedConfig: GlobalPromptConfig? = null
    private val cacheLock = Mutex()

    private val promptsDir: File
        get() = File(context.filesDir, PROMPTS_DIR).also { it.mkdirs() }

    private val globalPromptsFile: File
        get() = File(promptsDir, GLOBAL_PROMPTS_FILE)

    // 使用自定义适配器处理Map<PromptScene, ScenePromptConfig>
    private val mapType = Types.newParameterizedType(
        Map::class.java,
        String::class.java,
        ScenePromptConfig::class.java
    )

    /**
     * 读取全局配置
     *
     * 优先从缓存读取，缓存未命中则从文件读取
     * 文件不存在时自动创建默认配置
     * 自动执行版本迁移
     *
     * @return 全局配置或错误
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
                val config = parseConfig(json)
                    ?: return@withContext tryRestoreFromBackup()

                // 检查是否发生了迁移（版本号变化）
                val originalVersion = extractVersionFromJson(json)
                if (originalVersion < CURRENT_CONFIG_VERSION) {
                    // 执行迁移
                    val migratedConfig = migrateIfNeeded(config, originalVersion)
                    // 迁移后自动保存，确保下次读取时不需要再次迁移
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
     * 执行必要的迁移
     *
     * @param config 当前配置
     * @param fromVersion 原始版本号
     * @return 迁移后的配置
     */
    private fun migrateIfNeeded(config: GlobalPromptConfig, fromVersion: Int): GlobalPromptConfig {
        var migratedConfig = config

        // 迁移版本2 -> 3：合并CHECK到POLISH
        if (fromVersion < 3) {
            migratedConfig = migrateCheckToPolish(migratedConfig)
            Log.i(TAG, "执行迁移 v$fromVersion -> v3: CHECK合并到POLISH")
        }

        return migratedConfig.copy(version = CURRENT_CONFIG_VERSION)
    }

    /**
     * 将CHECK场景的自定义提示词合并到POLISH
     *
     * 迁移规则：
     * - 如果CHECK有自定义内容（非空且非默认值），追加到POLISH
     * - 迁移后更新版本号
     * - 保留原有POLISH内容
     *
     * @param config 原配置
     * @return 迁移后的配置
     */
    @Suppress("DEPRECATION")
    private fun migrateCheckToPolish(config: GlobalPromptConfig): GlobalPromptConfig {
        val checkConfig = config.prompts[PromptScene.CHECK]
        val polishConfig = config.prompts[PromptScene.POLISH]

        // 如果CHECK有自定义内容且不是默认值（空字符串）
        if (checkConfig != null &&
            checkConfig.userPrompt.isNotBlank() &&
            checkConfig.userPrompt != DefaultPrompts.getDefault(PromptScene.CHECK)
        ) {
            // 合并到POLISH
            val mergedPrompt = buildString {
                // 保留POLISH原有内容
                val existingPolish = polishConfig?.userPrompt
                    ?: DefaultPrompts.getDefault(PromptScene.POLISH)
                if (existingPolish.isNotBlank()) {
                    append(existingPolish)
                    appendLine()
                    appendLine()
                }
                appendLine("【原安全检查指令（已合并）】")
                append(checkConfig.userPrompt)
            }

            val updatedPolishConfig = (polishConfig ?: ScenePromptConfig(
                userPrompt = DefaultPrompts.getDefault(PromptScene.POLISH),
                enabled = true
            )).copy(userPrompt = mergedPrompt)

            Log.i(TAG, "CHECK自定义内容已合并到POLISH")
            
            // 移除废弃场景，只保留活跃场景
            val updatedPrompts = config.prompts
                .filterKeys { !it.isDeprecated }
                .toMutableMap()
            updatedPrompts[PromptScene.POLISH] = updatedPolishConfig
            
            return config.copy(prompts = updatedPrompts)
        }

        // 没有需要合并的内容，只移除废弃场景
        val updatedPrompts = config.prompts.filterKeys { !it.isDeprecated }
        return config.copy(prompts = updatedPrompts)
    }

    /**
     * 写入全局配置
     *
     * 写入前自动创建备份
     *
     * @param config 要保存的配置
     * @return 成功返回Unit，失败返回错误
     */
    suspend fun writeGlobalConfig(config: GlobalPromptConfig): Result<Unit> =
        withContext(ioDispatcher) {
            cacheLock.withLock {
                try {
                    // 写入前备份
                    if (globalPromptsFile.exists()) {
                        backup.createBackup(globalPromptsFile)
                    }

                    writeGlobalConfigInternal(config)
                    cachedConfig = config
                    Result.success(Unit)
                } catch (e: Exception) {
                    Log.e(TAG, "保存配置失败", e)
                    Result.failure(PromptError.StorageError("保存配置失败", e))
                }
            }
        }

    /**
     * 使缓存失效
     *
     * 下次读取时将从文件重新加载
     * 使用Mutex保护，确保线程安全
     */
    suspend fun invalidateCache() {
        cacheLock.withLock {
            cachedConfig = null
        }
    }

    /**
     * 内部写入方法
     */
    private fun writeGlobalConfigInternal(config: GlobalPromptConfig) {
        val updatedConfig = config.copy(
            lastModified = Instant.now().toString()
        )

        // 将Map<PromptScene, ScenePromptConfig>转换为Map<String, ScenePromptConfig>
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

    /**
     * 解析配置JSON
     */
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

            // 检测是否需要迁移（旧版本数据）
            val needsLegacyMigration = version < 2

            val prompts = promptsMap.mapNotNull { (key, value) ->
                val scene = try {
                    PromptScene.valueOf(key)
                } catch (e: Exception) {
                    return@mapNotNull null
                }

                var userPrompt = value["userPrompt"] as? String ?: ""
                val enabled = value["enabled"] as? Boolean ?: true

                // 迁移逻辑：如果是旧版本且包含变量占位符，清空用户提示词
                if (needsLegacyMigration && containsLegacyVariables(userPrompt)) {
                    userPrompt = ""
                }

                @Suppress("UNCHECKED_CAST")
                val historyList = value["history"] as? List<Map<String, String>> ?: emptyList()
                val history = historyList.mapNotNull { item ->
                    val timestamp = item["timestamp"] ?: return@mapNotNull null
                    val prompt = item["userPrompt"] ?: return@mapNotNull null
                    com.empathy.ai.domain.model.PromptHistoryItem(timestamp, prompt)
                }

                scene to ScenePromptConfig(userPrompt, enabled, history)
            }.toMap()

            // 返回解析后的配置（保持原版本号，迁移在外部处理）
            GlobalPromptConfig(
                version = version,
                lastModified = lastModified,
                prompts = prompts
            )
        } catch (e: Exception) {
            Log.e(TAG, "解析配置JSON失败", e)
            null
        }
    }

    /**
     * 检测提示词是否包含旧版本的变量占位符
     *
     * 旧版本提示词包含如 {contact_name}、{{relationship_status}} 等变量
     * 新版本三层分离架构中，用户提示词不应包含这些变量
     */
    private fun containsLegacyVariables(prompt: String): Boolean {
        return LEGACY_VARIABLE_PATTERN.containsMatchIn(prompt)
    }

    /**
     * 从JSON中提取版本号（不完整解析）
     */
    private fun extractVersionFromJson(json: String): Int {
        return try {
            val mapAdapter = moshi.adapter<Map<String, Any>>(
                Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
            )
            val rootMap = mapAdapter.fromJson(json)
            (rootMap?.get("version") as? Number)?.toInt() ?: 1
        } catch (e: Exception) {
            1
        }
    }

    /**
     * 尝试从备份恢复
     */
    private suspend fun tryRestoreFromBackup(): Result<GlobalPromptConfig> {
        val restoreResult = backup.restoreFromLatestBackup(globalPromptsFile)
        if (restoreResult.isSuccess) {
            return readGlobalConfig()
        }

        // 备份恢复失败，创建默认配置
        Log.w(TAG, "备份恢复失败，创建默认配置")
        val defaultConfig = GlobalPromptConfig.createDefault { scene ->
            DefaultPrompts.getDefault(scene)
        }
        writeGlobalConfigInternal(defaultConfig)
        cachedConfig = defaultConfig
        return Result.success(defaultConfig)
    }
}
