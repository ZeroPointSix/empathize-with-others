package com.empathy.ai.data.local

import android.content.Context
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
 */
@Singleton
class PromptFileStorage @Inject constructor(
    @ApplicationContext private val context: Context,
    private val moshi: Moshi,
    private val backup: PromptFileBackup,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    companion object {
        private const val PROMPTS_DIR = "prompts"
        private const val GLOBAL_PROMPTS_FILE = "global_prompts.json"
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

                cachedConfig = config
                Result.success(config)
            } catch (e: Exception) {
                tryRestoreFromBackup()
            }
        }
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

            val prompts = promptsMap.mapNotNull { (key, value) ->
                val scene = try {
                    PromptScene.valueOf(key)
                } catch (e: Exception) {
                    return@mapNotNull null
                }

                val userPrompt = value["userPrompt"] as? String ?: ""
                val enabled = value["enabled"] as? Boolean ?: true

                @Suppress("UNCHECKED_CAST")
                val historyList = value["history"] as? List<Map<String, String>> ?: emptyList()
                val history = historyList.mapNotNull { item ->
                    val timestamp = item["timestamp"] ?: return@mapNotNull null
                    val prompt = item["userPrompt"] ?: return@mapNotNull null
                    com.empathy.ai.domain.model.PromptHistoryItem(timestamp, prompt)
                }

                scene to ScenePromptConfig(userPrompt, enabled, history)
            }.toMap()

            GlobalPromptConfig(version, lastModified, prompts)
        } catch (e: Exception) {
            null
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
        val defaultConfig = GlobalPromptConfig.createDefault { scene ->
            DefaultPrompts.getDefault(scene)
        }
        writeGlobalConfigInternal(defaultConfig)
        cachedConfig = defaultConfig
        return Result.success(defaultConfig)
    }
}
