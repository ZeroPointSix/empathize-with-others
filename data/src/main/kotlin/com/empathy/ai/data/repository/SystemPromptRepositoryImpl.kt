package com.empathy.ai.data.repository

import com.empathy.ai.data.local.SystemPromptStorage
import com.empathy.ai.domain.model.SceneExportData
import com.empathy.ai.domain.model.SceneSystemPrompt
import com.empathy.ai.domain.model.SystemPromptConfig
import com.empathy.ai.domain.model.SystemPromptExport
import com.empathy.ai.domain.model.SystemPromptScene
import com.empathy.ai.domain.repository.SystemPromptRepository
import com.squareup.moshi.Moshi
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 系统提示词仓库实现
 *
 * @see PRD-00033 开发者模式与系统提示词编辑
 */
@Singleton
class SystemPromptRepositoryImpl @Inject constructor(
    private val storage: SystemPromptStorage,
    private val moshi: Moshi
) : SystemPromptRepository {

    // 内存缓存
    private var cachedConfig: SystemPromptConfig? = null

    override suspend fun getConfig(): Result<SystemPromptConfig> {
        cachedConfig?.let { return Result.success(it) }

        return storage.readConfig().also { result ->
            result.getOrNull()?.let { cachedConfig = it }
        }
    }

    override suspend fun saveConfig(config: SystemPromptConfig): Result<Unit> {
        return storage.writeConfig(config).also { result ->
            if (result.isSuccess) {
                cachedConfig = config
            }
        }
    }

    override suspend fun getHeader(scene: SystemPromptScene): String? {
        return getConfig().getOrNull()?.getHeader(scene)
    }

    override suspend fun getFooter(scene: SystemPromptScene): String? {
        return getConfig().getOrNull()?.getFooter(scene)
    }

    override suspend fun updateScene(
        scene: SystemPromptScene,
        header: String?,
        footer: String?
    ): Result<Unit> {
        val currentConfig = getConfig().getOrElse { SystemPromptConfig.createEmpty() }
        val updatedConfig = currentConfig.updateScene(scene, header, footer)
        return saveConfig(updatedConfig)
    }

    override suspend fun resetScene(scene: SystemPromptScene): Result<Unit> {
        val currentConfig = getConfig().getOrElse { return Result.success(Unit) }
        val updatedConfig = currentConfig.resetScene(scene)
        return saveConfig(updatedConfig)
    }

    override suspend fun resetAll(): Result<Unit> {
        cachedConfig = null
        return storage.deleteConfig()
    }

    override suspend fun exportToJson(): Result<String> {
        return try {
            val config = getConfig().getOrElse { SystemPromptConfig.createEmpty() }
            val export = SystemPromptExport(
                exportTime = Instant.now().toString(),
                scenes = config.scenes.map { (scene, prompt) ->
                    scene.name to SceneExportData(
                        header = prompt.header,
                        footer = prompt.footer
                    )
                }.toMap()
            )
            val adapter = moshi.adapter(SystemPromptExport::class.java).indent("  ")
            Result.success(adapter.toJson(export))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun exportToMarkdown(): Result<String> {
        return try {
            val config = getConfig().getOrElse { SystemPromptConfig.createEmpty() }
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(ZoneId.systemDefault())
            
            val markdown = buildString {
                appendLine("# 系统提示词配置")
                appendLine()
                appendLine("导出时间: ${formatter.format(Instant.now())}")
                appendLine()
                appendLine("---")
                appendLine()

                SystemPromptScene.getAllScenes().forEach { scene ->
                    val sceneConfig = config.scenes[scene]
                    appendLine("## ${scene.icon} ${scene.displayName} (${scene.name})")
                    appendLine()
                    appendLine("### Header")
                    appendLine()
                    appendLine("```")
                    appendLine(sceneConfig?.header ?: "(使用默认值)")
                    appendLine("```")
                    appendLine()
                    appendLine("### Footer")
                    appendLine()
                    appendLine("```")
                    appendLine(sceneConfig?.footer ?: "(使用默认值)")
                    appendLine("```")
                    appendLine()
                    appendLine("---")
                    appendLine()
                }
            }
            Result.success(markdown)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun importFromJson(json: String): Result<Unit> {
        return try {
            val adapter = moshi.adapter(SystemPromptExport::class.java)
            val export = adapter.fromJson(json)
                ?: return Result.failure(Exception("解析JSON失败"))

            val scenes = export.scenes.mapNotNull { (key, value) ->
                try {
                    val scene = SystemPromptScene.valueOf(key)
                    scene to SceneSystemPrompt(
                        header = value.header,
                        footer = value.footer
                    )
                } catch (e: IllegalArgumentException) {
                    null // 忽略未知场景
                }
            }.toMap()

            val config = SystemPromptConfig(scenes = scenes)
            saveConfig(config)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun hasCustomConfig(): Boolean {
        return storage.hasConfig() && getConfig().getOrNull()?.scenes?.isNotEmpty() == true
    }

    override suspend fun getCustomizedScenes(): List<SystemPromptScene> {
        return getConfig().getOrNull()?.scenes?.keys?.toList() ?: emptyList()
    }
}
