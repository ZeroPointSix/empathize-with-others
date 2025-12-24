package com.empathy.ai.data.repository

import android.util.Log
import com.empathy.ai.data.local.DefaultPrompts
import com.empathy.ai.data.local.PromptFileStorage
import com.empathy.ai.data.local.dao.ContactDao
import com.empathy.ai.domain.model.GlobalPromptConfig
import com.empathy.ai.domain.model.PromptError
import com.empathy.ai.domain.model.PromptHistoryItem
import com.empathy.ai.domain.model.PromptScene
import com.empathy.ai.domain.model.PromptValidationResult
import com.empathy.ai.domain.model.ScenePromptConfig
import com.empathy.ai.domain.repository.PromptRepository
import com.empathy.ai.domain.util.PromptSanitizer
import com.empathy.ai.domain.util.PromptValidator
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 提示词仓库实现
 *
 * 实现PromptRepository接口，集成验证、安全检查和存储功能
 */
@Singleton
class PromptRepositoryImpl @Inject constructor(
    private val fileStorage: PromptFileStorage,
    private val contactDao: ContactDao,
    private val validator: PromptValidator
) : PromptRepository {

    companion object {
        private const val TAG = "PromptRepository"
    }

    private val mutex = Mutex()

    private suspend fun updateSceneConfig(
        scene: PromptScene,
        configUpdater: (ScenePromptConfig?) -> ScenePromptConfig
    ): Result<Unit> {
        return mutex.withLock {
            val currentConfig = fileStorage.readGlobalConfig().getOrElse {
                return@withLock Result.failure(
                    PromptError.StorageError("读取配置失败", it)
                )
            }

            val currentSceneConfig = currentConfig.prompts[scene]
            val newSceneConfig = configUpdater(currentSceneConfig)

            val newPrompts = currentConfig.prompts.toMutableMap()
            newPrompts[scene] = newSceneConfig

            val newConfig = currentConfig.copy(prompts = newPrompts)
            fileStorage.writeGlobalConfig(newConfig)
        }
    }

    override suspend fun getGlobalConfig(): Result<GlobalPromptConfig> {
        return fileStorage.readGlobalConfig()
    }

    override suspend fun getGlobalPrompt(scene: PromptScene): Result<String> {
        return fileStorage.readGlobalConfig().map { config ->
            config.prompts[scene]?.userPrompt ?: DefaultPrompts.getDefault(scene)
        }
    }

    override suspend fun saveGlobalPrompt(scene: PromptScene, prompt: String): Result<Unit> {
        val validationResult = validator.validate(prompt, scene, allowEmpty = true)
        if (validationResult is PromptValidationResult.Error) {
            return Result.failure(
                PromptError.ValidationError(
                    message = validationResult.message,
                    errorType = validationResult.errorType
                )
            )
        }

        if (prompt.isNotBlank()) {
            val sanitizeResult = PromptSanitizer.detectDangerousContent(prompt)
            if (!sanitizeResult.isSafe) {
                Log.w(TAG, "检测到潜在风险: ${sanitizeResult.warnings}")
            }
        }

        return updateSceneConfig(scene) { currentSceneConfig ->
            if (currentSceneConfig != null) {
                currentSceneConfig.addHistory(currentSceneConfig.userPrompt)
                    .copy(userPrompt = prompt)
            } else {
                ScenePromptConfig(userPrompt = prompt)
            }
        }
    }

    override suspend fun getContactPrompt(contactId: String): Result<String?> {
        return try {
            val prompt = contactDao.getCustomPrompt(contactId)
            Result.success(prompt)
        } catch (e: Exception) {
            Log.e(TAG, "获取联系人提示词失败", e)
            Result.failure(PromptError.DatabaseError("获取联系人提示词失败", e))
        }
    }

    override suspend fun saveContactPrompt(contactId: String, prompt: String?): Result<Unit> {
        return try {
            if (prompt != null) {
                val sanitizeResult = PromptSanitizer.detectDangerousContent(prompt)
                if (!sanitizeResult.isSafe) {
                    Log.w(TAG, "联系人提示词检测到潜在风险: ${sanitizeResult.warnings}")
                }
            }

            contactDao.updateCustomPrompt(contactId, prompt)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "保存联系人提示词失败", e)
            Result.failure(PromptError.DatabaseError("保存联系人提示词失败", e))
        }
    }

    override suspend fun restoreDefault(scene: PromptScene): Result<Unit> {
        val defaultPrompt = DefaultPrompts.getDefault(scene)
        return updateSceneConfig(scene) { currentSceneConfig ->
            if (currentSceneConfig != null) {
                currentSceneConfig.addHistory(currentSceneConfig.userPrompt)
                    .copy(userPrompt = defaultPrompt)
            } else {
                ScenePromptConfig(userPrompt = defaultPrompt)
            }
        }
    }

    override suspend fun restoreFromHistory(
        scene: PromptScene,
        historyIndex: Int
    ): Result<Unit> {
        val currentConfig = fileStorage.readGlobalConfig().getOrElse {
            return Result.failure(PromptError.StorageError("读取配置失败", it))
        }

        val sceneConfig = currentConfig.prompts[scene]
            ?: return Result.failure(PromptError.StorageError("场景配置不存在"))

        if (historyIndex < 0 || historyIndex >= sceneConfig.history.size) {
            return Result.failure(
                PromptError.ValidationError(
                    "历史记录索引无效",
                    PromptValidationResult.ErrorType.INVALID_FORMAT
                )
            )
        }

        val historyItem = sceneConfig.history[historyIndex]
        return updateSceneConfig(scene) { currentSceneConfig ->
            currentSceneConfig!!.addHistory(currentSceneConfig.userPrompt)
                .copy(userPrompt = historyItem.userPrompt)
        }
    }

    override suspend fun getHistory(scene: PromptScene): Result<List<PromptHistoryItem>> {
        return fileStorage.readGlobalConfig().map { config ->
            config.prompts[scene]?.history ?: emptyList()
        }
    }
}
