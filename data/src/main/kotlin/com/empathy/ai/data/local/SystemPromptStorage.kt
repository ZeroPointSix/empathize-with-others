package com.empathy.ai.data.local

import android.content.Context
import android.util.Log
import com.empathy.ai.domain.model.SceneSystemPrompt
import com.empathy.ai.domain.model.SystemPromptConfig
import com.empathy.ai.domain.model.SystemPromptScene
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 系统提示词存储
 *
 * 负责将系统提示词配置持久化到JSON文件
 *
 * @see PRD-00033 开发者模式与系统提示词编辑
 */
@Singleton
class SystemPromptStorage @Inject constructor(
    @ApplicationContext private val context: Context,
    private val moshi: Moshi,
    @com.empathy.ai.data.di.IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    companion object {
        private const val TAG = "SystemPromptStorage"
        private const val PROMPTS_DIR = "prompts"
        private const val SYSTEM_PROMPTS_FILE = "system_prompts.json"
    }

    private val promptsDir: File by lazy {
        File(context.filesDir, PROMPTS_DIR).also { it.mkdirs() }
    }

    private val systemPromptsFile: File by lazy {
        File(promptsDir, SYSTEM_PROMPTS_FILE)
    }

    /**
     * 内部存储格式
     */
    private data class StorageConfig(
        val version: Int,
        val scenes: Map<String, StorageScenePrompt>,
        val lastModified: Long
    )

    private data class StorageScenePrompt(
        val header: String?,
        val footer: String?,
        val updatedAt: Long
    )

    private val storageAdapter: JsonAdapter<StorageConfig> by lazy {
        moshi.adapter(StorageConfig::class.java).indent("  ")
    }

    /**
     * 读取配置
     */
    suspend fun readConfig(): Result<SystemPromptConfig> = withContext(ioDispatcher) {
        try {
            if (!systemPromptsFile.exists()) {
                Log.d(TAG, "配置文件不存在，返回空配置")
                return@withContext Result.success(SystemPromptConfig.createEmpty())
            }

            val json = systemPromptsFile.readText(Charsets.UTF_8)
            val storageConfig = storageAdapter.fromJson(json)
                ?: return@withContext Result.failure(Exception("解析配置文件失败"))

            // 转换为领域模型
            val scenes = storageConfig.scenes.mapNotNull { (key, value) ->
                try {
                    val scene = SystemPromptScene.valueOf(key)
                    scene to SceneSystemPrompt(
                        header = value.header,
                        footer = value.footer,
                        updatedAt = value.updatedAt
                    )
                } catch (e: IllegalArgumentException) {
                    Log.w(TAG, "忽略未知场景: $key")
                    null
                }
            }.toMap()

            val config = SystemPromptConfig(
                version = storageConfig.version,
                scenes = scenes,
                lastModified = storageConfig.lastModified
            )

            Log.d(TAG, "读取配置成功，包含${config.scenes.size}个场景配置")
            Result.success(config)
        } catch (e: Exception) {
            Log.e(TAG, "读取配置失败", e)
            Result.failure(e)
        }
    }

    /**
     * 写入配置
     */
    suspend fun writeConfig(config: SystemPromptConfig): Result<Unit> = withContext(ioDispatcher) {
        try {
            // 转换为存储格式
            val storageScenes = config.scenes.map { (scene, prompt) ->
                scene.name to StorageScenePrompt(
                    header = prompt.header,
                    footer = prompt.footer,
                    updatedAt = prompt.updatedAt
                )
            }.toMap()

            val storageConfig = StorageConfig(
                version = config.version,
                scenes = storageScenes,
                lastModified = config.lastModified
            )

            val json = storageAdapter.toJson(storageConfig)
            systemPromptsFile.writeText(json, Charsets.UTF_8)
            Log.d(TAG, "写入配置成功")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "写入配置失败", e)
            Result.failure(e)
        }
    }

    /**
     * 删除配置文件
     */
    suspend fun deleteConfig(): Result<Unit> = withContext(ioDispatcher) {
        try {
            if (systemPromptsFile.exists()) {
                systemPromptsFile.delete()
                Log.d(TAG, "删除配置文件成功")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "删除配置文件失败", e)
            Result.failure(e)
        }
    }

    /**
     * 检查配置文件是否存在
     */
    fun hasConfig(): Boolean = systemPromptsFile.exists()
}
